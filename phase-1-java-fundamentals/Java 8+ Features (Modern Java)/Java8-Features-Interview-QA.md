# Java 8+ Features (Modern Java) - Top 10 Interview Questions & Answers

> Covering: Lambda Expressions, Stream API, Optional, Date-Time API, Default/Static Interface Methods, Collectors, Functional Interfaces, CompletableFuture

---

## Q1: Lambda Expressions kya hain? Functional Interface se kaise related hain?

**Answer:**

**Lambda** ek anonymous function hai jo functional interface ko implement karta hai — concise syntax mein behavior pass karna.

### Basic Syntax:

```java
// (parameters) -> expression
// (parameters) -> { statements; }

Runnable r = () -> System.out.println("Running");
Consumer<String> print = s -> System.out.println(s);
BiFunction<Integer, Integer, Integer> add = (a, b) -> a + b;
```

### Lambda vs Anonymous Class:

```java
// ❌ Anonymous class — verbose
Comparator<String> c1 = new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return s1.length() - s2.length();
    }
};

// ✅ Lambda — concise
Comparator<String> c2 = (s1, s2) -> s1.length() - s2.length();

// ✅ Best — method reference
Comparator<String> c3 = Comparator.comparingInt(String::length);
```

### Functional Interface — Lambda ka Target:

```java
@FunctionalInterface  // Sirf ek abstract method allowed
interface Calculator {
    int calculate(int a, int b);

    // Default/static methods allowed — count nahi hote
    default void printResult(int result) {
        System.out.println("Result: " + result);
    }
}

// Lambda se implement karo
Calculator add = (a, b) -> a + b;
Calculator multiply = (a, b) -> a * b;

System.out.println(add.calculate(5, 3));       // 8
System.out.println(multiply.calculate(5, 3));    // 15
```

### Effectively Final Variables:

```java
int multiplier = 10;  // Effectively final — change nahi hua

Function<Integer, Integer> timesTen = n -> n * multiplier;  // ✅ OK

// multiplier = 20;  // ❌ Ab effectively final nahi — compile error!
```

### Built-in Functional Interfaces:

| Interface | Method | Input → Output |
|-----------|--------|----------------|
| `Predicate<T>` | `test(T)` | T → boolean |
| `Function<T,R>` | `apply(T)` | T → R |
| `Consumer<T>` | `accept(T)` | T → void |
| `Supplier<T>` | `get()` | none → T |
| `UnaryOperator<T>` | `apply(T)` | T → T |
| `BinaryOperator<T>` | `apply(T,T)` | T,T → T |

**Short Interview Answer:**

Lambda anonymous functions hain jo functional interfaces implement karte hain. Syntax: `(params) -> expression`. Local variables effectively final hone chahiye. Built-in functional interfaces: Predicate, Function, Consumer, Supplier.

---

## Q2: Stream API — Intermediate vs Terminal operations ka difference?

**Answer:**

**Stream** data ka flow hai jisme functional operations chain kar sakte ho. Operations do types ke hain — intermediate (lazy) aur terminal (eager).

### Stream Pipeline:

```
Source → Intermediate Ops → Intermediate Ops → Terminal Op → Result
         (lazy)              (lazy)              (triggers all)
```

### Intermediate Operations (Lazy — Stream return karte hain):

```java
List<String> names = List.of("Sachin", "Rahul", "Priya", "Ankit", "Neha");

Stream<String> stream = names.stream()
    .filter(n -> n.length() > 4)    // Intermediate — lazy
    .map(String::toUpperCase)        // Intermediate — lazy
    .sorted();                       // Intermediate — lazy

// ⚠️ Abhi tak kuch execute nahi hua! Sirf pipeline build hui hai
```

### Terminal Operations (Eager — Pipeline execute karte hain):

```java
// Terminal op — ab pipeline execute hogi
List<String> result = names.stream()
    .filter(n -> n.length() > 4)
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());  // TERMINAL — triggers execution
// [ANKIT, PRIYA, RAHUL, SACHIN]
```

### Operations Table:

| Type | Operations | Returns | Lazy? |
|------|-----------|---------|-------|
| **Intermediate** | filter, map, flatMap, sorted, distinct, limit, skip, peek | Stream | ✅ Yes |
| **Terminal** | collect, forEach, reduce, count, min, max, findFirst, anyMatch | Result/void | ❌ No (triggers) |

