# Abstraction

## Status: Not Started

---

## Table of Contents

1. [Abstract Classes](#abstract-classes)
2. [Abstract Methods](#abstract-methods)
3. [Abstract Class vs Interface](#abstract-class-vs-interface)
4. [Template Method Pattern](#template-method-pattern)

---

## Abstract Classes

**Matlab:** Aisi class jo incomplete hai — directly instantiate nahi ho sakti, sirf extend karke use ho sakti hai.

### Abstract Class Declaration

```java
// abstract keyword se declare karo
abstract class Shape {
    String color;

    // Concrete method — implementation ke saath
    public void setColor(String color) {
        this.color = color;
    }

    public String getColor() {
        return color;
    }

    // Abstract method — sirf declaration, implementation nahi
    public abstract double getArea();

    public abstract double getPerimeter();
}
```

### Cannot Instantiate

```java
// ❌ Abstract class ka object nahi bana sakte
Shape shape = new Shape();  // ❌ Error: Shape is abstract

// ✅ Concrete subclass ka object banao
class Circle extends Shape {
    private double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }

    @Override
    public double getPerimeter() {
        return 2 * Math.PI * radius;
    }
}

class Rectangle extends Shape {
    private double width;
    private double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public double getArea() {
        return width * height;
    }

    @Override
    public double getPerimeter() {
        return 2 * (width + height);
    }
}

// Usage
Shape circle = new Circle(5);
System.out.println(circle.getArea());       // 78.54
System.out.println(circle.getColor());      // null (inherited)

Shape rectangle = new Rectangle(4, 6);
System.out.println(rectangle.getArea());    // 24.0
```

### Abstract Class Features

```java
abstract class Animal {
    // Fields
    protected String name;
    protected int age;

    // Constructor
    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Concrete methods
    public void eat() {
        System.out.println(name + " is eating");
    }

    public void sleep() {
        System.out.println(name + " is sleeping");
    }

    // Abstract methods — subclasses ko implement karna padega
    public abstract void makeSound();
    public abstract void move();
}
```

---

## Abstract Methods

**Matlab:** Sirf method declaration — implementation subclass mein hota hai.

### Rules

```java
abstract class Animal {
    // ✅ Abstract method — no body, semicolon
    public abstract void makeSound();

    // ✅ Abstract method with return type
    public abstract int getLegCount();

    // ✅ Abstract method with parameters
    public abstract void feed(String food, int amount);

    // ❌ Abstract method ka body nahi ho sakta
    // public abstract void makeSound() { }  // ❌ Error
}
```

### Subclass Must Implement

```java
abstract class Animal {
    public abstract void makeSound();
}

// ❌ Concrete subclass MUST implement all abstract methods
class Dog extends Animal {
    @Override
    public void makeSound() {
        System.out.println("Bark!");
    }
}

// ✅ Agar subclass bhi abstract hai toh implement karna zaruri nahi
abstract class Pet extends Animal {
    // makeSound() implement nahi kiya — allowed kyunki Pet bhi abstract hai
    public abstract void play();
}

// ❌ Concrete subclass — sab implement karna padega
class Cat extends Pet {
    @Override
    public void makeSound() {
        System.out.println("Meow!");
    }

    @Override
    public void play() {
        System.out.println("Playing with yarn");
    }
}
```

---

## Abstract Class vs Interface

### Comparison

| Feature | Abstract Class | Interface |
|---------|---------------|-----------|
| **Keyword** | `abstract class` | `interface` |
| **Methods** | Abstract + Concrete | Abstract (default), + `default`, `static`, `private` |
| **Fields** | Any type | `public static final` (constants only) |
| **Constructors** | ✅ Yes | ❌ No |
| **Inheritance** | `extends` (single) | `implements` (multiple) |
| **Access Modifiers** | Any | `public` (default) |
| **State** | Can have state (instance fields) | No state (only constants) |
| **Use Case** | "is-a" relationship + shared code | "can-do" capability / contract |

### When to Use Abstract Class

```java
// ✅ Use abstract class when:
// 1. Common code share karna ho
// 2. "is-a" relationship ho
// 3. State (fields) chahiye
// 4. Constructor chahiye

abstract class DatabaseRepository {
    protected DataSource dataSource;

    public DatabaseRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // Common method — sab subclasses ke liye same
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // Abstract method — har subclass ka alag implementation
    public abstract void save(Object entity);
    public abstract Object findById(Long id);
}

class UserRepository extends DatabaseRepository {
    public UserRepository(DataSource dataSource) {
        super(dataSource);  // Parent constructor
    }

    @Override
    public void save(Object entity) {
        // User-specific save logic
    }

    @Override
    public Object findById(Long id) {
        // User-specific find logic
    }
}
```

### When to Use Interface

```java
// ✅ Use interface when:
// 1. Sirf contract define karna ho
// 2. Multiple inheritance chahiye
// 3. No state needed
// 4. Unrelated classes ko same capability deni ho

interface Printable {
    void print();
}

interface Scannable {
    void scan();
}

class Printer implements Printable, Scannable {  // Multiple interfaces
    @Override
    public void print() {
        System.out.println("Printing...");
    }

    @Override
    public void scan() {
        System.out.println("Scanning...");
    }
}

// Unrelated classes — same capability
class Document implements Printable {
    @Override
    public void print() {
        System.out.println("Printing document");
    }
}

class Photo implements Printable {
    @Override
    public void print() {
        System.out.println("Printing photo");
    }
}
```

### Combined Approach

```java
// Interface — capability define karo
interface Payable {
    void processPayment(double amount);
}

// Abstract class — common code + interface implement karo
abstract class OnlinePayment implements Payable {
    protected String gateway;

    public OnlinePayment(String gateway) {
        this.gateway = gateway;
    }

    // Common method
    protected void logTransaction(double amount) {
        System.out.println("Processing " + amount + " via " + gateway);
    }

    // Abstract — subclass implement karega
    public abstract void processPayment(double amount);
}

// Concrete class
class CreditCardPayment extends OnlinePayment {
    private String cardNumber;

    public CreditCardPayment(String cardNumber) {
        super("CreditCardGateway");
        this.cardNumber = cardNumber;
    }

    @Override
    public void processPayment(double amount) {
        logTransaction(amount);  // Parent ka method
        System.out.println("Charging card: " + cardNumber);
    }
}
```

---

## Template Method Pattern

**Matlab:** Algorithm ka skeleton parent class mein define karo — kuch steps subclasses ko implement karne do.

### Basic Template Method

```java
abstract class DataParser {

    // Template method — final taaki override na ho
    public final void parseAndProcess(String filePath) {
        // Step 1: Open file
        openFile(filePath);

        // Step 2: Parse data (subclass implement karega)
        Data data = parseData(filePath);

        // Step 3: Validate data
        validateData(data);

        // Step 4: Process data (subclass implement karega)
        processData(data);

        // Step 5: Close file
        closeFile();
    }

    // Concrete methods — common logic
    private void openFile(String filePath) {
        System.out.println("Opening file: " + filePath);
    }

    private void validateData(Data data) {
        if (data == null) {
            throw new IllegalArgumentException("Invalid data");
        }
    }

    private void closeFile() {
        System.out.println("File closed");
    }

    // Abstract methods — subclass implement karega
    protected abstract Data parseData(String filePath);
    protected abstract void processData(Data data);
}

// Subclass 1: CSV Parser
class CsvParser extends DataParser {
    @Override
    protected Data parseData(String filePath) {
        System.out.println("Parsing CSV file...");
        return new Data("CSV Data");
    }

    @Override
    protected void processData(Data data) {
        System.out.println("Processing CSV data: " + data.getContent());
    }
}

// Subclass 2: JSON Parser
class JsonParser extends DataParser {
    @Override
    protected Data parseData(String filePath) {
        System.out.println("Parsing JSON file...");
        return new Data("JSON Data");
    }

    @Override
    protected void processData(Data data) {
        System.out.println("Processing JSON data: " + data.getContent());
    }
}

// Usage
DataParser csvParser = new CsvParser();
csvParser.parseAndProcess("data.csv");
// Output:
// Opening file: data.csv
// Parsing CSV file...
// Processing CSV data: CSV Data
// File closed

DataParser jsonParser = new JsonParser();
jsonParser.parseAndProcess("data.json");
// Output:
// Opening file: data.json
// Parsing JSON file...
// Processing JSON data: JSON Data
// File closed
```

### Template Method with Hooks

```java
abstract class Game {

    // Template method
    public final void play() {
        initialize();
        startPlay();
        endPlay();
    }

    // Abstract methods
    protected abstract void initialize();
    protected abstract void startPlay();
    protected abstract void endPlay();

    // Hook method — optional override
    protected boolean isGameFinished() {
        return true;  // Default behavior
    }
}

class Cricket extends Game {
    @Override
    protected void initialize() {
        System.out.println("Cricket game initialized");
    }

    @Override
    protected void startPlay() {
        System.out.println("Cricket game started");
    }

    @Override
    protected void endPlay() {
        System.out.println("Cricket game ended");
    }
}

class Football extends Game {
    @Override
    protected void initialize() {
        System.out.println("Football game initialized");
    }

    @Override
    protected void startPlay() {
        System.out.println("Football game started");
    }

    @Override
    protected void endPlay() {
        System.out.println("Football game ended");
    }

    @Override
    protected boolean isGameFinished() {
        return false;  // Override hook
    }
}
```

### Real-World Example: Spring Framework

```java
// Spring ka JdbcTemplate — Template Method pattern use karta hai

jdbcTemplate.query("SELECT * FROM users", new RowMapper<User>() {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        // Aap sirf row mapping implement karo
        // Spring baaki kaam (connection, statement, cleanup) khud karega
        return new User(rs.getLong("id"), rs.getString("name"));
    }
});

// Internally JdbcTemplate:
// 1. Connection open karta hai (common)
// 2. Statement create karta hai (common)
// 3. ResultSet iterate karta hai (common)
// 4. RowMapper.mapRow() call karta hai (your code) — Template Method hook
// 5. Connection close karta hai (common)
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Abstract Class** | Directly instantiate nahi ho sakti — extend karna padta hai |
| **Abstract Method** | Sirf declaration — subclass ko implement karna padta hai |
| **Concrete Subclass** | Sab abstract methods implement karna zaruri hai |
| **Abstract Class vs Interface** | Abstract = "is-a" + shared code, Interface = "can-do" + contract |
| **Template Method** | Algorithm skeleton parent mein — steps subclass implement kare |
| **Hook Methods** | Optional override — default behavior change kar sakte ho |
