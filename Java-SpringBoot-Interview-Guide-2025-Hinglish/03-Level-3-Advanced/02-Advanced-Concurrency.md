# Level 3: Advanced Concurrency (Q157 - Q174)

---

### Q157, Q158 & Q159: Executor Framework, ThreadPool & Future
**Answer (Hinglish):**

- **Problem with `new Thread().start()`:** Har thread OS-level native thread hota hai. Naya thread banana expensive hai (1MB memory stack + CPU context switching overhead). Agar 10,000 requests par 10,000 threads banayein to server crash ho jayega.
- **ThreadPool Concept:** Pre-created worker threads ka ek pool hota hai jo tasks ko internal queue se utha kar execute karta hai aur terminate hone ki jagah wapas pool me wait karta hai.
- **Executor Framework (`java.util.concurrent`):**
  - Task submission aur task execution ko decouple karta hai.
  - Types: `FixedThreadPool`, `CachedThreadPool`, `ScheduledThreadPool`, `SingleThreadExecutor`.
- **`Future<V>`:** Asynchronous task ke completion aur result ko represent karta hai. Methods: `get()` (blocking), `isDone()`, `cancel()`.

```java
ExecutorService executor = Executors.newFixedThreadPool(5);
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "Payment Processed";
});
String result = future.get(); // Blocks until result is ready
executor.shutdown();
```

---

### Q160: ReentrantLock vs Synchronized
**Answer (Hinglish):**

| Feature | `synchronized` (Keyword) | `ReentrantLock` (API Class) |
| :--- | :--- | :--- |
| **Control** | Implicit, automatic lock acquire & release. | Explicit code: `lock.lock()` aur `lock.unlock()` in finally block. |
| **Fairness Policy** | Unfair (koi bhi thread lock chheen sakta hai). | Fair lock option: `new ReentrantLock(true)` (FIFO order me lock milta hai). |
| **Timed Locking** | Possible nahi hai (Thread indefinitely block rehta hai). | `lock.tryLock(5, TimeUnit.SECONDS)` se timeout handle ho sakta hai. |
| **Interruptibility** | Waiting thread ko interrupt nahi kiya ja sakta. | `lock.lockInterruptibly()` se interruptible wait possible hai. |
| **Condition Variables**| Sirf 1 wait-set (`wait()`, `notify()`). | Multiple conditions support: `lock.newCondition()`. |

---

### Q161 & Q162: Volatile vs Synchronized & Thread Safety vs Atomicity
**Answer (Hinglish):**

- **Volatile vs Synchronized:**
  - `volatile`: Sirf variable visibility solve karta hai (Direct RAM read/write). No locks, lightweight.
  - `synchronized`: Mutual exclusion (Locking) + Visibility dono provide karta hai. Heavy.
- **Thread Safety vs Atomicity:**
  - **Atomicity:** Ek operation ya to poori tarah complete hoga ya bilkul nahi hoga (All-or-Nothing). Example: `count++` 3 operations hain (Read -> Increment -> Write). Agar do threads ek sath chalte hain to non-atomic hone ki wajah se data loss hota hai.
  - **Thread Safety:** Ek broader property hai jisme multiple threads concurrently data structure ko access karte hain aur program ka behavior hamesha consistent aur correct rehta hai.

---

### Q163: CountDownLatch vs CyclicBarrier
**Answer (Hinglish):**

| Feature | `CountDownLatch` | `CyclicBarrier` |
| :--- | :--- | :--- |
| **Concept** | Ek ya zyada threads wait karte hain jab tak doosre threads N tasks complete na kar lein. | Multiple threads ek barrier point par aakar ek doosre ka wait karte hain. |
| **Reusability** | **Non-reusable** (Count zero hone ke baad dobara reset nahi ho sakta). | **Reusable** (Sabhi threads barrier cross karne ke baad count automatically reset ho jata hai). |
| **Operations** | `countDown()` count ghatata hai, `await()` wait karta hai. | Har thread `await()` call karta hai jab tak sabhi pahunch na jayein. |
| **Real Analogy** | **Rocket Launch:** 5 systems check pass hone chahiye countdown zero hone ke liye. | **Friends trip / Picnic:** Sabhi 4 dost metro station pahuchenge tabhi aage train chalegi. |

---

### Q165: How CompletableFuture Works Internally
**Answer (Hinglish):**

