# Spring Boot Design Patterns (Used in Real Projects) 🚀 (Hinglish 🇮🇳)

Spring Boot internally 8 se zyada GoF (Gang of Four) Design Patterns use karta hai. Is complete guide me har ek design pattern ko **real-world production architecture, code examples, Spring internals, aur interview questions** ke sath aasan **Hinglish** me explain kiya gaya hai.

---

## 📑 Index & Navigation

```
                               DESIGN PATTERNS IN SPRING BOOT
         ┌───────────────────────────────┼───────────────────────────────┐
         ▼                               ▼                               ▼
    CREATIONAL                      STRUCTURAL                      BEHAVIORAL
  (Object Creation)             (Object Connections)            (Object Communication)
  • Singleton                     • Proxy                         • Strategy
  • Factory                       • Adapter                       • Template Method
  • Builder                       • Decorator                     • Observer (Spring Events)
                                                                  • Chain of Responsibility
```

### 1. [Introduction to Design Patterns](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/01-Introduction-to-Design-Patterns.md)
- What is a Design Pattern? (Blueprint Analogy)
- 3 Core Categories: Creational, Structural, Behavioral
- Why learn them? & Senior Developer Interview Tips

### 2. Creational Patterns (Object Creation)
- [**01. Singleton Pattern**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/02-Creational-Patterns/01-Singleton-Pattern.md): Classic Java vs Spring Singleton, Thread-safety Trap, Bean Scopes, GoF Singleton vs Spring Bean Singleton.
- [**02. Factory Pattern**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/02-Creational-Patterns/02-Factory-Pattern.md): `BeanFactory` & `ApplicationContext`, `@Bean` factory methods, Real Project Notification Factory, `Map<String, Service>` Spring DI Trick.
- [**03. Builder Pattern**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/02-Creational-Patterns/03-Builder-Pattern.md): Telescoping Constructor anti-pattern, Lombok `@Builder`, Spring's `ResponseEntity`, `WebClient`, `UriComponentsBuilder`.

### 3. Structural Patterns (Object Relationships)
- [**01. Proxy Pattern (Spring's Favorite)**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/03-Structural-Patterns/01-Proxy-Pattern.md): Stand-in Wrapper object, How AOP works, `@Transactional`, `@Cacheable`, `@Async`, JDK Dynamic Proxy vs CGLIB Proxy, **Self-Invocation Trap (`this.method()`)**.
- [**02. Adapter & Decorator Patterns**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/03-Structural-Patterns/02-Adapter-and-Decorator-Patterns.md): 3-Pin to 2-Pin socket analogy, 3rd party Payment Gateway Adapter (Razorpay/Stripe), Spring MVC `HandlerAdapter`, Coffee decorator, `ContentCachingRequestWrapper`, Comparison table (Adapter vs Decorator vs Proxy).

### 4. Behavioral Patterns (Object Communication)
- [**01. Strategy Pattern**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/04-Behavioral-Patterns/01-Strategy-Pattern.md): Killing the `if-else` monster, Payment Strategy (UPI / Card / NetBanking), Open/Closed Principle (OCP), Spring `List`/`Map` auto-injection.
- [**02. Template Method Pattern**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/04-Behavioral-Patterns/02-Template-Method-Pattern.md): Fixed skeleton vs flexible child steps, `JdbcTemplate`, `KafkaTemplate`, `RestTemplate`, Real project Report Exporter (CSV/PDF).
- [**03. Observer Pattern (Spring Events)**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/04-Behavioral-Patterns/03-Observer-Pattern-Spring-Events.md): Decoupling order placement, `ApplicationEventPublisher`, `@EventListener`, `@Async` listeners, `@TransactionalEventListener`, Spring Events vs Apache Kafka.
- [**04. Chain of Responsibility & Front Controller**](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/04-Behavioral-Patterns/04-Chain-of-Responsibility-and-Front-Controller.md): Airport security check analogy, Spring Security Filter Chain (`OncePerRequestFilter`), `HandlerInterceptor`, Front Controller pattern in `DispatcherServlet`.

### 5. [Interview Q&A + Master Cheat Sheet](file:///Users/sachin/Backend_Developer_Learning/Spring-Boot-Design-Patterns-Hinglish/05-Interview-QA-and-Cheatsheet.md)
- Complete Pattern $\rightarrow$ Spring Usage Mapping Table
- Top 6 Tricky Interview Questions & Answers
