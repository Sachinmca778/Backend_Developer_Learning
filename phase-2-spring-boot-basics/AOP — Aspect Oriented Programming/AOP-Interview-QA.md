# AOP — Aspect Oriented Programming - Top 10 Interview Questions & Answers

> Covering: AOP Concepts, Cross-Cutting Concerns, Advice Types, Spring AOP Implementation, Pointcut Expressions, and Limitations

---

## Q1: AOP kya hai aur Cross-Cutting Concerns kya hote hain?

**Answer:**

**AOP (Aspect Oriented Programming)** ek programming paradigm hai jo cross-cutting concerns ko business logic se alag handle karta hai.

### Problem Without AOP:

```java
@Service
public class UserService {
    public User createUser(UserDto dto) {
        log.info("Creating user...");     // Logging — har method mein repeat
        try {
            User user = userRepository.save(mapToUser(dto));
            log.info("User created");
            return user;
        } catch (Exception e) {
            log.error("Failed", e);       // Logging — har method mein repeat
            throw e;
        }
    }
    // updateUser, deleteUser — same logging repeat...
}
```

**Problem:** Logging, security, transactions — har method mein same code repeat ho raha hai.

### Solution With AOP:

```java
@Service
public class UserService {
    public User createUser(UserDto dto) {
        return userRepository.save(mapToUser(dto));  // Sirf business logic
    }
}

@Aspect
@Component
public class LoggingAspect {
    @Around("execution(* com.example.service.*.*(..))")
    public Object logMethodCalls(ProceedingJoinPoint pjp) throws Throwable {
        log.info("Starting: {}", pjp.getSignature().getName());
        Object result = pjp.proceed();
        log.info("Completed: {}", pjp.getSignature().getName());
        return result;
    }
}
```

### Common Cross-Cutting Concerns:

| Concern | Example |
|---------|---------|
| **Logging** | Entry/exit logging, performance tracking |
| **Security** | Role validation, token check |
| **Transactions** | `@Transactional` behavior |
| **Caching** | Result caching |
| **Audit** | Activity tracking |
| **Exception Handling** | Global error logging |

### OOP vs AOP:

```
OOP:  Vertical organization — UserService, OrderService, PaymentService
AOP:  Horizontal organization — LoggingAspect sab pe apply
```

---

## Q2: AOP ki important terminology explain karo — Aspect, Advice, Pointcut, JoinPoint, Weaving?

**Answer:**

### Key Terms:

| Term | Meaning |
|------|---------|
| **Aspect** | Cross-cutting concern ka complete module (`@Aspect` class) |
| **Advice** | Kya karna hai aur kab (`@Before`, `@Around`, etc.) |
| **Pointcut** | Kahan apply karna hai (expression matching methods) |
| **JoinPoint** | Specific execution point (method call) |
| **Weaving** | Aspect ko target code mein integrate karna |
| **Target** | Woh object jisme aspect apply ho raha hai |
| **Proxy** | Wrapped object (target + aspect) |

### Visual:

```
Method Execution:
         │
    ┌────┼────┐
    │         │
 @Before   @After
    │         │
    │    (method runs)
    │         │
    │    @AfterReturning (success)
    │    @AfterThrowing  (exception)
    │
 @Around (wraps everything)
```

### Weaving in Spring AOP:

```
Original:  UserService → createUser() → save()

After Weaving (Proxy):
    UserServiceProxy
        ├── @Before advice runs
        ├── Original createUser() runs
        ├── @AfterReturning advice runs
        └── Returns result

Client ko pata nahi — proxy transparent hai
```

Spring AOP **runtime proxy** use karta hai — JDK Dynamic Proxy (interfaces) ya CGLIB (classes).

---

## Q3: Advice types — @Before, @After, @Around mein kya difference hai?

**Answer:**

| Advice | When it Runs | Use Case |
|--------|-------------|----------|
| **@Before** | Method se pehle | Input validation, logging entry |
| **@After** | Method ke baad (success ya failure) | Cleanup, resource release |
| **@AfterReturning** | Successful return pe | Result logging, cache update |
| **@AfterThrowing** | Exception throw pe | Error logging, notification |
| **@Around** | Method ke around (most powerful) | Performance monitoring, security |

### @Before:

```java
@Before("execution(* com.example.service.UserService.*(..))")
public void logBefore(JoinPoint joinPoint) {
    log.info("About to call: {}", joinPoint.getSignature().getName());
}
```

### @After:

```java
@After("execution(* com.example.service.*.*(..))")
public void cleanupAfter(JoinPoint joinPoint) {
    log.info("Finished: {} (success or failure)", joinPoint.getSignature().getName());
    // finally block ki tarah — hamesha run hoga
}
```

### @AfterReturning:

```java
@AfterReturning(pointcut = "execution(* com.example.service.*.*(..))", returning = "result")
public void logSuccess(JoinPoint joinPoint, Object result) {
    log.info("Success: {} returned {}", joinPoint.getSignature().getName(), result);
}
```

### @AfterThrowing:

```java
@AfterThrowing(pointcut = "execution(* com.example.service.*.*(..))", throwing = "ex")
public void logException(JoinPoint joinPoint, Exception ex) {
    log.error("Exception in {}: {}", joinPoint.getSignature().getName(), ex.getMessage());
}
```

### @Around — Most Powerful:

```java
@Around("execution(* com.example.service.*.*(..))")
public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.currentTimeMillis();
    log.info("Starting: {}", pjp.getSignature().getName());

    try {
        Object result = pjp.proceed();  // Actual method call
        log.info("Completed in {}ms", System.currentTimeMillis() - start);
        return result;
    } catch (Exception e) {
        log.error("Failed: {}", e.getMessage());
        throw e;
    }
}
```

**Short Interview Answer:**

`@Around` sabse powerful hai — before, after, return modify, exception handle sab ek mein. `@Before`/`@After` simpler hain specific use cases ke liye.

---

## Q4: Spring AOP kaise setup karte hain?

**Answer:**

### Dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

Spring Boot mein `spring-boot-starter-aop` add karte hi AOP auto-enabled ho jata hai.

### @EnableAspectJAutoProxy (Optional):

```java
@Configuration
@EnableAspectJAutoProxy
public class AopConfig { }

// proxyTargetClass = true → Force CGLIB proxy
@EnableAspectJAutoProxy(proxyTargetClass = true)

// exposeProxy = true → Self-invocation fix ke liye
@EnableAspectJAutoProxy(exposeProxy = true)
```

### Define Aspect:

```java
@Aspect     // Yeh ek aspect hai
@Component  // Spring bean zaruri hai — sirf @Aspect se register nahi hoga!
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.example.service..*.*(..))")
    public Object logAndMeasure(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        log.info(">>> {}", method);

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("<<< {} time={}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("!!! {} error={}", method, e.getMessage());
            throw e;
        }
    }
}
```

### Important Rules:

```java
// ❌ Wrong — @Component missing
@Aspect
public class LoggingAspect { }

// ✅ Correct
@Aspect
@Component
public class LoggingAspect { }
```

---

## Q5: Spring AOP ki limitations kya hain?

**Answer:**

Spring AOP proxy-based hai — kuch important limitations hain.

### Limitations:

| Limitation | Description |
|------------|-------------|
| **Sirf Spring beans** | `@Component`, `@Service`, `@Repository`, `@Controller` |
| **Sirf public methods** | private/protected pe aspect apply nahi hoga |
| **Sirf external calls** | Self-invocation pe aspect apply nahi hoga |
| **Runtime weaving only** | Compile-time weaving nahi (full AspectJ chahiye toh) |

### Self-Invocation Problem:

```java
@Service
@Transactional
public class UserService {

    public void methodA() {
        methodB();  // ❌ @Transactional ignore — same class call
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() {
        // Naya transaction nahi banega!
    }
}
```

**Fix:**

```java
@Autowired
private UserService self;  // Self-inject through proxy

public void methodA() {
    self.methodB();  // ✅ Proxy ke through — transaction kaam karega
}

// Ya
AopContext.currentProxy()  // exposeProxy = true required
```

### JDK Dynamic Proxy vs CGLIB:

| Proxy Type | When | How |
|------------|------|-----|
| **JDK Dynamic Proxy** | Interface hai | Interface implement karke proxy |
| **CGLIB** | No interface | Class extend karke proxy |

```java
@EnableAspectJAutoProxy(proxyTargetClass = true)  // Force CGLIB
```

**Interview Point:** `@Transactional`, `@Cacheable`, `@Async` sab Spring AOP proxies use karte hain — same limitations apply.

---

## Q6: Pointcut expressions — execution() aur within() kaise use karte hain?

**Answer:**

Pointcut expression define karta hai ki advice kin methods pe apply hoga.

### execution() — Most Common:

```
execution(return-type declaring-type.method-name(params))

execution(* com.example.service.UserService.*(..))
//        ↑   ↑                  ↑    ↑      ↑
//   return  package+class    method  any  any params
```

