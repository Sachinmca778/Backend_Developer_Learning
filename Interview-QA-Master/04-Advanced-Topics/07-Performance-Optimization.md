# Performance & Optimization - Top 10 Interview Questions & Answers

> Covering: JVM Tuning, Garbage Collection, Connection Pool (HikariCP), Query Optimization, Indexes, Application Caching, Profiling & APM, and Load Testing

---

## Q1: JVM memory model kya hai aur heap ke alag parts kaunse hain?

**Answer:**

Total JVM memory sirf heap nahi hoti — kai components hote hain.

```
JVM Process Memory:
├── Heap (-Xms / -Xmx)
│   ├── Young Gen (Eden + S0/S1) — short-lived objects
│   └── Old Gen — long-lived objects
├── Metaspace — class metadata
├── Thread Stacks (-Xss per thread)
├── Code Cache — JIT-compiled code
└── Direct Buffers (NIO), Native libraries
```

### Total ≠ Heap:

```
Container limit: 1 GB
Heap (-Xmx): 768 MB
Metaspace: ~100 MB
Thread stacks: 200 × 1 MB = 200 MB
Code cache: ~48 MB
Total: ~1.1 GB → OOMKilled!
```

### Object Lifecycle:

```
New object → Eden → (survives GC) → Survivor → (survives more) → Old Gen
```

**Important:**

Sabse common galti: `-Xmx` ko container memory ke barabar set kar dena. Heap sirf ek part hai — metaspace, thread stacks, code cache, direct buffers bhi RAM khate hain. Container mein `-XX:MaxRAMPercentage=75` use karo taaki non-heap ke liye space bache aur OOMKilled na ho.

---

## Q2: Java mein garbage collectors kaunse hain aur kaunsa kab choose karte hain?

**Answer:**

| GC | Best For | Trade-off |
|----|----------|-----------|
| **Serial** | Small heaps, single CPU | Stop-the-world, simple |
| **Parallel** | Throughput-focused batch | High throughput, longer pauses |
| **G1GC** (default) | Balanced (most apps) | Predictable pauses, good all-round |
| **ZGC** | Low latency, large heaps | Sub-millisecond pauses (<10ms) |
| **Shenandoah** | Low latency | Concurrent, low pause |

### Choosing:

```
Batch processing / throughput → Parallel GC
General web app → G1GC (default, Java 9+)
Latency-critical (large heap) → ZGC / Shenandoah
```

### G1GC Tuning:

```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200      # target pause time
-XX:G1HeapRegionSize=16m
```

### ZGC (Ultra-Low Latency):

```bash
-XX:+UseZGC   # sub-millisecond pauses even for TB heaps
```

**Short Interview Answer:**

G1GC default hai (Java 9+) aur most apps ke liye balanced hai — predictable pauses. Throughput-heavy batch jobs ke liye Parallel GC. Latency-critical apps (large heaps, low pause chahiye) ke liye ZGC ya Shenandoah jo sub-millisecond pauses dete hain. Measure karke choose karo, guess nahi.

---

## Q3: Connection pool kya hai aur HikariCP ko kaise tune karte hain?

**Answer:**

Connection pool pre-created **reusable connections** rakhta hai taaki har request par connection open (50-100ms) na karna pade.

```
Without pool: har request → open connection (50-100ms) → query → close
With pool: borrow (< 1ms) → query → return to pool
```

### HikariCP (Spring Boot Default):

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Pool Sizing Formula:

```
pool size = (core_count × 2) + effective_spindle_count
```

→ Chhota pool aksar zyada performant hota hai! Bahut bade pool se DB overload aur context switching badh jaati hai.

### DB Limits:

```
app instances × pool size ≤ DB max_connections
Example: 10 instances × 20 pool = 200 > PostgreSQL default 100 → ❌
```

**Important:**

Common myth: "bada pool = zyada performance". Actually chhota pool (10-20) aksar better hai — DB ke paas limited cores hain, aur zyada connections context switching aur contention badhate hain. Total connections (`instances × pool_size`) DB ke `max_connections` se kam hone chahiye. `leak-detection-threshold` set karke connection leaks pakdo.

---

## Q4: Slow queries ko kaise identify aur optimize karte hain?

**Answer:**

### 1. Identify:

```sql
-- PostgreSQL
SELECT query, calls, mean_exec_time, total_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 20;
```

### 2. Analyze with EXPLAIN:

```sql
EXPLAIN ANALYZE SELECT * FROM orders WHERE user_id = 123;
```

→ Dekho: Seq Scan (bad, full table) vs Index Scan (good).

### 3. Optimize Techniques:

