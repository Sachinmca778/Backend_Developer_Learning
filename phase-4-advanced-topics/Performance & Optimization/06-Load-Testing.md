# Load Testing

## Status: Not Started

---

## Table of Contents

1. [Load Testing Kya Hai?](#load-testing-kya-hai)
2. [Types of Load Tests](#types-of-load-tests)
3. [Tools Comparison](#tools-comparison)
4. [k6 (Recommended)](#k6-recommended)
5. [JMeter](#jmeter)
6. [Gatling (Scala DSL)](#gatling-scala-dsl)
7. [Artillery, wrk, hey, ab](#artillery-wrk-hey-ab)
8. [Test Scenarios](#test-scenarios)
9. [Latency Targets — P50/P95/P99](#latency-targets--p50p95p99)
10. [Throughput (RPS / TPS)](#throughput-rps--tps)
11. [Error Rate Thresholds](#error-rate-thresholds)
12. [Capacity Planning](#capacity-planning)
13. [Production Pitfalls](#production-pitfalls)
14. [CI/CD Integration](#cicd-integration)
15. [Common Pitfalls](#common-pitfalls)
16. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Load Testing Kya Hai?

**Matlab:** Application ko **simulated traffic** se hit karna so you can:
- Verify it meets performance SLAs
- Find breaking point
- Catch performance regressions
- Plan capacity

> "Production is the worst place to discover your app crashes at 100 RPS."

### When to Load Test?

✅ Before launch
✅ Before traffic spikes (Black Friday, marketing campaign)
✅ After major refactors
✅ After infrastructure changes (DB upgrade, K8s migration)
✅ Continuously in CI/CD (regression detection)

❌ Without performance targets
❌ Against production (rarely; chaos eng instead)

---

## Types of Load Tests

### 1. Smoke Test

```
1-5 RPS for 1-5 min
Verify: app starts, basic endpoints work
```

→ "Does it work at all?"

### 2. Load Test (Standard)

```
Expected RPS for sustained period (15-60 min)
Verify: meets SLA at expected load
```

→ "Does it work at typical traffic?"

### 3. Stress Test

```
Ramp RPS up until app fails
Find: breaking point
```

→ "When does it fail?"

### 4. Spike Test

```
Sudden 10× RPS for short period
Verify: app recovers gracefully
```

→ "Can it handle bursts?"

### 5. Soak (Endurance) Test

```
Moderate RPS for long time (4-24+ hours)
Find: memory leaks, resource leaks, slow degradation
```

→ "Does it survive long runs?"

### 6. Scalability Test

```
Test at multiple instance counts (1, 2, 5, 10, 20)
Verify: linear scaling
```

→ "Does adding instances help?"

### Visualization

```
RPS
 │
 │           STRESS  ─────╲
 │  SOAK ─────────────────────────
 │ ┌──┐ SPIKE       
 │ │  │   ┌──┐                  
 │ │  │   │  │   LOAD ───────  
 │ │  │   │  │  ┌────────────┐  
 │ │  │   │  │  │            │  
 │ │  │   │  │  │            │  
 │ ┘  └───┘  └──┘            └─
 │
 └──────────────────────────────▶ time
```

---

## Tools Comparison

| Tool | Language | UI | Pros | Cons |
|------|----------|-----|------|------|
| **k6** | JS | CLI + dashboard | Modern, dev-friendly, cloud SaaS | JS limitations |
| **JMeter** | Java/GUI | Yes | Mature, plugins, GUI | Heavy, slower scripting |
| **Gatling** | Scala DSL | Reports | High perf, beautiful reports | Scala learning curve |
| **Artillery** | YAML/JS | CLI | Simple, good for HTTP/WS | Less mature than k6 |
| **wrk** | Lua | CLI | Very fast, simple | Limited scripting |
| **hey** | Go | CLI | Tiny, simple | Not for complex scenarios |
| **ab** (Apache Bench) | C | CLI | Built-in often | Very limited |
| **Locust** | Python | Web UI | Pythonic, distributed | Slower than Gatling/k6 |

### Recommendation

| Need | Pick |
|------|------|
| Modern dev experience | **k6** |
| Existing JMeter team | JMeter |
| Highest perf + reports | Gatling |
| Quick smoke test | wrk / hey |
| Python team | Locust |

---

## k6

**The most popular modern load tester.** Open-source by Grafana Labs (acquired k6 in 2021).

### Install

```bash
# macOS
brew install k6

# Docker
docker run -i --rm grafana/k6 run - <script.js
```

### Basic Test

```javascript
// load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 50,           // virtual users
  duration: '5m',     // run for 5 min
};

export default function () {
  const res = http.get('https://api.example.com/users');
  check(res, {
    'status is 200': (r) => r.status === 200,
    'duration < 200ms': (r) => r.timings.duration < 200,
  });
  sleep(1);
}
```

```bash
k6 run load-test.js
```

### Output

```
checks.........................: 100.00% ✓ 14987 ✗ 0
data_received..................: 4.2 MB  14 kB/s
data_sent......................: 1.5 MB  5.0 kB/s
http_req_blocked...............: avg=1.2ms   min=0s med=0s    max=234ms  p(90)=0s    p(95)=2ms
http_req_duration..............: avg=45ms    min=12ms med=42ms max=312ms  p(90)=78ms  p(95)=98ms
http_req_failed................: 0.00%   ✓ 0    ✗ 14987
http_reqs......................: 14987   49.96/s
iterations.....................: 14987   49.96/s
vus............................: 50
```

→ Clear stats including **percentiles** (p90, p95).

### Stages (Ramp Up / Down)

```javascript
export const options = {
  stages: [
    { duration: '2m', target: 50 },     // ramp up to 50 users
    { duration: '5m', target: 50 },     // stay at 50
    { duration: '2m', target: 200 },    // ramp to 200
    { duration: '5m', target: 200 },    // stay at 200
    { duration: '2m', target: 0 },      // ramp down
  ],
};
```

### Scenarios

```javascript
export const options = {
  scenarios: {
    constant_load: {
      executor: 'constant-vus',
      vus: 50,
      duration: '5m',
    },
    spike: {
      executor: 'ramping-vus',
      stages: [
        { duration: '30s', target: 500 },
        { duration: '30s', target: 0 },
      ],
      startTime: '5m',   // start after constant_load
    },
  },
};
```

### Thresholds (Pass/Fail)

```javascript
export const options = {
  thresholds: {
    http_req_duration: ['p(95)<200', 'p(99)<500'],   // P95 < 200ms, P99 < 500ms
    http_req_failed: ['rate<0.01'],                   // error rate < 1%
    'http_req_duration{type:write}': ['p(95)<300'],   // tagged threshold
  },
};
```

→ k6 exits with non-zero code if thresholds fail. Perfect for CI.

### Authenticated Workflows

```javascript
import http from 'k6/http';

export function setup() {
  const loginRes = http.post('https://api.example.com/auth/login',
    JSON.stringify({ user: 'test', pass: 'test' }),
    { headers: { 'Content-Type': 'application/json' } });
  return { token: loginRes.json('token') };
}

export default function (data) {
  const headers = { 'Authorization': `Bearer ${data.token}` };
  http.get('https://api.example.com/users', { headers });
}
```

### Realistic Scenarios — Workflow Test

```javascript
import { group } from 'k6';

export default function () {
  group('user journey', function () {
    const loginRes = http.post('/auth/login', ...);
    const token = loginRes.json('token');
    
    group('browse products', function () {
      http.get('/products', { headers });
      sleep(2);
      http.get('/products/123', { headers });
    });
    
    group('add to cart', function () {
      http.post('/cart', JSON.stringify({ productId: 123 }), { headers });
    });
    
    group('checkout', function () {
      http.post('/checkout', { headers });
    });
  });
}
```

### Cloud Output

```bash
k6 run --out cloud load-test.js   # k6 Cloud (paid SaaS)
k6 run --out influxdb=http://...  # custom backend
```

### Grafana Integration

```bash
k6 run --out experimental-prometheus-rw=http://prom:9090/api/v1/write
```

→ Live dashboard during test.

---

## JMeter

Mature, GUI-based, extensive plugin ecosystem. Heavy but still widely used.

### Install

```bash
brew install jmeter
jmeter   # opens GUI
```

### Test Plan Structure

```
Test Plan
├── Thread Group (50 users, 10 min)
│   ├── HTTP Request Defaults
│   ├── HTTP Header Manager
│   ├── HTTP Request: GET /users
│   ├── HTTP Request: POST /orders
│   ├── Assertion: Response code = 200
│   └── Listener: Summary Report
```

### CLI Mode (Recommended for CI)

```bash
jmeter -n -t test-plan.jmx -l results.jtl -e -o report-folder
#       ↑   ↑                ↑               ↑   ↑
#       no GUI test plan      output         html report
```

### Distributed Mode

```bash
# Master + workers
jmeter -n -t test.jmx -R worker1,worker2,worker3
```

### Pros / Cons

✅ Mature, lots of plugins
✅ GUI for non-coders
✅ Distributed support
✅ Extensive protocol support (HTTP, JDBC, JMS, FTP)

❌ Heavy memory usage
❌ XML test plans hard to version-control nicely
❌ Slower than Gatling / k6
❌ GUI mode not for actual testing (CLI only)

---

## Gatling (Scala DSL)

High-performance load tester with beautiful HTML reports.

### Setup

```bash
brew install gatling
# Or use Maven plugin
```

### Simulation (Scala)

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class LoadSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("https://api.example.com")
    .acceptHeader("application/json")
  
  val scn = scenario("User Flow")
    .exec(http("Get Users").get("/users"))
    .pause(1)
    .exec(http("Get Product").get("/products/123"))
    .pause(2)
    .exec(http("Add to Cart")
      .post("/cart")
      .body(StringBody("""{"productId":123}""")).asJson)
  
  setUp(
    scn.inject(
      rampUsers(100).during(60.seconds),
      constantUsersPerSec(50).during(5.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
      global.responseTime.percentile3.lt(200),  // P95 < 200ms
      global.failedRequests.percent.lt(1)       // < 1% failure
   )
}
```

### Run

```bash
mvn gatling:test
```

### Reports

Beautiful HTML report with:
- Active users over time
- Response time distribution
- Latency percentiles
- Per-request breakdown

### Pros / Cons

✅ Extremely fast (uses Akka under hood)
✅ Beautiful reports out of box
✅ Type-safe DSL
✅ Code-as-config

❌ Scala learning curve (Java/Kotlin DSL also available)
❌ Less ecosystem than JMeter

---

## Artillery, wrk, hey, ab

### Artillery (YAML)

```yaml
config:
  target: https://api.example.com
  phases:
    - duration: 60
      arrivalRate: 50
scenarios:
  - flow:
      - get:
          url: "/users"
      - post:
          url: "/orders"
          json:
            productId: 123
```

```bash
artillery run test.yml
```

### wrk (Tiny + Fast)

```bash
wrk -t12 -c400 -d30s --latency http://api.example.com/users
#    threads connections duration
```

```
Running 30s test @ http://api.example.com/users
  12 threads and 400 connections
  Latency Distribution
     50%   20.5ms
     75%   30.2ms
     90%   45.8ms
     99%  120.3ms
  Requests/sec:  15234.56
  Transfer/sec:    4.50MB
```

→ Great for quick benchmarks; minimal scripting.

### hey (Go)

```bash
hey -z 30s -c 100 http://api.example.com/users
```

→ Even simpler than wrk.

### Apache Bench (ab)

```bash
ab -n 10000 -c 100 http://api.example.com/users
```

→ Old; very limited; ubiquitous.

---

## Test Scenarios

### Realistic Workload

| Component | What |
|-----------|------|
| **Mixed endpoints** | Read/write/heavy queries proportionally |
| **Realistic data** | Random IDs from real distribution |
| **Think time** | Sleep between requests (real users pause) |
| **User sessions** | Login → action → action → logout |
| **Error injection** | Some requests with bad input |
| **Different user types** | 80% read, 15% write, 5% admin |

### Anti-Patterns

❌ All requests to `/health` (unrealistic)
❌ Same ID always (cache hit always)
❌ No think time (200 RPS != real user load)
❌ Single endpoint test (doesn't reflect production mix)

### Recording Real Traffic

```bash
# Capture from production
tcpdump → convert to k6 / JMeter scenario
```

→ Tools: GoReplay, Tcpcopy, k6's HAR converter.

---

## Latency Targets — P50/P95/P99

### Why Percentiles, Not Average?

```
9 requests at 50ms + 1 request at 5000ms

Average: 545ms              ← misleading, says "fast"
P50: 50ms                   ← median
P90: 5000ms                 ← shows the long tail
```

→ **Average lies.** Always report percentiles.

### Common Targets

| Endpoint Type | P50 | P95 | P99 |
|--------------|-----|-----|-----|
| Cached read | 1-5ms | 10ms | 50ms |
| DB read | 20-50ms | 100ms | 200ms |
| DB write | 50-100ms | 200ms | 500ms |
| Complex aggregation | 100-300ms | 500ms | 1s |
| External API call | 100-500ms | 1s | 2s |

### SLA / SLO Format

```
Service Level Objective:
  99% of requests should complete in < 200ms
  99.9% of requests should complete in < 500ms
  Error rate should be < 0.1%
```

### One-Nine vs Nine-Nines

| Availability | Downtime/year |
|--------------|---------------|
| 99% (2 nines) | 3.65 days |
| 99.9% (3 nines) | 8.77 hours |
| 99.99% (4 nines) | 52.6 min |
| 99.999% (5 nines) | 5.26 min |

→ Each "9" = ~10× more effort + cost.

### Latency vs Throughput

```
Low latency: per-request fast
High throughput: total RPS handled

Often trade-off:
  Optimize latency: smaller batches, sync calls
  Optimize throughput: bigger batches, async, queueing
```

---

## Throughput (RPS / TPS)

### Measuring

```
Requests Per Second (RPS): web-facing
Transactions Per Second (TPS): typically DB writes

Both = total / time
```

### Capacity per Instance

```
Single instance: 500 RPS at acceptable latency

Scale to 5000 RPS:
  Linear scaling: 10 instances
  Reality: 12-15 (overhead)
```

### Bottleneck Identification

```
Add instances → throughput plateaus
  → bottleneck is shared (DB, cache, network)
  
Add instances → throughput grows linearly
  → app-bound; can keep scaling
```

---

## Error Rate Thresholds

### Acceptable Limits

| Context | Acceptable |
|---------|-----------|
| Critical (payment) | < 0.01% |
| Standard API | < 0.1% |
| Background jobs | < 1% (with retry) |
| Reporting | < 5% (acceptable failures) |

### Types of Errors

```
HTTP 4xx: client errors (don't count toward server SLA)
HTTP 5xx: server errors (count toward SLA)
Timeouts: probably 5xx territory
Network errors: count
```

### k6 Threshold

```javascript
thresholds: {
  http_req_failed: ['rate<0.01'],   // 1% allowed
  'http_req_failed{endpoint:checkout}': ['rate<0.001'],  // checkout stricter
}
```

---

## Capacity Planning

### Step-by-Step

1. **Baseline single-instance capacity** — RPS at acceptable latency
2. **Scale to find shared-resource limits** (DB, cache)
3. **Estimate target peak load** (with safety margin 2-3×)
4. **Calculate instances needed** = peak / per-instance × buffer
5. **Plan auto-scaling thresholds** (CPU, RPS, latency)

### Example

```
Single instance: 500 RPS at P95 = 100ms
Peak target: 5000 RPS
Safety margin: 2× → 10000 RPS capacity needed
Instances: 10000 / 500 = 20

But with k8s HPA:
  Min 5 (handles 2500 RPS baseline)
  Max 25 (handles 12500 RPS spike)
  Scale at 70% CPU
```

### Bottleneck Hunting

```
DB CPU at 90% with 5 instances?
  → DB is bottleneck. Adding app instances won't help.
  → Optimize queries, add read replicas, cache.

App instances at 30% CPU but throughput plateau?
  → External API rate limit? Network? Investigate.
```

---

## Production Pitfalls

### Don't Test Production Without Plan

⚠️ Real users impacted; cost spike; data corruption.

→ Use staging that mirrors production, OR carefully canary in production with low traffic.

### Synthetic Data

Test data should look like prod:
- Similar size distribution
- Similar value distribution
- Similar relationships

→ Anonymize prod data → use as test fixture.

### Test Environment ≠ Production

Common mismatches:
- Less data → faster queries
- Single instance → no LB issues
- Local network → no latency
- No real auth provider

→ Document these; results are estimates not absolutes.

---

## CI/CD Integration

### Run on PR

```yaml
# .github/workflows/load-test.yml
name: Load Test
on:
  pull_request:
    branches: [main]

jobs:
  k6:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: grafana/k6-action@v0.2.0
        with:
          filename: load-test.js
        env:
          K6_VUS: 50
          K6_DURATION: 2m
```

### Threshold-Based Pass/Fail

```javascript
thresholds: {
  http_req_duration: ['p(95)<300'],
  http_req_failed: ['rate<0.01'],
}
```

→ Build fails if perf regresses.

### Nightly Long Tests

```yaml
schedule:
  - cron: '0 2 * * *'   # 2 AM daily — soak test
```

### Performance Budget

Track P95 over time. If new release pushes P95 from 100ms → 150ms = regression. Block deploy.

---

## Common Pitfalls

### 1. Testing in Wrong Environment

Local laptop ≠ production K8s cluster. Use realistic env.

### 2. Single Endpoint Tests

Production = mix of endpoints. Test the mix.

### 3. Ignoring Think Time

```javascript
sleep(1);   // ✅ realistic
```

→ Without sleep, you simulate impossible behavior.

### 4. Not Warming Up

JIT, caches, connection pools cold → first 30s misleading.

→ Add ramp-up phase.

### 5. Test Generator as Bottleneck

If load gen machine maxes CPU/network, you're measuring its limits, not the app.

→ Distribute load gen (k6 cloud, multi-machine JMeter).

### 6. Reporting Average Latency

Always P50, P95, P99. Average hides issues.

### 7. Not Monitoring Server Side

Only client-side metrics → miss DB CPU, GC pauses, queue depth.

→ Combine with APM during test.

### 8. No Realistic Data

```
All requests for product=1 (cache hit)
vs
Random product 1-10000 (cache miss + DB)
```

→ Massive perf difference.

### 9. Ignoring Capacity of Downstream

Your app handles 1000 RPS, but external API limits you to 100. Discover this in load test.

### 10. Linear Extrapolation

"100 users = 100ms, so 1000 users = 100ms" — wrong. Throughput often breaks down nonlinearly.

### 11. Not Testing Failure Scenarios

Spike test only. Add: DB slowdown, network hiccups, dependency failures (chaos engineering).

### 12. Stress Test Crashes Production

Be very careful what you point load tests at.

### 13. Ignoring Connections

```
50 users, no keepalive → 50 TCP handshakes/sec
50 users with keepalive → reused connections
```

→ Match production behavior.

### 14. No Baseline / Comparison

Single test = useless without comparison. Always compare:
- vs SLA
- vs previous test
- vs different config

---

## Summary Cheat Sheet

| Test Type | Goal |
|-----------|------|
| Smoke | Basic functionality |
| Load | SLA at expected load |
| Stress | Find breaking point |
| Spike | Burst recovery |
| Soak | Long-term stability |

| Tool | Best For |
|------|---------|
| **k6** | Modern dev workflow |
| **Gatling** | High perf + reports |
| **JMeter** | Mature, GUI |
| **wrk / hey** | Quick benchmarks |
| **Artillery** | YAML simplicity |
| **Locust** | Python team |

| Metric | Don't Use Avg |
|--------|--------------|
| P50 | Median user experience |
| P95 | Tail experience |
| P99 | Bad user experience |
| P99.9 | Worst-case scenario |

| Production Targets (Typical) |
|------------------------------|
| API P95 < 200ms |
| API P99 < 500ms |
| Error rate < 0.1% |
| Availability ≥ 99.9% |

| ✅ Do | ❌ Don't |
|-------|---------|
| Test realistic mix | Single endpoint only |
| Include think time | Hammer endpoint |
| P50/P95/P99 | Average only |
| Warm-up phase | Cold-start measurement |
| Threshold-based fail | Eyeball results |
| Compare to baseline | One-shot test |
| Monitor server-side too | Client metrics only |
| Run in CI | Manual once-a-quarter |
| Capacity plan from data | Guess "we'll add servers" |

---

## Practice

1. Install k6; write basic GET test for a Spring Boot endpoint.
2. Run load test (50 VUs, 5 min); observe P95 latency.
3. Add ramp-up + ramp-down stages.
4. Add thresholds; intentionally fail one to see CI behavior.
5. Build multi-step user journey (login → browse → checkout).
6. Run stress test; find breaking RPS.
7. Run spike test; verify graceful recovery.
8. Run 4-hour soak test; check for memory leaks (combine with JFR).
9. Compare same test in JMeter, k6, Gatling — observe perf differences.
10. Set up Grafana dashboard ingesting k6 metrics live.
11. Add load test stage to CI/CD pipeline; block on regression.
12. Capacity-plan for 10× your current peak; document scaling strategy.
