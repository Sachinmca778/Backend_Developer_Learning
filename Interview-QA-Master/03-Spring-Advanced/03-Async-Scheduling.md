# Async & Scheduling - Top 10 Interview Questions & Answers

> Covering: @Async, @Scheduled, Spring Events, TaskExecutor, ThreadPoolTaskExecutor, CompletableFuture, and Graceful Shutdown

---

## Q1: @EnableAsync aur @Async kya karte hain?

**Answer:**

`@EnableAsync` Spring mein asynchronous processing enable karta hai. `@Async` method ko alag thread pe execute karta hai — caller thread block nahi hota.

### Setup:

```java
@Configuration
@EnableAsync
public class AsyncConfig { }
```

### How It Works:

```
@EnableAsync → Spring AOP proxy create karta hai
→ @Async method call intercept hota hai
→ TaskExecutor ko task submit hota hai
→ Separate thread pe method execute hota hai
→ Caller immediately return ho jata hai
```

### Basic Usage:

```java
@Service
public class EmailService {

    @Async
    public void sendEmail(String to, String subject, String body) {
        // Slow operation — background mein chalega
        emailGateway.send(to, subject, body);
    }
}

@Service
@RequiredArgsConstructor
public class UserService {

    private final EmailService emailService;

    public void registerUser(User user) {
        userRepository.save(user);
        emailService.sendEmail(user.getEmail(), "Welcome!", "Welcome to our app");
        // Method immediately return — email background mein bheji jayegi
    }
}
```

**Important:**

Default executor `SimpleAsyncTaskExecutor` hai — har call pe naya thread banata hai. Production mein `ThreadPoolTaskExecutor` configure karna chahiye.

---

## Q2: @Async mein Self-Invocation problem kya hai aur fix kaise karte hain?

**Answer:**

Same class ke andar se `@Async` method call karne pe async nahi chalega — proxy bypass ho jata hai.

### Problem:

```java
@Service
public class ProblematicService {

    @Async
    public void asyncMethod() {
        System.out.println("Running async");
    }

    public void callerMethod() {
        asyncMethod();  // ❌ Proxy bypass — sync call hoga!
    }
}
```

Yeh `@Transactional` ke self-invocation problem jaisa hi hai.

### Fix 1: Self-Inject:

```java
@Service
public class FixedService {

    @Autowired
    private FixedService self;  // Proxy inject hoga

    @Async
    public void asyncMethod() { }

    public void callerMethod() {
        self.asyncMethod();  // ✅ Proxy ke through — async chalega
    }
}
```

### Fix 2: Separate Class:

```java
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

**Short Interview Answer:**

`@Async` sirf external bean call pe kaam karta hai. Same class ke andar call pe proxy bypass hota hai — self-inject ya alag service class use karo.

---

## Q3: ThreadPoolTaskExecutor kaise configure karte hain?

**Answer:**

Production mein `ThreadPoolTaskExecutor` use karo — thread pool size, queue capacity, aur rejection policy control kar sakte ho.

### Configuration:

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // Hamesha alive threads
        executor.setMaxPoolSize(10);       // Queue full hone pe extra threads
        executor.setQueueCapacity(100);    // Waiting tasks queue
        executor.setThreadNamePrefix("EmailExecutor-");
        executor.setKeepAliveSeconds(60);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
```

### Thread Pool Sizing:

| Workload | Formula |
|----------|---------|
| CPU-bound | `CPU cores + 1` |
| IO-bound (email, DB, network) | `CPU cores * 2` (ya zyada) |

### Rejection Handlers:

| Policy | Behavior |
|--------|----------|
| **AbortPolicy** (default) | `RejectedExecutionException` throw |
| **CallerRunsPolicy** | Caller thread pe execute (backpressure) |
| **DiscardPolicy** | Silently discard |
| **DiscardOldestPolicy** | Oldest queued task discard |

### Multiple Executors:

```java
@Async("emailExecutor")
public void sendEmail(String to) { }

@Async("reportExecutor")
public CompletableFuture<Report> generateReport() { }
```

Different workloads ke liye alag thread pools — isolation aur resource control.

---

## Q4: @Async method se CompletableFuture kaise return karte hain?

**Answer:**

`void` async methods fire-and-forget hote hain. Result chahiye ho toh `CompletableFuture<T>` return karo.

### Basic Usage:

```java
@Async("reportExecutor")
public CompletableFuture<Report> generateReport(Long userId) {
    Report report = reportRepository.generateForUser(userId);
    return CompletableFuture.completedFuture(report);
}

// Caller side
CompletableFuture<Report> future = reportService.generateReport(1L);

// Do other work...
Report report = future.get();  // Blocking wait

// With timeout
Report report = future.get(30, TimeUnit.SECONDS);
```

