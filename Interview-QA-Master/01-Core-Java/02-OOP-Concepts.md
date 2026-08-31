# Object-Oriented Programming - Top 10 Interview Questions & Answers

> Covering: Classes/Objects, Inheritance, Polymorphism, Encapsulation, Abstraction, Interfaces, Nested/Inner Classes, and SOLID Principles

---

## Q1: OOP ke 4 pillars kya hain? Har ek ko explain karo with example.

**Answer:**

OOP ke **4 pillars** hain — yeh object-oriented design ke foundation hain.

| Pillar | Matlab | Java Mein Kaise |
|--------|--------|-----------------|
| **Encapsulation** | Data hide karo, controlled access do | private fields + public getters/setters |
| **Inheritance** | Parent class ki properties/methods reuse karo | `extends` keyword |
| **Polymorphism** | Ek cheez ke multiple forms | Overloading + Overriding |
| **Abstraction** | Implementation hide karo, essential details dikhao | abstract class + interface |

### 1. Encapsulation

```java
public class BankAccount {
    private double balance;  // Data hidden

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance += amount;  // Controlled access with validation
        }
    }
}
```

### 2. Inheritance

```java
class Animal {
    public void eat() { System.out.println("Eating"); }
}

class Dog extends Animal {
    public void bark() { System.out.println("Bark!"); }
}

Dog dog = new Dog();
dog.eat();   // Inherited
dog.bark();  // Dog's own
```

### 3. Polymorphism

```java
// Compile-time — Overloading
class Calculator {
    int add(int a, int b) { return a + b; }
    double add(double a, double b) { return a + b; }
}

// Runtime — Overriding
Animal animal = new Dog();
animal.makeSound();  // "Bark!" — runtime pe decide hoga
```

### 4. Abstraction

```java
abstract class Shape {
    abstract double getArea();  // Implementation hide
}

interface Drawable {
    void draw();  // Contract only
}
```

**Short Interview Answer:**

Encapsulation = data hiding. Inheritance = code reuse via extends. Polymorphism = many forms (overloading + overriding). Abstraction = hide complexity via abstract class/interface.

---

## Q2: Inheritance vs Composition — kab kaunsa prefer karte hain?

**Answer:**

| Aspect | Inheritance (`is-a`) | Composition (`has-a`) |
|--------|---------------------|------------------------|
| Relationship | Dog **is an** Animal | Car **has an** Engine |
| Keyword | `extends` | Field reference |
| Coupling | Tight coupling | Loose coupling |
| Flexibility | Less flexible | More flexible |
| Multiple | Single class only | Multiple components |

### Inheritance Example

```java
class Animal {
    public void eat() { System.out.println("Eating"); }
}

class Dog extends Animal {  // Dog IS-A Animal
    public void bark() { System.out.println("Bark!"); }
}
```

### Composition Example

```java
class Engine {
    public void start() { System.out.println("Engine started"); }
}

class Car {  // Car HAS-A Engine
    private Engine engine = new Engine();

    public void startCar() {
        engine.start();
        System.out.println("Car moving");
    }
}
```

### Composition Over Inheritance — Why?

```java
// ❌ Inheritance — tight coupling, fragile
class Stack extends ArrayList {
    public void push(Object item) { add(item); }
    public Object pop() { return remove(size() - 1); }
    // Problem: Stack ko ArrayList ke saare methods mil gaye (add at index, etc.)
}

// ✅ Composition — clean API
class Stack {
    private List<Object> list = new ArrayList<>();

    public void push(Object item) { list.add(item); }
    public Object pop() { return list.remove(list.size() - 1); }
    // Sirf Stack ke methods expose hain
}
```

### When to Use What

```
Inheritance use karo jab:
✅ Genuine "is-a" relationship ho (Dog is Animal)
✅ Shared behavior reuse karna ho
✅ Polymorphism chahiye (Animal ref → Dog object)

Composition use karo jab:
✅ "has-a" relationship ho (Car has Engine)
✅ Runtime pe behavior change karna ho
✅ Multiple capabilities combine karni hon
✅ Loose coupling chahiye
```

