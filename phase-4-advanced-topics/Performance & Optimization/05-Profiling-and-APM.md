# Profiling & APM

## Status: Not Started

---

## Table of Contents

1. [Profiling Kya Hai?](#profiling-kya-hai)
2. [Profiling vs APM](#profiling-vs-apm)
3. [Local Profilers](#local-profilers)
4. [Async-Profiler (Recommended)](#async-profiler-recommended)
5. [Java Flight Recorder (JFR)](#java-flight-recorder-jfr)
6. [JDK Mission Control (JMC)](#jdk-mission-control-jmc)
7. [VisualVM](#visualvm)
8. [JProfiler & YourKit (Commercial)](#jprofiler--yourkit-commercial)
9. [Flame Graphs](#flame-graphs)
10. [Spring Boot Admin](#spring-boot-admin)
11. [Production APM](#production-apm)
12. [Distributed Tracing](#distributed-tracing)
13. [Logs + Metrics + Traces (3 Pillars)](#logs--metrics--traces-3-pillars)
14. [Heap Analysis](#heap-analysis)
15. [Common Pitfalls](#common-pitfalls)
16. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Profiling Kya Hai?

**Matlab:** Application ke runtime behavior ko **measure** karna — CPU usage, memory allocation, lock contention, I/O waits — taki **bottlenecks identify** kar sako.

> "Premature optimization is the root of all evil."  
> — Donald Knuth  
> 
> Corollary: "But measured optimization is the root of all good."

### Why Profile?

✅ Find actual bottlenecks (often surprising)
✅ Validate performance assumptions
✅ Justify optimization effort with data
✅ Catch regressions

❌ Guess + change code
❌ Optimize without baseline

---

## Profiling vs APM

| | Profiling | APM (Application Performance Monitoring) |
|--|-----------|------------------------------------------|
| **When** | On-demand, deep dive | Always-on, production |
| **Detail** | Method-level, samples | Aggregate stats, traces |
| **Overhead** | Higher (5-30%) | Lower (<5%) |
| **Tools** | JProfiler, async-profiler, JFR | Datadog, New Relic, Dynatrace |
| **Use** | Debugging perf issues | Continuous monitoring + alerting |

→ Use both: APM for "what's wrong?"; profiling for "where exactly?".

---

## Local Profilers

### Quick Comparison

| Tool | Cost | Overhead | Best For |
|------|------|----------|----------|
| **JFR + JMC** | Free | Very low (<2%) | Production-safe profiling |
| **async-profiler** | Free | Very low (~1%) | Flame graphs |
| **VisualVM** | Free | Medium | Quick GUI inspection |
| **JProfiler** | $$$ | Low | Enterprise, deep features |
| **YourKit** | $$$ | Low | Enterprise, polished UI |

→ **For most teams:** async-profiler + JFR + JMC. Free + low overhead + production-safe.

---

## Async-Profiler

**The most useful free profiler for Java.** Low overhead, no safepoint bias, supports CPU + memory + locks.

### Why Async-Profiler?

- **No safepoint bias** — samples at any instruction (other profilers can only sample at safepoints, biased toward "safe" methods)
- **Tiny overhead** (~1%)
- **Native** + uses Linux `perf_events` for accurate CPU samples
- **Multi-mode** — CPU, alloc, lock, wall

### Installation

```bash
# Linux
wget https://github.com/jvm-profiling-tools/async-profiler/releases/download/v3.0/async-profiler-3.0-linux-x64.tar.gz
tar -xzf async-profiler-3.0-linux-x64.tar.gz
cd async-profiler-3.0-linux-x64

# Or use container
docker run --rm -v $(pwd):/output ... 
```

### Profile a Running JVM

```bash
# 30-second CPU profile → flame graph SVG
./profiler.sh -d 30 -f /tmp/flame.html <pid>

# Allocation profile
./profiler.sh -e alloc -d 30 -f /tmp/alloc.html <pid>

# Lock contention
./profiler.sh -e lock -d 30 -f /tmp/locks.html <pid>

# Wall clock (includes I/O wait)
./profiler.sh -e wall -d 30 -f /tmp/wall.html <pid>
```

### Programmatic (Embed in App)

```java
import one.profiler.AsyncProfiler;

AsyncProfiler ap = AsyncProfiler.getInstance();
ap.execute("start,event=cpu,file=/tmp/profile.jfr");
// ... workload ...
ap.execute("stop,file=/tmp/profile.jfr");
```

### Spring Boot Integration

```xml
<dependency>
  <groupId>tools.profiler</groupId>
  <artifactId>async-profiler</artifactId>
  <version>3.0</version>
</dependency>
```

```java
@RestController
public class ProfilerController {
    @GetMapping("/profile/cpu")
    public ResponseEntity<byte[]> profileCpu() throws IOException {
        AsyncProfiler.getInstance().execute("start,event=cpu");
        Thread.sleep(30_000);
        String result = AsyncProfiler.getInstance().execute("stop,file=/tmp/p.html");
        byte[] data = Files.readAllBytes(Paths.get("/tmp/p.html"));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(data);
    }
}
```

→ ⚠️ Lock down endpoint with auth in production.

### Output

Open `flame.html` in browser → interactive flame graph.

---

## Java Flight Recorder (JFR)

**Built into JDK** since Java 11 (free; was commercial earlier). Continuous low-overhead profiling.

### Start at Launch

```bash
java -XX:StartFlightRecording=duration=60s,filename=/tmp/app.jfr -jar app.jar
```

### Start on Running JVM

```bash
jcmd <pid> JFR.start name=myrec settings=profile filename=/tmp/app.jfr duration=120s

# Check status
jcmd <pid> JFR.check

# Dump partial recording
jcmd <pid> JFR.dump name=myrec filename=/tmp/snapshot.jfr

# Stop
jcmd <pid> JFR.stop name=myrec
```

### Settings Profiles

| Profile | Overhead | Use |
|---------|----------|-----|
| `default` | < 1% | Always-on production |
| `profile` | ~2% | Detailed analysis |
| Custom `.jfc` | Tunable | Specific events |

### Continuous Recording (Production)

```bash
java -XX:StartFlightRecording=disk=true,maxage=1d,maxsize=500m,filename=/tmp/app.jfr -jar app.jar
```

→ Always-on; rotates files. Capture last day on demand.

### What JFR Captures

- CPU samples (method profiling)
- Memory allocations
- GC activity
- Thread states + locks
- I/O (file, socket)
- JIT compilation
- Class loading
- Exception throws
- Custom events (`@Name`, `@Label`)

### Custom Events

```java
@Name("my.app.OrderProcessed")
@Label("Order Processed")
@Description("Time taken to process an order")
@Category("Application")
public class OrderProcessedEvent extends Event {
    @Label("Order ID") long orderId;
    @Label("Status") String status;
}

// Usage
OrderProcessedEvent event = new OrderProcessedEvent();
event.begin();
// ... do work ...
event.orderId = order.getId();
event.status = "DONE";
event.commit();
```

→ Captured in JFR alongside JVM events.

---

## JDK Mission Control (JMC)

**GUI** to analyze JFR recordings. Free download from Oracle / OpenJDK.

### Install

```bash
# Linux
wget https://www.oracle.com/.../jmc-9.0.0.tar.gz
tar -xzf jmc-9.0.0.tar.gz
./jmc/JDK\ Mission\ Control.app
```

### Open Recording

File → Open File → select `.jfr`

### Key Views

| View | Shows |
|------|-------|
| **Java Application** | Top-level summary |
| **Method Profiling** | Hot methods (CPU samples) |
| **Memory** | Allocations, GC pressure |
| **Locks** | Contention hotspots |
| **I/O** | Slow file/socket operations |
| **Threads** | Per-thread timeline |
| **JVM Internals** | JIT, classloading, safepoints |

### Diagnostic Insights

JMC has built-in **Automated Analysis** that flags issues:

```
✗ "GC pauses exceed target"
✗ "Thread blocked on lock at com.example.X.method() — 15s total"
✗ "Allocation pressure in OrderProcessor — 200 MB/s"
```

→ Read these first; usually points to obvious problems.

---

## VisualVM

Free GUI; great for quick local inspection.

### Install

```bash
brew install visualvm   # macOS
# Or download standalone
visualvm
```

### Connect to JVM

```
Local: auto-discovers via JDP
Remote: JMX URL (jmx:rmi://host:port/jndi/rmi://host:port/jmxrmi)
```

### Tabs

- **Monitor** — heap, threads, GC live
- **Threads** — current state of all threads
- **Sampler** — CPU + memory sampling
- **Profiler** — instrumented profiling (higher overhead)
- **Heap Dump** — capture + analyze
- **Plugins** — JFR support, etc.

### When to Use?

✅ Quick sanity check on local JVM
✅ Capture heap dump
✅ Visualize GC patterns

❌ Production (high overhead in profiler mode)
❌ Detailed flame graphs (use async-profiler)

---

## JProfiler & YourKit (Commercial)

### JProfiler

- Highly polished GUI
- Method-level CPU + memory
- DB query profiling (JDBC, Hibernate)
- HTTP request tracking
- Heap analysis
- Comparison views

### YourKit

- Similar feature set
- Excellent CPU + memory profiling
- Snapshot comparison
- Strong support

### When Worth Paying?

✅ Enterprise teams with budget
✅ Need polished UI for non-experts
✅ Want vendor support

For most: free tools (async-profiler + JFR) cover 90% of needs.

---

## Flame Graphs

**Visualization** of stack samples — width = time spent.

```
    │ ┌──────────────────────────────────────┐
    │ │             main()                    │
    │ ├──────────────┬──────────────────────┤
    │ │ Controller   │  BackgroundJob        │
    │ ├──────┬──────┼─────┬─────┬──────────┤
    │ │ Svc1 │ Svc2 │ Job1│Job2 │  Repo     │
    │ ├──┬──┼─┬───┼─────┼─────┼─┬────────┤
    │ │A │B │ │ C │ ... │ ... │ │  ...     │
    │ └──┴──┴─┴───┴─────┴─────┴─┴────────┘
```

→ Wider = more CPU. Spot tall narrow stacks (deep call chains) and wide bases (hot methods).

### How to Read

1. **X-axis** = stack samples (sorted alphabetically, NOT time)
2. **Y-axis** = stack depth (call hierarchy)
3. **Width** = time spent in / under that frame
4. **Color** = often random (just for distinction)

### Tools That Generate

- **async-profiler** (`flame.html`)
- **JFR** + **flamegraph-jfr** plugin
- **perf** (Linux) + Brendan Gregg's flamegraph.pl

### Interactive

```html
<!-- Generated by async-profiler -->
Click to zoom into a frame
Search for method name
Hover for sample count + percentage
```

---

## Spring Boot Admin

Centralized **dashboard** for monitoring multiple Spring Boot apps.

### Setup

#### Server App

```xml
<dependency>
  <groupId>de.codecentric</groupId>
  <artifactId>spring-boot-admin-starter-server</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableAdminServer
public class AdminApp { ... }
```

#### Client (Each Monitored App)

```xml
<dependency>
  <groupId>de.codecentric</groupId>
  <artifactId>spring-boot-admin-starter-client</artifactId>
</dependency>
```

```yaml
spring.boot.admin.client.url: http://admin-server:8080
management.endpoints.web.exposure.include: '*'
```

### What You Get

- Health, env, config props per app
- JVM metrics (heap, threads, GC)
- HTTP traces (recent requests)
- Loggers — change log levels at runtime!
- Cache stats
- DB connection pool stats
- Notifications (email, Slack on health changes)

→ Great for small teams without full APM budget.

---

## Production APM

For continuous monitoring + alerting at scale.

### Major Players

| APM | Notes |
|-----|-------|
| **Datadog APM** | Polished, great UX, expensive |
| **New Relic** | Mature, broad features |
| **Dynatrace** | Auto-instrumentation, AI insights |
| **Elastic APM** | Open-source option, ELK integration |
| **AppDynamics** | Cisco-owned, enterprise focus |
| **Honeycomb** | Observability for distributed systems |

### What APM Tracks

- Request tracing (per endpoint latency, error rate)
- Distributed traces across services
- DB query performance
- External call performance (HTTP, gRPC)
- JVM metrics
- Logs correlation
- Alerts on thresholds (P99 latency, error %)

### Setup (Example: Elastic APM)

```xml
<dependency>
  <groupId>co.elastic.apm</groupId>
  <artifactId>apm-agent-attach</artifactId>
</dependency>
```

```bash
java -javaagent:elastic-apm-agent.jar \
  -Delastic.apm.service_name=my-app \
  -Delastic.apm.server_url=http://apm-server:8200 \
  -Delastic.apm.environment=production \
  -jar app.jar
```

### Setup (Example: Datadog)

```bash
java -javaagent:dd-java-agent.jar \
  -Ddd.service=my-app \
  -Ddd.env=prod \
  -Ddd.version=1.2.3 \
  -jar app.jar
```

→ Auto-instruments Spring, JDBC, HTTP, Kafka, etc.

### What to Monitor

| Metric | Alert Threshold |
|--------|-----------------|
| P99 latency | > SLA target |
| Error rate | > 1% |
| DB connection pool | > 80% utilized |
| GC pause time | > 200ms |
| Heap usage | > 85% |
| Thread count | Sudden spike |
| Request rate drop | > 50% drop |

---

## Distributed Tracing

For microservices — trace single request across services.

### W3C Trace Context

```
HTTP header: traceparent: 00-{trace-id}-{span-id}-{flags}
```

### Implementations

- **OpenTelemetry** (industry standard, open-source)
- **Zipkin** (older, simple)
- **Jaeger** (CNCF, popular)
- **AWS X-Ray**
- **Google Cloud Trace**

### Spring + OpenTelemetry

```xml
<dependency>
  <groupId>io.opentelemetry.instrumentation</groupId>
  <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

```yaml
otel:
  service.name: my-service
  exporter.otlp.endpoint: http://otel-collector:4317
  traces.sampler: parentbased_traceidratio
  traces.sampler.arg: 0.1   # sample 10% of traces
```

### Trace Visualization

```
[Frontend] ──▶ [API Gateway] ──▶ [User Svc] ──▶ [DB]
   100ms        20ms              50ms        30ms
                                    └──▶ [Redis]
                                          5ms
```

→ See where time is spent across services.

→ Cross-ref: `Microservices Architecture/06-Service-Discovery-and-Tracing.md`.

---

## Logs + Metrics + Traces (3 Pillars)

### Combined Stack

```
LOGS:    ELK stack (Elasticsearch + Logstash + Kibana), Loki + Grafana
METRICS: Prometheus + Grafana
TRACES:  Jaeger / Zipkin / Tempo
```

### Modern Trend — Single Backend

- **Datadog** — all three
- **Grafana Cloud** — Loki + Mimir + Tempo (OSS-based)
- **New Relic / Dynatrace** — all three

### Correlation

```
trace_id: abc123 → present in:
  - Logs (every log line)
  - Metrics (tagged span)
  - Traces (the trace itself)
```

→ Click trace → see related logs + metrics in same UI.

### Spring Boot + Micrometer + Tracing

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

```yaml
management:
  tracing:
    sampling.probability: 1.0   # 100% in dev
  zipkin.tracing.endpoint: http://zipkin:9411/api/v2/spans
```

---

## Heap Analysis

When investigating memory leaks / OOM.

### Capture Heap Dump

```bash
# On OOM (auto)
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof -jar app.jar

# Manual
jcmd <pid> GC.heap_dump /tmp/dump.hprof
jmap -dump:format=b,file=/tmp/dump.hprof <pid>
```

### Analyze with Eclipse MAT

```bash
# Open dump.hprof in MAT
File → Open Heap Dump
```

#### Key Reports

- **Leak Suspects** — auto-detects suspicious patterns
- **Histogram** — top classes by retained size
- **Dominator Tree** — biggest memory consumers
- **Path to GC Roots** — why an object isn't collected

### Common Memory Leak Patterns

```
1. Static collections growing forever
   static List<Order> cache = new ArrayList<>();   // never cleared
   
2. Listeners not unregistered
   eventBus.subscribe(this);   // forgot to unsubscribe
   
3. ThreadLocal not cleaned
   threadLocal.set(big_object);
   // forgot threadLocal.remove();
   
4. Connection / Resource leaks
   forgot to close()
   
5. Inner class holding outer reference
   new Runnable() { ... };   // captures 'this' → outer leaks
```

---

## Common Pitfalls

### 1. Profiling Without Reproducing Issue

Profile in dev when prod has different load. Use real (or load-tested) workloads.

### 2. Profiler Overhead Skewing Results

JProfiler instrumented mode adds overhead → may hide / amplify real bottlenecks.

→ Use sampling profilers (async-profiler, JFR).

### 3. Safepoint Bias

Traditional profilers sample only at safepoints → certain methods over-represented.

→ async-profiler or `perf` more accurate.

### 4. Profiling Cold JVM

JIT not warmed → measuring interpreter, not steady-state.

→ Warm up before profiling.

### 5. Tiny Sample Size

10s recording on 10 RPS app = 100 samples. Profile longer + on bigger workload.

### 6. Optimizing Edge Cases

Flame graph shows method takes 0.5% — optimizing wastes time.

→ Focus on top 5-10 hottest paths.

### 7. APM Without Sampling

100% trace sampling = high cost. Use 1-10% in production.

### 8. Logs vs Metrics Confusion

| Use | For |
|-----|-----|
| Logs | Specific events, debugging |
| Metrics | Aggregates, alerting |
| Traces | Request flow, latency breakdown |

→ Don't query logs for "average latency"; that's a metric.

### 9. Over-Aggregation Hides Issues

Average latency = lies (long tail hidden). Always look at P50, P95, P99.

### 10. APM Cost Surprises

Datadog can hit $$$$ for trace + log volume. Sample wisely.

### 11. Heap Dump in Production

Capturing dump pauses JVM (seconds). Schedule maintenance window.

### 12. Forgetting JFR

Free, low overhead, built-in. Yet many teams don't use it.

### 13. Ignoring APM Auto-Detected Issues

APM tools surface obvious wins ("slow query, no index" / "too many DB calls"). Read recommendations.

### 14. No Continuous Profiling

Profiling = one-time activity? No. Continuous JFR or **Pyroscope** = always-on flame graphs.

---

## Summary Cheat Sheet

| Tool | Use For |
|------|---------|
| **async-profiler** | Flame graphs, low overhead |
| **JFR + JMC** | Built-in, production-safe profiling |
| **VisualVM** | Quick local inspection |
| **JProfiler / YourKit** | Enterprise polished |
| **Eclipse MAT** | Heap dump analysis |
| **Spring Boot Admin** | Multi-app dashboard |
| **Datadog / New Relic** | Production APM |
| **OpenTelemetry** | Standardized tracing |
| **Pyroscope** | Continuous profiling |

| Question | Tool |
|----------|------|
| "Where's CPU spent?" | async-profiler / JFR |
| "Memory leak?" | Heap dump + MAT |
| "Slow request?" | APM + tracing |
| "GC pauses?" | GC log + JFR |
| "Lock contention?" | async-profiler -e lock |
| "DB queries?" | APM / p6spy / Hibernate logs |
| "External call slow?" | APM tracing |

| Metrics — 3 Pillars | Tool |
|--------------------|------|
| Logs | ELK / Loki |
| Metrics | Prometheus |
| Traces | Jaeger / Tempo / OpenTelemetry |

| ✅ Do | ❌ Don't |
|-------|---------|
| Use async-profiler / JFR | Hand-roll timing code |
| Sample at <5% in production APM | 100% trace sampling |
| P50 + P95 + P99 latency | Average only |
| Continuous profiling | One-shot panic profile |
| Correlate logs + metrics + traces | Use one in isolation |
| Capture heap dump on OOM | Lose evidence |
| Read APM auto-recommendations | Ignore them |
| Profile production-like load | Tiny dev workload |

---

## Practice

1. Install async-profiler; profile a Spring Boot app for 30s; view flame graph.
2. Enable continuous JFR; capture 10-min recording; analyze in JMC.
3. Profile allocation pressure; identify top allocator.
4. Profile lock contention; find synchronized hotspot.
5. Set up Spring Boot Admin to monitor 2 apps.
6. Add Micrometer + Prometheus + Grafana for app metrics.
7. Add OpenTelemetry tracing across 2 microservices; visualize in Jaeger.
8. Trigger OOM intentionally; analyze heap dump in Eclipse MAT.
9. Add custom JFR event for business operation timing.
10. Try Pyroscope (continuous profiling) on local app.
11. Compare profile of warm vs cold JVM (warmup matters).
12. Set up alerts in your APM for P99 > SLA, error rate > 1%.