### Common Terminal Operations:

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5);

// collect — result collect karo
List<Integer> doubled = nums.stream().map(n -> n * 2).collect(Collectors.toList());

// reduce — combine into single value
int sum = nums.stream().reduce(0, Integer::sum);  // 15

// count
long count = nums.stream().filter(n -> n > 2).count();  // 3

// anyMatch / allMatch / noneMatch
boolean anyEven = nums.stream().anyMatch(n -> n % 2 == 0);  // true

// findFirst / findAny
Optional<Integer> first = nums.stream().filter(n -> n > 3).findFirst();  // Optional[4]

// forEach — side effect
nums.stream().forEach(System.out::println);
```

### Stream Rules:

```java
// ❌ Stream sirf ek baar use kar sakte ho
Stream<String> stream = names.stream();
stream.forEach(System.out::println);
// stream.count();  // ❌ IllegalStateException — stream already consumed

// ❌ Terminal operation ke baad kuch nahi
// names.stream().filter(...).collect(...).map(...);  // ❌ collect returns List, not Stream
```

**Important:**

Intermediate operations lazy hain — short-circuit operations (`findFirst`, `anyMatch`) pe pipeline early terminate ho sakti hai. Stream consume hone ke baad reuse nahi kar sakte.

---

## Q3: map, flatMap, filter, reduce — explain with examples.

**Answer:**

Yeh Stream ke core operations hain — har ek ka specific purpose hai.

### filter — Elements Select Karo:

```java
List<String> names = List.of("Sachin", "Rahul", "Priya", "Ankit", "Neha");

List<String> longNames = names.stream()
    .filter(n -> n.length() > 5)
    .collect(Collectors.toList());
// ["Sachin", "Priya"]

// Multiple filters chain kar sakte ho
List<String> result = names.stream()
    .filter(n -> n.length() > 4)
    .filter(n -> n.startsWith("S"))
    .collect(Collectors.toList());
// ["Sachin"]
```

### map — Transform Karo (1-to-1):

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

### flatMap — Flatten Karo (1-to-many):

```java
List<List<String>> nested = List.of(
    List.of("Apple", "Banana"),
    List.of("Cherry", "Date"),
    List.of("Elderberry")
);

// ❌ map — Stream<List<String>> return karega
List<List<String>> mapped = nested.stream()
    .map(list -> list)
    .collect(Collectors.toList());  // Still nested!

// ✅ flatMap — Stream<String> return karega
List<String> flat = nested.stream()
    .flatMap(List::stream)  // Har inner list ko stream mein convert + flatten
    .collect(Collectors.toList());
// ["Apple", "Banana", "Cherry", "Date", "Elderberry"]

// flatMap with transformation
List<String> words = List.of("hello world", "java streams");
List<String> allWords = words.stream()
    .flatMap(s -> Arrays.stream(s.split(" ")))
    .collect(Collectors.toList());
// ["hello", "world", "java", "streams"]
```

### map vs flatMap:

| Operation | Input → Output | Use When |
|-----------|---------------|----------|
| **map** | T → R | 1 element → 1 element transform |
| **flatMap** | T → Stream<R> | 1 element → many elements flatten |

### reduce — Combine Karo (Single Value):

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5);

// Sum
int sum = nums.stream().reduce(0, Integer::sum);  // 15

// Product
int product = nums.stream().reduce(1, (a, b) -> a * b);  // 120

// Max (no identity)
Optional<Integer> max = nums.stream().reduce(Integer::max);  // Optional[5]

// String concatenation
List<String> words = List.of("Hello", " ", "World");
String sentence = words.stream().reduce("", String::concat);  // "Hello World"

// Custom reduce
String longest = names.stream()
    .reduce("", (a, b) -> a.length() >= b.length() ? a : b);
// "Sachin"
```

**Short Interview Answer:**

filter = select elements by condition. map = 1-to-1 transform. flatMap = flatten nested structures (1-to-many). reduce = combine all elements into single result. map returns Stream<R>, flatMap returns flattened Stream<R>.

---

## Q4: Optional<T> best practices kya hain? Null handling kaise karein?

**Answer:**

**Optional** null-safe wrapper hai — `NullPointerException` avoid karne ke liye explicit null handling force karta hai.

### Creating Optional:

