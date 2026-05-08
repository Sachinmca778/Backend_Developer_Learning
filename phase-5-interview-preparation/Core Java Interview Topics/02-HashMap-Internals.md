# HashMap Internals

## Status: Not Started

---

## Table of Contents

1. [Why This is Asked Always](#why-this-is-asked-always)
2. [Internal Structure](#internal-structure)
3. [Default Constants](#default-constants)
4. [Hash Computation](#hash-computation)
5. [`put` Flow Step-by-Step](#put-flow-step-by-step)
6. [Collision Handling — List → Tree](#collision-handling--list--tree)
7. [Resize / Rehash](#resize--rehash)
8. [`get` Flow](#get-flow)
9. [`equals` & `hashCode` Contract](#equals--hashcode-contract)
10. [`HashMap` vs `LinkedHashMap` vs `TreeMap` vs `ConcurrentHashMap`](#hashmap-vs-linkedhashmap-vs-treemap-vs-concurrenthashmap)
11. [`ConcurrentModificationException`](#concurrentmodificationexception)
12. [Java 7 vs Java 8 Differences](#java-7-vs-java-8-differences)
13. [Common Interview Qs](#common-interview-qs)
14. [Pitfalls](#pitfalls)
15. [Cheat Sheet](#cheat-sheet)

---

## Why This is Asked Always

- Tests data structure depth, hashing, OOP contracts, concurrency awareness — sab ek question mein.
- Real bugs (mutable keys, missing equals/hashCode) yahan se aate hain.

---

## Internal Structure

```
HashMap
 └── Node<K,V>[] table          // array (default 16)
       └── Node<K,V>            // linked list / tree node
             - hash
             - key
             - value
             - next
```

### Node

```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;
    final K key;
    V value;
    Node<K,V> next;
}
```

When bucket bada → tree node:

```java
static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> { ... }
```

---

## Default Constants

| Constant | Value |
|----------|-------|
| `DEFAULT_INITIAL_CAPACITY` | 16 |
| `DEFAULT_LOAD_FACTOR` | 0.75 |
| `MAXIMUM_CAPACITY` | 1 << 30 |
| `TREEIFY_THRESHOLD` | 8 |
| `UNTREEIFY_THRESHOLD` | 6 |
| `MIN_TREEIFY_CAPACITY` | 64 |

**Threshold formula:** `threshold = capacity × loadFactor` → 16 × 0.75 = **12** entries → resize.

---

## Hash Computation

```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 :
        (h = key.hashCode()) ^ (h >>> 16);
}
```

### Q: Yeh `h ^ (h >>> 16)` kyon?

> "Hash ke higher bits ko lower bits mein mix karne ke liye — taaki bucket index `(n-1) & hash` decision higher bits ko bhi consider kare. Bina mixing ke poor hash distributions clusters bana lete."

### Bucket Index

```java
index = (n - 1) & hash;          // n is power of 2
```

`%` ke jagah bitwise AND — power-of-2 capacity isiliye.

---

## `put` Flow Step-by-Step

```
1. hash = (key.hashCode()) ^ (h >>> 16)
2. index = (n - 1) & hash
3. if (table[index] == null) → place new Node here, done
4. else collision:
   a. if first node key matches (hash == && (key == || equals)) → overwrite value
   b. else if TreeNode → tree put
   c. else walk linked list:
        - found match → overwrite
        - reach tail → append
        - if list size >= 8 AND table.length >= 64 → treeify bucket
5. ++size
6. if (size > threshold) → resize()
```

---

## Collision Handling — List → Tree

### Java 7 (history)

Pure linked list per bucket. Worst case O(n) lookup with bad hashes (DoS).

### Java 8+

| Bucket size | Structure |
|-------------|-----------|
| ≤ 8 | Linked list |
| > 8 (and table ≥ 64) | Red-Black Tree |

→ Worst case lookup `O(log n)` instead of `O(n)`.

### Treeify Decision

```java
if (binCount >= TREEIFY_THRESHOLD - 1) {
    treeifyBin(tab, hash);
}

// treeifyBin first checks:
if (n < MIN_TREEIFY_CAPACITY) {
    resize();      // table choti hai → pehle resize karo, treeify nahi
}
```

→ Choti table mein collisions zyada → resize hi sahi answer.

### `UNTREEIFY_THRESHOLD = 6`

Resize ke baad bucket fragments → wapas list mein convert if size ≤ 6.

---

## Resize / Rehash

### Trigger

```
size > threshold   →   newCapacity = oldCapacity << 1   (double)
```

### Cost

`O(n)` because saare entries new buckets mein redistribute.

### Smart Move (Java 8)

Power-of-2 doubling ki wajah se entry ya **same bucket** rahti ya **bucket + oldCapacity** — recompute mod nahi karna padta.

```
new index = old index   OR   old index + oldCapacity
```

→ Decided by one extra hash bit.

---

## `get` Flow

```
1. hash = (key.hashCode()) ^ (h >>> 16)
2. index = (n - 1) & hash
3. first = table[index]
4. check first key match
5. else if TreeNode → tree get
6. else walk list comparing hash + (key == || equals)
```

---

## `equals` & `hashCode` Contract

### Rule (must)

Equal objects → same `hashCode`.
**Same `hashCode` ≠ equal** (collision allowed).

### Why important for HashMap?

- `put(k1, v)` then look up with `k2.equals(k1) == true` but `k1.hashCode() != k2.hashCode()` → bucket mismatch → "lost" entry.
- `key` mutate ho jaye after put → hash changes → `get` fails ("ghost entry").

```java
Map<Point, String> map = new HashMap<>();
Point p = new Point(1, 2);
map.put(p, "A");
p.x = 99;                    // mutated!
map.get(new Point(1, 2));    // null — bucket alag
map.get(p);                   // also null — hash differ from put time
```

→ **Keys ko immutable rakho.** (Cross-ref: `04-Equals-and-HashCode.md`.)

---

## `HashMap` vs `LinkedHashMap` vs `TreeMap` vs `ConcurrentHashMap`

| | HashMap | LinkedHashMap | TreeMap | ConcurrentHashMap |
|--|---------|---------------|---------|-------------------|
| Order | None | Insertion / access | Sorted (Comparator) | None |
| Null key | 1 | 1 | ❌ | ❌ |
| Null value | ✅ | ✅ | ✅ | ❌ |
| Lookup | O(1) avg | O(1) avg | O(log n) | O(1) avg |
| Thread-safe | ❌ | ❌ | ❌ | ✅ |
| Use | General | LRU cache, predictable iteration | Sorted views | Concurrent reads/writes |

### LRU with `LinkedHashMap`

```java
new LinkedHashMap<K,V>(16, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry<K,V> e) {
        return size() > MAX;
    }
};
```

`accessOrder = true` → access par re-order → eldest = least recently used.

### `ConcurrentHashMap` Highlights

- Java 7: segment locking (16 stripes default).
- Java 8+: **per-bucket CAS + synchronized head**, finer granularity.
- No null key/value (nicht ambiguous "absent vs null").
- `compute`, `computeIfAbsent`, `merge` — atomic single-key updates.

---

## `ConcurrentModificationException`

### Fail-Fast

```java
Map<String, Integer> m = new HashMap<>();
m.put("a", 1);
m.put("b", 2);

for (String k : m.keySet()) {
    if (k.equals("a")) m.remove(k);   // ❌ CME
}
```

### Iterator-Safe Removal

```java
Iterator<String> it = m.keySet().iterator();
while (it.hasNext()) {
    if (it.next().equals("a")) it.remove();   // ✅
}
```

### Java 8+

```java
m.entrySet().removeIf(e -> e.getKey().equals("a"));
```

### `ConcurrentHashMap`

Iterators **weakly consistent** — no CME, but may not see latest concurrent updates.

---

## Java 7 vs Java 8 Differences

| | Java 7 | Java 8 |
|--|--------|--------|
| Bucket structure | Linked list | List → Tree at threshold |
| Hash function | Multiple shifts | `h ^ (h >>> 16)` |
| Resize move | Recompute mod | One extra bit logic |
| Worst lookup | O(n) | O(log n) |
| Concurrent issue | Resize infinite loop (multi-thread) | Better (still not safe) |

---

## Common Interview Qs

### Q1. HashMap thread-safe hai?
Nahi. Concurrent puts → corrupted internal state. Use `ConcurrentHashMap`.

### Q2. `null` key allowed?
HashMap mein **1 null key** allowed (special bucket 0). ConcurrentHashMap mein **nahi**.

### Q3. Why power-of-2 capacity?
`(n-1) & hash` cheap modulo equivalent — only works for power of 2.

### Q4. Load factor 0.75 kyon?
Time-space trade-off — papers + benchmarks par tuned: lookup speed vs space waste sweet spot.

### Q5. Treeify always at 8?
Threshold 8 par check hota; agar table.length < 64 to pehle resize hota hai (treeify defer).

### Q6. Mutable key = ?
Disaster — entry "lost" ho jaati. Always immutable keys (`String`, `Integer`, your immutable VO).

### Q7. Order guaranteed?
HashMap → no. LinkedHashMap → insertion (or access). TreeMap → comparator order.

---

## Pitfalls

1. **Mutable keys** — hash change → ghost entries.
2. **Missing `hashCode`** — sab Object default → bucket scattered.
3. **Bad `hashCode`** (e.g. `return 1`) — all collide → list/tree bloat.
4. **Concurrent put** — corrupted; use `ConcurrentHashMap`.
5. **`null` key in `ConcurrentHashMap`** — NPE.
6. **Iterating + modifying** — CME.
7. **Computing capacity from expected size**: `Math.ceil(n / loadFactor)` to avoid mid-flight resize.

---

## Cheat Sheet

| Item | Value |
|------|-------|
| Default capacity | 16 |
| Load factor | 0.75 |
| Treeify threshold | 8 |
| Untreeify threshold | 6 |
| Min treeify capacity | 64 |
| Resize trigger | size > threshold |
| Bucket index | `(n-1) & hash` |
| Hash mix | `h ^ (h >>> 16)` |

| Map | Pick When |
|-----|----------|
| HashMap | General purpose |
| LinkedHashMap | Predictable iteration / LRU |
| TreeMap | Sorted / range queries |
| ConcurrentHashMap | Concurrent access |

---

## Practice

1. Bad `hashCode` (always 1) banake `put` 1000 keys + measure lookup time.
2. Mutable key class banake post-put mutation se `get` null show karo.
3. `LinkedHashMap` LRU cache implement.
4. Iterator vs `removeIf` for safe deletion compare.
5. Java 8 source `HashMap.put` step debugger se trace.
