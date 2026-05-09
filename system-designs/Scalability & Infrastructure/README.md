# Scalability & Infrastructure

Scalability layer ki **deep mastery** for system design — load balancing, CDN, message queues, API gateway. Hinglish + diagrams + tool comparisons + interview-grade trade-offs.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Load Balancing | [01-Load-Balancing.md](./01-Load-Balancing.md) | Complete |
| 2 | CDN & Edge Caching | [02-CDN-and-Edge-Caching.md](./02-CDN-and-Edge-Caching.md) | Complete |
| 3 | Message Queues & Async | [03-Message-Queues-and-Async.md](./03-Message-Queues-and-Async.md) | Complete |
| 4 | API Gateway Pattern | [04-API-Gateway-Pattern.md](./04-API-Gateway-Pattern.md) | Complete |

---

## What's Inside Each File?

### [01 — Load Balancing](./01-Load-Balancing.md)
**L4 (transport)** vs **L7 (application)** load balancers. Algorithms: Round Robin, Weighted RR, Least Connections, IP Hash, **Consistent Hashing**. Health checks, sticky sessions, SSL termination, GSLB (geo). Tools: **Nginx**, **HAProxy**, **AWS ALB/NLB**, **Envoy**.

### [02 — CDN & Edge Caching](./02-CDN-and-Edge-Caching.md)
**Push** vs **Pull** CDN, **Cache-Control headers** (`max-age`, `s-maxage`, `no-cache`, `no-store`, `stale-while-revalidate`), cache key tuning, signed URLs, edge compute (Lambda@Edge / Cloudflare Workers), purging / invalidation. Providers: **CloudFront**, **Fastly**, **Cloudflare**, **Akamai**.

### [03 — Message Queues & Async](./03-Message-Queues-and-Async.md)
**Point-to-point** (Queue) vs **Pub/Sub** (Topic). Async processing, traffic absorption, retry/backoff, **DLQ**, **idempotency**, ordering, exactly-once myth. Tools: **RabbitMQ** (AMQP routing), **Kafka** (log + replay + streams), **SQS/SNS/EventBridge**.

### [04 — API Gateway Pattern](./04-API-Gateway-Pattern.md)
Single entry point — routing, auth/authz, rate limiting, **TLS termination**, request/response transform, logging, **circuit breaker**, caching. **BFF (Backend-for-Frontend)** pattern. Tools: **Kong**, **AWS API Gateway**, **Spring Cloud Gateway**, **Envoy + Istio**, **Nginx Plus**.

---

## Recommended Order

```
1. Load Balancing       ← traffic distribution foundation
2. CDN & Edge Caching   ← latency + cost wins at the edge
3. Message Queues       ← decouple, absorb spikes, retry
4. API Gateway          ← single front door for clients
```

> Together these 4 = the **traffic + integration tier** sandwich between users and your services.

---

## Companion Folders

- [System Design Fundamentals](../System%20Design%20Fundamentals/) — RESHADED, CAP, PACELC
- [Data Storage & Database Design](../Data%20Storage%20%26%20Database%20Design/) — sharding, replication, caching
- [System Design Interviews](../../phase-5-interview-preparation/System%20Design%20Interviews/) — applied designs
- [AWS — Cloud Platform for Backend Engineers](../../mysql_aws_mongodb/AWS%20%E2%80%94%20Cloud%20Platform%20for%20Backend%20Engineers/) — ALB/NLB/CloudFront/SQS/SNS/API Gateway concrete services

---

## Quick Reference

| Need | Read |
|------|------|
| Distribute traffic | 01 — Load Balancing |
| Reduce latency / origin load | 02 — CDN |
| Decouple services / absorb spikes | 03 — Queues |
| Single client-facing entry point | 04 — API Gateway |

---

## Status Tracker

```
[x] 01 — Load Balancing
[x] 02 — CDN & Edge Caching
[x] 03 — Message Queues & Async
[x] 04 — API Gateway Pattern
```

> "**Scalability = remove single bottlenecks.** LB removes one server, CDN removes one origin, queue removes one synchronous chain, gateway removes one bespoke client wiring."
