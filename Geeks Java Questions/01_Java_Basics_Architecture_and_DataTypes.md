# Part 1: Java Basics, JVM Architecture & Data Types

---

## 1. Is Java Platform Independent? If then, how?

**Matlab:** Java ek platform-independent programming language hai. Iska matlab hai ki agar aapne Java code kisi ek Operating System (jaise Windows) par likha aur compile kiya, toh aap wahi compiled code kisi dusre Operating System (jaise Linux ya macOS) par bina kisi change ke chala sakte ho. Isse **WORA (Write Once, Run Anywhere)** kehte hain.

### Kaise Kaam Karta Hai? (Internal Architecture)

1. **Source Code Compilation:** Jab aap `.java` file ko `javac` compiler se compile karte ho, toh woh machine code (CPU specific binary code) nahi banata. Iski jagah, woh **Bytecode** (`.class` file) generate karta hai.
2. **Bytecode Role:** Bytecode ek intermediate, highly optimized instruction set hai jo kisi specific hardware ya OS par dependent nahi hota.
3. **JVM Execution:** Bytecode ko run karne ke liye har OS ke paas apna specific **JVM (Java Virtual Machine)** hota hai. Windows ka JVM bytecode ko Windows machine instructions mein translate karta hai, aur Linux ka JVM Linux machine instructions mein.

```

[ Java Code (.java) ] 
         | (javac compiler)
         v
  [ Bytecode (.class) ] ---- Intermediate Code (Platform Independent)
         |
 +-------+-------+
 v       v       v
[Windows [Linux  [macOS
  JVM]    JVM]    JVM]  -- Platform Dependent JVMs
   |       |       |
   v       v       v
[Windows [Linux  [macOS
 Machine]Machine]Machine]
```

> **Important Note:**
> **Key Note:** Java Language aur Bytecode platform-independent hain, lekin JVM itself platform-dependent hoti hai!

---

## 2. What are the top Java Features?

**Matlab:** Java multi-purpose, robust aur secure language hai jo enterprise software development mein sabse widely use hoti hai.

### Important Java Features:

1. **Simple:** Java ka syntax C/C++ se derive hua hai par easy hai. Isme complex features jaise explicit pointers aur operator overloading remove kar diye gaye hain.
2. **Platform Independent:** WORA concept par kaam karta hai (Bytecode + JVM).
3. **Object-Oriented (OOP):** Java mein sab kuch Object aur Class ke around ghoomta hai (Encapsulation, Inheritance, Polymorphism, Abstraction).
4. **Robust (Mazboot):** Automatic Garbage Collection, strong memory management aur Exception Handling features code ko crash hone se bachate hain.
5. **Secure:** Explicit Pointers access na hona, Classloader subsystem aur Security Manager runtime par untrusted code ko execute nahi hone dete.
6. **Multithreaded:** Java built-in multithreading support karta hai jisse ek hi program multiple tasks simultaneously run kar sakta hai (high throughput).
7. **High Performance:** **JIT (Just-In-Time) Compiler** ki wajah se frequently executed bytecode directly native machine code mein convert hota hai, jisse speed badh jaati hai.
8. **Distributed:** RMI (Remote Method Invocation) aur Web Services dwara Java remote network par data aur methods share karne mein madad karta hai.
9. **Dynamic:** Dynamic class loading support karta hai. Classes runtime par demand ke basis pe load hoti hain.

---

## 3. What is JVM? (Java Virtual Machine)

**Matlab:** JVM ek abstract virtual machine hai jo Java Bytecode (`.class` file) ko execute karti hai aur runtime environment provide karti hai.

### Core Architecture of JVM:

1. **Classloader Subsystem:** `.class` files ko load, link aur initialize karta hai.
2. **Runtime Data Areas (JVM Memory Storage):**
   - **Method Area:** Class level structure, static variables aur method metadata.
   - **Heap Area:** Sabhi Objects aur unke instance variables store hote hain.
   - **Stack Area:** Har thread ke liye alag stack banta hai jo local variables aur method call frames (stack frames) hold karta hai.
   - **PC Register:** Currently executing JVM instruction ka address store karta hai.
   - **Native Method Stack:** Native C/C++ code calls ke execution details store karta hai.
