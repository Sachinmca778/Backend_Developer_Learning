# Java Collections Framework - Top 10 Interview Questions & Answers

> Covering: List, Set, Map, Queue/Deque, Collections Utility, Comparable/Comparator, Iterators, fail-fast vs fail-safe, and Common Pitfalls

---

## Q1: ArrayList vs LinkedList — internal working, performance, aur kab kaunsa use karte hain?

**Answer:**

| Feature | ArrayList | LinkedList |
|---------|-----------|------------|
| **Internal Structure** | Dynamic array | Doubly linked list |
| **get(index)** | O(1) | O(n) |
| **add(end)** | O(1) amortized | O(1) |
| **add(middle)** | O(n) — shift elements | O(n) — traverse to position |
| **remove(middle)** | O(n) — shift | O(n) — traverse |
| **Memory** | Less overhead | More (node pointers) |
| **Implements** | List, RandomAccess | List, Deque, Queue |

### ArrayList Internal Working

```
ArrayList internally Object[] array use karta hai
Default capacity: 10
Full hone pe → naya array (1.5x size) → elements copy
```

```java
List<String> list = new ArrayList<>();
list.add("Apple");   // O(1) amortized
list.get(0);         // O(1) — direct index access
list.add(0, "Mango"); // O(n) — elements shift right
```

### LinkedList Internal Working

```
Node: [prev | data | next] ↔ [prev | data | next] ↔ ...
Head ←→ ... ←→ Tail
```

```java
LinkedList<String> list = new LinkedList<>();
list.add("Apple");       // O(1) — tail pe add
list.addFirst("Mango");  // O(1) — head pe add
list.get(2);             // O(n) — traverse from head/tail
```

### Performance Comparison

| Operation | ArrayList | LinkedList |
|-----------|-----------|------------|
| Access by index | O(1) ✅ | O(n) |
| Add at end | O(1) ✅ | O(1) ✅ |
| Add at beginning | O(n) | O(1) ✅ |
| Remove by index | O(n) | O(n) |
| Search (contains) | O(n) | O(n) |
| Memory overhead | Lower ✅ | Higher (2 pointers per node) |

### When to Use What

```java
// ✅ ArrayList — default choice for List
// - Frequent random access (get by index)
// - Mostly add/remove at end
// - Iteration heavy workloads
List<User> users = new ArrayList<>();

// ✅ LinkedList — specific use cases
// - Frequent add/remove at beginning
// - Queue/Deque operations needed
// - No random access needed
Deque<Task> taskQueue = new LinkedList<>();
```

**Important:**

- **90% cases mein ArrayList** sufficient hai — better cache locality, less memory
- LinkedList **rarely faster** in practice despite O(1) insert — traversal overhead dominates
- **Vector** = synchronized ArrayList (legacy) — use `CopyOnWriteArrayList` or `Collections.synchronizedList` instead

**Short Interview Answer:**

ArrayList = dynamic array, O(1) get, O(n) middle insert. LinkedList = doubly linked, O(1) head/tail ops, O(n) get. Default to ArrayList unless frequent head insertions or Deque needed.

---

## Q2: HashSet vs TreeSet vs LinkedHashSet — difference aur use cases?

**Answer:**

| Feature | HashSet | LinkedHashSet | TreeSet |
|---------|---------|---------------|---------|
| **Internal** | HashMap (hash table) | HashSet + linked list | Red-Black Tree |
| **Order** | No guarantee | Insertion order | Sorted order |
| **null allowed** | ✅ One null | ✅ One null | ❌ No null (NPE) |
| **add/contains/remove** | O(1) avg | O(1) avg | O(log n) |
| **Iteration order** | Unpredictable | Insertion order | Sorted |
| **Comparator** | No | No | Natural/Custom |

### HashSet

```java
Set<String> set = new HashSet<>();
set.add("Banana");
set.add("Apple");
set.add("Cherry");
set.add("Apple");  // Duplicate — ignored

System.out.println(set);  // [Apple, Cherry, Banana] — order unpredictable
// Internally HashMap use hota hai — element as key, dummy PRESENT as value
```

### LinkedHashSet — Insertion Order

