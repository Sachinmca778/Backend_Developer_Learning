# Java Language Basics - Top 10 Interview Questions & Answers

> Covering: Variables & Data Types, Operators, Control Flow, String/StringBuilder, Arrays, Primitives vs Wrappers, Autoboxing, and Common Pitfalls

---

## Q1: Primitive types aur Wrapper classes mein kya difference hai? Kab wrapper use karte hain?

**Answer:**

Java mein **8 primitive types** hain — yeh stack pe directly store hote hain, object nahi hain.

| Primitive | Size | Wrapper Class | Default Value |
|-----------|------|---------------|---------------|
| `byte` | 1 byte | `Byte` | 0 |
| `short` | 2 bytes | `Short` | 0 |
| `int` | 4 bytes | `Integer` | 0 |
| `long` | 8 bytes | `Long` | 0L |
| `float` | 4 bytes | `Float` | 0.0f |
| `double` | 8 bytes | `Double` | 0.0d |
| `boolean` | ~1 bit | `Boolean` | false |
| `char` | 2 bytes | `Character` | '\u0000' |

### Wrapper Classes Kyun Chahiye?

```java
// 1. Collections sirf objects store karti hain
List<Integer> numbers = new ArrayList<>();  // ✅
List<int> numbers = new ArrayList<>();      // ❌ Compile error

// 2. null value store kar sakte ho
Integer nullableAge = null;  // ✅
int age = null;              // ❌ Compile error

// 3. Utility methods available
int parsed = Integer.parseInt("123");
String binary = Integer.toBinaryString(10);
int max = Integer.MAX_VALUE;
```

### Primitive vs Wrapper — Performance

```java
// ❌ Slow — har iteration mein autoboxing/unboxing
Long sum = 0L;
for (long i = 0; i < 1_000_000; i++) {
    sum += i;  // long → Long → add → Long → long
}

// ✅ Fast — primitive directly use karo
long sum = 0L;
for (long i = 0; i < 1_000_000; i++) {
    sum += i;
}
```

**Important:**

- **Performance-critical code** mein primitives prefer karo
- **Collections, nullability, utility methods** ke liye wrappers use karo
- `int` aur `double` sabse commonly used types hain

**Short Interview Answer:**

Primitives = value types (stack), no object overhead. Wrappers = object versions for Collections, null, and utility methods. Performance ke liye primitive, flexibility ke liye wrapper.

---

## Q2: Autoboxing aur Unboxing kya hai? Common pitfalls kya hain?

**Answer:**

**Autoboxing** = primitive → wrapper automatically (`int` → `Integer`)
**Unboxing** = wrapper → primitive automatically (`Integer` → `int`)

Java 5 se yeh compiler automatically karta hai.

### How It Works Internally

```java
Integer num = 100;
// Compiler internally: Integer num = Integer.valueOf(100);

int primitive = num;
// Compiler internally: int primitive = num.intValue();
```

### Autoboxing in Collections

```java
List<Integer> list = new ArrayList<>();
list.add(10);  // Autoboxing: int 10 → Integer.valueOf(10)
list.add(20);

int first = list.get(0);  // Unboxing: Integer → int
```

### ⚠️ Common Pitfalls

**1. NullPointerException on Unboxing:**

```java
Integer num = null;
int value = num;  // ❌ NullPointerException!
// null.intValue() call hota hai internally
```

**2. == vs .equals() for Wrappers:**

```java
Integer a = 1000;
Integer b = 1000;
System.out.println(a == b);       // ❌ false (different objects)
System.out.println(a.equals(b));  // ✅ true (value compare)

// Integer Cache: -128 to 127
Integer x = 100;
Integer y = 100;
System.out.println(x == y);  // ✅ true (cached objects — same reference!)
```

**3. Integer Cache Range:**

```
Integer.valueOf() internally cache use karta hai: -128 to 127
Is range ke bahar == mat use karo — hamesha .equals() use karo
```

**4. Mixed Operations:**

```java
Integer a = 10;
Integer b = 20;
int sum = a + b;  // Unboxing → addition → result (int)
// Safe hai jab dono non-null hon
```

**Important:**