3. **Execution Engine:**
   - **Interpreter:** Bytecode ko line-by-line read aur execute karta hai.
   - **JIT Compiler:** Frequently called code ("Hotspots") ko direct machine code mein convert karta hai.
   - **Garbage Collector (GC):** Unreferenced heap objects ko automatically clear karta hai.

---

## 4. What is JIT Compiler? (Just-In-Time Compiler)

**Matlab:** JIT Compiler JVM execution engine ka ek part hai jo Java application ki performance aur speed badhane ke liye use hota hai.

### JIT Compiler Kaise Kaam Karta Hai?

- Purely interpreted languages slow hote hain kyunki har line har baar interpret hoti hai.
- JIT compiler program run hone ke dauran bytecode ko analyze karta hai aur jo methods baar-baar call hote hain (unhe **Hotspots** kehte hain), unhe **Native Machine Code** mein compile kar deta hai.
- Agli baar jab woh method call hota hai, toh JVM interpretation skipped karke direct Machine code run karti hai.

```java
public class JITDemo {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for(int i = 0; i < 100000; i++) {
            calculateSum(i, i + 1); // Repeated method call -> JIT compiles this to native code
        }
        System.out.println("Execution Done in: " + (System.currentTimeMillis() - start) + " ms");
    }

    private static int calculateSum(int a, int b) {
        return a + b;
    }
}
```

---

## 5. What are Memory Storages available with JVM?

**Matlab:** JVM runtime execution ke dauran RAM memory ko 5 primary areas mein divide karta hai:

| Memory Area | Ownership | Stored Data |
|---|---|---|
| **Heap Area** | Shared by all Threads | Sabhi Objects, Arrays aur Instance Variables. |
| **Method Area (Metaspace in Java 8+)** | Shared by all Threads | Class Structure, Static Variables, Constant Pool, Method Code. |
| **JVM Stack Area** | Per Thread | Local Variables, Method Parameters, Return Values (Stack Frames). |
| **PC (Program Counter) Register** | Per Thread | Currently executing instruction ka address. |
| **Native Method Stack** | Per Thread | C/C++ Native methods ke call details. |

---

## 6. What is a ClassLoader in Java?

**Matlab:** ClassLoader JVM ka ek subsystem hai jo `.class` files ko storage (disk/network) se read karke Memory (Method Area) mein load karta hai.

### Types of ClassLoaders (Hierarchy):

1. **Bootstrap ClassLoader:** Sabse top parent loader jo core Java API classes (`rt.jar` / `java.base` module - e.g., `java.lang.Object`, `java.util.String`) load karta hai. Written in C/C++.
2. **Extension / Platform ClassLoader:** `$JAVA_HOME/lib/ext` ya core platform extension packages ko load karta hai.
3. **Application / System ClassLoader:** User ka application classpath (`-classpath` ya `-jar`) se custom classes ko load karta hai.

### ClassLoader Principles:

- **Delegation Hierarchy Principle:** Class loader pehle apne parent loader ko search karne deta hai. Agar parent nahi milti tabhi khud load karta hai.
- **Visibility Principle:** Child ClassLoader parent classes ko dekh sakta hai, par Parent child classes ko nahi dekh sakta.
- **Uniqueness Principle:** Ek class ek hi ClassLoader se dubara load nahi hoti.

---

## 7. Difference between JVM, JRE, and JDK

**Matlab:** Teh teenon Java Environment ke core building blocks hain.

```

+-------------------------------------------------------------+
| JDK (Java Development Kit)                                  |
| +-----------------------------------------+ +-------------+ |
| | JRE (Java Runtime Environment)          | | Development | |
| | +----------------------+ +------------+ | | Tools       | |
| | | JVM                  | | Set of     | | | (javac,     | |
| | | (Executes Bytecode)  | | Libraries  | | |  javadoc,   | |
| | +----------------------+ +------------+ | |  jdb)       | |
| +-----------------------------------------+ +-------------+ |
+-------------------------------------------------------------+
```

