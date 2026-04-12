# String & StringBuilder

## Status: Not Started

---

## Table of Contents

1. [String Immutability](#string-immutability)
2. [String Pool Concept](#string-pool-concept)
3. [String Methods](#string-methods)
4. [StringBuilder vs StringBuffer](#stringbuilder-vs-stringbuffer)
5. [String.join](#stringjoin)
6. [String.format](#stringformat)

---

## String Immutability

**Matlab:** Ek baar String create ho gayi — usko modify nahi kar sakte. Har "modification" ek naya String object banata hai.

### What is Immutability?

```java
String name = "Sachin";
name.toUpperCase();           // "SACHIN" create hua, lekin name change nahi hua
System.out.println(name);     // "Sachin" (original same hai)

// Actually change karne ke liye reassign karo
name = name.toUpperCase();
System.out.println(name);     // "SACHIN" (ab naya object assign hua)
```

### Why Strings are Immutable?

```java
// 1. Security — passwords, connection strings
String password = "mySecret123";
// Agar mutable hota toh koi aur code modify kar sakta tha

// 2. Thread Safety — multiple threads safely use kar sakte hain
// Synchronization ki zarurat nahi

// 3. Caching — String Pool possible hota hai
// Same string ko memory mein ek baar store karo

// 4. Hash Code Caching — HashMap keys ke liye efficient
String key = "myKey";
// Hash code ek baar calculate, baar-baar use
```

### How Immutability Works

```java
String s1 = "Hello";
String s2 = s1.concat(" World");  // Naya object bana

System.out.println(s1);  // "Hello" (original unchanged)
System.out.println(s2);  // "Hello World" (naya object)

// Internally:
// s1 → "Hello" (memory location A)
// s2 → "Hello World" (memory location B) — different object
```

### Immutable Class Design

```java
// String jaisi immutable class kaise banayein
public final class ImmutablePerson {  // final class — extend nahi ho sakta
    private final String name;        // final fields
    private final int age;

    public ImmutablePerson(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Sirf getters — no setters
    public String getName() { return name; }
    public int getAge() { return age; }

    // Agar mutable field return karna ho toh copy banao
    // (String already immutable hai — copy ki zarurat nahi)
}
```

---

## String Pool Concept

**Matlab:** JVM memory mein ek special area jahan string literals store hote hain — duplicate strings avoid karte hain.

### String Pool Behavior

```java
// String literals — pool mein jaate hain
String s1 = "Hello";
String s2 = "Hello";

System.out.println(s1 == s2);       // true (same object in pool)
System.out.println(s1.equals(s2));  // true (same content)

// new String() — heap mein naya object
String s3 = new String("Hello");

System.out.println(s1 == s3);       // false (different objects)
System.out.println(s1.equals(s3));  // true (same content)
```

### Memory Layout

```
String Pool (Heap ke andar):
┌─────────────────┐
│ "Hello" @100    │ ← s1, s2 yahan point kar rahe hain
└─────────────────┘

Regular Heap:
┌─────────────────┐
│ "Hello" @200    │ ← s3 yahan point kar raha hai (new String())
└─────────────────┘
```

### intern() Method

```java
// new String() ko pool mein daalo
String s1 = new String("Hello");
String s2 = s1.intern();
String s3 = "Hello";

System.out.println(s1 == s2);  // false (s1 heap mein hai)
System.out.println(s2 == s3);  // true (dono pool mein hain)
```

### When to Use intern()

```java
// ✅ Use karo jab bahut sare duplicate strings hon
// Memory save hogi
List<String> words = new ArrayList<>();
for (int i = 0; i < 100000; i++) {
    words.add("common".intern());  // Sirf ek "common" pool mein
}

// ❌ Avoid karo jab strings unique hon
// Overhead badhega — pool lookup time lega
```

---

## String Methods

### split

**Matlab:** String ko delimiter ke basis pe todna.

```java
String csv = "apple,banana,cherry,date";

// Basic split
String[] fruits = csv.split(",");
// ["apple", "banana", "cherry", "date"]

// Limit parameter
String[] limited = csv.split(",", 2);
// ["apple", "banana,cherry,date"]

// Regex delimiter
String text = "one two  three   four";
String[] words = text.split("\\s+");  // One or more spaces
// ["one", "two", "three", "four"]

// Special characters ko escape karo
String path = "com.example.myapp";
String[] parts = path.split("\\.");  // . is regex metacharacter
// ["com", "example", "myapp"]
```

### substring

**Matlab:** String ka hissa extract karna.

```java
String text = "Hello, World!";

// substring(startIndex)
String part1 = text.substring(7);       // "World!"

// substring(startIndex, endIndex)
String part2 = text.substring(0, 5);    // "Hello"
String part3 = text.substring(7, 12);   // "World"

// ⚠️ endIndex exclusive hai
String part4 = text.substring(0, 5);    // Characters 0,1,2,3,4 (5 nahi)

// Last N characters
String last5 = text.substring(text.length() - 5);  // "orld!"
```

### indexOf / lastIndexOf

**Matlab:** Character ya substring ka position dhundhna.

```java
String text = "Hello, World! Hello again!";

// First occurrence
int first = text.indexOf('H');          // 0
int firstWorld = text.indexOf("World"); // 7

// First occurrence after index
int second = text.indexOf("Hello", 5);  // 14 (second "Hello")

// Last occurrence
int last = text.lastIndexOf('H');       // 14
int lastHello = text.lastIndexOf("Hello");  // 14

// Not found
int notFound = text.indexOf("XYZ");     // -1

// Check existence
boolean exists = text.indexOf("World") != -1;  // true
// Ya better: text.contains("World")
```

### replace / replaceAll

**Matlab:** Characters ya patterns ko replace karna.

```java
String text = "Hello, World!";

// replace — literal string replace (all occurrences)
String replaced = text.replace("World", "Java");
// "Hello, Java!"

// replaceAll — regex-based replace
String text2 = "one1two2three3";
String noDigits = text2.replaceAll("\\d+", "");
// "onetwothree"

// replaceFirst — sirf first occurrence
String text3 = "apple apple apple";
String replacedFirst = text3.replaceFirst("apple", "banana");
// "banana apple apple"

// ⚠️ replaceAll mein regex special characters escape karo
String path = "com.example.test";
String fixed = path.replaceAll("\\.", "/");
// "com/example/test"
```

### contains

**Matlab:** String mein substring exist karta hai ya nahi.

```java
String text = "Hello, World!";

boolean hasWorld = text.contains("World");  // true
boolean hasJava = text.contains("Java");    // false

// Case-sensitive
boolean hasWorld = text.contains("world");  // false (lowercase w)

// Case-insensitive check
boolean hasWorldCI = text.toLowerCase().contains("world");  // true
```

### trim / strip

**Matlab:** Leading/trailing whitespace remove karna.

```java
String text = "   Hello, World!   ";

// trim — ASCII whitespace only
String trimmed = text.trim();  // "Hello, World!"

// strip — Unicode whitespace (Java 11+)
String unicode = "\u2000Hello\u2000";  // Unicode whitespace
String stripped = unicode.strip();     // "Hello"

// stripLeading / stripTrailing (Java 11+)
String leading = text.stripLeading();  // "Hello, World!   "
String trailing = text.stripTrailing(); // "   Hello, World!"
```

### Other Useful Methods

```java
String text = "Hello, World!";

// Length
int len = text.length();              // 13

// Char at index
char ch = text.charAt(0);             // 'H'

// Convert to case
String upper = text.toUpperCase();    // "HELLO, WORLD!"
String lower = text.toLowerCase();    // "hello, world!"

// Check start/end
boolean starts = text.startsWith("Hello");  // true
boolean ends = text.endsWith("!");          // true

// Check empty
boolean isEmpty = text.isEmpty();           // false
boolean isBlank = text.isBlank();           // false (Java 11+)

// Convert to char array
char[] chars = text.toCharArray();          // ['H', 'e', 'l', 'l', 'o', ...]

// Repeat (Java 11+)
String repeated = "Ha".repeat(3);           // "HaHaHa"

// Lines (Java 11+)
String multiLine = "Line1\nLine2\nLine3";
multiLine.lines().forEach(System.out::println);
```

---

## StringBuilder vs StringBuffer

**Matlab:** Mutable strings — string ko modify karna without creating new objects.

### Why Not String Concatenation?

```java
// ❌ Inefficient — har + operator naya String object banata hai
String result = "";
for (int i = 0; i < 1000; i++) {
    result += i;  // 1000 String objects create honge!
}

// ✅ Efficient — ek hi object modify hota hai
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) {
    sb.append(i);  // Same object, no new allocations
}
String result = sb.toString();
```

### StringBuilder (Non-Synchronized — Fast)

```java
StringBuilder sb = new StringBuilder();

// Append
sb.append("Hello");
sb.append(" ");
sb.append("World");
// "Hello World"

// Method chaining
sb.append("Hello").append(" ").append("World");

// Insert
sb.insert(5, ",");  // "Hello, World"

// Delete
sb.delete(5, 7);    // "HelloWorld" (5 inclusive, 7 exclusive)

// Replace
sb.replace(5, 10, "Java");  // "HelloJava"

// Reverse
sb.reverse();  // "avaJolleH"

// Capacity
sb.capacity();  // Current capacity
sb.ensureCapacity(100);  // Ensure minimum capacity

// Convert to String
String result = sb.toString();
```

### StringBuffer (Synchronized — Thread-Safe)

```java
// Same API as StringBuilder, but thread-safe
StringBuffer sb = new StringBuffer();
sb.append("Hello").append(" ").append("World");

// Multiple threads safely use kar sakte hain
// But performance thoda slow hai (synchronization overhead)
```

### StringBuilder vs StringBuffer vs String

| Feature | String | StringBuilder | StringBuffer |
|---------|--------|---------------|--------------|
| **Mutable** | ❌ No | ✅ Yes | ✅ Yes |
| **Thread-Safe** | ✅ Yes | ❌ No | ✅ Yes |
| **Performance** | Slow (new objects) | Fast | Medium (sync overhead) |
| **Use When** | Fixed content | Single-threaded modifications | Multi-threaded modifications |

### When to Use What

```java
// ✅ String — content change nahi hoga
String greeting = "Hello, World!";

// ✅ StringBuilder — single-threaded, multiple modifications
StringBuilder sb = new StringBuilder();
sb.append("SELECT * FROM users");
sb.append(" WHERE active = true");
sb.append(" ORDER BY name");
String query = sb.toString();

// ✅ StringBuffer — multi-threaded modifications
StringBuffer sharedBuffer = new StringBuffer();
// Multiple threads append safely
```

---

## String.join

**Matlab:** Multiple strings ko ek delimiter ke saath join karna.

### Basic Usage

```java
// String.join (Java 8+)
String result = String.join(", ", "apple", "banana", "cherry");
// "apple, banana, cherry"

// Iterable (List, Set)
List<String> fruits = List.of("apple", "banana", "cherry");
String joined = String.join("-", fruits);
// "apple-banana-cherry"

// Array
String[] arr = {"one", "two", "three"};
String joined = String.join(" | ", arr);
// "one | two | three"
```

### Joining Collector (Streams)

```java
// Stream.collect with Joining (Java 8+)
List<String> names = List.of("Sachin", "Rahul", "Priya");

String commaSeparated = names.stream()
    .collect(Collectors.joining(", "));
// "Sachin, Rahul, Priya"

// With prefix and suffix
String formatted = names.stream()
    .collect(Collectors.joining(", ", "[", "]"));
// "[Sachin, Rahul, Priya]"

// Filtered + joined
String activeUsers = users.stream()
    .filter(User::isActive)
    .map(User::getName)
    .collect(Collectors.joining(", "));
```

---

## String.format

**Matlab:** Formatted string banana — printf style formatting.

### Basic Usage

```java
// String.format (Java 1.5+)
String name = "Sachin";
int age = 25;
double salary = 50000.50;

// String interpolation
String formatted = String.format("Name: %s, Age: %d", name, age);
// "Name: Sachin, Age: 25"

// Number formatting
String salaryStr = String.format("Salary: ₹%.2f", salary);
// "Salary: ₹50000.50"

// Padding
String padded = String.format("%-10s|", "Left");    // Left-aligned
// "Left      |"

String padded2 = String.format("%10s|", "Right");   // Right-aligned
// "     Right|"

// Zero padding
String number = String.format("%05d", 42);
// "00042"

// Hexadecimal
String hex = String.format("0x%08X", 255);
// "0x000000FF"

// Percentage
String percent = String.format("%.1f%%", 75.5);
// "75.5%"
```

### Format Specifiers

| Specifier | Type | Example |
|-----------|------|---------|
| `%s` | String | `"Hello %s".format("World")` |
| `%d` | Integer (decimal) | `"%d".format(42)` |
| `%f` | Float/Double | `"%.2f".format(3.14159)` |
| `%c` | Character | `"%c".format('A')` |
| `%b` | Boolean | `"%b".format(true)` |
| `%x` | Hexadecimal | `"%x".format(255)` → `"ff"` |
| `%o` | Octal | `"%o".format(8)` → `"10"` |
| `%n` | Newline | Platform-specific newline |

### Width and Precision

```java
// Width (minimum characters)
String.format("%10s", "Hi");  // "        Hi"

// Precision (decimal places)
String.format("%.2f", 3.14159);  // "3.14"

// Width + Precision
String.format("%10.2f", 3.14);  // "      3.14"

// Left-align
String.format("%-10s", "Hi");  // "Hi        "

// Zero-pad numbers
String.format("%05d", 42);  // "00042"

// Multiple arguments
String.format("%s is %d years old and earns ₹%.2f", "Sachin", 25, 50000.50);
// "Sachin is 25 years old and earns ₹50000.50"
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Immutability** | String modify nahi ho sakta — har "modification" naya object banata hai |
| **String Pool** | JVM memory mein duplicate strings avoid karta hai — literals pool mein jaate hain |
| **split** | Delimiter ke basis pe string todna |
| **substring** | String ka hissa extract karna — endIndex exclusive hai |
| **indexOf** | Character/substring ka position dhundhna — not found pe -1 |
| **replace/replaceAll** | Literal vs regex-based replacement |
| **StringBuilder** | Mutable string — single-threaded, fast |
| **StringBuffer** | Mutable string — multi-threaded, synchronized |
| **String.join** | Strings ko delimiter ke saath join karo |
| **String.format** | Formatted string — `%s`, `%d`, `%f`, etc. |
