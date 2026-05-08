# Exception Interview Questions

## Status: Not Started

---

## Table of Contents

1. [Throwable Hierarchy](#throwable-hierarchy)
2. [Checked vs Unchecked](#checked-vs-unchecked)
3. [`Error` vs `Exception`](#error-vs-exception)
4. [Catch `Error`?](#catch-error)
5. [`finally` vs `return` Trap](#finally-vs-return-trap)
6. [try-with-resources vs Manual `finally`](#try-with-resources-vs-manual-finally)
7. [Suppressed Exceptions](#suppressed-exceptions)
8. [Rethrowing & Chaining](#rethrowing--chaining)
9. [Multi-catch](#multi-catch)
10. [Custom Exceptions Design](#custom-exceptions-design)
11. [Spring Specific Notes](#spring-specific-notes)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Throwable Hierarchy

```
Throwable
 ├── Error                       (serious, usually unrecoverable)
 │    ├── OutOfMemoryError
 │    ├── StackOverflowError
 │    └── ...
 └── Exception                   (recoverable normally)
      ├── RuntimeException        (UNCHECKED)
      │    ├── NullPointerException
      │    ├── IllegalArgumentException
      │    ├── ClassCastException
      │    └── ...
      └── (others)                (CHECKED — must declare/catch)
           ├── IOException
           ├── SQLException
           └── ...
```

---

## Checked vs Unchecked

| | Checked | Unchecked (RuntimeException) |
|--|---------|-----------------------------|
| Compiler enforce | ✅ throws/catch needed | ❌ |
| Examples | `IOException`, `SQLException` | `NPE`, `IllegalArg`, `IllegalState` |
| Use | Recoverable / contract | Programming bugs |
| Declared | `throws` clause | Not required |

### Modern preference

> "Most modern Java code (Spring, Lombok `@SneakyThrows`) leans **unchecked** for cleaner APIs — but checked exceptions still useful when caller **must** handle (file IO contracts)."

---

## `Error` vs `Exception`

| | Error | Exception |
|--|-------|-----------|
| Cause | JVM-level / fatal | App-level |
| Recover | Usually no | Yes |
| Examples | `OOME`, `StackOverflow`, `LinkageError` | `IOException`, `NPE` |

---

## Catch `Error`?

> "Compiler allow karta hai — but generally NO."

### Why not?

- `OutOfMemoryError` aaya → JVM unstable; aur allocation try karna fatal.
- `StackOverflowError` recursive bug — fix code.
- `Error` hierarchy assumes app **cannot reasonably recover**.

### Exceptions to "no"

- **Frameworks** sometimes catch (e.g., Servlet container catches `Throwable` to log + return 500).
- Tests / specific harnesses.
- **`AssertionError`** sometimes caught in test frameworks.

### Catching `Throwable`

```java
try { ... }
catch (Throwable t) { ... }   // ⚠️ very wide; usually only at outer-most boundary
```

---

## `finally` vs `return` Trap

### Q. Output?

```java
public static int foo() {
    try {
        return 1;
    } finally {
        return 2;       // ❌ overrides try return
    }
}
```

→ Returns **2**.

### Q. Output?

```java
public static int bar() {
    int x = 0;
    try {
        x = 1;
        return x;        // value captured: 1
    } finally {
        x = 2;            // doesn't change returned value
    }
}
```

→ Returns **1**. (Primitive value already snapshot before finally.)

### Q. Object reference

```java
public static List<Integer> baz() {
    List<Integer> l = new ArrayList<>();
    try { return l; }
    finally { l.add(99); }   // mutation visible — same reference returned
}
```

→ Returns list with `[99]`.

### Rule of thumb

**Never `return` from `finally`** — masks original return / exception.

---

## try-with-resources vs Manual `finally`

### Old way (verbose, error-prone)

```java
InputStream in = null;
try {
    in = new FileInputStream("a");
    use(in);
} finally {
    if (in != null) {
        try { in.close(); } catch (IOException e) { /* swallow */ }
    }
}
```

### Modern (Java 7+)

```java
try (InputStream in = new FileInputStream("a")) {
    use(in);
}   // auto close in reverse declaration order
```

### Multi-resource

```java
try (Connection c = ds.getConnection();
     PreparedStatement ps = c.prepareStatement(SQL);
     ResultSet rs = ps.executeQuery()) {
    while (rs.next()) ...
}
```

→ Closed in **reverse** order. Resource must implement `AutoCloseable`.

---

## Suppressed Exceptions

```java
try (Resource r = new Resource()) {
    throw new RuntimeException("primary");
}
// If r.close() also throws → suppressed; primary keeps top.
```

Access:

```java
catch (Throwable t) {
    for (Throwable s : t.getSuppressed()) { log(s); }
}
```

→ Java 7+ feature; replaces "swallowed close exception masking real one" classic bug.

---

## Rethrowing & Chaining

### Wrap + chain (preserves stack)

```java
try {
    parse(...);
} catch (IOException e) {
    throw new ParseException("read failed", e);
}
```

→ `getCause()` se original retrievable.

### Bad: lose root cause

```java
catch (IOException e) {
    throw new ParseException("read failed");   // ❌ root cause lost
}
```

### More precise rethrow (Java 7+)

```java
public void m() throws IOException, SQLException {
    try {
        ...
    } catch (Exception e) {
        throw e;        // compiler narrows to IOException | SQLException
    }
}
```

---

## Multi-catch

```java
try { ... }
catch (IOException | SQLException e) {
    log.error("io/sql", e);
}
```

⚠️ `e` effectively final → re-throw allowed but reassign nahi.

---

## Custom Exceptions Design

```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Order not found: " + id);
    }
}
```

### Best practices

- Extend `RuntimeException` (default modern preference) — unless caller MUST handle.
- Provide `(message, cause)` constructor.
- Don't add lots of fields — use cause chain.
- Don't catch + wrap unnecessarily.
- Avoid building stack trace overhead in **hot paths** (consider `fillInStackTrace` override only if profiled hotspot).

---

## Spring Specific Notes

- **`DataAccessException`** — Spring's unchecked wrapper around vendor-specific SQL exceptions.
- `@ControllerAdvice` + `@ExceptionHandler` for HTTP-level handling.
- `@Transactional` rolls back on **`RuntimeException`** + `Error` by default; not on **checked**. Override with `rollbackFor`.

```java
@Transactional(rollbackFor = Exception.class)
```

→ Cross-ref: `phase-4 / Microservices Architecture` resilience patterns.

---

## Common Output Traps

### Q. Order of catch

```java
try { ... }
catch (Exception e) { }
catch (IOException e) { }   // compile error — unreachable
```

→ Specific subclass **must** come first.

### Q. Exception in finally

```java
try {
    throw new RuntimeException("A");
} finally {
    throw new RuntimeException("B");  // wins — A lost! (no suppression here)
}
```

→ "B" propagates; "A" lost (try-with-resources me suppression hota; manual finally throw me NOT).

### Q. NoClassDefFoundError vs ClassNotFoundException

| | NoClassDefFoundError | ClassNotFoundException |
|--|---------------------|------------------------|
| Type | Error | Checked Exception |
| When | Class missing at runtime (was present at compile) | Reflection / Class.forName |

### Q. Catch superclass

```java
try { code(); }
catch (Throwable t) { ... }   // valid, but discourages
```

---

## Pitfalls

1. **Empty catch** — silently swallow → debugging hell.
2. **Logging + rethrow** — log once at top; double-logging chaos.
3. **`catch (Exception e)` everywhere** — hides bugs.
4. **`return` in finally** — masks normal flow.
5. **Throw in finally** — lose original.
6. **Wrap losing cause** — `new X(message)` chhod, `new X(message, e)`.
7. **Checked-only thinking** — Spring `@Transactional` rollback default skip.
8. **Too granular custom exception classes** — every endpoint != new class.
9. **`Throwable.printStackTrace()` in prod** — use logger.
10. **Resource leak** — manual finally → use try-with-resources.

---

## Cheat Sheet

| | Checked | Unchecked |
|--|---------|-----------|
| Declare | `throws` | optional |
| Examples | IOException | NPE |
| Use | Caller must handle | Programming bugs |

| Construct | Use |
|-----------|-----|
| try-catch-finally | Standard |
| try-with-resources | AutoCloseable cleanup |
| multi-catch | Same handler, multiple types |
| rethrow | Preserve cause |

| Anti-Pattern | Better |
|--------------|--------|
| empty catch | log or rethrow |
| return in finally | avoid |
| swallow cause | chain via constructor |
| catch `Error` | usually no |

---

## Practice

1. Run "return in finally" snippets — predict, verify.
2. try-with-resources: 2 resources jahan close throws; observe suppressed.
3. Spring `@Transactional` checked exception → rollback nahi → demo + fix with `rollbackFor`.
4. Wrap IOException in custom `RepoException`; verify `getCause`.
5. NoClassDefFound vs ClassNotFound — produce both manually.