| Component | Full Form | Contains | Usage |
|---|---|---|---|
| **JVM** | Java Virtual Machine | Execution Engine, Memory Management, GC | Bytecode ko run karna. |
| **JRE** | Java Runtime Environment | JVM + Core Class Libraries (`rt.jar`) | Code **run/execute** karne ke liye (End users/Servers). |
| **JDK** | Java Development Kit | JRE + Development Tools (`javac`, `jdb`, `javap`) | Code **develop/compile** karne ke liye (Developers). |

---

## 8. What are the differences between Java and C++?

| Feature | Java | C++ |
|---|---|---|
| **Platform Dependency** | Platform Independent (WORA). | Platform Dependent. |
| **Pointers** | Explicit Pointers not supported (Security & Simplicity). | Direct Pointers supported. |
| **Memory Management** | Automatic (Garbage Collection). | Manual (`new` and `delete` operators). |
| **Multiple Inheritance** | Classes mein supported nahi (Interfaces dwara achievable). | Classes mein directly supported. |
| **Operator Overloading** | Not Supported (except `+` string concatenation). | Supported. |
| **Compilation** |` Source Code -> Bytecode -> Native Code`. |` Source Code -> Direct Machine Code`. |
| **Global Variables** | Global variables nahi hote (Class ke andar define hote hain). | Global variables supported hain. |

---

## 9. Explain `public static void main(String args[])` in Java

**Matlab:** Yeh Java application ka mandatory **entry point** method hai jahan se JVM code execution start karta hai.

### Word-by-Word Explanation:

- **`public`:** Access Modifier. JVM is method ko class ke bahar se call karta hai, isliye iska public hona zaroori hai.
- **`static`:** Static Method. JVM ko class ka object create kiye bina method call karne ki permission deta hai (`Test.main()`).
- **`void`:** Return Type. Program finish hone par JVM ko koi value return nahi karta.
- **`main`:** Method Identifier. Standard name jo JVM spec mein defined entry point hai.
- **`String args[]`:** Command Line Arguments. User dwara pass kiye gaye string values ko array format mein accept karta hai.

```java
public class MainExplanation {
    // Valid variations:
    // public static void main(String... args) -> Varargs syntax
    // static public void main(String[] args) -> Order of public & static interchangeable
    public static void main(String[] args) {
        System.out.println("Program Started with " + args.length + " command line arguments.");
    }
}
```

---

## 10. What is Java String Pool? (String Constant Pool - SCP)

**Matlab:** String Constant Pool Heap memory ke andar ek special area hai jahan Java String literals ko store karta hai taaki duplicate String objects na bane aur memory save ho.

### String Creation Mechanisms:

1. **Using Literal (`String s = "Hello";`):** JVM pehle SCP mein search karta hai. Agar `"Hello"` exist karta hai, toh existing reference return hota hai. Agar nahi, toh naya object SCP mein banta hai.
2. **Using `new` Keyword (`String s = new String("Hello");`):** JVM hamesha ek naya object **Heap memory** mein banata hai, chahe woh value SCP mein pehle se ho ya na ho.

```java
public class StringPoolDemo {
    public static void main(String[] args) {
        String s1 = "Java"; // SCP mein bana
        String s2 = "Java"; // SCP se existing reference s1 wala mila
        String s3 = new String("Java"); // Normal Heap memory mein naya object bana

        System.out.println(s1 == s2); // true (Same SCP reference)
        System.out.println(s1 == s3); // false (Heap vs SCP address)
        System.out.println(s1.equals(s3)); // true (Content evaluation)
    }
}
```

---

## 11. What will happen if we don't declare main method as static?

**Matlab:** Code compile successfully ho jayega bina kisi error ke (`javac Main.java` succeeds). Lekin jab aap application run karoge (`java Main`), toh runtime par JVM throw karega:
`NoSuchMethodError: main` ya `Error: Main method is not static in class...`