**Important:**

- **"Favor composition over inheritance"** — Design Patterns ka common principle
- Inheritance se **fragile base class problem** aa sakta hai — parent change → child break
- Java mein **multiple class inheritance nahi** — composition se workaround

**Short Interview Answer:**

Inheritance = is-a, tight coupling, single parent. Composition = has-a, loose coupling, flexible. Prefer composition when relationship isn't true is-a or when you need runtime flexibility.

---

## Q3: Method Overloading vs Method Overriding — difference aur rules?

**Answer:**

| Feature | Overloading | Overriding |
|---------|-------------|------------|
| **Where** | Same class | Parent-child class |
| **Signature** | Same name, different parameters | Same name + same parameters |
| **Return type** | Can differ (with params) | Same or covariant |
| **Binding** | Compile-time (static) | Runtime (dynamic) |
| **Polymorphism** | Compile-time | Runtime |
| **Access modifier** | Any | Same or broader |
| **static methods** | Can overload | Cannot override (hide) |

### Method Overloading

```java
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int add(int a, int b, int c) {
        return a + b + c;
    }

    public double add(double a, double b) {
        return a + b;
    }
}

Calculator calc = new Calculator();
calc.add(5, 10);       // int version
calc.add(5.5, 10.5);   // double version
```

### Overloading Rules

```java
// ✅ Valid — different parameters
void method(int a) { }
void method(double a) { }
void method(int a, int b) { }

// ❌ Invalid — sirf return type different
int method(int a) { return a; }
String method(int a) { return ""; }  // ❌ Compile error!
```

### Method Overriding

```java
class Animal {
    public void makeSound() {
        System.out.println("Some sound");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Bark!");
    }
}

Animal animal = new Dog();
animal.makeSound();  // "Bark!" — runtime pe Dog ka method call hoga
```

### Overriding Rules

```java
class Parent {
    public void method1() { }
    protected void method2() { }
    static void method3() { }
    final void method4() { }
    private void method5() { }
}

class Child extends Parent {
    @Override public void method1() { }       // ✅ Same access
    @Override public void method2() { }       // ✅ Broader (protected → public)
    static void method3() { }                 // ⚠️ Hiding, not overriding
    // void method4() { }                     // ❌ final — override nahi
    // void method5() { }                     // ❌ private — not inherited
}
```

**Important:**

- **`@Override`** annotation hamesha lagao — typo pe compile error milega
- **static methods override nahi hote** — method hiding hoti hai
- **private/final methods override nahi ho sakte**

**Short Interview Answer:**

Overloading = same class, different params, compile-time. Overriding = parent-child, same signature, runtime dispatch. @Override for safety. static = hiding, not overriding.

---

## Q4: Abstract class vs Interface — kab kaunsa use karte hain?

**Answer:**

| Feature | Abstract Class | Interface |
|---------|---------------|-----------|
| **Keyword** | `abstract class` | `interface` |
| **Methods** | Abstract + Concrete | Abstract + default + static + private |
| **Fields** | Any type (instance fields) | `public static final` only |
| **Constructors** | ✅ Yes | ❌ No |
| **Inheritance** | `extends` (single) | `implements` (multiple) |
| **State** | Can hold state | No instance state |
| **Use Case** | "is-a" + shared code | "can-do" contract/capability |

### Abstract Class

```java
abstract class Shape {
    String color;  // State rakh sakta hai

    public Shape(String color) {  // Constructor
        this.color = color;
    }

    public void setColor(String color) {  // Concrete method
        this.color = color;
    }

    public abstract double getArea();  // Abstract — subclass implement karega
}

class Circle extends Shape {
    private double radius;

    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }

    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }
}
```

### Interface

```java
interface Flyable {
    void fly();  // public abstract by default

    default void land() {  // Java 8+ default method
        System.out.println("Landing...");
    }

    static void info() {  // Java 8+ static method
        System.out.println("Flyable interface");
    }
}

interface Swimmable {
    void swim();
}

class Duck extends Animal implements Flyable, Swimmable {
    @Override public void fly() { System.out.println("Duck flying"); }
    @Override public void swim() { System.out.println("Duck swimming"); }
}
```

