# Spring AOP Implementation

## Status: Not Started

---

## Table of Contents

1. [Spring AOP Overview](#spring-aop-overview)
2. [Maven Dependency](#maven-dependency)
3. [@EnableAspectJAutoProxy](#enableaspectjautoproxy)
4. [@Aspect Annotation](#aspect-annotation)
5. [@Before Advice](#before-advice)
6. [@After Advice](#after-advice)
7. [@AfterReturning Advice](#afterreturning-advice)
8. [@AfterThrowing Advice](#afterthrowing-advice)
9. [@Around Advice (Most Powerful)](#around-advice-most-powerful)
10. [ProceedingJoinPoint](#proceedingjoinpoint)
11. [Complete Examples](#complete-examples)

---

## Spring AOP Overview

**Matlab:** Spring Framework ka AOP implementation. AspectJ ke annotations use karta hai par runtime pe **proxy-based** weaving karta hai.

### Spring AOP Limitations

```
Spring AOP sirf Spring beans pe kaam karta hai
→ @Component, @Service, @Repository, @Controller beans

Spring AOP sirf public methods pe kaam karta hai
→ private/protected methods pe aspect apply nahi hoga

Spring AOP sirf external calls pe kaam karta hai
→ Same class ke andar method call (self-invocation) pe aspect apply nahi hoga

Full AspectJ chahiye toh compile-time/load-time weaving use karo
```

---

## Maven Dependency

```xml
<!-- Spring Boot mein usually included hai spring-boot-starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Yeh internally include karta hai: -->
<!-- spring-aop, aspectjweaver, aspectjrt -->
```

### Gradle

```groovy
implementation 'org.springframework.boot:spring-boot-starter-aop'
```

---

## @EnableAspectJAutoProxy

**Matlab:** Spring ko bolta hai ki `@Aspect` annotated classes ko scan karo aur unke proxies banao.

### Spring Boot Auto-Configuration

```java
// Spring Boot mein usually auto-enabled hai
// spring-boot-starter-aop add karte hi automatic on ho jata hai

// Explicitly enable karna ho toh:
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {
    // @Aspect beans ab scan honge
}
```

### proxyTargetClass

```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)  // Force CGLIB
public class AopConfig { }

// proxyTargetClass = false (default):
// → Interface hai toh JDK Dynamic Proxy
// → Interface nahi hai toh CGLIB

// proxyTargetClass = true:
// → Hamesha CGLIB use karo (class-based proxy)
// → Interface methods ke alawa class methods bhi proxy honge
```

### Expose Proxy

```java
@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
public class AopConfig { }

// Self-invocation problem solve karne ke liye
// AopContext.currentProxy() se proxy access kar sakte ho
```

---

## @Aspect Annotation

**Matlab:** Ek class ko Aspect mark karta hai — cross-cutting concern ka module.

```java
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect     // Yeh ek aspect hai
@Component  // Spring bean bhi hona zaruri hai
public class LoggingAspect {

    // Advices yahan define honge
}
```

### Important: @Component Zaruri Hai

```java
// ❌ Wrong — Sirf @Aspect se Spring context mein register nahi hoga
@Aspect
public class LoggingAspect { }

// ✅ Correct — @Component se Spring bean banta hai
@Aspect
@Component
public class LoggingAspect { }

// Alternative — @Bean se bhi register kar sakte ho
@Configuration
public class AspectConfig {
    @Bean
    public LoggingAspect loggingAspect() {
        return new LoggingAspect();
    }
}
```

---

## @Before Advice

**Matlab:** Target method execute hone se pehle run hota hai.

### Basic @Before

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* com.example.service.UserService.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("Before: {}", methodName);
    }
}
```

### With Arguments Access

```java
@Aspect
@Component
@Slf4j
public class ValidationAspect {

    @Before("execution(* com.example.service.UserService.createUser(..)) && args(dto)")
    public void validateInput(JoinPoint joinPoint, UserDto dto) {
        if (dto.getUsername() == null || dto.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (dto.getEmail() == null || !dto.getEmail().contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        log.info("Input validated for createUser: {}", dto.getUsername());
    }
}
```

### With @annotation Pointcut

```java
// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    String value() default "";
}

// Aspect
@Aspect
@Component
@Slf4j
public class LogExecutionAspect {

    @Before("@annotation(logExecution)")
    public void logBeforeAnnotatedMethod(JoinPoint joinPoint, LogExecution logExecution) {
        String methodName = joinPoint.getSignature().getName();
        String description = logExecution.value();
        log.info("Starting {} - {}", methodName, description);
    }
}

// Usage
@Service
public class UserService {

    @LogExecution("Create a new user with validation")
    public User createUser(UserDto dto) {
        return userRepository.save(mapToUser(dto));
    }
}
```

---

## @After Advice

**Matlab:** Target method complete hone ke baad run hota hai — chahe success ho ya failure.

```java
@Aspect
@Component
@Slf4j
public class CleanupAspect {

    @After("execution(* com.example.service.*.*(..))")
    public void cleanupAfterMethod(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        log.info("Cleanup after: {}", methodName);
        // Resource cleanup, thread-local clear, etc.
        SecurityContextHolder.clearContext();
    }
}
```

### Important: @After ka Behavior

```java
@After("execution(* com.example.service.*.*(..))")
public void afterAdvice(JoinPoint joinPoint) {
    // Yeh hamesha run hoga:
    // → Method successfully return kare → run hoga
    // → Method exception throw kare    → bhi run hoga
    // → finally block ki tarah
}
```

---

## @AfterReturning Advice

**Matlab:** Sirf tab run hota hai jab method successfully return kare.

### Basic Usage

```java
@Aspect
@Component
@Slf4j
public class AuditAspect {

    @AfterReturning(
        pointcut = "execution(* com.example.service.UserService.*(..))",
        returning = "result"
    )
    public void auditSuccessfulOperation(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        log.info("Audit: {} completed successfully - result: {}", methodName, result);
    }
}
```

### With Specific Return Type

```java
@Aspect
@Component
@Slf4j
public class ResultLoggingAspect {

    // Sirf methods jo User return karte hain
    @AfterReturning(
        pointcut = "execution(* com.example.service.*.*(..)) && args(..)",
        returning = "user"
    )
    public void logUserResult(JoinPoint joinPoint, User user) {
        String methodName = joinPoint.getSignature().getName();
        log.info("Method {} returned user: {} ({})", methodName, user.getUsername(), user.getId());
    }
}
```

### Modify Return Value (Not Recommended but Possible)

```java
// @AfterReturning se return value MODIFY nahi kar sakte
// Uske liye @Around use karo

// ❌ Yeh kaam nahi karega
@AfterReturning(pointcut = "pc()", returning = "result")
public void modifyResult(Object result) {
    result = "modified";  // Original return value change nahi hoga
}

// ✅ @Around use karo for modification
@Around("pc()")
public Object modifyResult(ProceedingJoinPoint pjp) throws Throwable {
    Object result = pjp.proceed();
    return "modified";  // Yeh modified value return hoga
}
```

---

## @AfterThrowing Advice

**Matlab:** Sirf tab run hota hai jab method exception throw kare.

### Basic Usage

```java
@Aspect
@Component
@Slf4j
public class ExceptionLoggingAspect {

    @AfterThrowing(
        pointcut = "execution(* com.example.service.*.*(..))",
        throwing = "exception"
    )
    public void logException(JoinPoint joinPoint, Exception exception) {
        String methodName = joinPoint.getSignature().getName();
        log.error("Exception in {}: {}", methodName, exception.getMessage());
    }
}
```

### With Specific Exception Type

```java
@Aspect
@Component
@Slf4j
public class SpecificExceptionAspect {

    // Sirf RuntimeException aur uske subclasses
    @AfterThrowing(
        pointcut = "execution(* com.example.service.*.*(..))",
        throwing = "ex"
    )
    public void logRuntimeException(JoinPoint joinPoint, RuntimeException ex) {
        log.error("Runtime exception in {}: {}", joinPoint.getSignature().getName(), ex.getMessage());
    }

    // Sirf UserNotFoundException
    @AfterThrowing(
        pointcut = "execution(* com.example.service.UserService.*(..))",
        throwing = "ex"
    )
    public void logUserNotFound(JoinPoint joinPoint, UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        // Notification bhej sakte ho
        notificationService.alertAdmin("User lookup failed", ex.getMessage());
    }
}
```

### Exception Handling with Notification

```java
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorNotificationAspect {

    private final EmailService emailService;

    @AfterThrowing(
        pointcut = "execution(* com.example.service.*.*(..))",
        throwing = "ex"
    )
    public void handleCriticalException(JoinPoint joinPoint, Throwable ex) {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();

        log.error("Critical error in {}.{}: {}", className, methodName, ex.getMessage());

        // Admin ko email bhejo
        String subject = String.format("ERROR: %s.%s", className, methodName);
        String body = String.format("Exception: %s\nMessage: %s",
            ex.getClass().getSimpleName(), ex.getMessage());

        emailService.sendToAdmin(subject, body);
    }
}
```

---

## @Around Advice (Most Powerful)

**Matlab:** Method ke around wrap hota hai — before, after, return modify, exception handle — sab kuch ek hi advice mein.

### Complete @Around Example

```java
@Aspect
@Component
@Slf4j
public class PerformanceMonitoringAspect {

    @Around("execution(* com.example.service.*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. BEFORE - method call se pehle
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        log.info("Starting: {}", methodName);

        Object result = null;
        try {
            // 2. METHOD EXECUTION - actual method yahan call hoti hai
            result = joinPoint.proceed();

            // 3. AFTER RETURNING
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed: {} in {}ms", methodName, duration);

            // Slow method alert
            if (duration > 1000) {
                log.warn("Slow method: {} took {}ms", methodName, duration);
            }

            return result;

        } catch (Exception e) {
            // 4. AFTER THROWING
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed: {} after {}ms - {}", methodName, duration, e.getMessage());
            throw e;  // Exception ko re-throw karo
        }

        // 5. AFTER (finally)
        finally {
            // Cleanup if needed
        }
    }
}
```

### @Around Use Cases

```java
// 1. Return value modify
@Around("execution(* com.example.service.*.*(..))")
public Object modifyResponse(ProceedingJoinPoint pjp) throws Throwable {
    Object result = pjp.proceed();

    if (result instanceof String str) {
        return str.toUpperCase();  // Return value modify
    }

    return result;
}

// 2. Method skip karna (short-circuit)
@Around("@annotation(com.example.SkipOnMaintenance)")
public Object skipDuringMaintenance(ProceedingJoinPoint pjp) throws Throwable {
    if (maintenanceModeService.isUnderMaintenance()) {
        return "Service is under maintenance. Please try later.";
        // Original method call hi nahi hoga — pjp.proceed() skip
    }

    return pjp.proceed();  // Normal execution
}

// 3. Retry logic
@Around("@annotation(com.example.Retryable)")
public Object retry(ProceedingJoinPoint pjp) throws Throwable {
    int maxAttempts = 3;
    Exception lastException = null;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            return pjp.proceed();
        } catch (Exception e) {
            lastException = e;
            log.warn("Attempt {} failed: {}", attempt, e.getMessage());
            Thread.sleep(1000 * attempt);  // Exponential backoff
        }
    }

    throw lastException;  // Sab attempts fail ho gaye
}

// 4. Caching
@Around("@annotation(com.example.Cacheable)")
public Object cacheResult(ProceedingJoinPoint pjp) throws Throwable {
    String cacheKey = generateCacheKey(pjp);

    if (cache.containsKey(cacheKey)) {
        log.info("Cache hit: {}", cacheKey);
        return cache.get(cacheKey);  // Cached value return - method call skip
    }

    Object result = pjp.proceed();  // Method call karo
    cache.put(cacheKey, result);    // Result cache karo
    log.info("Cache miss - stored: {}", cacheKey);

    return result;
}
```

---

## ProceedingJoinPoint

**Matlab:** @Around advice mein use hota hai. `proceed()` method se actual target method ko call karta hai.

### Methods

```java
@Around("execution(* com.example.service.*.*(..))")
public Object aroundAdvice(ProceedingJoinPoint pjp) throws Throwable {

    // Method info get karo (JoinPoint se inherited)
    String methodName = pjp.getSignature().getName();
    String className = pjp.getSignature().getDeclaringTypeName();
    Object[] args = pjp.getArgs();

    // Proceed - actual method execute karo
    Object result = pjp.proceed();

    // Proceed with modified args
    Object[] newArgs = {"modified", 123};
    Object result2 = pjp.proceed(newArgs);

    return result;
}
```

### Important: proceed() Rules

```java
// Rule 1: proceed() call zaruri hai
@Around("pc()")
public Object advice(ProceedingJoinPoint pjp) throws Throwable {
    // pjp.proceed();  // ❌ Agar nahi call kiya toh target method run nahi hoga
    return "skipped";  // Target method bypass
}

// Rule 2: Multiple proceed possible (but careful!)
@Around("pc()")
public Object multipleProceed(ProceedingJoinPoint pjp) throws Throwable {
    pjp.proceed();  // First call
    pjp.proceed();  // Second call — method do baar run hoga!
    return null;
}

// Rule 3: Modified arguments
@Around("pc()")
public Object modifiedArgs(ProceedingJoinPoint pjp) throws Throwable {
    Object[] args = pjp.getArgs();
    args[0] = "sanitized-" + args[0];  // Argument modify karo
    return pjp.proceed(args);           // Modified args se method call
}

// Rule 4: Exception handling
@Around("pc()")
public Object exceptionHandling(ProceedingJoinPoint pjp) throws Throwable {
    try {
        return pjp.proceed();
    } catch (Exception e) {
        // Exception handle karo aur fallback value return karo
        return "fallback-value";
        // Ya exception re-throw karo
        // throw e;
    }
}
```

---

## Complete Examples

### Example 1: Complete Logging + Performance Aspect

```java
@Aspect
@Component
@Slf4j
public class CompleteLoggingAspect {

    @Around("execution(* com.example.service..*.*(..))")
    public Object logAndMeasure(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().toShortString();
        Object[] args = pjp.getArgs();

        log.info(">>> {} args={}", methodName, Arrays.toString(args));

        long startTime = System.currentTimeMillis();
        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - startTime;

            log.info("<<< {} result={} time={}ms", methodName, result, duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("!!! {} error={} time={}ms", methodName, e.getMessage(), duration);
            throw e;
        }
    }
}
```

### Example 2: Security Check Aspect

```java
@Aspect
@Component
@RequiredArgsConstructor
public class SecurityAspect {

    private final SecurityService securityService;

    @Around("@annotation(RequiresRole)")
    public Object checkRole(ProceedingJoinPoint pjp) throws Throwable {
        // Method pe @RequiresRole annotation se required role nikalo
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        RequiresRole annotation = method.getAnnotation(RequiresRole.class);

        String requiredRole = annotation.value();

        // Current user ka role check karo
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasRole = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_" + requiredRole));

        if (!hasRole) {
            log.warn("Access denied: {} requires ROLE_{}", pjp.getSignature().getName(), requiredRole);
            throw new AccessDeniedException("Insufficient permissions");
        }

        // Role hai → method execute karo
        return pjp.proceed();
    }
}

// Custom annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {
    String value();
}

// Usage
@Service
public class AdminService {

    @RequiresRole("ADMIN")
    public void deleteAllUsers() {
        userRepository.deleteAll();
    }

    @RequiresRole("MANAGER")
    public void generateReport() {
        reportService.generate();
    }
}
```

### Example 3: Transaction-like Aspect

```java
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionalAspect {

    private final PlatformTransactionManager transactionManager;

    @Around("@annotation(com.example.ManualTransactional)")
    public Object manageTransaction(ProceedingJoinPoint pjp) throws Throwable {
        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            Object result = pjp.proceed();
            transactionManager.commit(status);
            log.info("Transaction committed for: {}", pjp.getSignature().getName());
            return result;
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("Transaction rolled back for: {} - {}", pjp.getSignature().getName(), e.getMessage());
            throw e;
        }
    }
}
```

---

## Quick Reference

```java
// Setup
@EnableAspectJAutoProxy  // Enable AOP (auto-enabled in Spring Boot with starter-aop)

// Define aspect
@Aspect
@Component
public class MyAspect { }

// Advice types
@Before("execution(* com.example.service.*.*(..))")
@After("execution(* com.example.service.*.*(..))")
@AfterReturning(pointcut = "pc()", returning = "result")
@AfterThrowing(pointcut = "pc()", throwing = "exception")
@Around("execution(* com.example.service.*.*(..))")

// Around advice with ProceedingJoinPoint
@Around("pc()")
public Object advice(ProceedingJoinPoint pjp) throws Throwable {
    // Before
    Object result = pjp.proceed();  // Execute method
    // After
    return result;
}

// Limitations
→ Sirf Spring beans pe
→ Sirf public methods pe
→ Self-invocation pe nahi
```

---

## Summary

| Annotation | Purpose | Return Access? | Exception Access? |
|------------|---------|---------------|-------------------|
| **@Before** | Method se pehle | ❌ | ❌ |
| **@After** | Method ke baad (always) | ❌ | ❌ |
| **@AfterReturning** | Successful return pe | ✅ Yes | ❌ |
| **@AfterThrowing** | Exception throw hone pe | ❌ | ✅ Yes |
| **@Around** | Around wrap (most powerful) | ✅ Yes | ✅ Yes |
