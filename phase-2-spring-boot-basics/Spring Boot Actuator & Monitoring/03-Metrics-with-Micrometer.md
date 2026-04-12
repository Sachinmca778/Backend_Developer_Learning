# Metrics with Micrometer

## Status: Not Started

---

## Table of Contents

1. [Micrometer Overview](#micrometer-overview)
2. [Counter](#counter)
3. [Gauge](#gauge)
4. [Timer](#timer)
5. [DistributionSummary](#distributionsummary)
6. [@Timed Annotation](#timed-annotation)
7. [Prometheus Integration](#prometheus-integration)
8. [Grafana Dashboards](#grafana-dashboards)
9. [Custom Metrics Best Practices](#custom-metrics-best-practices)

---

## Micrometer Overview

**Matlab:** Metrics collection ka facade — ek API se multiple monitoring systems (Prometheus, Datadog, New Relic) ko metrics bhej sakte ho.

### The Problem Without Micrometer

```
Prometheus ke liye alag code
Datadog ke liye alag code
New Relic ke liye alag code
→ Har monitoring system ka different API learn karna padta hai
```

### With Micrometer

```
Application → Micrometer (single API)
                 ↓
    ┌────────────┼────────────┐
    ↓            ↓            ↓
Prometheus   Datadog     New Relic
```

### Setup

```xml
<!-- Micrometer core (already included with Actuator) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Prometheus registry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configuration

```properties
# Prometheus endpoint enable karo
management.endpoints.web.exposure.include=prometheus,health,info,metrics
management.endpoint.prometheus.enabled=true

# Application metrics prefix
management.metrics.tags.application=${spring.application.name:my-app}
```

### Injecting MeterRegistry

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MeterRegistry meterRegistry;

    // Metrics yahan se collect karo
}
```

---

## Counter

**Matlab:** Sirf increment ho sakta hai — count track karne ke liye (orders, logins, errors).

### Basic Counter

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final Counter orderCounter;

    // Constructor injection
    public OrderService(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.total")
            .description("Total number of orders placed")
            .tag("source", "web")
            .register(registry);
    }

    public Order placeOrder(OrderRequest request) {
        // Order logic
        Order order = orderRepository.save(order);

        // Counter increment
        orderCounter.increment();

        return order;
    }
}
```

### Increment with Amount

```java
// Single increment
orderCounter.increment();  // +1

// Multiple increment
orderCounter.increment(5);  // +5
orderCounter.increment(request.getQuantity());  // Dynamic amount
```

### Builder Pattern

```java
Counter.builder("orders.total")
    .description("Total number of orders")
    .tags("source", "web", "status", "success")  // Multiple tags
    .register(registry);

// Short version
registry.counter("orders.total", "source", "web", "status", "success");
```

### Tagged Counters

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MeterRegistry registry;

    public Order placeOrder(OrderRequest request) {
        Order order = orderRepository.save(order);

        // Tags ke saath counter
        Counter.builder("orders.total")
            .tag("status", order.getStatus().name())  // SUCCESS, FAILED
            .tag("paymentMethod", request.getPaymentMethod())  // CREDIT_CARD, UPI
            .tag("source", "web")
            .register(registry)
            .increment();

        return order;
    }
}
```

### Prometheus Output

```
# HELP orders_total Total number of orders
# TYPE orders_total counter
orders_total{source="web",status="SUCCESS",paymentMethod="CREDIT_CARD"} 150.0
orders_total{source="web",status="FAILED",paymentMethod="UPI"} 5.0
```

---

## Gauge

**Matlab:** Current value track karta hai — up ya down ja sakta hai (queue size, cache size, active users).

### Basic Gauge

```java
@Service
@RequiredArgsConstructor
public class QueueService {

    private final Gauge queueSizeGauge;
    private final Queue<Message> messageQueue = new LinkedList<>();

    public QueueService(MeterRegistry registry) {
        this.queueSizeGauge = Gauge.builder("queue.size", messageQueue, Queue::size)
            .description("Current size of message queue")
            .register(registry);
    }

    public void enqueue(Message message) {
        messageQueue.add(message);
    }

    public Message dequeue() {
        return messageQueue.poll();
    }
}
```

### How Gauge Works

```
Gauge current value read karta hai jab metric scrape hota hai
→ Object ka current state check karta hai
→ Value up ya down ja sakta hai
```

### Gauge with Atomic Types

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final AtomicLong activeUsersGauge;

    public UserService(MeterRegistry registry) {
        this.activeUsersGauge = AtomicLong.build(0);

        Gauge.builder("users.active", activeUsersGauge, AtomicLong::get)
            .description("Currently active users")
            .register(registry);
    }

    public void userLoggedIn(String userId) {
        activeUsersGauge.incrementAndGet();
    }

    public void userLoggedOut(String userId) {
        activeUsersGauge.decrementAndGet();
    }
}
```

### Gauge vs Counter

| Feature | Counter | Gauge |
|---------|---------|-------|
| **Direction** | Sirf up | Up ya down |
| **Use Case** | Total orders, errors, logins | Queue size, cache size, active users |
| **Behavior** | Accumulates | Current value |
| **Example** | "150 total orders" | "Currently 25 items in queue" |

---

## Timer

**Matlab:** Execution time track karta hai — method duration, response time, latency.

### Basic Timer

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final Timer orderProcessingTimer;

    public OrderService(MeterRegistry registry) {
        this.orderProcessingTimer = Timer.builder("order.processing.time")
            .description("Time taken to process an order")
            .tag("operation", "placeOrder")
            .register(registry);
    }

    public Order placeOrder(OrderRequest request) {
        return orderProcessingTimer.record(() -> {
            // Order processing logic
            return orderRepository.save(request.toEntity());
        });
    }
}
```

### Timer.recordCallable()

```java
public Order placeOrder(OrderRequest request) throws Exception {
    return orderProcessingTimer.recordCallable(() -> {
        // Order processing logic
        return orderRepository.save(request.toEntity());
    });
}
```

### Manual Timer

```java
public Order placeOrder(OrderRequest request) {
    Timer.Sample sample = Timer.start(registry);

    try {
        // Order processing logic
        return orderRepository.save(request.toEntity());
    } finally {
        sample.stop(orderProcessingTimer);
    }
}
```

### Timer with Percentiles

```java
Timer.builder("order.processing.time")
    .description("Order processing time")
    .publishPercentiles(0.5, 0.75, 0.9, 0.95, 0.99)  // Percentiles calculate
    .publishPercentileHistogram()  // Histogram bhi publish karo
    .sla(Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(500))  // SLA buckets
    .register(registry);
```

### Prometheus Output

```
# HELP order_processing_time_seconds Order processing time
# TYPE order_processing_time_seconds summary
order_processing_time_seconds_count{operation="placeOrder"} 1500.0
order_processing_time_seconds_sum{operation="placeOrder"} 45.5

# HELP order_processing_time_seconds_max Order processing time max
# TYPE order_processing_time_seconds_max gauge
order_processing_time_seconds_max{operation="placeOrder"} 0.85
```

---

## DistributionSummary

**Matlab:** Numeric values ka distribution track karta hai — request size, response size, payment amounts.

### Basic DistributionSummary

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final DistributionSummary orderAmountSummary;

    public OrderService(MeterRegistry registry) {
        this.orderAmountSummary = DistributionSummary.builder("order.amount")
            .description("Order payment amounts")
            .baseUnit("rupees")
            .scale(1.0)  // Value scale factor
            .register(registry);
    }

    public Order placeOrder(OrderRequest request) {
        Order order = orderRepository.save(request.toEntity());

        // Amount record karo
        orderAmountSummary.record(request.getTotalAmount());

        return order;
    }
}
```

### DistributionSummary with Percentiles

```java
DistributionSummary.builder("order.amount")
    .description("Order payment amounts")
    .baseUnit("rupees")
    .publishPercentiles(0.5, 0.75, 0.9, 0.99)
    .publishPercentileHistogram()
    .sla(100, 500, 1000, 5000, 10000)  // Amount buckets
    .register(registry);
```

### Prometheus Output

```
# HELP order_amount_rupees Order payment amounts
# TYPE order_amount_rupees summary
order_amount_rupees_count 1500.0
order_amount_rupees_sum 750000.0
```

### When to Use What

| Metric Type | Use Case | Example |
|-------------|----------|---------|
| **Counter** | Count things (only goes up) | Total orders, errors, logins |
| **Gauge** | Current value (up/down) | Queue size, cache size, active users |
| **Timer** | Duration/latency | Method execution time, response time |
| **DistributionSummary** | Value distribution | Payment amounts, request sizes |

---

## @Timed Annotation

**Matlab:** Method execution time automatically track karta hai — manual timer code nahi chahiye.

### Setup

```xml
<!-- AOP support chahiye -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

```java
// Enable @Timed
@Configuration
@EnableAspectJAutoProxy
public class TimedConfig { }
```

### Usage

```java
@Service
public class OrderService {

    @Timed(value = "order.processing.time", description = "Order processing time")
    public Order placeOrder(OrderRequest request) {
        // Order logic
        return orderRepository.save(request.toEntity());
    }

    @Timed(value = "order.fetch.time", extraTags = {"operation", "findById"})
    public Order findById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }
}
```

### @Timed Attributes

```java
@Timed(
    value = "order.time",           // Metric name
    description = "Order timing",   // Description
    longTask = true,                // Long task timer bhi enable
    percentiles = {0.5, 0.9, 0.99}, // Percentiles
    histogram = true                // Histogram enable
)
public Order placeOrder(OrderRequest request) { }
```

### @Timed on Class Level

```java
@Service
@Timed(value = "order.service.time", extraTags = {"component", "orderService"})
public class OrderService {

    // Sab methods automatically timed hongi
    public Order placeOrder(OrderRequest request) { }
    public Order findById(Long id) { }
    public List<Order> findAll() { }
}
```

### HTTP Requests Auto-Timed

Spring Boot automatically HTTP requests ko `http.server.requests` timer mein track karta hai.

```
# Prometheus output
# HELP http_server_requests_seconds
http_server_requests_seconds_count{method="GET",uri="/api/users/{id}",status="200"} 1500.0
http_server_requests_seconds_sum{method="GET",uri="/api/users/{id}",status="200"} 45.5
```

---

## Prometheus Integration

### Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### Configuration

```properties
# Prometheus endpoint expose karo
management.endpoints.web.exposure.include=prometheus,health,info,metrics
management.endpoint.prometheus.enabled=true

# Metrics export enable
management.metrics.export.prometheus.enabled=true
```

### Prometheus Endpoint

```
GET /actuator/prometheus

# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 256000000.0
jvm_memory_used_bytes{area="nonheap",id="Metaspace"} 128000000.0

# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",uri="/api/users/{id}",status="200"} 1500.0
http_server_requests_seconds_sum{method="GET",uri="/api/users/{id}",status="200"} 45.5

# HELP orders_total Total number of orders
# TYPE orders_total counter
orders_total{source="web",status="SUCCESS"} 150.0
```

### Prometheus Configuration

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'my-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
        labels:
          application: 'my-app'
          environment: 'dev'
```

### Prometheus Queries

```promql
# Request rate (per second)
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# 99th percentile latency
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# JVM memory usage
jvm_memory_used_bytes{area="heap"}

# Order count
orders_total{source="web"}
```

---

## Grafana Dashboards

### Setup

1. **Grafana install karo**
2. **Prometheus as data source add karo**
3. **Spring Boot dashboard import karo**

### Import JVM Micrometer Dashboard

```
Grafana → Import → Dashboard ID: 4701
Source: https://grafana.com/grafana/dashboards/4701
```

### Import Spring Boot 2.x/3.x Dashboard

```
Grafana → Import → Dashboard ID: 11378
Source: https://grafana.com/grafana/dashboards/11378
```

### Custom Dashboard Panels

| Panel | PromQL Query | Description |
|-------|-------------|-------------|
| **Request Rate** | `rate(http_server_requests_seconds_count[5m])` | Requests per second |
| **Error Rate** | `rate(http_server_requests_seconds_count{status=~"5.."}[5m])` | 5xx errors per second |
| **P99 Latency** | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))` | 99th percentile latency |
| **JVM Memory** | `jvm_memory_used_bytes{area="heap"}` | Heap memory usage |
| **GC Pauses** | `rate(jvm_gc_pause_seconds_sum[5m])` | GC pause time |
| **Active Threads** | `jvm_threads_live_threads` | Live thread count |
| **Order Count** | `orders_total{source="web"}` | Total orders |

### Grafana Alerting

```yaml
# Alert: High error rate
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "High 5xx error rate on {{ $labels.instance }}"
    description: "Error rate is {{ $value }} errors/sec"

# Alert: High latency
- alert: HighLatency
  expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 1.0
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "High P99 latency on {{ $labels.instance }}"
    description: "P99 latency is {{ $value }}s"
```

---

## Custom Metrics Best Practices

### 1. Use Descriptive Names

```java
// ✅ Good
Counter.builder("orders.total")
Counter.builder("orders.failed")
Timer.builder("order.processing.time")

// ❌ Bad
Counter.builder("counter1")
Counter.builder("myMetric")
```

### 2. Use Tags Wisely

```java
// ✅ Good — low cardinality tags
Counter.builder("orders.total")
    .tag("status", "success")  // Fixed values
    .tag("source", "web")
    .register(registry);

// ❌ Bad — high cardinality tag (user_id = unlimited values)
Counter.builder("orders.total")
    .tag("user_id", userId)  // BAD! Har user ke liye naya series banega
    .register(registry);
```

### 3. Cardinatlity Warning

```
High cardinality = Bahut sare unique tag values
→ Prometheus mein bahut sare time series banenge
→ Memory usage badhega
→ Performance slow hoga

Rule: Tags with limited values use karo (status, method, source)
Avoid: Tags with unlimited values (user_id, order_id, timestamp)
```

### 4. Register Metrics Once

```java
// ✅ Good — constructor mein register karo
@Service
public class OrderService {
    private final Counter orderCounter;

    public OrderService(MeterRegistry registry) {
        this.orderCounter = Counter.builder("orders.total")
            .tag("source", "web")
            .register(registry);
    }

    public void placeOrder() {
        orderCounter.increment();  // Use pre-registered counter
    }
}

// ❌ Bad — har baar register karo
public void placeOrder() {
    Counter.builder("orders.total")  // Har call pe naya counter banega
        .tag("source", "web")
        .register(registry)
        .increment();
}
```

### 5. Common Metrics Template

```java
@Component
public class ApplicationMetrics {

    private final Counter requestCounter;
    private final Counter errorCounter;
    private final Timer requestTimer;
    private final Gauge activeRequestsGauge;
    private final AtomicLong activeRequests = new AtomicLong(0);

    public ApplicationMetrics(MeterRegistry registry) {
        this.requestCounter = Counter.builder("app.requests.total")
            .description("Total requests")
            .register(registry);

        this.errorCounter = Counter.builder("app.errors.total")
            .description("Total errors")
            .register(registry);

        this.requestTimer = Timer.builder("app.request.time")
            .description("Request processing time")
            .publishPercentiles(0.5, 0.9, 0.95, 0.99)
            .register(registry);

        this.activeRequestsGauge = Gauge.builder("app.requests.active",
                activeRequests, AtomicLong::get)
            .description("Currently active requests")
            .register(registry);
    }

    public <T> T trackRequest(Supplier<T> operation) {
        activeRequests.incrementAndGet();
        requestCounter.increment();

        try {
            return requestTimer.record(operation);
        } catch (Exception e) {
            errorCounter.increment();
            throw e;
        } finally {
            activeRequests.decrementAndGet();
        }
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Micrometer** | Single API se multiple monitoring systems ko metrics bhejo |
| **Counter** | Sirf increment — total orders, errors, logins |
| **Gauge** | Current value — queue size, active users |
| **Timer** | Duration — method execution time, response latency |
| **DistributionSummary** | Value distribution — payment amounts, request sizes |
| **@Timed** | Method execution time automatically track karo |
| **Prometheus** | `/actuator/prometheus` endpoint se metrics scrape karo |
| **Grafana** | Dashboards import karo — JVM, HTTP, custom metrics |
| **Tags** | Low cardinality use karo — unlimited values avoid karo |
| **Registration** | Metrics ko constructor mein register karo — har call mein nahi |