### Decision Guide

```
Abstract Class choose karo jab:
✅ Shared state (fields) chahiye
✅ Common implementation reuse karna ho
✅ Constructor logic chahiye
✅ "is-a" relationship with base behavior

Interface choose karo jab:
✅ Contract/capability define karni ho ("can fly", "can swim")
✅ Multiple inheritance chahiye
✅ Unrelated classes ko same behavior dena ho
✅ API design / loose coupling chahiye
```

### Java 8+ Blurred Lines

```java
// Interface mein ab implementation ho sakti hai
interface PaymentProcessor {
    void processPayment(double amount);  // abstract

    default void logTransaction(String msg) {  // default implementation
        System.out.println("[LOG] " + msg);
    }
}
```

**Important:**

- Abstract class **instantiate nahi** ho sakti — `new Shape()` error
- Interface fields **implicitly public static final** hain
- Class **ek class extend + multiple interfaces implement** kar sakti hai

**Short Interview Answer:**

Abstract class = is-a, shared state/code, single inheritance. Interface = can-do contract, multiple inheritance, no state. Java 8+ interfaces have default/static methods.

---

## Q5: Interface default methods aur static methods — kyun introduce kiye (Java 8+)?

**Answer:**

Java 8 se pehle interfaces mein **sirf abstract methods** the — existing interfaces mein naya method add karna **breaking change** tha (sab implementing classes break ho jaati).

### Default Methods (Java 8+)

```java
interface Vehicle {
    void start();  // Abstract — implement karna padega

    default void honk() {  // Default implementation
        System.out.println("Beep beep!");
    }
}

class Car implements Vehicle {
    @Override
    public void start() {
        System.out.println("Car started");
    }
    // honk() inherit ho gaya — implement nahi karna pada
}

class SilentCar implements Vehicle {
    @Override
    public void start() { System.out.println("Silent start"); }

    @Override
    public void honk() { /* Override — no sound */ }
}
```

### Static Methods in Interface (Java 8+)

```java
interface MathUtils {
    static int max(int a, int b) {
        return a > b ? a : b;
    }

    static double square(double x) {
        return x * x;
    }
}

// Call via interface name — NOT via object
int result = MathUtils.max(5, 10);  // 10
```

### Private Methods in Interface (Java 9+)

```java
interface Logger {
    default void logInfo(String msg) {
        log("INFO", msg);
    }

    default void logError(String msg) {
        log("ERROR", msg);
    }

    private void log(String level, String msg) {  // Shared helper
        System.out.println("[" + level + "] " + msg);
    }
}
```

### Default Method Conflict Resolution

```java
interface A {
    default void greet() { System.out.println("Hello from A"); }
}

interface B {
    default void greet() { System.out.println("Hello from B"); }
}

// ❌ Compilation error — conflict!
class MyClass implements A, B { }

// ✅ Override karke resolve karo
class MyClass implements A, B {
    @Override
    public void greet() {
        A.super.greet();  // Explicitly choose A's default
    }
}
```

**Important:**

- Default methods = **backward compatibility** — purane interfaces mein naya method add kar sakte ho
- Static interface methods = **utility methods** — implementation class pe burden nahi
- Multiple default method conflict → **override + InterfaceName.super.method()**

**Short Interview Answer:**

Default methods = backward compatible interface evolution. Static methods = utility on interface. Private methods (Java 9) = code reuse within interface. Conflict resolution via override + super call.

---

## Q6: Encapsulation kya hai? Access modifiers explain karo.

**Answer:**

**Encapsulation** = data (fields) ko hide karo aur **controlled access** do through methods — internal representation change kar sakte ho bina external code break kiye.

### Access Modifiers Table

