# Change Streams & Real-time

## Status: Complete

---

## Table of Contents

1. [What are Change Streams](#what-are-change-streams)
2. [Requirements](#requirements)
3. [Watch Collection / DB / Cluster](#watch-collection--db--cluster)
4. [Event Types](#event-types)
5. [Resume Tokens](#resume-tokens)
6. [fullDocument & updateLookup](#fulldocument--updatelookup)
7. [Pipeline Filtering](#pipeline-filtering)
8. [Use Cases](#use-cases)
9. [Limits & Pitfalls](#limits--pitfalls)
10. [Cheat Sheet](#cheat-sheet)

---

## What are Change Streams

> "**Tailable cursor over oplog** abstraction — **real-time notifications** on inserts/updates/deletes/replaces without polling."

Driver opens **long-lived watch** — server pushes matching change events.

---

## Requirements

> "**Replica set or sharded cluster** — **standalone NOT supported** (production stance)."

Why:

- Backed by **oplog** replication infrastructure

---

## Watch Collection / DB / Cluster

### Collection

```javascript
const cs = db.orders.watch()
```

### Database

```javascript
const cs = db.watch()
```

### Deployment (cluster-wide)

```javascript
const cs = db.getMongo().watch()
```

---

## Event Types

Common `operationType`:

| Type | Meaning |
|------|---------|
| **insert** | New doc |
| **update** | Fields changed |
| **replace** | Whole doc replace |
| **delete** | Doc removed |
| **invalidate** | Collection dropped etc. |

Payload includes **`documentKey`**, **`clusterTime`**, **`resumeToken`**.

---

## Resume Tokens

> "**Checkpoint** — after disconnect, resume **without losing events** (within oplog window)."

```javascript
// Node pseudocode
let resumeAfter = savedToken
const stream = collection.watch(pipeline, { resumeAfter })
```

### Oplog window caveat

If down **too long**, oplog rolled past → **resume fails** — rebuild snapshot + fresh watch.

---

## fullDocument & updateLookup

Default update event may show **delta only** (`updateDescription`).

### Full document after update

```javascript
collection.watch(pipeline, { fullDocument: "updateLookup" })
```

> "**Server does extra read** to fetch post-image — convenience vs cost."

---

## Pipeline Filtering

Change streams accept **aggregation-like pipeline** early stages:

```javascript
db.orders.watch([
  { $match: { "fullDocument.status": "PAID" } }
])
```

Reduces network/processing — **match early**.

---

## Use Cases

| Use case | Pattern |
|----------|---------|
| **Cache invalidation** | Watch keys → evict Redis |
| **Real-time UI** | Websocket fan-out from change stream |
| **Audit log** | Append-only audit collection via worker |
| **Search sync** | Push to Elasticsearch/OpenSearch |
| **CQRS read model** | Projector updates view store |

---

## Limits & Pitfalls

1. **Standalone dev surprise** — doesn't work — use **single-node replica set** dev docker.
2. **High churn collection** — flood of events — backpressure handling.
3. **resume token loss** — reconciliation job needed.
4. **updateLookup cost** — extra reads per update.
5. **Ordering** — multi-collection causal ordering needs careful design (not global strong ordering guarantee across collections trivially).
6. **Auth** — user needs **read** privileges on watched namespace.
7. **Replica set step-down** — stream may error — driver reconnect patterns.

---

## Cheat Sheet

| Topic | Point |
|-------|-------|
| Mechanism | Oplog-backed |
| Topology | RS / sharded |
| Resume | `resumeAfter` / `startAfter` |
| Full doc updates | `fullDocument: "updateLookup"` |
| Filter | `$match` in pipeline |
| Failure | Oplog window exceeded |

---

## Practice

1. Local **docker replica set** — open change stream insert doc observe event JSON.
2. Implement **resume token** persistence file crash recovery demo.
3. Compare **polling** every 1s vs change stream architecture diagram.
4. Measure **updateLookup** overhead rough thought experiment.
5. Design **cache invalidation** rule map collection `_id` → redis keys.