### Chaining:

```java
CompletableFuture<User> userFuture = userService.getUserById(userId);
CompletableFuture<Product> productFuture = productService.getProduct(productId);

return userFuture.thenCombine(productFuture, (user, product) -> {
    return new Order(user, product);
});
```

### Exception Handling:

```java
future
    .thenAccept(result -> log.info("Success: {}", result))
    .exceptionally(ex -> {
        log.error("Error: {}", ex.getMessage());
        return null;
    });
```

**Note:** `CompletableFuture` return karne se caller ko result access karne ka control milta hai — unlike void async methods jahan exceptions silently lost ho sakti hain.

---

## Q5: @Async methods mein exception handling kaise karte hain?

**Answer:**

Default behavior mein void `@Async` methods ki exceptions silently swallow ho jati hain — caller ko pata nahi chalta.

### Problem:

```java
@Async
public void asyncMethod() {
    throw new RuntimeException("Something went wrong!");
    // ❌ Exception lost — caller ko pata nahi
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
            log.error("Async error in {}: {}", method.getName(), ex.getMessage(), ex);
        };
    }
}
```

### Solution 2: CompletableFuture

```java
@Async
public CompletableFuture<Void> asyncWithHandling() {
    return CompletableFuture.runAsync(() -> doSomething())
        .exceptionally(ex -> {
            log.error("Failed", ex);
            return null;
        });
}
```

### Solution 3: Try-Catch Inside Method

```java
@Async("emailExecutor")
public void sendEmailWithRetry(String to) {
    for (int attempt = 1; attempt <= 3; attempt++) {
        try {
            emailGateway.send(to);
            return;
        } catch (EmailException e) {
            if (attempt == 3) {
                deadLetterQueue.add(new FailedEmail(to, e));
            }
        }
    }
}
```

---

## Q6: @Scheduled — fixedRate, fixedDelay aur cron mein kya difference hai?

**Answer:**

### Setup:

```java
@Configuration
@EnableScheduling
public class SchedulingConfig { }
```

### fixedRate — Last START se interval:

```java
@Scheduled(fixedRate = 5000)  // Har 5 seconds pe (last start se)
public void collectMetrics() { }
```

```
T+0s: Start (2s duration)
T+2s: Complete
T+5s: Start again (5s from last START)
```

Use: Regular monitoring, health checks — execution time matter nahi karta.

### fixedDelay — Last END se interval:

```java
@Scheduled(fixedDelay = 10000)  // Complete hone ke 10s baad
public void processBatch() { }
```

```
T+0s: Start (3s duration)
T+3s: Complete
T+13s: Start again (10s after COMPLETION)
```

Use: Batch processing — previous task complete hone ke baad next start.

### Comparison:

| Feature | fixedRate | fixedDelay |
|---------|-----------|------------|
| Measurement | Last START | Last END |
| Overlapping | Possible | Never |
| Use Case | Monitoring | Batch jobs |

### cron — Complex scheduling:

```java
@Scheduled(cron = "0 0 9 * * ?")  // Daily 9 AM
public void generateDailyReport() { }

@Scheduled(cron = "0 */15 * * * ?")  // Every 15 minutes
public void checkAlerts() { }
```

### Cron Format:

```
second minute hour dayOfMonth month dayOfWeek
0      0      9    *          *     ?
```

| Expression | Meaning |
|------------|---------|
| `0 0 9 * * ?` | Daily 9 AM |
| `0 */5 * * * ?` | Every 5 minutes |
| `0 0 9 * * MON-FRI` | Weekdays 9 AM |

### initialDelay & zone:

```java
@Scheduled(initialDelay = 30000, fixedRate = 300000)  // 30s delay, then every 5 min

@Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")  // IST timezone
```

---

## Q7: Spring Events kaise kaam karte hain?

**Answer:**

Spring Events loosely coupled communication provide karte hain — components directly depend nahi karte.

### Publish Event:

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public User registerUser(UserRegistrationRequest request) {
        User user = userRepository.save(new User(request.getName(), request.getEmail()));
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));
        return user;
    }
}
```

### Listen Event:

```java
@Component
public class EmailService {

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        sendWelcomeEmail(event.getUser().getEmail());
    }
}

@Component
public class AuditService {

    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        auditLog("User registered: " + event.getUser().getName());
    }
}
```

### Flow:

```
UserService.registerUser()
    ↓
eventPublisher.publishEvent(UserRegisteredEvent)
    ↓
