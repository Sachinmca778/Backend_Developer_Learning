# Encapsulation

## Status: Not Started

---

## Table of Contents

1. [Access Modifiers](#access-modifiers)
2. [Getter/Setter Pattern](#gettersetter-pattern)
3. [JavaBeans Convention](#javabeans-convention)
4. [record Classes (Java 16+)](#record-classes-java-16)

---

## Access Modifiers

**Matlab:** Classes, fields, methods, aur constructors ki visibility control karna — kaun access kar sakta hai aur kaun nahi.

### Four Access Levels

| Modifier | Same Class | Same Package | Subclass (Different Package) | Different Package |
|----------|-----------|--------------|------------------------------|-------------------|
| **private** | ✅ | ❌ | ❌ | ❌ |
| **default** (no modifier) | ✅ | ✅ | ❌ | ❌ |
| **protected** | ✅ | ✅ | ✅ | ❌ |
| **public** | ✅ | ✅ | ✅ | ✅ |

### private

```java
class User {
    // Sirf is class ke andar access ho sakta hai
    private String password;
    private String ssn;  // Social Security Number

    private void validatePassword(String pwd) {
        // Internal logic — bahar se access nahi hona chahiye
    }
}

// ❌ Bahar se access nahi ho sakta
User user = new User();
user.password = "123";  // ❌ Error: password has private access
```

### default (Package-Private)

```java
// package com.example.app;

class UserService {
    // Same package mein access ho sakta hai
    String internalMethod() {
        return "Internal use only";
    }

    void process() {
        // Same package ka code access kar sakta hai
    }
}

// Same package mein — ✅ access ho sakta
class UserController {
    void handle() {
        UserService service = new UserService();
        service.process();  // ✅ Same package
    }
}

// Different package mein — ❌ access nahi ho sakta
// package com.example.api;
class ApiController {
    void handle() {
        UserService service = new UserService();
        service.process();  // ❌ Error: not visible
    }
}
```

### protected

```java
// package com.example.base;
public class Animal {
    protected String name;

    protected void eat() {
        System.out.println(name + " is eating");
    }
}

// Same package — ✅ access
class Zoo {
    void feed(Animal animal) {
        animal.eat();  // ✅ Same package
    }
}

// Different package + subclass — ✅ access
// package com.example.farm;
import com.example.base.Animal;

class Dog extends Animal {
    public void bark() {
        System.out.println(name + " is barking");  // ✅ Inherited protected field
        eat();  // ✅ Inherited protected method
    }
}

// Different package, NOT subclass — ❌ access nahi
class Farm {
    void check(Animal animal) {
        animal.eat();  // ❌ Error: protected access
    }
}
```

### public

```java
public class User {
    public String name;

    public void display() {
        System.out.println("User: " + name);
    }
}

// Kahin se bhi access ho sakta hai
User user = new User();
user.name = "Sachin";  // ✅
user.display();        // ✅
```

### Access Modifier Best Practices

```java
// ✅ Good — fields private, methods public/protected
public class User {
    // Fields — hamesha private rakho
    private String name;
    private int age;
    private String email;

    // Constructor — public
    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }

    // Getters/Setters — public
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    // Internal method — private
    private void validateEmail(String email) {
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }
}
```

---

## Getter/Setter Pattern

**Matlab:** Private fields ko access karne ka controlled tarika — validation aur encapsulation ke liye.

### Basic Getters/Setters

```java
public class User {
    private String name;
    private int age;

    // Getter — field read karo
    public String getName() {
        return name;
    }

    // Setter — field set karo (validation ke saath)
    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Invalid age: " + age);
        }
        this.age = age;
    }
}

// Usage
User user = new User();
user.setName("Sachin");   // Validation ke saath set
user.setAge(25);          // Validation ke saath set
System.out.println(user.getName());  // Get
System.out.println(user.getAge());   // Get
```

### Read-Only Property

```java
public class User {
    private final String id;  // final — sirf constructor mein set hoga
    private String name;

    public User(String id) {
        this.id = id;
    }

    // Getter only — no setter (immutable)
    public String getId() {
        return id;
    }

    // Getter + Setter — mutable
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
```

### Write-Only Property

```java
public class PasswordManager {
    private String password;

    // No getter — security ke liye password read nahi karna chahiye
    public void setPassword(String password) {
        this.password = hashPassword(password);
    }

    public boolean verifyPassword(String input) {
        return hashPassword(input).equals(this.password);
    }

    private String hashPassword(String pwd) {
        // Hashing logic
        return "hashed_" + pwd;
    }
}
```

### Computed Property (No Backing Field)

```java
public class Rectangle {
    private double width;
    private double height;

    // Getter — calculated value
    public double getArea() {
        return width * height;
    }

    public double getPerimeter() {
        return 2 * (width + height);
    }

    // Standard getters/setters
    public double getWidth() { return width; }
    public void setWidth(double width) { this.width = width; }
    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }
}

// Usage
Rectangle rect = new Rectangle();
rect.setWidth(5);
rect.setHeight(3);
System.out.println(rect.getArea());       // 15.0 (calculated)
System.out.println(rect.getPerimeter());  // 16.0 (calculated)
```

---

## JavaBeans Convention

**Matlab:** JavaBeans specification ke according classes banana — frameworks (Spring, Hibernate, Jackson) isi ko follow karte hain.

### JavaBeans Rules

```java
// 1. Public no-argument constructor
// 2. Private fields
// 3. Public getters/setters
// 4. Implements Serializable (optional but recommended)

public class User implements java.io.Serializable {

    // Private fields
    private String name;
    private int age;
    private boolean active;

    // No-argument constructor
    public User() {
    }

    // Parameterized constructor
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Getters: get<PropertyName>() or is<PropertyName>() for boolean
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public boolean isActive() {  // is<PropertyName> for boolean
        return active;
    }

    // Setters: set<PropertyName>(Type value)
    public void setName(String name) {
        this.name = name;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
```

### Naming Conventions

```java
// Property: name → Getter: getName(), Setter: setName()
private String name;
public String getName() { return name; }
public void setName(String name) { this.name = name; }

// Property: age → Getter: getAge(), Setter: setAge()
private int age;
public int getAge() { return age; }
public void setAge(int age) { this.age = age; }

// Property: active → Getter: isActive(), Setter: setActive()
private boolean active;
public boolean isActive() { return active; }
public void setActive(boolean active) { this.active = active; }

// Property: URL → Getter: getURL(), Setter: setURL()
private String URL;
public String getURL() { return URL; }
public void setURL(String URL) { this.URL = URL; }
```

### Why JavaBeans Matter?

```java
// Jackson (JSON serialization) — getter/setters use karta hai
User user = new User();
user.setName("Sachin");
user.setAge(25);

// Jackson internally calls:
String json = objectMapper.writeValueAsString(user);
// {"name":"Sachin","age":25,"active":false}

// Spring MVC — form binding
@PostMapping("/users")
public String createUser(@ModelAttribute User user) {
    // Spring internally: user.setName(request.getParameter("name"))
}

// Hibernate/JPA — entity mapping
@Entity
public class User {
    @Id
    private Long id;

    public Long getId() { return id; }  // JPA uses this
    public void setId(Long id) { this.id = id; }
}
```

---

## record Classes (Java 16+)

**Matlab:** Immutable data classes ke liye shorthand — encapsulation built-in hai.

### Basic record

```java
// Traditional JavaBean — boilerplate heavy
public class User implements Serializable {
    private final String name;
    private final int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String name() { return name; }
    public int age() { return age; }

    // equals, hashCode, toString — manually override
}

// record — same kaam, ek line mein
public record User(String name, int age) implements Serializable { }
```

### record Features

```java
public record User(String name, int age) {
    // Accessors (getters nahi)
    // name() → returns name
    // age() → returns age

    // equals() — content-based comparison
    // hashCode() — content-based hash
    // toString() → "User[name=Sachin, age=25]"

    // Compact constructor — validation
    public User {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }

    // Custom methods
    public boolean isAdult() {
        return age >= 18;
    }

    // Static factory method
    public static User anonymous() {
        return new User("Unknown", 0);
    }
}

// Usage
User user = new User("Sachin", 25);
System.out.println(user.name());     // "Sachin" (not getName())
System.out.println(user.age());      // 25
System.out.println(user.isAdult());  // true
System.out.println(user);            // User[name=Sachin, age=25]
```

### record vs JavaBean

| Feature | JavaBean | record |
|---------|----------|--------|
| **Mutability** | Mutable (setters) | Immutable (final fields) |
| **Getters** | `getName()` | `name()` |
| **Setters** | `setName(value)` | None |
| **equals/hashCode** | Manual override | Auto-generated |
| **toString** | Manual override | Auto-generated |
| **Constructors** | Full flexibility | Compact constructor |
| **Inheritance** | Can extend | Cannot extend |
| **Use Case** | Mutable entities, forms | DTOs, data carriers |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **private** | Sirf same class mein access |
| **default** | Same package mein access |
| **protected** | Same package + subclasses (different package) |
| **public** | Kahin se bhi access |
| **Encapsulation** | Fields private, access via getters/setters |
| **Getter/Setter** | Controlled access — validation possible |
| **JavaBeans** | No-arg constructor, private fields, public getters/setters |
| **record** | Java 16+ — immutable data classes, minimal boilerplate |