```java
Set<String> set = new LinkedHashSet<>();
set.add("Banana");
set.add("Apple");
set.add("Cherry");

System.out.println(set);  // [Banana, Apple, Cherry] — insertion order maintained!
```

### TreeSet — Sorted Order

```java
Set<Integer> set = new TreeSet<>();
set.add(5);
set.add(2);
set.add(8);
set.add(1);

System.out.println(set);  // [1, 2, 5, 8] — sorted!

// Custom comparator — reverse order
Set<Integer> maxSet = new TreeSet<>(Comparator.reverseOrder());
// set.add(null);  // ❌ NullPointerException
```

### hashCode() & equals() — Critical for HashSet

```java
class User {
    String name;
    int age;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return age == user.age && Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);  // MUST override both!
    }
}

Set<User> users = new HashSet<>();
users.add(new User("Sachin", 25));
users.add(new User("Sachin", 25));  // Duplicate — ignored only if equals+hashCode correct
```

### When to Use What

```
HashSet       → Unique elements, order doesn't matter, fastest
LinkedHashSet → Unique + insertion order (LRU cache-like)
TreeSet       → Unique + sorted order, range queries (headSet, tailSet)
```

**Important:**

- Custom objects ke liye **`equals()` + `hashCode()` override** mandatory for HashSet/LinkedHashSet
- TreeSet mein elements **`Comparable` implement** karein ya **Comparator** pass karo
- **HashSet iteration order unpredictable** — test mein assume mat karo

**Short Interview Answer:**

HashSet = hash table, no order, O(1). LinkedHashSet = insertion order. TreeSet = sorted, O(log n), no null. Override equals+hashCode for custom objects in HashSet.

---

## Q3: HashMap vs ConcurrentHashMap vs Hashtable — difference aur thread safety?

**Answer:**

| Feature | HashMap | ConcurrentHashMap | Hashtable |
|---------|---------|-------------------|-----------|
| **Thread-safe** | ❌ No | ✅ Yes | ✅ Yes |
| **Null key/value** | ✅ One null key, null values | ❌ No null | ❌ No null |
| **Locking** | None | Segment/CAS lock (fine-grained) | Entire table synchronized |
| **Performance** | Fastest (single-thread) | Good (multi-thread) | Slow (full lock) |
| **Iterator** | fail-fast | Weakly consistent | fail-fast (Enum) |
| **Legacy** | No | No (Java 5+) | Yes (deprecated approach) |

### HashMap Internal (Java 8+)

```
Array of Node buckets
Each Node: hash, key, value, next
Collision → linked list → balanced tree (if > 8 nodes)
Default capacity: 16, Load factor: 0.75
Resize when size > capacity * loadFactor
```

```java
Map<String, Integer> map = new HashMap<>();
map.put("Apple", 1);
map.put(null, 0);       // ✅ One null key allowed
map.put("Cherry", null); // ✅ Null value allowed

map.getOrDefault("Grape", 0);  // 0
map.putIfAbsent("Banana", 2);
map.merge("Apple", 1, Integer::sum);  // Apple = 2
```

### ConcurrentHashMap — Thread-Safe, Scalable

```java
Map<String, Integer> map = new ConcurrentHashMap<>();
map.put("Apple", 1);

// Thread-safe without external synchronization
// Fine-grained locking — poora map lock nahi hota
// Better performance than Hashtable in multi-threaded apps

// ❌ Null not allowed
// map.put(null, 1);  // NullPointerException
// map.put("key", null);  // NullPointerException
```

### Hashtable — Legacy, Avoid

```java
// ❌ Legacy — poor performance (synchronized on every operation)
Hashtable<String, Integer> table = new Hashtable<>();

// ✅ Use instead:
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
// OR
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
```

### HashMap Thread-Safety Options

```java
// Option 1: ConcurrentHashMap (best for concurrent reads/writes)
Map<String, Integer> map1 = new ConcurrentHashMap<>();

// Option 2: Collections.synchronizedMap (legacy wrapper)
Map<String, Integer> map2 = Collections.synchronizedMap(new HashMap<>());
// Iteration pe manual sync chahiye:
synchronized (map2) { for (var e : map2.entrySet()) { } }

// Option 3: Immutable map (read-only, thread-safe)
Map<String, Integer> map3 = Map.of("A", 1, "B", 2);
```

