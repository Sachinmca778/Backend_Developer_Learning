# Level 1: Core Java Fundamentals (Q1 - Q9)

---

### Q1: What is the difference between JDK, JRE, and JVM?
**Answer (Hinglish):**

Yeh Java architecture ka sabse basic aur favourite interview question hai. Isko samajhne ke liye ek simple relationship yaad rakho:  
`JDK = JRE + Development Tools (compiler, debugger, etc.)`  
`JRE = JVM + Core Libraries (rt.jar / modules)`

```
+-------------------------------------------------------------+
|                           JDK                               |
|  +-------------------------------------+  +---------------+ |
|  |                JRE                  |  |  javac, java, | |
|  |  +-------------------+  +---------+ |  |  javadoc, jdb | |
|  |  |       JVM         |  | Core    | |  |  (Dev Tools)  | |
|  |  | (Executes Bytecode|  | Libs    | |  +---------------+ |
|  |  |  line-by-line)    |  |         | |                    |
|  |  +-------------------+  +---------+ |                    |
|  +-------------------------------------+                    |
+-------------------------------------------------------------+
```

1. **JVM (Java Virtual Machine):**
   - **Kya hai:** Yeh ek abstract machine hai jo Java ke **bytecode (.class file)** ko read aur execute karti hai.
   - **Platform Dependent:** JVM har OS ke liye alag hota hai (Windows JVM, Mac JVM, Linux JVM). Isliye Java "Platform Independent" hai because bytecode same rehta hai, bas use chalane wala JVM machine-specific hota hai (**WORA - Write Once, Run Anywhere**).
   - **Core Parts:** ClassLoader, JVM Memory (Heap, Stack, Metaspace), Execution Engine (JIT Compiler, Interpreter, GC).

2. **JRE (Java Runtime Environment):**
   - **Kya hai:** Yeh wo environment provide karta hai jisme Java program execute ho sake.
   - **Kisko chahiye:** Agar aapko sirf Java app **run** karni hai (code compile ya develop nahi karna), to sirf JRE kaafi hai (e.g., client system).
   - **Isme kya hota hai:** JVM + standard Java class libraries (jaise java.lang, java.util).

3. **JDK (Java Development Kit):**
   - **Kya hai:** Complete software development package jo developers ko Java applications banane, compile karne, aur debug karne ke liye chahiye.
   - **Isme kya hota hai:** JRE + Developer tools (jaise `javac` compiler, `jdb` debugger, `jar` tool, `javadoc`).

> **💡 Quick Interview Tip:**  
> Interviewer pooche: *"Is JVM platform independent?"*  
> **Jawab:** Nahi! JVM **platform-dependent** hai. Bytecode **platform-independent** hota hai.

---

### Q2: What are the main features of Java (OOP principles)?
**Answer (Hinglish):**

Java ke 4 pillars (OOP principles) hote hain:

1. **Encapsulation (Data Hiding):**
   - Data (variables) aur methods ko ek single unit (Class) me bind karna aur fields ko `private` rakh kar `public` getters/setters se access dena.
   - **Fayda:** Data security aur validation control milta hai.
   ```java
   public class BankAccount {
       private double balance; // private field

       public double getBalance() { return balance; }
       public void deposit(double amount) {
           if (amount > 0) this.balance += amount; // validation logic
       }
   }
   ```

2. **Abstraction (Hiding Implementation Details):**
   - User ko sirf *kya karta hai* (essential features) dikhana aur *kaise karta hai* (internal details) chhipana.
   - Java me abstraction **Abstract Classes** (0-100%) aur **Interfaces** (100% pure contract) se achieve hota hai.
   - **Real-world Example:** Car chalate waqt accelerator dabana jaante hain, engine ke andar fuel combustion kaise ho raha hai wo chhipa rehta hai.

3. **Inheritance (Code Reusability):**
   - Ek child class (subclass) parent class (superclass) ke non-private fields aur methods ko inherit karti hai using `extends` keyword.
   - **Relation:** `IS-A` relationship (e.g., `Car IS-A Vehicle`).

4. **Polymorphism (Many Forms):**
   - Ek hi method name ka different situations me alag tarike se behave karna.
   - **Compile-time (Static):** Method Overloading (same method name, different parameter signature in same class).
   - **Runtime (Dynamic):** Method Overriding (subclass parent ke method ko apni specific implementation deti hai using `@Override`).

