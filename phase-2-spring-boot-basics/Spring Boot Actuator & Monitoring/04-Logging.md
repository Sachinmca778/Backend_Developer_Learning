# Logging

## Status: Not Started

---

## Table of Contents

1. [SLF4J + Logback (Default)](#slf4j--logback-default)
2. [Log Levels](#log-levels)
3. [@Slf4j (Lombok)](#slf4j-lombok)
4. [MDC (Mapped Diagnostic Context)](#mdc-mapped-diagnostic-context)
5. [Structured Logging (JSON Format)](#structured-logging-json-format)
6. [Log Rotation](#log-rotation)
7. [logging.level.* Config](#logginglevel-config)

---

## SLF4J + Logback (Default)

**Matlab:** Spring Boot ka default logging setup — SLF4J (interface) + Logback (implementation).

### How It Works

```
Application Code
    ↓
SLF4J API (Logger interface)
    ↓
Logback (actual implementation)
    ↓
Console / File / External systems
```

### Basic Usage

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public User createUser(UserRequest request) {
        log.info("Creating user with email: {}", request.getEmail());

        try {
            User user = userRepository.save(request.toEntity());
            log.info("User created successfully with id: {}", user.getId());
            return user;
        } catch (Exception e) {
            log.error("Failed to create user: {}", request.getEmail(), e);
            throw new RuntimeException("User creation failed", e);
        }
    }
}
```

### Why SLF4J?

```
SLF4J = Logging facade (interface)
Logback = Implementation

Agar baad mein Log4j2 use karna hua toh sirf dependency change karo
→ Application code change nahi hoga
→ SLF4J interface same rahega
```

### Log Methods

```java
log.trace("Trace message");   // Sabse detailed
log.debug("Debug message");   // Development debugging
log.info("Info message");     // Important events
log.warn("Warning message");  // Potential issues
log.error("Error message");   // Errors/exceptions
```

### Parameterized Logging

```java
// ✅ Good — lazy evaluation
log.info("User {} created with id: {}", email, userId);

// ❌ Bad — string concatenation (har baar execute hoga)
log.info("User " + email + " created with id: " + userId);

// Exception ke saath
log.error("Failed to process order: {}", orderId, exception);
```

---

## Log Levels

**Matlab:** Log messages ki severity define karta hai — kaunsa message print hoga aur kaunsa nahi.

### Levels (Low to High)

| Level | Purpose | Example |
|-------|---------|---------|
| **TRACE** | Sabse detailed — har chhoti detail | Method entry/exit, variable values |
| **DEBUG** | Development mein debugging | Query execution, API responses |
| **INFO** | Important events (default) | App start, user created, order placed |
| **WARN** | Potential issues | Deprecated API, slow query |
| **ERROR** | Errors/exceptions | Database connection failed, payment error |
| **OFF** | Logging disable | — |

### Level Filtering

```
Log level set karo: INFO

TRACE → ❌ Filtered out
DEBUG → ❌ Filtered out
INFO  → ✅ Printed
WARN  → ✅ Printed
ERROR → ✅ Printed
```

### Usage Examples

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    public Order placeOrder(OrderRequest request) {
        log.debug("Processing order request: {}", request);

        // Validate
        if (request.getItems().isEmpty()) {
            log.warn("Order placed with empty items for user: {}", request.getUserId());
        }

        try {
            log.info("Placing order for user: {}", request.getUserId());

            // Save order
            Order order = orderRepository.save(request.toEntity());
            log.debug("Order saved with id: {}", order.getId());

            // Process payment
            PaymentResult result = paymentGateway.process(request.getPayment());
            log.info("Payment processed: status={}", result.getStatus());

            return order;

        } catch (PaymentException e) {
            log.error("Payment failed for order: {}", request.getUserId(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error placing order for user: {}", request.getUserId(), e);
            throw new RuntimeException("Order placement failed", e);
        }
    }
}
```

---

## @Slf4j (Lombok)

**Matlab:** Boilerplate Logger declaration se bachne ka annotation — Lombok automatically logger create kar deta hai.

### Setup

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

### Usage

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j  // Yeh automatically "log" field create karega
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(UserRequest request) {
        log.info("Creating user: {}", request.getEmail());

        User user = userRepository.save(request.toEntity());

        log.info("User created with id: {}", user.getId());

        return user;
    }

    public User findById(Long id) {
        log.debug("Finding user by id: {}", id);

        return userRepository.findById(id)
            .orElseThrow(() -> {
                log.error("User not found with id: {}", id);
                return new ResourceNotFoundException("User not found: " + id);
            });
    }
}
```

### What @Slf4j Generates

```java
// @Slf4j iske barabar hai:
private static final org.slf4j.Logger log =
    org.slf4j.LoggerFactory.getLogger(UserService.class);
```

### Lombok Logging Annotations

| Annotation | Logger Field Name | Logger Type |
|------------|------------------|-------------|
| **@Slf4j** | `log` | SLF4J |
| **@Log4j2** | `log` | Log4j2 |
| **@CommonsLog** | `log` | Apache Commons Logging |
| **@Log** | `log` | Java Util Logging |

### IntelliJ Lombok Setup

1. **Settings → Plugins → Lombok** install karo
2. **Settings → Build → Annotation Processing → Enable** karo
3. Rebuild project

---

## MDC (Mapped Diagnostic Context)

**Matlab:** Request-specific data (correlation ID, user ID) ko thread-local store karna — har log line mein automatically include hota hai.

### The Problem Without MDC

```java
log.info("User created");  // Kaunsa user? Kaunsa request? Pata nahi chalta!
```

### Solution With MDC

```java
// Filter mein MDC set karo
@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Correlation ID generate/set karo
            String correlationId = request.getHeader("X-Correlation-Id");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }

            MDC.put("correlationId", correlationId);
            MDC.put("userId", getUserIdFromRequest(request));
            MDC.put("requestUri", request.getRequestURI());

            // Response header mein bhi add karo
            response.setHeader("X-Correlation-Id", correlationId);

            filterChain.doFilter(request, response);

        } finally {
            // Request complete hone pe MDC clear karo
            MDC.clear();
        }
    }

    private String getUserIdFromRequest(HttpServletRequest request) {
        // JWT se user ID extract karo
        return "anonymous";
    }
}
```

### Log Pattern with MDC

```properties
# application.properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{correlationId}] [%X{userId}] %-5level %logger{36} - %msg%n
```

**Output:**
```
2024-01-15 10:30:00 [http-nio-8080-exec-1] [abc-123-def] [sachin] INFO  UserService - Creating user: sachin@example.com
2024-01-15 10:30:00 [http-nio-8080-exec-1] [abc-123-def] [sachin] INFO  UserService - User created with id: 1
2024-01-15 10:30:01 [http-nio-8080-exec-2] [xyz-456-ghi] [rahul] INFO  OrderService - Placing order for user: 2
```

**Har log line mein correlationId aur userId automatically aa gaya!**

### MDC in Async Context

```java
// Async threads mein MDC pass nahi hota by default
@Async
public void sendEmail(String email) {
    // MDC context yahan available nahi hoga!
    log.info("Sending email to: {}", email);  // correlationId missing!
}

// Solution: MDC manually pass karo
public void processAsync(String email) {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    CompletableFuture.runAsync(() -> {
        MDC.setContextMap(mdcContext);  // MDC restore karo
        try {
            sendEmail(email);
        } finally {
            MDC.clear();
        }
    });
}
```

### TaskDecorator for @Async

```java
@Component
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

// Configuration
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setTaskDecorator(new MdcTaskDecorator());  // MDC pass hoga
        executor.initialize();
        return executor;
    }
}
```

---

## Structured Logging (JSON Format)

**Matlab:** Logs ko JSON format mein likhna — machine-readable — ELK stack, Splunk, Datadog mein easily parse ho.

### Plain Text vs JSON

```
Plain Text:
2024-01-15 10:30:00 [http-nio-8080-exec-1] [abc-123] INFO UserService - Creating user

