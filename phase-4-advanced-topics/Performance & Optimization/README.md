# Performance & Optimization

Production-grade **performance engineering** for Java/Spring backends — covers full stack: **JVM tuning**, **DB connection pools**, **query optimization**, **caching strategies**, **profiling & APM**, **load testing**. Sab Hinglish mein, real production examples, deep code, comparison tables, common pitfalls, practice exercises.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | JVM Performance Tuning | [01-JVM-Performance-Tuning.md](./01-JVM-Performance-Tuning.md) | Not Started |
| 2 | Connection Pool Tuning | [02-Connection-Pool-Tuning.md](./02-Connection-Pool-Tuning.md) | Not Started |
| 3 | Query Optimization | [03-Query-Optimization.md](./03-Query-Optimization.md) | Not Started |
| 4 | Application-Level Caching | [04-Application-Level-Caching.md](./04-Application-Level-Caching.md) | Not Started |
| 5 | Profiling & APM | [05-Profiling-and-APM.md](./05-Profiling-and-APM.md) | Not Started |
| 6 | Load Testing | [06-Load-Testing.md](./06-Load-Testing.md) | Not Started |

---

## What's Inside Each File?

### [01 — JVM Performance Tuning](./01-JVM-Performance-Tuning.md)
JVM memory model (heap, metaspace, thread stacks, code cache), heap sizing best practices (`-Xms = -Xmx`), **container-aware JVM** with `+UseContainerSupport` and `MaxRAMPercentage=75.0` (critical for Kubernetes), all garbage collectors compared (Serial/Parallel/CMS/**G1GC** default/**ZGC** sub-ms/Shenandoah), GC selection decision tree, **G1GC tuning flags** (`MaxGCPauseMillis`, `IHOP`, `G1NewSizePercent`), ZGC + generational ZGC for low-latency, **GC logging** (Java 9+ unified `-Xlog:gc*`), thread stack size (`-Xss` + virtual threads), other essential flags (`+ExitOnOutOfMemoryError`, `+HeapDumpOnOutOfMemoryError`, `+UseStringDeduplication`), **diagnostic tools** (jcmd, jstat, jmap, jstack, JFR), 14 common pitfalls including 32 GB CompressedOops boundary.

### [02 — Connection Pool Tuning](./02-Connection-Pool-Tuning.md)
Why connection pools matter (cost of TCP/TLS/auth handshake), **HikariCP** as Spring Boot default, **famous pool sizing formula `(cores × 2) + spindles`**, all key HikariCP settings explained (`maximum-pool-size`, `minimum-idle`, `connection-timeout`, `idle-timeout`, **`max-lifetime` < DB timeout** rule, `leak-detection-threshold`, `keepalive-time`), production YAML defaults, multiple DataSources (read/write split with `AbstractRoutingDataSource`), other pool implementations (DBCP2/Tomcat JDBC/C3P0 — why HikariCP wins), **HTTP connection pools** (Apache HttpClient, OkHttp, **Reactor Netty for WebClient** with `ConnectionProvider`), **Micrometer monitoring** (key metrics + alert thresholds), troubleshooting common errors, **PgBouncer pattern** for many-instance scale, 13 pitfalls including external HTTP calls inside `@Transactional`.

### [03 — Query Optimization](./03-Query-Optimization.md)
Identifying slow queries (**`pg_stat_statements`**, MySQL slow log, p6spy), **EXPLAIN / EXPLAIN ANALYZE** complete walkthrough (Seq Scan vs Index Scan vs Index Only Scan, costs, rows, red flags), **all index types** — B-tree (default), **composite** with left-prefix rule, **covering indexes** (Index Only Scan), **partial indexes** (PostgreSQL), **GIN** (JSONB/arrays/full-text), **GiST** (geo/range), **BRIN** (huge time-series), **avoid SELECT *** (and JPA projections alternative), **OFFSET vs keyset/cursor pagination** with performance comparison table (1ms vs 1s+), JOIN optimization (Hash vs Nested Loop, FK indexing), `ANALYZE` and statistics, query hints (use sparingly), **N+1 problem** with JPA + JOIN FETCH/`@EntityGraph`/`@BatchSize` solutions and detection via Hibernate Statistics, 14 pitfalls including functions on indexed columns and implicit type conversion.

### [04 — Application-Level Caching](./04-Application-Level-Caching.md)
**Cache hierarchy** (Browser → CDN → Reverse Proxy → L2 Distributed (Redis) → L1 Local (Caffeine) → DB), **Spring Cache abstraction** complete (`@Cacheable`, `@CachePut`, `@CacheEvict`, `@Caching`, `sync=true`, `condition`, `unless`, custom key generators), **cache key design** (namespacing, versioning, length limits), **TTL vs TTI** with Caffeine/Redis examples, **5 population strategies** (Cache-Aside, Read-Through, Write-Through, Write-Behind, **Refresh-Ahead**), invalidation strategies (TTL/Event/Pattern/Versioning/Tag), Redis SCAN vs KEYS, multi-layer invalidation with pub-sub, **cache warming** on startup + scheduled, **thundering herd problem** with full solution set, **probabilistic early expiration (XFetch)**, **distributed cache locking** with Redis SETNX + Redisson, hot key problem, **negative caching** with Bloom filter, 14 pitfalls including caching mutable objects + cache-in-transaction issue.

### [05 — Profiling & APM](./05-Profiling-and-APM.md)
Profiling vs APM distinction, **async-profiler** (recommended free tool — no safepoint bias, ~1% overhead, CPU/alloc/lock/wall modes, Spring Boot integration), **Java Flight Recorder (JFR)** built into JDK (continuous recording with `disk=true,maxage=1d`, custom events), **JDK Mission Control (JMC)** views and automated analysis, VisualVM for quick local inspection, JProfiler/YourKit when commercial worth it, **flame graphs** (how to read), **Spring Boot Admin** for multi-app dashboards, **production APM** comparison (Datadog, New Relic, Dynatrace, **Elastic APM** open-source, AppDynamics, Honeycomb), **distributed tracing** with W3C Trace Context + OpenTelemetry + Jaeger/Zipkin, **3 pillars of observability** (logs + metrics + traces) with correlation, heap analysis with Eclipse MAT (Leak Suspects, Dominator Tree, Path to GC Roots), common memory leak patterns, 14 pitfalls including safepoint bias and 100% trace sampling cost.

### [06 — Load Testing](./06-Load-Testing.md)
**6 types of load tests** (Smoke, Load, Stress, Spike, Soak, Scalability) with visualization, comprehensive **tools comparison** (k6, JMeter, Gatling, Artillery, wrk, hey, ab, Locust), **k6 deep dive** (basic test, stages, scenarios, **thresholds for pass/fail**, authenticated workflows, multi-step user journeys, cloud + Grafana integration), JMeter (CLI mode + distributed), Gatling Scala DSL with reports, simpler tools, **realistic test scenarios** (mixed endpoints, think time, error injection — anti-patterns to avoid), **why P50/P95/P99 not average** with examples, latency targets per endpoint type, availability nines table (99% to 99.999%), throughput RPS/TPS measuring + bottleneck identification, error rate thresholds, **capacity planning** step-by-step with k8s HPA example, production pitfalls (synthetic data, env mismatch), **CI/CD integration** with GitHub Actions example + nightly soak tests + perf budget, 14 pitfalls including JMeter generator as bottleneck.

---

## Recommended Learning Order

```
1. Profiling & APM (05)         ← measure first; foundation
2. JVM Performance Tuning (01)   ← Java runtime fundamentals
3. Connection Pool Tuning (02)   ← DB/HTTP throughput
4. Query Optimization (03)       ← biggest perf wins typically
5. Application-Level Caching (04)← reduce load on slow tiers
6. Load Testing (06)             ← validate + capacity plan
```

→ Reverse intuition: Most folks jump to caching/JVM tuning. **First profile**, find actual bottleneck, then fix.

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Heap sizing for K8s | 01 | Container-Aware JVM |
| Choose GC | 01 | Choosing the Right GC |
| GC log analysis | 01 | GC Logging |
| HikariCP tuning | 02 | Key HikariCP Settings |
| Pool sizing formula | 02 | Pool Sizing Formula |
| WebClient pool | 02 | HTTP Connection Pools |
| EXPLAIN plan reading | 03 | EXPLAIN / Execution Plans |
| Composite index design | 03 | Composite Indexes |
| Keyset pagination | 03 | Pagination — Offset vs Keyset |
| N+1 fix | 03 | N+1 Problem (JPA) |
| Spring `@Cacheable` | 04 | Spring Cache Abstraction |
| Multi-level cache | 04 | Cache Hierarchy |
| Thundering herd fix | 04 | Thundering Herd Problem |
| Distributed lock | 04 | Distributed Cache Locking |
| Flame graph | 05 | Async-Profiler |
| Always-on profiling | 05 | Java Flight Recorder |
| Heap dump analysis | 05 | Heap Analysis |
| APM tool selection | 05 | Production APM |
| k6 load test | 06 | k6 (Recommended) |
| Capacity planning | 06 | Capacity Planning |
| CI perf regression | 06 | CI/CD Integration |
| Latency targets | 06 | Latency Targets — P50/P95/P99 |

---

## Performance Engineering Workflow

```
┌────────────────────────────────────────────────────────────────┐
│  1. DEFINE TARGETS (file 06)                                     │
│     - SLA: P95 < 200ms, error < 0.1%, availability 99.9%        │
└────────────────────────────────┬───────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────┐
│  2. INSTRUMENT (file 05)                                         │
│     - Micrometer + Prometheus + APM + JFR continuous            │
└────────────────────────────────┬───────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────┐
│  3. LOAD TEST BASELINE (file 06)                                 │
│     - k6 / Gatling at expected + peak load                       │
└────────────────────────────────┬───────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────┐
│  4. PROFILE BOTTLENECK (file 05)                                 │
│     - Async-profiler flame graph                                  │
│     - JMC for JFR analysis                                        │
└────────────────────────────────┬───────────────────────────────┘
                                 ▼
              ┌──────────────────┴──────────────────┐
              ▼                                     ▼
    DB BOTTLENECK?                         APP BOTTLENECK?
    ─ Slow query (file 03)                 ─ JVM tuning (file 01)
    ─ Connection pool (file 02)            ─ Algorithm change
    ─ Add cache (file 04)                  ─ Async / parallelism
                       │                           │
                       └─────────────┬─────────────┘
                                     ▼
┌────────────────────────────────────────────────────────────────┐
│  5. RE-MEASURE (file 06)                                         │
│     - Run same load test; verify improvement                     │
└────────────────────────────────┬───────────────────────────────┘
                                 ▼
┌────────────────────────────────────────────────────────────────┐
│  6. CAPACITY PLAN (file 06)                                      │
│     - Per-instance capacity × safety margin                      │
│     - Configure HPA / auto-scaling                               │
└────────────────────────────────────────────────────────────────┘
```

---

## Cross-Stack Bottleneck Map

```
SYMPTOM                     LIKELY CAUSE                    READ FILE
──────────────────────────────────────────────────────────────────
P99 latency spikes          GC pauses                        01 (JVM)
                            DB query spikes                  03 (Query)
                            Cache miss + thundering herd     04 (Cache)
                            
"Connection timeout"        Pool exhausted                   02 (Pool)
                            DB max_connections hit            02 (PgBouncer)
                            Slow query hogging connections   03 (Query)
                            
OOMKilled in K8s            JVM not container-aware           01 (JVM)
                            Heap leak                         05 (Heap)
                            Off-heap buffer growth            01 (JVM)
                            
Throughput plateau          DB bottleneck                    03 (Query)
                            Single-threaded code             01 (JVM)
                            External API limit               06 (Load test reveals)
                            
High CPU                    Inefficient algorithm            05 (Profile)
                            Excessive GC                     01 (JVM)
                            Lock contention                  05 (async-profiler -e lock)
                            
Cold start slow             JIT warmup                       06 (Warm-up phase)
                            Cache cold                       04 (Cache warming)
                            Pool initialization              02 (minimum-idle)
                            
Latency degrades over time  Memory leak                      05 (Heap dump)
                            Connection leak                  02 (leak-detection)
                            Cache size unbounded             04 (maximumSize)
```

---

## Companion Folders

- [Microservices Architecture](../Microservices%20Architecture/) — `06-Service-Discovery-and-Tracing.md` for distributed tracing context
- [Spring Cloud](../Spring%20Cloud/) — Gateway has rate limiting + resilience; LoadBalancer for client-side LB
- [Messaging — Kafka](../Messaging%20—%20Kafka/) and [Messaging — RabbitMQ](../Messaging%20—%20RabbitMQ/) — async patterns help with throughput
- [Database Mastery](../../backend-skills/Database-Mastery/) — deep SQL/index knowledge supports query optimization
- [DevOps & CI/CD](../../backend-skills/DevOps-&-CI-CD/) — running load tests in pipelines
- [Code Quality & Best Practices](../../backend-skills/Code-Quality-&-Best-Practices/) — algorithmic complexity awareness

---

## Tools Reference

### JVM Tuning
- JDK 21+ recommended (virtual threads, modern G1)
- jcmd, jstat, jmap, jstack (built-in)
- VisualVM, JConsole (free GUIs)

### Connection Pools
- **HikariCP** (default Spring Boot) — DB
- Apache HttpClient + PoolingHttpClientConnectionManager — RestTemplate
- Reactor Netty ConnectionProvider — WebClient
- OkHttp ConnectionPool

### Query Optimization
- pgAdmin / DBeaver / DataGrip (DB GUIs)
- pg_stat_statements (Postgres extension)
- p6spy (log SQL with timings)
- explain.depesz.com / explain.dalibo.com (plan visualizers)

### Caching
- **Caffeine** — best in-memory L1
- **Redis** — distributed L2
- Spring Cache abstraction
- Redisson — Redis-based distributed primitives

### Profiling
- **async-profiler** — flame graphs
- **JFR + JDK Mission Control** — built-in
- **VisualVM** — quick GUI
- **Eclipse MAT** — heap dumps
- **Pyroscope** — continuous profiling

### APM (Production)
- **Datadog**, **New Relic**, **Dynatrace**, **AppDynamics** (commercial)
- **Elastic APM**, **SigNoz** (open-source)
- **Honeycomb** (observability-focused)
- **OpenTelemetry** (instrumentation standard)

### Load Testing
- **k6** (recommended modern)
- **Gatling** (Scala DSL, perf + reports)
- **JMeter** (mature, GUI)
- **Artillery, wrk, hey, ab** (simpler / quick)
- **Locust** (Python)

### Observability Stack
- **Prometheus + Grafana** (metrics)
- **ELK / Loki + Grafana** (logs)
- **Jaeger / Tempo / Zipkin** (traces)
- **Grafana Cloud** (managed all-in-one)

---

## Status Tracker

```
[ ] 01 — JVM Performance Tuning
[ ] 02 — Connection Pool Tuning
[ ] 03 — Query Optimization
[ ] 04 — Application-Level Caching
[ ] 05 — Profiling & APM
[ ] 06 — Load Testing
```

Topic complete hone par file header aur is README dono mein status update kar lena.

> "**Premature optimization is the root of all evil**" — Knuth.
> "**But measured optimization is the root of all good**" — corollary.
> 
> Profile first, fix bottleneck, repeat. Don't tune randomly.
