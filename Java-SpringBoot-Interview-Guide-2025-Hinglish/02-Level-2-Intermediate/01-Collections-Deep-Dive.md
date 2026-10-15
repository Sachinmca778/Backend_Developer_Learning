# Level 2: Collections Deep Dive (Q58 - Q70)

---

### Q58: Contract between hashCode() and equals() methods?
**Answer (Hinglish):**

Java me `hashCode()` aur `equals()` methods `java.lang.Object` class ke methods hain. Inka ek strict contract hota hai:

1. **Rule 1 (Equivalence implies same hash):**  
   Agar do objects `equals()` ke mutabiq equal hain (`obj1.equals(obj2) == true`), to unka `hashCode()` **hamesha 100% same integer return karna chahiye**.
2. **Rule 2 (Same hash does NOT guarantee equality):**  
   Agar do objects ka `hashCode()` same hai, to zaroori nahi hai ki wo `equals()` se bhi equal hon (**Hash Collision**).
3. **Consistency:**  
   Jab tak object ki fields change na hon, ek execution ke dauran `hashCode()` har baar same value dega.

> **⚠️ Critical Interview Question:**  
> *"Agar hum `equals()` override karein lekin `hashCode()` override na karein, to kya hoga?"*  
> **Jawab:** `HashMap` aur `HashSet` fail ho jayenge! Default `Object.hashCode()` memory address ke base par hash banata hai. Isliye jab aap `map.put(new Key("A"), "Val")` karoge aur baad me `map.get(new Key("A"))` karoge, to dono keys equal hone ke bawajood alag hash bucket me dhoondi jayengi aur result `null` aayega!

---

### Q59, Q60 & Q61: Internal Working of HashMap, Collision & Load Factor
**Answer (Hinglish):**

#### 1. Internal Working (`put` & `get` operations):
- `HashMap` internally ek **Array of Nodes (Buckets)** use karta hai: `Node<K, V>[] table`.
- Har node ke 4 parts hote hain: `int hash`, `K key`, `V value`, `Node<K, V> next`.
- Default initial capacity **16** hoti hai.

```
Index = hash(key) & (n - 1)   [where n is table size, e.g. 16]
Bucket 0:  null
Bucket 1:  [hash|key="apple"|val=10|next] ---> [hash|key="banana"|val=20|null] (Collision chained)
...
Bucket 15: null
```

#### 2. Collision Resolution:
- **Collision Kya Hai:** Jab do alag-alag keys ka hash function calculate hoke same bucket index par point karta hai.
- **Handling in Java 7:** Ek hi bucket me **Singly Linked List** ban jati hai (Chaining). New nodes head par judte the.
- **Handling in Java 8+ (Treeification):**  
  Agar kisi single bucket me elements ki sankhya **`TREEIFY_THRESHOLD = 8`** tak pahunch jaye aur total table capacity >= 64 ho, to wo Linked List automatically **Red-Black Self-Balancing Binary Search Tree (`TreeNode`)** me convert ho jati hai.
  - **Kyu?** Linked list me search time **O(n)** tha, Red-Black Tree me worst-case search time **O(log n)** ho jata hai, jisse Denial-of-Service (DoS) attacks aur performance degradation rukti hai.

#### 3. Load Factor (`0.75f`):
- Load factor ek measure hai jo batata hai ki HashMap ko resize (rehash) karne se pehle kitna percent full hone dena hai.
- **Threshold Formula:** `Threshold = Capacity * Load Factor`  
  Default: `16 * 0.75 = 12`. Jaise hi map me 13th element aayega, map apni capacity double (`32`) karke sabhi existing elements ko naye array me rehash kar dega.

---

### Q62 & Q63: HashMap vs LinkedHashMap vs TreeMap vs ConcurrentHashMap
**Answer (Hinglish):**

