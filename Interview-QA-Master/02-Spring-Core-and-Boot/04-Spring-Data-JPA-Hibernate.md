# Spring Data JPA & Hibernate - Top 10 Interview Questions & Answers

> Covering: JPA/ORM Concepts, Entity Mapping, Relationships, Spring Data Repositories, JPQL/Native Queries, Pagination, Projections, N+1 Problem, and Transactions

---

## Q1: JPA, Hibernate aur Spring Data JPA mein kya difference hai?

**Answer:**

Yeh teen alag concepts hain jo ek saath kaam karte hain, lekin role different hai.

### JPA (Java Persistence API)

- Ek **specification** hai — sirf rules aur interfaces define karta hai
- Koi actual implementation nahi deta
- Package: `jakarta.persistence.*`
- Annotations: `@Entity`, `@Table`, `@Id`, `@Column`, etc.

### Hibernate

- JPA ka **implementation** hai
- Actual ORM engine jo database operations perform karta hai
- Package: `org.hibernate.*`
- JPA ke saath extra features bhi deta hai (`@Formula`, `@BatchSize`, etc.)
- Spring Boot mein default JPA provider hai

### Spring Data JPA

- JPA ke upar ek **abstraction layer** hai
- Repository interfaces se automatically queries generate karta hai
- Package: `org.springframework.data.jpa.*`
- `JpaRepository`, method name derivation, `@Query` provide karta hai

### Relationship:

```
JPA Specification (jakarta.persistence)
    ↑ implements
Hibernate (org.hibernate)
    ↑ used by
Spring Data JPA (org.springframework.data.jpa)
```

### Dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Iske andar automatically:
     - spring-data-jpa
     - hibernate-core
     - jakarta.persistence-api
     - spring-orm
-->
```

### JDBC vs ORM Comparison:

| Feature | JDBC | JPA/Hibernate |
|---------|------|---------------|
| Boilerplate | Bahut zyada | Kam |
| Type-safe | ❌ No | ✅ Yes |
| Database independent | ❌ No | ✅ Yes |
| Relationships | Manual JOIN | `@OneToMany`, `@ManyToOne` |
| Caching | Manual | Built-in (Persistence Context) |

**Short Interview Answer:**

JPA specification hai, Hibernate uska implementation hai, aur Spring Data JPA repositories ke through database access aur query generation simplify karta hai.

---

## Q2: ORM kya hai aur Entity States (Transient, Managed, Detached, Removed) kya hote hain?

**Answer:**

**ORM (Object-Relational Mapping)** Java objects ko database tables se map karta hai. Isse hum SQL manually likhne ki jagah Java objects ke saath kaam kar sakte hain.

### ORM Example:

```java
// JDBC — bahut boilerplate
User user = new User();
user.setId(rs.getLong("id"));
user.setName(rs.getString("name"));

// JPA — ek line
User user = userRepository.findById(1L).orElse(null);
```

### Entity States:

Har JPA entity 4 states mein se kisi ek mein hoti hai.

| State | In DB? | Tracked? | Changes Saved? |
|-------|--------|----------|----------------|
| **Transient** | ❌ No | ❌ No | N/A |
| **Managed** | ✅ Yes | ✅ Yes | ✅ Automatically |
| **Detached** | ✅ Yes | ❌ No | ❌ Need `merge()` |
| **Removed** | ✅ (until flush) | ❌ No | Will be deleted |

### 1. Transient (New)

```java
User user = new User();
user.setName("Sachin");
// DB mein nahi hai, Hibernate track nahi kar raha
```

### 2. Managed

```java
entityManager.persist(user);
// DB mein insert ho gaya, Hibernate track kar raha hai

user.setName("Updated Name");
// Transaction commit pe automatically UPDATE fire hoga — save() call ki zarurat nahi!
```

### 3. Detached

```java
entityManager.persist(user);  // Managed
entityManager.clear();         // Ab detached

user.setName("Changed");  // DB mein nahi jayega

