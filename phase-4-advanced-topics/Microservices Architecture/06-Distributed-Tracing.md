# 06 - Distributed Tracing: Microservices Mein Debugging Kaise Karte Hain

> **Context:** Jab tumhare paas 20-30 microservices hain aur ek request multiple services ke through jaati hai, toh agar kuch fail ho toh kaise pata chalega ki exactly kahan problem aaya? Iska jawab hai **Distributed Tracing**.

---

## Table of Contents

1. [What is Distributed Tracing?](#1-what-is-distributed-tracing)
2. [Core Concepts: Trace ID, Span ID, Parent Span](#2-core-concepts-trace-id-span-id-parent-span)
3. [Architecture of Distributed Tracing](#3-architecture-of-distributed-tracing)
4. [B3 Propagation Format](#4-b3-propagation-format)
5. [MDC (Mapped Diagnostic Context) - Log Correlation](#5-mdc-mapped-diagnostic-context---log-correlation)
6. [Spring Cloud Sleuth (Spring Boot 2.x)](#6-spring-cloud-sleuth-spring-boot-2x)
7. [Micrometer Tracing (Spring Boot 3+)](#7-micrometer-tracing-spring-boot-3)
8. [Zipkin - Trace Visualization](#8-zipkin---trace-visualization)
9. [Complete Working Example (Spring Boot 3 + Micrometer + Zipkin)](#9-complete-working-example-spring-boot-3--micrometer--zipkin)
10. [Best Practices](#10-best-practices)
11. [Common Issues & Debugging Tips](#11-common-issues--debugging-tips)

---

## 1. What is Distributed Tracing?

**Simple language mein:** Distributed tracing ek technique hai jo ek single user request ko track karti hai jab woh multiple microservices ke through jaati hai.

### Ek Real-World Example Socho:

```
User ne "Place Order" click kiya

Order Service --> Payment Service --> Inventory Service --> Notification Service
     |                |                    |                    |
  50ms             200ms                150ms                 80ms

Total time: 480ms

Agar Payment Service slow hai (200ms le raha hai), 
toh distributed tracing se turant pata chal jaayega!
```

### Traditional Logging vs Distributed Tracing

| Feature | Traditional Logging | Distributed Tracing |
|---------|--------------------|--------------------|
| Request flow dekhna | Har service ka alag log, manually correlate karna padta hai | Ek hi dashboard mein pura flow dikh jaata hai |
| Latency analysis | Kaunsa service slow hai, guess karna padta hai | Har span ka exact time dikh jaata hai |
| Error root cause | Stack traces ko manually match karna | Error wala span immediately highlight ho jaata hai |
| Dependency mapping | Architecture diagram manually banana padta hai | Auto-discover ho jaata hai trace se |

---

## 2. Core Concepts: Trace ID, Span ID, Parent Span

### Trace ID

- **Trace ID** ek unique identifier hai jo ek **complete end-to-end request** ko represent karta hai
- Jab user se request aata hai, ek Trace ID generate hota hai
- Yeh Trace ID saare services ke through propagate hota hai
- **Format:** Usually 16 bytes (32 hex characters) ya 8 bytes (16 hex characters)

```
Trace ID: 4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e
                ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
                Yeh same rahega pure request flow mein
```

### Span ID

- **Span ID** ek **single operation** ko represent karta hai
- Har service call, har DB query, har HTTP request ek alag span hai
- Ek trace mein multiple spans ho sakte hain

```
Trace: 4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e

  Span 1: API Gateway receives request        [Span ID: a1b2c3d4]
    |
    +-- Span 2: Order Service processes order [Span ID: e5f6a7b8]
          |
          +-- Span 3: Payment Service charge  [Span ID: c9d0e1f2]
          +-- Span 4: Inventory Service check [Span ID: a3b4c5d6]
          +-- Span 5: Notification send       [Span ID: e7f8a9b0]
```

### Parent-Child Relationship

Har span ka ek **Parent Span ID** hota hai (except root span). Isse tree structure banta hai:

```
Root Span (API Gateway)
  |-- Child Span 1 (Order Service)
  |     |-- Child Span 1.1 (Payment Service)
  |     |-- Child Span 1.2 (Inventory Service)
  |     |-- Child Span 1.3 (Notification Service)
  |-- Child Span 2 (Analytics Service)
```

### Span Structure (Detailed)

```java
// Ek span ke andar yeh sab information hoti hai:
Span {
    traceId:      "4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e",  // Pure flow ka ID
    id:           "a1b2c3d4e5f6a7b8",                // Khud ka Span ID
    parentId:     "0000000000000000",                // Parent ka Span ID (root ke liye null/empty)
    name:         "POST /api/orders",                // Operation ka naam
    kind:         SERVER,                            // CLIENT, SERVER, PRODUCER, CONSUMER
    timestamp:    1681234567890000,                  // Microseconds mein start time
    duration:     150000,                            // Microseconds mein duration (150ms)
    tags: {
        "http.method": "POST",
        "http.url": "/api/orders",
        "http.status_code": "200",
        "service.name": "order-service"
    },
    annotations: [
        { timestamp: 1681234567890000, value: "sr" },  // Server Receive
        { timestamp: 1681234568040000, value: "ss" }   // Server Send
    ]
}
```

---

## 3. Architecture of Distributed Tracing

### High-Level Architecture (ASCII Diagram)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           USER REQUEST FLOW                             │
└─────────────────────────────────────────────────────────────────────────┘

      ┌──────────┐
      │  Client  │  (Browser / Mobile App)
      │          │
      └────┬─────┘
           │  HTTP Request
           │  (Trace ID: none yet)
           ▼
┌──────────────────────┐
│    API Gateway       │ ◄── Trace ID yahan GENERATE hota hai
│  (Spring Cloud       │     Span ID: gateway-span-001
│   Gateway)           │     Trace ID: trace-abc-123
└──────────┬───────────┘
           │  HTTP Request
           │  Headers: X-B3-TraceId: trace-abc-123
           │           X-B3-SpanId: order-span-002
           │           X-B3-ParentSpanId: gateway-span-001
           ▼
┌──────────────────────┐
│   Order Service      │ ◄── Trace ID SAME rehta hai (propagated)
│  (Port: 8081)        │     Naya Span ID generate hota hai
│                      │     Span ID: order-span-002
└──────────┬───────────┘     Parent Span ID: gateway-span-001
           │
           ├─────────────────────────┐
           │                         │
           ▼                         ▼
┌──────────────────────┐  ┌──────────────────────┐
│  Payment Service     │  │ Inventory Service    │
│  (Port: 8082)        │  │ (Port: 8083)         │
│  Span: pay-span-003  │  │ Span: inv-span-004   │
│  Parent: order-002   │  │ Parent: order-002    │
└──────────┬───────────┘  └──────────┬───────────┘
           │                         │
           ▼                         ▼
┌──────────────────────┐  ┌──────────────────────┐
│    PostgreSQL DB     │  │     Redis Cache      │
│  Span: db-span-005   │  │  Span: redis-span-006│
│  Parent: pay-span-003│  │  Parent: inv-span-004│
└──────────────────────┘  └──────────────────────┘


┌─────────────────────────────────────────────────────────────────────────┐
│                      TRACING BACKEND (Async Flow)                       │
└─────────────────────────────────────────────────────────────────────────┘

  Har Service
       │
       │  Spans ko asynchronously bhejta hai
       │  (UDP ya HTTP via reporter)
       ▼
┌──────────────────────┐
│   Zipkin Server      │ ◄── Saare spans yahan collect hote hain
│  (Port: 9411)        │     Spans ko Trace ID se group karta hai
│                      │     Timeline visualization banata hai
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│   Zipkin UI          │ ◄── Developer yahan dekh sakta hai
│  http://localhost:9411│     - Complete request flow
│                      │     - Har span ka latency
│                      │     - Error locations
│                      │     - Service dependency graph
└──────────────────────┘
```

### Data Flow Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Service   │     │   Service   │     │   Service   │
│     A       │────▶│     B       │────▶│     C       │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       │  Span Reports     │  Span Reports     │  Span Reports
       ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────┐
│              Zipkin Collector (Port: 9411)              │
│                                                         │
│   Spans ko receive karta hai aur store karta hai       │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Zipkin Storage (In-Memory / ES / MySQL)     │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Zipkin UI - Query & Visualize               │
└─────────────────────────────────────────────────────────┘
```

---

## 4. B3 Propagation Format

### B3 Kya Hai?

**B3** (BlackRock, Brave, Backend) propagation format ek **standard** hai jo define karta hai ki trace context ko HTTP headers ke through kaise propagate kiya jaata hai between services.

Yeh originally **Zipkin** project se aaya aur ab industry standard ban gaya hai.

### B3 Headers

B3 do formats support karta hai:

#### Multi-Header Format (Readable)

```http
GET /api/orders HTTP/1.1
Host: order-service:8081
X-B3-TraceId: 4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e
X-B3-SpanId: a1b2c3d4e5f6a7b8
X-B3-ParentSpanId: 1122334455667788
X-B3-Sampled: 1
```

| Header | Description |
|--------|-------------|
| `X-B3-TraceId` | Complete trace ka unique ID (same for all spans in a trace) |
| `X-B3-SpanId` | Current operation ka Span ID |
| `X-B3-ParentSpanId` | Parent span ka ID (jo yeh request bheja) |
| `X-B3-Sampled` | `1` = sample karo, `0` = mat karo |

#### Single-Header Format (Compact - B3 Single)

```http
GET /api/orders HTTP/1.1
Host: order-service:8081
b3: 4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e-a1b2c3d4e5f6a7b8-1
    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^ ^
    TraceId                          SpanId           Sampled (1=yes, 0=no)
```

Format: `b3: {TraceId}-{SpanId}-{Sampled}`

Agar parent span bhi include karna ho:
```
b3: {TraceId}-{SpanId}-{Sampled}-{ParentSpanId}
```

### W3C Trace Context (Newer Standard)

B3 ke alawa ab **W3C Trace Context** bhi popular ho raha hai:

```http
traceparent: 00-4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e-a1b2c3d4e5f6a7b8-01
             ^^ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^ ^^
             version    TraceId                       SpanId    flags
```

### B3 vs W3C Comparison

```
B3 Multi-Header:          W3C Trace Context:
X-B3-TraceId: 4f8a...     traceparent: 00-4f8a...-a1b2...-01
X-B3-SpanId: a1b2...
X-B3-Sampled: 1

B3 Single-Header:         W3C + B3 Combined:
b3: 4f8a...-a1b2...-1     traceparent: 00-4f8a...-a1b2...-01
                          b3: 4f8a...-a1b2...-1
```

### Code: Custom Propagation Dekhne Ke Liye

```java
@RestController
@RequestMapping("/debug")
public class DebugController {

    @GetMapping("/headers")
    public Map<String, String> showTraceHeaders(
            @RequestHeader HttpHeaders headers) {
        
        Map<String, String> traceHeaders = new HashMap<>();
        
        // B3 headers extract karo
        traceHeaders.put("X-B3-TraceId", 
            headers.getFirst("X-B3-TraceId"));
        traceHeaders.put("X-B3-SpanId", 
            headers.getFirst("X-B3-SpanId"));
        traceHeaders.put("X-B3-ParentSpanId", 
            headers.getFirst("X-B3-ParentSpanId"));
        
        // W3C headers
        traceHeaders.put("traceparent", 
            headers.getFirst("traceparent"));
        
        return traceHeaders;
    }
}
```

**Response example:**
```json
{
  "X-B3-TraceId": "4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e",
  "X-B3-SpanId": "a1b2c3d4e5f6a7b8",
  "X-B3-ParentSpanId": "1122334455667788",
  "traceparent": null
}
```

---

## 5. MDC (Mapped Diagnostic Context) - Log Correlation

### MDC Kya Hai?

**MDC (Mapped Diagnostic Context)** ek SLF4J feature hai jo **thread-local map** provide karta hai. Isme tum key-value pairs store kar sakte ho, aur woh automatically har log line mein append ho jaayenge.

**Sabse important use case:** Trace ID aur Span ID ko logs mein add karna, taaki tum ek specific trace ke saare logs nikal sako.

### Without MDC vs With MDC

**Without MDC (Problem):**
```
2024-04-11 10:00:01 [http-nio-8081-exec-1] INFO  OrderService - Order created
2024-04-11 10:00:01 [http-nio-8082-exec-3] INFO  PaymentService - Payment processed
2024-04-11 10:00:01 [http-nio-8081-exec-2] INFO  OrderService - Order created
2024-04-11 10:00:02 [http-nio-8082-exec-5] INFO  PaymentService - Payment failed
```
> Kaunsa log kis request ka hai? Pata nahi! 100 concurrent requests hain toh sab mix ho jaayega.

**With MDC (Solution):**
```
2024-04-11 10:00:01 [traceId=abc123,spanId=span1] [http-nio-8081-exec-1] INFO  OrderService - Order created
2024-04-11 10:00:01 [traceId=abc123,spanId=span2] [http-nio-8082-exec-3] INFO  PaymentService - Payment processed
2024-04-11 10:00:01 [traceId=def456,spanId=span3] [http-nio-8081-exec-2] INFO  OrderService - Order created
2024-04-11 10:00:02 [traceId=def456,spanId=span4] [http-nio-8082-exec-5] INFO  PaymentService - Payment failed
```
> Ab `grep "traceId=abc123"` karo toh sirf ek request ke saare logs milenge!

### MDC Configuration in Spring Boot

#### Step 1: Logback Configuration (`src/main/resources/logback-spring.xml`)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <!-- MDC mein traceId aur spanId ko use karne ke liye pattern -->
    <property name="LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-},%X{spanId:-}] [%thread] %-5level %logger{36} - %msg%n"/>
    
    <!-- %X{traceId:-} ka matlab: MDC mein traceId dhundho, 
         agar nahi mila toh "-" print karo -->

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/application.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

</configuration>
```

#### Step 2: Manual MDC Usage (Agar Sleuth use nahi kar rahe)

```java
import org.slf4j.MDC;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Aspect
@Component
public class TracingAspect {

    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public Object addTraceId(ProceedingJoinPoint joinPoint) throws Throwable {
        
        // Agar MDC mein traceId nahi hai toh generate karo
        if (MDC.get("traceId") == null) {
            String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            MDC.put("traceId", traceId);
        }
        
        try {
            return joinPoint.proceed();
        } finally {
            // IMPORTANT: MDC clear karna zaroori hai, warna thread reuse mein 
            // purana traceId next request mein chala jaayega!
            MDC.clear();
        }
    }
}
```

#### Step 3: Sleuth ke saath Automatic MDC (Recommended)

Jab tum Spring Cloud Sleuth ya Micrometer Tracing use karte ho, toh MDC mein traceId aur spanId **automatically** set ho jaate hain. Tumhe kuch extra configuration nahi karna padta.

```java
@RestController
@RequestMapping("/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    @PostMapping
    public Order createOrder(@RequestBody OrderRequest request) {
        // MDC mein traceId aur spanId already set hain (Sleuth ne kiya)
        log.info("Creating order for user: {}", request.getUserId());
        // Output: 2024-04-11 10:00:01 [abc123,span1] [http-nio-8081-exec-1] 
        //         INFO  OrderController - Creating order for user: user_123
        
        Order order = orderService.createOrder(request);
        
        log.info("Order created with ID: {}", order.getId());
        // Output: 2024-04-11 10:00:01 [abc123,span1] [http-nio-8081-exec-1] 
        //         INFO  OrderController - Order created with ID: ORD-789
        
        return order;
    }
}
```

#### Step 4: Custom MDC Fields Add Karna

```java
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // Custom fields MDC mein add karo
            String userId = request.getHeader("X-User-Id");
            if (userId != null) {
                MDC.put("userId", userId);
            }
            
            String requestId = request.getHeader("X-Request-Id");
            if (requestId != null) {
                MDC.put("requestId", requestId);
            }
            
            // Environment info (helpful for debugging in multi-env)
            MDC.put("environment", System.getenv("ENVIRONMENT") != null 
                ? System.getenv("ENVIRONMENT") : "local");
            
            filterChain.doFilter(request, response);
            
        } finally {
            MDC.clear(); // Hamesha finally block mein clear karo!
        }
    }
}
```

**Updated Log Pattern:**
```xml
<property name="LOG_PATTERN" 
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} [traceId=%X{traceId:-},spanId=%X{spanId:-},userId=%X{userId:-}] [%thread] %-5level %logger{36} - %msg%n"/>
```

**Output:**
```
2024-04-11 10:00:01 [traceId=abc123,spanId=span1,userId=user_456] [http-nio-8081-exec-1] INFO  OrderController - Creating order for user: user_456
```

### MDC Thread Pool ke Saath (Important Gotcha)

**Problem:** MDC thread-local hai. Agar tum `@Async` ya `CompletableFuture` use karte ho, toh naye thread mein MDC context copy nahi hota.

```java
@Async
public void sendNotification(Order order) {
    // Yahan MDC KHAAli hoga! Trace ID nahi milega!
    log.info("Sending notification for order: {}", order.getId());
    // Output: [traceId=-,spanId=-] ...  <-- Trace ID missing!
}
```

**Solution 1: TaskDecorator use karo**

```java
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import java.util.Map;

public class MdcTaskDecorator implements TaskDecorator {
    
    @Override
    public Runnable decorate(Runnable runnable) {
        // Parent thread ka MDC context capture karo
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        
        return () -> {
            try {
                // Child thread mein context restore karo
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.setTaskDecorator(new MdcTaskDecorator()); // <-- MDC propagate hoga
        executor.initialize();
        return executor;
    }
}
```

**Solution 2: Sleuth/Micrometer ka built-in decorator use karo**

```java
// Sleuth (Spring Boot 2.x)
@Autowired
private Tracer tracer;

public Executor tracingAwareExecutor(Executor delegate) {
    return new TraceableExecutorService(beanFactory, delegate);
}

// Micrometer (Spring Boot 3+)
@Autowired
private ObservationRegistry observationRegistry;

public Executor observationAwareExecutor(Executor delegate) {
    return new ObservationThreadLocalAccessor(observationRegistry)
        .wrap(delegate);
}
```

---

## 6. Spring Cloud Sleuth (Spring Boot 2.x)

> **Note:** Spring Cloud Sleuth **deprecated** ho chuka hai Spring Boot 3.x se. Ab **Micrometer Tracing** use hota hai. Lekin production mein abhi bhi bahut saare projects Spring Boot 2.x use kar rahe hain, isliye yeh knowledge zaroori hai.

### Sleuth Kya Karta Hai?

1. **Auto Trace ID Generation** - Har incoming request ke liye Trace ID generate karta hai
2. **Automatic Header Propagation** - Outgoing HTTP calls mein B3 headers automatically add karta hai
3. **MDC Integration** - Trace ID aur Span ID ko automatically MDC mein daalta hai
4. **Common Library Support** - RestTemplate, WebClient, Kafka, RabbitMQ, async operations sabko automatically trace karta hai
5. **Sampling** - Configurable sampling rate (100% ya 10% requests trace karo)

### Maven Dependencies

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>

<!-- Zipkin integration -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

### Spring Cloud BOM

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2021.0.8</version> <!-- For Spring Boot 2.7.x -->
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Basic Configuration (`application.yml`)

```yaml
spring:
  application:
    name: order-service  # Zipkin mein service ka naam yehi dikhega
  
  sleuth:
    sampler:
      probability: 1.0  # 1.0 = 100% requests trace karo, 0.1 = 10%
    
    # Custom trace ID format
    trace-id128: true   # 128-bit trace ID use karo (longer, more unique)
    
    # Propagation format
    propagation:
      type: B3          # B3 use karo (default bhi yehi hai)
    
    # Baggage (custom data jo trace ke saath propagate hoga)
    baggage:
      remote-fields: userId,tenantId  # Yeh fields HTTP headers mein jaayenge
      local-fields: debugFlag          # Sirf local process mein rahega
    
    # Log correlation
    log:
      slf4j:
        mdc:
          fields: traceId,spanId  # MDC mein yeh fields add honge (default)
    
  zipkin:
    base-url: http://localhost:9411  # Zipkin server ka address
    sender:
      type: web  # web (HTTP), kafka, rabbitmq
    discovery-client-enabled: false
```

### Sleuth Automatic Tracing - Kya Kya Cover Hota Hai

Sleuth in sab operations ko **automatically** trace karta hai bina kisi extra code ke:

```
┌─────────────────────────────────────────────────────────┐
│           Spring Cloud Sleuth Auto-Trace                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  HTTP:                                                  │
│  ✓ RestTemplate calls                                   │
│  ✓ @RestController incoming requests                    │
│  ✓ WebClient (Spring 5+)                                │
│  ✓ Feign Client                                         │
│                                                         │
│  Messaging:                                             │
│  ✓ Kafka producers/consumers                            │
│  ✓ RabbitMQ producers/consumers                         │
│  ✓ JMS                                                  │
│                                                         │
│  Database:                                              │
│  ✓ JDBC connections                                     │
│  ✓ JPA/Hibernate (via Brave instrumentation)            │
│                                                         │
│  Async:                                                 │
│  ✓ @Async methods (TraceAsyncListener se)               │
│  ✓ CompletableFuture                                    │
│  ✓ Scheduled tasks                                      │
│                                                         │
│  Gateway:                                               │
│  ✓ Spring Cloud Gateway                                 │
│  ✓ Zuul                                                 │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Custom Span Create Karna

Kabhi-kabhi tumhe khud span create karna padta hai, jaise kisi complex operation ke liye:

```java
import brave.Span;
import brave.Tracer;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {
    
    private final Tracer tracer;  // Brave ka Tracer inject hoga
    
    public OrderProcessingService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public Order processOrder(OrderRequest request) {
        // Custom span create karo
        Span span = tracer.nextSpan().name("process-order-business-logic").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
            
            // Yeh code is span ke andar execute hoga
            span.tag("order.userId", request.getUserId());
            span.tag("order.totalAmount", String.valueOf(request.getTotalAmount()));
            
            // Business logic
            validateOrder(request);
            calculateDiscount(request);
            applyCoupons(request);
            
            span.tag("discount.applied", "true");
            
            return saveOrder(request);
            
        } catch (Exception e) {
            span.error(e);  // Error ko span mein record karo
            span.tag("error.type", e.getClass().getSimpleName());
            throw e;
        } finally {
            span.finish();  // Span close karna ZAROORI hai!
        }
    }
    
    private void validateOrder(OrderRequest request) {
        // Naya child span banega automatically
        Span validationSpan = tracer.nextSpan().name("validate-order").start();
        try (Tracer.SpanInScope ws = tracer.withSpanInScope(validationSpan)) {
            // validation logic
        } finally {
            validationSpan.finish();
        }
    }
}
```

### RestTemplate ke Saath Manual Trace Context Pass Karna

Agar tum `RestTemplate` use kar rahe ho aur Sleuth configured hai, toh headers automatically add ho jaayenge. Lekin agar manually karna ho toh:

```java
import brave.propagation.CurrentTraceContext;
import brave.propagation.TraceContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderServiceClient {
    
    private final RestTemplate restTemplate;
    private final CurrentTraceContext currentTraceContext;
    
    public OrderServiceClient(RestTemplate restTemplate, 
                             CurrentTraceContext currentTraceContext) {
        this.restTemplate = restTemplate;
        this.currentTraceContext = currentTraceContext;
    }
    
    public PaymentResponse callPaymentService(PaymentRequest request) {
        // Current trace context get karo
        TraceContext context = currentTraceContext.context();
        
        HttpHeaders headers = new HttpHeaders();
        // B3 headers manually add karo (Sleuth normally auto karta hai)
        headers.add("X-B3-TraceId", context.traceIdString());
        headers.add("X-B3-SpanId", context.spanIdString());
        
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
        
        return restTemplate.postForObject(
            "http://payment-service:8082/payments", 
            entity, 
            PaymentResponse.class
        );
    }
}
```

### Feign Client ke Saath Sleuth

Feign client ke saath Sleuth automatically kaam karta hai:

```java
@FeignClient(name = "payment-service", url = "${payment.service.url}")
public interface PaymentServiceClient {
    
    @PostMapping("/api/payments")
    PaymentResponse createPayment(@RequestBody PaymentRequest request);
    // Sleuth automatically B3 headers add kar dega!
}
```

### Sampling Configuration

Sampling ka matlab hai: har request trace karein ya sirf kuch percentage?

```yaml
spring:
  sleuth:
    sampler:
      # Option 1: Probability-based (sabse common)
      probability: 0.1  # 10% requests trace hongi
      
      # Option 2: Rate-based (per second)
      # rate: 10  # Maximum 10 traces per second
      
      # Option 3: Never sample (disable tracing)
      # probability: 0.0
      
      # Option 4: Always sample (development mein useful)
      # probability: 1.0
```

**Production recommendation:**
- Development: `1.0` (100%)
- Staging: `0.5` (50%)
- Production (high traffic): `0.01` - `0.1` (1%-10%)
- Production (low traffic): `0.5` - `1.0` (50%-100%)

---

## 7. Micrometer Tracing (Spring Boot 3+)

> **IMPORTANT:** Spring Boot 3.x mein Spring Cloud Sleuth remove ho chuka hai. Ab **Micrometer Tracing** use hota hai. Yeh official replacement hai.

### Migration: Sleuth -> Micrometer

```
Spring Boot 2.x                    Spring Boot 3.x
─────────────────                  ─────────────────
spring-cloud-starter-sleuth   -->  micrometer-tracing-bridge-brave
spring-cloud-sleuth-zipkin    -->  micrometer-tracing-reporter-zipkin
brave.Tracer                  -->  io.micrometer.tracing.Tracer
brave.Span                    -->  io.micrometer.tracing.Span
```

### Maven Dependencies (Spring Boot 3+)

```xml
<!-- Micrometer Tracing with Brave bridge -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>

<!-- Zipkin reporter -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-reporter-zipkin</artifactId>
</dependency>

<!-- Optional: AOT/ GraalVM support ke liye -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-observation</artifactId>
</dependency>
```

### Complete `application.yml` for Spring Boot 3

```yaml
spring:
  application:
    name: order-service
  
  # Zipkin configuration
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans  # Zipkin API URL
    
  # Micrometer Tracing configuration
  management:
    tracing:
      sampling:
        probability: 1.0  # 100% sampling (development)
        # rate: 10        # Ya phir per-second rate
    
    # Observations ko configure karna
    observations:
      annotations:
        enabled: true  # @Observed annotation support
    
    # Actuator endpoints (optional, for metrics)
    endpoints:
      web:
        exposure:
          include: health,info,metrics,observations
```

### Using Tracer API in Spring Boot 3

```java
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    
    private final Tracer tracer;
    private final ObservationRegistry observationRegistry;
    
    public OrderService(Tracer tracer, ObservationRegistry observationRegistry) {
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }
    
    // Approach 1: Direct Tracer API (Sleuth jaisa style)
    public Order processOrder(OrderRequest request) {
        Span span = tracer.nextSpan().name("process-order").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("user.id", request.getUserId());
            span.tag("order.amount", String.valueOf(request.getAmount()));
            
            // Business logic
            Order order = createOrder(request);
            
            span.tag("order.id", order.getId());
            return order;
            
        } catch (Exception e) {
            span.tag("error", e.getMessage());
            throw e;
        } finally {
            span.end();  // Sleuth mein finish() tha, yahan end() hai
        }
    }
    
    // Approach 2: Observation API (Recommended - newer, more powerful)
    public Order processOrderWithObservation(OrderRequest request) {
        return Observation.createNotStarted(
            "order.process",  // Observation name
            observationRegistry
        )
        .contextualName("processing-order")  // Display name
        .lowCardinalityKeyValue("service.name", "order-service")  // Fixed tags
        .highCardinalityKeyValue("user.id", request.getUserId())  // Dynamic tags
        .observe(() -> {
            // Yeh code trace hoga
            return createOrder(request);
        });
    }
    
    // Approach 3: @Observed annotation (Simplest!)
    @Observed(name = "order.validation", 
              contextualName = "validating-order",
              lowCardinalityKeyValues = {"type", "order"})
    public boolean validateOrder(OrderRequest request) {
        // Yeh method automatically trace hoga!
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required");
        }
        // ... validation logic
        return true;
    }
}
```

### @Observed Annotation ke Saath AOP-based Tracing

```java
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    
    // @Observed lagate hi yeh method automatically trace ho jaayega
    @Observed(name = "payment.process",
              contextualName = "processing-payment")
    public PaymentResponse processPayment(PaymentRequest request) {
        // Yeh pura method ek span ban jaayega
        PaymentResponse response = callPaymentGateway(request);
        return response;
    }
    
    // Nested @Observed methods child spans banate hain
    @Observed(name = "payment.validate",
              contextualName = "validating-payment")
    private void validatePayment(PaymentRequest request) {
        // Yeh parent span ka child span banega
        if (request.getAmount() <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }
    }
    
    @Observed(name = "payment.log",
              contextualName = "logging-payment")
    private void logPayment(PaymentResponse response) {
        // Yeh bhi child span banega
        System.out.println("Payment logged: " + response.getTransactionId());
    }
}
```

### RestTemplate / WebClient ke Saath Auto-Tracing

Micrometer bhi automatically HTTP clients ko trace karta hai:

```java
@Configuration
public class RestClientConfig {
    
    // RestTemplate automatically trace hoga agar Micrometer configured hai
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .build();
    }
    
    // WebClient bhi auto-trace hota hai
    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
            .baseUrl("http://payment-service:8082")
            .build();
    }
}
```

### Kafka ke Saath Micrometer Tracing

```java
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaTracingConfig {
    
    private final ObservationRegistry observationRegistry;
    
    public KafkaTracingConfig(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }
    
    // Producer: Trace context ko Kafka headers mein propagate karo
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            ProducerFactory<String, Object> producerFactory) {
        
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setObservationRegistry(observationRegistry); // <-- Tracing enable
        return template;
    }
    
    // Consumer: Trace context ko Kafka headers se read karo
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setObservationRegistry(observationRegistry); // <-- Tracing enable
        return factory;
    }
}
```

### Async Tracing in Spring Boot 3

```java
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    
    private final ObservationRegistry observationRegistry;
    
    public AsyncConfig(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("async-worker-");
        
        // Observation context propagate karo
        executor.setTaskDecorator(runnable -> {
            var currentObservation = observationRegistry.getCurrentObservation();
            return () -> {
                var previous = observationRegistry.getCurrentObservation();
                try {
                    if (currentObservation != null) {
                        observationRegistry.setCurrentObservation(currentObservation);
                    }
                    runnable.run();
                } finally {
                    observationRegistry.setCurrentObservation(previous);
                }
            };
        });
        
        executor.initialize();
        return executor;
    }
}
```

### Sleuth vs Micrometer Tracing Comparison

| Feature | Spring Cloud Sleuth (Boot 2.x) | Micrometer Tracing (Boot 3.x) |
|---------|-------------------------------|------------------------------|
| Package | `org.springframework.cloud.sleuth` | `io.micrometer.tracing` |
| Tracer API | `brave.Tracer` | `io.micrometer.tracing.Tracer` |
| Span finish | `span.finish()` | `span.end()` |
| Annotation | `@Slf4j` + manual | `@Observed` annotation |
| Observation | N/A | `Observation` API (more powerful) |
| Metrics integration | Alag | Micrometer metrics ke saath integrated |
| GraalVM support | Nahi | Haan (AOT compatible) |
| Baggage | `spring.sleuth.baggage` | `management.tracing.baggage` |

---

## 8. Zipkin - Trace Visualization

### Zipkin Kya Hai?

**Zipkin** ek open-source distributed tracing system hai jo Twitter ne banaya tha. Yeh traces ko collect, store, aur visualize karta hai.

### Zipkin Setup

#### Option 1: Docker (Sabse Easy)

```bash
# Zipkin start karo (in-memory storage ke saath)
docker run -d \
  --name zipkin \
  -p 9411:9411 \
  openzipkin/zipkin:latest

# Access: http://localhost:9411
```

```bash
# Zipkin with Elasticsearch (persistent storage)
docker run -d \
  --name zipkin \
  -p 9411:9411 \
  -e STORAGE_TYPE=elasticsearch \
  -e ES_HOSTS=http://elasticsearch:9200 \
  openzipkin/zipkin:latest
```

```bash
# Zipkin with Kafka (high throughput production setup)
docker run -d \
  --name zipkin \
  -p 9411:9411 \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e KAFKA_TOPIC=zipkin \
  openzipkin/zipkin:latest
```

#### Option 2: JAR File

```bash
# Zipkin JAR download karo
curl -sSL https://zipkin.io/quickstart.sh | bash -s

# Start karo
java -jar zipkin.jar
```

### Zipkin UI Features

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Zipkin UI Dashboard                             │
│  http://localhost:9411                                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─ Search Traces ─────────────────────────────────────────────────┐   │
│  │                                                                 │   │
│  │  Service: [order-service ▼]  Operation: [POST /api/orders ▼]   │   │
│  │                                                                 │   │
│  │  Start Time: [2024-04-11 ▼]  End Time: [2024-04-11 ▼]          │   │
│  │                                                                 │   │
│  │  Min Duration: [____] ms     Max Duration: [____] ms            │   │
│  │                                                                 │   │
│  │  Annotations: [error ▼]     Tags: user.id = "user_123"          │   │
│  │                                                                 │   │
│  │  [ Search ]  [ Reset ]                                          │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─ Trace Results ─────────────────────────────────────────────────┐   │
│  │                                                                 │   │
│  │  Trace ID: 4f8a7c2d...  Duration: 480ms  Services: 4  Errors: 0 │   │
│  │  ▁▂▃▅▇████▇▅▃▂▁  [Timeline bar - color coded by service]       │   │
│  │                                                                 │   │
│  │  Trace ID: a1b2c3d4...  Duration: 1200ms  Services: 5  Errors: 1│   │
│  │  ▁▂▃▅██████▇▅▃  [Red section shows error]                       │   │
│  │                                                                 │   │
│  │  Trace ID: e5f6a7b8...  Duration: 350ms  Services: 3  Errors: 0 │   │
│  │  ▁▂▃▅▇██▇▅▃▂▁                                                 │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  ┌─ Individual Trace View (when clicked) ──────────────────────────┐   │
│  │                                                                 │   │
│  │  Trace ID: 4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e                    │   │
│  │  Total Duration: 480ms  Total Spans: 8                          │   │
│  │                                                                 │   │
│  │  ┌─ API Gateway ──────────────── [50ms] ──────────────────┐    │   │
│  │  │  ┌─ Order Service ────────── [200ms] ──────────────┐   │    │   │
│  │  │  │  ┌─ Payment Service ── [120ms] ────────────┐    │   │    │   │
│  │  │  │  │  └─ PostgreSQL ─ [45ms] ───┐             │    │   │    │   │
│  │  │  │  └─────────────────────────────┘             │    │   │    │   │
│  │  │  │  ┌─ Inventory Service ─ [60ms] ───────┐     │    │   │    │   │
│  │  │  │  │  └─ Redis ─ [15ms] ─┐              │     │    │   │    │   │
│  │  │  │  └─────────────────────┘              │     │    │   │    │   │
│  │  │  └───────────────────────────────────────┘     │    │   │    │   │
│  │  └────────────────────────────────────────────────┘    │   │    │   │
│  └────────────────────────────────────────────────────────┘   │   │    │
│                                                                 │   │    │
│  ┌─ Span Details (when span clicked) ──────────────────────┐   │   │    │
│  │  Span: POST /api/payments                                │   │   │    │
│  │  Service: payment-service                                │   │   │    │
│  │  Duration: 120ms                                         │   │   │    │
│  │  Parent: POST /api/orders                                │   │   │    │
│  │                                                          │   │   │    │
│  │  Tags:                                                   │   │   │    │
│  │    http.method: POST                                     │   │   │    │
│  │    http.url: /api/payments                               │   │   │    │
│  │    http.status_code: 200                                 │   │   │    │
│  │    payment.amount: 1500.00                               │   │   │    │
│  │                                                          │   │   │    │
│  │  Annotations:                                            │   │   │    │
│  │    [10:00:01.100] Server Receive                         │   │   │    │
│  │    [10:00:01.220] Server Send                            │   │   │    │
│  └──────────────────────────────────────────────────────────┘   │   │    │
│                                                                 │   │    │
│  ┌─ Dependency Graph ──────────────────────────────────────┐   │   │    │
│  │                                                          │   │   │    │
│  │     ┌─────────────┐                                     │   │   │    │
│  │     │ API Gateway  │                                    │   │   │    │
│  │     └──────┬──────┘                                     │   │   │    │
│  │            │                                            │   │   │    │
│  │     ┌──────▼──────┐                                     │   │   │    │
│  │     │Order Service│                                     │   │   │    │
│  │     └──┬─────┬────┘                                     │   │   │    │
│  │        │     │                                          │   │   │    │
│  │   ┌────▼┐   └────┐                                     │   │   │    │
│  │   │Pay..│       Inv..│                                  │   │   │    │
│  │   └─┬───┘   └─┬────┘                                    │   │   │    │
│  │     │         │                                         │   │   │    │
│  │   ┌─▼──┐    ┌─▼──┐                                     │   │   │    │
│  │   │PostgreSQL│Redis│                                    │   │   │    │
│  │   └──────┘   └────┘                                    │   │   │    │
│  └────────────────────────────────────────────────────────┘   │   │    │
└────────────────────────────────────────────────────────────────┘   │    │
```

### Zipkin API

Zipkin ka UI ke alawa ek REST API bhi hai jo programmatic access deta hai:

```bash
# Saare services list karo
curl http://localhost:9411/api/v2/services

# Saare span names ek service ke liye
curl "http://localhost:9411/api/v2/spans?serviceName=order-service"

# Traces search karo
curl "http://localhost:9411/api/v2/traces?serviceName=order-service&limit=10"

# Specific trace dekho
curl "http://localhost:9411/api/v2/trace/4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e"
```

### Zipkin Data Model

```json
// Ek span ka JSON format (jo Zipkin ko bheja jaata hai)
{
  "traceId": "4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e",
  "id": "a1b2c3d4e5f6a7b8",
  "parentId": "1122334455667788",
  "name": "POST /api/orders",
  "kind": "SERVER",
  "timestamp": 1681234567890000,
  "duration": 150000,
  "localEndpoint": {
    "serviceName": "order-service",
    "ipv4": "192.168.1.10",
    "port": 8081
  },
  "remoteEndpoint": {
    "serviceName": "payment-service",
    "ipv4": "192.168.1.11",
    "port": 8082
  },
  "tags": {
    "http.method": "POST",
    "http.url": "/api/orders",
    "http.status_code": "200",
    "user.id": "user_123"
  },
  "annotations": [
    {
      "timestamp": 1681234567890000,
      "value": "sr"  // Server Receive
    },
    {
      "timestamp": 1681234568040000,
      "value": "ss"  // Server Send
    }
  ]
}
```

### Zipkin Storage Options

```
┌─────────────────────────────────────────────────────────────────┐
│                    Zipkin Storage Options                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  1. In-Memory (Default - Development)                          │
│     ✓ Zero configuration                                         │
│     ✓ Fast                                                       │
│     ✗ Data lost on restart                                       │
│     ✗ Production mein use mat karo                               │
│     docker run -p 9411:9411 openzipkin/zipkin                    │
│                                                                 │
│  2. Elasticsearch (Recommended for Production)                  │
│     ✓ Persistent storage                                         │
│     ✓ Scalable                                                   │
│     ✓ Full-text search                                           │
│     docker run -p 9411:9411 \                                   │
│       -e STORAGE_TYPE=elasticsearch \                            │
│       -e ES_HOSTS=http://es:9200 \                               │
│       openzipkin/zipkin                                          │
│                                                                 │
│  3. Cassandra                                                    │
│     ✓ High write throughput                                      │
│     ✓ Good for large scale                                       │
│     docker run -p 9411:9411 \                                   │
│       -e STORAGE_TYPE=cassandra3 \                               │
│       -e CASSANDRA_CONTACT_POINTS=cassandra \                    │
│       openzipkin/zipkin                                          │
│                                                                 │
│  4. MySQL                                                        │
│     ✓ Agar already MySQL infrastructure hai                      │
│     ✥ Limited scale ke liye                                      │
│     docker run -p 9411:9411 \                                   │
│       -e STORAGE_TYPE=mysql \                                    │
│       -e MYSQL_HOST=mysql \                                      │
│       -e MYSQL_USER=zipkin \                                     │
│       -e MYSQL_PASS=zipkin \                                     │
│       openzipkin/zipkin                                          │
│                                                                 │
│  5. Kafka (Collector only - async processing)                    │
│     ✓ Services -> Kafka -> Zipkin (async)                        │
│     ✓ High throughput scenarios mein useful                      │
│     ✗ Zipkin UI mein direct nahi dikhta, storage chahiye         │
│     docker run -p 9411:9411 \                                   │
│       -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \                    │
│       -e KAFKA_TOPIC=zipkin \                                    │
│       openzipkin/zipkin                                          │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Complete Working Example (Spring Boot 3 + Micrometer + Zipkin)

### Project Structure

```
microservices-tracing-demo/
├── api-gateway/              (Port: 8080)
├── order-service/            (Port: 8081)
├── payment-service/          (Port: 8082)
├── inventory-service/        (Port: 8083)
└── docker-compose.yml        (Zipkin + other services)
```

### Step 1: Docker Compose (Zipkin Setup)

```yaml
# docker-compose.yml
version: '3.8'

services:
  zipkin:
    image: openzipkin/zipkin:latest
    container_name: zipkin
    ports:
      - "9411:9411"
    environment:
      - STORAGE_TYPE=mem  # Production mein elasticsearch use karo
    networks:
      - tracing-network

  # Optional: Elasticsearch for persistent storage
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    networks:
      - tracing-network

networks:
  tracing-network:
    driver: bridge
```

```bash
# Zipkin start karo
docker-compose up -d zipkin

# Check if running
curl http://localhost:9411/health

# UI access: http://localhost:9411/zipkin/
```

### Step 2: API Gateway (Spring Boot 3)

**`pom.xml`:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <groupId>com.example</groupId>
    <artifactId>api-gateway</artifactId>
    <version>1.0.0</version>
    
    <dependencies>
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Micrometer Tracing with Brave -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        
        <!-- Zipkin Reporter -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-reporter-zipkin</artifactId>
        </dependency>
        
        <!-- Spring Boot Actuator (for health, metrics) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
</project>
```

**`application.yml`:**
```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

management:
  tracing:
    sampling:
      probability: 1.0
  
  endpoints:
    web:
      exposure:
        include: health,info,metrics

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
  level:
    root: INFO
```

**Gateway Controller:**
```java
package com.example.apigateway.controller;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GatewayController {
    
    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);
    
    private final RestTemplate restTemplate;
    private final Tracer tracer;
    
    public GatewayController(RestTemplate restTemplate, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.tracer = tracer;
    }
    
    @PostMapping("/orders")
    @Observed(name = "gateway.place-order", contextualName = "placing-order")
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestBody Map<String, Object> orderRequest) {
        
        log.info("Gateway: Received order request, forwarding to order-service");
        
        // Trace ID log mein automatically print hoga (MDC se)
        String traceId = tracer.currentSpan().context().traceId();
        log.info("Gateway: Trace ID = {}", traceId);
        
        // Order service ko forward karo
        // B3 headers AUTOMATICALLY add ho jaayenge (Micrometer ki wajah se)
        ResponseEntity<Map> orderResponse = restTemplate.postForEntity(
            "http://localhost:8081/api/orders",
            orderRequest,
            Map.class
        );
        
        log.info("Gateway: Order service responded, returning to client");
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "order", orderResponse.getBody(),
            "traceId", traceId
        ));
    }
}
```

### Step 3: Order Service (Spring Boot 3)

**`application.yml`:**
```yaml
server:
  port: 8081

spring:
  application:
    name: order-service
  
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

management:
  tracing:
    sampling:
      probability: 1.0

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Order Controller:**
```java
package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    
    private final OrderService orderService;
    private final Tracer tracer;
    private final ObservationRegistry observationRegistry;
    
    public OrderController(OrderService orderService, 
                          Tracer tracer, 
                          ObservationRegistry observationRegistry) {
        this.orderService = orderService;
        this.tracer = tracer;
        this.observationRegistry = observationRegistry;
    }
    
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        
        // Yeh log line mein traceId aur spanId automatically hoga
        log.info("OrderService: Creating order for user: {}", order.getUserId());
        
        Order createdOrder = orderService.createOrder(order);
        
        log.info("OrderService: Order created successfully, ID: {}", createdOrder.getId());
        
        return ResponseEntity.ok(createdOrder);
    }
}
```

**Order Service (Business Logic):**
```java
package com.example.orderservice.service;

import com.example.orderservice.model.Order;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {
    
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final RestTemplate restTemplate;
    private final Tracer tracer;
    
    public OrderService(RestTemplate restTemplate, Tracer tracer) {
        this.restTemplate = restTemplate;
        this.tracer = tracer;
    }
    
    @Observed(name = "order.create", contextualName = "creating-order")
    public Order createOrder(Order order) {
        
        // Custom span ke andar business logic wrap karo
        Span processingSpan = tracer.nextSpan().name("order-business-logic").start();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(processingSpan)) {
            
            processingSpan.tag("order.userId", order.getUserId());
            processingSpan.tag("order.itemCount", String.valueOf(order.getItems().size()));
            
            log.info("OrderService: Processing order for user {}", order.getUserId());
            
            // Step 1: Validate order
            validateOrder(order);
            
            // Step 2: Process payment (calls payment-service)
            // Yeh call bhi trace hogi, child span banega
            boolean paymentSuccess = processPayment(order);
            
            if (!paymentSuccess) {
                processingSpan.tag("payment.status", "failed");
                throw new RuntimeException("Payment failed");
            }
            
            // Step 3: Update inventory (calls inventory-service)
            // Yeh bhi child span banega
            boolean inventoryUpdated = updateInventory(order);
            
            if (!inventoryUpdated) {
                processingSpan.tag("inventory.status", "failed");
                throw new RuntimeException("Inventory update failed");
            }
            
            // Step 4: Finalize order
            order.setId(UUID.randomUUID().toString());
            order.setStatus("COMPLETED");
            order.setCreatedAt(LocalDateTime.now());
            
            processingSpan.tag("order.status", "completed");
            processingSpan.tag("order.id", order.getId());
            
            log.info("OrderService: Order {} completed successfully", order.getId());
            
            return order;
            
        } catch (Exception e) {
            processingSpan.tag("error", e.getMessage());
            log.error("OrderService: Order processing failed", e);
            throw e;
        } finally {
            processingSpan.end();
        }
    }
    
    @Observed(name = "order.validate", contextualName = "validating-order")
    private void validateOrder(Order order) {
        if (order.getUserId() == null || order.getUserId().isEmpty()) {
            throw new IllegalArgumentException("User ID is required");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
        log.info("OrderService: Order validated successfully");
    }
    
    @Observed(name = "order.payment", contextualName = "processing-payment")
    private boolean processPayment(Order order) {
        log.info("OrderService: Calling payment-service for order {}", order.getId());
        
        try {
            // RestTemplate call - Micrometer automatically adds B3 headers
            // aur naya child span banata hai
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:8082/api/payments",
                Map.of(
                    "orderId", order.getId(),
                    "amount", order.getTotalAmount(),
                    "userId", order.getUserId()
                ),
                Map.class
            );
            
            log.info("OrderService: Payment service responded");
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("OrderService: Payment service call failed", e);
            return false;
        }
    }
    
    @Observed(name = "order.inventory", contextualName = "updating-inventory")
    private boolean updateInventory(Order order) {
        log.info("OrderService: Calling inventory-service for order {}", order.getId());
        
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                "http://localhost:8083/api/inventory/reserve",
                Map.of(
                    "orderId", order.getId(),
                    "items", order.getItems()
                ),
                Map.class
            );
            
            log.info("OrderService: Inventory service responded");
            return response.getStatusCode().is2xxSuccessful();
            
        } catch (Exception e) {
            log.error("OrderService: Inventory service call failed", e);
            return false;
        }
    }
}
```

### Step 4: Payment Service (Spring Boot 3)

**`application.yml`:**
```yaml
server:
  port: 8082

spring:
  application:
    name: payment-service
  
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

management:
  tracing:
    sampling:
      probability: 1.0

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Payment Controller:**
```java
package com.example.paymentservice.controller;

import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    
    private final Tracer tracer;
    
    public PaymentController(Tracer tracer) {
        this.tracer = tracer;
    }
    
    @PostMapping
    @Observed(name = "payment.create", contextualName = "creating-payment")
    public ResponseEntity<Map<String, Object>> processPayment(
            @RequestBody Map<String, Object> paymentRequest) {
        
        // MDC se traceId automatically log mein dikhega
        log.info("PaymentService: Processing payment for order: {}", 
                 paymentRequest.get("orderId"));
        
        // Simulate payment processing
        simulatePaymentProcessing();
        
        String transactionId = UUID.randomUUID().toString();
        
        log.info("PaymentService: Payment successful, transaction ID: {}", transactionId);
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "transactionId", transactionId,
            "amount", paymentRequest.get("amount")
        ));
    }
    
    @Observed(name = "payment.simulate", contextualName = "simulating-payment")
    private void simulatePaymentProcessing() {
        try {
            Thread.sleep(100); // Simulate network delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Step 5: Inventory Service (Spring Boot 3)

**`application.yml`:**
```yaml
server:
  port: 8083

spring:
  application:
    name: inventory-service
  
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

management:
  tracing:
    sampling:
      probability: 1.0

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Inventory Controller:**
```java
package com.example.inventoryservice.controller;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);
    
    @PostMapping("/reserve")
    @Observed(name = "inventory.reserve", contextualName = "reserving-inventory")
    public ResponseEntity<Map<String, Object>> reserveInventory(
            @RequestBody Map<String, Object> request) {
        
        log.info("InventoryService: Reserving inventory for order: {}", 
                 request.get("orderId"));
        
        // Simulate inventory check
        simulateInventoryCheck();
        
        log.info("InventoryService: Inventory reserved successfully");
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Inventory reserved"
        ));
    }
    
    private void simulateInventoryCheck() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### Step 6: Logback Configuration (All Services)

**`src/main/resources/logback-spring.xml`:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    
    <springProperty scope="context" name="appName" source="spring.application.name"/>
    
    <property name="LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [${appName},%X{traceId:-},%X{spanId:-}] [%thread] %-5level %logger{36} - %msg%n"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/${appName}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/${appName}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
    
    <!-- Zipkin/Micrometer internal logs ko DEBUG level pe rakho (debugging ke liye) -->
    <logger name="io.micrometer.tracing" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
    </logger>
    
</configuration>
```

### Step 7: Testing the Complete Setup

```bash
# Step 1: Zipkin start karo
docker-compose up -d zipkin

# Step 2: Saare services start karo (alag alag terminals)
cd api-gateway && mvn spring-boot:run
cd order-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd inventory-service && mvn spring-boot:run

# Step 3: Test request bhejo
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user_123",
    "items": [
      {"productId": "P001", "quantity": 2, "price": 500},
      {"productId": "P002", "quantity": 1, "price": 1000}
    ],
    "totalAmount": 2000
  }'

# Response:
# {
#   "status": "success",
#   "order": {
#     "id": "uuid-xxxxx",
#     "userId": "user_123",
#     "status": "COMPLETED",
#     "totalAmount": 2000
#   },
#   "traceId": "4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e"
# }

# Step 4: Zipkin UI mein jaake dekho
# http://localhost:9411/zipkin/
# Service: api-gateway select karo
# Trace ID se search karo
```

### Expected Log Output (Har Service)

**API Gateway:**
```
2024-04-11 10:00:01.100 [api-gateway,4f8a7c2d,span-001] [http-nio-8080-exec-1] INFO  GatewayController - Gateway: Received order request, forwarding to order-service
2024-04-11 10:00:01.600 [api-gateway,4f8a7c2d,span-001] [http-nio-8080-exec-1] INFO  GatewayController - Gateway: Order service responded, returning to client
```

**Order Service:**
```
2024-04-11 10:00:01.150 [order-service,4f8a7c2d,span-002] [http-nio-8081-exec-1] INFO  OrderController - OrderService: Creating order for user: user_123
2024-04-11 10:00:01.160 [order-service,4f8a7c2d,span-003] [http-nio-8081-exec-1] INFO  OrderService - OrderService: Processing order for user user_123
2024-04-11 10:00:01.170 [order-service,4f8a7c2d,span-004] [http-nio-8081-exec-1] INFO  OrderService - OrderService: Calling payment-service for order null
2024-04-11 10:00:01.300 [order-service,4f8a7c2d,span-004] [http-nio-8081-exec-1] INFO  OrderService - OrderService: Payment service responded
2024-04-11 10:00:01.310 [order-service,4f8a7c2d,span-005] [http-nio-8081-exec-1] INFO  OrderService - OrderService: Calling inventory-service for order null
2024-04-11 10:00:01.400 [order-service,4f8a7c2d,span-005] [http-nio-8081-exec-1] INFO  OrderService - OrderService: Inventory service responded
2024-04-11 10:00:01.410 [order-service,4f8a7c2d,span-003] [http-nio-8081-exec-1] INFO  OrderService - OrderService: Order uuid-xxxxx completed successfully
2024-04-11 10:00:01.420 [order-service,4f8a7c2d,span-002] [http-nio-8081-exec-1] INFO  OrderController - OrderService: Order created successfully, ID: uuid-xxxxx
```

**Payment Service:**
```
2024-04-11 10:00:01.200 [payment-service,4f8a7c2d,span-006] [http-nio-8082-exec-1] INFO  PaymentController - PaymentService: Processing payment for order: null
2024-04-11 10:00:01.300 [payment-service,4f8a7c2d,span-006] [http-nio-8082-exec-1] INFO  PaymentController - PaymentService: Payment successful, transaction ID: txn-yyyy
```

**Inventory Service:**
```
2024-04-11 10:00:01.320 [inventory-service,4f8a7c2d,span-007] [http-nio-8083-exec-1] INFO  InventoryController - InventoryService: Reserving inventory for order: null
2024-04-11 10:00:01.400 [inventory-service,4f8a7c2d,span-007] [http-nio-8083-exec-1] INFO  InventoryController - InventoryService: Inventory reserved successfully
```

> **Notice:** Saare services mein `4f8a7c2d` trace ID SAME hai! Yehi distributed tracing ka power hai. Tum `grep "4f8a7c2d"` karo toh pure distributed flow ke saare logs mil jaayenge.

### Zipkin UI Timeline View

```
Trace: 4f8a7c2d1e9b3f5a6d8c0e2f4a6b8d0e    Total: 500ms

api-gateway          [████████████████████] 500ms
  └─ order-service   [    ████████████████] 450ms
       ├─ payment-svc [      █████████] 100ms
       └─ inventory-svc [        ████████] 80ms
```

---

## 10. Best Practices

### 1. Sampling Rate Sahi Choose Karo

```yaml
# Development: 100% trace karo
management:
  tracing:
    sampling:
      probability: 1.0

# Staging: 50% trace karo
management:
  tracing:
    sampling:
      probability: 0.5

# Production (high traffic > 1000 RPS): 1-10% trace karo
management:
  tracing:
    sampling:
      probability: 0.01

# Production (medium traffic 100-1000 RPS): 10-50% trace karo
management:
  tracing:
    sampling:
      probability: 0.1
```

**Rule of thumb:** Jitna zyada traffic, utna kam sampling rate. Zipkin ko har request bhejne se performance impact ho sakta hai.

### 2. Sensitive Data Tags Mein Mat Daalo

```java
// ❌ BAD - Password, token, PII data tags mein mat daalo
span.tag("user.password", password);
span.tag("payment.creditCard", cardNumber);
span.tag("user.email", email);  // GDPR issue ho sakta hai

// ✅ GOOD - Sirf non-sensitive identifiers use karo
span.tag("user.id", userId);
span.tag("order.amount", String.valueOf(amount));
span.tag("payment.status", status);
```

### 3. Tag Naming Convention Follow Karo

```java
// Consistent tag naming convention use karo
// Format: <domain>.<entity>.<attribute>

span.tag("http.method", "POST");
span.tag("http.url", "/api/orders");
span.tag("http.status_code", "200");

span.tag("db.statement", "SELECT * FROM orders");
span.tag("db.system", "postgresql");

span.tag("messaging.system", "kafka");
span.tag("messaging.destination", "order-events");

span.tag("order.id", "ORD-123");
span.tag("order.userId", "user_456");
span.tag("order.totalAmount", "2500.00");
```

### 4. Error Handling in Spans

```java
Span span = tracer.nextSpan().name("risky-operation").start();
try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
    
    // Risky operation
    doSomethingRisky();
    
    span.tag("operation.status", "success");
    
} catch (Exception e) {
    // Error ko span mein record karo
    span.tag("error", e.getClass().getSimpleName());
    span.tag("error.message", e.getMessage());
    span.tag("operation.status", "failed");
    throw e;
} finally {
    span.end();
}
```

### 5. Span Name Meaningful Rakho

```java
// ❌ BAD - Generic names
Span span = tracer.nextSpan().name("process").start();
Span span = tracer.nextSpan().name("handler").start();

