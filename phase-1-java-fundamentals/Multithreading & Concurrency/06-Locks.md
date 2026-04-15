# Locks (java.util.concurrent.locks)

## Status: Not Started

---

## Table of Contents

1. [ReentrantLock](#reentrantlock)
2. [ReadWriteLock & ReentrantReadWriteLock](#readwritelock--reentrantreadwritelock)
3. [StampedLock](#stampedlock)
4. [Condition](#condition)
5. [tryLock with Timeout](#trylock-with-timeout)
6. [Lock Fairness](#lock-fairness)

---

## ReentrantLock

**Matlab:** `synchronized` ka flexible alternative — explicit lock/unlock, timeout support, fairness.

### Basic Usage

```java
class Counter {
    private final ReentrantLock lock = new ReentrantLock();
    private int count = 0;

    public void increment() {
        lock.lock();  // Acquire lock
        try {
            count++;  // Critical section
        } finally {
            lock.unlock();  // Always unlock — finally block mein
        }
    }

    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

### ReentrantLock vs synchronized

| Feature | ReentrantLock | synchronized |
|---------|--------------|-------------|
| **Lock/Unlock** | Explicit | Automatic |
| **Timeout** | ✅ tryLock(timeout) | ❌ No |
| **Fairness** | ✅ Optional | ❌ No |
| **Interruptible** | ✅ lockInterruptibly() | ❌ No |
| **Multiple Conditions** | ✅ Multiple Condition objects | ❌ Single wait/notify |
| **Performance** | Similar (Java 6+) | Similar (Java 6+) |
| **Safety** | Manual unlock — risk of forgetting | Automatic — safer |

### tryLock

```java
ReentrantLock lock = new ReentrantLock();

if (lock.tryLock()) {  // Non-blocking attempt
    try {
        // Critical section
    } finally {
        lock.unlock();
    }
} else {
    System.out.println("Lock not available — doing something else");
}
```

### tryLock with Timeout

```java
ReentrantLock lock = new ReentrantLock();

if (lock.tryLock(5, TimeUnit.SECONDS)) {  // Wait up to 5 seconds
    try {
        // Critical section
    } finally {
        lock.unlock();
    }
} else {
    System.out.println("Timed out — couldn't acquire lock");
}
```

### lockInterruptibly

```java
ReentrantLock lock = new ReentrantLock();

try {
    lock.lockInterruptibly();  // Can be interrupted while waiting
    try {
        // Critical section
    } finally {
        lock.unlock();
    }
} catch (InterruptedException e) {
    System.out.println("Lock acquisition interrupted");
    Thread.currentThread().interrupt();
}
```

---

## ReadWriteLock & ReentrantReadWriteLock

**Matlab:** Readers ko parallel access, writers ko exclusive access — read-heavy scenarios ke liye optimized.

### How It Works

```
Multiple readers → Parallel access allowed
Single writer → Exclusive access
Reader + Writer → Writers wait for readers to finish
Writer + Reader → Readers wait for writer to finish
```

### Basic Usage

```java
class Cache {
    private final Map<String, String> cache = new HashMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public String get(String key) {
        rwLock.readLock().lock();  // Multiple readers allowed
        try {
            return cache.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void put(String key, String value) {
        rwLock.writeLock().lock();  // Exclusive access
        try {
            cache.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
```

### ReadWriteLock Rules

```
1. Multiple threads can hold read lock simultaneously
2. Write lock is exclusive — no other reader or writer
3. If write lock is held, all read lock requests block
4. If read lock is held, write lock requests block
```

### Lock Upgrade/Downgrade

```java
// ❌ Lock upgrade NOT supported
// readLock → writeLock — deadlock!

// ✅ Lock downgrade supported
rwLock.writeLock().lock();
try {
    cache.put(key, value);
    rwLock.readLock().lock();  // Acquire read lock while holding write
} finally {
    rwLock.writeLock().unlock();  // Release write lock — downgrade complete
}
// Now only read lock is held
try {
    return cache.get(key);
} finally {
    rwLock.readLock().unlock();
}
```

### Fairness

```java
// Fair — readers/writers get turn in order
ReentrantReadWriteLock fairLock = new ReentrantReadWriteLock(true);

// Unfair — default (faster, but starvation possible)
ReentrantReadWriteLock unfairLock = new ReentrantReadWriteLock();
```

---

## StampedLock

**Matlab:** Java 8+ — ReadWriteLock ka optimized version, optimistic reads support.

### Lock Modes

```
1. Read Lock — shared (multiple readers)
2. Write Lock — exclusive
3. Optimistic Read — no lock, validate with stamp
```

### Basic Usage

```java
class Point {
    private final StampedLock lock = new StampedLock();
    private double x, y;

    // Write lock
    public void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();  // Exclusive
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    // Read lock
    public double distanceFromOrigin() {
        long stamp = lock.readLock();  // Shared
        try {
            return Math.sqrt(x * x + y * y);
        } finally {
            lock.unlockRead(stamp);
        }
    }
}
```

### Optimistic Read

```java
public double distanceFromOriginOptimistic() {
    long stamp = lock.tryOptimisticRead();  // No lock — optimistic
    double currentX = x;
    double currentY = y;

    // Validate — kisi ne write toh nahi kiya?
    if (lock.validate(stamp)) {
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }

    // Validation failed — read lock acquire karo
    stamp = lock.readLock();
    try {
        return Math.sqrt(x * x + y * y);
    } finally {
        lock.unlockRead(stamp);
    }
}
```

### StampedLock vs ReadWriteLock

| Feature | StampedLock | ReadWriteLock |
|---------|-------------|---------------|
| **Optimistic Read** | ✅ Yes | ❌ No |
| **Performance** | Better for read-heavy | Good |
| **Reentrant** | ❌ No | ✅ Yes |
| **Complexity** | More complex | Simpler |
| **Deadlock Risk** | Lower (non-reentrant) | Higher (reentrant) |

---

## Condition

**Matlab:** `synchronized` ke `wait()`/`notify()` ka flexible alternative — multiple wait conditions.

### Basic Usage

```java
class BoundedBuffer {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private final Object[] items = new Object[10];
    private int count;

    public void put(Object item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await();  // Wait until not full
            }
            items[count++] = item;
            notEmpty.signal();  // Signal one waiting consumer
        } finally {
            lock.unlock();
        }
    }

    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await();  // Wait until not empty
            }
            Object item = items[--count];
            notFull.signal();  // Signal one waiting producer
            return item;
        } finally {
            lock.unlock();
        }
    }
}
```

### Condition Methods

```java
Condition condition = lock.newCondition();

// Wait
condition.await();           // Indefinitely
condition.await(5, TimeUnit.SECONDS);  // With timeout
condition.awaitUninterruptibly();  // Not interruptible

// Signal
condition.signal();          // Wake up one waiting thread
condition.signalAll();       // Wake up all waiting threads
```

### Multiple Conditions

```java
class TaskQueue {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition hasHighPriority = lock.newCondition();
    private final Condition hasLowPriority = lock.newCondition();
    private final Queue<Task> highPriority = new LinkedList<>();
    private final Queue<Task> lowPriority = new LinkedList<>();

    public void addHighPriority(Task task) {
        lock.lock();
        try {
            highPriority.offer(task);
            hasHighPriority.signal();
        } finally {
            lock.unlock();
        }
    }

    public Task take() throws InterruptedException {
        lock.lock();
        try {
            while (highPriority.isEmpty() && lowPriority.isEmpty()) {
                hasHighPriority.await();
                hasLowPriority.await();
            }
            return highPriority.isEmpty() ? lowPriority.poll() : highPriority.poll();
        } finally {
            lock.unlock();
        }
    }
}
```

---

## tryLock with Timeout

```java
ReentrantLock lock = new ReentrantLock();

// Non-blocking attempt
if (lock.tryLock()) {
    try {
        // Critical section
    } finally {
        lock.unlock();
    }
} else {
    // Lock not available — fallback
    handleUnavailable();
}

// Timeout attempt
try {
    if (lock.tryLock(5, TimeUnit.SECONDS)) {
        try {
            // Critical section
        } finally {
            lock.unlock();
        }
    } else {
        // Timed out
        handleTimeout();
    }
} catch (InterruptedException e) {
    // Interrupted while waiting
    Thread.currentThread().interrupt();
}
```

---

## Lock Fairness

### Fair Lock

```java
// Fair — FIFO ordering
ReentrantLock fairLock = new ReentrantLock(true);

// Threads get lock in the order they requested
// Prevents starvation
// Slower performance
```

### Unfair Lock (Default)

```java
// Unfair — no ordering guarantee
ReentrantLock unfairLock = new ReentrantLock();  // Default

// Thread jo abhi ready hai woh pehle lock le sakta hai
// Faster performance
// Starvation possible
```

### When to Use Fair Lock

```
Use fair lock when:
- Starvation unacceptable hai
- Ordering important hai
- Performance impact acceptable hai

Use unfair lock (default) when:
- Performance priority hai
- Occasional starvation acceptable hai
```

---

## Summary

| Lock Type | Use Case | Key Feature |
|-----------|----------|-------------|
| **ReentrantLock** | Flexible synchronization | tryLock, timeout, fairness |
| **ReadWriteLock** | Read-heavy scenarios | Multiple readers, exclusive writer |
| **StampedLock** | Optimistic reads | No-lock reads, validation |
| **Condition** | Multiple wait conditions | wait/notify alternative |
| **tryLock** | Non-blocking attempt | Timeout support |
| **Fair Lock** | Starvation prevention | FIFO ordering |
