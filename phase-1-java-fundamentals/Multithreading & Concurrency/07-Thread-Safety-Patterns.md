# Thread Safety Patterns

## Status: Not Started

---

## Table of Contents

1. [Immutability](#immutability)
2. [Thread-Local Storage (ThreadLocal\<T\>)](#thread-local-storage-threadlocalt)
3. [Producer-Consumer Pattern](#producer-consumer-pattern)
4. [Double-Checked Locking Anti-Pattern](#double-checked-locking-anti-pattern)
5. [Thread Safety Checklist](#thread-safety-checklist)

---

## Immutability

**Matlab:** Ek baar object create ho gaya — usko modify nahi kar sakte. Thread-safe by default.

### Why Immutability is Thread-Safe

```
Immutable object:
- No thread usko modify kar sakta hai
- State change nahi hota
- Multiple threads safely share kar sakte hain
- No synchronization needed
```

### Creating Immutable Class

```java
// ✅ Immutable class
public final class User {  // final class — extend nahi ho sakta
    private final String name;      // final fields
    private final int age;
    private final List<String> roles;  // Mutable type — defensive copy

    public User(String name, int age, List<String> roles) {
        this.name = name;
        this.age = age;
        // Defensive copy — mutable input ko copy karo
        this.roles = Collections.unmodifiableList(new ArrayList<>(roles));
    }

    // Only getters — no setters
    public String getName() { return name; }
    public int getAge() { return age; }
    public List<String> getRoles() { 
        return new ArrayList<>(roles);  // Defensive copy on return
    }

    @Override
    public String toString() {
        return "User{name='" + name + "', age=" + age + "}";
    }
}
```

### Rules for Immutable Class

```
1. Class ko final banao — extend nahi ho sakta
2. Sab fields final banao — ek baar initialize, change nahi
3. No setters — state modify nahi kar sakte
4. Mutable fields ko defensive copy karo (constructor + getter)
5. State-changing methods mat banao
```

### Immutable with Builder

```java
public final class Configuration {
    private final String url;
    private final int timeout;
    private final Map<String, String> headers;

    private Configuration(Builder builder) {
        this.url = builder.url;
        this.timeout = builder.timeout;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
    }

    public String getUrl() { return url; }
    public int getTimeout() { return timeout; }
    public Map<String, String> getHeaders() { 
        return new HashMap<>(headers); 
    }

    public static class Builder {
        private String url;
        private int timeout = 30;
        private Map<String, String> headers = new HashMap<>();

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Configuration build() {
            return new Configuration(this);
        }
    }
}

// Usage
Configuration config = new Configuration.Builder()
    .url("https://api.example.com")
    .timeout(60)
    .header("Authorization", "Bearer token")
    .build();  // Immutable object
```

### Immutable Collections

```java
// Java 9+ — immutable collections
List<String> list = List.of("A", "B", "C");
Set<Integer> set = Set.of(1, 2, 3);
Map<String, Integer> map = Map.of("A", 1, "B", 2);

// Attempt to modify → UnsupportedOperationException
list.add("D");  // ❌ Exception!
```

---

## Thread-Local Storage (ThreadLocal\<T\>)

**Matlab:** Har thread ka apna isolated variable — threads share nahi karte.

### Basic Usage

```java
class ThreadLocalExample {
    // Each thread gets its own copy
    private static final ThreadLocal<Integer> threadLocal = 
        ThreadLocal.withInitial(() -> 0);

    public void increment() {
        Integer value = threadLocal.get();
        threadLocal.set(value + 1);
    }

    public Integer getValue() {
        return threadLocal.get();
    }

    public void remove() {
        threadLocal.remove();  // Memory leak prevention
    }
}

// Usage
ThreadLocalExample example = new ThreadLocalExample();

Thread t1 = new Thread(() -> {
    example.increment();
    System.out.println("Thread 1: " + example.getValue());  // 1
});

Thread t2 = new Thread(() -> {
    example.increment();
    example.increment();
    System.out.println("Thread 2: " + example.getValue());  // 2
});

t1.start();
t2.start();
// Results independent — threads don't interfere
```

### Real-World Example: SimpleDateFormat

```java
// ❌ SimpleDateFormat is NOT thread-safe
private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

// Multiple threads use same instance → corruption!

// ✅ ThreadLocal — each thread gets its own instance
private static final ThreadLocal<SimpleDateFormat> dateFormat = 
    ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));

public String formatDate(Date date) {
    return dateFormat.get().format(date);
}

// Usage
String formatted = formatDate(new Date());  // Thread-safe
```

### Real-World Example: User Context

```java
class UserContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }

    public static User getCurrentUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();  // Important: prevent memory leak
    }
}

// Web request filter
class AuthFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) {
        try {
            User user = authenticate(request);
            UserContext.setCurrentUser(user);
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();  // Always clean up
        }
    }
}
```

### Memory Leak Warning

```java
// ❌ Memory leak — ThreadLocal value never removed
ThreadLocal<HeavyObject> leaky = new ThreadLocal<>();
leaky.set(new HeavyObject());  // Thread pool mein thread reuse hota hai
// Value remain associated with thread — memory leak!

// ✅ Always remove
ThreadLocal<HeavyObject> safe = new ThreadLocal<>();
try {
    safe.set(new HeavyObject());
    // Use it
} finally {
    safe.remove();  // Clean up
}
```

---

## Producer-Consumer Pattern

**Matlab:** Producer data produce karta hai, Consumer data consume karta hai — BlockingQueue se coordinate hote hain.

### Basic Pattern

```java
class ProducerConsumer {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(10);

    class Producer implements Runnable {
        @Override
        public void run() {
            try {
                for (int i = 0; i < 100; i++) {
                    queue.put("Task-" + i);  // Blocks if full
                    System.out.println("Produced: Task-" + i);
                    Thread.sleep(10);  // Simulate work
                }
                queue.put("POISON_PILL");  // Signal consumer to stop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    class Consumer implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    String task = queue.take();  // Blocks if empty
                    if ("POISON_PILL".equals(task)) {
                        System.out.println("Consumer stopping");
                        break;
                    }
                    System.out.println("Consumed: " + task);
                    Thread.sleep(20);  // Simulate work
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void start() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(new Producer());
        executor.submit(new Consumer());
        executor.shutdown();
    }
}
```

### Multiple Producers/Consumers

```java
class MultiProducerConsumer {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(100);

    public void start(int producers, int consumers) {
        ExecutorService executor = Executors.newFixedThreadPool(producers + consumers);

        // Start producers
        for (int i = 0; i < producers; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        queue.put("Producer-" + id + "-Task-" + j);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Start consumers
        for (int i = 0; i < consumers; i++) {
            executor.submit(() -> {
                try {
                    while (true) {
                        String task = queue.poll(1, TimeUnit.SECONDS);
                        if (task == null) break;  // Timeout — exit
                        process(task);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
    }

    private void process(String task) {
        System.out.println("Processing: " + task);
    }
}
```

---

## Double-Checked Locking Anti-Pattern

**Matlab:** Singleton pattern ka flawed implementation — Java 5 se pehle broken tha.

### ❌ Broken Double-Checked Locking

```java
class Singleton {
    private static Singleton instance;  // NOT volatile

    public static Singleton getInstance() {
        if (instance == null) {  // First check — no locking
            synchronized (Singleton.class) {
                if (instance == null) {  // Second check — with locking
                    instance = new Singleton();  // ❌ May see partially constructed object!
                }
            }
        }
        return instance;
    }
}
```

### Why It's Broken

```
Thread 1: instance = new Singleton()
  Step 1: Memory allocate
  Step 2: Constructor start
  Step 3: instance reference assign (before constructor complete!)

Thread 2: if (instance == null) → false (reference assigned)
Thread 2: return instance;  // ❌ Partially constructed object!
```

### ✅ Fixed Double-Checked Locking

```java
class Singleton {
    private static volatile Singleton instance;  // MUST be volatile

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();  // ✅ volatile ensures visibility
                }
            }
        }
        return instance;
    }
}
```

### ✅ Better: Initialization-on-Demand Holder

```java
class Singleton {
    private Singleton() {}

    // Inner class — loaded only when accessed
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }

    public static Singleton getInstance() {
        return Holder.INSTANCE;  // Thread-safe, no synchronization needed
    }
}
```

### ✅ Best: Enum Singleton

```java
enum Singleton {
    INSTANCE;

    public void doSomething() {
        System.out.println("Doing something");
    }
}

// Usage
Singleton.INSTANCE.doSomething();

// ✅ Thread-safe, serialization-safe, reflection-safe
```

---

## Thread Safety Checklist

### ✅ Do

```
✅ Immutable objects use karo jab possible ho
✅ Thread-safe collections use karo (ConcurrentHashMap, etc.)
✅ volatile use karo for visibility-only scenarios
✅ Atomic classes use karo for simple counters
✅ ThreadLocal use karo for per-thread state
✅ ExecutorService use karo — manual thread creation avoid karo
✅ Always clean up ThreadLocal values
✅ Use try-finally with locks
✅ Document thread-safety guarantees
```

### ❌ Don't

```
❌ Mutable shared state without synchronization
❌ Double-checked locking without volatile
❌ ThreadLocal without remove() — memory leak
❌ synchronized(this) — use private lock objects
❌ Exceptions for thread communication
❌ Thread.stop(), Thread.suspend() — deprecated
❌ Assume thread-safety without documentation
```

### Thread-Safety Strategies

```
Strategy              | When to Use
──────────────────────┼─────────────────────────
Immutability          | State change rare nahi hai
Concurrent Collections | Shared data structure
Atomic Classes        | Simple counters/flags
ThreadLocal           | Per-thread state
Locks                 | Complex synchronization
ExecutorService       | Task execution
BlockingQueue         | Producer-consumer
```

---

## Summary

| Pattern | Key Point |
|---------|-----------|
| **Immutability** | Final class + final fields + no setters + defensive copies |
| **ThreadLocal** | Per-thread isolated variable — always `remove()` karo |
| **Producer-Consumer** | BlockingQueue se coordinate — multiple producers/consumers |
| **Double-Checked Locking** | Volatile zaruri hai — ya enum/holder pattern use karo |
| **Enum Singleton** | Thread-safe + serialization-safe + reflection-safe |
| **Defensive Copies** | Mutable input/output ko copy karo |
| **Thread Safety** | Immutability > Concurrent Collections > Locks |
