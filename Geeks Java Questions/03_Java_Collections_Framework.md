# Part 3: Java Collections Framework

---

## 117. What is Collection Framework in Java?

**Matlab:** Java Collections Framework (`java.util` package) classes aur interfaces ka ek standardized architecture set hai jo objects ke group ko store, manipulate, search, sort aur iterate karne ke liye efficient data structures (Lists, Sets, Queues, Maps) provide karta hai.

```
                    Iterable (Interface)
                       │
                   Collection (Interface)
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  List (Interface) Set (Interface) Queue (Interface)
    • ArrayList      • HashSet       • PriorityQueue
    • LinkedList     • LinkedHashSet • ArrayDeque
    • Vector         • TreeSet (SortedSet)
      └─ Stack
```

---

## 118. Explain various interfaces used in the Collection framework.

1. **`Collection`:** Root interface for List, Set, Queue.
2. **`List`:** Ordered collection, allows duplicates, positional index access (`ArrayList`, `LinkedList`, `Vector`).
3. **`Set`:** Unordered collection, **NO duplicate elements** allowed (`HashSet`, `LinkedHashSet`, `TreeSet`).
4. **`Queue`:** FIFO (First In First Out) ordering processing (`PriorityQueue`, `LinkedList`, `ArrayDeque`).
5. **`Map` (Not extending Collection):** Key-Value pair mapping, unique keys (`HashMap`, `LinkedHashMap`, `TreeMap`, `ConcurrentHashMap`).
6. **`SortedSet` / `SortedMap`:** Elements automatically sorted based on Natural order / Comparator (`TreeSet`, `TreeMap`).

---

## 119. How can you synchronize an `ArrayList` in Java?

`ArrayList` by default non-synchronized (not thread-safe) hota hai. Isse synchronize karne ke 2 main ways:

1. **`Collections.synchronizedList()` Wrapper:**
   ```java
   List<String> list = Collections.synchronizedList(new ArrayList<>());
   ```
2. **`CopyOnWriteArrayList` (Concurrent Collection - Recommended for High Read Operations):**
   ```java
   List<String> cowList = new CopyOnWriteArrayList<>();
   ```

---

## 120. Why do we need a synchronized `ArrayList` when we have `Vector` (which is synchronized)?

- **`Vector` is Legacy:** Vector har individual method par `synchronized` lock lagata hai, chahe single thread use kar raha ho, jisse heavy performance bottleneck banta hai.
- **`Collections.synchronizedList()` Flexibility:** Developer control karta hai ki lock kab aur kis block par lagana hai, aur non-thread safe code se thread safe code switch karna flexible rahta hai.

---

## 121. Why can't we create a generic array in Java? (`new T[10]` is invalid)

**Matlab:** Java mein Generic Arrays allowed nahi hote (`new T[10]` compile error deta hai).

### Technical Reason:
- **Type Erasure:** Generics Compile-Time par Type check karne ke baad Erasure dwara compile bytecode se type parameters ko remove kar dete hain.
- **Arrays Covariance:** Arrays runtime par apni element type remember rakhte hain (`ArrayStoreException` throw karte hain agar invalid object put ho).
- Runtime par `T` Erasure ki wajah se `Object` ban jata hai, jisse Array runtime type safety system break ho jata.

---

## 122. How are elements stored in memory for regular Arrays vs `ArrayList`?

- **Regular Primitive Arrays (`int[]`):** Continuous contiguous memory locations par primitive values directly store hoti hain.
- **`ArrayList`:** Internal level par **Array of Object References (`Object[] elementData`)** hold karta hai. Primitives autobox hokar Heap mein Wrapper Objects bante hain, aur array in Object References ko store karta hai.

---

## 123. Explain the method to convert `ArrayList` to Array and Array to `ArrayList`.

### 1. Array to `ArrayList`:
```java
String[] arr = {"A", "B", "C"};
List<String> list = new ArrayList<>(Arrays.asList(arr));
// Or Java 10+: List.of(arr)
```

### 2. `ArrayList` to Array:
```java
List<String> list = new ArrayList<>();
list.add("Java");
String[] arr = list.toArray(new String[0]);
```

---

## 124. How does the size of `ArrayList` grow dynamically? (Internal Mechanism)

**Matlab:** `ArrayList` internal Array (`Object[] elementData`) use karta hai.

1. **Initial Capacity:** Default initial capacity **10** hoti hai.
2. **Growth Formula:** Jab array full ho jata hai (`size == capacity`), ArrayList automatic capacity badhata hai:
   $$	ext{New Capacity} = 	ext{Old Capacity} + (	ext{Old Capacity} \gg 1) = 1.5 	imes 	ext{Old Capacity}$$
3. **Array Copy:** Old array elements `Arrays.copyOf()` se naye 1.5x size wale array mein copy kar diye jaate hain.

---

## 125. What is a Vector in Java?