- Wrapper objects pe **hamesha `.equals()`** use karo, `==` nahi (cache range ke bahar)
- Unboxing se pehle **null check** karo
- Hot loops mein wrapper avoid karo — performance hit hota hai

**Short Interview Answer:**

Autoboxing/unboxing automatic hai Java 5+. Pitfalls: NPE on null unboxing, `==` vs `.equals()` (Integer cache -128 to 127), aur loop mein performance overhead.

---

## Q3: Java operators — arithmetic, logical, bitwise, aur precedence explain karo.

**Answer:**

### Arithmetic Operators

| Operator | Example | Result | Note |
|----------|---------|--------|------|
| `+` | `5 + 3` | `8` | |
| `-` | `10 - 4` | `6` | |
| `*` | `6 * 7` | `42` | |
| `/` | `15 / 4` | `3` | Integer division — decimal lost! |
| `%` | `15 % 4` | `3` | Remainder |

```java
int a = 15, b = 4;
System.out.println(a / b);              // 3 (not 2.5!)
System.out.println((double) a / b);     // 3.75 (cast pehle karo)

// Modulus use cases
int num = 7;
boolean isOdd = (num % 2 != 0);         // true
int wrappedIndex = 7 % 5;             // 2 (circular index)
```

### Logical Operators (Short-Circuit)

| Operator | Behavior |
|----------|----------|
| `&&` | Dono true hona chahiye — pehla false → dusra evaluate nahi |
| `\|\|` | Koi ek true → kaafi — pehla true → dusra evaluate nahi |
| `!` | Invert |

```java
// ✅ Safe null check — short-circuit
if (str != null && str.length() > 0) {
    // str null hua toh str.length() call NAHI hoga
}

// ❌ Unsafe — & use karo toh NPE
if (str != null & str.length() > 0) {  // NPE if str is null!
}
```

### Bitwise & Shift (Interview Favorite)

```java
int num = 5;   // 0000 0101
num << 2;      // 0001 0100 = 20 (multiply by 4)
num >> 1;      // 0000 0010 = 2  (divide by 2)
num >>> 1;     // Unsigned right shift

// Permission flags example
int READ = 1, WRITE = 2, EXECUTE = 4;
int permissions = READ | WRITE;  // 0011
if ((permissions & READ) != 0) {
    System.out.println("Read allowed");
}
```

### Operator Precedence (High → Low)

```
++ -- ! ~  →  * / %  →  + -  →  << >> >>>  →  < > <= >=  →  == !=  →  &  →  ^  →  |  →  &&  →  ||  →  ? :
```

```java
int result = 5 + 3 * 2;    // 11 (not 16)
int result2 = (5 + 3) * 2; // 16
```

**Important:**

- Integer division mein decimal part **truncate** hota hai — `(double) a / b` use karo
- Null checks mein **`&&`** use karo, `&` nahi
- Complex expressions mein **parentheses** lagao clarity ke liye

**Short Interview Answer:**

Arithmetic: watch integer division. Logical: `&&`/`||` short-circuit — null-safe checks. Bitwise: flags aur fast multiply/divide by 2. Precedence: `* / %` before `+ -`.

---

## Q4: if/else aur switch — classic switch vs switch expression (Java 14+) mein kya difference hai?

**Answer:**

### if/else — Basic Decision Making

```java
int score = 75;

if (score >= 90) {
    System.out.println("Grade: A");
} else if (score >= 80) {
    System.out.println("Grade: B");
} else if (score >= 70) {
    System.out.println("Grade: C");
} else {
    System.out.println("Grade: F");
}
// Output: Grade: C
```

### Classic switch — Fall-Through Behavior

```java
int day = 3;

switch (day) {
    case 1: System.out.println("Monday"); break;
    case 2: System.out.println("Tuesday"); break;
    case 3: System.out.println("Wednesday"); break;
    default: System.out.println("Invalid");
}
// Output: Wednesday

// ⚠️ break miss karo toh fall-through!
switch (num) {
    case 1: System.out.println("One");
    // No break — falls through!
    case 2: System.out.println("Two"); break;
}
// Output: One, Two
```

