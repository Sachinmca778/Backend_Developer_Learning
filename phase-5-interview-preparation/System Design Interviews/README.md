# System Design Interviews

System Design interview ke liye **end-to-end framework** — requirements → estimation → high-level design → deep dive → trade-offs. Hinglish, with diagrams, comparison tables, and full case studies.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Design Fundamentals | [01-Design-Fundamentals.md](./01-Design-Fundamentals.md) | Not Started |
| 2 | Scalability Patterns | [02-Scalability-Patterns.md](./02-Scalability-Patterns.md) | Not Started |
| 3 | Database Design | [03-Database-Design.md](./03-Database-Design.md) | Not Started |
| 4 | Common System Designs | [04-Common-System-Designs.md](./04-Common-System-Designs.md) | Not Started |
| 5 | Microservices Design | [05-Microservices-Design.md](./05-Microservices-Design.md) | Not Started |
| 6 | High Availability & DR | [06-High-Availability-and-DR.md](./06-High-Availability-and-DR.md) | Not Started |

---

## What's Inside Each File?

### [01 — Design Fundamentals](./01-Design-Fundamentals.md)
Interview **framework** (clarify → estimate → high-level → deep-dive → trade-offs), **functional vs non-functional** requirements, **back-of-envelope estimation** (QPS, storage, bandwidth, memory math with powers of 2/10), **CAP theorem** real meaning ("during partition pick C or A"), **PACELC** extension, **ACID vs BASE**, **eventual consistency** strategies (read-your-writes, monotonic reads, causal).

### [02 — Scalability Patterns](./02-Scalability-Patterns.md)
**Vertical vs horizontal scaling** + cost curves, **load balancing** L4 (TCP/UDP) vs L7 (HTTP), strategies (round-robin, least connections, IP hash, **consistent hashing** with virtual nodes), **stateful vs stateless services**, **database sharding** (horizontal partitioning), **read replicas** + replication lag, **caching layers** (CDN → reverse proxy → app → DB), session affinity, autoscaling, **stateless design rules**.

### [03 — Database Design](./03-Database-Design.md)
**SQL vs NoSQL** decision criteria + when each, **denormalization** trade-offs, **indexing strategy** (B-tree, composite, covering, partial), **DB replication** (primary-replica, async vs sync, read scale), **sharding strategies** (range, hash, directory, geo), **partition key** selection, **PostgreSQL vs MySQL vs MongoDB vs Cassandra vs Redis vs DynamoDB vs Elasticsearch** decision matrix with use-case mapping.

### [04 — Common System Designs](./04-Common-System-Designs.md)
**Full case studies** with requirements, estimation, schema, API:
- **URL shortener** (base62 encoding, counter vs hash, DB schema, cache, redirect 301/302, custom URLs, expiry)
- **Rate limiter** (token bucket, leaky bucket, fixed window, sliding window log, sliding window counter, Redis-based distributed)
- **Notification system** (push/email/SMS, fan-out, retries, DLQ, user preferences)
- **News feed** (pull/push/hybrid model, fan-out on write vs read, ranking, celebrity problem)
- **Distributed cache** (Memcached vs Redis cluster, consistent hashing, replication, failover, hot key)

### [05 — Microservices Design](./05-Microservices-Design.md)
**Service decomposition** strategies (DDD bounded contexts, business capability), **REST vs gRPC vs GraphQL** comparison, **synchronous vs asynchronous** communication, **event-driven architecture** (events vs commands), **saga pattern** (orchestration vs choreography), **CQRS** (read vs write separation), **service mesh** (Istio, Linkerd — mTLS, observability, traffic mgmt), **API gateway**, **observability 3 pillars** (logs / metrics / traces).

### [06 — High Availability & DR](./06-High-Availability-and-DR.md)
**Availability nines** table (99% = 3.65d/yr, **99.99% = 52min/yr**, 99.999% = 5min/yr), **multi-AZ vs multi-region**, **active-active vs active-passive**, **RTO vs RPO** difference, **DR strategies** (backup-restore, pilot light, warm standby, multi-site active), **circuit breaker pattern**, **bulkhead** isolation, **timeout + retry + jitter**, **graceful degradation**, **chaos engineering** (Netflix Chaos Monkey, GameDays).

---

## Recommended Order

```
1. Design Fundamentals      ← framework + estimation (always start here)
2. Scalability Patterns     ← horizontal scale + LB + caching
3. Database Design          ← SQL/NoSQL + sharding + replication
4. Common System Designs    ← practice classics (URL shortener, rate limiter, etc.)
5. Microservices Design     ← service-level architecture
6. High Availability & DR   ← reliability + resiliency
```

→ For experienced backend roles, focus 4 + 5 + 6 in depth.

---

## System Design Interview Framework

```
1. Clarify Requirements       (5-10 min)
   ├── Functional: features, use cases
   ├── Non-functional: scale, latency, availability, consistency
   └── Out-of-scope: explicitly state

2. Back-of-envelope            (5 min)
   ├── DAU → QPS (read + write)
   ├── Storage (per record × volume × retention)
   └── Bandwidth (data × QPS)

3. High-level Design           (10 min)
   ├── Boxes & arrows: client → LB → API → cache → DB
   ├── DB choice + schema
   └── API design

4. Deep Dive                   (15 min)
   ├── Bottleneck: pick 1-2 hot components
   ├── Sharding strategy
   ├── Caching strategy
   ├── Async / queues
   └── Specific algorithms (rate limiter, recommendation, etc.)

5. Trade-offs / Edge Cases     (5-10 min)
   ├── Failure scenarios (DB down, cache miss storm)
   ├── Scaling beyond estimate (10x, 100x)
   ├── Consistency vs availability choice
   └── Monitoring + alerts
```

---

## Estimation Cheat Sheet

| Quantity | Approx |
|----------|--------|
| 1 KB | 10³ bytes |
| 1 MB | 10⁶ bytes |
| 1 GB | 10⁹ bytes |
| 1 TB | 10¹² bytes |
| Cache memory | 10s of GB |
| Server RAM | 10s–100s of GB |
| HDD | TBs |
| 1 day | 86,400 sec ≈ **10⁵** sec |
| 1 month | 30 × 10⁵ ≈ 2.6 × 10⁶ sec |

| Latency (rough) | |
|-----------------|---|
| L1 cache | 1 ns |
| L2 cache | 10 ns |
| RAM | 100 ns |
| SSD random | 0.1 ms |
| Network round-trip same DC | 0.5 ms |
| HDD seek | 10 ms |
| Cross-region RTT | 100-200 ms |

---

## Companion Folders

- [Performance & Optimization](../../phase-4-advanced-topics/Performance%20%26%20Optimization/) — caching, connection pools, JVM
- [Microservices Architecture](../../phase-4-advanced-topics/Microservices%20Architecture/) — practical Spring Cloud
- [Messaging Kafka / RabbitMQ](../../phase-4-advanced-topics/) — async communication
- [Database Mastery](../../backend-skills/Database-Mastery/) — SQL deep
- [Networking & Protocols](../../backend-skills/Networking-and-Protocols/) — HTTP / TCP / DNS / LB
- [Security Best Practices](../../backend-skills/Security-Best-Practices/) — security in design

---

## Status Tracker

```
[ ] 01 — Design Fundamentals
[ ] 02 — Scalability Patterns
[ ] 03 — Database Design
[ ] 04 — Common System Designs
[ ] 05 — Microservices Design
[ ] 06 — High Availability & DR
```

> "System design interview = **structured thinking** + **trade-off articulation** + **realistic numbers**. Memorize framework, practice 6-8 classic designs, understand trade-offs deeply."
