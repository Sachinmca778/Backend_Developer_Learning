# Multithreading & Concurrency - Top 10 Interview Questions & Answers

> Covering: Thread Basics, Synchronization, java.util.concurrent, Concurrent Collections, Atomic Classes, Locks, volatile, Deadlock, and Thread-Safety Patterns

---

## Q1: Thread vs Runnable vs Callable — difference kya hai? Kaunsa prefer karte ho?

**Answer:**

Teeno Java mein concurrent tasks run karne ke tareeke hain, lekin use case alag hai.

### Comparison Table:

| Feature | Thread | Runnable | Callable |
|---------|--------|----------|----------|
| **Type** | Class (extend karna padta hai) | Functional interface | Functional interface |
| **Return Value** | ❌ No | ❌ No | ✅ Yes (`V call()`) |
| **Exception** | Unchecked only | Unchecked only | Checked + Unchecked |
| **Inheritance** | Single inheritance block | Kisi class ko extend kar sakte ho | Kisi class ko extend kar sakte ho |
| **Use With** | `thread.start()` | `new Thread(runnable).start()` | `ExecutorService.submit()` |
| **Recommended** | ❌ Avoid | ✅ Yes | ✅ Yes (with ExecutorService) |

### Thread — Extending Thread Class:

```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Running in: " + Thread.currentThread().getName());
    }
}

MyThread thread = new MyThread();
thread.start();  // ✅ New thread start hoga
// thread.run();  // ❌ Sirf main thread mein execute hoga — start() use karo!
```

### Runnable — Preferred Approach:

```java
class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Task running");
    }
}

// Lambda se bhi
Thread thread = new Thread(() -> System.out.println("Lambda thread"));
thread.start();

// Multiple threads same Runnable share kar sakte ho
Runnable task = () -> System.out.println("Shared task");
new Thread(task).start();
new Thread(task).start();
```

### Callable — Return Value + Exceptions:

```java
Callable<Integer> callable = () -> {
    Thread.sleep(1000);
    if (Math.random() > 0.5) {
        throw new IOException("Random failure");  // Checked exception allowed
    }
    return 42;
};

ExecutorService executor = Executors.newFixedThreadPool(2);
Future<Integer> future = executor.submit(callable);

try {
    Integer result = future.get();  // Blocks until complete
    System.out.println("Result: " + result);
} catch (ExecutionException e) {
    System.out.println("Failed: " + e.getCause().getMessage());
}
executor.shutdown();
```

**Short Interview Answer:**

Runnable prefer karo — single inheritance free rehta hai. Callable tab use karo jab return value ya checked exception handle karna ho, usually ExecutorService ke saath. Thread extend karna avoid karo.

---

## Q2: start() vs run() — difference kya hai? Thread lifecycle states?

**Answer:**

Yeh bahut common trick question hai — `start()` aur `run()` mein bada difference hai.

### start() vs run():

```java
Thread thread = new Thread(() -> {
    System.out.println("Thread: " + Thread.currentThread().getName());
});

// ✅ start() — naya OS thread create karta hai, run() JVM call karta hai
thread.start();  // Output: Thread: Thread-0

// ❌ run() — directly main thread mein execute hota hai, naya thread NAHI
thread.run();    // Output: Thread: main
```

| Method | New Thread? | Who Calls run()? | Use |
|--------|-------------|------------------|-----|
| `start()` | ✅ Yes | JVM (new thread) | Production code |
| `run()` | ❌ No | Directly (current thread) | Testing only |

### Thread Lifecycle States:

```
NEW → RUNNABLE → (BLOCKED / WAITING / TIMED_WAITING) → TERMINATED
```