```java
Optional<String> present = Optional.of("Hello");         // Non-null (null → NPE)
Optional<String> maybeNull = Optional.ofNullable(getName());  // Null-safe
Optional<String> empty = Optional.empty();               // Explicitly empty
```

### ❌ Anti-Patterns (Avoid):

```java
// ❌ Optional ko null check jaisa use karna
Optional<User> opt = userRepository.findById(1L);
if (opt.isPresent()) {
    User user = opt.get();  // get() avoid karo
    System.out.println(user.getName());
}

// ❌ Optional as method parameter
public void process(Optional<String> name) { }  // Overloading better

// ❌ Optional in fields
private Optional<String> name;  // Use nullable field instead

// ❌ Collections of Optional
List<Optional<User>> users;  // Never do this
```

### ✅ Best Practices:

```java
// ✅ ifPresent — side effect
userRepository.findById(1L)
    .ifPresent(user -> System.out.println(user.getName()));

// ✅ map — transform if present
Optional<String> upperName = userRepository.findById(1L)
    .map(User::getName)
    .map(String::toUpperCase);

// ✅ flatMap — avoid nested Optional
Optional<String> city = userRepository.findById(1L)
    .flatMap(User::getAddress)       // Optional<Address>
    .flatMap(Address::getCity);      // Optional<String>

// ✅ filter
Optional<User> adult = userRepository.findById(1L)
    .filter(user -> user.getAge() >= 18);

// ✅ orElse — default value (always evaluated!)
String name = optional.orElse("Unknown");

// ✅ orElseGet — lazy default (Supplier)
String name = optional.orElseGet(() -> expensiveLookup());

// ✅ orElseThrow — exception if empty
User user = userRepository.findById(1L)
    .orElseThrow(() -> new UserNotFoundException("ID: 1"));
```

### orElse vs orElseGet:

```java
// orElse — default value HAMESHA evaluate hota hai
String result = optional.orElse(expensiveMethod());  // ❌ expensiveMethod() always called!

// orElseGet — default value SIRF empty pe evaluate hota hai
String result = optional.orElseGet(() -> expensiveMethod());  // ✅ Lazy
```

### Repository Pattern:

```java
// ✅ Return Optional from find methods
public Optional<User> findById(Long id) {
    return Optional.ofNullable(database.get(id));
}

// ✅ Return empty list instead of Optional<List>
public List<User> findByAge(int age) {
    List<User> users = database.findByAge(age);
    return users != null ? users : Collections.emptyList();
}
```

**Short Interview Answer:**

Use Optional for return types (not parameters/fields). Prefer `ifPresent`, `map`, `flatMap`, `orElseGet` over `isPresent()` + `get()`. Use `orElseGet` (lazy) over `orElse` (eager). Throw with `orElseThrow` when empty is an error.

---

## Q5: java.time (LocalDateTime) vs java.util.Date — kya difference hai?

**Answer:**

**java.time** (Java 8+) immutable, thread-safe, aur clear API deta hai. **java.util.Date** legacy hai — mutable aur confusing.

### Comparison:

| Feature | java.util.Date | java.time (LocalDateTime) |
|---------|---------------|---------------------------|
| **Immutable** | ❌ Mutable | ✅ Immutable |
| **Thread-Safe** | ❌ No | ✅ Yes |
| **Timezone** | Confusing | Clear separation |
| **API Design** | Poor (1900 offset months!) | Clean, intuitive |
| **Formatting** | SimpleDateFormat (not thread-safe) | DateTimeFormatter (thread-safe) |

### Old Date API Problems:

```java
// ❌ java.util.Date — confusing
Date date = new Date();
date.setMonth(0);  // January? No! Month 0-based but year 1900-based!

// ❌ SimpleDateFormat — NOT thread-safe
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
String formatted = sdf.format(date);  // Race condition in multi-thread!
```

### New Date-Time API:

```java
// ✅ LocalDate — sirf date (no time, no timezone)
LocalDate today = LocalDate.now();                    // 2024-01-15
LocalDate birthday = LocalDate.of(1998, 5, 15);
LocalDate parsed = LocalDate.parse("2024-01-15");

// ✅ LocalTime — sirf time
LocalTime now = LocalTime.now();                        // 14:30:00
LocalTime meeting = LocalTime.of(14, 30);

// ✅ LocalDateTime — date + time (no timezone)
LocalDateTime dateTime = LocalDateTime.now();
LocalDateTime specific = LocalDateTime.of(2024, 1, 15, 14, 30);

// ✅ ZonedDateTime — with timezone
ZonedDateTime ist = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"));
ZonedDateTime utc = ZonedDateTime.now(ZoneId.of("UTC"));

// ✅ Instant — UTC timestamp (epoch seconds)
Instant timestamp = Instant.now();
```