// ✅ GOOD - Descriptive names
Span span = tracer.nextSpan().name("order.create.validate").start();
Span span = tracer.nextSpan().name("payment.gateway.call").start();
Span span = tracer.nextSpan().name("inventory.reserve-stock").start();
```

### 6. Custom Baggage for Cross-Cutting Concerns

```yaml
# application.yml
management:
  tracing:
    baggage:
      remote-fields: userId,tenantId,correlationId  # HTTP headers mein jaayega
      local-fields: debugMode,featureFlag            # Sirf local process mein
```

```java
// Baggage use karna
Span span = tracer.currentSpan();
span.tag("userId", request.getUserId());

// Custom baggage header mein automatically propagate hoga
// aur receiving service mein automatically read ho jaayega
```

### 7. Log Correlation hamesha Enable Rakho

```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

Yeh ek line tumhare debugging life ko 10x easy bana dega.

### 8. Zipkin Retention Policy Set Karo

```bash
# Elasticsearch storage mein retention policy
docker run -d \
  --name zipkin \
  -p 9411:9411 \
  -e STORAGE_TYPE=elasticsearch \
  -e ES_HOSTS=http://elasticsearch:9200 \
  -e ES_INDEX_REPLICAS=1 \
  -e ES_INDEX_SHARDS=5 \
  -e ES_INDEX_TTL=7 \  # 7 days retention
  openzipkin/zipkin:latest
```

