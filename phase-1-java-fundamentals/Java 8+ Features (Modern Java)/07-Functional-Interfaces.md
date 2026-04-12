# Functional Interfaces

## Status: Not Started

---

## Table of Contents

1. [Function&lt;T,R&gt; & BiFunction](#functiontr--bifunction)
2. [Predicate&lt;T&gt; & BiPredicate](#predicatet--bipredicate)
3. [Consumer&lt;T&gt; & Supplier&lt;T&gt;](#consumert--suppliert)
4. [UnaryOperator & BinaryOperator](#unaryoperator--binaryoperator)
5. [Composing Functions](#composing-functions)

---

## Function&lt;T,R&gt; & BiFunction

**Matlab:** T input → R output — transformation ke liye.

### Function&lt;T, R&gt;

```java
// String → Integer
Function<String, Integer> parse = Integer::parseInt;
Integer num = parse.apply("123");  // 123

// Integer → String
Function<Integer, String> toString = Object::toString;
String str = toString.apply(456);  // "456"

// User → Name
Function<User, String> getName = User::getName;
String name = getName.apply(new User("Sachin", 25));  // "Sachin"
```

### BiFunction&lt;T, U, R&gt;

```java
// Two inputs → one output
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
Integer sum = add.apply(5, 10);  // 15

BiFunction<String, String, Integer> findIndex = String::indexOf;
Integer idx = findIndex.apply("Hello", "e");  // 1

// String → String → Integer
BiFunction<String, Integer, String> repeat = (str, count) -> str.repeat(count);
String repeated = repeat.apply("Hi", 3);  // "HiHiHi"
```

---

## Predicate&lt;T&gt; & BiPredicate

**Matlab:** T input → boolean output — condition check ke liye.

### Predicate&lt;T&gt;

```java
// String check
Predicate<String> isEmpty = String::isEmpty;
boolean result = isEmpty.test("");  // true

Predicate<String> isLong = s -> s.length() > 10;
boolean isLongStr = isLong.test("This is a long string");  // false

// Integer check
Predicate<Integer> isEven = n -> n % 2 == 0;
boolean even = isEven.test(4);  // true

// User check
Predicate<User> isAdult = u -> u.getAge() >= 18;
boolean adult = isAdult.test(new User("Sachin", 25));  // true
```

### BiPredicate&lt;T, U&gt;

```java
BiPredicate<String, String> startsWith = String::startsWith;
boolean result = startsWith.test("Hello", "He");  // true

BiPredicate<Integer, Integer> isDivisible = (a, b) -> a % b == 0;
boolean divisible = isDivisible.test(10, 5);  // true
```

---

## Consumer&lt;T&gt; & Supplier&lt;T&gt;

### Consumer&lt;T&gt;

**Matlab:** T input → no return — side effect ke liye.

```java
// Print
Consumer<String> print = System.out::println;
print.accept("Hello");  // "Hello"

// Logger
Consumer<String> logger = msg -> System.out.println("[LOG] " + msg);
logger.accept("Something happened");  // "[LOG] Something happened"

// BiConsumer — two inputs
BiConsumer<String, Integer> logWithLevel = (level, msg) -> 
    System.out.println("[" + level + "] " + msg);
logWithLevel.accept("ERROR", "Something failed");  // "[ERROR] Something failed"
```

### Supplier&lt;T&gt;

**Matlab:** No input → T output — factory/lazy initialization ke liye.

```java
// Random number
Supplier<Double> random = Math::random;
Double value = random.get();  // 0.123...

// Current time
Supplier<Long> currentTime = System::currentTimeMillis;
Long millis = currentTime.get();  // 1705309200000

// Factory
Supplier<User> defaultUser = () -> new User("Unknown", 0);
User user = defaultUser.get();  // User("Unknown", 0)

// Lazy initialization
Supplier<ExpensiveObject> lazy = () -> new ExpensiveObject();
ExpensiveObject obj = lazy.get();  // Sirf jab chahiye tab create hoga
```

---

## UnaryOperator & BinaryOperator

### UnaryOperator&lt;T&gt;

**Matlab:** T input → T output — same type transformation.

```java
// String → String
UnaryOperator<String> upper = String::toUpperCase;
String result = upper.apply("hello");  // "HELLO"

// Integer → Integer
UnaryOperator<Integer> square = n -> n * n;
Integer squared = square.apply(5);  // 25

// negate — Java 9+
Predicate<Integer> isEven = n -> n % 2 == 0;
Predicate<Integer> isOdd = isEven.negate();
boolean odd = isOdd.test(5);  // true
```

### BinaryOperator&lt;T&gt;

**Matlab:** T, T input → T output — same type binary operation.

```java
// Integer addition
BinaryOperator<Integer> add = (a, b) -> a + b;
Integer sum = add.apply(5, 10);  // 15

// String concatenation
BinaryOperator<String> concat = String::concat;
String joined = concat.apply("Hello", " World");  // "Hello World"

// min/max by
BinaryOperator<Integer> minOp = BinaryOperator.minBy(Integer::compare);
Integer min = minOp.apply(5, 10);  // 5

BinaryOperator<Integer> maxOp = BinaryOperator.maxBy(Integer::compare);
Integer max = maxOp.apply(5, 10);  // 10
```

---

## Composing Functions

### andThen

```java
// Function chaining — pehle this, phir after
Function<Integer, Integer> doubleIt = n -> n * 2;
Function<Integer, Integer> addTen = n -> n + 10;

Function<Integer, Integer> composed = doubleIt.andThen(addTen);
Integer result = composed.apply(5);  // 5 * 2 + 10 = 20

// Equivalent to: addTen.apply(doubleIt.apply(5))

// String example
Function<String, String> trim = String::trim;
Function<String, String> upper = String::toUpperCase;

Function<String, String> trimAndUpper = trim.andThen(upper);
String result2 = trimAndUpper.apply("  hello  ");  // "HELLO"
```

### compose

```java
// Function chaining — pehle before, phir this
Function<Integer, Integer> doubleIt = n -> n * 2;
Function<Integer, Integer> addTen = n -> n + 10;

Function<Integer, Integer> composed = doubleIt.compose(addTen);
Integer result = composed.apply(5);  // (5 + 10) * 2 = 30

// Equivalent to: doubleIt.apply(addTen.apply(5))
```

### andThen vs compose

```
f.andThen(g) = g(f(x))  — pehle f, phir g
f.compose(g) = f(g(x))  — pehle g, phir f
```

### Predicate Composition

```java
Predicate<Integer> isEven = n -> n % 2 == 0;
Predicate<Integer> isPositive = n -> n > 0;
Predicate<Integer> isLessThan100 = n -> n < 100;

// AND
Predicate<Integer> isEvenAndPositive = isEven.and(isPositive);
boolean result = isEvenAndPositive.test(10);  // true

// OR
Predicate<Integer> isEvenOrPositive = isEven.or(isPositive);
boolean result2 = isEvenOrPositive.test(-5);  // true (positive nahi, but... wait -5 is not positive)
// Actually: -5 % 2 != 0 (not even) AND -5 > 0 (not positive) → false

// NEGATE
Predicate<Integer> isNotEven = isEven.negate();
boolean result3 = isNotEven.test(5);  // true
```

### Consumer Composition

```java
Consumer<String> printUpper = s -> System.out.println(s.toUpperCase());
Consumer<String> printLower = s -> System.out.println(s.toLowerCase());

Consumer<String> both = printUpper.andThen(printLower);
both.accept("Hello");
// "HELLO"
// "hello"
```

---

## Summary

| Interface | Input | Output | Purpose |
|-----------|-------|--------|---------|
| **Function&lt;T,R&gt;** | T | R | Transform |
| **BiFunction&lt;T,U,R&gt;** | T, U | R | Two inputs → output |
| **Predicate&lt;T&gt;** | T | boolean | Condition check |
| **BiPredicate&lt;T,U&gt;** | T, U | boolean | Two inputs → boolean |
| **Consumer&lt;T&gt;** | T | void | Side effect |
| **BiConsumer&lt;T,U&gt;** | T, U | void | Two inputs → side effect |
| **Supplier&lt;T&gt;** | void | T | Factory/lazy |
| **UnaryOperator&lt;T&gt;** | T | T | Same type transform |
| **BinaryOperator&lt;T&gt;** | T, T | T | Same type binary op |
| **andThen** | — | — | Compose: f then g |
| **compose** | — | — | Compose: g then f |
| **negate** | — | — | Predicate invert |
