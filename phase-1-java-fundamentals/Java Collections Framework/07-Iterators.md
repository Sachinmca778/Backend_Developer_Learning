# Iterators

## Status: Not Started

---

## Table of Contents

1. [Iterator Interface](#iterator-interface)
2. [ListIterator](#listiterator)
3. [for-each Internally Uses Iterator](#for-each-internally-uses-iterator)
4. [ConcurrentModificationException](#concurrentmodificationexception)

---

## Iterator Interface

**Matlab:** Collection ko iterate karne ka standard tarika — `java.util.Iterator` interface.

### Interface Methods

```java
public interface Iterator<E> {
    boolean hasNext();    // Next element hai ya nahi?
    E next();             // Next element return karo
    default void remove() { throw new UnsupportedOperationException(); }  // Current element remove karo
}
```

### Basic Usage

```java
List<String> list = new ArrayList<>(List.of("Apple", "Banana", "Cherry"));

// Iterator get karo
Iterator<String> it = list.iterator();

// Iterate karo
while (it.hasNext()) {
    String fruit = it.next();
    System.out.println(fruit);
}
// Output: Apple, Banana, Cherry
```

### Remove During Iteration

```java
List<String> list = new ArrayList<>(List.of("Apple", "Banana", "Cherry", "Date"));

Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String fruit = it.next();
    if (fruit.length() > 5) {
        it.remove();  // ✅ Safe removal — Iterator ka remove() use karo
    }
}

System.out.println(list);  // [Apple, Date]
```

### Iterator on Set

```java
Set<String> set = new HashSet<>(Set.of("A", "B", "C"));

Iterator<String> it = set.iterator();
while (it.hasNext()) {
    String item = it.next();
    System.out.println(item);
}
```

### Iterator on Map

```java
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);

// KeySet iterator
Iterator<String> keyIt = map.keySet().iterator();
while (keyIt.hasNext()) {
    String key = keyIt.next();
    System.out.println(key + " → " + map.get(key));
}

// EntrySet iterator
Iterator<Map.Entry<String, Integer>> entryIt = map.entrySet().iterator();
while (entryIt.hasNext()) {
    Map.Entry<String, Integer> entry = entryIt.next();
    System.out.println(entry.getKey() + " → " + entry.getValue());
}
```

---

## ListIterator

**Matlab:** Iterator ka enhanced version — bidirectional iteration (forward + backward), aur list modification ke saath.

### Interface Methods

```java
public interface ListIterator<E> extends Iterator<E> {
    boolean hasNext();        // Aage element hai?
    E next();                 // Agle element pe jao
    boolean hasPrevious();    // Peeche element hai?
    E previous();             // Peeche wala element pe jao
    int nextIndex();          // Next element ka index
    int previousIndex();      // Previous element ka index
    void remove();            // Current element remove karo
    void set(E e);            // Current element replace karo
    void add(E e);            // Current position pe insert karo
}
```

### Forward Iteration

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));

ListIterator<String> it = list.listIterator();

while (it.hasNext()) {
    System.out.println(it.next());
}
// Output: A, B, C, D
```

### Backward Iteration

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));

// End se start karo
ListIterator<String> it = list.listIterator(list.size());

while (it.hasPrevious()) {
    System.out.println(it.previous());
}
// Output: D, C, B, A
```

### Both Directions

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));

ListIterator<String> it = list.listIterator();

// Forward
while (it.hasNext()) {
    String item = it.next();
    System.out.println("Forward: " + item);
}

// Ab iterator end pe hai — backward jao
while (it.hasPrevious()) {
    String item = it.previous();
    System.out.println("Backward: " + item);
}
```

### Index Tracking

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));

ListIterator<String> it = list.listIterator();

while (it.hasNext()) {
    int currentIndex = it.nextIndex();
    String item = it.next();
    System.out.println("Index " + currentIndex + ": " + item);
}
// Output:
// Index 0: A
// Index 1: B
// Index 2: C
// Index 3: D
```

