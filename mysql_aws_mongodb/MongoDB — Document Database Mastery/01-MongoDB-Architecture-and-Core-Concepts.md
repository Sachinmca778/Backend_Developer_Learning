# MongoDB Architecture & Core Concepts

## Status: Complete

---

## Table of Contents

1. [What is MongoDB?](#what-is-mongodb)
2. [Document Model & BSON](#document-model--bson)
3. [Collections & Dynamic Schema](#collections--dynamic-schema)
4. [_id & ObjectId](#_id--objectid)
5. [Namespace: db.collection](#namespace-dbcollection)
6. [RDBMS vs MongoDB Mental Map](#rdbms-vs-mongodb-mental-map)
7. [Core Processes](#core-processes)
8. [Deployment Topologies](#deployment-topologies)
9. [Pitfalls](#pitfalls)
10. [Cheat Sheet](#cheat-sheet)

---

## What is MongoDB?

> "**Document-oriented** NoSQL DB — data JSON-like documents mein; horizontal scale + flexible schema ke liye famous."

- Default storage **WiredTiger** (since 3.2)
- **BSON** on wire + disk (binary JSON + extra types)
- Query language: **CRUD + aggregation pipeline** (JSON-like stages)

---

## Document Model & BSON

### JSON vs BSON

| | JSON | BSON |
|--|------|------|
| Encoding | Text | Binary |
| Types | string, number, bool, null, array, object | + **Date**, **ObjectId**, **Binary**, **Decimal128**, **Int32/Int64**, **Timestamp**, **Regex**, **MinKey/MaxKey** |
| Size | Often larger on disk | Compact + type tags |

### Example document

```javascript
{
  _id: ObjectId("6652a1b2c3d4e5f678901234"),
  name: "Sachin",
  balance: NumberDecimal("99.99"),   // exact money
  tags: ["vip", "beta"],
  meta: { country: "IN" },
  createdAt: ISODate("2024-05-08T12:00:00Z")
}
```

### Why BSON matters

- **Decimal128** for money (avoid float drift)
- **Date** native (not string ISO confusion)
- **ObjectId** sortable roughly by creation time (first 4 bytes = seconds since Unix epoch)

---

## Collections & Dynamic Schema

### Collection

> "Documents ka bag — **table jaisa** but **no fixed columns**."

```javascript
db.users.insertOne({ name: "A", age: 20 })
db.users.insertOne({ name: "B", email: "b@x.com" })  // different shape OK
```

### Schema-less vs validation

- **Default**: no enforced schema (flexible)
- **Schema validation** (optional): `$jsonSchema` on collection

```javascript
db.createCollection("orders", {
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["userId", "total"],
      properties: {
        userId: { bsonType: "objectId" },
        total: { bsonType: "decimal", minimum: 0 }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
})
```

→ Production apps often **middle path**: flexible + validation for critical collections.

---

## _id & ObjectId

### Every document needs `_id`

- If omitted on insert → MongoDB auto-generates **`ObjectId`**
- **`_id`** is **unique within collection**
- **Immutable** (can't change `_id` in place — replace doc)

### ObjectId structure (12 bytes)

```
| 4 bytes      | 5 bytes random | 3 bytes counter |
| timestamp    | machine + proc | inc per second |
```

- **Rough ordering** by insert time (same second → counter)
- **Not globally sequential** — don't use as strict order guarantee under extreme concurrency
- **Predictable prefix** in same second — security note for enumeration

### Custom _id

```javascript
db.users.insertOne({ _id: "user:sachin", name: "Sachin" })   // string OK
db.users.insertOne({ _id: 10001, name: "Legacy" })          // int OK
```

→ Use **business natural keys** only when stable + unique.

---

## Namespace: db.collection

```
database_name.collection_name
```

Examples:

```
shop.products
shop.orders
analytics.events
```

### Limits

- Database name: case-sensitive on some systems; avoid weird chars
- Collection name max **120 bytes** (with prefix rules for internal collections)
- **`system.*`** reserved

---

## RDBMS vs MongoDB Mental Map

| RDBMS | MongoDB |
|-------|---------|
| Database | Database |
| Table | **Collection** |
| Row | **Document** |
| Column | **Field** |
| JOIN | **`$lookup`** / embedded docs / app-side |
| Primary key | **`_id`** |
| Index | Index (similar concepts, compound/multikey/text/geo) |
| Transaction | Multi-document TX (4.0+) |

### Important differences

| Topic | RDBMS | MongoDB |
|-------|-------|---------|
| Schema | Usually fixed | Flexible + optional validation |
| Relations | FK + JOIN native | Prefer embed/reference patterns |
| Scale vertical | Common | Horizontal **sharding** first-class |
| Consistency | Strong default | Tunable **read/write concern** |

---

## Core Processes

### mongod

> "**Database server process** — stores data, handles queries."

- One `mongod` per node (standalone or replica set member)
- Default port **27017**
- Config: `--dbpath`, `--replSet`, `--shardsvr`, `--configsvr`

### mongos

> "**Query router** for **sharded cluster** — apps connect here, not directly to shards."

```
App → mongos → shard1 / shard2 / shard3
        ↓
   config servers (metadata: chunks, zones)
```

- Stateless routing layer (multiple `mongos` for HA)
- Parses query → targets relevant shards → merges results

### Config servers (mongocfg / CSRS)

> "**Metadata store** for sharded cluster — chunk ranges, shard membership."

- **Replica set** of config servers (usually 3) — **Config Server Replica Set (CSRS)**
- Since 3.4: always replica set, not SCCC legacy

### Legacy namings

Docs sometimes say **mongocfg** — means config server processes (`mongod --configsvr`).

---

## Deployment Topologies

### 1. Standalone

```
mongod (single)
```

- Dev / CI only
- **No HA**, no oplog for production features like change streams in older docs — **4.0+ change streams need replica set**

### 2. Replica Set

```
Primary + Secondary + Secondary   (min 3 voting members typical)
```

- Automatic failover
- Read scaling (secondaries)
- Backup target

### 3. Sharded Cluster

```
mongos (×n) → shards (each shard = replica set) + config servers
```

- Horizontal partition by **shard key**
- **`mongos`** required for app routing

---

## Pitfalls

1. **Standalone in prod** — no failover; change streams limited historically.
2. **Treating ObjectId as secret** — partially guessable from timestamp.
3. **No `_id` index assumption** — `_id` always has unique index by default.
4. **Huge embedded arrays** — document **16 MB max** — blows up.
5. **mongo shell vs driver** — syntax mostly same; transactions API differs slightly.
6. **Case sensitivity** — field names **case-sensitive** (`name` ≠ `Name`).
7. **Dot in field names** — allowed but awkward for updates (`a.b` interpreted as nested path).

---

## Cheat Sheet

| Concept | Quick |
|---------|-------|
| BSON | Binary JSON + rich types |
| Collection | Group of documents |
| _id | Unique per doc; default ObjectId |
| ObjectId | 12 B = time + random + counter |
| Namespace | `db.collection` |
| mongod | Server |
| mongos | Sharding router |
| Config RS | Shard metadata |

| Topology | Use |
|----------|-----|
| Standalone | Dev |
| Replica set | Prod default |
| Sharded | Very large data / write scale |

---

## Practice

1. Insert docs with different shapes into same collection; observe behavior.
2. Decode ObjectId timestamp in shell: `ObjectId("...").getTimestamp()`
3. Draw diagram: app → mongos → 2 shards + config RS.
4. Compare embedding vs referencing for `orders` + `order_items` decision.
