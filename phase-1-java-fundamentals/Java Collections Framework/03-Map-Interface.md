# Map Interface

## Status: Not Started

---

## Table of Contents

1. [Map Interface Overview](#map-interface-overview)
2. [HashMap](#hashmap)
3. [LinkedHashMap](#linkedhashmap)
4. [TreeMap](#treemap)
5. [Hashtable](#hashtable)
6. [EnumMap](#enummap)
7. [IdentityHashMap](#identityhashmap)
8. [WeakHashMap](#weakhashmap)
9. [HashMap vs TreeMap vs LinkedHashMap](#hashmap-vs-treemap-vs-linkedhashmap)

---

## Map Interface Overview

**Matlab:** Key-Value pairs store karta hai — keys unique hoti hain, values duplicate ho sakti hain.

```
Map (interface) — Key-Value pairs
    ↑
    ├── HashMap
    ├── LinkedHashMap
    ├── TreeMap
    ├── Hashtable
    ├── EnumMap
    ├── IdentityHashMap
    └── WeakHashMap
```

### Map Features

```java
Map<String, Integer> map = new HashMap<>();

// Put (add)
map.put("Apple", 1);
map.put("Banana", 2);
map.put("Cherry", 3);

// Get
Integer value = map.get("Apple");  // 1
Integer missing = map.get("Grape"); // null (key not found)

// Get with default
Integer val = map.getOrDefault("Grape", 0);  // 0

// Contains
boolean hasKey = map.containsKey("Apple");   // true
boolean hasValue = map.containsValue(2);     // true

// Remove
map.remove("Apple");  // Returns 1 (removed value)
map.remove("Grape");  // Returns null (not present)

// Size
int size = map.size();  // 2

// Check empty
boolean isEmpty = map.isEmpty();  // false

// Clear
map.clear();
```

### Iterating Map

```java
Map<String, Integer> map = Map.of("Apple", 1, "Banana", 2, "Cherry", 3);

// KeySet iteration
for (String key : map.keySet()) {
    System.out.println(key + " → " + map.get(key));
}

// Values iteration
for (Integer value : map.values()) {
    System.out.println(value);
}

// EntrySet iteration (recommended)
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + " → " + entry.getValue());
}

// forEach (Java 8+)
map.forEach((key, value) -> System.out.println(key + " → " + value));

// Stream
map.entrySet().stream()
    .filter(e -> e.getValue() > 1)
    .forEach(e -> System.out.println(e.getKey() + " → " + e.getValue()));
```

---

## HashMap

**Matlab:** Hash table based Map — O(1) average operations, allows one null key, no order guarantee.

### Internal Working

```
HashMap internally array of nodes use karta hai
Each node: key, value, hash, next (for collision handling)
Java 8+: Collision resolve karne ke liye linked list → balanced tree (if many collisions)
Default capacity: 16, Load factor: 0.75
```

### Basic Usage

```java
HashMap<String, Integer> map = new HashMap<>();
map.put("Apple", 1);
map.put("Banana", 2);

// Null key allowed
map.put(null, 0);

// Null value allowed
map.put("Cherry", null);

System.out.println(map);  // {null=0, Apple=1, Cherry=null, Banana=2} — order unpredictable
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **put(key, value)** | O(1) average | Collision pe O(log n) |
| **get(key)** | O(1) average | Collision pe O(log n) |
| **remove(key)** | O(1) average | Collision pe O(log n) |
| **containsKey(key)** | O(1) average | Collision pe O(log n) |
| **containsValue(value)** | O(n) | Full traversal |
| **iteration** | O(n) | Order unpredictable |

### HashMap Methods

```java
Map<String, Integer> map = new HashMap<>();

// putIfAbsent (Java 8+)
map.putIfAbsent("Apple", 1);  // Adds if key not present
map.putIfAbsent("Apple", 5);  // No change — key already exists

// compute (Java 8+)
map.compute("Apple", (key, value) -> (value == null) ? 1 : value + 1);

// computeIfAbsent
map.computeIfAbsent("Banana", k -> 0);  // Adds Banana=0

// computeIfPresent
map.computeIfPresent("Apple", (k, v) -> v + 1);  // Only if present

// merge
map.merge("Apple", 1, Integer::sum);  // If present: old + 1, else: 1

// replace
map.replace("Apple", 10);  // Replace value if key exists
map.replace("Apple", 1, 10);  // Replace only if current value is 1

// getOrDefault
int value = map.getOrDefault("Grape", 0);  // 0 (default)
```

### Word Frequency Counter Example

```java
String text = "apple banana apple cherry banana apple";
String[] words = text.split(" ");

Map<String, Integer> freq = new HashMap<>();
for (String word : words) {
    freq.put(word, freq.getOrDefault(word, 0) + 1);
}

System.out.println(freq);  // {apple=3, banana=2, cherry=1}
```

---

## LinkedHashMap

**Matlab:** HashMap + insertion order maintain karta hai — doubly linked list internally.

### Basic Usage

```java
LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
map.put("Apple", 1);
map.put("Banana", 2);
map.put("Cherry", 3);

System.out.println(map);  // {Apple=1, Banana=2, Cherry=3} — insertion order!
```

### Access Order Mode

```java
// Access order — recently accessed entries end mein jaate hain
LinkedHashMap<String, Integer> map = new LinkedHashMap<>(16, 0.75f, true);

map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
System.out.println(map);  // {A=1, B=2, C=3}

map.get("A");  // Access A
System.out.println(map);  // {B=2, C=3, A=1} — A end mein gaya

map.get("B");  // Access B
System.out.println(map);  // {C=3, A=1, B=2} — B end mein gaya
```

### LRU Cache Implementation

```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;

    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);  // Access order
        this.capacity = capacity;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;  // Remove oldest when capacity exceeded
    }
}

// Usage
LRUCache<String, Integer> cache = new LRUCache<>(3);
cache.put("A", 1);
cache.put("B", 2);
cache.put("C", 3);
cache.put("D", 4);  // "A" removed (oldest)

System.out.println(cache);  // {B=2, C=3, D=4}
```

### Performance

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **put/get/remove** | O(1) | Same as HashMap |
| **iteration** | O(n) | **Faster than HashMap** — linked list follow |
| **Memory** | More | Linked list overhead |

---

## TreeMap

**Matlab:** Sorted Map — keys automatically sort hote hain. Red-Black tree internally.

### Basic Usage

```java
TreeMap<String, Integer> map = new TreeMap<>();
map.put("Banana", 2);
map.put("Apple", 1);
map.put("Cherry", 3);

System.out.println(map);  // {Apple=1, Banana=2, Cherry=3} — sorted by keys!
```

### NavigableMap Operations

```java
TreeMap<Integer, String> map = new TreeMap<>();
map.put(1, "One");
map.put(3, "Three");
map.put(5, "Five");
map.put(7, "Seven");
map.put(10, "Ten");

// First & Last
System.out.println(map.firstKey());     // 1
System.out.println(map.lastKey());      // 10
System.out.println(map.firstEntry());   // 1=One
System.out.println(map.lastEntry());    // 10=Ten

// Lower/Higher
System.out.println(map.lowerKey(5));     // 3 (strictly less than)
System.out.println(map.higherKey(5));    // 7 (strictly greater than)

// Floor/Ceiling
System.out.println(map.floorKey(5));     // 5 (less than or equal)
System.out.println(map.ceilingKey(5));   // 5 (greater than or equal)

// SubMap
SortedMap<Integer, String> sub = map.subMap(3, 7);   // {3=Three, 5=Five} (3 inclusive, 7 exclusive)
NavigableMap<Integer, String> sub2 = map.subMap(3, true, 7, true);  // Both inclusive

// HeadMap/TailMap
SortedMap<Integer, String> head = map.headMap(5);   // {1=One, 3=Three}
SortedMap<Integer, String> tail = map.tailMap(5);   // {5=Five, 7=Seven, 10=Ten}

// Descending
NavigableMap<Integer, String> desc = map.descendingMap();
System.out.println(desc);  // {10=Ten, 7=Seven, 5=Five, 3=Three, 1=One}

// Poll First/Last
Map.Entry<Integer, String> first = map.pollFirstEntry();  // Remove and return first
Map.Entry<Integer, String> last = map.pollLastEntry();    // Remove and return last
```

### Custom Sorting

```java
// Reverse order
TreeMap<String, Integer> descMap = new TreeMap<>(Comparator.reverseOrder());
descMap.put("Banana", 2);
descMap.put("Apple", 1);
System.out.println(descMap);  // {Banana=2, Apple=1}

// Length ke basis pe
TreeMap<String, Integer> byLength = new TreeMap<>(Comparator.comparingInt(String::length));
byLength.put("Fig", 3);
byLength.put("Apple", 5);
byLength.put("Banana", 6);
System.out.println(byLength);  // {Fig=3, Apple=5, Banana=6}
```

### Performance

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **put/get/remove** | O(log n) | Tree traversal |
| **containsKey** | O(log n) | Tree search |
| **firstKey/lastKey** | O(log n) | Tree edge |
| **subMap/headMap/tailMap** | O(log n + k) | k = returned entries |
| **iteration** | O(n) | In-order traversal |

---

## Hashtable

**Matlab:** Legacy synchronized Map — thread-safe lekin slow. Java 1.0 se hai.

### Basic Usage

```java
Hashtable<String, Integer> table = new Hashtable<>();
table.put("Apple", 1);
table.put("Banana", 2);

// Null keys/values NOT allowed
table.put(null, 0);  // ❌ NullPointerException
table.put("Cherry", null);  // ❌ NullPointerException
```

### Hashtable vs HashMap

| Feature | Hashtable | HashMap |
|---------|-----------|---------|
| **Synchronized** | ✅ Yes | ❌ No |
| **Null Keys** | ❌ No | ✅ One null |
| **Null Values** | ❌ No | ✅ Multiple nulls |
| **Performance** | Slow | Fast |
| **Legacy** | Java 1.0 | Java 1.2 |
| **Iterator** | Enumeration (fail-safe) | Iterator (fail-fast) |
| **Recommended?** | ❌ Avoid | ✅ Yes |

### Modern Alternatives

```java
// ✅ Instead of Hashtable use:
// Thread-safe Map
Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();

// Synchronized Map
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
```

---

## EnumMap

**Matlab:** Enum keys ke liye optimized Map — array internally use karta hai.

### Basic Usage

```java
enum Day { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

EnumMap<Day, String> schedule = new EnumMap<>(Day.class);
schedule.put(Day.MONDAY, "Gym");
schedule.put(Day.TUESDAY, "Work");
schedule.put(Day.WEDNESDAY, "Study");

System.out.println(schedule);
// {MONDAY=Gym, TUESDAY=Work, WEDNESDAY=Study}
// Order: Enum declaration order

// All values
EnumMap<Day, String> allDays = new EnumMap<>(Day.class);
for (Day day : Day.values()) {
    allDays.put(day, "Free");
}

// Get
String mondayTask = schedule.get(Day.MONDAY);  // "Gym"
String sundayTask = schedule.get(Day.SUNDAY);  // null (not present)
```

### Performance

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **put/get/remove** | O(1) | Array index access |
| **Memory** | Very low | Array internally |
| **Iteration** | O(n) | Very fast |

### When to Use

```java
// ✅ Use EnumMap when:
// - Enum type ka key hai
// - Performance critical hai

// ❌ Use HashMap when:
// - Non-enum keys hain
```

---

## IdentityHashMap

**Matlab:** Reference equality (==) use karta hai — equals() nahi.

### Basic Usage

```java
Map<String, Integer> map = new IdentityHashMap<>();

String s1 = new String("Apple");
String s2 = new String("Apple");

map.put(s1, 1);
map.put(s2, 2);  // Different reference — alag entry banegi!

System.out.println(map.size());  // 2 (HashMap mein 1 hota)
System.out.println(map.get(s1)); // 1
System.out.println(map.get(s2)); // 2
```

### When to Use

```java
// ✅ Use IdentityHashMap when:
// - Reference equality chahiye (not equals)
// - Object graph traversal
// - Proxy/decoration pattern tracking

// ❌ Avoid for normal use cases
```

---

## WeakHashMap

**Matlab:** Weak references use karta hai — keys automatically garbage collect ho jaate hain jab koi strong reference nahi bachta.

### Basic Usage

```java
Map<String, String> cache = new WeakHashMap<>();

String key1 = new String("key1");
cache.put(key1, "value1");

System.out.println(cache.get(key1));  // "value1"

// key1 ko null karo — ab koi strong reference nahi
key1 = null;

// GC run karo
System.gc();

// Ab entry automatically remove ho gayi
System.out.println(cache.size());  // 0 (possibly)
```

### When to Use

```java
// ✅ Use WeakHashMap when:
// - Automatic cleanup chahiye
// - Metadata/cache associate with objects
// - Memory leak se bachna hai

// Example: Object metadata cache
WeakHashMap<Object, String> metadata = new WeakHashMap<>();
metadata.put(userObject, "Some metadata");
// Jab userObject garbage collect hoga → metadata entry automatically remove hogi
```

---

## HashMap vs TreeMap vs LinkedHashMap

| Feature | HashMap | TreeMap | LinkedHashMap |
|---------|---------|---------|---------------|
| **Order** | No order | Sorted by keys | Insertion/Access order |
| **Performance** | O(1) | O(log n) | O(1) |
| **Null Keys** | ✅ One | ❌ No | ✅ One |
| **Internal** | Hash table | Red-Black tree | Hash table + linked list |
| **Use When** | Fast operations | Sorted keys needed | Order matters |

### Decision Guide

```
Map chahiye?
├── Sorted keys chahiye? → TreeMap
├── Insertion order maintain karna hai? → LinkedHashMap
├── Enum keys hain? → EnumMap
├── Thread-safety chahiye? → ConcurrentHashMap
└── Fast operations, no order? → HashMap
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Map** | Key-Value pairs, unique keys, duplicate values allowed |
| **HashMap** | O(1) operations, null key/value allowed, no order |
| **LinkedHashMap** | O(1) + insertion/access order maintained |
| **TreeMap** | O(log n), sorted keys, navigable operations |
| **Hashtable** | Legacy synchronized — avoid, use ConcurrentHashMap |
| **EnumMap** | Enum keys ke liye — array internally, extremely fast |
| **IdentityHashMap** | Reference equality (==) use karta hai |
| **WeakHashMap** | Keys weak reference — auto garbage collection |