**Important:**

- **Single-threaded → HashMap** (fastest)
- **Multi-threaded → ConcurrentHashMap** (not Hashtable!)
- HashMap **fail-fast iterator** — concurrent modification pe CME
- **Load factor 0.75** default — resize at 75% capacity

**Short Interview Answer:**

HashMap = not thread-safe, allows null, fastest. ConcurrentHashMap = thread-safe, fine-grained lock, no null. Hashtable = legacy, synchronized entire map, avoid. Use ConcurrentHashMap for concurrent access.

---

## Q4: TreeMap kya hai? HashMap se kab prefer karte hain?

**Answer:**

**TreeMap** = Red-Black Tree based Map — keys **sorted order** mein store hote hain.

| Feature | HashMap | TreeMap |
|---------|---------|---------|
| **Order** | No guarantee | Sorted by key |
| **Null key** | ✅ One allowed | ❌ Not allowed |
| **Performance** | O(1) avg get/put | O(log n) get/put |
| **Implementation** | Hash table | Red-Black Tree |
| **Range queries** | ❌ | ✅ headMap, tailMap, subMap |

### TreeMap Usage

```java
Map<String, Integer> map = new TreeMap<>();
map.put("Cherry", 3);
map.put("Apple", 1);
map.put("Banana", 2);

System.out.println(map);  // {Apple=1, Banana=2, Cherry=3} — sorted by key!

// Custom comparator — reverse order
Map<String, Integer> reverseMap = new TreeMap<>(Comparator.reverseOrder());

// Range queries
TreeMap<Integer, String> scores = new TreeMap<>();
scores.put(90, "A");
scores.put(75, "C");
scores.put(85, "B");

scores.headMap(85);   // Keys < 85
scores.tailMap(85);   // Keys >= 85
scores.subMap(75, 90); // 75 <= keys < 90

scores.firstKey();    // 75
scores.lastKey();     // 90
scores.ceilingEntry(80);  // {85=B} — >= 80 ka first entry
scores.floorEntry(80);    // {75=C} — <= 80 ka last entry
```

### Natural Ordering Requirement

```java
// Keys must be Comparable OR pass Comparator
Map<User, Integer> map = new TreeMap<>(Comparator.comparing(User::getName));

class User implements Comparable<User> {
    @Override
    public int compareTo(User other) {
        return this.name.compareTo(other.name);
    }
}
```

### When to Use TreeMap

```
✅ Sorted key iteration chahiye
✅ Range queries (all keys between X and Y)
✅ Nearest key search (ceiling, floor)
✅ NavigableMap operations

❌ Avoid when:
- Order doesn't matter (use HashMap — faster)
- Null keys needed
- Maximum performance needed (O(1) vs O(log n))
```

**Short Interview Answer:**

TreeMap = sorted keys via Red-Black Tree, O(log n) operations, no null keys. Use for sorted iteration and range queries. HashMap when order irrelevant and speed matters.

---

## Q5: fail-fast vs fail-safe iterators — kya difference hai?

**Answer:**

| Feature | fail-fast | fail-safe |
|---------|-----------|-----------|
| **Collections** | ArrayList, HashMap, HashSet | ConcurrentHashMap, CopyOnWriteArrayList |
| **Behavior** | ConcurrentModificationException throw | Weakly consistent, no CME |
| **Iterator reflects** | Original collection (at creation) | Snapshot or weakly consistent view |
| **Performance** | Faster | Slower (copy overhead) |
| **Modification during iteration** | ❌ Not allowed (except Iterator.remove) | ✅ Allowed |

### fail-fast — ConcurrentModificationException

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C"));

// ❌ for-each se remove — CME!
for (String item : list) {
    if (item.equals("B")) {
        list.remove(item);  // ConcurrentModificationException!
    }
}

// ✅ Iterator.remove() use karo
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    if (it.next().equals("B")) {
        it.remove();  // Safe
    }
}
```

### How fail-fast Works

```
Iterator create hote waqt expectedModCount store karta hai
Collection modify hone pe modCount change hota hai
Iterator check karta hai: expectedModCount == modCount?
Mismatch → ConcurrentModificationException
```

### fail-safe — No CME

```java
// CopyOnWriteArrayList — iteration pe snapshot
List<String> list = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));

