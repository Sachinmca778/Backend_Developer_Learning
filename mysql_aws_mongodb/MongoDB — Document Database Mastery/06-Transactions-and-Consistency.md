# Transactions & Consistency

## Status: Complete

---

## Table of Contents

1. [Multi-Document ACID](#multi-document-acid)
2. [Sessions API](#sessions-api)
3. [Replica Set vs Sharded Transactions](#replica-set-vs-sharded-transactions)
4. [Performance Cost](#performance-cost)
5. [Read Concern](#read-concern)
6. [Write Concern](#write-concern)
7. [Causal Consistency](#causal-consistency)
8. [Retryable Writes / Reads](#retryable-writes--reads)
9. [Practical Patterns](#practical-patterns)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## Multi-Document ACID

> "**MongoDB 4.0+**: replica set pe **multi-document transaction** — snapshot isolation style semantics (MVCC).  
> **4.2+**: **sharded cluster** cross-shard transactions."

### Scope

- Multiple docs **same collection** or **different collections** **same DB** typically (Mongo allows multi-collection in transaction)
- Sharded: multiple shards participate — **two-phase commit** coordination — higher latency

---

## Sessions API

### Node.js style (conceptual)

```javascript
const session = client.startSession()
try {
  session.startTransaction({
    readConcern: { level: "snapshot" },
    writeConcern: { w: "majority" }
  })

  await ordersColl.updateOne({ _id: oid }, { $set: { status: "PAID" } }, { session })
  await inventoryColl.updateOne({ sku }, { $inc: { qty: -1 } }, { session })

  await session.commitTransaction()
} catch (e) {
  await session.abortTransaction()
  throw e
} finally {
  session.endSession()
}
```

### mongosh

```javascript
const session = db.getMongo().startSession()
session.startTransaction()
try {
  db.orders.updateOne({ _id: oid }, { $inc: { version: 1 } }, { session })
  db.inventory.updateOne({ sku: "X" }, { $inc: { qty: -1 } }, { session })
  session.commitTransaction()
} catch (e) {
  session.abortTransaction()
  throw e
} finally {
  session.endSession()
}
```

### Rules snapshot

- Operations **inside transaction** must pass **`session`**
- **DDL** limited inside transactions (avoid createCollection etc.)
- **Max transaction duration** — default limits exist — long TX abort risk

---

## Replica Set vs Sharded Transactions

| | Replica set | Sharded (4.2+) |
|--|-------------|----------------|
| Coordination | Single primary | mongos + shards |
| Latency | Lower | Higher |
| Failure modes | Primary step-down abort | Cross-shard rollback |

---

## Performance Cost

> "**Transactions aren't free** — locks/abort retries + replication sync perceived lag."

Design tricks:

- Prefer **single-document atomic operations** (`findOneAndUpdate`, `$inc`) when enough
- Embed related state **same doc** if bounded
- Keep TX **short** — minimal work inside
- Avoid TX for **high throughput hot paths** if possible

---

## Read Concern

> "**How fresh / durable** read data you're willing to accept."

| Level | Meaning (simplified) |
|-------|----------------------|
| **local** | Read latest local node state — may be rolled back if primary fails |
| **majority** | Data committed to **majority** of replica set — durable committed |
| **linearizable** | Strong for primary reads — expensive — specific uses |
| **available** | Sharded secondaries — dirty reads possible (deprecated/changed — check version) |

### Typical pairs

- Money-sensitive after commit read: **`majority`** read concern + **`majority`** write concern
- Analytics latency-first: sometimes **`local`** acceptable

---

## Write Concern

```javascript
{ w: 1 }           // acknowledged by primary only
{ w: "majority" }  // journaled to majority voting nodes
{ w: 0 }           // fire-and-forget (unsafe for critical paths)

{ w: "majority", j: true }   // wait journal flush (when enabled)
```

### Tradeoff

| Stricter | Benefit | Cost |
|----------|---------|------|
| `majority` | Durability | Latency |

---

## Causal Consistency

> "**Session** guarantees: reads reflect **causally related** writes order."

```javascript
// Pseudocode — drivers set causal consistency on session
const session = client.startSession({ causalConsistency: true })
```

Use case:

- User creates doc → immediately reads list — must see own write **without stale secondary miss**.

---

## Retryable Writes / Reads

> "**Drivers auto-retry** certain operations once after transient errors (network blip, primary election)."

- Enabled default modern drivers + replica sets
- Idempotent operations safer (`insertOne` with generated `_id` tricky — driver handles known cases)

### Transactions + retry

Application pattern:

```javascript
// retry whole transaction on TransientTransactionError label (driver-specific)
```

---

## Practical Patterns

### Inventory decrement (avoid oversell)

Prefer **atomic single-doc** when possible:

```javascript
db.inventory.updateOne(
  { sku: "X", qty: { $gte: 1 } },
  { $inc: { qty: -1 } }
)
// matchedCount === 1 → success
```

Multi-SKU checkout → **transaction** across lines.

### Saga vs TX

Long business workflows → **outbox / saga** pattern instead long Mongo TX.

---

## Pitfalls

1. **Long transactions** — aborted / performance killer.
2. **Ignoring write concern** — "committed" in app but lost on failover.
3. **Secondary reads** without understanding **staleness**.
4. **DDL inside TX** — failures confusing.
5. **Spring-style @Transactional overuse** — hides latency.
6. **Sharded TX everywhere** — bottleneck mongos coord.

---

## Cheat Sheet

| API | Role |
|-----|------|
| startSession | Session |
| startTransaction | Begin |
| commitTransaction | OK |
| abortTransaction | Rollback |

| Read concern | Durability |
|----------------|------------|
| local | Fast, possible rollback window |
| majority | Committed durable |

| Write concern | Meaning |
|---------------|---------|
| w:1 | Primary ack |
| majority | Replication quorum |

---

## Practice

1. Implement **transfer balance** between two `accounts` docs with TX.
2. Same transfer with **two atomic updates** without TX — race demo.
3. Configure **`readConcern majority`** read after **`writeConcern majority`** write — discuss latency.
4. Explain **causal consistency** with secondary reads scenario.
5. When **retryable writes** won't save duplicate inserts?
