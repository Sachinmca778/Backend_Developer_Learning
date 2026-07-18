# Exception Handling - Top 10 Interview Questions & Answers

> Covering: Exception Hierarchy, try-catch-finally, Custom Exceptions, Checked vs Unchecked, try-with-resources, throw vs throws, Multi-catch, Suppressed Exceptions, and Best Practices

---

## Q1: Java Exception Hierarchy — Throwable, Error, Exception explain karo.

**Answer:**

Java mein sab errors aur exceptions ka root class **`java.lang.Throwable`** hai.

```
Throwable
    ├── Error (❌ Recover nahi ho sakta — JVM level)
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

### Throwable Key Methods

```java
try {
    riskyOperation();
} catch (Exception e) {
    e.getMessage();        // Error message
    e.getCause();          // Root cause exception
    e.printStackTrace();   // Stack trace print
    e.getStackTrace();     // Stack trace array
    e.toString();          // ClassName: message
}
```

### Error — Handle Mat Karo

```java
// OutOfMemoryError — heap space khatam
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024 * 1024]);  // 1MB each
}

// StackOverflowError — infinite recursion
public void recurse() {
    recurse();  // Stack overflow
}
```

**Important:**

- **Error** = JVM-level problems — catch karke fix nahi kar sakte, application design fix karo
- **Exception** = application-level — handle ya recover kar sakte ho
- **RuntimeException** = Unchecked — programming mistakes

**Short Interview Answer:**

Throwable → Error (JVM, unrecoverable) + Exception (handleable). Exception → RuntimeException (unchecked) + Checked (IOException, SQLException). Errors mat catch karo.

---

## Q2: Checked vs Unchecked exceptions — difference aur kab kaunsa use karte hain?

**Answer:**

| Feature | Checked Exception | Unchecked Exception |
|---------|-------------------|---------------------|
| **Extends** | `Exception` (not RuntimeException) | `RuntimeException` |
| **Compile-time check** | ✅ Yes — handle mandatory | ❌ No |
| **Handle how** | try-catch ya throws | Optional |
| **Represents** | Recoverable external failures | Programming errors |
| **Examples** | IOException, SQLException | NPE, IllegalArgumentException |

### Checked Exception — Compile-time Force

```java
// ❌ Compilation Error — handle nahi kiya
public void readFile() {
    FileInputStream fis = new FileInputStream("file.txt");
}

// ✅ try-catch
public void readFile() {
    try {
        FileInputStream fis = new FileInputStream("file.txt");
    } catch (FileNotFoundException e) {
        System.out.println("File not found: " + e.getMessage());
    }
}

// ✅ throws — caller handle karega
public void readFile() throws FileNotFoundException {
    FileInputStream fis = new FileInputStream("file.txt");
}
```

### Unchecked Exception — No Compile Force

```java
String str = null;
str.length();  // NullPointerException — compiler force nahi karta handle karne ke liye

int[] arr = new int[5];
arr[10] = 1;  // ArrayIndexOutOfBoundsException

int result = 10 / 0;  // ArithmeticException
```

### When to Use What

```
Checked Exception use karo jab:
✅ Caller recover kar sakta hai (retry, fallback)
✅ External resource problem (file, network, DB)
✅ Caller ko explicitly handle karna chahiye

Unchecked Exception use karo jab:
✅ Programming error hai (null check miss, invalid arg)
✅ Caller recover nahi kar sakta
✅ API misuse indicate karta hai
✅ Validation failure
```

### Custom Exception Choice

```java
// Checked — caller must handle
public class PaymentFailedException extends Exception {
    public PaymentFailedException(String message) { super(message); }
}

// Unchecked — optional handle
public class InvalidInputException extends RuntimeException {
    public InvalidInputException(String message) { super(message); }
}
```

**Important:**

- Modern frameworks (Spring) mostly **unchecked exceptions prefer** karte hain — cleaner code
- Checked exceptions **method signature pollute** karte hain — har caller pe burden
- **Effective Java** (Joshua Bloch) suggests: unchecked prefer for most application exceptions

**Short Interview Answer:**

Checked = compile-time mandatory handle (IOException). Unchecked = RuntimeException, no compile force (NPE). Checked for recoverable external failures; unchecked for programming/validation errors.

---

## Q3: Error vs Exception — kya difference hai? Errors catch karni chahiye?

**Answer:**

| Aspect | Error | Exception |
|--------|-------|-----------|
| **Severity** | Critical JVM failure | Application-level problem |
| **Recoverable** | ❌ Generally no | ✅ Yes |
| **Catch?** | ❌ Avoid | ✅ Yes |
| **Examples** | OutOfMemoryError, StackOverflowError | IOException, NPE |
| **Fix** | JVM/config/code redesign | try-catch, validation, retry |

### Error Examples

```java
// OutOfMemoryError
byte[] huge = new byte[Integer.MAX_VALUE];  // Heap exhausted

