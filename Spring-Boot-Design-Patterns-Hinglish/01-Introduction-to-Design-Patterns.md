# What are Design Patterns? (Introduction & Core Philosophy)

---

### 1. Definition (Design Pattern Kya Hai?)
**Answer (Hinglish):**

> **A Design Pattern = A proven, reusable solution to a commonly occurring software design problem.**

Yeh koi direct copy-paste code nahi hota jo aap kisi library se import kar lo, balki yeh **"ek sochne ka tareeka (A way of architectural thinking)"** aur ek blueprint hota hai jisse problem solve ki jaati hai.

---

### 2. Real-World Analogy (Building Blueprint)
- Jab koi architect ghar ya building design karta hai, to wo har naye ghar ke liye **"seedhiyan (stairs) kaise banayein"** ya **"doors kaise lagayein"** dobara reinvent nahi karta. Unke paas proven blueprints hote hain.
- Theek usi tarah, software developers ko har project me **"objects kaise create karein"**, **"classes ko kaise jodein"**, ya **"modules ke beech message kaise pass karein"** reinvent karne ki zaroorat nahi hoti.

---

### 3. The 3 Core Categories

Design patterns ko unke primary purpose ke hisaab se 3 categories me divide kiya gaya hai:

```
+---------------------------------------------------------------------------------------+
|                               DESIGN PATTERN CATEGORIES                               |
+---------------------------+-------------------------------+---------------------------+
| 1. CREATIONAL PATTERNS    | 2. STRUCTURAL PATTERNS        | 3. BEHAVIORAL PATTERNS    |
| (HOW objects are created) | (HOW objects are connected)   | (HOW objects talk to each)|
+---------------------------+-------------------------------+---------------------------+
| • Singleton               | • Proxy                       | • Strategy                |
| • Factory                 | • Adapter                     | • Template Method         |
| • Builder                 | • Decorator                   | • Observer (Events)       |
|                           |                               | • Chain of Responsibility |
+---------------------------+-------------------------------+---------------------------+
```

1. **Creational Patterns (Object Creation):**
   - Focus: Object instantiation mechanism ko control aur abstract karna.
   - *In Spring:*
     - `Singleton` $\rightarrow$ Spring managed beans (`@Service`, `@Component`).
     - `Factory` $\rightarrow$ `BeanFactory`, `ApplicationContext`, `@Bean` methods.
     - `Builder` $\rightarrow$ `ResponseEntity.ok().body(...)`, `WebClient.builder()`.

2. **Structural Patterns (Object Relationships & Composition):**
   - Focus: Classes aur objects ko aapas me seamlessly connect karke badi structures banana.
   - *In Spring:*
     - `Proxy` $\rightarrow$ Spring AOP, `@Transactional`, `@Cacheable`, `@Async`.
     - `Adapter` $\rightarrow$ Spring MVC `HandlerAdapter`.
     - `Decorator` $\rightarrow$ `ContentCachingRequestWrapper`.

3. **Behavioral Patterns (Object Communication & Responsibility):**
   - Focus: Objects ke beech algorithm aur responsibility ka clear communication distribute karna.
   - *In Spring:*
     - `Strategy` $\rightarrow$ Payment methods (injecting List/Map of interfaces).
     - `Template Method` $\rightarrow$ `JdbcTemplate`, `KafkaTemplate`, `RestTemplate`.
     - `Observer` $\rightarrow$ Spring Application Events (`@EventListener`).
     - `Chain of Responsibility` $\rightarrow$ Spring Security Filters (`SecurityFilterChain`).

---

### 4. Why Learn Design Patterns? (Fayde Kya Hain?)
1. **Clean, Flexible & Testable Code:** Tight coupling khatam hoti hai aur unit testing me mocks easily inject ho jaate hain.
2. **Common Vocabulary in Code Reviews:** Senior engineers review me kehte hain: *"Yahan if-else hatakar Strategy pattern lagao"*, ya *"Is service ko Proxy se wrap karo"*. Patterns aane se discussion fast hoti hai.
3. **Cracking LLD (Low-Level Design) Interviews:** Tech giants (FAANG/MNCs) backend interviews me direct design pattern ke scenarios poochti hain.

---

### 💡 Pro Interview Tip:
> **Interview me sirf pattern ki definition mat batao — yeh batao ki "Spring Framework is pattern ko KAHAN use karta hai"!**  
> Example: Interviewer agar pooche: *"Proxy pattern kya hai?"*  
> Agar aap bologe: *"Proxy pattern ek object ko wrap karta hai"* $\rightarrow$ Average answer.  
> Agar aap bologe: *"Proxy pattern real object ko wrap karke additional behavior inject karta hai. Spring Boot isko `@Transactional` me DB transaction manage karne aur Spring AOP me use karta hai"* $\rightarrow$ **You sound like an experienced Senior Engineer!**
