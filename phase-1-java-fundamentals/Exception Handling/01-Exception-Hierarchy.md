# Exception Hierarchy

## Status: Not Started

---

## Table of Contents

1. [Throwable Class](#throwable-class)
2. [Error vs Exception](#error-vs-exception)
3. [Checked vs Unchecked Exceptions](#checked-vs-unchecked-exceptions)
4. [Common Exceptions](#common-exceptions)
5. [Exception Hierarchy Diagram](#exception-hierarchy-diagram)

---

## Throwable Class

**Matlab:** Java mein sab errors aur exceptions ka root class — `java.lang.Throwable`.

```java
public class Throwable implements Serializable {
    private String detailMessage;      // Error message
    private Throwable cause;           // Root cause
    private StackTraceElement[] stackTrace;  // Stack trace
}
```

### Throwable Methods

```java
try {
    // Some code
} catch (Exception e) {
    e.getMessage();        // Error message string
    e.getCause();          // Root cause exception
    e.printStackTrace();   // Console pe stack trace print karo
    e.getStackTrace();     // Stack trace array
    e.toString();          // Class name + message
    e.fillInStackTrace();  // Current stack trace fill karo
}
```

---

## Error vs Exception

```
Throwable
    ├── Error (❌ Recover nahi ho sakta)
    │     ├── OutOfMemoryError
    │     ├── StackOverflowError
    │     ├── NoClassDefFoundError
    │     └── AssertionError
    │
    └── Exception (✅ Handle/recover ho sakta hai)
          ├── RuntimeException (Unchecked)
          │     ├── NullPointerException
          │     ├── ArrayIndexOutOfBoundsException
          │     ├── ClassCastException
          │     ├── IllegalArgumentException
          │     ├── IllegalStateException
          │     └── ArithmeticException
          │
          └── Checked Exceptions
                ├── IOException
                ├── SQLException
                ├── ClassNotFoundException
                └── FileNotFoundException
```

### Error — Recover Nahi Ho Sakta

**Matlab:** JVM-level problems — application inhe handle nahi kar sakta.

```java
// ❌ OutOfMemoryError — Heap space khatam
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024 * 1024]);  // 1MB each iteration
}
// Exception in thread "main" java.lang.OutOfMemoryError: Java heap space

// ❌ StackOverflowError — Infinite recursion
public void recurse() {
    recurse();  // Stack frames fill up
}
// Exception in thread "main" java.lang.StackOverflowError

// ❌ NoClassDefFoundError — Class runtime pe nahi mila
// Compile time pe class tha, runtime pe nahi
```

**Rule:** Errors ko catch mat karo — application ko fix karo, handle nahi.

---

## Checked vs Unchecked Exceptions

### Checked Exceptions

**Matlab:** Compile-time pe check hoti hain — handle karna zaruri hai (try-catch ya throws).

```java
// ❌ Compilation Error — handle nahi kiya
public void readFile() {
    FileInputStream fis = new FileInputStream("file.txt");  // ❌ FileNotFoundException handle nahi
}

// ✅ Handle with try-catch
public void readFile() {
    try {
        FileInputStream fis = new FileInputStream("file.txt");
    } catch (FileNotFoundException e) {
        System.out.println("File not found: " + e.getMessage());
    }
}

// ✅ Handle with throws
public void readFile() throws FileNotFoundException {
    FileInputStream fis = new FileInputStream("file.txt");
}
```

### Common Checked Exceptions

```java
// IOException
FileInputStream fis = new FileInputStream("file.txt");  // throws FileNotFoundException
int data = fis.read();  // throws IOException

// SQLException
Connection conn = DriverManager.getConnection(url);  // throws SQLException
Statement stmt = conn.createStatement();

// ClassNotFoundException
Class.forName("com.mysql.cj.jdbc.Driver");  // throws ClassNotFoundException

// InterruptedException
Thread.sleep(1000);  // throws InterruptedException
```

### Unchecked Exceptions (RuntimeException)

**Matlab:** Compile-time pe check nahi hoti — runtime pe throw hoti hain.

```java
// ❌ Runtime exceptions — compiler force nahi karta handle karne ke liye
String str = null;
str.length();  // ❌ NullPointerException

int[] arr = new int[5];
arr[10] = 1;  // ❌ ArrayIndexOutOfBoundsException

Object obj = "Hello";
Integer num = (Integer) obj;  // ❌ ClassCastException

int result = 10 / 0;  // ❌ ArithmeticException: / by zero
```

### When to Use What

```java
// ✅ Use Checked Exception when:
// - Caller ko recover karne ka chance hai
// - External resource problems (file, network, database)
// - Caller ko explicitly handle karna chahiye

public class PaymentGatewayException extends Exception {
    public PaymentGatewayException(String message) {
        super(message);
    }
}

// ✅ Use Unchecked Exception when:
// - Programming error hai (null check, bounds check)
// - Caller recover nahi kar sakta
// - API misuse indicate karta hai

public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) {
        super(message);
    }
}
```

---

## Common Exceptions

### NullPointerException

```java
// ❌ Null reference pe method call
String str = null;
int len = str.length();  // ❌ NPE

// ❌ Null reference pe field access
User user = null;
String name = user.getName();  // ❌ NPE

// ✅ Prevention
if (str != null) {
    int len = str.length();
}

// ✅ Optional use karo
Optional.ofNullable(str).map(String::length).orElse(0);

// ✅ Java 14+ — Better NPE message
// "Cannot invoke "String.length()" because "str" is null"
```

### ArrayIndexOutOfBoundsException

```java
// ❌ Invalid index
int[] arr = new int[5];
arr[5] = 10;  // ❌ Valid indices: 0-4

// ✅ Prevention
for (int i = 0; i < arr.length; i++) {
    arr[i] = i;
}

// ✅ Enhanced for-each
for (int num : arr) {
    System.out.println(num);
}
```

### ClassCastException

```java
// ❌ Invalid cast
Object obj = "Hello";
Integer num = (Integer) obj;  // ❌ String → Integer cast nahi ho sakta

// ✅ Prevention with instanceof
if (obj instanceof Integer) {
    Integer num = (Integer) obj;
}

// ✅ Java 14+ pattern matching
if (obj instanceof Integer num) {
    System.out.println(num);
}
```

### IllegalArgumentException

```java
// ❌ Invalid method argument
public void setAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("Invalid age: " + age);
    }
    this.age = age;
}

// ✅ Caller ko batata hai ki galat value pass ki
```

### IllegalStateException

```java
// ❌ Object ki state method call ke liye suitable nahi
public class Connection {
    private boolean connected = false;

    public void send(String data) {
        if (!connected) {
            throw new IllegalStateException("Not connected");
        }
        // Send data
    }

    public void connect() {
        this.connected = true;
    }
}

Connection conn = new Connection();
conn.send("Hello");  // ❌ IllegalStateException: Not connected
conn.connect();
conn.send("Hello");  // ✅ Works
```

### ArithmeticException

```java
// ❌ Division by zero
int result = 10 / 0;  // ❌ ArithmeticException: / by zero

// ✅ Prevention
int divisor = 0;
if (divisor != 0) {
    int result = 10 / divisor;
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Throwable** | Sab errors/exceptions ka root class |
| **Error** | JVM-level problems — handle nahi ho sakte |
| **Exception** | Handle/recover ho sakte hain |
| **Checked** | Compile-time check — handle karna zaruri |
| **Unchecked** | Runtime exceptions — programming errors |
| **NPE** | Null reference pe operation |
| **ArrayIndexOutOfBounds** | Invalid array index |
| **ClassCast** | Invalid type cast |
| **IllegalArgument** | Invalid method argument |
| **IllegalState** | Object state method ke liye suitable nahi |