**Reason:** Static hona zaroori hai kyunki JVM class load karte hi bina object instantiate kiye direct method invoke karti hai (`ClassName.main()`).

---

## 12. What are Packages in Java?

**Matlab:** Package ek folder structure/namespace hierarchy hai jo related Classes, Interfaces aur Sub-packages ko bundle karne ke liye use hoti hai.

### Example:

`package com.app.services;`

---

## 13. Why Packages are used?

1. **Avoid Naming Conflicts:** Dusre developers ke dwara banayi gayi class name agar same ho (e.g., `java.util.Date` vs `java.sql.Date`), toh packages unhe uniquely identify karte hain.
2. **Access Control:** Package-private (default) access specify karke classes aur members ko restricted access diya ja sakta hai.
3. **Organized Codebase:** Code maintainability aur searching aasan ho jaati hai.

---

## 14. What are the advantages of Packages in Java?

- **Reusability:** Packages ko JAR files mein export karke dusre projects mein import kiya ja sakta hai.
- **Encapsulation & Security:** Package level access protection rules set hote hain.
- **Maintainability:** Large enterprise applications modular structures mein divided rahte hain.

---

## 15. How many types of packages are there in Java?

1. **Built-in Packages:** Java Framework ke saath aate hain (`java.lang`, `java.util`, `java.io`, `java.sql`, `java.net`).
2. **User-Defined Packages:** Developers dwara application requirements ke aadhar par banaye jaate hain (`com.company.project.module`).

---

## 16. Explain different Data Types in Java.

**Matlab:** Data Types variable dwara hold karne wale data ka nature aur memory allocation size define karte hain.

```

                    Data Types in Java
                            |
         +------------------+------------------+
         v                                     v
Primitive Data Types                   Non-Primitive (Reference)
(8 Standard Built-in Types)            (Objects, Classes, Arrays)
  - Boolean: boolean                     - String
  - Numeric:                             - Array
      • Integer: byte, short, int, long  - Class / Object
      • Floating: float, double          - Interface
      • Character: char
```

---

## 17. When is a `byte` data type used?

**Matlab:** `byte` ek 8-bit signed two's complement integer hai jiski range **-128 se 127** hoti hai.

### Use Cases:

- Memory saving ke liye jab hum large arrays deal kar rahe hon aur values small integer range mein fixed hon.
- **Raw Byte I/O Streams:** Files read/write karte waqt (Images, Audio, Network Streams).

---

## 18. Can we declare Pointers in Java?

**Matlab:** Nahi, Java developers ko explicit pointers declare ya access karne ki permission **nahi** deta.

### Kyun Nahi Deta?

1. **Security:** Direct memory address manipulation se buffer overflows, memory leaks aur unauthorized memory access security leaks ho sakte hain.
2. **Simplicity & Safety:** Java memory management Garbage Collector ke dwaara control hoti hai.

---

## 19. What is the default value of `byte` data type in Java?

**Matlab:** Instance/Static level par `byte` data type ki default value **`0`** hoti hai.

---

## 20. What is the default value of `float` and `double` data type in Java?

- **`float` default value:** `0.0f`
- **`double` default value:** `0.0d` (ya `0.0`)

---

## 21. What is a Wrapper class in Java?

**Matlab:** Wrapper class primitive data types (`int`, `char`, `float` etc.) ko **Object** ke roop mein encapsulate/wrap karti hai.

| Primitive Type | Corresponding Wrapper Class |
|---|---|
| `byte` | `Byte` |
| `short` | `Short` |
| `int` | `Integer` |
| `long` | `Long` |
| `float` | `Float` |
| `double` | `Double` |
| `char` | `Character` |
| `boolean` | `Boolean` |

---

## 22. Why do we need Wrapper Classes?

