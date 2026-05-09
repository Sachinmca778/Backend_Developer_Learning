# ElastiCache (Redis & Memcached)

## Status: Complete

---

## Table of Contents

1. [What is ElastiCache](#what-is-elasticache)
2. [Redis vs Memcached on AWS](#redis-vs-memcached-on-aws)
3. [Redis Data Structures](#redis-data-structures)
4. [Persistence: RDB & AOF](#persistence-rdb--aof)
5. [Pub/Sub & Streams](#pubsub--streams)
6. [Lua Scripting](#lua-scripting)
7. [Cluster Mode (Sharding)](#cluster-mode-sharding)
8. [Replication](#replication)
9. [Global Datastore](#global-datastore)
10. [ElastiCache Serverless](#elasticache-serverless)
11. [Common Use Cases](#common-use-cases)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## What is ElastiCache

> "**Managed Redis or Memcached** — in-memory data store. AWS handles install, patch, backup, replication, failover."

Use for:

- **Sub-millisecond latency** reads
- **Session store**, **cache**, **rate limiter**, **leaderboard**, **pub/sub**, **queue**
- **Reduce DB load** for hot data

Two engines:

- **Redis** (recommended for almost all use cases)
- **Memcached** (legacy, simpler)

> Note: AWS also offers **MemoryDB for Redis** — Redis-compatible, durable storage (multi-AZ transactional log). Use when you need cache + system-of-record durability.

---

## Redis vs Memcached on AWS

| | **Redis** | **Memcached** |
|--|-----------|----------------|
| Data structures | **String, Hash, List, Set, ZSet, Stream, Bitmap, HyperLogLog, Geo** | String only |
| Persistence | **RDB snapshots, AOF log** | None — pure cache |
| Replication | **Yes** (multi-AZ failover) | No |
| Cluster mode (sharding) | **Yes** | Yes (client-side hashing) |
| Multi-thread | Single-thread (mostly) | **Multi-threaded** (better per-node CPU usage) |
| Pub/Sub | **Yes** | No |
| Lua scripting | **Yes** | No |
| Transactions | **MULTI/EXEC** | No |
| TLS | Yes | Yes (newer versions) |
| Best for | Almost everything | Pure ephemeral cache, multi-thread perf |

> Default choice: **Redis**. Memcached only if you specifically need its multi-thread simplicity for pure cache.

---

## Redis Data Structures

| Type | Example use |
|------|-------------|
| **String** | Cached HTML, counter (`INCR`), token storage |
| **Hash** | User profile fields (`HSET user:42 name "S" age 25`) |
| **List** | Recent activity feed, lightweight queue |
| **Set** | Unique tags, online users |
| **Sorted Set (ZSET)** | **Leaderboard**, time-series of events |
| **Stream** | Append-only log (Kafka-lite) for events / pub/sub durable |
| **Bitmap** | User activity bitmaps (1 bit per user per day) |
| **HyperLogLog** | Unique visitor counts (memory-efficient cardinality) |
| **Geo** | `GEOADD`, `GEOSEARCH` for location queries |

### Common commands

```
SET key value EX 300            # set with TTL 5 min
GET key
INCR counter

HSET user:42 name "S" age 25
HGETALL user:42

ZADD leaderboard 1500 "alice"
ZRANGE leaderboard 0 9 REV WITHSCORES

PFADD daily-uniques "user:42"
PFCOUNT daily-uniques
```

---

## Persistence: RDB & AOF

| | **RDB** | **AOF** |
|--|---------|---------|
| What | Periodic point-in-time **snapshot** of memory → S3 | **Append-only** log of every write op |
| Recovery | Faster (load snapshot) | Slower (replay log) |
| Data loss window | Up to snapshot interval (minutes) | <1 sec (`appendfsync everysec`) |
| Disk impact | Periodic spike | Continuous |
| ElastiCache | Backup snapshots to S3, configurable retention | Available, less commonly enabled |

### Practical

- Cache-only workloads → **persistence off** (faster, cheaper)
- Source-of-truth workloads → **MemoryDB** instead of ElastiCache (proper durability)

---

## Pub/Sub & Streams

### Pub/Sub

```
SUBSCRIBE chan-orders
PUBLISH chan-orders '{"id": 1, "total": 99}'
```

- **Fire and forget** — no persistence; offline subscribers miss messages
- Use for **transient notifications** (cache invalidation, real-time UI nudges)

### Streams (Redis 5+)

```
XADD orders * id 1 total 99
XREAD BLOCK 5000 STREAMS orders 0
XGROUP CREATE orders group1 $
XREADGROUP GROUP group1 worker1 COUNT 10 STREAMS orders >
```

- **Durable** append-only log
- **Consumer groups** like Kafka
- **At-least-once** delivery with ack (`XACK`)
- Cap with `MAXLEN ~ 1000000` to keep memory bounded

→ Use Streams for **durable pub/sub** within Redis stack; full Kafka still better at huge scale.

---

## Lua Scripting

> "**Atomic multi-step operations** — execute server-side, no roundtrip per step."

```lua
-- atomic check-and-decrement
if tonumber(redis.call('GET', KEYS[1])) > 0 then
  return redis.call('DECR', KEYS[1])
else
  return -1
end
```

```bash
EVAL "..." 1 inventory:sku-42
```

### Use for

- **Rate limiting** (token bucket all in one)
- **Atomic conditional updates**
- Reduce N round-trips → 1

---

## Cluster Mode (Sharding)

> "**Cluster Mode Enabled** — data partitioned across multiple shards (nodegroups). Each shard = primary + replicas."

```
Cluster
  ├── Shard 1 (primary + 2 replicas) — slots 0–5460
  ├── Shard 2 (primary + 2 replicas) — slots 5461–10922
  └── Shard 3 (primary + 2 replicas) — slots 10923–16383
```

### Hash slots

- Redis cluster has **16384 slots**
- Key hashed → slot → shard
- Multi-key operations require **same slot** — use **hash tags** `{user:42}:cart`

### Cluster mode disabled (single shard)

- 1 primary + up to 5 replicas
- Simpler, but limited to single-node memory ceiling

---

## Replication

| Mode | Failover | Read scale |
|------|----------|------------|
| **Single node** | None | None |
| **Cluster Mode Disabled w/ replicas** | Auto failover ~30s, multi-AZ | Yes (read endpoints) |
| **Cluster Mode Enabled** | Per-shard auto failover | Yes (per-shard read endpoints) |

### Endpoints

- **Primary endpoint** (writes)
- **Reader endpoint** (load-balances reads across replicas, single-shard mode)
- **Configuration endpoint** (cluster mode, client uses for cluster topology discovery)

---

## Global Datastore

> "**Cross-region replication** for ElastiCache Redis. Active-passive (writes to primary region, reads in secondary regions). <1s typical lag."

Use for:

- **Disaster recovery** across regions
- **Low-latency reads** from multiple regions

Not for:

- Active-active multi-region writes (use **MemoryDB** + careful design, or app-level conflict resolution)

---

## ElastiCache Serverless

> "**Auto-scales storage and compute** based on workload. No node selection. Pay per ECPUs (compute units) + GB-hour stored."

### When good

- Spiky / unpredictable workloads
- Don't want capacity planning
- Multi-tenant SaaS

### When not

- Steady high traffic — provisioned cheaper
- Need very fine cluster topology control

---

## Common Use Cases

### 1. Cache

```
GET user:42 → if MISS → DB query → SET user:42 with TTL
```

Patterns: **Cache-aside** (most common), **Write-through**, **Write-behind**.

### 2. Session store

- Stateless app servers + Redis sessions = scale horizontally
- TTL = session timeout

### 3. Rate limiting

- Token bucket / sliding window via Redis (Lua script atomic)

```lua
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local current = redis.call("INCR", key)
if current == 1 then redis.call("EXPIRE", key, window) end
if current > limit then return 0 end
return 1
```

### 4. Leaderboard

```
ZADD leaderboard 1500 "alice"
ZINCRBY leaderboard 50 "alice"
ZRANGE leaderboard 0 9 REV WITHSCORES
ZRANK leaderboard "alice"   # alice's rank
```

### 5. Pub/Sub & real-time

- Live dashboard updates
- Cache invalidation broadcast (or use Streams for durable)

---

## Pitfalls

1. **Cache stampede** — many requests cache miss simultaneously → DB hammered. Use **request coalescing** / **early refresh** / **bloom filter for negative cache**.
2. **No TTL** → memory grows until OOM eviction kicks in (or LRU evicts important data).
3. **Wrong eviction policy** — default `volatile-lru` only evicts keys with TTL; if no TTL keys, OOM. Use `allkeys-lru` for pure cache.
4. **Big keys** (multi-MB Hash, huge List) → blocks single-thread operations, latency spike.
5. **`KEYS *`** in production — blocks Redis until done. Use `SCAN` instead.
6. **Cluster mode + multi-key ops without hash tag** — `CROSSSLOT` error.
7. **Memcached for session store** — no persistence, restart wipes data.
8. **No multi-AZ replica** — single-node failure = downtime.
9. **Public access** — shouldn't be reachable from internet ever.
10. **Replication lag** during burst → reading from replica returns stale.

---

## Cheat Sheet

| Need | Use |
|------|-----|
| Cache | Redis (or Memcached if pure) |
| Session store | Redis |
| Leaderboard | Redis ZSET |
| Rate limiter | Redis + Lua |
| Real-time fan-out (transient) | Redis Pub/Sub |
| Durable in-Redis events | Redis Streams |
| HA | Replicas + multi-AZ |
| Horizontal scale | Cluster mode enabled |
| Cross-region read scale | Global Datastore |
| Spiky workload | ElastiCache Serverless |
| Cache + durability | **MemoryDB for Redis** |

---

## Practice

1. Build **cache-aside** for a `user:{id}` lookup with 5-min TTL.
2. Implement **token-bucket rate limiter** in Lua.
3. Create **leaderboard** with ZSET; add 1M scores; query top 10.
4. Set up cluster mode with **3 shards × 1 replica**; test failover.
5. Use **Streams + consumer group** for an order-events processor.
6. Replace `KEYS prefix:*` scan with `SCAN MATCH prefix:* COUNT 100`.
