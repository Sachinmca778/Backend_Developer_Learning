# Optional&lt;T&gt;

## Status: Not Started

---

## Table of Contents

1. [Creating Optional](#creating-optional)
2. [Checking Value](#checking-value)
3. [Getting Value](#getting-value)
4. [orElse / orElseGet / orElseThrow](#orelse--orellege--orelsethrow)
5. [map / flatMap / filter](#map--flatmap--filter)
6. [Optional Best Practices](#optional-best-practices)

---

## Creating Optional

**Matlab:** Null-safe wrapper — `null` hone pe exception nahi, controlled handling.

### Factory Methods

```java
// Optional.of — non-null value (null pe NullPointerException)
Optional<String> opt1 = Optional.of("Hello");
// Optional.of(null);  // ❌ NullPointerException

// Optional.ofNullable — null ya non-null dono
Optional<String> opt2 = Optional.ofNullable("Hello");
Optional<String> opt3 = Optional.ofNullable(null);  // ✅ Empty Optional

// Optional.empty — explicitly empty
Optional<String> empty = Optional.empty();
```

### Real-World Usage

```java
// Repository method
public Optional<User> findById(Long id) {
    User user = database.get(id);
    return Optional.ofNullable(user);  // null ho sakta hai
}

// Usage
Optional<User> userOpt = userRepository.findById(1L);
```

---

## Checking Value

```java
Optional<String> present = Optional.of("Hello");
Optional<String> absent = Optional.empty();

// isPresent — value hai ya nahi
System.out.println(present.isPresent());  // true
System.out.println(absent.isPresent());   // false

// isEmpty — Java 11+, value nahi hai
System.out.println(present.isEmpty());  // false
System.out.println(absent.isEmpty());   // true
```

### Old Pattern (Avoid)

```java
// ❌ Anti-pattern — null check jaisa hi hai
Optional<User> opt = userRepository.findById(1L);
if (opt.isPresent()) {
    User user = opt.get();
    System.out.println(user.getName());
}

// ✅ Better — ifPresent use karo
userRepository.findById(1L)
    .ifPresent(user -> System.out.println(user.getName()));
```

---

## Getting Value

```java
Optional<String> present = Optional.of("Hello");
Optional<String> absent = Optional.empty();

// get — value return karo (empty pe NoSuchElementException)
System.out.println(present.get());  // "Hello"
// absent.get();  // ❌ NoSuchElementException
```

---

## orElse / orElseGet / orElseThrow

### orElse

```java
Optional<String> present = Optional.of("Hello");
Optional<String> absent = Optional.empty();

// orElse — default value (hamesha evaluate hota hai)
String val1 = present.orElse("Default");  // "Hello"
String val2 = absent.orElse("Default");   // "Default"

// ⚠️ Default value hamesha create hoga
String val3 = absent.orElse(expensiveOperation());  // expensiveOperation() call hoga
```

### orElseGet

```java
// orElseGet — default value lazy evaluation (sirf jab Optional empty ho)
String val1 = present.orElseGet(() -> "Default");  // "Hello" (lambda call nahi hoga)
String val2 = absent.orElseGet(() -> "Default");   // "Default" (lambda call hoga)

// ✅ Better performance — expensive operation sirf jab chahiye
String val3 = absent.orElseGet(() -> expensiveOperation());  // Sirf empty pe call hoga
```

### orElseThrow

```java
// orElseThrow — empty pe exception throw karo (Java 10+)
User user = userOpt.orElseThrow();  // NoSuchElementException

// Custom exception
User user2 = userOpt.orElseThrow(() -> 
    new ResourceNotFoundException("User not found")
);

// Old style (Java 8)
User user3 = userOpt.orElseThrow(ResourceNotFoundException::new);
```

---

## map / flatMap / filter

### map

```java
Optional<User> userOpt = userRepository.findById(1L);

// User → Name
Optional<String> nameOpt = userOpt.map(User::getName);
// Optional["Sachin"] ya Optional.empty()

// Chaining
Optional<String> upperNameOpt = userOpt
    .map(User::getName)
    .map(String::toUpperCase);
// Optional["SACHIN"]
```

### flatMap

```java
// map returns Optional<Optional<T>> — flatMap chahiye
class Address {
    String city;
    Optional<String> getCity() { return Optional.ofNullable(city); }
}

class User {
    Optional<Address> getAddress() { ... }
}

// ❌ map → Optional<Optional<String>>
Optional<Optional<String>> cityOpt1 = userOpt.map(User::getAddress)
    .map(Address::getCity);

// ✅ flatMap → Optional<String>
Optional<String> cityOpt2 = userOpt.flatMap(User::getAddress)
    .flatMap(Address::getCity);
```

### filter

```java
Optional<User> userOpt = userRepository.findById(1L);

// Filter by condition
Optional<User> adultOpt = userOpt.filter(u -> u.getAge() >= 18);

// Chain with map
Optional<String> adultNameOpt = userOpt
    .filter(u -> u.getAge() >= 18)
    .map(User::getName);
```

---

## Optional Best Practices

### ✅ Do

```java
// Return type mein Optional use karo
public Optional<User> findById(Long id) { ... }

// ifPresent use karo
userOpt.ifPresent(user -> process(user));

// ifPresentOrElse (Java 9+)
userOpt.ifPresentOrElse(
    user -> process(user),
    () -> System.out.println("User not found")
);

// Chain operations
String result = userOpt
    .map(User::getName)
    .filter(n -> n.length() > 3)
    .orElse("Unknown");

// Stream se Optional
Optional<User> firstAdult = users.stream()
    .filter(u -> u.getAge() >= 18)
    .findFirst();
```

### ❌ Don't

```java
// ❌ Optional ko field mein mat use karo
class User {
    private Optional<String> name;  // ❌
}

// ✅ Field mein null use karo, getter mein Optional return karo
class User {
    private String name;
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }
}

// ❌ Optional ko method parameter mein mat use karo
public void process(Optional<User> user) { }  // ❌

// ✅ Overloaded methods use karo
public void process(User user) { }
public void process() { }  // No user case

// ❌ isPresent().get() pattern avoid karo
if (opt.isPresent()) {
    doSomething(opt.get());
}

// ✅ ifPresent use karo
opt.ifPresent(this::doSomething);
```

---

## Summary

| Method | Purpose | When to Use |
|--------|---------|-------------|
| **of(value)** | Non-null Optional | Value definitely present |
| **ofNullable(value)** | Null-safe Optional | Value null ho sakta hai |
| **empty()** | Empty Optional | No value |
| **isPresent()** | Value check | Avoid if possible |
| **ifPresent(action)** | Value pe action | Preferred over isPresent |
| **get()** | Value extract | Avoid — orElse use karo |
| **orElse(default)** | Default value | Simple default |
| **orElseGet(supplier)** | Lazy default | Expensive default |
| **orElseThrow()** | Exception on empty | Required value |
| **map(fn)** | Transform | One-to-one mapping |
| **flatMap(fn)** | Flatten Optional | Nested Optional |
| **filter(predicate)** | Condition check | Conditional presence |
