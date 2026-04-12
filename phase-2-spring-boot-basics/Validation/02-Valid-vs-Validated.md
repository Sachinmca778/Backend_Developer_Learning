# @Valid vs @Validated

## Status: Not Started

---

## Table of Contents

1. [@Valid (Standard JSR-380)](#valid-standard-jsr-380)
2. [@Validated (Spring)](#validated-spring)
3. [@Valid vs @Validated — Comparison](#valid-vs-validated--comparison)
4. [Nested Validation](#nested-validation)
5. [Group Validation](#group-validation)
6. [Class-Level @Validated](#class-level-validated)

---

## @Valid (Standard JSR-380)

**Matlab:** Standard Java Bean Validation annotation — `jakarta.validation.Valid`. Nested validation trigger karta hai.

### Usage

```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    // Validation trigger hoga
    return ResponseEntity.ok(userService.create(request));
}
```

### Nested Validation

```java
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    // @Valid — nested object bhi validate hoga
    @Valid
    @NotNull(message = "Address is required")
    private Address address;
}

public class Address {

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Zip code is required")
    @Pattern(regexp = "^[0-9]{5,6}$", message = "Zip code must be 5-6 digits")
    private String zipCode;
}
```

**Request:**
```json
{
  "name": "Sachin",
  "address": {
    "street": "",
    "city": null,
    "zipCode": "abc"
  }
}
```

**Errors:**
```
1. Name is required — ✅ valid
2. Address.street — Street is required
3. Address.city — City is required
4. Address.zipCode — Zip code must be 5-6 digits
```

**Without @Valid on Address:**
Sirf `name` field validate hoga — `address` ke andar ki validation skip ho jayegi.

### Nested Collections

```java
public class OrderRequest {

    @NotBlank(message = "Order name is required")
    private String name;

    // Collection ke andar bhi validate karo
    @Valid
    @NotEmpty(message = "Order must have at least one item")
    private List<OrderItem> items;
}

public class OrderItem {

    @NotBlank(message = "Product name is required")
    private String productName;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Positive(message = "Price must be positive")
    private BigDecimal price;
}
```

---

## @Validated (Spring)

**Matlab:** Spring-specific annotation — `org.springframework.validation.annotation.Validated`. Group validation aur class-level validation support karta hai.

### Basic Usage (Same as @Valid)

```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Validated @RequestBody CreateUserRequest request) {
    return ResponseEntity.ok(userService.create(request));
}
```

### Group Validation

Different scenarios mein different validation rules apply karna.

```java
// Validation Groups
public interface CreateGroup {}
public interface UpdateGroup {}

public class UserRequest {

    // Create aur Update dono mein validate hoga
    @NotBlank(message = "Name is required", groups = {CreateGroup.class, UpdateGroup.class})
    private String name;

    // Sirf Create mein validate hoga
    @NotBlank(message = "Password is required", groups = CreateGroup.class)
    @Size(min = 8, max = 100, message = "Password must be 8-100 chars", groups = CreateGroup.class)
    private String password;

    // Sirf Update mein validate hoga
    @Positive(message = "ID must be positive", groups = UpdateGroup.class)
    private Long id;
}
```

### Controller with Groups

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // Create — sirf CreateGroup validate hoga
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Validated(CreateGroup.class) @RequestBody UserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    // Update — sirf UpdateGroup validate hoga
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Validated(UpdateGroup.class) @RequestBody UserRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }
}
```

### Multiple Groups

```java
// Create + EmailVerification groups ek saath
@Validated({CreateGroup.class, EmailVerificationGroup.class})
@RequestBody UserRequest request
```

---

## @Valid vs @Validated — Comparison

| Feature | @Valid | @Validated |
|---------|--------|------------|
| **Package** | `jakarta.validation.Valid` | `org.springframework.validation.annotation.Validated` |
| **Standard** | ✅ JSR-380 standard | ❌ Spring-specific |
| **Nested Validation** | ✅ Triggers nested validation | ✅ Triggers nested validation |
| **Group Validation** | ❌ No | ✅ Yes |
| **Class-Level Usage** | ❌ No | ✅ Yes (method-level validation) |
| **Method Parameter** | ✅ Yes | ✅ Yes |
| **Portability** | Any validation framework | Spring only |

### When to Use What

| Scenario | Use |
|----------|-----|
| Simple validation | `@Valid` |
| Nested validation | `@Valid` |
| Group validation | `@Validated` |
| Class-level validation | `@Validated` |
| Framework-independent code | `@Valid` |

---

## Nested Validation

### @Valid with Nested Objects

```java
public class BookRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @Valid  // Nested object validate hoga
    @NotNull(message = "Author is required")
    private AuthorRequest author;
}

public class AuthorRequest {

    @NotBlank(message = "Author name is required")
    private String name;

    @Email(message = "Author email should be valid")
    private String email;
}
```

### @Valid with Nested Collections

```java
public class OrderRequest {

    @NotBlank(message = "Order name is required")
    private String name;

    @Valid
    @NotEmpty(message = "Items cannot be empty")
    private List<OrderItemRequest> items;
}
```

### @Valid Without Nested Object

Agar nested object pe `@Valid` nahi lagaya toh uske andar ki validation skip ho jayegi.

```java
public class UserRequest {

    @NotBlank(message = "Name is required")
    private String name;

    // ❌ No @Valid — Address validation nahi hoga
    @NotNull(message = "Address is required")
    private AddressRequest address;
}

// Sirf yeh validate hoga:
// 1. Name is required — check
// 2. Address is not null — check
// Address ke andar ki validation SKIP
```

---

## Group Validation

### Define Groups

```java
// Marker interfaces (no methods needed)
public interface Create {}
public interface Update {}
public interface Delete {}
```

### DTO with Groups

```java
public class ProductRequest {

    @NotBlank(message = "Name is required", groups = {Create.class, Update.class})
    private String name;

    @Positive(message = "Price must be positive", groups = {Create.class, Update.class})
    private BigDecimal price;

    // Sirf Create mein validate hoga
    @NotBlank(message = "SKU is required", groups = Create.class)
    @Pattern(regexp = "^[A-Z]{3}-[0-9]{4}$", message = "SKU format: XXX-0000", groups = Create.class)
    private String sku;

    // Sirf Update mein validate hoga
    @Positive(message = "ID must be positive", groups = Update.class)
    private Long id;

    // Default group — hamesha validate hoga
    @NotBlank(message = "Category is required")
    private String category;
}
```

### Controller

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Validated(Create.class) @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id,
            @Validated(Update.class) @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }
}
```

### Default Group

Agar `groups` attribute specify nahi kiya toh `Default` group mein jata hai — hamesha validate hoga.

```java
@NotBlank(message = "Category is required")  // Default group
private String category;

// Always validate hoga, chahe koi bhi group specify karo
@Validated(Create.class)    // ✅ Category validate hoga
@Validated(Update.class)    // ✅ Category validate hoga
@Validated({})              // ✅ Category validate hoga
```

### Group Inheritance

```java
public interface Base {}
public interface Create extends Base {}
public interface Update extends Base {}

public class UserRequest {

    @NotBlank(message = "Name is required", groups = Base.class)
    private String name;

    @NotBlank(message = "Password is required", groups = Create.class)
    private String password;

    @Positive(message = "ID must be positive", groups = Update.class)
    private Long id;
}

// Validate with Create — Base + Create dono validate honge
@Validated(Create.class)  // name + password validate honge

// Validate with Update — Base + Update dono validate honge
@Validated(Update.class)  // name + id validate honge
```

---

## Class-Level @Validated

**Matlab:** Class pe `@Validated` lagana — saare public methods pe method-level validation enable ho jaata hai.

### Usage

```java
@Service
@Validated  // Class-level — sab methods validate honge
public class UserService {

    // Method-level validation
    public User createUser(@Valid CreateUserRequest request) {
        return userRepository.save(toEntity(request));
    }

    public User updateUser(@Valid UpdateUserRequest request) {
        return userRepository.save(toEntity(request));
    }

    // @Valid nahi hai — validation nahi hoga
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

### With Constraints on Method Parameters

```java
@Service
@Validated
public class UserService {

    // @Valid ke bina bhi validation trigger hoga (class-level @Validated ki wajah se)
    public User createUser(CreateUserRequest request) {
        return userRepository.save(toEntity(request));
    }

    // Constraint annotations directly on parameters
    public User findByEmail(@Email @NotBlank String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<User> findByAge(@Min(18) @Max(120) int age) {
        return userRepository.findByAge(age);
    }
}
```

### Class-Level @Validated for Controllers

```java
@RestController
@RequestMapping("/api/users")
@Validated  // Class-level
public class UserController {

    // @Valid ke bina bhi validation hoga
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    // Direct parameter constraints
    @GetMapping("/{email}")
    public ResponseEntity<UserResponse> getUser(@Email @PathVariable String email) {
        return ResponseEntity.ok(userService.findByEmail(email));
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **@Valid** | Standard JSR-380 annotation — nested validation trigger karta hai |
| **@Validated** | Spring-specific — group validation aur class-level support |
| **Nested Validation** | `@Valid` lagao nested object/collection pe — andar ki validation bhi hogi |
| **Group Validation** | Different scenarios (Create/Update) mein different rules |
| **Class-Level @Validated** | Sab public methods pe method-level validation enable |
| **Default Group** | Bina `groups` attribute ke — hamesha validate hoga |
| **Group Inheritance** | Child group validate karne pe parent group bhi validate hoga |
