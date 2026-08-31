# Spring Boot Actuator & Monitoring - Top 10 Interview Questions & Answers

> Covering: Actuator Endpoints, Health Indicators, Micrometer Metrics, Prometheus, Grafana, and Logging

---

## Q1: Spring Boot Actuator kya hai aur production application mein iska use kyu hota hai?

**Answer:**

Spring Boot Actuator ek production-ready monitoring aur management module hai. Isse hum running application ka health, metrics, environment, beans, mappings, logs, thread dump, heap dump jaise details dekh sakte hain.

Simple words mein, Actuator application ke andar ek monitoring window provide karta hai jisse pata chalta hai app properly chal rahi hai ya nahi.

### Main Uses:

| Feature | Use |
|---------|-----|
| Health check | App, DB, disk, external service status check |
| Metrics | CPU, memory, JVM, HTTP request data |
| Info | App version, build info, git info |
| Loggers | Runtime pe log level view/change |
| Thread dump | Blocked threads/deadlock debugging |
| Heap dump | Memory leak analysis |

### Dependency:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Default Endpoint:

```
http://localhost:8080/actuator
```

**Important:**

By default Spring Boot sirf `health` aur `info` endpoints expose karta hai. Baaki endpoints expose karne ke liye configuration karni padti hai.

```properties
management.endpoints.web.exposure.include=health,info,metrics
```

Production mein `*` expose karna risky hota hai, kyunki `env`, `beans`, `heapdump` jaise endpoints sensitive information reveal kar sakte hain.

---

## Q2: Actuator endpoints ko enable aur expose kaise karte hain?

**Answer:**

Actuator endpoints app ke management URLs hote hain. Kuch endpoints default enabled hote hain, lekin web pe expose alag se karna padta hai.

### Common Configuration:

```properties
# Specific endpoints expose karo
management.endpoints.web.exposure.include=health,info,metrics,loggers

# All endpoints expose karo - development ke liye only
management.endpoints.web.exposure.include=*

# Specific endpoints exclude karo
management.endpoints.web.exposure.exclude=env,beans

# Base path change karo
management.endpoints.web.base-path=/monitor
```

Agar base path `/monitor` kar diya, toh endpoint URL banega:

```
http://localhost:8080/monitor/health
```

### Common Endpoints:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Application health |
| `/actuator/info` | App custom/build/git info |
| `/actuator/metrics` | Available metrics list |
| `/actuator/metrics/{name}` | Specific metric details |
| `/actuator/env` | Environment properties |
| `/actuator/beans` | Spring beans details |
| `/actuator/mappings` | Controller mappings |
| `/actuator/loggers` | Runtime log levels |
| `/actuator/threaddump` | Thread snapshot |
| `/actuator/heapdump` | JVM memory dump |

### Production Best Practice:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.env.enabled=false
management.endpoint.beans.enabled=false
```

Interview mein yaad rakho: endpoint enable hona aur endpoint expose hona dono alag concepts hain. Enable means endpoint available hai, expose means HTTP/JMX ke through access allowed hai.

---

## Q3: `/actuator/health` endpoint kya show karta hai aur health status kaise decide hota hai?

**Answer:**

`/actuator/health` application aur uske dependent components ka status show karta hai, jaise database, disk space, Redis, external APIs, mail server, etc.

### Basic Response:

```json
{
  "status": "UP"
}
```

By default details hidden hoti hain. Details show karne ke liye:

```properties
management.endpoint.health.show-details=always
```

### Detailed Response:

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Health Status Values:

| Status | Meaning |
|--------|---------|
| `UP` | Component properly working |
| `DOWN` | Component failed |
| `OUT_OF_SERVICE` | Component intentionally unavailable |
| `UNKNOWN` | Status determine nahi ho paya |

### HTTP Status Mapping:

```
UP             -> 200 OK
DOWN           -> 503 Service Unavailable
OUT_OF_SERVICE -> 503 Service Unavailable
```

Overall status aggregation mein failure priority high hoti hai. Agar ek important component `DOWN` hai, toh overall application health bhi `DOWN` ho sakti hai.

---

## Q4: Custom HealthIndicator kya hota hai aur kab banate hain?

**Answer:**

Custom `HealthIndicator` tab banate hain jab hume apne business-specific component ka health check karna ho, jaise payment gateway, third-party API, Redis, Kafka, email service, ya custom database query.

Spring Boot built-in indicators provide karta hai, but custom dependency ke liye apna indicator banana useful hota hai.

### Recommended Way: AbstractHealthIndicator

```java
@Component
public class PaymentGatewayHealthIndicator extends AbstractHealthIndicator {

    private final RestTemplate restTemplate;

