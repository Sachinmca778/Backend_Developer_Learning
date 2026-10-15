# Modern Java Features (2024-2025) & Reactive Programming (Q293 - Q306)

---

### Q293, Q294 & Q296: Virtual Threads vs Platform Threads (Project Loom - Java 21 LTS)
**Answer (Hinglish):**

Java 21 ka sabse bada game-changer feature **Virtual Threads (Project Loom)** hai.

| Feature | Platform Thread (Traditional) | Virtual Thread (Java 21+) |
| :--- | :--- | :--- |
| **OS Mapping** | 1:1 mapping with OS kernel thread. | **M:N mapping** (Lakhon Virtual Threads few Carrier OS threads par run hote hain). |
| **Memory Footprint** | Heavy (~1MB per thread stack). | **Ultra-Lightweight (~few KBs)**. Aap 10 lakh virtual threads bana sakte ho bina OOM ke! |
| **Context Switching**| OS Kernel switch (Expensive, high CPU latency). | JVM User-space switch (Extremely fast). |
| **Blocking I/O Cost**| Jab thread DB ya HTTP response ka wait karta hai, to OS thread block ho jata hai (Resources wasted). | Jab I/O block hota hai, JVM carrier OS thread se virtual thread ko detach ("unmount") kar deta hai, taaki OS thread doosra kaam kar sake! |

#### Creating Virtual Threads in Java 21:
```java
// ExecutorService with Virtual Threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    IntStream.range(0, 10_000).forEach(i -> {
        executor.submit(() -> {
            Thread.sleep(Duration.ofSeconds(1)); // Non-blocking in virtual threads!
            return i;
        });
    });
} // Auto-waits and closes executor
```

---

### Q295: When should you NOT use Virtual Threads?
**Answer (Hinglish):**

1. **CPU-Intensive Tasks:** Agar task pure computation (video encoding, cryptographic hashing, matrix multiplication) hai, to virtual threads koi faayda nahi denge; balki traditional thread pool / parallel streams better hain.
2. **Thread Pinning (`synchronized` limitation):**  
   Java 21 me agar aap `synchronized` block ke andar blocking I/O operation karte hain, to virtual thread underlying OS carrier thread se unmount nahi ho pata ("Pinned"). Solution: `synchronized` ki jagah `ReentrantLock` use karein.
3. **Don't Pool Virtual Threads:** Virtual threads ko pool karne ki galti na karein (`newFixedThreadPool` on virtual threads makes no sense). Har naye task ke liye naya virtual thread spawn hone ke liye design kiya gaya hai.

---

### Q297, Q298 & Q299: Modern Java Syntax Enhancements
**Answer (Hinglish):**

- **Pattern Matching for `instanceof` (Java 16+):**
  ```java
  // Pehle explicit casting karni padti thi
  if (obj instanceof String s) {
      System.out.println(s.toUpperCase()); // Direct access, no casting needed!
  }
  ```

- **Record Patterns in Switch Expressions (Java 21):**
  ```java
  record Point(int x, int y) {}

  static void print(Object obj) {
      switch (obj) {
          case Point(int x, int y) -> System.out.println("Point at: " + x + ", " + y);
          case String s -> System.out.println("String of length: " + s.length());
          default -> System.out.println("Unknown object");
      }
  }
  ```

- **Sequenced Collections (Java 21):**  
  Pehle `List`, `Deque`, `LinkedHashSet` me first/last element uthane ka koi common method nahi tha (`list.get(0)`, `list.get(list.size()-1)`).  
  Java 21 me unified interface aya:
  ```java
  List<String> list = List.of("A", "B", "C");
  String first = list.getFirst(); // "A"
  String last = list.getLast();   // "C"
  List<String> reversed = list.reversed();
  ```

---

### Q301, Q302 & Q306: Reactive Programming (Mono vs Flux) & Imperative vs Reactive
**Answer (Hinglish):**

- **Reactive Programming:** Asynchronous, non-blocking, event-driven data streams processing paradigm jo reactive stream specification (`Publisher`, `Subscriber`, `Subscription`) follow karta hai.
- **Spring WebFlux Core Types:**
  - **`Mono<T>`:** 0 ya 1 item emit karne wala asynchronous publisher (e.g. `Mono<User> getUserById(Long id)`).
  - **`Flux<T>`:** 0 se N items (stream of elements) emit karne wala publisher (e.g. `Flux<StockPrice> streamStockPrices()`).

| Feature | Imperative (Spring MVC) | Reactive (Spring WebFlux) |
| :--- | :--- | :--- |
| **Model** | Thread-per-request model (Tomcat). | Event Loop / Non-blocking I/O model (Netty). |
| **Concurrency** | Threads limit hoti hain (e.g. 200). Blocking I/O threads hold karta hai. | Chhote fixed threads pure load ko non-blocking handle karte hain. |
| **Debugging** | Stack trace straightforward aur sequential hota hai. | Stack trace complex hota hai (multiple async threads switch). |

---

### Q303: Backpressure in Reactive Streams
**Answer (Hinglish):**

- **The Problem:** Publisher bahut fast speed se data emit kar raha hai (10,000 events/sec), lekin Subscriber slow hai aur sirf 100 events/sec process kar sakta hai. Subscriber memory overflow / crash ho jayega.
- **Backpressure Mechanism:** Subscriber Publisher ko batata hai ki wo kitna data accept karne ke liye ready hai (`request(n)`).
- **Strategies:**
  1. `onBackpressureBuffer()`: Unprocessed data ko memory buffer me rakho.
  2. `onBackpressureDrop()`: Capacity full hone par latest data drop kar do.
  3. `onBackpressureLatest()`: Sirf sabse recent value retain karo, purani drop karo.

---

### Q304: When to use Reactive (WebFlux) vs Traditional Spring MVC?
**Answer (Hinglish):**

- **Choose Spring MVC (especially with Java 21 Virtual Threads):**  
  90% traditional enterprise CRUD applications, jahan SQL databases (JPA/Hibernate) use ho rahe hon. Code maintain karna aur debug karna simple rehta hai.
- **Choose Reactive (Spring WebFlux):**  
  Streaming architectures, real-time push notifications, high-frequency IoT pipelines, microservice API Gateways, jahan end-to-end non-blocking reactive DB drivers (`R2DBC`, Reactive Mongo) available hon.
