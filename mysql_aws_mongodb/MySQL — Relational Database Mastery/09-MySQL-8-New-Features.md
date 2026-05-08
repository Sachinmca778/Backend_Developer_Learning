# MySQL 8 New Features

## Status: Not Started

---

## Table of Contents

1. [Why MySQL 8 Matters](#why-mysql-8-matters)
2. [Window Functions](#window-functions)
3. [Common Table Expressions (CTEs)](#common-table-expressions-ctes)
4. [Recursive CTEs](#recursive-ctes)
5. [JSON Type & Functions](#json-type--functions)
6. [Invisible Indexes](#invisible-indexes)
7. [Descending Indexes](#descending-indexes)
8. [Functional Indexes](#functional-indexes)
9. [SKIP LOCKED & NOWAIT](#skip-locked--nowait)
10. [Roles](#roles)
11. [Histograms](#histograms)
12. [Other Notable Improvements](#other-notable-improvements)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why MySQL 8 Matters

MySQL 8.0 (2018) ka biggest feature jump in years:

- Modern SQL (window functions, CTEs)
- Native JSON
- caching_sha2_password (better security)
- Atomic DDL
- Performance improvements
- Removed query_cache (was unreliable)

→ **Default version for new projects.** 5.7 EOL October 2023.

---

## Window Functions

> "**Aggregate-like calculations across a 'window' of rows** without collapsing them."

### Syntax

```sql
SELECT col, AGG_FN(col) OVER (PARTITION BY x ORDER BY y) AS result
FROM table;
```

### Top window functions

| Function | What |
|----------|------|
| `ROW_NUMBER()` | 1, 2, 3, ... in order |
| `RANK()` | 1, 2, 2, 4 (ties leave gap) |
| `DENSE_RANK()` | 1, 2, 2, 3 (no gap) |
| `NTILE(n)` | Bucket rows into n groups |
| `LAG(col, k)` | k-th previous row's col |
| `LEAD(col, k)` | k-th next row's col |
| `FIRST_VALUE(col)` | First col in window |
| `LAST_VALUE(col)` | Last col in window |
| `SUM/AVG/COUNT(col) OVER (...)` | Running aggregates |

### Example 1 — Top N per group

```sql
-- Top 3 highest-paid employees per department
SELECT name, department, salary
FROM (
    SELECT *, ROW_NUMBER() OVER (PARTITION BY department ORDER BY salary DESC) AS rn
    FROM employees
) t
WHERE rn <= 3;
```

### Example 2 — Running total

```sql
SELECT
    order_date,
    daily_total,
    SUM(daily_total) OVER (ORDER BY order_date) AS cumulative_total
FROM daily_sales;
```

### Example 3 — Percentage of total

```sql
SELECT
    product_id,
    revenue,
    revenue * 100.0 / SUM(revenue) OVER () AS pct_of_total
FROM products;
```

### Example 4 — Day-over-day change with LAG

```sql
SELECT
    date,
    sales,
    sales - LAG(sales, 1) OVER (ORDER BY date) AS daily_change
FROM daily_sales;
```

### Frames (advanced)

```sql
SUM(sales) OVER (
    ORDER BY date
    ROWS BETWEEN 6 PRECEDING AND CURRENT ROW    -- 7-day rolling sum
) AS rolling_7d
```

---

## Common Table Expressions (CTEs)

> "**Named subquery** — improves readability + reusability."

### Syntax

```sql
WITH cte_name AS (
    SELECT ...
)
SELECT ... FROM cte_name;
```

### Example — Replace nested subqueries

#### Before (nested)

```sql
SELECT * FROM (
    SELECT user_id, COUNT(*) AS order_count
    FROM orders
    GROUP BY user_id
) t
WHERE t.order_count > 10;
```

#### After (CTE)

```sql
WITH user_orders AS (
    SELECT user_id, COUNT(*) AS order_count
    FROM orders
    GROUP BY user_id
)
SELECT * FROM user_orders WHERE order_count > 10;
```

### Multiple CTEs

```sql
WITH
    high_users AS (SELECT user_id FROM orders GROUP BY user_id HAVING COUNT(*) > 100),
    recent_users AS (SELECT user_id FROM users WHERE created_at > '2024-01-01')
SELECT * FROM high_users INTERSECT SELECT * FROM recent_users;
```

(MySQL doesn't natively support `INTERSECT` in 8.0; use `INNER JOIN` or `IN`.)

---

## Recursive CTEs

> "Self-referencing CTE — for **hierarchies** (org charts, categories, file trees)."

### Syntax

```sql
WITH RECURSIVE cte AS (
    -- Anchor (base case)
    SELECT ...
    UNION ALL
    -- Recursive part (refers to cte)
    SELECT ... FROM cte JOIN ...
)
SELECT * FROM cte;
```

### Example — Org hierarchy

```sql
CREATE TABLE employees (
    id INT,
    name VARCHAR(50),
    manager_id INT
);

WITH RECURSIVE org_chart AS (
    -- Anchor: top-level (CEO has no manager)
    SELECT id, name, manager_id, 0 AS level
    FROM employees
    WHERE manager_id IS NULL
    
    UNION ALL
    
    -- Recurse: each level's reports
    SELECT e.id, e.name, e.manager_id, oc.level + 1
    FROM employees e
    JOIN org_chart oc ON e.manager_id = oc.id
)
SELECT * FROM org_chart ORDER BY level, id;
```

### Example — Generate numbers

```sql
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < 10
)
SELECT * FROM numbers;
-- 1, 2, 3, ..., 10
```

### Limit depth

```sql
SET cte_max_recursion_depth = 1000;   -- default
```

→ Prevents runaway recursion.

---

## JSON Type & Functions

### Native JSON column

```sql
CREATE TABLE products (
    id INT PRIMARY KEY,
    attributes JSON
);

INSERT INTO products VALUES (1, '{"color": "red", "size": "M", "tags": ["new", "sale"]}');
```

### Extraction operators

| Operator | What |
|----------|------|
| `->` | `JSON_EXTRACT` (returns JSON) |
| `->>` | `JSON_UNQUOTE(JSON_EXTRACT)` (returns scalar) |

```sql
SELECT
    attributes->'$.color' AS color_json,           -- "red" (with quotes)
    attributes->>'$.color' AS color_text,          -- red (unquoted)
    attributes->'$.tags[0]' AS first_tag
FROM products;
```

### Functions

```sql
JSON_EXTRACT(col, '$.path')
JSON_UNQUOTE(JSON_EXTRACT(col, '$.path'))
JSON_SET(col, '$.field', value)         -- update field
JSON_INSERT(col, '$.new_field', value)  -- add new (no overwrite)
JSON_REPLACE(col, '$.field', value)     -- update existing only
JSON_REMOVE(col, '$.field')
JSON_CONTAINS(col, value, '$.path')
JSON_KEYS(col)
JSON_ARRAY('a', 'b', 'c')
JSON_OBJECT('k', 'v', 'k2', 'v2')
JSON_ARRAYAGG(col)                       -- aggregate column to array
JSON_OBJECTAGG(k, v)                     -- aggregate to object
```

### `JSON_TABLE` (powerful)

> "**Convert JSON array → relational rows**."

```sql
SELECT t.*
FROM products p,
JSON_TABLE(p.attributes, '$.tags[*]'
    COLUMNS (tag VARCHAR(50) PATH '$')
) t;
```

### Update field

```sql
UPDATE products
SET attributes = JSON_SET(attributes, '$.price', 1500)
WHERE id = 1;
```

### Index JSON

Functional index on path:

```sql
CREATE INDEX idx_color ON products( ((attributes->>'$.color')) );
```

→ Or generated stored column.

→ Cross-ref `02-Data-Types-and-Schema-Design.md`.

---

## Invisible Indexes

> "Index exists but optimizer ignores."

```sql
ALTER TABLE users ALTER INDEX idx_email INVISIBLE;
```

→ Test if index removal would hurt performance — without dropping (reversible).

```sql
ALTER TABLE users ALTER INDEX idx_email VISIBLE;
ALTER TABLE users DROP INDEX idx_email;
```

→ Cross-ref `03-Indexing-Deep-Dive.md`.

---

## Descending Indexes

> "Pre-8.0: index could only be ascending. **8.0+: real DESC indexes**."

### Pre-8 — DESC clause ignored

```sql
CREATE INDEX idx ON t (a, b DESC);   -- DESC silently ignored pre-8.0
ORDER BY a, b DESC                    -- requires filesort
```

### 8.0+ — actually descending

```sql
CREATE INDEX idx ON t (a, b DESC);
ORDER BY a, b DESC                    -- now uses index!
```

### Use case

Multi-column ORDER BY with mixed directions:

```sql
ORDER BY country ASC, sales DESC
-- Pre-8: filesort required
-- 8+: CREATE INDEX (country, sales DESC) → no filesort
```

---

## Functional Indexes

```sql
CREATE INDEX idx_lower_email ON users( (LOWER(email)) );
CREATE INDEX idx_color ON products( ((attributes->>'$.color')) );
```

→ Cross-ref `03-Indexing-Deep-Dive.md`.

---

## SKIP LOCKED & NOWAIT

> "**Queue patterns** — non-blocking row selection."

### Without SKIP LOCKED

```sql
-- Many workers
BEGIN;
SELECT * FROM jobs WHERE status = 'PENDING' LIMIT 1 FOR UPDATE;
-- Worker 2 blocks waiting for Worker 1
```

### With SKIP LOCKED

```sql
BEGIN;
SELECT * FROM jobs
WHERE status = 'PENDING'
ORDER BY priority, id
LIMIT 1
FOR UPDATE SKIP LOCKED;        -- skip rows others have locked

-- Each worker gets a different job; no waiting
UPDATE jobs SET status = 'PROCESSING' WHERE id = ?;
COMMIT;
```

→ **Production queue pattern** without external broker.

### NOWAIT

```sql
SELECT * FROM jobs WHERE id = 1 FOR UPDATE NOWAIT;
```

→ If row already locked, **immediately error** (no waiting).

```
ERROR 3572 (HY000): Statement aborted because lock(s) could not be acquired immediately
```

→ Use for "fail fast" patterns.

---

## Roles

> "Group privileges into reusable role; assign role to users."

### Pre-8 — directly grant per user (tedious for many users)

### 8.0+ — roles

```sql
CREATE ROLE 'app_read', 'app_write';

-- Grant to roles
GRANT SELECT ON appdb.* TO 'app_read';
GRANT INSERT, UPDATE, DELETE ON appdb.* TO 'app_write';

-- Create users + assign roles
CREATE USER 'reporting'@'%' IDENTIFIED BY 'pwd';
GRANT 'app_read' TO 'reporting'@'%';

CREATE USER 'app_user'@'%' IDENTIFIED BY 'pwd';
GRANT 'app_read', 'app_write' TO 'app_user'@'%';
```

### Activate role

```sql
SET DEFAULT ROLE ALL TO 'app_user'@'%';   -- auto-activate
-- OR per session
SET ROLE 'app_read', 'app_write';
```

→ Cross-ref `12-MySQL-Security.md`.

---

## Histograms

> "**Column-value distribution** captured for optimizer."

### Why?

Default optimizer uses **uniform assumption** for cardinality. If data skewed:

```
gender column: 'M' 80%, 'F' 20%
WHERE gender = 'M' → 80% of rows; index might be skipped (correct)
WHERE gender = 'F' → 20% of rows; index should be used
```

### Create histogram

```sql
ANALYZE TABLE users UPDATE HISTOGRAM ON gender, country WITH 100 BUCKETS;
```

→ Optimizer uses histogram for better selectivity estimates.

### View

```sql
SELECT * FROM information_schema.COLUMN_STATISTICS;
```

### Drop

```sql
ANALYZE TABLE users DROP HISTOGRAM ON gender;
```

### Where useful?

- Skewed distributions
- Low-cardinality columns
- Columns where indexes don't exist (but optimizer still benefits)

---

## Other Notable Improvements

### Atomic DDL

```sql
DROP TABLE t;     -- atomic in 8.0 (succeed or fail completely; no half-done state)
```

→ Pre-8 could leave inconsistent state on crash.

### Persisted system variables

```sql
SET PERSIST max_connections = 500;          -- writes to mysqld-auto.cnf
SET PERSIST_ONLY innodb_buffer_pool_size = 4G;
```

→ Survives restart without editing config files.

### caching_sha2_password (default)

→ Stronger than `mysql_native_password`.

→ Cross-ref `12-MySQL-Security.md`.

### Removed query cache

`query_cache_size` no longer exists. (Was unreliable; ProxySQL / app cache better.)

### Resource Groups

CPU pinning per query.

```sql
CREATE RESOURCE GROUP analytics
    TYPE = USER
    VCPU = 4-7
    THREAD_PRIORITY = 0;
```

### Common Table Expressions in `INSERT/UPDATE/DELETE`

```sql
WITH t AS (SELECT id FROM users WHERE inactive)
DELETE FROM users WHERE id IN (SELECT id FROM t);
```

### Lateral derived tables (8.0.14+)

```sql
SELECT * FROM users u,
LATERAL (SELECT COUNT(*) FROM orders WHERE user_id = u.id) AS oc;
```

### Optimizer hints expansion

`/*+ MAX_EXECUTION_TIME(2000) */` — query timeout.

### Spatial reference systems

Better GIS support.

---

## Pitfalls

1. **Window function on huge data** — runs through all rows; expensive without `PARTITION BY`.
2. **Recursive CTE infinite loop** — set `cte_max_recursion_depth`.
3. **JSON path string typo** — silently returns NULL, no error.
4. **`->` vs `->>` confusion** — `->` keeps quotes.
5. **Functional index on JSON** — must use **same expression syntax** in WHERE.
6. **Descending index needed** but used `ASC` index — filesort still happens for `DESC`.
7. **SKIP LOCKED without index** on `WHERE` column — locks too many rows.
8. **Too many roles** — complexity nightmare.
9. **No histograms on skewed columns** — optimizer wrong.
10. **Persisted variables forgotten** — `mysqld-auto.cnf` overrides `my.cnf`.

---

## Cheat Sheet

| Feature | Use |
|---------|-----|
| Window functions | Top-N per group, running totals |
| CTE | Readable nested queries |
| Recursive CTE | Hierarchies, sequences |
| JSON | Schema-less data |
| Invisible index | Test removal safely |
| Descending index | Mixed-direction ORDER BY |
| Functional index | LOWER(), JSON path |
| SKIP LOCKED | Queue patterns |
| Roles | Group privileges |
| Histograms | Skewed data optimizer |

| Window function | Pattern |
|-----------------|---------|
| `ROW_NUMBER()` | Unique sequential |
| `RANK()` | Ties same, gap after |
| `DENSE_RANK()` | Ties same, no gap |
| `LAG()` / `LEAD()` | Compare to previous/next |
| `SUM() OVER (ORDER BY ...)` | Running total |

| JSON | Use |
|------|-----|
| `->` | Extract JSON |
| `->>` | Extract scalar |
| `JSON_TABLE` | Array → rows |
| `JSON_SET` | Update field |

---

## Practice

1. Top 3 highest-paid employees per department using `ROW_NUMBER()`.
2. Org hierarchy via recursive CTE.
3. JSON column for product attributes; functional index on color path.
4. Implement queue worker pattern with `SKIP LOCKED`.
5. Create role for read-only reporting user.
6. Create histogram on a skewed column; observe EXPLAIN plan change.
