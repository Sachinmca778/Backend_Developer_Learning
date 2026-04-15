# @Scheduled

## Status: Not Started

---

## Table of Contents

1. [@EnableScheduling](#enablescheduling)
2. [@Scheduled(fixedRate)](#scheduledfixedrate)
3. [@Scheduled(fixedDelay)](#scheduledfixeddelay)
4. [@Scheduled(cron)](#scheduledcron)
5. [Cron Expression Syntax](#cron-expression-syntax)
6. [initialDelay & zone](#initialdelay--zone)
7. [Dynamic Scheduling](#dynamic-scheduling)

---

## @EnableScheduling

**Matlab:** Spring Boot mein scheduled task execution enable karna.

### Basic Setup

```java
@Configuration
@EnableScheduling  // Scheduling enable karo
public class SchedulingConfig {
    // Default: single-threaded scheduler
    // All scheduled tasks run sequentially on one thread
}
```

### How It Works

```
@EnableScheduling → ScheduledAnnotationBeanPostProcessor register hota hai
→ @Scheduled methods scan hote hain
→ TaskScheduler ke saath register hote hain
→ Specified time pe method execute hota hai
```

---

## @Scheduled(fixedRate)

**Matlab:** Fixed interval pe method execute karo — last execution start time se measure hota hai.

### Basic Usage

```java
@Service
public class MetricsService {

    // Har 5 seconds pe execute hoga
    @Scheduled(fixedRate = 5000)
    public void collectMetrics() {
        System.out.println("Collecting metrics at: " + LocalTime.now());
        // Metrics collection logic
    }
}
```

### fixedRate Behavior

```
Execution timeline:
T+0s:    Task starts (takes 2s)
T+2s:    Task completes
T+5s:    Task starts again (5s from last START)
T+7s:    Task completes
T+10s:   Task starts again
...

Key: Next execution 5 seconds after PREVIOUS START, regardless of duration
```

### Use Cases

```java
// ✅ Use fixedRate when:
// - Regular interval pe execute karna hai
// - Execution time matter nahi karta
// - Monitoring, metrics, health checks

@Scheduled(fixedRate = 10000)  // Every 10 seconds
public void checkHealth() { }

@Scheduled(fixedRate = 60000)  // Every minute
public void collectMetrics() { }
```

---

## @Scheduled(fixedDelay)

**Matlab:** Fixed interval pe method execute karo — last execution END time se measure hota hai.

### Basic Usage

```java
@Service
public class BatchService {

    // Task complete hone ke 10 seconds baad next execution
    @Scheduled(fixedDelay = 10000)
    public void processBatch() {
        System.out.println("Processing batch at: " + LocalTime.now());
        // Batch processing logic — may take variable time
    }
}
```

### fixedDelay Behavior

```
Execution timeline:
T+0s:     Task starts (takes 3s)
T+3s:     Task completes
T+13s:    Task starts again (10s after COMPLETION)
T+16s:    Task completes (takes 3s)
T+26s:    Task starts again (10s after COMPLETION)
...

Key: Next execution 10 seconds after PREVIOUS END
```

### fixedRate vs fixedDelay

| Feature | fixedRate | fixedDelay |
|---------|-----------|------------|
| **Measurement** | Last START time se | Last END time se |
| **Overlapping** | Possible (if task > rate) | Never (waits for completion) |
| **Use Case** | Regular monitoring | Batch processing |
| **Example** | `fixedRate = 5000` | `fixedDelay = 10000` |

```java
// fixedRate — tasks can overlap
@Scheduled(fixedRate = 2000)  // Every 2s
public void overlappingTask() {
    Thread.sleep(3000);  // Takes 3s → Next task starts before this completes!
}

// fixedDelay — never overlaps
@Scheduled(fixedDelay = 2000)  // 2s after completion
public void nonOverlappingTask() {
    Thread.sleep(3000);  // Takes 3s → Next task starts 2s after this completes
}
```

---

## @Scheduled(cron)

**Matlab:** Cron expression se complex scheduling — specific times/days pe execute karo.

### Basic Usage

```java
@Service
public class ReportService {

    // Every day at 9:00 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void generateDailyReport() {
        System.out.println("Generating daily report at: " + LocalDateTime.now());
    }

    // Every Monday at 8:00 AM
    @Scheduled(cron = "0 0 8 * * MON")
    public void generateWeeklyReport() { }

    // Every 15 minutes
    @Scheduled(cron = "0 */15 * * * ?")
    public void checkAlerts() { }

    // Every hour at minute 30
    @Scheduled(cron = "0 30 * * * ?")
    public void halfHourlyTask() { }
}
```

---

## Cron Expression Syntax

**Matlab:** 6 ya 7 fields — time schedule define karte hain.

### Format

```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌───────────── day of week (0-7 or SUN-SAT, 0 & 7 = Sunday)
│ │ │ │ │ │
* * * * * *
```

### Special Characters

| Character | Meaning | Example | Description |
|-----------|---------|---------|-------------|
| `*` | All values | `* * * * * ?` | Every second |
| `,` | Value list | `0 0,30 * * * ?` | Every hour at :00 and :30 |
| `-` | Range | `0 9-17 * * * ?` | 9 AM to 5 PM every hour |
| `/` | Increment | `0 */5 * * * ?` | Every 5 minutes |
| `?` | No specific value | `0 0 12 ? * MON` | Monday at 12 PM |
| `L` | Last | `0 0 12 L * ?` | Last day of month at 12 PM |
| `W` | Weekday | `0 0 12 15W * ?` | Nearest weekday to 15th |
| `#` | Nth day | `0 0 12 ? * 2#2` | 2nd Monday at 12 PM |

### Common Cron Expressions

| Expression | Description |
|------------|-------------|
| `0 0 * * * ?` | Every hour |
| `0 */5 * * * ?` | Every 5 minutes |
| `0 0 */2 * * ?` | Every 2 hours |
| `0 0 9 * * ?` | Daily at 9 AM |
| `0 0 9 * * MON-FRI` | Weekdays at 9 AM |
| `0 0 9 1 * ?` | 1st of every month at 9 AM |
| `0 0 0 * * ?` | Daily at midnight |
| `0 30 23 * * ?` | Daily at 11:30 PM |
| `0 0 12 ? * SUN` | Every Sunday at 12 PM |
| `0 0 0 1 1 ?` | Every year on Jan 1st at midnight |
| `0 */10 9-17 * * MON-FRI` | Every 10 min, 9-5, weekdays |

### Month & Day Names

```
Months: JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC
Days: SUN, MON, TUE, WED, THU, FRI, SAT

@Scheduled(cron = "0 0 9 * * MON-FRI")   // Weekdays at 9 AM
@Scheduled(cron = "0 0 0 1 JAN ?")       // New Year at midnight
@Scheduled(cron = "0 30 18 * * FRI")     // Friday at 6:30 PM
```

---

## initialDelay & zone

### initialDelay

**Matlab:** Application start hone ke baad pehli execution se pehle kitna wait karna hai.

```java
// Pehli execution 30 seconds baad, phir every 5 minutes
@Scheduled(initialDelay = 30000, fixedRate = 300000)
public void delayedTask() {
    System.out.println("Running after 30s initial delay");
}

// Cron ke saath bhi use kar sakte ho
@Scheduled(initialDelay = 60000, cron = "0 */10 * * * ?")
public void delayedCronTask() { }
```

### Use Cases

```java
// ✅ Use initialDelay when:
// - Application startup pe load kam karna hai
// - Dependencies ready hone ka wait karna hai
// - Cache warm up karna hai

@Scheduled(initialDelay = 120000, fixedRate = 3600000)  // 2 min delay, then every hour
public void syncExternalData() {
    // External API se data sync — app ready hone ke baad start karo
}
```

### zone

**Matlab:** Cron expression ka timezone specify karna.

```java
// IST timezone mein schedule karo
@Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
public void indiaScheduledTask() {
    // Yeh task IST 9 AM pe run hoga
    // Agar server US mein hai toh bhi IST 9 AM pe hi run hoga
}

// UTC timezone
@Scheduled(cron = "0 0 12 * * ?", zone = "UTC")
public void utcScheduledTask() { }

// Server default timezone
@Scheduled(cron = "0 0 9 * * ?", zone = "#{systemEnvironment['APP_TIMEZONE'] ?: 'UTC'}")
public void envBasedScheduledTask() { }
```

---

## Dynamic Scheduling

**Matlab:** Runtime pe schedule change karna — hardcoded cron expressions ke bajaye dynamic.

### Using PropertyPlaceholder

```java
@Service
public class DynamicScheduledService {

    @Scheduled(cron = "${myapp.report.cron:0 0 9 * * ?}")
    public void generateReport() {
        System.out.println("Generating report");
    }
}

// application.properties
myapp.report.cron=0 0 10 * * ?  // Change to 10 AM
```

### Programmatic Scheduling

```java
@Configuration
@EnableScheduling
public class DynamicSchedulingConfig implements SchedulingConfigurer {

    @Value("${myapp.task.cron:0 */5 * * * ?}")
    private String cronExpression;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
            () -> {
                // Task logic
                System.out.println("Running dynamic task at: " + LocalTime.now());
            },
            triggerContext -> {
                // Cron trigger
                CronTrigger trigger = new CronTrigger(cronExpression);
                return trigger.nextExecutionTime(triggerContext);
            }
        );
    }
}
```

### Multiple Threads for Scheduled Tasks

```java
@Configuration
@EnableScheduling
public class MultiThreadSchedulingConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // Default: single thread — tasks sequential hote hain
        // Multiple threads se tasks parallel run honge

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);  // 5 threads
        scheduler.setThreadNamePrefix("Scheduled-");
        scheduler.initialize();

        taskRegistrar.setTaskScheduler(scheduler);
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **@EnableScheduling** | Scheduled tasks enable karta hai |
| **fixedRate** | Last START time se interval measure |
| **fixedDelay** | Last END time se interval measure |
| **cron** | Complex scheduling — second minute hour dayOfMonth month dayOfWeek |
| **Cron special chars** | `*` (all), `,` (list), `-` (range), `/` (increment), `?` (no value), `L` (last) |
| **initialDelay** | App start ke baad pehli execution se pehle wait |
| **zone** | Cron expression ka timezone specify karo |
| **Default scheduler** | Single-threaded — tasks sequential run hote hain |
| **Multi-threaded** | TaskScheduler ya ThreadPoolTaskScheduler configure karo |
