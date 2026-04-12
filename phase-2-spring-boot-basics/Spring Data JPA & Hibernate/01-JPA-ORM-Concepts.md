# JPA & ORM Concepts

## Status: Not Started

---

## Table of Contents

1. [Object-Relational Mapping (ORM)](#object-relational-mapping-orm)
2. [JPA Specification vs Hibernate Implementation](#jpa-specification-vs-hibernate-implementation)
3. [EntityManager](#entitymanager)
4. [Persistence Context (First-Level Cache)](#persistence-context-first-level-cache)
5. [Entity States](#entity-states)

---

## Object-Relational Mapping (ORM)

**Matlab:** Java objects (OOP world) ko database tables (relational world) mein map karne ka technique.

### The Problem Without ORM

```java
// Traditional JDBC — bahut zyada boilerplate
public User findById(Long id) {
    String sql = "SELECT id, name, email, created_at FROM users WHERE id = ?";
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        conn = dataSource.getConnection();
        stmt = conn.prepareStatement(sql);
        stmt.setLong(1, id);
        rs = stmt.executeQuery();

        if (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            user.setCreatedAt(rs.getTimestamp("created_at"));
            return user;
        }
        return null;
    } finally {
        // Manual cleanup
        if (rs != null) rs.close();
        if (stmt != null) stmt.close();
        if (conn != null) conn.close();
    }
}
```

### With ORM (JPA/Hibernate)

```java
// Same kaam — sirf ek line
public User findById(Long id) {
    return entityManager.find(User.class, id);
}

// Ya Spring Data JPA aur bhi simple
public User findById(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

### ORM Advantages

| Advantage | Description |
|-----------|-------------|
| **Less boilerplate** | ResultSet → Object mapping automatic |
| **Type-safe** | Compile-time pe errors pakde jaate hain |
| **Database independent** | SQL dialect automatically handle hota hai |
| **Caching** | First-level cache built-in |
| **Relationships** | OneToMany, ManyToOne easily map hote hain |
| **Lazy loading** | Data on-demand load hota hai |

### How ORM Works

```
Java Class (Entity)         Database Table
═════════════════════       ═══════════════════════

@Entity                     CREATE TABLE users (
public class User {           id BIGINT PRIMARY KEY,
    @Id                       name VARCHAR(100),
    private Long id;          email VARCHAR(100),

    @Column                   created_at TIMESTAMP
    private String name;    );
    
    private String email;

    private LocalDateTime createdAt;
}
```

---

## JPA Specification vs Hibernate Implementation

### JPA (Java Persistence API)

- **Ek specification hai** (interface + annotations)
- **Koi implementation nahi** — sirf rules define karta hai
- **Part of Java EE / Jakarta EE**
- Standard way Java apps ko databases se connect karne ka

### Hibernate

- **JPA ka implementation hai** — JPA ke rules follow karta hai
- **Sabse popular ORM framework** Java ka
- **Extra features** deta hai jo JPA spec mein nahi hain
- Spring Boot mein default JPA provider hai

### Comparison

| Feature | JPA | Hibernate |
|---------|-----|-----------|
| **Type** | Specification (interfaces) | Implementation (concrete classes) |
| **Package** | `jakarta.persistence.*` | `org.hibernate.*` |
| **EntityManager** | `EntityManager` | `Session` (Hibernate specific) |
| **Extra Features** | ❌ No | ✅ Yes (Session, Criteria API, Interceptors) |
| **Annotation** | `@Entity`, `@Table`, etc. | Same + `@DynamicInsert`, `@Formula`, etc. |

### Relationship

```
JPA Specification (jakarta.persistence)
    ↑ implements
Hibernate (org.hibernate)
    ↑ used by
Spring Data JPA (org.springframework.data.jpa)
```

### Spring Boot Dependencies

```xml
<!-- Spring Data JPA — Hibernate automatically included -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Iske andar:
     - spring-data-jpa (Spring Data interfaces)
     - hibernate-core (JPA implementation)
     - jakarta.persistence-api (JPA spec)
     - spring-orm (Spring ORM support)
-->
```

---

## EntityManager

**Matlab:** JPA ka core interface — database operations (CRUD) perform karne ke liye use hota hai.

### Usage in Spring

```java
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final EntityManager entityManager;

    // Find by ID
    public User findById(Long id) {
        return entityManager.find(User.class, id);
    }

    // Save / Update
    public User save(User user) {
        if (user.getId() == null) {
            entityManager.persist(user);  // INSERT
            return user;
        } else {
            return entityManager.merge(user);  // UPDATE
        }
    }

    // Delete
    public void delete(User user) {
        // Detached entity ko pehle attach karo
        User managed = entityManager.merge(user);
        entityManager.remove(managed);  // DELETE
    }

    // JPQL Query
    public List<User> findByName(String name) {
        return entityManager.createQuery(
                "SELECT u FROM User u WHERE u.name = :name", User.class)
            .setParameter("name", name)
            .getResultList();
    }
}
```

### EntityManager Methods

| Method | Purpose | SQL |
|--------|---------|-----|
| **find()** | Entity by ID fetch karo | `SELECT` |
| **persist()** | Naya entity save karo | `INSERT` |
| **merge()** | Entity update karo | `UPDATE` |
| **remove()** | Entity delete karo | `DELETE` |
| **createQuery()** | JPQL query run karo | `SELECT` |
| **createNativeQuery()** | Native SQL run karo | `SELECT` |
| **flush()** | Pending changes DB mein bhejo | All pending SQL |
| **clear()** | Persistence context clear karo | — |

### EntityManager vs JpaRepository

```java
// EntityManager — low-level, manual
public User findByName(String name) {
    return entityManager.createQuery(
            "SELECT u FROM User u WHERE u.name = :name", User.class)
        .setParameter("name", name)
        .getSingleResult();
}

// JpaRepository — high-level, Spring Data
public List<User> findByName(String name) {
    return userRepository.findByName(name);  // Auto-implemented!
}
```

---

## Persistence Context (First-Level Cache)

**Matlab:** EntityManager ka internal cache — entities ko track karta hai. Same transaction mein same entity do baar fetch nahi hogi.

### How It Works

```java
// First query — DB se fetch hota hai
User user1 = entityManager.find(User.class, 1L);
// SQL: SELECT * FROM users WHERE id = 1

// Second query — Persistence Context se milega (DB hit nahi hoga)
User user2 = entityManager.find(User.class, 1L);
// No SQL — cache se mila

System.out.println(user1 == user2);  // true (same Java object!)
```

### Auto-Dirty Checking

```java
// Entity fetch karo
User user = entityManager.find(User.class, 1L);

// Field change karo
user.setName("Updated Name");

// Transaction commit pe — Hibernate automatically UPDATE fire karega
// Koi save() call karne ki zarurat nahi!
// SQL: UPDATE users SET name = 'Updated Name' WHERE id = 1
```

### Persistence Context Scope

| Scope | Description |
|-------|-------------|
| **Transaction-scoped** (default) | Har transaction ka alag Persistence Context |
| **Extended** | Multiple transactions share karta hai (stateful apps) |

### Clear / Flush

```java
// flush() — pending changes DB mein bhejo
entityManager.persist(new User("Sachin"));
// Abhi DB mein naya — pending hai
entityManager.flush();  // Abhi DB mein INSERT hoga

// clear() — sab entities detach karo
entityManager.clear();
// Ab sab entities detached hain — changes track nahi honge
```

---

## Entity States

Har JPA entity ek of 4 states mein ho sakti hai.

### Entity State Lifecycle

```
                    persist()
New (Transient) ──────────────→ Managed
     ↑                              │
     │         merge()              │ remove()
     │         ┌────────────────────│───────→ Removed
     │         │                    │
     └─────────┘                    │ flush()
     Detached ←─────────────────────┘
       (DB se delete)
```

### 1. Transient (New)

Entity create hui hai, lekin Persistence Context ka part nahi hai.

```java
User user = new User();
user.setName("Sachin");
user.setEmail("sachin@example.com");

// State: TRANSIENT
// - DB mein nahi hai
// - ID nahi hai (ya null hai)
// - Hibernate track nahi kar raha
```

### 2. Managed

Entity Persistence Context ka part hai — Hibernate changes track karta hai.

```java
entityManager.persist(user);

// State: MANAGED
// - DB mein insert ho chuka hai
// - ID assign ho gayi hai
// - Hibernate track kar raha hai — koi bhi change automatically DB mein jayega

user.setName("Sachin Updated");
// Transaction commit pe Hibernate automatically UPDATE fire karega
```

### 3. Detached

Entity pehle Managed thi, lekin ab Persistence Context se bahar hai.

```java
entityManager.persist(user);  // Managed
entityManager.clear();         // Sab detach ho gaye

// State: DETACHED
// - DB mein record hai
// - Lekin Hibernate ab track nahi kar raha
// - Changes DB mein nahi jayenge automatically

user.setName("Changed but won't save");  // DB mein nahi jayega

// Wapas attach karna ho toh merge() use karo
User merged = entityManager.merge(user);  // Ab managed hai
```

### 4. Removed

Entity delete mark ho gayi hai — next flush/commit pe DB se delete hogi.

```java
entityManager.persist(user);       // Managed
entityManager.remove(user);        // REMOVED

// State: REMOVED
// - DB se delete mark ho gayi hai
// - Next flush/commit pe actual DELETE query fire hogi

user.setName("Can't revive");  // Entity already marked for deletion
```

### State Comparison

| State | In DB? | Tracked? | Changes Saved? |
|-------|--------|----------|----------------|
| **Transient** | ❌ No | ❌ No | N/A |
| **Managed** | ✅ Yes | ✅ Yes | ✅ Automatically |
| **Detached** | ✅ Yes | ❌ No | ❌ Need merge() |
| **Removed** | ✅ (until flush) | ❌ No | Will be deleted |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **ORM** | Java objects ↔ Database tables — boilerplate kam karta hai |
| **JPA** | Specification (interfaces) — `jakarta.persistence.*` |
| **Hibernate** | JPA ka implementation — extra features deta hai |
| **EntityManager** | Core JPA interface — CRUD operations perform karta hai |
| **Persistence Context** | First-level cache — same entity do baar fetch nahi hoti |
| **Dirty Checking** | Managed entity ke changes automatically save hote hain |
| **Transient** | Naya object — DB mein nahi, tracked nahi |
| **Managed** | DB mein hai, tracked hai, changes auto-save |
| **Detached** | DB mein hai, tracked nahi — merge() se attach karo |
| **Removed** | Delete mark ho gaya — flush pe DB se delete hoga |