// StackOverflowError
void infinite() { infinite(); }

// NoClassDefFoundError — compile time pe class tha, runtime pe missing
// AssertionError — assert statement fail
```

### Exception Examples

```java
// Recoverable — retry possible
try {
    connection = DriverManager.getConnection(url);
} catch (SQLException e) {
    retryConnection();  // Recover attempt
}

// Programming error — fix code
if (user == null) {
    throw new IllegalArgumentException("User cannot be null");
}
```

### Kya Errors Catch Karni Chahiye?

```java
// ❌ Generally BAD — mask serious problems
try {
    processLargeData();
} catch (OutOfMemoryError e) {
    // Kya karoge? Memory nahi hai — catch se fix nahi hoga
}

// ✅ Better — prevent errors through design
// - Proper memory management
// - Avoid infinite recursion
// - Increase heap if needed (-Xmx)
// - Use iterative instead of deep recursion
```

**Important:**

- **Errors catch mat karo** except very specific cases (logging before shutdown)
- **OutOfMemoryError catch karke continue** = dangerous — unpredictable state
- **Exception** = normal exception handling flow

**Short Interview Answer:**

Error = JVM catastrophic failure, unrecoverable (OOM, StackOverflow). Exception = application issue, handleable. Don't catch Errors — fix root cause instead.

---

## Q4: try-catch-finally block kaise kaam karta hai? Execution order kya hai?

**Answer:**

### Basic Structure

```java
try {
    // Risky code — exception throw ho sakta hai
    int result = 10 / 0;
} catch (ArithmeticException e) {
    // Exception handle karo
    System.out.println("Cannot divide by zero");
} finally {
    // Hamesha execute hoga (almost always)
    System.out.println("Cleanup done");
}
```

### Execution Flow

```
Case 1: No exception
  try → finally → continue

Case 2: Exception caught
  try → catch → finally → continue

Case 3: Exception not caught
  try → finally → exception propagates up

Case 4: return in try/catch
  try → return → finally → then actual return
```

### finally Always Runs (Almost)

```java
try {
    System.out.println("Try");
    return;  // Return statement
} catch (Exception e) {
    System.out.println("Catch");
} finally {
    System.out.println("Finally");  // ✅ Return ke baad bhi run hoga!
}
// Output: Try → Finally
```

### When finally Does NOT Run

```java
// 1. System.exit()
try { System.exit(0); } finally { /* never runs */ }

// 2. JVM crash (fatal Error)
// 3. Infinite loop in try
// 4. Thread killed forcefully
```

### Multiple Catch Blocks — Ordering Rule

```java
try {
    // code
} catch (FileNotFoundException e) {
    // ✅ Specific pehle
} catch (IOException e) {
    // ✅ General baad mein
} catch (Exception e) {
    // ✅ Sabse general last
}

// ❌ WRONG — unreachable catch
try { } catch (Exception e) { } catch (IOException e) { }  // Compile error!
```

### ⚠️ finally mein return mat karo

```java
public int getValue() {
    try {
        return 10;
    } finally {
        return 30;  // ⚠️ Always returns 30 — BAD!
    }
}
```

**Important:**

- **Specific catch pehle**, general baad mein — warna unreachable code
- **finally = cleanup** — resources close, locks release
- **finally mein return avoid** karo — try/catch ka return override ho jaata hai

**Short Interview Answer:**

try = risky code. catch = handle exception. finally = always runs (cleanup). Order: specific catches first. finally runs even with return. Don't return from finally.

---

## Q5: Multi-catch (Java 7+) kya hai? Rules aur use cases?

**Answer:**

**Multi-catch** = ek hi catch block mein **multiple exception types** handle karna — code duplication kam hoti hai.

### Syntax

```java
try {
    Class.forName("com.mysql.cj.jdbc.Driver");
    FileInputStream fis = new FileInputStream("config.properties");
} catch (ClassNotFoundException | FileNotFoundException e) {
    // Dono exceptions same tarike se handle
    System.err.println("Configuration error: " + e.getMessage());
}
```

### Rules

```java
// ✅ Same handling logic — multi-catch use karo
catch (IOException | SQLException e) {
    log.error("Resource error", e);
}

