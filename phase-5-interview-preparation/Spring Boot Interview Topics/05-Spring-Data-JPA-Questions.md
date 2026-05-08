# Spring Data JPA Questions

## Status: Not Started

---

## Table of Contents

1. [JPA Mental Model Refresher](#jpa-mental-model-refresher)
2. [N+1 Problem](#n1-problem)
3. [`save()` vs `saveAll()`](#save-vs-saveall)
4. [`flush()` vs Commit](#flush-vs-commit)
5. [`clear()` vs `evict()`](#clear-vs-evict)
6. [First-Level Cache Behaviour](#first-level-cache-behaviour)
7. [`@Transactional` on Repository Methods](#transactional-on-repository-methods)
8. [Dirty Checking](#dirty-checking)
9. [`findById` Lazy Init Trap](#findbyid-lazy-init-trap)
10. [`@Modifying @Query` Bulk Updates](#modifying-query-bulk-updates)
11. [Custom Repository Implementations](#custom-repository-implementations)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

> Cross-ref: `phase-4 / Performance & Optimization / 03-Query-Optimization.md`.

---

## JPA Mental Model Refresher

```
EntityManager (per request, per @Transactional)
   └── Persistence Context (1st level cache)
          └── managed entities (id → entity object map)
   ↓
DataSource → DB
```

### States

```
Transient   →  persist()  →  Managed   ←  detach() / clear()  →  Detached
                              ↓ remove()
                            Removed
```

---

## N+1 Problem

### The classic

```java
List<Order> orders = orderRepo.findAll();    // 1 query
for (Order o : orders) {
    System.out.println(o.getCustomer().getName());   // N queries — one per order
}
```

→ 100 orders = **101 queries**. Killer for performance.

### Cause

`@ManyToOne(fetch = LAZY)` (default for `@ManyToOne` in Hibernate is actually EAGER, but `@OneToMany` is LAZY) — accessing relation triggers separate query.

### Fixes

#### 1. `JOIN FETCH`

```java
@Query("SELECT o FROM Order o JOIN FETCH o.customer")
List<Order> findAllWithCustomer();
```

#### 2. `@EntityGraph`

```java
@EntityGraph(attributePaths = {"customer", "items"})
List<Order> findAll();
```

#### 3. `@BatchSize`

```java
@Entity
public class Order {
    @ManyToOne(fetch = LAZY)
    @BatchSize(size = 50)
    private Customer customer;
}
```

→ Loads up to 50 customers per batch — N+1 → ~N/50.

#### 4. Disable open-in-view + force fetch in service

```yaml
spring.jpa.open-in-view: false
```

→ Forces explicit fetch in service layer (lazy access in view → exception).

---

## `save()` vs `saveAll()`

```java
repo.save(order);
repo.saveAll(List.of(o1, o2, o3));
```

### Source code-ish

```java
default <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
    List<S> result = new ArrayList<>();
    for (S e : entities) result.add(save(e));
    return result;
}
```

→ `saveAll` internally calls `save()` per entity.

### Optimization

For **batch insert** to actually use JDBC batch:

```yaml
spring.jpa.properties.hibernate.jdbc.batch_size: 50
spring.jpa.properties.hibernate.order_inserts: true
spring.jpa.properties.hibernate.order_updates: true
```

→ Then `saveAll` benefits from batched SQL.

### `save()` semantics

- New entity (id null / `isNew()`) → `persist`
- Existing → `merge` (returns managed copy → use returned instance!)

```java
Order saved = repo.save(o);   // use saved, not o, for managed access
```

---

## `flush()` vs Commit

| | `flush()` | Commit |
|--|-----------|--------|
| What | Push pending SQL to DB | End transaction |
| Visibility | DB sees in-progress (not committed) | Permanent |
| Triggers | `entityManager.flush()` / before queries | Tx end |

### When to flush manually?

- Need DB-generated id mid-transaction
- Native query needs latest state of entities
- Trigger DB constraint check before more logic

```java
repo.save(o);
repo.flush();        // SQL runs now; constraint violation surfaces here
```

→ But still **rollback if TX fails**.

---

## `clear()` vs `evict()`

| Method | Effect |
|--------|--------|
| `entityManager.clear()` | All managed entities → detached |
| `entityManager.detach(e)` | Single entity detached |
| Hibernate `Session.evict(e)` | Single entity removed from PC |

### Use case — bulk processing

```java
@Transactional
public void migrateAll() {
    int batchSize = 50;
    for (int i = 0; ; i++) {
        List<Foo> page = repo.findPage(i, batchSize);
        if (page.isEmpty()) break;
        page.forEach(this::transform);
        em.flush();
        em.clear();           // free PC memory; avoid OOM
    }
}
```

→ Without `clear()`, PC grows unbounded → memory exhaustion + slow dirty checking.

---

## First-Level Cache Behaviour

### Per persistence context (i.e., per `@Transactional`)

```java
@Transactional
public void demo() {
    Order o1 = repo.findById(1L).orElseThrow();   // SQL fired
    Order o2 = repo.findById(1L).orElseThrow();   // ✅ no SQL — cache hit
    System.out.println(o1 == o2);                  // true (same managed instance)
}
```

→ Useful for repeated lookups within transaction.

### Across transactions?

No. Each TX = fresh PC. (Second-level cache separate concept — `@Cache`, EhCache, etc.)

### `getReference()` (proxy without DB hit)

```java
em.getReference(Order.class, 1L);    // returns proxy; SQL deferred to first field access
```

→ Useful for setting FK without loading full entity.

---

## `@Transactional` on Repository Methods

### Spring Data default

`SimpleJpaRepository` methods are **`@Transactional`** internally:

- Read methods → `@Transactional(readOnly = true)`
- Write methods → `@Transactional`

→ Boot apps generally don't need `@Transactional` on repo methods directly.

### Service-level `@Transactional`

```java
@Service
@Transactional
public class OrderService { ... }
```

→ Per-method override fine. Service `@Transactional` outermost; repo's TX joins (REQUIRED default).

### Custom repository methods

```java
public interface OrderRepoCustom { void bulkInsert(List<Order> os); }

public class OrderRepoCustomImpl implements OrderRepoCustom {
    @PersistenceContext private EntityManager em;
    
    @Override
    public void bulkInsert(List<Order> os) { ... }   // ❌ no auto @Transactional
}
```

→ Caller must wrap (`@Transactional` on service) or explicitly add to method.

---

## Dirty Checking

```java
@Transactional
public void updateName(Long id, String name) {
    Order o = repo.findById(id).orElseThrow();
    o.setName(name);
    // No save() needed — Hibernate auto-detects + UPDATE on flush
}
```

→ Spring Data `save()` `merge` ke through redundant call sometimes; managed entity dirty checked anyway.

### Disable for read-heavy

```java
@Transactional(readOnly = true)
public List<Order> recent() { ... }
```

→ No dirty check overhead.

---

## `findById` Lazy Init Trap

```java
@Transactional(readOnly = true)
Optional<Order> get(Long id) { return repo.findById(id); }

// Caller (no TX)
Order o = service.get(1L).orElseThrow();
String c = o.getCustomer().getName();   // ❌ LazyInitializationException
```

→ TX closed → PC closed → lazy proxy can't load.

### Fixes

- Eager fetch via `@EntityGraph`
- Service returns DTO (preferred — encapsulation)
- `@Transactional` extends to caller scope
- `spring.jpa.open-in-view: true` (default — convenient but **anti-pattern in production**: causes N+1 + holds connection during view rendering)

---

## `@Modifying @Query` Bulk Updates

```java
@Modifying
@Query("UPDATE User u SET u.active = false WHERE u.lastLogin < :date")
int deactivateInactive(@Param("date") LocalDateTime date);
```

⚠️ Bulk update **bypasses persistence context** → existing managed entities **not updated**. Stale state.

### Fix

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("...")
int deactivateInactive(...);
```

→ Or call `em.clear()` after bulk op.

---

## Custom Repository Implementations

### Pattern

```java
public interface OrderRepository extends JpaRepository<Order, Long>, OrderRepositoryCustom { }

public interface OrderRepositoryCustom {
    List<Order> searchComplex(...);
}

public class OrderRepositoryImpl implements OrderRepositoryCustom {
    @PersistenceContext private EntityManager em;
    public List<Order> searchComplex(...) { ... }   // CriteriaBuilder / native SQL
}
```

→ Naming convention: `Impl` suffix. Spring Data merges automatically.

---

## Common Output Traps

### Q1. N+1 demo

(See section above.)

### Q2. Dirty check post-detach

```java
@Transactional
public void demo() {
    Order o = repo.findById(1L).get();
    em.detach(o);
    o.setName("X");
    // detached → no UPDATE; change LOST on commit
}
```

### Q3. Bulk update stale

```java
@Modifying @Query("UPDATE User u SET u.active=false")
int deactivateAll();

@Transactional
public void demo() {
    User u = userRepo.findById(1L).get();    // active=true loaded
    userRepo.deactivateAll();                 // bulk update bypasses PC
    System.out.println(u.isActive());         // still true! (stale)
}
```

### Q4. `findById` returns `Optional<Entity>` even outside TX (entity itself can be returned), but lazy access fails.

---

## Pitfalls

1. **N+1** — most common DB perf bug.
2. **`open-in-view: true`** in production — N+1 silently + connection held during view rendering.
3. **Bulk update** — clear PC after.
4. **Forgetting `save()` returned entity** — `merge` returns new managed copy.
5. **`@Modifying` without `@Transactional`** — Spring throws `InvalidDataAccessApiUsageException`.
6. **Equality on JPA entities** — id-based; pre-persist null id → equality bug (cross-ref `04-Equals-and-HashCode.md`).
7. **Mutable state on lazy proxy** — works only inside TX.
8. **Pagination with `JOIN FETCH`** — Hibernate warns "firstResult/maxResults specified with collection fetch; applying in memory" — use `@EntityGraph` for collections + `Slice`.
9. **`saveAll` without batch config** — no SQL batch; 1000 inserts = 1000 round trips.
10. **TX timeout missing** — long-running query holds connection.
11. **Repository method calling another repository method via `this`** — same self-invocation rule, but for repo proxies less common.
12. **OSIV + slow view rendering** → connection pool starvation.

---

## Cheat Sheet

| Issue | Fix |
|-------|-----|
| N+1 | JOIN FETCH / @EntityGraph / @BatchSize |
| Bulk PC stale | `clearAutomatically = true` / `em.clear()` |
| Lazy outside TX | DTO / EntityGraph |
| Slow batch insert | hibernate.jdbc.batch_size + saveAll |
| Stale entity after merge | use returned object |
| Read perf | `@Transactional(readOnly = true)` |
| OSIV pitfalls | `spring.jpa.open-in-view: false` |

| Method | Effect |
|--------|--------|
| `flush()` | Push SQL to DB |
| `clear()` | Detach all |
| `evict(e)` | Detach one |
| `getReference(id)` | Proxy without SQL |

---

## Practice

1. Reproduce N+1; fix via JOIN FETCH; verify single SQL.
2. `saveAll` batch tune via `hibernate.jdbc.batch_size` — count network round trips.
3. Open-in-view ON vs OFF — observe lazy access behaviour.
4. Bulk `@Modifying` update; show stale managed entity; fix with `clearAutomatically=true`.
5. Custom repo impl with EntityManager + CriteriaBuilder.
6. `flush() + clear()` pattern in batch ETL.
