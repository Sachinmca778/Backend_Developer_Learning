# High Availability & Disaster Recovery

## Status: Not Started

---

## Table of Contents

1. [Availability Nines](#availability-nines)
2. [HA vs DR](#ha-vs-dr)
3. [Multi-AZ vs Multi-Region](#multi-az-vs-multi-region)
4. [Active-Active vs Active-Passive](#active-active-vs-active-passive)
5. [RTO vs RPO](#rto-vs-rpo)
6. [DR Strategies](#dr-strategies)
7. [Circuit Breaker](#circuit-breaker)
8. [Bulkhead Pattern](#bulkhead-pattern)
9. [Timeout, Retry, Backoff, Jitter](#timeout-retry-backoff-jitter)
10. [Graceful Degradation](#graceful-degradation)
11. [Chaos Engineering](#chaos-engineering)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Availability Nines

| Nines | Uptime % | Downtime/year | Downtime/month |
|-------|----------|---------------|----------------|
| 1 | 90% | 36.5 days | 73 hours |
| 2 | 99% | 3.65 days | 7.2 hours |
| 3 | 99.9% | 8.76 hours | 43.8 min |
| 3.5 | 99.95% | 4.38 hours | 21.9 min |
| **4** | **99.99%** | **52.6 min** | **4.4 min** |
| 5 | 99.999% | 5.26 min | 26 sec |
| 6 | 99.9999% | 31.5 sec | 2.6 sec |

### Real targets

| System | Typical SLA |
|--------|-------------|
| Internal tools | 99% (3 nines OK) |
| Standard web app | 99.9% |
| Financial / mission-critical | 99.99% |
| Telecom / DNS / payment infra | 99.999% |

→ Cost grows **exponentially** with each 9.

### How to achieve 99.99%

- Multi-AZ deployment (no single AZ failure brings system down)
- Auto-failover for DB
- Multiple LB instances
- Stateless app servers + horizontal scale
- Health checks + automated remediation
- Continuous deployment with canary
- Strong observability

---

## HA vs DR

| | HA (High Availability) | DR (Disaster Recovery) |
|--|----------------------|------------------------|
| Scope | Within region | Cross-region |
| Failure type | Node / AZ failure | Region-wide / catastrophic |
| Recovery | Automatic, seconds | Often manual, hours |
| Cost | Lower | Higher (duplicate infra) |
| Trigger | Routine, automated | Rare events, planned |

→ Both needed for serious production systems.

---

## Multi-AZ vs Multi-Region

### Availability Zone (AZ)

A datacenter (or set) within a region, isolated from other AZs (separate power, network).

### Region

Geographic area (e.g., us-east-1, ap-south-1) — multiple AZs.

### Multi-AZ deployment

```
Region: us-east-1
├── AZ-A: 2 servers + DB primary
├── AZ-B: 2 servers + DB replica
└── AZ-C: 2 servers + DB replica
LB distributes traffic across AZs
```

→ AZ failure → traffic shifts; no user impact.

### Multi-Region

```
Region us-east-1: full stack
Region ap-south-1: full stack (active or passive)
Global LB / DNS routes by latency / failure
```

→ Region failure (rare) → other region serves.

### Latency consideration

- Within region: 1-5ms
- Cross-region: 100-200ms

→ Multi-region replication async (eventual consistency for write side).

### When multi-region?

- Compliance (data residency)
- Latency for global users
- DR against regional outage
- 99.99%+ SLA

---

## Active-Active vs Active-Passive

### Active-Passive (warm standby)

```
Active region serves all traffic
Passive region replicates data, idle
On failover: DNS/LB switches to passive
```

| Pros | Cons |
|------|------|
| Cheaper | Failover takes time (5-30 min) |
| Simpler | Passive resources idle |

### Active-Active (both serve traffic)

```
Both regions serve traffic concurrently
Data replicated bi-directionally
Routing: latency-based or geo-based
```

| Pros | Cons |
|------|------|
| Zero downtime failover | Conflict resolution needed for writes |
| Resources fully utilized | Complex (CRDTs, last-write-wins) |
| Lower latency (local) | Costlier |

### Conflict resolution (active-active)

- **Last-Write-Wins** (LWW) — timestamp-based
- **Vector clocks** — track causality
- **CRDTs** (Conflict-free Replicated Data Types) — math guarantees convergence
- **App-level reconciliation** (e.g., shopping cart merge)

---

## RTO vs RPO

### RTO (Recovery Time Objective)

> "How long can we be down?"

| Scenario | RTO |
|----------|-----|
| Critical financial | < 1 min |
| Standard web | < 1 hour |
| Internal tool | < 24 hours |

### RPO (Recovery Point Objective)

> "How much data can we lose?"

| Scenario | RPO |
|----------|-----|
| Banking | 0 (sync replication) |
| E-commerce | < 5 min (continuous replication) |
| Analytics | < 24 hours (daily backup) |

### Impact on DR strategy

| RTO / RPO | Strategy |
|-----------|----------|
| Both high (24h+) | Backup-restore (cheap) |
| RPO low, RTO high | Continuous backup + manual restore |
| RTO low, RPO low | Active-active, full duplicate |

---

## DR Strategies

(AWS canonical 4 strategies, but applies anywhere.)

### 1. Backup & Restore

```
Daily snapshots → S3
On disaster → spin up new region from snapshots
RTO: hours-days, RPO: 24h (or backup interval)
Cost: lowest
```

### 2. Pilot Light

```
Critical core (DB) replicated continuously to DR region
App servers OFF (cold)
On disaster → bring up app, point at replicated DB
RTO: 30-60 min, RPO: minutes
Cost: low
```

### 3. Warm Standby

```
Scaled-down version of full stack running in DR region
On disaster → scale up + redirect traffic
RTO: 5-15 min, RPO: seconds-minutes
Cost: medium
```

### 4. Multi-site Active-Active

```
Full prod stack in both regions, both serving
RTO: ~0, RPO: ~0
Cost: highest
```

### Comparison

| Strategy | RTO | RPO | Cost |
|----------|-----|-----|------|
| Backup-Restore | hours-days | hours-days | $ |
| Pilot Light | 10s of min | min | $$ |
| Warm Standby | 5-15 min | sec-min | $$$ |
| Multi-site A-A | ~0 | ~0 | $$$$ |

---

## Circuit Breaker

> "Stop calling a failing dependency for a while; let it recover."

### State machine

```
[CLOSED] ──── failure threshold ────→ [OPEN]
   ↑                                     │
   │                                     │ wait period
   │                                     ↓
[HALF_OPEN] ←──── trial succeeds ────────┘
```

| State | Behaviour |
|-------|-----------|
| CLOSED | Normal — calls allowed; track failures |
| OPEN | All calls fail-fast (return cached/default); no real call |
| HALF_OPEN | Probe — limited calls allowed; promote or reopen |

### Why?

- Prevent cascading failures (slow dependency → exhausted threads)
- Give downstream time to recover
- Fast user response (better than timeout)

### Spring + Resilience4j

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackPay")
public PaymentResult pay(...) { ... }

public PaymentResult fallbackPay(Throwable t) {
    return PaymentResult.queuedForLater();
}
```

```yaml
resilience4j.circuitbreaker:
  instances:
    paymentService:
      failureRateThreshold: 50          # 50% failures → open
      waitDurationInOpenState: 10s
      slidingWindowSize: 20
```

→ Cross-ref: `phase-4 / Microservices Architecture / Resilience4j` notes.

---

## Bulkhead Pattern

> "Isolate resources so failure in one part doesn't sink the ship."

(Named after ship compartments — flooded section sealed off.)

### Examples

#### Thread pool isolation

```
Service A → ThreadPool-A (10 threads)
Service B → ThreadPool-B (10 threads)
```

→ Slow Service A doesn't starve Service B threads.

```java
@Bulkhead(name = "paymentService", type = Type.THREADPOOL)
public PaymentResult pay(...) { ... }
```

#### Connection pool isolation

Separate DB connection pools per critical workload (read vs write, online vs batch).

#### Network isolation

Separate VPC subnets / namespaces.

### Trade-off

More isolation → less efficient resource use; balance.

---

## Timeout, Retry, Backoff, Jitter

### Timeout

> "Always set timeouts on network calls."

Default `Long.MAX_VALUE` waits forever → hung threads.

```java
HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
```

Reasonable HTTP timeout: 1-5 sec for inter-service.

### Retry

```
on failure: retry N times
```

⚠️ Retry storm — thousands of clients retrying simultaneously can DDoS service.

### Exponential Backoff

```
delay = base × (2^attempt)
attempt 1: 1s, 2: 2s, 3: 4s, 4: 8s
```

### Jitter (essential!)

```
delay = random(0, base × 2^attempt)
```

→ Spreads retries; prevents thundering herd.

### Resilience4j

```yaml
resilience4j.retry:
  instances:
    paymentService:
      maxAttempts: 3
      waitDuration: 1s
      exponentialBackoffMultiplier: 2
      randomizedWaitFactor: 0.5    # jitter
```

### Idempotency

Retry safely only on **idempotent** operations (GET, PUT, DELETE — not POST without idempotency key).

For non-idempotent: include `Idempotency-Key` header; server dedupes.

---

## Graceful Degradation

> "When dependencies fail, serve **degraded but useful** experience."

### Examples

| Component fails | Degraded mode |
|-----------------|---------------|
| Recommendation service | Show generic / popular items |
| Comment service | Hide comments, show post |
| Payment provider | Save order in pending; process later |
| Search relevance ML | Fallback to TF-IDF |
| Personalization | Show generic |

### Patterns

- **Fallback values** (cached, default)
- **Feature flags** to disable failing features quickly
- **Cached stale data** (better than nothing)
- **Read-only mode** (e.g., maintenance: allow browse, block writes)

### Communication

Tell users (banner: "search slow"); don't show 500.

---

## Chaos Engineering

> "Inject failures into production-like systems to find weaknesses **proactively**."

### Origin

Netflix Chaos Monkey — randomly killed prod instances.

### Tools

- **Chaos Monkey** — kill random VMs
- **Chaos Kong** — fail entire regions
- **Gremlin** — comprehensive injection
- **Litmus** — Kubernetes-native
- **AWS Fault Injection Simulator (FIS)**

### Experiments

| What to break | What to verify |
|---------------|----------------|
| Kill instance | LB removes; auto-scaler replaces |
| Network latency injection | Timeouts + circuit breaker open |
| Disk fill | Alerts + graceful failure |
| Memory hog | OOM handling |
| AZ failure | Multi-AZ takes over |

### Process

```
1. Hypothesis: "If we kill 1/3 instances, P99 stays < 500ms"
2. Run experiment in controlled blast radius
3. Observe + measure
4. Improve weak point
5. Expand blast radius
```

### Game Days

Scheduled team exercises simulating failures — practice runbooks, train operators.

---

## Common Output Traps

### Q1. "99.99% uptime" without specifying scope

→ Per-region? End-to-end? Excluding planned maintenance?

### Q2. "Multi-AZ" claimed but DB single-AZ

→ DB failover is the slow part; verify.

### Q3. Retry without backoff/jitter

→ Retry storm.

### Q4. Active-Active without conflict strategy

→ Data divergence.

### Q5. RPO 0 promised with async replication

→ Impossible; sync replication required for RPO 0.

---

## Pitfalls

1. **No timeout** — hung threads, cascading failure.
2. **Retry without idempotency** — duplicate side effects.
3. **No circuit breaker** — cascading.
4. **Single LB** — SPOF.
5. **Single AZ DB** — AZ failure = downtime.
6. **DNS TTL too high** — slow DR cutover.
7. **No tested DR** — discover problems during disaster.
8. **No observability** — can't detect or diagnose.
9. **Active-Active with sync replication** across regions — high latency.
10. **No back-pressure** — autoscale can't catch up.
11. **Overconfident in nines** — measure real, not promised.
12. **Region-locked S3 bucket** for backups — replicate cross-region.

---

## Cheat Sheet

| Nines | Downtime |
|-------|----------|
| 99% | 3.65 d/yr |
| 99.9% | 8.76 h/yr |
| 99.99% | 52 min/yr |
| 99.999% | 5 min/yr |

| Strategy | RTO | RPO |
|----------|-----|-----|
| Backup-Restore | hours-days | hours-days |
| Pilot Light | 10s of min | min |
| Warm Standby | 5-15 min | sec-min |
| Active-Active | 0 | 0 |

| Pattern | Use |
|---------|-----|
| Circuit Breaker | Failing dependency |
| Bulkhead | Resource isolation |
| Timeout + Retry + Jitter | Transient failures |
| Graceful Degradation | Partial outages |
| Chaos Engineering | Proactive validation |

| Term | Meaning |
|------|---------|
| RTO | Down time ok |
| RPO | Data loss ok |
| HA | Within region |
| DR | Cross-region |

---

## Practice

1. Calculate budget for 99.99% — how many minutes/month? plan accordingly.
2. Design DR strategy for each: blog (cheap), e-commerce (warm), bank (active-active).
3. Implement circuit breaker with Resilience4j; observe state transitions.
4. Add jitter to retry — write code; verify uniform distribution.
5. Design graceful degradation for: search outage, payment outage, recommendation outage.
6. Plan a Chaos Monkey experiment in dev/staging; identify a hypothesis.
