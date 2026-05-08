# Spring Boot Interview Topics

Spring Boot ke **interview rounds** ke liye must-know deep questions — Spring Core, auto-config, `@Transactional` gotchas, Security, JPA traps, AOP, Actuator, common Spring Boot questions. **Hinglish**, Q&A style, code snippets + traps + cheat sheets.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Spring Core Questions | [01-Spring-Core-Questions.md](./01-Spring-Core-Questions.md) | Not Started |
| 2 | Auto-Configuration | [02-Auto-Configuration.md](./02-Auto-Configuration.md) | Not Started |
| 3 | @Transactional Gotchas | [03-Transactional-Gotchas.md](./03-Transactional-Gotchas.md) | Not Started |
| 4 | Spring Security Questions | [04-Spring-Security-Questions.md](./04-Spring-Security-Questions.md) | Not Started |
| 5 | Spring Data JPA Questions | [05-Spring-Data-JPA-Questions.md](./05-Spring-Data-JPA-Questions.md) | Not Started |
| 6 | AOP Questions | [06-AOP-Questions.md](./06-AOP-Questions.md) | Not Started |
| 7 | Actuator & Monitoring | [07-Actuator-and-Monitoring.md](./07-Actuator-and-Monitoring.md) | Not Started |
| 8 | Common Spring Boot Qs | [08-Common-Spring-Boot-Qs.md](./08-Common-Spring-Boot-Qs.md) | Not Started |

---

## What's Inside Each File?

### [01 — Spring Core Questions](./01-Spring-Core-Questions.md)
**`@Autowired` resolution order** (type → `@Primary` → `@Qualifier` → name), constructor vs setter vs field injection, **circular dependency** kaise toot ti hai (constructor fails, setter works, redesign best), **`@Lazy`** se break, **bean scopes** (singleton/prototype/request/session/application), prototype-into-singleton trap + 3 fixes (ObjectProvider, lookup-method, `ApplicationContext.getBean`).

### [02 — Auto-Configuration](./02-Auto-Configuration.md)
**`@SpringBootApplication` ka breakdown** (`@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`), pre-2.7 **`spring.factories`** vs 2.7+ **`META-INF/spring/...AutoConfiguration.imports`**, **`@Conditional*`** family (`OnClass`, `OnMissingBean`, `OnProperty`, `OnWebApplication`), custom auto-config likhna step-by-step, debugging with `--debug` / `spring.autoconfigure.exclude`, autoconfig report read.

### [03 — @Transactional Gotchas](./03-Transactional-Gotchas.md)
**Self-invocation** (proxy bypass — `this.method()` skips advice), fixes (separate bean, `AopContext.currentProxy()` with `exposeProxy = true`, async events), **rollback only on `RuntimeException`/`Error`** by default — checked exceptions silently commit, **`rollbackFor`/`noRollbackFor`**, `readOnly = true` optimization, **propagation modes** (REQUIRED/REQUIRES_NEW/NESTED/SUPPORTS), isolation levels, `@Transactional` on private/final methods (no-op), exception inside `try-catch` = no rollback trap.

### [04 — Spring Security Questions](./04-Spring-Security-Questions.md)
**Filter chain** kaise build hoti hai (`SecurityFilterChain` bean, `DelegatingFilterProxy` → `FilterChainProxy`), order of common filters, **JWT stateless vs session-based** trade-offs, **CSRF** kab disable karein (stateless API yes, browser session no), **`@PreAuthorize` vs `@Secured` vs `@RolesAllowed`** comparison + SpEL power, **`SecurityContextHolder`** ka ThreadLocal model + async/`@Async` trap (`MODE_INHERITABLETHREADLOCAL` / `DelegatingSecurityContextExecutor`), Spring Security 6 lambda config snippet.

