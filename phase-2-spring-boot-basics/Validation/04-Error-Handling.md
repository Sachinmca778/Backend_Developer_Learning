# Error Handling

## Status: Not Started

---

## Table of Contents

1. [MethodArgumentNotValidException](#methodargumentnotvalidexception)
2. [BindingResult](#bindingresult)
3. [FieldError](#fielderror)
4. [ObjectError](#objecterror)
5. [Extracting Errors Programmatically](#extracting-errors-programmatically)
6. [@ControllerAdvice for Global Error Response](#controlleradvice-for-global-error-response)
7. [Complete Error Handling Setup](#complete-error-handling-setup)

---

## MethodArgumentNotValidException

**Matlab:** Jab `@Valid` ya `@Validated` validation fail hota hai toh Spring yeh exception throw karta hai.

### Default Behavior

```java
@PostMapping("/api/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.ok(userService.create(request));
}
```

**Invalid Request:**
```json
{
  "name": "",
  "email": "invalid",
  "password": "123"
}
```

**Default Response (Spring Boot):**
```json
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='createUserRequest'. Error count: 3",
  "path": "/api/users"
}
```

**Problem:** Field-specific errors nahi milte — sirf generic message.

### Custom Exception Handler

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.toList());

    ErrorResponse response = new ErrorResponse(
        HttpStatus.BAD_REQUEST.value(),
        "Validation failed",
        errors,
        LocalDateTime.now()
    );

    return ResponseEntity.badRequest().body(response);
}
```

**Custom Response:**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": [
    "Name is required",
    "Email should be valid",
    "Password must be between 8 and 100 characters"
  ],
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## BindingResult

**Matlab:** Validation results ko programmatically access karne ka interface. `@Valid` ke baad parameter mein lo.

### Basic Usage

```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request,
                                     BindingResult result) {

    if (result.hasErrors()) {
        // Validation errors hain — custom response banao
        List<String> errors = result.getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        return ResponseEntity.badRequest()
            .body(Map.of("errors", errors));
    }

    // Validation pass hua
    User user = userService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
}
```

### BindingResult Methods

```java
BindingResult result;

// Check if errors exist
result.hasErrors();              // boolean
result.hasFieldErrors();         // boolean
result.hasGlobalErrors();        // boolean

// Get all errors
result.getAllErrors();           // List<ObjectError>

// Get field-specific errors
result.getFieldErrors();         // List<FieldError>
result.getFieldErrors("name");   // List<FieldError> for "name" field

// Get first error for a field
result.getFieldError("email");   // FieldError or null

// Get rejected value
result.getFieldValue("email");   // Object (the invalid value that was submitted)

// Error count
result.getErrorCount();          // int
```

### Multiple DTOs

```java
@PostMapping("/orders")
public ResponseEntity<?> createOrder(@Valid @RequestBody OrderRequest order,
                                     BindingResult orderResult,
                                     @Valid @RequestBody PaymentRequest payment,
                                     BindingResult paymentResult) {

    List<String> errors = new ArrayList<>();

    if (orderResult.hasErrors()) {
        errors.addAll(orderResult.getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .toList());
    }

    if (paymentResult.hasErrors()) {
        errors.addAll(paymentResult.getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .toList());
    }

    if (!errors.isEmpty()) {
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    // Process order
}
```

**⚠️ Note:** Ek method mein sirf ek `@RequestBody` ho sakta hai. Multiple DTOs ke liye wrapper class banao.

---

## FieldError

**Matlab:** Ek specific field ki validation error detail.

### Fields

```java
FieldError error = result.getFieldError("email");

error.getField();              // "email"
error.getRejectedValue();      // "invalid-email-value"
error.getDefaultMessage();     // "Email should be valid"
error.getCode();               // "Email" (annotation ka simple name)
error.getCodes();              // ["Email.userRequest.email", "Email.email", "Email.java.lang.String", "Email"]
error.getArguments();          // Annotation attributes (for message resolution)
```

### Usage in Error Response

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

    // Field → Error message map
    Map<String, String> fieldErrors = new HashMap<>();

    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
        fieldErrors.put(error.getField(), error.getDefaultMessage());
    }

    return ResponseEntity.badRequest().body(Map.of(
        "status", HttpStatus.BAD_REQUEST.value(),
        "message", "Validation failed",
        "errors", fieldErrors,
        "timestamp", LocalDateTime.now().toString()
    ));
}
```

**Response:**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "name": "Name is required",
    "email": "Email should be valid",
    "password": "Password must be between 8 and 100 characters"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

### Multiple Errors Per Field

```java
public class UserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
}
```

**Request:**
```json
{ "email": "" }
```

**Both validations fail:**
```java
List<FieldError> emailErrors = result.getFieldErrors("email");
emailErrors.size();  // 1 (sirf first failed validation return hoti hai)
emailErrors.get(0).getDefaultMessage();  // "Email is required" (@NotBlank pehle fail hua)
```

---

## ObjectError

**Matlab:** Pure object level ki error — kisi specific field se related nahi. Class-level constraints mein use hota hai.

### Usage

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

    // Field-level errors
    Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage
        ));

    // Object-level errors (class-level constraints)
    List<String> globalErrors = ex.getBindingResult().getGlobalErrors()
        .stream()
        .map(ObjectError::getDefaultMessage)
        .collect(Collectors.toList());

    return ResponseEntity.badRequest().body(Map.of(
        "status", HttpStatus.BAD_REQUEST.value(),
        "message", "Validation failed",
        "fieldErrors", fieldErrors,
        "globalErrors", globalErrors,
        "timestamp", LocalDateTime.now().toString()
    ));
}
```

### ObjectError vs FieldError

| Feature | FieldError | ObjectError |
|---------|-----------|-------------|
| **Scope** | Specific field | Entire object |
| **Source** | Field-level annotations | Class-level annotations |
| **Field Name** | Available | null |
| **Example** | `@NotBlank` on `name` | `@PasswordMatches` on class |

---

## Extracting Errors Programmatically

### Method 1: Simple List of Messages

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<List<String>> handleValidation(MethodArgumentNotValidException ex) {

    List<String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.toList());

    return ResponseEntity.badRequest().body(errors);
}
```

**Response:**
```json
[
  "Name is required",
  "Email should be valid",
  "Password must be between 8 and 100 characters"
]
```

### Method 2: Field → Message Map

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {

    Map<String, String> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            FieldError::getDefaultMessage,
            (existing, replacement) -> existing  // First error keep karo
        ));

    return ResponseEntity.badRequest().body(errors);
}
```

**Response:**
```json
{
  "name": "Name is required",
  "email": "Email should be valid",
  "password": "Password must be between 8 and 100 characters"
}
```

### Method 3: Detailed Field Errors

```java
public record FieldErrorResponse(
    String field,
    Object rejectedValue,
    String message
) { }

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<List<FieldErrorResponse>> handleValidation(MethodArgumentNotValidException ex) {

    List<FieldErrorResponse> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(error -> new FieldErrorResponse(
            error.getField(),
            error.getRejectedValue(),
            error.getDefaultMessage()
        ))
        .collect(Collectors.toList());

    return ResponseEntity.badRequest().body(errors);
}
```

**Response:**
```json
[
  {
    "field": "email",
    "rejectedValue": "invalid-email",
    "message": "Email should be valid"
  },
  {
    "field": "password",
    "rejectedValue": "123",
    "message": "Password must be between 8 and 100 characters"
  }
]
```

### Method 4: Nested Object Errors

```java
private Map<String, String> extractErrors(BindingResult result) {
    Map<String, String> errors = new LinkedHashMap<>();

    for (FieldError error : result.getFieldErrors()) {
        String fieldPath = error.getField();  // e.g., "address.city"
        errors.put(fieldPath, error.getDefaultMessage());
    }

    return errors;
}

