# Projections

## Status: Not Started

---

## Table of Contents

1. [What are Projections?](#what-are-projections)
2. [Interface Projections](#interface-projections)
3. [Class Projections (DTOs)](#class-projections-dtos)
4. [@Value SpEL in Projections](#value-spel-in-projections)
5. [Dynamic Projections](#dynamic-projections)
6. [Reducing Data Fetched from DB](#reducing-data-fetched-from-db)

---

## What are Projections?

**Matlab:** Sirf specific columns/fields fetch karna — poora entity nahi. Performance optimize karne ka tarika hai.

### Without Projection (Full Entity Fetch)

```java
// ❌ Saare columns fetch ho rahe hain
@Entity
public class User {
    @Id private Long id;
    private String name;
    private String email;
    private String password;      // ❌ Unnecessary
    private String resetToken;    // ❌ Unnecessary
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findAll();  // SELECT * FROM users
}
```

**Query:** `SELECT id, name, email, password, reset_token, created_at, updated_at FROM users`

### With Projection (Only Needed Columns)

```java
// ✅ Sirf chahiye wahi columns
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

public interface UserRepository extends JpaRepository<User, Long> {
    List<UserSummary> findAllBy();  // SELECT id, name, email FROM users
}
```

**Query:** `SELECT id, name, email FROM users`

**Fayda:**
- Less data transfer
- Faster queries
- Less memory usage
- Sensitive fields expose nahi hote

---

## Interface Projections

**Matlab:** Spring Data automatically interface implement kar deta hai — sirf getter methods define karo.

### Closed Projections

Sirf entity ke existing fields return karta hai.

```java
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

public interface UserRepository extends JpaRepository<User, Long> {
    // Interface projection — Spring Data automatically implement karega
    List<UserSummary> findAllBy();

    List<UserSummary> findByActiveTrue();

    Optional<UserSummary> findById(Long id);
}
```

**How It Works:**
```java
// Spring Data internally creates proxy
public class UserSummaryProxy implements UserSummary {
    private final User user;  // Actual entity

    @Override
    public Long getId() { return user.getId(); }

    @Override
    public String getName() { return user.getName(); }

    @Override
    public String getEmail() { return user.getEmail(); }
}
```

### Open Projections

Computed / derived fields return kar sakta hai — jo entity mein directly nahi hain.

```java
public interface UserDetail {
    Long getId();
    String getName();

    // Computed field — entity mein nahi hai
    @Value("#{target.name + ' (' + target.email + ')'}")
    String getDisplayName();

    // Method call se value derive karo
    @Value("#{target.posts.size()}")
    int getPostCount();
}

public interface UserRepository extends JpaRepository<User, Long> {
    List<UserDetail> findAllBy();
}
```

### Nested Projections

```java
public interface PostSummary {
    Long getId();
    String getTitle();
    UserAuthor getAuthor();  // Nested projection

    interface UserAuthor {
        Long getId();
        String getName();
    }
}

public interface PostRepository extends JpaRepository<Post, Long> {
    List<PostSummary> findAllBy();
}
```

**Response:**
```json
{
  "id": 1,
  "title": "Spring Boot Guide",
  "author": {
    "id": 5,
    "name": "Sachin"
  }
}
```

---

## Class Projections (DTOs)

**Matlab:** Constructor expression use karke DTOs mein data map karna.

### DTO Class

```java
public class UserDTO {

    private Long id;
    private String name;
    private String email;

    // Constructor — field names aur order match karna chahiye
    public UserDTO(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
```

### Usage with @Query (Constructor Expression)

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT new com.example.dto.UserDTO(u.id, u.name, u.email) FROM User u")
    List<UserDTO> findAllDTOs();

    @Query("SELECT new com.example.dto.UserDTO(u.id, u.name, u.email) FROM User u WHERE u.active = true")
    List<UserDTO> findActiveUserDTOs();
}
```

### Usage with Interface (Spring Data 2.7+)

Spring Data automatically constructor ko match kar leta hai:

```java
public record UserRecord(Long id, String name, String email) { }

public interface UserRepository extends JpaRepository<User, Long> {
    List<UserRecord> findAllBy();  // Auto-mapped!
}
```

### Interface vs Class Projections

| Feature | Interface Projection | Class Projection (DTO) |
|---------|---------------------|----------------------|
| **Definition** | Interface with getters | Class with constructor |
| **Spring Data** | Auto-implements | Constructor expression in JPQL |
| **Computed Fields** | ✅ @Value SpEL | ❌ No (constructor mein logic) |
| **Type-Safe** | ✅ Yes | ✅ Yes |
| **Performance** | Proxy overhead | Direct object creation |
| **Best For** | Simple read-only views | Complex DTOs with logic |

---

## @Value SpEL in Projections

**Matlab:** Spring Expression Language (SpEL) se computed fields banana.

### Basic SpEL

```java
public interface UserProfile {
    Long getId();

    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();

    @Value("#{target.email != null ? target.email : 'No Email'}")
    String getContactEmail();
}
```

### Method Calls

```java
public interface UserStats {
    Long getId();
    String getName();

    @Value("#{target.posts.size()}")
    int getPostCount();

    @Value("#{target.posts.?[title.contains('Spring')].size()}")
    int getSpringPostCount();

    @Value("#{target.createdAt != null ? T(java.time.Period).between(target.createdAt, T(java.time.LocalDateTime).now()).getYears() : 0}")
    int getAccountAge();
}
```

### Conditional Logic

```java
public interface UserStatus {
    Long getId();

    @Value("#{target.active ? 'Active' : 'Inactive'}")
    String getStatusLabel();

    @Value("#{target.lastLoginAt != null ? 'Logged In' : 'Never Logged In'}")
    String getLoginStatus();
}
```

### SpEL Expression Limitations

```java
// ❌ Complex expressions may not work
@Value("#{target.posts.stream().filter(p -> p.getViews() > 1000).count()}")
int getPopularPostCount();  // Might fail

// ✅ Alternative: Method reference use karo
@Value("#{target.posts.?[views > 1000].size()}")
int getPopularPostCount();  // Works
```

---

## Dynamic Projections

**Matlab:** Runtime pe decide karna ki kaunsa projection use karna hai.

### Usage

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // Dynamic projection — type parameter se decide hota hai
    <T> List<T> findById(Long id, Class<T> type);

    <T> List<T> findByActiveTrue(Class<T> type);

    <T> Optional<T> findById(Long id, Class<T> type);
}
```

### Controller

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // Client decides projection via query param
    @GetMapping("/{id}")
    public Object getUser(@PathVariable Long id,
                          @RequestParam(defaultValue = "full") String view) {

        return switch (view) {
            case "summary" -> userRepository.findById(id, UserSummary.class);
            case "stats" -> userRepository.findById(id, UserStats.class);
            default -> userRepository.findById(id, User.class);  // Full entity
        };
    }

    @GetMapping
    public List<?> getUsers(@RequestParam(defaultValue = "summary") String view) {
        return switch (view) {
            case "summary" -> userRepository.findByActiveTrue(UserSummary.class);
            case "stats" -> userRepository.findByActiveTrue(UserStats.class);
            default -> userRepository.findByActiveTrue(User.class);
        };
    }
}
```

### Request Examples

```
GET /api/users/1?view=summary
→ { "id": 1, "name": "Sachin", "email": "sachin@example.com" }

GET /api/users/1?view=stats
→ { "id": 1, "name": "Sachin", "postCount": 15, "springPostCount": 8 }

GET /api/users/1?view=full
→ { "id": 1, "name": "Sachin", "email": "...", "password": "...", ... }

GET /api/users?view=summary
→ [{ "id": 1, "name": "Sachin", "email": "..." }, ...]
```

---

## Reducing Data Fetched from DB

Projections ka main purpose — database se sirf required data fetch karna.

### Problem: Full Entity Fetch

```java
@Entity
public class Article {
    @Id private Long id;
    private String title;
    private String content;       // Large TEXT column
    private String authorName;
    private String metadata;      // Large JSON column
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tags;          // Large text
}

// ❌ Saari columns fetch ho rahi hain (including large TEXT/JSON)
List<Article> articles = articleRepository.findAll();
```

### Solution: Projection

```java
public interface ArticlePreview {
    Long getId();
    String getTitle();
    String getAuthorName();
    LocalDateTime getCreatedAt();
}

// ✅ Sirf 4 columns fetch ho rahi hain
List<ArticlePreview> articles = articleRepository.findAllBy();
```

### Generated SQL Comparison

```sql
-- Without Projection (Full Entity)
SELECT id, title, content, author_name, metadata, created_at, updated_at, tags
FROM article;

-- With Projection
SELECT id, title, author_name, created_at
FROM article;
```

**Performance Impact:**
- Less data transfer over network
- Less memory usage in Java
- Faster query execution
- Better response time for APIs

### @EntityGraph Alternative

```java
// @EntityGraph — related entities bhi fetch karo (JOIN se)
@EntityGraph(attributePaths = {"posts", "comments"})
List<User> findAllWithDetails();

// vs Projection — sirf specific columns
public interface UserWithPostCount {
    Long getId();
    String getName();
    @Value("#{target.posts.size()}")
    int getPostCount();
}
```

### When to Use What

| Scenario | Use |
|----------|-----|
| All columns chahiye | Full Entity |
| Sirf few columns chahiye | Projection |
| Related entities bhi chahiye | @EntityGraph |
| Runtime pe decide karna hai | Dynamic Projection |
| Computed fields chahiye | @Value SpEL |
| Complex DTO logic chahiye | Class Projection (DTO) |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Projection** | Sirf specific columns fetch karo — poora entity nahi |
| **Interface Projection** | Spring Data auto-implement — getters define karo |
| **Closed Projection** | Entity ke existing fields return karta hai |
| **Open Projection** | @Value SpEL se computed fields add kar sakte ho |
| **Class Projection** | Constructor expression se DTO mein map karo |
| **@Value SpEL** | Computed/derived fields — `#{target.field}` |
| **Dynamic Projection** | Runtime pe projection type decide karo |
| **Performance** | Less data transfer, faster queries, less memory |
| **Nested Projection** | Related entities ka bhi projection define karo |