| Technique | Impact |
|-----------|--------|
| Add index on WHERE/JOIN columns | Huge |
| Avoid `SELECT *` | Less I/O |
| Keyset pagination (not OFFSET) | Deep pages fast |
| Fix N+1 queries | Fewer round-trips |
| Composite/covering indexes | Index-only scans |

### Example — Index:

```sql
-- Before: Seq Scan on 1M rows (500ms)
SELECT * FROM orders WHERE user_id = 123;

-- Add index
CREATE INDEX idx_orders_user_id ON orders(user_id);
-- After: Index Scan (2ms) — 250x faster
```

**Short Interview Answer:**

Pehle slow queries identify karo (`pg_stat_statements`, slow query log), phir `EXPLAIN ANALYZE` se execution plan dekho (Seq Scan = red flag). Optimize: WHERE/JOIN columns par index, `SELECT *` avoid, keyset pagination, N+1 fix. Query optimization backend performance mein highest ROI activity hai — 10x improvements common.

---

## Q5: Database indexes kaise kaam karte hain? Index types kaunse hain?

**Answer:**

Index ek data structure hai jo lookups fast karta hai (full table scan avoid) — book ke index jaisa.

### Types:

| Index | Use Case |
|-------|----------|
| **B-Tree** (default) | Equality, range, sorting (most common) |
| **Hash** | Exact equality only |
| **Composite** | Multiple columns |
| **Covering** | Query saare columns index se milen (index-only scan) |
| **Partial** | Subset of rows (`WHERE active = true`) |
| **GIN** | Full-text search, JSONB, arrays |

### Composite Index — Column Order Matters:

```sql
CREATE INDEX idx_orders ON orders(user_id, status, created_at);
```

→ Leftmost prefix rule: ye index `user_id`, ya `user_id + status`, ya teeno par kaam karega — but sirf `status` par nahi.

### Covering Index:

```sql
CREATE INDEX idx_cover ON orders(user_id) INCLUDE (total, status);
-- Query index se hi satisfy → table access nahi (fastest)
```

**Important:**

Indexes reads fast karte hain but writes slow (har insert/update par index update). Isliye randomly indexes mat banao — sirf actual slow queries ke liye. Composite index mein column order critical hai (leftmost prefix rule): sabse selective/frequently-filtered column pehle. Over-indexing write performance aur disk waste karti hai.

---

## Q6: Application-level caching kya hai aur kab use karte hain?

**Answer:**

Frequently/recently accessed data ko fast storage (memory/Redis) mein rakhna taaki expensive DB/API/computation avoid ho.

### Wins:

| Without Cache | With Cache |
|--------------|-----------|
| DB query: 50ms | Memory hit: 0.01ms (5000×) |
| External API: 200ms | Redis hit: 1ms (200×) |

### Cache Hierarchy:

```
L1: Local (in-process, Caffeine) — fastest, per-instance
L2: Distributed (Redis) — shared across instances
CDN: Edge cache — static content
```

### Spring Cache:

```java
@Cacheable(value = "products", key = "#id")
public Product getProduct(Long id) {
    return repository.findById(id).orElseThrow();
}
```

### When to Cache:

- Read-heavy data (read:write > 10:1)
- Expensive computation
- External API calls
- Static reference data

**Short Interview Answer:**

Caching frequently accessed data ko fast storage mein rakhta hai — DB/API round-trips avoid. L1 (local Caffeine, fastest), L2 (Redis, shared), CDN (edge). Read-heavy data, expensive computation, external APIs ke liye best. Challenge: cache invalidation aur consistency. TTL set karo, sensible keys design karo.

---

## Q7: Cache stampede (thundering herd) problem kya hai aur kaise solve karte hain?

**Answer:**

Jab ek popular cache key expire hoti hai, hazaaron concurrent requests ek saath DB par hit karti hain (kyunki sabko cache miss milta hai) — DB overwhelm.

```
Cache key "top-products" expires
→ 10,000 requests simultaneously miss
→ 10,000 identical DB queries
→ DB overload / crash
```

### Solutions:

### 1. Distributed Lock (single recompute):

```java
if (cache.miss(key)) {
    if (lock.tryLock(key)) {          // sirf ek thread compute kare
        value = db.query();
        cache.put(key, value);
        lock.unlock(key);
    } else {
        value = waitAndGetFromCache(key);  // baaki wait karen
    }
}
```

### 2. Probabilistic Early Expiration:

Cache expire hone se **thoda pehle** hi ek request proactively refresh kar deti hai (randomized), taaki sab ek saath expire na dekhen.

### 3. Cache Warming:

Startup par ya expiry se pehle proactively cache populate karo.

**Important:**

Cache stampede = ek hot key expire hone par sab requests ek saath DB par jaana. Solutions: distributed lock (sirf ek thread recompute kare, baaki wait karen), probabilistic early expiration (randomly thoda pehle refresh), aur cache warming. Hot key problem alag hai — ek key itni popular ho ki ek Redis node overload ho (solution: local L1 cache).