| State | Description | Example |
|-------|-------------|---------|
| **NEW** | Created, not started | `new Thread(runnable)` |
| **RUNNABLE** | Running or ready to run | After `start()` |
| **BLOCKED** | Waiting for monitor lock | `synchronized` block enter karne ki koshish |
| **WAITING** | Indefinitely waiting | `object.wait()`, `thread.join()` |
| **TIMED_WAITING** | Waiting with timeout | `Thread.sleep(ms)`, `wait(timeout)` |
| **TERMINATED** | Completed | `run()` finished |

```java
Thread t = new Thread(() -> {
    try { Thread.sleep(2000); } catch (InterruptedException e) {}
});

System.out.println(t.getState());  // NEW
t.start();
System.out.println(t.getState());  // RUNNABLE (or TIMED_WAITING during sleep)
t.join();
System.out.println(t.getState());  // TERMINATED
```

**Important:**

`start()` sirf ek baar call kar sakte ho — dobara call pe `IllegalThreadException`. Daemon threads bhi `start()` se hi start hote hain.

---

## Q3: synchronized keyword kaise kaam karta hai? wait/notify/notifyAll explain karo.

**Answer:**

`synchronized` mutual exclusion provide karta hai — ek time pe sirf ek thread synchronized block/method mein enter kar sakta hai.

### synchronized Method:

```java
class Counter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // Read → Modify → Write — ab atomic hai
    }

    public synchronized int getCount() {
        return count;
    }
}

// 1000 threads bhi increment karein — result hamesha correct
Counter counter = new Counter();
Thread t1 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) counter.increment();
});
Thread t2 = new Thread(() -> {
    for (int i = 0; i < 1000; i++) counter.increment();
});
t1.start(); t2.start();
t1.join(); t2.join();
System.out.println(counter.getCount());  // 2000 ✅
```

### synchronized Block — Fine-Grained Locking:

```java
class BankAccount {
    private double balance;
    private final Object lock = new Object();

    public void deposit(double amount) {
        synchronized (lock) {  // Sirf is block pe lock
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
}
```

### wait/notify — Thread Communication:

```java
class MessageQueue {
    private final Object lock = new Object();
    private String message;
    private boolean hasMessage = false;

    public void produce(String msg) throws InterruptedException {
        synchronized (lock) {
            while (hasMessage) {
                lock.wait();  // Consumer consume kare tab tak wait
            }
            message = msg;
            hasMessage = true;
            lock.notify();  // Consumer ko batao
        }
    }

    public String consume() throws InterruptedException {
        synchronized (lock) {
            while (!hasMessage) {
                lock.wait();  // Producer produce kare tab tak wait
            }
            String msg = message;
            hasMessage = false;
            lock.notify();  // Producer ko batao
            return msg;
        }
    }
}
```

### wait vs notify vs notifyAll:

| Method | Kya Karta Hai | Important |
|--------|---------------|-----------|
| `wait()` | Lock release + wait queue mein jaata hai | synchronized block ke andar call karo |
| `notify()` | Ek waiting thread ko wake karo | Which thread — undefined |
| `notifyAll()` | Sab waiting threads ko wake karo | Safer — sab check karenge condition |

**Important:**

`wait()`/`notify()` hamesha `while` loop ke saath use karo, `if` nahi — spurious wakeup handle karne ke liye. `notifyAll()` generally safer hai `notify()` se.

**Short Interview Answer:**

`synchronized` ek time pe ek thread ko critical section mein allow karta hai using intrinsic monitor lock. `wait()` lock release karke wait karta hai, `notify()`/`notifyAll()` waiting threads ko wake karta hai — producer-consumer pattern ke liye use hota hai.

---

## Q4: ExecutorService kya hai? Thread pools kaise kaam karte hain?

**Answer:**

**ExecutorService** thread management ka framework hai — manually thread create karne ki jagah tasks submit karte ho, pool reuse karta hai.

### Problem Without ExecutorService:

```java
// ❌ 1000 tasks = 1000 threads — memory overflow!
for (int i = 0; i < 1000; i++) {
    new Thread(() -> process(i)).start();
}
```

