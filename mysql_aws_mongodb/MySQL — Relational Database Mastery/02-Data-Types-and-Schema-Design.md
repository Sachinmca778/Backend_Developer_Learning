# Data Types & Schema Design

## Status: Not Started

---

## Table of Contents

1. [Why Data Type Choice Matters](#why-data-type-choice-matters)
2. [Integer Types](#integer-types)
3. [DECIMAL vs FLOAT/DOUBLE](#decimal-vs-floatdouble)
4. [VARCHAR vs CHAR](#varchar-vs-char)
5. [TEXT and BLOB](#text-and-blob)
6. [DATE / DATETIME / TIMESTAMP](#date--datetime--timestamp)
7. [ENUM and SET](#enum-and-set)
8. [JSON Type](#json-type)
9. [BOOLEAN / TINYINT(1)](#boolean--tinyint1)
10. [Schema Design Principles](#schema-design-principles)
11. [Normalization (1NF / 2NF / 3NF)](#normalization-1nf--2nf--3nf)
12. [When to Denormalize](#when-to-denormalize)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Data Type Choice Matters

> "Sahi data type = **less storage + faster queries + correct semantics**. Wrong choice = silent bugs + slower queries."

### Impact

| Smaller type | Bigger type |
|--------------|-------------|
| Less RAM | More RAM |
| More rows per page (16KB) | Fewer rows per page |
| More entries fit in buffer pool | Less |
| Smaller indexes | Larger indexes |

→ **Pick smallest type that fits your data forever.**

---

## Integer Types

| Type | Bytes | Signed Range | Unsigned Range | Use |
|------|-------|--------------|----------------|-----|
| TINYINT | 1 | -128 to 127 | 0 to 255 | Small flags, age |
| SMALLINT | 2 | -32K to 32K | 0 to 65K | Counters, IDs (small set) |
| MEDIUMINT | 3 | -8M to 8M | 0 to 16M | Mid-volume IDs |
| INT | 4 | -2B to 2B | 0 to 4B | **Default for IDs** |
| BIGINT | 8 | -9E18 to 9E18 | 0 to 18E18 | Massive IDs / timestamps in micro-sec |

### `UNSIGNED`

Use for non-negative IDs:

```sql
id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY
```

→ Doubles range without extra bytes.

### `INT(11)` display width — irrelevant

```sql
INT(11)   -- (11) is just for ZEROFILL display
```

→ Storage is always 4 bytes. **Forget the parens.**

→ Deprecated in MySQL 8.0.17+.

### Real example

User table on a 50M-user app:

```sql
-- BAD: BIGINT for everything
id BIGINT, age BIGINT, country_id BIGINT;
-- 24 bytes per row

-- GOOD: right-size each column
id INT UNSIGNED, age TINYINT UNSIGNED, country_id SMALLINT UNSIGNED;
-- 7 bytes per row
```

→ **3.4× space savings**. Multiply by 50M rows + indexes — massive.

---

## DECIMAL vs FLOAT/DOUBLE

### Money — always DECIMAL

```sql
price DECIMAL(10, 2)        -- up to 99,999,999.99
balance DECIMAL(15, 4)      -- up to 99,999,999,999.9999
```

### Why not FLOAT/DOUBLE?

```
0.1 + 0.2 in DOUBLE  → 0.30000000000000004
0.1 + 0.2 in DECIMAL → 0.3 exactly
```

→ **Approximate types** — fine for science / averages, **deadly for money**.

### When FLOAT/DOUBLE OK

- Coordinates (lat/long with margin)
- Statistical / scientific values
- Performance-critical aggregates

### Storage

| Type | Bytes |
|------|-------|
| FLOAT | 4 |
| DOUBLE | 8 |
| DECIMAL(M, D) | varies — ~M/2 bytes |

---

## VARCHAR vs CHAR

| | VARCHAR(N) | CHAR(N) |
|--|-----------|---------|
| Length | Variable (1 length byte for ≤255, 2 for >255) | Fixed (always N chars) |
| Padding | None | Right-padded with spaces |
| Storage | Length + content | Always N × bytes/char |
| Use | Names, addresses, descriptions | Country codes (CHAR(2)), MD5 (CHAR(32)) |

### Example

```sql
name VARCHAR(100)        -- "Sachin" → 1 + 6 = 7 bytes (utf8mb4 chars × 1)
country_code CHAR(2)     -- "IN" → exactly 2 bytes
```

### Why CHAR for fixed?

- No fragmentation when row updated (length doesn't change)
- Slightly faster lookup
- Predictable size

### Charset gotcha — utf8mb4

```sql
VARCHAR(255) with utf8mb4   →  255 × 4 = 1020 bytes max
```

→ Index limit: pre-MySQL 5.7, max key prefix 767 bytes; 5.7+ → 3072 with `innodb_large_prefix`.

→ **Use `utf8mb4`** (full Unicode, supports emoji). `utf8` is **3-byte legacy** subset.

```sql
CREATE DATABASE myapp CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

### `VARCHAR(255)` myth

> "Just use VARCHAR(255) — same as VARCHAR(50) anyway."

**Wrong** because:
- Sort buffers / temp tables allocate **max length** for each row
- Memory waste on 100M-row sort

→ **Actual max length** matters. Profile your data.

---

## TEXT and BLOB

| Type | Max Size |
|------|----------|
| TINYTEXT / TINYBLOB | 255 |
| TEXT / BLOB | 64 KB |
| MEDIUMTEXT / MEDIUMBLOB | 16 MB |
| LONGTEXT / LONGBLOB | 4 GB |

### Storage

- Stored **off-page** (overflow page) — doesn't count toward 16KB row size
- BLOB = binary; TEXT = with charset

### Performance

| Issue | Why |
|-------|-----|
| ❌ TEXT in indexed column | Index needs prefix — only first N chars indexed |
| ❌ `SELECT *` with TEXT | Off-page fetch per row → slow |
| ❌ TEXT in `ORDER BY` | Forces filesort, can't fit in memory |
| ❌ Large BLOBs | DB not great for BLOBs — use S3 / object store |

### Best practice

- **Don't store images/files in DB** — use S3 + DB stores URL
- Use TEXT for **content text** only (article body, description)
- Index TEXT only with prefix:

```sql
CREATE INDEX idx_body ON articles(body(100));   -- first 100 chars
```

---

## DATE / DATETIME / TIMESTAMP

| Type | Range | Bytes | Auto TZ Conversion |
|------|-------|-------|-------------------|
| DATE | 1000-01-01 to 9999-12-31 | 3 | No |
| TIME | -838:59:59 to 838:59:59 | 3 | No |
| DATETIME | 1000-01-01 to 9999-12-31 | 5-8 | No (stores wall-clock) |
| TIMESTAMP | 1970-01-01 to 2038-01-19 | 4-7 | **Yes (UTC store, session TZ display)** |
| YEAR | 1901-2155 | 1 | No |

### TIMESTAMP — auto timezone

```sql
SET time_zone = 'UTC';
INSERT INTO events VALUES (NOW());      -- stores UTC

SET time_zone = 'Asia/Kolkata';
SELECT timestamp_col FROM events;        -- displays IST
```

### DATETIME — wall-clock

```sql
INSERT INTO events VALUES ('2024-05-08 14:30:00');
-- Stored exactly as given; no TZ conversion
```

### Which to use?

| Use case | Pick |
|----------|------|
| Event happened at instant in time | TIMESTAMP (or DATETIME with UTC convention) |
| Birthday, holiday | DATE |
| Future date > 2038 | **DATETIME** (TIMESTAMP cap is 2038!) |
| Audit / created_at, updated_at | TIMESTAMP with `DEFAULT CURRENT_TIMESTAMP` |

### 2038 problem

```
TIMESTAMP max = 2038-01-19 03:14:07 UTC
```

→ Birthdates of children born today → use DATE / DATETIME, not TIMESTAMP.

### Auto-update pattern

```sql
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

### Microseconds (5.6.4+)

```sql
DATETIME(6)   -- microsecond precision
TIMESTAMP(3)  -- millisecond
```

---

## ENUM and SET

### ENUM

```sql
status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED')
```

### Pros

- 1-2 bytes storage (vs VARCHAR)
- Self-documenting
- Cannot insert invalid values

### Cons

- **Adding/removing values requires `ALTER TABLE`** (locks)
- Not portable across DBs
- Sorted by **declaration order**, not alphabetical
- Position-based — easy to mix up if redeclared

### Modern alternative

Use a small **lookup table** with FK:

```sql
CREATE TABLE order_status (
    id TINYINT PK,
    name VARCHAR(20)
);

CREATE TABLE orders (
    status_id TINYINT,
    FOREIGN KEY (status_id) REFERENCES order_status(id)
);
```

→ Adding new status = `INSERT` (no ALTER).

### SET (rare)

```sql
permissions SET('READ', 'WRITE', 'DELETE')   -- bitmask
```

→ Almost never used in modern schema. Use junction table.

---

## JSON Type

> "MySQL 5.7+ native JSON — validates structure, supports indexed access."

```sql
CREATE TABLE products (
    id INT PRIMARY KEY,
    attributes JSON
);

INSERT INTO products VALUES (1, '{"color": "red", "size": "M"}');

SELECT attributes->>'$.color' FROM products;     -- 'red'
```

### Functions

```sql
JSON_EXTRACT(col, '$.path')      -- or shorthand: col->'$.path'
JSON_UNQUOTE(JSON_EXTRACT(...))  -- or: col->>'$.path'
JSON_SET(col, '$.field', value)  -- in-place update
JSON_CONTAINS(col, value, '$.path')
JSON_ARRAY('a', 'b')
JSON_OBJECT('k1', 'v1')
```

### Indexing

Functional index on JSON path (MySQL 8+):

```sql
CREATE INDEX idx_color ON products( ((attributes->>'$.color')) );
```

### When JSON?

- Truly schema-less data (varying attributes per row)
- Sparse fields (storing 50+ optional cols)
- Configs / metadata

### When NOT?

- All rows have same structure → use **columns** (faster, simpler)
- Heavy queries on JSON paths → use proper schema

→ "Don't use JSON to avoid migrations." Migrations are part of life.

---

## BOOLEAN / TINYINT(1)

```sql
is_active BOOLEAN          -- alias for TINYINT(1)
is_active TINYINT(1)        -- same thing
```

→ `TRUE = 1`, `FALSE = 0`. Stored as 1 byte.

### Watch out

`TINYINT(1)` accepts any value from -128 to 127 — **not just 0/1**:

```sql
INSERT INTO users (is_active) VALUES (5);    -- ✅ no error!
SELECT is_active FROM users;                  -- 5
```

→ App logic should validate. Or use ENUM('0', '1') / lookup.

---

## Schema Design Principles

### 1. Right type for the job

Already covered above.

### 2. NOT NULL by default

```sql
name VARCHAR(100) NOT NULL
```

- NULL has special semantics (`NULL != NULL`, fails simple equality)
- Slower in indexes
- Confuses application logic

→ NULL only when "absence of value" is meaningful (e.g., `deleted_at` for soft delete).

### 3. Primary key always

- Use **AUTO_INCREMENT BIGINT UNSIGNED** for surrogate keys
- Avoid composite natural keys as PK (use unique constraint instead + surrogate PK)

### 4. Foreign keys for referential integrity

```sql
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
```

→ Catches orphan rows. Slight write overhead.

### 5. Indexes on FK + frequently filtered columns

InnoDB doesn't auto-index FK on the **referencing** side! Add manually.

### 6. Timestamps

```sql
created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
```

→ Auditing essentials.

### 7. Soft delete (sometimes)

```sql
deleted_at TIMESTAMP NULL DEFAULT NULL
```

→ Index `WHERE deleted_at IS NULL` for active rows.

---

## Normalization (1NF / 2NF / 3NF)

### 1NF — Atomic values

❌
```
| user | phones      |
|------|-------------|
| 1    | 9999, 8888  |
```

✅
```
| user_id | phone |
|---------|-------|
| 1       | 9999  |
| 1       | 8888  |
```

### 2NF — No partial dependency on composite PK

❌ Composite PK (order_id, product_id) but `product_name` depends only on `product_id`.

✅ Move `product_name` to `products` table.

### 3NF — No transitive dependency

❌ `users(id, city, country)` — `country` depends on `city`, not directly on `id`.

✅ Move `country` to `cities(city, country)` table.

### BCNF, 4NF, 5NF — rarely needed beyond 3NF in practice.

→ "**Normalize until it hurts; denormalize until it works**."

---

## When to Denormalize

> "Performance trumps purity in **OLAP / read-heavy** scenarios."

### Examples

#### 1. Computed columns (denormalized aggregate)

```sql
-- orders table has total_amount instead of computing from order_items
total_amount DECIMAL(10,2)
```

→ Recompute via trigger / application on `order_items` change.

#### 2. Cached lookups

```sql
-- Stay user_count in users_summary updated daily
CREATE TABLE users_summary (
    date DATE PK,
    total_users INT
);
```

#### 3. Materialized views (manual)

Pre-aggregated tables refreshed periodically.

#### 4. Read-only replica with denormalized schema

Primary normalized; read replica gets denormalized projection via CDC / events.

→ Cross-ref: CQRS in `phase-5 / System Design / 05-Microservices-Design.md`.

### When?

- 10:1+ read:write ratio
- JOINs causing unacceptable latency
- Reports / dashboards as primary use case

### Cost

- Update propagation logic
- Stale data risk
- Storage cost
- More complex schema migration

---

## Pitfalls

1. **VARCHAR(255) for everything** — wastes sort buffer.
2. **utf8 instead of utf8mb4** — emoji + many languages broken.
3. **FLOAT/DOUBLE for money** — silent rounding errors.
4. **TIMESTAMP for future dates** — 2038 cap.
5. **No NOT NULL** — silent NULL behaviour confusion.
6. **No PK** — replication breaks, slow updates.
7. **No FK indexes** — MySQL doesn't auto-index FK referencing side.
8. **ENUM for changing values** — `ALTER TABLE` lock pain.
9. **TEXT in WHERE / ORDER BY** without prefix index — full scan.
10. **JSON for fixed-schema data** — slower than columns.
11. **Composite natural PK** — index size + query complexity.
12. **No created_at / updated_at** — auditing impossible.
13. **`INT(11)` display width** confusion — has no effect on storage.
14. **Storing files in BLOB** — DB grows huge; use object storage.

---

## Cheat Sheet

| Use case | Type |
|----------|------|
| Boolean | TINYINT(1) / BOOLEAN |
| Money | DECIMAL(M, D) |
| ID | INT/BIGINT UNSIGNED |
| Short fixed | CHAR(N) |
| Variable text | VARCHAR(N) |
| Long text | TEXT (off-page) |
| Date only | DATE |
| Wall-clock instant | DATETIME |
| UTC instant + TZ | TIMESTAMP (pre-2038) |
| Enum | Lookup table > ENUM |
| Schema-less | JSON |

| Charset | Use |
|---------|-----|
| utf8mb4 | Default modern |
| utf8 (legacy 3-byte) | Don't |
| latin1 | Legacy only |

| Default rules | |
|---------------|---|
| NOT NULL | Default |
| Surrogate PK (BIGINT UNSIGNED AUTO_INC) | Always |
| created_at / updated_at | Always |
| FK index on child side | Always (manual) |

---

## Practice

1. Audit a table — column types right-sized? Compute storage savings.
2. Migration: shrink BIGINT to INT for an ID column you know < 2B.
3. utf8 → utf8mb4 migration (charset conversion script).
4. ENUM column → lookup table refactor.
5. Add `created_at / updated_at` to existing tables.
6. JSON column for storing optional product attributes; query with `->`.
