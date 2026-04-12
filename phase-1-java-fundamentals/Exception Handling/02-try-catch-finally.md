# try-catch-finally

## Status: Not Started

---

## Table of Contents

1. [Basic try-catch](#basic-try-catch)
2. [Multiple Catch Blocks](#multiple-catch-blocks)
3. [Multi-Catch](#multi-catch)
4. [finally Block](#finally-block)
5. [try-with-resources](#try-with-resources)
6. [Suppressed Exceptions](#suppressed-exceptions)

---

## Basic try-catch

**Matlab:** Exceptional code ko `try` mein wrap karo aur `catch` mein handle karo.

```java
try {
    // Code jo exception throw kar sakta hai
    int result = 10 / 0;
} catch (ArithmeticException e) {
    // Exception handle karo
    System.out.println("Cannot divide by zero: " + e.getMessage());
}

// Program continues — crash nahi hua
System.out.println("Program continues");
```

### try-catch-finally

```java
try {
    FileInputStream fis = new FileInputStream("file.txt");
    int data = fis.read();
    System.out.println(data);
} catch (FileNotFoundException e) {
    System.out.println("File not found: " + e.getMessage());
} catch (IOException e) {
    System.out.println("IO Error: " + e.getMessage());
} finally {
    // Hamesha run hoga — cleanup ke liye
    System.out.println("Finally block executed");
}
```

---

## Multiple Catch Blocks

**Matlab:** Different exceptions ko alag-alag handle karna — specific pehle, general baad mein.

### Catch Ordering Rule

```java
try {
    // Code
} catch (FileNotFoundException e) {
    // ✅ Specific exception pehle
    System.out.println("File not found");
} catch (IOException e) {
    // ✅ General exception baad mein
    System.out.println("IO error");
} catch (Exception e) {
    // ✅ Sabse general last mein
    System.out.println("General error");
}

// ❌ WRONG ORDER — unreachable catch blocks
try {
    // Code
} catch (Exception e) {
    // Yeh pehle aa gaya — baaki catch blocks unreachable!
} catch (FileNotFoundException e) {  // ❌ Unreachable code!
}
```

### Complete Example

```java
public void processFile(String filename) {
    try {
        FileInputStream fis = new FileInputStream(filename);
        int data = fis.read();
        String result = String.valueOf(data);
        int num = Integer.parseInt(result);
        System.out.println(num);
    } catch (FileNotFoundException e) {
        System.out.println("File not found: " + filename);
    } catch (IOException e) {
        System.out.println("Error reading file: " + e.getMessage());
    } catch (NumberFormatException e) {
        System.out.println("Invalid number format: " + e.getMessage());
    } catch (Exception e) {
        System.out.println("Unexpected error: " + e.getMessage());
    }
}
```

---

## Multi-Catch

**Matlab:** Java 7+ — multiple exceptions ko ek catch block mein handle karna.

### Syntax

```java
try {
    // Code
} catch (FileNotFoundException | IOException e) {
    // Dono exceptions ko same tarike se handle karo
    System.out.println("File error: " + e.getMessage());
}

// ❌ Cannot catch same exception hierarchy
// catch (Exception | IOException e) { }  // ❌ Compilation Error
```

### When to Use Multi-Catch

```java
// ✅ Same handling logic — multi-catch use karo
try {
    Class.forName("com.mysql.cj.jdbc.Driver");
    FileInputStream fis = new FileInputStream("config.properties");
} catch (ClassNotFoundException | FileNotFoundException e) {
    // Dono ka handling same hai
    System.err.println("Configuration error: " + e.getMessage());
    throw new RuntimeException("Failed to load configuration", e);
}

// ✅ Different handling — separate catch blocks use karo
try {
    // Code
} catch (FileNotFoundException e) {
    // File not found — create default
    useDefaultConfig();
} catch (IOException e) {
    // Other IO error — retry
    retryOperation();
}
```

---

## finally Block

**Matlab:** Hamesha run hone wala block — resource cleanup ke liye.

### finally Always Runs

```java
try {
    System.out.println("Try block");
    int result = 10 / 0;  // Exception
} catch (ArithmeticException e) {
    System.out.println("Catch block");
    return;  // Return statement
} finally {
    System.out.println("Finally block");  // ✅ Return ke baad bhi run hoga
}
// Output:
// Try block
// Catch block
// Finally block
```

### When finally Does NOT Run

```java
// ❌ finally nahi run hoga:
// 1. System.exit() call
try {
    System.exit(0);
} finally {
    // Never executed
}

// 2. JVM crash
try {
    // JVM crash (OutOfMemoryError, etc.)
} finally {
    // May not execute
}

// 3. Infinite loop in try
try {
    while (true);  // Infinite loop
} finally {
    // Never reached
}

// 4. Thread killed
try {
    // Thread forcefully stopped
} finally {
    // May not execute
}
```

### finally with Return

```java
// ⚠️ finally mein return mat karo — try/catch ka return override ho jata hai
public int getValue() {
    try {
        return 10;
    } catch (Exception e) {
        return 20;
    } finally {
        return 30;  // ⚠️ Always returns 30 — bad practice!
    }
}

// ✅ finally mein return mat karo — sirf cleanup karo
public int getValue() {
    try {
        return 10;
    } finally {
        // Cleanup only — no return
        System.out.println("Cleanup done");
    }
}
```

---

## try-with-resources

**Matlab:** Java 7+ — AutoCloseable resources automatically close hote hain — finally block ki zarurat nahi.

### Basic Usage

```java
// ❌ Old style — finally mein manually close karo
FileInputStream fis = null;
try {
    fis = new FileInputStream("file.txt");
    int data = fis.read();
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (fis != null) {
        try {
            fis.close();  // ❌ Close bhi exception throw kar sakta hai
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// ✅ New style — try-with-resources
try (FileInputStream fis = new FileInputStream("file.txt")) {
    int data = fis.read();
    System.out.println(data);
} catch (IOException e) {
    e.printStackTrace();
}
// fis automatically close hoga — finally ki zarurat nahi!
```

### Multiple Resources

```java
// Multiple resources — semicolon se separate karo
try (
    FileInputStream fis = new FileInputStream("input.txt");
    FileOutputStream fos = new FileOutputStream("output.txt")
) {
    int data;
    while ((data = fis.read()) != -1) {
        fos.write(data);
    }
} catch (IOException e) {
    e.printStackTrace();
}
// Dono resources automatically close honge
```

### Custom AutoCloseable

```java
class DatabaseConnection implements AutoCloseable {
    private Connection connection;

    public DatabaseConnection(String url) {
        this.connection = DriverManager.getConnection(url);
    }

    public void execute(String sql) {
        // Execute query
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connection closed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// Usage
try (DatabaseConnection db = new DatabaseConnection("jdbc:mysql://localhost:3306/mydb")) {
    db.execute("SELECT * FROM users");
} catch (Exception e) {
    e.printStackTrace();
}
// db.close() automatically called
```

### Effectively Final Variables

```java
// Java 9+ — effectively final variables bhi use kar sakte ho
FileInputStream fis = new FileInputStream("file.txt");
try (fis) {  // Already declared variable
    int data = fis.read();
} catch (IOException e) {
    e.printStackTrace();
}
// fis automatically close hoga
```

---

## Suppressed Exceptions

**Matlab:** Jab try block aur close() dono exception throw karein — close() ka exception suppress ho jaata hai.

### How It Works

```java
try (Resource resource = new Resource()) {
    resource.doWork();  // Exception 1 throw karta hai
} catch (Exception e) {
    // Exception 1 catch mein aayega
    // resource.close() ka exception (Exception 2) suppress ho jayega

    // Suppressed exceptions access karo
    for (Throwable suppressed : e.getSuppressed()) {
        System.out.println("Suppressed: " + suppressed.getMessage());
    }
}
```

### Example

```java
class Resource implements AutoCloseable {
    @Override
    public void close() {
        throw new RuntimeException("Close failed!");
    }

    public void doWork() {
        throw new RuntimeException("Work failed!");
    }
}

try (Resource r = new Resource()) {
    r.doWork();  // RuntimeException: Work failed!
} catch (Exception e) {
    System.out.println("Main: " + e.getMessage());  // "Work failed!"
    for (Throwable suppressed : e.getSuppressed()) {
        System.out.println("Suppressed: " + suppressed.getMessage());  // "Close failed!"
    }
}
// Output:
// Main: Work failed!
// Suppressed: Close failed!
```

### addSuppressed

```java
// Manually suppressed exception add karo
try {
    throw new IOException("Primary error");
} catch (IOException e) {
    try {
        throw new RuntimeException("Cleanup error");
    } catch (RuntimeException cleanupError) {
        e.addSuppressed(cleanupError);  // Manually add karo
    }
    throw e;  // Primary exception with suppressed
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **try-catch** | Exception handle karne ka basic mechanism |
| **Multiple catch** | Specific pehle, general baad mein |
| **Multi-catch** | Java 7+ — `catch (A \| B e)` same handling ke liye |
| **finally** | Hamesha run hoga — cleanup ke liye (except System.exit) |
| **try-with-resources** | AutoCloseable resources automatically close hote hain |
| **Suppressed** | Close() ka exception try block ke exception ke saath attach hota hai |
| **finally mein return** | Avoid karo — try/catch ka return override ho jaata hai |
