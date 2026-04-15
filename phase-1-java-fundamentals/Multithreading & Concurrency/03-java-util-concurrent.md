# java.util.concurrent

## Status: Not Started

---

## Table of Contents

1. [ExecutorService](#executorservice)
2. [Future\<T\> & Callable\<T\>](#futuret--callablet)
3. [Thread Pools](#thread-pools)
4. [ScheduledExecutorService](#scheduledexecutorservice)
5. [CountDownLatch](#countdownlatch)
6. [CyclicBarrier](#cyclicbarrier)
7. [Semaphore](#semaphore)
8. [Phaser](#phaser)

---

## ExecutorService

**Matlab:** Thread management ka framework — manually thread create karne ki jagah, executor ko task submit karo.

### Why ExecutorService?

```java
// ❌ Manual thread creation — expensive, unbounded
for (int i = 0; i < 1000; i++) {
    new Thread(() -> process(i)).start();  // 1000 threads! Memory overflow!
}

// ✅ ExecutorService — controlled, reusable
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 1000; i++) {
    executor.submit(() -> process(i));  // Sirf 10 threads reuse honge
}
executor.shutdown();
```

### Creating ExecutorService

```java
// Fixed thread pool
ExecutorService fixed = Executors.newFixedThreadPool(10);

// Cached thread pool
ExecutorService cached = Executors.newCachedThreadPool();

// Single thread executor
ExecutorService single = Executors.newSingleThreadExecutor();
```

### Submitting Tasks

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

// Runnable submit karo
executor.submit(() -> {
    System.out.println("Running task");
});

// Shutdown — pending tasks complete honge
executor.shutdown();

// Immediate shutdown — running tasks interrupt honge
executor.shutdownNow();
```

### Awaiting Termination

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

// Tasks submit karo
for (int i = 0; i < 10; i++) {
    executor.submit(() -> process(i));
}

// Shutdown
executor.shutdown();

// Wait for completion
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        System.out.println("Timeout — forcing shutdown");
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

---

## Future\<T\> & Callable\<T\>

### Callable\<T\>

**Matlab:** Runnable jaisa lekin return value aur exception throw kar sakta hai.

```java
// Runnable — no return, no checked exception
Runnable runnable = () -> {
    System.out.println("Running");
    // return nahi kar sakta
};

// Callable — return value + checked exception
Callable<Integer> callable = () -> {
    Thread.sleep(1000);
    return 42;  // Return kar sakta hai
};
```

### Future\<T\>

**Matlab:** Async task ka result — baad mein get kar sakte ho.

```java
ExecutorService executor = Executors.newFixedThreadPool(5);

// Callable submit karo
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(2000);
    return 42;
});

// Do other work...

// Result get karo (blocks until complete)
try {
    Integer result = future.get();  // 42
    System.out.println("Result: " + result);
} catch (ExecutionException e) {
    System.out.println("Task failed: " + e.getCause().getMessage());
} catch (InterruptedException e) {
    System.out.println("Task interrupted");
}
```

### Future Methods

```java
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(5000);
    return 42;
});

// get() — blocks indefinitely
Integer result = future.get();

// get(timeout) — timeout ke saath wait karo
try {
    Integer result = future.get(2, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("Task timed out");
}

// isDone() — task complete hua ya nahi
boolean done = future.isDone();

// isCancelled() — task cancel hua ya nahi
boolean cancelled = future.isCancelled();

// cancel() — task cancel karo
boolean cancelled = future.cancel(true);  // true = interrupt if running
```

---

## Thread Pools

### FixedThreadPool

```java
// Fixed size — tasks queue mein wait karte hain
ExecutorService fixed = Executors.newFixedThreadPool(10);

// 10 threads hamesha available
// 10 se zyada tasks aaye toh queue mein wait karenge
```

### CachedThreadPool

```java
// Dynamic size — zarurat pe threads banata hai
ExecutorService cached = Executors.newCachedThreadPool();

// Idle threads 60 seconds baad terminate hote hain
// Unbounded — bahut tasks aaye toh bahut threads ban sakte hain!
// ⚠️ Production mein avoid karo — resource exhaustion possible
```

### ScheduledExecutorService

```java
// Scheduled tasks — delay ya periodic execution
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

// Ek baar run — delay ke baad
scheduler.schedule(() -> {
    System.out.println("Running after 5 seconds");
}, 5, TimeUnit.SECONDS);

// Periodic run — fixed rate
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Running every 2 seconds");
}, 0, 2, TimeUnit.SECONDS);

// Periodic run — fixed delay
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("Running 2 seconds after previous completion");
}, 0, 2, TimeUnit.SECONDS);

// Shutdown
scheduler.shutdown();
```

---

## CountDownLatch

**Matlab:** Ek thread ko multiple threads ke complete hone ka wait karwana.

```java
int workerCount = 5;
CountDownLatch latch = new CountDownLatch(workerCount);

// Workers start karo
for (int i = 0; i < workerCount; i++) {
    new Thread(() -> {
        System.out.println("Worker starting");
        try {
            Thread.sleep(1000);  // Do work
            System.out.println("Worker done");
        } catch (InterruptedException e) {}
        finally {
            latch.countDown();  // Countdown
        }
    }).start();
}

// Main thread wait karega
latch.await();  // Sab workers complete hone tak block
System.out.println("All workers completed");
```

### Real-World Example

```java
// Application startup — multiple services initialize
CountDownLatch startupLatch = new CountDownLatch(3);

new Thread(() -> {
    initializeDatabase();
    startupLatch.countDown();
}).start();

new Thread(() -> {
    initializeCache();
    startupLatch.countDown();
}).start();

new Thread(() -> {
    initializeMessageQueue();
    startupLatch.countDown();
}).start();

startupLatch.await();  // Wait for all services
System.out.println("Application started");
```

---

## CyclicBarrier

**Matlab:** Multiple threads ko ek point pe rokna — sab aayein tabhi aage badhein. Reusable hai.

```java
int threadCount = 3;
CyclicBarrier barrier = new CyclicBarrier(threadCount, () -> {
    System.out.println("All threads reached barrier!");
});

// Threads
for (int i = 0; i < threadCount; i++) {
    new Thread(() -> {
        System.out.println("Thread starting");
        try {
            Thread.sleep(1000);  // Phase 1
            System.out.println("Thread reached barrier");
            barrier.await();  // Wait for others
            System.out.println("Thread continuing");
        } catch (Exception e) {}
    }).start();
}
```

### CyclicBarrier vs CountDownLatch

| Feature | CyclicBarrier | CountDownLatch |
|---------|---------------|----------------|
| **Reusable** | ✅ Yes | ❌ No |
| **Who waits** | All threads wait | One thread waits for others |
| **Count** | Increment nahi hota | countDown() se decrement |
| **Use Case** | Multi-phase computation | One-time event wait |

---

## Semaphore

**Matlab:** Resource access control — maximum N threads ko simultaneously allow karna.

```java
// Maximum 3 threads simultaneously access kar sakte hain
Semaphore semaphore = new Semaphore(3);

// 10 threads try karenge
for (int i = 0; i < 10; i++) {
    new Thread(() -> {
        try {
            semaphore.acquire();  // Permit lena padega
            System.out.println("Thread acquired: " + Thread.currentThread().getName());
            Thread.sleep(1000);  // Do work
        } catch (InterruptedException e) {
        } finally {
            semaphore.release();  // Permit wapas do
        }
    }).start();
}

// Sirf 3 threads simultaneously run karenge
```

### Fair Semaphore

```java
// Fair — FIFO ordering
Semaphore fair = new Semaphore(3, true);

// Unfair — default (faster, but starvation possible)
Semaphore unfair = new Semaphore(3);
```

### Real-World Example: Database Connection Pool

```java
class ConnectionPool {
    private final Semaphore semaphore;
    private final Queue<Connection> connections = new LinkedList<>();

    public ConnectionPool(int maxSize) {
        this.semaphore = new Semaphore(maxSize);
        // Initialize connections
        for (int i = 0; i < maxSize; i++) {
            connections.add(createConnection());
        }
    }

    public Connection getConnection() throws InterruptedException {
        semaphore.acquire();  // Wait for available slot
        synchronized (connections) {
            return connections.poll();
        }
    }

    public void releaseConnection(Connection conn) {
        synchronized (connections) {
            connections.offer(conn);
        }
        semaphore.release();  // Free up slot
    }
}
```

---

## Phaser

**Matlab:** CyclicBarrier ka advanced version — dynamic number of threads, multiple phases.

```java
Phaser phaser = new Phaser(3);  // 3 registered parties

for (int i = 0; i < 3; i++) {
    new Thread(() -> {
        System.out.println("Thread starting phase 1");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        phaser.arriveAndAwaitAdvance();  // Wait for others

        System.out.println("Thread starting phase 2");
        try { Thread.sleep(1000); } catch (InterruptedException e) {}

        phaser.arriveAndAwaitAdvance();  // Wait for others

        System.out.println("Thread done");
        phaser.arriveAndDeregister();  // Unregister
    }).start();
}

// Wait for all phases
phaser.awaitAdvance(phaser.getPhase());
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **ExecutorService** | Thread pool — manually create karne ki jagah submit karo |
| **Callable\<T\>** | Runnable with return value + checked exceptions |
| **Future\<T\>** | Async task result — `get()` se result lo |
| **FixedThreadPool** | Fixed size threads — tasks queue hote hain |
| **CachedThreadPool** | Dynamic size — avoid in production |
| **ScheduledExecutorService** | Delayed/periodic tasks |
| **CountDownLatch** | One-time wait — multiple threads complete hone ka wait |
| **CyclicBarrier** | Reusable barrier — multiple phases |
| **Semaphore** | Resource access control — max N simultaneous |
| **Phaser** | Advanced barrier — dynamic parties, multiple phases |