### Solution With ExecutorService:

```java
// ✅ Fixed pool — sirf 10 threads, baaki queue mein wait
ExecutorService executor = Executors.newFixedThreadPool(10);

for (int i = 0; i < 1000; i++) {
    final int taskId = i;
    executor.submit(() -> process(taskId));
}

executor.shutdown();  // No new tasks
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();  // Force shutdown
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### Thread Pool Types:

| Pool Type | Behavior | Use Case |
|-----------|----------|----------|
| **FixedThreadPool(n)** | Fixed n threads, unbounded queue | Controlled concurrency |
| **CachedThreadPool** | Dynamic threads, 60s idle timeout | Short-lived tasks (⚠️ production mein careful) |
| **SingleThreadExecutor** | Ek thread, FIFO order | Sequential processing |
| **ScheduledThreadPool** | Delay/periodic execution | Cron-like tasks |

```java
// Scheduled tasks
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

scheduler.schedule(() -> System.out.println("Once after 5 sec"), 5, TimeUnit.SECONDS);
scheduler.scheduleAtFixedRate(() -> System.out.println("Every 10 sec"), 0, 10, TimeUnit.SECONDS);
```

### Future<T> — Async Result:

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

Future<Integer> future = executor.submit(() -> {
    Thread.sleep(2000);
    return 42;
});

// Do other work while waiting...
Integer result = future.get(3, TimeUnit.SECONDS);  // Timeout ke saath
System.out.println("Result: " + result);

executor.shutdown();
```

**Short Interview Answer:**

ExecutorService thread pool manage karta hai — manually threads create karne se better performance aur control milta hai. `submit()` se Future milta hai, `shutdown()`/`awaitTermination()` se gracefully band karte hain.

---

## Q5: CompletableFuture vs Future — kya difference hai?

**Answer:**

**Future** basic async result deta hai — blocking `get()` se result milta hai.  
**CompletableFuture** non-blocking chaining, composition, aur exception handling support karta hai.

### Future Limitations:

```java
Future<String> future = executor.submit(() -> fetchData());

// ❌ Future — sirf blocking get()
String result = future.get();  // Blocks

// ❌ Chain nahi kar sakte
// ❌ Exception handling limited
// ❌ Multiple futures combine karna mushkil
```

### CompletableFuture — Chaining:

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchUserId())           // Async supply
    .thenApply(id -> fetchUserName(id))         // Transform result
    .thenApply(name -> "Hello, " + name)        // Chain another transform
    .exceptionally(ex -> "Guest");              // Handle error

String greeting = future.join();  // Non-blocking alternative to get()
System.out.println(greeting);  // "Hello, Sachin"
```

### Key Methods:

| Method | Input → Output | Use |
|--------|----------------|-----|
| `supplyAsync()` | () → T | Async task with return value |
| `runAsync()` | () → void | Async task without return |
| `thenApply(fn)` | T → U | Transform result (sync) |
| `thenCompose(fn)` | T → CompletableFuture<U> | Flatten nested futures |
| `thenAccept(consumer)` | T → void | Consume result |
| `exceptionally(fn)` | Exception → T | Error recovery |
| `allOf(futures...)` | Multiple → Void | Wait for all |
| `anyOf(futures...)` | Multiple → Object | First to complete |

```java
// Combine two async results
CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> "Sachin");
CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> 25);

CompletableFuture<String> combined = userFuture.thenCombine(ageFuture,
    (name, age) -> name + " is " + age + " years old");

