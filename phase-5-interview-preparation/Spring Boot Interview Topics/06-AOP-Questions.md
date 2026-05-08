# AOP Questions

## Status: Not Started

---

## Table of Contents

1. [Spring AOP Kya Hai?](#spring-aop-kya-hai)
2. [JDK Dynamic Proxy vs CGLIB](#jdk-dynamic-proxy-vs-cglib)
3. [Advice Types](#advice-types)
4. [`@Around` vs `@Before`](#around-vs-before)
5. [Pointcut Expressions](#pointcut-expressions)
6. [Order of Aspects](#order-of-aspects)
7. [Spring AOP Limitations](#spring-aop-limitations)
8. [`proxyTargetClass` Explained](#proxytargetclass-explained)
9. [Spring AOP vs AspectJ](#spring-aop-vs-aspectj)
10. [Common Output Traps](#common-output-traps)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

> Cross-ref: `phase-4 / Design Patterns in Java / 02-Structural-Patterns.md` (Proxy / Decorator).

---

## Spring AOP Kya Hai?

**Aspect-Oriented Programming** — cross-cutting concerns (logging, security, transactions, caching) ko business logic se alag rakhna.

```
Concerns:
  Business: order placement
  Cross-cutting: log entry/exit, transaction, security check, audit
```

### Spring's implementation

- **Proxy-based** AOP (runtime weaving)
- Class spring beans pe hi apply
- `@Aspect` + Spring AspectJ annotations subset

---

## JDK Dynamic Proxy vs CGLIB

### JDK Dynamic Proxy

- **Interface required** — proxies the interface
- `java.lang.reflect.Proxy.newProxyInstance(...)`
- Lightweight

```java
public interface UserService { User get(Long id); }

@Service
class UserServiceImpl implements UserService { ... }   // proxied via interface
```

### CGLIB

- **Subclass-based** — extends concrete class
- No interface needed
- Cannot proxy `final` classes / methods
- Slightly heavier

### Spring's choice

| Bean has interface? | Default proxy | Override |
|--------------------|---------------|----------|
| ✅ | JDK Dynamic | `proxyTargetClass = true` → CGLIB |
| ❌ | CGLIB | — |

→ **Spring Boot 2+ default**: `proxyTargetClass = true` → **always CGLIB**.

---

## Advice Types

| Annotation | When |
|-----------|------|
| `@Before` | Before method call |
| `@After` | After (finally — success or fail) |
| `@AfterReturning` | After successful return |
| `@AfterThrowing` | When exception thrown |
| `@Around` | Wraps — full control (call `proceed()`) |

### Example

```java
@Aspect @Component
public class LoggingAspect {

    @Before("execution(* com.example.service.*.*(..))")
    public void logEntry(JoinPoint jp) {
        log.info("Calling {}", jp.getSignature());
    }

    @AfterReturning(pointcut = "execution(* com.example.service.*.*(..))", returning = "ret")
    public void logSuccess(JoinPoint jp, Object ret) { ... }

    @AfterThrowing(pointcut = "execution(* com.example.service.*.*(..))", throwing = "ex")
    public void logFail(JoinPoint jp, Throwable ex) { ... }

    @Around("execution(* com.example.service.*.*(..))")
    public Object timed(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            log.info("{} took {}ns", pjp.getSignature(), System.nanoTime() - start);
        }
    }
}
```

---

## `@Around` vs `@Before`

| | `@Before` | `@Around` |
|--|-----------|-----------|
| Modify args | ❌ | ✅ |
| Skip method | ❌ | ✅ (don't call `proceed()`) |
| Modify return | ❌ | ✅ |
| Catch exception | ❌ | ✅ |
| Performance overhead | Lower | Slightly higher |
| Use | Pure side effect | Full control / timing / retry |

### Example — argument modify (Around only)

```java
@Around("execution(* save(..)) && args(entity)")
public Object normalize(ProceedingJoinPoint pjp, Entity entity) throws Throwable {
    entity.normalize();
    return pjp.proceed(new Object[]{entity});
}
```

---

## Pointcut Expressions

### Designators

| Designator | Meaning |
|------------|---------|
| `execution(...)` | Method execution match |
| `within(...)` | Type pattern |
| `this(Type)` | Proxy is type |
| `target(Type)` | Target object is type |
| `args(...)` | Method args match |
| `@annotation(MyAnno)` | Method annotated |
| `@within(MyAnno)` | Class annotated |
| `@args(MyAnno)` | Args annotated |
| `@target(MyAnno)` | Target object's class annotated (runtime) |
| `bean(name)` | Bean name (Spring extension) |

### `execution(...)` syntax

```
execution([modifier] return-type [package].class-name.method(parameters))
```

```java
execution(public * com.example.service.*.*(..))                  // any service public method
execution(* com.example..*.*(..))                                  // any class in package or sub-pkg
execution(* save*(..))                                              // method names start with save
execution(* findById(Long))                                         // exact param
execution(* *(..))                                                  // anything (rarely useful)
```

### Combining

```java
@Pointcut("within(com.example.service..*)")
public void serviceLayer() {}

@Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
public void getRequest() {}

@Around("serviceLayer() && getRequest()")
public Object trace(ProceedingJoinPoint pjp) throws Throwable { ... }
```

---

## Order of Aspects

```java
@Aspect @Component @Order(1) class A { ... }
@Aspect @Component @Order(2) class B { ... }
```

→ Lower order = outer wrap (runs first / wraps last).

### Key built-in advice orders (rough)

```
TX (around) → Cache (around) → Security (around) → custom
```

→ Can re-order with `@Order` if precedence matters.

---

## Spring AOP Limitations

### 1. Same-class method call (self-invocation)

```java
@Service
class S {
    public void outer() { inner(); }      // ❌ proxy bypassed
    
    @MyAspect public void inner() { ... }
}
```

→ Same as `@Transactional` self-invocation (cross-ref `03-Transactional-Gotchas.md`).

### 2. Private / final / static methods

| | Why no aspect |
|--|---------------|
| `private` | CGLIB can't override; not on interface |
| `final` | CGLIB can't subclass / override |
| `static` | Not instance-level; no proxy |

### 3. Final classes (CGLIB)

CGLIB needs to subclass — `final class` blocked.

### 4. Beans not in container

POJOs created with `new` aren't proxied.

### 5. Constructor advice

Spring AOP **cannot** advise constructors. Use AspectJ load-time/compile-time weaving for that.

---

## `proxyTargetClass` Explained

```java
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
```

- `false` → JDK Dynamic Proxy (interface required)
- `true` → CGLIB always

### Spring Boot default (2.x+)

```yaml
# Implicit; equivalent to:
spring.aop.proxy-target-class: true
```

→ CGLIB always. Avoids many "interface needed" surprises.

---

## Spring AOP vs AspectJ

| | Spring AOP | AspectJ |
|--|-----------|---------|
| Weaving | Runtime (proxy) | Compile-time / Load-time |
| Where | Spring beans | Any object including POJOs |
| Advise constructor | ❌ | ✅ |
| Advise field access | ❌ | ✅ |
| Self-invocation | ❌ | ✅ |
| Setup complexity | Low | Higher (compiler / agent) |

→ **Default**: Spring AOP. Use AspectJ load-time weaving (`-javaagent:aspectjweaver.jar`) only when needed.

---

## Common Output Traps

### Q1. Self-invocation skip

```java
@Service class S {
    public void a() { b(); }
    
    @MyTimer public void b() { ... }
}
// b()'s @MyTimer NOT triggered when called via a()
```

### Q2. Order without `@Order`

Multiple aspects on same join point — undefined order; explicit `@Order` needed.

### Q3. `@Around` forgot proceed

```java
@Around("...")
public Object miss(ProceedingJoinPoint pjp) {
    return null;       // ❌ method never executed
}
```

### Q4. CGLIB on final method

```java
@Service
class S {
    public final void m() { ... }    // ❌ aspect won't apply
}
```

### Q5. Private + `@Transactional`

(Already covered in TX file — same root cause.)

---

## Pitfalls

1. **Self-invocation** — `@Around` ignored on internal calls.
2. **Proxy on non-Spring beans** — manual `new` skips AOP.
3. **CGLIB on final** — silent skip.
4. **`@Aspect` not registered as bean** — `@Component` missing.
5. **`@EnableAspectJAutoProxy` missing** in non-Boot apps — Boot does it; plain Spring needs annotation.
6. **`proceed()` skipped** in `@Around` → method never runs.
7. **Multiple aspects no order** — debugging nightmare; declare `@Order`.
8. **Performance overhead** in hot paths → measure (cross-ref Performance folder).
9. **Constructor weaving expectation** — not Spring AOP.
10. **Pointcut typo** — silent miss; tests + verify with logs.

---

## Cheat Sheet

| Concept | Quick |
|---------|-------|
| Proxy types | JDK (interface) / CGLIB (class) |
| Boot default | CGLIB (`proxyTargetClass=true`) |
| Around vs Before | Around = full control |
| Self-invocation | Bypassed; refactor / `currentProxy()` |
| Private/final/static | No-op |
| Pointcut commons | `execution`, `within`, `@annotation`, `args` |
| Order | `@Order` on aspect |

| Limitation | Workaround |
|------------|-----------|
| Same-class call | Separate bean / `AopContext.currentProxy()` |
| Private method | Make package-private/public |
| Final class | Don't `final` Spring beans / use AspectJ |
| Constructor | AspectJ weaver |

---

## Practice

1. `@Around` timing aspect on `@Service` package; log per-method ns.
2. `@Before` on `@PostMapping` for entry log via annotation pointcut.
3. Self-invocation aspect bypass — fix using separate bean.
4. `@Order` two aspects; observe execution order.
5. `proxyTargetClass = false` se interface-only proxy try; bean without interface fail.
6. AspectJ load-time weaving setup (advanced bonus).
