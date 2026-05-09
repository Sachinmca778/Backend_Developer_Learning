# SQL vs NoSQL Choice

## Status: Complete

---

## Table of Contents

1. [Why This Choice Matters](#why-this-choice-matters)
2. [SQL — Relational Databases](#sql--relational-databases)
3. [NoSQL — The Four Flavors](#nosql--the-four-flavors)
4. [Document Databases](#document-databases)
5. [Key-Value Stores](#key-value-stores)
6. [Wide-Column Stores](#wide-column-stores)
7. [Graph Databases](#graph-databases)
8. [Decision Matrix](#decision-matrix)
9. [Polyglot Persistence](#polyglot-persistence)
10. [Interview Framing](#interview-framing)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Why This Choice Matters

> "**Database is the most painful migration**. Pick wrong → years of pain. Pick well → years of leverage. Always start with **access patterns**, not buzzwords."

Three questions to drive the choice:

1. **Schema** — fixed and well-known, or evolving and flexible?
2. **Access pattern** — joins / complex queries vs simple key lookups vs aggregations?
3. **Scale + consistency** — strong consistency at moderate scale vs eventual consistency at massive scale?

---

## SQL — Relational Databases

> "**Relational** = data in **tables** with **fixed columns**, **typed**, related via **foreign keys**, queried with **SQL** + **JOINs**, with **ACID transactions**."

### Strengths

| Strength | Why |
|----------|-----|
| **Strong consistency** | ACID transactions — all-or-nothing |
| **Joins** | Combine entities flexibly without app code |
| **Mature ecosystem** | Decades of tooling, monitoring, expertise |
| **Schema integrity** | DB rejects bad data; one source of truth |
| **Complex queries** | Aggregations, window functions, CTEs, subqueries |
| **Transactions across rows/tables** | Default behavior, not bolted on |

### Weaknesses

| Weakness | Why |
|----------|-----|
| **Vertical scale ceiling** | Hard to scale writes beyond single primary |
| **Schema migrations painful** | `ALTER TABLE` on huge tables → locks / downtime |
| **Object-relational mismatch** | App objects don't naturally map (ORM hides + leaks) |
| **JOIN cost at scale** | Big joins slow down once data sharded |

### Top picks

| DB | Notes |
|----|-------|
| **PostgreSQL** | Most loved — JSONB, partitioning, full-text, extensions (PostGIS, pgvector) |
| **MySQL** | Battle-tested, simpler, huge ecosystem |
| **Aurora** (AWS) | MySQL/Postgres-compatible, cloud-native |
| **CockroachDB**, **Spanner**, **YugabyteDB** | Distributed SQL — horizontal scale + ACID |

### Use SQL when

- Data **relational** with FKs (users → orders → items)
- **Complex queries / reporting** needed
- **Strong consistency** required (money, inventory)
- Scale is **moderate** (TB-scale reachable; PB needs help)
- Team SQL-fluent

---

## NoSQL — The Four Flavors

> "**NoSQL** = 'Not Only SQL' — umbrella term for non-relational stores. Each flavor optimizes for **different access patterns**."

```
NoSQL
 ├── Document      (MongoDB, Couchbase)         — flexible nested data
 ├── Key-Value     (Redis, DynamoDB, Memcached) — single-key reads/writes
 ├── Wide-Column   (Cassandra, ScyllaDB, HBase) — time-series, write-heavy
 └── Graph         (Neo4j, Neptune, JanusGraph) — relationships traversal
```

> Common myth: "NoSQL = no schema". False — it usually means **schema-on-read** instead of **schema-on-write**, but you still have an implicit schema in app code.

---

## Document Databases

> "**Documents** (JSON / BSON) instead of rows. Nested fields, flexible schema."

### Top picks

- **MongoDB** — most popular
- **Couchbase**, **Amazon DocumentDB** (mostly MongoDB-compatible)
- **PostgreSQL with JSONB** — best of both worlds for many cases

### Strengths

- **Flexible schema** — add fields anytime
- **Nested data natural** (no joins needed for "user with addresses")
- **Aggregation pipeline** powerful (MongoDB)
- **Horizontal scale** via sharding

### Weaknesses

- **Joins limited** (`$lookup` exists but expensive)
- **Multi-document transactions** were hard until 4.0+; still costlier than RDBMS
- **Easy to misuse** — schemaless ≠ no design (see schema patterns)

### Use when

- **Catalog with varying attributes** (e-commerce SKUs)
- **CMS / blogs** — nested content
- **User profiles with flexible fields**
- **Mobile sync** (Realm, Couchbase Mobile)

### Don't use when

- Heavy joins / reporting → SQL better
- Strong cross-document consistency required → think harder or pick SQL

---

## Key-Value Stores

> "**Simplest model**: `put(key, value)`, `get(key)`. Sub-millisecond latency at huge scale."

### Top picks

| Store | Use |
|-------|-----|
| **Redis** | In-memory, rich data structures, persistence optional |
| **Memcached** | Pure cache, multi-thread |
| **DynamoDB** | Managed, serverless, single-digit ms |
| **etcd / ZooKeeper** | Coordination (small, consistent) |
| **RocksDB / LevelDB** | Embedded |

### Strengths

- **Sub-ms latency** for key lookups
- **Scale horizontally** trivially (consistent hashing)
- **Simple ops** — fewer failure modes than complex DBs
- **Cache-friendly** by design

### Weaknesses

- **No queries beyond key** — can't `WHERE age > 18` without secondary index hacks
- **Range scans limited** unless ordered KV (RocksDB)
- **Aggregations** done in app

### Use when

- **Session store**, **rate limiter counters**, **shopping cart**, **leaderboard**
- **Lookup-heavy** workloads (user profile by ID, config by key)
- **Ephemeral cache** for hot data

### DynamoDB note

- **Single-table design** with composite keys (`PK + SK`) gives you query-like power
- Excels with **predictable access patterns** known upfront
- Bad if access patterns change a lot (no ad-hoc queries)

---

## Wide-Column Stores

> "**Rows with billions of columns**, accessed by row key + column. Write-optimized, time-series-friendly."

### Top picks

| Store | Notes |
|-------|-------|
| **Apache Cassandra** | Most popular, masterless, AP |
| **ScyllaDB** | Cassandra-compatible, much faster (C++ rewrite) |
| **HBase** | Hadoop ecosystem, CP |
| **Bigtable** (Google) | HBase's inspiration |

### Strengths

- **Massive write throughput** — append-only LSM trees
- **Time-series natural** — partition by entity + sort by time
- **Linear horizontal scaling** — add nodes, throughput grows
- **Tunable consistency** (Cassandra) per query

### Weaknesses

- **Query model limited** — must design schema around queries (no ad-hoc joins)
- **Tombstones** can hurt read performance after many deletes
- **Operational complexity** higher than managed alternatives

### Use when

- **Time-series** (metrics, IoT sensors, logs)
- **Massive write volume** (chat messages, click streams)
- **Geographically distributed** writes
- **Activity feeds** at huge scale

---

## Graph Databases

> "**Nodes + edges as first-class citizens.** Optimized for **relationships and traversals**."

### Top picks

- **Neo4j** — most popular, Cypher query language
- **Amazon Neptune** — managed, supports Gremlin + SPARQL
- **JanusGraph** — distributed
- **TigerGraph** — analytics-focused

### Strengths

- **Multi-hop queries fast** — "friends of friends of friends" doesn't blow up
- **Schema flexible** for relationships
- **Native graph algorithms** (shortest path, centrality, communities)

### Weaknesses

- **Niche** — most teams don't have graph expertise
- **Scale** harder than KV / document for huge graphs
- **Operational tooling** less mature

### Use when

- **Social networks** (followers, mutual connections)
- **Recommendations** (people who liked X also liked Y via graph)
- **Fraud detection** (suspicious connection patterns)
- **Knowledge graphs** (Wikidata-like)
- **Supply chain / dependency graphs**

### Don't use when

- Relationships are **few hops** only — SQL JOINs handle that fine
- Storage / write throughput is the bottleneck

---

## Decision Matrix

| Need | Best fit |
|------|----------|
| Strong consistency, transactions, joins | **SQL** (Postgres / MySQL / Aurora) |
| Flexible schema, nested docs | **Document** (MongoDB / Postgres JSONB) |
| Sub-ms key lookups, cache | **Key-Value** (Redis / DynamoDB) |
| Massive write throughput, time-series | **Wide-column** (Cassandra / Scylla) |
| Multi-hop relationship traversal | **Graph** (Neo4j / Neptune) |
| Full-text search | **Elasticsearch / OpenSearch** |
| Analytics / OLAP | **ClickHouse / Snowflake / BigQuery / Redshift** |
| Vector / embeddings | **pgvector / Pinecone / Weaviate / Milvus** |
| Geospatial heavy | **Postgres + PostGIS / MongoDB 2dsphere** |

---

## Polyglot Persistence

> "**Real systems use multiple DBs.** One does not fit all."

Example e-commerce stack:

```
Product catalog  → MongoDB (flexible attributes)
Orders / payments → PostgreSQL (ACID, reporting)
Sessions / cart   → Redis (fast, ephemeral)
Search            → Elasticsearch (full-text + filters)
Analytics events  → Cassandra → S3 → Snowflake
Recommendations   → Neo4j (collaborative filtering)
Media metadata    → DynamoDB (key lookups)
Media files       → S3
```

→ Pick best store **per workload**, accept the operational cost.

---

## Interview Framing

When asked "**SQL or NoSQL?**" never answer with one word. Use this skeleton:

> "Both have valid uses. The right answer depends on **access patterns** and **consistency requirements**.  
> For **structured data with relationships and joins** I'd use **PostgreSQL** because of ACID + mature ecosystem.  
> For **flexible documents / massive scale** I'd use **MongoDB** or **DynamoDB** — they trade joins for horizontal scale.  
> In a real system I'd likely use **multiple stores** — for example, **Postgres for orders + Redis for cache + Cassandra for event stream**."

Then drill into the **specific entity** they asked about.

---

## Pitfalls

1. **Picking NoSQL because it's "modern"** — without justifying access patterns.
2. **NoSQL with relational data** — recreating JOINs in app code = pain.
3. **SQL at massive write scale** without sharding plan.
4. **Mongo for transactional money** without `w: majority` + `readConcern: majority`.
5. **DynamoDB without modeled access patterns upfront** — query rewrites = full table scan or schema change.
6. **Graph DB for simple data** — overkill, harder to operate.
7. **Single DB for all features** — never matches all needs.
8. **No decision criteria** in design doc — just naming the DB.
9. **Skipping `JSONB` in Postgres** — often it's enough; saves running 2 systems.
10. **Locking into vendor early** — DynamoDB / Cosmos lock-in vs portable Postgres / Mongo.

---

## Cheat Sheet

| Type | Examples | Best for |
|------|----------|----------|
| **SQL** | Postgres, MySQL, Aurora | Joins, transactions, reporting |
| **Document** | MongoDB, Couchbase, Postgres JSONB | Flexible nested data |
| **Key-Value** | Redis, DynamoDB, Memcached | Sub-ms lookups, cache, session |
| **Wide-Column** | Cassandra, Scylla, HBase | Time-series, write-heavy |
| **Graph** | Neo4j, Neptune | Relationship traversal |
| **Search** | Elasticsearch, OpenSearch | Full-text + filters |
| **OLAP** | ClickHouse, Snowflake | Analytics |
| **Vector** | pgvector, Pinecone | Embeddings, semantic search |

| Question | Answer driver |
|----------|---------------|
| Joins needed? | SQL |
| Flexible schema? | Document |
| Sub-ms latency? | Key-Value |
| Write-heavy time-series? | Wide-column |
| Many-hop relationships? | Graph |

---

## Practice

1. For an Uber-like system, list each entity and pick a DB with justification.
2. Migrate a "user profile + addresses + orders" data model from Postgres to MongoDB. Compare query shapes.
3. Defend "Postgres + Redis is enough for 90% of startups" against "we need NoSQL to scale".
4. Design DynamoDB single-table model for a chat app — partition key + sort key choices.
5. Justify Cassandra vs DynamoDB for an IoT telemetry pipeline (1M events/sec).
