# Design: URL Shortener (like bit.ly)

## Status: Complete

---

## Table of Contents

1. [Requirements Clarification](#requirements-clarification)
2. [Capacity Back-of-Envelope](#capacity-back-of-envelope)
3. [API Design](#api-design)
4. [High-Level Architecture](#high-level-architecture)
5. [Short Code / ID Generation](#short-code--id-generation)
6. [Collision Handling](#collision-handling)
7. [Database Choice](#database-choice)
8. [Read Path & Caching](#read-path--caching)
9. [Custom Aliases & Expiry](#custom-aliases--expiry)
10. [Analytics](#analytics)
11. [Rate Limiting](#rate-limiting)
12. [Pitfalls & Interview Follow-ups](#pitfalls--interview-follow-ups)
13. [Cheat Sheet](#cheat-sheet)

---

## Requirements Clarification

### Functional

| Feature | Notes |
|---------|--------|
| Shorten long URL | Generate unique `short_code` |
| Redirect `GET /{code}` | HTTP 302/301 to long URL |
| Optional **custom alias** | User chooses code if available |
| Optional **TTL / expiry** | After expiry → 410 Gone or landing page |
| **Analytics** | Clicks per URL, geo rough, referrer (optional premium) |

### Non-functional (typical interview assumptions)

| NFR | Example target |
|-----|------------------|
| Read-heavy | **100:1** redirect vs create |
| Latency redirect | p99 **< 50 ms** (cache hit), **< 150 ms** DB |
| Availability | **99.9%+** |
| Scale | State assumptions explicitly |

---

## Capacity Back-of-Envelope

```
Assume:
  100M new URLs/month
  500M redirects/day

Creates: 100M / (30×86400) ≈ 39 QPS average, peak ~5× ≈ 200 QPS
Redirects: 500M / 86400 ≈ 5800 QPS average, peak ~5× ≈ 30K QPS

Storage per row (long URL + meta):
  long URL avg 500 B + short_code 10 B + indexes ~100 B ≈ 650 B
  100M/month × 650 B ≈ 65 GB/month raw (+ replication index overhead)

Bandwidth redirects:
  30K QPS × tiny redirect response → manageable; origin behind CDN optional
```

→ **Redirects dominate** → optimize read path (cache + LB).

---

## API Design

```
POST   /v1/urls
       Body: { "long_url": "https://...", "alias": "my-sale", "ttl_days": 30 }
       Headers: Authorization: Bearer …
       Response: 201 { "short_url": "https://short.ly/my-sale", "expires_at": "..." }

GET    /{code}
       302 Location: long_url   (or 301 permanent — SEO trade-off)
       410 if expired

GET    /v1/urls/{code}/stats   (analytics — auth owner)

DELETE /v1/urls/{code}        (optional — soft delete)
```

**Idempotency**: `POST` with same body — optional idempotency-key header to avoid duplicate shorts.

---

## High-Level Architecture

```
Clients
   │
   ▼
API Gateway (auth, rate limit)
   │
   ├──► Write path ─► URL Service ─► DB (insert mapping)
   │                      │
   │                      └──► (optional) Kafka ─► Analytics aggregator
   │
   └──► Read path ─► Redirect Service
                           │
                    ┌──────┴──────┐
                    │ Redis cache │  short_code → long_url (+ TTL mirror)
                    └──────┬──────┘
                           │ miss
                           ▼
                         DB / Replica (by short_code index)
```

Optional: **GeoDNS + regional replicas** for global redirect latency.

---

## Short Code / ID Generation

### Approach A — Counter + Base62 (recommended)

```
Global counter (Snowflake-like ID or DB sequence shard) → integer N
Encode N in base62 [a-zA-Z0-9] → fixed-length growing string
```

- **Pros**: No collision by construction (per shard uniqueness), short predictable growth
- **Cons**: Need distributed ID generator (**Snowflake**, DB sequences per shard, or dedicated counter service with Raft)

### Approach B — Hash truncated (e.g. SHA256 first 7 chars base62)

```
hash = sha256(long_url + salt)
short = base62(hash)[0:7]
```

- **Pros**: Stateless; same URL might map to same short (dedup opportunity)
- **Cons**: **Collision risk** — must check DB and retry with salt / counter suffix

### Approach C — Random + retry

```
loop: candidate = random_base62(7); if not exists in DB → use
```

- Simple until scale; birthday paradox matters at billions of keys

### Interview soundbite

> "**Production mein zyada tar counter + base62** predictable aur collision-free (with shard-aware IDs). **Hash truncate** sirf dedup-friendly demos ke liye — collision handling zaroori."

**Length**: 7 chars base62 ≈ 62^7 ≈ 3.5×10^12 space — plenty.

---

## Collision Handling

| Strategy | When |
|----------|------|
| **Pre-check INSERT unique constraint** | DB rejects dup → retry new ID |
| **Append suffix** | `abc12` exists → `abc12x` |
| **Re-hash with nonce** | Different salt until unique |

Custom aliases: **UNIQUE constraint on alias column** — user-facing collision = **409 Conflict**.

---

## Database Choice

### SQL (Postgres / MySQL)

- Table: `(short_code PK, long_url, user_id, created_at, expires_at, click_count)`
- **Index on short_code** (already PK)
- Strong consistency for mapping
- Scale: single primary + **read replicas** for redirect lookups; partition later by **prefix / shard**

### Cassandra / Dynamo (massive scale)

- Partition key: `short_code`
- Good for **billions** of URLs, multi-region
- Writes scaled; reads by key O(1)

### Interview pick

> "**Start Postgres + Redis.** Scale reads with replicas + cache. **Cassandra** tab jab row count / multi-region write path justify ho."

---

## Read Path & Caching

> "**Hot URLs** (top 1%) drive majority traffic → **Redis** `GET short:{code}` → long URL."

- TTL: align with URL expiry or **write-through invalidate** on delete
- **Negative cache** optional for spam codes (short TTL) to protect DB

Eviction: **allkeys-lru** if pure cache.

---

## Custom Aliases & Expiry

- **Custom alias**: separate column `alias` unique OR store as primary `short_code` when user-defined
- **Expiry**: `expires_at`; redirect worker checks → **410**; background job deletes row + cache purge
- **Cron / TTL index** for cleanup

---

## Analytics

### Options

| Approach | Pros | Cons |
|----------|------|------|
| **Counter in DB** | Simple | Hot row on viral URL |
| **Redis INCR** per code + periodic flush to DB | Fast | Approximate until flush |
| **Kafka click stream** | Scale, raw events | Pipeline complexity |

Hybrid: **Redis counters** + nightly **rollup** to warehouse (BigQuery/Snowflake).

---

## Rate Limiting

- **Per user / API key**: token bucket in Redis (Lua)
- **Per IP** for anonymous
- Gateway returns **429 + Retry-After**

---

## Pitfalls & Interview Follow-ups

1. **Open redirect abuse** — validate URL scheme (block `javascript:`), block internal IPs, SSRF checks
2. **Phishing** — reputation lists, reporting, browser warnings
3. **301 vs 302** — SEO vs analytics trade-off
4. **Collision** ignored with hash-only strategy
5. **Cache thundering herd** on expiry — single-flight
6. **Global uniqueness** across regions — either global ID service or conflict resolution
7. **GDPR** — delete mapping + logs

---

## Cheat Sheet

| Piece | Choice |
|-------|--------|
| ID | Counter + **base62** |
| DB | Postgres → Cassandra at huge scale |
| Hot reads | **Redis** |
| Collision | UNIQUE + retry |
| Limits | Gateway **Redis** token bucket |

---

## Practice

1. Estimate storage for 5 years at 200M URLs/month.
2. Write SQL schema + migration for soft-delete + expiry index.
3. Design **Snowflake-style** ID worker avoiding single counter SPOF.
4. How would you detect malicious URLs before shortening?
