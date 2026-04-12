# Custom Exceptions

## Status: Not Started

---

## Table of Contents

1. [Extending Exception vs RuntimeException](#extending-exception-vs-runtimeexception)
2. [Custom Fields & Constructors](#custom-fields--constructors)
3. [Exception Chaining (Cause)](#exception-chaining-cause)
4. [Checked vs Unchecked — When to Use What](#checked-vs-unchecked--when-to-use-what)
5. [Real-World Examples](#real-world-examples)

---

## Extending Exception vs RuntimeException

**Matlab:** Apni custom exception class banana — `Exception` ya `RuntimeException` ko extend karo.

### Custom Checked Exception

```java
// Checked exception — handle karna zaruri hai
public class PaymentFailedException extends Exception {
    
    // Default constructor
    public PaymentFailedException() {
        super("Payment failed");
    }

    // Message ke saath
    public PaymentFailedException(String message) {
        super(message);
    }

    // Cause ke saath
    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Usage — throws declare karna padega
public void processPayment(double amount) throws PaymentFailedException {
    if (amount <= 0) {
        throw new PaymentFailedException("Invalid amount: " + amount);
    }
    // Payment logic
}
```

### Custom Unchecked Exception

```java
// Unchecked exception — handle karna zaruri nahi
public class InvalidInputException extends RuntimeException {
    
    public InvalidInputException() {
        super("Invalid input");
    }

    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Usage — throws declare nahi karna padta
public void setUserAge(int age) {
    if (age < 0 || age > 150) {
        throw new InvalidInputException("Invalid age: " + age);
    }
    this.age = age;
}
```

---

## Custom Fields & Constructors

**Matlab:** Exception mein extra information store karna — error code, validation errors, etc.

### Example: Error Code ke Saath

```java
public class BusinessException extends RuntimeException {
    
    private final String errorCode;
    private final String details;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BusinessException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    // Getters
    public String getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }
}

// Usage
throw new BusinessException(
    "PAYMENT_001",
    "Payment gateway timeout",
    "Gateway: Stripe, Timeout: 30s"
);

// Exception handler mein access
catch (BusinessException e) {
    System.out.println("Error Code: " + e.getErrorCode());
    System.out.println("Message: " + e.getMessage());
    System.out.println("Details: " + e.getDetails());
}
```

### Example: Validation Errors ke Saath

```java
public class ValidationException extends RuntimeException {
    
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation failed with " + errors.size() + " errors");
        this.errors = new ArrayList<>(errors);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}

// Usage
List<String> errors = new ArrayList<>();
if (name == null || name.isBlank()) errors.add("Name is required");
if (email == null || !email.contains("@")) errors.add("Email is invalid");
if (age < 18) errors.add("Must be 18+");

if (!errors.isEmpty()) {
    throw new ValidationException(errors);
}
```

### Example: HTTP Status ke Saath

```java
public class ApiException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String message, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

// Usage
throw new ApiException(
    HttpStatus.NOT_FOUND,
    "User not found with id: 123",
    "USER_NOT_FOUND"
);
```

---

## Exception Chaining (Cause)

**Matlab:** Original exception ko custom exception ke andar wrap karna — root cause track karna.

### Wrapping Exceptions

```java
public class DataAccessException extends RuntimeException {
    
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Usage
public User findById(Long id) {
    try {
        return userRepository.findById(id).orElse(null);
    } catch (SQLException e) {
        // Original exception ko wrap karo
        throw new DataAccessException("Failed to fetch user: " + id, e);
    }
}
```

### Exception Chain Access

```java
try {
    service.processData();
} catch (DataAccessException e) {
    System.out.println("Message: " + e.getMessage());
    
    // Root cause dhundho
    Throwable cause = e;
    while (cause.getCause() != null) {
        cause = cause.getCause();
    }
    System.out.println("Root cause: " + cause.getClass().getSimpleName());
    System.out.println("Root message: " + cause.getMessage());
}
```

### Real-World Example: Layered Exception

```java
// Repository layer
public class UserRepository {
    public User findById(Long id) throws SQLException {
        // Database call
        throw new SQLException("Connection refused");
    }
}

// Service layer
public class UserService {
    public User getUser(Long id) {
        try {
            return userRepository.findById(id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to access user data", e);
        }
    }
}

// Controller layer
public class UserController {
    public UserResponse handleRequest(Long id) {
        try {
            User user = userService.getUser(id);
            return toResponse(user);
        } catch (DataAccessException e) {
            throw new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to process request",
                "DATA_ACCESS_ERROR"
            );
        }
    }
}

// Exception chain:
// ApiException → DataAccessException → SQLException
```

---

## Checked vs Unchecked — When to Use What

### Use Checked Exception When:

```java
// ✅ Caller ko recover karne ka chance hai
public class RetryableException extends Exception {
    private final int retryAfterSeconds;
    
    public RetryableException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
    
    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}

// ✅ External resource problems
public class NetworkException extends Exception {
    public NetworkException(String message) {
        super(message);
    }
}

// ✅ Caller ko explicitly handle karna chahiye
public class InsufficientFundsException extends Exception {
    private final double required;
    private final double available;
    
    public InsufficientFundsException(double required, double available) {
        super("Required: " + required + ", Available: " + available);
        this.required = required;
        this.available = available;
    }
}
```

### Use Unchecked Exception When:

```java
// ✅ Programming error
public class NullArgumentException extends RuntimeException {
    public NullArgumentException(String paramName) {
        super("Parameter '" + paramName + "' cannot be null");
    }
}

// ✅ API misuse
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String message) {
        super(message);
    }
}

// ✅ Validation failure
public class ValidationException extends RuntimeException {
    private final Map<String, String> fieldErrors;
    
    public ValidationException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = fieldErrors;
    }
}
```

### Decision Guide

```
Exception chahiye?
├── Caller recover kar sakta hai? → Checked Exception
├── External resource problem? → Checked Exception
├── Caller ko explicitly handle karna chahiye? → Checked Exception
├── Programming error hai? → RuntimeException
├── API misuse indicate karta hai? → RuntimeException
├── Validation failure hai? → RuntimeException
└── Caller recover nahi kar sakta? → RuntimeException
```

---

## Real-World Examples

### Spring Boot Global Exception Handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            ex.getErrors(),
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error",
            null,
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### Builder Pattern for Exceptions

```java
public class AppException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;
    private final String details;

    private AppException(Builder builder) {
        super(builder.message);
        this.status = builder.status;
        this.errorCode = builder.errorCode;
        this.details = builder.details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getDetails() { return details; }

    public static class Builder {
        private HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        private String message;
        private String errorCode;
        private String details;
        private Throwable cause;

        public Builder status(HttpStatus status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder details(String details) {
            this.details = details;
            return this;
        }

        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        public AppException build() {
            AppException ex = new AppException(this);
            return ex;
        }
    }
}

// Usage
throw AppException.builder()
    .status(HttpStatus.BAD_REQUEST)
    .message("Invalid request")
    .errorCode("VALIDATION_ERROR")
    .details("Email format is invalid")
    .build();
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Checked Exception** | `extends Exception` — handle karna zaruri |
| **Unchecked Exception** | `extends RuntimeException` — handle optional |
| **Custom Fields** | Error codes, validation errors, HTTP status add karo |
| **Exception Chaining** | `super(message, cause)` — root cause preserve karo |
| **Checked Use When** | Caller recover kar sakta hai, external resource problems |
| **Unchecked Use When** | Programming error, API misuse, validation failure |
| **Builder Pattern** | Complex exceptions ke liye — readable aur flexible |
| **Global Handler** | `@RestControllerAdvice` — centralized exception handling |
