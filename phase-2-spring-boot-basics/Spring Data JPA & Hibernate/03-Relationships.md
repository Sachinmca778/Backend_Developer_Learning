# Relationships

## Status: Not Started

---

## Table of Contents

1. [@OneToOne](#onetoone)
2. [@OneToMany](#onetomany)
3. [@ManyToOne](#manytoone)
4. [@ManyToMany](#manytomany)
5. [@JoinColumn](#joincolumn)
6. [@JoinTable](#jointable)
7. [mappedBy (Owning Side)](#mappedby-owning-side)
8. [Cascade Types](#cascade-types)
9. [FetchType (LAZY vs EAGER)](#fetchtype-lazy-vs-eager)

---

## @OneToOne

**Matlab:** Ek entity ka ek hi related entity hai.

```java
// User → UserProfile (One-to-One)
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToOne
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}

@Entity
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String bio;
    private String avatar;
}
```

**Database:**
```sql
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT,
    name VARCHAR(255),
    profile_id BIGINT,  -- Foreign key
    PRIMARY KEY (id),
    FOREIGN KEY (profile_id) REFERENCES user_profile(id)
);

CREATE TABLE user_profile (
    id BIGINT AUTO_INCREMENT,
    bio TEXT,
    avatar VARCHAR(255),
    PRIMARY KEY (id)
);
```

### Bidirectional One-to-One

```java
// Owning side (foreign key yahan hai)
@Entity
public class User {

    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "profile_id")
    private UserProfile profile;
}

// Inverse side (mappedBy se reference)
@Entity
public class UserProfile {

    @Id
    private Long id;

    @OneToOne(mappedBy = "profile")
    private User user;
}
```

---

## @OneToMany

**Matlab:** Ek entity ka multiple related entities hain.

```java
// User → Posts (One-to-Many)
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();

    // Helper method
    public void addPost(Post post) {
        posts.add(post);
        post.setUser(this);  // Both sides sync karo
    }

    public void removePost(Post post) {
        posts.remove(post);
        post.setUser(null);
    }
}

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String content;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
```

**Database:**
```sql
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE post (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255),
    content TEXT,
    user_id BIGINT,  -- Foreign key
    FOREIGN KEY (user_id) REFERENCES user(id)
);
```

### Unidirectional OneToMany (Without mappedBy)

```java
@Entity
public class User {

    @Id
    private Long id;

    @OneToMany
    @JoinColumn(name = "user_id")  // Foreign key yahan
    private List<Post> posts;
}
```

**⚠️ Not recommended** — extra join table ban sakti hai. Hamesha bidirectional approach use karo.

---

## @ManyToOne

**Matlab:** Multiple entities ka ek hi related entity hai. `@OneToMany` ka reverse side.

```java
@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    // Multiple posts ka ek hi user ho sakta hai
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
```

### Default Behavior

`@ManyToOne` ka **default fetch type EAGER** hai — har baar parent entity bhi load hoti hai.

```java
// Explicit LAZY set karo (recommended)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "user_id")
private User user;
```

---

## @ManyToMany

**Matlab:** Multiple entities ka multiple related entities hain. Ek junction (join) table banti hai.

```java
// Student ↔ Course (Many-to-Many)
@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private List<Course> courses = new ArrayList<>();
}

@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToMany(mappedBy = "courses")
    private List<Student> students = new ArrayList<>();
}
```

**Database:**
```sql
CREATE TABLE student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255)
);

-- Junction table
CREATE TABLE student_course (
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (course_id) REFERENCES course(id)
);
```

---

## @JoinColumn

**Matlab:** Foreign key column ka name customize karta hai.

```java
@Entity
public class Post {

    @Id
    private Long id;

    // Default: column name = "user_id" (field_name + "_id")
    @ManyToOne
    @JoinColumn(name = "author_id")  // Custom name
    private User author;

    // Without @JoinColumn — default name use hoga
    @ManyToOne
    private User reviewer;  // Column: "reviewer_id"
}
```

### @JoinColumn Attributes

| Attribute | Description | Example |
|-----------|-------------|---------|
| **name** | Foreign key column name | `@JoinColumn(name = "author_id")` |
| **nullable** | NULL allowed? | `@JoinColumn(nullable = false)` |
| **unique** | Unique constraint | `@JoinColumn(unique = true)` |
| **referencedColumnName** | Referenced column | `@JoinColumn(referencedColumnName = "uuid")` |

---

## @JoinTable

**Matlab:** Many-to-Many relationship ke liye junction table define karta hai.

```java
@ManyToMany
@JoinTable(
    name = "user_role",                    // Junction table name
    joinColumns = @JoinColumn(name = "user_id"),         // This entity's FK
    inverseJoinColumns = @JoinColumn(name = "role_id")   // Other entity's FK
)
private List<Role> roles = new ArrayList<>();
```

---

## mappedBy (Owning Side)

**Matlab:** Bidirectional relationships mein — kaunsi side foreign key manage karti hai woh decide karta hai.

### Owning Side vs Inverse Side

| Side | Responsibility | Annotation |
|------|----------------|------------|
| **Owning Side** | Foreign key manage karta hai | `@JoinColumn` use karta hai |
| **Inverse Side** | Sirf reference karta hai | `mappedBy` use karta hai |

### Example

```java
// Owning Side — Foreign key yahan hai (Post mein user_id)
@Entity
public class Post {

    @ManyToOne
    @JoinColumn(name = "user_id")  // FK yahan hai — owning side
    private User user;
}

// Inverse Side — Sirf reference karta hai
@Entity
public class User {

    @OneToMany(mappedBy = "user")  // "user" = Post class ka field name
    private List<Post> posts;
}
```

### mappedBy Rules

```
mappedBy ka value = Owning side ka FIELD NAME (not column name)

@OneToMany(mappedBy = "user")   ← "user" hai Post class ka field name
@OneToOne(mappedBy = "profile") ← "profile" hai User class ka field name
@ManyToMany(mappedBy = "roles") ← "roles" hai Student class ka field name
```

### ⚠️ Common Mistake

```java
// ❌ Galat — mappedBy mein column name de diya
@OneToMany(mappedBy = "user_id")  // Error!

// ✅ Sahi — mappedBy mein field name do
@OneToMany(mappedBy = "user")  // Correct!
```

---

## Cascade Types

**Matlab:** Parent entity pe operation perform karne pe child entities pe bhi same operation automatically apply hota hai.

### Cascade Types

| Cascade Type | persist | merge | remove | refresh | detach |
|--------------|---------|-------|--------|---------|--------|
| **ALL** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **PERSIST** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **MERGE** | ❌ | ✅ | ❌ | ❌ | ❌ |
| **REMOVE** | ❌ | ❌ | ✅ | ❌ | ❌ |
| **REFRESH** | ❌ | ❌ | ❌ | ✅ | ❌ |
| **DETACH** | ❌ | ❌ | ❌ | ❌ | ✅ |

### CascadeType.ALL (Most Common)

```java
@Entity
public class User {

    @Id
    private Long id;

    // Parent save karo → Posts automatically save honge
    // Parent delete karo → Posts automatically delete honge
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Post> posts = new ArrayList<>();
}
```

### Example Scenarios

```java
// Scenario 1: Cascade PERSIST
@OneToMany(cascade = CascadeType.PERSIST)
private List<Post> posts;

User user = new User();
user.addPost(new Post("Title 1"));
user.addPost(new Post("Title 2"));

entityManager.persist(user);  // User + Posts dono INSERT honge
```

```java
// Scenario 2: Cascade REMOVE + orphanRemoval
@OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
private List<Post> posts;

User user = entityManager.find(User.class, 1L);
user.getPosts().remove(0);  // Post automatically DELETE ho jayega
// orphanRemoval = true: List se remove hua toh DB se bhi delete hoga
```

### CascadeType.MERGE

```java
@OneToMany(cascade = CascadeType.MERGE)
private List<Post> posts;

User user = entityManager.find(User.class, 1L);
user.setName("Updated");
user.getPosts().get(0).setTitle("Updated Title");

entityManager.merge(user);  // User + Posts dono UPDATE honge
```

### ⚠️ Cascade Warning

```java
// ❌ Avoid — Bidirectional cascade infinite loop mein ja sakta hai
@Entity
public class A {
    @OneToMany(cascade = CascadeType.ALL)
    private List<B> bs;
}

@Entity
public class B {
    @ManyToOne(cascade = CascadeType.ALL)  // Dono sides pe ALL mat lagao!
    private A a;
}

// ✅ Recommended — Sirf owning side pe cascade lagao
@Entity
public class A {
    @OneToMany(cascade = CascadeType.ALL)
    private List<B> bs;
}

@Entity
public class B {
    @ManyToOne  // No cascade
    private A a;
}
```

---

## FetchType (LAZY vs EAGER)

**Matlab:** Related entities kab load hongi — immediately (EAGER) ya jab actually access karein (LAZY).

### Defaults

| Relationship | Default Fetch | Recommended |
|--------------|---------------|-------------|
| **@OneToOne** | EAGER | LAZY |
| **@OneToMany** | LAZY | LAZY ✅ |
| **@ManyToOne** | EAGER | LAZY |
| **@ManyToMany** | LAZY | LAZY ✅ |

### LAZY (Recommended)

```java
@OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
private List<Post> posts;

// Usage:
User user = userRepository.findById(1L).orElse(null);
// Posts abhi load nahi hue — sirf User loaded

// Jab actually access karoge tab SQL fire hoga
List<Post> posts = user.getPosts();  // Yahan SELECT * FROM post WHERE user_id = 1
```

### EAGER (Avoid Usually)

```java
@OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
private List<Post> posts;

// Usage:
User user = userRepository.findById(1L).orElse(null);
// Posts immediately load ho gaye — extra SQL query
// Agar 100 users fetch kiye toh 100 extra queries (N+1 problem!)
```

### Lazy Loading Outside Transaction

```java
// ❌ Error: LazyInitializationException
User user = userRepository.findById(1L).orElse(null);
// Transaction close ho gaya
List<Post> posts = user.getPosts();  // ERROR! Session close ho chuka hai

// ✅ Fix 1: JOIN FETCH
@Query("SELECT u FROM User u LEFT JOIN FETCH u.posts WHERE u.id = :id")
User findByIdWithPosts(@Param("Long") id);

// ✅ Fix 2: @EntityGraph
@EntityGraph(attributePaths = {"posts"})
User findById(Long id);

// ✅ Fix 3: Open Session In View (development only!)
spring.jpa.open-in-view=true
```

### Lazy Initialization Check

```java
// Check if collection is initialized
if (Hibernate.isInitialized(user.getPosts())) {
    // Posts loaded hain
} else {
    // Posts lazy hain — abhi load nahi hui
}

// Force initialize
Hibernate.initialize(user.getPosts());
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **@OneToOne** | Ek entity → ek related entity |
| **@OneToMany** | Ek entity → multiple related entities |
| **@ManyToOne** | Multiple entities → ek related entity |
| **@ManyToMany** | Multiple ↔ Multiple (junction table banti hai) |
| **@JoinColumn** | Foreign key column name customize karo |
| **@JoinTable** | Many-to-Many junction table define karo |
| **mappedBy** | Inverse side — foreign key manage nahi karti |
| **CascadeType.ALL** | Parent operation children pe bhi apply hota hai |
| **orphanRemoval** | Collection se remove hua toh DB se bhi delete hoga |
| **FetchType.LAZY** | Jab actually access karo tab load hoga (recommended) |
| **FetchType.EAGER** | Immediately load hota hai (avoid karo) |
