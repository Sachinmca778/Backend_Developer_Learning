# List Interface

## Status: Not Started

---

## Table of Contents

1. [List Interface Overview](#list-interface-overview)
2. [ArrayList](#arraylist)
3. [LinkedList](#linkedlist)
4. [Vector](#vector)
5. [CopyOnWriteArrayList](#copyonwritearraylist)
6. [ArrayList vs LinkedList vs Vector](#arraylist-vs-linkedlist-vs-vector)

---

## List Interface Overview

**Matlab:** Ordered collection (elements ka sequence) — duplicates allow, index-based access, null elements allow.

```
Collection (interface)
    ↑
List (interface) — ordered, duplicates allowed, index access
    ↑
    ├── ArrayList
    ├── LinkedList
    ├── Vector
    └── CopyOnWriteArrayList
```

### List Features

```java
List<String> list = new ArrayList<>();

// Add elements
list.add("Apple");       // Index 0
list.add("Banana");      // Index 1
list.add("Cherry");      // Index 2
list.add(1, "Mango");    // Index 1 pe insert — ["Apple", "Mango", "Banana", "Cherry"]

// Get by index
String first = list.get(0);  // "Apple"

// Update by index
list.set(0, "Apricot");  // ["Apricot", "Mango", "Banana", "Cherry"]

// Remove
list.remove(1);          // Index 1 remove → ["Apricot", "Banana", "Cherry"]
list.remove("Banana");   // Object remove → ["Apricot", "Cherry"]

// Size
int size = list.size();  // 2

// Check
boolean hasApple = list.contains("Apricot");  // true
boolean isEmpty = list.isEmpty();             // false

// Index search
int index = list.indexOf("Cherry");    // 1 (first occurrence)
int lastIndex = list.lastIndexOf("Cherry");  // 1 (last occurrence)

// SubList
List<String> sub = list.subList(0, 1);  // ["Apricot"] (0 inclusive, 1 exclusive)

// Clear
list.clear();
```

### Iterating List

```java
List<String> list = List.of("Apple", "Banana", "Cherry");

// for-each
for (String fruit : list) {
    System.out.println(fruit);
}

// Indexed for
for (int i = 0; i < list.size(); i++) {
    System.out.println(i + ": " + list.get(i));
}

// Iterator
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    System.out.println(it.next());
}

// ListIterator (bidirectional)
ListIterator<String> lit = list.listIterator();
while (lit.hasNext()) {
    System.out.println(lit.next());
}

// Stream (Java 8+)
list.stream().forEach(System.out::println);
```

---

## ArrayList

**Matlab:** Dynamic array — size automatically grow hoti hai. Most commonly used List implementation.

### Internal Working

```
ArrayList internally array use karta hai
Default capacity: 10
Jab array full hoti hai → naya array (1.5x size) banao → copy karo
```

### Basic Usage

```java
// Creation
ArrayList<String> list = new ArrayList<>();
List<String> list2 = new ArrayList<>();  // Interface reference (recommended)

// Initial capacity specify karo (performance optimization)
List<String> list3 = new ArrayList<>(100);  // 100 ki initial capacity

// Add elements
list.add("Apple");
list.add("Banana");
list.add("Cherry");

// Get — O(1)
String fruit = list.get(0);  // Fast — direct index access

// Contains — O(n)
boolean hasApple = list.contains("Apple");  // Linear search

// Remove — O(n)
list.remove(0);  // Elements shift honge
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **get(index)** | O(1) | Direct array access |
| **add(element)** | O(1) amortized | Resize hone pe O(n) |
| **add(index, element)** | O(n) | Elements shift honge |
| **remove(index)** | O(n) | Elements shift honge |
| **contains(element)** | O(n) | Linear search |
| **set(index, element)** | O(1) | Direct array update |

### When to Use ArrayList

```java
// ✅ Use ArrayList when:
// - Frequent read/random access chahiye
// - Elements end mein add hote hain
// - Thread-safety ki zarurat nahi

// ❌ Avoid when:
// - Frequent insert/delete in middle
// - Thread-safety chahiye
```

### ArrayList Methods

```java
List<Integer> nums = new ArrayList<>();

// Bulk operations
nums.addAll(List.of(1, 2, 3, 4, 5));
nums.removeAll(List.of(2, 4));
nums.retainAll(List.of(1, 3, 5));  // Keep only these

// Convert to array
Integer[] arr = nums.toArray(new Integer[0]);

// Sort
Collections.sort(nums);

// Replace all
nums.replaceAll(n -> n * 2);

// Remove if
nums.removeIf(n -> n > 5);

// Sort with comparator
nums.sort(Comparator.reverseOrder());
```

---

## LinkedList

**Matlab:** Doubly linked list — har element ke paas previous aur next ka reference hota hai.

### Internal Structure

```
Node {
    T data;
    Node prev;
    Node next;
}

head ↔ [Node1] ↔ [Node2] ↔ [Node3] ↔ tail
```

### Basic Usage

```java
LinkedList<String> list = new LinkedList<>();

// Add elements
list.add("Apple");       // End mein add
list.add("Banana");
list.addFirst("Cherry"); // Start mein add
list.addLast("Date");    // End mein add

// Get elements
String first = list.getFirst();  // "Cherry"
String last = list.getLast();    // "Date"
String byIndex = list.get(1);    // O(n) — traversal hota hai

// Remove
list.removeFirst();  // "Cherry"
list.removeLast();   // "Date"
list.remove("Apple"); // By value

// Queue operations
list.offer("Fig");     // Add to end
list.poll();           // Remove from start
list.peek();           // Get first without removing
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **get(index)** | O(n) | Traversal chahiye |
| **add(element)** | O(1) | End mein add |
| **addFirst(element)** | O(1) | Start mein add |
| **addLast(element)** | O(1) | End mein add |
| **removeFirst()** | O(1) | Start se remove |
| **removeLast()** | O(1) | End se remove |
| **contains(element)** | O(n) | Linear search |

### When to Use LinkedList

```java
// ✅ Use LinkedList when:
// - Frequent add/remove at start/end
// - Queue/Deque behavior chahiye
// - Random access ki zarurat nahi

// ❌ Avoid when:
// - Frequent get(index) chahiye
// - Memory efficient chahiye (Node overhead)
```

### LinkedList as Queue

```java
Queue<String> queue = new LinkedList<>();

queue.offer("Task1");  // Add to end
queue.offer("Task2");
queue.offer("Task3");

System.out.println(queue.poll());   // "Task1" (remove from front)
System.out.println(queue.peek());   // "Task2" (view front)
System.out.println(queue.size());   // 2
```

### LinkedList as Deque

```java
Deque<String> deque = new LinkedList<>();

deque.addFirst("A");
deque.addLast("B");
deque.addFirst("C");
// Deque: C ↔ A ↔ B

System.out.println deque.removeFirst());  // "C"
System.out.println deque.removeLast());   // "B"
```

---

## Vector

**Matlab:** Legacy synchronized version of ArrayList — thread-safe lekin slow.

### Basic Usage

```java
Vector<String> vector = new Vector<>();
vector.add("Apple");
vector.add("Banana");

// All methods are synchronized
String fruit = vector.get(0);
vector.remove(0);
```

### Vector vs ArrayList

| Feature | Vector | ArrayList |
|---------|--------|-----------|
| **Synchronized** | ✅ Yes | ❌ No |
| **Performance** | Slow (locking overhead) | Fast |
| **Capacity Growth** | Doubles (2x) | 1.5x |
| **Legacy** | Java 1.0 | Java 1.2 |
| **Iteration** | Enumeration + Iterator | Iterator + ListIterator |
| **Recommended?** | ❌ Avoid | ✅ Yes |

### Why Avoid Vector?

```java
// ❌ Vector — synchronized overhead
Vector<String> vector = new Vector<>();

// ✅ ArrayList + Collections.synchronizedList
List<String> list = Collections.synchronizedList(new ArrayList<>());

// ✅ Ya concurrent collection use karo
CopyOnWriteArrayList<String> concurrentList = new CopyOnWriteArrayList<>();
```

---

## CopyOnWriteArrayList

**Matlab:** Thread-safe List — har modification pe array ka copy banata hai. Read-heavy scenarios ke liye best.

### How It Works

```
Add/Remove operation → Array ka copy banao → Copy pe modify karo → Reference update karo
Read operation → Original array pe bina lock ke read karo
```

### Basic Usage

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

list.add("Apple");
list.add("Banana");
list.add("Cherry");

// Read operations — no locking, very fast
String fruit = list.get(0);
boolean hasApple = list.contains("Apple");

// Write operations — copy banata hai (expensive)
list.add("Date");
list.remove("Apple");
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **get(index)** | O(1) | No locking |
| **add(element)** | O(n) | Copy banata hai |
| **remove(element)** | O(n) | Copy banata hai |
| **Iterator** | O(1) | Snapshot iterator — ConcurrentModificationException nahi |

### When to Use

```java
// ✅ Use CopyOnWriteArrayList when:
// - Reads bahut zyada, writes bahut kam
// - Thread-safety chahiye
// - Iteration ke time modification possible hai

// ❌ Avoid when:
// - Frequent writes hote hain (copy overhead)
// - Large lists (memory overhead)

// Example: Event listeners list
private final CopyOnWriteArrayList<EventListener> listeners = new CopyOnWriteArrayList<>();

public void addListener(EventListener listener) {
    listeners.add(listener);  // Rare
}

public void fireEvent(Event e) {
    for (EventListener listener : listeners) {  // Frequent — no ConcurrentModificationException
        listener.onEvent(e);
    }
}
```

### ConcurrentModificationException Prevention

```java
// ❌ ArrayList — ConcurrentModificationException
List<String> list = new ArrayList<>(List.of("A", "B", "C"));
for (String s : list) {
    if (s.equals("B")) {
        list.remove(s);  // ❌ Exception!
    }
}

// ✅ CopyOnWriteArrayList — no exception
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));
for (String s : list) {
    if (s.equals("B")) {
        list.remove(s);  // ✅ Works — iteration pe copy use hota hai
    }
}
```

---

## ArrayList vs LinkedList vs Vector

| Feature | ArrayList | LinkedList | Vector |
|---------|-----------|------------|--------|
| **Internal** | Dynamic array | Doubly linked list | Dynamic array |
| **get(index)** | O(1) | O(n) | O(1) |
| **add(element)** | O(1) amortized | O(1) | O(1) amortized |
| **add(index, elem)** | O(n) | O(n) | O(n) |
| **remove(index)** | O(n) | O(n) | O(n) |
| **removeFirst/Last** | ❌ No | O(1) | ❌ No |
| **Thread-Safe** | ❌ No | ❌ No | ✅ Yes |
| **Memory** | Less | More (Node overhead) | Less |
| **Use When** | Random access, read-heavy | Queue/Deque, add/remove at ends | Legacy — avoid |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **List** | Ordered collection, duplicates allowed, index-based access |
| **ArrayList** | Dynamic array — O(1) get, O(n) insert/delete in middle |
| **LinkedList** | Doubly linked — O(1) add/remove at ends, O(n) get |
| **Vector** | Legacy synchronized ArrayList — avoid, use alternatives |
| **CopyOnWriteArrayList** | Thread-safe, read-heavy scenarios, copy-on-write |
| **ArrayList vs LinkedList** | Read-heavy → ArrayList, Queue/Deque → LinkedList |