    public PaymentGatewayHealthIndicator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity("https://payment.example.com/ping", String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                builder.up()
                    .withDetail("paymentGateway", "Online");
            } else {
                builder.down()
                    .withDetail("paymentGateway", "Offline")
                    .withDetail("status", response.getStatusCode());
            }
        } catch (Exception ex) {
            builder.down()
                .withDetail("paymentGateway", "Unreachable")
                .withDetail("error", ex.getMessage());
        }
    }
}
```

### Output:

```json
{
  "status": "UP",
  "components": {
    "paymentGateway": {
      "status": "UP",
      "details": {
        "paymentGateway": "Online"
      }
    }
  }
}
```

**Interview Point:**

Spring Boot automatically sab `HealthIndicator` beans ko collect karke `/actuator/health` response mein add kar deta hai. Manually register karne ki usually zarurat nahi hoti.

---

## Q5: Liveness aur Readiness probes mein kya difference hai?

**Answer:**

Liveness aur Readiness probes Kubernetes/cloud deployments mein application lifecycle manage karne ke liye use hote hain.

### Liveness Probe:

Liveness check karta hai ki application alive hai ya nahi.

Agar liveness fail hoti hai, Kubernetes pod ko restart karta hai.

Examples:

- Application deadlock mein chali gayi
- Process stuck ho gaya
- App respond nahi kar rahi

Endpoint:

```
/actuator/health/liveness
```

### Readiness Probe:

Readiness check karta hai ki application traffic receive karne ke liye ready hai ya nahi.

Agar readiness fail hoti hai, Kubernetes traffic bhejna stop kar deta hai, lekin pod restart nahi karta.

Examples:

- Database temporarily down hai
- Redis connect nahi ho raha
- External service unavailable hai
- App startup complete nahi hua

Endpoint:

```
/actuator/health/readiness
```

### Configuration:

```properties
management.endpoint.health.probes.enabled=true
management.endpoint.health.group.liveness.include=livenessState
management.endpoint.health.group.readiness.include=readinessState,db,redis
```

### Difference:

| Point | Liveness | Readiness |
|-------|----------|-----------|
| Question | App alive hai? | App traffic ke liye ready hai? |
| Failure action | Pod restart | Traffic stop |
| Checks | Deadlock, app stuck | DB, cache, external services |
| Restart? | Yes | No |

Short answer: Liveness app ko restart karwane ke liye hota hai, Readiness app ko load balancer traffic se temporarily remove karne ke liye hota hai.

---

## Q6: Micrometer kya hai aur Spring Boot monitoring mein iska role kya hai?

**Answer:**

Micrometer metrics collection ka facade hai. Jaise SLF4J logging ke liye facade hai, waise Micrometer metrics ke liye facade hai.

Iska fayda yeh hai ki application code ek common API use karta hai, aur metrics Prometheus, Datadog, New Relic, Graphite jaise systems ko bheje ja sakte hain.

### Without Micrometer:

```
Prometheus ke liye alag API
Datadog ke liye alag API
New Relic ke liye alag API
```

### With Micrometer:

```
Application -> Micrometer -> Prometheus / Datadog / New Relic
```

### Setup:

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

### Configuration:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.prometheus.enabled=true
management.metrics.tags.application=${spring.application.name:my-app}
```

### MeterRegistry:

```java
@Service
public class OrderService {

    private final MeterRegistry meterRegistry;

    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }
}
```

`MeterRegistry` Micrometer ka main object hai jiske through counters, gauges, timers, summaries register karte hain.

---

## Q7: Counter, Gauge, Timer aur DistributionSummary mein kya difference hai?

**Answer:**

Ye Micrometer ke common metric types hain. Har metric ka use case different hota hai.

### Counter:

Counter sirf increase hota hai. Total count track karne ke liye use hota hai.

Examples:

- Total orders
- Total login attempts
- Total errors

```java
Counter orderCounter = Counter.builder("orders.total")
    .description("Total orders placed")
    .tag("source", "web")
    .register(registry);

orderCounter.increment();
```

### Gauge:

Gauge current value track karta hai. Value increase bhi ho sakti hai aur decrease bhi.

Examples:

- Active users
- Queue size
- Cache size

```java
AtomicLong activeUsers = new AtomicLong(0);

Gauge.builder("users.active", activeUsers, AtomicLong::get)
    .description("Currently active users")
    .register(registry);
```

### Timer:

Timer duration ya latency track karta hai.

Examples:

- API response time
- Method execution time
- Order processing time

```java
Timer timer = Timer.builder("order.processing.time")
    .description("Order processing time")
    .register(registry);

timer.record(() -> processOrder());
```

### DistributionSummary:

DistributionSummary numeric values ka distribution track karta hai.

Examples:

- Payment amount
- Request size
- Response size

```java
DistributionSummary summary = DistributionSummary.builder("order.amount")
    .baseUnit("rupees")
    .register(registry);

summary.record(1500);
```

### Quick Comparison:

| Metric Type | Tracks | Example |
|-------------|--------|---------|
| Counter | Always increasing count | Total orders |
| Gauge | Current value | Active users |
| Timer | Time duration | API latency |
| DistributionSummary | Value distribution | Order amount |

---

