# MongoDB — Document Database Mastery

MongoDB ki **deep mastery** — BSON/document model, CRUD + operators, aggregation pipeline, indexing, schema patterns, transactions, replica sets, sharding, performance, change streams, security. Hinglish, with shell examples + Node/Java drivers patterns.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | MongoDB Architecture & Core Concepts | [01-MongoDB-Architecture-and-Core-Concepts.md](./01-MongoDB-Architecture-and-Core-Concepts.md) | Complete |
| 2 | CRUD Operations & Query Operators | [02-CRUD-and-Query-Operators.md](./02-CRUD-and-Query-Operators.md) | Complete |
| 3 | Aggregation Pipeline | [03-Aggregation-Pipeline.md](./03-Aggregation-Pipeline.md) | Complete |
| 4 | Indexing Strategy | [04-Indexing-Strategy.md](./04-Indexing-Strategy.md) | Complete |
| 5 | Schema Design Patterns | [05-Schema-Design-Patterns.md](./05-Schema-Design-Patterns.md) | Complete |
| 6 | Transactions & Consistency | [06-Transactions-and-Consistency.md](./06-Transactions-and-Consistency.md) | Complete |
| 7 | Replica Sets | [07-Replica-Sets.md](./07-Replica-Sets.md) | Complete |
| 8 | Sharding | [08-Sharding.md](./08-Sharding.md) | Complete |
| 9 | Performance & Monitoring | [09-Performance-and-Monitoring.md](./09-Performance-and-Monitoring.md) | Complete |
| 10 | Change Streams & Real-time | [10-Change-Streams-and-Real-time.md](./10-Change-Streams-and-Real-time.md) | Complete |
| 11 | MongoDB Security | [11-MongoDB-Security.md](./11-MongoDB-Security.md) | Complete |

---

## What's Inside Each File?

### [01 — Architecture & Core Concepts](./01-MongoDB-Architecture-and-Core-Concepts.md)
**BSON** vs JSON (Binary JSON + extra types), **collections** (schema-less default), **dynamic schema**, **`_id` / ObjectId** (12 bytes: 4 timestamp + 5 random + 3 counter), **namespace** `db.collection`, **RDBMS mapping** (DB/collection/document/field), **`mongod`** (server), **`mongos`** (router), **config servers** (mongocfg / CSRS), deployment types (standalone vs replica set vs sharded cluster).

### [02 — CRUD & Query Operators](./02-CRUD-and-Query-Operators.md)
**insertOne / insertMany**, **findOne / find** (cursor), **updateOne / updateMany / replaceOne** (`$set`, `$unset`, `$inc`, `$push`, `$pull`, `$addToSet`, `$arrayFilters`), **deleteOne / deleteMany**, **findOneAndUpdate** (atomic RMW). Query ops: `$eq`/`$ne`/`$gt`/`$gte`/`$lt`/`$lte`, `$in`/`$nin`, `$and`/`$or`/`$not`/`$nor`, `$exists`, `$type`, `$regex`, `$where`. Array: `$all`, `$elemMatch`, `$size`. **Projection** rules (1 include / 0 exclude — `_id` exception).

### [03 — Aggregation Pipeline](./03-Aggregation-Pipeline.md)
Stages: **`$match`** (early), **`$group`** + accumulators (`$sum`, `$avg`, `$min`, `$max`, `$push`, `$addToSet`, `$count`), **`$project`**, **`$sort`**, **`$limit`**, **`$skip`**, **`$lookup`** (left outer join), **`$unwind`**, **`$facet`**, **`$bucket` / `$bucketAuto`**, **`$graphLookup`**, **`$merge` / `$out`**. Pipeline optimization tips, allowDiskUse.

### [04 — Indexing Strategy](./04-Indexing-Strategy.md)
Single-field, **compound** (**ESR**: Equality → Sort → Range), **multikey** (arrays), **text** (`$text`/`$search`, weights, language), **2dsphere** (`$near`, `$geoWithin`), **hashed** (sharding + equality). Properties: **unique**, **sparse**, **TTL**, **partial**. Covered queries, index intersection.

