# JVM Performance Tuning

## Status: Not Started

---

## Table of Contents

1. [JVM Tuning Kya Hai?](#jvm-tuning-kya-hai)
2. [JVM Memory Model Refresher](#jvm-memory-model-refresher)
3. [Heap Sizing Fundamentals](#heap-sizing-fundamentals)
4. [Container-Aware JVM (Critical!)](#container-aware-jvm-critical)
5. [Garbage Collectors Overview](#garbage-collectors-overview)
6. [Choosing the Right GC](#choosing-the-right-gc)
7. [G1GC Tuning](#g1gc-tuning)
8. [ZGC & Shenandoah (Low-Latency)](#zgc--shenandoah-low-latency)
9. [GC Logging](#gc-logging)
10. [Thread Stack Size](#thread-stack-size)
11. [Other Useful JVM Flags](#other-useful-jvm-flags)
12. [Diagnostic Tools](#diagnostic-tools)
13. [Common Pitfalls](#common-pitfalls)
14. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## JVM Tuning Kya Hai?

**Matlab:** JVM ke runtime parameters ko adjust karna so that your app **uses memory wisely**, **GC pause ka impact minimize ho**, aur **throughput / latency targets meet ho**.

> "Default JVM = good enough for most apps. But for production at scale, tuning shaves 20-50% latency and prevents OOMs."

### When to Tune?

✅ Long GC pauses (P99 latency spikes)
✅ OOM errors
✅ High heap usage / frequent GC
✅ Throughput plateaus despite hardware
✅ Container memory limits being killed (OOMKilled)

❌ "Just to be safe" without measurement
❌ Without baseline metrics
❌ Premature — first profile, then tune

---

## JVM Memory Model Refresher

```
┌─────────────────────────────────────────────┐
│              JVM Process Memory              │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │       Heap (-Xms / -Xmx)                │ │
│  │  ┌────────────┐  ┌──────────────────┐  │ │
│  │  │ Young Gen  │  │  Old Gen          │  │ │
│  │  │  - Eden    │  │  (long-lived)     │  │ │
│  │  │  - S0/S1   │  │                   │  │ │
│  │  └────────────┘  └──────────────────┘  │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │       Metaspace (class metadata)        │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │       Thread Stacks (-Xss per thread)   │ │
│  └────────────────────────────────────────┘ │
│                                              │
│  ┌────────────────────────────────────────┐ │
│  │       Code Cache (JIT-compiled)         │ │
│  │       Direct Buffers (NIO)              │ │
│  │       Native libraries                  │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

### Total JVM Memory ≠ Heap

```
Container limit: 1 GB
Heap (-Xmx): 768 MB
Metaspace: ~100 MB
Thread stacks: 200 threads × 1 MB = 200 MB
Code cache: ~48 MB
Direct buffers: variable

Total: ~1.1 GB → OOMKilled by Kubernetes!
```

→ Heap is **only one part** of JVM memory.

---

## Heap Sizing Fundamentals

### `-Xms` and `-Xmx`

```bash
java -Xms2g -Xmx2g MyApp
#     ↑       ↑
#     initial  maximum heap
```

### Best Practice — Set Equal

```bash
-Xms2g -Xmx2g
```

**Why?**
- JVM doesn't waste time growing heap incrementally
- Avoids fragmentation
- Predictable memory profile (good for containers)
- No surprise allocations during peak load

### Sizing Guidance

| App Type | Heap Suggestion |
|----------|-----------------|
| Small REST microservice | 256MB - 1GB |
| Standard Spring Boot app | 1-4 GB |
| Heavy batch / ETL | 4-16 GB |
| In-memory cache (Redis-like) | 16-64 GB |

⚠️ **Avoid heaps > 32 GB** (loses CompressedOops optimization).
⚠️ **Avoid heaps > 100 GB** (GC pauses get unmanageable).

### Old vs New Generation Ratio

```
-XX:NewRatio=2   → old:young = 2:1 (default for many GCs)
-XX:NewSize=512m -XX:MaxNewSize=512m
```

→ G1GC manages this automatically. Don't override unless profiling shows need.

---

## Container-Aware JVM (Critical!)

### The Old Problem (Pre-Java 8u131)

```
JVM sees host's 64 GB → defaults heap to 16 GB
Container limit: 1 GB
Result: OOMKilled randomly!
```

### Modern Solution

```bash
-XX:+UseContainerSupport            # ✅ default since Java 10
-XX:MaxRAMPercentage=75.0           # use 75% of container memory for heap
-XX:InitialRAMPercentage=75.0
-XX:MinRAMPercentage=50.0
```

### Recommended Container Setup

```dockerfile
FROM eclipse-temurin:21-jre

ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError \
  -XX:HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app.jar"]
```

### Kubernetes Example

```yaml
resources:
  requests:
    memory: "1Gi"
    cpu: "500m"
  limits:
    memory: "1.5Gi"
    cpu: "1000m"

env:
  - name: JAVA_OPTS
    value: "-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
```

→ With 1.5Gi limit + 75% = ~1.1 GB heap. Other 400 MB for non-heap (metaspace, threads, etc.).

### Why 75% (not 100%)?

```
Container memory = Heap + Metaspace + Thread stacks + Code cache + Native + Buffer

If heap = 100% → OOMKilled when other components allocate.
75% rule of thumb leaves room.
```

### CPU Awareness

```bash
-XX:ActiveProcessorCount=2   # rarely needed; JVM auto-detects in modern versions
```

→ JVM auto-detects container CPU limits in Java 10+.

---

## Garbage Collectors Overview

### Generations of GCs

| GC | Latency | Throughput | Heap Size | Default In |
|-----|---------|-----------|-----------|------------|
| **Serial** | ❌ Stop-the-world | OK | < 100 MB | Tiny apps |
| **Parallel** | ❌ Long pauses | ✅ Best | Any | Pre-Java 9 |
| **CMS** (deprecated) | OK | OK | < 4 GB | Java 6-13 |
| **G1GC** | ✅ Predictable | ✅ Good | 4 GB - 100 GB | Java 9+ default |
| **ZGC** | ✅✅ Sub-ms | OK | 8 GB - 16 TB | Latency-critical |
| **Shenandoah** | ✅✅ Sub-ms | OK | Any | Latency-critical |

### Visualizing GC Pauses

```
Parallel GC:    [PAUSE 500ms] ──────────── [PAUSE 800ms] ────
G1GC:           [pause 50ms] ── [pause 80ms] ── [pause 60ms] ──
ZGC:            [<1ms] [<1ms] [<1ms] ────────────── continuous concurrent
```

→ G1 = balance. ZGC/Shenandoah = ultra-low latency.

---

## Choosing the Right GC

### Decision Tree

```
Heap < 100 MB?
  → Serial GC

Throughput most important (batch jobs, ETL)?
  → Parallel GC

Latency-sensitive (web apps, APIs)?
  Heap < 32 GB?
    → G1GC (default, safe choice)
  Heap > 32 GB OR P99 must be <10ms?
    → ZGC or Shenandoah

Cloud-native containerized microservice?
  → G1GC (default, container-aware)
```

### Recommended Default

```bash
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200    # target pause time
```

→ G1GC = sane default for most Spring apps in 2024+.

---

## G1GC Tuning

### What is G1GC?

**Garbage-First** — divides heap into ~2048 regions; collects regions with most garbage first. Tries to meet pause-time target.

### Key Flags

```bash
-XX:+UseG1GC                          # enable
-XX:MaxGCPauseMillis=200              # target max pause (default 200ms)
-XX:G1HeapRegionSize=16m              # auto-sized; rarely override
-XX:InitiatingHeapOccupancyPercent=45 # trigger concurrent cycle at 45% old gen
-XX:G1NewSizePercent=20               # min young gen %
-XX:G1MaxNewSizePercent=40            # max young gen %
-XX:ParallelGCThreads=N               # parallel GC threads
-XX:ConcGCThreads=N                   # concurrent GC threads
```

### Common Tuning Scenarios

#### Scenario 1: Long Pauses (>500ms)

```bash
-XX:MaxGCPauseMillis=100              # tighter target
-XX:InitiatingHeapOccupancyPercent=35 # trigger GC earlier
```

#### Scenario 2: GC Too Frequent

```bash
-Xmx4g                                 # bigger heap
-XX:G1MaxNewSizePercent=60            # more young gen → fewer old GCs
```

#### Scenario 3: Humongous Object Issues

```bash
-XX:G1HeapRegionSize=32m              # bigger regions for large objects
```

### Production Defaults That Just Work

```bash
JAVA_OPTS="-Xms2g -Xmx2g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+ParallelRefProcEnabled \
  -XX:+ExitOnOutOfMemoryError \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof"
```

---

## ZGC & Shenandoah (Low-Latency)

### When to Use?

✅ P99 latency must be **<10ms**
✅ Heap **>32 GB** (G1 struggles)
✅ User-facing real-time apps
✅ Trading systems, gaming

### ZGC

```bash
-XX:+UseZGC                # production-ready since Java 15
-Xmx16g
```

✅ Pauses < 1ms regardless of heap size
✅ Scales to 16 TB heap
❌ Slightly more CPU overhead vs G1

### Generational ZGC (Java 21+)

```bash
-XX:+UseZGC -XX:+ZGenerational
```

→ Better throughput by separating young/old gen.

### Shenandoah (Red Hat OpenJDK)

```bash
-XX:+UseShenandoahGC
```

→ Similar properties to ZGC; competing implementation.

### Trade-offs

```
ZGC/Shenandoah:
+ Sub-ms pauses
+ Scales to huge heaps
- Slightly higher CPU cost (~5-15%)
- More memory overhead
- Less mature than G1
```

---

## GC Logging

### Java 9+ Unified Logging

```bash
-Xlog:gc*:file=/var/log/gc.log:time,uptime,level,tags:filecount=10,filesize=10M
```

→ Logs all GC events with timestamps; rotates 10 files of 10MB each.

### What to Capture

```bash
-Xlog:gc,gc+heap=info:file=gc.log
-Xlog:safepoint:file=safepoint.log    # for analyzing safepoint stalls
```

### Old Style (Pre-Java 9)

```bash
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Xloggc:gc.log
-XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=10M
```

### Analyzing GC Logs

#### Tools
- **GCViewer** (open-source desktop)
- **GCEasy** (web upload — free tier)
- **Eclipse Mat** (heap dump analysis)
- **JITWatch** (JIT compilation analysis)

### Sample GC Log Snippet

```
[15.234s][info][gc] GC(42) Pause Young (G1 Evacuation Pause) 1024M->512M(2048M) 45.123ms
```

```
GC(42)              ← GC event number
Pause Young         ← young gen collection
G1 Evacuation Pause ← G1's mode
1024M → 512M(2048M) ← heap before/after/total
45.123ms            ← pause duration
```

### Key Metrics from GC Logs

| Metric | Healthy | Concerning |
|--------|---------|-----------|
| Young pause time | < 100ms | > 200ms |
| Old/Mixed pause time | < 200ms | > 500ms |
| GC overhead % of CPU | < 5% | > 10% |
| Throughput (app time / total) | > 95% | < 90% |
| Promotion failures | 0 | > 0 = bigger heap needed |
| Concurrent mode failures | 0 | > 0 = tune IHOP earlier |

---

## Thread Stack Size

### `-Xss`

Default ~1 MB per thread. Each thread allocates this much.

```bash
-Xss256k        # smaller stack — more threads possible
-Xss2m          # bigger stack — for deep recursion
```

### Math

```
1000 threads × 1 MB = 1 GB just for stacks!
```

→ For thread-heavy apps, lower `-Xss` to fit more threads.

### When to Lower?

```bash
-Xss512k        # often safe for typical web apps
```

### When to Raise?

- Deep recursion (parsers, recursive algos)
- StackOverflowError thrown

### Virtual Threads (Java 21+)

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

→ Virtual threads use **few KBs** each, not MBs. Game-changer for thread-per-request apps.

→ Cross-ref: `Microservices Architecture` for reactive vs virtual threads discussion.

---

## Other Useful JVM Flags

### Debugging / Crash Analysis

```bash
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/tmp/heapdump.hprof
-XX:+PrintFlagsFinal             # shows all JVM flags + values
-XX:+UnlockDiagnosticVMOptions
-XX:+ExitOnOutOfMemoryError      # crash hard rather than degraded state
```

### JIT Compilation

```bash
-XX:+TieredCompilation              # default — uses C1 + C2
-XX:ReservedCodeCacheSize=256m      # for code-heavy apps
-XX:+PrintCompilation               # log JIT compilations
```

### String Optimization

```bash
-XX:+UseStringDeduplication         # dedupe duplicate strings (G1 only)
```

### Compressed References

```bash
# Default ON for heaps < 32 GB — saves 50% of object header memory
-XX:+UseCompressedOops
-XX:+UseCompressedClassPointers
```

⚠️ Disabled automatically for heaps > 32 GB — that's why 32 GB threshold matters.

### NUMA Awareness (Multi-Socket Servers)

```bash
-XX:+UseNUMA
```

### Direct Memory

```bash
-XX:MaxDirectMemorySize=512m        # for NIO-heavy apps (Netty, Reactor)
```

### Spring Boot Application

```bash
JAVA_OPTS="-Xms1g -Xmx1g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
  -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -Xlog:gc*:file=/var/log/gc.log:time:filecount=5,filesize=10M"
```

---

## Diagnostic Tools

### Built-In JDK Tools

```bash
jps                                  # list Java processes
jstat -gcutil <pid> 1s               # GC stats every 1s
jstack <pid>                         # thread dump
jmap -dump:format=b,file=heap.hprof <pid>  # heap dump
jcmd <pid> GC.heap_info              # heap details
jcmd <pid> Thread.print              # thread dump (better than jstack)
jcmd <pid> VM.flags                  # all flags
```

### `jcmd` Cheatsheet

```bash
jcmd <pid> help                      # list available commands
jcmd <pid> GC.run                    # force GC
jcmd <pid> JFR.start duration=60s filename=recording.jfr
jcmd <pid> Thread.dump_to_file -format=json /tmp/threads.json
```

### JFR (Java Flight Recorder)

Built-in low-overhead profiler.

```bash
java -XX:StartFlightRecording=duration=60s,filename=app.jfr MyApp

jcmd <pid> JFR.start name=myrec settings=profile filename=/tmp/app.jfr duration=120s
jcmd <pid> JFR.stop name=myrec
```

→ Open `.jfr` file in **JDK Mission Control** (free).

### VisualVM

GUI tool — heap browse, CPU profiling, GC visualization.

```bash
visualvm
```

→ Connect to local or remote JMX.

### Async-Profiler

Low-overhead flame graphs.

→ Cross-ref: `05-Profiling-and-APM.md`.

---

## Common Pitfalls

### 1. Using Default JVM Settings in Production

Default heap = 1/4 of host memory. In container = often wrong (without `+UseContainerSupport`).

### 2. Heap > 32 GB

Loses CompressedOops → object headers double in size → memory waste.

### 3. `-Xmx` ≠ `-Xms`

JVM grows heap incrementally → GC triggers more often during ramp-up. Set equal.

### 4. Forgetting Non-Heap Memory

```
1Gi container, -Xmx1g
Result: OOMKilled (no room for metaspace + threads + native).
```

→ Use `-XX:MaxRAMPercentage=75.0`.

### 5. Switching GC Without Measuring

"ZGC sounds cool" → introduce 10-15% CPU overhead for app that doesn't need <10ms latency.

### 6. Disabling `+UseContainerSupport`

Sometimes done by accident; JVM ignores container limits.

### 7. Ignoring GC Logs

GC logs reveal everything. Enable from day one in production.

### 8. Tuning Without Baseline

```
"Latency improved!" — compared to what?
```

→ Always baseline first.

### 9. Tuning One Knob at a Time? No, Together

Some flags interact. Test changes in groups + measure.

### 10. Forgetting JIT Warmup

First few seconds: interpreter mode → slow. Apps need warmup before benchmarking.

```bash
-XX:CompileThreshold=10000          # default; tune for short-lived JVMs
```

### 11. Too Many Threads

200+ OS threads → context switching overhead + memory waste.

→ Use thread pools / virtual threads (Java 21+).

### 12. Ignoring `-XX:+ExitOnOutOfMemoryError`

App in degraded state after OOM > app dies fast for K8s to restart.

### 13. Heap Dump Path Not Writable

```bash
-XX:HeapDumpPath=/tmp/heapdump.hprof
# but /tmp not writable in container → no dump on OOM
```

→ Test heap dump path; ensure persistent volume in K8s.

### 14. CompressedOops Off Without Reason

```bash
-XX:-UseCompressedOops              # ❌ rarely needed
```

→ Default ON for heaps < 32 GB; leave it.

---

## Summary Cheat Sheet

| Flag | Purpose | Common Value |
|------|---------|--------------|
| `-Xms` / `-Xmx` | Initial / max heap | 2g / 2g (equal) |
| `-XX:+UseG1GC` | Use G1 | Recommended default |
| `-XX:MaxGCPauseMillis` | Target pause | 200 (default) |
| `-XX:+UseContainerSupport` | Honor container limits | Default Java 10+ |
| `-XX:MaxRAMPercentage` | % of container for heap | 75.0 |
| `-XX:+ExitOnOutOfMemoryError` | Die fast on OOM | Production must |
| `-XX:+HeapDumpOnOutOfMemoryError` | Capture for analysis | Always |
| `-Xss` | Per-thread stack | 512k - 1m |
| `-Xlog:gc*:file=...` | GC logging | Always |

| GC | Use For |
|----|---------|
| Parallel | Throughput-only batch |
| **G1GC** | Most apps (default) |
| ZGC / Shenandoah | <10ms P99, big heaps |

| Container Setup | Value |
|-----------------|-------|
| K8s memory limit | 1.5 GB |
| `-XX:MaxRAMPercentage` | 75 (= ~1.1 GB heap) |
| Reserved for non-heap | ~400 MB |

| ✅ Do | ❌ Don't |
|-------|---------|
| `-Xms = -Xmx` | Different values |
| `-XX:+UseContainerSupport` | Forget container limits |
| Enable GC logging | Fly blind |
| Capture heap dump on OOM | Lose evidence |
| G1GC default for Spring Boot | ZGC for low-load apps |
| Profile before tuning | Random flag tweaks |
| Heap < 32 GB | 64 GB monolith |
| Test in production-like env | Local-only validation |

---

## Practice

1. Run a Spring Boot app with default JVM settings; measure GC pauses.
2. Add G1 + container-aware flags; compare metrics.
3. Trigger OOM intentionally; verify heap dump generated.
4. Enable GC logging; analyze with GCEasy.
5. Profile with JFR for 60s; analyze in JDK Mission Control.
6. Compare heap with `-Xmx32g` vs `-Xmx33g` (CompressedOops difference).
7. Test container with 1Gi limit + `-Xmx1g` (without %); observe OOMKilled.
8. Switch to ZGC; measure CPU overhead vs G1.
9. Tune `MaxGCPauseMillis` from 200 → 50; observe throughput trade-off.
10. Use `jcmd` to capture thread dump + flags + GC info from running app.
11. For container app: try `MaxRAMPercentage=50` vs `75` vs `90`; observe behavior.
12. Try Java 21 virtual threads; compare memory vs platform threads.