## Q8: Prometheus aur Grafana Spring Boot ke saath kaise integrate hote hain?

**Answer:**

Prometheus metrics collect/scrape karta hai, aur Grafana un metrics ko dashboard mein visualize karta hai.

### Flow:

```
Spring Boot App -> /actuator/prometheus -> Prometheus -> Grafana Dashboard
```

### Dependencies:

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

### Spring Boot Configuration:

```properties
management.endpoints.web.exposure.include=prometheus,health,info,metrics
management.endpoint.prometheus.enabled=true
```

### Prometheus Endpoint:

```
GET /actuator/prometheus
```

Example output:

```text
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 256000000.0
http_server_requests_seconds_count{method="GET",status="200"} 1500.0
orders_total{source="web",status="SUCCESS"} 150.0
```

### prometheus.yml:

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: "spring-boot-app"
    metrics_path: "/actuator/prometheus"
    static_configs:
      - targets: ["localhost:8080"]
```

### Useful PromQL Queries:

```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Average response time
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# JVM heap memory
jvm_memory_used_bytes{area="heap"}
```

Grafana mein Prometheus ko data source add karke JVM, HTTP requests, latency, error rate, custom business metrics ke dashboards bana sakte hain.

---

## Q9: Spring Boot logging mein SLF4J, Logback, log levels aur MDC ka kya role hai?

**Answer:**

Spring Boot default logging stack mein SLF4J API aur Logback implementation use hota hai.

### SLF4J vs Logback:

| Component | Role |
|-----------|------|
| SLF4J | Logging facade/interface |
| Logback | Actual logging implementation |

Application code SLF4J ke through logs likhta hai. Logback decide karta hai logs console/file/JSON format mein kaise print honge.

### Basic Logging:

```java
private static final Logger log = LoggerFactory.getLogger(UserService.class);

log.info("User created with id: {}", userId);
log.error("Failed to create user: {}", email, exception);
```

### Lombok `@Slf4j`:

```java
@Slf4j
@Service
public class UserService {

    public void createUser(String email) {
        log.info("Creating user: {}", email);
    }
}
```

### Log Levels:

| Level | Use |
|-------|-----|
| TRACE | Very detailed debugging |
| DEBUG | Development debugging |
| INFO | Normal important events |
| WARN | Potential issue |
| ERROR | Failure/exception |

If root level `INFO` hai, toh `TRACE` aur `DEBUG` print nahi honge, but `INFO`, `WARN`, `ERROR` print honge.

```properties
logging.level.root=INFO
logging.level.com.example=DEBUG
logging.level.org.springframework.web=DEBUG
```

### MDC:

MDC ka full form hai Mapped Diagnostic Context. Yeh request-specific data, jaise `correlationId` aur `userId`, thread-local mein store karta hai. Phir har log line mein automatically include ho sakta hai.

```java
MDC.put("correlationId", correlationId);
MDC.put("userId", userId);
```

```properties
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%X{correlationId}] [%X{userId}] %-5level %logger{36} - %msg%n
```

**Important:**

Request complete hone ke baad `MDC.clear()` call karna chahiye, warna thread reuse hone par wrong context next request mein leak ho sakta hai.

---

## Q10: Actuator endpoints aur monitoring ko production mein secure kaise karte hain?

**Answer:**

Production mein Actuator endpoints powerful hote hain, isliye unko properly secure karna bahut important hai. Wrong exposure se secrets, config, internal mappings, heap data, environment variables leak ho sakte hain.

### Sensitive Endpoints:

| Endpoint | Risk |
|----------|------|
| `/actuator/env` | Secrets/config values reveal ho sakte hain |
| `/actuator/beans` | Internal app structure expose hota hai |
| `/actuator/mappings` | API routes expose hote hain |
| `/actuator/heapdump` | Memory mein sensitive data ho sakta hai |
| `/actuator/loggers` | Runtime log level modify ho sakta hai |
| `/actuator/httptraces` | Headers/tokens/request data expose ho sakta hai |

### Safe Exposure:

```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.endpoint.env.enabled=false
management.endpoint.beans.enabled=false
management.endpoint.heapdump.enabled=false
```

### Separate Management Port:

```properties
management.server.port=9090
management.server.address=127.0.0.1
```

Isse Actuator endpoints main app port se alag port pe run kar sakte hain.

### Spring Security:

```java
@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### Best Practices:

- Production mein `management.endpoints.web.exposure.include=*` avoid karo.
- Publicly sirf `health` expose karo, wo bhi details ke bina.
- `show-details=when-authorized` use karo.
- Actuator ko admin role, VPN, internal network ya separate port ke behind rakho.
- Heap dump, env, http traces jaise endpoints ko carefully enable karo.
- Logs aur metrics mein password, token, OTP, card number jaise sensitive data mat daalo.

**Short Interview Answer:**

Production mein Actuator ko least-privilege principle se expose karna chahiye: only required endpoints, proper authentication, no sensitive details, and preferably separate management port/internal network.
