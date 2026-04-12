# Default & Static Interface Methods

## Status: Not Started

---

## Table of Contents

1. [Default Methods](#default-methods)
2. [Default Method Conflicts](#default-method-conflicts)
3. [Static Methods in Interfaces](#static-methods-in-interfaces)
4. [Java 9+ Private Interface Methods](#java-9-private-interface-methods)

---

## Default Methods

**Matlab:** Interface mein implementation ke saath method — backward compatibility ke liye Java 8 mein introduced.

### Why Default Methods?

```
Problem: Java 8 mein Stream API add karni thi Collection interface mein
Collection interface mein method add karo → sab existing implementations break honge!

Solution: Default method — existing implementations ko affect nahi hoga
```

### Basic Default Method

```java
interface Vehicle {
    // Abstract method
    void start();

    // Default method — implementation ke saath
    default void stop() {
        System.out.println("Vehicle stopped");
    }

    default void honk() {
        System.out.println("Honk! Honk!");
    }
}

class Car implements Vehicle {
    @Override
    public void start() {
        System.out.println("Car started");
    }

    // stop() aur honk() automatically inherit honge
}

// Usage
Car car = new Car();
car.start();  // "Car started" (overridden)
car.stop();   // "Vehicle stopped" (inherited default)
car.honk();   // "Honk! Honk!" (inherited default)
```

### Real-World Example

```java
interface Repository<T> {
    void save(T entity);
    T findById(Long id);

    // Default method — existing implementations break nahi honge
    default Optional<T> findOptionalById(Long id) {
        T entity = findById(id);
        return Optional.ofNullable(entity);
    }

    default List<T> findAllById(List<Long> ids) {
        return ids.stream()
            .map(this::findById)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
```

---

## Default Method Conflicts

**Matlab:** Jab ek class multiple interfaces implement karti hai aur dono mein same default method hota hai.

### Conflict Scenario

```java
interface A {
    default void greet() {
        System.out.println("Hello from A");
    }
}

interface B {
    default void greet() {
        System.out.println("Hello from B");
    }
}

// ❌ Compilation Error — ambiguous default method
class MyClass implements A, B { }
```

### Resolution Rules

```
Rule 1: Class ka method sabse priority hai
Rule 2: Agar class mein nahi hai toh most specific interface ka method
Rule 3: Agar ambiguity hai toh class ko explicitly resolve karna padega
```

### Resolution Examples

```java
// Solution 1: Override karo
class MyClass implements A, B {
    @Override
    public void greet() {
        System.out.println("Hello from MyClass");
    }
}

// Solution 2: Interface A ka method call karo
class MyClass2 implements A, B {
    @Override
    public void greet() {
        A.super.greet();  // "Hello from A"
    }
}

// Solution 3: Interface B ka method call karo
class MyClass3 implements A, B {
    @Override
    public void greet() {
        B.super.greet();  // "Hello from B"
    }
}

// Solution 4: Custom logic
class MyClass4 implements A, B {
    @Override
    public void greet() {
        A.super.greet();
        B.super.greet();
        System.out.println("Hello from MyClass4");
    }
}
```

### Class vs Interface Priority

```java
interface A {
    default void show() {
        System.out.println("A");
    }
}

class Parent {
    public void show() {
        System.out.println("Parent");
    }
}

// Class method wins over interface default method
class Child extends Parent implements A {
    // show() inherit from Parent — A ka default ignore hoga
}

Child child = new Child();
child.show();  // "Parent" (class method wins)
```

---

## Static Methods in Interfaces

**Matlab:** Interface mein static methods — utility methods ke liye, implement nahi hote.

### Usage

```java
interface MathUtils {
    // Static method
    static int add(int a, int b) {
        return a + b;
    }

    static int multiply(int a, int b) {
        return a * b;
    }

    // Abstract method
    int calculate(int a, int b);
}

// Usage — interface name se call karo
System.out.println(MathUtils.add(5, 10));      // 15
System.out.println(MathUtils.multiply(3, 4));   // 12

// Static methods inherit nahi hote
class Calculator implements MathUtils {
    @Override
    public int calculate(int a, int b) {
        return a - b;
    }
}

// Calculator calc = new Calculator();
// calc.add(5, 10);  // ❌ Error! Static methods inherit nahi hote
MathUtils.add(5, 10);  // ✅ Interface name se call karo
```

### Real-World Example: Java Collections

```java
// Java 8 — Collections interface mein static factory methods
List<String> list = List.of("A", "B", "C");
Set<Integer> set = Set.of(1, 2, 3);
Map<String, Integer> map = Map.of("A", 1, "B", 2);

// Interface ke static methods
Comparator<String> comp = Comparator.naturalOrder();
Comparator<String> revComp = Comparator.reverseOrder();
```

---

## Java 9+ Private Interface Methods

**Matlab:** Interface mein private methods — default/static methods ke andar code reuse ke liye.

### Usage

```java
interface Notifier {
    // Default method
    default void sendEmail(String to, String message) {
        validateRecipient(to);  // Private method call
        logNotification(to, message);  // Private static method call
        System.out.println("Email sent to: " + to);
    }

    default void sendSMS(String phone, String message) {
        validatePhone(phone);  // Private method call
        logNotification(phone, message);  // Private static method call
        System.out.println("SMS sent to: " + phone);
    }

    // Private instance method — sirf interface ke andar accessible
    private void validateRecipient(String recipient) {
        if (!recipient.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    private void validatePhone(String phone) {
        if (phone.length() < 10) {
            throw new IllegalArgumentException("Invalid phone");
        }
    }

    // Private static method
    private static void logNotification(String target, String message) {
        System.out.println("Logging: " + target + " - " + message);
    }
}

class EmailNotifier implements Notifier {
    // Sirf default/abstract methods inherit honge
    // Private methods accessible nahi hain
}
```

### Rules

```java
interface MyInterface {
    // ✅ Private instance method
    private void helper() { }

    // ✅ Private static method
    private static void staticHelper() { }

    // Default method can call both
    default void doSomething() {
        helper();         // ✅ Instance private
        staticHelper();   // ✅ Static private
    }

    // Static method can only call static private methods
    static void doStatic() {
        // helper();      // ❌ Cannot call instance method from static
        staticHelper();   // ✅ Static private
    }
}
```

### Why Private Methods?

```java
// ❌ Without private methods — code duplication
interface Notifier {
    default void sendEmail(String to, String msg) {
        if (to == null || to.isEmpty()) throw new IllegalArgumentException();
        System.out.println("LOG: " + msg);
        System.out.println("Email sent");
    }

    default void sendSMS(String phone, String msg) {
        if (phone == null || phone.isEmpty()) throw new IllegalArgumentException();
        System.out.println("LOG: " + msg);
        System.out.println("SMS sent");
    }
}

// ✅ With private methods — code reuse
interface Notifier {
    default void sendEmail(String to, String msg) {
        validate(to);  // Reuse
        log(msg);      // Reuse
        System.out.println("Email sent");
    }

    default void sendSMS(String phone, String msg) {
        validate(phone);  // Reuse
        log(msg);         // Reuse
        System.out.println("SMS sent");
    }

    private void validate(String target) {
        if (target == null || target.isEmpty()) throw new IllegalArgumentException();
    }

    private static void log(String msg) {
        System.out.println("LOG: " + msg);
    }
}
```

---

## Summary

| Feature | Java Version | Purpose |
|---------|-------------|---------|
| **Default Methods** | Java 8+ | Interface mein implementation — backward compatibility |
| **Static Methods** | Java 8+ | Utility methods — interface name se call |
| **Private Methods** | Java 9+ | Code reuse within interface |
| **Conflict Resolution** | Java 8+ | `InterfaceName.super.method()` |
| **Priority** | — | Class method > Most specific interface > Explicit resolution |
