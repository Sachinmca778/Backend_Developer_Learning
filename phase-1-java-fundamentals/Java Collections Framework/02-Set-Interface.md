# Set Interface

## Status: Not Started

---

## Table of Contents

1. [Set Interface Overview](#set-interface-overview)
2. [HashSet](#hashset)
3. [LinkedHashSet](#linkedhashset)
4. [TreeSet](#treeset)
5. [EnumSet](#enumset)
6. [HashSet vs LinkedHashSet vs TreeSet](#hashset-vs-linkedhashset-vs-treeset)

---

## Set Interface Overview

**Matlab:** Collection jo unique elements store karta hai — duplicates not allowed, at most one null.

```
Collection (interface)
    ↑
Set (interface) — unique elements, no duplicates
    ↑
    ├── HashSet
    ├── LinkedHashSet
    ├── TreeSet
    └── EnumSet
```

### Set Features

```java
Set<String> set = new HashSet<>();

// Add elements
set.add("Apple");     // true
set.add("Banana");    // true
set.add("Apple");     // false (duplicate — add nahi hoga)

// Size
int size = set.size();  // 2

// Contains
boolean hasApple = set.contains("Apple");  // true

// Remove
set.remove("Apple");  // true
set.remove("Grape");  // false (not present)

// Check empty
boolean isEmpty = set.isEmpty();  // false

// Clear
set.clear();
```

### No Duplicates — How It Works

```java
Set<User> users = new HashSet<>();
users.add(new User("Sachin", 25));
users.add(new User("Sachin", 25));  // Duplicate? Depends on equals() & hashCode()

// User class must override equals() and hashCode() for proper duplicate detection
class User {
    String name;
    int age;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return age == user.age && name.equals(user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
```

---

## HashSet

**Matlab:** Hash table based Set — O(1) operations, no order guarantee.

### Internal Working

```
HashMap internally use hota hai
Add → HashMap.put(element, PRESENT)
PRESENT = ek dummy Object
```

### Basic Usage

```java
HashSet<String> set = new HashSet<>();
set.add("Apple");
set.add("Banana");
set.add("Cherry");
set.add("Apple");  // Duplicate — ignore

System.out.println(set);  // [Apple, Cherry, Banana] — order guaranteed nahi hai

// Initial capacity
HashSet<String> set2 = new HashSet<>(100);  // 100 ki initial capacity

// Load factor (default 0.75)
HashSet<String> set3 = new HashSet<>(100, 0.5f);  // 50% full hone pe resize
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **add(element)** | O(1) average | Hash collision pe O(n) |
| **remove(element)** | O(1) average | Hash collision pe O(n) |
| **contains(element)** | O(1) average | Hash collision pe O(n) |
| **iteration** | O(n) | Order unpredictable |

### How hashCode() Works

```java
// Object ka hashCode → bucket index → us bucket mein store
// Same hashCode wale objects ek bucket mein jaate hain (collision)
// Collision resolve karne ke liye equals() use hota hai

class Person {
    String name;
    int age;

    @Override
    public int hashCode() {
        return Objects.hash(name, age);  // name + age ka combined hash
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person)) return false;
        Person p = (Person) o;
        return age == p.age && name.equals(p.name);
    }
}
```

### HashSet Iteration

```java
Set<String> set = Set.of("Apple", "Banana", "Cherry");

// for-each
for (String fruit : set) {
    System.out.println(fruit);
}

// Iterator
Iterator<String> it = set.iterator();
while (it.hasNext()) {
    System.out.println(it.next());
}

// Stream
set.stream().filter(s -> s.startsWith("A")).forEach(System.out::println);
```

---

## LinkedHashSet

**Matlab:** HashSet + insertion order maintain karta hai — doubly linked list use karta hai internally.

### Basic Usage

```java
LinkedHashSet<String> set = new LinkedHashSet<>();
set.add("Apple");
set.add("Banana");
set.add("Cherry");
set.add("Apple");  // Duplicate — ignore

System.out.println(set);  // [Apple, Banana, Cherry] — insertion order maintained!
```

### Performance

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **add/remove/contains** | O(1) | Same as HashSet |
| **iteration** | O(n) | **Faster than HashSet** — linked list follow karta hai |
| **Memory** | More | Doubly linked list overhead |

### When to Use

```java
// ✅ Use LinkedHashSet when:
// - Insertion order maintain karna hai
// - Duplicates avoid karne hain
// - Iteration performance important hai

// Example: Remove duplicates from List while preserving order
List<String> list = List.of("Apple", "Banana", "Apple", "Cherry", "Banana");
LinkedHashSet<String> set = new LinkedHashSet<>(list);
List<String> unique = new ArrayList<>(set);
// ["Apple", "Banana", "Cherry"] — order preserved
```

---

## TreeSet

**Matlab:** Sorted Set — elements automatically sort hote hain. Red-Black tree internally use hota hai.

### Basic Usage

```java
TreeSet<Integer> set = new TreeSet<>();
set.add(5);
set.add(2);
set.add(8);
set.add(1);
set.add(5);  // Duplicate — ignore

System.out.println(set);  // [1, 2, 5, 8] — sorted!

// String sorting (natural/alphabetical)
TreeSet<String> names = new TreeSet<>();
names.add("Sachin");
names.add("Rahul");
names.add("Priya");
System.out.println(names);  // [Priya, Rahul, Sachin]
```

### Sorted Set Operations

```java
TreeSet<Integer> set = new TreeSet<>(Set.of(1, 2, 5, 8, 10, 15, 20));

// First & Last
System.out.println(set.first());    // 1
System.out.println(set.last());     // 20

// Lower/Greater
System.out.println(set.lower(10));    // 8 (strictly less than)
System.out.println(set.higher(10));   // 15 (strictly greater than)

// Floor/Ceiling
System.out.println(set.floor(10));    // 10 (less than or equal)
System.out.println(set.ceiling(10));  // 10 (greater than or equal)

// SubSet
SortedSet<Integer> sub = set.subSet(5, 15);  // [5, 8, 10] (5 inclusive, 15 exclusive)

// HeadSet/TailSet
SortedSet<Integer> head = set.headSet(10);   // [1, 2, 5, 8]
SortedSet<Integer> tail = set.tailSet(10);   // [10, 15, 20]

// Descending
NavigableSet<Integer> desc = set.descendingSet();
System.out.println(desc);  // [20, 15, 10, 8, 5, 2, 1]
```

### Custom Sorting (Comparator)

```java
// Reverse order
TreeSet<Integer> descSet = new TreeSet<>(Comparator.reverseOrder());
descSet.addAll(Set.of(5, 2, 8, 1));
System.out.println(descSet);  // [8, 5, 2, 1]

// String length ke basis pe
TreeSet<String> byLength = new TreeSet<>(Comparator.comparingInt(String::length));
byLength.addAll(Set.of("Apple", "Banana", "Fig", "Cherry"));
System.out.println(byLength);  // [Fig, Apple, Cherry, Banana] (3, 5, 6, 6)

// Case-insensitive string sorting
TreeSet<String> caseInsensitive = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
caseInsensitive.addAll(Set.of("banana", "Apple", "cherry"));
System.out.println(caseInsensitive);  // [Apple, banana, cherry]
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **add/remove/contains** | O(log n) | Tree traversal |
| **first/last** | O(log n) | Tree edge |
| **subSet/headSet/tailSet** | O(log n + k) | k = returned elements |
| **iteration** | O(n) | In-order traversal |

### When to Use

```java
// ✅ Use TreeSet when:
// - Sorted elements chahiye
// - Range queries (subSet, headSet, tailSet)
// - Natural ordering ya custom ordering chahiye

// ❌ Avoid when:
// - O(1) operations chahiye (use HashSet)
// - Insertion order maintain karna hai (use LinkedHashSet)
```

---

## EnumSet

**Matlab:** Enum types ke liye optimized Set — bahut fast aur memory efficient.

### Basic Usage

```java
enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

// Create EnumSet
EnumSet<Day> weekdays = EnumSet.of(Day.MONDAY, Day.TUESDAY, Day.WEDNESDAY, Day.THURSDAY, Day.FRIDAY);

System.out.println(weekdays);  // [MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY]

// All enum values
EnumSet<Day> allDays = EnumSet.allOf(Day.class);

// Range of enum values
EnumSet<Day> workDays = EnumSet.range(Day.MONDAY, Day.FRIDAY);

// Complement (sab except given)
EnumSet<Day> weekend = EnumSet.complementOf(workDays);
System.out.println(weekend);  // [SATURDAY, SUNDAY]

// None
EnumSet<Day> empty = EnumSet.noneOf(Day.class);
```

### Performance

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **add/remove/contains** | O(1) | Bit manipulation |
| **Memory** | Very low | Bit vector internally |
| **Iteration** | O(n) | Very fast |

### Internal Working

```
Enum internally ordinal values use karta hai:
MONDAY = 0, TUESDAY = 1, ..., SUNDAY = 6

EnumSet internally bit vector use karta hai:
weekdays = 0b0011111 (bits 0-4 set)
weekend  = 0b1100000 (bits 5-6 set)

Operations are bitwise — extremely fast!
```

### When to Use

```java
// ✅ Use EnumSet when:
// - Enum values ka set chahiye
// - Performance critical hai
// - Memory efficient chahiye

// ❌ Use HashSet instead when:
// - Non-enum values hain
```

---

## HashSet vs LinkedHashSet vs TreeSet

| Feature | HashSet | LinkedHashSet | TreeSet |
|---------|---------|---------------|---------|
| **Order** | No order | Insertion order | Sorted order |
| **Performance** | O(1) | O(1) | O(log n) |
| **Null Values** | One null allowed | One null allowed | ❌ No null (compareTo throws NPE) |
| **Internal** | Hash table | Hash table + linked list | Red-Black tree |
| **Memory** | Less | More (linked list overhead) | More (tree nodes) |
| **Use When** | Fast operations, no order | Order matters + fast ops | Sorted data needed |

### Decision Guide

```
Set chahiye?
├── Sorted elements chahiye? → TreeSet
├── Insertion order maintain karna hai? → LinkedHashSet
└── Sirf uniqueness chahiye, order matter nahi? → HashSet
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Set** | Unique elements, no duplicates, at most one null |
| **HashSet** | O(1) operations, no order guarantee |
| **LinkedHashSet** | O(1) operations + insertion order maintained |
| **TreeSet** | O(log n) operations, sorted, range queries support |
| **EnumSet** | Enum types ke liye — bit vector internally, extremely fast |
| **equals() + hashCode()** | Dono override karo — proper duplicate detection ke liye |