### Examples:

```java
// UserService ki sab methods
@Around("execution(* com.example.service.UserService.*(..))")

// Service package ki sab classes ki sab methods
@Around("execution(* com.example.service.*.*(..))")

// Service + sub-packages
@Around("execution(* com.example.service..*.*(..))")
//                                 ↑↑ dots = sub-packages

// Sirf create* methods
@Around("execution(* com.example.service.*.create*(..))")

// Sirf void methods
@Around("execution(void com.example.service.*.*(..))")
```

### within() — Type-Based:

```java
// UserService class ke sab methods
@Around("within(com.example.service.UserService)")

// Service package ki sab classes
@Around("within(com.example.service..*)")
```

### execution() vs within():

| Feature | execution() | within() |
|---------|-------------|----------|
| Matches | Method signature | Type (class/package) |
| Flexibility | More (return type, method name, params) | Simpler |
| Use | Specific method patterns | Entire layer/class |

### Layer-wise Pointcuts:

```java
@Around("within(com.example.service..*)")     // Service layer
@Around("within(com.example.controller..*)")  // Controller layer
@Around("within(com.example.repository..*)")   // Repository layer
```

---

## Q7: @annotation() aur custom annotation based AOP kaise implement karte hain?

**Answer:**

`@annotation()` sirf annotated methods pe aspect apply karta hai — sabse targeted approach.

### Step 1: Custom Annotation:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    String value() default "";
    boolean logArgs() default false;
}
```

### Step 2: Aspect:

```java
@Aspect
@Component
@Slf4j
public class LogExecutionAspect {

    @Around("@annotation(logExecution)")
    public Object logExecution(ProceedingJoinPoint pjp, LogExecution logExecution) throws Throwable {
        String method = pjp.getSignature().getName();

        if (logExecution.logArgs()) {
            log.info(">>> {} args={}", method, Arrays.toString(pjp.getArgs()));
        }

        Object result = pjp.proceed();
        log.info("<<< {} result={}", method, result);
        return result;
    }
}
```

### Step 3: Usage:

```java
@Service
public class UserService {

    @LogExecution(value = "Creating user", logArgs = true)
    public User createUser(UserDto dto) {
        return userRepository.save(mapToUser(dto));
    }

    @LogExecution("Finding user")
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}
```

### @within() — Class-Level:

```java
// @RestController classes ke sab methods
@Around("@within(org.springframework.web.bind.annotation.RestController)")
public Object logApiCalls(ProceedingJoinPoint pjp) throws Throwable { }

// @Service classes ke sab methods
@Around("@within(org.springframework.stereotype.Service)")
public Object logServices(ProceedingJoinPoint pjp) throws Throwable { }
```

### @annotation() vs @within():

| Feature | @annotation() | @within() |
|---------|---------------|-----------|
| Checks | Method-level annotation | Class-level annotation |
| Scope | Sirf annotated methods | Class ki sab methods |

---

## Q8: Pointcuts combine kaise karte hain aur @Pointcut se reuse kaise karte hain?

**Answer:**

### Logical Operators:

| Operator | Meaning | Example |
|----------|---------|---------|
| **&&** | AND | `execution() && @annotation()` |
| **\|\|** | OR | `bean(a) \|\| bean(b)` |
| **!** | NOT | `!@annotation(Override)` |

### Examples:

```java
// UserService + @Transactional methods
@Around("execution(* com.example.service.UserService.*(..)) && @annotation(Transactional)")

// Save YA Delete methods
@Around("execution(* com.example.repository.*.save*(..)) || " +
        "execution(* com.example.repository.*.delete*(..))")

// Sab services EXCEPT cacheService
@Around("bean(*Service) && !bean(cacheService)")
```

### Reusable @Pointcut:

```java
@Aspect
@Component
public class ReusablePointcuts {

    @Pointcut("within(com.example.service..*)")
    public void serviceLayer() {}