### 9. Production mein Zipkin ko Sidecar Pattern mein Deploy Karo

```
┌─────────────────────────────────────────────┐
│              Kubernetes Pod                 │
│                                             │
│  ┌─────────────┐    ┌──────────────────┐   │
│  │   Your      │───▶│  Zipkin Sidecar  │   │
│  │  Service    │    │  (local buffer + │   │
│  │             │    │   batch send)    │   │
│  └─────────────┘    └────────┬─────────┘   │
│                              │             │
└──────────────────────────────┼─────────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │   Central Zipkin     │
                    │   Server / Cluster   │
                    └──────────────────────┘
```

### 10. Multiple Tracing Backends ke Liye OpenTelemetry Consider Karo

Agar tumhe future-proof banana hai:

```xml
<!-- OpenTelemetry bridge -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
</dependency>
```

OpenTelemetry se tum Zipkin, Jaeger, AWS X-Ray, Datadog - kisi bhi backend ko switch kar sakte ho bina code change kiye.

---

## 11. Common Issues & Debugging Tips

### Issue 1: Trace ID Propagate Nahi Ho Raha

**Symptom:** Different services mein alag-alag trace IDs aa rahe hain.

**Causes & Solutions:**

```java
// Cause 1: Custom HTTP client use kar rahe ho jo B3 headers forward nahi kar raha
// Solution: RestTemplate/WebClient use karo (Micrometer auto-instrument karta hai)

// Cause 2: Async call mein context propagate nahi ho raha
// Solution: TaskDecorator use karo (dekho section 5)

// Cause 3: Message queue (Kafka/RabbitMQ) mein headers propagate nahi ho rahe
// Solution: KafkaTemplate.setObservationRegistry() set karo
```

