# CloudFront & Route 53

## Status: Complete

---

## Table of Contents

1. [Why CDN + DNS Together](#why-cdn--dns-together)
2. [CloudFront Basics](#cloudfront-basics)
3. [Origins](#origins)
4. [Behaviors](#behaviors)
5. [Cache Key Tuning](#cache-key-tuning)
6. [Origin Shield](#origin-shield)
7. [Lambda@Edge & CloudFront Functions](#lambdaedge--cloudfront-functions)
8. [Signed URLs / Cookies](#signed-urls--cookies)
9. [Route 53 Basics](#route-53-basics)
10. [Routing Policies](#routing-policies)
11. [Health Checks & Failover](#health-checks--failover)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Why CDN + DNS Together

> "**CloudFront** = bring content closer to users. **Route 53** = direct users to the right place. Both run on **400+ AWS edge locations** globally."

Together they:

- **Reduce latency** for global users
- **Absorb traffic spikes** (cache hits don't hit origin)
- **Hide origin** behind edge IPs (DDoS protection — combine with **AWS Shield + WAF**)

---

## CloudFront Basics

> "**Global CDN** with 400+ edge POPs + 13 regional edge caches. Caches HTTP responses, runs edge logic."

### Pricing model

- **Per-request** (HTTP/HTTPS) — varies by region
- **Per-GB egress** to internet — **cheaper than direct EC2/S3** egress (CDN volume discount)
- **Lambda@Edge / CloudFront Functions** — extra
- **Field-level encryption** — extra

### Why it saves money

- Cache hit → no origin request, no origin egress
- Use S3 origin → S3-to-CloudFront free (same AWS), CloudFront-to-user discounted

---

## Origins

> "Where CloudFront fetches the content from on cache miss."

| Origin | Use |
|--------|-----|
| **S3** | Static sites, assets, downloads |
| **ALB / NLB** | Dynamic apps |
| **EC2** | Direct (rare; use ALB) |
| **API Gateway** | Cache REST API responses |
| **MediaStore / MediaPackage** | Video |
| **Custom HTTP** | Any HTTPS endpoint (on-prem, other clouds) |
| **Origin Group** | Primary + failover origin |

### S3 origin best practice

- Use **Origin Access Control (OAC)** — only CloudFront can read S3
- Bucket can stay **private** + **Block Public Access ON**

### ALB/HTTP origin

- Restrict ALB SG to CloudFront IP ranges (or use **AWS-managed prefix list** `com.amazonaws.global.cloudfront.origin-facing`)
- Forward custom header (e.g., `X-Origin-Verify: secret`) to validate

---

## Behaviors

> "**Path-pattern → origin + cache settings + edge logic**. First-match-wins, default behavior catches rest."

```
Path pattern: /api/*       → Origin: ALB,    Cache: disabled, Methods: ALL
Path pattern: /images/*    → Origin: S3,     Cache: 1 yr,    Methods: GET/HEAD
Default      *             → Origin: S3,     Cache: 1 day
```

### What you control per behavior

- Origin
- Allowed HTTP methods
- Cache policy / Origin request policy / Response headers policy
- Lambda@Edge / CloudFront Functions associations
- Compress objects automatically
- Field-level encryption

---

## Cache Key Tuning

> "**Cache key** = what makes a cached response 'unique'. Smaller key = better hit ratio."

Default cache key:

- **URL path + query string** (configurable)
- **Headers** (configurable)
- **Cookies** (configurable)

### Modern way: Cache Policy + Origin Request Policy

| Policy | Controls |
|--------|----------|
| **Cache Policy** | Cache key + TTLs |
| **Origin Request Policy** | What headers/cookies/queries to **forward to origin** (without affecting cache key) |
| **Response Headers Policy** | Add/remove response headers |

### Tip

- **Forward minimum** to origin & cache key
- **`Authorization` in cache key** = no caching across users (intentional for private data)
- **Strip useless query strings** (`utm_*`, tracking) — boosts hit ratio

### Compressed objects

- Enable Brotli + gzip; CloudFront handles content negotiation

---

## Origin Shield

> "**Extra caching layer** in a chosen region between edge POPs and origin. Reduces origin load further."

```
User → Edge POP (Mumbai) → Regional Edge Cache → Origin Shield (us-east-1) → S3 origin (us-east-1)
```

### When good

- Multi-region origin pull pattern → consolidates requests at one shield region
- Origin can't handle request volume even after edge cache
- Live streaming / large catalogs

### Cost

- Extra request fee — measure before enabling

---

## Lambda@Edge & CloudFront Functions

(Also covered in `06-Lambda-and-Serverless.md` — recap.)

| | **CloudFront Functions** | **Lambda@Edge** |
|--|--------------------------|-----------------|
| Runtime | JavaScript (subset) | Node.js / Python |
| Trigger | viewer request / response only | viewer + origin request/response |
| Memory | 2 MB | up to 10 GB |
| Time | < 1 ms | up to 5 s viewer / 30 s origin |
| Cost | Cheap, designed for high-volume header logic | More expensive, full Lambda runtime |
| Use | Header rewrites, redirects, A/B routing | Auth at edge, image resizing, request transforms |

---

## Signed URLs / Cookies

> "**Restrict access to private content** via short-lived signed URLs (single object) or signed cookies (multiple objects)."

### Signed URL

- For: single video, single PDF, time-limited download
- One URL per object

### Signed Cookies

- For: streaming directory of segments (HLS), set of resources
- One cookie covers many resources

### Trust models

- **CloudFront key pair** (legacy) or **trusted KeyGroup** with public key
- Backend signs URL/cookie with private key → CloudFront validates

→ Common for **paid content**, **internal docs**, **DRM-lite**.

---

## Route 53 Basics

> "**DNS service.** Authoritative name server for your domains. Plus health checks + traffic routing."

### Record types

| Type | Use |
|------|-----|
| **A** | IPv4 address |
| **AAAA** | IPv6 |
| **CNAME** | Alias to another hostname (not for apex) |
| **Alias** (Route 53 specific) | Like CNAME but works at apex (`example.com → ALB`) — **free** queries |
| **MX** | Mail server |
| **TXT** | Verification, SPF/DKIM |
| **NS** | Nameserver delegation |
| **SRV** | Service discovery |
| **CAA** | Allowed cert issuers |

### Alias > CNAME for AWS targets

- **Alias** points to ALB/CloudFront/S3/API GW directly — works at apex (`example.com`)
- No charge for alias queries (CNAME charges)

---

## Routing Policies

| Policy | Use case |
|--------|----------|
| **Simple** | Single resource, no decisions |
| **Weighted** | A/B testing, gradual rollout (90% v1, 10% v2) |
| **Latency-based** | Send user to **lowest-latency region** (multi-region deploy) |
| **Failover** | Active-passive — primary down → secondary |
| **Geolocation** | Route by user's country/continent |
| **Geoproximity** (Route 53 Traffic Flow) | Geographic + bias adjustment |
| **Multivalue Answer** | Return up to 8 healthy IPs randomly (DNS-level lightweight LB) |

### Examples

#### Weighted (canary)

```
api.example.com  → ALB-old  (weight: 90)
api.example.com  → ALB-new  (weight: 10)
```

#### Latency-based (multi-region)

```
www.example.com → ALB-mumbai     (region: ap-south-1)
www.example.com → ALB-frankfurt  (region: eu-central-1)
www.example.com → ALB-virginia   (region: us-east-1)
```

DNS resolver returns the region with lowest latency for client.

#### Failover

```
api.example.com  → ALB-primary    (PRIMARY, health check)
api.example.com  → S3-static-page (SECONDARY)
```

#### Geolocation (compliance / language)

```
example.com → site-eu (continent: EU)
example.com → site-in (country: IN)
example.com → site-us (default)
```

---

## Health Checks & Failover

> "**Route 53 actively probes** endpoints — IP, domain, or even **CloudWatch Alarm** as health source."

### Configure

- HTTP/HTTPS endpoint
- Interval: 30 s (default) or 10 s (faster, more $)
- Failure threshold

### Calculated health checks

- Combine multiple child checks (AND/OR/N-of-M) into one parent

### Use cases

- Failover routing primary/secondary
- Remove unhealthy IPs from multivalue answer
- Trigger SNS alarm via CloudWatch

---

## Pitfalls

1. **CNAME at apex** — DNS doesn't allow it; use **Alias**.
2. **Cache TTL too high** for changing content — stale assets in users' cache for hours.
3. **Forwarding all headers** to origin — kills cache hit ratio.
4. **No `Cache-Control` headers** from origin — falls back to default min/max TTLs.
5. **HTTPS not enforced** — set CloudFront to **redirect HTTP → HTTPS**.
6. **OAC not configured** — S3 bucket public, anyone bypasses CloudFront.
7. **Wrong cert region** — edge-optimized API GW + CloudFront need **`us-east-1`** ACM cert.
8. **Latency routing without health checks** — may send to dead region.
9. **TTL = 0 for DNS** — can't cache, every query hits Route 53 (more $).
10. **Failover policy without secondary actually serving meaningful response** — users see error page silently.

---

## Cheat Sheet

| Need | Tool |
|------|------|
| Global static delivery | CloudFront + S3 + OAC |
| Cache REST API | CloudFront in front of API GW |
| Edge auth / redirects | CloudFront Functions |
| Heavy edge logic / image resize | Lambda@Edge |
| Custom domain HTTPS | ACM cert (`us-east-1` for edge) + Route 53 alias |
| Apex record to ALB | **Alias** (not CNAME) |
| Multi-region traffic | **Latency-based** routing |
| Canary rollout | **Weighted** routing |
| Country compliance | **Geolocation** routing |
| Active-passive HA | **Failover** routing + health checks |
| DDoS protection | CloudFront + AWS Shield + WAF |

---

## Practice

1. Front a static **S3** bucket with **CloudFront + OAC** + custom domain via Route 53 alias.
2. Compare cache hit ratio: forward all query strings vs forward none.
3. Implement **redirect HTTP→HTTPS + add HSTS header** via **CloudFront Functions**.
4. Set up **weighted** routing 90/10 for an API blue/green test.
5. Configure **failover** routing: ALB primary, S3 static error page secondary.
6. Generate **signed URL** for an S3 video accessible 5 min only.