@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

    return ResponseEntity.badRequest().body(Map.of(
        "status", HttpStatus.BAD_REQUEST.value(),
        "message", "Validation failed",
        "errors", extractErrors(ex.getBindingResult()),
        "timestamp", LocalDateTime.now().toString()
    ));
}
```

**Response (with nested errors):**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "name": "Name is required",
    "address.street": "Street is required",
    "address.city": "City is required",
    "address.zipCode": "Zip code must be 5-6 digits"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## @ControllerAdvice for Global Error Response

**Matlab:** Ek jagah saare controllers ke liye global error handling setup karna.

### Basic @ControllerAdvice

```java
@RestControllerAdvice  // @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler {

    // Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing
            ));

        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors,
            LocalDateTime.now().toString()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {

        ErrorResponse response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            null,
            LocalDateTime.now().toString()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Generic server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            null,
            LocalDateTime.now().toString()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

### ErrorResponse DTO

```java
public record ErrorResponse(
    int status,
    String message,
    Map<String, String> errors,
    String timestamp
) {
    // No errors version
    public ErrorResponse(int status, String message, String timestamp) {
        this(status, message, null, timestamp);
    }
}
```

### Specific vs Generic Exception Handlers

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Order matters — specific pehle, generic baad mein

    // 1. Validation — 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // ...
    }

    // 2. Type mismatch — 400
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid request body",
            LocalDateTime.now().toString()
        ));
    }

    // 3. Missing path variable — 400
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Missing parameter: " + ex.getParameterName(),
            LocalDateTime.now().toString()
        ));
    }

    // 4. Resource not found — 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now().toString()
        ));
    }

    // 5. Unauthorized — 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage(),
            LocalDateTime.now().toString()
        ));
    }

    // 6. Generic — 500 (last mein)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Log karo for debugging
        log.error("Unexpected error: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            LocalDateTime.now().toString()
        ));
    }
}
```

### @ControllerAdvice with Base Packages

```java
// Sirf specific packages ke controllers pe apply hoga
@RestControllerAdvice(basePackages = "com.example.api")
public class GlobalExceptionHandler { }