- `CompletableFuture<T>` Java 8 me introduce hua tha non-blocking, asynchronous reactive pipelines banane ke liye (Promise pattern).
- **Internal Mechanism:**
  - Har CF instance ek internal **Completion Dependency Stack / Treiber Stack** maintain karta hai.
  - Jab aap `.thenApply()` ya `.thenCompose()` likhte hain, to wo action CF ke completion queue me register ho jati hai.
  - Default execution `ForkJoinPool.commonPool()` par chalti hai (ya custom executor pass kar sakte hain).
  - Jaise hi parent task complete hota hai, wo callback stack ko pop karke dependent downstream stages ko cascade fire karta hai bina caller thread ko block kiye!

```java
CompletableFuture.supplyAsync(() -> fetchOrderFromDb(orderId))
    .thenApplyAsync(order -> calculateDiscount(order))
    .thenAccept(discountedOrder -> sendEmailNotification(discountedOrder));
```

---

### Q167: How to write a Thread-Safe Singleton (Double-Checked Locking)
**Answer (Hinglish):**

Production grade thread-safe singleton with high performance:

```java
public class DatabaseConnectionPool {
    // 1. volatile keyword zaroori hai CPU instruction reordering prevent karne ke liye!
    private static volatile DatabaseConnectionPool instance;

    // 2. Private constructor
    private DatabaseConnectionPool() {
        if (instance != null) {
            throw new RuntimeException("Use getInstance() method!"); // Reflection attack safety
        }
    }

    // 3. Double-checked locking
    public static DatabaseConnectionPool getInstance() {
        if (instance == null) { // 1st check (No locking overhead after initialization)
            synchronized (DatabaseConnectionPool.class) {
                if (instance == null) { // 2nd check (Within lock)
                    instance = new DatabaseConnectionPool();
                }
            }
        }
        return instance;
    }
}
```

---

### Q168 & Q174: Producer-Consumer Pattern using BlockingQueue
**Answer (Hinglish):**

`BlockingQueue` thread-safe queue hai jo automatic blocking provide karti hai:
- Agar queue FULL hai, to `put()` thread ko wait karata hai jab tak space khali na ho.
- Agar queue EMPTY hai, to `take()` thread ko wait karata hai jab tak naya item na aaye.

```java
BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

// Producer Thread
Runnable producer = () -> {
    try {
        while (true) {
            String message = generateLog();
            queue.put(message); // Blocks if queue is full
        }
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
};

// Consumer Thread
Runnable consumer = () -> {
    try {
        while (true) {
            String message = queue.take(); // Blocks if queue is empty
            processLog(message);
        }
    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
};
```

---

### Q169 & Q170: ForkJoinPool & Parallel Streams Internals
**Answer (Hinglish):**

- **ForkJoinPool:** Work-stealing algorithm par kaam karta hai. Badi computational problems ko chhote sub-tasks me divide (`fork`) karta hai aur results ko combine (`join`) karta hai.
  - **Work Stealing:** Har worker thread ka apna double-ended queue (deque) hota hai. Agar koi thread apna kaam pehle finish kar leta hai, to wo idle rehne ki jagah doosre busy worker ke deque ke tail se task **steal (chura)** leta hai.
- **Parallel Streams:** Internally `ForkJoinPool.commonPool()` share karte hain. CPU-bound calculations (jaise sorting 10 million numbers) me parallel streams fast hote hain, lekin I/O-bound ya blocking operations me common pool chok hone ka khatra rehta hai.

---

### Q171, Q172 & Q173: Live-lock, JMM & Semaphore
**Answer (Hinglish):**

- **Live-lock vs Deadlock:**
  - **Deadlock:** Threads `BLOCKED` state me hote hain, koi CPU consume nahi hota, freeze rehte hain.
  - **Live-lock:** Threads active hote hain aur bar-bar ek doosre ko rasta dene ke liye apni state continuously change karte rehte hain (e.g. do log corridor me aamne-saamne aakar ek hi taraf side hote rehte hain). CPU 100% chalta hai par koi actual progress nahi hoti.
- **Java Memory Model (JMM):** Specification jo define karta hai ki kab ek thread ka memory write operation doosre threads ko accurately visible hoga (Happens-Before rules).
- **Semaphore:** Counter-based concurrency control. Ye permit-based access deta hai.
  - Example: `new Semaphore(5)` ka matlab ek time par maximum **5 threads** hi critical resource (jaise DB connection pool ya rate limiter) access kar sakte hain.