### Modify During Iteration

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));

ListIterator<String> it = list.listIterator();

while (it.hasNext()) {
    String item = it.next();

    // Replace
    if (item.equals("B")) {
        it.set("X");  // "B" → "X"
    }

    // Add after current
    if (item.equals("C")) {
        it.add("Y");  // "C" ke baad "Y" add
    }

    // Remove
    if (item.equals("D")) {
        it.remove();  // "D" remove
    }
}

System.out.println(list);  // [A, X, C, Y]
```

### Start from Specific Index

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D", "E"));

// Index 2 se start karo
ListIterator<String> it = list.listIterator(2);

while (it.hasNext()) {
    System.out.println(it.next());  // C, D, E
}
```

---

## for-each Internally Uses Iterator

**Matlab:** Enhanced for-each loop internally Iterator use karta hai — compiler automatically convert karta hai.

### How It Works

```java
// for-each loop
for (String fruit : fruits) {
    System.out.println(fruit);
}

// Compiler internally convert karta hai:
Iterator<String> it = fruits.iterator();
while (it.hasNext()) {
    String fruit = it.next();
    System.out.println(fruit);
}
```

### Proof

```java
// Custom class jo Iterable implement karti hai
class MyCollection implements Iterable<String> {
    private List<String> items = new ArrayList<>();

    public void add(String item) {
        items.add(item);
    }

    @Override
    public Iterator<String> iterator() {
        System.out.println("iterator() called!");
        return items.iterator();
    }
}

// Usage
MyCollection coll = new MyCollection();
coll.add("A");
coll.add("B");

for (String item : coll) {  // iterator() automatically call hoga
    System.out.println(item);
}
// Output:
// iterator() called!
// A
// B
```

### Limitations of for-each

```java
// ❌ Cannot remove during iteration
for (String item : list) {
    if (item.length() > 5) {
        list.remove(item);  // ❌ ConcurrentModificationException
    }
}

// ✅ Use Iterator for removal
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    if (item.length() > 5) {
        it.remove();  // ✅ Safe
    }
}

// ❌ Cannot access index
for (String item : list) {
    // Index available nahi
}

// ✅ Use indexed for loop for index access
for (int i = 0; i < list.size(); i++) {
    System.out.println("Index " + i + ": " + list.get(i));
}

// ✅ Ya ListIterator use karo
ListIterator<String> it = list.listIterator();
while (it.hasNext()) {
    int index = it.nextIndex();
    String item = it.next();
    System.out.println("Index " + index + ": " + item);
}
```

### When to Use What

```
Iterate karna hai?
├── Sirf read karna hai? → for-each
├── Remove karna hai? → Iterator
├── Index chahiye? → indexed for loop ya ListIterator
├── Backward iteration chahiye? → ListIterator
└── Insert/replace during iteration? → ListIterator
```

---

## ConcurrentModificationException

**Matlab:** Jab collection ko iterate karne ke dauran modify karte ho (bina Iterator ke) — fail-fast behavior.

### What Causes It?

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));

// ❌ ConcurrentModificationException
for (String item : list) {
    if (item.equals("B")) {
        list.remove(item);  // ❌ Exception!
    }
}

// ❌ Same exception with explicit Iterator
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    if (item.equals("B")) {
        list.remove(item);  // ❌ Exception! Iterator.remove() use nahi kiya
    }
}
```

### Why It Happens

```
ArrayList internally "modCount" track karta hai — kitni baar modify hua
Iterator "expectedModCount" store karta hai — iteration start pe modCount

Jab list modify hoti hai → modCount badhta hai
Iterator next() call pe → modCount != expectedModCount → Exception!
```

### How to Fix

```java
// ✅ Fix 1: Iterator.remove() use karo
List<String> list = new ArrayList<>(List.of("A", "B", "C", "D"));
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    if (item.equals("B")) {
        it.remove();  // ✅ Safe — expectedModCount update hota hai
    }
}
System.out.println(list);  // [A, C, D]

