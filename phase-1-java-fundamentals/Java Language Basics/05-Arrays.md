# Arrays

## Status: Not Started

---

## Table of Contents

1. [1D Arrays](#1d-arrays)
2. [2D Arrays](#2d-arrays)
3. [Array Initialization](#array-initialization)
4. [Arrays Utility Class](#arrays-utility-class)
5. [System.arraycopy](#systemarraycopy)
6. [Array vs ArrayList Trade-offs](#array-vs-arraylist-trade-offs)

---

## 1D Arrays

**Matlab:** Same type ke elements ka fixed-size collection — contiguous memory mein store hote hain.

### Declaration & Creation

```java
// Declaration
int[] numbers;           // Preferred style
int numbers[];           // C-style (allowed but not preferred)

// Creation
numbers = new int[5];    // 5 elements, sab 0 (default value)

// Declaration + Creation
int[] nums = new int[5];

// Access elements
nums[0] = 10;
nums[1] = 20;
nums[2] = 30;
nums[3] = 40;
nums[4] = 50;

// Access by index
System.out.println(nums[0]);  // 10
System.out.println(nums[4]);  // 50

// Array length
int len = nums.length;  // 5 (property, not method)
```

### Default Values

```java
int[] intArr = new int[3];      // [0, 0, 0]
double[] doubleArr = new double[3];  // [0.0, 0.0, 0.0]
boolean[] boolArr = new boolean[3];  // [false, false, false]
String[] strArr = new String[3];     // [null, null, null]
char[] charArr = new char[3];        // ['\u0000', '\u0000', '\u0000']
```

### Array Index Out of Bounds

```java
int[] nums = new int[5];  // Valid indices: 0 to 4

nums[5] = 100;  // ❌ ArrayIndexOutOfBoundsException!
nums[-1] = 100; // ❌ ArrayIndexOutOfBoundsException!
```

### Iterating Arrays

```java
int[] numbers = {10, 20, 30, 40, 50};

// Regular for loop
for (int i = 0; i < numbers.length; i++) {
    System.out.println("Index " + i + ": " + numbers[i]);
}

// Enhanced for-each
for (int num : numbers) {
    System.out.println(num);
}

// Stream (Java 8+)
Arrays.stream(numbers).forEach(System.out::println);
```

### Array of Objects

```java
String[] fruits = new String[3];
fruits[0] = "Apple";
fruits[1] = "Banana";
fruits[2] = "Cherry";

// Null check zaruri hai
for (String fruit : fruits) {
    if (fruit != null) {
        System.out.println(fruit.toUpperCase());
    }
}
```

---

## 2D Arrays

**Matlab:** Array of arrays — rows aur columns mein data store karna (matrix).

### Declaration & Creation

```java
// 2D array — 3 rows, 4 columns
int[][] matrix = new int[3][4];

// Access elements
matrix[0][0] = 1;
matrix[0][1] = 2;
matrix[0][2] = 3;
matrix[0][3] = 4;

matrix[1][0] = 5;
matrix[1][1] = 6;
matrix[1][2] = 7;
matrix[1][3] = 8;

// Dimensions
int rows = matrix.length;        // 3
int cols = matrix[0].length;     // 4
```

### Iterating 2D Arrays

```java
int[][] matrix = {
    {1, 2, 3, 4},
    {5, 6, 7, 8},
    {9, 10, 11, 12}
};

// Nested for loop
for (int i = 0; i < matrix.length; i++) {
    for (int j = 0; j < matrix[i].length; j++) {
        System.out.print(matrix[i][j] + " ");
    }
    System.out.println();
}

// Enhanced for-each
for (int[] row : matrix) {
    for (int num : row) {
        System.out.print(num + " ");
    }
    System.out.println();
}

// Deep toString (print karo)
System.out.println(Arrays.deepToString(matrix));
// [[1, 2, 3, 4], [5, 6, 7, 8], [9, 10, 11, 12]]
```

### Jagged Arrays (Unequal Row Lengths)

```java
// Har row ki alag length ho sakti hai
int[][] jagged = new int[3][];
jagged[0] = new int[2];  // Row 0: 2 elements
jagged[1] = new int[4];  // Row 1: 4 elements
jagged[2] = new int[3];  // Row 2: 3 elements

// Initialize
jagged[0] = new int[]{1, 2};
jagged[1] = new int[]{3, 4, 5, 6};
jagged[2] = new int[]{7, 8, 9};

// Iterate
for (int[] row : jagged) {
    for (int num : row) {
        System.out.print(num + " ");
    }
    System.out.println();
}
```

### Common 2D Array Operations

```java
// Matrix addition
int[][] add(int[][] a, int[][] b) {
    int[][] result = new int[a.length][a[0].length];
    for (int i = 0; i < a.length; i++) {
        for (int j = 0; j < a[i].length; j++) {
            result[i][j] = a[i][j] + b[i][j];
        }
    }
    return result;
}

// Transpose
int[][] transpose(int[][] matrix) {
    int rows = matrix.length;
    int cols = matrix[0].length;
    int[][] result = new int[cols][rows];

    for (int i = 0; i < rows; i++) {
        for (int j = 0; j < cols; j++) {
            result[j][i] = matrix[i][j];
        }
    }
    return result;
}
```

---

## Array Initialization

### Basic Initialization

```java
// Declaration + initialization
int[] numbers = {10, 20, 30, 40, 50};

// String array
String[] fruits = {"Apple", "Banana", "Cherry"};

// Anonymous array
printArray(new int[]{1, 2, 3, 4});  // Method call mein directly
```

### Arrays.fill

```java
// Sab elements ko same value do
int[] arr = new int[5];
Arrays.fill(arr, 7);
// [7, 7, 7, 7, 7]

// Range fill
int[] arr2 = new int[10];
Arrays.fill(arr2, 2, 6, 99);  // Index 2 se 6 tak (exclusive)
// [0, 0, 99, 99, 99, 99, 0, 0, 0, 0]
```

### Arrays.setAll (Java 8+)

```java
// Generator function se initialize karo
int[] squares = new int[10];
Arrays.setAll(squares, i -> i * i);
// [0, 1, 4, 9, 16, 25, 36, 49, 64, 81]

// Random values
int[] random = new int[5];
Arrays.setAll(random, i -> (int)(Math.random() * 100));
```

### Stream Initialization (Java 8+)

```java
// Range of numbers
int[] range = java.util.stream.IntStream.range(1, 11).toArray();
// [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

// Closed range
int[] closedRange = java.util.stream.IntStream.rangeClosed(1, 10).toArray();
// [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]

// Map + toArray
int[] squares = java.util.stream.IntStream.range(1, 6)
    .map(i -> i * i)
    .toArray();
// [1, 4, 9, 16, 25]

// Random array
int[] random = new Random().ints(5, 1, 100).toArray();
// 5 random numbers between 1 and 99
```

---

## Arrays Utility Class

**Matlab:** `java.util.Arrays` — arrays ke liye built-in utility methods.

### sort

```java
int[] numbers = {5, 2, 8, 1, 9, 3};

// Ascending sort
Arrays.sort(numbers);
// [1, 2, 3, 5, 8, 9]

// Range sort (index 1 se 4 tak)
Arrays.sort(numbers, 1, 4);
// [1, 2, 5, 8, 9, 3] (sirf index 1-3 sort hua)

// Descending sort (Objects ke liye — Integer[] chahiye)
Integer[] nums = {5, 2, 8, 1, 9};
Arrays.sort(nums, Collections.reverseOrder());
// [9, 8, 5, 2, 1]

// Custom comparator
String[] names = {"Sachin", "Rahul", "Priya", "Ankit"};
Arrays.sort(names, (a, b) -> b.length() - a.length());  // Length ke basis pe
// ["Sachin", "Rahul", "Ankit", "Priya"]
```

### binarySearch

```java
// ⚠️ Array sorted honi chahiye!
int[] sorted = {2, 5, 8, 12, 16, 23};

int index = Arrays.binarySearch(sorted, 12);  // 3 (found at index 3)
int notFound = Arrays.binarySearch(sorted, 7);  // -4 (not found)

// Return value:
// >= 0: Element found at this index
// < 0: Element not found → -(insertion point) - 1

// Custom comparator (Objects ke liye)
String[] names = {"Apple", "Banana", "Cherry"};
int idx = Arrays.binarySearch(names, "Banana");  // 1
```

### copyOf / copyOfRange

```java
int[] original = {1, 2, 3, 4, 5};

// Copy first N elements
int[] copy3 = Arrays.copyOf(original, 3);
// [1, 2, 3]

// Copy with expansion
int[] expanded = Arrays.copyOf(original, 8);
// [1, 2, 3, 4, 5, 0, 0, 0] (new elements = default value)

// Copy range
int[] range = Arrays.copyOfRange(original, 1, 4);
// [2, 3, 4] (index 1 to 3, exclusive end)
```

### equals / deepEquals

```java
// 1D array comparison
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
int[] c = {1, 2, 4};

System.out.println(Arrays.equals(a, b));  // true
System.out.println(Arrays.equals(a, c));  // false

// 2D array comparison — deepEquals use karo
int[][] m1 = {{1, 2}, {3, 4}};
int[][] m2 = {{1, 2}, {3, 4}};
int[][] m3 = {{1, 2}, {3, 5}};

System.out.println(Arrays.equals(m1, m2));     // false (reference compare)
System.out.println(Arrays.deepEquals(m1, m2)); // true (content compare)
System.out.println(Arrays.deepEquals(m1, m3)); // false
```

### toString / deepToString

```java
// 1D array
int[] nums = {1, 2, 3, 4, 5};
System.out.println(Arrays.toString(nums));
// [1, 2, 3, 4, 5]

// 2D array — deepToString use karo
int[][] matrix = {{1, 2}, {3, 4}};
System.out.println(Arrays.toString(matrix));     // [[I@hashcode (useless)]
System.out.println(Arrays.deepToString(matrix)); // [[1, 2], [3, 4]]
```

### asList

```java
// Array → List (fixed-size)
String[] arr = {"Apple", "Banana", "Cherry"};
List<String> list = Arrays.asList(arr);

System.out.println(list.size());  // 3
System.out.println(list.get(1));  // "Banana"

// ⚠️ Fixed-size — add/remove nahi kar sakte
list.add("Date");  // ❌ UnsupportedOperationException!

// But modify kar sakte ho
list.set(1, "Blueberry");  // ✅ Works
System.out.println(arr[1]);  // "Blueberry" (original array bhi change hogi!)

// Mutable list chahiye toh
List<String> mutableList = new ArrayList<>(Arrays.asList(arr));
mutableList.add("Date");  // ✅ Works
```

### parallelSort (Java 8+)

```java
// Large arrays ke liye — multi-threaded sort
int[] largeArray = new int[1_000_000];
// ... fill with data
Arrays.parallelSort(largeArray);  // Faster than sort for large arrays
```

---

## System.arraycopy

**Matlab:** Native method for fast array copying — `Arrays.copyOf` se zyada control deta hai.

### Syntax

```java
System.arraycopy(
    src,        // Source array
    srcPos,     // Source start position
    dest,       // Destination array
    destPos,    // Destination start position
    length      // Number of elements to copy
);
```

### Examples

```java
// Basic copy
int[] src = {1, 2, 3, 4, 5};
int[] dest = new int[5];

System.arraycopy(src, 0, dest, 0, src.length);
// dest = [1, 2, 3, 4, 5]

// Partial copy
int[] src2 = {10, 20, 30, 40, 50};
int[] dest2 = new int[3];

System.arraycopy(src2, 1, dest2, 0, 3);
// dest2 = [20, 30, 40] (src2[1] se 3 elements copy)

// Overlapping copy (same array)
int[] nums = {1, 2, 3, 4, 5};
System.arraycopy(nums, 0, nums, 1, 3);
// nums = [1, 1, 2, 3, 5] (elements shift right)

// Array expand
int[] oldArr = {1, 2, 3};
int[] newArr = new int[5];
System.arraycopy(oldArr, 0, newArr, 0, oldArr.length);
// newArr = [1, 2, 3, 0, 0]
```

### arraycopy vs Arrays.copyOf

```java
int[] original = {1, 2, 3, 4, 5};

// System.arraycopy — zyada control
int[] dest1 = new int[5];
System.arraycopy(original, 1, dest1, 0, 3);
// dest1 = [2, 3, 4]

// Arrays.copyOf — simpler
int[] dest2 = Arrays.copyOf(original, 3);
// dest2 = [1, 2, 3]

// Recommendation: Arrays.copyOf use karo (simple API)
// System.arraycopy jab specific positions se copy karna ho
```

---

## Array vs ArrayList Trade-offs

### Comparison

| Feature | Array | ArrayList |
|---------|-------|-----------|
| **Size** | Fixed | Dynamic (grows automatically) |
| **Type** | Primitives + Objects | Sirf Objects (no primitives) |
| **Performance** | Faster (less overhead) | Slightly slower |
| **Memory** | Less overhead | More overhead (object wrapper) |
| **Methods** | None (basic) | Rich API (add, remove, contains, etc.) |
| **Generics** | ❌ No | ✅ Yes |
| **Iteration** | for, for-each | for, for-each, Iterator, Stream |

### When to Use Array

```java
// ✅ Fixed size pata hai
int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

// ✅ Performance critical
double[] largeDataset = new double[10_000_000];

// ✅ Primitives store karne hain
int[] scores = new int[1000];  // int[] = 4KB, ArrayList<Integer> = 24KB+

// ✅ Return from method (lightweight)
public int[] getCoordinates() {
    return new int[]{x, y, z};
}
```

### When to Use ArrayList

```java
// ✅ Size unknown / changes
List<String> names = new ArrayList<>();
names.add("Sachin");
names.add("Rahul");
names.remove("Sachin");

// ✅ Collections API ke saath kaam karna
List<User> users = userService.findAll();
users.sort(Comparator.comparing(User::getName));

// ✅ Rich API chahiye
List<Integer> numbers = new ArrayList<>();
numbers.contains(5);
numbers.indexOf(10);
numbers.subList(2, 5);

// ✅ Generics chahiye
List<String> stringList = new ArrayList<>();  // Type-safe
```

### Conversion

```java
// Array → ArrayList
String[] arr = {"A", "B", "C"};
List<String> list = new ArrayList<>(Arrays.asList(arr));

// ArrayList → Array
List<String> list2 = List.of("X", "Y", "Z");
String[] arr2 = list2.toArray(new String[0]);

// Primitive Array → ArrayList (manual conversion)
int[] intArr = {1, 2, 3};
List<Integer> intList = Arrays.stream(intArr)
    .boxed()
    .collect(Collectors.toList());
```

### Performance Comparison

```java
// Access time — both O(1)
arr[5];           // Fast
list.get(5);      // Fast (similar)

// Add element — Array nahi kar sakta
// arr.add(10);   // ❌ Not possible
list.add(10);     // ✅ Amortized O(1)

// Resize — Array manually karna padta hai
int[] newArr = Arrays.copyOf(arr, arr.length * 2);
// ArrayList automatically resize karta hai

// Memory
int[] arr = new int[1000];           // 4KB
ArrayList<Integer> list = ...;       // 24KB+ (object overhead)
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **1D Array** | Fixed-size, same type elements — `int[] arr = new int[5]` |
| **2D Array** | Array of arrays — `int[][] matrix = new int[3][4]` |
| **Jagged Array** | Har row ki alag length ho sakti hai |
| **Initialization** | `{1, 2, 3}`, `Arrays.fill()`, `Arrays.setAll()`, Streams |
| **Arrays.sort** | Ascending sort — primitives + objects |
| **Arrays.binarySearch** | Sorted array mein search — O(log n) |
| **Arrays.copyOf** | Array copy/expand — `Arrays.copyOf(arr, newSize)` |
| **Arrays.equals** | 1D array content compare — 2D ke liye `deepEquals` |
| **Arrays.asList** | Array → List (fixed-size, backed by array) |
| **System.arraycopy** | Low-level fast copy — specific positions se |
| **Array vs ArrayList** | Array = fixed, fast, primitives | ArrayList = dynamic, rich API, objects only |
