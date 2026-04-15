# @Async

## Status: Not Started

---

## Table of Contents

1. [@EnableAsync](#enableasync)
2. [@Async on Methods](#async-on-methods)
3. [ThreadPoolTaskExecutor Configuration](#threadpooltaskexecutor-configuration)
4. [Returning CompletableFuture\<T\>](#returning-completablefuturet)
5. [Exception Handling in @Async](#exception-handling-in-async)

---

## @EnableAsync

**Matlab:** Spring Boot mein async processing enable karna — proxy-based async method execution.

### Basic Setup

```java
@Configuration
@EnableAsync  // Async enable karo
public class AsyncConfig {
    // Default executor — SimpleAsyncTaskExecutor
    // Har call pe naya thread banata hai — production mein use mat karo
}
```

### How It Works

```
@EnableAsync → Spring AOP proxy create karta hai
→ @Async method call intercept hota hai
→ TaskExecutor ko submit hota hai
→ Separate thread pe method execute hota hai
→ Caller thread block nahi hota
```

---

## @Async on Methods

**Matlab:** Method ko asynchronously execute karna — caller thread wait nahi karega.

### Basic Usage

```java
@Service
public class EmailService {

    @Async  // Default executor pe run hoga
    public void sendEmail(String to, String subject, String body) {
        System.out.println("Sending email to: " + to);
        // Email sending logic — slow operation
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println("Email sent to: " + to);
    }
}

@Service
@RequiredArgsConstructor
public class UserService {

    private final EmailService emailService;

    public void registerUser(User user) {
        // User save karo
        userRepository.save(user);

        // Email async bhejo — caller block nahi hoga
        emailService.sendEmail(user.getEmail(), "Welcome!", "Welcome to our app");

        // Method immediately return hoga — email background mein bheji jayegi
        System.out.println("User registered, email sending in background");
    }
}
```

### ⚠️ Self-Invocation Problem

```java
@Service
public class ProblematicService {

    @Async
    public void asyncMethod() {
        System.out.println("Running async");
    }

    public void callerMethod() {
        // ❌ Yeh async nahi chalega — same class ka method call
        asyncMethod();  // Proxy bypass — direct call hoga

        // ✅ Fix: Self-inject ya ApplicationContext se call karo
    }
}

// ✅ Fix 1: Self-inject
@Service
public class FixedService {

    @Autowired
    private FixedService self;  // Proxy inject hoga

    @Async
    public void asyncMethod() {
        System.out.println("Running async");
    }

    public void callerMethod() {
        self.asyncMethod();  // ✅ Proxy ke through call — async chalega
    }
}

// ✅ Fix 2: Separate class
@Service
public class AsyncWorker {
    @Async
    public void doWork() { }
}

@Service
@RequiredArgsConstructor
public class BusinessService {
    private final AsyncWorker asyncWorker;

    public void process() {
        asyncWorker.doWork();  // ✅ Different bean — async chalega
    }
}
```

---

## ThreadPoolTaskExecutor Configuration

**Matlab:** Production-ready thread pool configure karna — simple async executor ka replacement.

### Basic Configuration

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core threads — hamesha alive rahenge
        executor.setCorePoolSize(5);

        // Max threads — queue full hone pe create honge
        executor.setMaxPoolSize(10);

        // Queue capacity — core threads busy hone pe tasks yahan wait karenge
        executor.setQueueCapacity(100);

        // Thread name prefix — debugging mein help karta hai
        executor.setThreadNamePrefix("EmailExecutor-");

        // Thread idle timeout — max threads kitni der tak alive rahenge
        executor.setKeepAliveSeconds(60);

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Shutdown timeout
        executor.setAwaitTerminationSeconds(30);

        // Rejected execution handler
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}
```

### Thread Pool Sizing

```
CPU-bound tasks (calculations, processing):
    threads = CPU cores + 1

IO-bound tasks (network, DB, file):
    threads = CPU cores * 2 (ya zyada)

Mixed workload:
    threads = CPU cores * (1 + waitTime/computeTime)

Example: 4 core CPU, IO-heavy app
    corePoolSize = 10
    maxPoolSize = 20
    queueCapacity = 200
```

### Multiple Executors

```java
@Configuration
@EnableAsync
public class MultiExecutorConfig {

    // Email executor — IO heavy
    @Bean(name = "emailExecutor")
    public ThreadPoolTaskExecutor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("Email-");
        executor.initialize();
        return executor;
    }

    // Report executor — CPU heavy
    @Bean(name = "reportExecutor")
    public ThreadPoolTaskExecutor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Report-");
        executor.initialize();
        return executor;
    }

    // Notification executor
    @Bean(name = "notificationExecutor")
    public ThreadPoolTaskExecutor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("Notification-");
        executor.initialize();
        return executor;
    }
}

// Usage
@Service
public class AsyncServices {

    @Async("emailExecutor")
    public void sendEmail(String to) { }

    @Async("reportExecutor")
    public CompletableFuture<Report> generateReport() { }

    @Async("notificationExecutor")
    public void sendNotification(String userId) { }
}
```

---

## Returning CompletableFuture\<T\>

**Matlab:** Async method se result return karna — caller ko future pe result milega.

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    @Async("reportExecutor")
    public CompletableFuture<Report> generateReport(Long userId) {
        System.out.println("Generating report in background...");

        // Expensive report generation
        Report report = reportRepository.generateForUser(userId);

        return CompletableFuture.completedFuture(report);
    }
}

// Usage
@Service
@RequiredArgsConstructor
public class UserController {

    private final ReportService reportService;

    public void requestReport(Long userId) throws Exception {
        // Async call — immediately returns Future
        CompletableFuture<Report> future = reportService.generateReport(userId);

        // Do other work...
        System.out.println("Report generation started, doing other work...");

        // Wait for result (blocking)
        Report report = future.get();  // Blocks until complete
        System.out.println("Report ready: " + report.getTitle());

        // Or with timeout
        try {
            Report report = future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            System.out.println("Report generation timed out");
            future.cancel(true);  // Interrupt
        }
    }
}
```

### Chaining Async Calls

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserService userService;
    private final Product productService;
    private final PaymentService paymentService;

    @Async("orderExecutor")
    public CompletableFuture<Order> processOrder(OrderRequest request) {
        // Multiple async calls chain karo
        CompletableFuture<User> userFuture = userService.getUserById(request.getUserId());
        CompletableFuture<Product> productFuture = productService.getProduct(request.getProductId());

        // Dono complete hone pe combine karo
        return userFuture.thenCombine(productFuture, (user, product) -> {
            // Validate
            if (!user.isActive()) {
                throw new IllegalArgumentException("User is not active");
            }

            // Process payment
            Payment payment = paymentService.processPayment(user, product, request.getAmount());

            // Create order
            return new Order(user, product, payment);
        });
    }
}
```

### Exception Handling with CompletableFuture

```java
@Async("taskExecutor")
public CompletableFuture<User> getUserWithException(Long id) {
    return CompletableFuture.supplyAsync(() -> {
        // Expensive operation
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    });
}

