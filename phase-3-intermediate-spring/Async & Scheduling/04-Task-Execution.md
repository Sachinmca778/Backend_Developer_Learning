# Task Execution

## Status: Not Started

---

## Table of Contents

1. [TaskExecutor Interface](#taskexecutor-interface)
2. [ThreadPoolTaskExecutor](#threadpooltaskexecutor)
3. [@Async with Multiple Executors](#async-with-multiple-executors)
4. [Monitoring Thread Pools via Actuator](#monitoring-thread-pools-via-actuator)
5. [Graceful Shutdown of Executors](#graceful-shutdown-of-executors)

---

## TaskExecutor Interface

**Matlab:** Spring ka TaskExecutor — Java ke Executor interface ka wrapper, exception handling aur lifecycle management ke saath.

### Hierarchy

```
java.util.concurrent.Executor
    ↑
org.springframework.core.task.TaskExecutor (Spring wrapper)
    ↑
org.springframework.core.task.AsyncTaskExecutor (async support)
    ↑
org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor (implementation)
```

### Built-in Implementations

| Implementation | Description | Use Case |
|---------------|-------------|----------|
| **SimpleAsyncTaskExecutor** | Har task pe naya thread | Testing only — production mein avoid |
| **SyncTaskExecutor** | Same thread pe execute | Synchronous fallback |
| **ConcurrentTaskExecutor** | java.util.concurrent.Executor wrapper | Existing Executor integration |
| **ThreadPoolTaskExecutor** | Configurable thread pool | ✅ Production recommended |
| **WorkManagerTaskExecutor** | CommonJ WorkManager | Legacy WebSphere/WebLogic |

### Basic Usage

```java
@Service
@RequiredArgsConstructor
public class TaskExecutorService {

    private final TaskExecutor taskExecutor;  // Auto-injected

    public void executeTask() {
        taskExecutor.execute(() -> {
            System.out.println("Running in thread: " + Thread.currentThread().getName());
            // Background task
        });
    }
}
```

---

## ThreadPoolTaskExecutor

**Matlab:** Production-ready thread pool — configurable, monitorable, lifecycle management ke saath.

### Configuration

```java
@Configuration
public class TaskExecutorConfig {

    @Bean(name = "defaultTaskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core threads — hamesha alive
        executor.setCorePoolSize(5);

        // Max threads — queue full hone pe create honge
        executor.setMaxPoolSize(15);

        // Queue capacity — tasks yahan wait karenge
        executor.setQueueCapacity(100);

        // Thread name prefix — debugging
        executor.setThreadNamePrefix("DefaultTask-");

        // Idle thread timeout (seconds)
        executor.setKeepAliveSeconds(60);

        // Allow core thread timeout
        executor.setAllowCoreThreadTimeOut(false);

        executor.initialize();
        return executor;
    }
}
```

### Thread Pool Sizing Formula

```java
// CPU-bound tasks
int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;

// IO-bound tasks
int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;

// Mixed workload
// threads = CPU_cores * (1 + wait_time / compute_time)
// Example: 4 cores, 90% wait time, 10% compute
// threads = 4 * (1 + 90/10) = 4 * 10 = 40
```

### Queue Capacity Impact

```
Queue Capacity: Small (10-50)
    → Tasks overflow to maxPoolSize quickly
    → More threads, higher memory usage
    → Faster rejection when overloaded

Queue Capacity: Large (500-1000)
    → Tasks queue hote hain
    → Fewer threads, lower memory usage
    → Slower rejection, potential memory issues
```

### Rejection Handlers

```java
// Default: AbortPolicy — throws RejectedExecutionException
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

// CallerRunsPolicy — caller thread pe execute karo (backpressure)
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

// DiscardPolicy — silently discard
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());

// DiscardOldestPolicy — oldest task discard karo
executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

// Custom handler
executor.setRejectedExecutionHandler((r, executor) -> {
    log.warn("Task rejected, queuing to dead letter queue");
    deadLetterQueue.add(r);
});
```

---

## @Async with Multiple Executors

**Matlab:** Different executors different workloads ke liye — isolation aur resource control.

### Multiple Executor Configuration

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
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // Report executor — CPU heavy
    @Bean(name = "reportExecutor")
    public ThreadPoolTaskExecutor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Report-");
        executor.initialize();
        return executor;
    }

    // Notification executor — high throughput
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
```

### Using Specific Executors

```java
@Service
public class AsyncServices {

    // Specific executor use karo
    @Async("emailExecutor")
    public void sendEmail(String to, String subject, String body) {
        System.out.println("Thread: " + Thread.currentThread().getName());
        // Email logic
    }

    @Async("reportExecutor")
    public CompletableFuture<Report> generateReport(Long userId) {
        // Report generation logic
        return CompletableFuture.completedFuture(new Report());
    }

    @Async("notificationExecutor")
    public void sendPushNotification(String userId, String message) {
        // Push notification logic
    }

    // Default executor (agar specify nahi kiya)
    @Async
    public void defaultAsyncTask() {
        // Default TaskExecutor use hoga
    }
}
```

### Executor Selection Strategy

```java
@Service
@RequiredArgsConstructor
public class SmartAsyncService {

    private final ThreadPoolTaskExecutor emailExecutor;
    private final ThreadPoolTaskExecutor reportExecutor;
    private final ThreadPoolTaskExecutor notificationExecutor;

    public void processTask(Task task) {
        switch (task.getType()) {
            case EMAIL -> emailExecutor.execute(() -> handleEmail(task));
            case REPORT -> reportExecutor.execute(() -> handleReport(task));
            case NOTIFICATION -> notificationExecutor.execute(() -> handleNotification(task));
            default -> throw new IllegalArgumentException("Unknown task type");
        }
    }
}
```

---

## Monitoring Thread Pools via Actuator

**Matlab:** Thread pool metrics expose karna — health, utilization, rejection monitoring.

### Setup

```xml
<!-- Actuator dependency -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Expose Metrics

```java
@Configuration
@EnableAsync
public class MonitoringConfig {

    @Bean(name = "monitoredExecutor")
    public ThreadPoolTaskExecutor monitoredExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Monitored-");

        // Metrics enable karo
        executor.setThreadNamePrefix("Monitored-");
        
        executor.initialize();
        return executor;
    }
}
```

### Actuator Metrics Endpoint

```bash
# Available metrics dekho
GET /actuator/metrics

# Thread pool specific metrics
GET /actuator/metrics/executor.active
GET /actuator/metrics/executor.completed
GET /actuator/metrics/executor.queued
GET /actuator/metrics/executor.pool.core
GET /actuator/metrics/executor.pool.max
GET /actuator/metrics/executor.pool.size
GET /actuator/metrics/executor.queue.remaining
GET /actuator/metrics/executor.queue.capacity
```

### Custom Health Indicator

```java
@Component
public class ThreadPoolHealthIndicator implements HealthIndicator {

    private final ThreadPoolTaskExecutor taskExecutor;

    public ThreadPoolHealthIndicator(ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public Health health() {
        ThreadPoolExecutor pool = taskExecutor.getThreadPoolExecutor();

        int activeThreads = pool.getActiveCount();
        int poolSize = pool.getPoolSize();
        int queueSize = pool.getQueue().size();
        int queueRemaining = pool.getQueue().remainingCapacity();

        Map<String, Object> details = Map.of(
            "activeThreads", activeThreads,
            "poolSize", poolSize,
            "queueSize", queueSize,
            "queueRemaining", queueRemaining,
            "completedTasks", pool.getCompletedTaskCount()
        );

        // Health check logic
        if (queueRemaining < 10) {
            return Health.down().withDetails(details).build();
        }
        if (poolSize >= taskExecutor.getMaxPoolSize()) {
            return Health.down().withDetails(details).build();
        }

        return Health.up().withDetails(details).build();
    }
}

// Access via
GET /actuator/health
{
  "status": "UP",
  "components": {
    "threadPool": {
      "status": "UP",
      "details": {
        "activeThreads": 3,
        "poolSize": 5,
        "queueSize": 20,
        "queueRemaining": 80
      }
    }
  }
}
```

### Micrometer Integration

```java
@Component
public class ThreadPoolMetrics {

    private final MeterRegistry meterRegistry;
    private final ThreadPoolTaskExecutor taskExecutor;

    public ThreadPoolMetrics(MeterRegistry meterRegistry, ThreadPoolTaskExecutor taskExecutor) {
        this.meterRegistry = meterRegistry;
        this.taskExecutor = taskExecutor;
    }

    @PostConstruct
    public void registerMetrics() {
        Gauge.builder("thread.pool.size", taskExecutor, 
                e -> e.getThreadPoolExecutor().getPoolSize())
            .register(meterRegistry);

        Gauge.builder("thread.pool.active", taskExecutor, 
                e -> e.getThreadPoolExecutor().getActiveCount())
            .register(meterRegistry);

        Gauge.builder("thread.queue.size", taskExecutor,
                e -> e.getThreadPoolExecutor().getQueue().size())
            .register(meterRegistry);

        Gauge.builder("thread.pool.completed", taskExecutor,
                e -> e.getThreadPoolExecutor().getCompletedTaskCount())
            .register(meterRegistry);
    }
}
```

---

## Graceful Shutdown of Executors

**Matlab:** Application shutdown pe running tasks ko safely complete karna — data loss avoid karna.

### Configuration

```java
@Bean(name = "gracefulExecutor")
public ThreadPoolTaskExecutor gracefulExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("Graceful-");

    // Graceful shutdown settings
    executor.setWaitForTasksToCompleteOnShutdown(true);  // Pending tasks complete hone do
    executor.setAwaitTerminationSeconds(30);             // Max 30 seconds wait

    executor.initialize();
    return executor;
}
```

### Shutdown Behavior

```
Without graceful shutdown:
    Application stop → All threads immediately killed
    → Running tasks interrupted
    → Data loss possible
    → Queue tasks lost

With graceful shutdown:
    Application stop signal
    ↓
    No new tasks accepted
    ↓
    Running tasks complete (up to 30s)
    ↓
    Queue tasks complete
    ↓
    ThreadPool shutdown
    ↓
    Application exits
```

### Custom Shutdown Hook

```java
@Component
@RequiredArgsConstructor
public class CustomShutdownHook {

    private final ThreadPoolTaskExecutor taskExecutor;

    @PreDestroy
    public void onShutdown() {
        System.out.println("Shutting down executor gracefully...");

        taskExecutor.shutdown();  // No new tasks, wait for completion

        try {
            if (!taskExecutor.getThreadPoolExecutor().awaitTermination(30, TimeUnit.SECONDS)) {
                System.out.println("Forcing shutdown...");
                taskExecutor.getThreadPoolExecutor().shutdownNow();  // Force interrupt
            }
            System.out.println("Executor shutdown complete");
        } catch (InterruptedException e) {
            taskExecutor.getThreadPoolExecutor().shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### Spring Boot 2.3+ Graceful Shutdown

```properties
# application.properties

# Server graceful shutdown
server.shutdown=graceful

# Shutdown timeout — pending requests + async tasks complete hone do
spring.lifecycle.timeout-per-shutdown-phase=30s
```

```java
// Spring Boot automatically respects these settings
// @Async tasks bhi gracefully shutdown honge
```

### Shutdown Order

```
1. Stop accepting new HTTP requests
2. Complete in-flight HTTP requests
3. @Async tasks complete (wait for termination)
4. @Scheduled tasks complete
5. ApplicationEvent listeners notified
6. Bean destroy methods called
7. JVM shutdown
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **TaskExecutor** | Spring's Executor interface — lifecycle management |
| **ThreadPoolTaskExecutor** | Production-ready — configurable pool size, queue, rejection policy |
| **Multiple Executors** | Different workloads ke liye alag pools — isolation |
| **@Async("executorName")** | Specific executor use karo |
| **Rejection Handlers** | AbortPolicy, CallerRunsPolicy, DiscardPolicy |
| **Actuator Metrics** | executor.active, executor.completed, executor.queue.size |
| **Health Indicator** | Thread pool status monitor karo |
| **Graceful Shutdown** | `setWaitForTasksToCompleteOnShutdown(true)` + `setAwaitTerminationSeconds()` |
| **Spring Boot Graceful** | `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase` |