### switch Expression (Java 14+) — Value Return

```java
int day = 3;

String dayName = switch (day) {
    case 1 -> "Monday";
    case 2 -> "Tuesday";
    case 3 -> "Wednesday";
    case 4, 5 -> "Weekday";
    default -> "Invalid";
};
// No fall-through with arrow syntax!
```

### Comparison Table

| Feature | Classic switch | switch Expression (Java 14+) |
|---------|---------------|-------------------------------|
| Returns value | ❌ No | ✅ Yes |
| Fall-through | ✅ Yes (without break) | ❌ No (arrow syntax) |
| Multiple cases | `case 1: case 2:` | `case 1, 2 ->` |
| Block syntax | N/A | `yield` keyword se value return |
| Supported types | int, byte, short, char, enum, String | Same |

### Supported Types in switch

```java
// ✅ int, byte, short, char, enum, String (Java 7+)
switch (fruit) {
    case "apple": break;
    case "banana": break;
}

// ❌ long, float, double, boolean — NOT supported
```

**Important:**

- Classic switch mein **`break` lagana mat bhoolo** — warna fall-through hoga
- Java 14+ mein switch expression prefer karo — cleaner, no fall-through bugs
- `long`, `float`, `double`, `boolean` switch mein use nahi ho sakte

**Short Interview Answer:**

Classic switch = statement, fall-through without break. Switch expression (Java 14+) = returns value, arrow syntax, no fall-through. String/enum supported; long/float/double/boolean not.

---

## Q5: Loops in Java — for, while, do-while, enhanced for-each. Kab kaunsa use karte hain?

**Answer:**

### Loop Comparison

| Loop | Type | Min Executions | Use When |
|------|------|----------------|----------|
| **for** | Entry-controlled | 0 | Fixed/known iterations |
| **while** | Entry-controlled | 0 | Unknown iterations, condition pehle check |
| **do-while** | Exit-controlled | 1 | Kam se kam ek baar run chahiye |
| **for-each** | Entry-controlled | 0 | Arrays/Collections iterate (read-only mostly) |

### for Loop

```java
for (int i = 0; i < 10; i++) {
    System.out.println(i);  // 0 to 9
}
// Structure: init → condition → body → update → repeat
```

### while Loop

```java
int num = 12345;
int digitCount = 0;

while (num > 0) {
    num /= 10;
    digitCount++;
}
System.out.println("Digits: " + digitCount);  // 5
```

### do-while Loop

```java
int count = 10;
do {
    System.out.println("Count: " + count);  // Ek baar toh run hoga
    count++;
} while (count < 5);
// Output: Count: 10 (condition false, phir bhi ek baar execute)
```

### Enhanced for-each

```java
int[] numbers = {10, 20, 30, 40, 50};

for (int num : numbers) {
    System.out.println(num);
}

// Collections
List<String> names = List.of("Sachin", "Rahul", "Priya");
for (String name : names) {
    System.out.println(name);
}
```

### for-each Limitations

```java
int[] numbers = {1, 2, 3, 4, 5};

// ❌ Index access nahi milta
// ❌ Modify nahi kar sakte
for (int num : numbers) {
    num = num * 2;  // Original array change NAHI hoga
}

// ✅ Modify ke liye indexed for
for (int i = 0; i < numbers.length; i++) {
    numbers[i] = numbers[i] * 2;  // ✅ Works
}

// ❌ Collection se remove — ConcurrentModificationException
List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
for (Integer num : list) {
    if (num == 2) list.remove(num);  // ❌ CME!
}
// ✅ Iterator.use remove() instead
```

### break, continue, labeled

```java
// break — loop terminate
for (int i = 0; i < 10; i++) {
    if (i == 5) break;
    System.out.println(i);  // 0,1,2,3,4
}

// continue — skip current iteration
for (int i = 0; i < 10; i++) {
    if (i % 2 == 0) continue;
    System.out.println(i);  // 1,3,5,7,9
}

// labeled break — nested loops
outer:
for (int i = 0; i < 5; i++) {
    for (int j = 0; j < 5; j++) {
        if (i * j > 6) break outer;
    }
}
```