JSON:
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "thread": "http-nio-8080-exec-1",
  "correlationId": "abc-123",
  "level": "INFO",
  "logger": "com.example.service.UserService",
  "message": "Creating user: sachin@example.com",
  "userId": "sachin"
}
```

### Setup JSON Logging

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Console appender (JSON format) -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>
                <mdcKeyName>correlationId</mdcKeyName>
                <mdcKeyName>userId</mdcKeyName>
            </includeMdcKeyName>
            <customFields>{"application":"my-app","environment":"dev"}</customFields>
        </encoder>
    </appender>

    <!-- File appender (JSON format) -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/app.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- Profile-specific logging -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

### JSON Output

```json
{
  "@timestamp": "2024-01-15T10:30:00.000+00:00",
  "@version": "1",
  "message": "Creating user: sachin@example.com",
  "logger_name": "com.example.service.UserService",
  "thread_name": "http-nio-8080-exec-1",
  "level": "INFO",
  "level_value": 20000,
  "correlationId": "abc-123-def",
  "userId": "sachin",
  "application": "my-app",
  "environment": "dev"
}
```

### Custom JSON Fields

```java
@Slf4j
@Service
public class OrderService {

    public Order placeOrder(OrderRequest request) {
        log.info("Order placed",
            kv("orderId", request.getId()),
            kv("userId", request.getUserId()),
            kv("amount", request.getTotalAmount()),
            kv("paymentMethod", request.getPaymentMethod())
        );

        return orderRepository.save(request.toEntity());
    }
}
```

---

## Log Rotation

**Matlab:** Log files ko automatically manage karna — old files archive/delete, new files banana.

### Logback Configuration

```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/myapp.log</file>

        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>

        <!-- Time + Size based rolling -->
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <!-- Daily rollover -->
            <fileNamePattern>logs/myapp.%d{yyyy-MM-dd}.%i.log</fileNamePattern>

            <!-- Max file size before rollover -->
            <maxFileSize>100MB</maxFileSize>

            <!-- Keep 30 days of logs -->
            <maxHistory>30</maxHistory>

            <!-- Total size cap -->
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### Rolling Policy Options

