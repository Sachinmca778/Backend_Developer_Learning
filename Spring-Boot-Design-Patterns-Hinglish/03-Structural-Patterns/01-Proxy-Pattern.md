# Structural Pattern: Proxy Pattern (Spring's Absolute Favorite) 🛡️

---

### 1. Core Concept (Proxy Pattern Kya Hai?)
**Answer (Hinglish):**

> **A Proxy is a stand-in wrapper object that wraps the real target object and adds extra work BEFORE and AFTER the real method call.**  
> *(Analogy: VIP Bodyguard / Security Guard ki tarah: Jo pehle checking karta hai, tabhi aapko VIP se milne deta hai, aur nikalte waqt exit record karta hai).*

Spring Boot me hum jitne bhi "magical" annotations dekhte hain (`@Transactional`, `@Cacheable`, `@Async`, `@PreAuthorize`), un sabke piche sirf aur sirf ek hi pattern kaam karta hai: **Dynamic Proxy Pattern**!

---

### 2. How the Proxy Interception Works Internally

Jab aap kisi Service bean ko inject karte ho, to Spring aapko direct real class ka object nahi deta! Balki ek **Proxy Object** deta hai jo real object ko wrap kiye hota hai:

```
                  ┌──────────────────────────────────────────────┐
                  │                 SPRING PROXY                 │
                  │                                              │
                  │  1. BEFORE:                                  │
                  │     • Start DB Transaction (BEGIN TX)        │
                  │     • Check Redis Cache                      │
                  │     • Verify JWT / Roles (@PreAuthorize)     │
                  │                                              │
Controller ──────>│  2. CALL:                                    │──────> [ Real OrderService ]
                  │     • Executes real business logic           │        (Target Object)
                  │                                              │
                  │  3. AFTER:                                   │
                  │     • Commit DB Transaction (COMMIT TX)      │
                  │     • Save return value to Cache             │
                  │     • Log execution time                     │
                  │     • Rollback on Exception                  │
                  └──────────────────────────────────────────────┘
```

---

### 3. Where Spring Uses Proxy Pattern Everyday

| Annotation / Feature | What the Proxy Injects |
| :--- | :--- |
| **`@Transactional`** | Method start hone se pehle DB transaction begin karta hai; success par `commit` aur exception par `rollback` karta hai. |
| **`@Cacheable`** | Method call karne se pehle cache me check karta hai. Agar value mil gayi to real method ko call kiye bina direct return kar deta hai. |
| **`@Async`** | Method call ko wrap karke background `TaskExecutor` thread pool me execute hone ke liye submit karta hai. |
| **`@PreAuthorize`** | Method enter hone se pehle `SecurityContext` me check karta hai ki user ke paas required permission hai ya nahi. |
| **Custom `@Aspect` (AOP)**| Logging, metrics, performance profiling, auditing. |
| **Spring Data JPA Repositories**| Aap sirf interface likhte ho (`public interface UserRepo extends JpaRepository`), Spring runtime par dynamic proxy se saare SQL methods generate kar deta hai! |

---

### 4. JDK Dynamic Proxy vs CGLIB Proxy

Spring do tarike se proxies generate karta hai:

```
[ JDK Dynamic Proxy ]                        [ CGLIB Proxy ]
- Java Reflection package built-in          - Bytecode generation library
- Target class MUST implement an Interface   - Subclasses the target class directly
- Works only on Interface methods            - Works on concrete classes without interface
                                            - Spring Boot 2+ & 3.x default mechanism
```

> **Note:** Spring Boot me property `spring.aop.proxy-target-class=true` by default hoti hai, isliye Spring Boot hamesha **CGLIB Proxy** use karta hai.

---

### 5. ⚠️ The Deadly Production Trap: Self-Invocation (`this.method()`)

Yeh 90% Spring Boot developers ka sabse common bug aur interviewers ka favourite trick question hai!

#### ❌ The Buggy Code:
```java
@Service
public class OrderService {

    public void createOrder() {
        // ... some non-transactional work ...
        
        // ❌ SELF-INVOCATION TRAP!
        this.saveToDatabase(); 
    }

    @Transactional // ⚠️ WARNING: @Transactional WILL NOT WORK HERE!
    public void saveToDatabase() {
        orderRepo.save(order);
        paymentRepo.save(payment);
    }
}
```

#### Why did `@Transactional` fail?
- Jab bahar se Controller `orderService.createOrder()` call karta hai, to call Proxy ke pass aati hai.
- Lekin `createOrder()` ke andar aapne **`this.saveToDatabase()`** call kar diya.
- `this` ka matlab hai **Direct Real Object**, Proxy nahi!
- Is wajah se Proxy bypass ho gayi aur Spring ka transaction interceptor execute hi nahi hua! Agar koi error aayi, to **koi rollback nahi hoga**!

#### ✅ How to Fix Self-Invocation:
1. **Refactor (Best Way):** `saveToDatabase()` method ko alag service class me move karein aur inject karein.
2. **Self-Injection (Spring 4.3+):**
   ```java
   @Service
   public class OrderService {
       @Autowired
       private OrderService self; // Injects the Proxy!

       public void createOrder() {
           self.saveToDatabase(); // Calls via Proxy! @Transactional works!
       }
   }
   ```

---

### 💡 Pro Interview Tip:
> **Question:** *"Which design pattern is behind Spring AOP?"*  
> **Your Answer:** *"Spring AOP uses the **Proxy Design Pattern** (specifically CGLIB or JDK Dynamic Proxies). The proxy wraps target beans to execute cross-cutting concerns like `@Transactional` and `@Cacheable`. Always mention the **self-invocation limitation** where internal `this.method()` calls bypass the proxy!"*