| Modifier | Same Class | Same Package | Subclass (Diff Package) | Anywhere |
|----------|-----------|--------------|-------------------------|----------|
| **private** | ✅ | ❌ | ❌ | ❌ |
| **default** | ✅ | ✅ | ❌ | ❌ |
| **protected** | ✅ | ✅ | ✅ | ❌ |
| **public** | ✅ | ✅ | ✅ | ✅ |

### Encapsulation Example

```java
public class Employee {
    private String name;       // Hidden
    private double salary;     // Hidden

    public Employee(String name, double salary) {
        setName(name);
        setSalary(salary);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        this.name = name;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        if (salary < 0) {
            throw new IllegalArgumentException("Salary cannot be negative");
        }
        this.salary = salary;
    }
}
```

### Getter/Setter Benefits

```java
// ✅ Validation add kar sakte ho
public void setAge(int age) {
    if (age < 0 || age > 150) throw new IllegalArgumentException("Invalid age");
    this.age = age;
}

// ✅ Read-only field bana sakte ho
public String getId() { return id; }
// No setter — id immutable after creation

// ✅ Lazy initialization
public Connection getConnection() {
    if (connection == null) {
        connection = createConnection();
    }
    return connection;
}
```

### JavaBeans Convention

```java
// Standard naming for frameworks (Spring, Hibernate)
private String firstName;

public String getFirstName() { return firstName; }  // getter: get + FieldName
public void setFirstName(String firstName) { this.firstName = firstName; }

private boolean active;
public boolean isActive() { return active; }  // boolean: is + FieldName
public void setActive(boolean active) { this.active = active; }
```

**Important:**

- Fields **hamesha private** rakho — public fields anti-pattern hain
- **Validation logic** setters mein daalo — invalid state prevent karo
- Java 16+ **`record`** classes automatic encapsulation deti hain immutable data ke liye

**Short Interview Answer:**

Encapsulation = hide data, expose via getters/setters with validation. Access modifiers: private (class only) → default (package) → protected (+ subclass) → public (everywhere).

---

## Q7: `this` aur `super` keywords ka kya use hai?

**Answer:**

| Keyword | Use |
|---------|-----|
| **`this`** | Current object reference — fields distinguish, constructor chaining |
| **`super`** | Parent class reference — parent methods/fields, constructor call |

### this Keyword

```java
class User {
    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;  // Field vs parameter distinguish
        this.age = age;
    }

    public User(String name) {
        this(name, 0);  // Constructor chaining — same class ka dusra constructor
    }

    public void display() {
        System.out.println(this);  // Current object reference
    }
}
```

### this — Variable Shadowing Fix

```java
class User {
    String name = "Default";

    public void setName(String name) {
        // name = name;       // ❌ Parameter ko hi assign — no effect!
        this.name = name;     // ✅ Instance field ko assign
    }
}
```

### super Keyword

```java
class Animal {
    String name;

    public Animal(String name) {
        this.name = name;
        System.out.println("Animal constructor");
    }

    public void eat() {
        System.out.println(name + " is eating");
    }
}

class Dog extends Animal {
    String breed;

    public Dog(String name, String breed) {
        super(name);  // Parent constructor MUST be first line
        this.breed = breed;
        System.out.println("Dog constructor");
    }

    @Override
    public void eat() {
        super.eat();  // Parent ka method call karo
        System.out.println("Dog is eating dog food");
    }
}

// Output: Animal constructor → Dog constructor
```

### Constructor Chaining Rules

```java
class User {
    String name;
    int age;
    String email;

    public User() {
        this("Unknown", 0, "unknown@mail.com");  // this() — same class
    }

    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }
}

class Dog extends Animal {
    public Dog(String name) {
        super(name);  // super() — parent class constructor (first line mandatory)
    }
}
```

**Important:**

- `super()` ya `this()` **constructor ki pehli line** honi chahiye
- Agar parent mein parameterized constructor hai aur child mein `super()` nahi → **compile error**
- `super.method()` se parent ka overridden method bhi call kar sakte ho

**Short Interview Answer:**

