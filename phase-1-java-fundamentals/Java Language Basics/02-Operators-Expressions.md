# Operators & Expressions

## Status: Not Started

---

## Table of Contents

1. [Arithmetic Operators](#arithmetic-operators)
2. [Relational Operators](#relational-operators)
3. [Logical Operators](#logical-operators)
4. [Bitwise Operators](#bitwise-operators)
5. [Shift Operators](#shift-operators)
6. [Ternary Operator](#ternary-operator)
7. [instanceof Operator](#instanceof-operator)
8. [Operator Precedence](#operator-precedence)
9. [Assignment Operators](#assignment-operators)

---

## Arithmetic Operators

**Matlab:** Mathematical calculations ke liye — addition, subtraction, multiplication, division, modulus.

### Basic Operators

| Operator | Symbol | Example | Result |
|----------|--------|---------|--------|
| **Addition** | `+` | `5 + 3` | `8` |
| **Subtraction** | `-` | `10 - 4` | `6` |
| **Multiplication** | `*` | `6 * 7` | `42` |
| **Division** | `/` | `15 / 4` | `3` (integer division) |
| **Modulus** | `%` | `15 % 4` | `3` (remainder) |

### Examples

```java
int a = 15;
int b = 4;

System.out.println(a + b);   // 19
System.out.println(a - b);   // 11
System.out.println(a * b);   // 60
System.out.println(a / b);   // 3 (integer division — decimal part lost!)
System.out.println(a % b);   // 3 (remainder: 15 = 4*3 + 3)

// Floating point division
double x = 15.0;
double y = 4.0;
System.out.println(x / y);   // 3.75 (decimal part preserved)

// Type casting for precise division
int num = 15;
int den = 4;
System.out.println((double) num / den);  // 3.75
```

### ⚠️ Integer Division

```java
int a = 5;
int b = 2;

int result = a / b;       // 2 (not 2.5!)
double result2 = a / b;   // 2.0 (still 2 — division happens before assignment)
double result3 = (double) a / b;  // 2.5 (correct — casting before division)
```

### Modulus Use Cases

```java
// Even/Odd check
int num = 7;
if (num % 2 == 0) {
    System.out.println("Even");
} else {
    System.out.println("Odd");  // ✅
}

// Divisible by N
if (num % 5 == 0) {
    System.out.println("Divisible by 5");
}

// Wrap around (circular index)
int[] arr = {10, 20, 30, 40, 50};
int index = 7;
int wrappedIndex = index % arr.length;  // 7 % 5 = 2 → arr[2] = 30

// Extract last digit
int number = 1234;
int lastDigit = number % 10;  // 4

// Extract digits
int lastTwoDigits = number % 100;  // 34
```

### Increment/Decrement

```java
int count = 5;

// Post-increment (value use karo, phir increment karo)
int a = count++;  // a = 5, count = 6

// Pre-increment (pehle increment karo, phir value use karo)
int b = ++count;  // count = 7, b = 7

// Post-decrement
int c = count--;  // c = 7, count = 6

// Pre-decrement
int d = --count;  // count = 5, d = 5
```

**Rule:**
- `var++` → pehle value use, phir increment
- `++var` → pehle increment, phir value use

---

## Relational Operators

**Matlab:** Do values ko compare karna — result hamesha `boolean` (true/false).

| Operator | Symbol | Example | Result |
|----------|--------|---------|--------|
| **Equal to** | `==` | `5 == 5` | `true` |
| **Not equal to** | `!=` | `5 != 3` | `true` |
| **Greater than** | `>` | `7 > 3` | `true` |
| **Less than** | `<` | `3 < 7` | `true` |
| **Greater than or equal** | `>=` | `5 >= 5` | `true` |
| **Less than or equal** | `<=` | `4 <= 5` | `true` |

### Examples

```java
int a = 10;
int b = 20;

System.out.println(a == b);   // false
System.out.println(a != b);   // true
System.out.println(a > b);    // false
System.out.println(a < b);    // true
System.out.println(a >= 10);  // true
System.out.println(b <= 15);  // false
```

### ⚠️ == vs .equals()

```java
// Primitives — == works
int x = 10;
int y = 10;
System.out.println(x == y);  // true

// Objects — == checks reference (memory address)
String s1 = new String("Hello");
String s2 = new String("Hello");
System.out.println(s1 == s2);       // false (different objects)
System.out.println(s1.equals(s2));  // true (same content)

// String literals — interned (same reference)
String s3 = "Hello";
String s4 = "Hello";
System.out.println(s3 == s4);       // true (same interned object)
System.out.println(s3.equals(s4));  // true
```

**Rule:**
- Primitives → `==` use karo
- Objects → `.equals()` use karo

---

## Logical Operators

**Matlab:** Multiple boolean expressions ko combine karna.

| Operator | Symbol | Description | Example |
|----------|--------|-------------|---------|
| **AND** | `&&` | Dono true hone chahiye | `true && true` → `true` |
| **OR** | `\|\|` | Koi ek true ho toh kaafi | `true \|\| false` → `true` |
| **NOT** | `!` | Invert karta hai | `!true` → `false` |

### Examples

```java
int age = 25;
boolean hasId = true;

// AND — dono conditions true honi chahiye
if (age >= 18 && hasId) {
    System.out.println("Entry allowed");
}

// OR — koi ek condition true ho
boolean isWeekend = true;
boolean isHoliday = false;
if (isWeekend || isHoliday) {
    System.out.println("Office closed");
}

// NOT — invert
boolean isLoggedIn = false;
if (!isLoggedIn) {
    System.out.println("Please login");
}
```

### Short-Circuit Behavior

```java
// && — pehla false hua toh dusra evaluate nahi hoga
boolean result1 = (5 > 10) && (someMethod());  // someMethod() call NAHI hoga

// || — pehla true hua toh dusra evaluate nahi hoga
boolean result2 = (5 < 10) || (someMethod());  // someMethod() call NAHI hoga
```

### Bitwise vs Logical AND/OR

```java
// Logical (short-circuit) — sirf boolean pe
boolean a = true;
boolean b = false;
boolean result = a && b;  // false

// Bitwise — integers aur dono pe
// Non-short-circuit version
boolean result2 = a & b;   // false (dono evaluate honge)
boolean result3 = a | b;   // true (dono evaluate honge)

// Practical use — null check
if (str != null && str.length() > 0) {
    // ✅ Safe — str null hua toh str.length() call nahi hoga
}

if (str != null & str.length() > 0) {
    // ❌ Unsafe — str null hua toh bhi str.length() call hoga → NPE
}
```

---

## Bitwise Operators

**Matlab:** Individual bits pe operate karna — integers aur long pe kaam karte hain.

| Operator | Symbol | Description | Example |
|----------|--------|-------------|---------|
| **AND** | `&` | Dono bits 1 hon toh 1 | `5 & 3` → `1` |
| **OR** | `\|` | Koi ek bit 1 ho toh 1 | `5 \| 3` → `7` |
| **XOR** | `^` | Bits different hon toh 1 | `5 ^ 3` → `6` |
| **NOT** | `~` | Bits invert karta hai | `~5` → `-6` |

### Binary Examples

```
5 = 0000 0101
3 = 0000 0011

5 & 3 = 0000 0001 = 1  (AND: dono 1 hon toh 1)
5 | 3 = 0000 0111 = 7  (OR: koi ek 1 ho toh 1)
5 ^ 3 = 0000 0110 = 6  (XOR: different hon toh 1)
~5    = 1111 1010 = -6 (NOT: sab bits flip)
```

### Use Cases

```java
// Odd/Even check (last bit check)
int num = 7;
if ((num & 1) == 0) {
    System.out.println("Even");
} else {
    System.out.println("Odd");  // ✅
}

// Toggle case (ASCII: uppercase ↔ lowercase differ by bit 5)
char ch = 'a';
ch = (char) (ch ^ 32);  // 'A'

// Swap without temp variable
int a = 5, b = 10;
a = a ^ b;  // a = 15
b = a ^ b;  // b = 5
a = a ^ b;  // a = 10

// Flags/Permissions
int READ = 1;    // 0001
int WRITE = 2;   // 0010
int EXECUTE = 4; // 0100

int permissions = READ | WRITE;  // 0011
if ((permissions & READ) != 0) {
    System.out.println("Read access granted");
}

// Check if specific bit is set
if ((permissions & WRITE) != 0) {
    System.out.println("Write access granted");
}
```

---

## Shift Operators

**Matlab:** Bits ko left ya right shift karna — multiplication/division by 2 ke liye fast.

| Operator | Symbol | Description | Example |
|----------|--------|-------------|---------|
| **Left Shift** | `<<` | Bits left shift — multiply by 2^n | `5 << 2` → `20` |
| **Right Shift** | `>>` | Bits right shift — divide by 2^n (sign preserve) | `20 >> 2` → `5` |
| **Unsigned Right Shift** | `>>>` | Right shift — sign bit bhi shift | `-20 >>> 2` → large positive |

### Left Shift

```java
int num = 5;    // 0000 0101

int result1 = num << 1;  // 0000 1010 = 10 (5 * 2)
int result2 = num << 2;  // 0001 0100 = 20 (5 * 4)
int result3 = num << 3;  // 0010 1000 = 40 (5 * 8)

// Formula: num << n = num * 2^n
```

### Right Shift

```java
int num = 40;   // 0010 1000

int result1 = num >> 1;  // 0001 0100 = 20 (40 / 2)
int result2 = num >> 2;  // 0000 1010 = 10 (40 / 4)
int result3 = num >> 3;  // 0000 0101 = 5  (40 / 8)

// Formula: num >> n = num / 2^n

// Negative numbers — sign bit preserve
int neg = -40;  // 1111 1111 1111 1111 1111 1111 1101 1000
int result = neg >> 2;  // 1111 1111 1111 1111 1111 1111 1111 0110 = -10
```

### Unsigned Right Shift

```java
// >>> — sign bit ko bhi 0 se replace karta hai
int neg = -1;  // 1111 1111 1111 1111 1111 1111 1111 1111
int result = neg >>> 1;  // 0111 1111 1111 1111 1111 1111 1111 1111 = 2147483647

// >> vs >>>
int a = -20;
System.out.println(a >> 2);   // -5 (sign preserved)
System.out.println(a >>> 2);  // 1073741819 (sign bit bhi shift)
```

---

## Ternary Operator

**Matlab:** Short form of if-else — ek line mein condition check.

### Syntax

```java
condition ? valueIfTrue : valueIfFalse;
```

### Examples

```java
// Basic
int age = 20;
String status = age >= 18 ? "Adult" : "Minor";  // "Adult"

// Min/Max
int a = 10, b = 20;
int min = a < b ? a : b;  // 10
int max = a > b ? a : b;  // 20

// Absolute value
int num = -5;
int abs = num >= 0 ? num : -num;  // 5

// Nested ternary (avoid — readability kam hoti hai)
int score = 75;
String grade = score >= 90 ? "A"
             : score >= 80 ? "B"
             : score >= 70 ? "C"
             : score >= 60 ? "D"
             : "F";  // "C"
```

### Equivalent if-else

```java
// Ternary
String result = age >= 18 ? "Adult" : "Minor";

// Same as if-else
String result;
if (age >= 18) {
    result = "Adult";
} else {
    result = "Minor";
}
```

### ⚠️ When to Avoid

```java
// ✅ Good — simple, readable
String type = age >= 18 ? "Adult" : "Minor";

// ❌ Bad — complex, nested
String result = condition1 ? (condition2 ? (condition3 ? "A" : "B") : "C") : "D";

// ✅ Better — if-else
if (condition1) {
    if (condition2) {
        result = condition3 ? "A" : "B";
    } else {
        result = "C";
    }
} else {
    result = "D";
}
```

---

## instanceof Operator

**Matlab:** Check karna ki koi object kisi specific class ka instance hai ya nahi.

### Basic Usage

```java
class Animal { }
class Dog extends Animal { }
class Cat extends Animal { }

Animal a1 = new Dog();
Animal a2 = new Cat();

System.out.println(a1 instanceof Dog);  // true
System.out.println(a1 instanceof Cat);  // false
System.out.println(a2 instanceof Cat);  // true
System.out.println(a1 instanceof Animal);  // true (parent class)
```

### Type Casting with instanceof

```java
// Old style (Java 14 se pehle)
if (obj instanceof String) {
    String str = (String) obj;  // Manual cast
    System.out.println(str.length());
}

// New style (Java 14+) — pattern matching
if (obj instanceof String str) {
    System.out.println(str.length());  // Auto-cast — str available hai
}

// Multiple types
if (obj instanceof String s) {
    System.out.println("String: " + s.length());
} else if (obj instanceof Integer i) {
    System.out.println("Integer: " + i);
} else if (obj instanceof List<?> list) {
    System.out.println("List size: " + list.size());
}
```

### instanceof with null

```java
String str = null;
System.out.println(str instanceof String);  // false (null kisi ka instance nahi)
```

---

## Operator Precedence

**Matlab:** Jab multiple operators ek expression mein hon — kaunsa pehle evaluate hoga.

### Precedence Table (High to Low)

| Priority | Operators | Description |
|----------|-----------|-------------|
| **1 (Highest)** | `++`, `--`, `!`, `~` | Unary |
| **2** | `*`, `/`, `%` | Multiplicative |
| **3** | `+`, `-` | Additive |
| **4** | `<<`, `>>`, `>>>` | Shift |
| **5** | `<`, `>`, `<=`, `>=` | Relational |
| **6** | `==`, `!=` | Equality |
| **7** | `&` | Bitwise AND |
| **8** | `^` | Bitwise XOR |
| **9** | `\|` | Bitwise OR |
| **10** | `&&` | Logical AND |
| **11** | `\|\|` | Logical OR |
| **12 (Lowest)** | `? :` | Ternary |

### Examples

```java
// Multiplication pehle, addition baad mein
int result = 5 + 3 * 2;  // 5 + 6 = 11 (not 16)

// Parentheses se order change karo
int result2 = (5 + 3) * 2;  // 8 * 2 = 16

// Complex expression
int a = 10, b = 5, c = 2;
int r = a + b * c > 15 && a - b < 10;
// Step 1: b * c = 10
// Step 2: a + 10 = 20
// Step 3: 20 > 15 = true
// Step 4: a - b = 5
// Step 5: 5 < 10 = true
// Step 6: true && true = true
```

### Best Practice

```java
// ❌ Hard to read
int result = a + b * c > 15 && a - b < 10;

// ✅ Use parentheses — intention clear
int result = ((a + (b * c)) > 15) && ((a - b) < 10);
```

---

## Assignment Operators

| Operator | Example | Equivalent To |
|----------|---------|---------------|
| `=` | `x = 5` | `x = 5` |
| `+=` | `x += 3` | `x = x + 3` |
| `-=` | `x -= 3` | `x = x - 3` |
| `*=` | `x *= 3` | `x = x * 3` |
| `/=` | `x /= 3` | `x = x / 3` |
| `%=` | `x %= 3` | `x = x % 3` |
| `&=` | `x &= 3` | `x = x & 3` |
| `\|=` | `x \|= 3` | `x = x \| 3` |
| `^=` | `x ^= 3` | `x = x ^ 3` |
| `<<=` | `x <<= 2` | `x = x << 2` |
| `>>=` | `x >>= 2` | `x = x >> 2` |

### Examples

```java
int count = 10;
count += 5;   // 15
count -= 3;   // 12
count *= 2;   // 24
count /= 4;   // 6
count %= 4;   // 2

// Bitwise assignment
int flags = 0;
flags |= 1;    // Set bit 0: 0001
flags |= 2;    // Set bit 1: 0011
flags &= ~1;   // Clear bit 0: 0010
flags ^= 2;    // Toggle bit 1: 0000
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Arithmetic** | `+`, `-`, `*`, `/`, `%` — integer division mein decimal part lost hota hai |
| **Relational** | `==`, `!=`, `>`, `<`, `>=`, `<=` — result boolean |
| **Logical** | `&&`, `\|\|`, `!` — short-circuit behavior |
| **Bitwise** | `&`, `\|`, `^`, `~` — individual bits pe kaam |
| **Shift** | `<<`, `>>`, `>>>` — multiply/divide by 2^n |
| **Ternary** | `condition ? true : false` — short if-else |
| **instanceof** | Object type check karna — Java 14+ pattern matching |
| **Precedence** | `* / %` pehle, `+ -` baad mein — parentheses use karo clarity ke liye |
| **Assignment** | `+=`, `-=`, `*=`, etc. — shorthand operators |