1. **Collections Framework:** Java Collections (`ArrayList`, `HashMap`, `HashSet`) sirf Objects store kar sakte hain, primitive data types nahi (`ArrayList<Integer>` valid hai, `ArrayList<int>` invalid hai).
2. **Utility Methods:** Type conversions ke for utility methods offer karte hain (`Integer.parseInt("123")`, `Double.valueOf()`).
3. **Null Values Support:** Primitives null value hold nahi kar sakte, par Wrapper Objects `null` hold kar sakte hain (Database operations/ORMs ke liye useful).
4. **Autoboxing & Unboxing:** Java automatically primitives aur wrappers ke beech conversion manage karti hai.

---

## 23. Differentiate between Instance and Local Variables.

| Property | Instance Variable | Local Variable |
|---|---|---|
| **Declaration Scope** | Class ke andar par kisi method/block ke bahar. | Method, Constructor ya Code Block ke andar. |
| **Memory Location** | **Heap Memory** (Object ka part hota hai). | **Stack Memory** (Method Stack frame ka part). |
| **Lifecycle** | Jab Object create hota hai tab banta hai, GC hone par destroy hota hai. | Method execution start hone par banta hai, end hone par destroy ho jata hai. |
| **Default Values** | JVM automatically default value deta hai (`0`, `null`, `false`). | Koi default value nahi hoti. Mandatory hai ki use karne se pehle initialize karo. |
| **Access Modifiers** | Modifiers apply ho sakte hain (`public`, `private`, `protected`). | Access modifiers apply nahi hote (sirf `final` scope valid hai). |

---

## 24. What are the default values assigned to variables and instances in Java?

Default values instance aur static fields par assign hoti hain:

- `byte`, `short`, `int`: `0`
- `long`: `0L`
- `float`: `0.0f`
- `double`: `0.0d`
- `char`: `' '` (Null character)
- `boolean`: `false`
- Object References (`String`, `Integer`, Custom Objects, Arrays): **`null`**

---

## 25. What is a Class Variable? (Static Variable)

**Matlab:** `static` keyword ke saath declare kiya gaya variable **Class Variable** hota hai. Yeh individual objects se nahi, balki poori Class se belong karta hai.

```java
public class Employee {
    String empName; // Instance variable (har employee ka alag name)
    static String companyName = "TechCorp"; // Class variable (sab ke liye single shared copy)
}
```

---

## 26. What is the default value stored in Local Variables?

**Matlab:** Local Variables mein **KOI default value nahi hoti**. Agar aap local variable ko initialize kiye bina read/use karne ki koshish karoge, toh compiler raise karega:
`java: variable x might not have been initialized`

---

## 27. Explain the difference between Instance Variable and Class Variable.

| Feature | Instance Variable | Class Variable (`static`) |
|---|---|---|
| **Storage Memory** | Heap (Har object ki apni copy hoti hai). | Method Area / Metaspace (Poori class ke liye sirf **1 shared copy**). |
| **Access Way** | Object Reference se (`emp.name`). | Class Name se (`Employee.companyName`). |
| **Memory Allocation** | Every time `new Class()` is called. | Class load hone par ek baar. |

---

## 28. How to create an Object in Java?

Java mein Objects create karne ke different tarike:

1. **Using `new` Keyword (Most Common):**
   `Student s = new Student();`
2. **Using `Class.forName().newInstance()` / `Constructor.newInstance()` (Reflection API):**
   `Student s = (Student) Class.forName("com.app.Student").getDeclaredConstructor().newInstance();`
3. **Using `clone()` Method:**
   `Student s2 = (Student) s1.clone();`
4. **Using Deserialization:**
   `ObjectInputStream in = new ObjectInputStream(fileInputStream); Student s = (Student) in.readObject();`

---

## 29. What is Command Line Argument in Java?

**Matlab:** Command Line Arguments woh parameters ya values hain jo program run karte waqt console/terminal se main method ko pass kiye jaate hain.

```bash
java MyProgram arg1 arg2 100
```

Yeh arguments `main(String[] args)` method mein `args[0] = "arg1"`, `args[1] = "arg2"`, `args[2] = "100"` ke form mein accessible hote hain.

---

## 30. What do you understand by an IO stream?