User merged = entityManager.merge(user);  // Wapas managed
```

### 4. Removed

```java
entityManager.remove(user);
// Next flush/commit pe DELETE query fire hogi
```

### Persistence Context (First-Level Cache):

```java
User user1 = entityManager.find(User.class, 1L);  // DB hit
User user2 = entityManager.find(User.class, 1L);  // Cache se — no DB hit

System.out.println(user1 == user2);  // true — same object!
```

**Important:**

Persistence Context ek transaction-scoped cache hai. Managed entities ke changes Hibernate automatically track karta hai (dirty checking) — explicitly `save()` call karne ki zarurat nahi hoti jab entity already managed ho.

---

## Q3: Entity mapping ke important annotations kya hain — @Entity, @Id, @GeneratedValue, @Column?

**Answer:**

Entity mapping mein Java class ko database table se connect karte hain.

### @Entity

Class ko database table se map karta hai.

```java
@Entity
@Table(name = "app_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    public User() {}  // Default constructor zaruri hai
}
```

### @Id

Primary key define karta hai. Har entity mein exactly ek `@Id` field hona chahiye.

### @GeneratedValue Strategies:

| Strategy | Description | Best For |
|----------|-------------|----------|
| **IDENTITY** | Database auto-increment | MySQL, PostgreSQL |
| **SEQUENCE** | Database sequence use karta hai | PostgreSQL, Oracle |
| **AUTO** | Hibernate khud decide karta hai (default) | Cross-database apps |
| **TABLE** | Table se next value leta hai | Legacy databases |

```java
// Most common — MySQL/PostgreSQL
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

### @Column Attributes:

| Attribute | Description | Example |
|-----------|-------------|---------|
| **name** | Column name | `@Column(name = "full_name")` |
| **nullable** | NULL allowed? | `@Column(nullable = false)` |
| **unique** | Unique constraint | `@Column(unique = true)` |
| **length** | String length | `@Column(length = 100)` |
| **updatable** | UPDATE mein include? | `@Column(updatable = false)` |

### Other Important Annotations:

```java
@Transient
private String confirmPassword;  // DB mein persist nahi hoga

@Enumerated(EnumType.STRING)  // ORDINAL avoid karo
private UserRole role;

@Embedded
private Address address;  // Address fields same table mein aayenge
```

**Important:**

- Entity class **final nahi** honi chahiye — Hibernate proxy banata hai
- **Default constructor** (public/protected) zaruri hai
- Enum ke liye hamesha `EnumType.STRING` use karo — `ORDINAL` se enum order change hone pe data corrupt ho sakta hai

---

## Q4: JPA Relationships — @OneToMany, @ManyToOne, mappedBy, Cascade aur FetchType explain karo.

**Answer:**

JPA relationships se entities ke beech database relations map hote hain.

### Relationship Types:

| Annotation | Meaning | Example |
|----------|---------|---------|
| **@OneToOne** | Ek → Ek | User → UserProfile |
| **@OneToMany** | Ek → Multiple | User → Posts |
| **@ManyToOne** | Multiple → Ek | Posts → User |
| **@ManyToMany** | Multiple ↔ Multiple | Student ↔ Course |

### Bidirectional Example:

```java
@Entity
public class User {

    @Id
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();
}

@Entity
public class Post {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

### mappedBy (Owning vs Inverse Side):

| Side | Responsibility | Annotation |
|------|----------------|------------|
| **Owning Side** | Foreign key manage karta hai | `@JoinColumn` |
| **Inverse Side** | Sirf reference karta hai | `mappedBy` |

```java
// Owning side — FK yahan hai
@ManyToOne
@JoinColumn(name = "user_id")
private User user;

// Inverse side — mappedBy = owning side ka FIELD name
@OneToMany(mappedBy = "user")
private List<Post> posts;
```

**⚠️ Common Mistake:**

```java
@OneToMany(mappedBy = "user_id")  // ❌ Column name nahi, field name do
@OneToMany(mappedBy = "user")     // ✅ Correct
```

### Cascade Types:

| Cascade | Effect |
|---------|--------|
| **PERSIST** | Parent save → children bhi save |
| **MERGE** | Parent update → children bhi update |
| **REMOVE** | Parent delete → children bhi delete |
| **ALL** | Sab operations cascade |

```java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Post> posts;

