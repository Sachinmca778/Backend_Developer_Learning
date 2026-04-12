# Bean Validation (JSR-380)

## Status: Not Started

---

## Table of Contents

1. [Bean Validation Overview](#bean-validation-overview)
2. [@NotNull](#notnull)
3. [@NotEmpty](#notempty)
4. [@NotBlank](#notblank)
5. [@Size](#size)
6. [@Min / @Max](#min--max)
7. [@Email](#email)
8. [@Pattern](#pattern)
9. [@Positive / @PositiveOrZero](#positive--positiveorzero)
10. [@Negative / @NegativeOrZero](#negative--negativeorzero)
11. [@Past / @Future](#past--future)
12. [@PastOrPresent / @FutureOrPresent](#pastorpresent--futureorpresent)
13. [@AssertTrue / @AssertFalse](#asserttrue--assertfalse)
14. [@Digits](#digits)
15. [Using Validation in Controllers](#using-validation-in-controllers)

---

## Bean Validation Overview

**Matlab:** Java objects pe validation rules lagana — taaki invalid data database tak na pahuche.

**JSR-380** = Bean Validation 3.0 specification — `jakarta.validation` package.

### Setup

```xml
<!-- Spring Boot Web mein already included hai -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### How It Works

```
Client Request (JSON)
    ↓
@RequestBody → Java Object
    ↓
@Valid / @Validated annotation
    ↓
Validator checks all constraint annotations
    ↓
┌─────────────────────────────────┐
│ All valid → Controller method  │
│ Any invalid → 400 Bad Request   │
└─────────────────────────────────┘
```

### Basic Example

```java
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be at most 120")
    private int age;
}
```

### Controller Usage

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Validation pass hua toh yeh line execute hogi
        User user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }
}
```

**Invalid Request:**
```json
{
  "name": "",
  "email": "invalid-email",
  "password": "123",
  "age": 15
}
```

**Response:**
```json
{
  "status": 400,
  "errors": [
    "Name is required",
    "Email should be valid",
    "Password must be between 8 and 100 characters",
    "Age must be at least 18"
  ]
}
```

---

## @NotNull

**Matlab:** Field `null` nahi hona chahiye. Empty string `""` allow hai.

```java
public class UserRequest {

    @NotNull(message = "Name cannot be null")
    private String name;

    @NotNull(message = "Age cannot be null")
    private Integer age;  // Wrapper class (primitive int pe kaam nahi karega)
}
```

**Valid/Invalid:**
| Value | Valid? |
|-------|--------|
| `null` | ❌ Invalid |
| `""` (empty string) | ✅ Valid |
| `" "` (spaces) | ✅ Valid |
| `"Sachin"` | ✅ Valid |

---

## @NotEmpty

**Matlab:** Field `null` ya empty nahi hona chahiye. Collections, arrays, strings pe kaam karta hai.

```java
public class UserRequest {

    @NotEmpty(message = "Name cannot be empty")
    private String name;

    @NotEmpty(message = "Tags cannot be empty")
    private List<String> tags;

    @NotEmpty(message = "Roles cannot be empty")
    private String[] roles;
}
```

**Valid/Invalid (String):**
| Value | Valid? |
|-------|--------|
| `null` | ❌ Invalid |
| `""` (empty string) | ❌ Invalid |
| `" "` (spaces) | ✅ Valid |
| `"Sachin"` | ✅ Valid |

**Valid/Invalid (Collection):**
| Value | Valid? |
|-------|--------|
| `null` | ❌ Invalid |
| `[]` (empty list) | ❌ Invalid |
| `["admin"]` | ✅ Valid |

---

## @NotBlank

**Matlab:** String `null`, empty, ya sirf whitespace nahi hona chahiye.

**Sirf String fields pe kaam karta hai.**

```java
public class UserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Username is required")
    private String username;
}
```

**Valid/Invalid:**
| Value | Valid? |
|-------|--------|
| `null` | ❌ Invalid |
| `""` (empty string) | ❌ Invalid |
| `"   "` (only spaces) | ❌ Invalid |
| `"Sachin"` | ✅ Valid |
| `"  Sachin  "` | ✅ Valid |

### @NotNull vs @NotEmpty vs @NotBlank

| Annotation | null | "" | " " | "text" |
|------------|------|----|-----|--------|
| **@NotNull** | ❌ | ✅ | ✅ | ✅ |
| **@NotEmpty** | ❌ | ❌ | ✅ | ✅ |
| **@NotBlank** | ❌ | ❌ | ❌ | ✅ |

**Rule of thumb:**
- **String fields** → `@NotBlank` use karo (sabse strict)
- **Collections/Arrays** → `@NotEmpty` use karo
- **Objects/Wrapper classes** → `@NotNull` use karo

---

## @Size

**Matlab:** String length, collection size, ya array size ki range define karta hai.

```java
public class UserRequest {

    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @Size(min = 1, max = 5, message = "Must have between 1 and 5 tags")
    private List<String> tags;

    @Size(max = 3, message = "Maximum 3 phone numbers allowed")
    private List<String> phoneNumbers;
}
```

**Valid/Invalid:**
| Value | min=2, max=10 | Valid? |
|-------|---------------|--------|
| `"A"` | length 1 | ❌ Invalid |
| `"AB"` | length 2 | ✅ Valid |
| `"ABCDEFGHIJK"` | length 11 | ❌ Invalid |

---

## @Min / @Max

**Matlab:** Numeric value ki minimum aur maximum limit define karta hai.

```java
public class ProductRequest {

    @Min(value = 1, message = "Price must be at least 1")
    @Max(value = 1000000, message = "Price must be at most 1,000,000")
    private BigDecimal price;

    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 9999, message = "Quantity must be at most 9,999")
    private int quantity;

    @Min(value = 18, message = "Age must be at least 18")
    private int age;
}
```

**Supported Types:** `int`, `long`, `short`, `BigInteger`, `BigDecimal`

---

## @Email

**Matlab:** String valid email format mein hona chahiye.

```java
public class UserRequest {

    @Email(message = "Email should be valid")
    private String email;

    @Email(message = "Email should be valid")
    @NotBlank
    private String workEmail;
}
```

**Valid/Invalid:**
| Value | Valid? |
|-------|--------|
| `null` | ✅ Valid (@NotBlank ke bina) |
| `""` | ✅ Valid (empty email technically valid per spec) |
| `"sachin"` | ❌ Invalid |
| `"sachin@"` | ❌ Invalid |
| `"sachin@example"` | ✅ Valid |
| `"sachin@example.com"` | ✅ Valid |
| `"sachin@example.co.in"` | ✅ Valid |
| `"sachin@example."` | ❌ Invalid |

### Custom Email Regex

```java
@Email(regexp = "^[a-zA-Z0-9._%+-]+@company\\.com$", message = "Must be a company email")
private String workEmail;
```

---

## @Pattern

**Matlab:** String ko regex pattern se match karta hai.

```java
public class UserRequest {

    // Phone number format
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phone;

    // Username — alphanumeric, 3-20 chars
    @Pattern(regexp = "^[a-zA-Z0-9]{3,20}$", message = "Username must be 3-20 alphanumeric characters")
    private String username;

    // PAN card format (India)
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN card format")
    private String panCard;

    // Aadhaar card format
    @Pattern(regexp = "^[0-9]{4}\\s[0-9]{4}\\s[0-9]{4}$", message = "Aadhaar must be in XXXX XXXX XXXX format")
    private String aadhaar;
}
```

**Valid/Invalid (Username):**
| Value | Valid? |
|-------|--------|
| `"ab"` | ❌ (too short) |
| `"sachin123"` | ✅ |
| `"sachin@123"` | ❌ (special character) |
| `"sachin_123"` | ❌ (underscore not allowed) |

---

## @Positive / @PositiveOrZero

**Matlab:** Number positive hona chahiye (> 0 ya >= 0).

```java
public class ProductRequest {

    @Positive(message = "Price must be positive")
    private BigDecimal price;

    @PositiveOrZero(message = "Discount cannot be negative")
    private BigDecimal discount;
}
```

| Value | @Positive | @PositiveOrZero |
|-------|-----------|-----------------|
| `-5` | ❌ | ❌ |
| `0` | ❌ | ✅ |
| `5` | ✅ | ✅ |

---

## @Negative / @NegativeOrZero

**Matlab:** Number negative hona chahiye (< 0 ya <= 0).

```java
public class TransactionRequest {

    @Negative(message = "Loss amount must be negative")
    private BigDecimal lossAmount;

    @NegativeOrZero(message = "Adjustment cannot be positive")
    private BigDecimal adjustment;
}
```

| Value | @Negative | @NegativeOrZero |
|-------|-----------|-----------------|
| `-5` | ✅ | ✅ |
| `0` | ❌ | ✅ |
| `5` | ❌ | ❌ |

---

## @Past / @Future

**Matlab:** Date/Time past ya future mein hona chahiye.

```java
public class UserRequest {

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Future(message = "Subscription start date must be in the future")
    private LocalDate subscriptionStart;
}
```

**Supported Types:** `LocalDate`, `LocalDateTime`, `Date`, `Calendar`, `Instant`

---

## @PastOrPresent / @FutureOrPresent

```java
public class ArticleRequest {

    @PastOrPresent(message = "Published date cannot be in the future")
    private LocalDateTime publishedAt;

    @FutureOrPresent(message = "Expiry date must be today or later")
    private LocalDateTime expiresAt;
}
```

| Value | @Past | @PastOrPresent | @Future | @FutureOrPresent |
|-------|-------|----------------|---------|------------------|
| Yesterday | ✅ | ✅ | ❌ | ❌ |
| Today | ❌ | ✅ | ❌ | ✅ |
| Tomorrow | ❌ | ❌ | ✅ | ✅ |

---

## @AssertTrue / @AssertFalse

**Matlab:** Boolean field `true` ya `false` hona chahiye.

```java
public class UserRegistrationRequest {

    @AssertTrue(message = "You must accept the terms and conditions")
    private Boolean termsAccepted;

    @AssertFalse(message = "You must not be a bot")
    private Boolean isBot;
}
```

---

## @Digits

**Matlab:** Number ke integer aur decimal places ki limit define karta hai.

```java
public class ProductRequest {

    @Digits(integer = 6, fraction = 2, message = "Price must have at most 6 integer digits and 2 decimal places")
    private BigDecimal price;

    @Digits(integer = 10, fraction = 0, message = "Quantity must be a whole number with at most 10 digits")
    private Long quantity;
}
```

| Value | integer=6, fraction=2 | Valid? |
|-------|----------------------|--------|
| `123456.78` | 6 int, 2 frac | ✅ |
| `1234567.78` | 7 int | ❌ |
| `123456.789` | 3 frac | ❌ |

---

## Using Validation in Controllers

### Basic Validation

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        // Validation pass hua toh yeh execute hoga
        User user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }
}
```

### Validation with BindingResult

```java
@PostMapping("/users")
public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request,
                                     BindingResult result) {
    if (result.hasErrors()) {
        // Validation errors ko manually handle karo
        List<String> errors = result.getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(Map.of("errors", errors));
    }

    User user = userService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
}
```

### Complete Request DTO Example

```java
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[!@#$%^&*]).*$",
             message = "Password must contain uppercase, lowercase, digit, and special character")
    private String password;

    @Min(value = 18, message = "Age must be at least 18")
    @Max(value = 120, message = "Age must be at most 120")
    private int age;

    @Size(max = 3, message = "Maximum 3 phone numbers allowed")
    private List<@NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$") String> phoneNumbers;

    @AssertTrue(message = "You must accept the terms and conditions")
    private Boolean termsAccepted;
}
```

---

## Summary

| Annotation | Purpose | Example |
|------------|---------|---------|
| **@NotNull** | Not null | `@NotNull` |
| **@NotEmpty** | Not null or empty | `@NotEmpty` (collections, strings) |
| **@NotBlank** | Not null, empty, or whitespace | `@NotBlank` (strings only) |
| **@Size** | Length/size range | `@Size(min = 2, max = 100)` |
| **@Min / @Max** | Numeric min/max | `@Min(18)`, `@Max(120)` |
| **@Email** | Valid email format | `@Email` |
| **@Pattern** | Regex match | `@Pattern(regexp = "^[a-z]+$")` |
| **@Positive** | > 0 | `@Positive` |
| **@PositiveOrZero** | >= 0 | `@PositiveOrZero` |
| **@Negative** | < 0 | `@Negative` |
| **@NegativeOrZero** | <= 0 | `@NegativeOrZero` |
| **@Past** | Date in past | `@Past` |
| **@Future** | Date in future | `@Future` |
| **@PastOrPresent** | Date ≤ today | `@PastOrPresent` |
| **@FutureOrPresent** | Date ≥ today | `@FutureOrPresent` |
| **@AssertTrue** | Must be true | `@AssertTrue` |
| **@AssertFalse** | Must be false | `@AssertFalse` |
| **@Digits** | Integer/decimal places | `@Digits(integer = 6, fraction = 2)` |