**Matlab:** I/O (Input/Output) Stream Java mein data ka **continuous flow (stream of data)** hai jo kisi source (Keyboard, File, Network Socket) se read karta hai ya kisi destination (File, Console, Network) par write karta hai.

- **Input Stream:** Source se application mein data read karne ke liye.
- **Output Stream:** Application se destination par data write karne ke liye.

---

## 31. What is the Difference Between Reader/Writer class hierarchy and InputStream/OutputStream class hierarchy?

| Property | Reader / Writer Hierarchy | InputStream / Outputstream Hierarchy |
|---|---|---|
| **Data Format** | **Character-based** (16-bit Unicode characters). | **Byte-based** (8-bit raw binary data). |
| **Target Data** | Text Files, Plain text, Strings, Character Streams. | Binary Files, Images, Videos, Audio, Compressed Zip files. |
| **Encoding** | Automatic Character Encoding/Decoding handling. | No character translation (raw bytes). |
| **Base Classes** | `java.io.Reader`, `java.io.Writer`. | `java.io.InputStream`, `java.io.OutputStream`. |

---

## 32. What are the super most classes for all streams in Java?

1. **Byte Streams Base Classes:**
   - `java.io.InputStream` (For byte input operations)
   - `java.io.OutputStream` (For byte output operations)
2. **Character Streams Base Classes:**
   - `java.io.Reader` (For character input operations)
   - `java.io.Writer` (For character output operations)

---

## 33. What are `FileInputStream` and `FileOutputStream`?

- **`FileInputStream`:** Ek Byte Input Stream class hai jo file se raw byte data (binary data like images/media files) read karne ke liye use hoti hai.
- **`FileOutputStream`:** Ek Byte Output Stream class hai jo byte-by-byte data file mein write karne ke liye use hoti hai.

```java
import java.io.*;

public class FileStreamDemo {
    public static void main(String[] args) throws IOException {
        // Copying an image/file byte by byte
        try (FileInputStream in = new FileInputStream("input.png");
             FileOutputStream out = new FileOutputStream("output.png")) {
            int byteData;
            while ((byteData = in.read()) != -1) {
                out.write(byteData);
            }
        }
    }
}
```

---

## 34. What is the purpose of using `BufferedInputStream` and `BufferedOutputStream` classes?

**Matlab:** Direct disk I/O operations slow hote hain kyunki har `read()` ya `write()` call OS level disk fetch activate karti hai.

`BufferedInputStream` aur `BufferedOutputStream` internal **Memory Buffer (byte array)** maintain karte hain:
- Reading ke waqt, ek saath bada block (e.g., 8KB) disk se RAM buffer mein read kar liya jata hai, jisse multiple physical read operations reduced ho jaate hain.
- Performance **significantly high** ho jaati hai.

---

## 35. What are FilterStreams?

**Matlab:** `FilterInputStream` aur `FilterOutputStream` decorator streams hoti hain jo kisi existing input/output stream ke uper wrap hokar usme extra capabilities (Jaise buffering, data conversion, encryption) add karti hain.

**Examples:** `BufferedInputStream`, `DataInputStream` (primitives read/write ke liye).

---

## 36. What is an I/O filter?

**Matlab:** I/O Filter ek stream wrapper object hota hai jo primary stream se read hone wale data ko transform ya modify karta hai (e.g., Compression, Encryption, Data parsing) pehle isse application ko handing over se pehle.

---

## 37. How many ways can you take input from the console in Java?

Java mein Console input ne ke main 4 tarike hain:

1. **`Scanner` Class (`java.util.Scanner`):** Easy parsing for primitives and tokens (`nextInt()`, `nextLine()`).
2. **`BufferedReader` Class (`java.io.BufferedReader` + `InputStreamReader`):** Fast, synchronized character reader.
3. **`Console` Class (`System.console()`):** Passwords securely read karne ke liye (Echoing disable ho jati hai).
4. **Command Line Arguments (`main(String[] args)`):** Application execution call ke time.

---

## 38. Difference in the use of `print`, `println`, and `printf` in Java.

