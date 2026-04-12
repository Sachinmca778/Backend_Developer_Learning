# Lambda Expressions

## Status: Not Started

---

## Table of Contents

1. [Lambda Syntax](#lambda-syntax)
2. [Method References](#method-references)
3. [Effectively Final Variables](#effectively-final-variables)
4. [Lambda with Functional Interfaces](#lambda-with-functional-interfaces)
5. [Lambda Best Practices](#lambda-best-practices)

---

## Lambda Syntax

**Matlab:** Anonymous function — bina naam ka function jo functional interface ko implement karta hai.

### Basic Syntax

```java
// (parameters) -> expression
// (parameters) -> { statements; }
```

### Syntax Variations

```java
// ✅ No parameters → ()
Runnable r = () -> System.out.println("Running");

// ✅ Single parameter → parentheses optional
Consumer<String> print = s -> System.out.println(s);
Consumer<String> print2 = (s) -> System.out.println(s);

// ✅ Multiple parameters → parentheses required
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;

// ✅ Single expression → return implicit
Function<Integer, Integer> square = x -> x * x;

// ✅ Block body → return explicit
Function<Integer, Integer> squareBlock = x -> {
    return x * x;
};

// ✅ Multiple statements → block required
Consumer<String> process = s -> {
    String trimmed = s.trim();
    System.out.println(trimmed.toUpperCase());
};
```

### Examples

```java
// Comparator
Comparator<String> byLength = (s1, s2) -> s1.length() - s2.length();

// Predicate
Predicate<String> isEmpty = s -> s == null || s.isBlank();

// Function
Function<String, Integer> parse = s -> Integer.parseInt(s);

// Consumer
Consumer<String> logger = msg -> System.out.println("[LOG] " + msg);

// Supplier
Supplier<Double> random = () -> Math.random();

// BinaryOperator
BinaryOperator<Integer> multiply = (a, b) -> a * b;
```

### Lambda vs Anonymous Class

```java
// Anonymous class — verbose
Runnable r1 = new Runnable() {
    @Override
    public void run() {
        System.out.println("Running");
    }
};

// Lambda — concise
Runnable r2 = () -> System.out.println("Running");

// Comparator comparison
// Anonymous class
Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return s1.length() - s2.length();
    }
});

// Lambda
Collections.sort(names, (s1, s2) -> s1.length() - s2.length());

// Even better — method reference
Collections.sort(names, Comparator.comparingInt(String::length));
```

---

## Method References

**Matlab:** Lambda ka short form — existing method ko directly reference karna.

### Types of Method References

| Type | Syntax | Example |
|------|--------|---------|
| **Static Method** | `ClassName::methodName` | `Integer::parseInt` |
| **Instance Method (specific object)** | `object::methodName` | `System.out::println` |
| **Instance Method (arbitrary object)** | `ClassName::methodName` | `String::length` |
| **Constructor** | `ClassName::new` | `String::new` |

### Static Method Reference

```java
// Lambda
Function<String, Integer> parse1 = s -> Integer.parseInt(s);

// Method reference
Function<String, Integer> parse2 = Integer::parseInt;

// Usage
Integer num = parse2.apply("123");  // 123

// More examples
Function<Double, String> toString = String::valueOf;
Comparator<String> ignoreCase = String::compareToIgnoreCase;
Function<String, String> upper = String::toUpperCase;
```

### Instance Method (Specific Object)

```java
// Lambda
Consumer<String> print1 = s -> System.out.println(s);

// Method reference
Consumer<String> print2 = System.out::println;

// Usage
print2.accept("Hello");  // "Hello"

// More examples
StringBuilder sb = new StringBuilder();
Consumer<String> appender = sb::append;
appender.accept("Hello");
appender.accept(" World");
System.out.println(sb);  // "Hello World"
```

### Instance Method (Arbitrary Object)

```java
// Lambda — type ka method call hoga
Function<String, Integer> length1 = s -> s.length();

// Method reference — String type pe method call
Function<String, Integer> length2 = String::length;

// Usage
System.out.println(length2.apply("Hello"));  // 5

// More examples
Predicate<String> isEmpty = String::isEmpty;
Function<String, String> trimmed = String::trim;
Function<String, String[]> splitOnSpace = String::split;  // Needs delimiter argument
```

### Constructor Reference

```java
// Lambda
Supplier<List<String>> createList1 = () -> new ArrayList<>();

// Constructor reference
Supplier<List<String>> createList2 = ArrayList::new;

// Usage
List<String> list = createList2.get();
list.add("Hello");

// Function — one argument constructor
Function<String, User> createUser = User::new;
User user = createUser.apply("Sachin");

// BiFunction — two argument constructor
BiFunction<String, Integer, User> createUser2 = User::new;
User user2 = createUser2.apply("Sachin", 25);

// Array constructor
Function<Integer, String[]> createArray = String[]::new;
String[] arr = createArray.apply(5);  // new String[5]
```

### Method Reference with Existing Object

```java
class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
}

Calculator calc = new Calculator();

// Lambda
BiFunction<Integer, Integer, Integer> add1 = (a, b) -> calc.add(a, b);

// Method reference (specific object)
BiFunction<Integer, Integer, Integer> add2 = calc::add;

System.out.println(add2.apply(5, 10));  // 15
```

---

## Effectively Final Variables

**Matlab:** Variable jo explicitly `final` nahi hai lekin ek baar assign hone ke baad change nahi hota — lambda ke andar accessible.

### Rules

```java
// ✅ Explicitly final
final String message = "Hello";
Runnable r1 = () -> System.out.println(message);

// ✅ Effectively final — ek baar assign, baad mein change nahi
String effectivelyFinal = "Hello";
Runnable r2 = () -> System.out.println(effectivelyFinal);

// ❌ Not final — change ho raha hai
String notFinal = "Hello";
notFinal = "World";  // Modified
Runnable r3 = () -> System.out.println(notFinal);  // ❌ Error!
```

### Why This Restriction?

```
Lambda ko execute hone pe variable ka value chahiye
Variable stack pe ho sakta hai — lambda baad mein execute ho
Agar variable change ho gaya toh lambda ko kaunsa value milega?
Solution: Sirf final/effectively final variables allow karo
```

### Common Patterns

```java
// ✅ Loop variable — effectively final per iteration
List<String> names = List.of("Sachin", "Rahul", "Priya");
names.forEach(name -> System.out.println(name));

// ✅ Capturing local variable
String prefix = "[LOG]";
Consumer<String> logger = msg -> System.out.println(prefix + msg);

// ❌ Modifying captured variable
int count = 0;
names.forEach(name -> count++);  // ❌ Error! count effectively final nahi hai

// ✅ Workaround — Atomic variable use karo
AtomicInteger count = new AtomicInteger(0);
names.forEach(name -> count.incrementAndGet());
System.out.println(count.get());  // 3
```

### Instance Variables

```java
class MyClass {
    private String instanceVar = "Hello";

    public void doSomething() {
        // Instance variables capture nahi hote — this reference capture hota hai
        Runnable r = () -> System.out.println(this.instanceVar);

        this.instanceVar = "World";
        r.run();  // "World" — instance variable ka current value
    }
}
```

---

## Lambda with Functional Interfaces

```java
// Runnable — no params, no return
Runnable r = () -> System.out.println("Running");
new Thread(r).start();

// Callable — no params, returns value
Callable<String> c = () -> "Result";

// Comparator — two params, returns int
Comparator<String> comp = (a, b) -> a.length() - b.length();

// Predicate — one param, returns boolean
Predicate<String> isEmpty = String::isEmpty;

// Consumer — one param, no return
Consumer<String> print = System.out::println;

// Supplier — no params, returns value
Supplier<Double> random = Math::random;

// Function — one param, returns value
Function<String, Integer> parseInt = Integer::parseInt;

// BiFunction — two params, returns value
BiFunction<String, String, Integer> findIndex = String::indexOf;
```

---

## Lambda Best Practices

```java
// ✅ Keep lambdas short — 1-2 lines
names.stream().filter(n -> n.length() > 3).collect(Collectors.toList());

// ❌ Avoid complex logic in lambda
names.stream().filter(n -> {
    if (n == null) return false;
    String trimmed = n.trim();
    if (trimmed.isEmpty()) return false;
    return trimmed.length() > 3;
}).collect(Collectors.toList());

// ✅ Extract to method
names.stream().filter(this::isValidName).collect(Collectors.toList());

private boolean isValidName(String name) {
    return name != null && !name.trim().isEmpty() && name.trim().length() > 3;
}

// ✅ Use method references when possible
list.forEach(System.out::println);  // Better than: list.forEach(s -> System.out.println(s));

// ✅ Type inference rely karo — explicit types mat likho
list.sort((a, b) -> a.compareTo(b));  // Better than: list.sort((String a, String b) -> a.compareTo(b));
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Lambda Syntax** | `(params) -> expression` ya `(params) -> { body }` |
| **Static Method Ref** | `ClassName::method` — `Integer::parseInt` |
| **Instance Method Ref** | `object::method` — `System.out::println` |
| **Arbitrary Object Method Ref** | `ClassName::method` — `String::length` |
| **Constructor Ref** | `ClassName::new` — `ArrayList::new` |
| **Effectively Final** | Ek baar assign, baad mein change nahi — lambda mein accessible |
| **this in Lambda** | Enclosing class ka `this` reference — lambda ka nahi |