| Policy | Description | Example |
|--------|-------------|---------|
| **TimeBasedRollingPolicy** | Time basis pe rollover | Daily, hourly |
| **SizeBasedTriggeringPolicy** | Size basis pe rollover | 100MB pe new file |
| **SizeAndTimeBasedRollingPolicy** | Dono combined | Daily + 100MB max |

### Rolling Policy Properties

```xml
<rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
    <!-- File pattern -->
    <fileNamePattern>logs/app.%d{yyyy-MM-dd}.%i.log</fileNamePattern>

    <!-- Max single file size -->
    <maxFileSize>50MB</maxFileSize>

    <!-- Retention period -->
    <maxHistory>60</maxHistory>

    <!-- Total size limit -->
    <totalSizeCap>5GB</totalSizeCap>

    <!-- Clean history on startup -->
    <cleanHistoryOnStart>true</cleanHistoryOnStart>
</rollingPolicy>
```

### application.properties Configuration

```properties
# Simple file logging (no rotation)
logging.file.name=logs/myapp.log

# Or directory (spring.log banega)
logging.file.path=logs/

# Logback config file specify karo
logging.config=classpath:logback-spring.xml
```

---

## logging.level.* Config

### Basic Configuration

```properties
# application.properties

# Root log level
logging.level.root=INFO

# Package-specific levels
logging.level.com.example=DEBUG
logging.level.com.example.service=DEBUG
logging.level.com.example.repository=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Disable specific logger
logging.level.com.example.legacy=OFF
```

### YAML Format

```yaml
logging:
  level:
    root: INFO
    com.example: DEBUG
    com.example.service: DEBUG
    com.example.repository: TRACE
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
  file:
    name: logs/myapp.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{correlationId}] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Profile-Specific Logging

```properties
# application-dev.properties
logging.level.root=DEBUG
logging.level.com.example=DEBUG
logging.level.org.springframework.web=DEBUG

# application-prod.properties
logging.level.root=WARN
logging.level.com.example=INFO
logging.level.org.springframework.web=WARN
```

### Common Debug Loggers

```properties
# Spring Web
logging.level.org.springframework.web=DEBUG

# Spring Security
logging.level.org.springframework.security=DEBUG

# Hibernate SQL
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Spring Data JPA
logging.level.org.springframework.data.jpa=DEBUG

# Transaction
logging.level.org.springframework.transaction=DEBUG

# Actuator
logging.level.org.springframework.boot.actuate=DEBUG
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **SLF4J + Logback** | Spring Boot default — SLF4J interface, Logback implementation |
| **Log Levels** | TRACE < DEBUG < INFO < WARN < ERROR — level set karo se us level aur above ke logs print honge |
| **@Slf4j** | Lombok annotation — Logger field automatically generate hota hai |
| **MDC** | Thread-local context — correlation ID, user ID har log line mein |
| **Structured Logging** | JSON format — ELK/Splunk/Datadog mein easily parse ho |
| **Log Rotation** | SizeAndTimeBasedRollingPolicy — daily + size basis pe rotate |
| **logging.level.*** | Package-specific log levels set karo |
| **Profile-specific** | Dev mein DEBUG, Prod mein INFO/WARN |