for (String item : list) {
    if (item.equals("B")) {
        list.remove(item);  // ✅ No CME — iterates over snapshot
    }
}

// ConcurrentHashMap — weakly consistent iterator
Map<String, Integer> map = new ConcurrentHashMap<>();
map.put("A", 1);
map.put("B", 2);

for (String key : map.keySet()) {
    map.remove(key);  // ✅ Safe — may or may not see changes
}
```

**Important:**

- **fail-fast ≠ guaranteed** — best-effort detection, not bulletproof
- **for-each internally uses Iterator** — structural modification during for-each = CME
- **Concurrent collections** use fail-safe — trade-off: memory + consistency

**Short Interview Answer:**

fail-fast (ArrayList, HashMap) throws CME on concurrent modification during iteration. fail-safe (ConcurrentHashMap, CopyOnWriteArrayList) iterates snapshot/weak view — no CME. Use Iterator.remove() for safe removal.

---

## Q6: Comparable vs Comparator — difference aur examples?

**Answer:**

| Feature | Comparable | Comparator |
|---------|-----------|------------|
| **Package** | `java.lang.Comparable` | `java.util.Comparator` |
| **Method** | `compareTo(T o)` | `compare(T o1, T o2)` |
| **Where defined** | Inside the class | Outside the class |
| **Sorting type** | Natural ordering (one way) | Multiple/custom orderings |
| **Modify class?** | Must modify class | No modification needed |
| **Used by** | TreeSet, TreeMap, Collections.sort | Collections.sort(list, comp), TreeSet(comp) |

### Comparable — Natural Ordering

```java
class Employee implements Comparable<Employee> {
    String name;
    int salary;

    @Override
    public int compareTo(Employee other) {
        return Integer.compare(this.salary, other.salary);  // Sort by salary
    }
}

List<Employee> list = new ArrayList<>();
Collections.sort(list);  // Natural ordering — salary ascending
list.sort(null);         // Same
```

### Comparator — External/Custom Ordering

```java
class Employee {
    String name;
    int salary;
    int age;
}

// Sort by name
Comparator<Employee> byName = Comparator.comparing(e -> e.name);

// Sort by salary descending
Comparator<Employee> bySalaryDesc = Comparator.comparingInt((Employee e) -> e.salary).reversed();

// Multi-field sort — salary then name
Comparator<Employee> multi = Comparator
    .comparingInt((Employee e) -> e.salary)
    .thenComparing(e -> e.name);