`this` = current object (field disambiguation, constructor chaining). `super` = parent class (constructor call, parent method access). Both must be first statement in constructor when used.

---

## Q8: Nested class, Inner class, aur Static nested class mein kya difference hai?

**Answer:**

| Type | static? | Outer instance needed? | Outer members access |
|------|---------|------------------------|---------------------|
| **Static Nested Class** | ✅ Yes | ❌ No | Static only (direct) |
| **Inner Class (Non-static)** | ❌ No | ✅ Yes | All (including private) |
| **Local Class** | ❌ No | ✅ Yes (in method) | All outer members |
| **Anonymous Class** | ❌ No | ✅ Yes | All outer members |

### Static Nested Class

```java
class Outer {
    private static String staticField = "Static";
    private String instanceField = "Instance";

    static class StaticNested {
        void display() {
            System.out.println(staticField);  // ✅ Static access
            // System.out.println(instanceField);  // ❌ Direct access nahi
            Outer outer = new Outer();
            System.out.println(outer.instanceField);  // ✅ Via object
        }
    }
}

// Usage — outer object NOT needed
Outer.StaticNested nested = new Outer.StaticNested();
```

### Inner Class (Non-Static)

```java
class Outer {
    private String message = "Hello from Outer";

    class Inner {
        void display() {
            System.out.println(message);       // ✅ Direct access — even private!
            System.out.println(Outer.this);    // Outer object reference
        }
    }
}

// Usage — outer object REQUIRED
Outer outer = new Outer();
Outer.Inner inner = outer.new Inner();
inner.display();
```

### Builder Pattern — Static Nested Class

```java
public class User {
    private final String name;
    private final int age;

    private User(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
    }

    public static class Builder {
        private String name;
        private int age;

        public Builder name(String name) { this.name = name; return this; }
        public Builder age(int age) { this.age = age; return this; }
        public User build() { return new User(this); }
    }
}

User user = new User.Builder().name("Sachin").age(25).build();
```

### When to Use What

```
Static Nested Class:
✅ Outer instance ki zarurat nahi
✅ Logical grouping (Map.Entry, Builder pattern)
✅ Memory efficient — no implicit outer reference

Inner Class:
✅ Outer object ke saath tightly coupled logic
✅ Outer ke private members access chahiye
✅ Event handlers, iterators
```

**Important:**

- Inner class mein **implicit reference** outer object ka hota hai — memory leak risk (long-lived inner, short-lived outer)
- Static nested class **independent** hai — `Outer.Inner` syntax se banao
- Java 8+ mein **lambda** anonymous classes replace kar dete hain mostly

**Short Interview Answer:**

Static nested = no outer instance needed, static members access. Inner = needs outer object, accesses all outer members. Builder pattern uses static nested. Prefer static nested unless outer access needed.

---

## Q9: Polymorphism — compile-time vs runtime, upcasting/downcasting?

**Answer:**

**Polymorphism** = "Poly" (many) + "Morph" (forms) — ek reference se multiple types ka behavior.

### Compile-time vs Runtime

| | Compile-time | Runtime |
|---|-------------|---------|
| **Mechanism** | Method Overloading | Method Overriding |
| **Binding** | Static / Early | Dynamic / Late |
| **Decision** | Compiler | JVM (actual object type) |
| **Example** | `add(int,int)` vs `add(double,double)` | `Animal a = new Dog(); a.makeSound()` |

### Runtime Polymorphism — Dynamic Method Dispatch

```java
class Animal {
    public void makeSound() { System.out.println("Generic sound"); }
}

class Dog extends Animal {
    @Override
    public void makeSound() { System.out.println("Bark!"); }
}

class Cat extends Animal {
    @Override
    public void makeSound() { System.out.println("Meow!"); }
}

Animal a1 = new Dog();
Animal a2 = new Cat();

a1.makeSound();  // "Bark!" — runtime pe Dog ka method
a2.makeSound();  // "Meow!" — runtime pe Cat ka method
```

### Upcasting (Automatic)