**Important:**

- **Modify/remove** during iteration → indexed for ya Iterator use karo
- **Menu-driven programs** → do-while ideal hai
- **Unknown iterations** → while loop

**Short Interview Answer:**

for = known count. while = condition-based, 0+ times. do-while = at least once. for-each = read-only iteration on arrays/collections — no index, no safe remove.

---

## Q6: String immutability kya hai? Strings immutable kyun hain?

**Answer:**

**Immutability** matlab ek baar String create ho gayi — usko modify nahi kar sakte. Har "modification" ek **naya String object** banata hai.

### Immutability Demo

```java
String name = "Sachin";
name.toUpperCase();           // Naya "SACHIN" bana — name change nahi hua
System.out.println(name);     // "Sachin" (original same)

name = name.toUpperCase();    // Reassign karo tab change dikhega
System.out.println(name);     // "SACHIN"
```

### Internally Kya Hota Hai

```java
String s1 = "Hello";
String s2 = s1.concat(" World");  // Naya object

System.out.println(s1);  // "Hello" (unchanged — memory location A)
System.out.println(s2);  // "Hello World" (naya object — memory location B)
```

### Strings Immutable Kyun Hain?

| Reason | Explanation |
|--------|-------------|
| **Security** | Passwords, connection strings — koi modify nahi kar sakta |
| **Thread Safety** | Multiple threads safely share kar sakte hain — sync ki zarurat nahi |
| **String Pool** | Same literal ko memory mein ek baar store — caching possible |
| **Hash Code Caching** | HashMap keys ke liye hash ek baar calculate, reuse |

### String Class Design

```java
public final class String {  // final — extend nahi ho sakta
    private final char[] value;  // internal array bhi final (older Java)
    // No setter methods — sirf methods naya String return karte hain
}
```

### Performance Impact — Concatenation in Loop

```java
// ❌ Bad — har + pe naya String object
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // 1000 objects create!
}

// ✅ Good — StringBuilder use karo
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);
}
String result = sb.toString();
```

**Important:**

- String methods (`toUpperCase()`, `replace()`, `trim()`) **original change nahi karte** — naya object return karte hain
- Loop mein concatenation ke liye **StringBuilder** mandatory hai
- Immutability se **String Pool** aur **HashMap key safety** milti hai

**Short Interview Answer:**

String immutable hai — modify nahi hoti, har change naya object banata hai. Reasons: security, thread safety, string pool caching, hash code stability. Loops mein `+` avoid karo.

---

## Q7: String vs StringBuilder vs StringBuffer — kab kaunsa use karte hain?

**Answer:**

| Feature | String | StringBuilder | StringBuffer |
|---------|--------|---------------|--------------|
| **Mutable** | ❌ No | ✅ Yes | ✅ Yes |
| **Thread-Safe** | ✅ Yes (immutable) | ❌ No | ✅ Yes (synchronized) |
| **Performance** | Slow (new objects) | Fast | Medium (sync overhead) |
| **Introduced** | Java 1.0 | Java 5 | Java 1.0 |
| **Use When** | Fixed content | Single-threaded modifications | Multi-threaded modifications |

### String — Fixed Content

```java
String greeting = "Hello, World!";
String email = user.getEmail();  // Content change nahi hoga frequently
```

### StringBuilder — Single-Threaded, Fast

```java
StringBuilder sb = new StringBuilder();
sb.append("SELECT * FROM users");
sb.append(" WHERE active = true");
sb.append(" ORDER BY name");
String query = sb.toString();

// Method chaining
String result = new StringBuilder()
    .append("Hello")
    .append(" ")
    .append("World")
    .toString();

// Other operations
sb.insert(5, ",");
sb.delete(5, 7);
sb.reverse();
```

### StringBuffer — Thread-Safe

```java
StringBuffer sb = new StringBuffer();
sb.append("Hello").append(" World");
// Same API as StringBuilder, but synchronized — thoda slow
// Multiple threads safely append kar sakte hain
```

### Performance Comparison

```java
// String concatenation in loop — O(n²) time, O(n²) space
String s = "";
for (int i = 0; i < 10000; i++) s += i;

// StringBuilder — O(n) time, O(n) space
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 10000; i++) sb.append(i);
```

