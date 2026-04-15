# Atomic Classes

## Status: Not Started

---

## Table of Contents

1. [AtomicInteger, AtomicLong, AtomicBoolean](#atomicinteger-atomiclong-atomicboolean)
2. [AtomicReference](#atomicreference)
3. [AtomicIntegerArray](#atomicintegerarray)
4. [Compare-and-Swap (CAS)](#compare-and-swap-cas)
5. [LongAdder (High-Contention Counter)](#longadder-high-contention-counter)

---

## AtomicInteger, AtomicLong, AtomicBoolean

**Matlab:** Lock-free thread-safe primitives — CAS (Compare-And-Swap) hardware instruction use karte hain.

### The Problem Without Atomic

```java
// ❌ Not thread-safe
class Counter {
    private int count = 0;

    public void increment() {
        count++;  // Read → Modify → Write — race condition!
    }
}

// ❌ synchronized — works but locking overhead
class SynchronizedCounter {
    private int count = 0;

    public synchronized void increment() {
        count++;  // Lock acquire → Read → Modify → Write → Lock release
    }
}

// ✅ AtomicInteger — lock-free, better performance
class AtomicCounter {
    private AtomicInteger count = new AtomicInteger(0);

    public void increment() {
        count.incrementAndGet();  // CAS operation — no locking!
    }
}
```

### AtomicInteger Usage

```java
AtomicInteger counter = new AtomicInteger(0);

// Get and increment
int val1 = counter.getAndIncrement();  // Returns old value, then increments
int val2 = counter.incrementAndGet();  // Increments, then returns new value

// Add and get
int val3 = counter.addAndGet(5);   // Adds 5, returns new value
int val4 = counter.getAndAdd(10);  // Returns old value, then adds 10

// Compare and set
boolean success = counter.compareAndSet(10, 20);  // If current == 10, set to 20

// Update with function
int updated = counter.updateAndGet(n -> n * 2);  // Double the value
int acc = counter.accumulateAndGet(5, (a, b) -> a + b);  // Accumulate
```

### AtomicLong

```java
AtomicLong longCounter = new AtomicLong(0);

long val1 = longCounter.incrementAndGet();
long val2 = longCounter.addAndGet(1000000);
boolean success = longCounter.compareAndSet(1000, 2000);
```

### AtomicBoolean

```java
AtomicBoolean flag = new AtomicBoolean(false);

// Atomic flip
boolean wasFalse = flag.compareAndSet(false, true);  // Flip if false

// Toggle
flag.set(!flag.get());

// Check and set
if (flag.compareAndSet(false, true)) {
    // Successfully changed from false to true
    doSomething();
}
```

---

## AtomicReference

**Matlab:** Object references ko atomically update karna.

### Basic Usage

```java
AtomicReference<String> ref = new AtomicReference<>("Hello");

// Get and set
String oldVal = ref.getAndSet("World");  // "Hello", now ref is "World"

// Compare and set
boolean success = ref.compareAndSet("World", "Hello Again");  // true

// Update with function
String updated = ref.updateAndGet(s -> s.toUpperCase());  // "HELLO AGAIN"
```

### Real-World Example: Immutable State Update

```java
class UserService {
    private AtomicReference<List<User>> usersRef = 
        new AtomicReference<>(Collections.emptyList());

    public List<User> getUsers() {
        return usersRef.get();  // Lock-free read
    }

    public void addUser(User user) {
        usersRef.updateAndGet(list -> {
            List<User> newList = new ArrayList<>(list);
            newList.add(user);
            return Collections.unmodifiableList(newList);
        });
    }

    public void removeUser(Long userId) {
        usersRef.updateAndGet(list -> 
            list.stream()
                .filter(u -> !u.getId().equals(userId))
                .collect(Collectors.toList())
        );
    }
}
```

### AtomicReferenceFieldUpdater

```java
// Existing class mein atomic field update
class Config {
    volatile String value;  // MUST be volatile
}

AtomicReferenceFieldUpdater<Config, String> updater = 
    AtomicReferenceFieldUpdater.newUpdater(Config.class, String.class, "value");

Config config = new Config();
updater.compareAndSet(config, null, "initial");  // Atomic update
```

---

## AtomicIntegerArray

**Matlab:** Array of integers — har element ko atomically update karna.

### Basic Usage

```java
int[] initial = {0, 0, 0, 0, 0};
AtomicIntegerArray array = new AtomicIntegerArray(initial);

// Get and set
array.set(2, 10);
int val = array.get(2);  // 10

// Atomic operations
int oldVal = array.getAndIncrement(0);  // Atomic increment at index 0
int newVal = array.addAndGet(1, 5);     // Add 5 at index 1

// Compare and set
boolean success = array.compareAndSet(0, 1, 10);  // If index 0 == 1, set to 10

// Update with function
int updated = array.updateAndGet(2, n -> n * 2);  // Double value at index 2
```

### Real-World Example: Vote Counting

```java
class VoteCounter {
    private final AtomicIntegerArray votes;  // votes[candidateIndex]

    public VoteCounter(int candidateCount) {
        this.votes = new AtomicIntegerArray(candidateCount);
    }

    public void vote(int candidateIndex) {
        votes.incrementAndGet(candidateIndex);  // Atomic vote
    }

    public int getVotes(int candidateIndex) {
        return votes.get(candidateIndex);
    }

    public int[] getAllVotes() {
        int[] result = new int[votes.length()];
        for (int i = 0; i < votes.length(); i++) {
            result[i] = votes.get(i);
        }
        return result;
    }
}
```

---

## Compare-and-Swap (CAS)

**Matlab:** Hardware-level atomic operation — lock-free synchronization ka foundation.

### How CAS Works

```
CAS Operation:
1. Read current value from memory
2. Compare with expected value
3. If equal → write new value
4. If not equal → do nothing

All in one atomic step — no thread can interfere!
```

### CAS Pseudocode

```java
boolean compareAndSwap(int[] memory, int address, int expected, int newValue) {
    if (memory[address] == expected) {
        memory[address] = newValue;
        return true;
    }
    return false;
}
```

### CAS in AtomicInteger

```java
// AtomicInteger.incrementAndGet() internally:
public final int incrementAndGet() {
    for (;;) {  // Spin loop
        int current = get();
        int next = current + 1;
        if (compareAndSet(current, next)) {  // CAS
            return next;
        }
        // If CAS failed, loop again — retry
    }
}
```

### CAS vs synchronized

| Feature | CAS | synchronized |
|---------|-----|-------------|
| **Locking** | No lock — optimistic | Lock-based — pessimistic |
| **Performance** | Better under low contention | Better under high contention |
| **Blocking** | Non-blocking — spin loop | Blocking — thread sleeps |
| **ABA Problem** | Possible | Not possible |
| **Complexity** | Simple for single variable | Better for compound operations |

### ABA Problem

```
Thread 1: Reads value A
Thread 2: Changes A → B
Thread 3: Changes B → A
Thread 1: CAS(A, C) — succeeds! But value changed in between

Solution: AtomicStampedReference (version number)
```

---

## LongAdder (High-Contention Counter)

**Matlab:** High contention scenarios ke liye optimized counter — cells array use karta hai.

### How It Works

```
LongAdder internally multiple cells maintain karta hai
Har thread alag cell pe increment karta hai — no contention
Final result = sum of all cells
```

### Usage

```java
LongAdder adder = new LongAdder();

// Increment
adder.increment();
adder.add(5);

// Get value (may be approximate during concurrent updates)
long total = adder.sum();

// Reset
adder.reset();
```

### LongAdder vs AtomicLong

```java
// Performance test
AtomicLong atomicLong = new AtomicLong(0);
LongAdder longAdder = new LongAdder();

int threadCount = 100;
int iterations = 1_000_000;

// AtomicLong — high contention
ExecutorService executor = Executors.newFixedThreadPool(threadCount);
for (int i = 0; i < threadCount; i++) {
    executor.submit(() -> {
        for (int j = 0; j < iterations; j++) {
            atomicLong.incrementAndGet();  // CAS retry hogi
        }
    });
}

// LongAdder — low contention
for (int i = 0; i < threadCount; i++) {
    executor.submit(() -> {
        for (int j = 0; j < iterations; j++) {
            longAdder.increment();  // Apne cell pe increment — no retry
        }
    });
}

// LongAdder significantly faster under high contention!
```

### When to Use What

```
AtomicLong:
- Single thread updates
- Low contention
- Need exact value at all times

LongAdder:
- Multiple threads update frequently
- High contention
- Final sum chahiye (intermediate values matter nahi)
- Metrics, counters, statistics
```

### LongAccumulator

```java
// Custom accumulation function
LongAccumulator accumulator = new LongAccumulator(
    (a, b) -> Math.max(a, b),  // Max accumulator
    0  // Initial value
);

accumulator.accumulate(5);
accumulator.accumulate(10);
accumulator.accumulate(3);

long max = accumulator.get();  // 10
```

---

## Summary

| Class | Purpose | Key Feature |
|-------|---------|-------------|
| **AtomicInteger** | Thread-safe int | CAS-based, lock-free |
| **AtomicLong** | Thread-safe long | Same as AtomicInteger |
| **AtomicBoolean** | Thread-safe boolean | Atomic flip/check |
| **AtomicReference** | Thread-safe object reference | Immutable state updates |
| **AtomicIntegerArray** | Array of atomic ints | Per-element atomic ops |
| **LongAdder** | High-contention counter | Cells array — minimal retries |
| **LongAccumulator** | Custom accumulation | Flexible reduce operation |
| **CAS** | Hardware-level atomic | Foundation of lock-free sync |
