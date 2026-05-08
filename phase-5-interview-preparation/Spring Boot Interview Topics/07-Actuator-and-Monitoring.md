# Actuator & Monitoring

## Status: Not Started

---

## Table of Contents

1. [Actuator Kya Hai?](#actuator-kya-hai)
2. [Common Endpoints](#common-endpoints)
3. [Production-Safe Exposure](#production-safe-exposure)
4. [Separate Management Port](#separate-management-port)
5. [Custom `HealthIndicator`](#custom-healthindicator)
6. [Liveness vs Readiness](#liveness-vs-readiness)
7. [Custom Metrics with Micrometer](#custom-metrics-with-micrometer)
8. [`@Timed` and `@Counted`](#timed-and-counted)
9. [`info` Contributors](#info-contributors)
10. [Graceful Shutdown](#graceful-shutdown)
11. [Securing Actuator](#securing-actuator)
12. [Common Output Traps](#common-output-traps)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Actuator Kya Hai?

**Spring Boot Actuator** = production-ready endpoints for **monitoring + ops**:

- Health checks
- Metrics (Micrometer)
- Env / config dump
- Loggers control
- Thread dump
- HTTP request traces

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

---

## Common Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Liveness/readiness aggregate |
| `/actuator/info` | App info (version, build) |
| `/actuator/metrics` | Metrics list + drill |
| `/actuator/prometheus` | Prometheus scrape |
| `/actuator/env` | Environment props |
| `/actuator/configprops` | `@ConfigurationProperties` dump |
| `/actuator/beans` | Bean graph |
| `/actuator/conditions` | Auto-config report |
| `/actuator/loggers` | Read/change log levels at runtime |
| `/actuator/threaddump` | Thread dump |
| `/actuator/heapdump` | Heap dump (binary) |
| `/actuator/mappings` | URL mappings |
| `/actuator/httptrace` | (Removed in Boot 3 — use `httpexchanges`) |

---

## Production-Safe Exposure

### Default

Boot 2+ exposes only `/health` + `/info` over HTTP by default.

### Recommended prod config

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
        # exclude: env, beans, configprops    # if not needed
  endpoint:
    health:
      show-details: when_authorized      # never expose internals to anonymous
      probes:
        enabled: true
```

→ `env`, `beans`, `configprops`, `heapdump` — sensitive; expose only inside private network or via authentication.

---

## Separate Management Port

```yaml
management:
  server:
    port: 9090         # different port from app (8080)
    address: 127.0.0.1 # bind to localhost / private interface
```

→ Public LB exposes 8080 only. Operators connect to 9090 via private channel.

→ Bonus: K8s sidecar / Prometheus scrape on 9090 — public 8080 unaffected.

---

## Custom `HealthIndicator`

```java
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin admin;

    @Override
    public Health health() {
        try (AdminClient ac = AdminClient.create(admin.getConfigurationProperties())) {
            ac.listTopics().names().get(2, TimeUnit.SECONDS);
            return Health.up().build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
```

→ Auto-included in `/actuator/health`.

### `HealthContributor` (Boot 2.2+) — group multiple

For composite checks (e.g., multiple downstreams).

### `ReactiveHealthIndicator`

WebFlux apps.

---

## Liveness vs Readiness

| | Liveness | Readiness |
|--|----------|----------|
| Question | "Process zinda hai?" | "Traffic accept kar sakta hai?" |
| Failure → | K8s restart container | K8s stop sending traffic |
| Causes | Deadlock, OOM | DB down, warming up, in-flight requests draining |

### Boot 2.3+ probes

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
```

Endpoints:

- `/actuator/health/liveness`
- `/actuator/health/readiness`

### K8s probes

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
```

### Programmatic state changes

```java
@EventListener(ApplicationReadyEvent.class)
public void ready() { ... }

@Autowired ApplicationAvailability availability;
availability.getReadinessState();   // ACCEPTING_TRAFFIC / REFUSING_TRAFFIC
```

```java
applicationContext.publishEvent(new AvailabilityChangeEvent<>(this, ReadinessState.REFUSING_TRAFFIC));
```

→ Use during shutdown (preStop hook) — readiness false → LB stops sending → graceful drain.

---

## Custom Metrics with Micrometer

```java
@Component
@RequiredArgsConstructor
public class OrderMetrics {

    private final MeterRegistry registry;

    public void recordOrder(double amount) {
        registry.counter("orders.placed", "currency", "INR").increment();
        registry.summary("orders.amount").record(amount);
    }
}
```

### Types

| Meter | Use |
|-------|-----|
| `Counter` | Monotonic count |
| `Timer` | Duration + count |
| `Gauge` | Instantaneous value (queue size) |
| `DistributionSummary` | Histogram of values |
| `LongTaskTimer` | In-flight long tasks |

### Timer

```java
Timer.Sample s = Timer.start(registry);
process();
s.stop(registry.timer("processing.time", "type", "order"));
```

### Gauge

```java
registry.gauge("queue.size", queue, Queue::size);
```

→ Note: registry holds **weak** reference — keep `queue` alive.

### Tags / labels best practice

- Low cardinality (status, region) ✅
- Avoid user_id, request_id (cardinality explosion) ❌

---

## `@Timed` and `@Counted`

```java
@Service
public class OrderService {

    @Timed(value = "orders.create", description = "Order creation time", percentiles = { 0.95, 0.99 })
    public Order create(...) { ... }
}
```

Need:

```java
@Bean
public TimedAspect timedAspect(MeterRegistry registry) {
    return new TimedAspect(registry);
}
```

→ Convenient annotation-based timing. Same proxy rules as `@Transactional`.

---

## `info` Contributors

```yaml
management:
  info:
    env:
      enabled: true
    java:
      enabled: true
    git:
      mode: full
    build:
      enabled: true

info:
  app:
    name: ${spring.application.name}
    version: @project.version@
    description: Order microservice
```

`/actuator/info`:

```json
{
  "app": { "name": "orders", "version": "1.2.3" },
  "git": { "branch": "main", "commit": { "id": "abc123" } },
  "build": { "artifact": "orders", "time": "2024-05-08" }
}
```

→ Useful for verifying deployed version.

---

## Graceful Shutdown

```yaml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

### Flow

```
1. SIGTERM received
2. Spring stops accepting new requests
3. In-flight requests get 30s to finish
4. Beans destroyed in reverse order
5. JVM exits
```

### K8s integration

```yaml
terminationGracePeriodSeconds: 60
lifecycle:
  preStop:
    exec:
      command: ["sh", "-c", "sleep 5"]   # let LB drop pod from rotation first
```

### Combine with readiness

```java
@PreDestroy
public void preDestroy() {
    publisher.publishEvent(new AvailabilityChangeEvent<>(this, ReadinessState.REFUSING_TRAFFIC));
}
```

→ Cross-ref: `phase-4 / Docker & Containerization / 04-Container-Best-Practices.md`.

---

## Securing Actuator

```java
@Bean
public SecurityFilterChain actuator(HttpSecurity http) throws Exception {
    return http
        .securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
            .anyRequest().hasRole("ACTUATOR"))
        .httpBasic(Customizer.withDefaults())
        .build();
}
```

### Or: separate management port + bind localhost (no auth needed externally).

---

## Common Output Traps

### Q1. `/actuator/health` returns UP but DB is down

→ Default health indicators include `db`; check that auto-config didn't get excluded. `show-details: always` to see breakdown.

### Q2. Custom HealthIndicator not picked up

→ Must be `@Component`; check name (suffix `HealthIndicator` becomes part of health key).

### Q3. `@Timed` no metrics

→ Forgot `TimedAspect` bean.

### Q4. Prometheus scrape empty

→ Forgot `micrometer-registry-prometheus` dependency or `prometheus` not in exposure.

### Q5. Endpoint exposes secrets

```yaml
exposure.include: '*'        # ❌ exposes env, beans, etc.
```

---

## Pitfalls

1. **`include: '*'` in production** — leaks env vars / beans.
2. **No auth on actuator** — sensitive data internet-exposed.
3. **Cardinality explosion** in metrics tags (user_id) → memory blow-up.
4. **`@Timed` on private method** — proxy doesn't apply.
5. **Liveness pings DB** — DB hiccup → restart loop. Liveness should be **app-internal** only.
6. **Readiness ignored on K8s** — pod gets traffic before warmup.
7. **No graceful shutdown** — in-flight requests dropped.
8. **`heapdump` exposed** — leaks heap data; never for anonymous.
9. **`httptrace` removed in Boot 3** — use `httpexchanges`.
10. **Missing `info.git`** — ops can't verify version deployed.

---

## Cheat Sheet

| Topic | Quick |
|-------|-------|
| Default exposed | health, info |
| Sensitive endpoints | env, beans, configprops, heapdump |
| Prod expose | health, info, metrics, prometheus |
| Mgmt port | `management.server.port` |
| Liveness | App alive (no DB!) |
| Readiness | Traffic OK |
| Graceful shutdown | `server.shutdown=graceful` + lifecycle timeout |
| Custom health | `@Component` + `HealthIndicator` |
| Custom metric | `MeterRegistry` injection |

| Probe | Failure ⇒ |
|-------|-----------|
| Liveness | Restart container |
| Readiness | No traffic |

---

## Practice

1. Add `spring-boot-starter-actuator`; expose health + prometheus.
2. Custom `HealthIndicator` for downstream API; simulate failure.
3. Liveness vs readiness — toggle readiness to `REFUSING_TRAFFIC`; verify endpoint.
4. Add `MeterRegistry` Counter + Timer; scrape Prometheus; visualize in Grafana.
5. Enable graceful shutdown; load test + send SIGTERM mid-request; verify completion.
6. Secure `/actuator/env` with role; verify denied for anonymous.
