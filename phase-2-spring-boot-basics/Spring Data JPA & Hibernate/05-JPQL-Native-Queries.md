# JPQL & Native Queries

## Status: Not Started

---

## Table of Contents

1. [JPQL Overview](#jpql-overview)
2. [JPQL Syntax](#jpql-syntax)
3. [@Query with JPQL](#query-with-jpql)
4. [Named Parameters (:name)](#named-parameters-name)
5. [Positional Parameters (?1)](#positional-parameters-1)
6. [Native Queries](#native-queries)
7. [@NamedQuery](#namedquery)
8. [@NamedNativeQuery](#namednativequery)

---

## JPQL Overview

**JPQL (Java Persistence Query Language)** — Entity aur field names use karke query likhte hain (not table/column names).

### JPQL vs SQL

| Feature | JPQL | SQL |
|---------|------|-----|
| **Works with** | Entities (Java objects) | Tables (database) |
| **Syntax** | `SELECT u FROM User u` | `SELECT * FROM users` |
| **Case Sensitive** | Entity/field names as-is | Depends on database |
| **Database Independent** | ✅ Yes | ❌ No (dialect-specific) |
| **Type-Safe** | ✅ Yes (compile-time check) | ❌ No |

### Example

```java
// SQL — table/column names
"SELECT * FROM users u WHERE u.status = 'ACTIVE' ORDER BY u.created_at DESC"

// JPQL — entity/field names
"SELECT u FROM User u WHERE u.status = 'ACTIVE' ORDER BY u.createdAt DESC"
```

---

## JPQL Syntax

### Basic SELECT

```java
// All users
"SELECT u FROM User u"

// With WHERE
"SELECT u FROM User u WHERE u.name = :name"

// With ORDER BY
"SELECT u FROM User u WHERE u.active = true ORDER BY u.createdAt DESC"

// With DISTINCT
"SELECT DISTINCT u.city FROM User u"
```

### JOIN Queries

```java
// INNER JOIN
"SELECT u FROM User u JOIN u.posts p WHERE p.title LIKE %:keyword%"

// LEFT JOIN
"SELECT u FROM User u LEFT JOIN u.posts p WHERE p.id IS NULL"

// Multiple JOINs
"SELECT p FROM Post p JOIN p.user u JOIN u.department d WHERE d.name = :deptName"
```

### Aggregate Functions

```java
// COUNT
"SELECT COUNT(u) FROM User u WHERE u.active = true"

// SUM, AVG, MIN, MAX
"SELECT AVG(u.age) FROM User u"
"SELECT MAX(u.salary) FROM User u"
"SELECT MIN(u.createdAt) FROM User u"

// GROUP BY
"SELECT u.city, COUNT(u) FROM User u GROUP BY u.city"

// HAVING
"SELECT u.city, COUNT(u) FROM User u GROUP BY u.city HAVING COUNT(u) > 5"
```

### Subqueries

```java
"SELECT u FROM User u WHERE u.id IN (SELECT p.user.id FROM Post p WHERE p.title LIKE %:keyword%)"
```

---

## @Query with JPQL

### Basic Usage

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // Simple JPQL
    @Query("SELECT u FROM User u WHERE u.name = :name")
    List<User> findByName(@Param("name") String name);

    // Multiple conditions
    @Query("SELECT u FROM User u WHERE u.name = :name AND u.age = :age")
    List<User> findByNameAndAge(@Param("name") String name, @Param("age") int age);

    // LIKE query
    @Query("SELECT u FROM User u WHERE u.email LIKE %:domain")
    List<User> findByEmailDomain(@Param("domain") String domain);

    // With ORDER BY
    @Query("SELECT u FROM User u WHERE u.active = true ORDER BY u.createdAt DESC")
    List<User> findActiveUsers();

    // JOIN
    @Query("SELECT DISTINCT u FROM User u JOIN u.posts p WHERE p.title LIKE %:keyword%")
    List<User> findUsersWithPostTitle(@Param("keyword") String keyword);
}
```

### Pagination with JPQL

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findActiveUsers(Pageable pageable);
}

// Usage
Page<User> page = userRepository.findActiveUsers(
    PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
);
```

### Count Query (for Pagination)

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    Page<User> searchByName(@Param("keyword") String keyword, Pageable pageable);

    // Custom count query (for complex queries)
    @Query("SELECT COUNT(u) FROM User u WHERE u.name LIKE %:keyword% AND u.active = true")
    long countActiveByName(@Param("keyword") String keyword);
}
```

---

## Named Parameters (:name)

**Matlab:** `:parameterName` syntax use karke parameters pass karte hain.

### Usage

```java
@Query("SELECT u FROM User u WHERE u.name = :name AND u.email = :email")
User findByNameAndEmail(@Param("name") String name, @Param("email") String email);
```

**Fayda:**
- Order-independent — parameter order change kar sakte ho
- Readable — name se pata chal jaata hai kya hai
- Safer — galat value galat parameter mein jaane ka risk nahi

### Multiple Parameters

```java
@Query("SELECT u FROM User u WHERE u.name = :name AND u.age BETWEEN :minAge AND :maxAge AND u.city = :city")
List<User> findByCriteria(@Param("name") String name,
                          @Param("minAge") int minAge,
                          @Param("maxAge") int maxAge,
                          @Param("city") String city);
```

### Collection Parameters (IN clause)

```java
@Query("SELECT u FROM User u WHERE u.city IN :cities")
List<User> findByCities(@Param("cities") List<String> cities);

// Usage
List<User> users = userRepository.findByCities(List.of("Delhi", "Mumbai", "Bangalore"));
// JPQL: WHERE u.city IN ('Delhi', 'Mumbai', 'Bangalore')
```

---

## Positional Parameters (?1)

**Matlab:** `?1, ?2, ?3` syntax — position ke basis pe parameters match hote hain.

### Usage

```java
@Query("SELECT u FROM User u WHERE u.name = ?1 AND u.email = ?2")
User findByNameAndEmail(String name, String email);

// Order matters — ?1 = first parameter, ?2 = second
@Query("SELECT u FROM User u WHERE u.name = ?1 AND u.age = ?2")
List<User> findByNameAndAge(String name, int age);
```

**⚠️ Avoid karo** — error-prone hai. Named parameters (`:name`) better hain.

### Comparison

```java
// ❌ Positional — order matter karta hai, error-prone
@Query("SELECT u FROM User u WHERE u.name = ?1 AND u.age = ?2")
List<User> findByNameAndAge(String name, int age);

// ✅ Named — readable, order-independent
@Query("SELECT u FROM User u WHERE u.name = :name AND u.age = :age")
List<User> findByNameAndAge(@Param("name") String name, @Param("age") int age);
```

---

## Native Queries

**Matlab:** Raw SQL queries — table/column names use hote hain. JPQL se zyada powerful lekin database-dependent.

### When to Use Native Queries

| Scenario | Use Native? |
|----------|-------------|
| Database-specific features (window functions, CTEs) | ✅ Yes |
| Complex queries JPQL mein nahi ban rahi | ✅ Yes |
| Performance optimization | ✅ Sometimes |
| Simple CRUD | ❌ Use JPQL |
| Cross-database app | ❌ Use JPQL |

### @Query with nativeQuery = true

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // Native SQL — table/column names use hote hain
    @Query(value = "SELECT * FROM users WHERE name = :name", nativeQuery = true)
    List<User> findByNameNative(@Param("name") String name);

    // Complex native query
    @Query(value = """
        SELECT u.* FROM users u
        INNER JOIN posts p ON u.id = p.user_id
        WHERE p.created_at > :date
        GROUP BY u.id
        HAVING COUNT(p.id) > :minPosts
        """, nativeQuery = true)
    List<User> findActiveAuthors(@Param("date") LocalDate date, @Param("minPosts") int minPosts);
}
```

### Native Query with Pagination (Spring Data JPA 2.0+)

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // Count query bhi specify karni padti hai native pagination ke liye
    @Query(value = "SELECT * FROM users WHERE name = :name",
           countQuery = "SELECT COUNT(*) FROM users WHERE name = :name",
           nativeQuery = true)
    Page<User> findByNameNativePaginated(@Param("name") String name, Pageable pageable);
}
```

### Native Query with Entity Mapping

```java
// Native query se Entity return karo
@Query(value = "SELECT * FROM users WHERE email = :email", nativeQuery = true)
User findByEmailNative(@Param("email") String email);

// Agar columns match nahi karte toh @SqlResultSetMapping use karo
```

---

## @NamedQuery

**Matlab:** Queries ko entity class mein define karna — reusable aur centralized.

### Usage

```java
@Entity
@Table(name = "users")
@NamedQuery(
    name = "User.findByName",
    query = "SELECT u FROM User u WHERE u.name = :name"
)
@NamedQuery(
    name = "User.findActiveUsers",
    query = "SELECT u FROM User u WHERE u.active = true ORDER BY u.createdAt DESC"
)
public class User {

    @Id
    private Long id;

    private String name;
    private boolean active;
    private LocalDateTime createdAt;
}
```

### Using NamedQueries

```java
// Via EntityManager
List<User> users = entityManager.createNamedQuery("User.findByName", User.class)
    .setParameter("name", "Sachin")
    .getResultList();

// Via Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(name = "User.findByName")
    List<User> findByName(@Param("name") String name);
}
```

---

## @NamedNativeQuery

**Matlab:** Native SQL queries ko entity class mein define karna.

### Usage

```java
@Entity
@Table(name = "users")
@NamedNativeQuery(
    name = "User.findActiveAuthors",
    query = """
        SELECT u.* FROM users u
        INNER JOIN posts p ON u.id = p.user_id
        WHERE p.created_at > :date
        GROUP BY u.id
        HAVING COUNT(p.id) > :minPosts
        """,
    resultClass = User.class
)
public class User {
    // ...
}
```

### Using NamedNativeQuery

```java
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(name = "User.findActiveAuthors", nativeQuery = true)
    List<User> findActiveAuthors(@Param("date") LocalDate date, @Param("minPosts") int minPosts);
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **JPQL** | Entity/field names use karta hai — database independent |
| **JPQL Syntax** | `SELECT u FROM User u WHERE u.name = :name` |
| **Named Parameters (:name)** | Recommended — readable, order-independent |
| **Positional Parameters (?1)** | Avoid — error-prone, order-dependent |
| **Native Query** | Raw SQL — table/column names, database-specific features |
| **@Query(nativeQuery = true)** | JPQL kaafi nahi jab native SQL chahiye |
| **@NamedQuery** | Entity class mein reusable JPQL queries define |
| **@NamedNativeQuery** | Entity class mein reusable native SQL queries define |
