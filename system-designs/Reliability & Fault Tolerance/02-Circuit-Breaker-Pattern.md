# Circuit Breaker Pattern

## Status: Complete

---

## Table of Contents

1. [The Problem — Cascading Failure](#the-problem--cascading-failure)
2. [Circuit Breaker Idea](#circuit-breaker-idea)
3. [State Machine](#state-machine)
4. [CLOSED State](#closed-state)
5. [OPEN State](#open-state)
6. [HALF-OPEN State](#half-open-state)
7. [Configuration Knobs](#configuration-knobs)
8. [Fallback Strategies](#fallback-strategies)
9. [Companion Patterns](#companion-patterns)
10. [Resilience4j Example](#resilience4j-example)
11. [Tools Compared](#tools-compared)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## The Problem — Cascading Failure

> "**One slow / failing service** → callers' threads blocked waiting → caller saturated → its callers also wait → entire system collapses domino-style."

```
Service A → calls B (now slow: 30s timeout)
   │
   └── 1000 RPS × 30s wait = 30,000 threads tied up
        → A runs out of threads → A also slow → A's callers fail
        → cascade up the chain
```

> Without protection, **one weak link kills the entire chain**. Circuit breaker stops the cascade.

---

## Circuit Breaker Idea

> "**Electrical fuse ki tarah** — agar downstream service consistently failing hai, breaker '**trip**' ho jata hai → caller ko **immediately fail / fallback** mil jata hai instead of waiting → resources free, downstream gets time to recover."

```
Without breaker:
  Caller → wait 30s → timeout → retry → wait 30s → ... (caller dies)

With breaker:
  Caller → breaker OPEN → return fallback in 1ms
        → no load on broken service → it recovers faster
```

---

## State Machine

```
        ┌─────────── successes ─────────────┐
        │                                    │
        ▼                                    │
   ┌────────┐  failures > threshold   ┌─────────┐
   │ CLOSED │ ───────────────────────► │  OPEN   │
   └────────┘                          └─────────┘
        ▲                                    │
        │                          cool-down timer
        │                                    │
        │                                    ▼
        │                            ┌─────────────┐
        └── all probe successes ──── │ HALF-OPEN   │
              probe failures          └─────────────┘
                  │
                  ▼
                back to OPEN
```

### Three states

| State | Behavior |
|-------|----------|
| **CLOSED** | Normal — calls pass through; track failures |
| **OPEN** | Trip — calls **short-circuit** (fail fast / fallback); don't touch downstream |
| **HALF-OPEN** | After cool-down — let **few probe calls** through; success → CLOSED, failure → OPEN |

---

## CLOSED State

> "**Normal operation.** Calls pass through; breaker monitors **failure rate** (or count) over a window."

### Failure tracking

- **Sliding count window** — last N calls (e.g., 100); fail if X% fail
- **Sliding time window** — last T seconds; fail if X% fail
- **Min calls threshold** — don't trip on 1 failure out of 1 call (need sample size)

### Trip conditions

```
Trip when (in window):
  - failure_rate ≥ 50%   (configurable)
  - min_calls   ≥ 20     (don't trip on small samples)
  - slow_call_rate ≥ 50% (e.g., calls > 2s count as slow)
```

→ On trip → state → **OPEN**.

---

## OPEN State

> "**Short-circuit.** Calls return **immediately** with fallback or exception — no actual call to downstream."

### Why this helps downstream

- Sudden **drop in traffic** to broken service → it can recover (GC, restart, scale)
- **No new connections** piling up
- **Quick failure** to caller — caller can retry with backoff or use fallback

### Why this helps caller

- **Threads / connections free** instantly — caller stays responsive
- **No cascading saturation**
- Better user experience (fast fail > slow death)

### Cool-down (`waitDurationInOpenState`)

- Stay OPEN for fixed time (e.g., 30 sec)
- Then transition to **HALF-OPEN** to test recovery

---

## HALF-OPEN State

> "**Probe state** — let a few calls through to see if downstream recovered. Don't unleash full traffic immediately (could re-overwhelm)."

### Behavior

- Allow **N probe calls** (e.g., 10)
- Track success/failure
- All probes succeed → **CLOSED** (back to normal)
- Any probe fails (or threshold breached) → **OPEN** again (extend cool-down)

### Why HALF-OPEN matters

Without HALF-OPEN, breaker would either:

- Stay OPEN forever (need manual reset), or
- Snap back to CLOSED + flood downstream → re-trip

HALF-OPEN gives **gradual ramp-up** — protects both sides.

---

## Configuration Knobs

| Setting | Meaning | Typical |
|---------|---------|---------|
| **failureRateThreshold** | % failures to trip | 50% |
| **slowCallRateThreshold** | % slow calls to trip | 50% |
| **slowCallDurationThreshold** | What counts as "slow" | 2s |
| **slidingWindowType** | COUNT_BASED or TIME_BASED | COUNT_BASED |
| **slidingWindowSize** | Window size (calls or seconds) | 100 calls |
| **minimumNumberOfCalls** | Sample size before trip eligible | 20 |
| **waitDurationInOpenState** | Cool-down before HALF-OPEN | 30 s |
| **permittedNumberOfCallsInHalfOpenState** | Probe count | 10 |
| **automaticTransitionFromOpenToHalfOpenEnabled** | Auto vs on-demand | true |

### Tuning tips

- Don't set threshold too low (one bad burst trips you unnecessarily)
- Don't set sliding window too small (one slow call ≠ outage)
- Cool-down long enough for downstream to actually recover (often 30–60s)
- Different breakers per dependency (DB breaker vs payment-API breaker)

---

## Fallback Strategies

> "**OPEN state mein kya return karein?** Always have a fallback — never just throw."

### Common fallbacks

| Strategy | Example |
|----------|---------|
| **Cached previous response** | Last good catalog / config |
| **Default value** | Empty list, neutral value |
| **Stale data with warning** | "Showing yesterday's price (live unavailable)" |
| **Reduced functionality** | Skip recommendations, show core content |
| **Queue for later** | Drop into SQS, process when downstream healthy |
| **Different service** | Failover to secondary provider |
| **Sensible error** | "Try again in a minute" with `Retry-After` |

### Graceful degradation example (e-commerce)

```
Recommendations API down → show "trending products" cached list
Reviews API down         → show product without reviews
Inventory API down       → say "verifying stock", queue for confirmation
Payment API down         → "we'll process when ready" (NEVER for primary checkout)
```

→ Critical path maintains; non-critical degrades gracefully.

---

## Companion Patterns

### Timeout

> "**Always set a timeout.** Without it, you wait forever — circuit breaker can't help."

```
HTTP client timeout: 3 seconds (typical)
DB query timeout:    5 seconds
RPC timeout:         match upstream SLA
```

### Retry with backoff + jitter

```
attempt 1: immediate
attempt 2: wait 100 ms + jitter(±50%)
attempt 3: wait 200 ms + jitter
attempt 4: wait 400 ms + jitter (max 3 attempts usually)
```

- **Idempotent only** — never retry POST that creates non-idempotent side effect (or use idempotency key)
- **Combined with circuit breaker** — retry within breaker; if breaker OPEN, no retry needed (fast fail)

### Bulkhead

> "**Isolated resource pools per dependency.** One slow downstream doesn't drain all threads / conns."

```
Thread pool A (10): for dependency A
Thread pool B (10): for dependency B
Thread pool C (10): for dependency C
```

- Dependency A spikes/slows → only its pool exhausted; B and C unaffected
- Hystrix popularized this; Resilience4j supports both **thread pool** and **semaphore** bulkheads

### Rate limiter (companion)

- Limit how often you call downstream regardless of breaker state
- Prevents bursts from overwhelming a recovering service

---

## Resilience4j Example

```java
// Java — Resilience4j
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)
    .slowCallRateThreshold(50)
    .slowCallDurationThreshold(Duration.ofSeconds(2))
    .waitDurationInOpenState(Duration.ofSeconds(30))
    .slidingWindowSize(100)
    .minimumNumberOfCalls(20)
    .permittedNumberOfCallsInHalfOpenState(10)
    .build();

CircuitBreaker breaker = CircuitBreaker.of("paymentService", config);

Supplier<PaymentResponse> decorated = CircuitBreaker
    .decorateSupplier(breaker, () -> paymentClient.charge(req));

PaymentResponse resp = Try.ofSupplier(decorated)
    .recover(throwable -> fallbackPayment(throwable))
    .get();
```

### Spring Boot integration

```yaml
resilience4j.circuitbreaker:
  instances:
    paymentService:
      failureRateThreshold: 50
      waitDurationInOpenState: 30s
      slidingWindowSize: 100
      minimumNumberOfCalls: 20
```

```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackCharge")
public PaymentResponse charge(Request req) { ... }

public PaymentResponse fallbackCharge(Request req, Throwable t) {
    return PaymentResponse.deferred();
}
```

### Metrics (auto-published)

- `resilience4j_circuitbreaker_state{state=open|closed|half_open}`
- `resilience4j_circuitbreaker_calls{kind=successful|failed|slow}`
- Alarm on **state=OPEN > N seconds**

---

## Tools Compared

| Tool | Language / runtime | Notes |
|------|---------------------|-------|
| **Resilience4j** | Java (Spring) | **Modern recommendation**; lightweight, functional API |
| **Hystrix** | Java | **Deprecated** (Netflix), historical importance |
| **Sentinel** (Alibaba) | Java | Powerful, dashboards |
| **Polly** | .NET | Standard for .NET retry/breaker |
| **opossum** | Node.js | Popular npm package |
| **PyBreaker** | Python | Lightweight |
| **gobreaker** | Go | Minimal, idiomatic |
| **Istio / Envoy** | Sidecar (any language) | Config-driven; outlier detection |
| **Linkerd** | Sidecar | Service mesh-level breakers |
| **AWS App Mesh** | Sidecar (deprecated) | Replaced by Service Connect / VPC Lattice |

---

## Pitfalls

1. **No timeout** → breaker doesn't help (you wait forever).
2. **Too low failure threshold** → trips on benign hiccups.
3. **No fallback** → just rethrow, defeats purpose.
4. **Single shared breaker for many dependencies** → one bad service trips all.
5. **No metrics / alarms** on breaker state → silent OPEN flapping.
6. **Retrying inside breaker without idempotency** → duplicate writes.
7. **Breaker around non-network calls** (CPU, in-process) → unnecessary.
8. **Trip threshold too high** → outage before breaker reacts.
9. **Cool-down too short** → breaker re-trips immediately, downstream never recovers.
10. **Synchronous fallback that itself calls another service** → just moves cascade.
11. **Hystrix in greenfield** → use **Resilience4j** instead (Hystrix EOL).
12. **No HALF-OPEN probe limit** → flood recovering service → re-trip.

---

## Cheat Sheet

| State | What |
|-------|------|
| **CLOSED** | Normal; track failures |
| **OPEN** | Short-circuit; fallback fast |
| **HALF-OPEN** | Probe with limited calls |

| Setting | Default-ish |
|---------|-------------|
| Failure rate | 50% |
| Min calls | 20 |
| Cool-down | 30 s |
| Probe calls | 10 |

| Companion | Why |
|-----------|-----|
| **Timeout** | Mandatory — bound the wait |
| **Retry + jitter** | Recover from transient |
| **Bulkhead** | Isolate resource pools |
| **Rate limiter** | Cap call rate |
| **Fallback** | Always have one |

| Tool | Use |
|------|-----|
| Java | **Resilience4j** |
| .NET | **Polly** |
| Node | opossum |
| Python | PyBreaker |
| Go | gobreaker |
| Polyglot mesh | **Istio/Envoy** |

---

## Practice

1. Add Resilience4j circuit breaker around a flaky 3rd-party API in Spring Boot; configure fallback.
2. Simulate downstream slowness (Toxiproxy 5s latency) → observe state transitions.
3. Implement bulkhead with separate thread pools for "search" and "checkout" dependencies.
4. Compose breaker + retry + timeout in correct order (timeout innermost).
5. Add CloudWatch alarm for `circuitbreaker_state{state=open}` > 60 sec → page.
6. Compare Hystrix vs Resilience4j config for same scenario; explain why Resilience4j wins.
