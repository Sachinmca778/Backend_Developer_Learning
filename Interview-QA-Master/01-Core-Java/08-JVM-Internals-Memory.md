# JVM Internals & Memory - Top 10 Interview Questions & Answers

> Covering: JVM Architecture, ClassLoader Hierarchy, Memory Areas (Heap, Stack, Metaspace, PC, Native), Young/Old Generation, GC Algorithms (Serial, Parallel, G1, ZGC), STW, JMM Happens-Before, volatile/synchronized Memory Effects, OutOfMemoryError Types, Escape Analysis

---

## Q1: JVM Architecture explain karo — main components kya hain?

**Answer:**

**JVM (Java Virtual Machine)** Java bytecode ko run karta hai — "Write Once, Run Anywhere" ka foundation.

### JVM Architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                        JVM Architecture                      │
├─────────────────────────────────────────────────────────────┤
│  ClassLoader Subsystem                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Bootstrap   │  │ Extension   │  │ Application/System  │  │
│  │  ClassLoader │  │ ClassLoader │  │ ClassLoader         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  Runtime Data Areas (Memory)                                 │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐   │
│  │ Heap       │  │ Stack      │  │ Metaspace            │   │
│  │ (Objects)  │  │ (Frames)   │  │ (Class Metadata)     │   │
│  └────────────┘  └────────────┘  └──────────────────────┘   │
│  ┌────────────┐  ┌────────────┐                              │
│  │ PC Register│  │ Native     │                              │
│  │            │  │ Stack      │                              │
│  └────────────┘  └────────────┘                              │
├─────────────────────────────────────────────────────────────┤
│  Execution Engine                                            │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐   │
│  │ Interpreter│  │ JIT Compiler│ │ Garbage Collector    │   │
│  └────────────┘  └────────────┘  └──────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### JVM vs JRE vs JDK:

| Component | Contains | Purpose |
|-----------|----------|---------|
| **JVM** | Execution engine | Bytecode run karta hai |
| **JRE** | JVM + libraries | Runtime environment |
| **JDK** | JRE + dev tools (javac, javadoc) | Development |

### Execution Flow:

```
.java file → javac → .class (bytecode) → ClassLoader → Memory → Execution Engine → Output
                                              ↓
                                    JIT compiles hot code to native
```

**Short Interview Answer:**

JVM has three main parts: ClassLoader (loads .class files), Runtime Data Areas (memory management), and Execution Engine (interpreter + JIT + GC). JDK includes JRE which includes JVM.

---

## Q2: ClassLoader hierarchy kya hai? Parent Delegation Model explain karo.

**Answer:**

**ClassLoader** `.class` files ko load, verify, aur initialize karta hai. Hierarchy top-down delegation follow karti hai.

### ClassLoader Hierarchy:

```
Bootstrap ClassLoader (C++ — JVM ka part)
    ↑ parent
Extension/Platform ClassLoader (Java)
    ↑ parent
Application/System ClassLoader (Java — CLASSPATH)
    ↑ parent
Custom ClassLoaders (optional)
```

### Each ClassLoader's Role:

| ClassLoader | Loads | Location |
|-------------|-------|----------|
| **Bootstrap** | Core Java classes (`java.lang.*`) | `rt.jar` (Java 8) / modules (Java 9+) |
| **Extension/Platform** | Extension classes | `$JAVA_HOME/lib/ext` |
| **Application/System** | Application classes | CLASSPATH / `-cp` |
| **Custom** | Dynamic/plugin loading | Network, encrypted JARs, etc. |

### Parent Delegation Model:

```
1. Class load request aati hai
2. Pehle parent ko delegate karo (Bootstrap → Extension → Application)
3. Parent load kar le → done
4. Parent fail → child khud try karega
5. Security: Core Java classes override nahi ho sakte
```