### Common Operations:

```java
LocalDate date = LocalDate.of(2024, 1, 15);

// Add/Subtract
LocalDate nextWeek = date.plusWeeks(1);
LocalDate lastMonth = date.minusMonths(1);
LocalDate nextYear = date.plusYears(1);

// Compare
boolean isBefore = date.isBefore(LocalDate.now());
boolean isAfter = date.isAfter(LocalDate.of(2020, 1, 1));

// Duration (time-based) vs Period (date-based)
Duration duration = Duration.between(LocalTime.of(9, 0), LocalTime.of(17, 30));
// PT8H30M

Period period = Period.between(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 1, 15));
// P4Y14D

// Formatting — thread-safe!
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
String formatted = dateTime.format(formatter);
LocalDateTime parsed = LocalDateTime.parse("15-01-2024 14:30", formatter);
```

### Migration:

```java
// Old → New conversion
Date oldDate = new Date();
Instant instant = oldDate.toInstant();
LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

// New → Old conversion
LocalDateTime ldt = LocalDateTime.now();
Date oldDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
```

**Short Interview Answer:**

Always use java.time in new code — immutable, thread-safe, clear API. LocalDate for dates, LocalTime for time, LocalDateTime for both, ZonedDateTime for timezone, Instant for UTC timestamps. DateTimeFormatter for formatting (thread-safe).

---

## Q6: Default methods aur static interface methods kyun introduce kiye?

**Answer:**

Java 8 mein **default methods** backward compatibility ke liye introduce kiye — existing interfaces mein naye methods add kar sakte ho bina implementations break kiye.

### Problem Without Default Methods:

```
Java 8 mein Collection interface mein stream() add karna tha
→ Sab existing implementations (ArrayList, HashSet, etc.) break ho jaate
→ Kyunki unhe stream() implement karna padta
```

### Default Method Solution:

```java
interface Collection<E> {
    // Existing abstract methods...
    boolean add(E e);
    int size();

    // ✅ Default method — existing implementations inherit karenge
    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    default void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        for (E e : this) {
            action.accept(e);
        }
    }
}
// ArrayList, HashSet — kuch change nahi karna pada!
```

### Default Method Example:

```java
interface Vehicle {
    void start();  // Abstract

    default void stop() {
        System.out.println("Vehicle stopped");
    }

    default void honk() {
        System.out.println("Honk!");
    }
}

class Car implements Vehicle {
    @Override
    public void start() { System.out.println("Car started"); }
    // stop() aur honk() automatically inherit
}
```

### Default Method Conflict Resolution:

```java
interface Flyable {
    default void move() { System.out.println("Flying"); }
}

interface Swimmable {
    default void move() { System.out.println("Swimming"); }
}

// ❌ Diamond problem — dono default methods
class Duck implements Flyable, Swimmable {
    @Override
    public void move() {
        Flyable.super.move();    // Explicitly choose
        // Swimmable.super.move();  // Ya yeh
    }
}
```

**Resolution Rules:**
1. Class method > Interface default method
2. Sub-interface default > Super-interface default
3. Dono interface se inherit → explicitly override karo

### Static Interface Methods:

```java
interface StringUtils {
    static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }
}

// Call directly — no implementation needed
StringUtils.isNullOrEmpty("");     // true
StringUtils.reverse("Hello");      // "olleH"
```

**Short Interview Answer:**

Default methods enable backward-compatible interface evolution — new methods without breaking existing implementations. Static interface methods provide utility methods on interfaces. Conflict resolution: class wins over interface, sub-interface over super-interface, explicit override for multiple inheritance.

---

## Q7: Collectors.groupingBy aur partitioningBy explain karo.

**Answer:**

**Collectors** Stream ke elements ko collect karke List, Set, Map mein convert karte hain. `groupingBy` aur `partitioningBy` data ko groups mein organize karte hain.

