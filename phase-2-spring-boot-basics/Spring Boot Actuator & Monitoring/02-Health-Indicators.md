# Health Indicators

## Status: Not Started

---

## Table of Contents

1. [HealthIndicator Interface](#healthindicator-interface)
2. [CompositeHealthContributor](#compositehealthcontributor)
3. [Custom Health Checks](#custom-health-checks)
4. [Health Groups](#health-groups)
5. [Liveness vs Readiness Probes](#liveness-vs-readiness-probes)

---

## HealthIndicator Interface

**Matlab:** Custom health check banana — database, external service, disk space, etc.

### Interface

```java
public interface HealthIndicator {
    Health health();
}
```

### Basic Custom Health Indicator

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(1000)) {
                return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("connection", "Valid")
                    .build();
            } else {
                return Health.down()
                    .withDetail("database", "MySQL")
                    .withDetail("connection", "Invalid")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "MySQL")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Using AbstractHealthIndicator (Recommended)

```java
@Component
public class ExternalApiHealthIndicator extends AbstractHealthIndicator {

    private final RestTemplate restTemplate;

    public ExternalApiHealthIndicator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                "https://api.example.com/health", String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                builder.up()
                    .withDetail("api", "External API")
                    .withDetail("status", response.getStatusCode())
                    .withDetail("responseTime", "50ms");
            } else {
                builder.down()
                    .withDetail("api", "External API")
                    .withDetail("status", response.getStatusCode());
            }
        } catch (Exception e) {
            builder.down()
                .withDetail("api", "External API")
                .withDetail("error", e.getMessage());
        }
    }
}
```

### Health Response

```
GET /actuator/health

{
  "status": "UP",
  "components": {
    "externalApi": {
      "status": "UP",
      "details": {
        "api": "External API",
        "status": "200 OK",
        "responseTime": "50ms"
      }
    },
    "database": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "connection": "Valid"
      }
    }
  }
}
```

### Status Aggregation

```java
// Multiple conditions — overall status kaise decide hoga
builder.up()       // UP
builder.down()     // DOWN
builder.outOfService()  // OUT_OF_SERVICE
builder.unknown()  // UNKNOWN
```

**Default Priority:** DOWN > OUT_OF_SERVICE > UP > UNKNOWN

---

## CompositeHealthContributor

**Matlab:** Multiple health indicators ko ek saath group karna — ek hi endpoint se sab check ho.

### Usage

```java
@Component
public class SystemHealthContributor implements CompositeHealthContributor {

    private final Map<String, HealthContributor> contributors = new LinkedHashMap<>();

    public SystemHealthContributor(
            DatabaseHealthIndicator dbHealth,
            DiskSpaceHealthIndicator diskHealth,
            ExternalApiHealthIndicator apiHealth) {

        contributors.put("database", dbHealth);
        contributors.put("diskSpace", diskHealth);
        contributors.put("externalApi", apiHealth);
    }

    @Override
    public HealthContributor getContributor(String name) {
        return contributors.get(name);
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return contributors.entrySet().stream()
            .map(entry -> NamedContributor.of(entry.getKey(), entry.getValue()))
            .iterator();
    }
}
```

### Spring Boot Default Behavior

Spring Boot automatically sab `HealthIndicator` beans ko collect karke composite health response banata hai — manually implement karne ki zarurat nahi.

```java
// Yeh sab automatically /actuator/health mein aa jayenge
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator { }

@Component
public class ExternalApiHealthIndicator extends AbstractHealthIndicator { }

@Component
public class CacheHealthIndicator extends AbstractHealthIndicator { }
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "database": { "status": "UP" },
    "externalApi": { "status": "UP" },
    "cache": { "status": "DOWN" }
  }
}
```

---

## Custom Health Checks

### Database Health Check

```java
@Component
public class CustomDatabaseHealthIndicator extends AbstractHealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public CustomDatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            // Simple query
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users", Integer.class
            );

            builder.up()
                .withDetail("database", "MySQL")
                .withDetail("userCount", count);

        } catch (Exception e) {
            builder.down()
                .withDetail("database", "MySQL")
                .withException(e);
        }
    }
}
```

### External Service Health Check

```java
@Component
public class PaymentGatewayHealthIndicator extends AbstractHealthIndicator {

    private final RestTemplate restTemplate;

    @Value("${payment.gateway.url}")
    private String paymentGatewayUrl;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                paymentGatewayUrl + "/ping", Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                builder.up()
                    .withDetail("paymentGateway", "Online")
                    .withDetail("responseTime", response.getHeaders().get("X-Response-Time"));
            } else {
                builder.down()
                    .withDetail("paymentGateway", "Offline")
                    .withDetail("statusCode", response.getStatusCode());
            }
        } catch (Exception e) {
            builder.down()
                .withDetail("paymentGateway", "Unreachable")
                .withDetail("error", e.getMessage());
        }
    }
}
```

### Redis Health Check

```java
@Component
public class RedisHealthIndicator extends AbstractHealthIndicator {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            if ("PONG".equals(pong)) {
                builder.up()
                    .withDetail("redis", "Connected")
                    .withDetail("ping", pong);
            } else {
                builder.down()
                    .withDetail("redis", "Unexpected response")
                    .withDetail("ping", pong);
            }
        } catch (Exception e) {
            builder.down()
                .withDetail("redis", "Connection failed")
                .withException(e);
        }
    }
}
```

### Disk Space Health Check (Built-in)

```properties
# Default: 10MB threshold
management.health.diskspace.threshold=10000000

# Custom threshold
management.health.diskspace.threshold=100000000  # 100MB

# Path to check
management.health.diskspace.path=/data
```

### Disable Built-in Health Indicators

```properties
# Database health disable karo
management.health.db.enabled=false

# Disk space health disable
management.health.diskspace.enabled=false

# Redis health disable
management.health.redis.enabled=false

# Mail health disable
management.health.mail.enabled=false
```

---

## Health Groups

**Matlab:** Health checks ko logical groups mein organize karna — alag-alag endpoints se different groups check karo.

### Configuration

```properties
# Health group define karo
management.endpoint.health.group.readiness.include=readinessState,db,redis
management.endpoint.health.group.liveness.include=livenessState

# Custom group
management.endpoint.health.group.infra.include=db,redis,diskSpace
management.endpoint.health.group.external.include=paymentGateway,mailService
```

### Response

```
GET /actuator/health/readiness

{
  "status": "UP",
  "components": {
    "readinessState": { "status": "UP" },
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}

GET /actuator/health/liveness

{
  "status": "UP",
  "components": {
    "livenessState": { "status": "UP" }
  }
}
```

### Group with Show Details

```properties
management.endpoint.health.group.infra.show-details=always
management.endpoint.health.group.external.show-details=when-authorized
```

---

## Liveness vs Readiness Probes

**Matlab:** Kubernetes health checks — application ko manage karne ke liye.

### Liveness Probe

**Matlab:** Application alive hai ya crash ho gayi?

```
Liveness check karta hai:
  → Application process chal raha hai?
  → Deadlock toh nahi hai?
  → Memory leak se crash toh nahi hua?

Agar liveness fail → Kubernetes pod ko restart karta hai
```

### Readiness Probe

**Matlab:** Application traffic accept karne ke liye ready hai?

```
Readiness check karta hai:
  → Database connected hai?
  → External services available hain?
  → Cache warm ho gaya?

Agar readiness fail → Kubernetes traffic bhejna band kar deta hai (but restart nahi karta)
```

### Configuration

```properties
# Liveness state expose karo
management.endpoint.health.probes.enabled=true

# Liveness probe
management.endpoint.health.group.liveness.include=livenessState

# Readiness probe
management.endpoint.health.group.readiness.include=readinessState,db,redis
```

### Spring Boot Auto-Configuration (Kubernetes Detection)

Spring Boot automatically Kubernetes environment detect karke liveness/readiness endpoints enable kar deta hai.

```java
// Auto-configured endpoints
/actuator/health/liveness   → Liveness probe
/actuator/health/readiness  → Readiness probe
```

### Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: my-app
  template:
    metadata:
      labels:
        app: my-app
    spec:
      containers:
        - name: my-app
          image: my-app:latest
          ports:
            - containerPort: 8080
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
```

### Liveness State Transitions

```
Application start
    ↓
Liveness: CORRECT (alive)
    ↓
If deadlock/memory leak → INCORRECT
    ↓
Kubernetes restarts pod
```

### Readiness State Transitions

```
Application start
    ↓
Readiness: REFUSING (not ready yet)
    ↓
DB connected + Cache warm → ACCEPTING
    ↓
Kubernetes starts sending traffic
    ↓
If DB disconnects → REFUSING
    ↓
Kubernetes stops sending traffic (but doesn't restart)
```

### Custom Readiness Check

```java
@Component
public class DatabaseReadinessState implements HealthIndicator {

    private final DataSource dataSource;

    public DatabaseReadinessState(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1000)) {
                return Health.up()
                    .withDetail("database", "Connected")
                    .build();
            }
            return Health.down()
                .withDetail("database", "Connection invalid")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("database", "Connection failed")
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

### Liveness vs Readiness Comparison

| Feature | Liveness | Readiness |
|---------|----------|-----------|
| **Question** | Application alive hai? | Traffic accept karne ke liye ready hai? |
| **Fail Action** | Pod restart | Traffic stop (no restart) |
| **Check** | Process running, no deadlock | DB connected, services available |
| **Frequency** | Less frequent (10s) | More frequent (5s) |
| **Timeout** | Longer (5s) | Shorter (3s) |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **HealthIndicator** | Custom health check banana — `health()` method implement karo |
| **AbstractHealthIndicator** | Recommended — `doHealthCheck()` override karo |
| **CompositeHealthContributor** | Multiple indicators ko group karo |
| **Custom Health Checks** | DB, external API, Redis, disk space — sabke liye custom checks |
| **Health Groups** | Health checks ko logical groups mein organize karo |
| **Liveness Probe** | Application alive hai — fail → restart |
| **Readiness Probe** | Application ready hai — fail → traffic stop |
| **Kubernetes** | `livenessProbe` aur `readinessProbe` endpoints use karta hai |