**Important:**

- **Default choice for building strings:** StringBuilder (single-threaded)
- **Multi-threaded string building:** StringBuffer ya external synchronization
- Java 9+ compiler sometimes **`+` ko StringBuilder mein optimize** karta hai — lekin loop mein manually StringBuilder likho

**Short Interview Answer:**

String = immutable, fixed content. StringBuilder = mutable, fast, not thread-safe — default for string building. StringBuffer = mutable, synchronized, thread-safe — legacy multi-thread use.

---

## Q8: String Pool (intern pool) kya hai? `==` vs `.equals()` strings ke liye?

**Answer:**

**String Pool** JVM heap ke andar ek special area hai jahan **string literals** store hote hain — duplicate strings avoid karne ke liye.

### Pool Behavior

```java
String s1 = "Hello";           // Pool mein create/reference
String s2 = "Hello";           // Pool se same reference
String s3 = new String("Hello"); // Heap pe naya object

System.out.println(s1 == s2);       // true (same pool object)
System.out.println(s1 == s3);       // false (different objects)
System.out.println(s1.equals(s3));  // true (same content)
```

### Memory Layout

```
String Pool (inside Heap):
┌─────────────────┐
│ "Hello" @100    │ ← s1, s2 point here
└─────────────────┘

Regular Heap:
┌─────────────────┐
│ "Hello" @200    │ ← s3 points here (new String())
└─────────────────┘
```

### intern() Method

```java
String s1 = new String("Hello");  // Heap
String s2 = s1.intern();          // Pool mein daalo/reference lo
String s3 = "Hello";              // Pool

System.out.println(s2 == s3);  // true (dono pool mein)
System.out.println(s1 == s2);  // false (s1 ab bhi heap pe)
```

### == vs .equals() Rule

| Type | Compare With |
|------|-------------|
| Primitives | `==` (value compare) |
| Objects (including String) | `.equals()` (content compare) |
| String literals (same content) | `==` might work (pool) but **`.equals()` prefer karo** |

```java
String a = new String("Hello");
String b = new String("Hello");
System.out.println(a == b);       // false
System.out.println(a.equals(b));  // true — ALWAYS use this
```

### When to Use intern()

```java
// ✅ Bahut sare duplicate strings — memory save
for (int i = 0; i < 100_000; i++) {
    words.add("common".intern());
}

// ❌ Unique strings pe intern() — overhead badhega (pool lookup)
```

**Important:**

- **Hamesha `.equals()`** use karo string content compare ke liye
- `==` sirf reference check karta hai — pool ke bahar false positives/negatives
- `new String("x")` **pool bypass** karta hai — unnecessary mat use karo

**Short Interview Answer:**

String literals string pool mein store hote hain — same literal = same reference. `new String()` heap pe naya object. Content compare = `.equals()`. Reference compare = `==` (pool trick questions mein fail ho sakta hai).

---

## Q9: Arrays in Java — 1D, 2D, jagged arrays, aur common pitfalls?

**Answer:**

Array = **fixed-size**, same-type elements ka collection — contiguous memory.

### 1D Array

```java
int[] nums = new int[5];     // Default: [0,0,0,0,0]
int[] values = {10, 20, 30}; // Literal initialization

nums[0] = 10;
System.out.println(nums.length);  // 5 — property, NOT method()

// ⚠️ ArrayIndexOutOfBoundsException
nums[5] = 100;  // ❌ Valid indices: 0 to 4
```

### Default Values

| Type | Default |
|------|---------|
| `int`, `short`, `byte`, `long` | 0 |
| `float`, `double` | 0.0 |
| `boolean` | false |
| Object references (`String[]`) | null |

### 2D Array (Matrix)

```java
int[][] matrix = {
    {1, 2, 3},
    {5, 6, 7},
    {9, 10, 11}
};

int rows = matrix.length;       // 3
int cols = matrix[0].length;    // 3

// Iterate
for (int i = 0; i < matrix.length; i++) {
    for (int j = 0; j < matrix[i].length; j++) {
        System.out.print(matrix[i][j] + " ");
    }
}
```

