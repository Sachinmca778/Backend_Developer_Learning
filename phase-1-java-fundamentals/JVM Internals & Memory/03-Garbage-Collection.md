# Garbage Collection

## Status: Not Started

---

## Table of Contents

1. [GC Roots](#gc-roots)
2. [Mark-and-Sweep Algorithm](#mark-and-sweep-algorithm)
3. [Generational GC](#generational-gc)
4. [Minor GC vs Major/Full GC](#minor-gc-vs-majorfull-gc)
5. [GC Algorithms](#gc-algorithms)
6. [GC Tuning Flags](#gc-tuning-flags)

---

## GC Roots

**Matlab:** Objects jo garbage collect nahi ho sakte — inhi se reachability check start hoti hai.

### What are GC Roots?

```
GC Roots → Live Objects → Dead Objects (collected)

Agar koi object GC root se reachable nahi hai → Garbage
```

### Types of GC Roots

```java
// 1. Local variables (stack pe)
public void method() {
    String local = new String("Hello");  // Stack reference → GC root
}

// 2. Active threads
Thread thread = new Thread(() -> {
    // Running thread → GC root
});
thread.start();

// 3. Static variables
class Config {
    static Map<String, String> settings = new HashMap<>();  // Static → GC root
}

// 4. JNI references
// Native code se Java objects ko reference kar raha hai

// 5. JVM internal references
// System class loader, exception objects, etc.
```

### Reachability

```java
String a = new String("Hello");  // a → GC root
String b = a;                     // b → reachable from a
String c = b;                     // c → reachable from b

a = null;                         // a ab GC root nahi
b = null;                         // b ab GC root nahi
// c abhi bhi reachable hai → c GC root se reachable nahi → garbage

c = null;                         // Ab "Hello" object unreachable → garbage
```

### Weak References

```java
// Strong reference (default)
String strong = new String("Hello");  // GC collect nahi karega

// Weak reference
WeakReference<String> weak = new WeakReference<>(new String("Hello"));
// GC collect kar sakta hai agar koi strong reference nahi hai

// Soft reference
SoftReference<String> soft = new SoftReference<>(new String("Hello"));
// GC collect karega only if memory low hai

// Phantom reference
PhantomReference<String> phantom = new PhantomReference<>(obj, queue);
// Object finalize hone ke baad queue mein aata hai
```

---

## Mark-and-Sweep Algorithm

**Matlab:** GC ka basic algorithm — pehle live objects mark karo, phir sweep (collect) karo.

### Phase 1: Mark

```java
// GC roots se start karo
// Reachable objects ko "mark" karo

Root: local variable → Object A (marked)
  → Object B (marked)
  → Object C (marked)

Object D → Unreachable (not marked) → Garbage
Object E → Unreachable (not marked) → Garbage
```

### Phase 2: Sweep

```java
// Unmarked objects ko collect karo
// Memory reclaim karo

Heap before sweep:
[A✓][B✓][C✓][D✗][E✗][F✓]

Heap after sweep:
[A✓][B✓][C✓][free][free][F✓]
```

### Problem: Fragmentation

```
Sweep ke baad memory fragmented ho jaati hai:
[Object][free][Object][free][free][Object]

Large object allocate karna mushkil ho jaata hai
```

### Solution: Mark-Sweep-Compact

```java
// Phase 3: Compact
// Live objects ko ek jagah shift karo

Before compact:
[A][free][B][free][free][C]

After compact:
[A][B][C][free][free][free]
```

---

## Generational GC

**Matlab:** Objects ko age ke basis pe alag-alag generations mein divide karo — different GC strategies.

### Generational Hypothesis

```
1. Most objects die young (short-lived)
2. Few objects survive long (long-lived)
3. Objects of same generation tend to have similar lifetime
```

### Generation Structure

```
┌─────────────────────────────────────────────────────────────┐
│                         Heap Memory                         │
├─────────────────────────────────────────────────────────────┤
│  Young Generation (1/3)           │  Old Generation (2/3)   │
│  ┌──────────┬──────────┬──────────┤                        │
│  │ Eden     │ S0       │ S1       │  Tenured              │
│  │ 80%      │ 10%      │ 10%      │                        │
│  └──────────┴──────────┴──────────┤                        │
└─────────────────────────────────────────────────────────────┘
```

### Why Generational?

```
Young Gen GC (Minor GC):
- Fast — sirf small area scan karna hai
- Frequent — bahut objects die young
- Copying collector — efficient for short-lived

Old Gen GC (Major/Full GC):
- Slow — large area scan karna hai
- Infrequent — few objects survive long
- Mark-compact — efficient for long-lived
```

---

## Minor GC vs Major/Full GC

### Minor GC (Young Generation)

```
Trigger: Eden space full

Process:
1. Mark live objects in Eden + active Survivor
2. Copy live objects to other Survivor Space
3. Increment age
4. Clear Eden and old Survivor
5. Swap Survivor roles

Duration: Fast (1-10ms)
Impact: Brief stop-the-world
Frequency: High (every few seconds)
```

### Major GC (Old Generation)

```
Trigger: Old Generation full

Process:
1. Mark all live objects in Old Gen
2. Compact live objects
3. Reclaim dead object space

Duration: Slow (100ms - several seconds)
Impact: Significant stop-the-world
Frequency: Low (minutes/hours apart)
```

### Full GC (Entire Heap)

```
Trigger:
- Old Generation full
- System.gc() call
- Metaspace full
- Allocation failure in Old Gen

Process:
1. Minor GC (Young Gen)
2. Major GC (Old Gen)
3. Metaspace cleanup (if needed)
4. Compaction

Duration: Very slow (seconds to minutes)
Impact: Major stop-the-world — application freeze
```

### GC Tuning Goal

```
✅ Maximize throughput — application time / total time
✅ Minimize pause times — user experience
✅ Minimize footprint — memory usage

Trade-off:
- Low pause times → More frequent GC → Lower throughput
- High throughput → Less frequent GC → Longer pauses
```

---

## GC Algorithms

### Serial GC

```bash
# Single-threaded GC
java -XX:+UseSerialGC MyApp

# Use Case:
# - Single-core systems
# - Small applications (< 200MB heap)
# - Simple batch jobs

# Pros: Simple, low overhead
# Cons: Stop-the-world pauses, not scalable
```

### Parallel GC (Throughput GC)

```bash
# Multi-threaded GC
java -XX:+UseParallelGC MyApp

# Use Case:
# - Batch processing
# - Throughput important, pauses acceptable
# - Multi-core systems

# Pros: Better throughput than Serial
# Cons: Longer pause times

# Tuning:
# -XX:GCTimeRatio=N  # Throughput target (1/(1+N))
# -XX:MaxGCPauseMillis=N  # Target max pause
```

### G1 GC (Garbage First) — Default Java 9+

```bash
# Default since Java 9
java -XX:+UseG1GC MyApp  # Default

# Use Case:
# - Large heaps (> 4GB)
# - Predictable pause times
# - Multi-core systems

# How G1 Works:
# 1. Heap ko equal-sized regions mein divide karta hai
# 2. Har region ko collect karne ka "cost" estimate karta hai
# 3. Sabse zyada garbage wale regions pehle collect karta hai (Garbage First)
# 4. Pause time target maintain karta hai

# Tuning:
# -XX:MaxGCPauseMillis=200  # Target pause time (default 200ms)
# -XX:G1HeapRegionSize=N    # Region size (1MB-32MB)
# -XX:InitiatingHeapOccupancyPercent=45  # Trigger GC at 45% heap usage
```

### G1 Heap Structure

```
┌─────────────────────────────────────────────────────────────┐
│                        G1 Heap                             │
├─────────────────────────────────────────────────────────────┤
│ Region │ Region │ Region │ Region │ Region │ Region │ ...   │
│   E    │   E    │   S    │   O    │   E    │   H    │       │
│   E    │   S    │   O    │   O    │   S    │        │       │
│   S    │   O    │   O    │   H    │        │        │       │
└─────────────────────────────────────────────────────────────┘

E = Eden, S = Survivor, O = Old, H = Humongous (large objects)
```

### ZGC (Z Garbage Collector) — Java 15+

```bash
# Ultra-low pause times
java -XX:+UseZGC MyApp

# Use Case:
# - Ultra-low pause times (< 1ms)
# - Very large heaps (up to 16TB)
# - Throughput acceptable trade-off

# Features:
# - Colored pointers
# - Load barriers
# - Concurrent marking, relocation

# Pause times: < 1ms (regardless of heap size)
# Throughput: 10-15% lower than G1
```

### Shenandoah GC — Java 15+

```bash
# Ultra-low pause times
java -XX:+UseShenandoahGC MyApp

# Use Case:
# - Similar to ZGC
# - Red Hat developed
# - Pause times independent of heap size

# Features:
# - Concurrent compaction
# - Brooks pointers
# - Load barriers

# Pause times: < 10ms
# Throughput: Similar to ZGC
```

### GC Algorithm Comparison

| Algorithm | Pause Time | Throughput | Heap Size | Java Version |
|-----------|-----------|------------|-----------|-------------|
| **Serial** | High | Low | < 200MB | All |
| **Parallel** | Medium-High | High | Any | All |
| **G1** | Medium | Medium-High | > 4GB | Java 7+ |
| **ZGC** | < 1ms | Medium | Up to 16TB | Java 15+ |
| **Shenandoah** | < 10ms | Medium | Large | Java 15+ |

---

## GC Tuning Flags

### Essential Flags

```bash
# GC algorithm select karo
-XX:+UseSerialGC        # Serial
-XX:+UseParallelGC      # Parallel (default in Java 8)
-XX:+UseG1GC            # G1 (default in Java 9+)
-XX:+UseZGC             # ZGC
-XX:+UseShenandoahGC    # Shenandoah

# Heap size
-Xms2g                  # Initial heap
-Xmx2g                  # Max heap (Xms = Xmx recommended)

# GC logging (Java 9+)
-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=5,filesize=10M

# GC logging (Java 8)
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:gc.log
```

### G1 Specific Flags

```bash
# Target pause time
-XX:MaxGCPauseMillis=200

# Heap region size
-XX:G1HeapRegionSize=4M

# Old gen occupancy threshold
-XX:InitiatingHeapOccupancyPercent=45

# Humongous object threshold
-XX:G1HeapWastePercent=5
```

### GC Logging

```bash
# Java 9+ unified logging
-Xlog:gc*:file=gc.log:time,uptime,level,tags:filecount=5,filesize=10M

# Log levels: off, error, warning, info, debug, trace
# Tags: gc, heap, ergo, init, task, stats, ref, age, freelist, cpu, survivor, tenuring, promotion

# Parse GC logs
# Tools: GCViewer, GCEasy, JFR (Java Flight Recorder)
```

### Monitoring GC

```bash
# JConsole — visual monitoring
jconsole

# VisualVM — detailed profiling
jvisualvm

# jstat — GC statistics
jstat -gcutil <pid> 1000  # Every 1 second

# jcmd — GC diagnostic
jcmd <pid> GC.run          # Force GC
jcmd <pid> GC.heap_info    # Heap info
jcmd <pid> GC.class_histogram  # Class histogram

# JFR (Java Flight Recorder)
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr MyApp
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **GC Roots** | Stack locals, active threads, static variables, JNI refs |
| **Mark-and-Sweep** | Mark live objects → sweep dead → compact |
| **Generational GC** | Young (Eden + Survivor) + Old Generation |
| **Minor GC** | Young Gen collection — fast, frequent |
| **Major/Full GC** | Old Gen + entire heap — slow, infrequent |
| **Serial GC** | Single-threaded, small apps |
| **Parallel GC** | Multi-threaded, throughput focused |
| **G1 GC** | Default Java 9+, predictable pauses |
| **ZGC/Shenandoah** | Ultra-low pauses (< 1ms), large heaps |
| **GC Tuning** | Xms=Xmx, MaxGCPauseMillis, GC logging |