list.sort(byName);
Collections.sort(list, multi);
```

### compareTo / compare Return Values

```
< 0  → first element comes BEFORE second
== 0 → equal (ordering doesn't matter)
> 0  → first element comes AFTER second
```

### Comparator Utility Methods (Java 8+)

```java
Comparator.comparing(Person::getName)           // By field
Comparator.comparingInt(Person::getAge)        // Primitive — no boxing
Comparator.comparing(Person::getName, String.CASE_INSENSITIVE_ORDER)
Comparator.nullsFirst(Comparator.naturalOrder()) // Null handling
comparator.reversed()                           // Reverse order
comparator.thenComparing(Person::getAge)        // Secondary sort
```

### ⚠️ compareTo vs equals Consistency

```java
// Rule: compareTo() == 0 MUST imply equals() == true
// Violation causes bugs in TreeSet/TreeMap (duplicates allowed incorrectly)
```

**Important:**

- **Comparable** = one natural order built into class
- **Comparator** = multiple sort strategies without modifying class
- TreeSet/TreeMap need **Comparable keys** or **Comparator** passed to constructor

**Short Interview Answer:**

Comparable = internal natural ordering via compareTo(). Comparator = external custom ordering via compare(). Comparable modifies class; Comparator doesn't. Use Comparator for multiple sort options.

---

## Q7: Queue, Deque, aur PriorityQueue explain karo — use cases?

**Answer:**

### Queue — FIFO

```
Collection → Queue (FIFO)
    ├── PriorityQueue (heap-based)
    ├── ArrayDeque (array-based deque)
    └── LinkedList (also implements Deque)
```

| Method | Throws Exception | Returns Special Value |
|--------|-----------------|----------------------|
| Insert | `add(e)` | `offer(e)` |
| Remove | `remove()` | `poll()` |
| Examine | `element()` | `peek()` |

```java
Queue<String> queue = new LinkedList<>();
queue.offer("Task1");
queue.offer("Task2");
queue.offer("Task3");

System.out.println(queue.peek());  // "Task1" — front, not removed
System.out.println(queue.poll());  // "Task1" — removed
System.out.println(queue.poll());  // "Task2"
```

### Deque — Double-Ended Queue

```java
Deque<String> deque = new ArrayDeque<>();

// Both ends pe add/remove — O(1)
deque.offerFirst("Front");  // Add at front
deque.offerLast("Back");    // Add at back
deque.pollFirst();          // Remove from front
deque.pollLast();           // Remove from back

// Stack operations
deque.push("A");   // = addFirst
deque.pop();       // = removeFirst
deque.peek();      // = peekFirst
```

### ArrayDeque vs LinkedList for Deque

| Feature | ArrayDeque | LinkedList |
|---------|-----------|------------|
| **Internal** | Circular array | Doubly linked list |
| **Performance** | Faster ✅ | Slower |
| **Null elements** | ❌ Not allowed | ✅ Allowed |
| **Memory** | Less overhead | More (pointers) |
| **Default choice** | ✅ Yes | Legacy |

```java
// ✅ Prefer ArrayDeque for Stack/Queue
Deque<Integer> stack = new ArrayDeque<>();
Deque<Integer> queue = new ArrayDeque<>();
```

### PriorityQueue — Min-Heap by Default

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(5);
pq.offer(2);
pq.offer(8);
pq.offer(1);

System.out.println(pq.peek());  // 1 (smallest)
while (!pq.isEmpty()) {
    System.out.println(pq.poll());  // 1, 2, 5, 8 (sorted removal)
}

// Max-heap
PriorityQueue<Integer> maxPQ = new PriorityQueue<>(Comparator.reverseOrder());
System.out.println(maxPQ.peek());  // 8 (largest)
```

### PriorityQueue Important Notes

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(5); pq.offer(2); pq.offer(8);

// ❌ Iteration order is NOT sorted!
for (int n : pq) {
    System.out.println(n);  // NOT guaranteed 2, 5, 8
}

// ✅ Sorted order ke liye poll karo
while (!pq.isEmpty()) System.out.println(pq.poll());

// ❌ null not allowed
pq.offer(null);  // NullPointerException
```

### Use Cases

```
Queue       → Task scheduling, BFS, order processing
Deque       → Stack, sliding window, palindrome check
ArrayDeque  → Default stack/queue implementation
PriorityQueue → Top K elements, Dijkstra, task priority scheduling
```

**Short Interview Answer:**

Queue = FIFO (offer/poll/peek). Deque = both ends — ArrayDeque preferred. PriorityQueue = heap, min-heap default, O(log n) insert/remove, O(1) peek. Iteration order ≠ sorted order for PriorityQueue.

---

## Q8: Collections utility class — sort, synchronized wrappers, unmodifiable collections?

**Answer:**

`java.util.Collections` — static utility methods for Collection operations.

### Collections.sort

```java
List<Integer> nums = new ArrayList<>(List.of(5, 2, 8, 1, 9));

Collections.sort(nums);  // [1, 2, 5, 8, 9] — natural order
Collections.sort(nums, Comparator.reverseOrder());  // descending

// Same as:
nums.sort(null);
nums.sort(Comparator.reverseOrder());

// Object sorting
Collections.sort(employees, Comparator.comparingInt(e -> e.salary));
```

### Collections.reverse, shuffle, fill

```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5));
Collections.reverse(list);  // [5, 4, 3, 2, 1]
Collections.shuffle(list);  // Random order
Collections.fill(list, 0);  // [0, 0, 0, 0, 0]

int count = Collections.frequency(list, 0);  // 5
boolean noOverlap = Collections.disjoint(list1, list2);  // true if no common
```

### Unmodifiable Wrappers — Read-Only View

```java
List<String> mutable = new ArrayList<>(List.of("A", "B", "C"));
List<String> immutable = Collections.unmodifiableList(mutable);