// ❌ Parent-child same multi-catch mein — compile error
catch (Exception | IOException e) { }  // ❌ IOException is subclass of Exception

// ❌ Variable reuse — multi-catch variable effectively final
catch (IOException | SQLException e) {
    // e = new IOException();  // ❌ Cannot reassign
}
```

### Multi-catch vs Separate Catches

```java
// ✅ Same handling — multi-catch (DRY)
try {
    loadConfig();
} catch (ClassNotFoundException | FileNotFoundException e) {
    useDefaultConfig();
    log.warn("Config load failed, using defaults", e);
}

// ✅ Different handling — separate catches
try {
    processFile();
} catch (FileNotFoundException e) {
    createDefaultFile();  // Specific recovery
} catch (IOException e) {
    retryOperation();     // Different recovery
}
```

### With Exception Chaining

```java
try {
    riskyOperation();
} catch (IOException | SQLException e) {
    throw new ServiceException("Operation failed", e);  // Wrap and rethrow
}
```

**Important:**

- Multi-catch mein exceptions **unrelated** honi chahiye (no inheritance overlap)
- Catch variable **effectively final** hai — reassign nahi kar sakte
- **Same handling logic** ho tab use karo — different logic ke liye separate catches

**Short Interview Answer:**

Multi-catch = `catch (A | B e)` — one block for multiple exceptions with same handling. Rules: no parent-child in same multi-catch, variable effectively final. Use when handling is identical.

---

## Q6: try-with-resources kya hai? finally block se kaise better hai?

**Answer:**

**try-with-resources** (Java 7+) = `AutoCloseable` resources **automatically close** hote hain — manual finally cleanup ki zarurat nahi.

### Old Style — finally mein Manual Close

```java
FileInputStream fis = null;
try {
    fis = new FileInputStream("file.txt");
    int data = fis.read();
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (fis != null) {
        try {
            fis.close();  // ❌ Close bhi exception throw kar sakta hai!
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### try-with-resources — Clean & Safe

```java
try (FileInputStream fis = new FileInputStream("file.txt")) {
    int data = fis.read();
    System.out.println(data);
} catch (IOException e) {
    e.printStackTrace();
}
// fis.close() automatically called — even if exception aaya!
```

### Multiple Resources

```java
try (
    FileInputStream fis = new FileInputStream("input.txt");
    FileOutputStream fos = new FileOutputStream("output.txt")
) {
    int data;
    while ((data = fis.read()) != -1) {
        fos.write(data);
    }
}
// Dono resources automatically close — reverse order mein
```

### Custom AutoCloseable

```java
class DatabaseConnection implements AutoCloseable {
    private Connection connection;

    public DatabaseConnection(String url) throws SQLException {
        this.connection = DriverManager.getConnection(url);
    }

    public void execute(String sql) throws SQLException {
        connection.createStatement().execute(sql);
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            System.out.println("Connection closed");
        }
    }
}

try (DatabaseConnection db = new DatabaseConnection(url)) {
    db.execute("SELECT * FROM users");
}
```

### finally vs try-with-resources

| Aspect | finally block | try-with-resources |
|--------|--------------|-------------------|
| Manual close | ✅ Required | ❌ Automatic |
| Close exception | Separate try-catch | Suppressed exception |
| Code verbosity | High | Low |
| Null check | Manual | Built-in |
| Multiple resources | Nested try-catch | Semicolon separated |

### Java 9+ — Effectively Final Variable

```java
FileInputStream fis = new FileInputStream("file.txt");
try (fis) {  // Already declared variable use kar sakte ho
    int data = fis.read();
}
```

**Important:**

- Resource **`AutoCloseable` implement** karna chahiye
- Resources **close reverse order** mein hote hain (last declared pehle close)
- **Hamesha try-with-resources prefer** karo AutoCloseable ke liye

**Short Interview Answer:**

try-with-resources auto-closes AutoCloseable resources. Cleaner than finally. Multiple resources supported. Close exceptions become suppressed. Always prefer over manual finally for resources.

---

## Q7: `throw` vs `throws` — difference aur usage?

**Answer:**

| Keyword | Purpose | Where Used |
|---------|---------|------------|
| **`throw`** | Exception **explicitly throw** karo | Method body ke andar |
| **`throws`** | Method **declare** karta hai ki exception throw ho sakta hai | Method signature mein |

### throw — Explicitly Throw Exception

```java
public void setAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("Invalid age: " + age);
    }
    this.age = age;
}

public void processPayment(double amount) throws PaymentException {
    if (amount <= 0) {
        throw new PaymentException("Amount must be positive");
    }
    // payment logic
}
```

### throws — Declare in Method Signature

```java
// Caller ko batata hai ki yeh method exception throw kar sakta hai
public void readFile(String path) throws FileNotFoundException, IOException {
    FileInputStream fis = new FileInputStream(path);
    fis.read();
}

// Caller MUST handle ya declare throws
public void caller() {
    try {
        readFile("data.txt");
    } catch (IOException e) {
        log.error("Read failed", e);
    }
}
```

### throw vs throws — Complete Example

```java
// Service layer — throws declare karo
public User findUser(Long id) throws UserNotFoundException {
    User user = repository.findById(id);
    if (user == null) {
        throw new UserNotFoundException("User not found: " + id);  // throw
    }
    return user;
}

// Controller layer — handle karo
public ResponseEntity<User> getUser(Long id) {
    try {
        User user = userService.findUser(id);
        return ResponseEntity.ok(user);
    } catch (UserNotFoundException e) {
        return ResponseEntity.notFound().build();
    }
}
```

### throws with Inheritance

```java
class Parent {
    void method() throws IOException { }
}

class Child extends Parent {
    @Override
    void method() throws FileNotFoundException { }  // ✅ Narrower exception OK
    // void method() throws Exception { }            // ❌ Broader — not allowed
}
```

**Important:**

- **`throw`** = action (exception create + throw)
- **`throws`** = declaration (caller ko inform)
- Unchecked exceptions ke liye **`throws` declare karna optional** hai
- Override mein **same ya narrower** exception declare kar sakte ho

**Short Interview Answer:**

`throw` = actually throw exception in method body. `throws` = declare checked exceptions in method signature. throw is action, throws is declaration. Caller must handle declared checked exceptions.

---

## Q8: Custom exceptions kaise design karte hain? Best practices?

**Answer:**

### Basic Custom Exception

```java
// Checked
public class PaymentFailedException extends Exception {
    public PaymentFailedException(String message) {
        super(message);
    }
    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Unchecked
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### Custom Fields — Rich Exception Info

```java
public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final String details;

    public BusinessException(String errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public String getDetails() { return details; }
}

// Usage
throw new BusinessException("PAY_001", "Payment timeout", "Gateway: Stripe, 30s");
```

### Validation Exception with Field Errors

```java
public class ValidationException extends RuntimeException {
    private final List<String> errors;

    public ValidationException(List<String> errors) {
        super("Validation failed: " + errors.size() + " errors");
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() { return errors; }
}

List<String> errors = new ArrayList<>();
if (name == null) errors.add("Name required");
if (age < 18) errors.add("Must be 18+");
if (!errors.isEmpty()) throw new ValidationException(errors);
```

### Exception Chaining — Preserve Root Cause

```java
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);  // Root cause preserve
    }
}

public User findById(Long id) {
    try {
        return jdbcTemplate.queryForObject(sql, User.class, id);
    } catch (SQLException e) {
        throw new DataAccessException("Failed to fetch user: " + id, e);
    }
}
```

### Exception Hierarchy Design

```
AppException (RuntimeException)
    ├── ValidationException
    ├── ResourceNotFoundException
    ├── AccessDeniedException
    └── PaymentException
          ├── PaymentGatewayException
          └── InsufficientFundsException
```

### Design Best Practices

```
✅ Meaningful class name — UserNotFoundException, not MyException
✅ Descriptive message — context include karo (userId, amount)
✅ errorCode field — API responses ke liye
✅ Cause chaining — super(message, cause) for root cause
✅ Appropriate base — Exception vs RuntimeException
✅ Serializable if needed — serialVersionUID for distributed systems
❌ Sensitive data in message mat daalo — passwords, tokens
```

**Short Interview Answer:**

Extend Exception (checked) or RuntimeException (unchecked). Add errorCode, context in message, cause chaining. Design hierarchy by domain. Meaningful names, no sensitive data in messages.

---

## Q9: Suppressed exceptions kya hain? try-with-resources mein kaise kaam karte hain?

**Answer:**

**Suppressed exception** = jab try block aur `close()` **dono exception throw** karein — close() ka exception try block ke exception ke saath **attach** ho jaata hai (primary exception suppress nahi hoti).

### Problem Without Suppression

```java
// Old finally — close() ka exception try ka exception overwrite kar deta tha
FileInputStream fis = null;
try {
    fis = new FileInputStream("file.txt");
    throw new RuntimeException("Read failed");  // Primary exception
} finally {
    if (fis != null) fis.close();  // Close bhi fail → primary LOST!
}
```

### try-with-resources — Suppression

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
    r.doWork();  // Primary: "Work failed!"
} catch (Exception e) {
    System.out.println("Main: " + e.getMessage());  // Work failed!
    for (Throwable suppressed : e.getSuppressed()) {
        System.out.println("Suppressed: " + suppressed.getMessage());  // Close failed!
    }
}
```

### How Suppression Works

```
1. try block exception throw karta hai (PRIMARY)
2. close() bhi exception throw karta hai
3. close() ka exception PRIMARY ke suppressed list mein add hota hai
4. PRIMARY exception caller ko milti hai — close exception lost nahi hoti
```

### Manually Add Suppressed

```java
try {
    throw new IOException("Primary error");
} catch (IOException e) {
    try {
        cleanup();
    } catch (RuntimeException cleanupError) {
        e.addSuppressed(cleanupError);  // Manually attach
    }
    throw e;
}
```

**Important:**

- **Primary exception** caller ko milti hai — debugging ke liye
- **Suppressed exceptions** `getSuppressed()` se access karo
- try-with-resources yeh **automatically handle** karta hai — finally se better

**Short Interview Answer:**

When try and close() both throw, close()'s exception is suppressed (attached to primary). Access via getSuppressed(). try-with-resources handles this automatically — primary exception preserved.

---

## Q10: Exception handling best practices — common anti-patterns?

**Answer:**

### 1. Never Swallow Exceptions

```java
// ❌ Empty catch — silent failure
try { processPayment(order); }
catch (PaymentException e) { }  // Kuch nahi hua — dangerous!

// ✅ Log + rethrow or handle
try { processPayment(order); }
catch (PaymentException e) {
    log.error("Payment failed for order: {}", order.getId(), e);
    throw e;
}
```

### 2. Don't Use Exceptions for Flow Control

```java
// ❌ Exception for normal flow — 500x slower!
try {
    for (User u : users) {
        if (u.getName().equals(name)) throw new FoundException(u);
    }
} catch (FoundException e) { return e.getUser(); }

// ✅ Normal return
for (User u : users) {
    if (u.getName().equals(name)) return u;
}
return null;
```

### 3. Prefer Specific Exception Types

```java
// ❌ Generic
throw new Exception("Something went wrong");

// ✅ Specific
throw new UserNotFoundException("User not found: " + id);
throw new InsufficientFundsException(required, available);
```

### 4. Log Before Rethrowing

```java
try {
    userRepository.save(user);
} catch (SQLException e) {
    log.error("Failed to save user: name={}, email={}", user.getName(), user.getEmail(), e);
    throw new DataAccessException("Failed to save user", e);
}
```

### 5. Always Clean Up Resources

```java
// ✅ try-with-resources
try (Connection conn = dataSource.getConnection();
     Statement stmt = conn.createStatement()) {
    // work
}

// ✅ Lock unlock in finally
lock.lock();
try { /* critical section */ }
finally { lock.unlock(); }
```

### Anti-Patterns Summary

| Anti-Pattern | Problem | Fix |
|-------------|---------|-----|
| Empty catch | Silent failure | Log + handle/rethrow |
| catch (Exception e) | Too broad | Specific catches |
| Exception for flow | Performance | if/return/Optional |
| return in finally | Overrides try return | Cleanup only in finally |
| Generic throw | Unclear error | Specific exception types |
| Sensitive data in message | Security leak | Sanitize messages |

### Checklist

```
✅ Specific exception types
✅ Log with context before rethrow
✅ try-with-resources for AutoCloseable
✅ Custom exceptions for business errors
✅ Exception chaining for root cause
✅ @RestControllerAdvice for centralized handling

❌ Empty catch blocks
❌ Exceptions for flow control
❌ Generic Exception throw
❌ return in finally
❌ Stack trace to end user
❌ Passwords/tokens in exception messages
```

**Short Interview Answer:**

Never swallow — log or rethrow. No exceptions for flow control. Specific types over generic. try-with-resources for cleanup. Log with context. No sensitive data in messages. @RestControllerAdvice for APIs.

---
