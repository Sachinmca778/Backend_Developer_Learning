# Concurrent Collections

## Status: Not Started

---

## Table of Contents

1. [ConcurrentHashMap](#concurrenthashmap)
2. [CopyOnWriteArrayList](#copyonwritearraylist)
3. [BlockingQueue](#blockingqueue)
4. [ConcurrentLinkedQueue](#concurrentlinkedqueue)
5. [Collection Selection Guide](#collection-selection-guide)

---

## ConcurrentHashMap

**Matlab:** Thread-safe HashMap — multiple threads simultaneously read/write kar sakte hain.

### Why Not Collections.synchronizedMap?

```java
// ❌ Old approach — single lock, poor performance
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
// Sab operations ek lock pe — serialization

// ✅ ConcurrentHashMap — fine-grained locking
ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
// Multiple threads parallel mein read/write kar sakte hain
```

### How It Works

```
Java 7: Segment locking — 16 segments, har segment ka apna lock
Java 8+: CAS + synchronized bins — even finer granularity

Read operations: Lock-free (volatile reads)
Write operations: Bin-level locking — sirf us bucket pe lock
```

### Basic Usage

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();

// Put — thread-safe
map.put("Apple", 1);
map.put("Banana", 2);

// Get — lock-free
Integer value = map.get("Apple");  // 1

// PutIfAbsent — atomic
map.putIfAbsent("Cherry", 3);  // Adds if not present
map.putIfAbsent("Apple", 10);  // No change — Apple already exists

// Compute — atomic update
map.compute("Apple", (key, oldValue) -> 
    oldValue == null ? 1 : oldValue + 1);

// Merge — atomic merge
map.merge("Banana", 1, Integer::sum);  // 2 + 1 = 3

// Remove — atomic
map.remove("Apple");
```

### Atomic Operations

```java
ConcurrentHashMap<String, AtomicInteger> wordCounts = new ConcurrentHashMap<>();

// Traditional way — race condition possible
if (!wordCounts.containsKey(word)) {
    wordCounts.put(word, new AtomicInteger(0));  // ❌ Race condition!
}
wordCounts.get(word).incrementAndGet();

// ✅ Atomic way
wordCounts.computeIfAbsent(word, k -> new AtomicInteger(0))
    .incrementAndGet();

// ✅ Even better — use LongAdder (high contention)
ConcurrentHashMap<String, LongAdder> counts = new ConcurrentHashMap<>();
counts.computeIfAbsent(word, k -> new LongAdder()).increment();
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **get** | O(1) | Lock-free read |
| **put** | O(1) | Bin-level locking |
| **remove** | O(1) | Bin-level locking |
| **size** | O(n) | Approximate — concurrent modifications |
| **iteration** | O(n) | Weakly consistent — may not reflect latest |

### Iteration Behavior

```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("A", 1);
map.put("B", 2);

// Iterator — weakly consistent
// May or may not reflect concurrent modifications
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + " → " + entry.getValue());
}

// ✅ Safe — concurrent modifications allowed during iteration
map.put("C", 3);  // No ConcurrentModificationException!
```

---

## CopyOnWriteArrayList

**Matlab:** Thread-safe List — har write operation pe array ka copy banata hai. Read-heavy scenarios ke liye.

### How It Works

```
Add/Remove → Array copy banao → Copy modify karo → Reference update karo
Read → Original array pe bina lock ke read karo
```

### Basic Usage

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

// Add — copy banata hai
list.add("Apple");
list.add("Banana");
list.add("Cherry");

// Read — lock-free
String first = list.get(0);  // "Apple"

// Contains — lock-free
boolean hasApple = list.contains("Apple");  // true

// Iteration — snapshot pe iterate hota hai
for (String item : list) {
    System.out.println(item);
    // List mein concurrent modifications allowed
    // Lekin iterator pe effect nahi padega
}
```

### When to Use

```java
// ✅ Use CopyOnWriteArrayList when:
// - Reads bahut zyada, writes bahut kam
// - Iteration ke time modification possible hai
// - Thread-safety chahiye

// ❌ Avoid when:
// - Frequent writes hote hain (copy overhead)
// - Large lists (memory overhead)

// Example: Event listeners list
private final CopyOnWriteArrayList<EventListener> listeners = 
    new CopyOnWriteArrayList<>();

public void addListener(EventListener listener) {
    listeners.add(listener);  // Rare write
}

public void fireEvent(Event e) {
    for (EventListener listener : listeners) {  // Frequent reads
        listener.onEvent(e);
    }
}
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **get** | O(1) | Lock-free |
| **add** | O(n) | Copy banata hai |
| **remove** | O(n) | Copy banata hai |
| **contains** | O(n) | Linear scan |
| **iteration** | O(n) | Snapshot — no ConcurrentModificationException |

---

## BlockingQueue

**Matlab:** Queue jo full/empty hone pe block karta hai — producer/consumer pattern ke liye.

### ArrayBlockingQueue

```java
// Fixed capacity
BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);

// Producer
queue.put("Task1");  // Blocks if full
queue.put("Task2");

// Consumer
String task = queue.take();  // Blocks if empty
System.out.println("Processing: " + task);
```

### LinkedBlockingQueue

```java
// Optional bound
BlockingQueue<String> bounded = new LinkedBlockingQueue<>(100);

// Unbounded (Integer.MAX_VALUE)
BlockingQueue<String> unbounded = new LinkedBlockingQueue<>();

bounded.put("Task");  // Blocks if 100 elements present
unbounded.put("Task");  // Never blocks (practically)
```

### PriorityBlockingQueue

```java
// Priority-based ordering
BlockingQueue<Task> queue = new PriorityBlockingQueue<>(10, 
    Comparator.comparingInt(Task::getPriority));

queue.add(new Task("Low", 5));
queue.add(new Task("High", 1));
queue.add(new Task("Medium", 3));

// take() — highest priority (lowest number) pehle aayega
System.out.println(queue.take());  // Task("High", 1)
```

### BlockingQueue Methods

```
Method Type │ Throws Exception │ Returns Special │ Blocks         │ Times Out
────────────┼──────────────────┼─────────────────┼────────────────┼──────────────
Insert      │ add(e)           │ offer(e)        │ put(e)         │ offer(e, time, unit)
Remove      │ remove()         │ poll()          │ take()         │ poll(time, unit)
Examine     │ element()        │ peek()          │ N/A            │ N/A
```

### Producer-Consumer Pattern

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
                    if ("POISON_PILL".equals(task)) break;
                    System.out.println("Consumed: " + task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
```

---

## ConcurrentLinkedQueue

**Matlab:** Non-blocking thread-safe queue — lock-free algorithm use karta hai.

### Basic Usage

```java
ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();

// Add — lock-free
queue.add("Apple");
queue.offer("Banana");

// Remove — lock-free
String first = queue.poll();  // "Apple"
String peek = queue.peek();   // "Banana"

// Iteration — weakly consistent
for (String item : queue) {
    System.out.println(item);
}
```

### When to Use

```java
// ✅ Use ConcurrentLinkedQueue when:
// - High throughput, low latency chahiye
// - Blocking behavior nahi chahiye
// - Order matter karta hai (FIFO)

// ❌ Don't use when:
// - Blocking behavior chahiye (use BlockingQueue)
// - Size() accurate chahiye (O(n) operation)
```

---

## Collection Selection Guide

```
Collection chahiye?
├── Map chahiye?
│   ├── Thread-safe + high performance? → ConcurrentHashMap
│   └── Single-threaded? → HashMap
├── List chahiye?
│   ├── Read-heavy + thread-safe? → CopyOnWriteArrayList
│   ├── Queue behavior? → BlockingQueue / ConcurrentLinkedQueue
│   └── Single-threaded? → ArrayList
├── Queue chahiye?
│   ├── Blocking behavior? → BlockingQueue
│   │   ├── Fixed capacity? → ArrayBlockingQueue
│   │   ├── Dynamic capacity? → LinkedBlockingQueue
│   │   └── Priority ordering? → PriorityBlockingQueue
│   └── Non-blocking? → ConcurrentLinkedQueue
└── Set chahiye?
    ├── Thread-safe? → ConcurrentHashMap.newKeySet()
    └── Single-threaded? → HashSet
```

---

## Summary

| Collection | Use Case | Key Feature |
|------------|----------|-------------|
| **ConcurrentHashMap** | Thread-safe Map | Fine-grained locking, O(1) ops |
| **CopyOnWriteArrayList** | Read-heavy List | Copy-on-write, iteration-safe |
| **ArrayBlockingQueue** | Bounded blocking queue | Fixed capacity, producer-consumer |
| **LinkedBlockingQueue** | Unbounded blocking queue | Optional bound |
| **PriorityBlockingQueue** | Priority queue | Heap-based ordering |
| **ConcurrentLinkedQueue** | Non-blocking queue | Lock-free, high throughput |