```java
// ClassLoader check karo
ClassLoader cl = MyClass.class.getClassLoader();
System.out.println(cl);  // sun.misc.Launcher$AppClassLoader

ClassLoader parent = cl.getParent();
System.out.println(parent);  // sun.misc.Launcher$ExtClassLoader

ClassLoader bootstrap = parent.getParent();
System.out.println(bootstrap);  // null (Bootstrap — C++ mein written)
```

### Class Loading Process:

```
1. Loading:     .class file read → binary data memory mein
2. Linking:
   a. Verification: Bytecode valid hai ya nahi
   b. Preparation: Static fields ko default values
   c. Resolution: Symbolic → direct references
3. Initialization: Static blocks execute, static fields ko actual values
```

**Important:**

`ClassNotFoundException` tab aata hai jab class classpath pe nahi milti. Parent delegation se security milti hai — koi malicious `java.lang.String` override nahi kar sakta.

---

## Q3: JVM Memory Areas — Heap, Stack, Metaspace, PC Register, Native Stack?

**Answer:**

JVM memory ko **per-thread areas** aur **shared areas** mein divide karta hai.

### Memory Layout:

| Area | Scope | Stores | GC? | OOM Type |
|------|-------|--------|-----|----------|
| **Heap** | Shared (all threads) | Objects, arrays | ✅ Yes | `Java heap space` |
| **Stack** | Per thread | Method frames, local vars, partial results | ❌ No | `StackOverflowError` |
| **Metaspace** | Shared | Class metadata, method code, constant pool | ❌ No* | `Metaspace` |
| **PC Register** | Per thread | Current instruction address | ❌ No | — |
| **Native Stack** | Per thread | Native (C/C++) method calls | ❌ No | — |

### Heap — Objects Yahan Bante Hain:

```java
String str = new String("Hello");  // "Hello" object → Heap
User user = new User();            // User object → Heap
int[] arr = new int[100];          // Array → Heap

// Reference variable stack pe, actual object heap pe
```

### Stack — Method Calls & Local Variables:

```java
public int calculate(int a, int b) {  // Frame push on stack
    int result = a + b;               // Local variable — stack pe
    return result;                    // Frame pop
}

// Har thread ka apna stack — thread-safe by design
// Default stack size: ~1MB per thread (-Xss flag se change)
```

### Metaspace (Java 8+ — PermGen ki jagah):

```java
// Class metadata, method bytecode, constant pool
// Native memory mein allocate hota hai (not heap)
// -XX:MaxMetaspaceSize=256m se limit set karo

class MyClass {
    static int count = 0;  // Static variable → Heap (NOT Metaspace)
    // Class definition itself → Metaspace
}
```

### Stack vs Heap — Quick Example:

```java
public void example() {
    int x = 10;                    // x → Stack (primitive)
    String name = "Sachin";        // name (ref) → Stack, "Sachin" → String Pool (Heap)
    User user = new User("Sachin"); // user (ref) → Stack, User object → Heap
}
```

**Short Interview Answer:**

Heap = shared, objects/arrays store hote hain, GC yahan hoti hai. Stack = per-thread, method frames aur local variables. Metaspace = class metadata (Java 8+). PC Register = current instruction pointer. Native Stack = JNI calls.

---

## Q4: Young Generation vs Old Generation — object lifecycle kya hai?

**Answer:**

Heap ko **generations** mein divide kiya gaya hai based on **Generational Hypothesis** — "zyada tar objects jaldi die ho jaate hain."

### Heap Structure:

```
┌─────────────────────────────────────────────────────────────┐
│                         Heap Memory                         │
├─────────────────────────────────────────────────────────────┤
│  Young Generation (~1/3)         │  Old Generation (~2/3)   │
│  ┌──────────┬───────────┬──────────┤                        │
│  │ Eden     │ Survivor  │ Survivor │  Tenured / Old Gen     │
│  │ Space    │ S0        │ S1       │  Long-lived objects    │
│  └──────────┴───────────┴──────────┤                        │
└─────────────────────────────────────────────────────────────┘
```

