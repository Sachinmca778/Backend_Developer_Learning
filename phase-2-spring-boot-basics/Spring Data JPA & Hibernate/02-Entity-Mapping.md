# Entity Mapping

## Status: Not Started

---

## Table of Contents

1. [@Entity](#entity)
2. [@Table](#table)
3. [@Id](#id)
4. [@GeneratedValue](#generatedvalue)
5. [@Column](#column)
6. [@Transient](#transient)
7. [@Enumerated](#enumerated)
8. [@Embedded / @Embeddable](#embedded--embeddable)
9. [@Lob](#lob)

---

## @Entity

**Matlab:** Ek Java class ko database table se map karta hai. Har `@Entity` class ka ek corresponding table hota hai database mein.

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
    private String email;
    private LocalDateTime createdAt;

    // Default constructor zaruri hai (JPA requirement)
    public User() {}

    // Getters, Setters
}
```

**Result:**
```sql
CREATE TABLE user (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    email VARCHAR(255),
    created_at TIMESTAMP,
    PRIMARY KEY (id)
);
```

### Important Rules

| Rule | Description |
|------|-------------|
| **Default constructor** | Hamesha chahiye (public ya protected) |
| **Not final class** | Hibernate proxy banata hai — final class pe nahi ban sakta |
| **Not final methods** | `public/protected` methods final nahi hone chahiye |
| **Serializable (optional)** | Distributed apps mein useful hai |

### @Entity with Custom Name

```java
@Entity(name = "AppUser")  // JPQL mein "AppUser" use hoga
public class User {
    // ...
}
```

---

## @Table

**Matlab:** Entity ka database table name customize karta hai. Default mein class name hi table name hota hai.

```java
@Entity
@Table(name = "app_users")  // Table name customize kiya
public class User {

    @Id
    private Long id;

    private String name;
}
```

**Result:**
```sql
CREATE TABLE app_users (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    PRIMARY KEY (id)
);
```

### @Table Attributes

```java
@Entity
@Table(
    name = "app_users",                        // Table name
    schema = "public",                         // Schema (PostgreSQL)
    catalog = "mydb",                          // Catalog (MySQL)
    uniqueConstraints = {                      // Unique constraints
        @UniqueConstraint(columnNames = {"email"}),
        @UniqueConstraint(columnNames = {"username"})
    },
    indexes = {                                // Indexes
        @Index(name = "idx_name", columnList = "name"),
        @Index(name = "idx_email", columnList = "email", unique = true)
    }
)
public class User {

    @Id
    private Long id;

    private String username;
    private String email;
    private String name;
}
```

**Result:**
```sql
CREATE TABLE app_users (
    id BIGINT NOT NULL,
    username VARCHAR(255),
    email VARCHAR(255),
    name VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT UK_email UNIQUE (email),
    CONSTRAINT UK_username UNIQUE (username)
);

CREATE INDEX idx_name ON app_users (name);
CREATE UNIQUE INDEX idx_email ON app_users (email);
```

---

## @Id

**Matlab:** Entity ki primary key define karta hai. Har entity mein exactly ek `@Id` field hona chahiye.

```java
@Entity
public class User {

    @Id
    private Long id;  // Primary key

    private String name;
}
```

### Composite Primary Key

Jab multiple columns milke primary key banate hain.

```java
// Embeddable primary key class
@Embeddable
public class OrderItemId implements Serializable {

    private Long orderId;
    private Long productId;

    // hashCode() aur equals() override karna zaruri hai
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItemId that = (OrderItemId) o;
        return Objects.equals(orderId, that.orderId) &&
               Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, productId);
    }
}

// Entity with composite key
@Entity
public class OrderItem {

    @EmbeddedId
    private OrderItemId id;

    private int quantity;
    private BigDecimal price;
}
```

---

## @GeneratedValue

**Matlab:** Primary key auto-generate karne ki strategy define karta hai.

### 4 Strategies

```java
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // MySQL, PostgreSQL
    private Long id;
}
```

| Strategy | Description | Best For |
|----------|-------------|----------|
| **IDENTITY** | Database auto-increment | MySQL, PostgreSQL, SQL Server |
| **SEQUENCE** | Database sequence use karta hai | PostgreSQL, Oracle |
| **AUTO** | Hibernate khud decide karta hai (default) | Cross-database apps |
| **TABLE** | Table se next value leta hai (rarely used) | Legacy databases |

### IDENTITY (Most Common)

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```

```sql
CREATE TABLE user (
    id BIGINT AUTO_INCREMENT,  -- MySQL
    -- ya
    id BIGINT GENERATED ALWAYS AS IDENTITY,  -- PostgreSQL
    ...
);
```

### SEQUENCE

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(
    name = "user_seq",
    sequenceName = "user_id_seq",
    allocationSize = 1  // Default: 50
)
private Long id;
```

```sql
-- PostgreSQL / Oracle
CREATE SEQUENCE user_id_seq START WITH 1 INCREMENT BY 1;
```

### AUTO (Default)

```java
@Id
@GeneratedValue(strategy = GenerationType.AUTO)  // Default strategy
private Long id;
```

Hibernate automatically database ke hisaab se strategy choose karta hai:
- MySQL → IDENTITY
- PostgreSQL → SEQUENCE
- H2 → IDENTITY

### TABLE (Legacy)

```java
@Id
@GeneratedValue(strategy = GenerationType.TABLE, generator = "user_gen")
@TableGenerator(
    name = "user_gen",
    table = "id_generator",
    pkColumnName = "entity_name",
    valueColumnName = "next_val",
    pkColumnValue = "user",
    allocationSize = 1
)
private Long id;
```

---

## @Column

**Matlab:** Entity field ko database column se map karta hai. Customizations ke liye use hota hai.

```java
@Entity
public class User {

    @Id
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(precision = 10, scale = 2)
    private BigDecimal salary;
}
```

**Result:**
```sql
CREATE TABLE user (
    id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    salary DECIMAL(10, 2),
    PRIMARY KEY (id)
);
```

### @Column Attributes

| Attribute | Description | Example |
|-----------|-------------|---------|
| **name** | Column name | `@Column(name = "full_name")` |
| **nullable** | NULL allowed? | `@Column(nullable = false)` |
| **unique** | Unique constraint | `@Column(unique = true)` |
| **length** | String column length | `@Column(length = 100)` |
| **precision** | Decimal precision | `@Column(precision = 10)` |
| **scale** | Decimal places | `@Column(scale = 2)` |
| **insertable** | INSERT mein include? | `@Column(insertable = false)` |
| **updatable** | UPDATE mein include? | `@Column(updatable = false)` |

### Field-Level @Column

```java
@Entity
public class User {

    @Id
    private Long id;

    // No @Column — default mapping (field name = column name)
    private String name;  // Column: "name"

    // @Column with snake_case
    @Column(name = "created_at")
    private LocalDateTime createdAt;  // Column: "created_at"

    // Insert only — update nahi hoga
    @Column(name = "created_at", insertable = true, updatable = false)
    private LocalDateTime createdAt;

    // Read only — insert mein nahi aayega
    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;
}
```

---

## @Transient

**Matlab:** Field ko database mein persist nahi karna — Hibernate isko ignore karega.

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;
    private String password;

    // Yeh field DB mein nahi jayegi
    @Transient
    private String confirmPassword;

    // Computed field — DB mein nahi chahiye
    @Transient
    private int age;

    @Transient
    private String fullName;  // Getter se compute hoga
}
```

**Alternative:** `transient` keyword bhi same kaam karta hai.

```java
private transient String confirmPassword;  // Same as @Transient
```

---

## @Enumerated

**Matlab:** Java enum ko database mein kaise store karna hai.

### Two Strategies

| Strategy | DB Type | Example | Recommended? |
|----------|---------|---------|--------------|
| **STRING** | VARCHAR | `"ADMIN"`, `"USER"` | ✅ Yes (readable) |
| **ORDINAL** | INTEGER | `0`, `1` | ❌ No (order change pe problem) |

### STRING (Recommended)

```java
public enum UserRole {
    ADMIN, USER, MODERATOR
}

@Entity
public class User {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;
}
```

**DB Value:**
```
id | role
1  | "ADMIN"
2  | "USER"
```

### ORDINAL (Avoid)

```java
@Enumerated(EnumType.ORDINAL)
private UserRole role;
```

**DB Value:**
```
id | role
1  | 0  (ADMIN)
2  | 1  (USER)
```

**Problem:** Agar enum order change hua toh sab data corrupt ho jayega.

---

## @Embedded / @Embeddable

**Matlab:** Multiple fields ko ek logical group mein combine karna — separate entity nahi, lekin reusable hai.

### @Embeddable

```java
@Embeddable
public class Address {

    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Default constructor zaruri hai
    public Address() {}

    // Getters, Setters
}
```

### @Embedded

```java
@Entity
public class User {

    @Id
    private Long id;

    private String name;

    @Embedded
    private Address address;  // Address ke fields is table mein aayenge
}
```

**Result:**
```sql
CREATE TABLE user (
    id BIGINT NOT NULL,
    name VARCHAR(255),
    street VARCHAR(255),     -- Address ke fields yahan
    city VARCHAR(100),
    state VARCHAR(50),
    zip_code VARCHAR(20),
    country VARCHAR(50),
    PRIMARY KEY (id)
);
```

### Custom Column Names

```java
@Entity
public class User {

    @Id
    private Long id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "home_street")),
        @AttributeOverride(name = "city", column = @Column(name = "home_city"))
    })
    private Address homeAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "office_street")),
        @AttributeOverride(name = "city", column = @Column(name = "office_city"))
    })
    private Address officeAddress;
}
```

---

## @Lob

**Matlab:** Large Object (BLOB / CLOB) store karne ke liye — images, documents, large text.

### CLOB (Character Large Object)

```java
@Entity
public class Article {

    @Id
    private Long id;

    private String title;

    @Lob  // TEXT / CLOB
    private String content;  // Large text content
}
```

**Result:**
```sql
CREATE TABLE article (
    id BIGINT NOT NULL,
    title VARCHAR(255),
    content TEXT,  -- Large text column
    PRIMARY KEY (id)
);
```

### BLOB (Binary Large Object)

```java
@Entity
public class Document {

    @Id
    private Long id;

    private String filename;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] data;  // Binary data (image, PDF, etc.)
}
```

**Result:**
```sql
CREATE TABLE document (
    id BIGINT NOT NULL,
    filename VARCHAR(255),
    data LONGBLOB,  -- Binary column
    PRIMARY KEY (id)
);
```

### ⚠️ Best Practice

Large data ko directly database mein store **avoid** karo. Better approach:

```java
@Entity
public class Document {

    @Id
    private Long id;

    private String filename;
    private String fileUrl;       // S3 / cloud storage URL
    private String storagePath;   // File system path

    // Binary data yahan mat rakho — external storage use karo
}
```

---

## Summary

| Annotation | Purpose | Example |
|------------|---------|---------|
| **@Entity** | Class ko table se map karo | `@Entity` |
| **@Table** | Table name customize karo | `@Table(name = "app_users")` |
| **@Id** | Primary key define karo | `@Id` |
| **@GeneratedValue** | Auto-generate strategy | `@GeneratedValue(strategy = IDENTITY)` |
| **@Column** | Column customize karo | `@Column(name = "full_name", nullable = false)` |
| **@Transient** | Field persist nahi karna | `@Transient` |
| **@Enumerated** | Enum store karna | `@Enumerated(EnumType.STRING)` |
| **@Embedded / @Embeddable** | Reusable field group | `@Embedded private Address address` |
| **@Lob** | Large data store karna | `@Lob private String content` |
