# String Internals

## Status: Not Started

---

## Table of Contents

1. [Why Strings Are Special](#why-strings-are-special)
2. [Q1: String Immutable Kyon Hai?](#q1-string-immutable-kyon-hai)
3. [String Pool — Heap vs Old PermGen](#string-pool--heap-vs-old-permgen)
4. [Literal vs `new String("...")`](#literal-vs-new-string)
5. [`intern()` — Manually Pool Mein Daalna](#intern--manually-pool-mein-daalna)
6. [`==` vs `equals()` — Classic Trap](#-vs-equals--classic-trap)
7. [String Concatenation — `+` ka Sach](#string-concatenation---ka-sach)
8. [`StringBuilder` vs `StringBuffer`](#stringbuilder-vs-stringbuffer)
9. [Why `char[]` Preferred for Passwords?](#why-char-preferred-for-passwords)
10. [String Switch / Pattern Matching](#string-switch--pattern-matching)
11. [Common Interview Outputs](#common-interview-outputs)
12. [Pitfalls & Tricks](#pitfalls--tricks)
13. [Cheat Sheet](#cheat-sheet)

---

## Why Strings Are Special

- Sabse common type backend mein
- Hashing, switch, security, network — har jagah
- `final class String` (Java 9+ internal repr `byte[]` + coder — **Compact Strings**)

---

## Q1: String Immutable Kyon Hai?

> "Bhai, design choice kyon? 4 mukhya reasons batao."

1. **Security** — file paths, URLs, DB credentials kabhi mid-flight modify na ho.
2. **Thread safety** — multiple threads share kar sakte without sync.
3. **Hashing performance** — `hashCode()` ek baar compute, **cached** (`hash` field).
4. **String pool / interning** — same content = same reference safely share.

> "Aur agar mutable hota toh class loading time pe loaded class names tamper ho sakte the."

---

## String Pool — Heap vs Old PermGen

### Modern JVM (Java 7+)

- **String pool heap mein** baith gaya (PermGen Java 8 mein hata).
- Fully GC-able pool entries.

### Visual

```
HEAP
 ├── String Pool (interned literals)
 │     "hello"   "java"   "spring"
 ├── Regular objects
 │     new String("hello")  ← alag object even if pool already has "hello"
 └── ...
```

---

## Literal vs `new String("...")`

```java
String a = "hello";              // pool entry (or reuses existing)
String b = "hello";              // SAME pool reference
String c = new String("hello");  // NEW heap object; literal still in pool
String d = new String("hello").intern();  // refers to pool

a == b;   // true
a == c;   // false
a == d;   // true
```

### Memory Picture

```
"hello" (pool) ←─────── a, b, d
                         
new String("hello")  ←─ c   (heap, separate object)
```

---

## `intern()` — Manually Pool Mein Daalna

```java
String s = new String("hello").intern();
```

- Agar pool mein same content hai → uska reference return.
- Nahi to pool mein add karke return.

### Use Case (rare)

- Massive strings repeat ho rahi ho — memory bachao.
- Modern: `String.intern()` ka overuse perf hit; **`-XX:+UseStringDeduplication`** (G1) auto-dedup karta hai.

---

## `==` vs `equals()` — Classic Trap

| Op | Compares |
|----|----------|
| `==` | Reference identity |
| `equals()` | Content (`String.equals` content compare) |

```java
String a = "java";
String b = "java";
String c = new String("java");

a == b;          // true (same pool)
a == c;          // false
a.equals(c);     // true
```

**Rule (interview gold):** Strings compare karne ke liye **always `equals()`** — pool optimization pe trust mat karo.

### Null-safe

```java
"java".equals(maybeNull);   // safe (literal pe call)
Objects.equals(a, b);       // null-safe utility
```

---

## String Concatenation — `+` ka Sach

### Compiler Magic (pre-Java 9)

```java
String s = a + b + c;
// compiler ≈
String s = new StringBuilder().append(a).append(b).append(c).toString();
```

### Java 9+ — `invokedynamic` + StringConcatFactory

JVM optimize karta hai (`makeConcatWithConstants`) — usually faster.

### Loop Trap (interview classic)

```java
String s = "";
for (int i = 0; i < 100_000; i++) {
    s += i;     // ❌ har iteration mein NEW StringBuilder!
}
```

→ **O(n²)** behaviour. Output ke saath har baar string copy.

**Fix:**

```java
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 100_000; i++) {
    sb.append(i);
}
String s = sb.toString();
```

### `String.join`

```java
String csv = String.join(",", "a", "b", "c");
```

---

## `StringBuilder` vs `StringBuffer`

| | StringBuilder | StringBuffer |
|--|---------------|--------------|
| Thread safe | ❌ | ✅ (synchronized methods) |
| Speed | Faster | Slower |
| Use | Local mutable string | Legacy / shared mutation |

**Modern advice:** Almost always **`StringBuilder`** — concurrency ke liye higher abstractions.

---

## Why `char[]` Preferred for Passwords?

> "Spring Security ka `Authentication` `Object credentials` mein bhi often `char[]` rakha jata tha."

### Reasons

1. **Mutability** — use ke baad `Arrays.fill(pwd, '\0')` se zero out kar sakte.
2. **String immutable** — once leaked, **GC tak memory mein**; heap dump mein readable.
3. **String pool risk** — accidentally interned string cleanup mushkil.
4. **Logging accident** — `String.toString()` log/print easy; `char[].toString()` `[C@hash` deta — naturally less leaky.

```java
char[] pwd = readPassword();
try {
    authenticate(pwd);
} finally {
    Arrays.fill(pwd, '\0');   // erase
}
```

⚠️ Modern frameworks often `Password`-type wrappers use karte hain; raw `char[]` exact answer interview ke liye sufficient.

---

## String Switch / Pattern Matching

```java
switch (status) {
    case "ACTIVE" -> handle();
    case "INACTIVE" -> skip();
    default -> throw new IllegalStateException();
}
```

**Internally:** `String.hashCode() + equals()` based dispatch.

Java 21+ pattern matching:

```java
Object o = ...;
String desc = switch (o) {
    case Integer i -> "int " + i;
    case String s when s.startsWith("ID-") -> "id " + s;
    default -> "?";
};
```

---

## Common Interview Outputs

### Q. Output?

```java
String a = "ab";
String b = "a" + "b";              // compile-time constant → pool
System.out.println(a == b);        // true

String c = "a";
String d = c + "b";                // runtime concat → new String
System.out.println(a == d);        // false
System.out.println(a.equals(d));   // true
```

### Q. `intern` chain

```java
String s1 = new String("hello");
String s2 = s1.intern();
String s3 = "hello";
s1 == s2;   // false
s2 == s3;   // true
```

### Q. Final + concat

```java
final String x = "foo";
final String y = "bar";
String z = x + y;                   // compile-time constant
String w = "foobar";
z == w;                             // true (pool)
```

`final` na ho to runtime concat → false.

---

## Pitfalls & Tricks

1. **`==` for strings** — almost always wrong.
2. **`intern()` overuse** — pool fragmentation; trust `UseStringDeduplication`.
3. **Concatenation in loop** — `StringBuilder` use karo.
4. **Locale-sensitive ops** (`toUpperCase()` Turkish locale `i` → dotted I).
5. **`String.format` slow path** — hot loops mein avoid; concat / builder use.
6. **`getBytes()` without charset** — `getBytes(StandardCharsets.UTF_8)` always.
7. **Compact strings (Java 9+)** — internally Latin-1 if possible — explanation interview mein "byte[] + coder field" mention.

---

## Cheat Sheet

| Concept | One-liner |
|---------|-----------|
| Immutable | Security + thread safe + hashing + pooling |
| Pool location | Heap (Java 7+) |
| Literal vs `new` | Pool reuse vs fresh heap |
| `intern()` | Force pool reference |
| `==` vs `equals()` | Identity vs content — always `equals` |
| `+` in loop | `StringBuilder` substitute |
| Password | `char[]` zero out, avoid `String` |
| Compact string | Latin-1 byte[] + coder |
| Dedup | G1 `+UseStringDeduplication` |

---

## Practice

1. `==` vs `equals()` ke 5 cases run karke output predict karo, then verify.
2. Loop concat 100k iterations time karo `+=` vs `StringBuilder`.
3. Heap dump (Eclipse MAT) mein password `String` dhundo — leak demonstrate.
4. `intern()` se memory savings benchmark karo (huge dataset).
5. Locale trap: `"i".toUpperCase(Locale.forLanguageTag("tr"))` ka output dekho.
