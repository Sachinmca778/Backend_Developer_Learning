# Pointcut Expressions

## Status: Not Started

---

## Table of Contents

1. [Pointcut Expression Overview](#pointcut-expression-overview)
2. [execution()](#execution)
3. [within()](#within)
4. [@annotation()](#annotation)
5. [@within()](#within)
6. [args()](#args)
7. [bean()](#bean)
8. [Combining Pointcuts](#combining-pointcuts)
9. [Reusable Pointcuts (@Pointcut)](#reusable-pointcuts-pointcut)
10. [Pointcut Cheat Sheet](#pointcut-cheat-sheet)

---

## Pointcut Expression Overview

**Matlab:** Expression jo define karta hai ki advice **kin methods** pe apply hoga. AOP ka "WHERE" part.

### Pointcut Expression Types

| Designator | Matches | Example |
|------------|---------|---------|
| **execution()** | Method execution | `execution(* com.example.service.*.*(..))` |
| **within()** | Type ke andar ke methods | `within(com.example.service..*)` |
| **@annotation()** | Specific annotation wale methods | `@annotation(Transactional)` |
| **@within()** | Class-level annotation | `@within(RestController)` |
| **args()** | Method arguments | `args(java.io.Serializable)` |
| **@args()** | Argument annotations | `@args(com.example.Secure)` |
| **bean()** | Specific Spring bean | `bean(userService)` |
| **target()** | Target object type | `target(com.example.UserRepository)` |
| **this()** | Proxy object type | `this(com.example.UserRepository)` |

---

## execution()

**Matlab:** Sabse commonly used pointcut designator. Method execution ko match karta hai.

### Syntax

```
execution(modifiers-pattern? return-type-pattern declaring-type-pattern? method-name-pattern(param-pattern) throws-pattern?)

Sirf yeh parts commonly use hote hain:
execution(return-type  declaring-type.method-name(params))
```

### Basic Examples

```java
// UserService class ki SAB methods
@Around("execution(* com.example.service.UserService.*(..))")
//                  ↑   ↑                  ↑    ↑      ↑
//             return  fully qualified    class method any
//             type    class name         name name  params

// Breakdown:
// *                        → Koi bhi return type
// com.example.service.     → Package
// UserService              → Class name
// .                        → Dot separator
// *                        → Koi bhi method name
// (..)                     → Koi bhi number/type ke parameters
```

### Detailed Patterns

```java
// Sirf UserService.createUser method — koi bhi return type, koi bhi params
@Around("execution(* com.example.service.UserService.createUser(..))")

// Sirf void methods
@Around("execution(void com.example.service.UserService.*(..))")

// Sirf User return karne wali methods
@Around("execution(com.example.model.User com.example.service.UserService.*(..))")

// Sirf methods jo String aur int parameters lete hain
@Around("execution(* com.example.service.UserService.*(String, int))")

// Sirf methods jo EXACTLY ek parameter lete hain (koi bhi type)
@Around("execution(* com.example.service.UserService.(*))")

// Sirf no-argument methods
@Around("execution(* com.example.service.UserService.*())")
```

### Wildcard Patterns

```java
// Service package ki SAB classes ki SAB methods
@Around("execution(* com.example.service.*.*(..))")
//                           ↑ package  ↑ class ↑ method

// Service package + sub-packages
@Around("execution(* com.example.service..*.*(..))")
//                                 ↑↑ dots = includes sub-packages

// Specific method patterns
@Around("execution(* com.example.service.*.create*(..))")
//                                    ↑ create se start hone wali methods

@Around("execution(* com.example.service.*.*By*(..))")
//                                    ↑ Methods with "By" in name (findBy, deleteBy)

@Around("execution(* com.example.service.*.*And*(..))")
//                                    ↑ Methods with "And" (saveAndFlush, findByNameAndEmail)
```

### Practical Examples

```java
@Aspect
@Component
public class ExecutionPointcutExamples {

    // 1. All service methods
    @Around("execution(* com.example.service.*.*(..))")
    public Object allServices(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // 2. All repository methods
    @Around("execution(* com.example.repository.*.*(..))")
    public Object allRepositories(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // 3. All controller methods returning ResponseEntity
    @Around("execution(org.springframework.http.ResponseEntity com.example.controller.*.*(..))")
    public Object controllerResponses(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // 4. All save/update methods
    @Around("execution(* com.example.repository.*.save*(..)) || " +
            "execution(* com.example.repository.*.update*(..)) || " +
            "execution(* com.example.repository.*.delete*(..))")
    public Object dataModifyingMethods(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}
```

---

## within()

**Matlab:** Specific type (class/package) ke **andar** ke sab methods match karta hai. `execution()` se simpler — sirf type-based matching.

### Basic Usage

```java
// UserService class ke SAB methods
@Around("within(com.example.service.UserService)")

// Service package ki SAB classes ke methods
@Around("within(com.example.service.*)")

// Service package + sub-packages
@Around("within(com.example.service..*)")

// Specific interface implement karne wali classes
@Around("within(com.example.service.UserServiceImpl)")
```

### within() vs execution()

```java
// within — type pe match karta hai
@Around("within(com.example.service.UserService)")
// Yeh UserService class ke ANDAR ki sab methods match karega

// execution — method signature pe match karta hai
@Around("execution(* com.example.service.UserService.*(..))")
// Yeh UserService ki methods ko method signature se match karega

// Dono same result denge for simple cases
// within() simpler hai par execution() zyada flexible hai
```

### Practical Examples

```java
@Aspect
@Component
public class WithinPointcutExamples {

    // All methods in service layer
    @Around("within(com.example.service..*)")
    public Object serviceLayer(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // All methods in controller layer
    @Around("within(com.example.controller..*)")
    public Object controllerLayer(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // All methods in repository layer
    @Around("within(com.example.repository..*)")
    public Object repositoryLayer(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // Specific class
    @Around("within(com.example.service.UserService)")
    public Object userServiceOnly(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}
```

---

## @annotation()

**Matlab:** Methods jo specific **annotation** se marked hain. Sabse targeted approach — sirf annotated methods pe aspect apply hoga.

### Basic Usage

```java
// @Transactional methods
@Around("@annotation(org.springframework.transaction.annotation.Transactional)")
public Object transactionalMethods(ProceedingJoinPoint pjp) throws Throwable {
    return pjp.proceed();
}

// @Scheduled methods
@Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
public Object scheduledMethods(ProceedingJoinPoint pjp) throws Throwable {
    return pjp.proceed();
}
```

### Custom Annotation

```java
// Step 1: Custom annotation banao
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecution {
    String value() default "";
    boolean logArgs() default false;
    boolean logResult() default true;
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    String value();  // e.g., "user:read", "user:write"
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cacheable {
    String key() default "";
    long ttl() default 3600;  // Time to live in seconds
}

// Step 2: Aspect mein @annotation use karo
@Aspect
@Component
@Slf4j
public class CustomAnnotationAspects {

    // @LogExecution annotated methods
    @Around("@annotation(logExecution)")
    public Object logExecution(ProceedingJoinPoint pjp, LogExecution logExecution) throws Throwable {
        String description = logExecution.value();
        boolean logArgs = logExecution.logArgs();
        String methodName = pjp.getSignature().getName();

        if (logArgs) {
            log.info(">>> {} args={} - {}", methodName, Arrays.toString(pjp.getArgs()), description);
        }

        Object result = pjp.proceed();

        if (logExecution.logResult()) {
            log.info("<<< {} result={}", methodName, result);
        }

        return result;
    }

    // @RequiresPermission annotated methods
    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequiresPermission requiresPermission) throws Throwable {
        String requiredPermission = requiresPermission.value();

        // Permission check
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean hasPermission = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(requiredPermission));

        if (!hasPermission) {
            throw new AccessDeniedException("Missing permission: " + requiredPermission);
        }

        return pjp.proceed();
    }

    // @Cacheable annotated methods
    @Around("@annotation(cacheable)")
    public Object cacheResult(ProceedingJoinPoint pjp, Cacheable cacheable) throws Throwable {
        String cacheKey = generateKey(pjp, cacheable.key());

        if (cache.containsKey(cacheKey)) {
            log.info("Cache hit: {}", cacheKey);
            return cache.get(cacheKey);
        }

        Object result = pjp.proceed();
        cache.put(cacheKey, result, cacheable.ttl());
        log.info("Cache miss - stored: {}", cacheKey);

        return result;
    }
}

// Step 3: Usage
@Service
public class UserService {

    @LogExecution(value = "Creating user", logArgs = true, logResult = true)
    @RequiresPermission("user:write")
    @Cacheable(key = "#dto.username", ttl = 600)
    public User createUser(UserDto dto) {
        return userRepository.save(mapToUser(dto));
    }

    @LogExecution("Finding user by ID")
    @Cacheable(key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}
```

---

## @within()

**Matlab:** Class-level annotation check karta hai. Agar class pe specific annotation hai toh us class ke **sab methods** match honge.

### Usage

```java
// @Service annotated classes ke sab methods
@Around("@within(org.springframework.stereotype.Service)")

// @RestController annotated classes
@Around("@within(org.springframework.web.bind.annotation.RestController)")

// Custom annotation on class
@Around("@within(com.example.Secured)")
```

### @within() vs @annotation()

```java
// @annotation — method pe annotation check karta hai
@Around("@annotation(Cacheable)")
// → Sirf methods pe @Cacheable hai unko match karega

// @within — class pe annotation check karta hai
@Around("@within(Secured)")
// → Agar class pe @Secured hai toh us class ki SAB methods match hongi
```

### Practical Examples

```java
// @RestController classes ke sab methods — API logging
@Aspect
@Component
@Slf4j
public class ApiLoggingAspect {

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logApiCalls(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        long startTime = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("API: {} completed in {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("API: {} failed after {}ms - {}", methodName, duration, e.getMessage());
            throw e;
        }
    }
}

// @Repository classes — query logging
@Aspect
@Component
@Slf4j
public class QueryLoggingAspect {

    @Around("@within(org.springframework.stereotype.Repository)")
    public Object logQueries(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        log.info("Query: {}", methodName);
        return pjp.proceed();
    }
}
```

---

## args()

**Matlab:** Method ke **parameters ke type** pe match karta hai.

### Basic Usage

```java
// Methods jo Serializable parameter lete hain
@Around("args(java.io.Serializable)")

// Methods jo String parameter lete hain
@Around("args(String)")

// Methods jo Long parameter lete hain
@Around("args(Long)")
```

### With Multiple Parameters

```java
// Methods jo EXACTLY 2 parameters lete hain: String aur int
@Around("args(String, int)")

// Methods jinke pehle parameter ka type User hai
@Around("args(com.example.model.User, ..)")
//                                ↑↑ = aur koi bhi params ho sakte hain

// Methods jinke aakhri parameter ka type Pageable hai
@Around("args(.., org.springframework.data.domain.Pageable)")
```

### With Argument Access

```java
@Aspect
@Component
@Slf4j
public class ArgsPointcutExample {

    // String parameter wale methods
    @Around("args(username)")
    public Object stringArgMethod(ProceedingJoinPoint pjp, String username) throws Throwable {
        log.info("Method called with username: {}", username);
        return pjp.proceed();
    }

    // User parameter wale methods
    @Around("args(user)")
    public Object userArgMethod(ProceedingJoinPoint pjp, User user) throws Throwable {
        log.info("Method called with user: {} ({})", user.getUsername(), user.getEmail());
        return pjp.proceed();
    }

    // Multiple args
    @Around("args(username, email)")
    public Object twoArgsMethod(ProceedingJoinPoint pjp, String username, String email) throws Throwable {
        log.info("Creating user: {} with email: {}", username, email);
        return pjp.proceed();
    }
}
```

### args() vs execution()

```java
// execution — method signature mein type specify karo
@Around("execution(* com.example.service.UserService.createUser(String, String))")
// → Specific method (createUser) with specific params

// args — kisi bhi class ka method ho, bas params match hone chahiye
@Around("args(String, String)")
// → Koi bhi method jo 2 String params leta hai
```

---

## bean()

**Matlab:** Specific Spring **bean name** pe match karta hai. Sirf Spring beans ke saath kaam karta hai.

### Basic Usage

```java
// Sirf "userService" bean
@Around("bean(userService)")

// Wildcard — "user*" se start hone wale beans
@Around("bean(user*)")

// Wildcard — "*Service" se end hone wale beans
@Around("bean(*Service)")

// Multiple beans
@Around("bean(userService) || bean(orderService) || bean(paymentService)")
```

### Practical Examples

```java
@Aspect
@Component
@Slf4j
public class BeanPointcutExample {

    // Specific service
    @Around("bean(userService)")
    public Object userServiceOnly(ProceedingJoinPoint pjp) throws Throwable {
        log.info("UserService method: {}", pjp.getSignature().getName());
        return pjp.proceed();
    }

    // All service beans
    @Around("bean(*Service)")
    public Object allServices(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // All repository beans
    @Around("bean(*Repository)")
    public Object allRepositories(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // Exclude specific bean
    @Around("bean(*Service) && !bean(cacheService)")
    public Object allServicesExceptCache(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}
```

### Bean Name Convention

```
@Service
public class UserService { }          // Bean name: "userService"

@Service("customUserService")
public class UserService { }          // Bean name: "customUserService"

@Repository
public class UserRepository { }        // Bean name: "userRepository"

@RestController
public class UserController { }        // Bean name: "userController"
```

---

## Combining Pointcuts

**Matlab:** Multiple pointcut expressions ko logical operators se combine karna.

### Logical Operators

| Operator | Meaning | Example |
|----------|---------|---------|
| **&&** | AND — dono conditions match honi chahiye | `execution() && @annotation()` |
| **\|\|\** | OR — koi ek condition match honi chahiye | `bean(a) \|\| bean(b)` |
| **!** | NOT — condition match nahi honi chahiye | `!@annotation(Override)` |

### AND (&&)

```java
// UserService class + sirf @Transactional methods
@Around("execution(* com.example.service.UserService.*(..)) && @annotation(Transactional)")

// Service package + sirf methods jo User parameter lete hain
@Around("within(com.example.service..*) && args(com.example.model.User)")

// @RestController + sirf POST methods (execution pe name match)
@Around("@within(RestController) && execution(* *.*.post*(..))")

// *Service beans + sirf methods jo String return karte hain
@Around("bean(*Service) && execution(String *.*.*(..))")
```

### OR (||)

```java
// UserService YA OrderService
@Around("bean(userService) || bean(orderService)")

// Save YA Delete methods
@Around("execution(* com.example.repository.*.save*(..)) || " +
        "execution(* com.example.repository.*.delete*(..))")

// @Transactional YA @Scheduled methods
@Around("@annotation(Transactional) || @annotation(Scheduled)")

// Service layer YA Controller layer
@Around("within(com.example.service..*) || within(com.example.controller..*)")
```

### NOT (!)

```java
// Sab services EXCEPT cacheService
@Around("bean(*Service) && !bean(cacheService)")

// Sab methods EXCEPT toString, hashCode, equals
@Around("execution(* com.example.service.*.*(..)) && " +
        "!execution(* com.example.service.*.toString(..)) && " +
        "!execution(* com.example.service.*.hashCode(..)) && " +
        "!execution(* com.example.service.*.equals(..))")

// Sab methods EXCEPT @Override wale
@Around("execution(* com.example.service.*.*(..)) && !@annotation(Override)")
```

### Complex Combinations

```java
@Aspect
@Component
@Slf4j
public class ComplexPointcutExample {

    // Services mein @Transactional YA @Cacheable methods
    @Around("(within(com.example.service..*) && " +
             "(@annotation(Transactional) || @annotation(Cacheable)))")
    public Object transactionalOrCacheable(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // Controllers mein POST/PUT/DELETE methods (data modifying)
    @Around("within(com.example.controller..*) && " +
            "(execution(* *.post*(..)) || " +
             "execution(* *.put*(..)) || " +
             "execution(* *.delete*(..)))")
    public Object dataModifyingEndpoints(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }

    // Services mein sab methods EXCEPT getter/setter
    @Around("within(com.example.service..*) && " +
            "!execution(* com.example.service..*.get*(..)) && " +
            "!execution(* com.example.service..*.set*(..)) && " +
            "!execution(* com.example.service..*.is*(..))")
    public Object businessLogicMethods(ProceedingJoinPoint pjp) throws Throwable {
        return pjp.proceed();
    }
}
```

---

## Reusable Pointcuts (@Pointcut)

**Matlab:** Pointcut expression ko ek jagah define karo aur multiple advices mein reuse karo.

### Define Reusable Pointcut

```java
@Aspect
@Component
public class ReusablePointcuts {

    // Service layer — sab service methods
    @Pointcut("within(com.example.service..*)")
    public void serviceLayer() {}

    // Controller layer — sab controller methods
    @Pointcut("within(com.example.controller..*)")
    public void controllerLayer() {}

    // Repository layer
    @Pointcut("within(com.example.repository..*)")
    public void repositoryLayer() {}

    // Transactional methods
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethods() {}

    // Custom annotation
    @Pointcut("@annotation(com.example.LogExecution)")
    public void loggedMethods() {}

    // User-related methods
    @Pointcut("execution(* com.example.service.UserService.*(..))")
    public void userServiceMethods() {}

    // Data modifying methods
    @Pointcut("execution(* com.example.repository.*.save*(..)) || " +
              "execution(* com.example.repository.*.delete*(..)) || " +
              "execution(* com.example.repository.*.update*(..))")
    public void dataModifyingMethods() {}
}
```

### Use Reusable Pointcuts

```java
@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // Reusable pointcut references
    private final ReusablePointcuts pc;

    public LoggingAspect(ReusablePointcuts pc) {
        this.pc = pc;
    }

    // Use pointcut by method reference
    @Around("com.example.aop.ReusablePointcuts.serviceLayer()")
    public Object logServices(ProceedingJoinPoint pjp) throws Throwable {
        log.info("Service: {}", pjp.getSignature().getName());
        return pjp.proceed();
    }

    // Combine reusable pointcuts
    @Around("com.example.aop.ReusablePointcuts.controllerLayer() || " +
            "com.example.aop.ReusablePointcuts.serviceLayer()")
    public Object logControllersAndServices(ProceedingJoinPoint pjp) throws Throwable {
        log.info("Controller/Service: {}", pjp.getSignature().getName());
        return pjp.proceed();
    }

    // Same class mein define karo aur reference karo
    @Around("userServiceMethods()")
    public Object logUserOperations(ProceedingJoinPoint pjp) throws Throwable {
        log.info("User operation: {}", pjp.getSignature().getName());
        return pjp.proceed();
    }

    // Pointcut in same class
    @Pointcut("execution(* com.example.service.UserService.*(..))")
    public void userServiceMethods() {}
}
```

### Named Pointcuts with Parameters

```java
@Aspect
@Component
public class ParameterizedPointcuts {

    // Specific package
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceClasses() {}

    // Specific annotation on method
    @Pointcut("@annotation(com.example.Secured)")
    public void securedMethods() {}
}
```

---

## Pointcut Cheat Sheet

### Quick Reference

```java
// execution() — Method execution match karo
execution(* com.example.service.*.*(..))        // All service methods
execution(* com.example.service.UserService.*(..))  // UserService methods
execution(* *.create*(..))                       // Methods starting with "create"
execution(void *.*(..))                          // All void methods
execution(* *.*())                               // No-arg methods

// within() — Type ke andar ke methods
within(com.example.service.*)                    // Service package
within(com.example.service..*)                   // Service + sub-packages
within(com.example.service.UserService)          // Specific class

// @annotation() — Method-level annotation
@annotation(Transactional)                       // @Transactional methods
@annotation(com.example.LogExecution)           // Custom annotation
@annotation(com.example.RequiresRole)           // Custom annotation

// @within() — Class-level annotation
@within(Service)                                 // @Service classes
@within(RestController)                          // @RestController classes

// args() — Parameter types
args(String)                                     // Methods with String param
args(String, Long)                               // Methods with String + Long params
args(com.example.model.User)                     // Methods with User param
args(com.example.model.User, ..)                 // First param is User, then anything

// bean() — Spring bean name
bean(userService)                                // Specific bean
bean(*Service)                                   // Beans ending with "Service"
bean(user*)                                      // Beans starting with "user"

// Combining
execution(* com.example.service.*.*(..)) && @annotation(Transactional)
bean(*Service) || bean(*Repository)
within(com.example.service..*) && !bean(cacheService)
```

### Common Patterns

```java
// Layer-wise
@Around("within(com.example.service..*)")         // Service layer
@Around("within(com.example.controller..*)")      // Controller layer
@Around("within(com.example.repository..*)")      // Repository layer

// Method pattern
@Around("execution(* com.example.service.*.create*(..))")   // Create methods
@Around("execution(* com.example.service.*.find*(..))")     // Find methods
@Around("execution(* com.example.service.*.delete*(..))")   // Delete methods

// Annotation-based
@Around("@annotation(Transactional)")             // Transactional methods
@Around("@annotation(Scheduled)")                 // Scheduled methods
@Around("@annotation(Cacheable)")                 // Cacheable methods
@Around("@within(RestController)")                // REST controllers

// Bean-based
@Around("bean(*Service)")                         // All service beans
@Around("bean(*Repository)")                      // All repository beans
@Around("bean(*Controller)")                      // All controller beans
```

---

## Summary

| Designator | Matches | Most Common Use |
|------------|---------|-----------------|
| **execution()** | Method signatures | `execution(* com.example.service.*.*(..))` |
| **within()** | Types (classes/packages) | `within(com.example.service..*)` |
| **@annotation()** | Method annotations | `@annotation(Transactional)` |
| **@within()** | Class annotations | `@within(Service)` |
| **args()** | Parameter types | `args(String)`, `args(User, ..)` |
| **bean()** | Spring bean names | `bean(userService)`, `bean(*Service)` |
| **&&** | AND — both match | `execution() && @annotation()` |
| **\|\|\** | OR — either matches | `bean(a) \|\| bean(b)` |
| **!** | NOT — exclude | `!@annotation(Override)` |
