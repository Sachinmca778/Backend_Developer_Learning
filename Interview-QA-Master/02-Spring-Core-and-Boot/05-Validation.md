# Validation - Top 10 Interview Questions & Answers

> Covering: Bean Validation (JSR-380), @Valid vs @Validated, Custom Validators, Nested/Group Validation, and Error Handling

---

## Q1: Bean Validation (JSR-380) kya hai aur Spring Boot mein kaise use karte hain?

**Answer:**

Bean Validation (JSR-380) Java objects pe validation rules lagane ka standard hai. Isse invalid data controller/service tak pahunchne se pehle hi reject ho jata hai.

### Setup:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### How It Works:

```
Client Request (JSON)
    ↓
@RequestBody → Java Object
    ↓
@Valid / @Validated annotation
    ↓
Validator checks all constraint annotations
    ↓
All valid → Controller method runs
Any invalid → 400 Bad Request
```

### Basic Example:

```java
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    private String password;
}

@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
}
```

**Important:**

Validation annotations DTO/request class pe lagate hain, entity pe directly nahi — kyunki API contract aur database schema alag concerns hain.

---

## Q2: @NotNull, @NotEmpty aur @NotBlank mein kya difference hai?

**Answer:**

Teeno null/empty checks karte hain, lekin strictness level different hai.

### Comparison:

| Annotation | null | "" | " " (spaces) | "text" |
|------------|------|----|--------------|--------|
| **@NotNull** | ❌ | ✅ | ✅ | ✅ |
| **@NotEmpty** | ❌ | ❌ | ✅ | ✅ |
| **@NotBlank** | ❌ | ❌ | ❌ | ✅ |

### @NotNull:

Field `null` nahi hona chahiye. Empty string allow hai.

```java
@NotNull(message = "Age cannot be null")
private Integer age;  // Wrapper class — primitive int pe kaam nahi karega
```

### @NotEmpty:

`null` ya empty nahi hona chahiye. Collections, arrays, strings pe kaam karta hai.

```java
@NotEmpty(message = "Tags cannot be empty")
private List<String> tags;
```

### @NotBlank:

Sirf String fields pe. `null`, empty, ya sirf whitespace nahi hona chahiye — sabse strict.

```java
@NotBlank(message = "Name is required")
private String name;
```

### Rule of Thumb:

| Field Type | Use |
|------------|-----|
| String fields | `@NotBlank` |
| Collections/Arrays | `@NotEmpty` |
| Objects/Wrapper classes | `@NotNull` |

---

## Q3: Commonly used Bean Validation annotations kaunse hain?

**Answer:**

### String Validations:

```java
@NotBlank(message = "Name is required")
@Size(min = 2, max = 100)
private String name;

@Email(message = "Email should be valid")
private String email;

@Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
private String phone;
```

### Numeric Validations:

```java
@Min(value = 18, message = "Age must be at least 18")
@Max(value = 120, message = "Age must be at most 120")
private int age;

@Positive(message = "Price must be positive")
private BigDecimal price;

@Digits(integer = 6, fraction = 2)
private BigDecimal amount;
```

### Date Validations:

```java
@Past(message = "Date of birth must be in the past")
private LocalDate dateOfBirth;

@FutureOrPresent(message = "Expiry must be today or later")
private LocalDate expiryDate;
```

### Boolean Validations:

```java
@AssertTrue(message = "You must accept terms and conditions")
private Boolean termsAccepted;
```

### Quick Reference:

| Annotation | Purpose |
|------------|---------|
| `@NotNull` | Not null |
| `@NotEmpty` | Not null or empty |
| `@NotBlank` | Not null, empty, or whitespace (strings) |
| `@Size` | Length/size range |
| `@Min` / `@Max` | Numeric min/max |
| `@Email` | Valid email format |
| `@Pattern` | Regex match |
| `@Positive` | > 0 |
| `@Past` / `@Future` | Date in past/future |
| `@AssertTrue` | Must be true |

---

## Q4: @Valid aur @Validated mein kya difference hai?

**Answer:**

| Feature | @Valid | @Validated |
|---------|--------|------------|
| **Package** | `jakarta.validation.Valid` | `org.springframework.validation.annotation.Validated` |
| **Standard** | ✅ JSR-380 standard | ❌ Spring-specific |
| **Nested Validation** | ✅ Yes | ✅ Yes |
| **Group Validation** | ❌ No | ✅ Yes |
| **Class-Level Usage** | ❌ No | ✅ Yes |
| **Portability** | Any validation framework | Spring only |