**Matlab:** `Vector` Java 1.0 ka legacy List implementation hai.
- Synchronized (Thread-safe) hai.
- Dynamic growth formula: Capacity doubles (**2x**) when full.
- Modern multi-threaded applications mein `CopyOnWriteArrayList` prefer kiya jata hai.

---

## 126. How to make Java `ArrayList` Read-Only?

```java
List<String> list = new ArrayList<>();
list.add("A");

// Unmodifiable List Wrapper
List<String> readOnlyList = Collections.unmodifiableList(list);
// readOnlyList.add("B"); -> Throws UnsupportedOperationException at runtime!
```

---

## 127. What is a PriorityQueue in Java?

**Matlab:** `PriorityQueue` ek Unbounded Queue implementation hai jo min-heap / max-heap data structure par based hota hai. Isme elements unki **Priority (Natural Ordering / Custom Comparator)** ke basis par process hote hain (FIFO not strictly guaranteed). Null elements allowed nahi hote.

---

## 128. Explain the `LinkedList` class in Java.

**Matlab:** `LinkedList` **Doubly Linked List** data structure implementation hai.
- Implements both `List` and `Deque` interfaces.
- Intermediate Insertions/Deletions fast hote hain (`O(1)` pointer adjustments).
- Random search slow hota hai (`O(N)` traversal cost).

---

## 129. What is the `Stack` class in Java and what are its methods?

**Matlab:** `Stack` `Vector` class ki subclass hai jo LIFO (Last In First Out) structure represent karti hai.

### Key Methods:
- `push(E item)`: Add element.
- `pop()`: Remove & return top element.
- `peek()`: Return top element without removal.
- `empty()`: Check if stack is empty.

---

## 130. What is `Set` in Collections Framework and list its implementations?

**Matlab:** `Set` interface duplicates allow **NAHI** karta.

### Major Implementations:
1. **`HashSet`:** Hashing backed by `HashMap`. Unordered, fast `O(1)` operations.
2. **`LinkedHashSet`:** Maintains **Insertion Order** via doubly-linked list buckets.
3. **`TreeSet`:** Red-Black Tree backed. Stores elements in **Sorted Order** (`O(log N)` complexity).

---

## 131. What is `HashSet` in Java and how does it store elements internally?

**Matlab:** `HashSet` internally ek **`HashMap` instance** use karta hai data store karne ke liye.

```java
// Internal implementation in HashSet.java
private transient HashMap<E, Object> map;
private static final Object PRESENT = new Object();

public boolean add(E e) {
    return map.put(e, PRESENT) == null; // Value is dummy PRESENT object
}
```
Jab aap `hashSet.add(element)` call karte ho, `HashSet` us element ko `HashMap` ke **KEY** ke form mein store karta hai aur Value me dummy `PRESENT` object pass karta hai.

---

## 132. What is `LinkedHashSet` in Java?

`LinkedHashSet` `HashSet` ki subclass hai. Yeh `HashSet` ki speed ke saath-saath elements ka **Insertion Order** bhi maintain rakhti hai (Doubly-linked list traversal maintain karke).

---

## 133. What is a `Map` interface in Java?

**Matlab:** `Map` Key-Value pairs storing structure hai. Duplicate keys allowed nahi hoti, har key at most 1 value se map hoti hai.

---

## 134. Explain `TreeMap` in Java.

**Matlab:** `TreeMap` Red-Black Tree Data Structure based `NavigableMap` implementation hai.
- Keys **Natural Sorting Order** (ya custom `Comparator`) mein automatically sorted rahti hain.
- Time complexity for search/insert: `O(log N)`.

---

## 135. What is `EnumSet`?

**Matlab:** `EnumSet` Java Collections ka specialized, high-performance `Set` implementation hai jo exclusively **Enum Types** ke saath kaam karta hai. Internally Bit-vectors/Bitmasks use hone ki wajah se yeh extremely fast aur memory-efficient hota hai.

---

## 136. What is `BlockingQueue` in Java?

**Matlab:** `BlockingQueue` (`java.util.concurrent` package) ek thread-safe Queue hai jo Producer-Consumer pattern ke liye built-in support deti hai.

- Queue full hone par Producer thread block ho jata hai jab tak space empty na ho.
- Queue empty hone par Consumer thread block ho jata hai jab tak new element arrive na ho.

---

## 137. What is `ConcurrentHashMap` in Java and how is it implemented internally?

**Matlab:** `ConcurrentHashMap` high-concurrency multi-threaded environments ke liye designed thread-safe Map implementation hai.

### Internal Working (Java 8+):
- Synchronized `Hashtable` ya `Collections.synchronizedMap` ki tarah poore Map ko lock **nahi** karta.
- Uses **Bucket-level CAS (Compare-And-Swap) operations** and **`synchronized` locks on individual Bucket Head Nodes (Node Bins)**.
- Multiple threads different buckets par simultaneously read/write kar sakte hain (**Lock Striping / Segment Locking concept**).

---

## 138. Can you use any class as a Map key?