### Object Lifecycle:

```
New Object → Eden Space
    ↓ Minor GC
Survived? → Survivor Space (S0 or S1), age = 1
    ↓ Minor GC (survive again)
Age increment → Other Survivor Space, age = 2
    ↓ ... repeat ...
Age reaches threshold (default 15) → Promoted to Old Generation
    ↓ Old Gen full
Major/Full GC → Still no space? → OutOfMemoryError
```

```java
// Short-lived objects — Eden mein create, Minor GC pe collect
for (int i = 0; i < 1000000; i++) {
    String temp = "temp-" + i;  // Eden → likely collected in Minor GC
}

// Long-lived objects — Old Gen mein promote
static Map<String, Object> cache = new ConcurrentHashMap<>();  // Old Gen
```

### Minor GC vs Major GC:

| GC Type | Where | Speed | Frequency | STW |
|---------|-------|-------|-----------|-----|
| **Minor GC** | Young Gen (Eden + Survivor) | Fast (ms) | Frequent | Brief pause |
| **Major/Full GC** | Entire Heap (Young + Old) | Slow (seconds) | Rare | Long pause |

### Tuning Flags:

```bash
# Young Gen size
java -XX:NewRatio=2 MyApp          # Old:Young = 2:1
java -XX:SurvivorRatio=8 MyApp     # Eden:Survivor = 8:1:1

# Promotion threshold
java -XX:MaxTenuringThreshold=15 MyApp  # Default 15 Minor GCs before Old Gen

# Heap size
java -Xms2g -Xmx2g MyApp           # Fixed heap — resizing overhead avoid
```

**Important:**

`-Xms` aur `-Xmx` same rakhna best practice hai production mein — heap resize ka overhead avoid hota hai.

---

## Q5: GC Algorithms — Serial, Parallel, G1, ZGC ka difference?

**Answer:**

Different GC algorithms different trade-offs offer karte hain — throughput vs latency.

### GC Algorithm Comparison:

| GC | Type | Threads | Pause Time | Use Case |
|----|------|---------|------------|----------|
| **Serial GC** | Stop-the-World | 1 | Long | Single-core, small apps |
| **Parallel GC** | Stop-the-World | Multiple | Medium | Throughput priority (batch) |
| **G1 GC** | Generational, Region-based | Multiple | Low (~200ms target) | Large heaps, balanced (default Java 9+) |
| **ZGC** | Concurrent, Region-based | Multiple | Ultra-low (<10ms) | Low-latency apps (Java 15+) |
| **Shenandoah** | Concurrent | Multiple | Ultra-low | Similar to ZGC |

### Serial GC:

```bash
java -XX:+UseSerialGC MyApp
# Single thread GC — simple but long pauses
# Good for: Client apps, small heap (<100MB)
```

### Parallel GC (Throughput Collector):

```bash
java -XX:+UseParallelGC MyApp
# Multiple threads GC — faster but still STW
# Good for: Batch processing, background jobs
# Default in Java 8
```

### G1 GC (Garbage First — Default Java 9+):

```bash
java -XX:+UseG1GC MyApp
java -XX:MaxGCPauseMillis=200 MyApp  # Target pause time

# Heap ko equal-sized regions mein divide karta hai
# Most garbage wale regions pehle collect
# Good for: Large heaps (4GB+), balanced latency/throughput
```

### ZGC (Ultra-Low Latency):

```bash
java -XX:+UseZGC MyApp
# Concurrent GC — application threads almost never pause
# Pause times < 10ms regardless of heap size
# Good for: Real-time systems, trading platforms, large heaps (TB scale)
```

### How to Choose:

```
Small app, single core?        → Serial GC
Batch processing, max throughput? → Parallel GC
General purpose, large heap?   → G1 GC (default)
Ultra-low latency required?    → ZGC / Shenandoah
```

**Short Interview Answer:**

