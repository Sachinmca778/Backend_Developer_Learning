# PostgreSQL Specific Features

## Status: Not Started

---

## Table of Contents

1. [JSONB Type](#jsonb-type)
2. [Full-Text Search](#full-text-search)
3. [Advisory Locks](#advisory-locks)
4. [LISTEN / NOTIFY (Pub-Sub)](#listen--notify-pub-sub)
5. [Materialized Views](#materialized-views)
6. [Table Partitioning](#table-partitioning)
7. [pg_stat_statements](#pg_stat_statements)
8. [VACUUM and ANALYZE](#vacuum-and-analyze)

---

## JSONB Type

**Matlab:** `JSONB` = "JSON Binary" — JSON data ko binary format mein store karta hai. Indexable hai, fast queries deta hai.

### JSON vs JSONB

| Feature | `JSON` | `JSONB` |
|---------|--------|---------|
| Storage | Text (raw) | Binary (parsed) |
| Insert speed | Faster | Slower (parsing overhead) |
| Read speed | Slower (parse on read) | Much faster |
| Indexing | ❌ Not supported | ✅ GIN index |
| Whitespace preserved | ✅ | ❌ |
| Duplicate keys | ✅ All kept | ❌ Last wins |
| Key order | Preserved | Not preserved |
| **Production use** | ❌ | ✅ Always JSONB |

### Creating JSONB Column

```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name TEXT,
    metadata JSONB
);

INSERT INTO products (name, metadata) VALUES
('Phone', '{"brand": "Apple", "price": 80000, "specs": {"ram": "8GB", "storage": "256GB"}}'),
('Laptop', '{"brand": "Dell", "price": 60000, "specs": {"ram": "16GB", "storage": "512GB"}}');
```

### Query Operators

```sql
-- ->  : returns JSONB
-- ->> : returns text

SELECT metadata->'brand' FROM products;       -- "Apple" (JSONB)
SELECT metadata->>'brand' FROM products;      -- Apple (text)

-- Nested access
SELECT metadata->'specs'->>'ram' FROM products;   -- 8GB

-- Path operators (faster for deep nesting)
SELECT metadata #> '{specs,ram}' FROM products;   -- "8GB" (JSONB)
SELECT metadata #>> '{specs,ram}' FROM products;  -- 8GB (text)
```

### Containment Operators

```sql
-- @> : "contains" (left side contains right)
SELECT * FROM products WHERE metadata @> '{"brand": "Apple"}';

-- <@ : "contained by"
SELECT * FROM products WHERE '{"brand": "Apple"}' <@ metadata;

-- ? : key exists
SELECT * FROM products WHERE metadata ? 'price';

-- ?| : ANY of these keys exist
SELECT * FROM products WHERE metadata ?| ARRAY['price', 'cost'];

-- ?& : ALL these keys exist
SELECT * FROM products WHERE metadata ?& ARRAY['brand', 'price'];
```

### GIN Index for Fast JSONB Queries

```sql
-- Default GIN: supports @>, ?, ?|, ?& operators
CREATE INDEX idx_products_metadata ON products USING GIN (metadata);

-- jsonb_path_ops: smaller, faster, but only @>
CREATE INDEX idx_products_metadata_path ON products 
USING GIN (metadata jsonb_path_ops);
```

### Updating JSONB

```sql
-- Set a key
UPDATE products 
SET metadata = jsonb_set(metadata, '{price}', '90000')
WHERE id = 1;

-- Add nested key
UPDATE products 
SET metadata = jsonb_set(metadata, '{specs,color}', '"Black"', true)
WHERE id = 1;

-- Remove a key
UPDATE products 
SET metadata = metadata - 'price'
WHERE id = 1;

-- Remove nested key (path)
UPDATE products 
SET metadata = metadata #- '{specs,color}'
WHERE id = 1;

-- Merge two JSONBs (|| operator)
UPDATE products 
SET metadata = metadata || '{"in_stock": true}'::jsonb
WHERE id = 1;
```

### When to Use JSONB

✅ **Good fit:**
- Schema flexible / evolving (e.g., user preferences, metadata)
- Sparse attributes (har row mein different keys)
- Document-style storage
- API payloads cache

❌ **Bad fit:**
- Highly structured data → use proper columns
- Frequent partial updates of large JSONB → expensive
- Need foreign keys / joins on inner fields

---

## Full-Text Search

**Matlab:** Postgres ka built-in search engine. Stemming, ranking, language-aware search.

### Basic Concepts

- **`tsvector`** → document ka searchable representation (lexemes + positions)
- **`tsquery`** → search query
- **`@@`** → match operator

### Simple Example

```sql
SELECT to_tsvector('english', 'The quick brown fox jumps');
-- Output: 'brown':3 'fox':4 'jump':5 'quick':2

SELECT to_tsquery('english', 'jumping & fox');
-- Output: 'jump' & 'fox'

SELECT to_tsvector('english', 'The quick brown fox jumps') @@
       to_tsquery('english', 'jumping & fox');
-- Output: TRUE
```

**Note:** `jumps` aur `jumping` dono `jump` lexeme mein convert ho gaye → stemming!

### Search on a Table

```sql
CREATE TABLE articles (
    id SERIAL PRIMARY KEY,
    title TEXT,
    body TEXT
);

-- Search
SELECT * FROM articles
WHERE to_tsvector('english', title || ' ' || body) 
   @@ to_tsquery('english', 'postgres & tutorial');
```

### Generated tsvector Column + GIN Index

```sql
ALTER TABLE articles 
ADD COLUMN search_vector tsvector
GENERATED ALWAYS AS (
    to_tsvector('english', coalesce(title, '') || ' ' || coalesce(body, ''))
) STORED;

CREATE INDEX idx_articles_search ON articles USING GIN (search_vector);

-- Now queries are fast
SELECT title FROM articles
WHERE search_vector @@ to_tsquery('english', 'postgres & tutorial');
```

### Ranking Results

```sql
SELECT title, ts_rank(search_vector, query) AS rank
FROM articles, to_tsquery('english', 'postgres') AS query
WHERE search_vector @@ query
ORDER BY rank DESC
LIMIT 10;
```

### Operators in tsquery

```sql
to_tsquery('english', 'postgres & tutorial')   -- AND
to_tsquery('english', 'postgres | mysql')      -- OR
to_tsquery('english', '!mongodb')              -- NOT
to_tsquery('english', 'postgres <-> tutorial') -- phrase (adjacent)

-- Or use plainto_tsquery / websearch_to_tsquery for user input
SELECT websearch_to_tsquery('english', 'postgres OR mysql -mongodb');
```

### `pg_trgm` Extension (Trigram)

For fuzzy matching, typo tolerance, `LIKE '%xyz%'` speedup.

```sql
CREATE EXTENSION pg_trgm;

-- Similarity
SELECT similarity('postgresql', 'postgrsql');   -- 0.66 (typo tolerant)

-- GIN/GiST index for fast LIKE queries
CREATE INDEX idx_users_name_trgm ON users USING GIN (name gin_trgm_ops);

-- Now fast
SELECT * FROM users WHERE name ILIKE '%rahul%';

-- Fuzzy match
SELECT * FROM users WHERE name % 'rahul';   -- similarity > threshold
```

---

## Advisory Locks

**Matlab:** Application-level locks jo DB tables se decouple hain. Use case: distributed locking, cron-job dedup.

### Why Advisory Locks?

- Row-level locks rows par tied hote hain
- Advisory locks **logical** hote hain — koi bhi number/string ko lock kar sakte ho
- DB connection ke saath tied hote hain (auto-release on disconnect)

### Types

| Type | Auto-release |
|------|--------------|
| `pg_advisory_lock(key)` | Session end |
| `pg_advisory_xact_lock(key)` | Transaction end |
| Try variants | Don't block, return bool |

### Example: Cron Job Dedup

```sql
-- Multiple workers cron run karte hain — sirf ek run hona chahiye

-- Worker 1
SELECT pg_try_advisory_lock(12345);  -- Returns TRUE
-- ... do work ...
SELECT pg_advisory_unlock(12345);

-- Worker 2 (concurrent)
SELECT pg_try_advisory_lock(12345);  -- Returns FALSE → skip
```

### Example: Application-Level Mutex

```sql
BEGIN;
-- Lock based on user_id
SELECT pg_advisory_xact_lock(hashtext('user_42'));

-- Critical section: read, compute, write
UPDATE accounts SET balance = balance - 100 WHERE user_id = 42;

COMMIT;  -- lock auto-released
```

### Two-Argument Form (Namespace + Key)

```sql
-- Use for grouping different lock types
SELECT pg_advisory_lock(1, user_id);   -- namespace 1 = user locks
SELECT pg_advisory_lock(2, order_id);  -- namespace 2 = order locks
```

---

## LISTEN / NOTIFY (Pub-Sub)

**Matlab:** Postgres ka built-in pub-sub. No separate message queue needed for simple cases.

### Basic Flow

```sql
-- Subscriber
LISTEN order_created;
-- (connection waits for notifications)

-- Publisher (different connection)
NOTIFY order_created, '{"order_id": 123, "user_id": 42}';
```

### From a Trigger

```sql
CREATE OR REPLACE FUNCTION notify_order_created()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('order_created', 
        json_build_object(
            'order_id', NEW.id,
            'user_id', NEW.user_id,
            'amount', NEW.amount
        )::text
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER order_created_trigger
AFTER INSERT ON orders
FOR EACH ROW EXECUTE FUNCTION notify_order_created();
```

### Node.js Subscriber (using `pg`)

```javascript
const { Client } = require('pg');
const client = new Client();
await client.connect();
await client.query('LISTEN order_created');

client.on('notification', (msg) => {
    const payload = JSON.parse(msg.payload);
    console.log('Order created:', payload);
    // Process it
});
```

### Limitations

- ❌ Persistence nahi → agar listener offline hai toh notification miss
- ❌ Payload size limit ~8KB
- ❌ Not for high-throughput (use Kafka/RabbitMQ for serious workloads)
- ✅ Good for: cache invalidation, simple in-app eventing

---

## Materialized Views

**Matlab:** Regular view ke alag — query ka result **physically store** hota hai. Heavy aggregations cache karne ke liye.

### View vs Materialized View

| | View | Materialized View |
|--|-----|-------------------|
| Storage | None (just a query) | Physical (rows stored) |
| Read speed | Slow (re-execute every time) | Fast (table scan) |
| Freshness | Always fresh | Stale (until refresh) |
| Indexing | ❌ | ✅ |

### Creating

```sql
CREATE MATERIALIZED VIEW daily_sales_summary AS
SELECT 
    DATE(created_at) AS date,
    COUNT(*) AS order_count,
    SUM(amount) AS total_revenue,
    AVG(amount) AS avg_order_value
FROM orders
GROUP BY DATE(created_at);

-- Index on the materialized view
CREATE INDEX idx_daily_sales_date ON daily_sales_summary (date);
```

### Refresh

```sql
-- Blocking refresh (locks the view)
REFRESH MATERIALIZED VIEW daily_sales_summary;

-- Concurrent refresh (no lock — but needs UNIQUE index)
REFRESH MATERIALIZED VIEW CONCURRENTLY daily_sales_summary;
```

### When to Use

✅ Heavy aggregations not needing real-time data
✅ Reports / dashboards (refresh nightly)
✅ Expensive joins for read-heavy workloads
❌ Real-time data needs (always stale!)

---

## Table Partitioning

**Matlab:** Ek logical badi table ko chhoti physical "partitions" mein todna. Performance + manageability ka jugaad.

### Why Partition?

- **Smaller indexes per partition** → faster queries
- **Drop old data fast** (just drop partition, no DELETE)
- **Parallel scans** across partitions
- **Maintenance** (vacuum) per partition

### Types

#### 1. Range Partitioning

Sabse common — date-based.

```sql
CREATE TABLE orders (
    id BIGINT,
    user_id BIGINT,
    amount NUMERIC,
    created_at TIMESTAMPTZ NOT NULL
) PARTITION BY RANGE (created_at);

-- Monthly partitions
CREATE TABLE orders_2024_01 PARTITION OF orders
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE orders_2024_02 PARTITION OF orders
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

-- Insert automatically routes to correct partition
INSERT INTO orders VALUES (1, 42, 999, '2024-01-15');
-- Goes into orders_2024_01

-- Query auto-prunes irrelevant partitions
SELECT * FROM orders WHERE created_at >= '2024-02-01';
-- Only scans orders_2024_02
```

#### 2. List Partitioning

Discrete values ke basis par.

```sql
CREATE TABLE users (
    id BIGINT,
    country TEXT NOT NULL,
    name TEXT
) PARTITION BY LIST (country);

CREATE TABLE users_in PARTITION OF users FOR VALUES IN ('IN');
CREATE TABLE users_us PARTITION OF users FOR VALUES IN ('US');
CREATE TABLE users_other PARTITION OF users DEFAULT;
```

#### 3. Hash Partitioning

Even distribution chahiye toh.

```sql
CREATE TABLE events (
    id BIGINT,
    user_id BIGINT NOT NULL,
    event_type TEXT
) PARTITION BY HASH (user_id);

CREATE TABLE events_p0 PARTITION OF events FOR VALUES WITH (MODULUS 4, REMAINDER 0);
CREATE TABLE events_p1 PARTITION OF events FOR VALUES WITH (MODULUS 4, REMAINDER 1);
CREATE TABLE events_p2 PARTITION OF events FOR VALUES WITH (MODULUS 4, REMAINDER 2);
CREATE TABLE events_p3 PARTITION OF events FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

### Maintenance — Drop Old Data Fast

```sql
-- Old way (slow)
DELETE FROM orders WHERE created_at < '2023-01-01';

-- Partitioned way (instant)
DROP TABLE orders_2023_01;
-- OR detach to keep data
ALTER TABLE orders DETACH PARTITION orders_2023_01;
```

### `pg_partman` Extension

Automated partition creation/dropping.

---

## pg_stat_statements

**Matlab:** Postgres ka query performance tracker. Kaunsi queries slow hain, kitni baar chal rahi hain — sab dikhata hai.

### Setup

```sql
-- postgresql.conf
shared_preload_libraries = 'pg_stat_statements'
-- (restart Postgres)

-- Then
CREATE EXTENSION pg_stat_statements;
```

### Usage — Top Slow Queries

```sql
SELECT 
    query,
    calls,
    total_exec_time / 1000 AS total_seconds,
    mean_exec_time AS avg_ms,
    rows,
    100 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_ratio
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

### Reset Stats

```sql
SELECT pg_stat_statements_reset();
```

### Useful Columns

| Column | Meaning |
|--------|---------|
| `calls` | Kitni baar query chali |
| `total_exec_time` | Total ms spent |
| `mean_exec_time` | Average ms per call |
| `rows` | Total rows returned |
| `shared_blks_hit` | Cache hits |
| `shared_blks_read` | Disk reads |

### Find N+1 Queries

Same query thousands of times → N+1 problem.

```sql
SELECT query, calls FROM pg_stat_statements
WHERE calls > 10000
ORDER BY calls DESC;
```

---

## VACUUM and ANALYZE

### Why VACUUM?

Postgres uses **MVCC** (Multi-Version Concurrency Control). UPDATE/DELETE actually delete nahi karte — old version "dead tuple" ban jaata hai. Yeh dead tuples disk pe occupy karte hain → table bloat.

`VACUUM` dead tuples reclaim karta hai.

### Why ANALYZE?

Query planner statistics use karta hai (column histograms, distinct values). Stale stats → bad query plans.

`ANALYZE` statistics update karta hai.

### Commands

```sql
-- Reclaim dead tuples (doesn't lock table)
VACUUM orders;

-- Vacuum + update stats
VACUUM ANALYZE orders;

-- Aggressive: rewrite table, reclaim disk space (LOCKS table!)
VACUUM FULL orders;

-- Just analyze
ANALYZE orders;
```

### Autovacuum

Postgres automatic vacuum chala raha hai background mein. Default thresholds:

```
autovacuum_vacuum_scale_factor = 0.2  (20% rows changed)
autovacuum_analyze_scale_factor = 0.1 (10% rows changed)
```

### Per-Table Tuning

```sql
-- High-traffic table → vacuum more aggressively
ALTER TABLE orders SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.025
);
```

### Monitor Bloat

```sql
SELECT schemaname, relname, n_dead_tup, n_live_tup,
       round(n_dead_tup::numeric / nullif(n_live_tup,0), 2) AS dead_ratio
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC LIMIT 10;
```

### When `VACUUM FULL`?

- Bahot zyada bloat (50%+)
- Maintenance window mein
- ⚠️ Table fully locked → app downtime
- Alternative: `pg_repack` (online rewrite)

---

## Summary Cheat Sheet

| Feature | Use For |
|---------|---------|
| **JSONB** | Flexible/sparse schema, document storage |
| **Full-Text Search** | Search engine, stemming, ranking |
| **pg_trgm** | Fuzzy match, fast `LIKE '%x%'` |
| **Advisory Locks** | App-level mutex, cron dedup |
| **LISTEN/NOTIFY** | Lightweight pub-sub, cache invalidation |
| **Materialized Views** | Cached aggregations, dashboards |
| **Partitioning** | Big tables, time-series, fast drops |
| **pg_stat_statements** | Find slow queries, optimization |
| **VACUUM/ANALYZE** | Reclaim bloat, update stats |

---

## Practice

1. Search articles by title using full-text search with ranking.
2. Create a JSONB column for product attributes; add GIN index; query products with `brand=Apple AND ram=16GB`.
3. Set up a monthly partitioned `orders` table with 6 months of partitions.
4. Build a NOTIFY-based cache invalidation: when row updates, notify app to evict cache.
5. Create a materialized view of `monthly_active_users`; schedule refresh via cron.
6. Use advisory lock to ensure only one cron worker processes a job.