### groupingBy — SQL GROUP BY Jaisa:

```java
class User {
    String name;
    int age;
    String department;
    // constructor, getters
}

List<User> users = List.of(
    new User("Sachin", 25, "Engineering"),
    new User("Rahul", 25, "Engineering"),
    new User("Priya", 30, "Marketing"),
    new User("Ankit", 30, "Marketing")
);

// Basic grouping — by age
Map<Integer, List<User>> byAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge));
// {25=[Sachin, Rahul], 30=[Priya, Ankit]}

// Group by department
Map<String, List<User>> byDept = users.stream()
    .collect(Collectors.groupingBy(User::getDepartment));
// {Engineering=[Sachin, Rahul], Marketing=[Priya, Ankit]}
```

### groupingBy with Downstream Collectors:

```java
// Group + count
Map<String, Long> countByDept = users.stream()
    .collect(Collectors.groupingBy(User::getDepartment, Collectors.counting()));
// {Engineering=2, Marketing=2}

// Group + average age
Map<String, Double> avgAgeByDept = users.stream()
    .collect(Collectors.groupingBy(
        User::getDepartment,
        Collectors.averagingInt(User::getAge)
    ));
// {Engineering=25.0, Marketing=30.0}

// Group + names only
Map<String, List<String>> namesByDept = users.stream()
    .collect(Collectors.groupingBy(
        User::getDepartment,
        Collectors.mapping(User::getName, Collectors.toList())
    ));
// {Engineering=[Sachin, Rahul], Marketing=[Priya, Ankit]}

// Multi-level grouping
Map<String, Map<Integer, List<User>>> byDeptThenAge = users.stream()
    .collect(Collectors.groupingBy(
        User::getDepartment,
        Collectors.groupingBy(User::getAge)
    ));
// {Engineering={25=[Sachin, Rahul]}, Marketing={30=[Priya, Ankit]}}
```

### partitioningBy — Boolean Groups (Special Case of groupingBy):

```java
// Partition by age >= 28
Map<Boolean, List<User>> partitioned = users.stream()
    .collect(Collectors.partitioningBy(u -> u.getAge() >= 28));
// {false=[Sachin(25), Rahul(25)], true=[Priya(30), Ankit(30)]}

// Partition + count
Map<Boolean, Long> countPartitioned = users.stream()
    .collect(Collectors.partitioningBy(
        u -> u.getAge() >= 28,
        Collectors.counting()
    ));
// {false=2, true=2}
```

### groupingBy vs partitioningBy:

| Feature | groupingBy | partitioningBy |
|---------|-----------|----------------|
| **Key Type** | Any type | Boolean only |
| **Groups** | Multiple (any count) | Exactly 2 (true/false) |
| **Result Map** | `Map<K, List<T>>` | `Map<Boolean, List<T>>` |
| **Use Case** | Category-based grouping | Yes/No split |

### Other Useful Collectors:

```java
// joining — String concatenation
String joined = names.stream().collect(Collectors.joining(", "));
// "Sachin, Rahul, Priya"

// toMap
Map<String, Integer> nameToAge = users.stream()
    .collect(Collectors.toMap(User::getName, User::getAge));

// summarizing
IntSummaryStatistics stats = users.stream()
    .collect(Collectors.summarizingInt(User::getAge));
// count=4, sum=110, min=25, max=30, average=27.5
```

**Short Interview Answer:**

groupingBy groups elements by any key (like SQL GROUP BY) — use downstream collectors for count/average/mapping. partitioningBy is special groupingBy with Boolean key — exactly two groups (true/false).

---

## Q8: Predicate, Function, Consumer, Supplier — kab kaunsa use karte ho?

**Answer:**

Yeh Java 8 ke core functional interfaces hain — har ek ka specific role hai.

### Quick Reference:

| Interface | Method | Input → Output | Use Case |
|-----------|--------|----------------|----------|
| **Predicate<T>** | `test(T)` | T → boolean | Filter/condition check |
| **Function<T,R>** | `apply(T)` | T → R | Transform/convert |
| **Consumer<T>** | `accept(T)` | T → void | Side effect (print, save) |
| **Supplier<T>** | `get()` | none → T | Factory/lazy generation |

### Predicate<T> — Condition Check:

```java
Predicate<String> isEmpty = String::isEmpty;
Predicate<String> isLong = s -> s.length() > 10;
Predicate<Integer> isEven = n -> n % 2 == 0;
Predicate<User> isAdult = u -> u.getAge() >= 18;

// Compose predicates
Predicate<String> notEmpty = isEmpty.negate();
Predicate<String> longAndNotEmpty = isLong.and(notEmpty);
Predicate<String> shortOrEmpty = isEmpty.or(isLong.negate());

// Use in Stream
List<String> filtered = names.stream()
    .filter(isLong.and(notEmpty))
    .collect(Collectors.toList());
```

### Function<T,R> — Transform:

```java
Function<String, Integer> parseInt = Integer::parseInt;
Function<Integer, String> toString = Object::toString;
Function<User, String> getName = User::getName;

// Compose functions
Function<String, String> trimAndUpper = ((Function<String, String>) String::trim)
    .andThen(String::toUpperCase);
// "  hello  " → "HELLO"

Function<String, String> upperAndTrim = String::toUpperCase
    .compose(String::trim);  // Reverse order

// Use in Stream
List<Integer> lengths = names.stream()
    .map(String::length)
    .collect(Collectors.toList());
```

### Consumer<T> — Side Effect (No Return):

```java
Consumer<String> print = System.out::println;
Consumer<String> log = msg -> logger.info(msg);
Consumer<User> saveToDb = user -> repository.save(user);

// Chain consumers
Consumer<String> printAndLog = print.andThen(log);

// Use in Stream
users.stream().forEach(System.out::println);
users.forEach(user -> repository.save(user));
```

### Supplier<T> — Generate/Provide (No Input):

```java
Supplier<Double> random = Math::random;
Supplier<String> uuid = () -> UUID.randomUUID().toString();
Supplier<List<String>> listFactory = ArrayList::new;
Supplier<User> defaultUser = () -> new User("Guest", 0);

// Lazy evaluation
String value = optional.orElseGet(uuid);  // Sirf empty pe generate

// Use in Stream.generate
Stream.generate(Math::random).limit(5).collect(Collectors.toList());
```

### Real-World Pipeline:

```java
List<User> activeAdults = users.stream()
    .filter(isAdult)                    // Predicate — filter
    .filter(u -> u.isActive())          // Predicate — filter
    .map(getName)                       // Function — transform
    .map(String::toUpperCase)           // Function — transform
    .peek(System.out::println)          // Consumer — side effect (debug)
    .collect(Collectors.toList());      // Terminal
```

**Short Interview Answer:**

Predicate = boolean test (filter). Function = transform T to R (map). Consumer = accept without return (forEach). Supplier = provide value without input (factory/lazy). Compose with and/andThen/compose for complex logic.

---

## Q9: CompletableFuture — thenApply, thenCompose, exceptionally explain karo.

**Answer:**

**CompletableFuture** async programming ka powerful tool hai — non-blocking task chaining, composition, aur error handling.

### Creating CompletableFuture:

```java
// supplyAsync — value return karta hai
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Hello";
});

// runAsync — void return
CompletableFuture<Void> voidFuture = CompletableFuture.runAsync(() -> {
    System.out.println("Running in background");
});
```

### thenApply — Transform Result (Sync):

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> "hello")
    .thenApply(String::toUpperCase)     // "HELLO"
    .thenApply(s -> s + " WORLD");      // "HELLO WORLD"

System.out.println(future.join());  // "HELLO WORLD"
```

### thenCompose — Flatten Nested Futures (Async):

```java
// thenApply — nested CompletableFuture (BAD)
CompletableFuture<CompletableFuture<String>> nested = CompletableFuture
    .supplyAsync(() -> "user123")
    .thenApply(id -> fetchUserAsync(id));  // Returns CompletableFuture<String>!

// thenCompose — flat chain (GOOD)
CompletableFuture<String> flat = CompletableFuture
    .supplyAsync(() -> "user123")
    .thenCompose(id -> fetchUserAsync(id));  // Returns CompletableFuture<String>