Serial = single-thread STW. Parallel = multi-thread STW, max throughput. G1 = region-based, predictable pauses (default). ZGC = concurrent, sub-10ms pauses for low-latency apps. Choose based on latency vs throughput requirements.

---

## Q6: Stop-The-World (STW) kya hai? GC ka application pe impact?

**Answer:**

**Stop-The-World (STW)** matlab GC ke dauran **saari application threads pause** ho jaati hain — koi Java code execute nahi hota.

### STW Process:

```
Application running → GC triggered → ALL threads PAUSE (STW)
    → GC marks live objects → GC sweeps/compacts → ALL threads RESUME
```

### GC Phases and STW:

| Phase | STW? | What Happens |
|-------|------|-------------|
| **Initial Mark** | Brief STW | Mark GC roots |
| **Concurrent Mark** | No (app runs) | Traverse object graph |
| **Remark** | Brief STW | Finalize marking |
| **Cleanup/Sweep** | STW (varies) | Reclaim dead objects |
| **Compact** | STW | Defragment memory |

### Impact on Application:

```java
// During STW — yeh sab pause ho jaata hai:
// - HTTP request processing
// - Database queries
// - Message consumption
// - Timer/scheduled tasks

// Long STW = high latency spikes
// P99 latency badh jaati hai even if average latency achhi ho
```

### Monitoring GC:

```bash
# GC logging enable karo
java -Xlog:gc*:file=gc.log MyApp          # Java 9+
java -XX:+PrintGCDetails -Xloggc:gc.log MyApp  # Java 8

# Useful flags
java -XX:+HeapDumpOnOutOfMemoryError MyApp
java -XX:HeapDumpPath=/tmp/heapdump.hprof MyApp
```

### Reducing STW Impact:

| Strategy | How |
|----------|-----|
| **Choose low-latency GC** | G1, ZGC instead of Parallel |
| **Tune heap size** | `-Xms` = `-Xmx`, adequate Young Gen |
| **Reduce object allocation** | Object pooling, reuse |
| **Avoid full GC triggers** | Tune `MaxTenuringThreshold` |
| **Monitor GC logs** | Identify pause patterns |

**Important:**

Minor GC bhi STW hoti hai lekin bahut brief (milliseconds). Full GC seconds tak pause kar sakti hai — production mein Full GC frequent hona red flag hai.

---

## Q7: Java Memory Model (JMM) aur Happens-Before rules kya hain?

**Answer:**

**JMM** define karta hai ki ek thread ka write dusre thread ko kab visible hoga — CPU caches aur compiler reordering handle karta hai.

### The Visibility Problem:

```
Thread 1 (Core 1): flag = true  → Core 1 cache mein likha
Thread 2 (Core 2): while(!flag) → Core 2 cache se padha (purana value!)

Bina JMM guarantee ke — Thread 2 ko change dikhta hi nahi!
```

### Happens-Before Rules:

```
A happens-before B means:
1. A ka result B ko dikhega
2. A se pehle B reorder nahi ho sakta
3. Memory visibility guaranteed
```

| Rule | Example |
|------|---------|
| **Program Order** | Same thread mein pehle likha woh pehle execute |
| **Monitor Lock** | `unlock()` happens-before next `lock()` |
| **Volatile Write** | Volatile write happens-before volatile read |
| **Thread Start** | `thread.start()` happens-before `run()` |
| **Thread Join** | Thread termination happens-before `join()` return |
| **Transitivity** | A→B aur B→C toh A→C |

### Examples:

```java
// Monitor Lock Rule
synchronized (lock) {
    x = 10;  // Write
}  // unlock → happens-before
// ...
synchronized (lock) {
    int val = x;  // lock → val hamesha 10
}

// Volatile Rule
volatile boolean flag = false;
int data = 0;

// Thread 1:
data = 42;       // 1
flag = true;     // 2 (volatile write)

// Thread 2:
if (flag) {      // 3 (volatile read — happens-after 2)
    int val = data;  // 4 — val hamesha 42 (transitivity)
}

// Thread Start Rule
int x = 10;
Thread t = new Thread(() -> System.out.println(x));  // Hamesha 10
t.start();

// Thread Join Rule
Thread t = new Thread(() -> x = 42);
t.start();
t.join();  // join return → x = 42 guaranteed
System.out.println(x);  // 42
```

