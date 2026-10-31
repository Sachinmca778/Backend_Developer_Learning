# Creational Pattern: Singleton Pattern in Spring Boot

---

### 1. Core Concept (Singleton Kya Hai?)
**Answer (Hinglish):**

> **Only ONE instance of a class exists, and everyone shares it.**  
> *(Bilkul Suraj / Sun ki tarah: Ek hi hai, aur sabhi ghar uski dhoop share karte hain).*

Application me kuch resources (jaise Configuration Manager, Database Connection Pools, Thread Pools) aise hote hain jinka multiple instances banana memory ka waste hai aur inconsistency paida kar sakta hai.

---

### 2. Classic Java (GoF) Singleton vs Spring Singleton

Dono me zameen-aasmaan ka farq hai:

```
[ CLASSIC GoF SINGLETON ]                     [ SPRING BOOT SINGLETON ]
- Private Constructor                         - Normal Public Constructor
- Static getInstance() method                 - Spring IoC Container manages lifecycle
- Scoped to JVM ClassLoader                   - Scoped to ApplicationContext per bean ID
```

#### A. Classic Java Singleton (GoF):
```java
public class ConfigManager {
    // Eagerly initialized single instance
    private static final ConfigManager INSTANCE = new ConfigManager();

    // 1. Private constructor ensures no one can call "new ConfigManager()" outside
    private ConfigManager() {}

    // 2. Global access point
    public static ConfigManager getInstance() {
        return INSTANCE;
    }
}
```
- **Scope:** Pure JVM ClassLoader ke andar sirf 1 instance.

#### B. Spring Singleton:
```java
@Service
public class PaymentService {
    // Normal class! No private constructor needed!
    public void processPayment() {
        // ...
    }
}
```
- **Scope:** **One instance per Spring ApplicationContext container**.
- Spring container khud ensure karta hai ki jab bhi koi `@Autowired PaymentService` maange, wahi cached singleton instance inject kiya jaye.

---

### 3. ⚠️ The Thread-Safety Trap (Danger in Production!)

Spring Boot me incoming HTTP requests alag-alag background threads (Tomcat worker threads) par aati hain. Aur sabhi threads **same singleton bean** ko access karte hain!

```
Request 1 (Thread 1) ──┐
Request 2 (Thread 2) ──┼───> [ @Service OrderService ] ───> [ private int count; ] ⚡ RACE CONDITION!
Request 3 (Thread 3) ──┘
```

#### ❌ The Buggy Code (Mutable Instance Field):
```java
@Service
public class OrderService {
    private int orderCount = 0; // ❌ DANGER: Shared mutable state across all users!

    public void createOrder() {
        orderCount++; // Thread-safety bug! Race condition under high concurrent traffic!
    }
}
```

#### ✅ The Best Practice (Keep Beans STATELESS):
```java
@Service
public class OrderService {

    // ✅ Instance variables me sirf immutable dependencies (@Autowired) rakhein
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void createOrder(OrderRequest request) {
        // ✅ Local variables use karein! Local variables thread ke private stack me rehte hain
        int orderCount = calculateCount(); 
        orderRepository.save(new Order(request));
    }
}
```
> **Rule of Thumb:** Spring singleton beans ko hamesha **STATELESS** rakho. Agar mutable state rakhna zaroori hai, to use method ke andar local variable banao ya `ThreadLocal` / `AtomicInteger` use karo.

---

### 4. Spring Bean Scopes Comparison

| Scope | Instances Created | Lifecycle / Use Case |
| :--- | :--- | :--- |
| **`singleton` (Default)** | **One per container** | Default for all `@Service`, `@Repository`, `@Component`. |
| **`prototype`** | **New one every time requested** | Jab har caller ko fresh stateful object chahiye (`@Scope("prototype")`). |
| **`request`** | **One per HTTP request** | Web applications me user-request specific data ke liye. |
| **`session`** | **One per HTTP session** | E-commerce shopping cart jo user session tak rahe. |

---

### 💡 Pro Interview Tip:
> **Question:** *"Is Spring Singleton same as GoF Singleton?"*  
> **Direct Answer:** **NO!**  
> - **GoF Singleton:** Scope poora **ClassLoader** hota hai (Hardcoded static variable). Isko mock karna ya unit test karna mushkil hota hai.  
> - **Spring Singleton:** Scope **Spring ApplicationContext (Container)** hota hai per bean name. Aap chahein to same class ke do alag-alag named beans (`primaryDb`, `secondaryDb`) container me define kar sakte hain!
