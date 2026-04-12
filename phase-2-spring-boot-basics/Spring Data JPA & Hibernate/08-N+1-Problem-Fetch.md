# N+1 Problem & Fetch Strategies

## Status: Not Started

---

## Table of Contents

1. [What is N+1 Problem?](#what-is-n+1-problem)
2. [Why N+1 is Bad](#why-n+1-is-bad)
3. [Solution 1: JOIN FETCH in JPQL](#solution-1-join-fetch-in-jpql)
4. [Solution 2: @EntityGraph](#solution-2-entitygraph)
5. [Solution 3: @BatchSize](#solution-3-batchsize)
6. [EAGER Fetch — Why It's a Bad Solution](#eager-fetch--why-its-a-bad-solution)
7. [Comparison of All Solutions](#comparison-of-all-solutions)

---

## What is N+1 Problem?

**Matlab:** Ek query parent entities fetch karne ke liye + N queries unke related child entities fetch karne ke liye. Total = N+1 queries.

### The Problem

```java
@Entity
public class User {
    @Id private Long id;
    private String name;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Post> posts;
}

@Entity
public class Post {
    @Id private Long id;
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

```java
// ❌ N+1 Problem
List<User> users = userRepository.findAll();

for (User user : users) {
    System.out.println(user.getName());
    // Lazy loaded — jab posts access karoge tab query fire hogi
    System.out.println(user.getPosts().size());
}
```

### Generated SQL

```sql
-- Query 1: All users fetch (1 query)
SELECT * FROM users;

-- Query 2: Posts for user 1 (N queries — har user ke liye alag)
SELECT * FROM posts WHERE user_id = 1;

-- Query 3: Posts for user 2
SELECT * FROM posts WHERE user_id = 2;

-- Query 4: Posts for user 3
SELECT * FROM posts WHERE user_id = 3;

-- ... N more queries
```

**Total:** 1 (users) + N (posts per user) = **N+1 queries**

### Visual Representation

```
Database Queries:
┌──────────────────────────────────────────────┐
│ Query 1: SELECT * FROM users                 │ ← 1 query
│ Result: [User1, User2, User3, ..., UserN]   │
└──────────────────────────────────────────────┘
         ↓
┌──────────────────────────────────────────────┐
│ Query 2: SELECT * FROM posts WHERE user_id=1 │ ← N queries
│ Query 3: SELECT * FROM posts WHERE user_id=2 │
│ Query 4: SELECT * FROM posts WHERE user_id=3 │
│ ...                                          │
│ Query N+1: SELECT * FROM posts WHERE user_id=N│
└──────────────────────────────────────────────┘

Total: N+1 queries!
```

---

## Why N+1 is Bad

### Performance Impact

| Users | Posts per User | Total Queries (N+1) | Time (50ms/query) |
|-------|----------------|---------------------|-------------------|
| 10 | 5 | 11 | 550ms |
| 100 | 5 | 101 | 5,050ms (~5s) |
| 1,000 | 5 | 1,001 | 50,050ms (~50s) |

### Problems

| Problem | Description |
|---------|-------------|
| **Slow queries** | Har query round-trip time leti hai |
| **Database load** | Database pe unnecessary pressure |
| **Network overhead** | Each query = network round-trip |
| **Memory waste** | Multiple result sets process karne padte hain |

### How to Detect N+1

```properties
# Hibernate SQL logging enable karo
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Log SQL bindings
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

**Look for:** Repeated `SELECT * FROM posts WHERE user_id = ?` queries with different IDs.

---

## Solution 1: JOIN FETCH in JPQL

**Matlab:** JPQL query mein `JOIN FETCH` use karo — related entities ko ek hi query mein fetch karo.

### Usage

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // ❌ Without JOIN FETCH — N+1
    List<User> findAll();

    // ✅ With JOIN FETCH — 1 query
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.posts")
    List<User> findAllWithPosts();

    // With WHERE clause
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.posts WHERE u.active = true")
    List<User> findActiveUsersWithPosts();

    // Multiple JOIN FETCH
    @Query("SELECT DISTINCT u FROM User u JOIN FETCH u.posts JOIN FETCH u.comments")
    List<User> findAllWithPostsAndComments();
}
```

### Generated SQL

```sql
-- JOIN FETCH: Sirf 1 query!
SELECT DISTINCT u.*, p.*
FROM users u
INNER JOIN posts p ON u.id = p.user_id;
```

### ⚠️ Important: DISTINCT Use

```java
// ❌ Without DISTINCT — duplicate users aayenge (JOIN ki wajah se)
@Query("SELECT u FROM User u JOIN FETCH u.posts")
List<User> findAllWithPosts();

// ✅ With DISTINCT — duplicates hat jaayenge
@Query("SELECT DISTINCT u FROM User u JOIN FETCH u.posts")
List<User> findAllWithPosts();
```

### JOIN FETCH with Pagination

```java
// ❌ JOIN FETCH + Pageable = Error
@Query("SELECT DISTINCT u FROM User u JOIN FETCH u.posts")
Page<User> findAllWithPosts(Pageable pageable);  // May not work correctly

// ✅ Two approaches:

// Approach 1: Count query separately
@Query(value = "SELECT DISTINCT u FROM User u JOIN FETCH u.posts",
       countQuery = "SELECT COUNT(u) FROM User u")
Page<User> findAllWithPosts(Pageable pageable);

// Approach 2: Use @EntityGraph (see below)
```

---

## Solution 2: @EntityGraph

**Matlab:** Annotation-based approach — Spring Data automatically JOIN query generate karta hai.

### Usage

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // @EntityGraph — related entities eagerly fetch karo
    @EntityGraph(attributePaths = {"posts"})
    List<User> findAll();

    // With condition
    @EntityGraph(attributePaths = {"posts"})
    List<User> findByActiveTrue();

    // Multiple associations
    @EntityGraph(attributePaths = {"posts", "comments"})
    List<User> findAll();

    // Custom query ke saath
    @EntityGraph(attributePaths = {"posts"})
    @Query("SELECT u FROM User u WHERE u.name LIKE %:keyword%")
    List<User> searchByName(@Param("keyword") String keyword);
}
```

### Generated SQL

```sql
-- @EntityGraph automatically JOIN use karta hai
SELECT u.*, p.*
FROM users u
LEFT OUTER JOIN posts p ON u.id = p.user_id
WHERE u.active = true;
```

### @EntityGraph Types

```java
// FETCH graph — specified attributes + their eagerly fetched associations
@EntityGraph(attributePaths = {"posts"}, type = EntityGraph.EntityGraphType.FETCH)
List<User> findAll();

// LOAD graph — specified attributes (default)
@EntityGraph(attributePaths = {"posts"})  // Same as above
List<User> findAll();
```

### @EntityGraph vs JOIN FETCH

| Feature | @EntityGraph | JOIN FETCH |
|---------|-------------|------------|
| **Syntax** | Annotation — cleaner | JPQL mein likhna padta hai |
| **Flexibility** | Less — fixed attributes | More — complex queries possible |
| **Pagination** | ✅ Works well | ⚠️ Issues with Page |
| **Readability** | ✅ Better | ❌ Verbose |
| **Multiple JOINs** | ✅ Easy | ✅ Possible but complex |

---

## Solution 3: @BatchSize

**Matlab:** Lazy-loaded associations ko batch mein fetch karta hai — N queries ki jagah 1 query with IN clause.

### Usage

```java
@Entity
public class User {
    @Id private Long id;
    private String name;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @BatchSize(size = 10)  // 10 users ke posts ek query mein
    private List<Post> posts;
}
```

### Generated SQL

```sql
-- Without @BatchSize: N queries
SELECT * FROM posts WHERE user_id = 1;
SELECT * FROM posts WHERE user_id = 2;
SELECT * FROM posts WHERE user_id = 3;
...

-- With @BatchSize(size=10): 1 query with IN clause
SELECT * FROM posts WHERE user_id IN (1, 2, 3, ..., 10);
SELECT * FROM posts WHERE user_id IN (11, 12, 13, ..., 20);
...
```

### Total Queries Reduction

```
Without @BatchSize:  N queries (100 users = 100 queries)

With @BatchSize(10): N/10 queries (100 users = 10 queries)
With @BatchSize(50): N/50 queries (100 users = 2 queries)
```

### Class-Level @BatchSize

```java
@Entity
@BatchSize(size = 25)
public class User {
    // All lazy-loaded collections of this entity will use batch size 25
}
```

### @BatchSize vs @EntityGraph

| Feature | @BatchSize | @EntityGraph |
|---------|-----------|--------------|
| **Queries** | Multiple (reduced) | Single (JOIN) |
| **When** | Jab lazy load actually trigger ho | Immediately (eager) |
| **Memory** | Less (loads on demand) | More (loads everything) |
| **Flexibility** | Runtime pe decide | Compile-time fixed |
| **Best For** | When not always needed | When always needed |

---

## EAGER Fetch — Why It's a Bad Solution

```java
// ❌ BAD: EAGER fetch — hamesha load hoga
@Entity
public class User {
    @Id private Long id;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)  // ❌
    private List<Post> posts;
}
```

### Why EAGER is Bad

| Problem | Description |
|---------|-------------|
| **Always loads** | Jab posts chahiye hi nahi, tab bhi load hoga |
| **No control** | Query mein specify nahi kar sakte |
| **Cartesian product** | Multiple EAGER associations = huge result set |
| **Hidden N+1** | N+1 problem nahi dikhta, but phir bhi hota hai |

### Example: Multiple EAGER

```java
@Entity
public class User {
    @OneToMany(fetch = FetchType.EAGER)
    private List<Post> posts;     // EAGER

    @OneToMany(fetch = FetchType.EAGER)
    private List<Comment> comments;  // EAGER

    @ManyToOne(fetch = FetchType.EAGER)
    private Department department;   // EAGER
}
```

**Result:** Cartesian product — 10 posts × 20 comments = 200 rows per user!

### Recommended Approach

```java
// ✅ LAZY by default
@Entity
public class User {
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Post> posts;
}

// Fetch jab chahiye tab explicitly fetch karo
@EntityGraph(attributePaths = {"posts"})
List<User> findByName(String name);

// Ya JOIN FETCH
@Query("SELECT DISTINCT u FROM User u JOIN FETCH u.posts WHERE u.name = :name")
List<User> findByNameWithPosts(@Param("name") String name);
```

---

## Comparison of All Solutions

| Solution | Queries | When to Use | Best For |
|----------|---------|-------------|----------|
| **N+1 (LAZY)** | 1 + N | ❌ Never intentional | — |
| **EAGER** | 1 + N (hidden) | ❌ Avoid | — |
| **JOIN FETCH** | 1 | Related data hamesha chahiye | Specific queries |
| **@EntityGraph** | 1 (JOIN) | Related data usually chahiye | Default behavior |
| **@BatchSize** | N/batchSize | Related data sometimes chahiye | Large datasets |
| **DTO Projection** | 1 | Sirf specific columns chahiye | List endpoints |

### Decision Flow

```
Related entity chahiye?
├── Hamesha chahiye → @EntityGraph ya JOIN FETCH
├── Kabhi-kabhi chahiye → @BatchSize
├── Sirf few columns chahiye → DTO Projection
└── Bilkul nahi chahiye → LAZY (default) — don't fetch
```

### Practical Example

```java
public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ EntityGraph — posts usually chahiye
    @EntityGraph(attributePaths = {"posts"})
    List<User> findAll();

    // ✅ JOIN FETCH — specific user with all data
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.posts p " +
           "JOIN FETCH u.comments c " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithDetails(@Param("Long") id);

    // ✅ BatchSize — kabhi-kabhi posts chahiye
    // (Entity level pe @BatchSize(size = 10))
    List<User> findByName(String name);

    // ✅ DTO Projection — sirf list view ke liye
    @Query("SELECT new com.example.dto.UserSummary(u.id, u.name, u.email) FROM User u")
    List<UserSummary> findAllSummaries();
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **N+1 Problem** | 1 query for list + N queries for each item's association |
| **Detection** | Hibernate SQL logging enable karo — repeated SELECT dekho |
| **JOIN FETCH** | JPQL mein `JOIN FETCH` — 1 query mein sab fetch |
| **@EntityGraph** | Annotation-based JOIN — cleaner syntax |
| **@BatchSize** | IN clause use karo — N queries ko N/batchSize mein reduce |
| **EAGER Fetch** | ❌ Avoid — hidden N+1, no control, cartesian product |
| **LAZY Fetch** | ✅ Default — jab actually chahiye tab fetch |
| **DTO Projection** | Sirf specific columns — best for list endpoints |