immutable.add("D");  // ❌ UnsupportedOperationException
mutable.add("D");  // ✅ Original still modifiable — wrapper doesn't copy!
```

### Synchronized Wrappers — Thread Safety

```java
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

// ⚠️ Iteration pe manual sync required
synchronized (syncList) {
    for (String item : syncList) {
        System.out.println(item);
    }
}
// Better: use ConcurrentHashMap, CopyOnWriteArrayList instead
```

### Other Useful Methods

```java
Collections.max(list);
Collections.min(list);
Collections.binarySearch(sortedList, key);  // List must be sorted!
Collections.nCopies(5, "X");  // ["X", "X", "X", "X", "X"]

// Empty immutable collections
Collections.emptyList();
Collections.emptyMap();
Collections.emptySet();

// Singleton immutable
Collections.singletonList("only");
Collections.singletonMap("key", 1);
```

**Important:**

- `unmodifiableXxx()` = **view** wrapper, not copy — backing collection change ho sakti hai
- **Truly immutable** → `List.of()`, `Map.of()`, `Set.of()` (Java 9+)
- `synchronizedXxx()` = legacy thread safety — prefer concurrent collections

**Short Interview Answer:**

Collections utility: sort, reverse, shuffle, frequency, binarySearch (sorted list required). unmodifiableXxx = read-only view. synchronizedXxx = thread-safe wrapper. Prefer List.of/Map.of for true immutability.

---

## Q9: Iterator vs ListIterator vs forEach — kab kaunsa use karte hain?

**Answer:**

| Feature | Iterator | ListIterator | forEach (Java 8+) |
|---------|----------|-------------|-------------------|
| **Collections** | All Collection | List only | Collection + Stream |
| **Direction** | Forward only | Forward + Backward | Forward only |
| **remove()** | ✅ | ✅ | ❌ |
| **add/set** | ❌ | ✅ | ❌ |
| **Index info** | ❌ | nextIndex/previousIndex | ❌ |
| **During iteration modify** | Iterator.remove only | add/set/remove | ❌ (CME risk) |

### Iterator — Basic Forward Iteration

```java
List<String> list = new ArrayList<>(List.of("Apple", "Banana", "Cherry"));

Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String fruit = it.next();
    if (fruit.length() > 5) {
        it.remove();  // ✅ Safe removal
    }
}
// list = [Apple, Date] (if Date was there)
```

### ListIterator — Bidirectional + Modify

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));
ListIterator<String> it = list.listIterator();

// Forward
while (it.hasNext()) {
    String item = it.next();
    if (item.equals("B")) it.set("X");  // Replace B with X
    if (item.equals("C")) it.add("NEW"); // Insert after C
}

// Backward from end
ListIterator<String> rev = list.listIterator(list.size());
while (rev.hasPrevious()) {
    System.out.println(rev.previous());
}
```

### forEach — Java 8+ Lambda

```java
List<String> list = List.of("Apple", "Banana", "Cherry");

// Collection.forEach
list.forEach(fruit -> System.out.println(fruit));
list.forEach(System.out::println);

// Stream forEach (can filter, map)
list.stream()
    .filter(f -> f.startsWith("A"))
    .forEach(System.out::println);

// Map forEach
Map<String, Integer> map = Map.of("A", 1, "B", 2);
map.forEach((key, value) -> System.out.println(key + " → " + value));
```

### for-each Internally Uses Iterator

```java
// This:
for (String item : list) {
    System.out.println(item);
}

// Compiler converts to:
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    System.out.println(item);
}
```

### When to Use What

```
Iterator       → Remove during iteration (any Collection)
ListIterator   → Bidirectional traversal, replace/add during iteration (List)
for-each       → Simple read-only iteration
forEach lambda → Functional style, method reference
Stream         → Filter/map/reduce pipeline
Indexed for    → Need index, modify by index
```

**Important:**

- **Remove during iteration** → Iterator.remove() or ListIterator.remove()
- **for-each/lambda forEach se remove** → ConcurrentModificationException
- **ListIterator** sirf List pe kaam karta hai

**Short Interview Answer:**

