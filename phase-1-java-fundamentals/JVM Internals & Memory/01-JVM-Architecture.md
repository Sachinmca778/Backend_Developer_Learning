# JVM Architecture

## Status: Not Started

---

## Table of Contents

1. [JVM Overview](#jvm-overview)
2. [ClassLoader Subsystem](#classloader-subsystem)
3. [Runtime Data Areas](#runtime-data-areas)
4. [Execution Engine](#execution-engine)
5. [JIT Compiler](#jit-compiler)

---

## JVM Overview

**Matlab:** Java Virtual Machine — Java code ko run karne wala virtual machine. "Write Once, Run Anywhere" ka foundation.

### JVM Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        JVM Architecture                      │
├─────────────────────────────────────────────────────────────┤
│  ClassLoader Subsystem                                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Bootstrap   │  │ Extension   │  │ Application/System  │  │
│  │  ClassLoader │  │ ClassLoader │  │ ClassLoader         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│  Runtime Data Areas (Memory)                                 │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐   │
│  │ Heap       │  │ Stack      │  │ Method Area / Meta   │   │
│  │ (Objects)  │  │ (Frames)   │  │ (Class Metadata)     │   │
│  └────────────┘  └────────────┘  └──────────────────────┘   │
│  ┌────────────┐  ┌────────────┐                              │
│  │ PC Register│  │ Native     │                              │
│  │            │  │ Stack      │                              │
│  └────────────┘  └────────────┘                              │
├─────────────────────────────────────────────────────────────┤
│  Execution Engine                                            │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────────┐   │
│  │ Interpreter│  │ JIT Compiler│ │ Garbage Collector    │   │
│  └────────────┘  └────────────┘  └──────────────────────┘   │
├─────────────────────────────────────────────────────────────┤
│  Native Method Interface (JNI)                               │
└─────────────────────────────────────────────────────────────┘
```

### JVM vs JRE vs JDK

```
JDK (Java Development Kit)
  ├── JRE (Java Runtime Environment)
  │     └── JVM (Java Virtual Machine)
  ├── javac (compiler)
  ├── java (launcher)
  └── dev tools (javadoc, jar, etc.)

JDK = Development + Runtime
JRE = Runtime only
JVM = Core engine that runs bytecode
```

---

## ClassLoader Subsystem

**Matlab:** `.class` files ko load karna, verify karna, aur memory mein initialize karna.

### ClassLoader Hierarchy

```
Bootstrap ClassLoader (C++)
    ↑
Extension ClassLoader
    ↑
Application/System ClassLoader
    ↑
Custom ClassLoaders
```

### Bootstrap ClassLoader

```java
// C++ mein written — JVM ka part
// Core Java classes load karta hai
// rt.jar (Java 8) ya modules (Java 9+) load karta hai

// Parent nahi hota — sabse upar
```

### Extension ClassLoader

```java
// Java mein written
// Extension classes load karta hai
// $JAVA_HOME/lib/ext directory se load hota hai
// Bootstrap ka child
```

### Application/System ClassLoader

```java
// Application classes load karta hai
// CLASSPATH se load hota hai
// Extension ka child

// ClassLoader get karo
ClassLoader cl = MyClass.class.getClassLoader();
System.out.println(cl);  // sun.misc.Launcher$AppClassLoader
```

### ClassLoader Parent Delegation Model

```
Custom ClassLoader → App ClassLoader → Ext ClassLoader → Bootstrap ClassLoader
                                                                    ↓
                                                            Load try karo
                                                                    ↓
                                                            Nahi mila?
                                                                    ↓
                                                            Child ko delegate karo
```

**Rules:**
1. Har ClassLoader apne parent ko pehle delegate karta hai
2. Parent load kar le toh done
3. Parent nahi kar paata toh child try karta hai
4. Security — core Java classes ko override nahi kar sakte

### Custom ClassLoader

```java
class NetworkClassLoader extends ClassLoader {
    @Override
    protected Class<?> findClass(String name) {
        byte[] classBytes = downloadClassFromNetwork(name);
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    private byte[] downloadClassFromNetwork(String name) {
        // Network se .class file download karo
        return new byte[0];
    }
}
```

### Class Loading Process

```
1. Loading: .class file read karo → binary data
2. Linking:
   a. Verification: Bytecode verify karo — valid hai ya nahi
   b. Preparation: Static fields ko default values do
   c. Resolution: Symbolic references ko actual references mein convert karo
3. Initialization: Static fields ko actual values do, static blocks execute karo
```

---

## Runtime Data Areas

**Matlab:** JVM ka memory layout — kahan kya store hota hai.

### Memory Layout

```
┌─────────────────────────────────────────────────────────────┐
│                    Runtime Data Areas                        │
├─────────────────────────────────────────────────────────────┤
│  Per-Thread Areas:          │  Shared Areas:                │
│  ┌────────────────────┐     │  ┌──────────────────────┐     │
│  │ Program Counter    │     │  │ Heap                 │     │
│  │ (PC) Register      │     │  │ (Objects, Arrays)    │     │
│  ├────────────────────┤     │  ├──────────────────────┤     │
│  │ Java Virtual       │     │  │ Method Area /        │     │
│  │ Machine Stack      │     │  │ Metaspace            │     │
│  │ (Frames, Locals)   │     │  │ (Class Metadata)     │     │
│  ├────────────────────┤     │  ├──────────────────────┤     │
│  │ Native Method      │     │  │ Runtime Constant     │     │
│  │ Stack              │     │  │ Pool                 │     │
│  └────────────────────┘     │  └──────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### Heap (Shared)

```java
// Objects aur arrays yahan store hote hain
// Sab threads share karte hain
// Garbage Collection yahan hoti hai

String str = new String("Hello");  // Heap mein
User user = new User();            // Heap mein
int[] arr = new int[100];          // Heap mein
```

### Stack (Per Thread)

```java
// Har thread ka apna stack hota hai
// Method calls, local variables, partial results store hote hain

public void methodA() {
    int x = 10;           // Stack mein
    methodB();
}

public void methodB() {
    String s = "Hello";   // Stack pe reference, object heap mein
    methodC();
}

public void methodC() {
    int[] arr = {1,2,3};  // Stack pe reference, array heap mein
}
```

**Stack Frame Structure:**
```
┌─────────────────────────────────────────┐
│ Stack Frame for methodC()               │
├─────────────────────────────────────────┤
│ Local Variables: arr reference          │
│ Operand Stack: Intermediate results     │
│ Frame Data: Exception table, constants  │
└─────────────────────────────────────────┘
```

### Method Area / Metaspace (Shared)

```java
// Class metadata store hoti hai
// Java 8 se pehle: PermGen (fixed size)
// Java 8+: Metaspace (auto-growing, native memory)

// Yahan store hota hai:
// - Class structures
// - Method code
// - Constant pool
// - Field and method data
// - Annotations
```

### PC Register (Per Thread)

```java
// Current instruction ka address store karta hai
// JVM kis instruction pe hai yeh track karta hai
// Native method execute ho rahi hai toh undefined hota hai
```

### Native Method Stack

```java
// Native methods (C/C++) ke liye stack
// JNI (Java Native Interface) calls yahan track hote hain
```

---

## Execution Engine

**Matlab:** Bytecode ko execute karne wala component — interpreter + JIT compiler.

### Components

```
Execution Engine
    ├── Interpreter
    ├── JIT Compiler
    └── Garbage Collector
```

### Interpreter

```java
// Bytecode ko line by line execute karta hai
// Fast startup — compile time nahi chahiye
// Slow execution — har baar interpret karta hai

// Example:
// First execution → Interpreter
// Frequent execution (hotspot) → JIT compile karega
```

### JIT Compiler (Just-In-Time)

```java
// Hot methods ko native code mein compile karta hai
// Compilation → Caching → Fast execution
// Adaptive optimization — runtime pe decide karta hai

// Levels:
// C1: Client compiler — fast compilation, less optimization
// C2: Server compiler — slow compilation, aggressive optimization
// Tiered: Dono ka combination
```

### Garbage Collector

```java
// Unused objects ko automatically remove karta hai
// Heap memory manage karta hai
// Different algorithms available hain
```

---

## JIT Compiler

**Matlab:** Bytecode ko native machine code mein convert karta hai — performance boost.

### Why JIT?

```
Interpreter: Fast startup, slow execution
JIT: Slow startup, fast execution

JIT interpretation:
1. Method frequently execute hota hai (hotspot detection)
2. JIT bytecode ko native code mein compile karta hai
3. Compiled code cache mein store hota hai
4. Next calls pe cached native code use hota hai
```

### JIT Optimization Techniques

```java
// 1. Method Inlining
// Small methods ko caller mein inline kar deta hai
int add(int a, int b) { return a + b; }
// int result = add(5, 10); → int result = 5 + 10;

// 2. Dead Code Elimination
// Unused code remove kar deta hai
if (false) {
    System.out.println("Never executed");  // Remove ho jayega
}

// 3. Loop Unrolling
// Loop iterations ko expand kar deta hai
for (int i = 0; i < 3; i++) {
    arr[i] = i;
}
// → arr[0] = 0; arr[1] = 1; arr[2] = 2;

// 4. Constant Folding
// Compile-time constants ko fold kar deta hai
int x = 5 * 10 + 3;  // → int x = 53;

// 5. Escape Analysis
// Object sirf method ke andar use ho raha hai toh stack pe allocate karo
public void method() {
    User user = new User();  // Escape nahi kar raha → stack pe allocate
    user.doSomething();
}
```

### JIT Compilation Levels

```
Level 0: Interpret only (no compilation)
Level 1: C1 only (fast compilation, less optimization)
Level 2: C2 only (slow compilation, aggressive optimization)
Level 3: C1 profiling (collect data for C2)
Level 4: C2 compilation (full optimization)

Default: Tiered compilation (levels 0-4)
```

### Viewing JIT Activity

```bash
# JIT compilation dekho
java -XX:+PrintCompilation MyApplication

# Inlining dekho
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining MyApplication

# Escape analysis dekho
java -XX:+DoEscapeAnalysis -XX:+PrintEscapeAnalysis MyApplication
```

---

## Summary

| Component | Purpose | Key Point |
|-----------|---------|-----------|
| **ClassLoader** | .class files load karna | Parent delegation model |
| **Bootstrap CL** | Core Java classes | C++ written, no parent |
| **Extension CL** | Extension classes | $JAVA_HOME/lib/ext |
| **Application CL** | Application classes | CLASSPATH se load |
| **Heap** | Objects/Arrays store | Shared, GC yahan hoti hai |
| **Stack** | Method frames, locals | Per thread, automatic cleanup |
| **Metaspace** | Class metadata | Java 8+, replaces PermGen |
| **PC Register** | Current instruction | Per thread |
| **Native Stack** | JNI calls | Per thread |
| **Interpreter** | Bytecode execute | Fast startup, slow execution |
| **JIT Compiler** | Native code compile | Slow startup, fast execution |
| **Hotspot** | Frequently executed code | JIT compilation trigger |
