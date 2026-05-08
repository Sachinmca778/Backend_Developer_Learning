# Common System Designs

## Status: Not Started

---

## Table of Contents

1. [URL Shortener](#url-shortener)
2. [Rate Limiter](#rate-limiter)
3. [Notification System](#notification-system)
4. [News Feed](#news-feed)
5. [Distributed Cache](#distributed-cache)
6. [Common Output Traps](#common-output-traps)
7. [Pitfalls](#pitfalls)
8. [Cheat Sheet](#cheat-sheet)

---

## URL Shortener

> bit.ly / tinyurl

### 1. Requirements

**Functional:**
- Shorten long URL → short URL (e.g., `bit.ly/abc123`)
- Redirect short → long URL
- Custom URL alias (premium)
- Expiry / TTL
- Analytics (click count) — optional

**Non-functional:**
- High availability (no missed redirects)
- Low latency (< 50ms redirect)
- 100M new URLs/month
- 100:1 read:write ratio

### 2. Estimation

```
New URLs/month   = 100M
Writes/sec       = 100M / (30 × 10⁵) ≈ 33/sec
Reads/sec        = 33 × 100 = 3,300/sec
Storage 5 years  = 100M × 12 × 5 = 6B URLs
   Each entry    = 500 bytes (long URL up to 2KB cap, short, metadata)
   Total         = 6B × 500B = 3 TB
Bandwidth read   = 3,300 × 500 B = 1.65 MB/sec (small)
Bandwidth write  = 33 × 500 B = small
```

### 3. API

```
POST /shorten         body: { long_url, custom_alias?, expiry? }
                      response: { short_url }
GET /{short_code}     302 → long URL
DELETE /{short_code}
```

### 4. Short code generation

#### Option A: Hash (MD5/SHA + base62)

```
hash(long_url) → take first 6 chars → base62
```

**Pros:** Idempotent (same URL → same short).
**Cons:** Collisions need handling.

#### Option B: Counter + base62 (preferred)

```
auto-increment counter → base62 encode (e.g., 1 → "1", 62 → "10", ...)
```

```
62^7 = 3.5 × 10¹² → 7-char alias supports trillions
```

**Pros:** No collisions, predictable length.
**Cons:** Sequential (predictable enumeration risk) — solve with random offset / hash within range.

### 5. Schema

```sql
CREATE TABLE urls (
    short_code  VARCHAR(10) PRIMARY KEY,
    long_url    TEXT NOT NULL,
    user_id     BIGINT,
    created_at  TIMESTAMP,
    expires_at  TIMESTAMP NULL,
    click_count BIGINT DEFAULT 0
);
CREATE INDEX idx_user ON urls(user_id);
CREATE INDEX idx_expires ON urls(expires_at);
```

→ DB choice: PostgreSQL (ACID) or DynamoDB (managed key-value).

### 6. High-level architecture

```
Client → CDN → API Gateway → URL Service → Cache (Redis) → DB
                                              ↓
                                          Counter Service (distributed)
```

### 7. Counter at scale

Single global counter = bottleneck. Options:

- **Range allocation** — service allocates counter ranges in chunks (e.g., 1000 IDs at a time per server)
- **Snowflake-style ID** — timestamp + machine ID + sequence
- **ZooKeeper / etcd** for atomic increment (slow at scale)

### 8. Redirect flow

```
GET /abc123
  ↓
Cache lookup → hit? → 302 Location: long_url
  ↓ miss
DB lookup → write to cache → 302
```

→ **Cache hit ratio 90%+** (URLs are popular distribution).

### 9. 301 vs 302

| | 301 (permanent) | 302 (temporary) |
|--|----------------|-----------------|
| Cached by browser | Aggressively | Less |
| Use | Forever shorts | Analytics tracking (302 to count clicks) |

→ For analytics, **302** so each click hits server.

### 10. Cleanup expired

Background job: scan `expires_at < now()` → delete in batches.

---

## Rate Limiter

> Prevent abuse / DDoS at API gateway, per-user / per-IP.

### Algorithms

#### 1. Fixed Window Counter

```
counter[user_id] in current minute
if counter > limit → block
reset every minute
```

**Pros:** Simple.
**Cons:** Burst at window boundaries (e.g., 100 at 11:59:59 + 100 at 12:00:01 = 200 in 2s).

#### 2. Sliding Window Log

```
log[user_id] = list of timestamps within last 60 sec
if size(log) > limit → block
```

**Pros:** Accurate.
**Cons:** Memory cost (one entry per request).

#### 3. Sliding Window Counter (hybrid — recommended)

```
prev_window_count + curr_window_count × window_progress
```

→ Approximates sliding window with constant memory. Used at scale (Cloudflare).

#### 4. Token Bucket

```
bucket size = capacity (e.g., 10)
refill rate = tokens/sec (e.g., 1/sec)

on request:
    if tokens >= 1: tokens -= 1, allow
    else: deny
```

**Pros:** Allows bursts up to capacity. Smooth.
**Cons:** Slightly complex.

#### 5. Leaky Bucket

```
queue with max size; dequeue at fixed rate
overflow → drop
```

→ Smooths bursts at request level. Used in TCP routing.

### Comparison

| Algorithm | Memory | Accuracy | Burst handling |
|-----------|--------|----------|----------------|
| Fixed window | O(1) | Low | Allows boundary bursts |
| Sliding log | O(N) | High | Accurate |
| Sliding counter | O(1) | Medium-high | Smooth |
| Token bucket | O(1) | Configurable | Burst-friendly |
| Leaky bucket | O(N) queue | High | Burst smoothing |

### Distributed rate limiter — Redis

```
INCR rate:user:{id}:{minute}
EXPIRE 60
if value > limit → 429
```

→ Race-free with `EVAL` Lua script for atomic check-and-increment.

### Token bucket in Redis (Lua)

```lua
local tokens = redis.call('GET', KEYS[1]) or capacity
local last_refill = redis.call('GET', KEYS[1] .. ':ts') or now
local elapsed = now - last_refill
tokens = math.min(capacity, tokens + elapsed * refill_rate)
if tokens >= 1 then
    tokens = tokens - 1
    redis.call('SET', KEYS[1], tokens)
    return 1
else
    return 0
end
```

### Where to enforce

- API gateway (centralized, language-agnostic)
- Per-service limits (defense in depth)
- Bucketed by user / IP / endpoint

→ Cross-ref: `phase-4 / Spring Cloud / 02-Spring-Cloud-Gateway.md`.

---

## Notification System

> Send notifications via push / email / SMS to millions of users.

### Requirements

**Functional:**
- Channels: push (FCM/APNs), email (SES/SendGrid), SMS (Twilio)
- Per-user preferences (opt-in/out per channel + category)
- Templating (transactional vs marketing)
- Schedule + retry on failure
- Throttling per user

**Non-functional:**
- High throughput (millions/min for marketing burst)
- At-least-once delivery
- Per-channel SLA

### Architecture

```
[Producer Services] → Notification API
                         ↓
                  [Kafka topic: notifications]
                         ↓
            ┌────────────┼────────────┐
            ↓            ↓            ↓
       [Push Worker] [Email Worker] [SMS Worker]
            ↓            ↓            ↓
       FCM/APNs       SES/SendGrid Twilio
            
                  [DLQ for failed]
```

### Schema

```sql
CREATE TABLE user_preferences (
    user_id BIGINT,
    channel VARCHAR(20),       -- 'push', 'email', 'sms'
    category VARCHAR(50),      -- 'marketing', 'transactional', etc.
    enabled BOOLEAN,
    PRIMARY KEY (user_id, channel, category)
);

CREATE TABLE notification_log (
    id BIGINT PK,
    user_id BIGINT,
    channel VARCHAR(20),
    status VARCHAR(20),        -- pending, sent, failed
    sent_at TIMESTAMP,
    INDEX (user_id, sent_at)
);
```

### Flow

```
1. Service publishes event ("order_shipped")
2. Notification orchestrator looks up template + user preferences
3. Fan out to channel-specific topics (push / email / sms)
4. Workers consume + call external API
5. Retry with backoff on transient failure
6. DLQ on exhaustion + alert ops
```

### Throttling

- Per user: max 5 notifications/hour
- Global rate limit on external API (FCM, SES quotas)

→ Cross-ref: `phase-4 / Messaging — Kafka / 06-Kafka-Patterns.md` (DLT, retry topics).

---

## News Feed

> Twitter / Facebook timeline.

### Requirements

**Functional:**
- Post message
- Follow other users
- View feed (latest from followed users)
- Like / comment / share
- Ranking (chronological / algorithmic)

**Non-functional:**
- Read-heavy (100:1)
- Low-latency feed load (< 200ms)
- 200M DAU
- Eventual consistency OK

### Estimation

```
DAU             = 200M
Posts/user/day  = 0.5 → Total = 100M posts/day
Feed views/day  = 200M × 5 = 1B
Read QPS        = 1B / 10⁵ = 10K
Write QPS       = 100M / 10⁵ = 1K
```

### Approaches

#### 1. Pull (Fan-out on Read)

When user requests feed, query latest posts from followed users.

**Pros:** Cheap writes, fresh data.
**Cons:** Expensive reads (JOIN across many followers' posts), repeat work.

#### 2. Push (Fan-out on Write)

When user posts, **inject into all followers' inbox** (precomputed).

**Pros:** Read = fetch own inbox (fast).
**Cons:** Write amplification ("celebrity" with 100M followers → 100M writes).

#### 3. Hybrid (Production)

- Push for users with < N followers (10K threshold)
- Pull for celebrities (avoid write storm)
- Combine at read time

### Architecture (Hybrid)

```
[Post Service] → Kafka (posts topic)
   ├─→ Fan-out worker: inject into followers' Redis lists (ZSET ordered by timestamp)
   └─→ DB persists post

[Feed Service] →
   ├─ Get user's pre-computed feed from Redis
   ├─ Pull recent posts from celebrities followed
   └─ Merge + rank + return
```

### Schema

```sql
CREATE TABLE posts (
    id BIGINT PK,
    user_id BIGINT,
    content TEXT,
    created_at TIMESTAMP,
    INDEX (user_id, created_at)
);

CREATE TABLE follows (
    follower_id BIGINT,
    followee_id BIGINT,
    PRIMARY KEY (follower_id, followee_id),
    INDEX (followee_id)         -- "find followers of X"
);
```

### Redis structure

```
ZADD feed:{user_id} <timestamp> <post_id>
ZREVRANGE feed:{user_id} 0 19    -- top 20 recent
```

→ TTL on feed key (e.g., 1 day) to limit memory.

### Ranking (advanced)

Algorithmic feed (FB / Twitter For You):
- Recency
- Engagement (likes / comments)
- User affinity (interaction history)
- ML model scoring

---

## Distributed Cache

> Memcached / Redis cluster.

### Requirements

- Sub-millisecond reads
- Capacity: 1 TB hot data
- Replication for availability
- Auto-failover

### Architecture

```
Client → Cache Proxy → [Cache nodes — consistent hash ring]
                          │
                       Replicas
                          │
                       Persistence (Redis AOF/RDB or Memcached: none)
```

### Key concepts

- **Consistent hashing** for key → node
- **Replication** for HA (e.g., 3 replicas per shard)
- **Failover** automatic (Sentinel for Redis, Cluster mode)
- **Eviction** LRU / LFU / TTL
- **Hot key** mitigation (replicate hot key to multiple nodes)

### Memcached vs Redis

| | Memcached | Redis |
|--|-----------|-------|
| Data types | String only | String, list, set, sorted set, hash, stream |
| Persistence | None | RDB + AOF |
| Replication | Built-in (newer) | Replica sets / Cluster |
| Use | Pure cache | Cache + data structures + pub-sub |
| Throughput | Slightly higher | Comparable |
| Multi-threaded | Yes | Single-threaded (Redis 6+ optional IO threads) |

→ **Default modern choice = Redis** (richer features).

### Hot key problem

Single key with millions of hits → one node hot.

**Solutions:**
- **Replicate** hot key across N nodes; client picks random
- **Local L1 cache** in app (Caffeine) for ultra-hot
- **Sharded counter** (key:1, key:2, ..., key:N); aggregate on read

### Cache miss storm (thundering herd)

Popular key expires → millions of clients hit DB.

**Solutions:**
- **Probabilistic early expiration** — refresh before TTL
- **Distributed lock** — only one client refills cache (others wait)
- **Stale-while-revalidate** — serve stale during refill
- **Negative caching** — cache "not found" for short TTL

→ Cross-ref: `phase-4 / Performance & Optimization / 04-Application-Level-Caching.md`.

---

## Common Output Traps

### Q1. URL shortener — using sequential ID directly

→ Predictable; users can enumerate. Add random offset / hash.

### Q2. Rate limiter — fixed window without burst safety

→ 2x limit possible at boundary.

### Q3. News feed pull-based for 100M users

→ Read latency unacceptable; mention hybrid.

### Q4. Distributed cache without consistent hashing

→ Resharding pain.

### Q5. Notification system without DLQ

→ Lost messages on transient failures.

---

## Pitfalls

1. **URL shortener counter SPOF** — distributed counter / Snowflake ID.
2. **Cache without eviction** — OOM.
3. **Rate limit per IP only** — shared NAT IPs unfairly throttled.
4. **Push fan-out for celebrity** — write storm; hybrid.
5. **Notification retry without backoff** — DDoS external service.
6. **News feed sync calculation per request** — precompute.
7. **Hot key in cache** — replicate / local cache.
8. **Cache stampede** — distributed lock / probabilistic expire.
9. **No analytics** for URL shortener — miss product feature.
10. **Missing TTLs** — cache grows unbounded.

---

## Cheat Sheet

| Design | Key Components |
|--------|---------------|
| URL shortener | Counter + base62, cache, 302 redirect |
| Rate limiter | Token bucket / sliding counter, Redis |
| Notification | Kafka + worker pools per channel |
| News feed | Hybrid push/pull, Redis ZSET inbox |
| Distributed cache | Consistent hash ring + replication |

| Strategy | When |
|----------|------|
| Fan-out on write | < 10K followers |
| Fan-out on read | Celebrity / sparse access |
| Hybrid | Realistic large social |

| Rate limit | Best |
|-----------|------|
| Sliding window counter | Most production |
| Token bucket | Burst-tolerant API |
| Leaky bucket | Network smoothing |

---

## Practice

1. Design URL shortener — counter strategy, base62, schema, 301 vs 302 trade-off.
2. Implement token bucket in Redis Lua; calculate latency.
3. News feed — explain hybrid choice + Redis ZSET design.
4. Notification system retry strategy — exponential backoff, DLQ, alerting.
5. Distributed cache hot key — 3 mitigation strategies.
