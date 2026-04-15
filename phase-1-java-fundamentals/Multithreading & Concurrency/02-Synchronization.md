# Synchronization

## Status: Not Started

---

## Table of Contents

1. [The Memory Visibility Problem](#the-memory-visibility-problem)
2. [synchronized Keyword](#synchronized-keyword)
3. [Intrinsic Locks (Monitor)](#intrinsic-locks-monitor)
4. [volatile Keyword](#volatile-keyword)
5. [Happens-Before Relationship](#happens-before-relationship)

---

## The Memory Visibility Problem

**Matlab:** Multiple threads alag-alag CPU cores pe run hote hain — har core ka apna cache hota hai. Ek thread ka dusre thread ko data dikhta nahi.

### The Problem

```java
class SharedFlag {
    boolean running = true;  // Shared variable
}

SharedFlag flag = new SharedFlag();

// Thread 1 — flag change karega
new Thread(() -> {
    try { Thread.sleep(100); } catch (InterruptedException e) {}
    flag.running = false;  // Change kiya
    System.out.println("Flag set to false");
}).start();

// Thread 2 — flag check karega
new Thread(() -> {
    while (flag.running) {  // ❌ Infinite loop — change dikhta nahi!
        // Busy wait
    }
    System.out.println("Loop exited");  // ❌ Never printed
}).start();
```

### Why This Happens

```
Thread 2 → CPU Core 1 → Cache: running = true
Thread 1 → CPU Core 2 → Cache: running = false (change kiya)

Thread 2 apne cache se read karta hai → running = true (purana value)
Thread 1 ka change Thread 2 ko dikhta nahi!
```

---

## synchronized Keyword

**Matlab:** Mutual exclusion — ek time pe sirf ek thread hi synchronized block/method mein enter kar sakta hai.

### synchronized Method

```java
class Counter {
    private int count = 0;

    // synchronized method — sirf ek thread enter kar sakta hai
    public synchronized void increment() {
        count++;  // Read → Modify → Write (atomic operation ban gaya)
    }

    public synchronized int getCount() {
        return count;
    }
}

// Usage
Counter counter = new Counter();

Thread t1 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        counter.increment();
    }
});

Thread t2 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        counter.increment();
    }
});

t1.start();
t2.start();
t1.join();
t2.join();

System.out.println(counter.getCount());  // 2000 ✅ (always correct)
```

### synchronized Block

```java
class BankAccount {
    private double balance;
    private final Object lock = new Object();  // Custom lock object

    public void deposit(double amount) {
        // Sirf is block pe lock — method ke baaki hisse free hain
        synchronized (lock) {
            balance += amount;
        }
    }

    public void withdraw(double amount) {
        synchronized (lock) {
            if (balance >= amount) {
                balance -= amount;
            }
        }
    }

    public double getBalance() {
        synchronized (lock) {
            return balance;
        }
    }
}
```

### synchronized on `this`

```java
class SharedResource {
    private int data;

    // synchronized method = synchronized(this) block
    public synchronized void setData(int data) {
        this.data = data;
    }

    // Same as above
    public void setDataAlt(int data) {
        synchronized (this) {
            this.data = data;
        }
    }
}
```

### Static synchronized Method

```java
class GlobalCounter {
    private static int count = 0;

    // Class-level lock — sab instances ke liye same lock
    public static synchronized void increment() {
        count++;
    }

    // Alternative — explicit class lock
    public static void incrementAlt() {
        synchronized (GlobalCounter.class) {
            count++;
        }
    }
}
```

### ⚠️ synchronized Limitations

```
1. Performance — locking overhead
2. Deadlock risk — agar ordering proper nahi hai
3. No timeout — wait indefinitely
4. No fairness — thread starvation possible
```

---

## Intrinsic Locks (Monitor)

**Matlab:** Har Java object ka ek built-in lock hota hai — `synchronized` isi ko use karta hai.

### How Monitor Works

```java
class MonitorExample {
    private int value;

    public synchronized void setValue(int value) {
        this.value = value;
        // Method exit pe lock release hota hai
    }

    public synchronized int getValue() {
        return value;
        // Method exit pe lock release hota hai
    }
}

// Thread 1: setValue(10) → lock acquire → set → lock release
// Thread 2: getValue() → lock acquire → get → lock release
// Thread 3: setValue(20) → wait karega jab tak Thread 2 lock release na kare
```

### Lock Acquisition Rules

```
1. Sirf ek thread ek time pe object ka lock hold kar sakta hai
2. Thread lock acquire karta hai jab synchronized block/method enter karta hai
3. Thread lock release karta hai jab block/method exit hota hai (normal ya exception)
4. Same thread baar-baar same lock acquire kar sakta hai (reentrant)
```

### Reentrant Lock

```java
class ReentrantExample {
    public synchronized void methodA() {
        System.out.println("Method A");
        methodB();  // Same lock dobara acquire karega — allowed!
    }

    public synchronized void methodB() {
        System.out.println("Method B");
    }
}

// Thread calls methodA() → lock acquire → calls methodB() → same lock acquire → exit B → exit A
// Deadlock nahi hoga — Java locks reentrant hote hain
```

### Lock per Object

```java
class Counter {
    private int count;
    // Har Counter instance ka apna lock hai
}

Counter c1 = new Counter();
Counter c2 = new Counter();

// Thread 1: c1.increment() → c1 ka lock
// Thread 2: c2.increment() → c2 ka lock
// Dono threads parallel run kar sakte hain — different objects!
```

---

## volatile Keyword

**Matlab:** Variable ko main memory mein read/write karo — caching skip karo. Visibility guarantee deta hai.

### volatile Example

```java
class SharedFlag {
    volatile boolean running = true;  // ✅ volatile — visibility guaranteed
}

SharedFlag flag = new SharedFlag();

// Thread 1 — flag change karega
new Thread(() -> {
    try { Thread.sleep(100); } catch (InterruptedException e) {}
    flag.running = false;  // Main memory mein write hoga
    System.out.println("Flag set to false");
}).start();

// Thread 2 — flag check karega
new Thread(() -> {
    while (flag.running) {  // ✅ Main memory se read hoga — change dikhega!
        // Busy wait
    }
    System.out.println("Loop exited");  // ✅ Printed!
}).start();
```

### volatile vs synchronized

| Feature | volatile | synchronized |
|---------|----------|-------------|
| **Visibility** | ✅ Guarantees | ✅ Guarantees |
| **Atomicity** | ❌ No | ✅ Yes |
| **Locking** | ❌ No lock | ✅ Lock-based |
| **Performance** | Faster | Slower |
| **Use Case** | Single variable read/write | Compound operations |

### When volatile is NOT Enough

```java
class Counter {
    volatile int count = 0;

    public void increment() {
        count++;  // ❌ NOT atomic! Read → Modify → Write
        // Thread 1: read(5) → Thread 2: read(5) → T1: write(6) → T2: write(6)
        // Lost update! count should be 7, but is 6
    }
}

// ✅ Solution: synchronized use karo
class SafeCounter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // ✅ Atomic — sirf ek thread enter kar sakta hai
    }
}
```

### When to Use volatile

```java
// ✅ Use volatile for:
// - Single variable read/write
// - Flag/status variables
// - One-writer, multiple-readers

// ❌ Don't use volatile for:
// - Compound operations (read-modify-write)
// - Multiple variables ko coordinate karna
// - Atomic increment/decrement
```

---

## Happens-Before Relationship

**Matlab:** Java Memory Model — ek thread ka write dusre thread ko kab dikhega yeh guarantee karta hai.

### Happens-Before Rules

```
1. Program Order Rule: Thread ke andar, jo pehle likha hai woh pehle execute hoga
2. Monitor Lock Rule: Lock release → uske baad ka lock acquire
3. Volatile Variable Rule: volatile write → uske baad ka volatile read
4. Thread Start Rule: thread.start() → us thread ka run()
5. Thread Join Rule: Thread terminates → thread.join() return
6. Transitivity: A → B aur B → C toh A → C
```

### Monitor Lock Example

```java
class SharedData {
    private int value;

    public synchronized void set(int value) {
        this.value = value;  // Write
    }  // Lock release → happens-before

    public synchronized int get() {
        return value;  // Lock acquire → happens-after
    }
}

// Thread 1: set(42) → Lock release
// Thread 2: get() → Lock acquire → value = 42 guaranteed
```

### volatile Happens-Before

```java
class SharedConfig {
    volatile boolean initialized = false;
    int configValue;  // NOT volatile

    public void initialize() {
        configValue = 100;      // 1. Non-volatile write
        initialized = true;     // 2. volatile write → happens-before
    }

    public int getConfig() {
        if (initialized) {      // 3. volatile read → happens-after
            return configValue; // 4. Guaranteed to see 100!
        }
        return -1;
    }
}
```

### Why Happens-Before Matters

```
Without happens-before guarantee:
Thread 1: configValue = 100; initialized = true;
Thread 2: if (initialized) → configValue might still be 0!

With happens-before (volatile):
Thread 1: configValue = 100; volatile initialized = true;
Thread 2: if (volatile initialized) → configValue guaranteed to be 100!
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Memory Visibility** | Multiple threads alag-alag cache use karte hain |
| **synchronized** | Mutual exclusion + visibility guarantee |
| **Intrinsic Lock** | Har object ka built-in lock — `synchronized` use karta hai |
| **volatile** | Visibility guarantee — atomicity nahi |
| **Happens-Before** | Java Memory Model — writes kab visible honge |
| **Reentrant** | Same thread baar-baar same lock acquire kar sakta hai |
| **volatile vs synchronized** | volatile = visibility only, synchronized = visibility + atomicity |
