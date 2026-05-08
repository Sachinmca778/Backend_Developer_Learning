# Query Optimization & EXPLAIN

## Status: Not Started

---

## Table of Contents

1. [Why EXPLAIN Matters](#why-explain-matters)
2. [EXPLAIN Output Columns](#explain-output-columns)
3. [`type` Column — Performance Ranking](#type-column--performance-ranking)
4. [`Extra` Column Flags](#extra-column-flags)
5. [`EXPLAIN ANALYZE` (MySQL 8+)](#explain-analyze-mysql-8)
6. [JSON Format EXPLAIN](#json-format-explain)
7. [Common Query Patterns + Fixes](#common-query-patterns--fixes)
8. [Slow Query Log](#slow-query-log)
9. [pt-query-digest](#pt-query-digest)
10. [Query Hints](#query-hints)
11. [N+1 Query Problem](#n1-query-problem)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why EXPLAIN Matters

> "**EXPLAIN = X-ray of query execution.** Without reading EXPLAIN fluently, query optimization = guesswork."

### When to EXPLAIN

- Slow query
- New feature query before prod
- After schema/index changes
- Production hot queries periodically

```sql
EXPLAIN SELECT * FROM orders WHERE user_id = 100 AND status = 'PENDING';
```

→ Returns plan **without executing** the query.

```sql
EXPLAIN ANALYZE SELECT * FROM orders WHERE user_id = 100;
```

→ MySQL 8+ — **executes** + returns actual timing.

---

## EXPLAIN Output Columns

```
+----+-------------+-------+------+---------------+--------+---------+-------+------+-------+
| id | select_type | table | type | possible_keys | key    | key_len | ref   | rows | Extra |
+----+-------------+-------+------+---------------+--------+---------+-------+------+-------+
|  1 | SIMPLE      | users | ref  | idx_email     | idx_email | 767  | const | 1    | NULL  |
+----+-------------+-------+------+---------------+--------+---------+-------+------+-------+
```

### Column meanings

| Column | Meaning |
|--------|---------|
| **id** | Sequential ID for query parts (subqueries / unions) |
| **select_type** | SIMPLE / PRIMARY / SUBQUERY / DERIVED / UNION / DEPENDENT SUBQUERY |
| **table** | Which table being read |
| **type** | Access strategy (CRITICAL — see below) |
| **possible_keys** | Indexes optimizer considered |
| **key** | Index actually used (NULL = full scan) |
| **key_len** | Bytes of index used (lower = more selective) |
| **ref** | Column / constant compared with index |
| **rows** | Estimated rows examined |
| **filtered** | % of `rows` matching query (high = good) |
| **Extra** | Special flags (CRITICAL — see below) |

### Reading flow

1. **`type`** first — anything but `ALL` is a win
2. **`key`** — index used? (NULL = bad usually)
3. **`rows × filtered/100`** — actual rows examined
4. **`Extra`** — `Using filesort`, `Using temporary` = expensive

---

## `type` Column — Performance Ranking

| Type | Meaning | Performance |
|------|---------|-------------|
| **system** | Single row | Best (special) |
| **const** | PK / unique index = const | Excellent (1 row) |
| **eq_ref** | PK / unique index in JOIN, 1 row per outer row | Excellent |
| **ref** | Non-unique index lookup | Good |
| **range** | Index range (BETWEEN, >, <, IN) | Good |
| **index** | Full **index scan** (smaller than table scan) | Mediocre |
| **ALL** | **Full table scan** | ❌ Bad (unless tiny table) |

### Examples

#### const — `id = 100`

```sql
EXPLAIN SELECT * FROM users WHERE id = 100;
-- type: const
```

#### eq_ref — JOIN on PK

```sql
EXPLAIN SELECT * FROM orders o JOIN users u ON o.user_id = u.id;
-- u row: type = eq_ref (PK match)
-- o row: type = ALL (no index on user_id?)
```

#### ref — Non-unique index lookup

```sql
EXPLAIN SELECT * FROM orders WHERE user_id = 100;
-- (user_id has index but not unique) type = ref
```

#### range — Range query

```sql
EXPLAIN SELECT * FROM orders WHERE created_at BETWEEN '2024-01-01' AND '2024-06-30';
-- type = range
```

#### index — Full index scan (avoids table)

```sql
EXPLAIN SELECT id FROM users;
-- (id is PK, all rows from PK index) type = index
```

#### ALL — Full table scan

```sql
EXPLAIN SELECT * FROM users WHERE name = 'Sachin';
-- (no index on name) type = ALL
```

### Improvement target

| Current | Target |
|---------|--------|
| ALL | range / ref / eq_ref |
| index (full scan) | range / ref |
| range (broad) | ref / eq_ref |

---

## `Extra` Column Flags

| Flag | Meaning | Performance |
|------|---------|-------------|
| **Using index** | Covering index — no table fetch | ✅ Excellent |
| **Using where** | Filter applied after rows fetched | OK |
| **Using index condition** | ICP (Index Condition Pushdown) — filter at index layer | ✅ Good |
| **Using filesort** | External sort needed (no index for ORDER BY) | ⚠️ Expensive |
| **Using temporary** | Temp table created (often for GROUP BY / DISTINCT) | ⚠️ Expensive |
| **Using join buffer** | No index for JOIN — buffered nested loop | ⚠️ |
| **Range checked for each record** | Worst — re-evaluates per row | ❌ |
| **Impossible WHERE** | Optimizer detected impossible filter | ✅ skipped |
| **Select tables optimized away** | COUNT() / MAX() answered from index | ✅ |
| **Distinct** | Optimizer can stop after first match | ✅ |

### `Using filesort` fix

```sql
EXPLAIN SELECT * FROM users WHERE city = 'Bengaluru' ORDER BY created_at;
-- Extra: Using filesort

-- Fix: composite index that satisfies sort
CREATE INDEX idx_city_created ON users(city, created_at);
```

→ Now ORDER BY served by index — no filesort.

### `Using temporary` fix

Often from GROUP BY / DISTINCT on non-indexed columns. Add appropriate index or rewrite.

### `Using index` (covering)

Goal for hot queries:

```sql
EXPLAIN SELECT email FROM users WHERE id = 100;
-- (id is PK; email needs row fetch)

-- With covering index INDEX(id, email):
-- Extra: Using index
```

---

## `EXPLAIN ANALYZE` (MySQL 8+)

> "Actually runs query + returns **real timings + actual rows**."

```sql
EXPLAIN ANALYZE
SELECT u.name, COUNT(o.id) AS orders
FROM users u
JOIN orders o ON o.user_id = u.id
WHERE u.country = 'IN'
GROUP BY u.id;
```

### Sample output

```
-> Aggregate: count(o.id)
  -> Nested loop inner join (cost=12345 rows=1234) (actual time=0.1..50.2 rows=1500 loops=1)
    -> Index lookup on u using idx_country (country='IN') (cost=200 rows=500) (actual time=0.05..1.5 rows=550 loops=1)
    -> Index lookup on o using idx_user (user_id=u.id) (cost=10 rows=2) (actual time=0.05..0.1 rows=2.7 loops=550)
```

### Read top-down

- "Aggregate" = outermost step
- Indented = inner steps
- `actual time=startup..total` per step
- `rows=actual_count loops=times_called`

→ **Compare estimated `rows` vs actual `rows`** — big mismatch → stale stats.

→ Cross-ref: `phase-4 / Performance & Optimization / 03-Query-Optimization.md`.

---

## JSON Format EXPLAIN

```sql
EXPLAIN FORMAT=JSON SELECT * FROM users WHERE id = 100;
```

### Returns detailed JSON

```json
{
  "query_block": {
    "select_id": 1,
    "cost_info": {"query_cost": "1.00"},
    "table": {
      "table_name": "users",
      "access_type": "const",
      "key": "PRIMARY",
      "key_length": "8",
      "rows_examined_per_scan": 1,
      "rows_produced_per_join": 1,
      "filtered": "100.00",
      "cost_info": {"read_cost": "0.00", "eval_cost": "0.10", "data_read_per_join": "120"},
      "used_columns": ["id", "name", "email"]
    }
  }
}
```

→ More detail than text format. **`cost_info`** + **`used_columns`** especially useful.

→ Workbench / DataGrip have visual EXPLAIN tools.

---

## Common Query Patterns + Fixes

### 1. `SELECT *`

❌
```sql
SELECT * FROM users WHERE id = 100;
```

→ Fetches all columns including TEXT/BLOB.

✅
```sql
SELECT id, name, email FROM users WHERE id = 100;
```

→ **Less data transferred + can use covering index**.

### 2. OR with different columns

❌
```sql
WHERE user_id = ? OR email = ?
```

→ Often only one index used (or full scan).

✅
```sql
SELECT ... WHERE user_id = ?
UNION
SELECT ... WHERE email = ?
```

### 3. Subquery vs JOIN

```sql
-- Subquery
WHERE user_id IN (SELECT id FROM users WHERE country = 'IN')

-- JOIN
JOIN users ON ... WHERE users.country = 'IN'
```

→ MySQL optimizer mostly handles equally now (5.6+). JOIN often more readable.

### 4. LIMIT with large OFFSET

❌
```sql
SELECT * FROM orders ORDER BY id LIMIT 1000000, 20;
```

→ Reads + discards 1M rows.

✅ Keyset pagination
```sql
SELECT * FROM orders WHERE id > 1234567 ORDER BY id LIMIT 20;
```

### 5. NOT IN / NOT EXISTS

```sql
WHERE id NOT IN (SELECT user_id FROM blacklist)   -- Beware NULLs!
WHERE NOT EXISTS (SELECT 1 FROM blacklist b WHERE b.user_id = users.id)  -- ✅ NULL-safe
```

### 6. Function in WHERE

❌ `WHERE YEAR(created_at) = 2024` → no index.

✅ `WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01'` → index used.

### 7. Loose `WHERE` returning many rows

If query naturally returns 90%+ of table → full scan often **faster** than index.

→ Optimizer may choose ALL even if index exists. That's correct behaviour.

---

## Slow Query Log

> "**Logs queries slower than threshold**. Best diagnostic tool."

### Enable

```sql
SET GLOBAL slow_query_log = ON;
SET GLOBAL long_query_time = 1;            -- log queries > 1 sec
SET GLOBAL log_queries_not_using_indexes = ON;
SET GLOBAL slow_query_log_file = '/var/log/mysql/slow.log';
```

### Persist (in `my.cnf`)

```ini
[mysqld]
slow_query_log = 1
long_query_time = 1
log_queries_not_using_indexes = 1
slow_query_log_file = /var/log/mysql/slow.log
```

### Sample log entry

```
# Time: 2024-05-08T14:30:22.123456Z
# User@Host: app[app] @ [10.0.1.5]
# Query_time: 3.245678  Lock_time: 0.001234  Rows_sent: 1500  Rows_examined: 5000000
SELECT u.name, COUNT(o.id) FROM users u JOIN orders o ON ...;
```

### Notable fields

- **Query_time** — total execution
- **Lock_time** — waited on locks
- **Rows_examined** — most important; high = index issue likely

---

## pt-query-digest

> "**Percona Toolkit** — aggregate slow log entries, find worst queries."

### Install

```bash
apt install percona-toolkit
```

### Run

```bash
pt-query-digest /var/log/mysql/slow.log > slow-report.txt
```

### Sample output

```
# Profile
# Rank Query ID                      Response time   Calls  R/Call
# ==== ============================ =============== ====== ======
#    1 0xABC...                     1234.567 (45%)    523   2.36
#    2 0xDEF...                      890.123 (32%)   1012   0.88
```

### What to look for

- **Top 5 by response time** — fix these first
- **High calls** = optimization has compounding benefit
- **Long queries with low calls** = critical issue

→ Run weekly in production; track top-5 trend.

---

## Query Hints

> "**Override optimizer**. Use only when you know better than optimizer."

### `FORCE INDEX`

```sql
SELECT * FROM users FORCE INDEX (idx_email)
WHERE email = ? AND status = 'ACTIVE';
```

→ Forces optimizer to use specific index.

### `IGNORE INDEX`

```sql
SELECT * FROM users IGNORE INDEX (idx_status)
WHERE email = ? AND status = 'ACTIVE';
```

→ Skip a specific index.

### `STRAIGHT_JOIN` — JOIN order

```sql
SELECT STRAIGHT_JOIN ...
FROM small_table a JOIN big_table b ON ...
```

→ Forces JOIN order matching FROM clause (no optimizer reordering).

### MySQL 8+ optimizer hints

```sql
SELECT /*+ INDEX(users idx_email) */ * FROM users WHERE email = ?;
SELECT /*+ NO_INDEX_MERGE(users) */ * FROM users WHERE ...;
SELECT /*+ MAX_EXECUTION_TIME(2000) */ * FROM users WHERE ...;  -- 2 sec timeout
```

### When to hint?

- **Optimizer making clearly wrong choice** (test 5+ times)
- Statistics stale + immediate fix needed
- Specific index strategy needed (covering)

→ **Last resort.** Better — fix stats / schema / query.

---

## N+1 Query Problem

> "App-level — 1 query for list + N queries for related items = 1+N total."

### Bad (PHP/Java/any ORM)

```php
$users = $db->query("SELECT * FROM users");
foreach ($users as $u) {
    $orders = $db->query("SELECT * FROM orders WHERE user_id = ?", $u['id']);
}
```

→ 1 + 1000 queries for 1000 users.

### Fix 1 — JOIN

```sql
SELECT u.*, o.* FROM users u LEFT JOIN orders o ON o.user_id = u.id;
```

### Fix 2 — `IN` query

```sql
SELECT * FROM orders WHERE user_id IN (1, 2, 3, ..., 1000);
```

### Fix 3 — JPA `@EntityGraph` / `JOIN FETCH`

```java
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();
```

→ Cross-ref: `phase-5 / Spring Boot Interview Topics / 05-Spring-Data-JPA-Questions.md`.

---

## Common Output Traps

### Q1. EXPLAIN shows index used, but query still slow

→ Possible: `rows` huge (millions). Check `rows × filtered/100` actual examined.

### Q2. `Using filesort` for small result

```sql
SELECT * FROM users WHERE id = 100 ORDER BY name;
-- Tiny result; filesort negligible — ignore
```

### Q3. `Using temporary` always bad?

Not always. Small temp table for DISTINCT in memory = fast.

→ Issue: large temp tables spilling to disk (`tmp_table_size`, `max_heap_table_size`).

### Q4. ANALYZE before vs after EXPLAIN

Stale stats give wrong plan. Run `ANALYZE TABLE` first if data changed significantly.

### Q5. Slow log catches setup queries

`USE db` might appear if `log_throttle_queries_not_using_indexes` low. Configure properly.

---

## Pitfalls

1. **Reading `key_len` as bytes used efficiently** — actually shows index usage depth.
2. **`type: index` mistaken for "good"** — it's full index scan, can be slow.
3. **Filtering on `rows` only** — high `filtered` % matters too.
4. **EXPLAIN without ANALYZE** — never executed; estimates may be off.
5. **Hint on every query** — fragile; prefer schema fix.
6. **Not enabling slow log in prod** — flying blind.
7. **`SELECT *` in covering-index test** — kills covering opportunity.
8. **Subquery DEPENDENT SUBQUERY** — recomputed per row; rewrite.
9. **Stale stats** after big DML — `ANALYZE TABLE`.
10. **OR queries** with multiple non-indexed cols — use UNION / dedicated index.
11. **OFFSET pagination** at scale — keyset instead.
12. **Ignoring `Using join buffer`** — missing index on JOIN column.

---

## Cheat Sheet

| Step | Action |
|------|--------|
| 1 | EXPLAIN to see plan |
| 2 | Identify problematic `type` (ALL / index) |
| 3 | Check `Extra` for filesort / temp |
| 4 | Add / fix index |
| 5 | Verify with EXPLAIN ANALYZE |

| `type` | Strategy |
|--------|----------|
| const / eq_ref | Best |
| ref | Good |
| range | Good |
| index | Mediocre (covering ok) |
| ALL | Bad (unless tiny) |

| `Extra` | Action |
|---------|--------|
| Using index | ✅ Covering |
| Using where | OK |
| Using filesort | ⚠️ Add ORDER BY index |
| Using temporary | ⚠️ Add GROUP BY index / rewrite |

| Optimization quick wins | |
|------------------------|--|
| `SELECT *` → specific columns | covering index |
| OFFSET → keyset pagination | constant time |
| Function on column → range filter | uses index |
| Subquery → JOIN | optimizer-friendlier |
| Add `ANALYZE TABLE` | stats fresh |

| Tools | |
|-------|--|
| EXPLAIN | Plan |
| EXPLAIN ANALYZE | Plan + actual |
| Slow query log | Catch slow |
| pt-query-digest | Aggregate top queries |
| Workbench Visual EXPLAIN | Plan tree |

---

## Practice

1. Take 3 hot queries from your app → EXPLAIN each → identify weak spot.
2. Enable slow query log; collect 1 day; run pt-query-digest; fix top 3.
3. Add covering index for top SELECT; verify `Using index` in EXPLAIN.
4. Replace OFFSET pagination with keyset; benchmark on 10M-row table.
5. Run EXPLAIN ANALYZE on a join query; compare estimated vs actual rows.
6. Use FORCE INDEX hint; benchmark vs without; understand when optimizer is right.
