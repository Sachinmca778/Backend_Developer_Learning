# Load Balancing

## Status: Complete

---

## Table of Contents

1. [What is Load Balancing](#what-is-load-balancing)
2. [Layer 4 (Transport)](#layer-4-transport)
3. [Layer 7 (Application)](#layer-7-application)
4. [L4 vs L7 Comparison](#l4-vs-l7-comparison)
5. [Load Balancing Algorithms](#load-balancing-algorithms)
6. [Health Checks](#health-checks)
7. [Sticky Sessions](#sticky-sessions)
8. [SSL/TLS Termination](#ssltls-termination)
9. [Global Load Balancing (GSLB)](#global-load-balancing-gslb)
10. [High Availability of LB Itself](#high-availability-of-lb-itself)
11. [Tools Compared](#tools-compared)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## What is Load Balancing

> "**Load balancer** = traffic ka **traffic cop** — incoming requests ko **multiple backend servers** mein distribute karta hai. Without it: ek server pe load → crash. With it: smooth scaling, HA, rolling deploys."

LB ke 4 main jobs:

1. **Distribute** load across N servers
2. **Health check** — dead servers ko traffic mat bhejo
3. **Failover** — instance fail ho to baaki pe shift
4. **Decouple clients** from server count / IPs

---

## Layer 4 (Transport)

> "**TCP / UDP level** par work karta hai. Sirf **IP + port** dekh ke route karta hai — payload **read nahi karta**. Bahut fast, lightweight."

```
Client → L4 LB
            ├── inspects: src IP, dst IP, port, protocol (TCP/UDP)
            ├── decides backend (algorithm)
            └── forwards packets unchanged
```

### Pros

- **Very fast** — no packet inspection beyond headers
- **Protocol agnostic** — kuch bhi TCP/UDP route kar sakta hai (DBs, Redis, custom protocols)
- **Lower CPU/memory** footprint

### Cons

- **Can't inspect URL/headers** — `/api/v1/users` aur `/static/img.png` distinguish nahi kar sakta
- **Sticky session** sirf IP-based (NAT'd users sab same backend = unbalanced)

### Examples

- **AWS NLB** (Network Load Balancer)
- **HAProxy** in TCP mode
- **Nginx stream module**
- **LVS / IPVS** (Linux kernel-level)

### Use when

- Game servers (UDP)
- DB connection pooling proxy (RDS Proxy, PgBouncer in front of Postgres)
- WebSocket bulk forwarding
- Need **millions of CPS** (connections per second)
- Lower-level routing — protocols other than HTTP

---

## Layer 7 (Application)

> "**HTTP / gRPC level** par work karta hai. **URL, headers, cookies, body** sab read kar sakta hai → smart routing decisions."

```
Client → L7 LB
            ├── parses HTTP request fully
            ├── routing: path / host / header / cookie based
            ├── can rewrite headers, paths
            ├── can do auth, rate limit, WAF
            └── forwards (often new TCP connection to backend)
```

### Pros

- **Smart routing** — `/api/*` → service A, `/static/*` → service B
- **Path/host-based virtual hosting**
- **Header injection** (X-Forwarded-For, request ID)
- **Compression, caching, WAF, auth** at edge
- **A/B testing**, **canary deploys** by header / cookie

### Cons

- **Slower** than L4 (parses HTTP)
- **HTTPS termination** required to inspect (or use SNI for routing only)
- Higher CPU/memory

### Examples

- **AWS ALB** (Application Load Balancer)
- **Nginx**, **HAProxy** in HTTP mode
- **Envoy** (modern, Istio uses it)
- **Traefik** (auto-discovers services)

### Use when

- HTTP / REST / gRPC microservices
- Multiple services behind one domain
- Need URL/header routing
- Want SSL termination + WAF + caching at LB

---

## L4 vs L7 Comparison

| Feature | **L4 (NLB)** | **L7 (ALB)** |
|---------|--------------|---------------|
| Inspects | IP, port, protocol | URL, headers, body |
| Speed | Very fast | Slower (parsing) |
| Routing | Per connection | Per request |
| Sticky sessions | IP-based | Cookie-based (precise) |
| TLS termination | Pass-through or terminate | Termination + re-encrypt |
| Path-based routing | No | Yes |
| Host-based routing | Limited (SNI) | Yes |
| WAF / auth | No | Yes |
| WebSocket | Yes (transparent) | Yes (with config) |
| HTTP/2, gRPC | Pass-through | Native |
| Cost (AWS) | Per-LCU, cheaper for high BW | Per-LCU, cheaper for HTTP |
| Use cases | DB proxy, gaming, raw TCP | Web/APIs, microservices |

---

## Load Balancing Algorithms

### 1. Round Robin (RR)

> "**Sequential** — server 1, 2, 3, 1, 2, 3, ..."

- **Simplest**, default in most LBs
- Assumes **uniform server capacity** + **uniform request cost**
- Bad if requests have wildly different costs (one slow, one fast)

### 2. Weighted Round Robin

> "**Weights**: server A=3, B=1 → ratio 3:1 traffic."

- Use when **heterogeneous hardware** (some servers bigger)
- Or **gradual deploy**: new version weight=1 (10%), old weight=9 (90%) → canary

### 3. Least Connections

> "**Send to backend with fewest active connections.**"

- Better for **long-lived connections** (WebSocket, DB pool, video streaming)
- Self-balances when request durations vary
- Default for many production setups

### 4. Least Response Time

> "Combines **active connections** + **observed latency**."

- Smartest general-purpose algorithm
- HAProxy `leastconn` + `slowstart` etc.

### 5. IP Hash

> "**Hash client IP** → deterministically map to backend. Same IP = same backend."

- Lightweight **session stickiness** without cookies
- **Problem**: NAT'd users (offices, mobile carriers) lump on one backend → uneven
- Use only if you really need IP-based affinity

### 6. Consistent Hashing

> "**Hash key** → ring → next clockwise backend. Adding/removing backend moves ~1/N of keys."

- Used by **CDNs** (key = URL → cache shard)
- **Memcached / Redis cluster clients** (Ketama)
- **Stateful services** where each key has a "home" backend

### 7. Random / Random Two-Choice

> "**Pick 2 at random**, send to less-loaded one. Surprisingly good — close to optimal with simple math."

- Cheap, near-optimal for stateless workloads
- Used internally in many large-scale systems (Google, Microsoft)

### Algorithm choice guide

| Workload | Algorithm |
|----------|-----------|
| Stateless HTTP, uniform | **Round Robin** |
| Heterogeneous capacity | **Weighted RR** |
| Long-lived connections | **Least Connections** |
| Mixed request cost | **Least Response Time** |
| Need stickiness, no cookies | **IP Hash** |
| Cache shard / key-affinity | **Consistent Hashing** |
| Default modern fallback | **Random Two-Choice** |

---

## Health Checks

> "**LB periodically pings each backend** to know who's alive. Fail check → remove from rotation. Pass again → re-add."

### Two flavors

| Type | Description |
|------|-------------|
| **Active (probing)** | LB sends synthetic request (HTTP GET `/health`) every N sec |
| **Passive (observing)** | LB monitors actual traffic — high error rate / timeouts → mark unhealthy |

### Health check best practices

- **Dedicated `/health` endpoint** — not behind auth, fast, no DB hit
- **`/health` (liveness)** vs **`/ready` (readiness)** distinction:
  - **Liveness** — am I alive? (restart if not)
  - **Readiness** — am I ready to serve? (warmed cache, DB conn open)
- **Interval** 5–30 sec, **threshold** 2–3 consecutive failures (avoid flap on transient blip)
- **Slow-start** — newly added backend gets traffic ramped up gradually
- **Deep health check** — touches DB / cache lightly to detect downstream outages

### Anti-pattern

- `/health` returns 200 always, ignores DB outage → LB sends traffic to broken backend → cascading 500s

---

## Sticky Sessions

> "**Same client → same backend** for duration of session. Useful for in-memory session state."

### Implementations

| Method | How |
|--------|-----|
| **Source IP affinity** | Hash client IP (L4 / L7) |
| **Cookie-based** (LB-issued) | LB sets cookie like `AWSALB=xyz`, routes by it |
| **Application cookie** | App-issued (e.g., `JSESSIONID`) — LB hashes it |

### Trade-offs

- ✅ Allows **in-memory session** without distributed store
- ❌ **Uneven load** if some clients heavier
- ❌ **Backend death** = session lost → use Redis-backed session instead
- ❌ Hard to **drain** instances cleanly (must wait for sessions to expire)

> **Modern best practice**: **stateless backends** + **Redis/JWT session** → no stickiness needed. Sticky sessions are a smell.

---

## SSL/TLS Termination

> "**Decrypt HTTPS at LB**, send plain HTTP to backends. Saves backend CPU + lets LB inspect request."

```
Client ──HTTPS──► LB ──HTTP (or new HTTPS)──► Backend
```

### Variants

| Mode | Description |
|------|-------------|
| **TLS termination** | LB decrypts, talks plain HTTP to backend (cheaper, internal network trusted) |
| **TLS bridging / re-encryption** | LB decrypts, re-encrypts to backend (defense in depth, compliance) |
| **TLS passthrough** | LB doesn't decrypt; passes encrypted bytes (L4 mode; backend handles cert) |

### Why terminate at LB

- **Centralized cert management** (ACM auto-renew)
- **Inspect for routing / WAF**
- **Save backend CPU** (TLS is ~5–15% CPU)
- Easier **TLS upgrades** (1.3, ciphers)

### Don't terminate when

- End-to-end encryption mandated (HIPAA, PCI in some configs)
- Backend needs client cert (mTLS)
- LB doesn't need to inspect

---

## Global Load Balancing (GSLB)

> "**DNS-level load balancing** — direct users to **nearest / healthiest region**. Each region usually has its own L4/L7 LB locally."

```
User in Mumbai
   ↓ DNS query
Route 53 (latency-based)
   ↓ returns IP of ALB in ap-south-1
Mumbai ALB → Mumbai backends
```

### Strategies (Route 53 example)

- **Latency-based routing** — lowest RTT region
- **Geolocation routing** — by country / continent
- **Weighted routing** — gradual region rollout
- **Failover routing** — primary down → secondary region
- **Multivalue answer** — DNS-level lightweight LB (returns N healthy IPs)

### Anycast (alternative to DNS GSLB)

- Same IP advertised from multiple regions; BGP routes user to nearest
- Used by **Cloudflare**, **CloudFront**, **AWS Global Accelerator**
- Faster failover than DNS (no TTL caching delay)

---

## High Availability of LB Itself

> "**LB is single point of failure** if you only have one. Always **multi-instance**."

### Patterns

| Pattern | How |
|---------|-----|
| **Active-passive** | Two LBs, one floating IP (VRRP / keepalived) |
| **Active-active** | DNS round robin or anycast across multiple LBs |
| **Managed cloud LB** | AWS ALB/NLB are auto multi-AZ, no DIY |

### AWS ALB/NLB

- Internally **multi-AZ**, multiple ENIs across AZs
- Each AZ has its own LB node — DNS returns one per AZ
- Cross-zone load balancing (default ON for ALB, configurable for NLB)

---

## Tools Compared

| Tool | L4 | L7 | Strengths | Notes |
|------|-----|-----|-----------|-------|
| **AWS ALB** | – | ✅ | Managed, easy, integrates with AWS | Per-LCU pricing |
| **AWS NLB** | ✅ | – | Millions of CPS, static IP, low latency | TCP/UDP only |
| **AWS Global Accelerator** | ✅ | – | Anycast IPs, fast cross-region failover | Premium |
| **Nginx** | ✅ | ✅ | Open source, fast, ubiquitous | Config files; commercial Plus |
| **HAProxy** | ✅ | ✅ | Battle-tested, rich algorithms | Config files |
| **Envoy** | ✅ | ✅ | Modern, dynamic config (xDS), gRPC native | Used by Istio, AWS App Mesh |
| **Traefik** | – | ✅ | Auto-discover (Docker, K8s), Let's Encrypt | Cloud-native |
| **Caddy** | – | ✅ | Auto HTTPS, simple config | Smaller community |
| **Cloudflare** | ✅ | ✅ | DDoS, CDN, WAF bundled | SaaS |

---

## Pitfalls

1. **No health check** → traffic to dead backend.
2. **`/health` returns 200 always** → useless, masks outage.
3. **Single LB instance** → SPOF; use multi-AZ / managed.
4. **Sticky sessions everywhere** → uneven load, hard to drain instances.
5. **L4 when you need URL routing** → bolt-on hacks.
6. **L7 for raw TCP** → unnecessary cost / complexity.
7. **TLS termination without re-encryption** in regulated env → audit fail.
8. **Round Robin with mixed request costs** → uneven CPU.
9. **No connection draining** during deploy → in-flight requests killed.
10. **Aggressive health-check thresholds** → flap on transient blips.
11. **DNS-only GSLB without app-level fallback** → DNS TTL caching delays failover.
12. **Slow-start ignored** → cold instance overwhelmed at full load.

---

## Cheat Sheet

| Decision | Choose |
|----------|--------|
| HTTP / REST / gRPC | **L7 (ALB / Nginx / Envoy)** |
| Raw TCP / UDP / DB proxy | **L4 (NLB / HAProxy TCP)** |
| Cross-region failover | **Route 53 + per-region LB** or **Global Accelerator** |
| Cache-key affinity | **Consistent Hashing** |
| Long-lived connections | **Least Connections** |
| Heterogeneous fleet | **Weighted RR** |
| Stateless web | **Round Robin** |

| Pitfall | Fix |
|---------|-----|
| Dead backend served | Real `/health` checks |
| Cold instance overload | Slow-start |
| Single LB SPOF | Multi-AZ / managed |
| Lost sessions on failover | Redis-backed session, not stickiness |

---

## Practice

1. Diagram traffic flow: User → Route 53 (latency) → ALB → ECS service in 3 AZs.
2. Choose algorithm for: WebSocket chat / static asset host / DB proxy / cache shards.
3. Configure deep health check: liveness vs readiness for a Spring Boot app.
4. Migrate sticky-session app to **stateless + Redis session** — what changes?
5. Compare cost / perf: NLB vs ALB for 1M RPS HTTP traffic in AWS.
6. Set up Nginx with **least_conn + slow_start** for a backend pool of 5.
