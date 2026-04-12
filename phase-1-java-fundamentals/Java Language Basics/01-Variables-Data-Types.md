# Variables & Data Types

## Status: Not Started

---

## Table of Contents

1. [Primitive Data Types](#primitive-data-types)
2. [Wrapper Classes](#wrapper-classes)
3. [Autoboxing & Unboxing](#autoboxing--unboxing)
4. [Type Casting](#type-casting)
5. [var Keyword (Java 10+)](#var-keyword-java-10)
6. [Variable Declaration & Scope](#variable-declaration--scope)

---

## Primitive Data Types

**Matlab:** Java ke built-in data types jo language ke core hain — inka koi object nahi hota.

### 8 Primitive Types

| Type | Size | Default | Range | Example | Use Case |
|------|------|---------|-------|---------|----------|
| **byte** | 1 byte | 0 | -128 to 127 | `byte b = 100;` | Memory-sensitive apps, binary data |
| **short** | 2 bytes | 0 | -32,768 to 32,767 | `short s = 30000;` | Legacy systems, large arrays |
| **int** | 4 bytes | 0 | -2³¹ to 2³¹-1 | `int i = 100000;` | **Most common** — counters, IDs |
| **long** | 8 bytes | 0L | -2⁶³ to 2⁶³-1 | `long l = 100000L;` | Timestamps, large numbers |
| **float** | 4 bytes | 0.0f | ~7 decimal digits | `float f = 3.14f;` | Graphics, memory-sensitive |
| **double** | 8 bytes | 0.0d | ~15 decimal digits | `double d = 3.14159;` | **Most common** — scientific calc |
| **boolean** | 1 bit | false | true / false | `boolean b = true;` | Flags, conditions |
| **char** | 2 bytes | '\u0000' | Unicode characters | `char c = 'A';` | Single characters |

### Declaration Examples

```java
// Integer types
byte age = 25;
short population = 32000;
int userId = 1001;
long worldPopulation = 8_000_000_000L;  // Underscores for readability

// Floating point types
float price = 99.99f;      // 'f' suffix zaruri hai
double pi = 3.14159265359;

// boolean
boolean isActive = true;
boolean isLoggedIn = false;

// char
char grade = 'A';
char unicode = '\u0041';  // 'A' in Unicode
char emoji = '\uD83D\uDE00';  // 😀
```

### Memory Comparison

```
byte  → 1 byte   (8 bits)   → 256 possible values
short → 2 bytes  (16 bits)  → 65,536 possible values
int   → 4 bytes  (32 bits)  → 4 billion possible values
long  → 8 bytes  (64 bits)  → 18 quintillion possible values
float → 4 bytes  (32 bits)  → IEEE 754 floating point
double→ 8 bytes  (64 bits)  → IEEE 754 floating point
char  → 2 bytes  (16 bits)  → Unicode support
boolean → ~1 bit (JVM dependent)
```

### When to Use What

```java
// ✅ Use int for most integer values
int count = 100;
int age = 25;

// ✅ Use long for very large numbers
long timestamp = System.currentTimeMillis();
long bigNumber = 9_000_000_000L;

// ✅ Use double for decimal values
double temperature = 36.6;
double salary = 50000.50;

// ✅ Use float only when memory is critical
float gpsCoordinate = 28.6139f;

// ✅ Use boolean for flags
boolean enabled = true;

// ✅ Use char for single characters
char initial = 'S';
```

---

## Wrapper Classes

**Matlab:** Har primitive type ka corresponding object class — primitives ko objects ki tarah use karne ke liye.

### Primitive ↔ Wrapper Mapping

| Primitive | Wrapper Class |
|-----------|---------------|
| `byte` | `Byte` |
| `short` | `Short` |
| `int` | `Integer` |
| `long` | `Long` |
| `float` | `Float` |
| `double` | `Double` |
| `boolean` | `Boolean` |
| `char` | `Character` |

### Usage

```java
// Primitive → Wrapper
int primitiveInt = 42;
Integer wrapperInt = Integer.valueOf(42);  // Explicit
Integer wrapperInt2 = 42;                  // Autoboxing (see below)

// Wrapper ke methods
Integer num = 100;
String binary = Integer.toBinaryString(num);    // "1100100"
String hex = Integer.toHexString(num);          // "64"
int parsed = Integer.parseInt("123");           // 123
int max = Integer.MAX_VALUE;                    // 2147483647
int min = Integer.MIN_VALUE;                    // -2147483648

// Double wrapper
Double pi = 3.14;
double parsed = Double.parseDouble("3.14");     // 3.14
double nan = Double.NaN;                        // Not a Number
double inf = Double.POSITIVE_INFINITY;          // Infinity

// Boolean wrapper
Boolean flag = true;
boolean parsed = Boolean.parseBoolean("true");  // true
boolean parsed2 = Boolean.parseBoolean("yes");  // false (only "true" works)
```

### Why Wrapper Classes?

```java
// 1. Collections mein sirf objects store ho sakte hain
List<Integer> numbers = new ArrayList<>();  // ✅ Works
List<int> numbers = new ArrayList<>();      // ❌ Error!

// 2. null value store kar sakte ho
Integer nullableInt = null;  // ✅ Works
int primitiveInt = null;     // ❌ Error!

// 3. Utility methods available
Integer.parseInt("123");
Integer.toBinaryString(10);
Integer.compare(5, 10);
```

---

## Autoboxing & Unboxing

**Matlab:** Automatic conversion between primitives aur wrapper classes — Java 5 mein introduced.

### Autoboxing (Primitive → Wrapper)

```java
// Without autoboxing (old style)
Integer num1 = Integer.valueOf(42);

// With autoboxing (Java 5+)
Integer num2 = 42;  // Automatically → Integer.valueOf(42)

// Collections mein
List<Integer> list = new ArrayList<>();
list.add(10);  // Autoboxing: int → Integer
list.add(20);
list.add(30);
```

### Unboxing (Wrapper → Primitive)

```java
// Without unboxing (old style)
Integer wrapper = 42;
int primitive = wrapper.intValue();

// With unboxing (Java 5+)
Integer wrapper = 42;
int primitive = wrapper;  // Automatically → wrapper.intValue()

// Operations mein
Integer a = 10;
Integer b = 20;
int sum = a + b;  // Unboxing → addition → result
```

### How It Works Internally

```java
// Autoboxing
Integer num = 100;
// Compiler internally: Integer num = Integer.valueOf(100);

// Unboxing
int primitive = num;
// Compiler internally: int primitive = num.intValue();
```

### ⚠️ Common Pitfalls

```java
// 1. NullPointerException
Integer num = null;
int primitive = num;  // ❌ NullPointerException!

// 2. Object comparison (== checks reference, not value)
Integer a = 1000;
Integer b = 1000;
System.out.println(a == b);  // ❌ false (different objects)
System.out.println(a.equals(b));  // ✅ true (correct way)

// 3. Integer Cache (-128 to 127)
Integer x = 100;
Integer y = 100;
System.out.println(x == y);  // ✅ true (cached!)

Integer p = 200;
Integer q = 200;
System.out.println(p == q);  // ❌ false (not cached)

// Cache range: -128 to 127 (default)
// Iske bahar == use mat karo — .equals() use karo
```

### Performance Impact

```java
// ❌ Slow — har iteration mein autoboxing/unboxing
Long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;  // long → Long → add → Long → long
}

// ✅ Fast — primitive use karo
long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;  // Direct primitive addition
}
```

---

## Type Casting

**Matlab:** Ek data type ko dusre data type mein convert karna.

### Implicit Casting (Widening)

**Matlab:** Chhote type se bade type mein — automatically, no data loss.

```java
// byte → short → int → long → float → double

byte b = 10;
short s = b;      // ✅ byte → short
int i = s;        // ✅ short → int
long l = i;       // ✅ int → long
float f = l;      // ✅ long → float (precision loss possible)
double d = f;     // ✅ float → double

// Direct
int num = 100;
double result = num;  // 100.0
```

### Explicit Casting (Narrowing)

**Matlab:** Bade type se chhote type mein — manually, data loss possible.

```java
// double → float → long → int → short → byte

double d = 9.78;
float f = (float) d;    // ✅ 9.78f
long l = (long) d;      // ✅ 9 (decimal part lost!)
int i = (int) d;        // ✅ 9

int num = 1000;
byte b = (byte) num;    // ⚠️ -24 (overflow! byte range: -128 to 127)

// Explanation: 1000 = 1111101000 (binary)
// byte sirf last 8 bits leta hai: 11101000 = -24 (two's complement)
```

### Char ↔ Int Casting

```java
// char → int (Unicode value)
char c = 'A';
int code = c;  // 65 (ASCII/Unicode value)

// int → char
int code = 66;
char c = (char) code;  // 'B'

// Character arithmetic
char start = 'A';
for (int i = 0; i < 5; i++) {
    System.out.print((char)(start + i) + " ");  // A B C D E
}
```

### Boolean Casting

```java
// ❌ boolean ko kisi aur type mein cast nahi kar sakte
boolean flag = true;
int num = (int) flag;  // ❌ Compilation Error!
```

---

## var Keyword (Java 10+)

**Matlab:** Local variable type inference — compiler automatically type deduce kar leta hai.

### Usage

```java
// Without var
String name = "Sachin";
List<String> list = new ArrayList<>();
Map<String, Integer> map = new HashMap<>();

// With var (Java 10+)
var name = "Sachin";           // Type: String
var list = new ArrayList<String>();  // Type: ArrayList<String>
var map = new HashMap<String, Integer>();  // Type: HashMap<String, Integer>
```

### How It Works

```java
var num = 42;        // Type: int (inferred)
var price = 99.99;   // Type: double (inferred)
var flag = true;     // Type: boolean (inferred)
var text = "Hello";  // Type: String (inferred)
var nums = List.of(1, 2, 3);  // Type: List<Integer>
```

### ⚠️ Limitations

```java
// ✅ Works — local variables only
void method() {
    var name = "Sachin";
    var count = 10;
}

// ❌ Class fields — not allowed
class MyClass {
    var name = "Sachin";  // ❌ Error!
}

// ❌ Method parameters — not allowed
void method(var param) { }  // ❌ Error!

// ❌ Return type — not allowed
var method() { }  // ❌ Error!

// ❌ Null initialization — type infer nahi ho sakta
var name = null;  // ❌ Error!

// ❌ Lambda expressions — type ambiguous
var func = () -> System.out.println("Hi");  // ❌ Error!
```

### When to Use var

```java
// ✅ Good — obvious type
var list = new ArrayList<String>();           // Type clear hai
var map = new HashMap<String, Integer>();    // Type clear hai
var stream = Files.lines(path);              // Type clear hai

// ⚠️ Avoid — type unclear hai
var result = process(data);    // Return type kya hai? Pata nahi!
var value = getValue();        // Ambiguous

// ✅ Better — explicit type
User result = process(data);   // Type clear hai
int value = getValue();        // Type clear hai
```

---

## Variable Declaration & Scope

### Types of Variables

```java
class User {
    // Instance variable (class level)
    String name;
    int age;

    // Static variable (class level, shared)
    static int count = 0;

    // Constructor
    public User(String name, int age) {
        this.name = name;
        this.age = age;
        count++;  // Static variable access
    }

    // Method
    public void display() {
        // Local variable (method level)
        String message = "User: " + name;
        System.out.println(message);
    }

    // Loop variable
    public void printNumbers() {
        for (int i = 0; i < 10; i++) {  // i is loop variable
            System.out.println(i);
        }
        // System.out.println(i);  // ❌ Error! i is out of scope
    }
}
```

### Variable Scope

| Type | Scope | Lifetime |
|------|-------|----------|
| **Local** | Method/block ke andar | Method execution tak |
| **Instance** | Class level (non-static) | Object lifetime tak |
| **Static** | Class level (static) | Application lifetime tak |

### Shadowing

```java
class User {
    String name = "Default";  // Instance variable

    public void setName(String name) {  // Parameter (shadows instance)
        name = name;  // ❌ Parameter ko assign kar raha hai (no effect)
        this.name = name;  // ✅ Instance variable ko assign kar raha hai
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Primitive Types** | 8 types — byte, short, int, long, float, double, boolean, char |
| **int** | Most common integer type |
| **double** | Most common decimal type |
| **Wrapper Classes** | Primitives ko objects ki tarah use karne ke liye |
| **Autoboxing** | Primitive → Wrapper (automatic) |
| **Unboxing** | Wrapper → Primitive (automatic) |
| **Integer Cache** | -128 to 127 cached — == use mat karo, .equals() use karo |
| **Widening Casting** | Small → Large (automatic) |
| **Narrowing Casting** | Large → Small (manual, data loss possible) |
| **var** | Local variable type inference (Java 10+) — type clear hona chahiye |
| **Scope** | Local < Instance < Static |
