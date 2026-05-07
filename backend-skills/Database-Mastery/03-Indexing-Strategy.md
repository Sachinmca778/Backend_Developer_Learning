# Indexing Strategy

## Status: Not Started

---

## Table of Contents

1. [Index Kya Hai?](#index-kya-hai)
2. [B-tree Index](#b-tree-index)
3. [Hash Index](#hash-index)
4. [GIN Index](#gin-index)
5. [GiST Index](#gist-index)
6. [Partial Index](#partial-index)
7. [Composite Index (Leftmost Prefix Rule)](#composite-index-leftmost-prefix-rule)
8. [Covering Index (INCLUDE)](#covering-index-include)
9. [Index Bloat](#index-bloat)
10. [When NOT to Index](#when-not-to-index)

---

## Index Kya Hai?

**Matlab:** Index ek alag data structure hai jo specific column(s) ko fast lookup ke liye organize karta hai. Book ke index ki tarah — pure book read karne ki bajaye, page number directly mil jaata hai.

### Trade-off

| Pro | Con |
|-----|-----|
| Read 100x-1000x faster | Write slow (index bhi update hota hai) |
| `WHERE`, `JOIN`, `ORDER BY` fast | Disk space extra (10-30% of table size) |
| `UNIQUE` constraint enforce | Maintenance overhead (vacuum) |

### Without Index — Sequential Scan

```sql
SELECT * FROM users WHERE email = 'rahul@example.com';
-- Postgres reads EVERY row → O(N)
-- 10M rows = 10M comparisons!
```

### With Index — Index Scan

```sql
CREATE INDEX idx_users_email ON users(email);

SELECT * FROM users WHERE email = 'rahul@example.com';
-- B-tree lookup → O(log N)
-- 10M rows = ~24 comparisons
```

---

## B-tree Index

**Matlab:** Postgres ka **default** index type. Balanced tree structure. Sabse common use case.

### Use Cases

- Equality: `=`
- Range: `<`, `<=`, `>`, `>=`, `BETWEEN`
- `IN`, `IS NULL`, `IS NOT NULL`
- `LIKE 'prefix%'` (left-anchored only!)
- `ORDER BY` (sorted output)

### Creating

```sql
CREATE INDEX idx_users_email ON users(email);
-- Default: B-tree

-- Explicit
CREATE INDEX idx_users_email ON users USING BTREE(email);
```

### What B-tree Can't Help With

```sql
-- ❌ Function applied → can't use index
SELECT * FROM users WHERE LOWER(email) = 'rahul@example.com';

-- ✅ Solution: Functional index
CREATE INDEX idx_users_email_lower ON users(LOWER(email));

-- ❌ Right-anchored LIKE
SELECT * FROM users WHERE email LIKE '%@gmail.com';

-- ✅ Solution: pg_trgm GIN index, or reverse the column
CREATE INDEX idx_users_email_trgm ON users USING GIN (email gin_trgm_ops);
```

### How B-tree Works (Mental Model)

```
        [50]
       /    \
    [25]    [75]
    / \     / \
  [10][30][60][90]
   leaves contain (key, row pointer)
```

- Tree depth ~3-5 levels for billions of rows
- Each node fits in 8KB page
- Sorted → range queries fast

---

## Hash Index

**Matlab:** Hash function ke through key → bucket. Sirf equality.

### Pros / Cons

| Pro | Con |
|-----|-----|
| O(1) lookup (better than B-tree's O(log N)) | Only equality (no range, no ORDER BY) |
| Smaller for large unique keys | Crash recovery weak (pre PG10) |

### Creating

```sql
CREATE INDEX idx_users_email_hash ON users USING HASH(email);
```

### When to Use?

**Almost never** in practice. B-tree handles equality fine, and is more versatile. Hash index sirf tab consider karo jab:
- Sirf `=` queries hain
- Bohot bade unique keys (UUID/long strings)
- Index size B-tree se choti chahiye

### B-tree vs Hash

| Operation | B-tree | Hash |
|-----------|--------|------|
| `=` | ✅ O(log N) | ✅ O(1) |
| `<`, `>` | ✅ | ❌ |
| `BETWEEN` | ✅ | ❌ |
| `ORDER BY` | ✅ | ❌ |
| `LIKE 'x%'` | ✅ | ❌ |
| Unique constraint | ✅ | ❌ |

---

## GIN Index

**Matlab:** **G**eneralized **IN**verted Index. Postgres ka "search engine"-style index — har element ka inverted list maintain karta hai.

### Use Cases

- `JSONB` (containment, key existence)
- Arrays (`@>`, `<@`, `&&`)
- Full-text search (`tsvector`)
- Trigrams (`pg_trgm` for fuzzy LIKE)

### How It Works (Mental Model)

```
Document 1: ["apple", "banana", "cherry"]
Document 2: ["banana", "date"]
Document 3: ["apple", "date"]

GIN inverted index:
  apple  → [1, 3]
  banana → [1, 2]
  cherry → [1]
  date   → [2, 3]

Query: arrays containing "apple"
→ Direct lookup: [1, 3]
```

### Examples

```sql
-- JSONB
CREATE INDEX idx_products_meta ON products USING GIN (metadata);
SELECT * FROM products WHERE metadata @> '{"brand": "Apple"}';

-- Array
CREATE INDEX idx_posts_tags ON posts USING GIN (tags);
SELECT * FROM posts WHERE tags @> ARRAY['postgres'];
SELECT * FROM posts WHERE tags && ARRAY['sql', 'postgres'];  -- overlap

-- Full-text
CREATE INDEX idx_articles_search ON articles USING GIN (search_vector);
SELECT * FROM articles WHERE search_vector @@ to_tsquery('english', 'postgres');

-- Trigram (fast LIKE)
CREATE EXTENSION pg_trgm;
CREATE INDEX idx_users_name_trgm ON users USING GIN (name gin_trgm_ops);
SELECT * FROM users WHERE name ILIKE '%rahul%';
```

### `jsonb_path_ops` (Smaller Variant)

```sql
-- Smaller, faster, but only @> operator
CREATE INDEX idx_products_meta_path ON products 
USING GIN (metadata jsonb_path_ops);
```

### Trade-off

| Pro | Con |
|-----|-----|
| Fast lookups for set membership | Slow updates (har element re-index) |
| Multi-element search efficient | Larger index size |

---

## GiST Index

**Matlab:** **G**eneralized **S**earch **T**ree. Pluggable index for "tree-able" data — geometric, ranges, full-text.

### Use Cases

- Geometric data (PostGIS)
- Range types (`int4range`, `tsrange`)
- Full-text (alternative to GIN)
- Nearest-neighbor search

### Examples

```sql
-- Geometric / spatial (PostGIS)
CREATE EXTENSION postgis;
CREATE INDEX idx_locations_geo ON locations USING GIST (point);
SELECT * FROM locations 
ORDER BY point <-> ST_MakePoint(12.97, 77.59)
LIMIT 5;  -- 5 nearest

-- Range type — exclusion constraints (no overlap)
CREATE TABLE bookings (
    room_id INT,
    period TSRANGE,
    EXCLUDE USING GIST (room_id WITH =, period WITH &&)
);
-- Same room ke overlapping bookings prevent

-- Full-text
CREATE INDEX idx_articles_search_gist ON articles USING GIST (search_vector);
```

### GIN vs GiST (Full-Text)

| | GIN | GiST |
|--|-----|------|
| Lookup speed | Faster (3x) | Slower |
| Build/update speed | Slower | Faster |
| Index size | Larger | Smaller |
| Best for | Read-heavy | Write-heavy |

---

## Partial Index

**Matlab:** Index sirf **subset** of rows par — `WHERE` clause se filter.

### Why?

- Smaller index = faster
- Less disk space
- Less maintenance

### Examples

```sql
-- Sirf active users par index
CREATE INDEX idx_users_email_active ON users(email)
WHERE is_active = true;

-- Soft-deleted rows skip
CREATE INDEX idx_orders_user_id ON orders(user_id)
WHERE deleted_at IS NULL;

-- Rare but important: pending orders only
CREATE INDEX idx_orders_pending ON orders(created_at)
WHERE status = 'pending';
```

### When Postgres Uses Partial Index

Query ka `WHERE` clause partial index ke `WHERE` clause ko **imply** karna chahiye.

```sql
-- Index: WHERE is_active = true
CREATE INDEX idx_users_email_active ON users(email)
WHERE is_active = true;

-- ✅ Uses index
SELECT * FROM users WHERE email = 'x@y.com' AND is_active = true;

-- ❌ Doesn't use index (no WHERE is_active = true)
SELECT * FROM users WHERE email = 'x@y.com';
```

### Real-world Use Case

```sql
-- 99% orders are 'completed', 1% 'pending'
-- Most queries scan 'pending' for processing

-- Bad: full index — wasted on 'completed' rows
CREATE INDEX idx_orders_status ON orders(status);

-- Good: partial — 100x smaller
CREATE INDEX idx_orders_pending ON orders(id)
WHERE status = 'pending';
```

---

## Composite Index (Leftmost Prefix Rule)

**Matlab:** Multiple columns par single index. **Column order matters!**

### Creating

```sql
CREATE INDEX idx_orders_user_date ON orders(user_id, created_at);
```

### Leftmost Prefix Rule

Index `(A, B, C)` is usable when query filters on:
- `A`
- `A, B`
- `A, B, C`
- `A` + range on `B`

But **NOT** for:
- `B` alone
- `C` alone
- `B, C`

### Examples

```sql
-- Index: (user_id, created_at, status)

-- ✅ Uses index
WHERE user_id = 42;
WHERE user_id = 42 AND created_at > '2024-01-01';
WHERE user_id = 42 AND created_at > '2024-01-01' AND status = 'paid';

-- ❌ Doesn't use index (no user_id)
WHERE created_at > '2024-01-01';
WHERE status = 'paid';

-- ⚠️ Partial use — only user_id portion
WHERE user_id = 42 AND status = 'paid';  
-- (skips middle column → status check is post-filter)
```

### Column Order Strategy

**General rule:** Most selective / most-equal-filtered column first.

```sql
-- Query: WHERE country = 'IN' AND age > 25
-- country has 200 distinct values, age has range

-- ✅ Better: equality first, range last
CREATE INDEX idx_users ON users(country, age);

-- ❌ Worse: range first
CREATE INDEX idx_users ON users(age, country);
```

**Real example:**
```sql
-- App query patterns:
--   1. WHERE user_id = ?
--   2. WHERE user_id = ? AND status = ?
--   3. WHERE user_id = ? AND status = ? ORDER BY created_at DESC

-- Single composite index covers all 3
CREATE INDEX idx_orders_compound ON orders(user_id, status, created_at DESC);
```

### When NOT to Use Composite

- Each column queried independently → separate single-column indexes
- Bitmap index scan can combine multiple single-column indexes

---

## Covering Index (INCLUDE)

**Matlab:** Index mein extra columns "include" karna jo filter mein use nahi hote, but `SELECT` mein chahiye. Index-only scan possible.

### Without Covering Index

```sql
CREATE INDEX idx_orders_user ON orders(user_id);

SELECT amount, status FROM orders WHERE user_id = 42;
-- 1. Index lookup → finds row pointers
-- 2. Heap fetch → reads actual row from table for 'amount', 'status'
-- Two-step process
```

### With Covering Index

```sql
CREATE INDEX idx_orders_user_covering 
ON orders(user_id) INCLUDE (amount, status);

SELECT amount, status FROM orders WHERE user_id = 42;
-- 1. Index has user_id, amount, status
-- 2. Index-only scan → no table read
-- Faster! (especially for wide tables)
```

### Verify with EXPLAIN

```sql
EXPLAIN (ANALYZE, BUFFERS) 
SELECT amount, status FROM orders WHERE user_id = 42;

-- Look for: "Index Only Scan using idx_orders_user_covering"
```

### Trade-off

| Pro | Con |
|-----|-----|
| Index-only scans → faster | Larger index (extra columns stored) |
| Avoid heap fetches | Slower writes |

### When?

- Hot read query path
- Wide tables where reading the row is expensive
- Few extra columns (don't include 50 columns!)

---

## Index Bloat

**Matlab:** Time ke saath, index mein dead entries collect ho jaate hain (UPDATE/DELETE ki wajah se). Index size badh jaata hai but useful entries kam.

### Why It Happens

- Postgres MVCC: UPDATE = old version + new version
- Index entries point to dead tuples
- VACUUM cleans dead tuples but doesn't shrink index much

### Detect Bloat

```sql
SELECT 
    schemaname, tablename, indexname,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size,
    idx_scan AS index_scans
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC
LIMIT 20;
```

### Find Unused Indexes

```sql
SELECT schemaname, tablename, indexname, idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
ORDER BY pg_relation_size(indexrelid) DESC;
-- Drop these — they cost writes for no benefit
```

### Fix Bloat

```sql
-- Method 1: REINDEX (locks index — DON'T use in prod)
REINDEX INDEX idx_orders_user;

-- Method 2: REINDEX CONCURRENTLY (Postgres 12+)
REINDEX INDEX CONCURRENTLY idx_orders_user;

-- Method 3: pg_repack extension (online, no lock)
```

### Prevention

- Tune `autovacuum`
- Use partial indexes where possible
- Avoid over-indexing

---

## When NOT to Index

### 1. Small Tables

```
< 1000 rows → seq scan often faster than index
```

### 2. Low-Cardinality Columns

```sql
-- Bad: only 2 distinct values (true/false)
CREATE INDEX idx_users_active ON users(is_active);
-- Postgres might skip index, do seq scan
-- ✅ Use partial index instead
```

### 3. Frequently Updated Columns

Index ko bhi update karna padta hai → write performance hit.

### 4. Write-Heavy, Read-Rare Tables

Audit logs, event streams → indexes might cost more than they save.

### 5. Already Covered by Composite

```sql
-- Redundant — composite already covers user_id queries
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_user_date ON orders(user_id, created_at);
-- Drop the first one
```

---

## Index Selection Decision Tree

```
Need to filter / join / sort?
├── Equality + range + sort?       → B-tree (default)
├── Only equality, fast?           → Hash (rare)
├── JSONB / array / full-text?     → GIN
├── Spatial / range type / KNN?    → GiST
├── Subset of rows?                → Partial
├── Multiple columns together?     → Composite (leftmost first!)
└── SELECT cols not filtered?      → INCLUDE (covering)
```

---

## Common Mistakes

1. **Indexing every column** → write performance dies
2. **Wrong column order** in composite (leftmost rule!)
3. **Function on indexed column** → index bypassed
4. **Forgetting partial index** for skewed data
5. **Not monitoring** unused indexes (`pg_stat_user_indexes`)
6. **`SELECT *`** when covering index could help

---

## Summary Cheat Sheet

| Index Type | Best For |
|-----------|----------|
| **B-tree** | Default — equality + range + sort |
| **Hash** | Pure equality (rarely used) |
| **GIN** | JSONB, arrays, full-text, trigrams |
| **GiST** | Spatial, ranges, KNN |
| **Partial** | Skewed data (filter subset) |
| **Composite** | Multi-column queries (leftmost prefix!) |
| **Covering** | Index-only scans |

| Operation | Index? |
|-----------|--------|
| `WHERE col = ?` | ✅ B-tree |
| `WHERE col > ?` | ✅ B-tree |
| `WHERE LOWER(col) = ?` | ✅ Functional index on `LOWER(col)` |
| `WHERE col LIKE 'x%'` | ✅ B-tree |
| `WHERE col LIKE '%x%'` | ✅ GIN with pg_trgm |
| `WHERE jsonb_col @> ?` | ✅ GIN |
| `WHERE array_col && ?` | ✅ GIN |
| `WHERE NOT col = ?` | ❌ Usually seq scan |

---

## Practice

1. Find slow queries in your app, add appropriate indexes, verify with `EXPLAIN ANALYZE`.
2. Create a composite index — try different column orders, observe plans.
3. Add a partial index for `WHERE status = 'pending'` orders, test query.
4. Identify and drop unused indexes via `pg_stat_user_indexes`.
5. Set up GIN index for JSONB column, test containment query speed before/after.
6. Compare index-only scan vs index scan using `INCLUDE` columns.
