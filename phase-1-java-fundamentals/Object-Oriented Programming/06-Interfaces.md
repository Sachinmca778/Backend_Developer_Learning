# Interfaces

## Status: Not Started

---

## Table of Contents

1. [Interface Declaration](#interface-declaration)
2. [Implementing Multiple Interfaces](#implementing-multiple-interfaces)
3. [Default Methods (Java 8+)](#default-methods-java-8)
4. [Static Methods in Interfaces (Java 8+)](#static-methods-in-interfaces-java-8)
5. [Private Methods (Java 9+)](#private-methods-java-9)
6. [Functional Interfaces (@FunctionalInterface)](#functional-interfaces-functioninterface)
7. [Marker Interfaces](#marker-interfaces)

---

## Interface Declaration

**Matlab:** Ek contract ya blueprint — sirf method declarations (abstract methods), koi implementation nahi (Java 8 se pehle).

### Basic Interface

```java
// Interface declaration
interface Drawable {
    // Abstract methods (public abstract by default)
    void draw();
    double getArea();

    // Constants (public static final by default)
    String COLOR = "Black";  // Same as: public static final String COLOR = "Black";
}

// Implementing class
class Circle implements Drawable {
    private double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public void draw() {
        System.out.println("Drawing a circle");
    }

    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }
}

class Rectangle implements Drawable {
    private double width;
    private double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw() {
        System.out.println("Drawing a rectangle");
    }

    @Override
    public double getArea() {
        return width * height;
    }
}

// Usage
Drawable circle = new Circle(5);
circle.draw();
System.out.println(circle.getArea());

Drawable rectangle = new Rectangle(4, 6);
rectangle.draw();
System.out.println(rectangle.getArea());

// Constants access
System.out.println(Drawable.COLOR);  // "Black"
```

### Interface Rules

```java
interface MyInterface {
    // Methods — by default public abstract
    void method1();              // Same as: public abstract void method1();
    public void method2();       // Explicit public
    public abstract void method3();  // Full declaration

    // Fields — by default public static final
    int CONSTANT = 10;           // Same as: public static final int CONSTANT = 10;
    public static final String NAME = "Test";  // Explicit

    // ❌ Constructors nahi hote
    // public MyInterface() { }  // ❌ Error

    // ❌ Instance fields nahi hote
    // private int count;        // ❌ Error (Java 8 se pehle)
}
```

### Interface Inheritance

```java
interface BasicOperations {
    void add();
    void delete();
}

interface AdvancedOperations extends BasicOperations {
    void update();
    void search();
}

// Implementing class — saare methods implement karne honge
class CrudService implements AdvancedOperations {
    @Override public void add() { System.out.println("Adding"); }
    @Override public void delete() { System.out.println("Deleting"); }
    @Override public void update() { System.out.println("Updating"); }
    @Override public void search() { System.out.println("Searching"); }
}
```

---

## Implementing Multiple Interfaces

**Matlab:** Ek class multiple interfaces implement kar sakti hai — Java ka "multiple inheritance" workaround.

### Basic Multiple Implementation

```java
interface Printable {
    void print();
}

interface Scannable {
    void scan();
}

interface Faxable {
    void fax();
}

// Ek class — multiple interfaces
class MultiFunctionPrinter implements Printable, Scannable, Faxable {
    @Override
    public void print() {
        System.out.println("Printing...");
    }

    @Override
    public void scan() {
        System.out.println("Scanning...");
    }

    @Override
    public void fax() {
        System.out.println("Faxing...");
    }
}

// Usage
MultiFunctionPrinter mfp = new MultiFunctionPrinter();
mfp.print();
mfp.scan();
mfp.fax();

// Interface reference
Printable p = new MultiFunctionPrinter();
p.print();  // ✅ Only print() accessible
```

### Default Method Conflict

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

// ❌ Compilation Error — conflict resolve karna padega
class MyClass implements A, B { }

// ✅ Solution — override karo
class MyClass implements A, B {
    @Override
    public void greet() {
        // Choose one:
        A.super.greet();  // "Hello from A"
        // OR
        B.super.greet();  // "Hello from B"
        // OR custom logic
        System.out.println("Hello from MyClass");
    }
}
```

### Class + Interface

```java
class Animal {
    public void eat() {
        System.out.println("Eating");
    }
}

interface Trainable {
    void train();
}

interface Pettable {
    void pet();
}

// extends pehle, implements baad mein
class Dog extends Animal implements Trainable, Pettable {
    @Override
    public void train() {
        System.out.println("Training dog");
    }

    @Override
    public void pet() {
        System.out.println("Petting dog");
    }
}
```

---

## Default Methods (Java 8+)

**Matlab:** Interface mein implementation ke saath method — backward compatibility ke liye introduced.

### Why Default Methods?

```
Problem: Java 8 mein Streams API add karni thi List interface mein
List interface mein method add karo → sab existing implementations break honge!

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

    // Another default method
    default void honk() {
        System.out.println("Honk! Honk!");
    }
}

class Car implements Vehicle {
    @Override
    public void start() {
        System.out.println("Car started");
    }

    // stop() aur honk() inherit honge — override karna optional hai
}

// Usage
Car car = new Car();
car.start();  // "Car started" (overridden)
car.stop();   // "Vehicle stopped" (inherited default)
car.honk();   // "Honk! Honk!" (inherited default)
```

### Overriding Default Methods

```java
class SportsCar implements Vehicle {
    @Override
    public void start() {
        System.out.println("SportsCar roars to life");
    }

    @Override
    public void stop() {
        System.out.println("SportsCar stops with ABS");
    }

    // honk() inherit hoga — override nahi kiya
}
```

### Calling Interface Default Method

```java
interface A {
    default void show() {
        System.out.println("A");
    }
}

interface B {
    default void show() {
        System.out.println("B");
    }
}

class MyClass implements A, B {
    @Override
    public void show() {
        A.super.show();  // "A"
        B.super.show();  // "B"
        System.out.println("MyClass");
    }
}
```

---

## Static Methods in Interfaces (Java 8+)

**Matlab:** Interface mein static methods — utility methods ke liye.

### Usage

```java
interface MathUtils {
    // Static method — interface name se call karo
    static double square(double x) {
        return x * x;
    }

    static double cube(double x) {
        return x * x * x;
    }

    // Abstract method
    double calculate(double a, double b);
}

// Usage
System.out.println(MathUtils.square(5));  // 25.0
System.out.println(MathUtils.cube(3));    // 27.0

// Implementing class
class Adder implements MathUtils {
    @Override
    public double calculate(double a, double b) {
        return a + b;
    }
}

// Static methods inherit nahi hote
Adder adder = new Adder();
// adder.square(5);  // ❌ Error! Static methods inherit nahi hote
MathUtils.square(5);  // ✅ Interface name se call karo
```

### Real-World Example: Java Collections

```java
// Java 8 — Collections interface mein static methods
List<String> list = List.of("a", "b", "c");  // Static factory method
List<String> sorted = List.copyOf(list);     // Static factory method
```

---

## Private Methods (Java 9+)

**Matlab:** Interface mein private methods — code duplication avoid karne ke liye (default methods ke andar use hote hain).

### Usage

```java
interface Notifier {
    // Default method
    default void sendEmail(String to, String message) {
        validateEmail(to);  // Private method call
        logNotification(to, message);  // Private method call
        System.out.println("Email sent to: " + to);
    }

    default void sendSMS(String phone, String message) {
        validatePhone(phone);  // Private method call
        logNotification(phone, message);  // Private method call
        System.out.println("SMS sent to: " + phone);
    }

    // Private method — sirf interface ke andar accessible
    private void validateEmail(String email) {
        if (!email.contains("@")) {
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
    // Methods inherit honge — private methods accessible nahi hain
}
```

### Private Method Rules

```java
interface MyInterface {
    // ✅ Private instance method
    private void helper() { }

    // ✅ Private static method
    private static void staticHelper() { }

    // Default method can call private methods
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

---

## Functional Interfaces (@FunctionalInterface)

**Matlab:** Interface jisme sirf ek abstract method ho — lambda expressions ke liye use hote hain.

### Basic Functional Interface

```java
@FunctionalInterface
interface MyPredicate<T> {
    boolean test(T t);  // Sirf ek abstract method

    // Default methods allowed
    default MyPredicate<T> and(MyPredicate<T> other) {
        return t -> this.test(t) && other.test(t);
    }

    // Static methods allowed
    static <T> MyPredicate<T> alwaysTrue() {
        return t -> true;
    }

    // Object methods don't count
    @Override
    boolean equals(Object obj);  // Yeh count nahi hota
}

// Usage with Lambda
MyPredicate<Integer> isEven = n -> n % 2 == 0;
System.out.println(isEven.test(4));  // true

MyPredicate<Integer> isPositive = n -> n > 0;
MyPredicate<Integer> isEvenAndPositive = isEven.and(isPositive);
System.out.println(isEvenAndPositive.test(4));  // true
System.out.println(isEvenAndPositive.test(-4)); // false
```

### Built-in Functional Interfaces

```java
// Predicate<T> — boolean return
Predicate<String> isEmpty = String::isEmpty;
Predicate<String> isLong = s -> s.length() > 10;

// Function<T, R> — T input, R output
Function<String, Integer> parse = Integer::parseInt;
Function<Integer, String> toString = Object::toString;

// Consumer<T> — T input, no return
Consumer<String> print = System.out::println;
Consumer<String> log = s -> System.out.println("LOG: " + s);

// Supplier<T> — no input, T output
Supplier<Double> random = Math::random;
Supplier<String> greeting = () -> "Hello!";

// UnaryOperator<T> — T input, T output
UnaryOperator<Integer> doubleIt = n -> n * 2;

// BinaryOperator<T> — T, T input, T output
BinaryOperator<Integer> add = (a, b) -> a + b;
```

### @FunctionalInterface Annotation

```java
// ✅ Valid — single abstract method
@FunctionalInterface
interface Valid {
    void execute();
}

// ✅ Valid — single abstract method + default/static
@FunctionalInterface
interface Valid2 {
    void execute();
    default void log() { }
    static void helper() { }
}

// ❌ Invalid — multiple abstract methods
@FunctionalInterface
interface Invalid {
    void execute();
    void process();  // ❌ Second abstract method
}

// Annotation optional hai — bina annotation ke bhi functional interface hai
interface WithoutAnnotation {
    void run();  // Still a functional interface
}
```

---

## Marker Interfaces

**Matlab:** Empty interfaces — koi method nahi, sirf type marking ke liye.

### Built-in Marker Interfaces

```java
// Serializable — object ko serialize/deserialize kar sakte hain
class User implements java.io.Serializable {
    private String name;
    private int age;
    // No methods in interface — sirf marker
}

// Cloneable — object.clone() call kar sakte hain
class Product implements Cloneable {
    private String name;

    @Override
    public Product clone() {
        try {
            return (Product) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

// Remote (RMI) — remote object
interface MyRemote extends java.rmi.Remote { }
```

### Custom Marker Interface

```java
// Custom marker
interface Auditable { }
interface Cacheable { }

// Usage
class Order implements Auditable, Cacheable {
    private String id;
    private double amount;
}

class Product implements Cacheable {
    private String name;
    private double price;
}

// Runtime pe check karo
public void process(Object obj) {
    if (obj instanceof Auditable) {
        System.out.println("Auditing: " + obj);
    }
    if (obj instanceof Cacheable) {
        System.out.println("Caching: " + obj);
    }
}
```

### Marker Interface vs Annotation

```java
// Marker Interface
interface Auditable { }

// Equivalent Annotation
@interface AuditableAnnotation { }

// Comparison
// Marker Interface: instanceof check kar sakte ho
// Annotation: hasAnnotation() se check karo

// Modern Java: Annotations prefer karo (more flexible, metadata support)
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Interface** | Contract — methods declare karta hai, classes implement karti hain |
| **Multiple Interfaces** | Ek class multiple interfaces implement kar sakti hai |
| **Default Methods** | Java 8+ — interface mein implementation, backward compatibility |
| **Static Methods** | Java 8+ — utility methods, interface name se call |
| **Private Methods** | Java 9+ — default methods ke andar code reuse |
| **Functional Interface** | Sirf ek abstract method — lambda expressions ke liye |
| **@FunctionalInterface** | Compiler check — sirf ek abstract method hona chahiye |
| **Marker Interface** | Empty interface — type marking (Serializable, Cloneable) |
