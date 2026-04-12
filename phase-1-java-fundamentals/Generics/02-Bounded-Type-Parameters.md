# Bounded Type Parameters

## Status: Not Started

---

## Table of Contents

1. [Upper Bounded (&lt;T extends Number&gt;)](#upper-bounded-t-extends-number)
2. [Lower Bounded (&lt;? super Integer&gt;)](#lower-bounded--super-integer)
3. [Multiple Bounds (&lt;T extends Comparable & Serializable&gt;)](#multiple-bounds-t-extends-comparable--serializable)
4. [When to Use Bounded Types](#when-to-use-bounded-types)

---

## Upper Bounded (&lt;T extends Number&gt;)

**Matlab:** Type parameter ko restrict karna — sirf certain types (ya unke subtypes) allowed honge.

### The Problem Without Bounds

```java
// ❌ Without bounds — koi bhi type aa sakta hai
class Calculator<T> {
    public T add(T a, T b) {
        // ❌ T pe koi operation nahi kar sakte — compiler ko pata nahi T kya hai
        // return a + b;  // ❌ Error!
        return null;
    }
}
```

### Solution: Upper Bound

```java
// ✅ Upper bound — sirf Number ya uske subtypes allowed
class Calculator<T extends Number> {
    public double add(T a, T b) {
        return a.doubleValue() + b.doubleValue();  // ✅ doubleValue() Number ka method hai
    }

    public double multiply(T a, T b) {
        return a.doubleValue() * b.doubleValue();
    }
}

// Usage
Calculator<Integer> intCalc = new Calculator<>();
System.out.println(intCalc.add(5, 10));  // 15.0

Calculator<Double> doubleCalc = new Calculator<>();
System.out.println(doubleCalc.add(3.14, 2.86));  // 6.0

// Calculator<String> strCalc = new Calculator<>();  // ❌ Error! String extends Number nahi karta
```

### Bounded Generic Method

```java
class MathUtils {

    // Upper bounded type parameter
    public static <T extends Number> double sum(List<T> numbers) {
        double total = 0;
        for (T num : numbers) {
            total += num.doubleValue();
        }
        return total;
    }

    public static <T extends Number> T max(T a, T b) {
        return Double.compare(a.doubleValue(), b.doubleValue()) > 0 ? a : b;
    }
}

// Usage
List<Integer> ints = List.of(1, 2, 3, 4, 5);
System.out.println(MathUtils.sum(ints));  // 15.0

List<Double> doubles = List.of(1.1, 2.2, 3.3);
System.out.println(MathUtils.sum(doubles));  // 6.6

// MathUtils.sum(List.of("A", "B"));  // ❌ Error! String Number nahi hai
```

### Common Upper Bounds

```java
// Comparable bound — sorting/comparison ke liye
public static <T extends Comparable<T>> T findMax(List<T> list) {
    T max = list.get(0);
    for (T item : list) {
        if (item.compareTo(max) > 0) {
            max = item;
        }
    }
    return max;
}

// Number bound — mathematical operations ke liye
public static <T extends Number> double average(List<T> numbers) {
    return numbers.stream()
        .mapToDouble(Number::doubleValue)
        .average()
        .orElse(0);
}

// CharSequence bound — String, StringBuilder, etc. ke liye
public static <T extends CharSequence> int totalLength(List<T> strings) {
    return strings.stream()
        .mapToInt(CharSequence::length)
        .sum();
}
```

---

## Lower Bounded (&lt;? super Integer&gt;)

**Matlab:** Type parameter ko lower bound dena — specified type ya uske supertypes allowed honge.

### Upper Bound vs Lower Bound

```java
// Upper bound (? extends T) — T ya uske subtypes
List<? extends Number> numbers;
// Accepts: List<Integer>, List<Double>, List<Number>

// Lower bound (? super T) — T ya uske supertypes
List<? super Integer> integers;
// Accepts: List<Integer>, List<Number>, List<Object>
```

### Lower Bound Example

```java
// Lower bounded method — Integer ya uske supertypes mein add kar sakte ho
public static void addNumbers(List<? super Integer> list, int count) {
    for (int i = 1; i <= count; i++) {
        list.add(i);  // ✅ Integer add kar sakte ho
    }
}

// Usage
List<Integer> intList = new ArrayList<>();
addNumbers(intList, 5);
System.out.println(intList);  // [1, 2, 3, 4, 5]

List<Number> numList = new ArrayList<>();
addNumbers(numList, 3);
System.out.println(numList);  // [1, 2, 3]

List<Object> objList = new ArrayList<>();
addNumbers(objList, 2);
System.out.println(objList);  // [1, 2]
```

### PECS Rule

**Producer Extends, Consumer Super**

```java
// Producer — data read karte ho → ? extends T
public static double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number n : numbers) {  // ✅ Read kar sakte ho
        total += n.doubleValue();
    }
    return total;
}

// Consumer — data write karte ho → ? super T
public static void addNumbers(List<? super Integer> list, int count) {
    for (int i = 1; i <= count; i++) {
        list.add(i);  // ✅ Write kar sakte ho
    }
}
```

### PECS Explanation

```
? extends T (Producer):
- List<Integer> → List<? extends Number>
- Read kar sakte ho (Number milega)
- Write nahi kar sakte (compiler allow nahi karega)
- Kyunki actual type kya hai pata nahi — Integer? Double?

? super T (Consumer):
- List<Number> → List<? super Integer>
- Write kar sakte ho (Integer add kar sakte ho)
- Read kar sakte ho lekin sirf Object milega
- Kyunki actual type Number ya Object ho sakta hai
```

### PECS Examples

```java
// ✅ Producer — List se read karo
List<Integer> ints = List.of(1, 2, 3);
List<? extends Number> numbers = ints;
Number n = numbers.get(0);  // ✅ Read works
// numbers.add(4);  // ❌ Write nahi kar sakte

// ✅ Consumer — List mein write karo
List<Number> nums = new ArrayList<>();
List<? super Integer> intSup = nums;
intSup.add(5);  // ✅ Write works
Object obj = intSup.get(0);  // Read karo toh Object milega
```

---

## Multiple Bounds (&lt;T extends Comparable & Serializable&gt;)

**Matlab:** Ek type parameter ko multiple bounds dena — T ko multiple interfaces/classes implement karne honge.

### Syntax

```java
// Multiple bounds — & use karo, comma nahi
// First bound class ya interface ho sakta hai
// Baaki bounds sirf interfaces honge

public static <T extends Comparable<T> & Serializable> T process(T item) {
    // T ko Comparable aur Serializable dono implement karna padega
    return item;
}
```

### Example: Multiple Bounds

```java
class SortableContainer<T extends Comparable<T> & Serializable> {
    private List<T> items = new ArrayList<>();

    public void add(T item) {
        items.add(item);
    }

    public void sort() {
        Collections.sort(items);  // ✅ sort() ke liye Comparable chahiye
    }

    public byte[] serialize() throws IOException {
        // ✅ Serialization ke liye Serializable chahiye
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(items);
            return baos.toByteArray();
        }
    }
}

// Usage — String dono interfaces implement karta hai
SortableContainer<String> container = new SortableContainer<>();
container.add("Banana");
container.add("Apple");
container.add("Cherry");
container.sort();
System.out.println(container);  // [Apple, Banana, Cherry]
```

### Class + Interface Bounds

```java
// First bound class ho sakta hai, baaki interfaces
// <T extends Class & Interface1 & Interface2>

class Processor<T extends Number & Comparable<T>> {
    public int compare(T a, T b) {
        return a.compareTo(b);  // ✅ Number + Comparable
    }

    public double process(T value) {
        return value.doubleValue();  // ✅ Number ka method
    }
}

// Usage
Processor<Integer> intProc = new Processor<>();  // Integer extends Number implements Comparable
System.out.println(intProc.compare(5, 10));  // -1
System.out.println(intProc.process(3.14));   // Error! Integer pass karo

Processor<Double> doubleProc = new Processor<>();
System.out.println(doubleProc.compare(3.14, 2.71));  // 1
System.out.println(doubleProc.process(3.14));  // 3.14
```

### Multiple Bounds with Generic Method

```java
public static <T extends Comparable<T> & Serializable> void saveAndSort(List<T> list) {
    Collections.sort(list);  // Sort (Comparable needed)
    // Serialize to file (Serializable needed)
}

// String dono implement karta hai
List<String> names = new ArrayList<>(List.of("Sachin", "Rahul", "Priya"));
saveAndSort(names);  // ✅ Works
System.out.println(names);  // [Priya, Rahul, Sachin]

// Integer bhi dono implement karta hai
List<Integer> nums = new ArrayList<>(List.of(5, 2, 8, 1));
saveAndSort(nums);  // ✅ Works
System.out.println(nums);  // [1, 2, 5, 8]
```

---

## When to Use Bounded Types

### Use Upper Bound (&lt;T extends X&gt;)

```java
// ✅ Use when:
// - Type pe specific methods call karne hon
// - Type ko restrict karna ho
// - Comparison/sorting operations

// Example: Generic sorting algorithm
public static <T extends Comparable<T>> void sort(List<T> list) {
    for (int i = 0; i < list.size() - 1; i++) {
        for (int j = 0; j < list.size() - i - 1; j++) {
            if (list.get(j).compareTo(list.get(j + 1)) > 0) {
                T temp = list.get(j);
                list.set(j, list.get(j + 1));
                list.set(j + 1, temp);
            }
        }
    }
}
```

### Use Lower Bound (&lt;? super T&gt;)

```java
// ✅ Use when:
// - Collection mein write karna ho
// - Consumer pattern — data push karna ho

// Example: Copy elements from one list to another
public static <T> void copy(List<? extends T> source, List<? super T> dest) {
    for (T item : source) {
        dest.add(item);  // dest mein write kar rahe ho
    }
}

// Usage
List<Integer> source = List.of(1, 2, 3);
List<Number> dest = new ArrayList<>();
copy(source, dest);  // ✅ dest accepts Integer
```

### Use Multiple Bounds

```java
// ✅ Use when:
// - Type ko multiple contracts implement karne hon
// - Sorting + serialization dono chahiye
// - Comparison + cloning dono chahiye

// Example: Cloneable + Comparable
public static <T extends Comparable<T> & Cloneable> T cloneAndCompare(T original, T other) {
    T clone = original.clone();
    return clone.compareTo(other) > 0 ? clone : other;
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Upper Bounded** | `&lt;T extends Number&gt;` — T = Number ya uska subtype |
| **Lower Bounded** | `&lt;? super Integer&gt;` — ? = Integer ya uska supertype |
| **PECS Rule** | Producer Extends, Consumer Super |
| **Multiple Bounds** | `&lt;T extends Comparable & Serializable&gt;` — & use karo |
| **Class + Interface** | Pehla bound class, baaki interfaces — `&lt;T extends Number & Comparable&lt;T&gt;&gt;` |
| **Upper Bound Use** | Methods call karne ke liye type chahiye |
| **Lower Bound Use** | Collection mein write karne ke liye |