**Other Core Features of Java:**
- **Simple & Secure:** No explicit pointers, automatic memory management via Garbage Collector.
- **Robust:** Strong type checking, exception handling mechanism.
- **Multithreaded:** Built-in concurrency support.

---

### Q3: What are primitive data types in Java?
**Answer (Hinglish):**

Java me **8 primitive data types** hote hain. Yeh objects nahi hote, directly value ko stack memory me store karte hain. Inka size platform-independent hota hai:

| Data Type | Size | Default Value | Range / Use Case |
| :--- | :--- | :--- | :--- |
| `byte` | 1 byte (8 bits) | `0` | -128 to 127 |
| `short` | 2 bytes (16 bits) | `0` | -32,768 to 32,767 |
| `int` | 4 bytes (32 bits) | `0` | Default integer type (-2^31 to 2^31-1) |
| `long` | 8 bytes (64 bits) | `0L` | Badi values ke liye (suffix `L`) |
| `float` | 4 bytes (32 bits) | `0.0f` | Single-precision decimal (suffix `f`) |
| `double` | 8 bytes (64 bits) | `0.0d` | Double-precision decimal (default for floating point) |
| `char` | 2 bytes (16 bits) | `'\u0000'` | Unicode characters (0 to 65,535) |
| `boolean`| 1 bit (JVM dependent)| `false` | `true` or `false` |

> **💡 Tricky Interview Question:**  
> *"Java me `char` 2 bytes ka kyu hota hai jabki C/C++ me 1 byte ka hota hai?"*  
> **Jawab:** C/C++ sirf ASCII set use karte hain (128-256 characters). Java **Unicode** standard use karta hai taaki worldwide language scripts (Hindi, Chinese, etc.) support ho sakein.

---

### Q4: What is autoboxing and unboxing?
**Answer (Hinglish):**

Java 5 me introduce hua tha primitive types aur unke corresponding Wrapper classes (`Integer`, `Double`, etc.) ke beech automatic conversion ke liye:

1. **Autoboxing:**
   - Primitive type ka uske wrapper class object me automatically convert hona.
   - Example: `int` -> `Integer`
   ```java
   int a = 50;
   Integer obj = a; // Compiler internally converts: Integer.valueOf(a);
   ```

2. **Unboxing:**
   - Wrapper class object ka primitive value me automatically convert hona.
   - Example: `Integer` -> `int`
   ```java
   Integer obj = Integer.valueOf(100);
   int b = obj; // Compiler internally converts: obj.intValue();
   ```

> **⚠️ Common Production Pitfall (NullPointerException):**
> Agar wrapper object `null` hai aur unboxing ho rahi hai, to runtime par `NullPointerException` throw hoga:
> ```java
> Integer count = null;
> int val = count; // Throws NullPointerException! (count.intValue() call hua null pe)
> ```

---

### Q5: What are access modifiers in Java (private, protected, public, default)?
**Answer (Hinglish):**

Java me 4 access levels hote hain jo classes, variables, constructors, aur methods ki visibility decide karte hain:

| Modifier | Same Class | Same Package | Subclass (Different Pkg) | World (Everywhere) |
| :--- | :---: | :---: | :---: | :---: |
| `private` | ✅ Yes | ❌ No | ❌ No | ❌ No |
| `default` (no keyword) | ✅ Yes | ✅ Yes | ❌ No | ❌ No |
| `protected` | ✅ Yes | ✅ Yes | ✅ Yes (via inheritance) | ❌ No |
| `public` | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |

**Summary in Simple Words:**
- `private`: Sirf usi class ke andar accessible.
- `default` (Package-private): Apne poore package ke andar kahin bhi accessible.
- `protected`: Apne package me to accessible hai hi, doosre package ke sirf **child class (subclass)** ke through accessible hai.
- `public`: Pure project me kahin se bhi accessible.

---

### Q6: What is a package in Java?
**Answer (Hinglish):**