**Debugging:**
```bash
# Service A se Service B ka curl karo aur headers check karo
curl -v http://service-a:8081/api/test

# Response headers mein dekho:
# X-B3-TraceId: <trace-id>
# X-B3-SpanId: <span-id>
```

### Issue 2: Logs Mein Trace ID Nahi Dikha Raha

**Symptom:** Log lines mein `[,-]` aa raha hai traceId/spanId ki jagah.

**Solution:**
```xml
<!-- logback-spring.xml mein check karo: -->
<!-- %X{traceId:-} default mein hyphen print karega agar value nahi hai -->
<property name="LOG_PATTERN" 
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} [${spring.application.name:},%X{traceId:-},%X{spanId:-}] [%thread] %-5level %logger{36} - %msg%n"/>
```

### Issue 3: Zipkin UI Mein Traces Nahi Dikha Rahe

**Checklist:**

```
1. Zipkin server chal raha hai?
   curl http://localhost:9411/health
   
2. Endpoint sahi configured hai?
   # application.yml mein check karo:
   spring.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
   
3. Sampling rate 0 toh nahi hai?
   management.tracing.sampling.probability=1.0  # temporarily 1.0 karo
   
4. Network connectivity theek hai?
   # Container se Zipkin tak ping karo
   curl http://zipkin:9411/health
   
5. Service ka naam set hai?
   spring.application.name=your-service-name  # yeh zaroori hai
```

