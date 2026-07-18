# Scalability & Infrastructure - Top 10 Interview Questions & Answers

> Covering: Load Balancing, CDN & Edge Caching, Message Queues, API Gateway, and scaling trade-offs

---

## Q1: Scalability aur reliability mein difference kya hai?

**Answer:**

| Term | Meaning |
|------|---------|
| **Scalability** | Load badhne par system performance maintain kare |
| **Reliability** | Failures ke baad bhi system correct service de |

### Example:
- 1K RPS se 100K RPS tak smoothly jana = scalability
- 1 node fail hone par bhi service running = reliability

**Short Interview Answer:**

Scalability answers “can it grow?”, reliability answers “can it survive failures?”. Good systems need both.

---

## Q2: Load balancer kya karta hai? L4 vs L7 difference?

**Answer:**

Load balancer incoming traffic ko multiple backend servers mein distribute karta hai.

| Type | Layer | Routing basis | Use case |
|------|------|---------------|----------|
| **L4** | Transport | IP/Port/TCP | Fast, low overhead |
| **L7** | Application | URL, headers, cookies | Smart routing |

### Algorithms:
- Round Robin
- Weighted Round Robin
- Least Connections
- IP Hash
- Consistent Hashing

**Important:**

L7 powerful hai (path-based routing, auth integration), but L4 generally lower latency.

---

## Q3: Sticky sessions vs stateless services?

**Answer:**

### Sticky sessions:
- User ko same backend server par bhejna
- Useful for legacy session-in-memory apps
- Problem: uneven load + node failure pain

### Stateless approach (recommended):
- Session/token ko Redis/JWT mein store
- Koi bhi backend request serve kar sake
- Better autoscaling and failover

**Short Interview Answer:**

Prefer stateless services. Sticky sessions only when unavoidable, because they reduce balancing efficiency and hurt resilience.

---

## Q4: CDN kya hai aur kab use karte hain?

**Answer:**

CDN (Content Delivery Network) static/dynamic content ko user ke nearest edge location se serve karta hai.

### Benefits:
- Lower latency
- Origin server load kam
- Better global performance
- DDoS absorption

### Typical CDN Content:
- Images, CSS, JS
- Video chunks
- Public API responses (cacheable)

**Important:**

Cache headers sahi hone chahiye:

```http
Cache-Control: public, max-age=300, s-maxage=600, stale-while-revalidate=30
```

---

## Q5: CDN cache miss aur invalidation kaise handle karte hain?

**Answer:**

### Cache Miss flow:
User → Edge miss → Origin fetch → Edge store → User response

### Invalidation strategies:
1. Versioned asset URLs (`app.v42.js`)
2. Purge API call (path-based invalidation)
3. Short TTL for frequently changing data

### Best practice:
- Static assets: long TTL + versioning
- Dynamic pages: short TTL + revalidation

**Short Interview Answer:**

For CDN consistency, I prefer versioned assets over frequent global purge. Purge is costly and can cause traffic spikes to origin.

---

## Q6: Message queue kab use karte hain instead of synchronous calls?

**Answer:**

Async queue use karo jab immediate response mandatory na ho.

### Good use cases:
- Email/SMS notifications
- Video/image processing
- Payment reconciliation
- Audit logs
- Retry workflows

### Benefits:
- Producer-consumer decoupling
- Traffic spike buffering
- Retry + DLQ support

**Important:**

"Exactly once" practically rare hota hai; mostly at-least-once + idempotent consumers use karo.

---

## Q7: RabbitMQ vs Kafka - infrastructure perspective se kya choose karein?

**Answer:**

| Need | Better fit |
|------|------------|
| Task queue / RPC | RabbitMQ |
| Complex routing (topic/header exchange) | RabbitMQ |
| High-throughput event streaming | Kafka |
| Replay history | Kafka |
| Stream processing pipelines | Kafka |

**Short Interview Answer:**

RabbitMQ for command/task workflows; Kafka for event streaming and replay. Many production systems use both.

---

## Q8: API Gateway pattern kya hai aur kyun zaroori hai?

**Answer:**

API Gateway client ke liye single entry point hota hai.

### Responsibilities:
- Routing
- Authentication/Authorization
- Rate limiting
- TLS termination
- Request/response transformation
- Observability (logs/metrics/traces)

```text
Client → Gateway → User Service
                 → Order Service
                 → Payment Service
```

**Important:**

Gateway ko business logic dump zone mat banao. Ye cross-cutting concern layer hai.

---

## Q9: Rate limiting infrastructure level par kaise design karte ho?

**Answer:**

### Common algorithms:
- Token Bucket (burst-friendly)
- Leaky Bucket (smooth output)
- Sliding Window Counter (balanced accuracy)

### Deployment locations:
1. Edge/CDN
2. API Gateway
3. Service-level

### Response pattern:

```http
429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
```

**Short Interview Answer:**

I usually enforce coarse limits at gateway and fine-grained limits at service level, backed by Redis for distributed counters.

---

## Q10: Scalability interview mein strong architecture answer kaise dena chahiye?

**Answer:**

Structure:
1. Traffic profile (current + peak)
2. Entry tier (CDN + LB + Gateway)
3. App tier (stateless services + autoscaling)
4. Async tier (queues)
5. Data tier (cache + DB replicas/shards)
6. Reliability layer (health checks, circuit breaker, rate limit)

### One-line example:

```text
I’d place CDN at edge, L7 gateway for policy and routing, stateless service fleet behind autoscaled load balancers, Kafka/RabbitMQ for async workloads, Redis for hot reads, and DB replicas/shards based on read-write profile.
```

**Important:**

Har design decision ke saath trade-off bolo: latency vs consistency, cost vs reliability, simplicity vs flexibility.

