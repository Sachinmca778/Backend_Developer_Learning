# Level 1: Basic Collections (Q21 - Q29)

---

### Q21: What is a Collection in Java?
**Answer (Hinglish):**

- **Definition:** Java Collection ek aisa single unit (framework) hota hai jo multiple objects ko ek saath group, store, manipulate, aur process karne ki capability deta hai (jaise searching, sorting, insertion, deletion wagairah).
- **Collection Framework Hierarchy:**
  - Root interface: `Iterable<E>` -> `Collection<E>`
  - Main sub-interfaces:
    1. `List`: Ordered collection, duplicates allowed (e.g. `ArrayList`, `LinkedList`, `Vector`).
    2. `Set`: Unordered, unique elements only (e.g. `HashSet`, `LinkedHashSet`, `TreeSet`).
    3. `Queue`: FIFO processing order (e.g. `PriorityQueue`, `ArrayDeque`).
  - *Note:* `Map<K, V>` (Key-Value pair) collection framework ka part hai, lekin wo `Collection` interface ko directly extend **nahi** karta.

---

### Q22 & Q28: Difference between Array and ArrayList (Array vs List)?
**Answer (Hinglish):**

| Feature | Array (`int[]` or `String[]`) | ArrayList (`ArrayList<T>`) |
| :--- | :--- | :--- |
| **Size** | **Fixed size** (Ek baar create karne ke baad resize nahi ho sakta). | **Dynamic size** (Elements add hone par automatically grow karta hai). |
| **Data Types** | Primitive types (`int`, `char`) aur Objects dono store kar sakta hai. | Sirf **Objects / Wrapper types** (`Integer`, `String`) store karta hai. |
| **Performance** | Thoda faster hota hai (no autoboxing/wrapper overhead). | Flexible features aur methods milte hain (`add()`, `remove()`, `contains()`). |
| **Memory** | Contiguous memory allocation. | Internally array use karta hai, full hone par naya array banakar copy karta hai (Growth: 1.5x). |
| **Generics Support** | No compile-time type safety checking. | Generics fully supported (`List<String>`). |

---

### Q23: What is the difference between ArrayList and LinkedList?
**Answer (Hinglish):**

| Operation / Feature | ArrayList | LinkedList |
| :--- | :--- | :--- |
| **Internal Data Structure** | Resizable Dynamic Array. | Doubly Linked List (Nodes with `prev`, `data`, `next`). |
| **Random Access (`get(index)`)**| **O(1) - Super Fast** (Index based direct pointer math). | **O(n) - Slow** (Head ya Tail se node-by-node traverse karna padta hai). |
| **Insertion / Deletion in Middle** | **O(n) - Slow** (Element add/remove karne par baaki sabhi elements ko shift karna padta hai). | **O(1) - Fast** (Agar pointer already wahan ho, sirf node pointers update karne hote hain, koi shifting nahi). |
| **Memory Overhead** | Kam memory (Sirf data store karta hai). | Zyada memory (Har node me data ke alawa 2 pointers `next` aur `prev` bhi store hote hain). |

> **Best Practice Tip:**  
> 90% cases me real-world me **`ArrayList`** prefer kiya jata hai kyunki memory locality aur cache friendly hone ki wajah se sequential operations me ArrayList LinkedList se fast execute hota hai.

---

### Q24: What is a HashMap? Basic operations?
**Answer (Hinglish):**

- **Definition:** `HashMap<K, V>` ek Key-Value based data structure hai jo hashing algorithm par kaam karta hai.
- **Key Characteristics:**
  - Duplicate keys allow **nahi** karta (naya value purane value ko overwrite kar deta hai).
  - One `null` key allow karta hai, aur multiple `null` values allow karta hai.
  - Elements ka order maintain nahi rehta (unordered).
  - Time complexity: Average case **O(1)** for `get()` and `put()`.

**Basic Operations Code:**
```java
Map<Integer, String> empMap = new HashMap<>();

// 1. Insert / Update
empMap.put(101, "Sachin");
empMap.put(102, "Amit");

// 2. Fetch value
String name = empMap.get(101); // "Sachin"

// 3. Check key existence
boolean exists = empMap.containsKey(102); // true

// 4. Remove entry
empMap.remove(102);

// 5. Default value if absent (Java 8)
String result = empMap.getOrDefault(999, "Unknown");
```

---

### Q25: What is the difference between HashSet and TreeSet?
**Answer (Hinglish):**

| Feature | HashSet | TreeSet |
| :--- | :--- | :--- |
| **Internal Implementation** | Internally `HashMap` use karta hai. | Internally `NavigableMap` / `TreeMap` (Red-Black Self-Balancing Tree) use karta hai. |
| **Ordering** | Koi order guarantee nahi hoti (Random/Hash order). | **Sorted order** (Natural ascending order ya custom `Comparator`). |
| **Performance** | **O(1)** constant time for add, remove, contains. | **O(log n)** time for operations (tree re-balancing cost). |
| **Null Elements** | 1 `null` element allow karta hai. | `null` elements allow **nahi** karta (Throws `NullPointerException` comparison ke time). |

---

### Q26: What is an Iterator?
**Answer (Hinglish):**

- **Definition:** `Iterator` Java Collection framework ka ek universal cursor (interface) hai jo kisi bhi Collection ke elements ko one-by-one traverse (iterate) karne ke kaam aata hai.
- **Main Methods:**
  - `hasNext()`: Returns `true` agar aage koi aur element bacha ho.
  - `next()`: Agla element return karta hai aur cursor aage badhata hai.
  - `remove()`: Current element ko underlying collection se safely remove karta hai.

**Kyu use karein normal For Loop ke comparison me?**
- Normal for-each loop ke dauran agar aap collection me se item delete karte ho to **`ConcurrentModificationException`** aa jata hai.
- `Iterator.remove()` bina exception ke element ko iterate karte hue safely delete karta hai:
```java
List<String> list = new ArrayList<>(List.of("A", "B", "C"));
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    if ("B".equals(item)) {
        it.remove(); // Safe removal!
    }
}
```

---

### Q27 & Q29: What is the difference between List and Set (Set vs List)?
**Answer (Hinglish):**

| Feature | List | Set |
| :--- | :--- | :--- |
| **Duplicates** | **Duplicates allowed** (Same element bar-bar add kar sakte ho). | **No Duplicates** (Sirf unique elements allowed). |
| **Ordering** | **Insertion order preserved** (Jis order me daaloge usi order me rahega). | **No guaranteed order** (Except `LinkedHashSet` which preserves insertion order, and `TreeSet` which sorts). |
| **Index Access** | Index-based access possible (`list.get(2)`). | Index-based access **nahi** hota (No `set.get(i)`). |
| **Popular Classes** | `ArrayList`, `LinkedList`, `Vector` | `HashSet`, `LinkedHashSet`, `TreeSet` |
| **Null Elements** | Multiple `null` values add kar sakte ho. | At most 1 `null` (TreeSet me not allowed). |
