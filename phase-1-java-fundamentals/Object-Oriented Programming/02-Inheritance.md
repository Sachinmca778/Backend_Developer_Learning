# Inheritance

## Status: Not Started

---

## Table of Contents

1. [extends Keyword](#extends-keyword)
2. [Method Overriding (@Override)](#method-overriding-override)
3. [super Keyword](#super-keyword)
4. [Calling Parent Constructor](#calling-parent-constructor)
5. [final class/method](#final-classmethod)
6. [Covariant Return Types](#covariant-return-types)
7. [Constructor Inheritance Rules](#constructor-inheritance-rules)

---

## extends Keyword

**Matlab:** Ek class ko dusri class ki properties aur methods inherit karne deta hai — code reusability ke liye.

### Basic Inheritance

```java
// Parent class (Base/Super class)
class Animal {
    String name;
    int age;

    public void eat() {
        System.out.println(name + " is eating");
    }

    public void sleep() {
        System.out.println(name + " is sleeping");
    }
}

// Child class (Derived/Sub class)
class Dog extends Animal {
    // Dog ko Animal ke saare fields aur methods mil jaate hain
    public void bark() {
        System.out.println(name + " is barking");  // name inherited hai
    }
}

// Usage
Dog dog = new Dog();
dog.name = "Buddy";  // Inherited field
dog.age = 3;         // Inherited field
dog.eat();           // Inherited method
dog.bark();          // Dog ka apna method
```

### Inheritance Hierarchy

```
Object (root of all classes)
    ↑
Animal
    ↑
    ├── Dog
    │     ├── Labrador
    │     └── Bulldog
    ├── Cat
    └── Bird
```

### Java Inheritance Rules

```java
// ✅ Single inheritance supported
class Dog extends Animal { }

// ❌ Multiple inheritance NOT supported
class Dog extends Animal, Pet { }  // ❌ Error!

// ✅ Multi-level inheritance supported
class Animal { }
class Dog extends Animal { }
class Labrador extends Dog { }  // Labrador → Dog → Animal

// ✅ Single class can extend only one class
// But multiple interfaces implement kar sakta hai
class Dog extends Animal implements Pet, Trainable { }
```

### What is Inherited?

```java
class Parent {
    public int publicField;       // ✅ Inherited
    protected int protectedField; // ✅ Inherited
    int defaultField;             // ✅ Inherited (same package)
    private int privateField;     // ❌ NOT inherited

    public void publicMethod() {}    // ✅ Inherited
    protected void protectedMethod() {} // ✅ Inherited
    void defaultMethod() {}          // ✅ Inherited (same package)
    private void privateMethod() {}  // ❌ NOT inherited
}

class Child extends Parent {
    // publicField, protectedField, defaultField → inherited
    // privateField → NOT inherited (but exists in memory)

    // publicMethod(), protectedMethod(), defaultMethod() → inherited
    // privateMethod() → NOT inherited
}
```

---

## Method Overriding (@Override)

**Matlab:** Child class parent class ke method ko redefine karti hai — same signature, different implementation.

### Basic Overriding

```java
class Animal {
    public void makeSound() {
        System.out.println("Some generic sound");
    }
}

class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Bark! Bark!");
    }
}

class Cat extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Meow! Meow!");
    }
}

// Usage
Animal animal = new Animal();
animal.makeSound();  // "Some generic sound"

Dog dog = new Dog();
dog.makeSound();     // "Bark! Bark!"

Cat cat = new Cat();
cat.makeSound();     // "Meow! Meow!"
```

### @Override Annotation

```java
class Dog extends Animal {
    @Override  // Compiler ko batao — yeh method override ho raha hai
    public void makeSound() {
        System.out.println("Bark!");
    }
}
```

**Benefits of @Override:**
1. Compile-time check — method signature match hona chahiye
2. Readability — clearly dikhta hai ki override ho raha hai
3. Safety — typo pe compiler error dega

```java
// ❌ Without @Override — typo silently ignored
class Dog extends Animal {
    public void makesSound() { }  // Naya method bana, override nahi hua!
}

// ✅ With @Override — compiler error
class Dog extends Animal {
    @Override
    public void makesSound() { }  // ❌ Error! Parent mein aisa method nahi hai
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
    @Override
    public void method1() { }       // ✅ Same access

    @Override
    public void method2() { }       // ✅ Broader (protected → public)

    @Override
    public void method3() { }       // ✅ Broader (default → public)

    // ❌ Cannot override private method
    // @Override
    // public void method4() { }    // ❌ Private methods inherit nahi hote

    // ❌ Cannot override static methods (hidden, not overridden)
    // @Override
    // public static void method5() { }  // ❌ Static methods override nahi hote

    // ❌ Cannot override final methods
    // @Override
    // public void method6() { }    // ❌ Final methods override nahi hote
}
```

---

## super Keyword

**Matlab:** Parent class ko reference karta hai — parent ke fields, methods, aur constructor ko access karne ke liye.

### super for Fields

```java
class Parent {
    String name = "Parent";
}

class Child extends Parent {
    String name = "Child";

    public void display() {
        System.out.println(name);         // "Child" (current class)
        System.out.println(super.name);   // "Parent" (parent class)
    }
}
```

### super for Methods

```java
class Animal {
    public void eat() {
        System.out.println("Animal is eating");
    }
}

class Dog extends Animal {
    @Override
    public void eat() {
        super.eat();  // Parent ka method call karo
        System.out.println("Dog is eating");
    }
}

// Usage
Dog dog = new Dog();
dog.eat();
// Output:
// Animal is eating
// Dog is eating
```

---

## Calling Parent Constructor

**Matlab:** `super()` se parent class ka constructor call karna.

### Basic super() Call

```java
class Person {
    String name;
    int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
        System.out.println("Person constructor called");
    }
}

class Student extends Person {
    String studentId;

    public Student(String name, int age, String studentId) {
        super(name, age);  // Parent constructor call
        this.studentId = studentId;
        System.out.println("Student constructor called");
    }
}

// Usage
Student student = new Student("Sachin", 25, "STU001");
// Output:
// Person constructor called
// Student constructor called
```

### super() Rules

```java
// ✅ super() must be first statement
public Student(String name, int age, String studentId) {
    super(name, age);  // First line pe hona chahiye
    this.studentId = studentId;
}

// ❌ Error — super() first nahi hai
public Student(String name, int age, String studentId) {
    System.out.println("Start");
    super(name, age);  // ❌ Error!
}

// ✅ Implicit super() call (no-arg constructor)
class Person {
    public Person() {
        System.out.println("Person default");
    }
}

class Student extends Person {
    public Student() {
        // super() automatically call hoga
        System.out.println("Student default");
    }
}

// ❌ Error — parent ka default constructor nahi hai
class Person {
    public Person(String name) { }  // Only parameterized constructor
}

class Student extends Person {
    public Student() {
        // super() call hoga — lekin parent ka default constructor nahi hai!
        // ❌ Compilation Error
    }
}
```

---

## final class/method

### final class

**Matlab:** Class ko extend nahi kar sakte — inheritance band.

```java
final class MathUtils {
    public static double PI = 3.14159;

    public static double add(double a, double b) {
        return a + b;
    }
}

// ❌ Cannot extend final class
class MyMath extends MathUtils { }  // ❌ Error!
```

**Use Cases:**
- Immutable classes (String, Integer, etc.)
- Security-critical classes
- Utility classes (sab static methods)

### final method

**Matlab:** Method ko override nahi kar sakte.

```java
class Animal {
    public final void breathe() {
        System.out.println("Breathing...");
    }

    public void makeSound() {
        System.out.println("Some sound");
    }
}

class Dog extends Animal {
    // ❌ Cannot override final method
    // @Override
    // public void breathe() { }  // ❌ Error!

    // ✅ Can override non-final method
    @Override
    public void makeSound() {
        System.out.println("Bark!");
    }
}
```

---

## Covariant Return Types

**Matlab:** Overriding method ka return type parent ke return type ka subclass ho sakta hai — Java 5+.

### Example

```java
class Animal {
    public Animal getAnimal() {
        return new Animal();
    }
}

class Dog extends Animal {
    @Override
    public Dog getAnimal() {  // ✅ Covariant return — Dog is subclass of Animal
        return new Dog();
    }
}

// Real-world example
class Shape {
    public Shape clone() {
        return new Shape();
    }
}

class Circle extends Shape {
    @Override
    public Circle clone() {  // ✅ Covariant return
        return new Circle();
    }
}

class Rectangle extends Shape {
    @Override
    public Rectangle clone() {  // ✅ Covariant return
        return new Rectangle();
    }
}
```

---

## Constructor Inheritance Rules

### Key Rules

```java
// 1. Constructors are NOT inherited
class Parent {
    public Parent(String name) { }
}

class Child extends Parent {
    // Parent ka constructor available nahi hai
    // Child ko apna constructor define karna padega
    public Child(String name) {
        super(name);  // Parent constructor call karo
    }
}

// 2. super() must be first statement (if used)
class Child extends Parent {
    public Child(String name) {
        super(name);  // First line
        // Additional initialization
    }
}

// 3. If parent has only parameterized constructors, child MUST call super()
class Parent {
    public Parent(String name) { }
    // No default constructor
}

class Child extends Parent {
    public Child(String name) {
        super(name);  // Must call parent constructor
    }

    // ❌ This won't compile
    public Child() {
        // Implicit super() call — but parent has no default constructor!
    }
}

// 4. Constructor chaining order
class GrandParent {
    public GrandParent() {
        System.out.println("GrandParent");
    }
}

class Parent extends GrandParent {
    public Parent() {
        System.out.println("Parent");
    }
}

class Child extends Parent {
    public Child() {
        System.out.println("Child");
    }
}

// Usage
new Child();
// Output:
// GrandParent  (top to bottom)
// Parent
// Child
```

### Constructor Flow Diagram

```
new Child()
    ↓
Child constructor calls super()
    ↓
Parent constructor calls super()
    ↓
GrandParent constructor calls super()
    ↓
Object constructor (implicit)
    ↓
GrandParent constructor body
    ↓
Parent constructor body
    ↓
Child constructor body
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **extends** | Ek class dusri class ki properties/methods inherit karti hai |
| **@Override** | Parent method ko redefine karo — compiler check ke saath |
| **super** | Parent class ko reference karo — fields, methods, constructor |
| **super()** | Parent constructor call karo — first statement hona chahiye |
| **final class** | Extend nahi ho sakta |
| **final method** | Override nahi ho sakta |
| **Covariant Return** | Overriding method ka return type subclass ho sakta hai |
| **Constructor Inheritance** | Constructors inherit nahi hote — child ko apna banana padta hai |
| **Constructor Order** | Parent → Child (top to bottom) |
