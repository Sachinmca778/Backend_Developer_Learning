# Reliability & Fault Tolerance

Reliability layer ki **deep mastery** for system design — HA design, circuit breaker, rate limiting, distributed consensus. Hinglish mix + diagrams + interview-grade trade-offs.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | High Availability Design | [01-High-Availability-Design.md](./01-High-Availability-Design.md) | Complete |
| 2 | Circuit Breaker Pattern | [02-Circuit-Breaker-Pattern.md](./02-Circuit-Breaker-Pattern.md) | Complete |
| 3 | Rate Limiting | [03-Rate-Limiting.md](./03-Rate-Limiting.md) | Complete |
| 4 | Distributed Consensus | [04-Distributed-Consensus.md](./04-Distributed-Consensus.md) | Complete |

---

## What's Inside Each File?

### [01 — High Availability Design](./01-High-Availability-Design.md)
**Availability math** (9's table — 99.9% / 99.99% / 99.999%), SLI/SLO/SLA, **eliminate SPoF** at every layer (LB, app, DB, network, region), **Active-Active vs Active-Passive**, MTBF/MTTR, **chaos engineering** (Chaos Monkey, GameDays), runbooks, blast radius reduction.

### [02 — Circuit Breaker Pattern](./02-Circuit-Breaker-Pattern.md)
Prevent cascading failures. **CLOSED → OPEN → HALF-OPEN** state machine, failure threshold + cool-down, **fallback strategies** (cached / default / graceful degradation), **bulkhead**, **timeout**, **retry with jitter**. Tools: **Resilience4j** (recommended), Hystrix (deprecated), Polly, Istio/Envoy built-in.

### [03 — Rate Limiting](./03-Rate-Limiting.md)
Algorithms compared: **Token Bucket** (bursts OK), **Leaky Bucket** (strict shaping), **Fixed Window** (simple, boundary spikes), **Sliding Window Log** (accurate, memory-heavy), **Sliding Window Counter** (best balance). **Redis + Lua** atomic implementation, **distributed rate limiting**, response headers (`429 + Retry-After + X-RateLimit-*`), per-user/IP/API-key/endpoint scoping.

### [04 — Distributed Consensus](./04-Distributed-Consensus.md)
**Raft** (leader election, log replication, **n/2+1 quorum**) vs **Paxos** (theoretical, harder), **ZAB** (ZooKeeper), **Multi-Paxos**. Use cases: leader election, distributed locks, config management, cluster membership. Real systems: **etcd, Consul, CockroachDB, Zookeeper, Kafka KRaft, Vitess**.

---

## Recommended Order

```
1. HA Design             ← foundation: redundancy + 9's
2. Circuit Breaker       ← stop cascading failures
3. Rate Limiting         ← protect from overload + abuse
4. Distributed Consensus ← coordinate state across nodes
```

> "Reliability = **prevent**, **detect**, **isolate**, **recover**. Yeh 4 patterns har layer mein lagte hain."

---

## Companion Folders

- [System Design Fundamentals](../System%20Design%20Fundamentals/) — RESHADED, CAP, PACELC
- [Data Storage & Database Design](../Data%20Storage%20%26%20Database%20Design/) — sharding, replication
- [Scalability & Infrastructure](../Scalability%20%26%20Infrastructure/) — LB, CDN, queues, gateway
- [System Design Interviews](../../phase-5-interview-preparation/System%20Design%20Interviews/) — applied designs
- [AWS — Cloud Platform for Backend Engineers](../../mysql_aws_mongodb/AWS%20%E2%80%94%20Cloud%20Platform%20for%20Backend%20Engineers/) — Multi-AZ, ALB, Route 53 failover

---

## Quick Reference

| Need | Read |
|------|------|
| Hit 99.99% uptime | 01 — HA Design |
| Stop one slow service from killing all | 02 — Circuit Breaker |
| Protect API from abuse / runaway clients | 03 — Rate Limiting |
| Elect leader / lock / config across nodes | 04 — Consensus |

---

## Status Tracker

```
[x] 01 — High Availability Design
[x] 02 — Circuit Breaker Pattern
[x] 03 — Rate Limiting
[x] 04 — Distributed Consensus
```

> "**Failures inevitable hain.** Goal = **bounce back fast** + **blast radius small** + **users ko pata bhi na chale**. Reliability = engineering, not luck."
