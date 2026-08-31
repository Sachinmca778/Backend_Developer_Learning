# REST API Development - Top 10 Interview Questions & Answers

> Covering: REST Controllers, Request/Response Handling, DTO Pattern, Jackson, Content Negotiation, and HATEOAS

---

## Q1: @RestController aur @Controller mein kya difference hai?

**Answer:**

```
@RestController = @Controller + @ResponseBody
```

| Feature | @Controller | @RestController |
|---------|-------------|-----------------|
| **Returns** | View name (JSP, Thymeleaf) | JSON/XML response body |
| **@ResponseBody** | Manually add karna padta hai | Automatically included |
| **Use Case** | Server-side rendering (web pages) | REST APIs |

### @RestController:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);  // Automatically JSON
    }
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com"
}
```

### @Controller with @ResponseBody:

```java
@Controller
public class OldStyleController {

    @GetMapping("/api/users/{id}")
    @ResponseBody  // Zaruri hai @Controller mein
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

**Short Interview Answer:**

`@RestController` REST APIs ke liye hai — return value automatically JSON mein convert hoti hai. `@Controller` views return karta hai (Thymeleaf, JSP).

---

## Q2: REST API mein HTTP methods (GET, POST, PUT, PATCH, DELETE) ka use kya hai?

**Answer:**

| Method | Purpose | Idempotent? | Example |
|--------|---------|-------------|---------|
| **GET** | Data read/fetch | ✅ Yes | `GET /api/users/1` |
| **POST** | New resource create | ❌ No | `POST /api/users` |
| **PUT** | Full resource replace | ✅ Yes | `PUT /api/users/1` |
| **PATCH** | Partial update | ❌ No | `PATCH /api/users/1` |
| **DELETE** | Resource delete | ✅ Yes | `DELETE /api/users/1` |

### Examples:

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { }

@PostMapping("/users")
@ResponseStatus(HttpStatus.CREATED)  // 201
public User createUser(@RequestBody User user) { }

@PutMapping("/users/{id}")
public User updateUser(@PathVariable Long id, @RequestBody User user) { }

@PatchMapping("/users/{id}")
public User patchUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) { }

@DeleteMapping("/users/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)  // 204
public void deleteUser(@PathVariable Long id) { }
```

### PUT vs PATCH:

| Feature | PUT | PATCH |
|---------|-----|-------|
| Update Type | Complete replacement | Partial update |
| Payload | Poora object bhejo | Sirf changed fields |

```json
// PATCH — sirf name update
{ "name": "Sachin Updated" }
```

### Common HTTP Status Codes:

| Status | Use Case |
|--------|----------|
| **200 OK** | GET/PUT/PATCH success |
| **201 Created** | POST — resource created |
| **204 No Content** | DELETE success |
| **400 Bad Request** | Invalid input |
| **404 Not Found** | Resource not found |
| **500 Internal Server Error** | Unexpected error |

---

## Q3: @RequestParam aur @PathVariable mein kya difference hai?

**Answer:**

| Feature | @RequestParam | @PathVariable |
|---------|---------------|---------------|
| **URL** | `/users?id=1&page=0` | `/users/1` |
| **Use Case** | Filtering, pagination, optional params | Resource identification |
| **REST Style** | Less RESTful | More RESTful |
| **Required** | Default: true | Default: true |

### @RequestParam — Query Parameters:

```java
@GetMapping("/users")
public List<User> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String sortBy
) { }
```

**Request:** `GET /api/users?page=2&size=20&sortBy=name`

### @PathVariable — Path Parameters:

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) { }

@GetMapping("/users/{userId}/posts/{postId}")
public Post getPost(@PathVariable Long userId, @PathVariable Long postId) { }
```

**Request:** `GET /api/users/1/posts/5`

### Other Request Annotations:

```java
@RequestHeader("Authorization") String authHeader
@CookieValue("sessionId") String sessionId
@RequestBody CreateUserRequest request
```

---

## Q4: @RequestBody, @ResponseBody aur ResponseEntity ka use kya hai?

**Answer:**

### @RequestBody:

Incoming JSON ko Java object mein deserialize karta hai.

```java
@PostMapping("/users")
public User createUser(@RequestBody CreateUserRequest request) {
    return userService.save(request);
}
```

```
JSON Request → Jackson → Java Object → Controller method
```

### @ResponseBody:

Return value ko JSON mein serialize karta hai. `@RestController` mein automatically included hai.

### ResponseEntity<T> — Full Control:

Status code, headers, aur body teen customize kar sakte ho.

