# Wildcards

## Status: Not Started

---

## Table of Contents

1. [Unbounded Wildcard &lt;?&gt;](#unbounded-wildcard-)
2. [Upper Bounded &lt;? extends T&gt;](#upper-bounded--extends-t)
3. [Lower Bounded &lt;? super T&gt;](#lower-bounded--super-t)
4. [Wildcard Capture](#wildcard-capture)

---

## Unbounded Wildcard &lt;?&gt;

**Matlab:** Unknown type — koi bhi type ho sakta hai. Jab type matter nahi karta tab use karte hain.

### Basic Usage

```java
// Unbounded wildcard — koi bhi type
public static void printList(List<?> list) {
    for (Object item : list) {
        System.out.println(item);
    }
}

// Usage
printList(List.of("Apple", "Banana"));     // List<String>
printList(List.of(1, 2, 3, 4, 5));         // List<Integer>
printList(List.of(1.1, 2.2, 3.3));        // List<Double>
```

### When to Use Unbounded Wildcard

```java
// ✅ Use when:
// - Type matter nahi karta
// - Sirf read operations (Object methods)
// - Generic methods jo kisi bhi type pe kaam karein

// Example: Size check karo — type matter nahi karta
public static boolean isEmpty(Collection<?> collection) {
    return collection.isEmpty();
}

// Example: Clear karo — type matter nahi karta
public static void clearAll(Collection<?> collection) {
    collection.clear();
}

// Example: Contains check — Object use karta hai
public static boolean containsAny(Collection<?> collection, Object target) {
    return collection.contains(target);
}
```

### Unbounded Wildcard vs Raw Type

```java
// ✅ Unbounded wildcard — type-safe
public static void printList(List<?> list) {
    for (Object item : list) {
        System.out.println(item);
    }
}

// ❌ Raw type — type-safe nahi
public static void printListRaw(List list) {
    for (Object item : list) {
        System.out.println(item);
    }
}

// Unbounded wildcard use karo, raw types avoid karo!
```

### Limitations

```java
public static void process(List<?> list) {
    // ❌ Cannot add (except null)
    list.add("Hello");  // ❌ Error! Type unknown hai

    // ✅ Can read as Object
    Object item = list.get(0);

    // ✅ Can call methods that don't depend on type
    int size = list.size();
    boolean empty = list.isEmpty();
}
```

---

## Upper Bounded &lt;? extends T&gt;

**Matlab:** T ya uske subtypes — reading/producing data ke liye.

### Basic Usage

```java
// Upper bounded wildcard — T ya uske subtypes
public static double sum(List<? extends Number> numbers) {
    double total = 0;
    for (Number num : numbers) {  // ✅ As Number read kar sakte ho
        total += num.doubleValue();
    }
    return total;
}

// Usage
List<Integer> ints = List.of(1, 2, 3, 4, 5);
System.out.println(sum(ints));  // 15.0

List<Double> doubles = List.of(1.1, 2.2, 3.3);
System.out.println(sum(doubles));  // 6.6

List<Number> numbers = List.of(1, 2.5, 3L);
System.out.println(sum(numbers));  // 6.5
```

### What You Can Do

```java
public static void process(List<? extends Number> list) {
    // ✅ Read as Number
    Number num = list.get(0);

    // ✅ Call Number methods
    double d = list.get(0).doubleValue();
    int i = list.get(0).intValue();

    // ✅ Read-only operations
    int size = list.size();
    boolean contains = list.contains(5);

    // ❌ Cannot write (except null)
    list.add(10);         // ❌ Error!
    list.set(0, 100);     // ❌ Error!
}
```

### Why Can't We Write?

```java
List<Integer> intList = new ArrayList<>();
List<? extends Number> numbers = intList;

// Agar allow hota:
// numbers.add(3.14);  // Double add karne ki koshish

// Lekin actual list List<Integer> hai!
// Integer list mein Double add nahi ho sakta — type safety break!

// Isliye compiler write operations allow nahi karta
```

### Real-World Examples

```java
// Find maximum element
public static <T extends Comparable<? super T>> T max(List<? extends T> list) {
    T max = list.get(0);
    for (T item : list) {
        if (item.compareTo(max) > 0) {
            max = item;
        }
    }
    return max;
}

// Calculate average
public static double average(List<? extends Number> numbers) {
    return numbers.stream()
        .mapToDouble(Number::doubleValue)
        .average()
        .orElse(0);
}

// Filter and return
public static <T> List<T> filter(List<? extends T> source, Predicate<T> predicate) {
    List<T> result = new ArrayList<>();
    for (T item : source) {
        if (predicate.test(item)) {
            result.add(item);
        }
    }
    return result;
}
```

---

## Lower Bounded &lt;? super T&gt;

**Matlab:** T ya uske supertypes — writing/consuming data ke liye.

### Basic Usage

```java
// Lower bounded wildcard — T ya uske supertypes
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

### What You Can Do

```java
public static void process(List<? super Integer> list) {
    // ✅ Write Integer (or its subtypes)
    list.add(10);
    list.add(20);

    // ✅ Read as Object (not as Integer!)
    Object obj = list.get(0);

    // ❌ Cannot read as Integer
    // Integer num = list.get(0);  // ❌ Error! Object milega
}
```

### Why Read Returns Object?

```java
List<Object> objList = new ArrayList<>();
List<? super Integer> list = objList;

list.add(42);  // ✅ Integer add kar sakte ho

// Agar read karne pe Integer milta:
// Integer num = list.get(0);
// Lekin actual list mein String bhi ho sakta hai!
// Isliye sirf Object return hota hai
```

### Real-World Examples

```java
// Copy elements — source se read, dest mein write
public static <T> void copy(List<? extends T> source, List<? super T> dest) {
    for (T item : source) {
        dest.add(item);  // ✅ dest mein write kar rahe ho
    }
}

// Usage
List<Integer> source = List.of(1, 2, 3);
List<Number> dest = new ArrayList<>();
copy(source, dest);
System.out.println(dest);  // [1, 2, 3]

// Add all from array to list
public static <T> void addAll(T[] array, List<? super T> list) {
    for (T item : array) {
        list.add(item);
    }
}

// Usage
String[] names = {"Sachin", "Rahul", "Priya"};
List<Object> objects = new ArrayList<>();
addAll(names, objects);
System.out.println(objects);  // [Sachin, Rahul, Priya]
```

---

## Wildcard Capture

**Matlab:** Compiler ko wildcard ko specific type mein convert karwana — generic methods ke andar use hota hai.

### The Problem

```java
// ❌ Wildcard ko directly use nahi kar sakte
public static void reverse(List<?> list) {
    // ❌ Cannot capture wildcard for generic method call
    reverseHelper(list);  // ❌ Error!
}

private static <T> void reverseHelper(List<T> list) {
    // Reverse logic
    for (int i = 0; i < list.size() / 2; i++) {
        int j = list.size() - 1 - i;
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}
```

### Solution: Helper Method

```java
// ✅ Helper method se wildcard capture karo
public static void reverse(List<?> list) {
    reverseHelper(list);  // ✅ Compiler wildcard ko capture karta hai
}

private static <T> void reverseHelper(List<T> list) {
    for (int i = 0; i < list.size() / 2; i++) {
        int j = list.size() - 1 - i;
        T temp = list.get(i);
        list.set(i, list.get(j));
        list.set(j, temp);
    }
}

// Usage
List<String> names = new ArrayList<>(List.of("A", "B", "C", "D"));
reverse(names);
System.out.println(names);  // [D, C, B, A]

List<Integer> nums = new ArrayList<>(List.of(1, 2, 3, 4, 5));
reverse(nums);
System.out.println(nums);  // [5, 4, 3, 2, 1]
```

### Another Capture Example

```java
// Swap elements at two positions
public static void swap(List<?> list, int i, int j) {
    swapHelper(list, i, j);
}

private static <T> void swapHelper(List<T> list, int i, int j) {
    T temp = list.get(i);
    list.set(i, list.get(j));
    list.set(j, temp);
}

// Usage
List<String> list = new ArrayList<>(List.of("A", "B", "C"));
swap(list, 0, 2);
System.out.println(list);  // [C, B, A]
```

### When Capture Happens

```java
// ✅ Capture happens — compiler type infer kar leta hai
public static void process(List<?> list) {
    helperMethod(list);  // ✅ Capture
}

private static <T> void helperMethod(List<T> list) { }

// ✅ Direct generic method call — no capture needed
public static <T> void processDirect(List<T> list) {
    // T is known — no capture needed
}
```

### When Capture Fails

```java
// ❌ Capture doesn't work — multiple wildcards
public static void merge(List<?> list1, List<?> list2) {
    // mergeHelper(list1, list2);  // ❌ Error! Two independent wildcards
}

// ✅ Solution — bounded type parameter
public static <T> void merge(List<T> list1, List<T> list2) {
    // Both lists have same type T
}
```

---

## Wildcard Decision Guide

```
Wildcard chahiye?
├── Type matter nahi karta, sirf read (Object methods)? → <?>
├── Data read/produce kar rahe ho (T ya subtype)? → <? extends T>
├── Data write/consume kar rahe ho (T ya supertype)? → <? super T>
└── Dono read + write? → <T> (type parameter)
```

### Quick Reference

| Wildcard | Read Type | Write Type | Use Case |
|----------|-----------|------------|----------|
| **`<?>`** | Object | Nothing (except null) | Type doesn't matter |
| **`<? extends T>`** | T | Nothing (except null) | Producer — read data |
| **`<? super T>`** | Object | T (or subtype) | Consumer — write data |
| **`<T>`** | T | T | Both read and write |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Unbounded `<?>`** | Unknown type — sirf Object methods, read-only |
| **Upper Bounded `<? extends T>`** | T ya subtype — read as T, write nahi kar sakte |
| **Lower Bounded `<? super T>`** | T ya supertype — write T, read as Object |
| **PECS Rule** | Producer Extends, Consumer Super |
| **Wildcard Capture** | Helper method se wildcard ko specific type mein convert karo |
| **Multiple Wildcards** | Independent wildcards capture nahi hote — type parameter use karo |