// List se remove kiya → DB se bhi delete (orphanRemoval = true)
user.getPosts().remove(0);
```

### FetchType — LAZY vs EAGER:

| Relationship | Default | Recommended |
|--------------|---------|-------------|
| **@OneToOne** | EAGER | LAZY |
| **@OneToMany** | LAZY | LAZY ✅ |
| **@ManyToOne** | EAGER | LAZY |
| **@ManyToMany** | LAZY | LAZY ✅ |

```java
// LAZY — jab access karoge tab load hoga
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<Post> posts;

// Transaction ke bahar access → LazyInitializationException
```

**Short Interview Answer:**

`@ManyToOne` owning side hota hai (FK wahan hota hai), `@OneToMany` inverse side `mappedBy` use karta hai. Hamesha `LAZY` fetch prefer karo, cascade sirf owning side pe lagao.

---

## Q5: Spring Data JPA Repository hierarchy aur Method Name Query Derivation kaise kaam karta hai?

**Answer:**

Spring Data JPA repository interfaces provide karta hai jinke methods automatically implement ho jaate hain.

### Repository Hierarchy:

```
Repository (Marker)
    ↑
CrudRepository<T, ID>
    ↑
PagingAndSortingRepository<T, ID>
    ↑
JpaRepository<T, ID>  ← Most commonly used
```

| Interface | Features |
|-----------|----------|
| **CrudRepository** | Basic CRUD — save, findById, delete |
| **PagingAndSortingRepository** | + Pagination + Sorting |
| **JpaRepository** | + flush, batch delete, findAllById |

### Basic Usage:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Methods already available — implement nahi karna padta!
}

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElse(null);
    }
}
```

### Method Name Query Derivation:

Spring Data method name dekh ke automatically query generate kar deta hai.

```
findBy + [Field] + [Keyword]

findByNameAndEmail → WHERE name = ? AND email = ?
```

### Examples:

```java
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByName(String name);
    User findByEmail(String email);
    List<User> findByNameAndEmail(String name, String email);
    List<User> findByAgeGreaterThan(int age);
    List<User> findByNameContaining(String keyword);
    List<User> findByActiveTrue();
    boolean existsByEmail(String email);
    long countByName(String name);

    // Nested properties
    List<User> findByAddressCity(String city);
    List<Post> findByUser_Name(String userName);
}
```

### Common Keywords:

| Keyword | SQL |
|---------|-----|
| `And` | `AND` |
| `Or` | `OR` |
| `GreaterThan` | `> ?` |
| `LessThan` | `< ?` |
| `Between` | `BETWEEN ? AND ?` |
| `Containing` | `LIKE %?%` |
| `In` | `IN (?, ?, ?)` |
| `IsNull` | `IS NULL` |
| `OrderBy` | `ORDER BY` |
| `First` / `Top` | `LIMIT` |

**Important:**

Method name derivation simple queries ke liye best hai. Complex queries ke liye `@Query` use karo — warna method names bahut lambe ho jaate hain.

---

## Q6: JPQL aur Native Query mein kya difference hai? @Query aur @Modifying kaise use karte hain?

**Answer:**

### JPQL vs Native SQL:

| Feature | JPQL | Native SQL |
|---------|------|------------|
| **Works with** | Entities (Java objects) | Tables (database) |
| **Syntax** | `SELECT u FROM User u` | `SELECT * FROM users` |
| **Database Independent** | ✅ Yes | ❌ No |
| **Use When** | Standard queries | DB-specific features chahiye |

```java
// JPQL — entity/field names
"SELECT u FROM User u WHERE u.status = 'ACTIVE'"

// Native SQL — table/column names
"SELECT * FROM users WHERE status = 'ACTIVE'"
```

### @Query with JPQL:

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.name = :name")
    List<User> findByName(@Param("name") String name);

    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    List<User> searchByName(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT u FROM User u JOIN u.posts p WHERE p.title LIKE %:keyword%")
    List<User> findUsersWithPostTitle(@Param("keyword") String keyword);
}
```

### Named vs Positional Parameters:

```java
// ✅ Named — readable, order-independent (recommended)
@Query("SELECT u FROM User u WHERE u.name = :name AND u.age = :age")
List<User> findByNameAndAge(@Param("name") String name, @Param("age") int age);