### @Valid — Standard JSR-380:

```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    return ResponseEntity.ok(userService.create(request));
}
```

### @Validated — Group Validation:

```java
@PostMapping
public ResponseEntity<UserResponse> create(
        @Validated(CreateGroup.class) @RequestBody UserRequest request) { }

@PutMapping("/{id}")
public ResponseEntity<UserResponse> update(
        @Validated(UpdateGroup.class) @RequestBody UserRequest request) { }
```

### When to Use What:

| Scenario | Use |
|----------|-----|
| Simple validation | `@Valid` |
| Nested validation | `@Valid` |
| Group validation (Create/Update) | `@Validated` |
| Class-level method validation | `@Validated` |
| Framework-independent code | `@Valid` |

**Short Interview Answer:**

`@Valid` standard annotation hai nested validation ke liye. `@Validated` Spring-specific hai — group validation aur class-level validation support karta hai.

---

## Q5: Nested Validation kaise kaam karti hai?

**Answer:**

Nested validation tab chahiye jab request object ke andar dusra object ya collection ho aur uski bhi validation karni ho.

### Nested Object:

```java
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @Valid  // ⚠️ Zaruri hai — nested object validate hoga
    @NotNull(message = "Address is required")
    private Address address;
}

public class Address {

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;
}
```

### Nested Collection:

```java
public class OrderRequest {

    @Valid
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItem> items;
}

public class OrderItem {

    @NotBlank(message = "Product name is required")
    private String productName;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
```

### Without @Valid on Nested Object:

```java
@NotNull(message = "Address is required")
private Address address;  // ❌ No @Valid

// Sirf address != null check hoga
// Address ke andar ki validation SKIP ho jayegi!
```

**Important:**

Parent object pe `@Valid` lagao aur nested object/collection pe bhi `@Valid` lagao — warna inner fields validate nahi honge.

---

## Q6: Validation Groups kya hain aur kab use karte hain?

**Answer:**

Validation groups alag scenarios mein alag validation rules apply karne ke liye use hote hain — jaise Create vs Update.

### Define Groups:

```java
public interface CreateGroup {}
public interface UpdateGroup {}
```

### DTO with Groups:

```java
public class UserRequest {

    @NotBlank(groups = {CreateGroup.class, UpdateGroup.class})
    private String name;

    @NotBlank(groups = CreateGroup.class)  // Sirf create pe
    @Size(min = 8, groups = CreateGroup.class)
    private String password;

    @Positive(groups = UpdateGroup.class)  // Sirf update pe
    private Long id;

    @NotBlank  // Default group — hamesha validate
    private String category;
}
```

### Controller:

```java
@PostMapping
public ResponseEntity<UserResponse> create(
        @Validated(CreateGroup.class) @RequestBody UserRequest request) { }

@PutMapping("/{id}")
public ResponseEntity<UserResponse> update(
        @Validated(UpdateGroup.class) @RequestBody UserRequest request) { }
```

### Default Group:

Agar `groups` attribute specify nahi kiya, toh `Default` group mein jata hai — hamesha validate hoga chahe koi bhi group specify karo.

### Group Inheritance:

```java
public interface Base {}
public interface Create extends Base {}

@NotBlank(groups = Base.class)
private String name;

@NotBlank(groups = Create.class)
private String password;

// @Validated(Create.class) → name + password dono validate honge
```

---

## Q7: Custom Validator kaise banate hain?

**Answer:**

Jab built-in annotations kaafi na hon — tab custom validator banate hain.

### Step 1: Custom Annotation:

```java
@Documented
@Constraint(validatedBy = ValidPhone.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPhone {
    String message() default "Invalid phone number";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String country() default "IN";
}
```

### Step 2: ConstraintValidator:

```java
public class ValidPhone implements ConstraintValidator<ValidPhone, String> {

    private String country;

    @Override
    public void initialize(ValidPhone annotation) {
        this.country = annotation.country();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;  // @NotBlank alag se handle karega
        }
        return switch (country) {
            case "IN" -> value.matches("^\\+91[0-9]{10}$");
            case "US" -> value.matches("^\\+1[0-9]{10}$");
            default -> true;
        };
    }
}
```