```java
Dog dog = new Dog();
Animal animal = dog;  // Upcasting — automatic, safe

animal.eat();        // ✅ Inherited methods
animal.makeSound();  // ✅ Overridden method — Dog ka version call hoga
// animal.bark();    // ❌ Dog-specific method — compile error
```

### Downcasting (Manual + instanceof)

```java
Animal animal = new Dog();

// ❌ Unsafe cast
Dog dog = (Dog) animal;  // Works if actually Dog

Animal cat = new Cat();
// Dog dog2 = (Dog) cat;  // ❌ ClassCastException at runtime!

// ✅ Safe downcasting
if (animal instanceof Dog dog) {  // Java 14+ pattern matching
    dog.bark();  // Safe — auto-cast
}

// Old style
if (animal instanceof Dog) {
    Dog d = (Dog) animal;
    d.bark();
}
```

**Important:**

- **Upcasting** hamesha safe — parent reference se child object
- **Downcasting** pehle `instanceof` check karo — warna ClassCastException
- **Method resolution** runtime pe actual object type se hoti hai (overriding)
- **Field access** compile-time type se hota hai (no field polymorphism)

**Short Interview Answer:**

Compile-time poly = overloading. Runtime poly = overriding + dynamic dispatch. Upcasting automatic. Downcasting needs instanceof check. JVM calls method based on actual object type.

---

## Q10: SOLID principles briefly explain karo — Java context mein.

**Answer:**

SOLID = 5 design principles jo maintainable, flexible code ke liye hain.

### S — Single Responsibility Principle

**Ek class ka ek hi reason hona chahiye change hone ka.**

```java
// ❌ Bad — multiple responsibilities
class User {
    void saveToDatabase() { }
    void sendEmail() { }
    void generateReport() { }
}

// ✅ Good — separate classes
class UserRepository { void save(User user) { } }
class EmailService { void sendEmail(User user) { } }
class ReportGenerator { void generate(User user) { } }
```

### O — Open/Closed Principle

**Open for extension, closed for modification.**

```java
// ✅ New shapes add karo bina existing code change kiye
interface Shape { double getArea(); }

class Circle implements Shape {
    double radius;
    public double getArea() { return Math.PI * radius * radius; }
}

class Rectangle implements Shape {
    double w, h;
    public double getArea() { return w * h; }
}

// AreaCalculator mein change nahi — naya Shape implement karo
double totalArea(List<Shape> shapes) {
    return shapes.stream().mapToDouble(Shape::getArea).sum();
}
```

### L — Liskov Substitution Principle

**Parent ki jagah child use karo — behavior break nahi hona chahiye.**

```java
// ❌ Violation — Square extends Rectangle but breaks behavior
class Rectangle { void setWidth(int w) { } void setHeight(int h) { } }
class Square extends Rectangle {
    void setWidth(int w) { /* sets both width and height */ }
}

// ✅ Better — separate classes or common interface
interface Shape { double getArea(); }
```

### I — Interface Segregation Principle

**Clients ko woh interfaces do jo unhe chahiye — fat interfaces mat banao.**

```java
// ❌ Fat interface
interface Worker { void work(); void eat(); void sleep(); }

// ✅ Segregated
interface Workable { void work(); }
interface Eatable { void eat(); }

class Human implements Workable, Eatable { }
class Robot implements Workable { }  // Robot ko eat/sleep nahi chahiye
```

### D — Dependency Inversion Principle

**High-level modules low-level pe depend na kare — dono abstractions pe depend kare.**

```java
// ❌ Direct dependency
class OrderService {
    MySqlRepository repo = new MySqlRepository();  // Tight coupling
}

// ✅ Depend on abstraction
interface OrderRepository { void save(Order order); }

class OrderService {
    private final OrderRepository repo;
    OrderService(OrderRepository repo) { this.repo = repo; }  // DI
}
```

**Short Interview Answer:**

S = one class, one job. O = extend via new classes, don't modify existing. L = child substitutable for parent. I = small specific interfaces. D = depend on abstractions, not concrete classes. Spring DI follows D principle.

---
