# Multithreading Questions

## Status: Not Started

---

## Table of Contents

1. [Threads — Quick Refresher](#threads--quick-refresher)
2. [`volatile` vs `synchronized`](#volatile-vs-synchronized)
3. [Race Conditions](#race-conditions)
4. [Deadlock](#deadlock)
5. [Livelock vs Starvation](#livelock-vs-starvation)
6. [Thread-Safe Singleton](#thread-safe-singleton)
7. [Producer-Consumer](#producer-consumer)
8. [Thread Pool Sizing](#thread-pool-sizing)
9. [`ExecutorService` Essentials](#executorservice-essentials)
10. [`CompletableFuture` Quick Hits](#completablefuture-quick-hits)
11. [`ThreadLocal`](#threadlocal)
12. [Atomics & CAS](#atomics--cas)
13. [Virtual Threads (Java 21+)](#virtual-threads-java-21)
14. [Common Output Traps](#common-output-traps)
15. [Pitfalls](#pitfalls)
16. [Cheat Sheet](#cheat-sheet)

---

## Threads — Quick Refresher

```java
Runnable r = () -> System.out.println("hi");
Thread t = new Thread(r);
t.start();              // not run() — start spawns new thread
t.join();
```

### Lifecycle

```
NEW → RUNNABLE → BLOCKED / WAITING / TIMED_WAITING → TERMINATED
```

---

## `volatile` vs `synchronized`

### `volatile`

- **Visibility** guarantee — write thread se write, doosre threads ko **immediately visible** (no caching in registers / per-thread cache).
- **Atomic for single read/write** of variable (long/double bhi atomic post-Java 5 on volatile).
- **No mutual exclusion** — compound ops (e.g., `count++`) still racey.

```java
private volatile boolean running = true;
public void stop() { running = false; }
public void loop() { while (running) { ... } }
```

### `synchronized`

- **Mutual exclusion** + **visibility** + **happens-before**.
- Performance cost (uncontended cheap, contended expensive).
- Reentrant.

```java
public synchronized void inc() { count++; }       // method-level
public void inc() { synchronized (this) { count++; } }
public void inc() { synchronized (lock) { count++; } }   // dedicated lock
```

### Comparison

| | volatile | synchronized |
|--|----------|--------------|
| Atomic compound op | ❌ | ✅ |
| Visibility | ✅ | ✅ |
| Mutual exclusion | ❌ | ✅ |
| Cost | Lowest | Higher |
| Use | Flags, single var | Critical sections |

---

## Race Conditions

```java
class Counter {
    int v;
    public void inc() { v++; }   // read-modify-write — NOT atomic
}
```

→ Two threads `inc()` simultaneously → final value off.

### Fix options

1. `synchronized`
2. `AtomicInteger`
3. `LongAdder` (high-contention)
4. Lock (`ReentrantLock`)

---

## Deadlock

### 4 conditions (Coffman)

1. **Mutual exclusion** — locks not shareable
2. **Hold and wait** — hold one, ask another
3. **No preemption** — locks released voluntarily
4. **Circular wait** — A waits B, B waits A

### Classic example

```java
synchronized (lock1) {
    synchronized (lock2) { ... }
}
// other thread:
synchronized (lock2) {
    synchronized (lock1) { ... }
}
```

### Prevention

| Strategy | Note |
|----------|------|
| **Lock ordering** | Always acquire in same order |
| **Try-lock with timeout** | `ReentrantLock.tryLock(...)` |
| **Single coarse lock** | Simpler if perf OK |
| **Avoid nested locks** | Refactor critical section |

```java
if (lock1.tryLock(1, SECONDS)) {
    try {
        if (lock2.tryLock(1, SECONDS)) {
            try { ... } finally { lock2.unlock(); }
        }
    } finally { lock1.unlock(); }
}
```

### Detection

- `jstack <pid>` — JVM detects + reports "Found one Java-level deadlock".

---

## Livelock vs Starvation

| Term | Meaning |
|------|---------|
| **Deadlock** | Threads stuck waiting (zero progress) |
| **Livelock** | Threads keep changing state but no progress (e.g., both yield to each other) |
| **Starvation** | Some threads never get CPU / lock (e.g., low-priority) |

### Livelock example

Two threads keep retrying with backoff but always collide.

### Starvation example

`synchronized` is unfair → some thread always gets lock first.

→ `ReentrantLock(true)` for fair lock (slower but starvation-resistant).

---

## Thread-Safe Singleton

### Double-Checked Locking (DCL)

```java
class Config {
    private static volatile Config instance;
    public static Config getInstance() {
        if (instance == null) {                       // 1st check
            synchronized (Config.class) {
                if (instance == null) {                // 2nd check
                    instance = new Config();
                }
            }
        }
        return instance;
    }
}
```

⚠️ `volatile` mandatory — prevents reordering exposing partially constructed object.

### Bill Pugh / Holder

```java
class Config {
    private static class Holder { static final Config I = new Config(); }
    public static Config getInstance() { return Holder.I; }
}
```

→ JVM class loading guarantees thread-safe lazy init. Cleanest IMO.

### Enum

```java
public enum Config { INSTANCE; ... }
```

→ Reflection-safe + serialization-safe.

→ Cross-ref: `phase-4 / Design Patterns in Java/01-Creational-Patterns.md`.

---

## Producer-Consumer

### `BlockingQueue`

```java
BlockingQueue<Job> q = new ArrayBlockingQueue<>(100);

// Producer
q.put(new Job(...));    // blocks if full

// Consumer
Job j = q.take();        // blocks if empty
```

### Variants

| Queue | Note |
|-------|------|
| `ArrayBlockingQueue` | Bounded, fair option |
| `LinkedBlockingQueue` | Optionally bounded |
| `PriorityBlockingQueue` | Priority order |
| `SynchronousQueue` | Direct handoff |
| `LinkedTransferQueue` | Hybrid; advanced |

### Old way (`wait` / `notify`)

```java
synchronized (queue) {
    while (queue.size() == capacity) queue.wait();
    queue.add(item);
    queue.notifyAll();
}
```

→ Modern code: BlockingQueue use karo, `wait/notify` avoid.

---

## Thread Pool Sizing

### CPU-bound

```
threads ≈ NUM_CPU + 1
```

### IO-bound

```
threads ≈ NUM_CPU × (1 + WAIT_TIME / COMPUTE_TIME)
```

### Brian Goetz formula (Java Concurrency in Practice)

```
threads = N_CPU × U_CPU × (1 + W/C)
  N_CPU  = number of CPUs
  U_CPU  = target CPU utilization (0..1)
  W/C    = wait time / compute time ratio
```

### Practical defaults

| Workload | Threads |
|----------|---------|
| CPU heavy (compression, math) | cores or cores+1 |
| Mixed | 2 × cores |
| IO heavy (HTTP/DB calls) | 50-200, sometimes hundreds (or virtual threads) |

### `ExecutorService` factories

```java
Executors.newFixedThreadPool(n);
Executors.newCachedThreadPool();        // unbounded — careful
Executors.newSingleThreadExecutor();
Executors.newScheduledThreadPool(n);
Executors.newVirtualThreadPerTaskExecutor();   // Java 21+
```

⚠️ `newCachedThreadPool` unbounded — production load se exhaust ho sakta. Custom `ThreadPoolExecutor` with `LinkedBlockingQueue` capacity recommended.

---

## `ExecutorService` Essentials

### Submit + Future

```java
ExecutorService es = Executors.newFixedThreadPool(4);
Future<Integer> f = es.submit(() -> compute());
int v = f.get(5, TimeUnit.SECONDS);
es.shutdown();
es.awaitTermination(10, TimeUnit.SECONDS);
```

### `shutdown()` vs `shutdownNow()`

| | shutdown | shutdownNow |
|--|----------|-------------|
| New tasks | Rejected | Rejected |
| Running tasks | Allowed to finish | Interrupted |
| Queue | Drains | Returned (List) |

---

## `CompletableFuture` Quick Hits

```java
CompletableFuture.supplyAsync(() -> fetchUser(id))
    .thenApply(this::enrich)
    .thenCompose(u -> CompletableFuture.supplyAsync(() -> loadOrders(u)))
    .thenCombine(otherFuture, (a, b) -> merge(a, b))
    .exceptionally(ex -> fallback())
    .thenAccept(result -> log.info(...));
```

### Where executes?

Default `ForkJoinPool.commonPool()` — like parallel streams. **Custom executor pass karo** for IO:

```java
.thenApplyAsync(this::enrich, customExecutor);
```

### `allOf` / `anyOf`

```java
CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
all.join();
```

---

## `ThreadLocal`

Per-thread storage:

```java
private static final ThreadLocal<DateFormat> FMT =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

String s = FMT.get().format(new Date());
```

### Pitfall — Pool Reuse

Thread pool me thread reuse hota — `set` ki value next request ko leak. **Always `remove()` after use**:

```java
try { ... } finally { FMT.remove(); }
```

→ Spring `RequestContextHolder` and `MDC` use ThreadLocal — Spring framework cleanup karta hai per request.

### Modern alternative

`ScopedValue` (Java 21+ preview / standard recently) — safer immutable per-thread scoped values.

---

## Atomics & CAS

```java
AtomicInteger ctr = new AtomicInteger(0);
ctr.incrementAndGet();        // CAS-based, lock-free
ctr.compareAndSet(5, 10);
```

| Class | Use |
|-------|-----|
| `AtomicInteger`, `AtomicLong` | Counters |
| `AtomicReference<T>` | Object swap |
| `AtomicReferenceArray` | Array slot |
| `LongAdder` / `LongAccumulator` | High-contention counters (better than AtomicLong) |

→ Internally: CAS (Compare-And-Swap) on hardware.

---

## Virtual Threads (Java 21+)

```java
try (var es = Executors.newVirtualThreadPerTaskExecutor()) {
    es.submit(() -> handle(req));
}
```

- Lightweight (KB instead of MB stack)
- Block freely on IO — JVM auto-mounts/unmounts on carrier threads
- Game-changer for thread-per-request servers

⚠️ Don't pin: `synchronized` on long sections may pin virtual to carrier (Java 21 issue, improving).

---

## Common Output Traps

### Q1. start vs run

```java
Thread t = new Thread(() -> System.out.println(Thread.currentThread()));
t.run();    // runs in CALLER thread
t.start();  // runs in NEW thread
```

### Q2. volatile not enough

```java
volatile int v;
v++;        // race — read+write not atomic
```

→ Use `AtomicInteger` or sync.

### Q3. wait without sync

```java
Object o = new Object();
o.wait();   // IllegalMonitorStateException
```

→ Must hold monitor: `synchronized (o) { o.wait(); }`

### Q4. shutdownNow effect

Tasks should respect `Thread.interrupted()` to actually stop; pure `while(true)` ignores interrupt.

---

## Pitfalls

1. **`HashMap` shared across threads** — corrupted; use `ConcurrentHashMap`.
2. **DCL without volatile** — partially-constructed object visible.
3. **`SimpleDateFormat`** is NOT thread-safe — use `DateTimeFormatter` (immutable).
4. **Caching `Boolean` flag without volatile** — visibility bug.
5. **Calling `run()` instead of `start()`** — no new thread.
6. **`Thread.sleep` in synchronized block** — holds lock while sleeping.
7. **Catching + ignoring `InterruptedException`** — restore: `Thread.currentThread().interrupt()`.
8. **Unbounded `newCachedThreadPool`** — DoS your own JVM.
9. **`ThreadLocal` leak** in pools — always `remove()`.
10. **Optimistic CAS hot loop** — high contention; consider `LongAdder`.
11. **`stop()` / `suspend()`** — deprecated; use cooperative interruption.

---

## Cheat Sheet

| Concept | One-liner |
|---------|-----------|
| volatile | Visibility, no atomicity for compound |
| synchronized | Mutex + visibility |
| AtomicInteger | Lock-free counter |
| LongAdder | High-contention counter |
| ReentrantLock | Explicit lock + tryLock + fairness |
| BlockingQueue | Producer-consumer |
| CompletableFuture | Async pipeline |
| ThreadLocal | Per-thread; remove() after use |
| Virtual thread | Cheap, blocking-friendly |

| Pool sizing | Approx |
|-------------|--------|
| CPU | cores+1 |
| Mixed | 2×cores |
| IO heavy | many — or virtual threads |

| Singleton | Best |
|-----------|------|
| DCL with volatile | Yes |
| Bill Pugh holder | Yes |
| Enum | Yes (also serialize-safe) |

---

## Practice

1. Counter increment with 4 threads — show race; fix with synchronized + Atomic.
2. Deadlock demo — `jstack` se detect.
3. Producer-consumer with `ArrayBlockingQueue`.
4. CompletableFuture pipeline (`supplyAsync → thenCompose → exceptionally`).
5. ThreadLocal leak in pool — observe; add `remove()`.
6. Compare AtomicLong vs LongAdder under high-contention.
7. Virtual threads run 100k tasks; compare memory vs platform threads.
