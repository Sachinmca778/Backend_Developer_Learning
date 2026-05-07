# Data Modeling

## Status: Not Started

---

## Table of Contents

1. [Data Modeling Kya Hai?](#data-modeling-kya-hai)
2. [Entity-Relationship (ER) Modeling](#entity-relationship-er-modeling)
3. [Normalization (1NF → BCNF)](#normalization-1nf--bcnf)
4. [Denormalization Trade-offs](#denormalization-trade-offs)
5. [Polymorphic Associations](#polymorphic-associations)
6. [Time-Series Data Modeling](#time-series-data-modeling)
7. [Audit Tables](#audit-tables)
8. [Common Patterns & Pitfalls](#common-patterns--pitfalls)

---

## Data Modeling Kya Hai?

**Matlab:** Real-world entities + relationships ko database tables/columns mein represent karna. Acche modeling se queries simple, performance acchi, data consistent.

### Three Levels

| Level | Example |
|-------|---------|
| **Conceptual** | "User has many Orders" |
| **Logical** | Tables: `users`, `orders` with relationships |
| **Physical** | Postgres tables with types, indexes, constraints |

---

## Entity-Relationship (ER) Modeling

### Building Blocks

#### 1. Entity

**Matlab:** Real-world object — User, Order, Product. Banta hai → table.

#### 2. Attribute

**Matlab:** Entity ki property — name, email, price. Banta hai → column.

#### 3. Relationship

**Matlab:** Entities ke beech connection.

### Relationship Cardinalities

#### One-to-One (1:1)

**Example:** User ↔ UserProfile (har user ka ek profile)

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email TEXT UNIQUE
);

CREATE TABLE user_profiles (
    user_id INT PRIMARY KEY REFERENCES users(id),  -- FK + UNIQUE
    bio TEXT,
    avatar_url TEXT
);
```

**When to use 1:1?**
- Hot vs cold columns (frequently vs rarely accessed)
- Optional data
- Security (sensitive data in separate table)

#### One-to-Many (1:N)

**Example:** User → Orders (one user ke kayi orders)

```sql
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id),
    amount NUMERIC,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
```

**FK lives on the "many" side.**

#### Many-to-Many (M:N)

**Example:** Students ↔ Courses (student kayi courses, course mein kayi students)

```sql
CREATE TABLE students (id SERIAL PRIMARY KEY, name TEXT);
CREATE TABLE courses (id SERIAL PRIMARY KEY, title TEXT);

-- Junction / association table
CREATE TABLE student_courses (
    student_id INT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    course_id INT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    enrolled_at TIMESTAMPTZ DEFAULT NOW(),
    grade TEXT,
    PRIMARY KEY (student_id, course_id)
);
```

✅ Composite primary key prevents duplicate enrollments.

### ER Diagram (Mental Model)

```
┌──────────┐        ┌──────────┐
│  USER    │ 1    N │  ORDER   │
│  id PK   │────────│ id PK    │
│  email   │        │ user_id FK│
└──────────┘        │ amount   │
                    └──────────┘
                          │ 1
                          │
                          │ N
                    ┌──────────┐
                    │ ORDER_ITEM│
                    │ id PK    │
                    │ order_id │
                    │ product_id│
                    └──────────┘
```

---

## Normalization (1NF → BCNF)

**Matlab:** Data redundancy reduce karne aur anomalies prevent karne ke rules.

### Why Normalize?

- **Update anomaly:** ek info kayi jagah → update inconsistent
- **Insert anomaly:** ek entity ka data dusre par dependent
- **Delete anomaly:** ek row delete → unintended info loss

### 1NF (First Normal Form)

**Rule:** Atomic values — no arrays/multi-value in single cell.

#### ❌ Not 1NF

```
| user_id | name  | phones                 |
|---------|-------|------------------------|
| 1       | Rahul | "9999, 8888"           |  ← multi-value!
| 2       | Priya | "7777, 6666, 5555"     |
```

#### ✅ 1NF

```
users:
| id | name  |
|----|-------|
| 1  | Rahul |
| 2  | Priya |

phones:
| id | user_id | phone |
|----|---------|-------|
| 1  | 1       | 9999  |
| 2  | 1       | 8888  |
| 3  | 2       | 7777  |
```

(In Postgres you could use a JSONB array, but for relational purity, separate table.)

### 2NF (Second Normal Form)

**Rule:** 1NF + every non-key column depends on **whole** primary key (not partial — for composite keys).

#### ❌ Not 2NF

```
order_items (composite PK: order_id + product_id)
| order_id | product_id | quantity | product_name |
|----------|------------|----------|--------------|
| 1        | 100        | 2        | Apple        |
| 1        | 101        | 1        | Banana       |
| 2        | 100        | 5        | Apple        |  ← duplicated!
```

`product_name` depends only on `product_id`, not on full PK.

#### ✅ 2NF

```
order_items:                   products:
| order_id | product_id | qty |  | id  | name   |
|----------|------------|-----|  |-----|--------|
| 1        | 100        | 2   |  | 100 | Apple  |
| 1        | 101        | 1   |  | 101 | Banana |
| 2        | 100        | 5   |
```

### 3NF (Third Normal Form)

**Rule:** 2NF + no transitive dependencies — non-key column depends only on PK, not on another non-key column.

#### ❌ Not 3NF

```
employees:
| id | name  | dept_id | dept_name | dept_location |
|----|-------|---------|-----------|---------------|
| 1  | Rahul | 10      | Tech      | Bangalore     |
| 2  | Priya | 10      | Tech      | Bangalore     |  ← repeated
```

`dept_name`, `dept_location` depend on `dept_id`, not employee `id`.

#### ✅ 3NF

```
employees:                     departments:
| id | name  | dept_id |        | id | name | location  |
|----|-------|---------|        |----|------|-----------|
| 1  | Rahul | 10      |        | 10 | Tech | Bangalore |
| 2  | Priya | 10      |
```

### BCNF (Boyce-Codd Normal Form)

**Rule:** 3NF + every functional dependency's left side is a super key.

Stricter version of 3NF. Most 3NF tables are also BCNF in practice.

#### Edge Case Example

```
| course | instructor | room |
```

If "instructor → course" (each instructor teaches one course), but `instructor` not a key → BCNF violated.

### Practical Advice

In real life:
- **Aim for 3NF** for most schemas
- **BCNF for high-integrity** systems
- **Denormalize selectively** for performance

---

## Denormalization Trade-offs

**Matlab:** Intentionally redundancy add karna — performance ke liye normalization rules tod dena.

### When to Denormalize?

✅ Read-heavy workload (write rare)
✅ Joins becoming a bottleneck
✅ Reporting / analytics tables
✅ Need to avoid expensive aggregations every read

### Examples

#### 1. Counter Caches

```sql
-- ❌ Normalized (expensive on every read)
SELECT u.*, COUNT(o.id) AS order_count
FROM users u LEFT JOIN orders o ON o.user_id = u.id
WHERE u.id = 1
GROUP BY u.id;

-- ✅ Denormalized counter
ALTER TABLE users ADD COLUMN order_count INT DEFAULT 0;

-- Maintain via trigger
CREATE TRIGGER increment_order_count
AFTER INSERT ON orders
FOR EACH ROW
EXECUTE FUNCTION update_order_count();
```

#### 2. Pre-Joined Materialized View

```sql
CREATE MATERIALIZED VIEW user_order_summary AS
SELECT u.id, u.name, COUNT(o.id) AS order_count, SUM(o.amount) AS total_spent
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
GROUP BY u.id, u.name;

-- Refresh nightly
REFRESH MATERIALIZED VIEW CONCURRENTLY user_order_summary;
```

#### 3. Embedded Snapshots

```sql
-- Order has 'shipping_address' as JSONB snapshot
-- (don't break order history if user changes address)
CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    user_id INT,
    shipping_address JSONB,  -- snapshot at order time
    ...
);
```

### Trade-off Table

| Pro | Con |
|-----|-----|
| Faster reads | Slower writes |
| Simpler queries | Risk of inconsistency |
| Less join work | More storage |
|  | Maintenance complexity |

### Rules of Thumb

1. **Normalize first**, denormalize as needed
2. **Document** every denormalization (why, who maintains)
3. **Use triggers / events** to keep redundant data in sync
4. **Read replicas + materialized views** before duplicating columns

---

## Polymorphic Associations

**Matlab:** Ek FK jo multiple tables mein se kisi bhi ek ko refer kar sakti hai. E.g., `Comment` jo `Post`, `Photo`, ya `Video` par ho sakta hai.

### Approach 1: Single Table Inheritance (STI)

**Matlab:** Sab subtypes ek hi table mein, with `type` column + nullable subtype-specific columns.

```sql
CREATE TABLE animals (
    id SERIAL PRIMARY KEY,
    type TEXT NOT NULL,            -- 'Dog', 'Cat', 'Bird'
    name TEXT,
    
    -- Dog-specific
    breed TEXT,
    
    -- Bird-specific
    can_fly BOOLEAN,
    
    -- Cat-specific
    is_indoor BOOLEAN
);
```

**Pros:**
- ✅ Simple, one table to query
- ✅ Easy polymorphism

**Cons:**
- ❌ Many NULLable columns (sparse)
- ❌ No DB-level constraint per subtype
- ❌ Doesn't scale beyond a few subtypes

### Approach 2: Class Table Inheritance (CTI)

**Matlab:** Common columns parent table mein, subtype-specific columns child tables mein.

```sql
CREATE TABLE animals (
    id SERIAL PRIMARY KEY,
    name TEXT,
    type TEXT NOT NULL
);

CREATE TABLE dogs (
    animal_id INT PRIMARY KEY REFERENCES animals(id) ON DELETE CASCADE,
    breed TEXT
);

CREATE TABLE birds (
    animal_id INT PRIMARY KEY REFERENCES animals(id) ON DELETE CASCADE,
    can_fly BOOLEAN
);
```

**Pros:**
- ✅ No NULLs — clean schema
- ✅ Subtype-specific constraints
- ✅ Scales to many subtypes

**Cons:**
- ❌ Joins required for full data
- ❌ More complex queries

### Approach 3: Polymorphic FK (Anti-pattern but common)

**Matlab:** `commentable_id` + `commentable_type` columns — FK without DB-level integrity.

```sql
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    body TEXT,
    commentable_id INT NOT NULL,
    commentable_type TEXT NOT NULL    -- 'Post', 'Photo', 'Video'
);
```

**Pros:**
- ✅ Flexible — any model can be commentable

**Cons:**
- ❌ **No FK integrity** — DB can't enforce referential integrity
- ❌ Joins are awkward
- ❌ Can't index FK across tables

### Approach 4: Multiple Nullable FKs (Cleaner)

```sql
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    body TEXT,
    post_id INT REFERENCES posts(id) ON DELETE CASCADE,
    photo_id INT REFERENCES photos(id) ON DELETE CASCADE,
    video_id INT REFERENCES videos(id) ON DELETE CASCADE,
    
    -- Constraint: exactly one must be set
    CHECK (
        (post_id IS NOT NULL)::int +
        (photo_id IS NOT NULL)::int +
        (video_id IS NOT NULL)::int = 1
    )
);
```

**Pros:**
- ✅ DB-level FK integrity
- ✅ Easy joins per type

**Cons:**
- ❌ Schema change for new commentable types
- ❌ Extra columns

### Recommendation

| Few subtypes (3-5), need FK integrity | Multiple Nullable FKs |
| Many subtypes, complex inheritance | CTI |
| Simple, fast prototyping | STI |
| Rails-style, dynamic types | Polymorphic FK (with care) |

---

## Time-Series Data Modeling

**Matlab:** Time-stamped data — metrics, events, logs, sensor data. Special considerations for scale.

### Characteristics

- High write volume
- Append-only (mostly)
- Time-range queries dominant
- Old data archived/dropped

### Schema Patterns

#### 1. Wide Table (Simple)

```sql
CREATE TABLE metrics (
    time TIMESTAMPTZ NOT NULL,
    sensor_id INT NOT NULL,
    temperature NUMERIC,
    humidity NUMERIC,
    pressure NUMERIC,
    PRIMARY KEY (time, sensor_id)
);

CREATE INDEX idx_metrics_sensor_time ON metrics(sensor_id, time DESC);
```

#### 2. Narrow Table (Flexible)

```sql
CREATE TABLE measurements (
    time TIMESTAMPTZ NOT NULL,
    sensor_id INT NOT NULL,
    metric_name TEXT NOT NULL,    -- 'temperature', 'humidity'
    value NUMERIC NOT NULL,
    PRIMARY KEY (time, sensor_id, metric_name)
);
```

✅ Add new metric types without schema change.
❌ More rows, harder to query multiple metrics together.

### Partitioning by Time

**Critical for scale.**

```sql
CREATE TABLE events (
    time TIMESTAMPTZ NOT NULL,
    event_type TEXT,
    user_id INT,
    payload JSONB
) PARTITION BY RANGE (time);

CREATE TABLE events_2024_01 PARTITION OF events
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
CREATE TABLE events_2024_02 PARTITION OF events
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
-- ... per month
```

**Benefits:**
- Drop old partitions instantly (`DROP TABLE events_2023_01;`)
- Smaller indexes per partition
- Parallel scans

### Aggregation / Rollups

Raw data + aggregated tables for queries.

```sql
-- Raw second-level data (high volume)
CREATE TABLE metrics_raw (...);

-- Hourly rollup (cheaper queries)
CREATE TABLE metrics_hourly (
    hour TIMESTAMPTZ,
    sensor_id INT,
    avg_temp NUMERIC,
    min_temp NUMERIC,
    max_temp NUMERIC,
    sample_count INT,
    PRIMARY KEY (hour, sensor_id)
);

-- Daily rollup
CREATE TABLE metrics_daily (...);
```

Use cron / scheduled jobs to populate rollups.

### Specialized Time-Series DBs

For massive scale:
- **TimescaleDB** (Postgres extension) — auto-partitioning, hypertables
- **InfluxDB**, **Prometheus**, **ClickHouse**

### Retention

```sql
-- Drop old partitions
DROP TABLE events_2023_01;

-- Or detach (keep data but separate)
ALTER TABLE events DETACH PARTITION events_2023_01;
```

---

## Audit Tables

**Matlab:** Track karna ki **kaun**, **kab**, **kya** badla — compliance, debugging, history.

### Standard Audit Columns

```sql
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email TEXT,
    name TEXT,
    
    -- Audit columns
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    deleted_at TIMESTAMPTZ,                          -- soft delete
    created_by INT REFERENCES users(id),
    updated_by INT REFERENCES users(id)
);
```

### Auto-Update `updated_at` Trigger

```sql
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION update_updated_at();
```

### Soft Delete Pattern

```sql
-- Don't actually delete
UPDATE users SET deleted_at = NOW() WHERE id = 1;

-- Filter in queries
SELECT * FROM users WHERE deleted_at IS NULL;

-- Partial index for active users
CREATE INDEX idx_users_active_email ON users(email)
WHERE deleted_at IS NULL;
```

**Trade-off:** Soft delete preserves history but every query needs filter. Some teams use **partial unique indexes** for emails:

```sql
-- Allow email reuse after soft delete
CREATE UNIQUE INDEX uniq_users_email_active 
ON users(email) WHERE deleted_at IS NULL;
```

### Full Audit Log Table

```sql
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    table_name TEXT NOT NULL,
    row_id INT NOT NULL,
    action TEXT NOT NULL,         -- 'INSERT', 'UPDATE', 'DELETE'
    old_data JSONB,
    new_data JSONB,
    changed_by INT,
    changed_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_table_row ON audit_log(table_name, row_id);
CREATE INDEX idx_audit_changed_at ON audit_log(changed_at DESC);
```

### Trigger to Populate

```sql
CREATE OR REPLACE FUNCTION audit_trigger()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        INSERT INTO audit_log(table_name, row_id, action, old_data)
        VALUES (TG_TABLE_NAME, OLD.id, 'DELETE', to_jsonb(OLD));
        RETURN OLD;
    ELSIF TG_OP = 'UPDATE' THEN
        INSERT INTO audit_log(table_name, row_id, action, old_data, new_data)
        VALUES (TG_TABLE_NAME, NEW.id, 'UPDATE', to_jsonb(OLD), to_jsonb(NEW));
        RETURN NEW;
    ELSIF TG_OP = 'INSERT' THEN
        INSERT INTO audit_log(table_name, row_id, action, new_data)
        VALUES (TG_TABLE_NAME, NEW.id, 'INSERT', to_jsonb(NEW));
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_users
AFTER INSERT OR UPDATE OR DELETE ON users
FOR EACH ROW EXECUTE FUNCTION audit_trigger();
```

### Temporal Tables (History via Range)

```sql
CREATE TABLE products_history (
    id INT,
    name TEXT,
    price NUMERIC,
    valid_period TSRANGE NOT NULL,    -- when this version was active
    EXCLUDE USING GIST (id WITH =, valid_period WITH &&)
);

-- Query: price of product at specific time
SELECT * FROM products_history
WHERE id = 1 AND valid_period @> '2024-06-15'::timestamp;
```

---

## Common Patterns & Pitfalls

### ✅ Patterns

#### 1. Surrogate Keys Over Natural Keys

```sql
-- ✅ Surrogate (artificial)
CREATE TABLE users (id SERIAL PRIMARY KEY, email TEXT UNIQUE);

-- ❌ Natural (email as PK) — what if user changes email?
CREATE TABLE users (email TEXT PRIMARY KEY);
```

#### 2. UUID for Distributed Systems

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ...
);
```

✅ No central ID generator needed
❌ Larger (16 bytes vs 8 for BIGINT)

#### 3. ENUM-like Lookup Tables

```sql
-- ❌ Hardcoded ENUM (rigid)
CREATE TYPE order_status AS ENUM ('pending', 'paid', 'shipped');

-- ✅ Lookup table (flexible)
CREATE TABLE order_statuses (
    code TEXT PRIMARY KEY,
    label TEXT NOT NULL,
    sort_order INT
);
CREATE TABLE orders (
    ...
    status TEXT REFERENCES order_statuses(code)
);
```

#### 4. Optimistic Locking with Version

```sql
CREATE TABLE products (
    id SERIAL PRIMARY KEY,
    name TEXT,
    version INT DEFAULT 1
);

-- Update only if version matches
UPDATE products SET name = 'New', version = version + 1
WHERE id = 1 AND version = $expected_version;
-- Affected rows = 0 → conflict
```

### ❌ Anti-Patterns

#### 1. EAV (Entity-Attribute-Value)

```sql
-- ❌ Avoid unless absolutely necessary
CREATE TABLE attributes (
    entity_id INT,
    attr_name TEXT,
    attr_value TEXT
);
```

→ Slow, hard to query. Use JSONB instead.

#### 2. Boolean for Status

```sql
-- ❌ Limited
is_active BOOLEAN

-- ✅ Better
status TEXT  -- 'active', 'suspended', 'deleted', 'banned'
```

Future-proof for more states.

#### 3. NULL-Heavy Schemas

Too many NULLs → STI overuse, denormalization needed.

#### 4. No Constraints

DB-level constraints (FK, CHECK, UNIQUE, NOT NULL) are your safety net. Don't skip.

#### 5. `String` Everything

```sql
-- ❌ TEXT for everything
created_at TEXT;
amount TEXT;

-- ✅ Proper types
created_at TIMESTAMPTZ;
amount NUMERIC(10,2);
```

---

## Quick Decision Guide

| Need | Approach |
|------|----------|
| Hierarchy / tree | Adjacency list + recursive CTE, or `ltree`, or closure table |
| Many-to-many with extra data | Junction table with extra columns |
| Polymorphic associations (3-5 types) | Multiple nullable FKs + CHECK |
| Polymorphic (many types) | CTI (parent + children tables) |
| Time-series at scale | Partitioning + rollup tables |
| Audit history | `created_at`, `updated_at` + audit log triggers |
| Soft delete | `deleted_at` + partial indexes |
| Counter / aggregate | Denormalize via trigger or materialized view |

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **1NF** | Atomic values |
| **2NF** | No partial dependency |
| **3NF** | No transitive dependency |
| **BCNF** | Every FD's LHS is super key |
| **1:1** | UNIQUE FK or shared PK |
| **1:N** | FK on "many" side |
| **M:N** | Junction table with composite PK |
| **STI** | One table + type column |
| **CTI** | Parent + child tables |
| **Soft Delete** | `deleted_at` column |
| **Audit Log** | Trigger → JSONB before/after |
| **Time-Series** | Partition by time + rollups |
| **Counter Cache** | Denormalize for fast reads |

---

## Practice

1. Model an e-commerce system: users, products, orders, order_items, addresses (with audit).
2. Implement soft delete on `users` with partial unique index on email.
3. Build polymorphic comment system using multiple nullable FKs.
4. Create a partitioned time-series `events` table with 12 monthly partitions.
5. Add full audit log via trigger to one of your tables.
6. Convert a 2NF schema to 3NF: identify and split transitive dependencies.
7. Design hierarchy table (categories with subcategories) using adjacency list, then query with recursive CTE.