// ❌ Positional — order matter karta hai, error-prone
@Query("SELECT u FROM User u WHERE u.name = ?1 AND u.age = ?2")
List<User> findByNameAndAge(String name, int age);
```

### Native Query:

```java
@Query(value = "SELECT * FROM users WHERE name = :name", nativeQuery = true)
List<User> findByNameNative(@Param("name") String name);

// Pagination ke liye countQuery bhi specify karo
@Query(value = "SELECT * FROM users WHERE name = :name",
       countQuery = "SELECT COUNT(*) FROM users WHERE name = :name",
       nativeQuery = true)
Page<User> findByNameNativePaginated(@Param("name") String name, Pageable pageable);
```

### @Modifying (UPDATE/DELETE):

By default `@Query` sirf SELECT ke liye hota hai. UPDATE/DELETE ke liye `@Modifying` chahiye.

```java
@Modifying
@Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
int updateName(@Param("id") Long id, @Param("name") String name);

@Modifying
@Query("DELETE FROM User u WHERE u.active = false")
int deleteInactiveUsers();
```

**⚠️ @Modifying queries `@Transactional` ke andar run karni padti hain:**

```java
@Service
public class UserService {

    @Transactional
    public int updateName(Long id, String name) {
        return userRepository.updateName(id, name);
    }
}
```

---

## Q7: Pagination aur Sorting Spring Data JPA mein kaise implement karte hain? Page vs Slice?

**Answer:**

Spring Data JPA built-in pagination aur sorting support deta hai.

### Basic Pagination:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Page<User> findAll(Pageable pageable);
}

// Usage
Pageable pageable = PageRequest.of(0, 10);  // Page 0, 10 items
Page<User> page = userRepository.findAll(pageable);
```

**Note:** Page number **zero-based** hai.

```
PageRequest.of(0, 10)  → First page (items 0-9)
PageRequest.of(1, 10)  → Second page (items 10-19)
```

### With Sorting:

```java
Pageable pageable = PageRequest.of(0, 10,
    Sort.by(Sort.Direction.DESC, "createdAt")
);

// Multiple sort fields
Sort sort = Sort.by(
    Sort.Order.desc("createdAt"),
    Sort.Order.asc("name")
);
```

### Controller Example:

```java
@GetMapping("/users")
public Page<User> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(defaultValue = "id") String sortBy,
    @RequestParam(defaultValue = "ASC") String direction
) {
    Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
    return userRepository.findAll(PageRequest.of(page, size, sort));
}
```

**Request:** `GET /api/users?page=0&size=10&sort=name,asc`

### Page<T> vs Slice<T>:

| Feature | Page<T> | Slice<T> |
|---------|---------|----------|
| **Total Count** | ✅ `getTotalElements()` | ❌ Not available |
| **Total Pages** | ✅ `getTotalPages()` | ❌ Not available |
| **Has Next** | ✅ `hasNext()` | ✅ `hasNext()` |
| **Extra Query** | COUNT(*) fire karta hai | No extra query |
| **Performance** | Slower (large tables) | Faster |
| **Use When** | Page numbers UI chahiye | Infinite scroll / Load More |

```java
Page<User> page = userRepository.findAll(PageRequest.of(0, 10));
page.getTotalElements();  // 150
page.getTotalPages();     // 15
page.getContent();        // Current page data
page.hasNext();           // true

Slice<User> slice = userRepository.findAll(PageRequest.of(0, 10));
slice.hasNext();          // true
// slice.getTotalElements() — NOT available
```

**Short Interview Answer:**

`Page<T>` total count jaanta hai (extra COUNT query), `Slice<T>` sirf next page hai ya nahi batata hai — large datasets ke liye `Slice` faster hai.

---

## Q8: Projections kya hain aur database se data fetch optimize kaise karte hain?

**Answer:**

**Projection** ka matlab hai sirf specific columns/fields fetch karna — poora entity nahi. Isse performance improve hoti hai.

### Problem — Full Entity Fetch:

```java
@Entity
public class User {
    @Id private Long id;
    private String name;
    private String email;
    private String password;      // ❌ Unnecessary expose
    private String resetToken;    // ❌ Unnecessary
}

List<User> users = userRepository.findAll();
// SELECT * FROM users — saari columns
```

### Solution — Interface Projection:

```java
public interface UserSummary {
    Long getId();
    String getName();
    String getEmail();
}

public interface UserRepository extends JpaRepository<User, Long> {
    List<UserSummary> findAllBy();
    // SELECT id, name, email FROM users — sirf required columns
}
```

Spring Data automatically interface implement kar deta hai — sirf getter methods define karo.

### Open Projection (Computed Fields):

```java
public interface UserDetail {
    Long getId();
    String getName();

    @Value("#{target.name + ' (' + target.email + ')'}")
    String getDisplayName();

    @Value("#{target.posts.size()}")
    int getPostCount();
}
```

### Class Projection (DTO):

```java
public record UserDTO(Long id, String name, String email) {}

@Query("SELECT new com.example.dto.UserDTO(u.id, u.name, u.email) FROM User u")
List<UserDTO> findAllDTOs();
```

### Dynamic Projection:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    <T> Optional<T> findById(Long id, Class<T> type);
}

// Runtime pe decide karo
userRepository.findById(1L, UserSummary.class);  // Summary
userRepository.findById(1L, User.class);          // Full entity
```

### When to Use What:

| Scenario | Use |
|----------|-----|
| All columns chahiye | Full Entity |
| Sirf few columns chahiye | Projection |
| Related entities bhi chahiye | `@EntityGraph` |
| Computed fields chahiye | `@Value` SpEL |
| Complex DTO logic | Class Projection |

**Benefits:**

- Less data transfer
- Faster queries
- Less memory usage
- Sensitive fields expose nahi hote (password, tokens)

---

## Q9: N+1 Problem kya hai aur isko kaise solve karte hain?

**Answer:**

**N+1 Problem** tab hota hai jab 1 query parent entities fetch karti hai aur phir har parent ke liye alag query child entities fetch karti hai. Total queries = **N + 1**.

### The Problem:

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Post> posts;
}

// ❌ N+1 Problem
List<User> users = userRepository.findAll();  // 1 query

for (User user : users) {
    user.getPosts().size();  // N queries — har user ke liye alag!
}
```

### Generated SQL:

```sql
-- Query 1
SELECT * FROM users;

-- Query 2 to N+1
SELECT * FROM posts WHERE user_id = 1;
SELECT * FROM posts WHERE user_id = 2;
SELECT * FROM posts WHERE user_id = 3;
-- ... 100 users = 101 queries!
```

### Solution 1: JOIN FETCH

```java
@Query("SELECT DISTINCT u FROM User u JOIN FETCH u.posts")
List<User> findAllWithPosts();

// Sirf 1 query!
-- SELECT u.*, p.* FROM users u INNER JOIN posts p ON u.id = p.user_id
```

### Solution 2: @EntityGraph

```java
@EntityGraph(attributePaths = {"posts"})
List<User> findAll();

// Annotation-based — cleaner syntax
```

### Solution 3: @BatchSize

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<Post> posts;
}

// N queries ki jagah N/10 queries
-- SELECT * FROM posts WHERE user_id IN (1, 2, 3, ..., 10)
```

### Solution Comparison:

| Solution | Queries | Best For |
|----------|---------|----------|
| **JOIN FETCH** | 1 | Related data hamesha chahiye |
| **@EntityGraph** | 1 (JOIN) | Related data usually chahiye |
| **@BatchSize** | N/batchSize | Related data kabhi-kabhi chahiye |
| **DTO Projection** | 1 | Sirf specific columns chahiye |
| **EAGER Fetch** | ❌ Avoid | Hidden N+1, no control |

### EAGER Fetch — Why Bad:

```java
@OneToMany(fetch = FetchType.EAGER)  // ❌ Avoid
private List<Post> posts;
```

- Hamesha load hoga — chahe chahiye ho ya nahi
- Multiple EAGER associations = Cartesian product
- N+1 problem hide ho jaati hai but phir bhi hoti hai

### How to Detect:

```properties
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