---

## Q8: Profiling kya hai aur kaunse tools use karte hain?

**Answer:**

Profiling application ke runtime behavior ko measure karta hai (CPU, memory, locks, I/O) taaki actual bottlenecks pata chalen — guess nahi.

> "Premature optimization is the root of all evil" — but measured optimization root of all good.

### Profiling vs APM:

| | Profiling | APM |
|--|-----------|-----|
| Where | Dev/staging (deep) | Production (continuous) |
| Detail | Method-level, flame graphs | Request-level, dashboards |
| Tools | async-profiler, JFR | Datadog, New Relic, Dynatrace |

### Tools:

- **async-profiler** — low overhead, flame graphs (recommended)
- **Java Flight Recorder (JFR)** — built-in, production-safe
- **JDK Mission Control (JMC)** — JFR analysis
- **VisualVM** — free, basic profiling

### Flame Graphs:

```
Width = time spent (wider = hotter)
→ Widest bars = bottlenecks
```

### 3 Pillars of Observability:

```
Logs (what happened) + Metrics (aggregated numbers) + Traces (request journey)
```

**Short Interview Answer:**

Profiling runtime behavior (CPU, memory, locks) measure karta hai actual bottlenecks find karne ke liye. Dev mein deep profiling (async-profiler, JFR, flame graphs), production mein APM (Datadog, New Relic) continuous monitoring. Flame graphs mein widest bars = hotspots. Hamesha measure karke optimize karo, assume nahi.

---

## Q9: Load testing kya hai aur latency percentiles (P50/P95/P99) kya matlab rakhte hain?

**Answer:**

Load testing app ko **simulated traffic** se hit karke SLA verify, breaking point find, aur regressions catch karta hai.

### Types:

| Type | Purpose |
|------|---------|
| **Load test** | Expected traffic par behaviour |
| **Stress test** | Breaking point dhundna |
| **Spike test** | Sudden traffic surge |
| **Soak test** | Long duration (memory leaks) |

### Percentiles:

```
P50 (median): 50% requests isse fast — typical user
P95: 95% requests isse fast — most users
P99: 99% requests isse fast — worst-case (tail latency)
```

→ **Average deceptive hai!** P99 dekho — 1% users ka experience bhi matter karta hai.

### k6 Example:

```javascript
import http from 'k6/http';
export const options = {
    vus: 100,           // virtual users
    duration: '5m',
    thresholds: {
        http_req_duration: ['p(99)<500'],  // P99 < 500ms
        http_req_failed: ['rate<0.01'],    // error rate < 1%
    },
};
export default function () {
    http.get('https://api.example.com/orders');
}
```

**Important:**

Average latency deceptive hai — P95/P99 (tail latency) dekho kyunki wo real user experience batati hai. Load testing production launch se pehle, traffic spikes (Black Friday) se pehle, aur CI/CD mein regression detection ke liye karo. Tools: k6 (modern), JMeter, Gatling. Realistic scenarios + performance targets ke saath test karo.

---

## Q10: Backend performance optimize karne ka systematic approach kya hai?

**Answer:**

### Golden Rule: Measure First, Optimize Later.

```
1. Baseline metrics collect karo (APM, profiling)
2. Bottleneck identify karo (aksar surprising hota hai)
3. Optimize (highest ROI first)
4. Measure again (verify improvement)
5. Repeat
```

### Common Bottlenecks (Priority Order):

| Area | Fix | Typical Impact |
|------|-----|----------------|
| **Database queries** | Indexes, fix N+1, query rewrite | 10x+ (highest ROI) |
| **Missing caching** | Redis/Caffeine cache | 100-5000x on hits |
| **Connection pools** | HikariCP tuning | Throughput |
| **N+1 queries** | Fetch joins, batch | Fewer round-trips |
| **JVM/GC** | GC tuning, heap sizing | Latency spikes |
| **External calls** | Async, circuit breaker | Resilience |

### Layers to Optimize:

```
Database (indexes, queries) → highest ROI
    ↓
Caching (avoid repeated work)
    ↓
Application (algorithms, async)
    ↓
JVM (GC, memory)
    ↓
Infrastructure (scaling, CDN)
```

**Short Interview Answer:**

Systematic approach: pehle measure karo (APM/profiling se baseline aur bottleneck), phir highest ROI first optimize karo. Priority: database queries + indexes (10x+), caching (100x+ on hits), connection pool tuning, N+1 fixes, phir JVM/GC. Optimize karne ke baad phir measure karo. Kabhi guess ke basis par optimize mat karo — "premature optimization is the root of all evil".
