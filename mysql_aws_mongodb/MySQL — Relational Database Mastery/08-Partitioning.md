# Partitioning

## Status: Not Started

---

## Table of Contents

1. [Partitioning vs Sharding](#partitioning-vs-sharding)
2. [Why Partition?](#why-partition)
3. [Partition Types](#partition-types)
4. [RANGE Partitioning](#range-partitioning)
5. [LIST Partitioning](#list-partitioning)
6. [HASH Partitioning](#hash-partitioning)
7. [KEY Partitioning](#key-partitioning)
8. [Partition Pruning](#partition-pruning)
9. [Subpartitioning](#subpartitioning)
10. [Managing Partitions](#managing-partitions)
11. [Limitations](#limitations)
12. [When NOT to Partition](#when-not-to-partition)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Partitioning vs Sharding

> "Both = horizontal split. **Partitioning = single server**. **Sharding = multiple servers.**"

| | Partitioning | Sharding |
|--|--------------|---------|
| Where | One MySQL server | Multiple MySQL servers |
| App-aware | No (transparent) | Yes (routes to shard) |
| Storage limit | Single server disk | N × server disk |
| HA | Single point | Each shard separately |

→ Partitioning is **MySQL feature**; sharding is **app-level architecture**.

---

## Why Partition?

| Benefit | How |
|---------|-----|
| **Query speed** (selective) | Optimizer skips partitions (pruning) |
| **Maintenance** | DROP PARTITION — instant archive |
| **Index size** | Each partition has its own index → smaller per-partition index |
| **Concurrency** | Partition-level locking (some operations) |
| **Time-series workload** | Drop old data fast |

### Common use case

Time-series data (logs, events):

```sql
CREATE TABLE events (
    id BIGINT NOT NULL,
    user_id INT,
    event_type VARCHAR(50),
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id, created_at)
)
PARTITION BY RANGE (TO_DAYS(created_at)) (
    PARTITION p2024_01 VALUES LESS THAN (TO_DAYS('2024-02-01')),
    PARTITION p2024_02 VALUES LESS THAN (TO_DAYS('2024-03-01')),
    PARTITION p2024_03 VALUES LESS THAN (TO_DAYS('2024-04-01')),
    PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

→ Drop January's partition (after retention) = instant.

---

## Partition Types

| Type | Partitions defined by |
|------|----------------------|
| **RANGE** | Value ranges of a column (numeric or date) |
| **LIST** | Discrete value list (enum-like) |
| **HASH** | Hash function on integer column |
| **KEY** | MySQL's internal hash on any column type |
| **COLUMNS** | RANGE/LIST on multiple cols / non-int |

→ Most common: **RANGE** (time-series), **HASH** (uniform distribution).

---

## RANGE Partitioning

```sql
CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT,
    total DECIMAL(10, 2),
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id, created_at)            -- partition col in PK
)
PARTITION BY RANGE (YEAR(created_at)) (
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION pfuture VALUES LESS THAN MAXVALUE
);
```

### Use case

- Time-based archive
- Drop old data with `DROP PARTITION` (instant)

### Add partition for new range

```sql
ALTER TABLE orders ADD PARTITION (PARTITION p2025 VALUES LESS THAN (2026));
```

⚠️ Cannot add new partition if `MAXVALUE` partition exists. Reorganize:

```sql
ALTER TABLE orders REORGANIZE PARTITION pfuture INTO (
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION pfuture VALUES LESS THAN MAXVALUE
);
```

### Drop old data

```sql
ALTER TABLE orders DROP PARTITION p2022;
```

→ **Instant** — no row-by-row delete. **Massive perf win** vs `DELETE`.

---

## LIST Partitioning

```sql
CREATE TABLE users (
    id INT NOT NULL,
    country_code CHAR(2) NOT NULL,
    name VARCHAR(100),
    PRIMARY KEY (id, country_code)
)
PARTITION BY LIST COLUMNS (country_code) (
    PARTITION p_in VALUES IN ('IN'),
    PARTITION p_us VALUES IN ('US', 'CA'),
    PARTITION p_eu VALUES IN ('GB', 'DE', 'FR', 'IT', 'ES'),
    PARTITION p_other VALUES IN (DEFAULT)        -- catch-all (8.0+)
);
```

### Use case

- Geo-segmented data
- Tenant separation
- Status-based archival

### Note

- Every row's value must match a partition (or be in `DEFAULT`)
- Old `LIST` (without `COLUMNS`) = integers only

---

## HASH Partitioning

```sql
CREATE TABLE sessions (
    id BIGINT NOT NULL,
    user_id INT NOT NULL,
    data TEXT,
    PRIMARY KEY (id, user_id)
)
PARTITION BY HASH(user_id)
PARTITIONS 8;
```

### How

```
partition_index = MOD(user_id, 8)
```

### Use case

- Even distribution across partitions
- Avoid hotspot of any single partition
- No natural range / list

### Cons

- No partition pruning for range queries on partition column
- Adding partitions = rehash (expensive)

---

## KEY Partitioning

> "Like HASH but uses **MySQL's internal hash function**, supports non-integer columns."

```sql
CREATE TABLE products (
    id INT NOT NULL,
    sku VARCHAR(50) NOT NULL,
    PRIMARY KEY (id, sku)
)
PARTITION BY KEY (sku)
PARTITIONS 16;
```

→ Useful when partition column is string / date.

→ HASH only works on integer expressions; KEY works on any.

---

## Partition Pruning

> "Optimizer detects which partitions can satisfy query → skips others."

### Example

```sql
EXPLAIN PARTITIONS
SELECT * FROM orders WHERE created_at = '2024-05-08';

-- partitions: p2024     (only 1 partition scanned!)
```

### Without pruning (e.g., function on column)

```sql
EXPLAIN PARTITIONS
SELECT * FROM orders WHERE YEAR(created_at) = 2024;

-- May not prune; depends on optimizer
```

→ Use **direct comparison** on partition column.

### Verify pruning

```sql
EXPLAIN PARTITIONS SELECT ...;
```

→ `partitions` column shows scanned partitions.

---

## Subpartitioning

> "Partition within partition — RANGE/LIST → HASH/KEY."

```sql
CREATE TABLE orders (
    id BIGINT NOT NULL,
    user_id INT,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (id, user_id, created_at)
)
PARTITION BY RANGE (YEAR(created_at))
SUBPARTITION BY HASH(user_id)
SUBPARTITIONS 4 (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025)
);
```

→ Each year split into 4 hash buckets by user_id.

→ Useful for:
- Time-series + load distribution
- Massive scale (TB+)

→ **Operationally complex** — use only if necessary.

---

## Managing Partitions

### Show partitions

```sql
SELECT
    PARTITION_NAME,
    PARTITION_ORDINAL_POSITION,
    PARTITION_METHOD,
    PARTITION_EXPRESSION,
    TABLE_ROWS,
    DATA_LENGTH
FROM information_schema.PARTITIONS
WHERE TABLE_NAME = 'orders';
```

### Add partition

```sql
ALTER TABLE orders ADD PARTITION (
    PARTITION p2025 VALUES LESS THAN (2026)
);
```

### Drop partition

```sql
ALTER TABLE orders DROP PARTITION p2022;
```

→ Removes data + partition (instant).

### Truncate partition

```sql
ALTER TABLE orders TRUNCATE PARTITION p2022;
```

→ Empty partition (keeps it).

### Reorganize

```sql
ALTER TABLE orders REORGANIZE PARTITION pfuture INTO (
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION pfuture VALUES LESS THAN MAXVALUE
);
```

### Exchange partition with table

```sql
ALTER TABLE orders EXCHANGE PARTITION p2022 WITH TABLE orders_archive_2022;
```

→ Swap data; useful for archive workflow.

### Optimize partition

```sql
ALTER TABLE orders OPTIMIZE PARTITION p2024;
```

→ Reclaim space, defrag.

---

## Limitations

### 1. Foreign keys not supported

```sql
-- This won't work
ALTER TABLE orders ADD FOREIGN KEY (user_id) REFERENCES users(id);
ERROR 1506 (HY000): Foreign keys are not yet supported in conjunction with partitioning
```

→ Enforce at app level if partitioned.

### 2. All unique indexes (incl. PK) must include partition key

```sql
-- partition by created_at → PK MUST include created_at
PRIMARY KEY (id, created_at)
UNIQUE KEY (sku, created_at)
```

→ Major design constraint. Plan upfront.

### 3. Max 8192 partitions per table (incl. subpartitions)

→ Plenty for most use cases.

### 4. Some functions not allowed in partition expression

Allowed: `YEAR()`, `TO_DAYS()`, `MONTH()`, `MOD()`, etc.
Not: arbitrary user functions, most string functions.

### 5. ALTER TABLE often rebuilds whole table

For partition-incompatible changes; can be **slow on huge tables**.

→ Use `pt-online-schema-change` or careful planning.

### 6. Per-partition row count not maintained for `COUNT(*)`

```sql
SELECT COUNT(*) FROM orders;
-- Still scans (or uses index) — partition pruning helps if WHERE present
```

---

## When NOT to Partition

- Small tables (< 1M rows)
- Queries don't have partition column in WHERE → no pruning
- FK relationships needed
- Most queries hit all partitions anyway
- Complexity not justified

→ "**Premature partitioning** is worse than no partitioning."

---

## Pitfalls

1. **Partition column not in PK / unique** — schema rejected.
2. **Function on partition column in WHERE** — pruning fails.
3. **MAXVALUE partition** prevents adding new range — REORGANIZE needed.
4. **Forgetting to add new partitions** for upcoming periods — INSERT fails (no partition fits).
5. **Foreign keys mistakenly added** — error.
6. **Partition column wrong choice** — uneven distribution (one giant partition).
7. **`DELETE FROM ... WHERE created_at < '...'`** instead of DROP PARTITION — slow.
8. **Mismatch between partition unit and query unit** — query touches many partitions.
9. **Partition + replication binlog** — STATEMENT format issues; use ROW.
10. **Subpartitioning everything** — operational nightmare.

---

## Cheat Sheet

| Type | Best for |
|------|----------|
| RANGE | Time-series, IDs |
| LIST | Geo, status, tenant |
| HASH | Uniform distribution (integer) |
| KEY | Uniform distribution (any type) |

| Operation | Performance |
|-----------|-------------|
| DROP PARTITION | Instant |
| TRUNCATE PARTITION | Instant |
| EXCHANGE PARTITION | Fast |
| ADD PARTITION | Fast (RANGE/LIST) |
| REORGANIZE PARTITION | Slow (rebuild) |
| OPTIMIZE PARTITION | Slow per partition |

| Limitation | |
|-----------|--|
| FK | Not allowed |
| Unique index | Must include partition col |
| Max partitions | 8192 |

| When YES | When NO |
|----------|---------|
| Time-series data | Small table |
| Drop old data fast | FK needed |
| Selective queries | Queries hit all partitions |
| TB+ scale | <1M rows |

---

## Practice

1. Create RANGE-partitioned `events` table by month; populate; observe pruning in EXPLAIN PARTITIONS.
2. DROP an old partition; verify instant + space reclaimed.
3. HASH-partition `sessions(user_id)` into 8; observe row distribution.
4. Try adding FK on partitioned table; observe error.
5. Use EXCHANGE PARTITION to archive month's data into separate table.
6. Compare DELETE vs DROP PARTITION timing on 100M rows.