System.out.println(combined.join());  // "Sachin is 25 years old"
```

**Short Interview Answer:**

Future blocking hai aur chain nahi ho sakta. CompletableFuture non-blocking composition deta hai — `thenApply`, `thenCompose`, `exceptionally`, `allOf`/`anyOf` se complex async workflows build kar sakte ho.

---

## Q6: ConcurrentHashMap kaise thread-safe hai? HashMap vs synchronizedMap vs ConcurrentHashMap?

**Answer:**

**ConcurrentHashMap** fine-grained locking use karta hai — poori map pe ek lock nahi, sirf specific bucket pe lock lagta hai.

### Comparison:

| Feature | HashMap | synchronizedMap | ConcurrentHashMap |
|---------|---------|-----------------|-------------------|
| **Thread-Safe** | ❌ No | ✅ Yes | ✅ Yes |
| **Locking** | None | Single lock (whole map) | Bin-level / CAS |
| **Read Performance** | Fast | Slow (serialized) | Fast (lock-free reads) |
| **Null Keys/Values** | Allowed | Allowed | ❌ Not allowed |
| **Iterator** | Fail-fast | Fail-fast | Weakly consistent |

### How ConcurrentHashMap Works (Java 8+):

```
Read operations:  Lock-free (volatile reads)
Write operations: CAS + synchronized on specific bin (bucket)
                  Sirf us bucket pe lock — baaki buckets free
```

### Basic Usage:

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

map.put("Apple", 1);
map.putIfAbsent("Banana", 2);   // Atomic — add only if absent
map.compute("Apple", (k, v) -> v == null ? 1 : v + 1);  // Atomic update
map.merge("Banana", 1, Integer::sum);  // Atomic merge

// Thread-safe iteration — no ConcurrentModificationException
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    map.put("Cherry", 3);  // ✅ Safe during iteration
}
```

### Word Count Example (Atomic):

```java
ConcurrentHashMap<String, LongAdder> wordCounts = new ConcurrentHashMap<>();

String[] words = {"hello", "world", "hello", "java", "world", "hello"};

Arrays.stream(words).parallel().forEach(word ->
    wordCounts.computeIfAbsent(word, k -> new LongAdder()).increment()
);

wordCounts.forEach((word, count) ->
    System.out.println(word + ": " + count.sum())
);
// hello: 3, world: 2, java: 1
```

**Important:**

High-contention counters ke liye `AtomicInteger` se better `LongAdder` use karo — internally cells distribute karta hai contention kam karne ke liye.

---

## Q7: AtomicInteger aur CAS kya hai? synchronized se kaise better hai?

**Answer:**

**Atomic classes** lock-free thread-safe operations provide karte hain using **CAS (Compare-And-Swap)** — hardware-level atomic instruction.

### Problem — count++ Not Atomic:

```java
// ❌ count++ = Read → Modify → Write (3 steps — race condition!)
class UnsafeCounter {
    private int count = 0;
    public void increment() { count++; }
}
// 2 threads, 1000 increments each → result < 2000 possible!
```

### synchronized Solution:

```java
class SyncCounter {
    private int count = 0;
    public synchronized void increment() { count++; }
    // Works but locking overhead
}
```

### AtomicInteger — Lock-Free:

```java
class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet();  // CAS — no lock!
    }

    public int get() { return count.get(); }
}

// Usage
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();           // ++count, return new
counter.getAndIncrement();           // count++, return old
counter.addAndGet(5);                // count += 5
counter.compareAndSet(10, 20);       // if count==10, set to 20
counter.updateAndGet(n -> n * 2);    // Functional update
```

### CAS (Compare-And-Swap):

```
1. Read current value (e.g., count = 5)
2. Calculate new value (e.g., 6)
3. CAS: "If current still 5, set to 6"
   - Success → Done
   - Fail (another thread changed it) → Retry from step 1
```

| Approach | Locking | Performance | Use Case |
|----------|---------|-------------|----------|
| **synchronized** | Yes (blocking) | Good | Complex critical sections |
| **AtomicInteger** | No (CAS) | Better (low contention) | Simple counters/flags |
| **LongAdder** | No (striped cells) | Best (high contention) | High-frequency counters |

