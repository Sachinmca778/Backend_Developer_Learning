# Part 6: Java Difference & Comparison Questions

---

## 197. Differentiate between `Iterable` and `Iterator`.

| Feature | `Iterable` (`java.lang`) | `Iterator` (`java.util`) |
|---|---|---|
| **Purpose** | Class ko For-Each Loop (`for (T x : collection)`) in-built capability dene ke liye. | Actual Traversal / Iteration state management object. |
| **Method** | `Iterator<T> iterator();` method produce karta hai. | `hasNext()`, `next()`, `remove()` operations execute karta hai. |
| **Hierarchy** | Collections Framework ka Root Interface hai (`Collection` extends `Iterable`). | Cursor interface hai. |

---

## 198. Differentiate between `List` and `Set`.

| Feature | `List` | `Set` |
|---|---|---|
| **Duplicates** | **Duplicates Allowed** (Multiple identical elements store kar sakta hai). | **NO Duplicates** (Unique elements ONLY). |
| **Order** | **Ordered Collection** (Insertion order maintain rehta hai). | **Unordered** (`HashSet`), except `LinkedHashSet` & `TreeSet`. |
| **Null Elements** | Multiple Null values allow karta hai. | `HashSet` sirf **1 Null value** allow karta hai; `TreeSet` null allow **nahi** karta. |
| **Index Access** | Positional Index access available (`get(i)`). | Index based direct access available **Nahi** hai. |

---

## 199. Differentiate between `List` and `Map`.

| Feature | `List` | `Map` |
|---|---|---|
| **Data Structure** | Sequence of elements (Single Values). | Key-Value pairs mapping (`K -> V`). |
| **Duplicates** | Duplicates allowed. | Duplicate Keys **NOT allowed**; Duplicate Values allowed. |
| **Interface Parent** | Extends `Collection` interface. | Does **NOT** extend `Collection` interface. |

---

## 200. Differentiate between `Queue` and `Stack`.

| Feature | `Queue` | `Stack` |
|---|---|---|
| **Ordering Principle** | **FIFO** (First In First Out). | **LIFO** (Last In First Out). |
| **Insertion / Removal** | Insertion at Tail (`offer()`), Removal at Head (`poll()`). | Both Insertion & Removal at Top (`push()`, `pop()`). |

---

## 201. Differentiate between `PriorityQueue` and `TreeSet`.

| Feature | `PriorityQueue` | `TreeSet` |
|---|---|---|
| **Interface** | Implements `Queue` interface. | Implements `NavigableSet` / `Set` interface. |
| **Duplicates** | **Duplicates Allowed**. | **NO Duplicates** allowed. |
| **Internal Data Structure** | **Binary Min/Max Heap** (Array based). | **Red-Black Self-balancing Tree**. |
| **Element Access** | Head element priority fetch (`peek()`, `poll()`). | Full sorted range traversal & set operations (`subSet()`). |

---

## 202. Differentiate between Singly Linked List and Doubly Linked List.

| Feature | Singly Linked List | Doubly Linked List |
|---|---|---|
| **Pointers per Node** | Single pointer (`next`). | Two pointers (`prev` and `next`). |
| **Traversal Direction** | Forward Direction only. | Bi-directional (Forward & Backward). |
| **Memory per Node** | Less memory (1 reference pointer). | More memory (2 reference pointers). |
| **Deletion Cost** | Previous node traversal needed (`O(N)`). | Direct deletion via `prev` pointer (`O(1)`). |

---

## 203. Differentiate between Fail-Fast and Fail-Safe Iterators.

| Feature | Fail-Fast Iterator | Fail-Safe Iterator |
|---|---|---|
| **Exception** | `ConcurrentModificationException` throw karta hai agar iteration ke beech modification ho. | Exception throw **NAHI** karta. |
| **Data Copy** | Original collection data par directly work karta hai (`modCount` check). | Collection ki **Copy (Snapshot)** ya concurrent data structures par operate karta hai. |
| **Memory & Speed** | High speed, Low memory. | Slightly higher memory overhead (due to clone copy). |
| **Examples** | `ArrayList`, `HashMap`, `HashSet` iterators. | `CopyOnWriteArrayList`, `ConcurrentHashMap`. |

---

## 204. Differentiate between `HashMap` and `TreeMap`.

| Feature | `HashMap` | `TreeMap` |
|---|---|---|
| **Ordering** | **No Order** maintained. | **Sorted Order** (Natural / Custom Comparator). |
| **Time Complexity** | `O(1)` average get/put operations. | `O(log N)` search/insert operations. |
| **Null Key Support** | Allows **1 Null Key**. | Does **NOT** allow Null Keys (`NullPointerException`). |
| **Internal Structure** | Hash Table + Array of Buckets + Red-Black Tree (in Java 8+ collision). | Red-Black Tree Data Structure. |

---

## 205. Differentiate between `Queue` and `Deque`.

| Feature | `Queue` | `Deque` (Double Ended Queue) |
|---|---|---|
| **Operation Ends** | Single-ended (Enqueue at tail, Dequeue at head). | **Double-ended** (Insert & Remove at BOTH Head and Tail). |
| **Stack Behavior** | Cannot act as Stack natively. | Can act as BOTH Queue (FIFO) and Stack (LIFO). |

---

## 206. Differentiate between `HashSet` and `TreeSet`.

| Feature | `HashSet` | `TreeSet` |
|---|---|---|
| **Sorting** | Unordered. | Automatically **Sorted Order**. |
| **Performance** | `O(1)` fast lookup. | `O(log N)` lookup. |
| **Null Elements** | Allows **1 Null value**. | Does **NOT allow Null** values. |
| **Internal Implementation** | Backed by `HashMap`. | Backed by `TreeMap` (Red-Black Tree). |