- **Definition:** Package Java me related classes, interfaces, aur sub-packages ka ek container (directory folder) hota hai.
- **Example:** `java.util.*`, `java.io.*`, `com.company.service.*`
- **Purpose & Benefits:**
  1. **Namespace Collisions Avoidance:** Agar do alag developers ne `Date` naam ki class banayi, to `java.util.Date` aur `java.sql.Date` alag-alag package me hone ke karan conflict nahi hoga.
  2. **Access Protection:** Package-private (`default`) aur `protected` members ko bundle level par access manage karne deta hai.
  3. **Maintainability:** Code organized rehta hai (e.g. `controller`, `service`, `repository`, `dto`).

---

### Q7: What is a static keyword? Static variables and methods?
**Answer (Hinglish):**

`static` keyword ka matlab hai: **"Class level, NOT Object level"**.  
Jab koi cheez `static` hoti hai, wo object banaye bina memory me load hoti hai aur saare objects ke beech **share** hoti hai.

1. **Static Variable (Class Variable):**
   - Class load hote waqt Metaspace / Class area me memory allocate hoti hai (sirf 1 copy).
   - Sabhi objects usi same variable ko share karte hain. Common counter ya configuration ke liye best hai.
   ```java
   public class Employee {
       static String company = "Google"; // Sabhi employees ke liye common
       String name; // Har employee ka alag
   }
   ```

2. **Static Method:**
   - Object banaye bina direct class name se call kar sakte hain: `ClassName.methodName()`.
   - **Rules:**
     - Static method sirf static variables aur doosre static methods ko direct access kar sakta hai.
     - Static method ke andar `this` aur `super` keywords use **nahi** kar sakte (kyunki `this` current object ko refer karta hai, aur static me koi object context nahi hota).
     - Static methods override nahi hote, balki **method hiding** hoti hai.

3. **Static Block:**
   - Class load hote waqt sabse pehle execute hota hai (constructors se bhi pehle). Static resources ko initialize karne ke kaam aata hai.

---

### Q8: What is a Constructor in Java? Types of constructors?
**Answer (Hinglish):**

- **Definition:** Constructor ek special method jaisa block hota hai jo object creation ke time (`new` keyword ke sath) invoke hota hai. Iska main kaam object ki initial state (instance variables) ko initialize karna hota hai.
- **Rules:**
  1. Constructor ka naam class ke naam ke bilkul same hona chahiye.
  2. Iska koi return type nahi hota (even `void` bhi nahi).

**Types of Constructors:**
1. **Default Constructor (Compiler Provided):**
   - Agar aapne class me koi bhi constructor nahi likha, to Java compiler automatically ek no-argument empty constructor inject kar deta hai.
2. **No-Argument Constructor:**
   - Programmer khud likhta hai jisme koi parameters nahi hote.
3. **Parameterized Constructor:**
   - Arguments accept karta hai taaki object create karte waqt custom values set ki ja sakein.
   ```java
   public class Student {
       int id;
       String name;

       // Parameterized constructor
       public Student(int id, String name) {
           this.id = id;
           this.name = name;
       }
   }
   ```

> **Important Concept:**  
> Agar aapne koi bhi parameterized constructor define kar diya, to Java compiler default constructor provide **nahi** karega! Agar default no-arg constructor chahiye, to manually likhna padega.

---

### Q9: Can we use static methods in a Constructor?
**Answer (Hinglish):**

**Haan, bilkul!** Ham constructor ke andar static methods ko call kar sakte hain.

**Kyu aur Kaise?**
- Kyunki static methods class loading time par hi memory me load ho chuke hote hain. Jab constructor run hota hai, static method already available aur ready to use hota hai.
- Aap direct static method name ya `ClassName.methodName()` use kar sakte ho.

```java
public class Order {
    private String orderId;

    public Order() {
        // Calling static method inside constructor
        this.orderId = generateUniqueId();
    }

    public static String generateUniqueId() {
        return "ORD-" + System.currentTimeMillis();
    }
}
```

> **Reverse Question (Interviewer Trap):**  
> *"Kya constructor ko static bana sakte hain?"*  
> **Jawab:** **Nahi!** Constructor ko `static` nahi bana sakte. Constructor ka purpose hi naye object (instance) ko initialize karna hota hai, jabki `static` ka matlab class-level hota hai. Isliye constructor sirf `public`, `protected`, `private`, ya package-private ho sakta hai.
