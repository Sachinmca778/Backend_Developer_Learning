# @Transactional Gotchas

## Status: Not Started

---

## Table of Contents

1. [Why This is Bug Hot-Spot](#why-this-is-bug-hot-spot)
2. [How `@Transactional` Works (Proxy)](#how-transactional-works-proxy)
3. [Self-Invocation Trap](#self-invocation-trap)
4. [Self-Invocation Fixes](#self-invocation-fixes)
5. [Default Rollback Rule](#default-rollback-rule)
6. [`rollbackFor` / `noRollbackFor`](#rollbackfor--norollbackfor)
7. [`readOnly = true` Optimization](#readonly--true-optimization)
8. [Propagation Modes](#propagation-modes)
9. [Isolation Levels](#isolation-levels)
10. [`@Transactional` on Private/Final/Static — No-Op](#transactional-on-privatefinalstatic--no-op)
11. [Try-Catch Inside `@Transactional`](#try-catch-inside-transactional)
12. [`@Transactional` on Tests](#transactional-on-tests)
13. [Common Output Traps](#common-output-traps)
14. [Pitfalls](#pitfalls)
15. [Cheat Sheet](#cheat-sheet)

---

## Why This is Bug Hot-Spot

> "TX commit ho gaya jab error hua" — production data loss / inconsistency. Real life mein **most asked + most buggy** Spring topic.

---

## How `@Transactional` Works (Proxy)

```
Caller → ProxyBean ────► [TX Interceptor begin]
                          ↓
                          target.method()
                          ↓
                         [TX commit / rollback]
                         ↓
                         return / throw
```

→ Spring **proxy banata** hai aapke bean ka. Proxy mein TX advice attach hota.

→ Cross-ref: `06-AOP-Questions.md` for proxy mechanics.

---

## Self-Invocation Trap

```java
@Service
public class OrderService {

    public void placeOrder(Order o) {
        // No @Transactional here
        validate(o);
        save(o);                  // ⚠️ direct call → proxy bypassed!
    }

    @Transactional
    public void save(Order o) {
        repo.save(o);
    }
}
```

### Issue

`placeOrder` ne `this.save(o)` call kiya — proxy involved nahi → **`@Transactional` ignore**, no TX!

### Why?

Spring proxy intercept karta hai **external** calls (caller → bean). `this.method()` JVM-level direct invocation hai — proxy doesn't see it.

---

## Self-Invocation Fixes

### Fix 1 — Move method to separate bean (recommended)

```java
@Service
class OrderService {
    private final OrderPersistence persistence;
    public void placeOrder(Order o) { persistence.save(o); }
}

@Service
class OrderPersistence {
    @Transactional
    public void save(Order o) { repo.save(o); }
}
```

### Fix 2 — Use `AopContext.currentProxy()`

```java
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class App { }

@Service
class OrderService {
    public void placeOrder(Order o) {
        ((OrderService) AopContext.currentProxy()).save(o);   // calls via proxy
    }
    
    @Transactional
    public void save(Order o) { repo.save(o); }
}
```

⚠️ Casting + tight coupling — code smell.

### Fix 3 — Self-injection (slightly cleaner)

```java
@Service
class OrderService {
    @Lazy @Autowired private OrderService self;
    
    public void placeOrder(Order o) { self.save(o); }
    
    @Transactional public void save(Order o) { ... }
}
```

→ `@Lazy` zaroori — circular dep avoid.

### Fix 4 — Async event for outside scope

```java
publisher.publishEvent(new OrderCreatedEvent(o));   // listener can be @Transactional
```

---

## Default Rollback Rule

> "TX rollback hota hai sirf **`RuntimeException`** ya **`Error`** par. Checked exceptions par **commit** hota hai by default."

### Demo

```java
@Transactional
public void doIt() throws IOException {
    repo.save(...);
    throw new IOException("oops");    // ← checked → COMMIT happens!
}
```

→ Surprising. JTA / EJB legacy from 2003 carries forward.

---

## `rollbackFor` / `noRollbackFor`

### Force rollback for checked

```java
@Transactional(rollbackFor = Exception.class)
public void doIt() throws IOException { ... }
```

### Most common interview answer

```java
@Transactional(rollbackFor = Exception.class)     // rollback for any exception
```

→ Many teams set this on a custom `@AppTransactional` meta-annotation.

### Skip rollback for specific RT exception

```java
@Transactional(noRollbackFor = NotFoundException.class)
```

### Best practice

Codebase-wide convention; **don't mix** rules per method without reason.

---

## `readOnly = true` Optimization

```java
@Transactional(readOnly = true)
public List<Order> findRecent() { ... }
```

### What changes

| Aspect | Effect |
|--------|--------|
| Hibernate dirty checking | Skipped → fewer SELECTs/UPDATEs |
| Flush mode | `MANUAL` (no auto-flush) |
| Connection hint | DB driver "read-only" mode |
| Query plan / replicas | Possible routing to read replica with custom AbstractRoutingDataSource |

### Important nuance

- Hibernate skips `update` SQL even if you mutate entities — surprises beginners.
- Don't make a **write method `readOnly = true`** — silently ignored writes.

---

## Propagation Modes

| Mode | Behaviour |
|------|-----------|
| `REQUIRED` (default) | Join existing TX or create new |
| `REQUIRES_NEW` | **Suspend** outer; new TX always |
| `SUPPORTS` | Join if exists; else non-tx |
| `NOT_SUPPORTED` | Suspend outer; run non-tx |
| `MANDATORY` | Must have outer TX, else error |
| `NEVER` | Must have **no** TX, else error |
| `NESTED` | Savepoint within outer TX (DB support needed) |

### `REQUIRES_NEW` use case

Audit log hamesha commit karna hai, even if main TX rolls back:

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logAudit(String msg) { auditRepo.save(new Audit(msg)); }
```

→ Outer TX rollback ho jaye → audit row still committed. **But:** self-invocation rule applies (separate bean / proxy).

---

## Isolation Levels

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
```

| Level | Phenomena prevented |
|-------|---------------------|
| READ_UNCOMMITTED | None (dirty reads possible) |
| READ_COMMITTED (Postgres default) | Dirty reads |
| REPEATABLE_READ (MySQL InnoDB default) | Non-repeatable reads |
| SERIALIZABLE | All — but slowest |

→ Database-specific defaults vary; usually leave default + handle conflicts at app level.

---

## `@Transactional` on Private/Final/Static — No-Op

| Modifier | Why no-op (CGLIB / JDK proxy) |
|----------|-------------------------------|
| `private` | Proxy can't override / intercept |
| `final` | CGLIB can't subclass / override |
| `static` | Not on instance, no proxy involvement |

→ Always **`public` non-final** for `@Transactional` to work.

---

## Try-Catch Inside `@Transactional`

```java
@Transactional
public void doIt() {
    try {
        risky();
    } catch (RuntimeException e) {
        log.warn("ignored", e);     // ❌ TX won't rollback
    }
}
```

→ Spring deciding rollback dekha **method exit time** — exception swallowed → "TX completed normally" → commit.

### Fix

```java
catch (RuntimeException e) {
    log.warn("ignored", e);
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
}
```

→ Or rethrow.

---

## `@Transactional` on Tests

```java
@SpringBootTest
@Transactional
class UserServiceTest {
    // Each test rolls back automatically — clean DB
}
```

⚠️ Trap: if production code uses `Propagation.REQUIRES_NEW`, test method's rollback **won't undo** it (because nested outer TX). Use `@Sql` cleanup or no `@Transactional`.

---

## Common Output Traps

### Q1. Self-invocation TX skip

(See section above.)

### Q2. Checked exception, no rollbackFor

```java
@Transactional
public void m() throws IOException {
    repo.save(x);
    throw new IOException();        // commit happens
}
```

### Q3. Caught + swallowed

Try-catch trap.

### Q4. Outer ROLLBACK vs inner REQUIRES_NEW

```java
@Transactional public void outer() {
    auditService.log("start");      // REQUIRES_NEW → committed
    throw new RuntimeException();   // outer rolls back
}
```

→ Audit row remains.

### Q5. `@Transactional` on private

No effect — silent.

---

## Pitfalls

1. **Self-invocation** — most common bug.
2. **Checked exception** without `rollbackFor`.
3. **Try-catch swallowing** in `@Transactional`.
4. **Private/final/static** + `@Transactional` — no-op.
5. **Long-running TX** — DB connection pool starved (pool exhausted earlier in calling external HTTP inside TX, see Performance folder).
6. **`readOnly = true` on write method** — Hibernate silently skips updates.
7. **Test forgetting `REQUIRES_NEW` rollback** — leftover data.
8. **`@Transactional` on `Repository`** — Spring Data already provides one; layered double TX (usually fine but redundant).
9. **Using `Propagation.SUPPORTS`** for writes — non-TX in some contexts → no rollback safety.
10. **Multiple data sources** — TX manager mismatch; need `@Transactional("primaryTxManager")`.

---

## Cheat Sheet

| Trap | Fix |
|------|-----|
| Self-invocation | Separate bean / `AopContext.currentProxy()` / event |
| Checked → no rollback | `rollbackFor = Exception.class` |
| Try-catch eats exception | `setRollbackOnly()` or rethrow |
| Read query slow | `readOnly = true` |
| Audit must commit | `REQUIRES_NEW` (separate bean) |
| Method on private | Make public non-final |

| Default | Value |
|---------|-------|
| Propagation | REQUIRED |
| Rollback | RuntimeException + Error |
| Isolation | DB default |

---

## Practice

1. Self-invocation TX skip reproduce karke 4 fixes try karo.
2. Checked exception rollback fail dekhao + `rollbackFor` se fix.
3. `readOnly = true` mein UPDATE silently skip dekho.
4. `REQUIRES_NEW` audit pattern banakar outer rollback karke independence verify karo.
5. Try-catch swallow + `setRollbackOnly()` from `TransactionAspectSupport`.