**Short Interview Answer:**

JMM defines visibility and ordering guarantees between threads. Happens-before rules ensure that actions in one thread are visible to another — through synchronized, volatile, thread start/join, and transitivity.

---

## Q8: volatile aur synchronized ke memory effects kya hain?

**Answer:**

Dono memory visibility guarantee karte hain, lekin scope aur atomicity alag hai.

### Memory Effects Comparison:

| Feature | volatile | synchronized |
|---------|----------|-------------|
| **Visibility** | ✅ Write visible to all threads | ✅ Write visible to all threads |
| **Atomicity** | ❌ Single read/write only | ✅ Entire block atomic |
| **Ordering** | Prevents reordering around volatile | Prevents reordering inside block |
| **Mutual Exclusion** | ❌ No | ✅ Yes |
| **Performance** | Lightweight | Heavier (lock acquire/release) |

### volatile Semantics:

```java
class Status {
    volatile boolean shutdown = false;
    volatile int processedCount = 0;

    // ✅ volatile write → flush to main memory
    // ✅ volatile read → read from main memory
    public void shutdown() {
        shutdown = true;  // Visible to all threads immediately
    }
}
```

### synchronized Semantics:

```java
class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // Atomic + Visible
        // 1. Lock acquire → flush local cache
        // 2. Execute critical section
        // 3. Lock release → write back to main memory
    }
}
```

### Double-Checked Locking (volatile zaroori hai):

```java
class Singleton {
    private static volatile Singleton instance;  // volatile MUST!

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();  // Reordering safe with volatile
                }
            }
        }
        return instance;
    }
}
// Bina volatile: partial construction dusre thread ko dikh sakti hai!
```

### When to Use What:

```
Single flag/status variable?     → volatile
Compound operation (count++)?    → synchronized or AtomicInteger
Critical section (multiple vars)?  → synchronized or Lock
Read-heavy, write-rare?           → ReadWriteLock
```

**Important:**

`volatile` sirf visibility guarantee deta hai — `count++` jaise compound operations ke liye `AtomicInteger` ya `synchronized` use karo.

---

## Q9: OutOfMemoryError ke types kya hain? Kaise debug karte ho?

**Answer:**

**OutOfMemoryError (OOM)** tab aata hai jab JVM ko aur memory nahi milti — different areas ke liye different types hain.

### OOM Types:

| Error | Area | Cause | Fix |
|-------|------|-------|-----|
| `Java heap space` | Heap | Objects zyada, GC collect nahi kar paaya | Increase `-Xmx`, fix memory leak |
| `Metaspace` | Metaspace | Too many classes loaded (dynamic proxies, class loaders) | Increase `-XX:MaxMetaspaceSize`, fix class loader leak |
| `Unable to create new native thread` | OS | Too many threads created | Reduce threads, increase OS limits |
| `GC overhead limit exceeded` | Heap | 98% time GC, <2% heap recovered | Increase heap, fix memory leak |
| `Direct buffer memory` | Native | NIO direct buffers exhausted | Increase `-XX:MaxDirectMemorySize` |
| `Request array size exceeds VM limit` | Heap | Array size > Integer.MAX_VALUE | Fix code logic |

### Common Scenarios:

