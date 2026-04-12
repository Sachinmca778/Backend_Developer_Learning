# Collections Utility Class

## Status: Not Started

---

## Table of Contents

1. [Collections.sort](#collectionssort)
2. [Collections.shuffle](#collectionsshuffle)
3. [Collections.reverse](#collectionsreverse)
4. [Collections.unmodifiableList](#collectionsunmodifiablelist)
5. [Collections.synchronizedList](#collectionssynchronizedlist)
6. [Collections.frequency](#collectionsfrequency)
7. [Collections.disjoint](#collectionsdisjoint)
8. [Collections.nCopies](#collectionsncopies)
9. [Other Useful Methods](#other-useful-methods)

---

## Collections.sort

**Matlab:** List ko sort karna — natural ordering ya custom comparator ke saath.

### Basic Sort

```java
List<Integer> nums = new ArrayList<>(List.of(5, 2, 8, 1, 9, 3));

// Natural ordering (ascending)
Collections.sort(nums);
System.out.println(nums);  // [1, 2, 3, 5, 8, 9]

// Descending order
Collections.sort(nums, Comparator.reverseOrder());
System.out.println(nums);  // [9, 8, 5, 3, 2, 1]
```

### Custom Comparator

```java
List<String> names = new ArrayList<>(List.of("Sachin", "Rahul", "Priya", "Ankit"));

// Length ke basis pe
Collections.sort(names, Comparator.comparingInt(String::length));
System.out.println(names);  // [Ankit, Rahul, Priya, Sachin]

// Reverse length
Collections.sort(names, Comparator.comparingInt(String::length).reversed());
System.out.println(names);  // [Sachin, Priya, Ankit, Rahul]

// Case-insensitive
Collections.sort(names, String.CASE_INSENSITIVE_ORDER);
```

### Sorting Objects

```java
class Employee {
    String name;
    int salary;
    int age;

    Employee(String name, int salary, int age) {
        this.name = name;
        this.salary = salary;
        this.age = age;
    }

    @Override
    public String toString() {
        return name + "(" + salary + ")";
    }
}

List<Employee> employees = new ArrayList<>();
employees.add(new Employee("Sachin", 50000, 25));
employees.add(new Employee("Rahul", 60000, 30));
employees.add(new Employee("Priya", 50000, 28));

// Sort by salary
Collections.sort(employees, Comparator.comparingInt(e -> e.salary));

// Sort by name
Collections.sort(employees, Comparator.comparing(e -> e.name));

// Sort by salary, then by name
Collections.sort(employees,
    Comparator.comparingInt((Employee e) -> e.salary)
        .thenComparing(e -> e.name));

System.out.println(employees);
// [Priya(50000), Sachin(50000), Rahul(60000)]
```

### sort() vs List.sort()

```java
// Both same kaam karte hain
Collections.sort(list);
list.sort(null);  // Same as Collections.sort(list)

list.sort(Comparator.reverseOrder());
Collections.sort(list, Comparator.reverseOrder());  // Same
```

---

## Collections.shuffle

**Matlab:** List ko randomly rearrange karna — randomization ke liye.

### Basic Usage

```java
List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

// Random shuffle
Collections.shuffle(nums);
System.out.println(nums);  // [3, 7, 1, 9, 2, 8, 5, 10, 4, 6] (random order)

// Shuffle with seed (reproducible)
Collections.shuffle(nums, new Random(42));
System.out.println(nums);  // Same order every time with same seed
```

### Use Cases

```java
// Card game — deck shuffle
List<String> deck = new ArrayList<>();
for (String suit : List.of("Hearts", "Diamonds", "Clubs", "Spades")) {
    for (String rank : List.of("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")) {
        deck.add(rank + " of " + suit);
    }
}
Collections.shuffle(deck);

// Random quiz questions
List<Question> questions = new ArrayList<>(allQuestions);
Collections.shuffle(questions);
List<Question> quiz = questions.subList(0, 10);  // First 10 random questions

// A/B testing — random assignment
List<User> users = new ArrayList<>(allUsers);
Collections.shuffle(users);
List<User> groupA = users.subList(0, users.size() / 2);
List<User> groupB = users.subList(users.size() / 2);
```

---

## Collections.reverse

**Matlab:** List ko reverse karna — last element first, first element last.

### Basic Usage

```java
List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5));

Collections.reverse(nums);
System.out.println(nums);  // [5, 4, 3, 2, 1]
```

### Other Collections

```java
// List of strings
List<String> names = new ArrayList<>(List.of("Alice", "Bob", "Charlie"));
Collections.reverse(names);
System.out.println(names);  // [Charlie, Bob, Alice]

// Works on any List implementation
LinkedList<Integer> ll = new LinkedList<>(List.of(1, 2, 3));
Collections.reverse(ll);
System.out.println(ll);  // [3, 2, 1]
```

### Reverse + Sort

```java
List<Integer> nums = new ArrayList<>(List.of(5, 2, 8, 1, 9));

// Sort then reverse = descending
Collections.sort(nums);
Collections.reverse(nums);
System.out.println(nums);  // [9, 8, 5, 2, 1]

// Better: directly sort descending
Collections.sort(nums, Comparator.reverseOrder());
```

---

## Collections.unmodifiableList

**Matlab:** Read-only view of a List — modify karne ki koshish pe UnsupportedOperationException throw hoga.

### Basic Usage

```java
List<String> original = new ArrayList<>(List.of("Apple", "Banana", "Cherry"));
List<String> unmodifiable = Collections.unmodifiableList(original);

// Read operations work
System.out.println(unmodifiable.get(0));  // "Apple"
System.out.println(unmodifiable.size());  // 3

// Write operations throw exception
unmodifiable.add("Date");           // ❌ UnsupportedOperationException
unmodifiable.remove(0);             // ❌ UnsupportedOperationException
unmodifiable.set(0, "Apricot");     // ❌ UnsupportedOperationException

// Original list can still be modified
original.add("Date");
System.out.println(unmodifiable);  // [Apple, Banana, Cherry, Date] (reflects original!)
```

### Other Unmodifiable Collections

```java
// Set
Set<String> unmodifiableSet = Collections.unmodifiableSet(new HashSet<>(Set.of("A", "B")));

// Map
Map<String, Integer> unmodifiableMap = Collections.unmodifiableMap(
    new HashMap<>(Map.of("A", 1, "B", 2))
);

// Sorted Set/Map
SortedSet<String> unmodifiableSortedSet = Collections.unmodifiableSortedSet(new TreeSet<>(Set.of("A", "B")));
SortedMap<String, Integer> unmodifiableSortedMap = Collections.unmodifiableSortedMap(new TreeMap<>(Map.of("A", 1)));
```

### Use Cases

```java
// ✅ Return unmodifiable view from method
public List<String> getNames() {
    return Collections.unmodifiableList(names);
}

// ✅ Defensive copy + unmodifiable
public List<String> getNamesCopy() {
    return Collections.unmodifiableList(new ArrayList<>(names));
}

// ✅ Constant collections
public static final List<String> DAYS = Collections.unmodifiableList(
    List.of("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
);
```

### Java 9+ Alternative

```java
// Java 9+ — List.of() already immutable
List<String> immutable = List.of("Apple", "Banana", "Cherry");
immutable.add("Date");  // ❌ UnsupportedOperationException
```

---

## Collections.synchronizedList

**Matlab:** Thread-safe wrapper around a List — all methods synchronized hote hain.

### Basic Usage

```java
List<String> list = new ArrayList<>();
List<String> synchronizedList = Collections.synchronizedList(list);

synchronizedList.add("Apple");
synchronizedList.add("Banana");

// Read
String first = synchronizedList.get(0);

// Iteration — manually synchronize karna padta hai
synchronized (synchronizedList) {
    for (String item : synchronizedList) {
        System.out.println(item);
    }
}
```

### ⚠️ Iteration Warning

```java
// ❌ WRONG — ConcurrentModificationException possible
for (String item : synchronizedList) {
    System.out.println(item);
}

// ✅ CORRECT — synchronize during iteration
synchronized (synchronizedList) {
    for (String item : synchronizedList) {
        System.out.println(item);
    }
}
```

### Other Synchronized Collections

```java
// Set
Set<String> syncSet = Collections.synchronizedSet(new HashSet<>());

// Map
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// Sorted Set/Map
SortedSet<String> syncSortedSet = Collections.synchronizedSortedSet(new TreeSet<>());
SortedMap<String, Integer> syncSortedMap = Collections.synchronizedSortedMap(new TreeMap<>());
```

### Modern Alternative: ConcurrentHashMap

```java
// ❌ Old way
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// ✅ Better — concurrent, no manual synchronization needed
Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
concurrentMap.put("A", 1);
concurrentMap.putIfAbsent("B", 2);
concurrentMap.compute("A", (k, v) -> v + 1);

// Iteration — no synchronization needed
for (Map.Entry<String, Integer> entry : concurrentMap.entrySet()) {
    System.out.println(entry.getKey() + " → " + entry.getValue());
}
```

---

## Collections.frequency

**Matlab:** Collection mein ek element kitni baar aata hai — count karta hai.

### Basic Usage

```java
List<String> fruits = List.of("Apple", "Banana", "Apple", "Cherry", "Apple", "Banana");

int appleCount = Collections.frequency(fruits, "Apple");    // 3
int bananaCount = Collections.frequency(fruits, "Banana");  // 2
int grapeCount = Collections.frequency(fruits, "Grape");    // 0
```

### Use Cases

```java
// Most frequent element
List<String> words = List.of("a", "b", "a", "c", "b", "a", "d");
String mostFrequent = words.stream()
    .distinct()
    .max(Comparator.comparingInt(w -> Collections.frequency(words, w)))
    .orElse(null);
System.out.println(mostFrequent);  // "a" (frequency 3)

// Check if element appears more than N times
boolean appearsMoreThanTwice = Collections.frequency(words, "a") > 2;  // true
```

---

## Collections.disjoint

**Matlab:** Check karta hai ki do collections mein koi common element hai ya nahi.

### Basic Usage

```java
List<String> list1 = List.of("Apple", "Banana", "Cherry");
List<String> list2 = List.of("Date", "Elderberry", "Fig");
List<String> list3 = List.of("Banana", "Grape");

boolean disjoint1 = Collections.disjoint(list1, list2);  // true (no common elements)
boolean disjoint2 = Collections.disjoint(list1, list3);  // false (Banana common hai)
```

### Use Cases

```java
// Check if user has any of the required permissions
List<String> userPermissions = List.of("READ", "WRITE");
List<String> requiredPermissions = List.of("ADMIN", "DELETE");

boolean hasNoRequiredPermission = Collections.disjoint(userPermissions, requiredPermissions);
// true — user ke paas koi required permission nahi hai

// Check overlap between two groups
Set<Integer> group1 = Set.of(1, 2, 3);
Set<Integer> group2 = Set.of(3, 4, 5);

boolean hasOverlap = !Collections.disjoint(group1, group2);  // true (3 common hai)
```

---

## Collections.nCopies

**Matlab:** Immutable list with N copies of the same element.

### Basic Usage

```java
// 5 copies of "Hello"
List<String> copies = Collections.nCopies(5, "Hello");
System.out.println(copies);  // [Hello, Hello, Hello, Hello, Hello]

// Read-only
copies.add("World");  // ❌ UnsupportedOperationException
copies.set(0, "Hi");  // ❌ UnsupportedOperationException

// Size
System.out.println(copies.size());  // 5

// Convert to mutable list
List<String> mutable = new ArrayList<>(Collections.nCopies(5, "Hello"));
mutable.set(0, "Hi");  // ✅ Works
```

### Use Cases

```java
// Initialize list with default values
List<Integer> defaults = new ArrayList<>(Collections.nCopies(10, 0));
// [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]

// Fill with nulls
List<String> nulls = new ArrayList<>(Collections.nCopies(5, null));
// [null, null, null, null, null]

// Pre-allocate with placeholder
List<User> users = new ArrayList<>(Collections.nCopies(100, null));
// Baad mein actual values se replace karo
users.set(0, new User("Sachin", 25));
```

---

## Other Useful Methods

### Binary Search

```java
// Sorted list pe binary search — O(log n)
List<Integer> sorted = List.of(1, 3, 5, 7, 9, 11);
int index = Collections.binarySearch(sorted, 7);
System.out.println(index);  // 3

int notFound = Collections.binarySearch(sorted, 6);
System.out.println(notFound);  // -5 (insertion point = 4, so -4-1 = -5)
```

### Min/Max

```java
List<Integer> nums = List.of(5, 2, 8, 1, 9);

int min = Collections.min(nums);  // 1
int max = Collections.max(nums);  // 9

// Custom comparator
List<String> names = List.of("Sachin", "Rahul", "Priya");
String shortest = Collections.min(names, Comparator.comparingInt(String::length));  // "Rahul"
String longest = Collections.max(names, Comparator.comparingInt(String::length));   // "Sachin"
```

### Fill

```java
List<Integer> nums = new ArrayList<>(Collections.nCopies(5, 0));
Collections.fill(nums, 42);
System.out.println(nums);  // [42, 42, 42, 42, 42]
```

### Swap

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));
Collections.swap(list, 0, 3);
System.out.println(list);  // [D, B, C, A]
```

### Rotate

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D", "E"));
Collections.rotate(list, 2);
System.out.println(list);  // [D, E, A, B, C]
```

### Replace All

```java
List<String> list = new ArrayList<>(List.of("Apple", "Banana", "Cherry"));
Collections.replaceAll(list, "Banana", "Blueberry");
System.out.println(list);  // [Apple, Blueberry, Cherry]
```

### Add All

```java
List<String> target = new ArrayList<>(List.of("A", "B"));
Collections.addAll(target, "C", "D", "E");
System.out.println(target);  // [A, B, C, D, E]
```

---

## Summary

| Method | Description | Use Case |
|--------|-------------|----------|
| **sort** | List sort karo | Sorting data |
| **shuffle** | Randomly rearrange | Randomization, games |
| **reverse** | List reverse karo | Descending order |
| **unmodifiableList** | Read-only view | Immutable collections |
| **synchronizedList** | Thread-safe wrapper | Multi-threaded apps |
| **frequency** | Element count karo | Occurrence counting |
| **disjoint** | Common elements check karo | Overlap detection |
| **nCopies** | N copies ki list banao | Default initialization |
| **binarySearch** | Sorted list mein search | O(log n) search |
| **min/max** | Min/max element find karo | Finding extremes |
| **fill** | Sab elements ko same value do | Resetting lists |
