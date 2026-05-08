# Database Design

## Status: Not Started

---

## Table of Contents

1. [SQL vs NoSQL — Decision Criteria](#sql-vs-nosql--decision-criteria)
2. [Database Choice Matrix](#database-choice-matrix)
3. [Normalization vs Denormalization](#normalization-vs-denormalization)
4. [Indexing Strategy](#indexing-strategy)
5. [Database Replication](#database-replication)
6. [Sharding Strategies (Detailed)](#sharding-strategies-detailed)
7. [Partition Key Selection](#partition-key-selection)
8. [PostgreSQL vs MySQL](#postgresql-vs-mysql)
9. [MongoDB vs Cassandra](#mongodb-vs-cassandra)
10. [Redis Use Cases](#redis-use-cases)
11. [Polyglot Persistence](#polyglot-persistence)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## SQL vs NoSQL — Decision Criteria

### SQL (Relational)

→ Structured data, complex queries, ACID, strong consistency.

| Use case | Example |
|----------|---------|
| Banking, finance | Transactions, balance, audit |
| E-commerce orders | Inventory, payment integrity |
| ERP / CRM | Complex relationships |
| Reporting / OLAP | JOIN-heavy queries |

### NoSQL (categories)

| Type | DB | Use |
|------|----|----|
| Key-value | Redis, DynamoDB | Cache, session, simple lookup |
| Document | MongoDB, CouchDB | Flexible schema, JSON |
| Column-family | Cassandra, HBase | Wide rows, time-series, write-heavy |
| Graph | Neo4j, Neptune | Relationships, recommendation |
| Search | Elasticsearch, Solr | Full-text search, log analytics |
| Time-series | InfluxDB, TimescaleDB | Metrics, IoT |

### When NoSQL?

- Massive scale (millions+ writes/sec)
- Schema flexibility needed
- Eventual consistency acceptable
- Read patterns known up-front
- Avoid JOINs (denormalize)

---

## Database Choice Matrix

| Need | Best |
|------|------|
| ACID money/inventory | PostgreSQL / MySQL |
| Massive write throughput, time-series | Cassandra / DynamoDB |
| Flexible schema (JSON-heavy) | MongoDB / PostgreSQL JSONB |
| Full-text search | Elasticsearch |
| Cache / sessions | Redis |
| Real-time leaderboard | Redis (Sorted Set) |
| Graph (recommendation) | Neo4j |
| Analytics (OLAP) | ClickHouse / BigQuery / Snowflake |
| Geospatial queries | PostGIS / MongoDB |
| Embedded ML features | Vertica / Redshift |

→ Real systems: **multiple DBs** (polyglot persistence).

---

## Normalization vs Denormalization

### Normalized

```
users(id, name, email)
orders(id, user_id, total)
order_items(id, order_id, product_id, qty)
products(id, name, price)
```

| | Pros | Cons |
|--|------|------|
| Normalized | No redundancy, easy update | JOINs costly at scale |

### Denormalized

```
order_view(order_id, user_name, user_email, total, items_json)
```

| | Pros | Cons |
|--|------|------|
| Denormalized | Fast read (no JOIN) | Update propagation, storage cost |

### Rule

> "Normalize for OLTP correctness. Denormalize for read scale at the cost of write complexity."

### Hybrid

- **Normalized primary** (source of truth)
- **Denormalized projections** (read-optimized) — built async via events / CDC
- Cross-ref: CQRS in `05-Microservices-Design.md`

---

## Indexing Strategy

### Why index?

- Without index: full table scan O(n)
- With B-tree index: O(log n)

### Index types

| Type | Use |
|------|-----|
| **B-tree** | Default; `=`, `<`, `>`, `BETWEEN`, sort |
| **Hash** | Equality only (Postgres rare) |
| **Composite** | Multi-column queries (leftmost prefix rule) |
| **Covering** | Index includes all selected columns (no table fetch) |
| **Partial** | Subset of rows (e.g., `WHERE active=true`) |
| **GIN** (Postgres) | Full-text, JSONB, arrays |
| **GiST** | Geospatial |
| **BRIN** | Huge tables, sorted by insert order |

### Cardinality

> "Higher cardinality (more unique values) → better index."

- Boolean column → bad index
- User ID → great index

### Composite index leftmost prefix

```sql
CREATE INDEX idx_user_status_created ON orders(user_id, status, created_at);
```

→ Useful for:

```sql
WHERE user_id = ?                              -- ✅
WHERE user_id = ? AND status = ?               -- ✅
WHERE user_id = ? AND status = ? AND created_at > ? -- ✅
WHERE status = ? AND created_at > ?            -- ❌ (skipped user_id)
```

### When NOT to index

- Tiny tables (full scan faster)
- Write-heavy tables (every write updates index)
- Low-cardinality column (boolean)

→ Cross-ref: `phase-4 / Performance & Optimization / 03-Query-Optimization.md`.

---

## Database Replication

### Topologies

#### Primary-Replica (most common)

```
Writes → Primary
           │
           ├─→ Replica 1 (async)
           ├─→ Replica 2
           └─→ Replica 3
Reads ← Replicas
```

#### Multi-Primary (master-master)

```
Writes → P1 ⇄ P2 (replicate both ways)
```

→ Conflict resolution required (last-write-wins, CRDTs, app-level).

#### Chain replication

```
P → R1 → R2 → R3 (ack from tail)
```

→ Strong consistency + simpler vs Paxos.

### Sync vs Async

| | Sync | Async |
|--|------|-------|
| Wait | Yes | No |
| Latency | Higher | Lower |
| Data loss on primary fail | None | Possible |
| Common | PostgreSQL sync | MySQL default |

### Use cases

- Read scale: read replicas
- DR: cross-region replicas
- HA: synchronous replication for failover

---

## Sharding Strategies (Detailed)

### 1. Range-based

```
Shard 1: user_id 0 - 1M
Shard 2: user_id 1M - 2M
```

| Pros | Cons |
|------|------|
| Range queries efficient | Hotspots (recent users) |

→ Add `bucket` prefix: `bucket_id (0-99) + user_id` → distributes hot writes.

### 2. Hash-based

```
shard_id = hash(user_id) % N
```

| Pros | Cons |
|------|------|
| Uniform distribution | Range queries hard, resharding pain |

### 3. Consistent hashing

→ Same idea as hash but with **smooth resharding**.

### 4. Directory-based

```
Lookup service: user_id → shard_id
```

| Pros | Cons |
|------|------|
| Flexible (move users freely) | Lookup service = SPOF + extra hop |

### 5. Geographic

```
Region IN → India shards
Region US → US shards
```

| Pros | Cons |
|------|------|
| Low latency for users | Imbalance (some regions much larger) |

### 6. Date-based (time-series)

```
events_2024_01, events_2024_02, ... (monthly partitions)
```

| Pros | Cons |
|------|------|
| Old data archived easily | Recent month hot |

---

## Partition Key Selection

### Rules

1. **High cardinality** — avoid 2-3 unique values
2. **Even distribution** — no hotspot
3. **Aligned with query patterns** — most queries should hit one shard
4. **Avoid monotonic** keys (timestamp alone)

### Example — Twitter tweets

| Candidate | Verdict |
|-----------|---------|
| `tweet_id` (sequential) | ❌ Hot shard (latest writes) |
| `user_id` | ✅ Even, aligns with "user's tweets" |
| `tweet_id` (snowflake — random suffix) | ✅ Distribution + sortable by time |

### Composite shard keys

```
shard_key = (region_id, user_id)
```

→ Locality + uniformity.

---

## PostgreSQL vs MySQL

| | PostgreSQL | MySQL |
|--|------------|-------|
| Default storage | row-oriented | row (InnoDB) |
| Replication | logical + physical | binlog |
| Consistency | strict, ACID | configurable |
| JSON support | JSONB (indexed, native) | JSON (less mature) |
| Extensions | rich (PostGIS, TimescaleDB, etc.) | limited |
| Window functions | rich | added later |
| Transaction default isolation | READ COMMITTED | REPEATABLE READ |
| Use | analytics + OLTP, geospatial | OLTP, read-heavy web |
| MVCC | Yes | Yes (InnoDB) |
| MV ROLLBACK on DDL | Yes | Older MySQL: no |

### Default pick

- New projects → **PostgreSQL** (stronger features, modern)
- Legacy / massive read web → MySQL still common
- Cross-ref: `backend-skills / Database-Mastery / 01-SQL-Advanced.md`

---

## MongoDB vs Cassandra

| | MongoDB | Cassandra |
|--|---------|-----------|
| Type | Document | Column-family |
| Schema | Flexible JSON | Rows with columns (wide) |
| Query | Rich (find, aggregate) | CQL (limited JOIN) |
| Consistency | Strong (replica set) | Tunable per-operation |
| Write throughput | Good | Excellent (LSM-tree) |
| Use | Catalog, CMS | Time-series, IoT, write-heavy |
| Indexes | Secondary indexes | Limited (partition + clustering keys primary) |
| CAP | CP | AP (tunable) |

### MongoDB shines

- Flexible schema
- Nested documents (e.g., embedded comments)
- Replica sets + automatic failover
- ACID at document level (transactions added in 4.0+)

### Cassandra shines

- Linear write scale
- Multi-DC replication out of box
- No single point of failure
- Time-series workloads

---

## Redis Use Cases

### Key-value cache

```
GET user:123 → cached profile JSON
```

### Session store

```
SET session:abc <data> EX 1800
```

### Rate limiter (sliding window)

```
INCR + EXPIRE per user / endpoint
```

### Leaderboard (Sorted Set)

```
ZADD leaderboard 1500 user_alice
ZRANGE leaderboard 0 9 WITHSCORES   // top 10
```

### Pub/Sub

Lightweight messaging.

### Distributed lock (SETNX + EXPIRE)

```
SET lock:resource <token> NX EX 30
```

→ For real distributed lock, use **Redlock** algorithm.

### Streams (Redis 5+)

Lightweight Kafka-like log.

### Geospatial (`GEOADD`, `GEORADIUS`)

Driver finder, store finder.

---

## Polyglot Persistence

> "Use multiple databases — each for what it's best at."

### Example: e-commerce architecture

```
PostgreSQL  → Orders, payments (ACID)
MongoDB     → Product catalog (flexible schema)
Elasticsearch → Search
Redis       → Cart, sessions, cache
S3          → Product images
Neo4j       → Recommendations
ClickHouse  → Analytics dashboards
```

### Coordination

- Events for cross-DB consistency (Outbox pattern)
- ETL / CDC pipeline (Debezium → Kafka)
- API composition layer

---

## Common Output Traps

### Q1. "MongoDB doesn't support transactions"

→ Wrong post-4.0; supports multi-document ACID transactions.

### Q2. "NoSQL is faster than SQL"

→ Depends. PostgreSQL on simple key-lookup with index ≈ Redis for cold data; Redis wins on cached hot data.

### Q3. "Sharding from day 1"

→ Premature optimization. Start single DB; shard when justified by data.

### Q4. Index for every column

→ Hurts writes; index only what's queried often.

### Q5. JOIN across shards "naturally"

→ Won't work; pre-aggregate or fan-out + merge.

---

## Pitfalls

1. **Choosing NoSQL for relational data** — JOINs become app-side merges.
2. **No backups** of NoSQL — devs assume "auto magic"; verify.
3. **Index every column** — write amplification.
4. **Composite index leftmost violated** — index unused; check explain plan.
5. **Sharding without shard key** — query needs all shards (fan-out).
6. **MongoDB without replica set** — single instance; no HA.
7. **Cassandra with `ALLOW FILTERING`** — full scan (bad).
8. **Redis without persistence** (RDB/AOF) — restart loses data.
9. **PostgreSQL sequence + sharding** — sequences global; bad in shards.
10. **Cross-shard JOINs** in app — slow + complex.
11. **Polyglot without need** — operational overhead.
12. **Not using JSONB in Postgres** when schema flexibility needed.

---

## Cheat Sheet

| Choose | When |
|--------|------|
| PostgreSQL | OLTP + analytics + JSONB, default modern |
| MySQL | Legacy, simple OLTP |
| MongoDB | Flexible schema, document-natural model |
| Cassandra | Massive writes, time-series, multi-DC |
| Redis | Cache, sessions, leaderboard, real-time |
| DynamoDB | Managed key-value, AWS native |
| Elasticsearch | Full-text search, log analytics |
| Neo4j | Relationships heavy (recommendation) |
| ClickHouse | OLAP, analytics |

| Sharding key | Pick |
|--------------|------|
| High cardinality | ✅ |
| Aligned with query | ✅ |
| Monotonic | ❌ hotspot |
| Boolean | ❌ no spread |

| Replication | Use |
|-------------|-----|
| Async primary-replica | Most common |
| Sync replica | Strong consistency |
| Multi-primary | Multi-region writes (CRDT) |

---

## Practice

1. Choose DB for: Twitter feed / Order processing / Real-time chat / Search.
2. Design schema for URL shortener (single DB) → SQL or NoSQL? why?
3. Sharding key for: tweets / messages / orders / IoT readings.
4. Read replica strategy: which queries land on replica, which on primary?
5. Polyglot setup for e-commerce — list 5 DBs + their role.
