# CompletableFuture

## Status: Not Started

---

## Table of Contents

1. [supplyAsync & runAsync](#supplyasync--runasync)
2. [thenApply, thenAccept, thenRun](#thenapply-thenaccept-thenrun)
3. [thenCompose & thenCombine](#thencompose--thencombine)
4. [exceptionally & handle](#exceptionally--handle)
5. [allOf & anyOf](#allof--anyof)
6. [join & get](#join--get)

---

## supplyAsync & runAsync

**Matlab:** Asynchronous tasks start karna — background mein execute hote hain.

### supplyAsync — Value Return Karta Hai

```java
// Async task — value return karta hai
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // Background thread pe run hoga
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    return "Result";
});

// Result wait karo
String result = future.get();  // "Result" (blocks until complete)
```

### runAsync — Value Return Nahi Karta

```java
// Async task — void return
CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
    System.out.println("Running in background");
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    System.out.println("Done");
});

future.get();  // Blocks until complete
```

### Custom Executor

```java
// Default ForkJoinPool use hota hai — custom executor bhi de sakte ho
ExecutorService executor = Executors.newFixedThreadPool(10);

CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    return "Result";
}, executor);

// Executor shutdown mat bhoolna
future.thenRun(executor::shutdown);
```

---

## thenApply, thenAccept, thenRun

**Matlab:** CompletableFuture chain karna — ek task complete hone pe next task trigger karna.

### thenApply — Transform Result

```java
CompletableFuture<Integer> future = CompletableFuture
    .supplyAsync(() -> 10)
    .thenApply(n -> n * 2)      // 10 → 20
    .thenApply(n -> n + 5);     // 20 → 25

System.out.println(future.get());  // 25
```

### thenAccept — Consume Result

```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> "Hello")
    .thenAccept(msg -> System.out.println("Received: " + msg));

future.get();  // "Received: Hello"
```

### thenRun — Action After Completion

```java
CompletableFuture<Void> future = CompletableFuture
    .supplyAsync(() -> {
        System.out.println("Task running");
        return "Result";
    })
    .thenRun(() -> System.out.println("Task completed"));

future.get();
// Output:
// Task running
// Task completed
```

### thenApplyAsync — Different Thread

```java
// thenApply — same thread pe run hoga
CompletableFuture<Integer> f1 = CompletableFuture
    .supplyAsync(() -> 10)
    .thenApply(n -> n * 2);  // Same thread

// thenApplyAsync — alag thread pe run hoga
CompletableFuture<Integer> f2 = CompletableFuture
    .supplyAsync(() -> 10)
    .thenApplyAsync(n -> n * 2);  // New thread
```

---

## thenCompose & thenCombine

### thenCompose — FlatMap Jaisa

**Matlab:** Ek CompletableFuture ke result se dusra CompletableFuture banana — chaining.

```java
// Method jo CompletableFuture return karta hai
CompletableFuture<String> getUserById(Long id) {
    return CompletableFuture.supplyAsync(() -> "User-" + id);
}

CompletableFuture<String> getEmail(String username) {
    return CompletableFuture.supplyAsync(() -> username + "@mail.com");
}

// thenCompose — chain async operations
CompletableFuture<String> emailFuture = getUserById(1L)
    .thenCompose(username -> getEmail(username));

System.out.println(emailFuture.get());  // "User-1@mail.com"

// ❌ thenApply se — nested CompletableFuture aayega
// CompletableFuture<CompletableFuture<String>> nested = getUserById(1L).thenApply(this::getEmail);
```

### thenCombine — Do Futures Combine Karna

```java
CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> "Sachin");
CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> 25);

// Dono complete hone pe combine karo
CompletableFuture<String> combined = nameFuture.thenCombine(ageFuture,
    (name, age) -> name + " is " + age + " years old");

System.out.println(combined.get());  // "Sachin is 25 years old"
```

### thenAcceptBoth

```java
CompletableFuture<String> nameFuture = CompletableFuture.supplyAsync(() -> "Sachin");
CompletableFuture<Integer> ageFuture = CompletableFuture.supplyAsync(() -> 25);

// Consume combined result
nameFuture.thenAcceptBoth(ageFuture, (name, age) ->
    System.out.println(name + " is " + age + " years old"));
```

---

## exceptionally & handle

### exceptionally — Error Recovery

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        if (true) throw new RuntimeException("Something went wrong!");
        return "Result";
    })
    .exceptionally(ex -> "Fallback: " + ex.getMessage());

System.out.println(future.get());  // "Fallback: java.lang.RuntimeException: Something went wrong!"
```

### handle — Always Executes

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        // if (true) throw new RuntimeException("Error!");
        return "Success";
    })
    .handle((result, ex) -> {
        if (ex != null) {
            return "Error: " + ex.getMessage();
        }
        return "Result: " + result;
    });

System.out.println(future.get());  // "Result: Success" (ya error message)
```

### exceptionally vs handle

```
exceptionally: Sirf exception pe execute hota hai
handle: Hamesha execute hota hai (success ya failure dono pe)
```

---

## allOf & anyOf

### allOf — Sab Complete Hone Pe

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> "Result 1");
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> "Result 2");
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> "Result 3");

// Sab complete hone pe trigger
CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);

