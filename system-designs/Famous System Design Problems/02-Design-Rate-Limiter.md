# Design: Rate Limiter

## Status: Complete

---

## Table of Contents

1. [Problem Statement](#problem-statement)
2. [Where to Place the Limiter](#where-to-place-the-limiter)
3. [Algorithms Recap](#algorithms-recap)
4. [Storage: Redis Patterns](#storage-redis-patterns)
5. [Race Conditions & Atomicity](#race-conditions--atomicity)
6. [Lua: Sliding Window Example](#lua-sliding-window-example)
7. [HTTP Response Contract](#http-response-contract)
8. [Soft vs Hard Limit](#soft-vs-hard-limit)
9. [Multi-Dimensional Limits](#multi-dimensional-limits)
10. [Architecture Sketch](#architecture-sketch)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Problem Statement

> "**Protect APIs** from abuse + accidental overload. Enforce **max N requests per window** per identity (IP, user, key, route)."

Classic interview: design **distributed** rate limiter used by many API gateway instances.

---

## Where to Place the Limiter

| Layer | Pros | Cons |
|-------|------|------|
| **Client-side** | Zero server load | **Untrusted** — bypass trivial |
| **API Gateway** | Central, consistent, before app logic | Single policy unless multi-gateway sync |
| **Middleware / sidecar** | Per-service flexibility | Duplicated config unless centralized |
| **Edge (CDN / WAF)** | DDoS first line | Coarser rules |

> "**Defense in depth**: edge (IP flood) + gateway (business quotas) + optional app-level (expensive endpoints)."

---

## Algorithms Recap

| Algorithm | Burst | Accuracy | Memory |
|-----------|-------|---------|--------|
| **Token bucket** | ✅ Allowed | Good | O(1) |
| **Fixed window** | Boundary spike | Low | O(1) |
| **Sliding window log** | ✅ | Best | High |
| **Sliding window counter** | ✅ | Good balance | Medium |

Interview default: **token bucket** or **sliding window counter** with Redis.

---

## Storage: Redis Patterns

### Simple fixed window

```
INCR ratelimit:{user}:{minute_bucket}
EXPIRE ratelimit:{user}:{minute_bucket} 120
IF count > limit → 429
```

Fast but **boundary double spike**.

### Token bucket (Lua better)

Single key holding `(tokens, last_refill_ts)` — update atomically.

---

## Race Conditions & Atomicity

> "**Read-modify-write** without atomicity → two concurrent requests both see count=99, both increment → **burst past limit**."

Fix:

- **`INCR`** is atomic — good for fixed window
- **Lua script** — multiple reads/writes in one atomic step
- **`SET NX` + compare** — fragile; prefer Lua

---

## Lua: Sliding Window Example

Conceptual **sorted set** per user:

```lua
-- KEYS[1] = key, ARGV[1]=now_ms, ARGV[2]=window_ms, ARGV[3]=limit
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1]-ARGV[2])
local c = redis.call('ZCARD', KEYS[1])
if c < tonumber(ARGV[3]) then
  redis.call('ZADD', KEYS[1], ARGV[1], ARGV[1] .. '-' .. redis.call('INCR','seq'))
  redis.call('PEXPIRE', KEYS[1], ARGV[2])
  return {1, ARGV[3]-c-1}
end
return {0, 0}
```

(Production scripts add jitter cleanup + cardinality caps for huge windows.)

---

## HTTP Response Contract

```
429 Too Many Requests
Retry-After: 42

X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1715123456
```

Body: `{ "error": "rate_limit_exceeded", "retry_after_seconds": 42 }`

---

## Soft vs Hard Limit

| Type | Behavior |
|------|----------|
| **Hard** | Strict reject at limit |
| **Soft** | Allow burst over briefly / degrade (throttle delay) / serve lower tier |

**Soft** useful for paying customers — queue requests vs instant 429.

---

## Multi-Dimensional Limits

Composite key examples:

```
limit:user:{id}:global
limit:user:{id}:endpoint:/search
limit:ip:{ip}
limit:api_key:{key}
```

Evaluate **all** applicable limits; **most restrictive** wins or fail-fast on first breach.

---

## Architecture Sketch

```
Client → CDN (optional IP limit)
      → API Gateway
            │
            ├─► Rate Limit Filter ──► Redis Cluster (Lua scripts)
            │
            └─► Backend services
```

**Fail-open vs fail-closed**: auth endpoints often **fail-closed**; read-mostly catalog might **fail-open** with logging.

---

## Pitfalls

1. **Per-instance in-memory limit** → effective limit × N instances
2. **No Retry-After** → retry storm
3. **NAT shared IP** → unfair blocking — pair IP limit with user/key limit
4. **Huge Lua CPU** on Redis — keep scripts tight
5. **Clock skew** across regions — prefer relative windows server-side

---

## Cheat Sheet

| Question | Answer |
|----------|--------|
| Distributed store? | **Redis + Lua** |
| Headers? | `429`, `Retry-After`, `X-RateLimit-*` |
| Client-side? | Untrusted — cosmetic only |
| Atomicity? | **INCR** or **Lua** |

---

## Practice

1. Implement token bucket in Redis Lua with refill rate + capacity.
2. Design limits for: login (IP), API (key), search (user+endpoint).
3. Compare fail-open vs fail-closed for payment vs catalog APIs.