```java
// 200 OK
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return userRepository.findById(id)
        .map(user -> ResponseEntity.ok(user))
        .orElse(ResponseEntity.notFound().build());
}

// 201 Created with Location header
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    User created = userService.save(user);
    URI location = URI.create("/api/users/" + created.getId());
    return ResponseEntity.created(location).body(created);
}

// 204 No Content
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

### ResponseEntity Builder Methods:

| Method | Status |
|--------|--------|
| `ResponseEntity.ok(body)` | 200 |
| `ResponseEntity.created(uri).body(body)` | 201 |
| `ResponseEntity.noContent().build()` | 204 |
| `ResponseEntity.badRequest().build()` | 400 |
| `ResponseEntity.notFound().build()` | 404 |

**Best Practice:** Production APIs mein `ResponseEntity<T>` use karo jab status ya headers customize karne hon.

---

## Q5: DTO Pattern kya hai aur Entity directly expose kyu nahi karte?

**Answer:**

**DTO (Data Transfer Object)** data ko ek layer se dusri layer tak safely transfer karne ke liye use hota hai.

```
Client → Request DTO → Service → Entity → Database
Database → Entity → Service → Response DTO → Client
```

### Problem — Entity Directly Expose:

```java
@Entity
public class User {
    private Long id;
    private String name;
    private String password;      // ❌ Security risk!
    private String resetToken;    // ❌ Sensitive data leak!
}

@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);  // ❌ Password expose ho sakta hai!
}
```

### Solution — DTO Pattern:

```java
// Request DTO
public class CreateUserRequest {
    @NotBlank private String name;
    @Email private String email;
    @Size(min = 8) private String password;
}

// Response DTO
public record UserResponse(Long id, String name, String email, LocalDateTime createdAt) {}
```

### Why Use DTOs:

| Reason | Description |
|--------|-------------|
| **Security** | Password, tokens hide karo |
| **Separation** | Entity = DB schema, DTO = API contract |
| **Flexibility** | Entity change ho toh API affect nahi |
| **Multiple Views** | Summary vs detailed DTOs |
| **Validation** | Request DTO pe `@Valid` lagao |

### Entity to DTO Mapping:

```java
// Manual
private UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
}

// MapStruct (automated)
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
    User toEntity(CreateUserRequest request);
}
```

---

## Q6: Jackson configuration aur common annotations kya hain?

**Answer:**

**Jackson** Spring Boot ka default JSON library hai — Java objects ↔ JSON convert karta hai.

### application.properties:

```properties
spring.jackson.default-property-inclusion=non_null
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.deserialization.fail-on-unknown-properties=false
spring.jackson.time-zone=Asia/Kolkata
```

### Common Jackson Annotations:

```java
public class UserResponse {

    @JsonProperty("user_id")           // Custom JSON field name
    private Long id;

    @JsonIgnore                        // Serialization/deserialization se exclude
    private String password;

    @JsonInclude(JsonInclude.Include.NON_NULL)  // null fields skip
    private String middleName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)  // Request only
    private String password;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)   // Response only
    private Long id;
}
```

### Annotation Summary:

| Annotation | Purpose |
|------------|---------|
| `@JsonProperty` | Custom field name / read-write access |
| `@JsonIgnore` | Field exclude from JSON |
| `@JsonInclude` | Include condition (NON_NULL, NON_EMPTY) |
| `@JsonFormat` | Date/number format |
| `@JsonSerialize` / `@JsonDeserialize` | Custom serializer |

### LocalDateTime Handling:

```java
// application.properties
spring.jackson.serialization.write-dates-as-timestamps=false

// Output: "2024-01-15T10:30:00" (ISO format)
// Not: 1705312200000 (timestamp)
```

---

## Q7: Content Negotiation kya hai aur produces/consumes kaise use karte hain?

**Answer:**

Content Negotiation client aur server ke beech data format decide karta hai — JSON, XML, etc.

### How It Works:

```
Client Request:
    Accept: application/json        → "Mujhe JSON chahiye"
    Content-Type: application/json  → "Main JSON bhej raha hoon"

Server Response:
    Content-Type: application/json
    Body: { "name": "Sachin" }
```

### produces — Response Format:

```java
@GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
public User getUser(@PathVariable Long id) { }

// Multiple formats — Accept header ke basis pe
@GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public User getUser(@PathVariable Long id) { }
```

### consumes — Request Format:

```java
@PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
public User createUser(@RequestBody User user) { }
```

### Format Mismatch:

```
POST /users
Content-Type: application/xml