Repeated `SELECT * FROM posts WHERE user_id = ?` queries with different IDs dekho.

**Short Interview Answer:**

N+1 = 1 list query + N association queries. Fix: `JOIN FETCH`, `@EntityGraph`, ya `@BatchSize`. EAGER fetch avoid karo.

---

## Q10: @Transactional kya hai? Propagation, readOnly aur rollbackFor explain karo.

**Answer:**

`@Transactional` multiple database operations ko ek **atomic unit** mein wrap karta hai — ya toh sab succeed honge ya sab rollback honge.

### Without Transaction (Problem):

```java
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    from.setBalance(from.getBalance().subtract(amount));
    accountRepository.save(from);  // ✅ Success

    throw new RuntimeException("Error!");  // ❌ Exception

    to.setBalance(to.getBalance().add(amount));
    accountRepository.save(to);  // ❌ Never reached
}
// Result: from se paisa kat gaya, to mein add nahi hua — data inconsistent!
```

### With Transaction (Solution):

```java
@Transactional
public void transferMoney(Long fromId, Long toId, BigDecimal amount) {
    from.setBalance(from.getBalance().subtract(amount));
    accountRepository.save(from);

    throw new RuntimeException("Error!");

    to.setBalance(to.getBalance().add(amount));
    accountRepository.save(to);
}
// Result: Exception → rollback → from account mein paisa wapas aa gaya
```

### Class vs Method Level:

```java
@Service
@Transactional  // Sab methods pe apply
public class UserService {

    public void createUser(User user) { }  // Transactional

    @Transactional(readOnly = true)  // Method level override
    public User findById(Long id) { }
}
```

**Rule:** Method level annotation class level ko override karta hai.

### Propagation Types:

| Propagation | Behavior | Use Case |
|-------------|----------|----------|
| **REQUIRED** (default) | Existing transaction use karo, nahi to naya banao | Most common |
| **REQUIRES_NEW** | Hamesha naya transaction (existing suspend) | Audit logging |
| **NESTED** | Existing ke andar nested — partial rollback | Batch processing |
| **SUPPORTS** | Transaction hai to use karo, nahi to bina | Read operations |

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logAudit(String action) {
    // Outer transaction rollback ho toh bhi yeh commit hoga
    auditRepository.save(new AuditLog(action));
}
```

### readOnly = true:

```java
@Transactional(readOnly = true)
public List<User> findAll() {
    return userRepository.findAll();
}
```

Benefits:
- Database read-only optimization
- Hibernate dirty checking skip karta hai
- Read replicas pe route kar sakta hai

### rollbackFor:

Default mein sirf `RuntimeException` aur `Error` pe rollback hota hai. Checked exceptions pe nahi.

```java
// Checked exception pe bhi rollback
@Transactional(rollbackFor = IOException.class)
public void processOrder(Order order) throws IOException {
    throw new IOException("DB error");  // Ab rollback hoga
}

// Specific exception pe rollback nahi karna
@Transactional(noRollbackFor = ValidationException.class)
public void processOrder(Order order) {
    throw new ValidationException("Invalid");  // Commit hoga
}
```

### Best Practices:

| Practice | Description |
|----------|-------------|
| **Service layer pe lagao** | Controller pe nahi |
| **Self-invocation avoid** | Same class method call → `@Transactional` ignore hota hai |
| **Read methods** | `readOnly = true` use karo |
| **Timeout set karo** | Long operations ke liye `@Transactional(timeout = 30)` |

### Self-Invocation Problem:

```java
@Service
@Transactional
public class UserService {

    public void methodA() {
        methodB();  // ❌ @Transactional ignore — same class call
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void methodB() { }
}

// Fix: Self-inject through proxy
@Autowired
private UserService self;

public void methodA() {
    self.methodB();  // ✅ Proxy ke through — transaction kaam karega
}
```

**Short Interview Answer:**

`@Transactional` atomicity ensure karta hai. Default propagation `REQUIRED` hai. Read operations ke liye `readOnly = true`. Checked exceptions pe rollback ke liye `rollbackFor` use karo. Hamesha service layer pe lagao.
