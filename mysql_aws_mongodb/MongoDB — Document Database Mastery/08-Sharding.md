# Sharding

## Status: Complete

---

## Table of Contents

1. [Why Shard](#why-shard)
2. [Architecture](#architecture)
3. [Shard Key](#shard-key)
4. [Hashed vs Ranged Shard Key](#hashed-vs-ranged-shard-key)
5. [Chunks & Balancer](#chunks--balancer)
6. [mongos Routing](#mongos-routing)
7. [sh.status()](#shstatus)
8. [Zone Sharding](#zone-sharding)
9. [Indexes Per Shard](#indexes-per-shard)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## Why Shard

> "**Vertical scale limit** — single machine RAM/disk/CPU band — **horizontal partition** data across shards."

When:

- Dataset **TB+** or growth trajectory huge
- Write throughput **single primary** saturated
- Operational isolation per tenant/geo needed (**zones**)

---

## Architecture

```
           mongos (router)
                |
    +-----------+-----------+
    |           |           |
 shard A       shard B     shard C
(replica set) (rep set)   (rep set)

Config Servers (replica set) — chunk metadata
```

- Each **shard** = typically **replica set** (not single mongod prod)
- **mongos** stateless — scale horizontally
- **Config servers** hold namespace/chunk maps

---

## Shard Key

> "**Indexed field(s)** chosen at collection shard time — immutable for collection."

```javascript
sh.shardCollection("shop.orders", { userId: 1 })

// hashed shard key
sh.shardCollection("shop.events", { _id: "hashed" })
```

### Good shard key properties

| Property | Want |
|----------|------|
| **Cardinality** | High — many distinct values |
| **Frequency** | Even distribution — no mega-key hotspot |
| **Monotonic** | **Avoid** monotonic increasing only key on ranged sharding — **write hotspot** last chunk |

### Bad examples

- **`createdAt` ascending alone** ranged → all inserts hit **last chunk** → single shard hot
- **Low cardinality** `country` only → few chunks busy

### Mitigation

- **Compound shard key** with high-card prefix
- **Hashed shard key** on `_id` or field — spreads writes — loses efficient range queries on that field

---

## Hashed vs Ranged Shard Key

| | Ranged | Hashed |
|--|--------|--------|
| Distribution | Risk hotspot monotonic | Even spread typical |
| Range queries | Targeted shards | Often **scatter gather** all shards |
| Sort | Range-friendly | Not for hashed field |

Choose by **dominant query pattern**.

---

## Chunks & Balancer

> "**Chunk** = contiguous shard key range subset — moved between shards for balance."

- Default chunk size ~ **64 MB** (configurable history — verify cluster setting)
- **Balancer** migrates chunks from busy → idle shards

### Jumbo chunks

Uneven splittable failures → **jumbo chunk** stuck — manual intervention nightmare — avoid bad shard key.

---

## mongos Routing

1. Query includes **shard key** equality → **targeted** shards
2. Query no shard key → **broadcast** — all shards — expensive

```javascript
db.orders.find({ userId: "x" })      // targeted if sharded { userId: 1 }
db.orders.find({ status: "OPEN" })   // scatter if no shard key in predicate
```

---

## sh.status()

```javascript
sh.status()
```

Shows:

- Shards list
- Balancer state
- Chunk counts per shard
- Database/collection sharding status

---

## Zone Sharding

> "**Tag-aware sharding** — shard key ranges pinned to zones (regions, tiers)."

Use cases:

- **GDPR** — EU data EU shards only
- **Multi-tenant premium** — tenant subset dedicated hardware

```javascript
sh.addShardTag("shard0001", "EU")
sh.updateZoneKeyRange(
  "shop.users",
  { region: "EU", _id: MinKey },
  { region: "EU", _id: MaxKey },
  "EU"
)
```

→ Requires careful **shard key design** including discriminant field (`region`).

---

## Indexes Per Shard

Each shard maintains **own indexes** — deploy index builds coordinated (rolling).

---

## Pitfalls

1. **Wrong shard key immutability** — painful resharding later (6.0+ features evolving — still heavy).
2. **Scatter queries** — forgotten shard key in queries.
3. **Hot shard** — ObjectId-only ranged shard key anti-pattern.
4. **Small cluster premature optimization** — ops overhead before need.
5. **Transactions cross-shard** — latency + limits.
6. **Global uniqueness** — unique index constraints across shards extra care (unless localized by shard key prefix patterns).

---

## Cheat Sheet

| Piece | Role |
|-------|------|
| mongos | Router |
| Config RS | Metadata |
| Shard RS | Data partition |
| Shard key | Partition discriminator |
| Chunk | Key range slice |
| Balancer | Moves chunks |
| Zone | Data locality rules |

| Strategy | Writes | Range query |
|----------|--------|-------------|
| Ranged | Hotspot risk mono | Good |
| Hashed | Even | Poor targeting |

---

## Practice

1. Identify **hotspot** scenario: timestamps ascending ranged shard key.
2. Query plan difference targeted vs scatter — examples.
3. Design **GDPR zone** with `region` in shard key tradeoffs.
4. Read **`sh.status()`** output sketch interpretation.
5. When **not** to shard yet?
