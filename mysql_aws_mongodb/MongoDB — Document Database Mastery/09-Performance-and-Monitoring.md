# Performance & Monitoring

## Status: Complete

---

## Table of Contents

1. [Performance Triangle](#performance-triangle)
2. [currentOp & killOp](#currentop--killop)
3. [explain("executionStats")](#explainexecutionstats)
4. [Collection & Index Stats](#collection--index-stats)
5. [mongostat](#mongostat)
6. [mongotop](#mongotop)
7. [Atlas Performance Advisor](#atlas-performance-advisor)
8. [Connection Pooling](#connection-pooling)
9. [Logs & Profiler](#logs--profiler)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## Performance Triangle

> "**Schema + Indexes + Queries** — teeno milke throughput decide karte hain; sirf hardware bump blind spot."

Checklist:

1. Query **uses index**? (`explain`)
2. **Working set** fits RAM?
3. **Hot keys** / **large docs** churn?
4. **Pool** sizing sane?

---

## currentOp & killOp

### See running operations

```javascript
db.currentOp({
  $or: [
    { op: "query", "command.find": { $exists: true } },
    { op: "getmore" },
    { op: "insert" },
    { op: "update" },
    { op: "remove" },
    { op: "command" }
  ]
})
```

Filter active long ops:

```javascript
db.currentOp({
  active: true,
  secs_running: { $gt: 5 }
})
```

### Kill operation

```javascript
db.killOp(opId)   // opid from currentOp output
```

⚠️ Internal ops kill dangerous — target **user queries** carefully.

---

## explain("executionStats")

```javascript
db.orders.find({ userId: uid, status: "OPEN" }).explain("executionStats")
```

### Key fields

| Field | Meaning |
|-------|---------|
| **executionStages.stage** | **COLLSCAN** vs **IXSCAN** vs **FETCH** |
| **nReturned** | Docs returned to client |
| **totalKeysExamined** | Index keys scanned |
| **totalDocsExamined** | Documents examined |
| **executionTimeMillis** | Wall clock (relative) |

### Healthy-ish shape

> **`nReturned` ≈ `totalDocsExamined`** when IXSCAN selective — big gap → index not selective or in-memory sort blowup.

### Aggregation explain

```javascript
db.orders.explain("executionStats").aggregate([...])
```

---

## Collection & Index Stats

```javascript
db.orders.stats()
db.orders.stats({ scale: 1024 })   // KB

// WiredTiger cache / general server stats
db.serverStatus()
```

Shows:

- Collection size
- Storage size
- Index sizes (`totalIndexSize`)
- avgObjSize

---

## mongostat

```bash
mongostat --uri "mongodb://host:27017" 2
```

Columns include:

- **qr/qw** — queued reads/writes (pressure signal)
- **conn** — connections
- **dirty** — cache dirty percent (WiredTiger)

---

## mongotop

```bash
mongotop --uri "mongodb://host:27017" 5
```

> "**Per-collection read/write time** snapshot — find hot collections."

---

## Atlas Performance Advisor

> "**Managed Atlas** suggests missing indexes from slow queries sample."

Still verify:

- Index **cardinality**
- **Write amplification**
- **Partial** index possibility

---

## Connection Pooling

> "**Driver maintains pool** — default **`maxPoolSize` ~100** (driver dependent)."

Problems:

| Too small | Too large |
|-----------|-----------|
| Latency queue | RAM + conn storm on DB |

Tune with:

- App instance count × pool ≤ **`mongod` maxIncomingConnections** headroom
- Monitor **`serverStatus().connections`**

---

## Logs & Profiler

### Profiler levels

```javascript
db.setProfilingLevel(1, { slowms: 100 })
db.system.profile.find().limit(5).sort({ ts: -1 }).pretty()
```

Level **2** all ops — dev only — heavy.

---

## Pitfalls

1. **`explain`** on idle system misleading — production load differs.
2. **`killOp`** wrong id — instability.
3. **Profiler always-on level 2** — performance collapse.
4. **Micro-benchmark wrong** — network RTT dominates tiny queries.
5. **Missing projection** — shipping fat docs over network.
6. **No readiness/liveness** distinction overwhelming mongos with tiny pings oversized pool.

---

## Cheat Sheet

| Tool | Use |
|------|-----|
| currentOp | Who running |
| killOp | Stop op |
| explain | Plan analysis |
| stats | Size/index footprint |
| mongostat | Live counters |
| mongotop | Hot collections |
| profiler | Slow queries |
| Pool maxPoolSize | App tuning |

---

## Practice

1. Find **COLLSCAN** query → add compound index → compare `totalDocsExamined`.
2. Simulate **pool exhaustion** symptom — metric watch `queues`.
3. Interpret **`qr`** spike during traffic surge.
4. Use **`explain`** on aggregation with `$lookup` — cost awareness.
5. Define SLIs for Mongo layer (latency p95, replication lag).
