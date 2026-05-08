# Design Patterns in Java

Production-grade **Design Patterns** focused on Java + Spring Boot — classic GoF (Creational, Structural, Behavioral) **plus** Spring-specific idioms (Repository, Service Layer, DTO, Specification, Front Controller). Sab Hinglish mein, real code examples, Spring mappings, comparison tables, common pitfalls.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Creational Patterns | [01-Creational-Patterns.md](./01-Creational-Patterns.md) | Not Started |
| 2 | Structural Patterns | [02-Structural-Patterns.md](./02-Structural-Patterns.md) | Not Started |
| 3 | Behavioral Patterns | [03-Behavioral-Patterns.md](./03-Behavioral-Patterns.md) | Not Started |
| 4 | Spring-Specific Patterns | [04-Spring-Specific-Patterns.md](./04-Spring-Specific-Patterns.md) | Not Started |

---

## What's Inside Each File?

### [01 — Creational Patterns](./01-Creational-Patterns.md)
**Singleton** (naive vs synchronized vs **double-checked locking with volatile** vs **Bill Pugh holder** vs **enum singleton** — Joshua Bloch's recommendation), **Builder** (telescoping constructor problem, manual implementation, **Lombok `@Builder`** with `@Singular` and `@Builder.Default`, step builder, real-world like `HttpRequest.newBuilder()`), **Factory Method** (manual switch, **Spring `@Bean` factory methods**, polymorphic Map-based registry with Spring DI, **`FactoryBean<T>`**), **Abstract Factory** (factory of factories — UI theme example, JDBC `Connection`, JAXB), **Prototype** (`Cloneable` pitfalls, **copy constructor approach**, **Spring `@Scope("prototype")`**, deep copy strategies), Object Pool bonus (HikariCP).

### [02 — Structural Patterns](./02-Structural-Patterns.md)
**Adapter** (object vs class adapter, multi-provider example like Stripe/Razorpay → common `PaymentGateway`, JDK examples), **Decorator** (vs subclass explosion, classic **`InputStream` wrappers** chain — Buffered + GZIP + Data, custom `Notifier` chain, Spring AOP wrappers), **Facade** (order processing facade, **`JpaRepository` as JPA facade**, `RestTemplate` facade), **Proxy** (virtual/protection/remote/logging/caching types, **JDK Dynamic Proxy** vs **CGLIB**, **Spring AOP** with `@Transactional`/`@Cacheable`, **self-invocation pitfall**), **Composite** (file system tree, recursive size calculation, Spring Security filter chain), **Bridge** (Shape × Color decoupling, JDBC API + drivers, SLF4J + implementations), Flyweight bonus.

### [03 — Behavioral Patterns](./03-Behavioral-Patterns.md)
**Strategy** (manual + **Spring Map injection idiom** for payment methods, functional Comparator), **Observer** (manual implementation, **Spring `@EventListener`** with sync/async/`@TransactionalEventListener`, why `java.util.Observable` deprecated), **Template Method** (skeleton + hooks, **`JdbcTemplate.query()` walkthrough**, hook methods), **Command** (undo/redo, `Runnable` as command, `ExecutorService` job queues), **Chain of Responsibility** (manual handler chain, **Spring Security filter chain**, Servlet filters, functional Predicate chain), **State** (vs endless if/switch — Order lifecycle, **Spring State Machine**), **Visitor** (Shape area/serialize visitors, **Java 17+ pattern matching alternative** with sealed classes), Iterator + Mediator bonus.

### [04 — Spring-Specific Patterns](./04-Spring-Specific-Patterns.md)
**Repository** (origin in DDD, **Spring Data method name conventions** — `findBy`/`countBy`/etc., `@Query` and Modifying queries, custom repo implementations, multiple backends), **Service Layer** (responsibilities, transaction boundaries, anemic vs rich domain model debate), **DTO** (why entity ≠ API contract, request + response DTOs, mapping options — **Manual vs MapStruct (recommended) vs ModelMapper (avoid)**, **Java records as DTOs**, request DTOs with validation), **Specification** (DDD origin, **Spring Data `JpaSpecificationExecutor`**, composable predicates, dynamic search with pagination, type-safe metamodel, **QueryDSL** alternative), **Front Controller** (`DispatcherServlet` deep dive, components like `HandlerMapping`/`HandlerAdapter`/`MessageConverter`, filter chain vs interceptor), full **layered architecture** picture, bonus patterns (DI, AOP, Auto-Configuration, Open Session in View anti-pattern).

---

## Recommended Learning Order

```
1. Creational (01)        ← object creation foundations
2. Structural (02)        ← composition for flexibility
3. Behavioral (03)        ← communication between objects
4. Spring-Specific (04)   ← assemble all into modern Spring stack
```

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Singleton thread-safe | 01 | Singleton Pattern |
| Lombok `@Builder` | 01 | Builder Pattern |
| Strategy via Spring map | 03 | Strategy Pattern |
| Spring application events | 03 | Observer Pattern |
| `@Transactional` proxy explanation | 02 | Proxy Pattern |
| Spring AOP self-invocation | 02 | Proxy Pattern (pitfall) |
| `JdbcTemplate` internals | 03 | Template Method |
| Spring Security filter chain | 03 | Chain of Responsibility |
| Order state machine | 03 | State Pattern |
| Repository interface design | 04 | Repository Pattern |
| Service vs Controller scope | 04 | Service Layer Pattern |
| When to use DTO | 04 | DTO Pattern |
| Dynamic search queries | 04 | Specification Pattern |
| `DispatcherServlet` flow | 04 | Front Controller |
| MapStruct setup | 04 | DTO Pattern (mapping) |
| Layered architecture diagram | 04 | Layered Architecture |

---

## Pattern → Spring Mapping Cheat Sheet

| GoF Pattern | Spring's Implementation |
|-------------|------------------------|
| Singleton | Default bean scope |
| Prototype | `@Scope("prototype")` |
| Factory Method | `@Bean` methods |
| Abstract Factory | `BeanFactory`, `FactoryBean<T>` |
| Builder | Lombok `@Builder`, `RestTemplateBuilder` |
| Adapter | `HandlerAdapter` |
| Decorator | AOP wrappers |
| Facade | `JpaRepository`, `RestTemplate` |
| Proxy | `@Transactional`, `@Cacheable`, `@Async` |
| Composite | Spring Security filter chain |
| Bridge | Spring Data abstractions over JPA/Mongo/Redis |
| Strategy | Map-based bean injection |
| Observer | `@EventListener`, `ApplicationEventPublisher` |
| Template Method | `JdbcTemplate`, `RestTemplate`, `RedisTemplate` |
| Command | `Runnable`, `@Async` methods |
| Chain of Responsibility | Filters + Interceptors |
| State | Spring State Machine |
| Visitor | Annotation processors |
| Iterator | `Stream`, `Iterable` |
| Mediator | `ApplicationEventPublisher` |

---

## Architecture at a Glance

```
┌──────────────────────────────────────────────────────────────────┐
│                  HTTP REQUEST → Front Controller                   │
│                      (DispatcherServlet)                           │
└────────────────────────────┬───────────────────────────────────────┘
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Controller (HTTP boundary, DTO validation)                        │
│  Pattern: Front Controller + Adapter (HandlerAdapter)              │
└────────────────────────────┬───────────────────────────────────────┘
                             │ DTOs
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Service (Business logic, orchestration, transactions)             │
│  Patterns: Service Layer, Strategy (payment methods),              │
│           Observer (events), Facade (over multiple repos)          │
└────────────────────────────┬───────────────────────────────────────┘
                             │ Domain entities
                             ▼
┌──────────────────────────────────────────────────────────────────┐
│  Repository (Data access)                                          │
│  Patterns: Repository (DDD), Specification (dynamic queries),      │
│           Template Method (JpaRepository), Bridge (JPA/Mongo)      │
└────────────────────────────┬───────────────────────────────────────┘
                             ▼
                          Database

  Cross-Cutting (via AOP/Proxy):
  ─ @Transactional, @Cacheable, @Async, @PreAuthorize
```

---

## Companion Folders

- [Microservices Architecture](../Microservices%20Architecture/) — patterns at distributed system scale (Saga, Circuit Breaker, etc.)
- [Spring Cloud](../Spring%20Cloud/) — Spring distributed toolkit using these patterns
- [Messaging — Kafka](../Messaging%20—%20Kafka/) and [Messaging — RabbitMQ](../Messaging%20—%20RabbitMQ/) — Observer + Command at infrastructure level
- [Database Mastery](../../backend-skills/Database-Mastery/) — repositories backed by SQL knowledge
- [Code Quality & Best Practices](../../backend-skills/Code-Quality-&-Best-Practices/) — SOLID + clean code applies these patterns

---

## Tools Reference

### Code Generation
- **Lombok** (`@Builder`, `@Data`, `@RequiredArgsConstructor`)
- **MapStruct** (compile-time DTO mapping)
- **Java Records** (immutable DTOs, Java 14+)

### Spring Stack
- `spring-boot-starter-web` — Front Controller (DispatcherServlet)
- `spring-boot-starter-data-jpa` — Repository, Specification
- `spring-boot-starter-aop` — Proxy / Decorator support
- `spring-statemachine-core` — State pattern formal

### Pattern Implementation Helpers
- `EventBus` (Guava) — Mediator
- Reactor / RxJava — Observer (reactive)
- QueryDSL — alternative to Specifications

### Reading Material
- *Design Patterns* (GoF — original 1994 book)
- *Effective Java* (Joshua Bloch — chapter on enum singletons, builders)
- *Patterns of Enterprise Application Architecture* (Martin Fowler — Repository, Service Layer, DTO)
- *Domain-Driven Design* (Eric Evans — Repository, Specification origins)

---

## Status Tracker

```
[ ] 01 — Creational Patterns
[ ] 02 — Structural Patterns
[ ] 03 — Behavioral Patterns
[ ] 04 — Spring-Specific Patterns
```

Topic complete hone par file header aur is README dono mein status update kar lena.

> Patterns sirf academic concept nahi — **Spring framework har jagah inhe use karta hai**.
> Pattern samajhna = Spring's code samajhna = better designs likhna.
> But patterns are **tools**, not goals — overuse = over-engineering. Right pattern, right place.
