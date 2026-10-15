# Level 2: Multithreading Basics (Q93 - Q101)

---

### Q93: What is Multithreading in Java? Thread Lifecycle?
**Answer (Hinglish):**

- **Multithreading:** Ek hi process ke andar multiple threads ko simultaneously (concurrently) execute karne ki process ko multithreading kehte hain. Isse CPU cores ka maximum utilization hota hai aur application responsive banti hai.
- **Thread Lifecycle (6 States in `Thread.State`):**
  1. **NEW:** Thread object create hua (`new Thread()`), lekin abhi `start()` call nahi hua.
  2. **RUNNABLE:** `start()` call ho chuka hai; thread execution ke liye ready hai ya CPU core par execute ho raha hai.
  3. **BLOCKED:** Thread kisi synchronized monitor lock ka wait kar raha hai jo kisi doosre thread ne hold kar rakha hai.
  4. **WAITING:** Thread kisi doosre thread ke action ka indefinitely wait kar raha hai (`wait()`, `join()`).
  5. **TIMED_WAITING:** Thread specified time ke liye wait kar raha hai (`sleep(1000)`, `wait(5000)`).
  6. **TERMINATED:** Thread ka `run()` method complete ho gaya ya unhandled exception se terminate ho gaya.

---

### Q94 & Q101: Synchronization in Java (Method vs Block)
**Answer (Hinglish):**

- **Synchronization:** Jab multiple threads ek common shared mutable resource (variable/object) ko simultaneously modify karte hain, to **Race Condition** aati hai. Synchronization ek mechanism hai jo ensure karta hai ki ek time par sirf ek hi thread critical section me enter kare (Mutual Exclusion).

| Criteria | Synchronized Method | Synchronized Block |
| :--- | :--- | :--- |
| **Lock Scope** | Poore method par lock lagta hai (current instance `this` ya class lock). | Sirf specified block of code par lock lagta hai. |
| **Performance** | **Slower** (Poora method block hone ki wajah se baaki threads unnecessarily wait karte hain). | **Faster & Granular** (Sirf critical lines par lock hota hai, baaki method concurrently execute ho sakta hai). |
| **Lock Object** | Implicit hota hai (`this` for instance, `Class` for static). | Custom lock object pass kar sakte hain (`synchronized(customLock) { ... }`). |

**Best Practice Example:**
```java
public void updateBalance(double amount) {
    // Non-critical operations (no lock needed)
    validateAmount(amount);

    // Synchronized Block: Granular critical section
    synchronized (this) {
        this.balance += amount;
    }
}
```

---

### Q95: What is the difference between wait() and sleep()?
**Answer (Hinglish):**

| Feature | `wait()` | `sleep()` |
| :--- | :--- | :--- |
| **Origin Class** | `java.lang.Object` class ka method hai. | `java.lang.Thread` class ka static method hai. |
| **Lock Release?** | **Lock release kar deta hai** taaki doosra waiting thread lock le sake. | **Lock release NAHI karta** (Lock pakad kar sota hai). |
| **Condition to Wake Up**| `notify()`, `notifyAll()`, ya timeout expire hone par. | Specified time duration khatam hone par. |
| **Synchronization** | Sirf **synchronized block/method ke andar hi** call ho sakta hai (warna `IllegalMonitorStateException`). | Synchronized context ke bina kahin bhi call ho sakta hai. |

---

### Q96: What is the volatile keyword?
**Answer (Hinglish):**

- **Problem (CPU Caching & Visibility):**  
  Modern CPUs me har core ka apna L1/L2 cache hota hai. Jab Thread A kisi variable ko change karta hai, to wo turant main memory me write nahi hota, balki local CPU cache me rehta hai. Is wajah se doosre CPU core par chal raha Thread B purani (stale) value dekhta rehta hai.
- **Solution (`volatile`):**  
  Jab aap kisi variable ko `volatile` mark karte ho:
  1. Uski reads aur writes **directly Main Memory (RAM)** se hoti hain, local CPU cache se nahi.
  2. JVM memory me **Happens-Before relationship** guarantee hota hai.
- **Limitation:** `volatile` sirf **Visibility** guarantee karta hai, **Atomicity nahi** (Compound operations jaise `count++` thread-safe nahi hote; unke liye `AtomicInteger` ya `synchronized` chahiye).

---

### Q97: What is ThreadLocal?
**Answer (Hinglish):**

- **Definition:** `ThreadLocal` ek aisa class hai jo har ek individual thread ko us variable ki apni **private, isolated copy** provide karta hai. Ek thread jo value set karta hai, doosra thread use na to dekh sakta hai aur na hi modify kar sakta hai.
- **Real-World Spring Boot Use Cases:**
  1. **Security Context:** Spring Security user ka authentication context `SecurityContextHolder` me ThreadLocal ke through store karta hai.
  2. **Transaction Management:** Database connection current transaction thread ke sath bind rehta hai.
  3. **Correlation ID / Tracing:** Distributed logging me `MDC.put("requestId", uuid)` ThreadLocal me rehta hai.

> **⚠️ Memory Leak Warning in Tomcat/Web Servers:**  
> Web servers thread pool use karte hain (threads reuse hote hain). Isliye request complete hone par **`threadLocal.remove()`** call karna zaroori hai, warna purana thread doosre user ki request me purana data leak kar dega aur OOM error aayega!

---

### Q98: What is a Deadlock in Java? How can it be avoided?
**Answer (Hinglish):**

- **Deadlock:** Ek aisi situation jahan Thread 1 ne Resource A ko lock kar rakha hai aur Resource B ka wait kar raha hai, jabki Thread 2 ne Resource B ko lock kar rakha hai aur Resource A ka wait kar raha hai. Dono threads hamesha ke liye block ho jaate hain.

```
Thread 1 (Holds Lock A) ─── Waiting for ───> Lock B (Held by Thread 2)
Thread 2 (Holds Lock B) ─── Waiting for ───> Lock A (Held by Thread 1)
```

- **Deadlock Avoidance Strategies:**
  1. **Consistent Lock Ordering:** Sabhi threads me locks ko hamesha **same global sequence** me acquire karein (Pehle Lock A lo, fir Lock B lo).
  2. **Lock Timeout (`tryLock`):** `synchronized` ki jagah `ReentrantLock.tryLock(timeout)` use karein. Agar specified time me lock na mile to backoff karke release kar dein.
  3. **Avoid Nested Locks:** Ek lock pakad kar doosra lock acquire karne se bachein.

---

### Q99 & Q100: Thread vs Runnable vs Callable
**Answer (Hinglish):**

| Feature | `Thread` (Class) | `Runnable` (Interface) | `Callable<V>` (Interface) |
| :--- | :--- | :--- | :--- |
| **Type** | Class | Functional Interface | Functional Interface |
| **Method** | `run()` | `void run()` | `V call() throws Exception` |
| **Return Value** | Nothing (`void`) | Nothing (`void`) | **Generic value return karta hai (`V`)** |
| **Exception Handling** | Checked exceptions throw nahi kar sakta. | Checked exceptions throw nahi kar sakta. | **Checked exceptions directly throw kar sakta hai**. |
| **Executor Integration**| Not suitable. | `executor.submit(runnable)` | `Future<V> future = executor.submit(callable)` |
| **Multiple Inheritance**| Ek baar class inherit ho gayi to aur koi class extend nahi ho sakti. | Interface hai, isliye multiple interfaces implement ho sakte hain. | Interface hai, flexible implementation. |