```java
// High contention — LongAdder better than AtomicLong
LongAdder adder = new LongAdder();
IntStream.range(0, 1000).parallel().forEach(i -> adder.increment());
System.out.println(adder.sum());  // 1000
```

**Short Interview Answer:**

AtomicInteger uses CAS for lock-free thread-safe increments. Better than synchronized for simple operations like counters. High contention pe LongAdder use karo.

---

## Q8: ReentrantLock vs synchronized — kab kaunsa use karte ho?

**Answer:**

Dono mutual exclusion provide karte hain, lekin ReentrantLock zyada flexible hai.

### Comparison:

| Feature | synchronized | ReentrantLock |
|---------|-------------|---------------|
| **Lock/Unlock** | Automatic | Manual (try-finally) |
| **Timeout** | ❌ No | ✅ `tryLock(timeout)` |
| **Interruptible** | ❌ No | ✅ `lockInterruptibly()` |
| **Fairness** | ❌ No guarantee | ✅ Optional fair lock |
| **Multiple Conditions** | ❌ Single wait set | ✅ Multiple Condition objects |
| **Performance** | Similar (Java 6+) | Similar |
| **Safety** | Automatic unlock | Manual — forget risk |

### synchronized:

```java
class SyncCounter {
    private int count = 0;

    public synchronized void increment() {
        count++;
    }
}
```

### ReentrantLock:

```java
class LockCounter {
    private final ReentrantLock lock = new ReentrantLock(true);  // fair = true
    private int count = 0;

    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();  // ALWAYS in finally!
        }
    }

    public boolean tryIncrement() {
        if (lock.tryLock(2, TimeUnit.SECONDS)) {  // Timeout support
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;  // Couldn't acquire lock
    }
}
```

### ReadWriteLock — Read-Heavy Scenarios:

```java
class CachedData {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Map<String, String> cache = new HashMap<>();

    public String get(String key) {
        rwLock.readLock().lock();  // Multiple readers allowed
        try {
            return cache.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void put(String key, String value) {
        rwLock.writeLock().lock();  // Exclusive write
        try {
            cache.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

**Important:**

Default mein `synchronized` prefer karo — simpler aur safer. ReentrantLock tab use karo jab timeout, fairness, interruptible lock, ya ReadWriteLock chahiye.

---

## Q9: volatile keyword kya karta hai? Deadlock kaise hota hai aur kaise avoid karein?

**Answer:**

### volatile — Visibility Guarantee:

`volatile` ensure karta hai ki ek thread ka write dusre threads ko immediately visible ho — CPU cache issue solve karta hai.

```java
class SharedFlag {
    volatile boolean running = true;  // ✅ Visibility guaranteed
}

// Thread 1
new Thread(() -> {
    flag.running = false;  // Write visible to all threads
}).start();

// Thread 2
new Thread(() -> {
    while (flag.running) { /* busy wait */ }
    System.out.println("Loop exited");  // ✅ Will print
}).start();
```

### volatile vs synchronized:

| Feature | volatile | synchronized |
|---------|----------|-------------|
| **Visibility** | ✅ Yes | ✅ Yes |
| **Atomicity** | ❌ No (compound ops unsafe) | ✅ Yes |
| **Mutual Exclusion** | ❌ No | ✅ Yes |
| **Use Case** | Single variable flag/status | Critical sections |

```java
// ❌ volatile does NOT make count++ atomic!
volatile int count = 0;
count++;  // Still race condition — use AtomicInteger instead
```

### Deadlock — Circular Wait:

```java
// ❌ Classic deadlock
Object lockA = new Object();
Object lockB = new Object();

Thread t1 = new Thread(() -> {
    synchronized (lockA) {
        sleep(100);
        synchronized (lockB) { /* work */ }  // Waiting for lockB
    }
});