Iterator = forward, remove. ListIterator = bidirectional, add/set/remove, List only. forEach = simple read-only lambda iteration. Use Iterator.remove for safe deletion during iteration.

---

## Q10: Collections Framework common pitfalls — hashCode/equals, ConcurrentModification, null handling?

**Answer:**

### Pitfall 1: Missing hashCode()/equals() Override

```java
// ❌ Default equals — reference comparison
class BadUser {
    String name;
    // No hashCode/equals override
}

Set<BadUser> set = new HashSet<>();
set.add(new BadUser("Sachin"));
set.add(new BadUser("Sachin"));  // Both added — "duplicates"!
System.out.println(set.size());  // 2 ❌

// ✅ Override both
class GoodUser {
    String name;
    @Override public boolean equals(Object o) { /* ... */ }
    @Override public int hashCode() { return Objects.hash(name); }
}
```

**Contract:** Equal objects MUST have same hashCode. Same hashCode doesn't mean equal.

### Pitfall 2: Modifying Collection During Iteration

```java
List<Integer> list = new ArrayList<>(List.of(1, 2, 3, 4, 5));

// ❌ CME
for (Integer n : list) {
    if (n == 3) list.remove(n);
}

// ✅ Safe
list.removeIf(n -> n == 3);
// OR
Iterator<Integer> it = list.iterator();
while (it.hasNext()) {
    if (it.next() == 3) it.remove();
}
```

### Pitfall 3: Arrays.asList Fixed Size

```java
String[] arr = {"A", "B", "C"};
List<String> list = Arrays.asList(arr);

list.set(0, "X");  // ✅ OK — modifies backing array
list.add("D");     // ❌ UnsupportedOperationException

// ✅ Mutable copy
List<String> mutable = new ArrayList<>(Arrays.asList(arr));
```

### Pitfall 4: Null in TreeSet/TreeMap/ConcurrentHashMap

```java
TreeSet<String> set = new TreeSet<>();
set.add(null);  // ❌ NullPointerException

ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put(null, 1);  // ❌ NullPointerException
map.put("key", null);  // ❌ NullPointerException

// HashMap allows one null key and null values ✅
```

### Pitfall 5: Using Raw Types

```java
// ❌ Raw type — no type safety
List list = new ArrayList();
list.add("String");
list.add(123);  // Mixed types — ClassCastException at runtime

// ✅ Generics
List<String> safeList = new ArrayList<>();
```

### Pitfall 6: Comparable Inconsistency with equals

```java
class Person implements Comparable<Person> {
    String name;
    int age;

    @Override
    public int compareTo(Person other) {
        return Integer.compare(this.age, other.age);  // Compare by age only
    }

    @Override
    public boolean equals(Object o) {
        // Compare by name AND age — INCONSISTENT!
        Person p = (Person) o;
        return this.name.equals(p.name) && this.age == p.age;
    }
}
// TreeSet mein same age wale "different" persons ek hi slot mein aa sakte hain
```

### Pitfall 7: PriorityQueue Iteration Assumption

```java
PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(5, 2, 8, 1));
// ❌ for-each order ≠ sorted order
for (int n : pq) { System.out.println(n); }  // Unpredictable

// ✅ Poll for sorted output
while (!pq.isEmpty()) System.out.println(pq.poll());
```

### Quick Reference Checklist

```
✅ Override equals + hashCode for HashSet/HashMap keys
✅ compareTo consistent with equals for TreeSet/TreeMap
✅ Iterator.remove for safe iteration removal
✅ new ArrayList<>(Arrays.asList()) for mutable list from array
✅ ConcurrentHashMap for thread-safe maps
✅ ArrayDeque for stack/queue

❌ Modify collection during for-each
❌ Null in TreeSet/TreeMap/ConcurrentHashMap
❌ Assume HashMap/HashSet iteration order
❌ Assume PriorityQueue iteration = sorted
❌ Use Hashtable (legacy)
```

**Short Interview Answer:**

Top pitfalls: missing hashCode/equals (HashSet duplicates fail), CME on for-each remove (use Iterator.remove), Arrays.asList fixed-size, null in Tree/Concurrent collections, Comparable/equals inconsistency, PriorityQueue iteration ≠ sorted.

---