EmailService.onUserRegistered() → Send email
AuditService.onUserRegistered() → Log audit
```

### Conditional Listening:

```java
@EventListener(condition = "#orderEvent.totalAmount > 1000")
public void onHighValueOrder(OrderCreatedEvent orderEvent) { }
```

### Async Events:

```java
@Async("eventExecutor")
@EventListener
public void onUserRegistered(UserRegisteredEvent event) {
    // Separate thread pe execute
}
```

---

## Q8: @TransactionalEventListener kya hai aur kab use karte hain?

**Answer:**

Jab event listener ko transaction ke specific phase mein run karna ho — data consistency ke liye.

### Problem:

```java
@Transactional
public User registerUser(UserRegistrationRequest request) {
    User user = userRepository.save(user);
    eventPublisher.publishEvent(new UserRegisteredEvent(this, user));
    // ❌ Event immediately handle — DB commit nahi hua!
    // EmailService user query kare toh UserNotFoundException aa sakta hai
    return user;
}
// Transaction yahan commit hota hai
```

### Solution:

```java
@Component
public class EmailService {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        // Ab user DB mein commit ho chuka hai — safe to query
        sendWelcomeEmail(event.getUser().getEmail());
    }
}
```

### Transaction Phases:

| Phase | When | Use Case |
|-------|------|----------|
| **BEFORE_COMMIT** | Commit se pehle | Final validation |
| **AFTER_COMMIT** (default) | Commit ke baad | Email, notifications |
| **AFTER_ROLLBACK** | Rollback ke baad | Cleanup, compensating actions |
| **AFTER_COMPLETION** | Commit ya rollback ke baad | Resource cleanup |

### fallbackExecution:

```java
@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
public void handleEvent(MyEvent event) { }
// No transaction pe bhi execute hoga
```

---

## Q9: TaskExecutor aur graceful shutdown kaise implement karte hain?

**Answer:**

### TaskExecutor Hierarchy:

```
Executor → TaskExecutor → AsyncTaskExecutor → ThreadPoolTaskExecutor
```

| Implementation | Use Case |
|----------------|----------|
| **SimpleAsyncTaskExecutor** | Testing only — har task pe naya thread |
| **ThreadPoolTaskExecutor** | ✅ Production recommended |
| **SyncTaskExecutor** | Same thread — synchronous fallback |

### Graceful Shutdown:

```java
@Bean
public ThreadPoolTaskExecutor gracefulExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setWaitForTasksToCompleteOnShutdown(true);  // Pending tasks complete
    executor.setAwaitTerminationSeconds(30);           // Max 30s wait
    executor.initialize();
    return executor;
}
```

### Spring Boot Properties:

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

### Shutdown Order:

```
1. Stop accepting new HTTP requests
2. Complete in-flight HTTP requests
3. @Async tasks complete
4. @Scheduled tasks complete
5. Bean destroy methods
6. JVM shutdown
```

### Monitoring via Actuator:

```
GET /actuator/metrics/executor.active
GET /actuator/metrics/executor.completed
GET /actuator/metrics/executor.queue.size
```

---

## Q10: Async aur Scheduling ke best practices kya hain?

**Answer:**

### Async Best Practices:

| Practice | Description |
|----------|-------------|
| **ThreadPoolTaskExecutor** | Production mein SimpleAsyncTaskExecutor avoid karo |
| **Multiple executors** | IO-heavy aur CPU-heavy tasks alag pools mein |
| **Self-invocation fix** | Self-inject ya separate service class |
| **Exception handling** | AsyncUncaughtExceptionHandler ya CompletableFuture |
| **Graceful shutdown** | `waitForTasksToCompleteOnShutdown=true` |
| **MDC propagation** | Async threads mein correlation ID pass karo |

### Scheduling Best Practices:

| Practice | Description |
|----------|-------------|
| **fixedDelay for batch** | Overlapping avoid karo |
| **cron for specific times** | Reports, cleanup jobs |
| **initialDelay** | App startup load kam karo |
| **zone attribute** | Timezone specify karo |
| **Multi-threaded scheduler** | Parallel scheduled tasks ke liye |

### Events Best Practices:

| Practice | Description |
|----------|-------------|
| **@TransactionalEventListener** | DB-dependent listeners ke liye AFTER_COMMIT |
| **@Async for slow listeners** | Email, SMS — publisher block mat karo |
| **@Order** | Listener execution order control karo |
| **Loose coupling** | Direct service calls ki jagah events |

**Short Interview Answer:**

Production async = ThreadPoolTaskExecutor + exception handling + graceful shutdown. Scheduling = fixedDelay for batches, cron for time-based jobs. Events = AFTER_COMMIT for DB-dependent side effects.
