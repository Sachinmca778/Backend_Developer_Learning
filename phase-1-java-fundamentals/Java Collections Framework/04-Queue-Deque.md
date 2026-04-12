# Queue & Deque

## Status: Not Started

---

## Table of Contents

1. [Queue Interface](#queue-interface)
2. [PriorityQueue](#priorityqueue)
3. [ArrayDeque](#arraydeque)
4. [LinkedList as Queue](#linkedlist-as-queue)
5. [BlockingQueue](#blockingqueue)

---

## Queue Interface

**Matlab:** FIFO (First-In-First-Out) data structure — jo pehle aaya, woh pehle gaya.

```
Collection (interface)
    ↑
Queue (interface) — FIFO ordering
    ↑
    ├── PriorityQueue
    ├── ArrayDeque
    └── LinkedList
```

### Queue Operations

```
Method Type │ Throws Exception │ Returns Special Value
────────────┼──────────────────┼──────────────────────
Insert      │ add(e)           │ offer(e)
Remove      │ remove()         │ poll()
Examine     │ element()        │ peek()
```

### Basic Usage

```java
Queue<String> queue = new LinkedList<>();

// Add elements
queue.add("Task1");     // Throws exception if capacity restricted
queue.offer("Task2");   // Returns false if capacity restricted
queue.offer("Task3");

// Examine (without removing)
String first = queue.peek();     // "Task1" (returns null if empty)
String first2 = queue.element(); // "Task1" (throws exception if empty)

// Remove elements
String task = queue.poll();      // "Task1" (returns null if empty)
String task2 = queue.remove();   // "Task2" (throws exception if empty)

// Size
int size = queue.size();  // 1

// Check
boolean isEmpty = queue.isEmpty();  // false
```

### Queue Iteration

```java
Queue<String> queue = new LinkedList<>();
queue.offer("A");
queue.offer("B");
queue.offer("C");

// for-each (does NOT remove elements)
for (String item : queue) {
    System.out.println(item);  // A, B, C
}

// Poll until empty (removes elements)
while (!queue.isEmpty()) {
    String item = queue.poll();
    System.out.println("Processing: " + item);
}
```

---

## PriorityQueue

**Matlab:** Heap-based queue — elements priority ke basis pe remove hote hain (natural ordering ya custom comparator).

### Internal Working

```
PriorityQueue internally binary heap use karta hai (min-heap by default)
Smallest element hamesha root pe hota hai
Add: O(log n) — heapify up
Poll: O(log n) — heapify down
Peek: O(1) — root element
```

### Basic Usage

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();

pq.offer(5);
pq.offer(2);
pq.offer(8);
pq.offer(1);

System.out.println(pq.peek());  // 1 (smallest element)

while (!pq.isEmpty()) {
    System.out.println(pq.poll());  // 1, 2, 5, 8 (sorted order mein niklega)
}
```

### Custom Comparator

```java
// Max-heap (reverse order)
PriorityQueue<Integer> maxPQ = new PriorityQueue<>(Comparator.reverseOrder());
maxPQ.offer(5);
maxPQ.offer(2);
maxPQ.offer(8);

System.out.println(maxPQ.peek());  // 8 (largest element)

// Custom object priority
class Task implements Comparable<Task> {
    String name;
    int priority;

    public Task(String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    @Override
    public int compareTo(Task other) {
        return Integer.compare(this.priority, other.priority);  // Lower number = higher priority
    }

    @Override
    public String toString() {
        return name + "(" + priority + ")";
    }
}

PriorityQueue<Task> taskQueue = new PriorityQueue<>();
taskQueue.offer(new Task("Low", 5));
taskQueue.offer(new Task("High", 1));
taskQueue.offer(new Task("Medium", 3));

while (!taskQueue.isEmpty()) {
    System.out.println(taskQueue.poll());  // High(1), Medium(3), Low(5)
}

// Comparator se
PriorityQueue<Task> pq2 = new PriorityQueue<>(Comparator.comparingInt(t -> -t.priority));
```

### Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **offer/poll** | O(log n) | Heap operations |
| **peek** | O(1) | Root element |
| **remove(object)** | O(n) | Linear search + heapify |
| **contains** | O(n) | Linear search |
| **iteration** | O(n) | **No order guarantee** |

### ⚠️ Important Notes

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.offer(5);
pq.offer(2);
pq.offer(8);

// ❌ Iteration order is NOT sorted!
for (int n : pq) {
    System.out.println(n);  // 2, 5, 8 (ya koi aur order — heap structure pe depend)
}

// ✅ Sorted order mein chahiye toh poll karo
while (!pq.isEmpty()) {
    System.out.println(pq.poll());  // 2, 5, 8 (sorted)
}

// ❌ null elements NOT allowed
pq.offer(null);  // ❌ NullPointerException
```

### Use Cases

```java
// Top K elements
public List<Integer> topK(int[] nums, int k) {
    PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.reverseOrder());
    for (int num : nums) {
        pq.offer(num);
    }
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < k; i++) {
        result.add(pq.poll());
    }
    return result;
}

// Merge K sorted lists
public ListNode mergeKLists(ListNode[] lists) {
    PriorityQueue<ListNode> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
    for (ListNode list : lists) {
        if (list != null) pq.offer(list);
    }
    // ... merge logic
}
```

---

## ArrayDeque

**Matlab:** Resizable array-based Deque (Double-Ended Queue) — stack aur queue dono ke liye use ho sakta hai.

### Basic Usage

```java
Deque<String> deque = new ArrayDeque<>();

// Add at both ends
deque.addFirst("A");      // Front mein add
deque.addLast("B");       // End mein add
deque.offerFirst("C");    // Front mein add
deque.offerLast("D");     // End mein add

System.out.println(deque);  // [C, A, B, D]

// Remove from both ends
String first = deque.removeFirst();  // "C"
String last = deque.removeLast();    // "D"

// Examine
String peekFirst = deque.peekFirst();  // "A"
String peekLast = deque.peekLast();    // "B"

// Stack operations (push/pop)
deque.push("E");  // Same as addFirst
String popped = deque.pop();  // Same as removeFirst
```

### As Queue (FIFO)

```java
Queue<String> queue = new ArrayDeque<>();
queue.offer("Task1");
queue.offer("Task2");
queue.offer("Task3");

System.out.println(queue.poll());  // "Task1"
System.out.println(queue.poll());  // "Task2"
```

### As Stack (LIFO)

```java
Deque<String> stack = new ArrayDeque<>();
stack.push("A");
stack.push("B");
stack.push("C");

System.out.println(stack.pop());  // "C"
System.out.println(stack.pop());  // "B"
```

### Performance

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| **addFirst/addLast** | O(1) amortized | Array resize hone pe O(n) |
| **removeFirst/removeLast** | O(1) | Direct access |
| **peekFirst/peekLast** | O(1) | Direct access |
| **contains** | O(n) | Linear search |

### ArrayDeque vs Stack/LinkedList

| Feature | ArrayDeque | Stack | LinkedList |
|---------|-----------|-------|------------|
| **Type** | Deque | Class (extends Vector) | List + Deque |
| **Performance** | Fast | Slow (synchronized) | Medium |
| **Memory** | Less | More | More (Node overhead) |
| **Null Elements** | ❌ No | ✅ Yes | ✅ Yes |
| **Recommended?** | ✅ Yes | ❌ Avoid | ⚠️ Use as Queue |

### When to Use

```java
// ✅ Use ArrayDeque when:
// - Stack chahiye (push/pop)
// - Queue chahiye with both-end access
// - Fast operations chahiye

// ❌ Don't use when:
// - Null elements store karne hain
// - Index-based access chahiye (use ArrayList)
```

---

## LinkedList as Queue

**Matlab:** LinkedList Queue interface implement karta hai — FIFO operations ke liye use ho sakta hai.

### Usage

```java
Queue<String> queue = new LinkedList<>();

queue.offer("Task1");
queue.offer("Task2");
queue.offer("Task3");

System.out.println(queue.peek());  // "Task1"
System.out.println(queue.poll());  // "Task1"
System.out.println(queue.poll());  // "Task2"

// LinkedList specific methods
LinkedList<String> ll = (LinkedList<String>) queue;
ll.addFirst("Urgent");  // Front mein add
ll.addLast("Low");      // End mein add
```

### LinkedList as Deque

```java
Deque<String> deque = new LinkedList<>();

deque.addFirst("A");
deque.addLast("B");
deque.addFirst("C");
// Deque: C ↔ A ↔ B

System.out.println(deque.removeFirst());  // "C"
System.out.println(deque.removeLast());   // "B"
```

---

## BlockingQueue

**Matlab:** Thread-safe Queue — producer/consumer pattern ke liye. Queue full/empty hone pe block karta hai.

### Types

```
BlockingQueue (interface)
    ↑
    ├── ArrayBlockingQueue     // Fixed size, array-based
    ├── LinkedBlockingQueue    // Optional bound, linked list-based
    ├── PriorityBlockingQueue  // Priority-based ordering
    ├── DelayQueue             // Elements with delay
    └── SynchronousQueue       // Zero capacity — direct handoff
```

### ArrayBlockingQueue

```java
// Fixed capacity
BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);

// Producer
queue.put("Task1");  // Blocks if queue is full
queue.put("Task2");
queue.put("Task3");

// Consumer
String task = queue.take();  // Blocks if queue is empty

// Non-blocking alternatives
queue.offer("Task4");        // Returns false if full
queue.offer("Task5", 1, TimeUnit.SECONDS);  // Wait 1 sec, then return false
queue.poll();                // Returns null if empty
queue.poll(1, TimeUnit.SECONDS);  // Wait 1 sec, then return null
```

### LinkedBlockingQueue

```java
// Unbounded (Integer.MAX_VALUE capacity)
BlockingQueue<String> unbounded = new LinkedBlockingQueue<>();

// Bounded
BlockingQueue<String> bounded = new LinkedBlockingQueue<>(10);

unbounded.put("Task");  // Never blocks (practically unbounded)
bounded.put("Task");    // Blocks if 10 elements already present
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

    public void start() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(new Producer());
        executor.submit(new Consumer());
        executor.shutdown();
    }
}
```

### BlockingQueue Methods

```
Method Type │ Throws Exception │ Returns Special │ Blocks         │ Times Out
────────────┼──────────────────┼─────────────────┼────────────────┼──────────────
Insert      │ add(e)           │ offer(e)        │ put(e)         │ offer(e, time, unit)
Remove      │ remove()         │ poll()          │ take()         │ poll(time, unit)
Examine     │ element()        │ peek()          │ N/A            │ N/A
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Queue** | FIFO — first in, first out |
| **PriorityQueue** | Heap-based, priority ordering, O(log n) poll |
| **ArrayDeque** | Double-ended queue, faster than Stack/LinkedList |
| **LinkedList as Queue** | Queue implementation — doubly linked |
| **BlockingQueue** | Thread-safe, producer/consumer pattern, blocks on full/empty |
| **ArrayBlockingQueue** | Fixed capacity, array-based |
| **LinkedBlockingQueue** | Optional bound, linked list-based |
