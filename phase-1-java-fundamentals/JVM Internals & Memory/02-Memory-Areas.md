# Memory Areas

## Status: Not Started

---

## Table of Contents

1. [Heap Memory](#heap-memory)
2. [Young Generation](#young-generation)
3. [Old Generation](#old-generation)
4. [Stack Memory](#stack-memory)
5. [Metaspace](#metaspace)
6. [Memory Area Comparison](#memory-area-comparison)

---

## Heap Memory

**Matlab:** JVM ka sabse bada memory area — objects aur arrays yahan allocate hote hain.

### Heap Structure

```
┌─────────────────────────────────────────────────────────────┐
│                         Heap Memory                         │
├─────────────────────────────────────────────────────────────┤
│  Young Generation (1/3 of heap)    │  Old Generation (2/3)  │
│  ┌──────────┬───────────┬──────────┤                        │
│  │ Eden     │ Survivor  │ Survivor │  Tenured / Old Gen     │
│  │ Space    │ Space 0   │ Space 1  │                        │
│  │ (New     │ (S0)      │ (S1)     │  Long-lived objects    │
│  │ Objects) │           │          │                        │
│  └──────────┴───────────┴──────────┤                        │
├─────────────────────────────────────────────────────────────┤
│  Metaspace (separate, native memory)                         │
│  Class metadata, method code, constant pool                  │
└─────────────────────────────────────────────────────────────┘
```

### Heap Size Configuration

```bash
# Default heap size — system RAM pe depend karta hai

# Initial heap size set karo
java -Xms512m MyApp

# Maximum heap size set karo
java -Xmx2g MyApp

# Recommended: Xms = Xmx (prevent resizing overhead)
java -Xms2g -Xmx2g MyApp

# Heap dump on OOM
java -XX:+HeapDumpOnOutOfMemoryError MyApp
java -XX:HeapDumpPath=/path/to/dump.hprof MyApp
```

### Object Lifecycle in Heap

```
New Object → Eden Space
    ↓
Minor GC → Survived? → Survivor Space (S0 or S1)
    ↓
Minor GC → Still survived? → Other Survivor Space
    ↓
Age reaches threshold (default 15) → Old Generation
    ↓
Old Generation full? → Major/Full GC
    ↓
Still no space? → OutOfMemoryError: Java heap space
```

---

## Young Generation

**Matlab:** Naye objects yahan create hote hain — short-lived objects ke liye optimized.

### Eden Space

```java
// 80% of Young Generation
// Naye objects yahan allocate hote hain

String str = new String("Hello");  // Eden mein
User user = new User();            // Eden mein
int[] arr = new int[100];          // Eden mein
```

### Survivor Spaces (S0, S1)

```java
// 10% each — Eden se survive karne wale objects yahan jaate hain
// Copying collector — live objects ko ek space se dusre mein copy karta hai

// Minor GC process:
// 1. Eden + active Survivor se live objects dhundho
// 2. Unhe dusre Survivor Space mein copy karo
// 3. Eden aur purana Survivor clear karo
// 4. Roles swap karo (S0 ↔ S1)
```

### Object Aging

```java
// Har object ka "age" hota hai — kitni baar GC survive kiya
// Default threshold: 15 (-XX:MaxTenuringThreshold)

// Age 0: Newly created in Eden
// Age 1-14: Survivor spaces mein survive kiya
// Age 15+: Old generation mein promote ho gaya
```

### Minor GC

```
Trigger: Eden space full ho jati hai

Process:
1. Mark all live objects in Eden + active Survivor
2. Copy live objects to other Survivor Space
3. Increment age of copied objects
4. Clear Eden and old Survivor
5. Swap Survivor roles

Duration: Fast (milliseconds)
Impact: Stop-the-world (brief pause)
```

---

## Old Generation

**Matlab:** Long-lived objects yahan store hote hain — applications ka main data.

### What Goes to Old Gen

```java
// Objects jo 15+ Minor GC survive kar lete hain
// Large objects (directly Old Gen mein jaate hain)
// Static variables
// Application state (caches, sessions, etc.)

// Examples:
// - Database connection pools
// - Application caches
// - Session data
// - Configuration objects
```

### Major GC / Full GC

```
Major GC: Sirf Old Generation collect karta hai
Full GC: Young + Old + Metaspace — sab collect karta hai

Trigger:
- Old Generation full
- System.gc() call (suggestion, not guarantee)
- Metaspace full

Duration: Slow (seconds)
Impact: Stop-the-world (significant pause)
```

### Old Generation Tuning

```bash
# Old generation size control
java -XX:NewRatio=2 MyApp
# NewRatio=2 means Old:Young = 2:1

# Survivor ratio
java -XX:SurvivorRatio=8 MyApp
# Eden:S0:S1 = 8:1:1

# Max tenuring threshold
java -XX:MaxTenuringThreshold=15 MyApp
# Kitni baar survive karne ke baad Old Gen mein jayega
```

---

## Stack Memory

**Matlab:** Har thread ka apna stack — method calls, local variables, partial results.

### Stack Structure

```
┌─────────────────────────────────────────┐
│ Thread Stack                            │
├─────────────────────────────────────────┤
│ Stack Frame: main()                     │
│   Local Variables: args                 │
│   Operand Stack                         │
│   Return Address                        │
├─────────────────────────────────────────┤
│ Stack Frame: methodA()                  │
│   Local Variables: x, user              │
│   Operand Stack                         │
│   Return Address                        │
├─────────────────────────────────────────┤
│ Stack Frame: methodB()                  │
│   Local Variables: s, result            │
│   Operand Stack                         │
│   Return Address                        │
└─────────────────────────────────────────┘
```

### Stack Frame Components

```java
public int calculate(int a, int b) {
    int sum = a + b;        // Local variable
    int product = a * b;    // Local variable
    return sum + product;   // Operand stack
}

// Stack Frame:
// - Local Variables: a, b, sum, product
// - Operand Stack: Intermediate results (a+b, a*b, sum+product)
// - Return Address: Caller ko wapas jana hai
```

### Stack Size

```bash
# Per thread stack size
java -Xss256k MyApp    # 256 KB per thread
java -Xss1m MyApp      # 1 MB per thread (default on many systems)

# Deep recursion → StackOverflowError
# Bahut zyada threads → OutOfMemoryError: unable to create native thread
```

### Stack vs Heap

```java
public void example() {
    // Stack pe: reference variables
    String str;
    User user;
    int[] arr;

    // Heap pe: actual objects
    str = new String("Hello");  // Object heap mein
    user = new User();          // Object heap mein
    arr = new int[100];         // Array heap mein
}

// Stack: str, user, arr references
// Heap: String object, User object, int array
```

---

## Metaspace

**Matlab:** Java 8+ — Class metadata store karne ke liye native memory use hoti hai. PermGen ka replacement.

### Metaspace vs PermGen

| Feature | PermGen (Java 7-) | Metaspace (Java 8+) |
|---------|-------------------|---------------------|
| **Location** | Heap mein | Native memory (off-heap) |
| **Size** | Fixed (-XX:MaxPermSize) | Auto-growing |
| **OOM** | java.lang.OutOfMemoryError: PermGen space | java.lang.OutOfMemoryError: Metaspace |
| **GC** | Full GC pe hi clean hota tha | Automatically managed |

### What Goes to Metaspace

```java
// Class structures
class User {
    private String name;  // Field metadata
    public void display() {}  // Method code + metadata
}

// Constant pool
// Annotations
// Method code (bytecode)
// Runtime constant pool
// JIT compiled code (sometimes)
```

### Metaspace Configuration

```bash
# Initial Metaspace size
java -XX:MetaspaceSize=256m MyApp

# Maximum Metaspace size
java -XX:MaxMetaspaceSize=512m MyApp

# Without MaxMetaspaceSize — unlimited growth (can cause system OOM)

# Metaspace GC dekho
java -XX:+TraceClassLoading MyApp
java -XX:+TraceClassUnloading MyApp
```

### ClassLoader Leak

```java
// ClassLoader + loaded classes Metaspace mein hote hain
// ClassLoader GC nahi hota agar koi reference hold kar raha hai

// Common causes:
// 1. ThreadLocal with ClassLoader reference
// 2. Static fields holding class references
// 3. JNI global references
// 4. Listener registrations

// Detection:
// Metaspace continuously grow ho raha hai?
// Classes load ho rahi hain par unload nahi ho rahi?
```

---

## Memory Area Comparison

### Stack vs Heap

| Feature | Stack | Heap |
|---------|-------|------|
| **Scope** | Per thread | Shared by all threads |
| **Stores** | Local variables, method frames | Objects, arrays |
| **Lifetime** | Method call duration | Object lifetime (until GC) |
| **Access** | Fast, LIFO | Slower, random access |
| **Size** | Small (-Xss) | Large (-Xmx) |
| **Cleanup** | Automatic (method return) | Garbage Collector |
| **Error** | StackOverflowError | OutOfMemoryError |

### Heap vs Metaspace

| Feature | Heap | Metaspace |
|---------|------|-----------|
| **Stores** | Objects, arrays | Class metadata, method code |
| **Location** | JVM memory | Native memory |
| **GC** | Minor/Major GC | Class unloading |
| **Sizing** | -Xms, -Xmx | -XX:MetaspaceSize, -XX:MaxMetaspaceSize |
| **Growth** | Bounded by -Xmx | Auto-growing (bounded by MaxMetaspaceSize) |

### Memory Layout Summary

```
JVM Memory
├── Heap (objects, arrays) — -Xms/-Xmx
│   ├── Young Generation (Eden + S0 + S1)
│   └── Old Generation
├── Metaspace (class metadata) — native memory
├── Thread Stacks (per thread) — -Xss
├── PC Registers (per thread)
└── Native Method Stacks (per thread)
```

---

## Summary

| Area | Purpose | Configuration |
|------|---------|---------------|
| **Eden Space** | New objects allocate | Part of Young Gen |
| **Survivor Spaces** | Objects surviving Minor GC | -XX:SurvivorRatio |
| **Old Generation** | Long-lived objects | -XX:NewRatio |
| **Stack** | Method frames, locals | -Xss |
| **Metaspace** | Class metadata | -XX:MaxMetaspaceSize |
| **PC Register** | Current instruction | Internal |
| **Native Stack** | JNI calls | Internal |
