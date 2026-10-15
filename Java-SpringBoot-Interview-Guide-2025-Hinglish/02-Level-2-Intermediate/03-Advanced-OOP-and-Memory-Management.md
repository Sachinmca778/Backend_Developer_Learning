# Level 2: Advanced OOP & Memory Management (Q80 - Q92)

---

### Q81: What are Sealed Classes (Java 17)?
**Answer (Hinglish):**

- **Problem in Traditional Java:**  
  Pehle ya to class `final` hoti thi (kisi ko extend nahi karne deti thi) ya `public` hoti thi (koi bhi kahin se bhi extend kar sakta tha). Middle ground nahi tha.
- **Sealed Classes Solution (Java 17 LTS):**  
  Sealed class developer ko yeh specify karne ki freedom deti hai ki **sirf kaun si specific classes use extend ya implement kar sakti hain**.
- **Syntax:**
  ```java
  // Sirf Circle aur Rectangle hi Shape ko inherit kar sakte hain
  public sealed class Shape permits Circle, Rectangle {
  }

  // Permitted subclass ko final, sealed, ya non-sealed banana zaroori hai
  public final class Circle extends Shape {}
  public final class Rectangle extends Shape {}
  ```
- **Benefits:** Domain modeling me high security milti hai aur Java switch pattern matching ke saath exhaustive compile-time checking possible hoti hai.

---

### Q82 & Q83: Fail-Fast vs Fail-Safe Iterators & ConcurrentModificationException
**Answer (Hinglish):**

| Criteria | Fail-Fast Iterators | Fail-Safe (Non-Fail-Fast) Iterators |
| :--- | :--- | :--- |
| **Behavior** | Collection me traversal ke dauran koi structural change (add/remove) hone par **turant `ConcurrentModificationException`** throw kar deta hai. | Iteration ke dauran original collection me changes hone par bhi **kabhi exception throw nahi karta**. |
| **How it Works** | Internal counter `modCount` check karta hai. Agar iteration ke time `expectedModCount != modCount` hua, crash kar deta hai. | Original collection ki **Cloned / Snapshot copy** par iterate karta hai. |
| **Examples** | `ArrayList`, `HashSet`, `HashMap` ke iterators. | `CopyOnWriteArrayList`, `ConcurrentHashMap` ke iterators. |
| **Memory Overhead** | Low (Direct underlying data par iterate karta hai). | High (Naya snapshot copy memory me create hota hai write ke time). |

---

### Q84: Final vs Finally vs Finalize()?
**Answer (Hinglish):**

| Keyword / Method | Type | Purpose / Behavior |
| :--- | :--- | :--- |
| **`final`** | Keyword | - Variable par: Constant (value cannot change).<br>- Method par: Method overriding restrict karta hai.<br>- Class par: Class inheritance restrict karta hai. |
| **`finally`** | Block | `try-catch` ke baad guaranteed execute hone wala block jo resource cleanup ke kaam aata hai. |
| **`finalize()`** | Method | `Object` class ka method jo Garbage Collector object ko destroy karne se pehle invoke karta tha (Java 9 se **Deprecated** ho chuka hai). |

---

### Q86 & Q87: Shallow Copy vs Deep Copy (and Cloneable)
**Answer (Hinglish):**

- **Shallow Copy (Default in `clone()`):**  
  Yeh sirf primitive fields ki values copy karta hai. Lekin agar object ke andar koi doosra reference object (e.g. `Address` class) ho, to wo naya address create nahi karta balki **same reference address copy** kar deta hai.  
  *Result:* Agar clone object ne address badla, to original object ka address bhi badal jayega!

- **Deep Copy:**  
  Deep copy me original object ke sath-sath uske andar ke saare nested reference objects ka bhi **fresh naya object memory me create** kiya jata hai. Dono objects completely independent ho jate hain.

```java
// Deep Copy using Copy Constructor (Recommended way):
public class User {
    private String name;
    private Address address;

    public User(User other) {
        this.name = other.name;
        this.address = new Address(other.address.getCity(), other.address.getZip()); // New independent object
    }
}
```

---

### Q88 & Q91: Java Memory Management (Stack vs Heap Allocation)
**Answer (Hinglish):**

JVM memory mainly do primary areas me divide hoti hai:

```
+------------------------------------------------------------------+
|                           JVM MEMORY                             |
|  +---------------------------+   +-----------------------------+ |
|  |       STACK MEMORY        |   |        HEAP MEMORY          | |
|  | - Thread-specific (Private|   | - Shared across all threads | |
|  | - Method frames           |   | - Objects & instance vars   | |
|  | - Primitive local vars    |   | - String Constant Pool      | |
|  | - Object references       |   | - Managed by GC             | |
|  | - Super Fast (LIFO)       |   | - Slower allocation         | |
|  | - StackOverflowError      |   | - OutOfMemoryError          | |
|  +---------------------------+   +-----------------------------+ |
|  +-------------------------------------------------------------+ |
|  |             METASPACE (Native Memory - Class metadata)      | |
|  +-------------------------------------------------------------+ |
+------------------------------------------------------------------+
```

| Criteria | Stack Memory | Heap Memory |
| :--- | :--- | :--- |
| **What is Stored?** | Method call frames, primitive local variables, references to objects. | Actual Objects and JRE classes. |
| **Scope & Threading**| Har thread ka apna alag private Stack hota hai (Thread-safe). | Sabhi threads ek common Heap share karte hain (Not thread-safe). |
| **Lifecycle** | Jab method finish hota hai, stack frame turant pop/destroy ho jata hai. | Objects tab tak rehte hain jab tak Garbage Collector unhe reclaim na kare. |
| **Error When Full** | `java.lang.StackOverflowError` (e.g. infinite recursion). | `java.lang.OutOfMemoryError: Java heap space`. |

---

### Q89: What is Garbage Collection in Java? Types of GC?
**Answer (Hinglish):**

- **Garbage Collection (GC) Kya Hai:**  
  Java ka automatic memory management process jo Heap memory me se unreferenced (unreachable) objects ko identify karke delete karta hai aur memory reclaim karta hai.
- **Generational GC Concept:**
  - **Young Generation:** Naye objects `Eden` space me bante hain, survive karne par `Survivor (S0, S1)` me shift hote hain (Minor GC).
  - **Old (Tenured) Generation:** Lambe time tak zinda rehne wale objects yahan move hote hain (Major / Full GC).

- **Major Types of GC Collectors:**
  1. **Serial GC (`-XX:+UseSerialGC`):** Single-threaded, chhote batch apps ke liye.
  2. **Parallel GC (`-XX:+UseParallelGC`):** Multi-threaded throughput-focused collector.
  3. **G1 GC (Garbage-First) (`-XX:+UseG1GC`):** Large heap memory (>4GB) ke liye default collector (Java 9+). Low latency pauses provide karta hai.
  4. **ZGC (Z Garbage Collector) & Shenandoah:** Ultra-low latency GC (< 1ms pause times) jo multi-terabyte heaps ko handle karte hain.

---

### Q90: What is the finalize() method?
**Answer (Hinglish):**

- `finalize()` `java.lang.Object` class ka ek protected method tha jo GC object ko destroy karne se just pehle run karta tha taaki cleanup (e.g. file close) ho sake.
- **Kyu Deprecate Hua (Java 9+)?**  
  1. GC kab chalega iska koi fixed timing nahi hota (unpredictable).
  2. Iske chalne se performance drop hoti thi aur deadlocks ka risk rehta tha.
- **Alternative:** `AutoCloseable` interface aur `try-with-resources` block use karna chahiye.

---

### Q92: How to handle OutOfMemoryError (OOM)?
**Answer (Hinglish):**

Production me OOM handle karne ka systematic approach:

1. **JVM Arguments Enable Karein (Heap Dump On OOM):**
   ```bash
   -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/var/log/dumps/heapdump.hprof
   ```
2. **Analyze Dump File:** Eclipse MAT (Memory Analyzer Tool) ya VisualVM me `.hprof` file open karke **"Leak Suspects"** identify karein (kaun sa object 90% heap roke hue hai).
3. **Common Root Causes & Fixes:**
   - **Static Collections:** Static List ya Map jisme se items kabhi remove nahi ho rahe (Memory Leak). Fix: WeakHashMap ya eviction cache use karein.
   - **Unclosed Resources:** DB connections ya streams leak ho rahe hain. Fix: `try-with-resources`.
   - **Large Result Sets:** DB se bina pagination ke 10 lakh rows ek saath fetch karna (`findAll()`). Fix: Pagination (`PageRequest.of(...)`).
   - **Insufficient Heap Size:** Agar genuine high load hai, to JVM flags se heap size badhayein: `-Xms4g -Xmx4g`.
