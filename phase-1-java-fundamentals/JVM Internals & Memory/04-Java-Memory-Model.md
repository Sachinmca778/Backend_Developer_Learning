# Java Memory Model (JMM)

## Status: Not Started

---

## Table of Contents

1. [Happens-Before Rules](#happens-before-rules)
2. [Visibility Guarantees](#visibility-guarantees)
3. [Reordering](#reordering)
4. [Volatile Semantics](#volatile-semantics)
5. [Synchronized Semantics](#synchronized-semantics)
6. [Final Field Semantics](#final-field-semantics)

---

## Happens-Before Rules

**Matlab:** Java Memory Model ka foundation — ek thread ka write dusre thread ko kab dikhega yeh guarantee karta hai.

### What is Happens-Before?

```
A happens-before B means:
1. A ka result B ko dikhega
2. A se pehle B reorder nahi ho sakta
3. Memory visibility guaranteed hai
```

### Happens-Before Rules

```
1. Program Order Rule:
   Thread ke andar, jo pehle likha hai woh pehle execute hoga

2. Monitor Lock Rule:
   Lock release → uske baad ka lock acquire

3. Volatile Variable Rule:
   Volatile write → uske baad ka volatile read

4. Thread Start Rule:
   thread.start() → us thread ka run()

5. Thread Join Rule:
   Thread terminates → thread.join() return

6. Transitivity:
   A → B aur B → C toh A → C

7. Default Values Rule:
   Object initialization → default values visible

8. Interruption Rule:
   Thread.interrupt() → interrupted status detect
```

### Examples

```java
// 1. Program Order
int x = 10;
int y = x + 5;  // y hamesha 15 hoga

// 2. Monitor Lock (synchronized)
synchronized (lock) {
    x = 10;  // Write
}  // Lock release → happens-before
// ...
synchronized (lock) {
    int val = x;  // Lock acquire → val hamesha 10 hoga
}

// 3. Volatile
volatile boolean flag = false;
int data = 0;

// Thread 1:
data = 42;       // 1
flag = true;     // 2 (volatile write → happens-before)

// Thread 2:
if (flag) {      // 3 (volatile read → happens-after 2)
    int val = data;  // 4 — val hamesha 42 hoga (transitivity: 1→2→3→4)
}

// 4. Thread Start
int x = 10;
Thread t = new Thread(() -> {
    System.out.println(x);  // Hamesha 10 print hoga
});
t.start();  // start() happens-before run()

// 5. Thread Join
Thread t = new Thread(() -> {
    x = 42;
});
t.start();
t.join();  // join() return happens-after thread terminates
System.out.println(x);  // Hamesha 42 print hoga
```

---

## Visibility Guarantees

**Matlab:** Ek thread ka write dusre thread ko kab dikhega — CPU caches aur memory barriers.

### The Problem: Memory Visibility

```java
class SharedData {
    int counter = 0;  // NOT volatile
}

SharedData data = new SharedData();

// Thread 1
new Thread(() -> {
    while (true) {
        data.counter++;  // CPU cache mein write
    }
}).start();

// Thread 2
new Thread(() -> {
    int last = 0;
    while (true) {
        if (data.counter != last) {
            System.out.println("Counter: " + data.counter);
            last = data.counter;
        }
        // ❌ Thread 2 ka CPU cache purana value dikhata rahega
        // Thread 1 ka write dikhta nahi!
    }
}).start();
```

### Why Visibility Fails

```
Thread 1 → CPU Core 1 → L1 Cache: counter = 5
Thread 2 → CPU Core 2 → L1 Cache: counter = 0 (stale)

CPU Core 2 apne cache se read karta hai → stale value
CPU Core 1 ka write CPU Core 2 ko nahi dikhta
```

### Solutions for Visibility

```java
// Solution 1: volatile
volatile int counter = 0;  // ✅ Main memory se read/write

// Solution 2: synchronized
synchronized void increment() { counter++; }  // ✅ Lock acquire/release

// Solution 3: Atomic
AtomicInteger counter = new AtomicInteger(0);  // ✅ CAS operations

// Solution 4: final
final int value = 42;  // ✅ Construction ke baad visible
```

---

## Reordering

**Matlab:** Compiler aur CPU instructions ko reorder kar sakte hain — performance optimization ke liye.

### Why Reordering Happens

```java
// Original code:
int a = 1;
int b = 2;
int c = a + b;

// Reordered by compiler/CPU (valid):
int b = 2;    // b pehle calculate ho sakta hai
int a = 1;
int c = a + b;

// Invalid reordering (c before a/b):
int c = a + b;  // ❌ Error: a aur b abhi defined nahi
int a = 1;
int b = 2;
```

### Reordering Problem

```java
class ReorderingExample {
    int x = 0;
    int y = 0;
    boolean ready = false;

    // Thread 1
    void writer() {
        x = 10;          // 1
        y = 20;          // 2
        ready = true;    // 3 (volatile)
    }

    // Thread 2
    void reader() {
        if (ready) {     // 4
            int val = x + y;  // 5
            System.out.println(val);  // Kya print hoga?
        }
    }
}
```

### What Can Happen

```
Without happens-before:
Thread 1: x=10, y=20, ready=true
Thread 2: ready check → true → x + y → ?

Possible outcomes:
- 30 (correct: x=10, y=20)
- 20 (x=0, y=20 — x reorder ho gaya)
- 10 (x=10, y=0 — y reorder ho gaya)
- 0  (x=0, y=0 — dono reorder)

With volatile ready:
ready = true → happens-before → x=10 and y=20 are visible
So reader always sees x=10, y=20 → 30
```

### Memory Barriers

```
Load Barrier:  CPU ko force karta hai ki cache se read kare (not stale cache)
Store Barrier: CPU ko force karta hai ki main memory mein write kare

volatile read  → Load Barrier
volatile write → Store Barrier
```

---

## Volatile Semantics

**Matlab:** Variable ko main memory mein read/write karo — visibility + ordering guarantee.

### What Volatile Guarantees

```java
volatile int counter = 0;

// Visibility:
// - Write hamesha main memory mein hota hai
// - Read hamesha main memory se hota hai
// - Dusre threads ko latest value dikhegi

// Ordering:
// - volatile write se pehle ke operations reorder nahi ho sakte
// - volatile read ke baad ke operations reorder nahi ho sakte

// NOT Atomic:
counter++;  // Read → Modify → Write — race condition possible!
```

### Volatile vs Non-Volatile

```java
// Non-volatile
int x = 0;
// Thread 1: x = 10;  // CPU cache
// Thread 2: print(x);  // Stale cache se 0

// Volatile
volatile int x = 0;
// Thread 1: x = 10;  // Main memory
// Thread 2: print(x);  // Main memory se 10
```

### When to Use Volatile

```java
// ✅ Use volatile for:
// - Single variable read/write
// - Flag/status variables
// - One-writer, multiple-readers
// - Simple state machine

// ❌ Don't use volatile for:
// - Compound operations (read-modify-write)
// - Multiple variables ko coordinate karna
// - Atomic increment/decrement
```

### Volatile Example: Status Flag

```java
class Worker {
    private volatile boolean running = true;

    public void run() {
        while (running) {
            // Do work
            // running check karo — latest value milegi
        }
    }

    public void stop() {
        running = false;  // Main memory mein write — worker ko dikhega
    }
}
```

---

## Synchronized Semantics

**Matlab:** Mutual exclusion + visibility — sirf ek thread enter kar sakta hai, aur sab writes visible hote hain.

### What Synchronized Guarantees

```java
synchronized (lock) {
    // Entry: Acquire lock → all previous writes visible
    x = 10;
    y = 20;
    // Exit: Release lock → all writes flushed to main memory
}
```

### Memory Visibility with Synchronized

```java
class SharedData {
    int x = 0;
    int y = 0;

    public synchronized void writer() {
        x = 10;  // 1
        y = 20;  // 2
    }  // Lock release → happens-before → x=10, y=20 visible

    public synchronized void reader() {
        // Lock acquire → writer ke sab writes visible
        int val = x + y;  // Hamesha 30 (ya 0 agar writer nahi run hua)
    }
}
```

### Synchronized vs Volatile

| Feature | Volatile | Synchronized |
|---------|----------|-------------|
| **Visibility** | ✅ | ✅ |
| **Atomicity** | ❌ | ✅ |
| **Ordering** | ✅ | ✅ |
| **Locking** | ❌ No lock | ✅ Lock acquire/release |
| **Performance** | Faster (no lock) | Slower (lock overhead) |
| **Compound Ops** | ❌ Not safe | ✅ Safe |
| **Multiple Vars** | ❌ No coordination | ✅ Coordinates multiple vars |

---

## Final Field Semantics

**Matlab:** Final fields ko safe publication guarantee milti hai — construction ke baad sab threads ko visible hote hain.

### Safe Publication with Final

```java
class User {
    final String name;  // Final field
    final int age;      // Final field

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// Thread 1:
User user = new User("Sachin", 25);

// Thread 2:
System.out.println(user.name);  // Hamesha "Sachin" — guaranteed visible
System.out.println(user.age);   // Hamesha 25 — guaranteed visible
```

### Why Final Fields are Special

```
JVM guarantee:
1. Final fields constructor mein initialize hote hain
2. Object reference kisi thread ko tabhi dikhega jab constructor complete ho
3. Final fields ka value sab threads ko visible hoga

Non-final fields:
- Constructor ke baad bhi visibility guarantee nahi
- Threads stale/deka values dekh sakte hain (without synchronization)
```

### Final vs Non-Final in Constructor

```java
class Container {
    final int finalValue;
    int nonFinalValue;

    public Container(int value) {
        finalValue = value;    // 1
        nonFinalValue = value; // 2
    }
}

// Thread 1:
Container c = new Container(42);

// Thread 2:
System.out.println(c.finalValue);    // ✅ Always 42 — guaranteed
System.out.println(c.nonFinalValue); // ⚠️ May be 0 — no guarantee
```

### Immutability and Final

```java
// Immutable class — sab fields final
public final class ImmutablePoint {
    private final int x;
    private final int y;

    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
}

// Thread-safe without synchronization
// Final fields + no setters = safe publication
ImmutablePoint point = new ImmutablePoint(10, 20);
// All threads see x=10, y=20 — guaranteed
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Happens-Before** | Visibility aur ordering guarantee — 8 rules |
| **Program Order** | Thread ke andar sequential consistency |
| **Monitor Lock** | Lock release → lock acquire |
| **Volatile** | Volatile write → volatile read |
| **Thread Start/Join** | start() → run(), thread end → join() return |
| **Transitivity** | A → B, B → C implies A → C |
| **Visibility** | CPU caches — stale values problem |
| **Reordering** | Compiler/CPU optimization — memory barriers prevent |
| **Volatile** | Main memory read/write — visibility + ordering, no atomicity |
| **Synchronized** | Lock acquire/release — visibility + atomicity + ordering |
| **Final Fields** | Safe publication — constructor complete hone pe visible |
