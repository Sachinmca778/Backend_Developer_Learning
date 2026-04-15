## Class Declaration

**Matlab:** Ek blueprint ya template jisme object ke properties (fields) aur behaviors (methods) define hote hain.

### Class Components

| Component | Description | Example |
|-----------|-------------|---------|
| **Fields** | Object ki state/data | `String name;` |
| **Methods** | Object ka behavior | `void display() {}` |
| **Constructors** | Object initialize karne ke liye | `User() {}` |
| **Blocks** | Static/Instance initialization | `static {}` |


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

**Note:** Agar aap koi constructor nahi likhte toh Java automatically default constructor provide karti hai. Lekin agar aap koi bhi constructor likh dete ho toh default constructor automatically nahi banta.

### Parameterized Constructor

### Copy Constructor — existing object se naya object banao

### Constructor Chaining (this())

**Rule:** `this()` hamesha constructor ki **pehli line** pe hona chahiye.

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

### Multiple Objects  // Sab alag objects hain — alag memory mein

### Object Reference Copy  // Reference copy — dono same object ko point kar rahe hain

### Anonymous Object  // Ek baar use kiya, phir garbage

## Static vs Instance Members

**Matlab:** `static` keyword se class-level members bante hain — bina object ke access ho sakte hain.


### Instance Members (Object ke saath)
// Instance fields — har object ka alag value
// Instance methods — object pe operate karte hain

### Static Members (Class ke saath)
    // Static field — sab objects ke liye same
    // Static method — bina object ke call kar sakte ho
System.out.println(User.getCount());  // 3 (static method — class name se call)

### Static vs Instance Comparison

| Feature | Instance Member | Static Member |
|---------|----------------|---------------|
| **Belongs to** | Object | Class |
| **Access** | `object.member` | `ClassName.member` |
| **Memory** | Har object ke liye alag | Sirf ek copy (shared) |
| **Initialization** | Object creation pe | Class load hone pe |
| **Example** | `user.name` | `User.count` |

### Static Block
    // Static block — class load hone pe ek baar run hota hai

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

## this Keyword

**Matlab:** Current object ka reference — instance fields aur methods ko refer karta hai.

## finalize()

**Matlab:** Object garbage collect hone se pehle cleanup kaam karne ke liye — **deprecated since Java 9**.

### Why Deprecated?

```
1. Execution time uncertain — kab call hoga pata nahi
2. Performance impact — GC slow ho jata hai
3. Not guaranteed — ho sakta hai call hi na ho
4. Exception handling muskil
```