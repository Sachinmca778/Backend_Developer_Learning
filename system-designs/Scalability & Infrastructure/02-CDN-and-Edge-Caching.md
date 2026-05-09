# CDN & Edge Caching

## Status: Complete

---

## Table of Contents

1. [What is a CDN](#what-is-a-cdn)
2. [Why CDN Helps](#why-cdn-helps)
3. [Push CDN vs Pull CDN](#push-cdn-vs-pull-cdn)
4. [Cache-Control Headers](#cache-control-headers)
5. [Cache Key Tuning](#cache-key-tuning)
6. [Origin Shield / Tiered Caching](#origin-shield--tiered-caching)
7. [Purging & Invalidation](#purging--invalidation)
8. [Signed URLs / Cookies](#signed-urls--cookies)
9. [Edge Compute](#edge-compute)
10. [What to Cache (and What Not)](#what-to-cache-and-what-not)
11. [CDN Providers Compared](#cdn-providers-compared)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## What is a CDN

> "**Content Delivery Network** — duniya bhar mein **edge servers (POPs)** at hundreds of locations. Static (and increasingly dynamic) content **users ke nazdeek** cache karta hai → low latency + high throughput."

```
User in Delhi → CDN edge in Mumbai (5 ms)  ✅ cache HIT
                       ↓ cache MISS
                Origin in us-east-1 (200 ms)
```

Examples:

- **CloudFront** (AWS) — 400+ POPs
- **Cloudflare** — 300+ POPs, Workers at edge
- **Fastly** — programmable edge (VCL → Compute@Edge)
- **Akamai** — oldest, enterprise
- **Bunny.net**, **KeyCDN** — affordable alternatives

---

## Why CDN Helps

| Wins | How |
|------|-----|
| **Low latency** | Content served from nearest POP — RTT 5–20 ms vs 100–200 ms cross-region |
| **Reduced origin load** | Cache hits = no origin traffic |
| **Bandwidth cost ↓** | CDN egress cheaper than direct AWS/GCP egress (volume discounts) |
| **DDoS protection** | Edge absorbs floods; AWS Shield + Cloudflare WAF integrated |
| **Failover** | Origin down → stale-while-revalidate / origin failover serves cache |
| **TLS at edge** | Faster TLS handshake (geographic proximity) |

### Rough numbers

| Layer | Latency |
|-------|---------|
| Same-city POP | 5–20 ms |
| Same-region origin | 30–80 ms |
| Cross-continent origin | 150–300 ms |
| **Cache hit at edge** | dominates win |

---

## Push CDN vs Pull CDN

### Pull CDN (most common)

> "**Lazy** — CDN fetches from origin on **first request**, caches per **TTL**, evicts on TTL expiry or LRU."

```
User → POP → MISS → Origin → POP caches → User
User → POP → HIT  → returns from cache
```

✅ Pros:

- Zero pre-upload work
- Auto-handles new content
- Origin remains source of truth

❌ Cons:

- First user pays the latency penalty
- For huge catalogs of long-tail content, **cache hit ratio low**

Use for: **websites, APIs, dynamic apps, e-commerce catalogs**.

### Push CDN

> "**Eager** — content **uploaded** to CDN proactively (build / deploy time). Origin not contacted on user request."

```
Build pipeline → upload assets to CDN → User → POP → always HIT
```

✅ Pros:

- **No origin contact** ever — origin can be smaller / offline
- **Predictable performance** for known content
- Useful for **static-site-only** stacks (no dynamic origin)

❌ Cons:

- **Manual upload** (or pipeline) for every change
- **Wasted storage** for content rarely accessed

Use for: **video VOD libraries, large static asset bundles, software downloads, game patches**.

### Hybrid (modern reality)

Most CDNs are **pull-based**, but you can:

- **Pre-warm** caches by hitting expected URLs after deploy
- **Push** for very large rarely-cached items
- **Stale-while-revalidate** to hide misses

---

## Cache-Control Headers

> "**Origin tells CDN (and browser) how to cache.** Most important header in the CDN world."

### Common directives

| Directive | Meaning |
|-----------|---------|
| **`max-age=N`** | Cache for N seconds (browser + CDN) |
| **`s-maxage=N`** | CDN-only TTL (overrides `max-age` for CDNs/proxies) |
| **`public`** | OK to cache anywhere (CDN, browser) |
| **`private`** | Cache **only** in browser (not CDN) |
| **`no-cache`** | Cache, but **revalidate with origin** before serving (`If-None-Match`) |
| **`no-store`** | **Never cache** — sensitive data |
| **`must-revalidate`** | Once expired, MUST go to origin (no serving stale) |
| **`stale-while-revalidate=N`** | Serve stale up to N sec while refreshing in background |
| **`stale-if-error=N`** | Serve stale up to N sec if origin errors |
| **`immutable`** | Never changes — browser won't even revalidate |

### Examples

```http
# Long-lived static asset (versioned filename)
Cache-Control: public, max-age=31536000, immutable

# HTML page that may change
Cache-Control: public, max-age=60, s-maxage=300, stale-while-revalidate=600

# Personalized API response
Cache-Control: private, max-age=0, no-store

# Logged-in user dashboard
Cache-Control: no-cache, no-store, must-revalidate
```

### Best practice

- **Static assets** (JS, CSS, images) → **versioned filenames** (`app.abc123.js`) + `max-age=1y, immutable`
- **HTML** → short `max-age` (60–300 s) + **SWR**
- **APIs** → cache only **idempotent reads**, with private/short TTL
- **Auth-protected** → `no-store` if sensitive

### ETag & Conditional Requests

```http
ETag: "abc123"           ← origin sends
If-None-Match: "abc123"  ← client sends on revalidate
→ Origin: 304 Not Modified (no body) — saves bandwidth
```

---

## Cache Key Tuning

> "**Cache key** = what makes a cached response unique. Smaller key = better hit ratio."

Default: **URL** (host + path + query string).

### Knobs

| Knob | Effect |
|------|--------|
| **Forward query strings** | Cache varies per query — for `?id=42` distinct entries |
| **Strip query strings** | Same content for any `?utm_*` — high hit rate |
| **Forward headers** | `Accept-Language`, `Authorization` → per-language / per-user variants |
| **Forward cookies** | Per-cookie variants — usually too granular |
| **Vary header** | Tell CDN/browser to vary by header (`Vary: Accept-Encoding`) |

### Tip

- **Strip tracking params** (`utm_source`, `gclid`, `fbclid`) → boosts hit ratio
- Forward only what your app actually uses
- Avoid forwarding `Authorization` to public cache (cache leak risk)

### CloudFront Cache Policy + Origin Request Policy

- **Cache Policy** — defines cache key + TTLs
- **Origin Request Policy** — what gets forwarded to origin (without affecting cache key)
- Lets you **forward more to origin** than your cache key, e.g., logging headers

---

## Origin Shield / Tiered Caching

> "**Extra cache layer** between edge POPs and origin. Reduces origin load further."

```
User → Edge POP (Mumbai) → Origin Shield (us-east-1) → Origin
```

### When useful

- **Many POPs**, each rarely sees same key — without shield, many edge misses → many origin requests
- **Live streaming** — collapse parallel requests for same segment
- **Large catalog** with regional access patterns

### Cost

- Extra hop = extra request fee — measure before enabling

---

## Purging & Invalidation

> "**Force-evict** content from CDN — for breaking content updates, takedowns, security."

| Method | Speed | Cost |
|--------|-------|------|
| **Path/URL purge** | Seconds (Cloudflare/Fastly) to minutes (CloudFront) | Per-purge fee on some CDNs |
| **Wildcard purge** (`/images/*`) | Slower | More expensive |
| **Tag-based purge** (Fastly surrogate keys, Cloudflare cache tags) | Seconds, surgical | Best for grouping content |
| **Purge all** | Slow, dangerous (cache cliff → origin DDoS) | Avoid |

### Best practice

- **Versioned URLs** (`/v2/app.js`) → no purge needed; just deploy with new path
- **Tag-based** for grouped content (e.g., all blog posts of category X)
- **Soft purge** (Fastly) — mark stale but keep for SWR

---

## Signed URLs / Cookies

> "**Time-limited or restricted access** to private content via cryptographic signature."

### Signed URL

```
https://cdn.example.com/video.mp4?Expires=...&Signature=...&KeyPairId=...
```

- Per-object access
- Time-limited
- Can include IP restriction
- Use for: paid content, private documents, one-time downloads

### Signed Cookies

- Cover **multiple URLs** (entire `/private/*` path)
- Useful for: **HLS video streaming** (many .ts segments under one stream), private dashboards

### Trust models (CloudFront)

- **Trusted KeyGroup** with public key (modern)
- **Trusted Signers** with key pair (legacy)
- Backend signs, CDN verifies

---

## Edge Compute

> "**Run code at edge POPs** — modify request/response, do auth, A/B test, image resize, headers."

| Provider | Edge runtime |
|----------|--------------|
| **CloudFront Functions** | JS subset, <1 ms, viewer events only |
| **Lambda@Edge** | Node/Python, full Lambda, viewer + origin events |
| **Cloudflare Workers** | V8 isolates (JS, Rust via WASM), <50 ms cold start |
| **Fastly Compute@Edge** | WASM (any language) |
| **Vercel Edge Functions** | V8 isolates |
| **Akamai EdgeWorkers** | JS |

### Common use cases

- **Auth at edge** — verify JWT before hitting origin
- **A/B testing** — set cookie, route to variant
- **Header rewrites** — add security headers (CSP, HSTS, X-Frame-Options)
- **Geo routing** — different content per country
- **Image resizing** on the fly
- **Bot blocking / WAF**

---

## What to Cache (and What Not)

### ✅ Cache aggressively

- Static assets (JS, CSS, images, fonts)
- Video / audio segments (HLS, DASH)
- Public API GET endpoints (with `max-age` + `Vary`)
- Marketing pages, blog posts
- API responses with no PII (config, catalog)

### ⚠️ Cache carefully

- Personalized pages (use **`Vary: Cookie`** or per-user URL)
- Search results (high cardinality of query strings)
- Pricing / inventory (short TTL + invalidation)

### ❌ Don't cache

- POST/PUT/DELETE responses (almost never)
- Auth-protected endpoints with sensitive data (use `no-store`)
- Real-time data (stock prices, live scores) — unless sub-second SWR

---

## CDN Providers Compared

| | **CloudFront** | **Cloudflare** | **Fastly** | **Akamai** |
|--|----------------|-----------------|-------------|--------------|
| Pricing | Per-request + GB | Free tier huge; pro $20/mo+ | Per-request + GB; minimum spend | Enterprise contracts |
| Edge compute | CF Functions, Lambda@Edge | Workers (V8) | Compute@Edge (WASM) | EdgeWorkers |
| Programmability | Limited | High | Highest (VCL → Compute) | Medium |
| DDoS / WAF | Shield + WAF | Built-in, strong | Yes | Strongest, premium |
| Free TLS / certs | ACM | Free at all tiers | Yes | Yes |
| Best for | AWS-native stack | Startups, generalist | Performance-critical, programmable | Enterprise, video |

---

## Pitfalls

1. **No `Cache-Control` headers** → CDN falls back to defaults (might cache too long or too short).
2. **Forwarding `Authorization` to public cache** → cache leak, served to other users.
3. **Caching personalized pages** without `Vary` → wrong user sees other's data.
4. **Forwarding all query strings** → low cache hit ratio.
5. **No versioning of static assets** → can't purge effectively, browsers serve stale.
6. **Aggressive purge-all** → cache cliff, origin overwhelmed.
7. **POST cached** because of misconfigured intermediary.
8. **No HTTPS at edge** → MITM risk, browser warnings.
9. **Origin without rate limiting** → cache miss flood becomes DDoS.
10. **Long TTL for changing content** → stale data hours.
11. **No `stale-if-error`** → CDN returns 5xx when origin blips.
12. **Not measuring cache hit ratio** — you're optimizing blind.

---

## Cheat Sheet

| Need | Approach |
|------|----------|
| Static asset | Versioned filename + `max-age=1y, immutable` |
| HTML | `max-age=60` + **SWR** |
| API GET (public) | `s-maxage=N` + minimal cache key |
| Personalized | `Vary: Cookie` or `private` |
| Auth content | Signed URLs / cookies |
| Edge logic | CF Functions / Workers |
| Hide misses | `stale-while-revalidate` |
| Hide errors | `stale-if-error` |
| Force update | Versioned URL or tag purge |

| Header | Use |
|--------|-----|
| `Cache-Control: public, max-age=N` | Browser + CDN cache |
| `Cache-Control: s-maxage=N` | CDN only |
| `Cache-Control: private` | Browser only |
| `Cache-Control: no-store` | Never cache |
| `ETag` + `If-None-Match` | Conditional revalidation |
| `Vary: X` | Cache per X header |

---

## Practice

1. Configure CloudFront in front of S3 + ALB; static `/assets/*` → S3, `/api/*` → ALB no-cache.
2. Set `Cache-Control: public, max-age=31536000, immutable` for hashed assets, measure hit ratio.
3. Implement **stale-while-revalidate=600** on HTML pages; observe behavior on origin error.
4. Build a **CloudFront Function** that adds HSTS + X-Frame-Options headers globally.
5. Generate a **signed URL** for a private S3 video (5 min expiry).
6. Diagnose: cache hit ratio is 30% — list 5 suspects (query strings, headers, TTL, key, etc.).