Thread t2 = new Thread(() -> {
    synchronized (lockB) {
        sleep(100);
        synchronized (lockA) { /* work */ }  // Waiting for lockA
    }
});
// DEADLOCK — t1 has A wants B, t2 has B wants A
```

### Deadlock Prevention:

| Strategy | How |
|----------|-----|
| **Lock Ordering** | Hamesha same order mein locks lo (A then B) |
| **tryLock with Timeout** | Deadlock detect + retry/backoff |
| **Avoid Nested Locks** | Ek time pe ek lock |
| **Use Concurrent Collections** | Lock-free alternatives |
| **Lock Timeout** | `tryLock(timeout)` use karo |

```java
// ✅ Lock ordering — always A before B
synchronized (lockA) {
    synchronized (lockB) {
        // Both threads same order → no deadlock
    }
}
```

**Short Interview Answer:**

volatile visibility guarantee deta hai but atomicity nahi. Deadlock tab hota hai jab threads circular wait mein hain — fix: consistent lock ordering, tryLock with timeout, aur concurrent utilities use karo.

---

## Q10: Thread-safety patterns kya hain? Immutability, ThreadLocal explain karo.

**Answer:**

Thread safety achieve karne ke multiple patterns hain — best approach problem pe depend karta hai.

### 1. Immutability — Simplest & Safest:

```java
// ✅ Immutable class — thread-safe by default
public final class User {
    private final String name;
    private final int age;
    private final List<String> roles;

    public User(String name, int age, List<String> roles) {
        this.name = name;
        this.age = age;
        this.roles = Collections.unmodifiableList(new ArrayList<>(roles));  // Defensive copy
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public List<String> getRoles() { return new ArrayList<>(roles); }  // Defensive copy
}
```

**Rules:** final class, final fields, no setters, defensive copy for mutable fields.

### 2. ThreadLocal — Per-Thread State:

```java
// Har thread ka apna copy — no sharing, no synchronization
private static ThreadLocal<SimpleDateFormat> dateFormat =
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

// Thread 1
dateFormat.get().format(new Date());  // Thread 1 ka formatter

// Thread 2
dateFormat.get().format(new Date());  // Thread 2 ka alag formatter

// ⚠️ Cleanup important — memory leak in thread pools!
try {
    dateFormat.get().format(new Date());
} finally {
    dateFormat.remove();  // Thread pool mein zaroori
}
```

### 3. Producer-Consumer (BlockingQueue):

```java
BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);

// Producer
executor.submit(() -> {
    queue.put(new Task("data"));  // Blocks if full
});

// Consumer
executor.submit(() -> {
    Task task = queue.take();  // Blocks if empty
    process(task);
});
```

### 4. Double-Checked Locking — Anti-Pattern (Old):

```java
// ❌ Broken without volatile (pre-Java 5)
// ✅ Fixed with volatile
private volatile Singleton instance;

public Singleton getInstance() {
    if (instance == null) {
        synchronized (this) {
            if (instance == null) {
                instance = new Singleton();
            }
        }
    }
    return instance;
}

// ✅ Better — use enum or static holder
public enum Singleton { INSTANCE; }
```

### Thread Safety Checklist:

| Pattern | When to Use | Pros | Cons |
|---------|-------------|------|------|
| **Immutability** | Shared read-only data | Simplest, no sync | Can't modify |
| **ThreadLocal** | Per-thread state (user context, formatter) | No sharing issues | Memory leak risk in pools |
| **Synchronization** | Simple critical sections | Easy | Performance bottleneck |
| **Concurrent Collections** | Shared maps/lists/queues | Optimized | API differences |
| **Atomic Classes** | Counters, flags | Lock-free | Limited operations |
| **Locks** | Complex coordination | Flexible | Deadlock risk |

**Short Interview Answer:**

Best thread-safety: immutability (no sync needed). ThreadLocal for per-thread state. Concurrent collections + atomic classes for shared mutable state. synchronized/ReentrantLock for complex critical sections. Always prefer higher-level abstractions over raw threads.
