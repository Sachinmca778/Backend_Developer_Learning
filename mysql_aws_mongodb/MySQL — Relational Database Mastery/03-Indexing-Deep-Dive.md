# Indexing Deep Dive

## Status: Not Started

---

## Table of Contents

1. [Why Index?](#why-index)
2. [B-Tree Index (Default)](#b-tree-index-default)
3. [Hash Index](#hash-index)
4. [Full-Text Index](#full-text-index)
5. [Composite Indexes & Leftmost Prefix](#composite-indexes--leftmost-prefix)
6. [Covering Index](#covering-index)
7. [Functional Indexes (MySQL 8+)](#functional-indexes-mysql-8)
8. [Invisible Indexes](#invisible-indexes)
9. [Cardinality & Selectivity](#cardinality--selectivity)
10. [Clustered vs Secondary Index](#clustered-vs-secondary-index)
11. [When NOT to Index](#when-not-to-index)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Index?

> "Index = **shortcut** to find rows.  Without index → full table scan O(n). With B-tree index → O(log n)."

### Math

| Rows | Full scan | B-tree lookup |
|------|-----------|---------------|
| 10K | ~10ms | ~0.1ms |
| 1M | ~1 sec | ~0.5ms |
| 100M | ~100 sec | ~1ms |

→ Massive impact at scale.

### Cost

- **Disk space** (typically 5-20% of table size per index)
- **Slower writes** — every INSERT/UPDATE/DELETE updates index
- **Maintenance** — fragmentation over time

→ "**Don't index every column.** Index for query patterns."

---

## B-Tree Index (Default)

> "MySQL/InnoDB ka **default index type** — B+Tree (variant)."

### Structure

```
                       [50]
                      /    \
                  [25]     [75]
                  /  \      /  \
              [10] [40]  [60] [90]
              ↓    ↓     ↓    ↓
            (leaf nodes — sorted, linked)
```

### Key properties

- **Self-balancing** — height stays O(log n)
- **Sorted** — supports range queries + ORDER BY
- **Leaf nodes linked** — efficient range scan
- **Each node = 16KB page** (in InnoDB)

### What B-tree supports

| Operation | Works? |
|-----------|--------|
| `WHERE col = ?` | ✅ Equality |
| `WHERE col > ?`, `<`, `BETWEEN` | ✅ Range |
| `WHERE col LIKE 'prefix%'` | ✅ Prefix |
| `WHERE col LIKE '%suffix'` | ❌ Wildcard at start = no index |
| `ORDER BY col` | ✅ |
| `GROUP BY col` | ✅ |
| `WHERE col IN (...)` | ✅ |
| `WHERE col != ?` | ⚠️ May not use index (low selectivity) |
| `WHERE col IS NULL` | ✅ (NULLs are indexed in InnoDB) |
| `WHERE func(col) = ?` | ❌ unless functional index |

### Create

```sql
CREATE INDEX idx_users_email ON users(email);
-- or
ALTER TABLE users ADD INDEX idx_email(email);
```

### Drop

```sql
DROP INDEX idx_users_email ON users;
ALTER TABLE users DROP INDEX idx_email;
```

---

## Hash Index

> "Memory engine + InnoDB adaptive hash index. **Equality only** — no range."

### Memory engine

```sql
CREATE TABLE cache (
    key VARCHAR(50),
    value VARCHAR(255),
    INDEX USING HASH (key)
) ENGINE=Memory;
```

### What hash supports

| Operation | Works? |
|-----------|--------|
| `WHERE col = ?` | ✅ O(1) |
| `WHERE col > ?` | ❌ |
| `ORDER BY col` | ❌ |
| `LIKE 'prefix%'` | ❌ |

→ **Niche use**. Memory engine for ephemeral hot lookup.

### Adaptive Hash Index (InnoDB)

InnoDB monitors hot B-tree pages → auto-builds hash → speedup. Transparent.

```sql
SHOW VARIABLES LIKE 'innodb_adaptive_hash_index';
```

---

## Full-Text Index

> "**Text search** — keyword matching with relevance scoring."

### Create

```sql
CREATE TABLE articles (
    id INT PRIMARY KEY,
    title VARCHAR(255),
    body TEXT,
    FULLTEXT(title, body)
);
```

### Query — `MATCH ... AGAINST`

```sql
-- Natural language mode (default)
SELECT * FROM articles
WHERE MATCH(title, body) AGAINST('mysql performance');

-- Boolean mode (advanced operators)
SELECT * FROM articles
WHERE MATCH(title, body) AGAINST('+mysql +performance -slow' IN BOOLEAN MODE);

-- With relevance score
SELECT *, MATCH(title, body) AGAINST('mysql performance') AS score
FROM articles
ORDER BY score DESC;
```

### Boolean operators

| Op | Meaning |
|----|---------|
| `+word` | Must contain |
| `-word` | Must NOT contain |
| `word*` | Wildcard prefix |
| `"phrase"` | Exact phrase |
| `>word` | Increase relevance |
| `<word` | Decrease relevance |

### Limitations

- **Min word length** = 4 chars (default `ft_min_word_len`)
- **Stop words** ignored ("the", "and", etc.)
- Stemming limited
- For real search (Elasticsearch-level) → use Elasticsearch / OpenSearch

### When?

- Light text search (FAQs, products, articles)
- Few hundred MB to GB of text
- Don't want extra service

---

## Composite Indexes & Leftmost Prefix

### Multi-column index

```sql
CREATE INDEX idx_user_status_created ON orders(user_id, status, created_at);
```

### Leftmost prefix rule

Index can be used for queries that **filter by leftmost columns in order**:

| Query | Uses index? |
|-------|-------------|
| `WHERE user_id = ?` | ✅ |
| `WHERE user_id = ? AND status = ?` | ✅ |
| `WHERE user_id = ? AND status = ? AND created_at > ?` | ✅ |
| `WHERE status = ?` | ❌ (skipped user_id) |
| `WHERE created_at > ?` | ❌ |
| `WHERE user_id = ? AND created_at > ?` | ✅ partial (uses user_id, then range filter) |
| `WHERE user_id = ? AND status IN (...) AND created_at > ?` | ✅ |

### Order matters!

`(a, b, c)` ≠ `(c, b, a)`. Order based on:

1. **Equality columns first**
2. **Then range columns**

→ Equality columns "narrow down"; range "scans".

### Example design

```sql
-- Query: WHERE user_id = ? AND status = 'ACTIVE' AND created_at > ?
-- Best index:
INDEX(user_id, status, created_at)

-- Why? user_id eq, then status eq, then created_at range.
```

---

## Covering Index

> "All columns needed for query are **inside the index** — no table fetch needed."

### Example

```sql
-- Index
CREATE INDEX idx_email_name ON users(email, name);

-- Query
SELECT name FROM users WHERE email = 'sachin@example.com';
```

→ Index already has `name` → no table lookup. EXPLAIN shows **`Using index`**.

### How to design

For frequent queries, include selected columns in the composite index:

```sql
-- Frequent query
SELECT id, name, email FROM users WHERE created_at > ?;

-- Covering index
CREATE INDEX idx_created_covering ON users(created_at, id, name, email);
```

→ All 4 columns in index. Single index scan, no table fetch.

### Trade-off

Larger index = more disk + slower writes. Use for hot queries only.

### Note

InnoDB **secondary indexes always include PK** in leaf nodes (clustered index pointer). So `id` is "free" in any secondary index covering check.

---

## Functional Indexes (MySQL 8+)

> "Index on **expression**, not just column."

### Pre-MySQL 8

```sql
SELECT * FROM users WHERE LOWER(email) = 'sachin@example.com';
-- ❌ Index on email NOT used (function applied)
```

### MySQL 8+

```sql
CREATE INDEX idx_lower_email ON users( (LOWER(email)) );

-- Now works:
SELECT * FROM users WHERE LOWER(email) = 'sachin@example.com';
-- ✅ Uses idx_lower_email
```

### Other use cases

```sql
-- JSON path index
CREATE INDEX idx_attr_color ON products( ((attributes->>'$.color')) );

-- Date functions
CREATE INDEX idx_year ON orders( (YEAR(created_at)) );

-- Computed column
CREATE INDEX idx_total ON orders( (qty * price) );
```

### Generated columns alternative

```sql
ALTER TABLE products ADD COLUMN color VARCHAR(20)
    GENERATED ALWAYS AS (attributes->>'$.color') STORED;

CREATE INDEX idx_color ON products(color);
```

→ Generated stored column = explicit; functional index = implicit.

---

## Invisible Indexes

> "Index exists but optimizer ignores. Test impact of dropping without committing."

### MySQL 8+

```sql
ALTER TABLE users ALTER INDEX idx_email INVISIBLE;
```

→ Optimizer skips it. Queries that depended on it now slower.

→ Verify performance impact in staging / prod (low traffic window).

→ If safe to remove:

```sql
ALTER TABLE users DROP INDEX idx_email;
```

→ If needed back:

```sql
ALTER TABLE users ALTER INDEX idx_email VISIBLE;
```

### Use case

Cleaning up old / unused indexes safely.

---

## Cardinality & Selectivity

### Cardinality

> "Number of **unique values** in a column."

```sql
SHOW INDEX FROM users;
-- Cardinality column for each index
```

### Selectivity

> "Cardinality / total rows. **Higher = more selective = better index candidate**."

| Column | Cardinality / Total | Selectivity | Index? |
|--------|---------------------|-------------|--------|
| user_id (1M users) | 1M / 1M | 1.0 | ✅ Excellent |
| email | 999K / 1M | 0.999 | ✅ Excellent |
| status (5 values) | 5 / 1M | 0.000005 | ❌ Don't index alone |
| is_deleted (boolean) | 2 / 1M | 0.000002 | ❌ |

### Composite for low-selectivity

```sql
-- status alone: bad
-- (user_id, status): good — equality on user_id narrows fast
INDEX(user_id, status)
```

### Update statistics

```sql
ANALYZE TABLE users;   -- recompute cardinality / histogram
```

→ Run periodically; bad stats → bad query plans.

---

## Clustered vs Secondary Index

### InnoDB clustered index = primary key

```
Clustered index leaf = Actual row data (stored in PK order on disk)
```

### Secondary indexes

```
Secondary index leaf = (indexed columns, primary_key_value)
```

→ Lookup via secondary:
1. Find matching entry in secondary → get PK value
2. Lookup PK in clustered → get full row

→ "**Secondary index = always 2 lookups** (unless covering)."

### Implications

#### 1. PK choice matters big

- **Auto-increment INT/BIGINT** = compact, sequential — minimal index size, no page splits
- **UUID** = random, large (16 bytes), causes page splits + bad cache behaviour

→ Use `BIGINT UNSIGNED AUTO_INCREMENT` PK + UUID as separate column if needed.

#### 2. Smaller PK = smaller secondary indexes

Every secondary index stores PK → if PK is BIGINT (8B) vs UUID (16B) → big difference at scale.

#### 3. PK lookups are fastest (clustered)

```sql
SELECT * FROM users WHERE id = 1234;   -- single B-tree traversal
```

---

## When NOT to Index

### 1. Tiny tables (<1000 rows)

Full scan faster than index lookup overhead.

### 2. Write-heavy tables with low read

Index update cost > read benefit.

### 3. Low-selectivity columns alone

`is_active`, `gender`, etc. — index won't be used.

### 4. Very wide columns (TEXT, BLOB) without prefix

Use prefix index:

```sql
CREATE INDEX idx_body ON articles(body(100));   -- first 100 chars
```

### 5. Columns rarely / never queried

Audit query logs first.

---

## Common Output Traps

### Q1. Index not used due to function

```sql
WHERE YEAR(created_at) = 2024     -- ❌ no index
WHERE created_at >= '2024-01-01' AND created_at < '2025-01-01'  -- ✅ uses index
```

### Q2. Implicit type conversion

```sql
WHERE id = '1234'             -- string vs INT — may not use index
WHERE id = 1234               -- ✅
```

### Q3. Leading wildcard

```sql
WHERE name LIKE '%sachin'     -- ❌
WHERE name LIKE 'sachin%'     -- ✅
```

### Q4. OR with different columns

```sql
WHERE user_id = ? OR email = ?    -- May not use either index efficiently
```

→ Use UNION or composite index logic.

### Q5. Composite index leftmost violated

```sql
INDEX(a, b, c)
WHERE b = ? AND c = ?         -- ❌ leftmost a missing
```

---

## Pitfalls

1. **Over-indexing** — every column indexed; writes slow.
2. **Under-indexing** — full scans on common queries.
3. **Wrong column order** in composite — leftmost prefix violated.
4. **UUID as PK** — page splits + bloat.
5. **Function on column in WHERE** — index unused.
6. **Implicit type cast** — index unused (string vs int).
7. **Stale stats** — `ANALYZE TABLE` periodically.
8. **No index on FK** — slow JOINs / `ON DELETE CASCADE`.
9. **Leading wildcard LIKE** — no index.
10. **Indexing low-selectivity column alone** — useless.
11. **Forgetting `Using index`** in EXPLAIN — opportunity for covering index missed.
12. **Not testing with realistic data** — small dev DB looks fine; prod tanks.

---

## Cheat Sheet

| Index Type | Use |
|------------|-----|
| B-tree | Default; equality + range + ORDER BY |
| Hash | Memory engine, equality only |
| Full-text | Text search |
| Spatial | Geospatial (R-tree) |
| Functional (8+) | Index on expression |
| Invisible (8+) | Test removing |

| Composite | Order |
|-----------|-------|
| Equality first | ✅ |
| Range after equality | ✅ |
| Most-selective first (when equality) | ✅ |

| Common patterns | Tip |
|-----------------|-----|
| `WHERE a = ? AND b > ?` | INDEX(a, b) |
| `WHERE a = ? ORDER BY b` | INDEX(a, b) — sorted! |
| `WHERE a = ? AND b = ?` | INDEX(a, b) or INDEX(b, a) — pick higher selectivity first |

| Diagnostic | Command |
|-----------|---------|
| Show indexes on table | `SHOW INDEX FROM tbl;` |
| Update stats | `ANALYZE TABLE tbl;` |
| Optimize index | `OPTIMIZE TABLE tbl;` (rebuilds) |
| EXPLAIN | `EXPLAIN SELECT ...;` |

---

## Practice

1. Take a slow query → run EXPLAIN → identify missing index → create → re-EXPLAIN.
2. Design composite index for: `WHERE user_id = ? AND status = ? ORDER BY created_at DESC LIMIT 20`.
3. Add covering index for top 5 SELECT queries on `users` table.
4. Find unused indexes via `sys.schema_unused_indexes`.
5. Migrate UUID PK → BIGINT PK; measure index size diff.
6. Use functional index for case-insensitive email lookup.