### Issue 4: High Memory Usage with Tracing

**Problem:** Tracing enable karne ke baad memory usage badh gaya.

**Solutions:**

```yaml
# 1. Sampling rate kam karo
management:
  tracing:
    sampling:
      probability: 0.01  # 1% requests hi trace karo

# 2. Zipkin sender type change karo (async)
spring:
  zipkin:
    sender:
      type: kafka  # web (sync) ki jagae kafka (async) use karo

# 3. Tag size limit karo
# Bahut saare tags ya bade tag values memory lete hain
```

### Issue 5: Trace Context Lost in Thread Pool

**Problem:** `@Async` ya `CompletableFuture` mein trace ID disappear ho jaati hai.

**Solution:** (Detailed solution section 5 mein hai - MDC TaskDecorator)

```java
// Quick fix for CompletableFuture
public CompletableFuture<Order> processAsync(Order order) {
    Span currentSpan = tracer.currentSpan();
    
    return CompletableFuture.supplyAsync(() -> {
        try (Tracer.SpanInScope ws = tracer.withSpan(currentSpan)) {
            return processOrder(order);
        }
    }, executor);
}
```

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    DISTRIBUTED TRACING CHEAT SHEET                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  TRACE ID:       Ek complete request ka unique ID (same across services)│
│  SPAN ID:        Ek single operation ka ID (har operation ka alag)      │
│  PARENT SPAN ID: Kis span se yeh call aaya (tree structure banata hai)  │
│                                                                         │
│  B3 Format:                                                             │
│    Multi: X-B3-TraceId, X-B3-SpanId, X-B3-ParentSpanId                 │
│    Single: b3: {traceId}-{spanId}-{sampled}                             │
│                                                                         │
│  Spring Boot 2.x:                                                       │
│    Dependency: spring-cloud-starter-sleuth + spring-cloud-sleuth-zipkin │
│    Config: spring.sleuth.sampler.probability=1.0                        │
│                                                                         │
│  Spring Boot 3.x:                                                       │
│    Dependency: micrometer-tracing-bridge-brave + micrometer-tracing-... │
│    Config: management.tracing.sampling.probability=1.0                  │
│                                                                         │
│  MDC Pattern (logback-spring.xml):                                      │
│    %d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-},%X{spanId:-}] ...         │
│                                                                         │
│  Zipkin Start:                                                          │
│    docker run -d -p 9411:9411 openzipkin/zipkin                         │
│    UI: http://localhost:9411/zipkin/                                    │
│                                                                         │
│  Custom Span:                                                           │
│    Span span = tracer.nextSpan().name("operation-name").start();        │
│    try (Tracer.SpanInScope ws = tracer.withSpan(span)) { ... }         │
│    finally { span.end(); }                                              │
│                                                                         │
│  @Observed (Spring Boot 3):                                             │
│    @Observed(name = "operation.name", contextualName = "display-name") │
│    public void myMethod() { ... }  // Auto-traced!                      │
│                                                                         │
│  Best Practices:                                                        │
│    1. Sampling rate traffic ke hisaab se set karo                       │
│    2. Sensitive data tags mein mat daalo                                │
│    3. Consistent tag naming convention follow karo                      │
│    4. Error ko span mein record karo                                    │
│    5. Async operations mein context propagate karo                      │
│    6. Production mein Zipkin retention policy set karo                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Interview Questions (Self-Test)