| Feature | HashMap | LinkedHashMap | TreeMap | ConcurrentHashMap |
| :--- | :--- | :--- | :--- | :--- |
| **Ordering** | No guaranteed order. | **Insertion order** (ya Access order - LRU cache). | **Sorted order** (Natural/Comparator). | No guaranteed order. |
| **Data Structure** | Array + LinkedList + Red-Black Tree. | HashMap + Doubly Linked List running across entries. | Red-Black Tree. | Synchronized Buckets + CAS (Compare-And-Swap). |
| **Time Complexity** | O(1) average. | O(1) average (slightly slower due to pointer updates). | **O(log n)**. | O(1) concurrent. |
| **Thread-Safe** | ❌ No. | ❌ No. | ❌ No. | ✅ **Yes (Highly scalable)**. |
| **Null Key/Value** | 1 Null Key, Multiple Null Values. | 1 Null Key, Multiple Null Values. | Null Key NOT allowed (NPE). | **No Null Key, No Null Value**. |

---

### Q64: How can you convert a HashMap into an ArrayList?
**Answer (Hinglish):**

Aap keys, values ya pure entries ko alag-alag ArrayList me convert kar sakte hain:

```java
Map<Integer, String> map = new HashMap<>();
map.put(1, "Java");
map.put(2, "Spring");

// 1. Convert Keys to ArrayList
List<Integer> keyList = new ArrayList<>(map.keySet());

// 2. Convert Values to ArrayList
List<String> valueList = new ArrayList<>(map.values());

// 3. Convert Entries (Key-Value pairs) to ArrayList
List<Map.Entry<Integer, String>> entryList = new ArrayList<>(map.entrySet());
```

---

### Q65: What are the differences between Comparable and Comparator?
**Answer (Hinglish):**

| Difference Point | Comparable (`java.lang`) | Comparator (`java.util`) |
| :--- | :--- | :--- |
| **Definition** | Object ko **Natural Ordering** (default sorting) deta hai. | Object ko **Custom / Multiple Sorting Sequences** deta hai. |
| **Method** | `public int compareTo(T o)` | `public int compare(T o1, T o2)` |
| **Class Modification** | Original model class ko modify karna padta hai (`implements Comparable<T>`). | Original class ko chhedne ki zaroorat nahi; alag class ya Lambda expression se define kar sakte hain. |
| **Package** | `java.lang` (Automatically imported). | `java.util`. |
| **Collections Method** | `Collections.sort(list)` | `Collections.sort(list, comparator)` ya `list.sort(...)`. |

**Example:**
```java
// Comparator using Java 8 Lambda (Very popular in interviews):
Comparator<Employee> bySalaryDesc = (e1, e2) -> Double.compare(e2.getSalary(), e1.getSalary());
employees.sort(Comparator.comparing(Employee::getSalary).reversed());
```

---

### Q66 & Q67: ConcurrentHashMap vs Hashtable (How ConcurrentHashMap works)?
**Answer (Hinglish):**

- **Hashtable (Legacy & Slow):**
  - Thread-safety ke liye Hashtable har method (`put`, `get`, etc.) par **poore table-level lock (`synchronized`)** laga deta hai. Agar ek thread read kar raha hai, to doosra thread write ya read kuch nahi kar sakta. Huge bottleneck!

- **ConcurrentHashMap (Modern & Fast):**
  - **Java 7 approach:** **Segment Locking** (Table ko 16 segments me baant kar alag-alag locks lagata tha).
  - **Java 8+ approach:**  
    1. Lock-free reading using `volatile` reads.
    2. Write operations ke liye **CAS (Compare-And-Swap)** instructions use karta hai agar bucket khali ho.
    3. Agar collision ho, to sirf us **specific bucket node ke head par `synchronized`** lock lagata hai, baaki poora map open rehta hai.
  - **Result:** Multiple threads ek sath concurrently alag-alag buckets me read aur write kar sakte hain bina kisi performance drop ke.

---

### Q68, Q69 & Q70: Quick Collection Comparisons (Vector vs ArrayList, HashMap vs Hashtable)
**Answer (Hinglish):**

1. **Vector vs ArrayList:**
   - `Vector` legacy class hai jisme saare methods `synchronized` hain (slow).
   - `ArrayList` modern, non-synchronized aur fast hai. Agar thread-safety chahiye to `Collections.synchronizedList(new ArrayList<>())` ya `CopyOnWriteArrayList` prefer karte hain.

2. **HashMap vs Hashtable:**
   - `HashMap` non-synchronized hai (fast), 1 `null` key aur multiple `null` values allow karta hai.
   - `Hashtable` synchronized hai (slow), koi bhi `null` key ya value allow **nahi** karta.
