# Rate Limiting

## Status: Complete

---

## Table of Contents

1. [Why Rate Limit](#why-rate-limit)
2. [Where to Limit (Scoping)](#where-to-limit-scoping)
3. [Token Bucket](#token-bucket)
4. [Leaky Bucket](#leaky-bucket)
5. [Fixed Window Counter](#fixed-window-counter)
6. [Sliding Window Log](#sliding-window-log)
7. [Sliding Window Counter](#sliding-window-counter)
8. [Algorithm Comparison](#algorithm-comparison)
9. [Distributed Rate Limiting](#distributed-rate-limiting)
10. [Redis + Lua Implementation](#redis--lua-implementation)
11. [Response Format & Headers](#response-format--headers)
12. [Throttling vs Rate Limiting vs Backpressure](#throttling-vs-rate-limiting-vs-backpressure)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Why Rate Limit

> "**API ki self-defense mechanism.** Without rate limit: ek runaway client / scraper / DDoS = whole API down. With it: smooth + fair."

Wins:

- **Protect** backends from overload
- **Fairness** — one tenant can't hog all capacity
- **Cost control** — limits API/AWS bill from abuse
- **Tiered pricing** — Free / Pro / Enterprise
- **Security** — slow brute-force attacks (login attempts)
- **Compliance** — partner SLAs

---

## Where to Limit (Scoping)

| Scope | Use case |
|-------|----------|
| **Per IP** | Anonymous traffic, basic DDoS defense |
| **Per user / API key** | Fairness across paying users |
| **Per endpoint** | Expensive endpoints get tighter limit (search heavier than ping) |
| **Per region / data center** | Capacity planning |
| **Global** | Protect overall API capacity |
| **Per tenant** | Multi-tenant SaaS isolation |

### Combine for defense in depth

```
Anonymous: 60 req/min per IP
Authenticated: 1000 req/min per user
Endpoint /search: 100 req/min per user (extra tight)
Global: 100K req/sec across all
```

> Multiple layers — each catches a different abuse pattern.

---

## Token Bucket

> "**Bucket of tokens** — har request 1 token consume karta hai. Bucket **refills at fixed rate**. Empty → reject (429)."

```
bucket capacity = 10 tokens (max burst)
refill rate     = 1 token/sec (sustained rate)

t=0:   bucket=10, request → bucket=9
t=0.1: request → bucket=8
... 10 quick requests → bucket=0
t=0.5: request → bucket=0 → 429 (or wait)
t=1:   refill → bucket=1, request → bucket=0
```

### Behavior

- Allows **bursts up to bucket size**
- **Sustained rate** = refill rate
- Smooth and intuitive

### Pros

- **Bursts allowed** — good for human users (clicking quickly)
- Easy to tune (capacity = burst, rate = sustained)
- Most popular algorithm in production

### Cons

- Not strict shaping — small bursts pass through

### Use when

- General API rate limiting
- Allowing reasonable bursts (most user-facing APIs)
- Examples: **AWS API Gateway**, **Stripe**, **GitHub API**

---

## Leaky Bucket

> "**Bucket leaks at fixed rate.** Requests fill bucket; if full → reject. Output rate constant — strict shaping."

```
bucket size  = 10 (max queue)
leak rate    = 1 req/sec (output rate)

Requests come in at 100/sec → bucket fills → overflow → drop excess
Output to backend = 1/sec, no matter input
```

### Pros

- **Strict output rate** — perfect for shaping toward downstream that needs steady load
- Predictable load on downstream
- Smooths bursty input

### Cons

- **No bursts allowed** — even if bucket has space, output is steady
- Sudden spike of legit traffic gets queued / dropped

### Use when

- **Shaping traffic** to a slow downstream (legacy API, payment processor)
- **Strict billing** by rate (1 req/sec exactly)
- Examples: network packet shaping (QoS), modem traffic control

### Token vs Leaky in one line

> "**Token Bucket** = bursts allowed, average steady. **Leaky Bucket** = strict output, queue input."

---

## Fixed Window Counter

> "**Sabse simple:** counter resets at every fixed window (e.g., per minute). Increment per request; > limit → reject."

```
window = 1 minute, limit = 100 req

12:00 → counter=0
12:00:30 → 50 requests → counter=50
12:00:59 → 50 more → counter=100
12:01:00 → reset → counter=0
12:01:00 → 100 requests → all pass
```

### Pros

- **Trivial to implement** (Redis `INCR` + `EXPIRE`)
- Low memory (one counter per window)

### Cons

- **Boundary spike problem** — at minute boundary, can get **2× limit** in 2 sec
  ```
  12:00:59 → 100 req allowed (window 12:00)
  12:01:00 → 100 req allowed (new window 12:01)
  → 200 req in 1 sec at boundary
  ```
- Not smooth

### Use when

- Simple internal limits
- Coarse-grained protection
- Prototype / non-critical

---

## Sliding Window Log

> "**Track timestamp of every request** in window. Count = how many timestamps within last N seconds. > limit → reject."

```
limit = 100 req / 60 sec
Sorted set in Redis: { timestamp1, timestamp2, ... }

On request:
  1. Remove timestamps older than now - 60 sec
  2. Count remaining
  3. If count < limit → accept + add current timestamp
  4. Else → reject
```

### Pros

- **Most accurate** — exact rolling window
- No boundary spike

### Cons

- **Memory-heavy** — store every request timestamp
- High traffic = millions of entries
- Cleanup overhead

### Use when

- Low-volume, high-precision needs (financial, security-critical)
- Sliding billing windows

---

## Sliding Window Counter

> "**Approximation** of sliding window log — keep counters per **sub-window** (e.g., per second), sum the last N. Cheap + accurate enough."

```
limit = 100 req / 60 sec

Bucket per second: { sec1: 5, sec2: 10, ..., sec60: 8 }

On request:
  1. Find current second bucket → increment
  2. Sum last 60 buckets → count
  3. If count < limit → accept; else reject
  4. Old buckets expire automatically
```

Or weighted approach:

```
Take previous full window count + current partial window count weighted by elapsed
e.g., 75% of previous minute (still in sliding range) + this minute so far
```

### Pros

- **Best balance** of accuracy + memory
- No boundary spike (smooths across windows)
- Production-friendly

### Cons

- Slightly more complex than fixed window
- Approximation (small error vs log)

### Use when

- **Default modern choice** for most APIs
- Examples: **Cloudflare**, **Kong**, custom Redis-backed limiters

---

## Algorithm Comparison

| Algorithm | Memory | Accuracy | Boundary spike | Bursts allowed | Use |
|-----------|--------|----------|----------------|----------------|-----|
| **Token Bucket** | O(1) | ✅ | No | ✅ Yes | General API (most common) |
| **Leaky Bucket** | O(bucket) | ✅ | No | ❌ No | Strict shaping |
| **Fixed Window** | O(1) | ❌ | **Yes** | Limited | Simple / prototyping |
| **Sliding Log** | O(req in window) | ✅✅ | No | ✅ Yes | Precision-critical |
| **Sliding Counter** | O(buckets) | ✅ | No | ✅ Yes | **Default modern choice** |

> **Most production setups use Token Bucket or Sliding Window Counter.**

---

## Distributed Rate Limiting

> "**Multiple gateway / app instances** sharing same limit → counters need a **shared store**."

### Options

| Approach | Pros | Cons |
|----------|------|------|
| **Local in-memory** (per-instance) | Fast, simple | Limit applied per instance, not global |
| **Redis with INCR + EXPIRE** | Shared, fast (~1 ms) | Single Redis = SPOF |
| **Redis with Lua** | Atomic, no race conditions | Slightly more complex |
| **Redis Cluster** | Scale + HA | Hashing nuances |
| **Memcached CAS** | Distributed cache as counter | Less common |
| **DynamoDB conditional updates** | Multi-region | Higher latency |
| **Cloudflare Workers + Durable Objects** | Edge-distributed | Vendor-locked |

### Local + global hybrid

- **Local approximation** for hot counters (fast)
- **Periodic sync** to global (eventual consistency on quotas)
- Used at huge scale (envoy local + global ratelimit service)

---

## Redis + Lua Implementation

> "**Atomic** rate limiting using Lua script (single round-trip, no race conditions)."

### Token bucket in Lua

```lua
-- KEYS[1] = bucket key
-- ARGV[1] = capacity
-- ARGV[2] = refill_rate (tokens per sec)
-- ARGV[3] = now (unix sec)
-- ARGV[4] = requested tokens

local capacity   = tonumber(ARGV[1])
local refill     = tonumber(ARGV[2])
local now        = tonumber(ARGV[3])
local requested  = tonumber(ARGV[4])

local data = redis.call("HMGET", KEYS[1], "tokens", "ts")
local tokens = tonumber(data[1]) or capacity
local ts     = tonumber(data[2]) or now

-- refill based on elapsed
local elapsed = math.max(0, now - ts)
tokens = math.min(capacity, tokens + elapsed * refill)

local allowed = 0
if tokens >= requested then
  tokens = tokens - requested
  allowed = 1
end

redis.call("HMSET", KEYS[1], "tokens", tokens, "ts", now)
redis.call("EXPIRE", KEYS[1], 3600)

return { allowed, math.floor(tokens) }
```

### Sliding window counter in Redis

```lua
-- Use sorted set keyed by user_id; timestamp scores
-- ZADD key score=now value=now
-- ZREMRANGEBYSCORE key 0 (now - window)
-- ZCARD key → count
-- if count > limit → reject
```

### Why Lua

- **Atomic** — Redis runs script as single op (no other commands interleave)
- Avoids race conditions (read → check → write)
- Fewer round trips → faster

---

## Response Format & Headers

> "**Tell the client what happened + when to retry.** Good API hygiene."

### HTTP status

```
429 Too Many Requests
```

### Response body

```json
{
  "error": "rate_limit_exceeded",
  "message": "Too many requests, please slow down",
  "retry_after": 12
}
```

### Headers (informational)

```
X-RateLimit-Limit: 1000          ← limit per window
X-RateLimit-Remaining: 0         ← left in current window
X-RateLimit-Reset: 1715000000    ← unix timestamp when window resets
Retry-After: 12                  ← seconds (or HTTP-date)
```

### Standardization

- **`Retry-After`** is RFC standard
- `RateLimit-*` headers being standardized in IETF draft (`RateLimit`, `RateLimit-Policy`)
- Always include — clients can self-throttle

---

## Throttling vs Rate Limiting vs Backpressure

| Concept | Description |
|---------|-------------|
| **Rate limiting** | Hard cap on requests per time window — reject excess |
| **Throttling** | Slow down (delay) instead of reject |
| **Backpressure** | Signal upstream to **slow producing** (reactive streams, gRPC flow control) |
| **Load shedding** | Drop low-priority requests when system overloaded (preserve critical path) |

### When to use which

- **Rate limit**: per-customer fairness, abuse prevention
- **Throttle**: slow uploads, gradual API ramp
- **Backpressure**: streaming, async pipelines (Kafka consumer lag)
- **Load shed**: protect critical path during overload (drop analytics first, keep checkout)

---

## Pitfalls

1. **Limit per-instance** in distributed system → effective limit = N × instances.
2. **Race conditions** without atomic ops → counter undercounts → leaks past limit.
3. **No `Retry-After`** header → clients hammer back instantly.
4. **Fixed window in security-critical context** → 2× limit at boundary = brute-force.
5. **Limit too low** → false positives, angry users.
6. **Limit too high** → no protection.
7. **Same limit for all endpoints** → expensive endpoints under-protected.
8. **No per-user limit on auth attempts** → brute-force possible.
9. **Rate limiter SPOF** (single Redis without HA) → API down when limiter down.
10. **No graceful degradation when limiter down** — fail open vs fail closed decision matters.
11. **Counting by IP only** behind NAT → unfair (entire office shares one IP).
12. **Hidden retry storms** when 429 triggers retry → make it worse.

### Fail open vs fail closed

- **Fail open** — limiter down → allow all (availability > security)
- **Fail closed** — limiter down → block all (security > availability)
- Pick based on context (auth = fail closed; public read = fail open with caps).

---

## Cheat Sheet

| Algorithm | When |
|-----------|------|
| **Token Bucket** | Default — bursts OK |
| **Leaky Bucket** | Strict shaping |
| **Fixed Window** | Simple internal |
| **Sliding Log** | Precision critical |
| **Sliding Counter** | Best balance, modern default |

| Layer | Where |
|-------|-------|
| Edge | Cloudflare / API Gateway |
| App | Spring filter / Express middleware |
| Service mesh | Envoy local + global ratelimit |
| Per-DB | Statement / connection limits |

| Response | |
|----------|--|
| Status | **429** |
| Headers | `Retry-After`, `X-RateLimit-*` |
| Body | machine-readable error code + message |

| Storage | |
|---------|--|
| Single instance | In-memory (Caffeine, Bucket4j) |
| Multi-instance | **Redis + Lua** |
| Edge | Cloudflare KV / Durable Objects |

---

## Practice

1. Implement **token bucket** in Redis Lua for `limit=100/min, burst=20`.
2. Build per-IP + per-user composite limiter for an API gateway.
3. Compare boundary spike: fixed window vs sliding counter — simulate.
4. Add `Retry-After` + `X-RateLimit-*` to 429 responses in Spring Boot.
5. Decide fail-open vs fail-closed for: login API / public catalog / payment.
6. Distributed limiter with N=10 gateway instances; design global counter via Redis.