1. **Trace ID aur Span ID mein kya difference hai?**
2. **B3 propagation format kya hai? Multi-header vs Single-header mein kya farak hai?**
3. **MDC kya hai aur yeh distributed tracing mein kaise help karta hai?**
4. **Spring Boot 3 mein Sleuth kyun nahi hai? Uska replacement kya hai?**
5. **Async operations mein trace context kaise propagate karte hain?**
6. **Sampling rate ka kya matlab hai? Production mein kya rate rakhte hain?**
7. **Zipkin ke alawa kaunse distributed tracing tools hain?**
8. **W3C Trace Context aur B3 mein kya difference hai?**
9. **Agar trace ID propagate nahi ho rahi toh kaise debug karoge?**
10. **Custom span kaise create karte hain Micrometer mein?**

---

## Next Steps

- **Jaeger** explore karo (Zipkin ka alternative, CNCF project hai)
- **OpenTelemetry** padho (vendor-neutral tracing standard)
- **AWS X-Ray** dekho agar AWS pe deploy kar rahe ho
- **Grafana Tempo** try karo (Grafana ecosystem mein tracing)
- Production mein **error rate alerts** setup karo tracing data se
- **Service Level Objectives (SLOs)** tracing se track karo

---

> **TL;DR:** Distributed tracing microservices debugging ka sabse powerful tool hai. Trace ID ek request ko end-to-end track karti hai, Span ID har operation ko measure karta hai. Spring Boot 2.x mein Sleuth use karo, Spring Boot 3.x mein Micrometer Tracing use karo. Zipkin se visualize karo. MDC se logs ko correlate karo. Sampling rate production mein kam rakho. Yeh setup tumhari debugging time ko hours se minutes mein laa dega!
