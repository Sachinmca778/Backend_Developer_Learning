# API Gateway Pattern

## Status: Complete

---

## Table of Contents

1. [What is an API Gateway](#what-is-an-api-gateway)
2. [Core Responsibilities](#core-responsibilities)
3. [Routing](#routing)
4. [Authentication & Authorization](#authentication--authorization)
5. [Rate Limiting](#rate-limiting)
6. [SSL / TLS Termination](#ssl--tls-termination)
7. [Request / Response Transformation](#request--response-transformation)
8. [Logging, Tracing, Metrics](#logging-tracing-metrics)
9. [Circuit Breaker & Resilience](#circuit-breaker--resilience)
10. [Caching](#caching)
11. [BFF — Backend for Frontend](#bff--backend-for-frontend)
12. [API Gateway vs Service Mesh](#api-gateway-vs-service-mesh)
13. [Tools Compared](#tools-compared)
14. [Pitfalls](#pitfalls)
15. [Cheat Sheet](#cheat-sheet)

---

## What is an API Gateway

> "**Single entry point** for all client requests. Microservices ke saamne baith ke routing, auth, rate limit, logging — saare cross-cutting concerns ek jagah handle karta hai."

```
Mobile / Web / Partner clients
            ↓
     [ API Gateway ]
            ↓
  ┌─────────┼─────────┐
  ↓         ↓         ↓
User      Order     Inventory
Service   Service   Service
```

Without gateway → har client har microservice ke saath alag deal kare → mess (auth duplicated, rate limit per-service, partial outage hard to handle).

---

## Core Responsibilities

| Concern | What gateway does |
|---------|---------------------|
| **Routing** | Path/host/header → backend service |
| **Authn / Authz** | Validate JWT/OAuth/API key, set user context |
| **Rate limiting** | Throttle per user / API key / IP |
| **TLS termination** | Decrypt HTTPS at edge |
| **Transformation** | Reshape request/response (e.g., gRPC ↔ REST) |
| **Logging / metrics / tracing** | Centralized observability |
| **Circuit breaker** | Fast-fail when downstream unhealthy |
| **Caching** | Cache GET responses |
| **Versioning** | `/v1/` vs `/v2/` paths |
| **CORS** | Cross-origin handling for browsers |
| **API composition** | Aggregate calls to multiple services into one client response |
| **Schema validation** | Reject malformed requests |

---

## Routing

> "**Path / host / header / method** based routing — har request right backend ko jaaye."

```
GET  /v1/users/*       → user-service
POST /v1/orders        → order-service
*    /admin/*          → admin-service (with extra auth)
*    api.example.com   → public-API gateway
*    internal.example.com → internal-services gateway
```

### Patterns

- **Path-based** (`/users/*` → user-service) — most common
- **Host-based** (`api.example.com` vs `admin.example.com`)
- **Header-based** (`X-Tenant: acme` → tenant-specific routing)
- **Method-based** (different rate limits for GET vs POST)
- **Weighted routing** (canary, A/B)

### Service discovery

- **Static config** — list backends in config (small clusters)
- **Dynamic** — pull from Consul / Eureka / Kubernetes API (modern)
- **Sidecar service mesh** — gateway delegates to mesh

---

## Authentication & Authorization

> "**Centralized auth** at gateway — backends trust gateway-set headers (`X-User-ID`, `X-Roles`)."

### Common schemes

| Scheme | Use |
|--------|-----|
| **JWT** (Bearer token) | Stateless, scalable; gateway validates signature |
| **OAuth 2.0 / OIDC** | Federated login (Google, GitHub, Cognito) |
| **API key** | Server-to-server, partner APIs |
| **Mutual TLS (mTLS)** | High-trust internal/B2B |
| **HMAC** | Webhook signatures |
| **Session cookie** | Browser apps with stateful session |

### Pattern

```
Client → Gateway
            ↓ validate JWT
            ↓ inject X-User-ID, X-Roles headers
            ↓ remove auth headers (don't forward to backend)
         Backend (trusts headers)
```

### Authorization

- **Coarse-grained** at gateway (e.g., must have `admin` role for `/admin/*`)
- **Fine-grained** at service (object-level — "can user X edit document Y?")
- Don't put complex business authz at gateway — too coupled

### Trust boundary

- **Backend services** must NOT trust client headers (`X-User-ID`) directly
- Only trust headers **set by gateway** (set after stripping any client-supplied versions)

---

## Rate Limiting

> "**Throttle requests** to protect backends from abuse / runaway clients."

### Algorithms

| Algorithm | Description | Use |
|-----------|-------------|-----|
| **Fixed window** | Counter resets every N seconds (e.g., per minute) | Simple but bursty at boundary |
| **Sliding window log** | Track timestamps; count in last N sec | Accurate, more memory |
| **Sliding window counter** | Weighted prev + curr window | Approximate, efficient |
| **Token bucket** | Refill rate + bucket size; allows bursts | Most common |
| **Leaky bucket** | Constant drain rate | Smooth shaping |

### Token bucket example

```
bucket capacity = 10 tokens
refill rate     = 1 token / sec

Each request consumes 1 token.
If bucket empty → 429 Too Many Requests.
Allows bursts up to 10 then sustained 1/sec.
```

### Where to apply

- **Per IP** — protect against scrapers / DDoS
- **Per user / API key** — fairness across clients
- **Per endpoint** — expensive endpoints get tighter limit
- **Global** — protect overall capacity

### Implementation

- **Local memory** — only works for single gateway instance
- **Distributed (Redis)** — counters in Redis with Lua scripts (atomic) — works across N gateways
- **Managed** (AWS API Gateway, Cloudflare) — built-in

### Response

- **429 Too Many Requests** with `Retry-After` header
- Optional: include `X-RateLimit-*` headers (limit, remaining, reset)

---

## SSL / TLS Termination

> "**HTTPS decrypt at gateway**, plain HTTP to internal services (or re-encrypt for compliance)."

(Detailed in [01 — Load Balancing § TLS Termination](./01-Load-Balancing.md#ssltls-termination).)

### At gateway specifically

- Centralized **cert management** (ACM auto-renew)
- **HTTPS only** (redirect HTTP → HTTPS)
- Modern TLS 1.3, strong ciphers
- HSTS, CSP, X-Frame-Options headers added by gateway

---

## Request / Response Transformation

> "**Reshape requests/responses** between client expectations and backend reality."

### Examples

- **REST ↔ gRPC** — client sends REST, backend speaks gRPC
- **GraphQL ↔ multiple REST** — gateway aggregates
- **Header injection / stripping** — add `X-Request-ID`, strip internal headers
- **Field renaming** (`first_name` → `firstName`)
- **Versioning shim** — old `/v1` clients still work after backend `/v2` upgrade
- **Compression / decompression**
- **JSON ↔ XML**

### Tools

- **AWS API Gateway** REST: VTL templates (powerful but ugly)
- **Kong**: plugins
- **Spring Cloud Gateway**: filter functions in Java
- **Envoy**: WASM filters / Lua

> Keep transformation **light** — heavy logic belongs in a service, not the gateway.

---

## Logging, Tracing, Metrics

> "**Single point** for cross-cutting observability."

| Concern | What gateway does |
|---------|---------------------|
| **Access logs** | Every request: method, path, status, duration, user — to CloudWatch / Datadog / ELK |
| **Distributed tracing** | Inject **trace ID** header (X-Request-ID, traceparent) — propagate to backends (X-Ray / Jaeger / OTel) |
| **Metrics** | RPS, latency p50/p99, error rate per route — Prometheus / CloudWatch |
| **Audit log** | Sensitive endpoints — who accessed what when |

### Trace ID pattern

```
Client request → Gateway generates X-Request-ID: uuid
              → Adds to log line
              → Forwards as header to backend
              → Backend logs include same X-Request-ID
              → Search across all services by ID
```

→ **Mandatory** in microservices — without trace ID, debugging multi-service request = nightmare.

---

## Circuit Breaker & Resilience

> "**Detect failing backend**, **stop calling it temporarily**, **fail fast**. Saves caller from waiting on dead service."

### State machine

```
   CLOSED  (normal)  ──50% errors──►  OPEN  (fail fast)
      ▲                                  │
      │                          after timeout
      │                                  ▼
      └──── successes ──── HALF_OPEN (test with 1 request)
```

### Behavior in OPEN

- Return cached response, default value, or **error fast** (don't wait timeout)
- Saves caller threads / connections
- Gives downstream time to recover

### Other resilience patterns

| Pattern | Description |
|---------|-------------|
| **Timeout** | Max wait before giving up (always set!) |
| **Retry** | With backoff + jitter; idempotent operations only |
| **Bulkhead** | Isolate resource pools per backend (one slow backend doesn't drain all threads) |
| **Fallback** | On failure return cached / static response |
| **Rate limit (above)** | Prevent overload |
| **Hedged requests** | Send to 2 backends, take faster; cancel slower |

### Tools

- **Resilience4j** (Java)
- **Polly** (.NET)
- **Istio / Envoy** built-in circuit breakers (config-driven)
- **AWS API Gateway** has timeouts but limited circuit breaker — pair with downstream resilience

---

## Caching

> "**Cache GET responses** at gateway** to reduce backend load and latency."

### Configurable

- Per-route TTL
- Cache key includes URL + query + selected headers
- Invalidate on write (or short TTL + accept staleness)
- Often **CloudFront / CDN does this better** for public content

### When useful

- Read-heavy idempotent endpoints
- Stable cache keys
- Tolerable staleness (catalog, config, public data)

### When not

- Per-user personalized data (cache key explosion)
- Real-time data
- Sensitive data (cache leak risk)

→ For most personalized APIs, cache in **app + Redis**, not gateway.

---

## BFF — Backend for Frontend

> "**One gateway per client type** — mobile, web, partner each get own BFF tailored to their needs."

```
        Mobile App ─► Mobile BFF ──┐
                                    │
        Web App    ─► Web BFF    ──┼──► Microservices (User, Order, Inventory, ...)
                                    │
        Partner    ─► Partner BFF ─┘
                       (API key auth, different rate limits)
```

### Why separate BFFs

| Reason | Explanation |
|--------|-------------|
| **Different needs** | Mobile = small payloads, web = richer, partner = stable contracts |
| **Different release cadence** | Mobile takes weeks (app store); web ships daily |
| **Different auth** | Mobile JWT, web cookies, partner API keys |
| **Different aggregation** | Mobile combines 3 calls into 1; web shows raw data |
| **Independent ownership** | Mobile team owns mobile BFF, web team owns web BFF |

### Anti-patterns

- **One BFF for all clients** — kills the whole point
- **Business logic in BFF** — keep it in domain services
- **BFF calling BFF** — unnecessary indirection

### Modern BFF

- **GraphQL gateway** (Apollo Federation, Hasura) often serves as natural BFF — clients query exactly what they need
- **tRPC / gRPC-Web** for typed contracts

---

## API Gateway vs Service Mesh

> "**Different problems.** Gateway = north-south traffic (clients ↔ services). Service mesh = east-west (service ↔ service inside cluster)."

| | **API Gateway** | **Service Mesh** |
|--|------------------|--------------------|
| Traffic direction | North-south (in/out) | East-west (internal) |
| Clients | External | Internal services |
| Auth | End-user (JWT, OAuth) | Service identity (mTLS) |
| Examples | Kong, AWS API Gateway, Spring Cloud Gateway | Istio, Linkerd, Consul, AWS App Mesh |
| Per-service sidecar | No | Yes (Envoy proxy per pod) |
| Observability | Edge metrics | Per-service mesh metrics |

### Use both at scale

- **Gateway** handles client traffic, auth, rate limits
- **Mesh** handles service-to-service mTLS, retries, traffic shifting (canary internal)

---

## Tools Compared

| Tool | Strengths | Notes |
|------|-----------|-------|
| **AWS API Gateway** | Managed, integrated with Lambda/IAM | REST (full features) vs HTTP (cheaper, simpler) |
| **Kong** | OSS, plugins ecosystem, runs anywhere | Built on Nginx + Lua / now also Go |
| **Apigee** (Google) | Enterprise, monetization, analytics | Heavy, paid |
| **Spring Cloud Gateway** | Java, code-driven filters | Embedded in Spring stack |
| **Envoy + Istio** | Modern, gRPC-first, dynamic config | Service mesh + ingress combined |
| **Nginx + Nginx Plus** | High perf, ubiquitous | Plus has GUI, dynamic reconfig |
| **Traefik** | Auto-discover (Docker/K8s), Let's Encrypt | Cloud-native focus |
| **Tyk** | OSS, multi-cloud, dashboard | Enterprise tier |
| **Cloudflare API Shield** | DDoS + WAF + schema validation at edge | SaaS |

---

## Pitfalls

1. **Gateway as monolith of business logic** → fat gateway, deployment bottleneck.
2. **No circuit breaker** → one slow service drags everything down.
3. **No timeouts** → threads exhausted on slow downstream.
4. **Trusting client headers** for user identity → auth bypass.
5. **Single gateway instance** → SPOF; deploy multi-AZ.
6. **No rate limiting** → easy DDoS vector.
7. **Caching personalized responses** → wrong user sees other's data.
8. **Sync aggregation chains** in gateway → high latency, fragile.
9. **One BFF for all** → defeats the pattern.
10. **Skipping trace ID** → multi-service debugging hell.
11. **Heavy transformation** in gateway → CPU bottleneck.
12. **No schema validation** → garbage requests reach backends.
13. **Tight client coupling** to internal microservice paths — defeats abstraction.

---

## Cheat Sheet

| Concern | Gateway tool/feature |
|---------|---------------------|
| Routing | path/host/header rules |
| Auth | JWT validation + header injection |
| Rate limit | Token bucket per user/IP |
| TLS | Termination at edge |
| Transformation | Light only — VTL / filters |
| Logging | X-Request-ID + structured logs |
| Tracing | OTel / X-Ray header propagation |
| Resilience | Timeout + retry + circuit breaker |
| Caching | GET only, short TTL |
| Per-client tailoring | **BFF** |

| Pattern | Use |
|---------|-----|
| **API Gateway** | North-south client traffic |
| **BFF** | Per-client-type tailored gateway |
| **Service Mesh** | East-west internal service traffic |

---

## Practice

1. Design API gateway for an e-commerce: routes, auth, rate limit per user vs per IP.
2. Implement **token bucket** in Redis with Lua (atomic) for rate limiting.
3. Set up **JWT validation** + `X-User-ID` injection in Spring Cloud Gateway.
4. Add **circuit breaker** (Resilience4j) for a flaky downstream; observe behavior.
5. Build separate **mobile BFF** and **web BFF** for same backend services.
6. Configure **timeout (3s) + retry (1) + jitter** for an upstream call; explain trade-offs.
