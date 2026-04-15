# Thread Basics

## Status: Not Started

---

## Table of Contents

1. [Thread Class & Runnable Interface](#thread-class--runnable-interface)
2. [Thread Lifecycle](#thread-lifecycle)
3. [Thread Priority](#thread-priority)
4. [Daemon Threads](#daemon-threads)
5. [Thread Naming](#thread-naming)

---

## Thread Class & Runnable Interface

**Matlab:** Java mein concurrent execution — ek se zyada tasks parallel mein run karna.

### Creating Thread — Extending Thread Class

```java
class MyThread extends Thread {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("MyThread: " + i);
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
    }
}

// Usage
MyThread thread = new MyThread();
thread.start();  // Thread start karo — run() automatically call hoga
```

### Creating Thread — Implementing Runnable

```java
class MyTask implements Runnable {
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println("MyTask: " + i);
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
    }
}

// Usage
Thread thread = new Thread(new MyTask());
thread.start();
```

### Using Lambda

```java
// Lambda se Runnable banao
Thread thread = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("Lambda Thread: " + i);
        try { Thread.sleep(100); } catch (InterruptedException e) {}
    }
});
thread.start();
```

### Thread Class vs Runnable

| Feature | Extends Thread | Implements Runnable |
|---------|---------------|-------------------|
| **Inheritance** | Single inheritance limit | Kisi bhi class ko extend kar sakta hai |
| **Reusability** | Kam flexible | Zyada flexible — multiple threads mein use |
| **Recommended** | ❌ Avoid | ✅ Yes |

```java
// ✅ Runnable preferred — inheritance free
class MyService implements Runnable {
    private final String name;

    public MyService(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        System.out.println("Service " + name + " running");
    }
}
```

---

## Thread Lifecycle

**Matlab:** Thread ka journey — creation se termination tak.

### Thread States

```
NEW → RUNNABLE → (BLOCKED / WAITING / TIMED_WAITING) → TERMINATED
```

### State Descriptions

| State | Description | Example |
|-------|-------------|---------|
| **NEW** | Thread create hua, start nahi hua | `new Thread(runnable)` |
| **RUNNABLE** | Running ya ready to run | `thread.start()` ke baad |
| **BLOCKED** | Lock ke liye wait kar raha hai | synchronized block enter karne ki koshish |
| **WAITING** | Indefinitely wait kar raha hai | `object.wait()`, `thread.join()` |
| **TIMED_WAITING** | Time ke saath wait kar raha hai | `Thread.sleep(ms)`, `wait(timeout)` |
| **TERMINATED** | Thread complete ho gaya | run() method complete |

### State Transitions

```java
Thread thread = new Thread(() -> {
    System.out.println("Running");
});

// NEW state
System.out.println(thread.getState());  // NEW

// Start karo → RUNNABLE
thread.start();
System.out.println(thread.getState());  // RUNNABLE

// Sleep → TIMED_WAITING
Thread thread2 = new Thread(() -> {
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
});
thread2.start();
System.out.println(thread2.getState());  // TIMED_WAITING

// Join → WAITING
Thread mainThread = Thread.currentThread();
Thread thread3 = new Thread(() -> {
    try { thread.join(); } catch (InterruptedException e) {}
});
thread3.start();
System.out.println(thread3.getState());  // WAITING (ya TIMED_WAITING)

// Complete → TERMINATED
thread.join();
System.out.println(thread.getState());  // TERMINATED
```

### Blocking Example

```java
class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }

    public synchronized int getCount() {
        return count;
    }
}

// Two threads trying to access synchronized method
Counter counter = new Counter();

Thread t1 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        counter.increment();  // Lock lena padega
    }
});

Thread t2 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) {
        counter.increment();  // Lock lena padega — BLOCKED ho sakta hai
    }
});

t1.start();
t2.start();
```

---

## Thread Priority

**Matlab:** Thread scheduler ko hint dena ki kaunsa thread zyada important hai.

### Priority Range

```java
Thread.MIN_PRIORITY     // 1
Thread.NORM_PRIORITY    // 5 (default)
Thread.MAX_PRIORITY     // 10
```

### Setting Priority

```java
Thread lowPriority = new Thread(() -> {
    System.out.println("Low priority thread");
});
lowPriority.setPriority(Thread.MIN_PRIORITY);  // 1

Thread highPriority = new Thread(() -> {
    System.out.println("High priority thread");
});
highPriority.setPriority(Thread.MAX_PRIORITY);  // 10

lowPriority.start();
highPriority.start();
```

### ⚠️ Priority Warning

```
Priority sirf HINT hai — guarantee nahi
OS thread scheduler actually decide karta hai
Different OS pe different behavior ho sakta hai
```

---

## Daemon Threads

**Matlab:** Background threads jo JVM ke liye support kaam karte hain — JVM exit hone pe automatically terminate ho jaate hain.

### Daemon vs User Thread

```java
// User thread — JVM wait karega jab tak complete na ho
Thread userThread = new Thread(() -> {
    try { Thread.sleep(5000); } catch (InterruptedException e) {}
    System.out.println("User thread done");
});
userThread.start();

// Daemon thread — JVM wait nahi karega
Thread daemonThread = new Thread(() -> {
    try { Thread.sleep(5000); } catch (InterruptedException e) {}
    System.out.println("Daemon thread done");  // ❌ Print nahi hoga
});
daemonThread.setDaemon(true);  // MUST be set before start()
daemonThread.start();

// Main thread complete → JVM exit
// User thread complete hone tak JVM wait karega
// Daemon thread ko JVM ignore karega
```

### Common Daemon Threads

```java
// JVM ke internal daemon threads:
// - Garbage Collector
// - Finalizer
// - Signal Dispatcher
// - Reference Handler

// Check if daemon
Thread current = Thread.currentThread();
System.out.println(current.isDaemon());  // false (main thread is user thread)
```

### When to Use Daemon Threads

```java
// ✅ Use daemon for:
// - Background monitoring
// - Cache cleanup
// - Heartbeat signals
// - Log rotation

// ❌ Don't use daemon for:
// - Critical business logic
// - Data persistence
// - File operations that must complete
```

---

## Thread Naming

**Matlab:** Thread ko meaningful name dena — debugging aur monitoring ke liye.

### Setting Thread Name

```java
// Constructor mein
Thread thread1 = new Thread(() -> {
    System.out.println("Running");
}, "Worker-Thread-1");

// Ya setName se
Thread thread2 = new Thread(() -> {
    System.out.println("Running");
});
thread2.setName("Worker-Thread-2");

thread1.start();
thread2.start();
```

### Getting Current Thread Info

```java
Thread current = Thread.currentThread();
System.out.println("Name: " + current.getName());       // "main"
System.out.println("ID: " + current.getId());           // 1
System.out.println("Priority: " + current.getPriority()); // 5
System.out.println("State: " + current.getState());     // RUNNABLE
System.out.println("Daemon: " + current.isDaemon());    // false
```

### Naming Best Practices

```java
// ✅ Meaningful names — debugging easy
Thread paymentProcessor = new Thread(this::processPayments, "PaymentProcessor");
Thread emailSender = new Thread(this::sendEmails, "EmailSender");
Thread reportGenerator = new Thread(this::generateReport, "ReportGenerator");

// ❌ Generic names — debugging mushkil
Thread t1 = new Thread(this::processPayments);
Thread t2 = new Thread(this::sendEmails);

// ✅ Thread factory se consistent naming
ThreadFactory factory = new ThreadFactory() {
    private int count = 0;

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, "MyApp-Worker-" + count++);
        t.setDaemon(false);
        return t;
    }
};
```

### Stack Trace mein Thread Name

```
"PaymentProcessor" #12 prio=5 os_prio=0 cpu=123.45ms elapsed=456.78s
    at com.example.PaymentService.process(PaymentService.java:42)
    at com.example.PaymentService.run(PaymentService.java:28)
    at java.lang.Thread.run(Thread.java:834)
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Thread Creation** | `new Thread(runnable)` ya `extends Thread` |
| **Runnable Preferred** | Inheritance free, reusable |
| **NEW** | Thread created but not started |
| **RUNNABLE** | Running or ready to run |
| **BLOCKED** | Waiting for lock |
| **WAITING** | Indefinite wait (`wait()`, `join()`) |
| **TIMED_WAITING** | Timed wait (`sleep()`, `wait(timeout)`) |
| **TERMINATED** | Thread complete |
| **Priority** | 1 (MIN) to 10 (MAX), default 5 — sirf hint |
| **Daemon** | Background thread — JVM exit pe terminate |
| **Naming** | `setName()` — debugging ke liye essential |