### [05 — Spring Data JPA Questions](./05-Spring-Data-JPA-Questions.md)
**N+1 problem** + 3 fixes (`JOIN FETCH`, `@EntityGraph`, `@BatchSize`), **`save()` vs `saveAll()`** (each row event vs batch hint), **`flush()` vs commit** (push to DB vs end TX), **`clear()` vs `evict(entity)`** (whole vs single PC eviction), **first-level cache** behaviour (per persistence context), **`@Transactional` on repository methods** (default read-only proxies, why custom impls don't auto get it), `findById` lazy init pitfall, dirty checking, Spring Data dialect of `@Modifying @Query`.

### [06 — AOP Questions](./06-AOP-Questions.md)
**JDK Dynamic Proxy vs CGLIB** (interface vs class, when each), `@Around` vs `@Before` vs `@After*`, pointcut expression breakdown (`execution`, `within`, `@annotation`, `bean`, `args`), proxy chain order (`@Order`), **limitations** (same-class call bypass, private/final/static methods, no auto-proxy on internal calls), `proxyTargetClass = true` impact, AspectJ load-time vs Spring AOP differences.

### [07 — Actuator & Monitoring](./07-Actuator-and-Monitoring.md)
Default endpoints, **prod-safe exposure** (`/health`, `/info`, `/metrics`, `/prometheus` only), `management.server.port` separate management port, **custom `HealthIndicator`** + `HealthContributor`, **custom metrics with Micrometer** (`Counter`, `Timer`, `Gauge`, `@Timed`), **liveness vs readiness** probe difference + Boot 2.3+ groups, `application/availability` event, `info` contributors, **graceful shutdown** (`server.shutdown=graceful`, `lifecycle.timeout-per-shutdown-phase`), security best practices.

### [08 — Common Spring Boot Qs](./08-Common-Spring-Boot-Qs.md)
**Multiple data sources** setup (primary + secondary, `@ConfigurationProperties`, separate `EntityManagerFactory`/`TransactionManager`, **routing DataSource** for read/write), **Spring Boot 2 vs 3** differences (**Java 17+ baseline**, **`jakarta.*` namespace** migration, **Spring Security 6** lambda DSL, observability via Micrometer Tracing, native AOT, GraalVM, deprecated bits), **embedded server** customization (`WebServerFactoryCustomizer`, port, threads, connection timeouts), **startup optimization** (lazy init, AOT, `spring.main.lazy-initialization`, conditional autoconfig exclusion), CommandLineRunner vs ApplicationRunner.

---

## Recommended Order

```
1. Spring Core Questions       ← DI fundamentals
2. Auto-Configuration          ← Spring Boot magic explained
3. @Transactional Gotchas      ← most common production bug area
4. AOP Questions               ← prereq deeper for proxies
5. Spring Security             ← framework-specific deep
6. Spring Data JPA             ← persistence depth
7. Actuator & Monitoring       ← prod readiness
8. Common Spring Boot Qs       ← Boot 2→3 + multi-DS + startup
```

---

## Interview Strategy

| Round | Approach |
|-------|----------|
| Theory | "Default behaviour kya hai → kab break hota hai → fix" |
| Code reasoning | Self-invocation / rollback traps explain step-by-step |
| Architecture | Multi-DS, security chain, async + SecurityContext |
| Debug story | "Production issue dekha — autoconfig exclude / actuator se diagnose" |

---

## Companion Folders

- [Core Java Interview Topics](../Core%20Java%20Interview%20Topics/) — JVM/threads basics
- [Phase-3 Intermediate Spring](../../phase-3-intermediate-spring/) — basics refresher
- [Performance & Optimization](../../phase-4-advanced-topics/Performance%20%26%20Optimization/) — JPA query, JVM, caching
- [Microservices Architecture](../../phase-4-advanced-topics/Microservices%20Architecture/) — Spring Cloud / patterns
- [Design Patterns in Java](../../phase-4-advanced-topics/Design%20Patterns%20in%20Java/) — proxy / strategy / template

---

## Status Tracker

```
[ ] 01 — Spring Core Questions
[ ] 02 — Auto-Configuration
[ ] 03 — @Transactional Gotchas
[ ] 04 — Spring Security Questions
[ ] 05 — Spring Data JPA Questions
[ ] 06 — AOP Questions
[ ] 07 — Actuator & Monitoring
[ ] 08 — Common Spring Boot Qs
```

> Spring Boot interview = "**default kaise kaam karta hai** + **kahan toot ta hai** + **kaise debug** karoge". Ye 8 files wahi 3 angles cover karti hain.