- **`System.out.print()`:** Output screen par print karta hai aur cursor **same line** par rehta hai.
- **`System.out.println()`:** Output screen par print karta hai aur cursor ko **next new line** par move kar deta hai.
- **`System.out.printf()`:** Formatted string printing ke liye C-style placeholders accept karta hai (`%d`, `%s`, `%f`).

---

## 39. What are Operators in Java?

**Matlab:** Operators special symbols hote hain jo variables aur values (operands) par mathematical, logical ya bitwise calculations carry out karte hain.

---

## 40. How many types of operators are available in Java?

1. **Arithmetic Operators:** `+`, `-`, `*`, `/`, `%`
2. **Unary Operators:** `++`, `--`, `+`, `-`, `!`
3. **Assignment Operators:** `=`, `+=`, `-=`, `*=`, `/=`, `%=`
4. **Relational Operators:** `==`, `!=`, `>`, `<`, `>=`, `<=`
5. **Logical Operators:** `&&` (Short-circuit AND), `||` (Short-circuit OR), `!`
6. **Ternary Operator:** `variable = (condition) ? value_if_true : value_if_false;`
7. **Bitwise Operators:** `&`, `|`, `^` (XOR), `~` (Complement)
8. **Shift Operators:** `<<` (Left shift), `>>` (Signed right shift), `>>>` (Unsigned right shift)
9. **Instanceof Operator:** `object instanceof ClassName`

---

## 41. Explain the difference between `>>` and `>>>` operators.

| Operator | Full Name | Behavior with Negative Numbers | Leftmost Bit Filling |
|---|---|---|---|
| **`>>`** | **Signed Right Shift** | Sign bit (MSB) maintain karta hai. | Negative numbers ke liye `1` se fill karta hai, positive ke liye `0` se. |
| **`>>>`** | **Unsigned Right Shift** | Sign ignore karke purely bit pattern shift karta hai. | Hamesha **`0`** se fill karta hai (Negative number positive integer ban jata hai). |

```java
public class ShiftDemo {
    public static void main(String[] args) {
        int a = -16;
        System.out.println("a >> 1: " + (a >> 1));   // Output: -8
        System.out.println("a >>> 1: " + (a >>> 1)); // Output: 2147483640 (Large positive integer)
    }
}
```

---

## 42. Which Java operator is right associative?

**Matlab:** Java mein **Assignment Operators** (`=`, `+=`, `-=`, `*=`, `/=`) aur **Ternary Operator** (`? :`) **Right-to-Left Associative** hote hain.

```java
int a, b, c;
a = b = c = 50; // Evaluated from right to left: c = 50 -> b = c -> a = b
```

---

## 43. What is the Dot operator (`.`) in Java?

**Matlab:** Dot operator (`.`) ka use kisi Class ya Object ke members (fields, methods, nested classes) ko access karne ke liye aur Packages se Sub-packages/Classes ko import/reference karne ke liye kiya jata hai (`System.out.println()`).

---

## 44. What is Covariant Return Type in Java?

**Matlab:** Method Overriding karte waqt subclass ka overridden method parent class ke method dwara specified return type ka **sub-type** return kar sakta hai. Isse **Covariant Return Type** kehte hain (Introduced in Java 5).

```java
class SuperClass {
    Object getDetails() {
        return "Parent Details";
    }
}

class SubClass extends SuperClass {
    @Override
    String getDetails() { // Covariant Return Type: String is a subclass of Object
        return "Child Specific String Details";
    }
}
```

---

## 45. What is the `transient` keyword in Java?

**Matlab:** `transient` keyword field/variable level modifier hai jo Java **Serialization** mein use hota hai. 

Jab kisi variable ko `transient` mark kiya jata hai, toh Serialization process (object ko byte stream mein write karte waqt) us field ki state/value ko ignore kar deta hai, aur deserialization par default value assign ho jati hai.

```java
import java.io.Serializable;

public class UserSession implements Serializable {
    private String username;
    private transient String password; // Will NOT be saved to file/network stream
}
```
