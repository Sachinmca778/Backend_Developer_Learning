# Level 3: JVM Internals & Performance (Q147 - Q156)

---

### Q147: How does the JVM work internally? (ClassLoader, Memory Areas, GC, Execution Engine)
**Answer (Hinglish):**

JVM 3 major subsystems se milkar banta hai:

```
[ .class files ]
       ↓
[ 1. ClassLoader Subsystem ] (Loading -> Linking [Verify, Prepare, Resolve] -> Initialization)
       ↓
[ 2. JVM Memory Areas ] (Method/Metaspace, Heap, Stack, PC Register, Native Method Stack)
       ↓
[ 3. Execution Engine ] (Interpreter + JIT Compiler + Garbage Collector + Profiler)
```

1. **ClassLoader Subsystem:** Bytecode `.class` files ko load karta hai, verify karta hai, static variables initialize karta hai.
2. **JVM Memory Areas:** Data aur instructions ko store karta hai.
3. **Execution Engine:**
   - **Interpreter:** Bytecode ko line-by-line machine instructions me convert karta hai (Fast startup, slow execution).
   - **JIT (Just-In-Time) Compiler:** Frequently executed code ("hotspots") ko direct native CPU machine code me compile karke cache kar leta hai.
   - **Garbage Collector:** Dead objects ko sweep/compact karta hai.

---

### Q148: What are the different types of ClassLoaders in Java?
**Answer (Hinglish):**

Java me **Delegation-Hierarchy Principle** follow hota hai:

1. **Bootstrap ClassLoader:** C/C++ me likha primary loader jo core Java classes (`java.lang.*`, `rt.jar` / `java.base`) ko `$JAVA_HOME/lib` se load karta hai.
2. **Platform / Extension ClassLoader:** Java platform extensions ko load karta hai.
3. **Application / System ClassLoader:** Application ke `CLASSPATH` aur dependencies (Maven/Gradle jars) ko load karta hai.
4. **Custom ClassLoader:** Developer apna classloader create kar sakta hai (e.g. encrypted bytecode decrypt karke load karna ya network se byte stream load karna).

---

### Q149: Strong, Weak, Soft, and Phantom References in Java
**Answer (Hinglish):**

| Reference Type | Class | GC Behavior | Best Use Case |
| :--- | :--- | :--- | :--- |
| **Strong** | Default (`Object obj = new Object()`) | Jab tak reference reachable hai, GC **kabhi collect nahi karega**. | Normal business objects. |
| **Soft** | `SoftReference<T>` | GC tabhi collect karega jab **memory khatam hone wali ho** (OOM se just pehle). | Memory-sensitive caching (in-memory image cache). |
| **Weak** | `WeakReference<T>` | Jaise hi GC run hoga, **turant collect ho jayega** chahe memory bachi ho ya na ho. | Canonical mapping (e.g. `WeakHashMap`). |
| **Phantom** | `PhantomReference<T>` | Object memory se reclaim hone ke baad queue me jata hai. `get()` hamesha `null` return karta hai. | Post-mortem cleanup, direct native off-heap memory deallocation. |

---

### Q150 & Q151: How Java handles memory leaks & GC Algorithms Comparison
**Answer (Hinglish):**

- **Memory Leak in Java:** Jab koi object application ke liye useless ho chuka ho, lekin galti se kisi live root reference (e.g. static variable, unremoved listener, open thread) se juda reh jata hai, to GC use delete nahi kar pata.
- **Major GC Algorithms:**
  1. **Mark and Sweep:** Reachable objects ko mark karta hai, baaki ko sweep/delete karta hai (Fragmentation rehti hai).
  2. **Mark-Sweep-Compact:** Delete karne ke baad memory ko compact karta hai contiguous space banane ke liye.
  3. **G1 (Garbage-First):** Heap ko uniform regions (1MB to 32MB) me divide karta hai aur jis region me sabse zyada garbage hota hai use priority par pehle collect karta hai.
  4. **ZGC (Z Garbage Collector):** Colored pointers aur load barriers use karta hai; pause times < 1ms guarantee karta hai chahe heap 16TB ka ho.

---

### Q152: How do you tune JVM performance?
**Answer (Hinglish):**

Production tuning ke 5 critical pillars:
1. **Heap Sizing:** `-Xms4g -Xmx4g` (Initial aur Max heap same rakhne se heap resizing pauses avoid hote hain).
2. **GC Selection:** Low latency REST APIs ke liye `-XX:+UseG1GC` ya `-XX:+UseZGC`.
3. **Metaspace Tuning:** `-XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m` (Memory leak aur class unloading issues avoid karne ke liye).
4. **GC Logging:** `-Xlog:gc*:file=/var/log/gc.log:time,uptime:filecount=5,filesize=50M` se GC pause times monitor karein.
5. **Thread Stack Size:** `-Xss512k` (Default 1MB hota hai, ise kam karke high-thread concurrency me stack memory bachayi ja sakti hai).

---

### Q153: What is JIT compilation?
**Answer (Hinglish):**

JIT (Just-In-Time) compiler JVM ka execution optimization engine hai:
- Shuruat me JVM bytecode ko interpreter se execute karta hai.
- Sath me JVM ek **Profiler** run karta hai jo method invocation counter aur loop backedge counter maintain karta hai.
- Jab koi method ya loop bar-bar run hota hai (Hot Spot), to JIT compiler background me us bytecode ko highly optimized **Native Machine Code** me compile karta hai.
- **Techniques Used:** Method Inlining (method call overhead hatana), Loop Unrolling, Dead Code Elimination, Escape Analysis (objects ko heap ki jagah stack par allocate karna).

---

### Q154 & Q155: JVM Memory Areas & Metaspace in Java 8+
**Answer (Hinglish):**

- **Heap:** Objects, instance variables, arrays store karta hai.
- **Stack:** Local variables aur thread execution stack frames.
- **Program Counter (PC) Register:** Current executing instruction ka memory address hold karta hai.
- **Native Method Stack:** C/C++ native methods (`JNI`) ke execution frames.
- **Metaspace (Java 8+):**
  - Java 7 tak PermGen (Permanent Generation) JVM heap ke andar tha jisme class metadata aur static variables store hote the, aur frequently `java.lang.OutOfMemoryError: PermGen space` crash deta tha.
  - Java 8 me PermGen ko completely hata kar **Metaspace** laya gaya jo **Native OS Memory (RAM)** use karta hai. Iska size default unlimited (OS memory limit tak) hota hai.

---

### Q156: How do you analyze heap dumps and thread dumps?
**Answer (Hinglish):**

- **Thread Dump Analysis:**
  - **Capture:** `jcmd <PID> Thread.print > threaddump.txt` ya `jstack <PID>`.
  - **Tool:** FastThread.io ya VisualVM.
  - **Look For:** Threads in `BLOCKED` state, circular locks (Deadlocks), High CPU consumer threads matching OS thread LWP IDs.
- **Heap Dump Analysis:**
  - **Capture:** `jcmd <PID> GC.heap_dump /path/dump.hprof`.
  - **Tool:** Eclipse Memory Analyzer Tool (MAT).
  - **Look For:** "Dominator Tree" and "Leak Suspects Report". Check which specific class or collection retains >70% of total heap memory.