all.thenRun(() -> {
    System.out.println("All tasks completed");
    System.out.println(f1.join());  // "Result 1"
    System.out.println(f2.join());  // "Result 2"
    System.out.println(f3.join());  // "Result 3"
});

all.get();  // Blocks until all complete
```

### anyOf — Koi Ek Complete Hone Pe

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(3000); } catch (InterruptedException e) {}
    return "Slow Result";
});

CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    return "Fast Result";
});

// Pehla complete hone pe trigger
CompletableFuture<Object> any = CompletableFuture.anyOf(f1, f2);

System.out.println(any.get());  // "Fast Result" (pehle complete hua)
```

---

## join & get

### join

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "Result");

// join — checked exception wrap nahi karta
String result = future.join();  // "Result"

// get — checked exception throw karta hai (InterruptedException, ExecutionException)
try {
    String result2 = future.get();  // "Result"
} catch (InterruptedException | ExecutionException e) {
    e.printStackTrace();
}
```

### get with Timeout

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    try { Thread.sleep(5000); } catch (InterruptedException e) {}
    return "Slow Result";
});

// Timeout ke saath wait karo
try {
    String result = future.get(2, TimeUnit.SECONDS);  // 2 seconds wait
} catch (TimeoutException e) {
    System.out.println("Timed out!");
}

// join ke saath timeout nahi — orTimeout use karo (Java 9+)
CompletableFuture<String> futureWithTimeout = CompletableFuture
    .supplyAsync(() -> {
        try { Thread.sleep(5000); } catch (InterruptedException e) {}
        return "Slow Result";
    })
    .orTimeout(2, TimeUnit.SECONDS);  // 2 seconds baad TimeoutException

try {
    futureWithTimeout.join();
} catch (CompletionException e) {
    System.out.println("Timed out!");
}
```

---

## Real-World Example

```java
class OrderService {
    
    CompletableFuture<User> getUser(Long userId) {
        return CompletableFuture.supplyAsync(() -> {
            // DB call
            return new User("Sachin", 25);
        });
    }
    
    CompletableFuture<Product> getProduct(Long productId) {
        return CompletableFuture.supplyAsync(() -> {
            // DB call
            return new Product("Laptop", 50000);
        });
    }
    
    CompletableFuture<Order> createOrder(Long userId, Long productId) {
        return getUser(userId)
            .thenCombine(getProduct(productId), (user, product) -> {
                // Order create
                return new Order(user, product);
            })
            .exceptionally(ex -> {
                System.err.println("Order creation failed: " + ex.getMessage());
                return null;
            });
    }
}

// Usage
OrderService service = new OrderService();
CompletableFuture<Order> orderFuture = service.createOrder(1L, 1L);
orderFuture.thenAccept(order -> {
    if (order != null) {
        System.out.println("Order created: " + order);
    }
});
```

---

## Summary

| Method | Purpose | Returns |
|--------|---------|---------|
| **supplyAsync** | Async task with return value | `CompletableFuture<T>` |
| **runAsync** | Async task without return value | `CompletableFuture<Void>` |
| **thenApply** | Transform result | `CompletableFuture<R>` |
| **thenAccept** | Consume result | `CompletableFuture<Void>` |
| **thenRun** | Action after completion | `CompletableFuture<Void>` |
| **thenCompose** | Chain async operations | `CompletableFuture<R>` |
| **thenCombine** | Combine two futures | `CompletableFuture<V>` |
| **exceptionally** | Error recovery | `CompletableFuture<T>` |
| **handle** | Always execute (success/failure) | `CompletableFuture<R>` |
| **allOf** | Wait for all futures | `CompletableFuture<Void>` |
| **anyOf** | Wait for any one future | `CompletableFuture<Object>` |
| **join** | Get result (runtime exception) | `T` |
| **get** | Get result (checked exception) | `T` |
