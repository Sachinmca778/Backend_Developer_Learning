# Caching Strategies

## Status: Complete

---

## Table of Contents

1. [Why Cache](#why-cache)
2. [Where to Cache (Layers)](#where-to-cache-layers)
3. [Cache-Aside (Lazy Loading)](#cache-aside-lazy-loading)
4. [Read-Through](#read-through)
5. [Write-Through](#write-through)
6. [Write-Behind (Write-Back)](#write-behind-write-back)
7. [Refresh-Ahead](#refresh-ahead)
8. [Eviction Policies](#eviction-policies)
9. [TTL & Invalidation](#ttl--invalidation)
10. [Cache Stampede](#cache-stampede)
11. [Negative Caching](#negative-caching)
12. [Multi-Tier Cache](#multi-tier-cache)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Cache

> "**Cache = save the result of expensive work** — DB query, computed value, API call — in **fast storage** so next request skips the work."

Wins:

- **Latency**: 100 µs cache vs 10 ms DB = **100× faster**
- **Throughput**: protect DB from repeated identical work
- **Cost**: fewer DB cycles → smaller DB instances
- **Resilience**: cache absorbs spikes, DB stays healthy

> "Two hardest things in CS: **cache invalidation** and naming things." — Phil Karlton

---

## Where to Cache (Layers)

```
[Browser]   ← HTTP cache headers
   ↓
[CDN edge]  ← CloudFront / Cloudflare
   ↓
[Reverse proxy]  ← Nginx / Varnish (page cache)
   ↓
[App in-process cache]   ← Caffeine / Guava / Map
   ↓
[Distributed cache]      ← Redis / Memcached
   ↓
[Database query cache + buffer pool]
```

> Best caches are **closest to user**. Most savings come from **CDN + Redis**.

---

## Cache-Aside (Lazy Loading)

> "**App is the boss.** Reads check cache; on miss → query DB → populate cache. Most common pattern."

```
read(key):
  v = cache.get(key)
  if v is not None:
    return v                 # cache hit
  v = db.query(key)          # cache miss
  cache.set(key, v, ttl=300)
  return v

write(key, v):
  db.update(key, v)
  cache.delete(key)          # OR cache.set(key, v) — depends on strategy
```

### Pros

- **Simple** — cache failure doesn't break reads (still fall through to DB)
- **Only requested data cached** — no waste
- **Resilient** — cache restart only causes cold-cache slowdown

### Cons

- **Cold cache latency** — first request after deploy / restart slow
- **Stale risk** — if write doesn't invalidate, cache may serve old data
- **Cache stampede** — many concurrent misses → DB hammered

### Use when

- General-purpose application caching (default choice)
- Read-heavy workloads with reasonable hit ratio

---

## Read-Through

> "**Cache is the boss.** App always asks the cache; cache itself fetches from DB on miss. Cache library handles loading."

```
read(key):
  return cache.get(key, loader=db.query)
```

Examples: **Caffeine `LoadingCache`**, **Hibernate L2**, **Guava `CacheLoader`**, **Redis with cache-loader pattern**.

### Pros

- **Cleaner app code** — no manual fall-through logic
- Loader logic in **one place**
- Built-in **single-flight** in many libs (one DB call even if 100 misses arrive)

### Cons

- **Tight coupling** to cache library
- **Cache provider needs DB connectivity** (config / network)

### vs Cache-Aside

- Cache-aside: app code orchestrates
- Read-through: cache layer orchestrates

> In practice, "read-through" with a **library like Caffeine** is just a tidy version of cache-aside.

---

## Write-Through

> "**Writes go through the cache to the DB, synchronously.** Cache always has fresh data."

```
write(key, v):
  cache.set(key, v)
  db.update(key, v)          # both succeed before return
```

### Pros

- **Cache always consistent** with DB
- No "stale cache" risk on subsequent reads

### Cons

- **Slower writes** — two stores instead of one
- **Cache full of write-only data** that may never be read (waste)

### Use when

- **Write-then-immediately-read** workflows (you'll read soon, so populating now saves a miss)
- Combined with **TTL** to evict cold writes

### Hybrid

- Write-through for **hot data**, cache-aside for everything else

---

## Write-Behind (Write-Back)

> "**Writes go to cache only**; cache **asynchronously batches** them to DB. Fastest writes — riskiest durability."

```
write(key, v):
  cache.set(key, v)
  cache.queue_for_db(v)   # background worker writes to DB

background:
  every N ms: flush queue to DB (batched)
```

### Pros

- **Very fast writes** — DB latency hidden
- **Batching** — combine many writes into one DB call (huge throughput win)
- Absorbs write spikes

### Cons

- **Data loss risk** — cache crash before flush = lose writes
- **Complexity** — durability requires WAL on cache, retry, ordering
- **Eventual consistency** — DB lags cache momentarily

### Use when

- **Metrics / counters / non-critical aggregates**
- **Click streams** before flushing to data warehouse
- **Bulk write batching** for hot keys

### Don't use when

- **Money / orders / source-of-truth data**

---

## Refresh-Ahead

> "**Predict expiry**: refresh hot keys **before TTL** so they don't go stale → no cache miss latency."

```
On read of key with TTL < 20% remaining → trigger background refresh
```

### Pros

- **Always-warm** for popular keys
- No "cliff" miss when TTL expires

### Cons

- **Wasted refreshes** for keys not actually re-requested
- Implementation complexity

### Use when

- **Predictable popular keys** (top SKUs, trending posts)
- **Latency budget very tight**

---

## Eviction Policies

> "**Cache memory is finite.** When full, which entry to evict?"

| Policy | Logic | Use |
|--------|-------|-----|
| **LRU** (Least Recently Used) | Evict least recently accessed | **Default for most caches** — works well for general workloads |
| **LFU** (Least Frequently Used) | Evict least frequently accessed | Better when access **frequency** stable; worse for "trending then forgotten" patterns |
| **FIFO** | Evict oldest insert (regardless of use) | Rarely best, but simple |
| **Random** | Evict random entry | Surprisingly OK; constant cost |
| **TTL-based (expiration)** | Evict by absolute time, not access | Great for time-bound data (sessions, OTP) |
| **TinyLFU + LRU window** (Caffeine) | Modern hybrid — best general-purpose | Used in **Caffeine** (Java) — beats pure LRU |

### Redis specifics

```
maxmemory-policy options:
  noeviction         — error on write when full (safe; can break app)
  allkeys-lru        — evict any key by LRU (pure cache)
  volatile-lru       — evict only keys with TTL by LRU
  allkeys-lfu        — LFU
  volatile-lfu
  allkeys-random
  volatile-random
  volatile-ttl       — evict TTL-soonest key
```

> For pure cache use case → `allkeys-lru` or `allkeys-lfu`.

---

## TTL & Invalidation

### TTL (Time-To-Live)

> "**Auto-expire** entries after N seconds. Fire-and-forget freshness."

Choose TTL:

- **Hot/short-lived data** (rate limits, session tokens) — seconds to minutes
- **Frequently changing entities** — 1–5 min
- **Mostly static reference data** — hours
- **Strong consistency need** — short TTL or explicit invalidation

### Invalidation patterns

| Pattern | When |
|---------|------|
| **TTL only** | Eventual consistency OK |
| **Write-through update** | Cache consistent with each write |
| **Delete on write** (cache-aside) | Force next read to reload |
| **Pub/Sub invalidation** | Multi-instance: notify all caches to evict (Redis Pub/Sub, change streams) |
| **Tag-based invalidation** | Tag entries; invalidate by tag (CDN style — **Varnish**, **Cloudflare cache tags**) |

### Stale-while-revalidate

> Serve **stale** data immediately, refresh in background. Used by **CDNs**, **Next.js**, **service workers**. Hides revalidation latency.

---

## Cache Stampede

> "**Many concurrent misses** for same hot key → all hit DB simultaneously → DB collapses."

### Mitigations

| Technique | How |
|-----------|-----|
| **Single-flight / mutex** | Only one request fetches; others wait | (most common) |
| **Probabilistic early expiration** | Each request independently considers refreshing as TTL approaches | Avoids synchronized expiry |
| **Lock + double-check** | First miss takes lock, computes, populates; others retry | Implementations: **Redis SETNX** lock |
| **Background refresh** | Refresh before expiry (refresh-ahead) | Avoids miss entirely |
| **Stale-while-revalidate** | Serve stale, refresh async | Smooths spikes |

### Single-flight pseudocode

```python
def get_cached(key):
    v = cache.get(key)
    if v is not None: return v
    if cache.set_if_absent(f"lock:{key}", "1", ttl=10):
        try:
            v = db.query(key)
            cache.set(key, v, ttl=300)
            return v
        finally:
            cache.delete(f"lock:{key}")
    else:
        time.sleep(0.05)
        return get_cached(key)   # retry
```

---

## Negative Caching

> "**Cache the absence of data.** If `user:42` doesn't exist, cache `null` (briefly) so next miss doesn't re-query DB."

```
read(key):
  v = cache.get(key)
  if v is SENTINEL_NULL: return None
  if v is not None: return v
  v = db.query(key)
  if v is None:
    cache.set(key, SENTINEL_NULL, ttl=60)   # negative cache short TTL
  else:
    cache.set(key, v, ttl=300)
  return v
```

### Why

- Prevent **cache penetration** — attacker requests random non-existent IDs → all miss → all hit DB

### Caution

- **Short TTL** for negative cache — entity may be created soon
- Combine with **Bloom filter** for very high traffic (probabilistic "definitely not present")

---

## Multi-Tier Cache

> "**Layered caches** — cheap & local first, fall through to bigger / shared."

```
L1: In-process (Caffeine, Guava)  — nanoseconds, per-instance
L2: Redis cluster                 — sub-ms, shared across instances
L3: CDN edge                      — ms, global
L4: DB (with its own buffer pool) — last resort
```

### Trade-offs

| Tier | Pros | Cons |
|------|------|------|
| **L1 (in-proc)** | Fastest | Per-instance — invalidation hard, memory bound to JVM |
| **L2 (Redis)** | Shared, large | Network hop |
| **CDN** | Global, free egress | Static / cacheable HTTP only |

### Invalidation across L1

- Use **Redis Pub/Sub** to broadcast "evict key X" to all app instances
- Or **short L1 TTL** (10–30 sec) and accept slight inconsistency

---

## Pitfalls

1. **No TTL** → stale data forever or memory bloat.
2. **Cache stampede** ignored → DB falls over on hot key expiry.
3. **Cache consistency assumed** with cache-aside (write doesn't evict).
4. **Storing huge objects** in Redis → blocks single thread.
5. **`KEYS *` in production** → blocks Redis; use `SCAN`.
6. **Wrong eviction policy** (`noeviction` in pure cache) → write errors.
7. **Caching write-once data** that's never read again → memory waste.
8. **No metrics** — hit rate, miss rate, eviction rate invisible.
9. **Negative cache too long** → newly created entity invisible to users.
10. **Single Redis node** = SPOF — use cluster + replicas.
11. **Cache-coupled tightly to schema** — hard to evolve.
12. **Race on cache-aside delete** — old read overwrites new value (TTL helps).

### Race condition example

```
t1: write key=v2 to DB
t2: cache.delete(key)
t3: another reader gets cache miss, reads DB → sees v2 (good)
   OR
t1: reader sees cache miss, reads DB v1 (in flight)
t2: writer updates DB to v2 + deletes cache
t3: reader writes v1 to cache (stale!)  ← problem
```

Mitigation: **short TTL** + **delete after small delay** (delayed double delete) or **versioned cache key**.

---

## Cheat Sheet

| Pattern | Tradeoff |
|---------|----------|
| **Cache-aside** | Default. Resilient. App orchestrates. |
| **Read-through** | Cleaner app. Cache loads. |
| **Write-through** | Cache always fresh. Slower writes. |
| **Write-behind** | Fast writes. Risk durability. |
| **Refresh-ahead** | Avoid TTL miss cliff. |

| Eviction |  |
|----------|--|
| **LRU** | Default general |
| **LFU** | Stable frequency |
| **TTL** | Time-bound data |
| **TinyLFU** (Caffeine) | Modern best general |

| Problem | Solution |
|---------|----------|
| Stampede | Single-flight / lock / refresh-ahead / SWR |
| Penetration (missing keys) | Negative cache + Bloom filter |
| Stale read | Short TTL + invalidation |
| Hot key | Multi-tier, replicate, hash + salt |
| Memory bloat | TTL + eviction policy |

---

## Practice

1. Implement **cache-aside** with single-flight lock for a `user:{id}` lookup.
2. Configure Redis with `maxmemory-policy=allkeys-lru` and 1 GB cap; load 2 GB; observe evictions.
3. Add **negative cache** for non-existent user IDs (60 sec TTL).
4. Compare **TTL=60s + cache-aside** vs **write-through** for an entity that updates 1000×/min.
5. Implement Redis Pub/Sub broadcast to invalidate **L1 in-process cache** across N instances.
6. Build a **stale-while-revalidate** wrapper around an HTTP API call.
