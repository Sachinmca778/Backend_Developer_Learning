# Best Practices

## Status: Not Started

---

## Table of Contents

1. [Never Swallow Exceptions](#never-swallow-exceptions)
2. [Log Before Rethrowing](#log-before-rethrowing)
3. [Don't Use Exceptions for Flow Control](#dont-use-exceptions-for-flow-control)
4. [Always Clean Up Resources](#always-clean-up-resources)
5. [Prefer Specific Exception Types](#prefer-specific-exception-types)
6. [Exception Handling Anti-Patterns](#exception-handling-anti-patterns)
7. [Exception Handling Checklist](#exception-handling-checklist)

---

## Never Swallow Exceptions

**Matlab:** Empty catch block — exception ko ignore mat karo.

### ❌ Bad: Swallowing Exception

```java
try {
    processPayment(order);
} catch (PaymentException e) {
    // ❌ Empty catch — exception silently ignored!
    // Payment fail ho gaya lekin kisi ko pata nahi chala
}
// User ko pata nahi chala ki payment fail hua — order process hota raha!
```

### ✅ Good: At Least Log

```java
try {
    processPayment(order);
} catch (PaymentException e) {
    // ✅ Log karo — pata chalega kya hua
    log.error("Payment failed for order: {}", order.getId(), e);
    throw e;  // Ya rethrow karo
}
```

### ✅ Better: Handle or Rethrow

```java
// Option 1: Handle karo
try {
    processPayment(order);
} catch (PaymentException e) {
    log.warn("Payment failed, using fallback for order: {}", order.getId());
    useFallbackPayment(order);
}

// Option 2: Wrap and rethrow
try {
    processPayment(order);
} catch (PaymentException e) {
    throw new OrderProcessingException("Failed to process order: " + order.getId(), e);
}

// Option 3: Return error response
try {
    processPayment(order);
} catch (PaymentException e) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", "Payment failed: " + e.getMessage()));
}
```

### Why Swallowing is Bad

```
1. Debugging impossible — kya galat hua pata nahi chalta
2. Data corruption — incomplete data save ho sakta hai
3. User experience — user ko pata nahi chalta ki kuch galat hua
4. Silent failures — production mein issue discover nahi hota
```

---

## Log Before Rethrowing

**Matlab:** Exception ko handle karne se pehle log karo — debugging aur monitoring ke liye.

### ❌ Bad: Rethrow Without Logging

```java
try {
    userRepository.save(user);
} catch (SQLException e) {
    throw new DataAccessException("Failed to save user", e);
    // ❌ Log nahi kiya — sirf exception wrap kiya
}
```

### ✅ Good: Log + Rethrow

```java
try {
    userRepository.save(user);
} catch (SQLException e) {
    // ✅ Log with context
    log.error("Failed to save user: name={}, email={}", user.getName(), user.getEmail(), e);
    throw new DataAccessException("Failed to save user: " + user.getName(), e);
}
```

### Logging Best Practices

```java
// ✅ Include context in log message
log.error("Payment failed: userId={}, amount={}, gateway={}", 
    userId, amount, gateway, e);

// ✅ Log exception stack trace
log.error("Database connection failed", e);  // e as last parameter

// ❌ Don't just log message — stack trace chahiye
log.error("Error: " + e.getMessage());  // ❌ Stack trace missing

// ✅ Use structured logging
log.error("Payment processing failed", 
    kv("userId", userId),
    kv("amount", amount),
    kv("gateway", gateway),
    e);
```

### When NOT to Log

```java
// ✅ Don't log — expected flow
if (user != null) {
    return user;
} else {
    return createDefaultUser();  // Normal case, no logging needed
}

// ❌ Log — unexpected error
try {
    return userRepository.findById(id);
} catch (SQLException e) {
    log.error("Database error fetching user: id={}", id, e);  // ✅ Log
    throw new DataAccessException("Failed to fetch user", e);
}
```

---

## Don't Use Exceptions for Flow Control

**Matlab:** Exceptions expensive hote hain — normal control flow ke liye use mat karo.

### ❌ Bad: Exceptions for Flow Control

```java
// ❌ Exception se loop control
public User findUser(List<User> users, String name) {
    try {
        for (User user : users) {
            if (user.getName().equals(name)) {
                throw new UserFoundException(user);  // ❌ Exception for flow control
            }
        }
    } catch (UserFoundException e) {
        return e.getUser();
    }
    return null;
}

// ❌ Exception se validation
public void processAge(int age) {
    try {
        if (age < 18) {
            throw new IllegalArgumentException("Too young");
        }
        // Process
    } catch (IllegalArgumentException e) {
        // Handle
    }
}
```

### ✅ Good: Normal Control Flow

```java
// ✅ Return value se control
public User findUser(List<User> users, String name) {
    for (User user : users) {
        if (user.getName().equals(name)) {
            return user;  // ✅ Normal return
        }
    }
    return null;  // Ya Optional.empty()
}

// ✅ Condition check before
public void processAge(int age) {
    if (age < 18) {
        log.warn("User too young: age={}", age);
        return;  // ✅ Early return
    }
    // Process
}
```

### Performance Comparison

```java
// Exception — slow (stack trace creation)
long start = System.nanoTime();
for (int i = 0; i < 100_000; i++) {
    try {
        throw new RuntimeException("test");
    } catch (RuntimeException e) {
        // Handle
    }
}
long exceptionTime = System.nanoTime() - start;  // ~500ms

// Normal if-check — fast
start = System.nanoTime();
for (int i = 0; i < 100_000; i++) {
    if (true) {
        // Handle
    }
}
long ifCheckTime = System.nanoTime() - start;  // ~1ms

// Exception handling is 500x slower!
```

---

## Always Clean Up Resources

**Matlab:** Database connections, file handles, network connections — sabko close karo.

### ❌ Bad: Resource Leak

```java
// ❌ File close nahi hoga agar exception aaya
FileInputStream fis = new FileInputStream("file.txt");
int data = fis.read();  // Exception yahan aaya toh file open rahegi
fis.close();  // Never reached
```

### ✅ Good: try-with-resources

```java
// ✅ Automatically close hoga
try (FileInputStream fis = new FileInputStream("file.txt")) {
    int data = fis.read();
} catch (IOException e) {
    log.error("File read failed", e);
}
```

### ✅ Good: finally Block (Legacy)

```java
FileInputStream fis = null;
try {
    fis = new FileInputStream("file.txt");
    int data = fis.read();
} catch (IOException e) {
    log.error("File read failed", e);
} finally {
    if (fis != null) {
        try {
            fis.close();
        } catch (IOException e) {
            log.error("Failed to close file", e);
        }
    }
}
```

### Resources to Always Clean Up

```java
// ✅ Database connections
try (Connection conn = dataSource.getConnection();
     Statement stmt = conn.createStatement();
     ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
    // Process results
}

// ✅ File handles
try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        System.out.println(line);
    }
}

// ✅ Network connections
try (Socket socket = new Socket("localhost", 8080);
     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
    // Communication
}

// ✅ Locks
Lock lock = new ReentrantLock();
lock.lock();
try {
    // Critical section
} finally {
    lock.unlock();  // ✅ Always unlock
}
```

---

## Prefer Specific Exception Types

**Matlab:** Generic `Exception` ya `RuntimeException` throw mat karo — specific type use karo.

### ❌ Bad: Generic Exception

```java
// ❌ Kya galat hua pata nahi chalta
throw new Exception("Something went wrong");
throw new RuntimeException("Error occurred");
```

### ✅ Good: Specific Exceptions

```java
// ✅ Caller ko exact pata kya galat hua
if (user == null) {
    throw new UserNotFoundException("User not found: " + id);
}

if (!user.isActive()) {
    throw new AccountDisabledException("Account is disabled: " + id);
}

if (!user.hasPermission("READ")) {
    throw new AccessDeniedException("User lacks READ permission: " + id);
}

if (order.getTotal() > user.getBalance()) {
    throw new InsufficientFundsException(
        "Required: " + order.getTotal() + ", Available: " + user.getBalance()
    );
}
```

### Exception Hierarchy Design

```
AppException (extends RuntimeException)
    ├── ValidationException
    ├── ResourceNotFoundException
    ├── AccessDeniedException
    ├── PaymentException
    │     ├── PaymentGatewayException
    │     └── InsufficientFundsException
    └── ExternalServiceException
          ├── EmailServiceException
          └── NotificationServiceException
```

### Mapping to HTTP Status

```java
@RestControllerAdvice
public class ExceptionMapper {
    
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(UserNotFoundException ex) {
        return ErrorResponse.of(404, ex.getMessage(), "USER_NOT_FOUND");
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponse.of(403, ex.getMessage(), "ACCESS_DENIED");
    }
    
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(ValidationException ex) {
        return ErrorResponse.of(400, ex.getMessage(), "VALIDATION_ERROR");
    }
    
    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleInsufficientFunds(InsufficientFundsException ex) {
        return ErrorResponse.of(422, ex.getMessage(), "INSUFFICIENT_FUNDS");
    }
}
```

---

## Exception Handling Anti-Patterns

### 1. Catch and Ignore

```java
// ❌ Anti-pattern
try {
    doSomething();
} catch (Exception e) {
    // Nothing — silently ignored
}
```

### 2. Catch Generic Exception

```java
// ❌ Too broad — sab exceptions ek saath handle
try {
    doSomething();
} catch (Exception e) {
    log.error("Error", e);
}

// ✅ Catch specific exceptions
try {
    doSomething();
} catch (IOException e) {
    log.error("IO error", e);
} catch (SQLException e) {
    log.error("Database error", e);
}
```

### 3. Throw from finally

```java
// ❌ finally se exception throw mat karo
try {
    doSomething();
} finally {
    throw new RuntimeException("Cleanup failed");  // Original exception lost!
}
```

### 4. Use Exception Class Name in Logic

```java
// ❌ Exception class name pe logic
catch (Exception e) {
    if (e.getClass().getName().contains("Timeout")) {
        retry();
    }
}

// ✅ Use instanceof or specific catch
catch (TimeoutException e) {
    retry();
}
```

### 5. Multiple Exceptions in One Catch

```java
// ❌ Different problems, same handling
catch (FileNotFoundException | NullPointerException e) {
    log.error("Error", e);
}

// ✅ Different handling for different problems
catch (FileNotFoundException e) {
    createDefaultFile();
} catch (NullPointerException e) {
    log.error("Null reference", e);
}
```

---

## Exception Handling Checklist

### ✅ Do

```
✅ Specific exception types use karo
✅ Exception ko log karo before rethrowing
✅ Context add karo exception message mein
✅ try-with-resources use karo for AutoCloseable
✅ Custom exceptions for business errors
✅ Exception chaining for root cause tracking
✅ Global exception handler for centralized handling
✅ Return appropriate HTTP status for API errors
```

### ❌ Don't

```
❌ Empty catch blocks (exception swallowing)
❌ Exceptions for normal flow control
❌ Generic Exception throw karo
❌ finally block se return karo
❌ Exception class name pe logic base karo
❌ Sensitive data exception message mein expose karo
❌ Stack trace end user ko dikhao
```

---

## Summary

| Practice | Key Point |
|----------|-----------|
| **Never Swallow** | Hamesha log ya rethrow karo — empty catch mat use karo |
| **Log Before Rethrow** | Context ke saath log karo — debugging ke liye |
| **No Flow Control** | Exceptions expensive hain — if/return use karo |
| **Clean Up Resources** | try-with-resources hamesha use karo |
| **Specific Types** | Generic Exception mat throw karo — specific type banao |
| **Anti-Patterns** | Catch-ignore, generic catch, throw-from-finally avoid karo |
| **Checklist** | Specific types, logging, context, try-with-resources, chaining |
