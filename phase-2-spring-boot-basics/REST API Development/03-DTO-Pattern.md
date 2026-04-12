# DTO Pattern

## Status: Not Started

---

## Table of Contents

1. [What is DTO Pattern?](#what-is-dto-pattern)
2. [Why Use DTOs?](#why-use-dtos)
3. [Request DTOs](#request-dtos)
4. [Response DTOs](#response-dtos)
5. [Entity to DTO Mapping](#entity-to-dto-mapping)
6. [MapStruct (Automated Mapping)](#mapstruct-automated-mapping)
7. [Jackson Annotations](#jackson-annotations)

---

## What is DTO Pattern?

**DTO (Data Transfer Object)** ek simple object hai jo data ko ek layer se dusri layer tak le jaata hai. Entity (database model) ko directly API expose karne ke bajaye, DTO use karte hain.

```
Client → Request DTO → Service → Entity → Database
Database → Entity → Service → Response DTO → Client
```

### Without DTO (Bad Practice)

```java
// Entity directly expose kar rahe ho
@Entity
public class User {
    @Id
    private Long id;
    private String name;
    private String email;
    private String password;       // ❌ Password API mein expose ho gaya!
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isActive;
    private String resetToken;     // ❌ Sensitive data leak!
}

@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);  // ❌ Entity directly return — dangerous!
    }
}
```

**Response (Problem):**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com",
  "password": "$2a$10$dXJ3SW6G7P50lGmMkkmwe...",  // ❌ Hash bhi expose nahi hona chahiye
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00",
  "isActive": true,
  "resetToken": "abc123-token"  // ❌ Security risk!
}
```

---

## Why Use DTOs?

| Reason | Description |
|--------|-------------|
| **Security** | Sensitive fields (password, tokens) hide karo |
| **Separation of Concerns** | Entity = DB schema, DTO = API contract |
| **Flexibility** | Entity change ho toh DTO affect nahi hoga |
| **Multiple Views** | Same entity ke liye alag-alag DTOs (summary, detailed) |
| **Validation** | Request DTO pe validation annotations lagao |
| **Versioning** | API version karna easy ho jata hai |

---

## Request DTOs

**Matlab:** Client se aane wale data ke liye DTO — sirf woh fields jo client bhejta hai.

```java
// User create karne ke liye DTO
public class CreateUserRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
```

**Usage:**
```java
@PostMapping("/users")
public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    User user = new User();
    user.setName(request.getName());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));

    User created = userService.save(user);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(toResponse(created));
}
```

**Request JSON:**
```json
{
  "name": "Sachin",
  "email": "sachin@example.com",
  "password": "mySecret123"
}
```

### Update Request DTO

```java
// Partial update ke liye — sirf change hone wale fields
public class UpdateUserRequest {

    @Size(min = 2, max = 100)
    private String name;

    @Email
    private String email;

    // Password optional hai update mein
    @Size(min = 8)
    private String password;

    // Getters and Setters
}
```

---

## Response DTOs

**Matlab:** Client ko jaane wale data ke liye DTO — sirf woh fields jo client ko dikhane hain.

```java
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;

    // Constructor
    public UserResponse(Long id, String name, String email, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.createdAt = createdAt;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
```

**Response JSON:**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Notice:** `password`, `resetToken`, `isActive` — yeh sab fields hidden hain!

### Multiple Response DTOs

```java
// Summary view (list endpoint ke liye)
public class UserSummary {
    private Long id;
    private String name;
}

// Detailed view (single user endpoint ke liye)
public class UserDetail {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    private List<PostSummary> posts;
}
```

---

## Entity to DTO Mapping

### Manual Mapping

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Entity → Response DTO
    private UserResponse toResponse(User user) {
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(toResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserSummary>> getAllUsers() {
        List<User> users = userService.findAll();
        List<UserSummary> summaries = users.stream()
            .map(u -> new UserSummary(u.getId(), u.getName()))
            .collect(Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User created = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }
}
```

---

## MapStruct (Automated Mapping)

**Matlab:** Compile-time pe mapping code generate karta hai. Manual boilerplate se bachata hai.

### Setup

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>1.5.5.Final</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Mapper Interface

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    // Entity → Response DTO
    UserResponse toResponse(User user);

    // Entity → Summary DTO
    UserSummary toSummary(User user);

    // Request DTO → Entity
    User toEntity(CreateUserRequest request);

    // List mappings
    List<UserResponse> toResponseList(List<User> users);
    List<UserSummary> toSummaryList(List<User> users);

    // Custom mapping
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
```

### Generated Implementation (Behind the Scenes)

```java
// MapStruct automatically yeh code generate karta hai
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponse toResponse(User user) {
        if (user == null) return null;

        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }
}
```

### Usage in Controller

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;  // MapStruct mapper inject

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }

    @GetMapping
    public ResponseEntity<List<UserSummary>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(userMapper.toSummaryList(users));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User created = userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponse(created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @RequestBody UpdateUserRequest request) {
        User user = userService.findById(id);
        userMapper.updateEntityFromRequest(request, user);

        User updated = userService.save(user);
        return ResponseEntity.ok(userMapper.toResponse(updated));
    }
}
```

---

## Jackson Annotations

Jackson annotations JSON serialization/deserialization ko control karte hain.

### @JsonProperty

Field ka JSON name change karta hai.

```java
public class UserResponse {

    @JsonProperty("user_id")
    private Long id;

    @JsonProperty("full_name")
    private String name;

    @JsonProperty("email_address")
    private String email;
}
```

**Output:**
```json
{
  "user_id": 1,
  "full_name": "Sachin",
  "email_address": "sachin@example.com"
}
```

### @JsonIgnore

Field ko JSON se completely hide karta hai.

```java
public class User {

    private Long id;
    private String name;

    @JsonIgnore  // Yeh field JSON mein nahi aayega
    private String password;

    @JsonIgnore  // Yeh bhi nahi aayega
    private String resetToken;
}
```

**Output:**
```json
{
  "id": 1,
  "name": "Sachin"
  // password aur resetToken nahi hain
}
```

### @JsonInclude

Null ya empty fields ko skip karta hai.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;  // null hai toh JSON mein nahi aayega
}
```

**Include Options:**
| Option | Behavior |
|--------|----------|
| `ALWAYS` | Hamesha include karo (default) |
| `NON_NULL` | Null fields skip karo |
| `NON_EMPTY` | Null ya empty collections/strings skip karo |
| `NON_DEFAULT` | Default values skip karo |

**Output (with NON_NULL):**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com"
  // "phone": null — included nahi hoga
}
```

### @JsonAlias

Deserialization ke time multiple names accept karta hai.

```java
public class UserRequest {

    @JsonAlias({ "full_name", "userName", "name" })
    private String fullName;

    @JsonAlias({ "email_address", "emailAddress" })
    private String email;
}
```

**All these JSON inputs will work:**
```json
{ "full_name": "Sachin", "email_address": "sachin@example.com" }
{ "userName": "Sachin", "emailAddress": "sachin@example.com" }
{ "name": "Sachin", "email": "sachin@example.com" }
```

### @JsonFormat

Date/Time formatting control karta hai.

```java
public class UserResponse {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Kolkata")
    private LocalDate dateOfBirth;
}
```

**Output:**
```json
{
  "id": 1,
  "createdAt": "2024-01-15 10:30:00",
  "dateOfBirth": "1995-05-20"
}
```

### @JsonSerialize / @JsonDeserialize

Custom serialization/deserialization.

```java
// Custom Serializer
public class MoneySerializer extends JsonSerializer<BigDecimal> {
    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeString(value.setScale(2, RoundingMode.HALF_UP) + " INR");
    }
}

// Custom Deserializer
public class MoneyDeserializer extends JsonDeserializer<BigDecimal> {
    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        String text = p.getText().replace(" INR", "");
        return new BigDecimal(text);
    }
}

// Usage
public class ProductResponse {

    private String name;

    @JsonSerialize(using = MoneySerializer.class)
    @JsonDeserialize(using = MoneyDeserializer.class)
    private BigDecimal price;
}
```

**Output:**
```json
{
  "name": "Laptop",
  "price": "50000.00 INR"
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **DTO** | Entity ko directly expose nahi karna — security + flexibility |
| **Request DTO** | Client se aane wale data — validation annotations lagao |
| **Response DTO** | Client ko jaane wale data — sensitive fields hide karo |
| **Manual Mapping** | Simple cases mein — constructor ya setter se map karo |
| **MapStruct** | Complex projects mein — compile-time mapping, boilerplate free |
| **@JsonProperty** | JSON field name change karo |
| **@JsonIgnore** | Field ko JSON se completely hide karo |
| **@JsonInclude(NON_NULL)** | Null fields skip karo |
| **@JsonAlias** | Multiple names accept karo deserialization ke time |
| **@JsonFormat** | Date/Time formatting control karo |
| **@JsonSerialize/Deserialize** | Custom serialization/deserialization logic |