### [05 — Schema Design Patterns](./05-Schema-Design-Patterns.md)
**Embed vs reference** decision tree, **Extended Reference**, **Subset**, **Outlier**, **Bucket** (time-series). Anti-patterns (unbounded arrays, mega-documents).

### [06 — Transactions & Consistency](./06-Transactions-and-Consistency.md)
Multi-doc ACID (4.0+), sessions API (`startSession`, `startTransaction`, `commitTransaction`, `abortTransaction`), cross-collection/shard (4.2+), cost trade-offs, **read concern** (local/majority/linearizable), **write concern** (`w`, `j`), **causal consistency**, retryable writes/reads.

### [07 — Replica Sets](./07-Replica-Sets.md)
Min 3 nodes (primary + secondaries / arbiter), **election** (`electionTimeoutMillis`), **oplog**, **read preferences**, hidden/delayed members, priority, rolling upgrades.

### [08 — Sharding](./08-Sharding.md)
Shard key **cardinality / frequency / monotonic** trade-offs, **ranged vs hashed**, chunks, **balancer**, **`mongos`**, **`sh.status()`**, **zones** (data locality).

### [09 — Performance & Monitoring](./09-Performance-and-Monitoring.md)
`db.currentOp()`, `db.killOp()`, **`explain("executionStats")`** (COLLSCAN vs IXSCAN, keys examined vs docs examined), `collStats`, **mongostat**, **mongotop**, Atlas Performance Advisor, driver **connection pool** (`maxPoolSize`).

### [10 — Change Streams](./10-Change-Streams-and-Real-time.md)
`watch()` on collection/DB/cluster, resume tokens, `fullDocument: "updateLookup"`, use cases (cache invalidation, audit, sync). Replica set / sharded requirement.

### [11 — MongoDB Security](./11-MongoDB-Security.md)
**SCRAM-SHA-256**, x.509, LDAP/Kerberos, **RBAC** (built-in + custom roles), IP allowlist, **TLS**, encryption at rest (Enterprise vs FS), field-level encryption, audit (Enterprise), never expose `mongod` publicly.

---

## Recommended Order

```
1. Architecture          ← mental model + processes
2. CRUD & Operators      ← daily queries
3. Indexing              ← performance foundation
4. Aggregation           ← analytics + joins
5. Schema Patterns       ← modeling decisions
6. Transactions          ← consistency when needed
7. Replica Sets          ← HA + reads
8. Sharding              ← horizontal scale
9. Performance           ← ops + EXPLAIN
10. Change Streams       ← real-time
11. Security             ← lock down prod
```

---

## Companion Folders

- [MySQL — Relational Database Mastery](../MySQL%20%E2%80%94%20Relational%20Database%20Mastery/) — SQL vs document trade-offs
- [Database Mastery](../../backend-skills/Database-Mastery/) — general SQL
- [System Design](../../phase-5-interview-preparation/System%20Design%20Interviews/) — when to pick MongoDB

---

## Quick Reference

| Need | Where |
|------|-------|
| Slow query | `explain("executionStats")` (file 09) |
| Join-like | `$lookup` (file 03) |
| Hot writes | Shard key / bucket pattern (08, 05) |
| HA | Replica set (07) |
| Real-time | Change streams (10) |
| ACID multi-doc | Transactions (06) |

---

## Status Tracker

```
[x] 01 — Architecture & Core Concepts
[x] 02 — CRUD & Query Operators
[x] 03 — Aggregation Pipeline
[x] 04 — Indexing Strategy
[x] 05 — Schema Design Patterns
[x] 06 — Transactions & Consistency
[x] 07 — Replica Sets
[x] 08 — Sharding
[x] 09 — Performance & Monitoring
[x] 10 — Change Streams
[x] 11 — Security
```

> "MongoDB = **schema-on-read** + **aggregation pipeline** + **index discipline**. Embeddings galat = scale pain; shard key galat = irreversible pain."
