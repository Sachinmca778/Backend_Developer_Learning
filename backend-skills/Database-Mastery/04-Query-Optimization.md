# Query Optimization

## Status: Not Started

---

## Table of Contents

1. [Query Planner Kaise Kaam Karta Hai?](#query-planner-kaise-kaam-karta-hai)
2. [EXPLAIN vs EXPLAIN ANALYZE](#explain-vs-explain-analyze)
3. [Reading EXPLAIN Output](#reading-explain-output)
4. [Scan Types](#scan-types)
5. [Join Types](#join-types)
6. [Statistics (pg_statistic)](#statistics-pg_statistic)
7. [Common Performance Problems](#common-performance-problems)
8. [Optimization Workflow](#optimization-workflow)

---

## Query Planner Kaise Kaam Karta Hai?

Postgres ka **query planner** SQL query ko execution plan mein convert karta hai. Plan choose karne ke liye:

1. **Statistics** dekhta hai (table size, distinct values, distribution)
2. **Cost estimate** karta hai (har possible plan ka)
3. **Cheapest plan** select karta hai

```
SQL Query
   ↓
Parser (syntax check)
   ↓
Rewriter (views expand)
   ↓
Planner/Optimizer  ← yahan magic hota hai
   ↓
Executor (actual execution)
```

---

## EXPLAIN vs EXPLAIN ANALYZE

### EXPLAIN

Sirf planner ka **estimated** plan dikhata hai. Query actually execute nahi hoti.

```sql
EXPLAIN SELECT * FROM users WHERE email = 'rahul@x.com';
```

### EXPLAIN ANALYZE

Query execute karta hai aur **actual** timings + rows dikhata hai.

```sql
EXPLAIN ANALYZE SELECT * FROM users WHERE email = 'rahul@x.com';
```

⚠️ `EXPLAIN ANALYZE` ko **mutating queries** par mat chalao — `INSERT/UPDATE/DELETE` actually run ho jaayenge! Wrap in transaction:

```sql
BEGIN;
EXPLAIN ANALYZE DELETE FROM logs WHERE id < 1000;
ROLLBACK;
```

### Useful Options

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, FORMAT TEXT)
SELECT ...;
```

| Option | Shows |
|--------|-------|
| `ANALYZE` | Actual execution time + rows |
| `BUFFERS` | Cache hits / disk reads |
| `VERBOSE` | Schema + output columns |
| `COSTS` | Estimated cost (default on) |
| `TIMING` | Per-node timing (default on) |
| `FORMAT JSON` | Machine-readable output |

---

## Reading EXPLAIN Output

### Sample Output

```
EXPLAIN ANALYZE
SELECT u.name, COUNT(o.id)
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.country = 'IN'
GROUP BY u.name;
```

```
HashAggregate  (cost=2150.00..2200.00 rows=5000 width=40)
               (actual time=45.2..47.1 rows=4823 loops=1)
  Group Key: u.name
  ->  Hash Right Join  (cost=350.00..1900.00 rows=50000 width=32)
                       (actual time=12.3..38.5 rows=48230 loops=1)
        Hash Cond: (o.user_id = u.id)
        ->  Seq Scan on orders o  (cost=0..1000 rows=100000 width=16)
                                  (actual time=0.05..15.2 rows=100000 loops=1)
        ->  Hash  (cost=300..300 rows=5000 width=24)
                  (actual time=8.1..8.1 rows=5000 loops=1)
              ->  Index Scan using idx_users_country on users u
                  (cost=0..300 rows=5000 width=24)
                  (actual time=0.02..6.5 rows=5000 loops=1)
                  Index Cond: (country = 'IN')
Planning Time: 0.45 ms
Execution Time: 48.3 ms
```

### Key Things to Look At

#### 1. Cost vs Actual

```
(cost=2150.00..2200.00 rows=5000 width=40)
(actual time=45.2..47.1 rows=4823 loops=1)
```

| Field | Meaning |
|-------|---------|
| `cost=START..TOTAL` | Planner's estimated cost (arbitrary units) |
| `rows=N` | Estimated rows |
| `width=N` | Avg row size in bytes |
| `actual time=START..END` | Actual ms |
| `actual rows=N` | Actual rows returned |
| `loops=N` | Times this node executed |

#### 2. Estimated vs Actual Rows Mismatch

```
rows=5000  (estimated)
actual rows=48230  (real)
```

🚨 Big mismatch (10x+) → planner has stale stats → run `ANALYZE`.

#### 3. Reading Tree (Inside-Out)

Plans are trees. **Innermost (deepest) node executes first**, results bubble up.

```
Outer node (last)
  ↓
  Middle node
    ↓
    Inner node (first to execute)
```

#### 4. Loops

```
loops=1     → executed once (good)
loops=10000 → nested loop ka inner side → 10000 baar execute hua!
```

Per-iteration time × loops = total impact.

---

## Scan Types

### 1. Seq Scan (Sequential Scan)

**Matlab:** Pura table read karta hai — har row check.

```
Seq Scan on orders  (cost=0..1000 rows=100000 width=64)
```

**Kab use hota hai?**
- Koi index nahi
- Index hai but planner ko lagta hai bahut rows match karenge
- Small table (sequential faster than random I/O)

**Kab problem?**
- Big table + WHERE clause + missing index → ALWAYS slow

```sql
-- ❌ Slow on 10M row table
SELECT * FROM orders WHERE user_id = 42;
-- "Seq Scan on orders" → BAD

-- ✅ Fix
CREATE INDEX idx_orders_user ON orders(user_id);
-- Now: "Index Scan using idx_orders_user"
```

### 2. Index Scan

**Matlab:** Index se row pointers leta hai, phir table se actual rows fetch.

```
Index Scan using idx_users_email on users  (cost=0.42..8.44 rows=1)
  Index Cond: (email = 'rahul@x.com')
```

Faster than seq scan when filtering small subset.

### 3. Index Only Scan

**Matlab:** Index mein hi saara required data hai → table touch nahi.

```
Index Only Scan using idx_users_covering on users
  Index Cond: (email = 'rahul@x.com')
  Heap Fetches: 0   ← good!
```

`Heap Fetches: 0` matlab fully covered. Use `INCLUDE` columns.

### 4. Bitmap Heap Scan + Bitmap Index Scan

**Matlab:** Index se bitmap banata hai (rows ka), phir sorted order mein heap se read.

```
Bitmap Heap Scan on orders  (cost=...)
  Recheck Cond: ...
  ->  Bitmap Index Scan on idx_orders_status
        Index Cond: (status = 'pending')
```

**Kab?**
- Medium-sized result set (Index Scan = small, Seq Scan = huge)
- Multiple index conditions can be combined

```sql
-- Multiple bitmap scans combined
EXPLAIN SELECT * FROM orders 
WHERE user_id = 42 AND status = 'pending';

-- Output might show:
-- BitmapAnd
--   ->  Bitmap Index Scan on idx_orders_user
--   ->  Bitmap Index Scan on idx_orders_status
```

### Comparison

| Scan Type | Best For |
|-----------|----------|
| **Seq Scan** | Small tables, "all rows" queries |
| **Index Scan** | Few rows match (high selectivity) |
| **Index Only Scan** | Few rows + all columns in index |
| **Bitmap Heap Scan** | Medium result, multi-condition |

---

## Join Types

### 1. Nested Loop Join

**Matlab:** Outer table ki har row ke liye, inner table mein search.

```
Nested Loop  (cost=...)
  ->  Seq Scan on outer (rows=10)
  ->  Index Scan on inner (rows=1, loops=10)
```

**Algorithm:**
```
for each row in outer:
    for each row in inner:
        if match: output
```

**Best when:**
- Outer table small
- Inner has good index
- Result is small

**Worst case:** O(N × M) — disaster on big tables.

### 2. Hash Join

**Matlab:** Smaller table ka in-memory hash table banata hai, phir bigger table scan kar ke probe karta hai.

```
Hash Join  (cost=...)
  Hash Cond: (orders.user_id = users.id)
  ->  Seq Scan on orders (rows=1000000)
  ->  Hash
        ->  Seq Scan on users (rows=10000)   ← built into hash
```

**Algorithm:**
```
1. Build phase: hash table from smaller table
2. Probe phase: scan bigger table, lookup in hash
```

**Best when:**
- Both tables medium-large
- Equality joins (`=`)
- Smaller table fits in `work_mem`

### 3. Merge Join

**Matlab:** Dono tables sort karke parallel walk.

```
Merge Join
  Merge Cond: (a.id = b.id)
  ->  Index Scan on a  (sorted by id)
  ->  Index Scan on b  (sorted by id)
```

**Algorithm:**
```
1. Sort both inputs (or use sorted index)
2. Walk both in parallel
```

**Best when:**
- Very large tables, both sorted on join key
- Indexes provide pre-sorted data

### Join Type Selection (Mental Model)

| Scenario | Likely Join |
|----------|-------------|
| Small × Big with index | Nested Loop |
| Medium × Medium | Hash Join |
| Big × Big, both sorted | Merge Join |

### Force / Disable for Testing

```sql
SET enable_nestloop = off;
SET enable_hashjoin = off;
SET enable_mergejoin = off;
-- Run query, see what happens
RESET enable_nestloop;
```

---

## Statistics (pg_statistic)

Postgres planner statistics par depend karta hai. Stale stats = bad plans.

### What Stats Are Tracked?

- Total rows (`reltuples`)
- Most common values (MCV) + frequencies
- Histograms (for range queries)
- Distinct values (`n_distinct`)
- NULL fraction
- Correlation between physical and logical order

### View Stats

```sql
-- Table-level
SELECT relname, reltuples, relpages 
FROM pg_class WHERE relname = 'orders';

-- Column-level (human-friendly view)
SELECT * FROM pg_stats WHERE tablename = 'orders';
-- Columns: most_common_vals, histogram_bounds, n_distinct, null_frac
```

### Updating Stats

```sql
-- Manual analyze
ANALYZE orders;

-- Specific column
ANALYZE orders (user_id, status);

-- Verbose
ANALYZE VERBOSE orders;
```

### Increase Stats Resolution

```sql
-- Default 100 buckets in histogram
ALTER TABLE orders ALTER COLUMN user_id SET STATISTICS 1000;
ANALYZE orders;
-- Better estimates, slightly more analyze time
```

### When Stats Get Stale?

- Bulk inserts/deletes
- Major data distribution change
- New deployment after migrations

🚨 Always `ANALYZE` after large bulk loads.

---

## Common Performance Problems

### 1. N+1 Queries

```javascript
// ❌ BAD
const users = await db.query('SELECT * FROM users');
for (const user of users) {
    user.orders = await db.query(
        'SELECT * FROM orders WHERE user_id = ?', [user.id]
    );
}
// 1 + N queries!
```

```sql
-- ✅ GOOD: Single JOIN
SELECT u.*, o.*
FROM users u
LEFT JOIN orders o ON o.user_id = u.id;
```

**Detect:** `pg_stat_statements` mein same query template thousands of times.

### 2. Missing Index

```
Seq Scan on big_table  (cost=0..50000 rows=100)
  Filter: user_id = 42
  Rows Removed by Filter: 9999900   ← throwing away 99.99%!
```

→ Add index on `user_id`.

### 3. Stale Statistics

```
rows=100   (estimated)
actual rows=100000  (1000x off!)
```

→ Run `ANALYZE`.

### 4. Function on Indexed Column

```sql
-- ❌ Index unused
SELECT * FROM users WHERE LOWER(email) = 'x@y.com';

-- ✅ Functional index
CREATE INDEX idx_users_email_lower ON users(LOWER(email));
```

### 5. Implicit Type Casts

```sql
-- ❌ id is BIGINT, '42' is TEXT → cast → no index
SELECT * FROM users WHERE id = '42';

-- ✅ Match types
SELECT * FROM users WHERE id = 42;
```

### 6. SELECT *

```sql
-- ❌ Loads all columns including TEXT/JSONB blobs
SELECT * FROM articles WHERE id = 1;

-- ✅ Only needed columns → may use index-only scan
SELECT id, title, slug FROM articles WHERE id = 1;
```

### 7. OR Conditions

```sql
-- ⚠️ Postgres often can't use index efficiently
SELECT * FROM orders WHERE user_id = 42 OR email = 'x@y.com';

-- ✅ UNION ALL is sometimes faster
SELECT * FROM orders WHERE user_id = 42
UNION ALL
SELECT * FROM orders WHERE email = 'x@y.com' AND user_id != 42;
```

### 8. LIMIT + ORDER BY without Index

```sql
-- ❌ Sorts entire 10M row table for top 10
SELECT * FROM orders ORDER BY created_at DESC LIMIT 10;

-- ✅ Index supports the ORDER BY
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
```

### 9. Pagination with OFFSET

```sql
-- ❌ OFFSET 1000000 → DB still reads all 1M rows
SELECT * FROM orders ORDER BY id LIMIT 20 OFFSET 1000000;

-- ✅ Keyset pagination
SELECT * FROM orders WHERE id > $last_seen_id 
ORDER BY id LIMIT 20;
```

---

## Optimization Workflow

### Step-by-Step

1. **Identify slow queries**
   ```sql
   SELECT query, mean_exec_time, calls 
   FROM pg_stat_statements
   ORDER BY mean_exec_time DESC LIMIT 20;
   ```

2. **Run EXPLAIN ANALYZE**
   ```sql
   EXPLAIN (ANALYZE, BUFFERS) <query>;
   ```

3. **Look for red flags**
   - Seq Scan on big table?
   - Estimated vs actual rows mismatch?
   - High `loops`?
   - High `Heap Fetches`?
   - Sort/Hash spilling to disk (`work_mem` exceeded)?

4. **Fix**
   - Add/remove index
   - Rewrite query
   - Update stats (`ANALYZE`)
   - Tune `work_mem`, `effective_cache_size`

5. **Re-measure**
   ```sql
   EXPLAIN (ANALYZE, BUFFERS) <query>;
   -- Compare before/after timings
   ```

### Visual Tools

- [explain.depesz.com](https://explain.depesz.com) — paste plan, get visual
- [pev2.netlify.app](https://pev2.netlify.app) — interactive plan tree
- pgAdmin / DataGrip → built-in plan visualizer

---

## Key Postgres Config for Performance

| Param | Default | Tuning |
|-------|---------|--------|
| `shared_buffers` | 128MB | 25% of RAM |
| `effective_cache_size` | 4GB | 50-75% of RAM |
| `work_mem` | 4MB | Per-operation; 16-64MB for OLAP |
| `maintenance_work_mem` | 64MB | 1-2GB for VACUUM, REINDEX |
| `random_page_cost` | 4.0 | 1.1 for SSD |
| `effective_io_concurrency` | 1 | 100-200 for SSD |

---

## Summary Cheat Sheet

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| Seq Scan on big table | Missing index | `CREATE INDEX` |
| Estimated rows way off | Stale stats | `ANALYZE` |
| Nested loop with high `loops` | Wrong join type | More indexes / `work_mem` |
| Heap Fetches > 0 | Not index-only scan | Add `INCLUDE` columns |
| Sort spills to disk | `work_mem` too small | Increase `work_mem` |
| Bitmap heap scan slow | Random I/O | Cluster by index, SSD |

---

## Practice

1. Take any slow query in your app, run `EXPLAIN ANALYZE`, and identify bottleneck.
2. Build a million-row table, run a query without index, then with — compare plans.
3. Force `enable_seqscan = off` and observe how planner adapts.
4. Identify a query with bad estimated row count, fix with `ANALYZE` or stats tuning.
5. Convert a query that uses Nested Loop to use Hash Join, observe difference.
6. Set up `pg_stat_statements` and identify your top 5 slow queries.
