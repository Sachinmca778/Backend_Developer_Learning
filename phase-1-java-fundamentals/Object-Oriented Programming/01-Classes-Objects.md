# Classes & Objects

## Status: Not Started

---

## Table of Contents

1. [Class Declaration](#class-declaration)
2. [Constructors](#constructors)
3. [Object Creation](#object-creation)
4. [Static vs Instance Members](#static-vs-instance-members)
5. [this Keyword](#this-keyword)
6. [finalize()](#finalize)
7. [record Classes (Java 16+)](#record-classes-java-16)

---

## Class Declaration

**Matlab:** Ek blueprint ya template jisme object ke properties (fields) aur behaviors (methods) define hote hain.

### Basic Class Structure

```java
// Class declaration
public class User {

    // Fields (instance variables)
    private String name;
    private int age;
    private String email;

    // Constructor
    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }

    // Methods (behaviors)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAdult() {
        return age >= 18;
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', age=" + age + ", email='" + email + "'}";
    }
}
```

### Class Components

| Component | Description | Example |
|-----------|-------------|---------|
| **Fields** | Object ki state/data | `String name;` |
| **Methods** | Object ka behavior | `void display() {}` |
| **Constructors** | Object initialize karne ke liye | `User() {}` |
| **Blocks** | Static/Instance initialization | `static {}` |

### Class Modifiers

```java
// public — kisi bhi package se access
public class User { }

// default — sirf same package se access (no modifier)
class InternalHelper { }

// final — extend nahi ho sakta
final class Constants { }

// abstract — instantiate nahi ho sakta, extend karna padega
abstract class Shape { }
```

---

## Constructors

**Matlab:** Special method jo object create hone pe automatically call hota hai — object initialize karne ke liye.

### Constructor Rules

```java
class User {
    // Constructor rules:
    // 1. Class ke same name ka hona chahiye
    // 2. Koi return type nahi (void bhi nahi)
    // 3. Object creation pe automatically call hota hai
}
```

### Default Constructor

```java
class User {
    String name;
    int age;

    // Default constructor (no parameters)
    public User() {
        System.out.println("Default constructor called");
    }
}

// Usage
User user = new User();  // "Default constructor called"
```

**Note:** Agar aap koi constructor nahi likhte toh Java automatically default constructor provide karti hai. Lekin agar aap koi bhi constructor likh dete ho toh default constructor automatically nahi banta.

### Parameterized Constructor

```java
class User {
    String name;
    int age;

    // Parameterized constructor
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}

// Usage
User user = new User("Sachin", 25);
```

### Copy Constructor

```java
class User {
    String name;
    int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Copy constructor — existing object se naya object banao
    public User(User other) {
        this.name = other.name;
        this.age = other.age;
    }
}

// Usage
User original = new User("Sachin", 25);
User copy = new User(original);  // Copy constructor call
```

### Constructor Chaining (this())

```java
class User {
    String name;
    int age;
    String email;

    // Default constructor
    public User() {
        this("Unknown");  // Ek parameter wale constructor ko call karo
    }

    // One parameter
    public User(String name) {
        this(name, 0);  // Do parameter wale constructor ko call karo
    }

    // Two parameters
    public User(String name, int age) {
        this(name, age, "unknown@mail.com");  // Three parameters ko call karo
    }

    // Three parameters (main constructor)
    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }
}

// Usage
User u1 = new User();                        // "Unknown", 0, unknown@mail.com
User u2 = new User("Sachin");                // "Sachin", 0, unknown@mail.com
User u3 = new User("Sachin", 25);            // "Sachin", 25, unknown@mail.com
User u4 = new User("Sachin", 25, "s@mail.com");  // All values set
```

**Rule:** `this()` hamesha constructor ki **pehli line** pe hona chahiye.

```java
public User() {
    this("Unknown");  // ✅ First line pe hona chahiye
    System.out.println("Done");
}

public User() {
    System.out.println("Start");
    this("Unknown");  // ❌ Error! this() must be first statement
}
```

---

## Object Creation

**Matlab:** Class ka instance (object) banana — `new` keyword use hota hai.

### Using new Keyword

```java
// Object creation
User user = new User("Sachin", 25);

// Breakdown:
// User user    → Reference variable declare kiya
// new User()   → Heap memory mein naya object bana
// =            → Reference ko object se link kiya
```

### Multiple Objects

```java
User user1 = new User("Sachin", 25);
User user2 = new User("Rahul", 30);
User user3 = new User("Priya", 28);

// Sab alag objects hain — alag memory mein
System.out.println(user1 == user2);  // false (different objects)
```

### Object Reference Copy

```java
User user1 = new User("Sachin", 25);
User user2 = user1;  // Reference copy — dono same object ko point kar rahe hain

user2.setName("Rahul");
System.out.println(user1.getName());  // "Rahul" (same object hai!)
```

### Anonymous Object

```java
// Object bina reference ke
new User("Sachin", 25).display();  // Ek baar use kiya, phir garbage

// Method argument mein
processUser(new User("Sachin", 25));
```

---

## Static vs Instance Members

**Matlab:** `static` keyword se class-level members bante hain — bina object ke access ho sakte hain.

### Instance Members (Object ke saath)

```java
class User {
    // Instance fields — har object ka alag value
    String name;
    int age;

    // Instance methods — object pe operate karte hain
    public void display() {
        System.out.println(name + " - " + age);
    }
}

// Usage — object chahiye
User user = new User();
user.name = "Sachin";
user.display();
```

### Static Members (Class ke saath)

```java
class User {
    // Static field — sab objects ke liye same
    static int count = 0;

    // Instance field
    String name;

    public User(String name) {
        this.name = name;
        count++;  // Har object creation pe count badhega
    }

    // Static method — bina object ke call kar sakte ho
    public static int getCount() {
        return count;
    }
}

// Usage
User u1 = new User("Sachin");
User u2 = new User("Rahul");
User u3 = new User("Priya");

System.out.println(User.getCount());  // 3 (static method — class name se call)
```

### Static vs Instance Comparison

| Feature | Instance Member | Static Member |
|---------|----------------|---------------|
| **Belongs to** | Object | Class |
| **Access** | `object.member` | `ClassName.member` |
| **Memory** | Har object ke liye alag | Sirf ek copy (shared) |
| **Initialization** | Object creation pe | Class load hone pe |
| **Example** | `user.name` | `User.count` |

### Static Block

```java
class DatabaseConfig {
    static String url;
    static String driver;

    // Static block — class load hone pe ek baar run hota hai
    static {
        url = "jdbc:mysql://localhost:3306/mydb";
        driver = "com.mysql.cj.jdbc.Driver";
        System.out.println("Static block executed");
    }
}

// Usage
System.out.println(DatabaseConfig.url);  // Static block pehle run hoga
```

### When to Use Static

```java
// ✅ Use static for:
// - Utility methods (Math.random(), Collections.sort())
// - Constants (public static final)
// - Factory methods
// - Singleton pattern

// ❌ Don't use static for:
// - Object-specific data (use instance fields)
// - Methods that need object state
```

---

## this Keyword

**Matlab:** Current object ka reference — instance fields aur methods ko refer karta hai.

### this for Field vs Parameter

```java
class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;  // this.name = instance field, name = parameter
        this.age = age;    // this.age = instance field, age = parameter
    }
}
```

### this for Constructor Chaining

```java
class User {
    private String name;
    private int age;

    public User() {
        this("Unknown");  // Constructor call
    }

    public User(String name) {
        this(name, 0);  // Constructor call
    }

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

### this for Method Call

```java
class User {
    public void display() {
        System.out.println("Display called");
    }

    public void show() {
        this.display();  // Current object ka method call
        display();       // Same thing — this optional hai
    }
}
```

### this as Method Argument

```java
class User {
    public void register() {
        UserService.register(this);  // Current object ko pass karo
    }
}
```

### this for Return

```java
class User {
    private String name;

    public User setName(String name) {
        this.name = name;
        return this;  // Current object return — method chaining
    }

    public User setAge(int age) {
        this.age = age;
        return this;
    }
}

// Usage — method chaining
User user = new User()
    .setName("Sachin")
    .setAge(25);
```

---

## finalize()

**Matlab:** Object garbage collect hone se pehle cleanup kaam karne ke liye — **deprecated since Java 9**.

### Usage (Deprecated)

```java
class DatabaseConnection {
    private Connection connection;

    @Override
    protected void finalize() throws Throwable {
        try {
            if (connection != null) {
                connection.close();  // Cleanup
            }
        } finally {
            super.finalize();
        }
    }
}
```

### Why Deprecated?

```
1. Execution time uncertain — kab call hoga pata nahi
2. Performance impact — GC slow ho jata hai
3. Not guaranteed — ho sakta hai call hi na ho
4. Exception handling muskil
```

### Modern Alternatives

```java
// ✅ AutoCloseable + try-with-resources
class DatabaseConnection implements AutoCloseable {
    private Connection connection;

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

// Usage — automatically close hoga
try (DatabaseConnection db = new DatabaseConnection()) {
    // Use database
} // db.close() automatically called
```

---

## record Classes (Java 16+)

**Matlab:** Immutable data classes ke liye shorthand — boilerplate code kam karta hai.

### Basic record

```java
// Traditional class — boilerplate heavy
class User {
    private final String name;
    private final int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String name() { return name; }
    public int age() { return age; }

    @Override
    public boolean equals(Object o) { /* ... */ }

    @Override
    public int hashCode() { /* ... */ }

    @Override
    public String toString() { /* ... */ }
}

// record — same kaam, kam code
record User(String name, int age) { }
```

### What record Automatically Provides

```java
record User(String name, int age) {
    // Automatically generated:
    // 1. Constructor: public User(String name, int age)
    // 2. Accessors: public String name(), public int age()
    // 3. equals() — content-based comparison
    // 4. hashCode() — content-based hash
    // 5. toString() — "User[name=Sachin, age=25]"
}
```

### record Usage

```java
User user = new User("Sachin", 25);

// Accessors (getters nahi, accessor methods)
System.out.println(user.name());  // "Sachin"
System.out.println(user.age());   // 25

// equals — content compare
User user2 = new User("Sachin", 25);
System.out.println(user.equals(user2));  // true

// toString
System.out.println(user);  // User[name=Sachin, age=25]

// hashCode — same content = same hash
System.out.println(user.hashCode() == user2.hashCode());  // true
```

### Custom record Constructor

```java
record User(String name, int age) {
    // Compact constructor — validation
    public User {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }

    // Custom method
    public boolean isAdult() {
        return age >= 18;
    }

    // Static factory method
    public static User anonymous() {
        return new User("Unknown", 0);
    }
}
```

### record vs class

| Feature | record | class |
|---------|--------|-------|
| **Mutability** | Immutable (final fields) | Mutable ya immutable |
| **Inheritance** | Extend nahi kar sakta | Extend kar sakta hai |
| **Constructors** | Compact constructor possible | Full constructors |
| **Fields** | Automatically generated | Manual declare |
| **Methods** | Accessors auto-generated | Manual getters/setters |
| **equals/hashCode** | Auto-generated (content-based) | Manual override karna padta hai |
| **Use Case** | Data carriers, DTOs | Full OOP classes |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Class** | Blueprint — fields (state) + methods (behavior) |
| **Constructor** | Object initialize — default, parameterized, copy |
| **Constructor Chaining** | `this()` se dusre constructor ko call karo — first line pe hona chahiye |
| **Object Creation** | `new` keyword — heap memory mein object banta hai |
| **Instance Members** | Object ke saath — har object ka alag copy |
| **Static Members** | Class ke saath — sab objects share karte hain |
| **this** | Current object reference — fields vs parameters, constructor chaining |
| **finalize()** | Deprecated (Java 9+) — `AutoCloseable` use karo |
| **record** | Java 16+ — immutable data classes, boilerplate-free |
