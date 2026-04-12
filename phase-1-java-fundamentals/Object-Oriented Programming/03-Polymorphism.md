# Polymorphism

## Status: Not Started

---

## Table of Contents

1. [Compile-time vs Runtime Polymorphism](#compile-time-vs-runtime-polymorphism)
2. [Method Overloading](#method-overloading)
3. [Method Overriding](#method-overriding)
4. [Dynamic Method Dispatch](#dynamic-method-dispatch)
5. [Upcasting & Downcasting](#upcasting--downcasting)
6. [instanceof Check](#instanceof-check)

---

## Compile-time vs Runtime Polymorphism

**Matlab:** "Poly" = many, "morph" = forms — ek cheez ka multiple roop.

### Comparison

| Feature | Compile-time Polymorphism | Runtime Polymorphism |
|---------|--------------------------|---------------------|
| **Achieved by** | Method Overloading | Method Overriding |
| **Resolved at** | Compile time | Runtime |
| **Also known as** | Static binding / Early binding | Dynamic binding / Late binding |
| **Performance** | Faster | Slightly slower |
| **Flexibility** | Less flexible | More flexible |

### Example Side by Side

```java
class Calculator {
    // Compile-time polymorphism (Overloading)
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

class Animal {
    public void makeSound() {
        System.out.println("Some sound");
    }
}

class Dog extends Animal {
    // Runtime polymorphism (Overriding)
    @Override
    public void makeSound() {
        System.out.println("Bark!");
    }
}
```

---

## Method Overloading

**Matlab:** Same class mein multiple methods ka same name lekin different parameters.

### Different Number of Parameters

```java
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public int add(int a, int b, int c) {
        return a + b + c;
    }
}

// Usage
Calculator calc = new Calculator();
calc.add(5, 10);       // 15
calc.add(5, 10, 15);   // 30
```

### Different Types of Parameters

```java
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }

    public double add(double a, double b) {
        return a + b;
    }

    public String add(String a, String b) {
        return a + b;  // Concatenation
    }
}

// Usage
calc.add(5, 10);           // int version → 15
calc.add(5.5, 10.5);       // double version → 16.0
calc.add("Hello", "World"); // String version → "HelloWorld"
```

### Different Order of Parameters

```java
class Printer {
    public void print(String message, int count) {
        for (int i = 0; i < count; i++) {
            System.out.println(message);
        }
    }

    public void print(int count, String message) {
        for (int i = 0; i < count; i++) {
            System.out.println(message);
        }
    }
}

// Usage
printer.print("Hello", 3);   // String, int
printer.print(3, "Hello");   // int, String
```

### Overloading with Autoboxing

```java
class Processor {
    public void process(int n) {
        System.out.println("int: " + n);
    }

    public void process(Integer n) {
        System.out.println("Integer: " + n);
    }
}

// Usage
Processor p = new Processor();
p.process(10);           // "int: 10" (exact match preferred)
p.process(new Integer(10)); // "Integer: 10"
```

### Overloading with Varargs

```java
class MathUtils {
    public int sum(int... numbers) {
        int total = 0;
        for (int n : numbers) {
            total += n;
        }
        return total;
    }
}

// Usage
MathUtils math = new MathUtils();
math.sum();              // 0
math.sum(1, 2);          // 3
math.sum(1, 2, 3, 4, 5); // 15
```

### ⚠️ Overloading Rules

```java
// ✅ Valid overloads
class Valid {
    void method(int a) { }
    void method(double a) { }          // Different type
    void method(int a, int b) { }      // Different count
    void method(int a, String b) { }   // Different type
    String method(int a) { }           // ❌ Invalid — only return type different
}

// ❌ Invalid — return type alone is not enough
class Invalid {
    int method(int a) { return a; }
    String method(int a) { return ""; }  // ❌ Compilation Error!
}

// ✅ Return type + parameter change = valid
class ValidReturn {
    int method(int a) { return a; }
    String method(String a) { return a; }  // ✅ Parameter type bhi different hai
}
```

---

## Method Overriding

**Matlab:** Child class parent class ke method ko same signature ke saath redefine karti hai.

### Basic Overriding

```java
class Animal {
    public void makeSound() {
        System.out.println("Some generic sound");
    }

    public void move() {
        System.out.println("Animal is moving");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Bark! Bark!");
    }

    // move() inherited — override nahi kiya
}

class Cat extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Meow! Meow!");
    }

    @Override
    public void move() {
        System.out.println("Cat is walking gracefully");
    }
}
```

### Overriding Rules

```java
class Parent {
    public void method1() { }
    protected void method2() { }
    void method3() { }
    private void method4() { }
    static void method5() { }
    final void method6() { }
}

class Child extends Parent {
    // ✅ Can override with same or broader access
    @Override public void method1() { }         // ✅ Same
    @Override public void method2() { }         // ✅ Broader (protected → public)
    @Override public void method3() { }         // ✅ Broader (default → public)

    // ❌ Cannot override
    // @Override public void method4() { }      // ❌ Private — inherited nahi hota
    // @Override public static void method5() { } // ❌ Static — hide hota hai, override nahi
    // @Override public void method6() { }      // ❌ Final — override nahi hota

    // ✅ Exception rules
    @Override
    public void method1() throws RuntimeException { }  // ✅ Unchecked exception allowed
}
```

---

## Dynamic Method Dispatch

**Matlab:** Runtime pe decide hota hai ki kaunsa method call hoga — parent reference se child method call hota hai.

### How It Works

```java
class Animal {
    public void makeSound() {
        System.out.println("Animal sound");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Bark!");
    }
}

class Cat extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Meow!");
    }
}

// Dynamic method dispatch
public class Main {
    public static void main(String[] args) {
        Animal animal1 = new Dog();  // Upcasting
        Animal animal2 = new Cat();  // Upcasting

        animal1.makeSound();  // "Bark!" — Dog ka method call hua
        animal2.makeSound();  // "Meow!" — Cat ka method call hua

        // Compile-time pe: Animal reference — makeSound() exist karta hai
        // Runtime pe: Actual object Dog/Cat — unka overridden method call hoga
    }
}
```

### Real-World Example

```java
// Payment processing
interface PaymentProcessor {
    void processPayment(double amount);
}

class CreditCardProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing credit card payment: $" + amount);
    }
}

class PayPalProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing PayPal payment: $" + amount);
    }
}

class CryptoProcessor implements PaymentProcessor {
    @Override
    public void processPayment(double amount) {
        System.out.println("Processing crypto payment: $" + amount);
    }
}

// Usage
PaymentProcessor processor = getProcessor("paypal");
processor.processPayment(100.0);  // Runtime pe decide hoga kaunsa processor

private PaymentProcessor getProcessor(String type) {
    return switch (type) {
        case "credit" -> new CreditCardProcessor();
        case "paypal" -> new PayPalProcessor();
        case "crypto" -> new CryptoProcessor();
        default -> new CreditCardProcessor();
    };
}
```

### What is NOT Overridden

```java
class Parent {
    String name = "Parent";

    public void showName() {
        System.out.println(name);
    }

    public static void staticMethod() {
        System.out.println("Parent static");
    }
}

class Child extends Parent {
    String name = "Child";  // Field hiding (not overriding)

    @Override
    public void showName() {
        System.out.println(name);
    }

    public static void staticMethod() {  // Method hiding (not overriding)
        System.out.println("Child static");
    }
}

// Usage
Parent obj = new Child();

// Fields — compile-time pe decide (reference type)
System.out.println(obj.name);       // "Parent" (reference type ka field)

// Methods — runtime pe decide (actual object)
obj.showName();                     // "Child" (overridden method)

// Static methods — compile-time pe decide (reference type)
Parent.staticMethod();              // "Parent static"
```

---

## Upcasting & Downcasting

### Upcasting (Safe — Automatic)

**Matlab:** Child reference ko Parent type mein convert karna — automatically hota hai.

```java
class Animal { }
class Dog extends Animal { }

// Upcasting
Dog dog = new Dog();
Animal animal = dog;  // ✅ Automatic upcasting

// Ya directly
Animal animal = new Dog();  // Upcasting

// Usage
Animal animal = new Dog();
// animal sirf Animal ke methods access kar sakta hai
// animal.makeSound() — Dog ka makeSound() call hoga (overriding)
```

### Downcasting (Manual — Risky)

**Matlab:** Parent reference ko Child type mein convert karna — manually, risky.

```java
Animal animal = new Dog();  // Upcasting

// Downcasting — manually
Dog dog = (Dog) animal;  // ✅ Works — actual object Dog hai

// ❌ Wrong downcast
Animal animal2 = new Cat();
Dog dog2 = (Dog) animal2;  // ❌ ClassCastException! (actual object Cat hai)
```

### When to Downcast

```java
class Animal {
    public void eat() {
        System.out.println("Eating");
    }
}

class Dog extends Animal {
    public void fetch() {
        System.out.println("Fetching ball");
    }
}

// Usage
Animal animal = new Dog();
animal.eat();     // ✅ Inherited method
// animal.fetch(); // ❌ Animal reference — fetch() available nahi

// Downcast to access child-specific methods
if (animal instanceof Dog dog) {  // Java 14+ pattern matching
    dog.fetch();  // ✅ Works
}

// Old style
if (animal instanceof Dog) {
    Dog d = (Dog) animal;
    d.fetch();
}
```

### Upcasting vs Downcasting

| Feature | Upcasting | Downcasting |
|---------|-----------|-------------|
| **Direction** | Child → Parent | Parent → Child |
| **Automatic?** | ✅ Yes | ❌ Manual `(Type)` |
| **Safe?** | ✅ Always safe | ❌ Can throw ClassCastException |
| **Access** | Parent methods only | Child methods available |
| **Example** | `Animal a = new Dog();` | `Dog d = (Dog) animal;` |

---

## instanceof Check

**Matlab:** Check karna ki koi object kisi specific class ka instance hai ya nahi.

### Basic Usage

```java
class Animal { }
class Dog extends Animal { }
class Cat extends Animal { }

Animal animal1 = new Dog();
Animal animal2 = new Cat();

System.out.println(animal1 instanceof Dog);  // true
System.out.println(animal1 instanceof Cat);  // false
System.out.println(animal1 instanceof Animal);  // true
System.out.println(animal1 instanceof Object);  // true
```

### Pattern Matching (Java 14+)

```java
// Old style
if (animal instanceof Dog) {
    Dog dog = (Dog) animal;  // Manual cast
    dog.fetch();
}

// New style (Java 14+) — automatic cast
if (animal instanceof Dog dog) {
    dog.fetch();  // No manual cast needed
}

// Multiple types
public void handle(Object obj) {
    if (obj instanceof String s) {
        System.out.println("String length: " + s.length());
    } else if (obj instanceof Integer i) {
        System.out.println("Integer value: " + i);
    } else if (obj instanceof List<?> list) {
        System.out.println("List size: " + list.size());
    }
}
```

### instanceof with null

```java
String str = null;
System.out.println(str instanceof String);  // false

// instanceof null pe false return karta hai — safe check
if (obj instanceof String s) {
    // yeh block tabhi execute hoga jab obj non-null String ho
}
```

### instanceof in equals()

```java
class User {
    private String name;
    private int age;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User other)) return false;

        return this.name.equals(other.name) && this.age == other.age;
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Polymorphism** | Ek object ka multiple forms |
| **Compile-time** | Method overloading — same name, different parameters |
| **Runtime** | Method overriding — same name, same parameters, different implementation |
| **Dynamic Method Dispatch** | Parent reference se child method call — runtime pe decide |
| **Upcasting** | Child → Parent — automatic, safe |
| **Downcasting** | Parent → Child — manual `(Type)`, risky (ClassCastException) |
| **instanceof** | Object type check karna — Java 14+ pattern matching |
| **Overloading Rules** | Parameters different hone chahiye — return type alone kaafi nahi |
| **Overriding Rules** | Same signature, same/broader access, narrower exceptions |
