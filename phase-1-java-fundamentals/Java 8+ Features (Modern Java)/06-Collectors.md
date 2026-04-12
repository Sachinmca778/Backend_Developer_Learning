# Collectors

## Status: Not Started

---

## Table of Contents

1. [Basic Collectors](#basic-collectors)
2. [groupingBy](#groupingby)
3. [partitioningBy](#partitioningby)
4. [joining](#joining)
5. [Summarizing Collectors](#summarizing-collectors)
6. [Advanced Collectors](#advanced-collectors)
7. [Custom Collectors](#custom-collectors)

---

## Basic Collectors

**Matlab:** Stream ke elements ko collect karke List, Set, Map, etc. mein convert karna.

### toList / toSet

```java
List<String> names = List.of("Sachin", "Rahul", "Priya");

// To List
List<String> list = names.stream()
    .collect(Collectors.toList());

// To Set (duplicates removed)
Set<String> set = names.stream()
    .collect(Collectors.toSet());

// Java 10+ — unmodifiable
List<String> unmodifiableList = names.stream()
    .collect(Collectors.toUnmodifiableList());

Set<String> unmodifiableSet = names.stream()
    .collect(Collectors.toUnmodifiableSet());
```

### toMap

```java
class User {
    String name;
    int age;
    // constructor, getters
}

List<User> users = List.of(
    new User("Sachin", 25),
    new User("Rahul", 30),
    new User("Priya", 28)
);

// Basic toMap — key mapper, value mapper
Map<String, Integer> nameToAge = users.stream()
    .collect(Collectors.toMap(
        User::getName,    // Key
        User::getAge      // Value
    ));
// {Sachin=25, Rahul=30, Priya=28}

// With merge function (duplicate keys handle karo)
List<String> words = List.of("apple", "banana", "apple", "cherry");
Map<String, Long> freq = words.stream()
    .collect(Collectors.toMap(
        w -> w,                    // Key
        w -> 1L,                   // Value
        Long::sum                  // Merge function
    ));
// {apple=2, banana=1, cherry=1}

// With specific Map implementation
Map<String, Integer> linkedMap = users.stream()
    .collect(Collectors.toMap(
        User::getName,
        User::getAge,
        (a, b) -> a,
        LinkedHashMap::new
    ));
```

---

## groupingBy

**Matlab:** Elements ko kisi key ke basis pe group karna — SQL GROUP BY jaisa.

### Basic groupingBy

```java
List<User> users = List.of(
    new User("Sachin", 25),
    new User("Rahul", 25),
    new User("Priya", 30),
    new User("Ankit", 30)
);

// Group by age
Map<Integer, List<User>> byAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge));
// {25=[Sachin(25), Rahul(25)], 30=[Priya(30), Ankit(30)]}

// Group by first letter of name
Map<Character, List<User>> byFirstLetter = users.stream()
    .collect(Collectors.groupingBy(u -> u.getName().charAt(0)));
// {S=[Sachin(25)], R=[Rahul(25)], P=[Priya(30)], A=[Ankit(30)]}
```

### groupingBy with Downstream Collector

```java
// Group + count
Map<Integer, Long> countByAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge, Collectors.counting()));
// {25=2, 30=2}

// Group + sum
Map<Integer, Integer> totalAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge, Collectors.summingInt(User::getAge)));
// {25=50, 30=60}

// Group + average
Map<Integer, Double> avgAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge, Collectors.averagingInt(User::getAge)));
// {25=25.0, 30=30.0}

// Group + max
Map<Integer, Optional<User>> oldestByAge = users.stream()
    .collect(Collectors.groupingBy(User::getAge, Collectors.maxBy(Comparator.comparingInt(User::getAge))));

// Group + join names
Map<Integer, String> namesByAge = users.stream()
    .collect(Collectors.groupingBy(
        User::getAge,
        Collectors.mapping(User::getName, Collectors.joining(", "))
    ));
// {25="Sachin, Rahul", 30="Priya, Ankit"}
```

### Multi-level Grouping

```java
class Employee {
    String department;
    String city;
    int salary;
    // constructor, getters
}

List<Employee> emps = List.of(
    new Employee("IT", "Delhi", 50000),
    new Employee("IT", "Mumbai", 60000),
    new Employee("HR", "Delhi", 45000),
    new Employee("HR", "Mumbai", 55000)
);

// Group by department, then by city
Map<String, Map<String, List<Employee>>> grouped = emps.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.groupingBy(Employee::getCity)
    ));
// {IT={Delhi=[...], Mumbai=[...]}, HR={Delhi=[...], Mumbai=[...]}}

// Group by department, then count by city
Map<String, Map<String, Long>> countGrouped = emps.stream()
    .collect(Collectors.groupingBy(
        Employee::getDepartment,
        Collectors.groupingBy(Employee::getCity, Collectors.counting())
    ));
// {IT={Delhi=1, Mumbai=1}, HR={Delhi=1, Mumbai=1}}
```

---

## partitioningBy

**Matlab:** Special case of groupingBy — sirf true/false mein partition karta hai.

### Basic Usage

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

// Partition: even vs odd
Map<Boolean, List<Integer>> partitioned = nums.stream()
    .collect(Collectors.partitioningBy(n -> n % 2 == 0));
// {false=[1, 3, 5, 7, 9], true=[2, 4, 6, 8, 10]}

List<Integer> evens = partitioned.get(true);   // [2, 4, 6, 8, 10]
List<Integer> odds = partitioned.get(false);   // [1, 3, 5, 7, 9]
```

### With Downstream

```java
// Partition + count
Map<Boolean, Long> countPartitioned = nums.stream()
    .collect(Collectors.partitioningBy(n -> n > 5, Collectors.counting()));
// {false=5, true=5} (5 numbers <= 5, 5 numbers > 5)

// Partition + sum
Map<Boolean, Integer> sumPartitioned = nums.stream()
    .collect(Collectors.partitioningBy(
        n -> n % 2 == 0,
        Collectors.summingInt(Integer::intValue)
    ));
// {false=25, true=30} (odd sum=25, even sum=30)
```

---

## joining

**Matlab:** Strings ko join karna — delimiter, prefix, suffix support.

```java
List<String> names = List.of("Sachin", "Rahul", "Priya");

// Basic joining
String joined = names.stream()
    .collect(Collectors.joining());
// "SachinRahulPriya"

// With delimiter
String commaSeparated = names.stream()
    .collect(Collectors.joining(", "));
// "Sachin, Rahul, Priya"

// With prefix and suffix
String formatted = names.stream()
    .collect(Collectors.joining(", ", "[", "]"));
// "[Sachin, Rahul, Priya]"

// Filtered + joined
String adults = users.stream()
    .filter(u -> u.getAge() >= 28)
    .map(User::getName)
    .collect(Collectors.joining(", "));
// "Rahul, Priya"
```

---

## Summarizing Collectors

**Matlab:** Multiple statistics ek saath — count, sum, min, max, average.

### IntSummaryStatistics

```java
List<Integer> nums = List.of(1, 2, 3, 4, 5);

IntSummaryStatistics stats = nums.stream()
    .collect(Collectors.summarizingInt(Integer::intValue));

System.out.println(stats.getCount());    // 5
System.out.println(stats.getSum());      // 15
System.out.println(stats.getMin());      // 1
System.out.println(stats.getMax());      // 5
System.out.println(stats.getAverage());  // 3.0
```

### DoubleSummaryStatistics

```java
List<User> users = List.of(
    new User("Sachin", 25),
    new User("Rahul", 30),
    new User("Priya", 28)
);

DoubleSummaryStatistics ageStats = users.stream()
    .collect(Collectors.summarizingDouble(User::getAge));

System.out.println(ageStats.getAverage());  // 27.666
System.out.println(ageStats.getMax());      // 30.0
System.out.println(ageStats.getMin());      // 25.0
```

---

## Advanced Collectors

### collectingAndThen

```java
// Collect + transform
String result = names.stream()
    .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        list -> String.join(", ", list)
    ));
// "Sachin, Rahul, Priya"

// To unmodifiable list
List<String> unmodifiable = names.stream()
    .collect(Collectors.collectingAndThen(
        Collectors.toList(),
        Collections::unmodifiableList
    ));
```

### tee (Java 12+)

```java
// Two downstream collectors ek saath
List<Integer> nums = List.of(1, 2, 3, 4, 5);

String result = nums.stream()
    .collect(Collectors.teeing(
        Collectors.summingInt(Integer::intValue),  // Sum
        Collectors.counting(),                      // Count
        (sum, count) -> "Sum: " + sum + ", Count: " + count  // Merger
    ));
// "Sum: 15, Count: 5"
```

### toCollection

```java
// Specific collection type
LinkedList<String> linkedList = names.stream()
    .collect(Collectors.toCollection(LinkedList::new));

TreeSet<String> treeSet = names.stream()
    .collect(Collectors.toCollection(TreeSet::new));
```

---

## Custom Collectors

```java
// Custom collector — odd numbers ko comma-separate karo
Collector<Integer, StringBuilder, String> customCollector = Collector.of(
    StringBuilder::new,                           // Supplier
    (sb, n) -> sb.append(n).append(", "),        // Accumulator
    (sb1, sb2) -> sb1.append(sb2),               // Combiner
    sb -> sb.length() > 0 ? sb.substring(0, sb.length() - 2) : ""  // Finisher
);

String result = List.of(1, 2, 3, 4, 5).stream()
    .filter(n -> n % 2 != 0)
    .collect(customCollector);
// "1, 3, 5"
```

---

## Summary

| Collector | Purpose | Example |
|-----------|---------|---------|
| **toList/toSet** | Stream → List/Set | `collect(toList())` |
| **toMap** | Stream → Map | `collect(toMap(key, value))` |
| **groupingBy** | Group elements | `collect(groupingBy(classifier))` |
| **partitioningBy** | True/False partition | `collect(partitioningBy(predicate))` |
| **joining** | Strings join karo | `collect(joining(", "))` |
| **counting** | Count elements | `collect(counting())` |
| **summingInt** | Sum of ints | `collect(summingInt(mapper))` |
| **averagingInt** | Average | `collect(averagingInt(mapper))` |
| **summarizingInt** | All stats | `collect(summarizingInt(mapper))` |
| **collectingAndThen** | Collect + transform | `collect(collectingAndThen(...))` |
| **teeing** | Two collectors | Java 12+ |
