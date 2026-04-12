# Generic Classes & Methods

## Status: Not Started

---

## Table of Contents

1. [Generic Class Declaration &lt;T&gt;](#generic-class-declaration-t)
2. [Multiple Type Params &lt;K,V&gt;](#multiple-type-params-kv)
3. [Generic Methods](#generic-methods)
4. [Type Inference with Diamond Operator &lt;&gt;](#type-inference-with-diamond-operator-)
5. [Raw Types (Avoid Them)](#raw-types-avoid-them)

---

## Generic Class Declaration &lt;T&gt;

**Matlab:** Class ko type-parameterized banana — compile-time pe type safety mil jaati hai, casting ki zarurat nahi.

### The Problem Without Generics

```java
// ❌ Without Generics — type safety nahi, casting chahiye
class Box {
    private Object content;

    public void setContent(Object content) {
        this.content = content;
    }

    public Object getContent() {
        return content;
    }
}

Box box = new Box();
box.setContent("Hello");
String str = (String) box.getContent();  // Manual casting — error-prone

box.setContent(123);  // ❌ Koi bhi object daal sakta hai — type safety nahi
String wrong = (String) box.getContent();  // ❌ ClassCastException at runtime!
```

### The Solution With Generics

```java
// ✅ With Generics — type-safe, no casting
class Box<T> {
    private T content;

    public void setContent(T content) {
        this.content = content;
    }

    public T getContent() {
        return content;
    }
}

// Usage
Box<String> stringBox = new Box<>();
stringBox.setContent("Hello");
String str = stringBox.getContent();  // No casting needed!

// stringBox.setContent(123);  // ❌ Compile-time error! Type-safe
```

### How Type Parameter Works

```java
class Box<T> {
    // T = Type Parameter (placeholder)
    // Actual type compile-time pe replace ho jaati hai (Type Erasure)

    private T content;       // T = String → String content
    private T[] items;       // T = Integer → Integer[] items
    private List<T> list;    // T = Double → List<Double> list

    public T getContent() {  // Return type bhi T se resolve hota hai
        return content;
    }

    public void setContent(T content) {  // Parameter type bhi T
        this.content = content;
    }
}
```

### Real-World Example: Generic Repository

```java
class Repository<T> {
    private List<T> items = new ArrayList<>();

    public void add(T item) {
        items.add(item);
    }

    public T get(int index) {
        return items.get(index);
    }

    public List<T> getAll() {
        return new ArrayList<>(items);
    }

    public void remove(T item) {
        items.remove(item);
    }

    public int size() {
        return items.size();
    }
}

// Usage
Repository<User> userRepo = new Repository<>();
userRepo.add(new User("Sachin", 25));
userRepo.add(new User("Rahul", 30));

User user = userRepo.get(0);  // No casting!
List<User> allUsers = userRepo.getAll();  // Type-safe list

Repository<Product> productRepo = new Repository<>();
productRepo.add(new Product("Laptop", 50000));
Product product = productRepo.get(0);  // No casting!
```

---

## Multiple Type Params &lt;K,V&gt;

**Matlab:** Ek se zyada type parameters use karna — key-value pairs, tuples, etc.

### Basic Usage

```java
class Pair<K, V> {
    private K key;
    private V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }

    @Override
    public String toString() {
        return "(" + key + ", " + value + ")";
    }
}

// Usage
Pair<String, Integer> age = new Pair<>("Sachin", 25);
String name = age.getKey();     // "Sachin"
Integer ageValue = age.getValue();  // 25

Pair<String, List<String>> subjectMap = new Pair<>("Sachin", List.of("Math", "Science"));
```

### Triple (Three Type Params)

```java
class Triple<A, B, C> {
    private A first;
    private B second;
    private C third;

    public Triple(A first, B second, C third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public A getFirst() { return first; }
    public B getSecond() { return second; }
    public C getThird() { return third; }
}

// Usage
Triple<String, Integer, Boolean> result = new Triple<>("Sachin", 25, true);
```

### Generic Map Entry

```java
class MyEntry<K, V> {
    private K key;
    private V value;

    public MyEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() { return key; }
    public V getValue() { return value; }
    public void setValue(V value) { this.value = value; }
}

class MyMap<K, V> {
    private List<MyEntry<K, V>> entries = new ArrayList<>();

    public void put(K key, V value) {
        entries.add(new MyEntry<>(key, value));
    }

    public V get(K key) {
        for (MyEntry<K, V> entry : entries) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
```

---

## Generic Methods

**Matlab:** Methods ko type-parameterized banana — class generic nahi bhi ho sakti, method generic ho sakta hai.

### Generic Method Declaration

```java
class Utility {

    // Generic method — type parameter method level pe
    public static <T> T getFirst(T[] array) {
        return array.length > 0 ? array[0] : null;
    }

    // Generic method with multiple params
    public static <T> void printArray(T[] array) {
        for (T item : array) {
            System.out.println(item);
        }
    }

    // Generic method with return type different from params
    public static <T, R> R convert(T input, Function<T, R> converter) {
        return converter.apply(input);
    }
}

// Usage
String[] strings = {"Apple", "Banana", "Cherry"};
String first = Utility.getFirst(strings);  // T = String

Integer[] nums = {1, 2, 3, 4, 5};
Integer firstNum = Utility.getFirst(nums);  // T = Integer

Utility.printArray(strings);
Utility.printArray(nums);
```

### Generic Method in Non-Generic Class

```java
class Collections {

    // Class generic nahi hai, method generic hai
    public static <T> List<T> toList(T... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    public static <T> T max(T a, T b) {
        return ((Comparable<T>) a).compareTo(b) > 0 ? a : b;
    }

    public static <T> void swap(List<T> list, int i, int j) {
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}

// Usage
List<String> names = Collections.toList("Sachin", "Rahul", "Priya");
String maxName = Collections.max("Apple", "Banana");
Collections.swap(names, 0, 1);
```

### Generic Method with Multiple Type Params

```java
class Mapper {

    public static <T, R> List<R> map(List<T> list, Function<T, R> mapper) {
        List<R> result = new ArrayList<>();
        for (T item : list) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    public static <K, V> Map<K, V> toMap(List<K> keys, List<V> values) {
        if (keys.size() != values.size()) {
            throw new IllegalArgumentException("Lists must be same size");
        }
        Map<K, V> map = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        return map;
    }
}

// Usage
List<String> names = List.of("Sachin", "Rahul");
List<Integer> lengths = Mapper.map(names, String::length);
// [6, 5]

Map<String, Integer> map = Mapper.toMap(
    List.of("A", "B", "C"),
    List.of(1, 2, 3)
);
// {A=1, B=2, C=3}
```

---

## Type Inference with Diamond Operator &lt;&gt;

**Matlab:** Java 7+ mein type inference — compiler automatically type deduce kar leta hai.

### Before Diamond Operator (Java 6)

```java
// ❌ Verbose — type do baar likhna padta tha
Map<String, List<Integer>> map = new HashMap<String, List<Integer>>();
List<String> list = new ArrayList<String>();
Pair<String, Integer> pair = new Pair<String, Integer>("Sachin", 25);
```

### With Diamond Operator (Java 7+)

```java
// ✅ Concise — compiler type infer kar leta hai
Map<String, List<Integer>> map = new HashMap<>();
List<String> list = new ArrayList<>();
Pair<String, Integer> pair = new Pair<>("Sachin", 25);
```

### How Type Inference Works

```java
// Compiler right side ka type left side se deduce karta hai
List<String> list = new ArrayList<>();
// ← Left side: List<String> → Compiler: ArrayList<String> banao

Map<String, Integer> map = new HashMap<>();
// ← Left side: Map<String, Integer> → Compiler: HashMap<String, Integer> banao
```

### Diamond Operator with Methods

```java
// Generic method — type inference automatic hai
Utility.<String>getFirst(new String[]{"A", "B"});  // Explicit type
Utility.getFirst(new String[]{"A", "B"});          // Inferred — String

// Java 8+ — target type inference
List<String> list = Collections.emptyList();  // Compiler infers String
Set<Integer> set = Collections.emptySet();    // Compiler infers Integer
```

---

## Raw Types (Avoid Them)

**Matlab:** Generic class bina type argument ke use karna — type safety lost ho jaati hai.

### What is a Raw Type?

```java
// Generic class
class Box<T> {
    private T content;
    public T getContent() { return content; }
    public void setContent(T content) { this.content = content; }
}

// ✅ Parameterized type — type-safe
Box<String> stringBox = new Box<>();
stringBox.setContent("Hello");
String str = stringBox.getContent();  // No casting

// ❌ Raw type — type safety lost
Box rawBox = new Box();
rawBox.setContent("Hello");
rawBox.setContent(123);  // ❌ Koi bhi object daal sakta hai!
String wrong = (String) rawBox.getContent();  // ❌ ClassCastException possible!
```

### Why Raw Types are Bad

```java
// Raw type se type safety completely khatam
List<String> stringList = new ArrayList<>();
stringList.add("Hello");

List rawList = stringList;  // Raw type reference
rawList.add(123);  // ❌ Integer add ho gaya — compile-time pe koi error nahi!

for (String s : stringList) {  // ❌ ClassCastException at runtime!
    System.out.println(s.toUpperCase());
}
```

### Compiler Warnings

```java
// Raw type use karne pe compiler warning aata hai
Box rawBox = new Box();  // ⚠️ Warning: Box is a raw type

// @SuppressWarnings se warning hide kar sakte ho (but avoid karo)
@SuppressWarnings("rawtypes")
Box rawBox = new Box();
```

### Legacy Code Compatibility

```java
// Raw types sirf legacy code ke saath kaam karne ke liye hain
// Java 5 se pehle generics nahi the — isliye raw types exist karte hain

// Naye code mein hamesha parameterized types use karo
List<String> list = new ArrayList<>();  // ✅
List rawList = new ArrayList();         // ❌ Avoid
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Generic Class &lt;T&gt;** | Type parameterized class — compile-time type safety |
| **Multiple Type Params** | &lt;K, V&gt; — key-value pairs, tuples, etc. |
| **Generic Methods** | Method level pe type parameter — class generic nahi bhi ho sakti |
| **Diamond Operator &lt;&gt;** | Java 7+ — compiler type infer kar leta hai |
| **Raw Types** | Bina type argument ke — type safety lost, avoid karo |
| **Type Erasure** | Compile-time pe generic types replace ho jaati hain — runtime pe nahi hoti |