// ✅ Fix 2: removeIf() use karo (Java 8+)
list.removeIf(item -> item.equals("B"));
System.out.println(list);  // [A, C, D]

// ✅ Fix 3: Copy pe iterate karo
for (String item : new ArrayList<>(list)) {
    if (item.equals("B")) {
        list.remove(item);  // ✅ Copy pe iterate, original pe modify
    }
}
System.out.println(list);  // [A, C, D]

// ✅ Fix 4: ListIterator use karo
ListIterator<String> lit = list.listIterator();
while (lit.hasNext()) {
    String item = lit.next();
    if (item.equals("B")) {
        lit.remove();  // ✅ Safe
    }
}

// ✅ Fix 5: Collect and remove later
List<String> toRemove = new ArrayList<>();
for (String item : list) {
    if (item.length() > 1) {
        toRemove.add(item);
    }
}
list.removeAll(toRemove);
```

### Multi-threading Scenario

```java
// Thread 1 iterate kar raha hai
// Thread 2 modify kar raha hai
// → ConcurrentModificationException

// Solution 1: Synchronized collection
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
synchronized (syncList) {
    for (String item : syncList) {
        // Safe — synchronized block ke andar
    }
}

// Solution 2: CopyOnWriteArrayList
CopyOnWriteArrayList<String> concurrentList = new CopyOnWriteArrayList<>();
concurrentList.add("A");
concurrentList.add("B");
concurrentList.add("C");

for (String item : concurrentList) {
    if (item.equals("B")) {
        concurrentList.remove(item);  // ✅ Safe — iteration pe copy use hota hai
    }
}

// Solution 3: ConcurrentHashMap
Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
concurrentMap.put("A", 1);
concurrentMap.put("B", 2);

for (Map.Entry<String, Integer> entry : concurrentMap.entrySet()) {
    if (entry.getValue() == 1) {
        concurrentMap.remove(entry.getKey());  // ✅ Safe
    }
}
```

### Fail-Fast vs Fail-Safe

```
Fail-Fast Iterators:
- ArrayList, HashMap, HashSet ke iterators
- Modification pe turant ConcurrentModificationException throw karte hain
- Fast failure — bug jaldi pakda jaata hai

Fail-Safe Iterators:
- CopyOnWriteArrayList, ConcurrentHashMap ke iterators
- Modification pe exception nahi — snapshot pe iterate karte hain
- Safe lekin stale data read ho sakti hai
```

### Fail-Fast Example

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C"));

try {
    for (String item : list) {
        list.remove(item);  // ❌ ConcurrentModificationException
    }
} catch (ConcurrentModificationException e) {
    System.out.println("Fail-fast: Modification detected during iteration");
}
```

### Fail-Safe Example

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));

for (String item : list) {
    list.remove(item);  // ✅ No exception — snapshot pe iterate ho raha hai
}
System.out.println(list);  // [] (empty — sab remove ho gaye)
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Iterator** | Collection iterate karne ka standard tarika — `hasNext()`, `next()`, `remove()` |
| **ListIterator** | Bidirectional — forward + backward, `add()`, `set()`, index access |
| **for-each** | Internally Iterator use karta hai — clean syntax lekin limitations |
| **ConcurrentModificationException** | Collection modify karte hue iterate karne pe — fail-fast behavior |
| **Fix: Iterator.remove()** | Safe removal during iteration |
| **Fix: removeIf()** | Java 8+ — predicate-based removal |
| **Fix: CopyOnWriteArrayList** | Thread-safe, fail-safe iteration |
| **Fail-Fast** | ArrayList, HashMap — exception throw karte hain |
| **Fail-Safe** | CopyOnWriteArrayList, ConcurrentHashMap — exception nahi |