```java
// 1. Java heap space — memory leak
List<Object> cache = new ArrayList<>();
while (true) {
    cache.add(new byte[1024 * 1024]);  // 1MB each — never removed!
}

// 2. Metaspace — class loader leak (common in app servers)
while (true) {
    URLClassLoader loader = new URLClassLoader(urls);
    Class<?> clazz = loader.loadClass("com.example.MyClass");
    // loader never dereferenced → classes never unloaded
}

// 3. Too many threads
while (true) {
    new Thread(() -> {
        try { Thread.sleep(Long.MAX_VALUE); } catch (InterruptedException e) {}
    }).start();
}
```

### Debugging Steps:

```bash
# 1. Heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/dump.hprof MyApp

# 2. Analyze with tools
# - Eclipse MAT (Memory Analyzer Tool)
# - VisualVM
# - jmap -dump:live,format=b,file=heap.bin <pid>

# 3. GC logging
java -Xlog:gc*:file=gc.log MyApp

# 4. Monitor in production
# - Prometheus + Grafana
# - JVM metrics: heap usage, GC frequency, thread count
```

### Heap Dump Analysis (MAT):

```
1. Open .hprof file in Eclipse MAT
2. "Leak Suspects Report" run karo
3. Dominator tree dekho — sabse bade objects
4. GC roots se unreachable objects = leak candidates
5. "Path to GC Roots" — kaun hold kar raha hai reference
```

**Short Interview Answer:**

Common OOM types: heap space (object leak), Metaspace (class loader leak), native thread (too many threads). Debug with `-XX:+HeapDumpOnOutOfMemoryError`, analyze with MAT, check GC logs, and fix root cause (usually memory/class loader leak).

---

## Q10: Escape Analysis kya hai? Stack allocation ka concept?

**Answer:**

**Escape Analysis** JVM ka optimization hai — agar object method ke bahar "escape" nahi karta, toh JVM usko stack pe allocate kar sakta hai instead of heap pe.

### Object Escape Scenarios:

```java
// ❌ Escapes — returned to caller (heap allocation)
public String createAndReturn() {
    StringBuilder sb = new StringBuilder("Hello");
    return sb.toString();
}

// ❌ Escapes — stored in instance field (heap allocation)
private Object field;
public void storeEscape() {
    field = new Object();
}

// ✅ No Escape — sirf method ke andar use (stack allocation possible!)
public int calculate() {
    Point p = new Point(10, 20);  // JVM may allocate on stack!
    return p.x + p.y;
}
```

### Optimizations from Escape Analysis:

| Optimization | What Happens |
|-------------|-------------|
| **Stack Allocation** | Object stack pe allocate — no GC needed |
| **Scalar Replacement** | Object ko individual fields mein break — object hi na banao |
| **Lock Elision** | Agar lock sirf thread-local object pe hai — lock remove |

### Scalar Replacement Example:

```java
// Source code
public int sum() {
    Point p = new Point(10, 20);
    return p.x + p.y;
}

// After scalar replacement (conceptually)
public int sum() {
    int x = 10;  // No Point object created!
    int y = 20;
    return x + y;
}
```

### Lock Elision:

```java
public String concat() {
    StringBuffer sb = new StringBuffer();  // Local — no escape
    sb.append("Hello");  // synchronized method but lock elided!
    sb.append(" World");
    return sb.toString();
}
// JVM detects sb doesn't escape → removes synchronized overhead
```

### Verify Escape Analysis:

```bash
# Enable diagnostics
java -XX:+PrintCompilation -XX:+UnlockDiagnosticVMOptions -XX:+PrintEscapeAnalysis MyApp

# Disable to compare performance
java -XX:-DoEscapeAnalysis MyApp
```

**Important:**

Escape analysis JVM ka internal optimization hai — tumhe code change nahi karna padta. Interview mein bas concept samjhao: local non-escaping objects ko JVM stack pe allocate kar sakta hai, GC pressure kam hoti hai.

**Short Interview Answer:**

Escape analysis determines if an object stays within a method scope. Non-escaping objects can be stack-allocated or scalar-replaced (fields inlined), avoiding heap allocation and GC. Lock elision removes unnecessary synchronization on thread-local objects.
