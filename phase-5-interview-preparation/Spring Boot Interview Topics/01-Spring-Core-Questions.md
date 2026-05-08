# Spring Core Questions

## Status: Not Started

---

## Table of Contents

1. [IoC & DI Fundamentals](#ioc--di-fundamentals)
2. [Constructor vs Setter vs Field Injection](#constructor-vs-setter-vs-field-injection)
3. [`@Autowired` Resolution Order](#autowired-resolution-order)
4. [`@Primary` vs `@Qualifier`](#primary-vs-qualifier)
5. [Circular Dependency](#circular-dependency)
6. [`@Lazy` to Break Circular Dep](#lazy-to-break-circular-dep)
7. [Bean Scopes](#bean-scopes)
8. [Prototype Inside Singleton — Trap + Fixes](#prototype-inside-singleton--trap--fixes)
9. [Bean Lifecycle Callbacks](#bean-lifecycle-callbacks)
10. [`@Configuration` vs `@Component` for `@Bean`](#configuration-vs-component-for-bean)
11. [`ApplicationContext` vs `BeanFactory`](#applicationcontext-vs-beanfactory)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## IoC & DI Fundamentals

### IoC

**Inversion of Control** — object creation + wiring framework karta hai, app code nahi.

### DI

**Dependency Injection** — IoC ka concrete pattern; constructor / setter / field se beans inject hote.

### Spring container

```
@SpringBootApplication
       ↓
ApplicationContext = BeanFactory + i18n + events + resource loading + more
```

---

## Constructor vs Setter vs Field Injection

### Constructor (recommended)

```java
@Service
public class UserService {
    private final UserRepository repo;
    
    public UserService(UserRepository repo) {   // Spring auto-injects
        this.repo = repo;
    }
}
```

**Pros:**
- **Final** fields → immutability
- **Mandatory** dependencies enforced (compile time)
- **Easy unit test** (just `new UserService(mock)`)
- Helps detect circular deps **at startup**

### Setter

```java
@Autowired
public void setRepo(UserRepository r) { this.repo = r; }
```

**Use:** Optional dependency / circular cycle break.

### Field

```java
@Autowired
private UserRepository repo;
```

**Cons:**
- Test mein reflection chahiye
- Hidden dependencies
- Encourages God objects

→ Modern Spring + Lombok (`@RequiredArgsConstructor`) → **constructor by default**.

---

## `@Autowired` Resolution Order

Container kaise decide karta hai kaunsa bean inject?

```
1. By TYPE (matching class/interface)
   ↓ multiple matches?
2. @Primary annotated candidate (if any)
   ↓ still multiple OR none-primary?
3. @Qualifier matches by name
   ↓ no qualifier?
4. By name (parameter / field name == bean name)
   ↓ none match?
5. NoSuchBeanDefinition / NoUniqueBeanDefinition
```

### Visual Example

```java
public interface PaymentGateway {}

@Component                  class StripeGateway implements PaymentGateway {}
@Component @Primary         class RazorpayGateway implements PaymentGateway {}
@Component("legacyPay")     class LegacyGateway implements PaymentGateway {}

@Service
class CheckoutService {
    
    // 1. Just by type — multiple → @Primary wins
    @Autowired PaymentGateway gateway;        // → RazorpayGateway
    
    // 2. Qualifier overrides primary
    @Autowired @Qualifier("legacyPay")
    PaymentGateway legacy;                     // → LegacyGateway
    
    // 3. Param name matches bean
    @Autowired
    public CheckoutService(PaymentGateway stripeGateway) {  // → StripeGateway
        ...
    }
}
```

---

## `@Primary` vs `@Qualifier`

| | `@Primary` | `@Qualifier` |
|--|-----------|--------------|
| Where | Bean definition side | Injection point side |
| Effect | Default candidate | Explicit pick |
| Multiple beans | Resolves ambiguity | Resolves ambiguity |
| Override priority | Lower than `@Qualifier` | Higher |

**Rule:** Use `@Primary` for "ye most common bean hai", `@Qualifier` for "iss specific jagah pe wo bean chahiye".

### Custom qualifier (cleaner)

```java
@Qualifier
@Retention(RUNTIME)
public @interface Stripe {}

@Component @Stripe class StripeGateway implements PaymentGateway {}

@Autowired @Stripe PaymentGateway gateway;
```

---

## Circular Dependency

### Constructor cycle (FAILS at startup)

```java
@Service class A { A(B b) {} }
@Service class B { B(A a) {} }
```

→ Spring Boot 2.6+ **default off** for circular references → `BeanCurrentlyInCreationException`.

### Setter / Field cycle (works — but is a smell)

```java
@Service
class A {
    @Autowired B b;
}
@Service
class B {
    @Autowired A a;
}
```

→ Singleton beans → Spring partial-init trick allows resolution. **Pre-2.6** allowed. **2.6+** need:

```yaml
spring.main.allow-circular-references: true
```

### Why constructor fails?

Object create karne se pehle **complete dependencies** chahiye → A construction needs B, B needs A → infinite chain.

### Fixes (preferred order)

1. **Redesign** — extract third bean / interface
2. **`@Lazy`** on one side
3. **Setter / field injection** (last resort)

---

## `@Lazy` to Break Circular Dep

```java
@Service
class A {
    private final B b;
    public A(@Lazy B b) { this.b = b; }   // lazy proxy injected
}
```

→ Spring inject karta hai **proxy** → first method call par actual `B` resolve.

### Bean-level `@Lazy`

```java
@Component @Lazy
class HeavyService { ... }
```

→ First use par instantiate. Useful for slow startup.

⚠️ Lazy bean me startup-time problems baad mein surface hote — use cautiously.

---

## Bean Scopes

| Scope | Lifetime |
|-------|----------|
| **singleton** (default) | One per `ApplicationContext` |
| **prototype** | New on each `getBean()` |
| **request** | Per HTTP request (web only) |
| **session** | Per HTTP session |
| **application** | Per `ServletContext` |
| **websocket** | Per WS session |

```java
@Component
@Scope("prototype")
public class TaskRunner { ... }
```

### Important interview points

- **Singleton beans should be stateless** — shared.
- **Prototype destroy callbacks** Spring **does not call** — caller responsibility.
- **`request` scope** needs proxy when injected into singleton:

```java
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext { ... }
```

---

## Prototype Inside Singleton — Trap + Fixes

### Trap

```java
@Component class Singleton {
    @Autowired Prototype p;     // injected ONCE at startup → not new each time!
}
```

→ User assumes "har call new instance" — galat. Singleton mein reference fix.

### Fixes

#### 1. `ObjectProvider` (modern, idiomatic)

```java
@Component
class Singleton {
    private final ObjectProvider<Prototype> provider;
    public Singleton(ObjectProvider<Prototype> p) { this.provider = p; }
    
    public void use() {
        Prototype fresh = provider.getObject();
        fresh.run();
    }
}
```

#### 2. Lookup method (Spring will override at runtime)

```java
@Component
abstract class Singleton {
    public void use() { createPrototype().run(); }
    
    @Lookup
    protected abstract Prototype createPrototype();
}
```

#### 3. Inject `ApplicationContext` (least clean)

```java
@Autowired ApplicationContext ctx;
Prototype fresh = ctx.getBean(Prototype.class);
```

---

## Bean Lifecycle Callbacks

```java
@Component
class MyBean {
    
    public MyBean(Dep d) { ... }                  // 1. constructor
    
    @PostConstruct
    public void init() { ... }                    // 2. after deps wired
    
    @PreDestroy
    public void close() { ... }                   // on container shutdown
}
```

Other phases (less asked):

- `BeanPostProcessor.postProcessBeforeInitialization`
- `InitializingBean.afterPropertiesSet`
- `BeanPostProcessor.postProcessAfterInitialization`
- `DisposableBean.destroy`

→ **`@PostConstruct`** + constructor injection — 95% cases enough.

---

## `@Configuration` vs `@Component` for `@Bean`

```java
@Configuration                          @Component
class C {                               class C {
    @Bean A a() { return new A(); }         @Bean A a() { return new A(); }
    @Bean B b() { return new B(a()); }      @Bean B b() { return new B(a()); }
}                                       }
```

| | `@Configuration` | `@Component` (lite) |
|--|------------------|---------------------|
| `@Bean` calls cached | ✅ (CGLIB proxy) | ❌ (each call → new instance) |
| Inter-bean refs honor singleton | ✅ | ❌ — fresh objects! |

→ **Bean factories ke liye `@Configuration`** use karo (default in `@SpringBootApplication`).

---

## `ApplicationContext` vs `BeanFactory`

| | BeanFactory | ApplicationContext |
|--|-------------|-------------------|
| Eager init singletons | ❌ (lazy) | ✅ |
| Internationalization | ❌ | ✅ |
| Event publishing | ❌ | ✅ |
| BeanPostProcessor auto-detect | ❌ | ✅ |

→ Spring Boot mein practically `ApplicationContext` hi.

---

## Common Output Traps

### Q1. Multiple beans, no qualifier

```java
@Service public class A implements Pay {}
@Service public class B implements Pay {}

@Service
class Use {
    @Autowired Pay p;     // ❌ NoUniqueBeanDefinitionException
}
```

→ Add `@Primary` ya `@Qualifier`.

### Q2. Field name match by name

```java
@Component class fooService implements I {}
@Component class barService implements I {}

@Autowired I fooService;     // matched by name → fooService
```

### Q3. `@Configuration` vs `@Component`

```java
@Component
class Cfg {
    @Bean A a() { return new A(); }
    @Bean B b() { return new B(a()); }   // calls a() each time → multiple A!
}
```

→ Use `@Configuration` for singleton semantics.

### Q4. `@Async` on private method

```java
@Service class S {
    @Async public void doIt() { internal(); }      // proxied → async
    @Async private void internal() {}              // ❌ ignored (private + self call)
}
```

---

## Pitfalls

1. **Field injection in tests** — reflection / `ReflectionTestUtils` needed.
2. **Constructor circular dep + Boot 2.6+** — needs redesign / `@Lazy`.
3. **Prototype inside singleton** without provider → reference cached.
4. **`@Component` for config classes** — bean inter-call breaks singleton.
5. **`@Bean` returning interface** — Spring sees declared return type for autowiring.
6. **`@Qualifier` typos** — `NoSuchBeanDefinitionException` at startup.
7. **Calling `@PostConstruct` directly** — never call manually.
8. **Static fields** with `@Autowired` — silently `null`.
9. **Bean creation in static factory** without `@Bean` — Spring won't manage.
10. **Web scopes injection into singleton** without proxy mode → "no thread-bound request".

---

## Cheat Sheet

| Resolution Order | Step |
|------------------|------|
| 1 | Type match |
| 2 | `@Primary` |
| 3 | `@Qualifier` |
| 4 | By name |

| Injection | Use |
|-----------|-----|
| Constructor | Default |
| Setter | Optional / circular fix |
| Field | ❌ avoid |

| Scope | Use |
|-------|-----|
| singleton | Default |
| prototype | Stateful workers |
| request | Per HTTP req |

| Symptom | Fix |
|---------|-----|
| Circular dep error | Redesign / `@Lazy` |
| Multiple bean ambiguity | `@Primary` / `@Qualifier` |
| Stale prototype | `ObjectProvider` / `@Lookup` |
| Self-invocation skipping | Cross-ref `03-Transactional-Gotchas.md` |

---

## Practice

1. 2 implementations of same interface — observe ambiguity, fix with `@Primary`, then with `@Qualifier`.
2. Constructor cycle banayo (A↔B) — startup fail; fix with `@Lazy`.
3. Singleton bean me prototype inject karo — reference reuse confirm; ObjectProvider se fix.
4. `@Component` se `@Configuration` swap — bean count compare.
5. `@Async` private method — verify no async behaviour.