**Matlab:** **Haan**, aap kisi bhi custom class ko Map key ke roop mein use kar sakte ho, **LEKIN 2 mandatory rules follow hone chahiye**:

1. Class ko **`equals()` aur `hashCode()` methods override** karne chahiye (Contract consistency ke liye).
2. Class preferable **Immutable** honi chahiye (Key ka state put hone ke baad modify nahi hona chahiye, varna hash value change hone se value search un-retrievable ho jaati hai).

---

## 139. What is an `Iterator`?

**Matlab:** `Iterator` (`java.util.Iterator`) ek cursor interface hai jo kisi bhi `Collection` elements ko forward direction mein sequential iterate / traverse karne ke liye use hota hai.

---

## 140. What is an `Enumeration`?

**Matlab:** `Enumeration` Java 1.0 ka Legacy Read-Only cursor interface hai (primarily used with `Vector` & `Hashtable`). Isme remove capability nahi hoti.

---

## 141. Difference between `Collection` and `Collections`.

- **`Collection`:** Interface hai (`java.util.Collection`) jo List, Set, Queue ki root interface hai.
- **`Collections`:** Utility Class hai (`java.util.Collections`) jisme static helper methods hote hain (`sort()`, `reverse()`, `synchronizedList()`, `binarySearch()`).

---

## 142. Differentiate between Array and `ArrayList` in Java.

| Feature | Array | `ArrayList` |
|---|---|---|
| **Size** | Fixed Size. | Dynamic Resizable Size. |
| **Data Types** | Primitives & Objects hold kar sakta hai. | Sirf Objects hold kar sakta hai. |
| **Performance** | Fast (Direct memory index). | Slightly slower due to autoboxing/resizing. |

---

## 143. Difference between Array and Collection in Java.

- Arrays fixed size hote hain; Collections dynamic sizes manage karti hain.
- Arrays homogeneous hote hain; Collections (without generics) heterogeneous objects store kar sakti hain.
- Collections rich built-in utility methods (sort, search, filter) provide karti hain.

---

## 144. Difference between `ArrayList` and `LinkedList`.

| Feature | `ArrayList` | `LinkedList` |
|---|---|---|
| **Internal Structure** | Resizable Dynamic Array. | Doubly Linked List nodes. |
| **Random Search (`get(i)`)** | Fast `O(1)`. | Slow `O(N)` traversal. |
| **Insertion/Deletion** | Slow `O(N)` (Element shifting needed). | Fast `O(1)` (Only pointer update). |

---

## 145. Differentiate between `ArrayList` and `Vector`.

- `ArrayList` non-synchronized (Fast) hai; `Vector` synchronized (Slow legacy) hai.
- `ArrayList` 50% capacity grow karta hai (`1.5x`); `Vector` 100% capacity grow karta hai (`2x`).

---

## 146. Difference between `Iterator` and `ListIterator`.

| Feature | `Iterator` | `ListIterator` |
|---|---|---|
| **Applicability** | Kisi bhi `Collection` (List, Set, Queue) par. | Sirf **`List`** implementations par. |
| **Traversal Direction** | Sirf Forward Direction (`hasNext()`, `next()`). | Bi-directional Forward & Backward (`hasPrevious()`, `previous()`). |
| **Element Modification** | Can only remove elements (`remove()`). | Can Add, Replace, and Remove elements. |

---

## 147. Differentiate between `HashMap` and `HashTable`.

| Feature | `HashMap` | `HashTable` |
|---|---|---|
| **Thread Safety** | Non-synchronized (Not Thread-Safe). | Synchronized (Thread-Safe). |
| **Null Keys/Values** | Allows **1 Null Key** & multiple Null Values. | **NO Null Key or Null Value** allowed (`NullPointerException`). |
| **Performance** | Fast. | Slow due to thread locks. |
| **Legacy** | Introduced in Java 1.2. | Legacy class from Java 1.0. |

---

## 148. Difference between `Iterator` and `Enumeration`.

- `Iterator` fail-fast hota hai aur `remove()` allow karta hai; `Enumeration` read-only legacy interface hai.
- `Iterator` method names clean hain (`hasNext()`, `next()`); `Enumeration` methods verbose hain (`hasMoreElements()`, `nextElement()`).

---

## 149. What is the difference between `Comparable` and `Comparator`?

| Feature | `Comparable` (`java.lang`) | `Comparator` (`java.util`) |
|---|---|---|
| **Sorting Logic** | Single **Natural Sorting Order**. | Multiple **Custom Sorting Orders**. |
| **Method** | `public int compareTo(T o)` | `public int compare(T o1, T o2)` |
| **Class Modification** | Original Class ko modify karke implement karna padta hai. | Original Class ko touch kiye bina separate comparator class/lambda banti hai. |
| **Usage** | `Collections.sort(list)` | `Collections.sort(list, customComparator)` |

---

## 150. What is the difference between `Set` and `Map`?

- **`Set`:** Single values ka collection (Unique elements).
- **`Map`:** Key-Value pairs ka collection (Unique Keys mapped to values).