    @Pointcut("within(com.example.controller..*)")
    public void controllerLayer() {}

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethods() {}
}

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("com.example.aop.ReusablePointcuts.serviceLayer()")
    public Object logServices(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // Same class mein define + reference
    @Pointcut("execution(* com.example.service.UserService.*(..))")
    public void userServiceMethods() {}

    @Around("userServiceMethods()")
    public Object logUserOps(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}
```

### bean() Pointcut:

```java
@Around("bean(userService)")       // Specific bean
@Around("bean(*Service)")           // All *Service beans
@Around("bean(*Repository)")        // All *Repository beans
```

---

## Q9: @Around advice ke practical use cases kya hain?

**Answer:**

`@Around` sabse powerful advice hai — before, after, return modify, exception handle, method skip — sab kuch.

### 1. Performance Monitoring:

```java
@Around("execution(* com.example.service..*.*(..))")
public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
    long start = System.currentTimeMillis();
    Object result = pjp.proceed();
    long duration = System.currentTimeMillis() - start;

    if (duration > 1000) {
        log.warn("Slow method: {} took {}ms", pjp.getSignature().getName(), duration);
    }
    return result;
}
```

### 2. Security Check:

```java
@Around("@annotation(requiresRole)")
public Object checkRole(ProceedingJoinPoint pjp, RequiresRole requiresRole) throws Throwable {
    if (!securityService.hasRole(requiresRole.value())) {
        throw new AccessDeniedException("Insufficient permissions");
    }
    return pjp.proceed();
}
```

### 3. Caching:

```java
@Around("@annotation(cacheable)")
public Object cacheResult(ProceedingJoinPoint pjp, Cacheable cacheable) throws Throwable {
    String key = generateKey(pjp);

    if (cache.containsKey(key)) {
        return cache.get(key);  // Method call skip
    }

    Object result = pjp.proceed();
    cache.put(key, result);
    return result;
}
```

### 4. Retry Logic:

```java
@Around("@annotation(retryable)")
public Object retry(ProceedingJoinPoint pjp) throws Throwable {
    for (int attempt = 1; attempt <= 3; attempt++) {
        try {
            return pjp.proceed();
        } catch (Exception e) {
            if (attempt == 3) throw e;
            Thread.sleep(1000 * attempt);
        }
    }
    return null;
}
```

### 5. Method Skip (Short-Circuit):

```java
@Around("@annotation(skipOnMaintenance)")
public Object skipDuringMaintenance(ProceedingJoinPoint pjp) throws Throwable {
    if (maintenanceModeService.isUnderMaintenance()) {
        return "Service under maintenance";  // pjp.proceed() skip
    }
    return pjp.proceed();
}
```

### ProceedingJoinPoint Rules:

```java
// proceed() call zaruri hai — warna target method run nahi hoga
Object result = pjp.proceed();

// Modified arguments
Object[] args = pjp.getArgs();
args[0] = "sanitized";
return pjp.proceed(args);
```

---

## Q10: AOP real-world examples — Logging, Security, Transactions?

**Answer:**

### Example 1: Complete Logging Aspect:

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.example.service..*.*(..))")
    public Object logAndMeasure(ProceedingJoinPoint pjp) throws Throwable {
        String method = pjp.getSignature().toShortString();
        log.info(">>> {} args={}", method, Arrays.toString(pjp.getArgs()));

        long start = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            log.info("<<< {} time={}ms", method, System.currentTimeMillis() - start);
            return result;
        } catch (Exception e) {
            log.error("!!! {} error={}", method, e.getMessage());
            throw e;
        }
    }
}
```

### Example 2: Security Aspect:

```java
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint pjp, RequiresRole requiresRole) throws Throwable {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasRole = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + requiresRole.value()));

        if (!hasRole) {
            throw new AccessDeniedException("Missing role: " + requiresRole.value());
        }
        return pjp.proceed();
    }
}

// Usage
@RequiresRole("ADMIN")
public void deleteAllUsers() { }
```

### Example 3: @Transactional (Built-in AOP):

```java
@Transactional  // Spring AOP proxy banata hai
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    // Sab operations ek transaction mein
    // Exception → rollback
}
```

### Common AOP Use Cases in Spring:

| Feature | AOP-Based? |
|---------|-----------|
| `@Transactional` | ✅ Yes |
| `@Cacheable` | ✅ Yes |
| `@Async` | ✅ Yes |
| `@Secured` | ✅ Yes |
| Custom logging | ✅ Yes (manual aspect) |

### Best Practices:

- `@Around` prefer karo jab multiple concerns ek saath handle karne hon
- `@annotation()` use karo targeted control ke liye
- Reusable `@Pointcut` define karo
- Self-invocation ka dhyan rakho
- Aspect class pe `@Component` zarur lagao

**Short Interview Answer:**

AOP cross-cutting concerns (logging, security, transactions, caching) ko business logic se alag rakhta hai. Spring AOP runtime proxies use karta hai — sirf public methods on Spring beans pe kaam karta hai.