// Usage
CompletableFuture<User> future = userService.getUserWithException(1L);

future
    .thenAccept(user -> System.out.println("User: " + user.getName()))
    .exceptionally(ex -> {
        System.err.println("Error: " + ex.getCause().getMessage());
        return null;
    });
```

---

## Exception Handling in @Async

**Matlab:** Async method mein exception handle karna — default behavior exceptions ko silently swallow kar deta hai.

### The Problem

```java
@Async
public void asyncMethodWithException() {
    // Exception throw karo
    throw new RuntimeException("Something went wrong!");
    // ❌ Exception silently lost — caller ko pata nahi chalta
}
```

### Solution 1: AsyncUncaughtExceptionHandler

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            // Log karo
            log.error("Async error in method: {} with params: {}", 
                method.getName(), Arrays.toString(params), ex);

            // Alert bhejo
            // alertService.sendAlert("Async error: " + method.getName(), ex);
        };
    }

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Async-");
        executor.initialize();
        return executor;
    }
}
```

### Solution 2: CompletableFuture with Exception Handling

```java
@Async("taskExecutor")
public CompletableFuture<Void> asyncMethodWithHandling() {
    return CompletableFuture.runAsync(() -> {
        // Expensive operation
        doSomething();
    }).exceptionally(ex -> {
        log.error("Async method failed", ex);
        return null;
    });
}
```

### Solution 3: Try-Catch Inside Async Method

```java
@Async("emailExecutor")
public void sendEmailWithRetry(String to, String subject, String body) {
    int maxRetries = 3;
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            emailGateway.send(to, subject, body);
            log.info("Email sent to: {} on attempt {}", to, attempt);
            return;  // Success
        } catch (EmailException e) {
            log.warn("Email send attempt {} failed for {}: {}", attempt, to, e.getMessage());
            if (attempt == maxRetries) {
                log.error("All {} attempts failed for email to: {}", maxRetries, to);
                // Dead letter queue mein daalo
                deadLetterQueue.add(new FailedEmail(to, subject, body, e));
            }
            // Wait before retry
            try { Thread.sleep(1000 * attempt); } catch (InterruptedException ie) {}
        }
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **@EnableAsync** | Async processing enable karta hai — AOP proxy create hota hai |
| **@Async** | Method ko separate thread pe execute karta hai |
| **Self-Invocation** | Same class ka @Async method call nahi hoga — proxy bypass hota hai |
| **ThreadPoolTaskExecutor** | Production-ready thread pool — core/max pool size, queue capacity |
| **CompletableFuture\<T\>** | Async method se result return karo — chaining possible |
| **AsyncUncaughtExceptionHandler** | Void @Async methods ke exceptions handle karo |
| **Multiple Executors** | Alag-alag executors different workloads ke liye |
| **Rejected Execution** | Queue full hone pe CallerRunsPolicy use karo |
