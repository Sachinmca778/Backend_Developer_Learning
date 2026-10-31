# Top Design Patterns Interview Questions & Cheat Sheet 📋

---

## 📌 Master Pattern $\rightarrow$ Spring Usage Mapping Table

| Pattern | Category | Where Spring Uses It in Real Projects |
| :--- | :--- | :--- |
| **Singleton** | Creational | Default scope for all Spring beans (`@Service`, `@Component`, `@Repository`). |
| **Factory** | Creational | `BeanFactory`, `ApplicationContext`, `@Bean` factory methods. |
| **Builder** | Creational | `ResponseEntity.ok().body()`, `WebClient.builder()`, `UriComponentsBuilder`. |
| **Proxy** | Structural | `@Transactional`, `@Cacheable`, `@Async`, `@PreAuthorize`, Spring AOP. |
| **Adapter** | Structural | Spring MVC `HandlerAdapter`, 3rd-party Payment Gateway integrations. |
| **Decorator** | Structural | `ContentCachingRequestWrapper` for repeated body reading. |
| **Strategy** | Behavioral | Injecting `List<Interface>` or `Map<String, Interface>` to eliminate large if-else ladders. |
| **Template Method** | Behavioral | `JdbcTemplate`, `KafkaTemplate`, `RestTemplate`, `TransactionTemplate`. |
| **Observer** | Behavioral | Spring Application Events (`ApplicationEventPublisher`, `@EventListener`). |
| **Chain of Responsibility** | Behavioral | Spring Security `SecurityFilterChain`, `OncePerRequestFilter`, `HandlerInterceptor`. |
| **Front Controller** | Architectural | Spring MVC's core `DispatcherServlet`. |

---

## 🎯 Top 6 Tricky Interview Questions (Direct Answers in Hinglish)

### Q1: Which design patterns does Spring Framework use internally?
**Answer:**  
Spring Framework GoF ke lagbhag 8+ patterns heavily use karta hai:
- **Singleton:** Default bean scope.
- **Factory:** `BeanFactory` / `ApplicationContext`.
- **Proxy:** AOP, `@Transactional`, `@Cacheable`.
- **Template Method:** `JdbcTemplate`, `KafkaTemplate`.
- **Observer:** `ApplicationEventPublisher`, `@EventListener`.
- **Strategy:** Authentication providers, dynamic strategy injection via Map/List.
- **Chain of Responsibility:** Servlet `SecurityFilterChain`.
- **Front Controller:** `DispatcherServlet`.

---

### Q2: Are Spring singleton beans thread-safe?
**Answer:**  
**NAHI!** By default, Spring singleton beans thread-safe **nahi hote**.  
Spring container sirf ek instance create karne ki guarantee deta hai, thread-safety ki nahi. Kyunki web requests multiple background threads me aati hain, isliye:
- **Golden Rule:** Singleton beans ko hamesha **STATELESS** rakhein. Unke andar koi shared mutable instance variable (`private int counter;`) na banayein!
- Data ko methods ke andar **local variables** me pass karein ya `AtomicInteger` / `ThreadLocal` use karein.

---

### Q3: How do you eliminate a big `if-else` or `switch` block?
**Answer:**  
**Strategy Pattern + Spring Map Injection!**  
1. Saare branches ke common behavior ke liye ek **Interface** banayein.
2. Har branch ka logic ek alag `@Component` class me implement karein.
3. Service class me Spring ke through `Map<String, Interface>` inject karein. Spring automatically bean name ko map key banakar saari strategies register kar deta hai. Run-time par `map.get(type).execute()` call karein!

---

### Q4: Which design pattern is behind Spring AOP?
**Answer:**  
**Dynamic Proxy Pattern!**  
Spring Boot AOP me target beans ko Proxy object se wrap karta hai (default **CGLIB Proxy** use karke). Yeh proxy method call ke pehle (`@Before`) aur baad me (`@After`) cross-cutting code inject karta hai.  
*Limitation to mention:* Same class ke andar internal method calls (`this.method()`) proxy ko bypass kar deti hain (**Self-Invocation bug**).

---

### Q5: Strategy Pattern vs Template Method Pattern?
**Answer:**  
- **Strategy Pattern:** **Composition** use karta hai (Context class interface reference hold karti hai). Poora algorithm runtime par swap ho sakta hai.
- **Template Method Pattern:** **Inheritance** use karta hai (Abstract class `final` method ke roop me skeleton step define karti hai). Child classes sirf un steps ko override karti hain jo vary karte hain, lekin sequence of execution change nahi kar sakti!

---

### Q6: Adapter vs Decorator Pattern?
**Answer:**  
- **Adapter Pattern:** **Interface ko badal deta hai** (Changes the interface). Do incompatible systems (like Indian plug and US socket, or Custom code and Razorpay SDK) ko compatible banata hai.
- **Decorator Pattern:** **Same interface maintain rakhta hai, par additional features add karta hai** (Like Coffee me Milk aur Sugar add karna, ya `HttpServletRequest` ko `ContentCachingRequestWrapper` se decorate karna).
