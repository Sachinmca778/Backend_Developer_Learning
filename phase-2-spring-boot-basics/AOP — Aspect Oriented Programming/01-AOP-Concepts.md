# AOP Concepts

## Status: Not Started

---

## Table of Contents

1. [What is AOP?](#what-is-aop)
2. [Cross-Cutting Concerns](#cross-cutting-concerns)
3. [AOP Terminology](#aop-terminology)
4. [Aspect](#aspect)
5. [Advice Types](#advice-types)
6. [Pointcut](#pointcut)
7. [JoinPoint](#joinpoint)
8. [Weaving](#weaving)
9. [OOP vs AOP](#oop-vs-aop)

---

## What is AOP?

**AOP (Aspect Oriented Programming) ka matlab:** Aisa programming paradigm jo **cross-cutting concerns** ko alag se handle karta hai bina business logic ko touch kiye.

### The Problem Without AOP

```java
// ❌ Without AOP - Logging har method mein repeat ho rahi hai
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public User createUser(UserDto dto) {
        log.info("Creating user: {}", dto.getName());        // Logging
        try {
            User user = userRepository.save(mapToUser(dto));
            log.info("User created successfully: {}", user.getId());
            return user;
        } catch (Exception e) {
            log.error("Failed to create user: {}", e.getMessage());  // Logging
            throw e;
        }
    }

    public User updateUser(Long id, UserDto dto) {
        log.info("Updating user: {}", id);                    // Logging
        try {
            User user = userRepository.findById(id).orElseThrow();
            user.setName(dto.getName());
            userRepository.save(user);
            log.info("User updated successfully: {}", id);
            return user;
        } catch (Exception e) {
            log.error("Failed to update user {}: {}", id, e.getMessage());  // Logging
            throw e;
        }
    }

    public void deleteUser(Long id) {
        log.info("Deleting user: {}", id);                    // Logging
        try {
            userRepository.deleteById(id);
            log.info("User deleted successfully: {}", id);
            return;
        } catch (Exception e) {
            log.error("Failed to delete user {}: {}", id, e.getMessage());  // Logging
            throw e;
        }
    }
}
```

**Problem:** Har method mein logging code repeat ho raha hai. Business logic ke saath logging mix ho gayi hai.

### The Solution With AOP

```java
// ✅ With AOP - Business logic clean, logging alag
@Service
public class UserService {

    // Sirf business logic - koi logging nahi
    public User createUser(UserDto dto) {
        return userRepository.save(mapToUser(dto));
    }

    public User updateUser(Long id, UserDto dto) {
        User user = userRepository.findById(id).orElseThrow();
        user.setName(dto.getName());
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}

// ✅ Logging alag aspect mein
@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.example.service.UserService.*(..))")
    public Object logMethodCalls(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        log.info("Starting: {}", methodName);

        try {
            Object result = joinPoint.proceed();
            log.info("Completed: {} - Success", methodName);
            return result;
        } catch (Exception e) {
            log.error("Failed: {} - {}", methodName, e.getMessage());
            throw e;
        }
    }
}
```

---

## Cross-Cutting Concerns

**Matlab:** Woh functionality jo application ke multiple modules/layers mein common hoti hai — lekin business logic ka part nahi hai.

### Common Cross-Cutting Concerns

| Concern | Description | Example |
|---------|-------------|---------|
| **Logging** | Method calls log karna | Entry/exit logging, performance tracking |
| **Security** | Authorization check | Role validation, token validation |
| **Transactions** | Transaction management | @Transactional behavior |
| **Performance Monitoring** | Execution time track | Method kitna time leta hai |
| **Exception Handling** | Global error handling | Exception logging, fallback |
| **Caching** | Result caching | Cache check, cache populate |
| **Audit** | Activity tracking | Kaun kya kar raha hai |
| **Rate Limiting** | Request throttling | API rate limit check |

### Without AOP (Scattered)

```
UserService.createUser():
    → log.info("Creating user")          ← Logging concern
    → securityCheck(user)                ← Security concern
    → @Transactional                     ← Transaction concern
    → userRepository.save(user)          ← BUSINESS LOGIC
    → cache.put(user)                    ← Caching concern
    → audit.log("User created")          ← Audit concern

OrderService.createOrder():
    → log.info("Creating order")         ← Logging concern
    → securityCheck(user)                ← Security concern
    → @Transactional                     ← Transaction concern
    → orderRepository.save(order)        ← BUSINESS LOGIC
    → cache.put(order)                   ← Caching concern
    → audit.log("Order created")         ← Audit concern
```

**Same concerns multiple places — code duplication!**

### With AOP (Centralized)

```
LoggingAspect    → One place → Sab methods pe apply
SecurityAspect   → One place → Sab methods pe apply
TransactionAspect→ One place → Sab methods pe apply
AuditAspect      → One place → Sab methods pe apply

Business Logic   → Sirf core functionality
```

---

## AOP Terminology

### Key Terms Overview

```
Aspect       → Cross-cutting concern ka complete module (class)
Advice       → Kya karna hai aur kab karna hai (action + timing)
Pointcut     → Kahan apply karna hai (which methods)
JoinPoint    → Execution point (specific method call)
Weaving      → Aspect ko target code mein integrate karna
Target       → Woh object jisme aspect apply ho raha hai
Proxy        → Wrapped object (target + aspect)
```

---

## Aspect

**Matlab:** Ek complete module (class) jo ek cross-cutting concern ko encapsulate karta hai. `@Aspect` annotation se mark hota hai.

```java
// Complete Logging Aspect
@Aspect           // Yeh ek aspect hai
@Component        // Spring bean bhi hona chahiye
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // Advice + Pointcut combined
    @Around("execution(* com.example.service.*.*(..))")
    public Object logAllServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        log.info(">>> Calling: {}", methodName);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("<<< Completed: {} in {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("!!! Failed: {} after {}ms - {}", methodName, duration, e.getMessage());
            throw e;
        }
    }
}
```

---

## Advice Types

**Matlab:** Aspect ka **action** — kya karna hai aur **kab** karna hai.

### 5 Advice Types

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
    │
 @Around (wraps everything - most powerful)
```

| Advice | When it Runs | Use Case |
|--------|-------------|----------|
| **@Before** | Method call se pehle | Input validation, logging entry |
| **@After** | Method ke baad (success ya failure — dono mein) | Cleanup, resource release |
| **@AfterReturning** | Method successful return ke baad | Result logging, cache update |
| **@AfterThrowing** | Method exception throw karne pe | Error logging, notification |
| **@Around** | Method ke around (before + after + control) | Performance monitoring, security check |

---

## @Before Advice

**Matlab:** Target method execute hone se **pehle** run hota hai.

```java
@Aspect
@Component
public class BeforeAdviceExample {

    private static final Logger log = LoggerFactory.getLogger(BeforeAdviceExample.class);

    @Before("execution(* com.example.service.UserService.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        log.info("About to call: {} with args: {}", methodName, Arrays.toString(args));
    }
}

// UserService methods call hone se pehle yeh log print hoga
// "About to call: createUser with args: [UserDto(name=Sachin)]"
```

---

## @After Advice

**Matlab:** Target method complete hone ke **baad** run hota hai — chahe method successful ho ya exception throw kare.

```java
@Aspect
@Component
public class AfterAdviceExample {

    private static final Logger log = LoggerFactory.getLogger(AfterAdviceExample.class);

    @After("execution(* com.example.service.UserService.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("Finished: {} (success or failure)", methodName);
    }
}

// Yeh hamesha run hoga — method succeed kare ya fail kare
```

---

## @AfterReturning Advice

**Matlab:** Sirf tab run hota hai jab method **successfully** return kare. Exception aane pe nahi run hoga.

```java
@Aspect
@Component
public class AfterReturningAdviceExample {

    private static final Logger log = LoggerFactory.getLogger(AfterReturningAdviceExample.class);

    @AfterReturning(
        pointcut = "execution(* com.example.service.UserService.*(..))",
        returning = "result"
    )
    public void logSuccess(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        log.info("Successfully completed: {} - returned: {}", methodName, result);
    }
}

// Sirf tab run hoga jab method bina exception ke return kare
```

---

## @AfterThrowing Advice

**Matlab:** Sirf tab run hota hai jab method **exception throw** kare.

```java
@Aspect
@Component
public class AfterThrowingAdviceExample {

    private static final Logger log = LoggerFactory.getLogger(AfterThrowingAdviceExample.class);

    @AfterThrowing(
        pointcut = "execution(* com.example.service.UserService.*(..))",
        throwing = "exception"
    )
    public void logException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        log.error("Exception in: {} - {}", methodName, exception.getMessage());
    }
}

// Sirf tab run hoga jab method exception phenke
```

---

## @Around Advice

**Matlab:** Method ke **around** wrap hota hai — sabse powerful advice. Before, after, return value modify, exception handle — sab kuch kar sakta hai.

```java
@Aspect
@Component
public class AroundAdviceExample {

    private static final Logger log = LoggerFactory.getLogger(AroundAdviceExample.class);

    @Around("execution(* com.example.service.UserService.*(..))")
    public Object logAndMeasure(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();

        // BEFORE - method call se pehle
        log.info("Starting: {}", methodName);
        long startTime = System.currentTimeMillis();

        try {
            // METHOD EXECUTION - actual method yahan call hoti hai
            Object result = joinPoint.proceed();

            // AFTER RETURNING - success ke baad
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed: {} in {}ms", methodName, duration);

            return result;

        } catch (Exception e) {
            // AFTER THROWING - exception aane pe
            log.error("Failed: {} - {}", methodName, e.getMessage());
            throw e;  // Exception ko re-throw karo
        }

        // AFTER - finally block ki tarah (hamesha run)
        // Note: finally block mein likh sakte ho for cleanup
    }
}
```

### @Around vs Other Advices

```java
// ❌ Multiple advices for same concern
@Before("pointcut()")     → log entry
@After("pointcut()")      → log exit
@AfterReturning(...)      → log result
@AfterThrowing(...)       → log error

// ✅ Single @Around does everything
@Around("pointcut()")     → log entry + execute + log result/error + measure time
```

---

## Pointcut

**Matlab:** Expression jo define karta hai ki advice **kin methods** pe apply hoga.

```java
// Pointcut expression: UserService ki sab methods
@Around("execution(* com.example.service.UserService.*(..))")
                        ↑
                   Yeh hai pointcut expression

// Breakdown:
// execution(...)  → Method execution pe match karo
// *               → Koi bhi return type
// com.example.service.UserService.  → Is class ki
// *               → Koi bhi method name
// (..)            → Koi bhi arguments
```

---

## JoinPoint

**Matlab:** Ek specific execution point — usually ek method call. Advice ke parameter mein milta hai.

```java
@Aspect
@Component
public class JoinPointExample {

    @Before("execution(* com.example.service.*.*(..))")
    public void logDetails(JoinPoint joinPoint) {
        // Method ka naam
        String methodName = joinPoint.getSignature().getName();

        // Class ka naam
        String className = joinPoint.getSignature().getDeclaringTypeName();

        // Method ke arguments
        Object[] args = joinPoint.getArgs();

        // Full signature
        String signature = joinPoint.getSignature().toString();

        System.out.println("Class: " + className);
        System.out.println("Method: " + methodName);
        System.out.println("Args: " + Arrays.toString(args));
        System.out.println("Signature: " + signature);
    }
}
```

### ProceedingJoinPoint (@Around ke liye)

```java
@Around("execution(* com.example.service.*.*(..))")
public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    // Proceed se pehle — before logic
    System.out.println("Before method");

    // Method execute karo
    Object result = joinPoint.proceed();

    // Proceed ke baad — after logic
    System.out.println("After method");

    // Return value modify kar sakte ho
    return result;
}
```

---

## Weaving

**Matlab:** Aspect ko target object ke code mein integrate karne ka process. Aspect ka code target method ke around inject hota hai.

### Weaving Types

| Type | When | How | Used By |
|------|------|-----|---------|
| **Compile-time** | Compilation pe | Bytecode modification | AspectJ |
| **Load-time** | Class loading pe | ClassLoader weaving | AspectJ with LTW |
| **Runtime** | Runtime pe | Proxy creation | **Spring AOP** |

### Spring AOP Weaving (Runtime Proxy)

```
Original Object:
    UserService → createUser() → save()

After Weaving (Proxy):
    UserServiceProxy
        ├── @Before advice runs
        ├── Original createUser() runs
        ├── @AfterReturning advice runs
        └── Returns result

Client code ko pata nahi chalta — proxy transparent hai
```

### How Spring Creates Proxy

```java
// Without AOP
UserService userService = new UserService();
userService.createUser(dto);

// With AOP (what Spring does internally)
UserService realService = new UserService();
UserService proxy = createProxy(realService, loggingAspect, securityAspect);
// Client ko proxy milta hai
proxy.createUser(dto);
    → LoggingAspect.before()
    → SecurityAspect.check()
    → realService.createUser(dto)
    → LoggingAspect.after()
```

### JDK Dynamic Proxy vs CGLIB

```
JDK Dynamic Proxy (default for interfaces):
    → Interface implement karke proxy banata hai
    → Sirf interface methods proxy hote hain

CGLIB Proxy (default for classes without interface):
    → Target class extend karke proxy banata hai
    → Class methods proxy hote hain

@EnableAspectJAutoProxy(proxyTargetClass = true)  → Force CGLIB
```

---

## OOP vs AOP

```
OOP (Object Oriented Programming):
    → Vertical organization (modules/layers)
    → Inheritance, polymorphism, encapsulation
    → Business logic organize karta hai

    UserService
    OrderService
    PaymentService

AOP (Aspect Oriented Programming):
    → Horizontal organization (cross-cutting)
    → Aspects, advices, pointcuts
    → Logging, security, transactions organize karta ha

    LoggingAspect    ← UserService, OrderService, PaymentService sab pe apply
    SecurityAspect   ← UserService, OrderService, PaymentService sab pe apply
    AuditAspect      ← UserService, OrderService, PaymentService sab pe apply
```

---

## Quick Reference

```java
// Aspect = complete module
@Aspect
@Component
public class LoggingAspect { }

// Advice types
@Before("pointcut")          → Before method
@After("pointcut")           → After method (always)
@AfterReturning(pointcut = "pc", returning = "result")  → On success
@AfterThrowing(pointcut = "pc", throwing = "exception") → On error
@Around("pointcut")          → Around method (most powerful)

// JoinPoint
JoinPoint joinPoint          → Method info (name, args, class)
ProceedingJoinPoint pjp      → For @Around (has proceed())

// Weaving
Spring AOP uses runtime proxy (JDK Dynamic Proxy or CGLIB)
```

---

## Summary

| Term | Meaning |
|------|---------|
| **Cross-Cutting Concern** | Common functionality across modules (logging, security, etc.) |
| **Aspect** | Complete module for a cross-cutting concern (@Aspect class) |
| **Advice** | What to do + when (@Before, @After, @Around, etc.) |
| **Pointcut** | Where to apply (expression matching methods) |
| **JoinPoint** | Specific execution point (method call) |
| **Weaving** | Integrating aspect code into target code (runtime proxy in Spring AOP) |