// Sirf specific controllers pe
@RestControllerAdvice(assignableTypes = {UserController.class, OrderController.class})
public class GlobalExceptionHandler { }
```

---

## Complete Error Handling Setup

### ErrorResponse DTOs

```java
public record ErrorResponse(
    int status,
    String message,
    Map<String, String> errors,
    String timestamp
) {
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, null, LocalDateTime.now().toString());
    }

    public static ErrorResponse validationError(Map<String, String> errors) {
        return new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors,
            LocalDateTime.now().toString()
        );
    }
}
```

### GlobalExceptionHandler

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing
            ));

        return ResponseEntity.badRequest().body(ErrorResponse.validationError(errors));
    }

    // Constraint violation (single field)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex) {

        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String fieldName = propertyPath.substring(propertyPath.lastIndexOf(".") + 1);
            errors.put(fieldName, violation.getMessage());
        }

        return ResponseEntity.badRequest().body(ErrorResponse.validationError(errors));
    }

    // Bad request
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
            .body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "Invalid request body"));
    }

    // Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    // Business exception
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse.of(HttpStatus.UNPROCESSABLE_ENTITY.value(), ex.getMessage()));
    }

    // Generic error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error"));
    }
}
```

### Custom Exception Classes

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
```

### Usage in Controllers

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        User user = userService.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return ResponseEntity.ok(toResponse(user));
    }
}
```

### API Response Examples

**Success:**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com"
}
```

**Validation Error (400):**
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "name": "Name is required",
    "email": "Email should be valid"
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**Not Found (404):**
```json
{
  "status": 404,
  "message": "User not found with id: 999",
  "errors": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

**Server Error (500):**
```json
{
  "status": 500,
  "message": "Internal server error",
  "errors": null,
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **MethodArgumentNotValidException** | Validation fail hone pe Spring throw karta hai |
| **BindingResult** | Validation results programmatically access karo |
| **FieldError** | Specific field ki error detail |
| **ObjectError** | Object-level error (class-level constraints) |
| **Extracting Errors** | `getFieldErrors()` → `getDefaultMessage()` → List/Map banao |
| **@ControllerAdvice** | Global error handling — sab controllers ke liye ek jagah |
| **@RestControllerAdvice** | @ControllerAdvice + @ResponseBody — JSON response |
| **Specific → Generic** | Exception handlers specific se generic order mein likho |
| **ErrorResponse DTO** | Consistent error response structure |
