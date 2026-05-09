# Database Sharding

## Status: Complete

---

## Table of Contents

1. [Why Shard](#why-shard)
2. [Sharding vs Partitioning vs Replication](#sharding-vs-partitioning-vs-replication)
3. [Shard Key — The Most Important Decision](#shard-key--the-most-important-decision)
4. [Range-Based Sharding](#range-based-sharding)
5. [Hash-Based Sharding](#hash-based-sharding)
6. [Consistent Hashing](#consistent-hashing)
7. [Directory / Lookup Sharding](#directory--lookup-sharding)
8. [Geographic Sharding](#geographic-sharding)
9. [Cross-Shard Joins](#cross-shard-joins)
10. [Distributed Transactions & Saga](#distributed-transactions--saga)
11. [Rebalancing](#rebalancing)
12. [Hotspot Avoidance](#hotspot-avoidance)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Shard

> "**Single-server vertical scale ka ceiling hai** — RAM, CPU, disk, network — sab finite. Shard karke **horizontal scale** karte hain — N servers, each holds 1/N of data."

When to shard:

- Dataset > **single server disk** capacity
- Write throughput > **single primary** capacity
- Working set > **single server RAM** → constant disk I/O
- Backup / restore times **too long** on monolithic DB

When NOT to shard:

- Below ~**1 TB** and **<10K QPS** → vertical scale + read replicas + cache usually enough
- "We might need it later" — premature sharding = ops nightmare

> "**Shard last, not first.**"

---

## Sharding vs Partitioning vs Replication

| Concept | Meaning |
|---------|---------|
| **Partitioning** | Split a table within **single DB** (Postgres `PARTITION BY`, MySQL partitions) — same machine |
| **Sharding** | Split data across **multiple DB servers** — different machines |
| **Replication** | **Same data** copied to multiple servers (HA / read scale) |

> Sharding = scale **writes/storage**. Replication = scale **reads/HA**. **Use both together** at large scale.

---

## Shard Key — The Most Important Decision

> "**Shard key galat = irreversible pain.** Re-sharding terabytes of live data is one of the worst engineering events."

### Properties of a good shard key

| Property | Meaning |
|----------|---------|
| **Cardinality (high)** | Many distinct values → fine-grained distribution |
| **Frequency (even)** | No "celebrity" key with 90% of traffic |
| **Monotonic (avoid)** | Sequential `id` / `timestamp` → newest writes hit one shard (hotspot) |
| **Query alignment** | Most queries should include shard key → single-shard query (fast) |

### Bad examples

- **`country_code`** — only 200 values, India alone = 30% of traffic → uneven
- **`created_at`** ascending — every new row hits last shard (hotspot)
- **`auto_increment_id`** alone — same issue

### Good examples

- **`user_id`** (hashed) — high cardinality, evenly distributed
- **`(tenant_id, document_id)`** — multi-tenant SaaS isolated per shard
- **`hash(order_id)`** — distributes order writes evenly

---

## Range-Based Sharding

> "**Split key space into contiguous ranges**, one per shard."

```
Shard 1:  user_id 1        – 10,000,000
Shard 2:  user_id 10M+1    – 20,000,000
Shard 3:  user_id 20M+1    – 30,000,000
```

### Pros

- **Range queries efficient** (`WHERE user_id BETWEEN 1M AND 5M` = 1 shard)
- Easy to understand + debug

### Cons

- **Hotspot risk** — sequential IDs / timestamps → newest writes all hit last shard
- **Uneven distribution** if data clusters (e.g., dense user IDs in some range)
- Manual range adjustment when one shard outgrows others

### Use when

- Range queries dominate (analytics on time windows)
- Key distribution is naturally even
- Examples: Bigtable, HBase, MongoDB ranged sharding

---

## Hash-Based Sharding

> "**Hash the shard key** → modulo number of shards → assign to shard."

```
shard = hash(user_id) % N_SHARDS
```

### Pros

- **Even distribution** — randomization spreads load
- No hotspot from monotonic IDs
- Simple to implement

### Cons

- **Range queries scatter** — must query all shards (`WHERE user_id BETWEEN ...`)
- **Re-sharding pain** — change `N` → most keys move

### Use when

- Workload is **point lookups** (`WHERE user_id = X`)
- Even write distribution is critical
- Examples: DynamoDB partition key, MongoDB hashed sharding

---

## Consistent Hashing

> "**Solves the rehash-everything problem.** Place shards on a virtual ring; key hashes to a position; goes to next shard clockwise. Adding/removing shard moves only ~1/N of keys."

```
   ┌── Shard A
   │
   │     hash(key) → next clockwise shard
   │
   ├── key1 → A
   │
   ├── Shard B
   │
   ├── key2 → B
   │
   └── Shard C  ← add here, only keys between B and old C move
```

### Virtual nodes (vnodes)

- Each physical shard maps to **many positions** on the ring
- Smooths out distribution when ring is sparse
- Cassandra default: **256 vnodes per node**

### Pros

- Adding/removing shards moves ~**1/N** of keys (vs ~all in plain hashing)
- Foundation of **Cassandra**, **DynamoDB internals**, **Riak**, **memcached clients (Ketama)**

### Cons

- Slightly more complex to implement
- Range queries still scatter

### Use when

- **Dynamic cluster** — nodes added/removed often
- **Cache layer** with multiple servers (memcached/Redis cluster)
- Examples: Cassandra, DynamoDB, Riak, Couchbase

---

## Directory / Lookup Sharding

> "**Lookup table** maps key → shard. Flexible but introduces a service to query."

```
Lookup Service:
  user_id 1..1M    → shard A
  user_id 1M..3M   → shard B
  user_id 3M..    → shard C  (and dynamically adjustable)
```

### Pros

- **Maximum flexibility** — move any key to any shard, any time
- **Heterogeneous shards** — premium tenants on dedicated hardware
- Fine-grained control for compliance / locality

### Cons

- **Lookup service = SPOF / bottleneck** — must be highly available + cached
- Extra hop on every request (mitigate with client-side cache)

### Use when

- **Multi-tenant SaaS** with VIP customers needing dedicated shards
- **Compliance** requirements (per-region data placement)
- Examples: many tenancy-aware platforms, custom systems

---

## Geographic Sharding

> "**Shard by location** — EU users on EU shards, IN users on IN shards."

### Pros

- **Low latency** for users (data near them)
- **Data residency / compliance** (GDPR, RBI)
- **Independent regional failure domains**

### Cons

- **Cross-region operations** expensive
- **Migration of users** crossing regions painful
- Schema and code complexity

### Use when

- Globally distributed user base
- Strict data residency law
- Examples: WhatsApp, Uber regional databases, MongoDB Zone Sharding

---

## Cross-Shard Joins

> "**Avoid them.** Joins across shards = N round-trips → slow + complex."

### Strategies

| Strategy | How |
|----------|-----|
| **Co-locate** | Shard related entities on **same key** (orders + items both by `user_id`) |
| **Denormalize** | Embed needed fields (Extended Reference pattern) |
| **App-level join** | Fetch from each shard, merge in app — OK for small fan-out |
| **Materialized view** | Pre-aggregate cross-shard data (CDC → search index / analytics DB) |
| **Scatter-gather** | Query all shards in parallel, merge — last resort |

### Example

```
BAD:   join(orders sharded by order_id, users sharded by user_id)
GOOD:  shard orders by user_id too → join inside one shard
```

---

## Distributed Transactions & Saga

> "**Cross-shard ACID transactions are slow + fragile.** 2PC works but blocks. Most modern systems use **saga** pattern."

### 2PC (Two-Phase Commit)

```
Coordinator → Prepare (all shards) → Commit / Abort
```

- Pros: ACID
- Cons: blocking on coordinator failure, latency, scale-killer

### Saga pattern

> "**Local transaction per service** + **compensating transactions** if a step fails."

```
Place Order (shard 1)
  → Reserve Inventory (shard 2)
    → Charge Payment (shard 3)
      → Send Confirmation
   ↓ on failure: compensate backwards (refund, release inventory, cancel order)
```

Two flavors:

- **Choreography** — services emit events, others react (event-driven, decentralized)
- **Orchestration** — central orchestrator drives steps (e.g., Step Functions, Temporal)

### Use cases

- **E-commerce checkout** (saga) — multi-step with compensations
- **Microservices workflows** — cross-service state machines

---

## Rebalancing

> "**Adding/removing shards** moves data. Without consistent hashing, this is brutal."

### Strategies

| Strategy | Movement |
|----------|----------|
| **Plain hash mod N** | Most keys move when N changes |
| **Consistent hashing** | ~1/N of keys move |
| **Pre-split + virtual shards** | Many small "logical" shards mapped to physical nodes; rebalance at logical level |

### Operational pattern

1. Add new node
2. **Background copy** of relevant key ranges (or vnode reassign)
3. Read from old + new during transition
4. Cut over writes
5. Decommission old assignment

> Modern systems (Cassandra, DynamoDB, MongoDB sharded) automate this. DIY sharded MySQL = manual misery.

---

## Hotspot Avoidance

> "**Hotspot** = one shard handles disproportionate traffic. Even small skew kills your design."

### Common hotspots

- **Monotonic shard key** (timestamp, auto-increment id) → newest writes one shard
- **Celebrity user** in social app with 100M followers
- **Single tenant** with 90% of multi-tenant traffic
- **Trending product / SKU** during sale

### Mitigations

| Mitigation | Example |
|------------|---------|
| **Hash shard key** | Avoid sequential pattern |
| **Compound shard key** | `(user_id, timestamp)` — distributes user's data across time |
| **Salting** | Prefix key with random bucket: `0_`, `1_`, ..., `9_` → spreads across 10 partitions |
| **Cache hot keys** | Read traffic absorbed by Redis |
| **Pre-aggregate** | Counter sharding — track in 100 sub-counters, sum on read |
| **Dedicated shard** | Move VIP / hot tenant to own shard via directory |

### Salting example

```
post_id  → original key
salted   → "5_post_id" (random 0–9 prefix)
write    → 10 partitions instead of 1
read     → must query all 10 (acceptable for hot keys)
```

---

## Pitfalls

1. **Sharding too early** — operational pain before need.
2. **Wrong shard key** — re-sharding TBs is engineering trauma.
3. **Cross-shard joins** in critical path — latency + complexity.
4. **Distributed TX everywhere** — 2PC bottleneck.
5. **Ignoring hotspots** — average load fine but one shard dies.
6. **No rebalancing strategy** — adding nodes = ten thousand moves.
7. **Sharding by `created_at`** — guaranteed write hotspot.
8. **Too few shards** initially — re-shard soon.
9. **Too many tiny shards** — operational overhead, more failure points.
10. **Forgetting backups + restore** scale separately per shard.
11. **No monitoring per shard** — uneven load invisible.

---

## Cheat Sheet

| Strategy | Best for | Tradeoff |
|----------|----------|----------|
| **Range** | Range queries | Hotspot risk |
| **Hash** | Even distribution | Range queries scatter |
| **Consistent hash** | Dynamic cluster | Slightly complex |
| **Directory** | Flexible / multi-tenant | Lookup SPOF |
| **Geographic** | Locality / compliance | Cross-region cost |

| Pain | Fix |
|------|-----|
| Hotspot | Hash + salting + cache |
| Cross-shard join | Co-locate + denormalize |
| Cross-shard TX | Saga pattern |
| Re-shard cost | Consistent hashing + vnodes |
| Ad-hoc query | Materialized view / search index |

---

## Practice

1. Choose shard key for: e-commerce orders. Justify (cardinality, hotspot, query alignment).
2. Convert a hot timestamp shard key into a compound key avoiding hotspot.
3. Design a salted counter for a viral post's likes.
4. Sketch a saga for an Uber ride: request → match → start → complete → pay → rate.
5. Compare consistent hashing vs plain modulo when scaling from 4 → 5 shards.
6. Multi-tenant SaaS with one customer at 80% load — propose directory sharding plan.