Response: 415 Unsupported Media Type
```

### Common MediaTypes:

| Format | Media Type |
|--------|-----------|
| JSON | `application/json` |
| XML | `application/xml` |
| Plain Text | `text/plain` |
| Form Data | `multipart/form-data` |

**Note:** REST APIs mein mostly JSON use hota hai — XML legacy systems ke liye.

---

## Q8: HATEOAS kya hai aur Spring Boot mein kaise implement karte hain?

**Answer:**

**HATEOAS** = Hypermedia As The Engine Of Application State — API responses mein links include hote hain jo client ko batate hain aage kya kar sakta hai.

### REST Maturity Levels:

```
Level 0: POX — HTTP as transport
Level 1: Resources — Proper URIs
Level 2: HTTP Verbs — GET, POST, PUT, DELETE
Level 3: HATEOAS — Links in responses
```

### Without HATEOAS:

```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com"
}
```

Client ko manually URLs construct karne padte hain.

### With HATEOAS:

```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com",
  "_links": {
    "self": { "href": "http://localhost:8080/api/users/1" },
    "users": { "href": "http://localhost:8080/api/users" },
    "posts": { "href": "http://localhost:8080/api/users/1/posts" }
  }
}
```

### Setup:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-hateoas</artifactId>
</dependency>
```

### Implementation:

```java
@GetMapping("/users/{id}")
public EntityModel<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);

    return EntityModel.of(user,
        linkTo(methodOn(UserController.class).getUser(id)).withSelfRel(),
        linkTo(methodOn(UserController.class).getAllUsers()).withRel("users"),
        linkTo(methodOn(UserController.class).getUserPosts(id)).withRel("posts")
    );
}
```

### Key Components:

| Component | Purpose |
|-----------|---------|
| **EntityModel<T>** | Single resource + links |
| **CollectionModel<T>** | Collection + links |
| **WebMvcLinkBuilder** | Links build karna |
| **PagedModel** | Paginated collection + links |

**Benefits:** API discoverable ban jaata hai, URL changes pe client code change nahi karna padta.

---

## Q9: Pagination REST API mein kaise implement karte hain?

**Answer:**

Spring Data JPA built-in pagination support deta hai.

### Repository:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findAll(Pageable pageable);
}
```

### Controller:

```java
@GetMapping("/users")
public Page<UserResponse> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "ASC") String direction
) {
    Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
    Pageable pageable = PageRequest.of(page, size, sort);

    return userRepository.findAll(pageable).map(this::toResponse);
}
```

**Request:** `GET /api/users?page=0&size=10&sort=name,asc`

**Note:** Page number **zero-based** hai — `page=0` first page.

### Custom Pagination Response:

```json
{
  "content": [
    { "id": 1, "name": "Sachin", "email": "sachin@example.com" }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 45,
  "totalPages": 5,
  "first": true,
  "last": false,
  "hasNext": true
}
```

### Page vs Slice:

| Feature | Page<T> | Slice<T> |
|---------|---------|----------|
| Total Count | ✅ Yes (extra COUNT query) | ❌ No |
| Use When | Page numbers UI | Infinite scroll / Load More |

---

## Q10: REST API development ke best practices kya hain?

**Answer:**

### 1. Proper HTTP Methods aur Status Codes:

```java
POST   → 201 Created
GET    → 200 OK / 404 Not Found
PUT    → 200 OK
DELETE → 204 No Content
Validation fail → 400 Bad Request
```

### 2. DTO Pattern — Entity Expose Mat Karo:

```java
// ✅ Request DTO + Response DTO
@PostMapping
public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request)

// ❌ Entity directly
public User create(@RequestBody User user)
```

### 3. ResponseEntity for Full Control:

```java
return ResponseEntity.created(location).body(createdUser);
return ResponseEntity.notFound().build();
```

### 4. Validation on Request DTOs:

```java
@PostMapping
public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request)
```

### 5. Global Exception Handling:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)  // 404
    @ExceptionHandler(MethodArgumentNotValidException.class)  // 400
}
```

### 6. Consistent URL Naming:

```
✅ /api/users          (plural, lowercase)
✅ /api/users/1/posts
❌ /api/getUsers
❌ /api/User
```

### 7. Versioning:

```
/api/v1/users
/api/v2/users
```

### 8. Security:

- Sensitive fields DTO mein exclude karo (`@JsonIgnore`)
- Password hash kabhi response mein mat bhejo
- Proper authentication/authorization lagao

**Short Interview Answer:**

Use correct HTTP verbs, status codes, DTOs, validation, `ResponseEntity`, global exception handling, and consistent URL conventions.
