# Scalability Patterns

## Status: Not Started

---

## Table of Contents

1. [Vertical vs Horizontal Scaling](#vertical-vs-horizontal-scaling)
2. [Stateless vs Stateful](#stateless-vs-stateful)
3. [Load Balancing — L4 vs L7](#load-balancing--l4-vs-l7)
4. [Load Balancing Algorithms](#load-balancing-algorithms)
5. [Consistent Hashing](#consistent-hashing)
6. [Database Scaling](#database-scaling)
7. [Sharding (Horizontal Partitioning)](#sharding-horizontal-partitioning)
8. [Read Replicas](#read-replicas)
9. [Caching Layers](#caching-layers)
10. [Cache Strategies](#cache-strategies)
11. [Auto-scaling](#auto-scaling)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Vertical vs Horizontal Scaling

### Vertical (scale up)

→ Add more CPU / RAM / disk to single machine.

| | |
|--|--|
| Pros | Simple, no code change, low latency (single hop) |
| Cons | Hardware limit, costly at top end, single point of failure |

### Horizontal (scale out)

→ Add more machines to a cluster.

| | |
|--|--|
| Pros | Linear scale, fault-tolerant, commodity hardware |
| Cons | Complexity (LB, distributed state, consistency) |

### Cost curve

```
Cost
 │     /vertical (super-linear after threshold)
 │    /
 │   /
 │  /  ___ horizontal (linear)
 │ /__/
 │/_/
 └────────── Capacity
```

→ Modern web-scale = horizontal. Vertical for DB primary in early phase.

---

## Stateless vs Stateful

### Stateless services

- No server-side session memory
- Each request self-contained
- Can route any request to any node
- **Trivially horizontally scalable**

```
Request → LB → any of [Server1, Server2, Server3]
```

### Stateful services

- Holds in-memory state (sessions, locks)
- Must route same user to same node (sticky session) OR
- Store state in shared store (Redis, DB)

### Rule of thumb

> "Stateless services scale horizontally. **Push state to data layer**."

### Where state lives

| Type | Storage |
|------|---------|
| Session | Redis / DynamoDB / signed cookie (JWT) |
| Cache | Redis / Memcached cluster |
| User data | DB |
| Files | S3 / object store |

---

## Load Balancing — L4 vs L7

### Layer 4 (Transport — TCP/UDP)

- Routes by IP + port
- Doesn't inspect payload
- **Faster** (less CPU)
- Less smart

```
Client (TCP) → L4 LB → Backend (one chosen TCP connection)
```

Examples: AWS NLB, HAProxy in TCP mode, Linux IPVS.

### Layer 7 (Application — HTTP/HTTPS)

- Inspects HTTP headers, URL, cookies
- Path-based / host-based routing
- SSL termination
- WebSocket support
- Rate limiting, auth

```
Client (HTTP) → L7 LB → Backend (per-request routing)
```

Examples: AWS ALB, Nginx, Envoy, Spring Cloud Gateway, HAProxy in HTTP mode.

### When to use which

| Need | Layer |
|------|-------|
| Pure TCP throughput | L4 |
| HTTP routing by path/host | L7 |
| WebSocket sticky | L7 (or L4 with hash) |
| TLS termination | L7 (commonly) |
| Lowest latency | L4 |

→ Cross-ref: `backend-skills/Networking-and-Protocols/04-Load-Balancing-and-Proxies.md`.

---

## Load Balancing Algorithms

| Algorithm | How |
|-----------|-----|
| **Round Robin** | Rotation through servers |
| **Weighted RR** | Weights per server (more powerful gets more) |
| **Least Connections** | Send to server with fewest active connections |
| **Least Response Time** | Pick fastest responding |
| **IP Hash** | Hash(client IP) → server (sticky) |
| **Consistent Hashing** | For distributed cache / sharding |
| **Random** | Pick random (with **two-choice** trick — pick 2 random, choose lesser-loaded) |

### Health checks

LB pings backends periodically:

```
GET /health → 200 OK every 5 sec
```

Unhealthy → temporarily removed from rotation.

→ Cross-ref: `phase-4 / Performance & Optimization / 02-Connection-Pool-Tuning.md`.

---

## Consistent Hashing

**Problem:** Naive hash `key % N` — adding/removing server invalidates almost all keys.

### Idea

Map both keys + servers onto a **hash ring** (e.g., 0 to 2³²). Each key goes to **next clockwise server**.

```
            S1
        K3 ●  ●
       ●        \
      K1         K2
       ●          ●
        \        /
         S3 ─── S2
```

Adding S4 → only keys between previous server and S4 move (~K/N keys, not all).

### Virtual nodes (vnodes)

Each physical server → many positions on ring (e.g., 200) → smoother distribution.

### Use cases

- Distributed cache (Memcached, Redis cluster, DynamoDB)
- Service-to-shard mapping
- Load balancers with sticky users

### Java pseudocode

```java
TreeMap<Long, Server> ring = new TreeMap<>();
for (Server s : servers) {
    for (int i = 0; i < VNODES; i++) {
        long h = hash(s.id + ":" + i);
        ring.put(h, s);
    }
}

Server lookup(String key) {
    long h = hash(key);
    Map.Entry<Long, Server> e = ring.ceilingEntry(h);
    return e != null ? e.getValue() : ring.firstEntry().getValue();
}
```

---

## Database Scaling

```
Stage 1: Single DB
   ↓ Add Read replicas
Stage 2: Primary + Read replicas (read scale)
   ↓ Add Cache
Stage 3: + Application cache (Redis)
   ↓ Vertical limit hit
Stage 4: Sharding (write scale)
   ↓ Multi-region
Stage 5: Multi-region replication
```

→ Most apps **never need stage 4+**.

---

## Sharding (Horizontal Partitioning)

> "Split a single logical DB across multiple physical machines based on a **shard key**."

```
Tenant table sharded by user_id:
  Shard 1: user_id 1 - 1M
  Shard 2: user_id 1M - 2M
  Shard 3: user_id 2M - 3M
```

### Strategies

| Strategy | How | Pros | Cons |
|----------|-----|------|------|
| **Range** | Sort by key range | Simple, good for range queries | Hotspots (recent IDs hot) |
| **Hash** | hash(key) % N | Uniform | Range queries hard, resharding pain |
| **Consistent Hash** | Hash ring | Smooth resharding | Range queries hard |
| **Directory** | Lookup table key→shard | Flexible | Single point of failure (lookup) |
| **Geo** | Region-based | Low latency | Imbalance |

### Choosing shard key

- High cardinality (avoid hotspot)
- Even distribution
- Aligns with most queries (locality)
- Avoid "monotonic" keys (timestamp-only) → all writes go to last shard

### Cross-shard queries

- Avoid ideally
- Scatter-gather (query all shards) — slow + complex
- Pre-aggregate / denormalize for common queries

→ Cross-ref: `03-Database-Design.md`.

---

## Read Replicas

```
Writes → Primary
            │
            ├─→ Replica 1 (async / sync replication)
            ├─→ Replica 2
            └─→ Replica 3
Reads ← Replicas (round-robin)
```

### Pros

- Read scale (10x reads possible with 9 replicas)
- Geo-distributed reads (replica in each region)

### Cons

- **Replication lag** (10ms - 100s of ms async)
- Reading own writes can return stale
- More complex topology

### Read-your-writes consistency

- Route writes to primary
- Reads after write → primary (until lag clears)
- Or use sticky session per user

### Sync vs Async replication

| | Sync | Async |
|--|------|-------|
| Lag | None (waits for replica ack) | Possible |
| Latency | Higher (write blocks) | Lower |
| Durability | Stronger | Weaker (data loss possible if primary dies) |
| Tools | PostgreSQL sync replicas, Galera | MySQL default, RDS replicas |

---

## Caching Layers

```
Client → CDN → Reverse Proxy → App Cache → DB
         ↑      ↑                ↑          ↑
       static  edge           Redis     query cache
```

### CDN

- Cloudflare, Akamai, CloudFront
- Static + sometimes dynamic
- Reduces origin hits + latency

### Reverse Proxy / Edge Cache

- Nginx, Varnish
- Cache HTTP responses

### Application Cache

- In-process: Caffeine, Ehcache (per server)
- Distributed: Redis, Memcached

### DB query cache

- MySQL query cache (deprecated)
- DB internal buffer pool
- Application-side query memoization

→ Cross-ref: `phase-4 / Performance & Optimization / 04-Application-Level-Caching.md`.

---

## Cache Strategies

### 1. Cache-Aside (Lazy loading)

```
read(key):
    val = cache.get(key)
    if val == null:
        val = db.get(key)
        cache.put(key, val, TTL)
    return val
```

→ Most common. Cache miss → DB read + populate.

### 2. Write-Through

```
write(key, val):
    db.put(key, val)
    cache.put(key, val)
```

→ Cache always fresh. Slower writes.

### 3. Write-Behind (Write-back)

```
write(key, val):
    cache.put(key, val)
    asyncQueue.enqueue(key, val)   // DB updated later
```

→ Fast writes; risk: data loss if cache crashes before flush.

### 4. Refresh-Ahead

Background refresh before TTL expiry → prevent cache miss spike.

### Eviction

| Policy | When |
|--------|------|
| LRU | Common default (Redis with allkeys-lru) |
| LFU | Frequency-based |
| FIFO | Simple, less ideal |
| Random | Edge cases |
| TTL | Time-based always |

---

## Auto-scaling

### Metrics-based triggers

- CPU > 70% → scale out
- Memory > 80% → scale out
- Request queue depth > X
- P95 latency > target
- Custom (CloudWatch / Prometheus)

### Predictive autoscaling

ML on historical traffic → pre-scale before peaks.

### Best practices

- **Stateless services** — autoscaling friendly
- **Warmup period** — JVM apps need 30-60s post-spin
- **Grace period** for scale-in (avoid thrashing)
- **Health checks** before adding to LB
- **Pre-baked AMIs / images** for fast spin

### K8s HPA

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 3
  maxReplicas: 50
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

---

## Common Output Traps

### Q1. "Just add more servers" without LB

→ Single LB now bottleneck; mention LB tier (active-active LB pair, ELB).

### Q2. Stateful service horizontally scaled without sticky / shared store

→ Inconsistent behavior across nodes.

### Q3. `key % N` sharding without resharding plan

→ Painful when N changes; use consistent hashing.

### Q4. Read replica without lag awareness

→ "Read-after-write" returns stale.

### Q5. Cache without TTL or invalidation

→ Stale data forever.

---

## Pitfalls

1. **Single LB** = SPOF — use **floating IP** / DNS multi-A or active-active LB pair.
2. **Sticky session in stateful service** prevents auto-scale balancing.
3. **Shard key with hotspot** — recent timestamp / sequential ID.
4. **Resharding nightmare** — consistent hashing avoids most pain.
5. **Replication lag ignored** — read-your-writes broken.
6. **Cache thundering herd** — popular key expires, all hit DB simultaneously.
7. **Cache stampede** — server restart = cold cache, DB overload.
8. **No back-pressure** — autoscaling can't keep up; use rate limiting.
9. **Vertical scaling forever** — eventual cliff (cost + size limits).
10. **Cross-shard JOINs** — denormalize or pre-aggregate.

---

## Cheat Sheet

| Need | Pattern |
|------|---------|
| More compute | Horizontal + LB |
| More reads (DB) | Read replicas |
| More writes (DB) | Sharding |
| Reduce DB load | Cache (Redis) |
| Geo-distributed | CDN + multi-region replicas |
| Smooth resharding | Consistent hashing |

| LB Algorithm | Use |
|--------------|-----|
| Round robin | Equal servers |
| Least connections | Long-lived (chat, WS) |
| IP hash | Sticky |
| Consistent hash | Sharded cache |

| Cache strategy | Use |
|----------------|-----|
| Cache-aside | Most reads |
| Write-through | Strong consistency |
| Write-behind | Fast writes, accept risk |
| Refresh-ahead | Predictable hot keys |

---

## Practice

1. Design read scale: 10K → 1M reads/sec, 100 writes/sec — primary + read replicas math.
2. Design 100M users system — at what point does sharding become necessary?
3. Implement consistent hashing in pseudocode.
4. Choose LB algorithm for: WebSocket chat, REST API, sharded cache.
5. Cache stampede mitigation — describe 3 strategies.