### Step 3: Usage:

```java
@NotBlank
@ValidPhone(country = "IN", message = "Indian phone must be +91XXXXXXXXXX")
private String phone;
```

### Database Check Validator:

```java
@Component
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserRepository userRepository;

    public UniqueUsernameValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isBlank()) return true;
        return !userRepository.existsByUsername(username);
    }
}
```

**Important:**

`isValid()` mein `null` ke liye `true` return karo — null check `@NotNull`/`@NotBlank` handle karega. Custom validator sirf apna specific rule check kare.

---

## Q8: Cross-field aur Class-level validation kaise implement karte hain?

**Answer:**

Jab multiple fields compare karke validate karna ho — jaise password == confirmPassword, ya startDate < endDate.

### Class-Level Annotation:

```java
@PasswordMatches(
    password = "password",
    confirmPassword = "confirmPassword",
    message = "Passwords must match"
)
public class UserRegistrationRequest {

    @NotBlank
    private String password;

    @NotBlank
    private String confirmPassword;
}
```

### Validator:

```java
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    String password();
    String confirmPassword();
}

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Reflection se password aur confirmPassword compare karo
        String password = BeanUtils.getProperty(value, "password");
        String confirmPassword = BeanUtils.getProperty(value, "confirmPassword");
        if (password == null || confirmPassword == null) return true;
        return password.equals(confirmPassword);
    }
}
```

### Date Range Example:

```java
@ValidDateRange(startDate = "startDate", endDate = "endDate")
public class EventRequest {
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
}
```

**Note:** Class-level constraints `ObjectError` generate karte hain, field-level `FieldError`.

---

## Q9: Validation fail hone pe MethodArgumentNotValidException kaise handle karte hain?

**Answer:**

Jab `@Valid` validation fail hota hai, Spring `MethodArgumentNotValidException` throw karta hai.

### Default Response (Problem):

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='createUserRequest'. Error count: 3"
}
```

Field-specific errors nahi milte — sirf generic message.

### Custom Handler with @ControllerAdvice:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

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

        return ResponseEntity.badRequest().body(
            new ErrorResponse(400, "Validation failed", errors, LocalDateTime.now().toString())
        );
    }
}
```

### Custom Response:

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "name": "Name is required",
    "email": "Email should be valid",
    "password": "Password must be between 8 and 100 characters"
  }
}
```

### BindingResult (Manual Handling):

```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request,
                                     BindingResult result) {
    if (result.hasErrors()) {
        List<String> errors = result.getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .toList();
        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }
    return ResponseEntity.ok(userService.create(request));
}
```

### FieldError vs ObjectError:

| Feature | FieldError | ObjectError |
|---------|-----------|-------------|
| Scope | Specific field | Entire object |
| Source | `@NotBlank` on field | `@PasswordMatches` on class |
| Field Name | Available | null |

---

## Q10: Validation ke best practices kya hain?

**Answer:**

### 1. DTO pe Validate Karo, Entity pe Nahi:

```java
// ✅ Request DTO pe validation
public class CreateUserRequest {
    @NotBlank private String name;
    @Email private String email;
}

// ❌ Entity pe API validation mat lagao
@Entity
public class User {
    @NotBlank private String name;  // Avoid for API validation
}
```

### 2. Meaningful Error Messages:

```java
@NotBlank(message = "Name is required")           // ✅ Clear
@Size(min = 8, message = "Password must be at least 8 characters")  // ✅ Specific
@NotBlank                                        // ❌ Default message unclear
```

### 3. Global Error Handler Use Karo:

```java
@RestControllerAdvice  // Sab controllers ke liye ek jagah
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // ...
    @ExceptionHandler(ConstraintViolationException.class)  // Path/query param validation
    // ...
}
```

### 4. Class-Level @Validated for Service Layer:

```java
@Service
@Validated
public class UserService {

    public User findByEmail(@Email @NotBlank String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
}
```

### 5. Validation Order:

```
@NotBlank → @Size → @Pattern → @Email
Pehle null/empty check, phir format check
```

### 6. Nested Objects:

```java
@Valid @NotNull private Address address;  // Dono lagao
```

**Short Interview Answer:**

Validate on DTOs, use `@Valid` for nested objects, `@Validated` for groups, custom validators for business rules, and `@ControllerAdvice` for consistent error responses.
