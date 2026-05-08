# Java Memory & GC (Interview Lens)

## Status: Not Started

---

## Table of Contents

1. [JVM Memory Big Picture](#jvm-memory-big-picture)
2. [Stack vs Heap](#stack-vs-heap)
3. [Where Do Primitives & Objects Live?](#where-do-primitives--objects-live)
4. [Method Area / Metaspace](#method-area--metaspace)
5. [Memory Leaks in Java](#memory-leaks-in-java)
6. [GC Roots](#gc-roots)
7. [Reachability — Strong / Soft / Weak / Phantom](#reachability--strong--soft--weak--phantom)
8. [GC Algorithms (Cliff-Notes)](#gc-algorithms-cliff-notes)
9. [Common Output Trap Qs](#common-output-trap-qs)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

> Deeper tuning ke liye: `phase-4-advanced-topics/Performance & Optimization/01-JVM-Performance-Tuning.md`. Yahan **interview level** focus.

---

## JVM Memory Big Picture

```
┌────────── JVM Process ──────────┐
│ Heap                              │
│   ├── Young Gen (Eden + S0/S1)    │
│   └── Old Gen                     │
│ Metaspace (class metadata)        │
│ Thread stacks (per thread)        │
│ Code cache (JIT)                  │
│ Direct memory (NIO)               │
│ Native libraries                  │
└───────────────────────────────────┘
```

---

## Stack vs Heap

| Aspect | Stack | Heap |
|--------|-------|------|
| Per | Thread | JVM-wide |
| Stores | Frames: locals, partial results, return addr | Objects + arrays |
| Size | Small (`-Xss` default ~1 MB) | Big (`-Xms`/`-Xmx`) |
| Lifecycle | LIFO with method calls | GC managed |
| Errors | `StackOverflowError` | `OutOfMemoryError: Java heap space` |

---

## Where Do Primitives & Objects Live?

### Local primitives

```java
void foo() {
    int x = 5;          // x → method's stack frame
}
```

### Object references vs object content

```java
void foo() {
    Person p = new Person("A");
    // 'p' (the reference) → stack frame
    // Person object itself → HEAP
}
```

### Field primitives

```java
class Person {
    int age;            // primitive, but field → object header me, on heap
}
```

→ Field primitives object ke saath heap par travel karte hain. Sirf **method local primitives** (and reference variables) stack frame mein.

### Escape Analysis (advanced answer)

JIT compiler kabhi kabhi short-lived objects ko **stack allocate** kar sakta hai (escape analysis) — bonus answer for advanced rounds.

---

## Method Area / Metaspace

- **Class metadata** (definitions, method bytecode, constants).
- Java 8+ → **Metaspace** (native memory, not heap).
- Tunable: `-XX:MaxMetaspaceSize=256m` — leaks here cause `OutOfMemoryError: Metaspace` (e.g., classloader leaks in app servers).

---

## Memory Leaks in Java

> "GC hai phir leak kaisa?" — "Reachable but useless" = leak.

### 1. Unbounded static collections

```java
class Cache {
    static final Map<String, Object> CACHE = new HashMap<>();
}
```

Add but never remove → grows forever.

### 2. Unclosed resources

```java
InputStream in = new FileInputStream(file);
// forgot in.close()
```

→ Use **try-with-resources**.

### 3. Listeners / observers not unregistered

```java
eventBus.subscribe(this);
// destroy on lifecycle end? otherwise eventBus holds 'this'
```

### 4. ThreadLocal not cleaned

```java
private static final ThreadLocal<HeavyObj> TL = new ThreadLocal<>();
TL.set(big);
// forgot TL.remove() — esp. in thread pools (thread reused)
```

### 5. Inner / anonymous class capturing outer

```java
button.setListener(new Listener() {
    public void onClick() { /* uses outer 'this' */ }
});
// Listener holds outer reference → outer can't be GC'd
```

### 6. Classloader leaks (app servers)

Web app redeploy → old classloader held → metaspace leak.

### 7. JDBC / pool leaks

Connection borrow but `close()` skip → pool exhaustion (cross-ref Performance folder).

---

## GC Roots

GC objects ko reachability se collect karta hai. **Roots** kahan se start hota hai?

| Root Type | Examples |
|-----------|---------|
| Active thread stacks | Local vars, params |
| Static fields | Class statics |
| JNI references | Native code holds |
| System classloader | Bootstrap classes |
| Synchronized monitors | Held locks |
| Internal JVM (e.g., `Thread.currentThread`) | — |

> "Anything reachable from a GC root via reference chain = alive."

---

## Reachability — Strong / Soft / Weak / Phantom

```java
import java.lang.ref.*;
```

### Strong (default)

```java
Object o = new Object();   // strong ref
```

→ Kabhi GC nahi until ref nullified or scope exit.

### SoftReference

```java
SoftReference<byte[]> sr = new SoftReference<>(new byte[10_000_000]);
byte[] b = sr.get();   // may be null after memory pressure
```

→ Memory pressure mein collect → **caches** ke liye good (memory-sensitive).

### WeakReference

```java
WeakReference<Object> wr = new WeakReference<>(new Object());
wr.get();   // very next GC may return null
```

→ `WeakHashMap` keys, listeners — collect at next GC if no strong refs.

### PhantomReference

```java
ReferenceQueue<Object> q = new ReferenceQueue<>();
PhantomReference<Object> pr = new PhantomReference<>(obj, q);
```

→ `get()` always null. Used to know **after object is finalized** to perform cleanup (replacement for `finalize()`).

### When to mention which?

| Use case | Type |
|----------|------|
| Cache value | Soft |
| Map keys (auto-clean) | Weak (`WeakHashMap`) |
| Pre-finalize cleanup | Phantom (`Cleaner` API) |

---

## GC Algorithms (Cliff-Notes)

| GC | Pause | When |
|----|-------|------|
| Serial | Long | Tiny apps |
| Parallel | Long but throughput | Batch |
| **G1** | Predictable (~ms) | Default modern |
| **ZGC / Shenandoah** | Sub-ms | Big heaps / latency apps |

### Generational hypothesis

> "Most objects die young."

→ Young Gen (Eden + 2 Survivors) frequent minor GC; long-lived → Old Gen via tenuring.

### Stop-the-world

GC ke kuch phases mein **all app threads pause**. G1/ZGC isko minimize karte hain.

---

## Common Output Trap Qs

### Q1. Will this compile/run? Memory?

```java
public static void leak() {
    while (true) {
        new Object();
    }
}
```

→ Compiles. New objects allocated, but **no strong reference** kept → GC eligible immediately. **Leak nahi**, just churn. Eventually GC keeps running fine; CPU heavy.

### Q2. Static reference + GC

```java
class Holder {
    static List<byte[]> list = new ArrayList<>();
}
Holder.list.add(new byte[10_000_000]);
```

→ Strong ref via static → never GC'd → leak.

### Q3. WeakReference behavior

```java
Object o = new Object();
WeakReference<Object> w = new WeakReference<>(o);
o = null;
System.gc();
w.get();   // likely null (not guaranteed)
```

---

## Pitfalls

1. **`System.gc()` trust** — JVM may ignore; for tests only.
2. **`finalize()` use** — deprecated; use `Cleaner` / try-with-resources.
3. **OOM = always heap?** — could be metaspace, direct memory, GC overhead, native — read message carefully.
4. **`-Xmx` raise jab GC churn high** — sometimes heap chhota; sometimes leak — diagnose first.
5. **Heap dump on prod accidentally** — pauses JVM.
6. **Soft vs Weak confusion** — Soft = memory-sensitive cache; Weak = aggressive cleanup.
7. **ThreadLocal in pool** — value persists across requests → security/data leak.

---

## Cheat Sheet

| Concept | One-liner |
|---------|-----------|
| Stack | Per-thread frames; locals + ref vars |
| Heap | Objects + arrays |
| Metaspace | Class metadata (Java 8+) |
| GC root | Starting point for reachability |
| Strong | Default; never auto-collected |
| Soft | Memory-pressure clear (caches) |
| Weak | Next GC clear (`WeakHashMap`) |
| Phantom | Post-finalization cleanup |
| Memory leak | Reachable but unused |
| OOM | Heap / Metaspace / Direct / GC overhead |

---

## Practice

1. Heap dump leak demo: static List me 1 GB allocate → MAT mein dominator tree dekho.
2. WeakHashMap vs HashMap — entry auto-clean dekho post `System.gc()`.
3. ThreadLocal leak: pool thread reuse with set + skip remove → snapshot.
4. Inner class leak demo: anon listener holding outer.
5. SoftReference cache: low Xmx + allocate → observe GC clearing.
