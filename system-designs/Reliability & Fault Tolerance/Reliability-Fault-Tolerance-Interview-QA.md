# Reliability & Fault Tolerance - Top 10 Interview Questions & Answers

> Covering: High Availability, Circuit Breaker, Rate Limiting, and Distributed Consensus for production-grade systems

---

## Q1: Reliability aur fault tolerance mein kya difference hai?

**Answer:**

| Term | Meaning |
|------|---------|
| **Reliability** | System expected behavior consistently de |
| **Fault Tolerance** | Component fail hone par bhi system continue kare |

### Simple view:
- Reliability = failure kam ho
- Fault tolerance = failure hone par bhi user impact kam ho

**Short Interview Answer:**

Reliability is about consistency of service over time; fault tolerance is about surviving component failures without total outage.

---

## Q2: High availability design karte waqt sabse pehle kya sochna chahiye?

**Answer:**

### 1) Target availability define karo
- 99.9%, 99.99%, etc.

### 2) SPoF (single point of failure) remove karo
- Load balancer redundancy
- Multiple app instances
- DB failover/replicas
- Multi-AZ network paths

### 3) Detect + recover fast
- Health checks
- Auto-healing
- Runbooks

| Availability | Yearly downtime approx |
|-------------|-------------------------|
| 99.9% | ~8.76 hours |
| 99.99% | ~52.6 minutes |
| 99.999% | ~5.26 minutes |

**Important:**

High availability sirf "replicas add kar do" nahi hota. Detection time + recovery time (MTTR) equally important hain.

---

## Q3: Active-Active vs Active-Passive architecture kab choose karte ho?

**Answer:**

| Pattern | Pros | Cons |
|---------|------|------|
| Active-Passive | Simpler failover | Secondary mostly idle |
| Active-Active | Better utilization + faster failover | Conflict/data consistency complexity |

### Choose Active-Passive:
- Simpler ops chahiye
- Strong consistency critical

### Choose Active-Active:
- Global low latency
- High read/write distribution need

**Short Interview Answer:**

I prefer active-passive for simpler systems and strict consistency workflows, and active-active for global scale where availability and latency are prioritized.

---

## Q4: Circuit breaker pattern kya hai aur state machine kaise kaam karti hai?

**Answer:**

Circuit breaker cascading failures rokta hai.

### States:
1. **CLOSED**: normal traffic, failures monitor karo
2. **OPEN**: failure threshold cross, downstream calls block
3. **HALF_OPEN**: limited trial requests allow, recovery check

```text
CLOSED --(too many failures)--> OPEN
OPEN --(cooldown timeout)--> HALF_OPEN
HALF_OPEN --(success)--> CLOSED
HALF_OPEN --(failure)--> OPEN
```

### Benefits:
- Thread exhaustion prevent
- Fast fail behavior
- Downstream service ko recovery time

---

## Q5: Circuit breaker ke saath retry ka use kaise karte hain?

**Answer:**

Retry aur circuit breaker ko blindly combine mat karo.

### Good pattern:
- Small bounded retries (2-3 max)
- Exponential backoff + jitter
- Timeout mandatory
- Retry only transient errors

### Bad pattern:
- Infinite retries
- Same timeoutless call repeat
- Non-idempotent operations pe retry

**Short Interview Answer:**

Use timeout + bounded retry + circuit breaker together. Retries should be limited and jittered, otherwise they amplify outages and trigger retry storms.

---

## Q6: Rate limiting reliability perspective se kyun important hai?

**Answer:**

Rate limiting sirf abuse control nahi, reliability protection bhi hai.

### Protects against:
- Malicious traffic spikes
- Buggy clients (infinite loops)
- Internal fan-out explosions
- Cost blowups

### Common algorithms:
| Algorithm | Best for |
|----------|----------|
| Token Bucket | Bursty but controlled traffic |
| Leaky Bucket | Smooth fixed output |
| Sliding Window Counter | Balanced fairness and cost |

**Important:**

Rate limiting ko edge + gateway + service level par layered way mein apply karo.

---

## Q7: 429 response design ka best practice kya hai?

**Answer:**

Client-friendly rate limit response do:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1712345678
```

### Why important?
- Client backoff implement kar sake
- Ecosystem predictable bane
- API consumer frustration kam ho

**Short Interview Answer:**

A good 429 must include Retry-After and rate-limit metadata so clients can recover gracefully instead of hammering harder.

---

## Q8: Distributed consensus kya hota hai aur kyun chahiye?

**Answer:**

Consensus ka goal: multiple nodes ko ek agreed state/decision par lana despite failures.

### Use cases:
- Leader election
- Distributed lock
- Config management
- Membership coordination

### Core idea:
- Quorum-based decision (usually n/2 + 1)
- Majority agreement ke bina commit nahi

**Important:**

Consensus data plane ke liye nahi, control plane coordination ke liye hota hai.

---

## Q9: Raft vs Paxos interview mein kaise explain karte ho?

**Answer:**

| Aspect | Raft | Paxos |
|--------|------|-------|
| Understandability | Easier | Harder |
| Structure | Leader-centric | More abstract |
| Industry adoption | Very high | Mostly theoretical/advanced impl |

### Raft basics:
- Leader election
- Log replication
- Safety via term/index rules

### Real systems:
- etcd, Consul, Kafka KRaft use Raft-style consensus

**Short Interview Answer:**

Both solve consensus, but Raft is easier to reason about and implement. In interviews, explaining Raft clearly is usually more practical than deep Paxos internals.

---

## Q10: Reliability architecture ka final interview summary kaise dena chahiye?

**Answer:**

Final summary structure:
1. SLO target
2. Redundancy plan
3. Failure isolation patterns
4. Recovery mechanics
5. Observability + operations

### Example closing:

```text
To meet 99.99% availability, we remove SPoFs at LB/app/DB layers, use active-passive failover for data safety, enforce circuit breakers and rate limits to stop cascade failures, and run health-check driven auto-recovery with clear runbooks and SLO alerts.
```

**Important:**

Reliability is not a single feature; it is layered engineering: prevention, detection, isolation, and fast recovery.