CompletableFuture<String> fetchUserAsync(String id) {
    return CompletableFuture.supplyAsync(() -> "User: " + id);
}
```

### thenApply vs thenCompose:

| Method | Input | Output | Use When |
|--------|-------|--------|----------|
| **thenApply(fn)** | T | U | Transform result (sync) |
| **thenApplyAsync(fn)** | T | U | Transform result (async) |
| **thenCompose(fn)** | T | CompletableFuture<U> | Flatten nested async |
| **thenAccept(consumer)** | T | void | Consume result |
| **thenRun(action)** | — | void | Run after completion |

### exceptionally — Error Recovery:

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        if (Math.random() > 0.5) {
            throw new RuntimeException("API failed!");
        }
        return "Success";
    })
    .exceptionally(ex -> {
        System.out.println("Error: " + ex.getMessage());
        return "Fallback Value";  // Recovery value
    });

System.out.println(future.join());  // "Success" or "Fallback Value"
```

### handle — Result + Error Dono Handle:

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> fetchData())
    .handle((result, ex) -> {
        if (ex != null) {
            return "Error: " + ex.getMessage();
        }
        return result;
    });
```

### Combining Futures:

```java
CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(() -> "Sachin");
CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> 25);

// Combine two results
CompletableFuture<String> combined = userFuture.thenCombine(ageFuture,
    (name, age) -> name + " is " + age);
// "Sachin is 25"

// Wait for all
CompletableFuture<Void> all = CompletableFuture.allOf(future1, future2, future3);

// First to complete
CompletableFuture<Object> any = CompletableFuture.anyOf(future1, future2);
```

**Short Interview Answer:**

thenApply = sync transform (T → U). thenCompose = flatten nested async (T → CompletableFuture<U>). exceptionally = error recovery with fallback. handle = process both result and exception. thenCombine = merge two futures. allOf/anyOf = wait for all/first.

---

## Q10: Java 8 features interview mein common questions — quick revision?

**Answer:**

### Feature Summary Table:

| Feature | Key Point | Common Use |
|---------|-----------|------------|
| **Lambda** | `(params) -> expression` | Functional interfaces, callbacks |
| **Stream** | Lazy pipeline, one-time use | Collection processing |
| **Optional** | Null-safe wrapper | Repository return types |
| **java.time** | Immutable date/time | Replace Date/Calendar |
| **Default Methods** | Interface evolution | Collection.stream() |
| **Method Reference** | `Class::method` | Shorter than lambda |
| **CompletableFuture** | Async composition | Parallel API calls |

### Method References:

```java
// Static method
Function<String, Integer> parse = Integer::parseInt;

// Instance method (specific object)
Consumer<String> print = System.out::println;

// Instance method (arbitrary object)
Function<String, Integer> length = String::length;

// Constructor
Supplier<ArrayList<String>> listFactory = ArrayList::new;
```

### Stream Best Practices:

```java
// ✅ Method reference over lambda when possible
.map(String::toUpperCase)          // Better
.map(s -> s.toUpperCase())         // OK

// ✅ Specific collectors
.collect(Collectors.toList());    // Mutable
.collect(Collectors.toUnmodifiableList());  // Immutable (Java 10+)

// ✅ Avoid parallel streams for small data
list.parallelStream()  // Overhead > benefit for small lists

// ✅ Use primitive streams for performance
IntStream.range(1, 100).sum();  // Better than Stream<Integer>
```

### Parallel Streams Warning:

```java
// ⚠️ Parallel streams use common ForkJoinPool
// ⚠️ Thread-safe operations required
// ⚠️ Order not guaranteed
// ⚠️ Small data = overhead > benefit

List<Integer> nums = IntStream.range(1, 1_000_000)
    .boxed()
    .parallel()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());  // OK for large data + CPU-bound
```

### Interview Quick Answers:

```
Q: Lambda kya hai?
A: Anonymous function for functional interfaces. Syntax: (params) -> body.

Q: Stream lazy kyun hai?
A: Intermediate ops build pipeline; terminal op triggers execution. Optimization possible.

Q: Optional kab use karo?
A: Return types for potentially absent values. Not for fields/parameters/collections.

Q: thenApply vs thenCompose?
A: thenApply transforms (T→U). thenCompose flattens (T→CompletableFuture<U>).

Q: Date vs LocalDateTime?
A: Always java.time — immutable, thread-safe, clear API.
```

**Short Interview Answer:**

Java 8 brought functional programming to Java: lambdas + functional interfaces, Stream API for declarative collection processing, Optional for null safety, java.time for dates, default methods for interface evolution, and CompletableFuture for async programming. Master these and you cover 80% of modern Java interview questions.
