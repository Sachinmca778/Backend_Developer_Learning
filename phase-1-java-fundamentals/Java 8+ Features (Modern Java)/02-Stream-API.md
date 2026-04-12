# Stream API

## Status: Not Started

---

## Table of Contents

1. [Stream Creation](#stream-creation)
2. [Intermediate Operations](#intermediate-operations)
3. [Terminal Operations](#terminal-operations)
4. [Parallel Streams](#parallel-streams)
5. [Stream Best Practices](#stream-best-practices)

---

## Stream Creation

**Matlab:** Stream = data ka flow — elements pe functional operations perform karne ke liye.

### From Collection

```java
List<String> list = List.of("Apple", "Banana", "Cherry");
Stream<String> stream1 = list.stream();

Set<Integer> set = Set.of(1, 2, 3);
Stream<Integer> stream2 = set.stream();

Map<String, Integer> map = Map.of("A", 1, "B", 2);
Stream<Map.Entry<String, Integer>> stream3 = map.entrySet().stream();
```

### From Array

```java
String[] arr = {"Apple", "Banana", "Cherry"};
Stream<String> stream = Arrays.stream(arr);

int[] nums = {1, 2, 3, 4, 5};
IntStream intStream = Arrays.stream(nums);
```

### Using Stream.of()

```java
Stream<String> stream1 = Stream.of("Apple", "Banana", "Cherry");
Stream<Integer> stream2 = Stream.of(1, 2, 3, 4, 5);

// Single element
Stream<String> single = Stream.of("Only");
```

### Using Stream.iterate()

```java
// Infinite stream — limit karna padega
Stream<Integer> numbers = Stream.iterate(0, n -> n + 2);  // 0, 2, 4, 6, ...
List<Integer> first10 = numbers.limit(10).collect(Collectors.toList());
// [0, 2, 4, 6, 8, 10, 12, 14, 16, 18]

// With predicate (Java 9+) — stop condition
Stream<Integer> bounded = Stream.iterate(0, n -> n < 20, n -> n + 2);
List<Integer> result = bounded.collect(Collectors.toList());
// [0, 2, 4, 6, 8, 10, 12, 14, 16, 18]
```

### Using Stream.generate()

```java
// Infinite stream
Stream<Double> randoms = Stream.generate(Math::random);
List<Double> fiveRandoms = randoms.limit(5).collect(Collectors.toList());

// Supplier se
Stream<String> echoes = Stream.generate(() -> "Echo");
List<String> three = echoes.limit(3).collect(Collectors.toList());
// ["Echo", "Echo", "Echo"]
```

### Primitive Streams

```java
// IntStream
IntStream intStream = IntStream.range(1, 10);         // 1 to 9
IntStream intStream2 = IntStream.rangeClosed(1, 10);  // 1 to 10

// LongStream
LongStream longStream = LongStream.range(1, 100);

// DoubleStream
DoubleStream doubleStream = DoubleStream.generate(Math::random).limit(5);
```

---

## Intermediate Operations

**Matlab:** Stream ko transform/filter karte hain — ek naya Stream return hote hain, lazy evaluation.

### filter

```java
List<String> names = List.of("Sachin", "Rahul", "Priya", "Ankit", "Neha");

// Sirf 5+ character wale names
List<String> longNames = names.stream()
    .filter(n -> n.length() > 5)
    .collect(Collectors.toList());
// ["Sachin", "Rahul", "Priya"]

// Multiple filters
List<String> filtered = names.stream()
    .filter(n -> n.length() > 4)
    .filter(n -> n.startsWith("S"))
    .collect(Collectors.toList());
// ["Sachin"]
```

### map

```java
List<String> names = List.of("Sachin", "Rahul", "Priya");

// String → Length
List<Integer> lengths = names.stream()
    .map(String::length)
    .collect(Collectors.toList());
// [6, 5, 5]

// String → Uppercase
List<String> upper = names.stream()
    .map(String::toUpperCase)
    .collect(Collectors.toList());
// ["SACHIN", "RAHUL", "PRIYA"]

// Object → Property
List<User> users = List.of(new User("Sachin", 25), new User("Rahul", 30));
List<String> userNames = users.stream()
    .map(User::getName)
    .collect(Collectors.toList());
```

### flatMap

**Matlab:** Stream of streams ko flat stream mein convert karna.

```java
List<List<String>> nested = List.of(
    List.of("Apple", "Banana"),
    List.of("Cherry", "Date"),
    List.of("Elderberry")
);

// Flatten
List<String> flat = nested.stream()
    .flatMap(List::stream)
    .collect(Collectors.toList());
// ["Apple", "Banana", "Cherry", "Date", "Elderberry"]

// String words to characters
List<String> words = List.of("Hello", "World");
List<String> chars = words.stream()
    .flatMap(word -> Arrays.stream(word.split("")))
    .collect(Collectors.toList());
// ["H", "e", "l", "l", "o", "W", "o", "r", "l", "d"]

// Optional handling
List<Optional<String>> optionals = List.of(
    Optional.of("Apple"),
    Optional.empty(),
    Optional.of("Cherry")
);
List<String> values = optionals.stream()
    .flatMap(Optional::stream)
    .collect(Collectors.toList());
// ["Apple", "Cherry"]
```

### distinct

```java
List<Integer> nums = List.of(1, 2, 2, 3, 3, 3, 4);
List<Integer> unique = nums.stream()
    .distinct()
    .collect(Collectors.toList());
// [1, 2, 3, 4]
```

### sorted

```java
List<String> names = List.of("Sachin", "Rahul", "Priya", "Ankit");

// Natural ordering
List<String> sorted = names.stream()
    .sorted()
    .collect(Collectors.toList());
// ["Ankit", "Priya", "Rahul", "Sachin"]

// Custom comparator
List<String> byLength = names.stream()
    .sorted(Comparator.comparingInt(String::length))
    .collect(Collectors.toList());
// ["Rahul", "Priya", "Ankit", "Sachin"] (all same length except Sachin)

// Reverse order
List<String> reverse = names.stream()
    .sorted(Comparator.reverseOrder())
    .collect(Collectors.toList());
// ["Sachin", "Rahul", "Priya", "Ankit"]
```

### limit / skip

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// First 3 elements
List<Integer> first3 = nums.stream()
    .limit(3)
    .collect(Collectors.toList());
// [1, 2, 3]

// Skip first 3, take rest
List<Integer> after3 = nums.stream()
    .skip(3)
    .collect(Collectors.toList());
// [4, 5, 6, 7, 8, 9, 10]

// Pagination — page 2, size 3
List<Integer> page2 = nums.stream()
    .skip(3)  // (page - 1) * size = (2-1) * 3
    .limit(3)
    .collect(Collectors.toList());
// [4, 5, 6]
```

### peek

```java
// Debugging — elements ko bina modify kiye dekhna
List<String> result = List.of("Apple", "Banana", "Cherry").stream()
    .filter(s -> s.length() > 5)
    .peek(s -> System.out.println("After filter: " + s))
    .map(String::toUpperCase)
    .peek(s -> System.out.println("After map: " + s))
    .collect(Collectors.toList());
// Output:
// After filter: Banana
// After map: BANANA
// After filter: Cherry
// After map: CHERRY
```

---

## Terminal Operations

**Matlab:** Stream ko consume karte hain — result return karte hain, stream close ho jaata hai.

### collect

```java
List<String> names = List.of("Sachin", "Rahul", "Priya");

// To List
List<String> list = names.stream().collect(Collectors.toList());

// To Set
Set<String> set = names.stream().collect(Collectors.toSet());

// To Map
Map<String, Integer> map = names.stream()
    .collect(Collectors.toMap(
        name -> name,        // Key
        String::length       // Value
    ));
// {Sachin=6, Rahul=5, Priya=5}

// Joining
String joined = names.stream().collect(Collectors.joining(", "));
// "Sachin, Rahul, Priya"

// Grouping
List<User> users = List.of(
    new User("Sachin", 25), new User("Rahul", 25), new User("Priya", 30)
);
Map<Integer, List<User>> byAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge));
// {25=[Sachin, Rahul], 30=[Priya]}
```

### forEach

```java
List<String> names = List.of("Sachin", "Rahul", "Priya");

// Print each name
names.stream().forEach(System.out::println);

// With index (workaround)
AtomicInteger index = new AtomicInteger(0);
names.stream().forEach(name -> 
    System.out.println(index.getAndIncrement() + ": " + name)
);
```

### count

```java
long count = List.of("Apple", "Banana", "Cherry").stream()
    .filter(s -> s.length() > 5)
    .count();
// 2
```

### reduce

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5);

// Sum without identity
Optional<Integer> sum = nums.stream().reduce((a, b) -> a + b);
// Optional[15]

// Sum with identity
int sumWithIdentity = nums.stream().reduce(0, (a, b) -> a + b);
// 15

// Product
int product = nums.stream().reduce(1, (a, b) -> a * b);
// 120

// Max
Optional<Integer> max = nums.stream().reduce(Integer::max);
// Optional[5]

// String concatenation
String joined = List.of("A", "B", "C").stream()
    .reduce("", (a, b) -> a + b);
// "ABC"
```

### findFirst / findAny

```java
List<String> names = List.of("Sachin", "Rahul", "Priya");

// First element
Optional<String> first = names.stream().findFirst();
// Optional[Sachin]

// Any element (parallel streams mein different ho sakta hai)
Optional<String> any = names.stream().findAny();
// Optional[Sachin] (ya koi aur — order guaranteed nahi)

// First matching element
Optional<String> startsWithS = names.stream()
    .filter(n -> n.startsWith("S"))
    .findFirst();
// Optional[Sachin]
```

### anyMatch / allMatch / noneMatch

```java
List<Integer> nums = List.of(2, 4, 6, 8, 10);

// Koi ek even hai?
boolean anyEven = nums.stream().anyMatch(n -> n % 2 == 0);  // true

// Sab even hain?
boolean allEven = nums.stream().allMatch(n -> n % 2 == 0);  // true

// Koi odd nahi hai?
boolean noneOdd = nums.stream().noneMatch(n -> n % 2 != 0);  // true

// Koi 100 se bada hai?
boolean anyOver100 = nums.stream().anyMatch(n -> n > 100);  // false
```

### min / max

```java
List<String> names = List.of("Sachin", "Rahul", "Priya", "A");

Optional<String> shortest = names.stream()
    .min(Comparator.comparingInt(String::length));
// Optional[A]

Optional<String> longest = names.stream()
    .max(Comparator.comparingInt(String::length));
// Optional[Sachin]

// Numeric
List<Integer> nums = List.of(5, 2, 8, 1, 9);
OptionalInt min = nums.stream().mapToInt(Integer::intValue).min();
// OptionalInt[1]
```

---

## Parallel Streams

**Matlab:** Stream ko multiple threads pe process karna — performance improvement ke liye.

### Creating Parallel Streams

```java
List<Integer> nums = IntStream.rangeClosed(1, 1_000_000)
    .boxed()
    .collect(Collectors.toList());

// Parallel stream
long count = nums.parallelStream()
    .filter(n -> n % 2 == 0)
    .count();

// Ya existing stream ko parallel banao
long count2 = nums.stream()
    .parallel()
    .filter(n -> n % 2 == 0)
    .count();

// Wapas sequential
long count3 = nums.stream()
    .parallel()
    .filter(n -> n % 2 == 0)
    .sequential()
    .count();
```

### When to Use Parallel Streams

```java
// ✅ Use parallel when:
// - Large dataset (10,000+ elements)
// - CPU-intensive operations
// - Operations are independent
// - Order doesn't matter

// ❌ Avoid parallel when:
// - Small dataset (overhead > benefit)
// - I/O operations (network, disk)
// - Order matters
// - Stateful operations

// Example: Good use case
long sum = LongStream.rangeClosed(1, 10_000_000)
    .parallel()
    .sum();  // Fast — CPU bound, independent operations

// Example: Bad use case
List<String> small = List.of("A", "B", "C");
small.parallelStream().forEach(System.out::println);  // Slow — overhead > benefit
```

### Parallel Stream Pitfalls

```java
// ❌ Non-thread-safe collection
List<Integer> result = new ArrayList<>();
IntStream.range(0, 1000).parallel().forEach(result::add);
// ❌ ConcurrentModificationException ya data loss

// ✅ Thread-safe collection
List<Integer> result = IntStream.range(0, 1000)
    .parallel()
    .boxed()
    .collect(Collectors.toList());  // ✅ Thread-safe collection

// ❌ Order-dependent operations
List<Integer> nums = List.of(1, 2, 3, 4, 5);
nums.parallelStream().forEach(System.out::println);  // Order unpredictable

// ✅ Use forEachOrdered for order guarantee
nums.parallelStream().forEachOrdered(System.out::println);  // 1, 2, 3, 4, 5
```

---

## Summary

| Operation | Type | Purpose |
|-----------|------|---------|
| **filter** | Intermediate | Elements filter karo |
| **map** | Intermediate | Transform each element |
| **flatMap** | Intermediate | Stream of streams flatten karo |
| **distinct** | Intermediate | Duplicates remove karo |
| **sorted** | Intermediate | Elements sort karo |
| **limit/skip** | Intermediate | Elements truncate/skip karo |
| **peek** | Intermediate | Debugging — side effect dekho |
| **collect** | Terminal | Stream ko collection mein convert karo |
| **forEach** | Terminal | Har element pe action karo |
| **count** | Terminal | Element count karo |
| **reduce** | Terminal | Single value mein combine karo |
| **findFirst/Any** | Terminal | First/any element dhundho |
| **anyMatch/allMatch/noneMatch** | Terminal | Boolean check karo |
| **min/max** | Terminal | Min/max element dhundho |