### Jagged Array (Unequal Row Lengths)

```java
int[][] jagged = new int[3][];
jagged[0] = new int[]{1, 2};
jagged[1] = new int[]{3, 4, 5, 6};
jagged[2] = new int[]{7, 8, 9};
// Har row ki alag length ho sakti hai
```

### Array vs ArrayList

| Feature | Array | ArrayList |
|---------|-------|-----------|
| Size | Fixed | Dynamic |
| Primitives | ✅ Direct | ❌ Wrappers only |
| Performance | Faster | Slightly slower |
| Methods | None | Rich API |

**Important:**

- `length` property hai, `length()` method nahi
- 2D array print ke liye `Arrays.deepToString()` use karo
- Object arrays mein **null check** zaruri hai before method calls

**Short Interview Answer:**

Arrays = fixed size, primitives/objects, `length` property. 2D = array of arrays. Jagged = rows different lengths. Pitfall: index out of bounds, null elements in object arrays.

---

## Q10: Arrays utility class — sort, binarySearch, copyOf, asList. Common pitfalls?

**Answer:**

`java.util.Arrays` — arrays ke liye built-in utility methods.

### sort & binarySearch

```java
int[] numbers = {5, 2, 8, 1, 9, 3};

Arrays.sort(numbers);  // [1, 2, 3, 5, 8, 9]

// ⚠️ binarySearch — array SORTED honi chahiye!
int index = Arrays.binarySearch(numbers, 8);   // 4 (found)
int notFound = Arrays.binarySearch(numbers, 7); // -5 (not found)
// Negative return = -(insertion point) - 1

// Custom sort
String[] names = {"Sachin", "Rahul", "Ankit", "Priya"};
Arrays.sort(names, Comparator.comparingInt(String::length));
```

### copyOf & copyOfRange

```java
int[] original = {1, 2, 3, 4, 5};

int[] copy = Arrays.copyOf(original, 3);      // [1, 2, 3]
int[] expanded = Arrays.copyOf(original, 8);  // [1,2,3,4,5,0,0,0]
int[] range = Arrays.copyOfRange(original, 1, 4); // [2, 3, 4]
```

### equals vs deepEquals

```java
int[] a = {1, 2, 3};
int[] b = {1, 2, 3};
System.out.println(Arrays.equals(a, b));  // true

int[][] m1 = {{1, 2}, {3, 4}};
int[][] m2 = {{1, 2}, {3, 4}};
System.out.println(Arrays.equals(m1, m2));      // false (reference compare!)
System.out.println(Arrays.deepEquals(m1, m2));  // true (content compare)
```

### asList — ⚠️ Major Pitfall

```java
String[] arr = {"Apple", "Banana", "Cherry"};
List<String> list = Arrays.asList(arr);

list.set(1, "Blueberry");  // ✅ Works
System.out.println(arr[1]);  // "Blueberry" — original array bhi change!

list.add("Date");  // ❌ UnsupportedOperationException — fixed-size list!

// ✅ Mutable list chahiye toh:
List<String> mutable = new ArrayList<>(Arrays.asList(arr));
mutable.add("Date");  // ✅ Works
```

### Other Useful Methods

```java
Arrays.fill(arr, 0);                    // Sab elements 0
Arrays.setAll(arr, i -> i * i);         // Java 8+ generator
System.out.println(Arrays.toString(arr));      // 1D print
System.out.println(Arrays.deepToString(matrix)); // 2D print
Arrays.parallelSort(largeArray);         // Large arrays — multi-threaded sort
```

**Important:**

- `binarySearch` pehle **`sort` karo** — warna wrong result
- 2D arrays ke liye **`deepEquals`/`deepToString`** — `equals`/`toString` fail karenge
- `Arrays.asList()` = **fixed-size**, backed by original array — add/remove nahi

**Short Interview Answer:**

Arrays utility: sort (O(n log n)), binarySearch (sorted array required), copyOf, deepEquals for 2D. Pitfall: asList is fixed-size and modifies backing array; binarySearch on unsorted array gives wrong results.

---
