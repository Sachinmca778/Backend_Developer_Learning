# Query Optimization

## Status: Not Started

---

## Table of Contents

1. [Query Optimization Kya Hai?](#query-optimization-kya-hai)
2. [Identifying Slow Queries](#identifying-slow-queries)
3. [EXPLAIN / Execution Plans](#explain--execution-plans)
4. [Index Fundamentals](#index-fundamentals)
5. [B-Tree Indexes](#b-tree-indexes)
6. [Composite Indexes](#composite-indexes)
7. [Covering Indexes](#covering-indexes)
8. [Partial Indexes](#partial-indexes)
9. [Hash, GIN, GiST, BRIN Indexes](#hash-gin-gist-brin-indexes)
10. [Avoid SELECT *](#avoid-select-)
11. [Pagination — Offset vs Keyset](#pagination--offset-vs-keyset)
12. [JOIN Optimization](#join-optimization)
13. [Statistics & ANALYZE](#statistics--analyze)
14. [Query Hints](#query-hints)
15. [N+1 Problem (JPA)](#n1-problem-jpa)
16. [Common Pitfalls](#common-pitfalls)
17. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Query Optimization Kya Hai?

**Matlab:** Database queries ko **fast** + **efficient** banane ka art — sahi indexes, sahi query structure, sahi pagination, sahi `SELECT` use karke.

> "Query optimization = highest ROI activity in backend perf. 10x improvements common."

### When to Optimize?

✅ Slow query log shows queries > target (e.g., > 100ms)
✅ APM shows DB as bottleneck
✅ User-facing latency P99 > SLA
✅ Database CPU / IO maxed
✅ Read replicas underutilized

❌ "Just optimize everything" without measurement
❌ Adding indexes randomly

---

## Identifying Slow Queries

### PostgreSQL — `pg_stat_statements`

```sql
CREATE EXTENSION pg_stat_statements;

SELECT query, calls, mean_exec_time, total_exec_time, rows
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

→ Top slow queries by average time.

### MySQL — Slow Query Log

```sql
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 0.1;   -- 100ms threshold
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';
```

```bash
mysqldumpslow -s t /var/log/mysql/slow.log    # sort by time
```

### APM (Application-Side)

- Spring Boot Actuator + Micrometer → exposes JDBC metrics
- New Relic / Datadog → automatic slow query identification
- p6spy → log every query with timing

```xml
<!-- p6spy for development -->
<dependency>
  <groupId>p6spy</groupId>
  <artifactId>p6spy</artifactId>
  <version>3.9.1</version>
</dependency>
```

```yaml
spring:
  datasource:
    url: jdbc:p6spy:postgresql://localhost:5432/mydb
    driver-class-name: com.p6spy.engine.spy.P6SpyDriver
```

→ Cross-ref: `Database-Mastery` for deep DB topics.

---

## EXPLAIN / Execution Plans

The most important tool for query optimization.

### PostgreSQL

```sql
EXPLAIN SELECT * FROM users WHERE email = 'a@b.com';

EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'a@b.com';
-- ANALYZE actually runs query

EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT ...;
```

### Sample Output

```
Seq Scan on users  (cost=0.00..1234.00 rows=1 width=64) (actual time=15.234..15.234 rows=1 loops=1)
  Filter: (email = 'a@b.com'::text)
  Rows Removed by Filter: 999999
Planning Time: 0.123 ms
Execution Time: 15.234 ms
```

### Reading the Plan

| Term | Meaning |
|------|---------|
| **Seq Scan** | Full table scan — slow on big tables |
| **Index Scan** | Uses index — fast |
| **Index Only Scan** | Uses covering index — fastest |
| **Bitmap Heap Scan** | Multiple index lookups combined |
| **Hash Join** | Builds hash table for joins |
| **Merge Join** | Both inputs sorted, merged |
| **Nested Loop** | For each row in A, scan B (slow if B big) |
| **cost=startup..total** | Estimated cost units |
| **rows=N** | Estimated row count |
| **actual time=...** | Actual time (only with ANALYZE) |

### Red Flags

```
Seq Scan on big_table     → missing index
Rows Removed by Filter    → poor index selectivity
Hash Join with huge build → memory pressure
Nested Loop on big tables → pick wrong join algo
```

### Visualize with PEV / Postgres Explain Visualizer

Web tools that render plans graphically:
- https://explain.depesz.com (Polish, but excellent)
- https://explain.dalibo.com

### MySQL

```sql
EXPLAIN SELECT * FROM users WHERE email = 'a@b.com';
EXPLAIN FORMAT=JSON SELECT ...;
EXPLAIN ANALYZE SELECT ... ;   -- MySQL 8+
```

### MySQL EXPLAIN Columns

| Column | Important Values |
|--------|-----------------|
| `type` | const, eq_ref, ref, range, index, ALL (worst) |
| `key` | Index used (NULL = no index) |
| `rows` | Estimated rows examined |
| `Extra` | Using index, Using where, Using filesort, Using temporary |

→ `Using filesort` + `Using temporary` = optimization candidates.

---

## Index Fundamentals

### What's an Index?

**Sorted data structure** (B-tree usually) that maps column values → row locations. Like a book's index.

### Trade-offs

| Pro | Con |
|-----|-----|
| Faster SELECT / WHERE | Slower INSERT / UPDATE / DELETE |
| Faster JOIN | Disk space |
| Faster ORDER BY | Maintenance overhead |

### Rule of Thumb

```
Read-heavy table (10:1 read:write): index aggressively
Write-heavy table: minimal indexes (only essential)
```

### Where to Index?

- WHERE clause columns
- JOIN columns (foreign keys often need indexes!)
- ORDER BY columns
- GROUP BY columns
- Unique constraint columns (auto-indexed)

### Don't Index

- Tiny tables (< 1000 rows; full scan often faster)
- Columns rarely queried
- Highly write-heavy columns
- Low-cardinality columns alone (e.g., `gender BOOL`)

---

## B-Tree Indexes

**Default index type** in most DBs (PostgreSQL, MySQL).

### Use For

- Equality (`=`)
- Range (`<`, `>`, `BETWEEN`)
- Prefix matching (`LIKE 'foo%'`)
- ORDER BY

### Create

```sql
CREATE INDEX idx_users_email ON users(email);
CREATE UNIQUE INDEX idx_users_email_unique ON users(email);
```

### Doesn't Help With

- Suffix matching (`LIKE '%foo'`)
- Function calls (use **functional index**)
- Inequality (`!=`)

### Functional Index

```sql
CREATE INDEX idx_users_lower_email ON users(LOWER(email));

-- Only used if query matches function:
SELECT * FROM users WHERE LOWER(email) = 'a@b.com';   -- ✅ uses index
SELECT * FROM users WHERE email = 'a@b.com';            -- ❌ different
```

---

## Composite Indexes

**Multi-column index** — order matters!

```sql
CREATE INDEX idx_users_status_created ON users(status, created_at);
```

### Left-Prefix Rule

```sql
-- Index: (status, created_at)

WHERE status = 'ACTIVE'                            -- ✅ uses index
WHERE status = 'ACTIVE' AND created_at > '2024-01-01'  -- ✅ best
WHERE created_at > '2024-01-01'                    -- ❌ doesn't use this index
```

→ Filter on **leftmost columns** to use composite index.

### Column Order Strategy

```
Order by (selectivity × frequency):

(high_selectivity, low_selectivity)
(equality_filter, range_filter, sort_column)
```

### Example

```sql
-- Common query
SELECT * FROM orders
WHERE customer_id = 123
  AND status = 'SHIPPED'
ORDER BY created_at DESC;

-- Best index:
CREATE INDEX idx_orders_cs_cd ON orders(customer_id, status, created_at DESC);
```

→ Equality filters first, then range/sort.

---

## Covering Indexes

**Matlab:** Index includes all columns needed by query → DB never needs to read the table.

### Without Covering

```sql
CREATE INDEX idx_users_email ON users(email);

SELECT id, email, name FROM users WHERE email = 'a@b.com';
-- Steps:
--   1. Index lookup → row location
--   2. Heap fetch (read row from table) → get name, id
```

### With Covering Index

```sql
-- PostgreSQL syntax
CREATE INDEX idx_users_email_inc ON users(email) INCLUDE (id, name);

-- MySQL syntax (covering by including columns in key)
CREATE INDEX idx_users_email ON users(email, id, name);

-- Now query satisfied entirely from index — no heap fetch
SELECT id, email, name FROM users WHERE email = 'a@b.com';
```

→ EXPLAIN shows **Index Only Scan**. Big speedup for read-heavy queries.

### Trade-off

```
+ Faster reads
- Larger index (more disk + memory)
- Slower updates (more index data to maintain)
```

---

## Partial Indexes

**Matlab:** Index only a subset of rows matching a condition.

### Use Case — Skip Inactive Records

```sql
CREATE INDEX idx_users_active_email ON users(email)
WHERE active = true;
```

→ Index only **active** users; smaller, faster, used when query matches `WHERE active = true`.

### Use Case — Pending Orders

```sql
CREATE INDEX idx_orders_pending ON orders(created_at)
WHERE status = 'PENDING';

SELECT * FROM orders WHERE status = 'PENDING' ORDER BY created_at;
-- Uses partial index; tiny + fast
```

### Why?

```
Full index: all 10 million orders
Partial index: only ~10K pending orders
→ 1000× smaller, faster scan, less memory
```

### Caveats

- Query MUST match the partial condition
- Postgres only (MySQL has no native equivalent yet)

---

## Hash, GIN, GiST, BRIN Indexes

### Hash Index (Postgres)

```sql
CREATE INDEX idx_users_id_hash ON users USING HASH(id);
```

- Equality only (`=`)
- Faster than B-tree for pure equality
- Rare use; B-tree usually fine

### GIN (Generalized Inverted Index)

For multi-valued data:
- JSONB
- Arrays
- Full-text search (`tsvector`)

```sql
CREATE INDEX idx_users_tags ON users USING GIN(tags);   -- tags is array

CREATE INDEX idx_docs_search ON documents USING GIN(to_tsvector('english', content));
SELECT * FROM documents WHERE to_tsvector('english', content) @@ to_tsquery('java & spring');
```

### GiST (Generalized Search Tree)

For geometric, range, full-text:

```sql
CREATE INDEX idx_locations_geo ON locations USING GIST(geo_point);
SELECT * FROM locations WHERE geo_point <-> POINT(longitude, latitude) < 1000;
```

### BRIN (Block Range Index)

For huge sorted/append-only data (logs, time series):

```sql
CREATE INDEX idx_events_timestamp ON events USING BRIN(created_at);
```

→ Tiny size; works because consecutive blocks have nearby timestamps.

---

## Avoid SELECT *

### Why?

```sql
SELECT * FROM users WHERE id = 1;
```

| Problem | Impact |
|---------|--------|
| Reads all columns | Slower disk I/O |
| Network bandwidth | More bytes transferred |
| Application memory | Larger objects |
| Breaks covering index | Forces heap fetch |
| Schema changes break code | New column = unexpected behavior |

### Better

```sql
SELECT id, name, email FROM users WHERE id = 1;
```

→ Only needed columns. Often satisfied by covering index.

### JPA / Hibernate

```java
// ❌ fetches all entity fields
@Query("SELECT u FROM User u WHERE u.id = :id")
Optional<User> findById(@Param("id") Long id);

// ✅ projection — only needed fields
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

@Query("SELECT u.id AS id, u.name AS name, u.email AS email FROM User u WHERE u.id = :id")
Optional<UserSummary> findSummaryById(@Param("id") Long id);
```

---

## Pagination — Offset vs Keyset

### Offset Pagination (Common but Slow on Large Tables)

```sql
SELECT * FROM users ORDER BY created_at DESC LIMIT 20 OFFSET 1000000;
```

→ DB scans + discards 1,000,000 rows, returns 20.

```
OFFSET = 0      → fast (~1ms)
OFFSET = 1000   → ~10ms
OFFSET = 100K   → ~100ms
OFFSET = 1M     → ~1s+    ❌ unusable
```

### ✅ Keyset (Cursor) Pagination

```sql
-- First page
SELECT * FROM users ORDER BY created_at DESC, id DESC LIMIT 20;

-- Subsequent: pass last row's values as cursor
SELECT * FROM users
WHERE (created_at, id) < ('2024-05-08 10:30:00', 12345)
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

→ **O(log N)** — uses index for direct seek.

### Performance Comparison

| Approach | 1K rows | 100K rows | 1M rows |
|----------|---------|-----------|---------|
| OFFSET 1000 | 5ms | 50ms | 500ms+ |
| Keyset | 1ms | 1ms | 1ms |

### Spring Data Implementation

```java
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE (u.createdAt, u.id) < (:cursorTime, :cursorId) " +
           "ORDER BY u.createdAt DESC, u.id DESC")
    List<User> findPageAfter(@Param("cursorTime") LocalDateTime time,
                              @Param("cursorId") Long id,
                              Pageable pageable);
}

// Controller
@GetMapping("/users")
public List<UserDto> list(
    @RequestParam(required = false) String cursor,
    @RequestParam(defaultValue = "20") int limit) {
    
    if (cursor == null) {
        return userRepo.findFirstPage(PageRequest.of(0, limit));
    }
    Cursor c = Cursor.decode(cursor);
    return userRepo.findPageAfter(c.time, c.id, PageRequest.of(0, limit));
}
```

→ Encode cursor as base64 for opacity.

### When to Use Each

| Use Case | Approach |
|----------|----------|
| Small data (<10K), random page jump | Offset |
| Infinite scroll / "load more" | Keyset |
| Admin tables with page numbers | Offset (with `LIMIT 1000` safety) |
| Public feeds, large datasets | Keyset |

---

## JOIN Optimization

### Common Issue — Cartesian Joins

```sql
SELECT * FROM orders o, users u WHERE u.id = o.user_id;   -- old syntax
```

→ Same as INNER JOIN; just use modern syntax.

### Hash Join vs Nested Loop

```
Hash Join:
  - Build hash table on one side
  - Probe with other
  - Good for large × large
  
Nested Loop:
  - For each row in A, scan B
  - Good when B small + indexed
  - Disaster when B big + no index
```

→ DB optimizer picks; check EXPLAIN.

### Index on JOIN Columns

```sql
-- Foreign key join
SELECT * FROM orders o JOIN users u ON o.user_id = u.id;

-- Make sure orders.user_id is indexed!
CREATE INDEX idx_orders_user_id ON orders(user_id);
```

→ FK columns often forgotten; major perf wins from indexing.

### Reduce Joined Data

```sql
-- ❌ joins all orders
SELECT u.*, o.* FROM users u JOIN orders o ON o.user_id = u.id WHERE u.created_at > '2024-01-01';

-- ✅ filter early
WITH recent_users AS (
    SELECT * FROM users WHERE created_at > '2024-01-01'
)
SELECT * FROM recent_users u JOIN orders o ON o.user_id = u.id;
```

---

## Statistics & ANALYZE

### Why?

DB optimizer uses **statistics** about tables (row count, value distribution) to pick query plan.

→ Outdated stats = bad plans.

### PostgreSQL

```sql
ANALYZE users;          -- update stats for one table
ANALYZE;                -- all tables
VACUUM ANALYZE users;   -- clean up dead rows + analyze
```

### Auto-Analyze

PostgreSQL auto-analyzes when ~10% of rows change. Tune via `autovacuum_analyze_scale_factor`.

### MySQL

```sql
ANALYZE TABLE users;
```

### Checking Stats

```sql
-- PostgreSQL
SELECT schemaname, tablename, n_distinct, correlation
FROM pg_stats WHERE tablename = 'users';

-- MySQL
SELECT * FROM information_schema.statistics WHERE table_name = 'users';
```

### When to Manually Analyze

- After bulk INSERT / UPDATE
- After schema changes
- When EXPLAIN shows wildly wrong row estimates

---

## Query Hints

Most DBs allow forcing specific plans (use sparingly!).

### MySQL

```sql
SELECT /*+ INDEX(users idx_users_email) */ * FROM users WHERE email = 'a@b.com';
SELECT /*+ NO_INDEX(users) */ * FROM users WHERE email = 'a@b.com';
```

### PostgreSQL — `pg_hint_plan` Extension

```sql
/*+ IndexScan(users idx_users_email) */
SELECT * FROM users WHERE email = 'a@b.com';
```

### When to Use?

✅ DB consistently picks bad plan
✅ Stats can't capture data skew
✅ Last resort

❌ Workaround for missing index
❌ "Just to be safe"

→ Hints become tech debt; revisit after DB upgrades.

---

## N+1 Problem (JPA)

The most common JPA performance bug.

### The Problem

```java
List<Order> orders = orderRepo.findAll();   // 1 query
for (Order o : orders) {
    System.out.println(o.getUser().getName());   // 1 query per order!
}
```

→ 100 orders = 1 + 100 = **101 queries**.

### Solution 1 — JOIN FETCH

```java
@Query("SELECT o FROM Order o JOIN FETCH o.user")
List<Order> findAllWithUsers();
```

→ Single query with JOIN.

### Solution 2 — `@EntityGraph`

```java
@EntityGraph(attributePaths = {"user", "items"})
List<Order> findAll();
```

### Solution 3 — `@BatchSize`

```java
@Entity
public class Order {
    @ManyToOne(fetch = LAZY)
    @BatchSize(size = 50)
    private User user;
}
```

→ Loads 50 users at a time instead of 1 per row.

### Detection

- Enable Hibernate SQL logging
- Use Hibernate Statistics
- p6spy + count queries
- Datasource Proxy `@QueryCountInterceptor`

```java
@Test
void should_not_have_n_plus_1() {
    QueryCountHolder.clear();
    
    List<Order> orders = orderRepo.findAll();
    orders.forEach(o -> o.getUser().getName());
    
    assertEquals(1, QueryCountHolder.getGrandTotal().getSelect());
}
```

→ Cross-ref: `Database-Mastery` for ORM deep-dive.

---

## Common Pitfalls

### 1. No Indexes on Foreign Keys

JOINs become full table scans.

### 2. Index on Everything

Each index = write overhead. Audit + remove unused.

```sql
-- PostgreSQL
SELECT * FROM pg_stat_user_indexes WHERE idx_scan = 0;
```

### 3. Composite Index Wrong Order

```sql
CREATE INDEX idx ON t(b, a);    -- wrong order for "WHERE a=? AND b=?"
```

### 4. SELECT * Everywhere

Wastes bandwidth, breaks covering indexes.

### 5. OFFSET on Big Tables

Slow; use keyset pagination.

### 6. N+1 from ORM

Lazy loading + iteration = death.

### 7. Functions on Indexed Columns

```sql
WHERE LOWER(email) = 'a@b.com'   -- skips idx_users_email
```

→ Use functional index OR store lowercase column.

### 8. Implicit Type Conversion

```sql
WHERE id = '123'   -- string vs bigint → may not use index
```

→ Match types.

### 9. Forgetting to ANALYZE After Bulk Load

Stats stale → bad plans.

### 10. Index on Low-Cardinality Column Alone

```sql
CREATE INDEX idx ON users(gender);   -- 2 values: useless alone
```

→ Useful only as part of composite or with partial WHERE.

### 11. LIKE '%foo%' Without GIN

```sql
WHERE name LIKE '%john%'    -- full scan; B-tree useless
```

→ Use trigram index (`pg_trgm`) or full-text search.

### 12. Big IN Lists

```sql
WHERE id IN (1,2,...,10000)
```

→ DB struggles. Use temp table or `JOIN` to values list.

### 13. ORDER BY Without Limit

```sql
SELECT * FROM users ORDER BY created_at;
```

→ Sorts entire table. Add LIMIT.

### 14. Forgetting EXPLAIN ANALYZE

Optimizing without seeing actual plan = guessing.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| `EXPLAIN ANALYZE` | Always check before optimizing |
| **Seq Scan on big table** | Add index |
| **B-tree** | Default; equality + range |
| **Composite** | Order matters; left-prefix rule |
| **Covering** | Include needed columns; Index Only Scan |
| **Partial** | Subset (e.g., active rows only) |
| **GIN** | JSONB, arrays, full-text |
| **BRIN** | Huge append-only / time series |
| **OFFSET** | Slow on big tables |
| **Keyset cursor** | Fast at any depth |
| **JOIN FETCH** | Avoid N+1 |
| **ANALYZE** | Refresh stats after bulk changes |

| Symptom | Likely Fix |
|---------|-----------|
| `Seq Scan` on big table | Add index |
| `Rows Removed by Filter` | Better selectivity / partial index |
| `Using filesort` (MySQL) | Index matching ORDER BY |
| Many fast queries (N+1) | JOIN FETCH / EntityGraph |
| OFFSET 1M slow | Keyset cursor |
| Plan changed for worse | ANALYZE |

| ✅ Do | ❌ Don't |
|-------|---------|
| `EXPLAIN ANALYZE` first | Guess + index everywhere |
| Index FK columns | Forget JOIN columns |
| Covering index for hot reads | SELECT * |
| Keyset pagination for large | OFFSET 1M |
| `JOIN FETCH` for entity graphs | Lazy load + iterate |
| Partial index for filtered queries | Massive index whole table |
| ANALYZE after bulk changes | Stale stats |
| Functional index for transforms | Function-on-column = full scan |

---

## Practice

1. Identify top 10 slow queries via `pg_stat_statements`.
2. EXPLAIN ANALYZE each — find Seq Scans.
3. Add appropriate B-tree indexes; re-EXPLAIN.
4. Build composite index; verify left-prefix rule with different WHERE clauses.
5. Convert hot read query to use covering index; observe Index Only Scan.
6. Create partial index for `WHERE active = true`; compare size vs full index.
7. Convert OFFSET pagination to keyset; benchmark with 1M+ rows.
8. Trigger N+1 in JPA; fix with JOIN FETCH; verify single query in logs.
9. Run ANALYZE; compare plans before / after.
10. Build GIN index for JSONB column; query nested fields.
11. Use BRIN on huge time-series table; compare size vs B-tree.
12. Use p6spy or Hibernate logging to count queries per controller endpoint; reduce to minimum.
