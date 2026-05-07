# Spring Cloud LoadBalancer

## Status: Not Started

---

## Table of Contents

1. [Spring Cloud LoadBalancer Kya Hai?](#spring-cloud-loadbalancer-kya-hai)
2. [Why Client-Side Load Balancing?](#why-client-side-load-balancing)
3. [Architecture](#architecture)
4. [Setup](#setup)
5. [@LoadBalanced RestTemplate](#loadbalanced-resttemplate)
6. [@LoadBalanced WebClient](#loadbalanced-webclient)
7. [Integration with Feign](#integration-with-feign)
8. [Service Discovery Integration](#service-discovery-integration)
9. [Load Balancer Strategies](#load-balancer-strategies)
10. [Caching ServiceInstances](#caching-serviceinstances)
11. [Custom ServiceInstanceListSupplier](#custom-serviceinstancelistsupplier)
12. [Health Checks](#health-checks)
13. [Zone-Based / Hint-Based Routing](#zone-based--hint-based-routing)
14. [Retries](#retries)
15. [Common Pitfalls](#common-pitfalls)
16. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Spring Cloud LoadBalancer Kya Hai?

**Matlab:** **Client-side** load balancer — har service instance choose karta hai client itself (using a strategy), instead of server-side LB (nginx, AWS ALB).

> Successor to **Netflix Ribbon** (deprecated since Spring Cloud 2020).

### Built On

- **Reactor** (reactive) → `ReactorLoadBalancer<T>`
- Integrates with **Spring Cloud Discovery** (Eureka, Consul, Nacos, Kubernetes)
- Used internally by **OpenFeign**, `@LoadBalanced RestTemplate`, `@LoadBalanced WebClient`

### Modern Replacement

```
2020 →
Netflix Ribbon (deprecated)  →  Spring Cloud LoadBalancer
```

---

## Why Client-Side Load Balancing?

### Server-Side LB

```
Client → Load Balancer → ┬→ Service Instance 1
                         ├→ Service Instance 2
                         └→ Service Instance 3
```

✅ Simple
❌ Extra network hop
❌ LB itself is bottleneck / SPOF
❌ Service registry needs to push to LB

### Client-Side LB

```
                       ┌→ Service Instance 1
Client (LB logic) ────→├→ Service Instance 2
                       └→ Service Instance 3
```

✅ No extra hop
✅ Client knows all instances (via discovery)
✅ Per-client tuning (zone affinity, etc.)
❌ Each client needs LB logic
❌ Discovery client overhead

### When What?

| Scenario | LB Type |
|----------|---------|
| Microservices in K8s | K8s Service (server-side, simple) |
| Microservices with Eureka | Client-side (Spring Cloud LB) |
| Public traffic | nginx / cloud LB (server-side) |
| Service mesh (Istio, Linkerd) | Sidecar handles LB |

→ Modern trend: K8s + service mesh handle LB; Spring Cloud LB still relevant for non-K8s or fine-grained client logic.

→ Cross-ref: `Networking-&-Protocols/04-Load-Balancing-and-Proxies.md`.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│   Client Application                                         │
│   ┌──────────────────────────────────────────────────────┐   │
│   │  RestTemplate / WebClient / Feign                     │   │
│   └─────────────────┬─────────────────────────────────────┘   │
│                     │                                         │
│   ┌─────────────────▼─────────────────────────────────────┐   │
│   │ LoadBalancerInterceptor / LoadBalancerExchangeFilter   │   │
│   └─────────────────┬─────────────────────────────────────┘   │
│                     │                                         │
│   ┌─────────────────▼─────────────────────────────────────┐   │
│   │ BlockingLoadBalancerClient / ReactiveLoadBalancer     │   │
│   └─────────────────┬─────────────────────────────────────┘   │
│                     │                                         │
│   ┌─────────────────▼─────────────────────────────────────┐   │
│   │ LoadBalancer impl: RoundRobin / Random / etc.         │   │
│   └─────────────────┬─────────────────────────────────────┘   │
│                     │                                         │
│   ┌─────────────────▼─────────────────────────────────────┐   │
│   │ ServiceInstanceListSupplier (cached)                  │   │
│   └─────────────────┬─────────────────────────────────────┘   │
│                     │                                         │
│   ┌─────────────────▼─────────────────────────────────────┐   │
│   │ DiscoveryClient (Eureka / Consul / Nacos / K8s)       │   │
│   └────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### Request Flow

```
1. App calls: restTemplate.getForObject("http://order-service/orders/1", ...)
2. Interceptor catches → "order-service" needs resolution
3. LoadBalancer asks ServiceInstanceListSupplier for instances
4. Supplier returns cached list (from discovery)
5. Strategy (RoundRobin) picks one: 10.0.1.5:8080
6. URL rewritten: http://10.0.1.5:8080/orders/1
7. Real HTTP call
```

---

## Setup

### Maven Dependencies

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.cloud</groupId>
      <artifactId>spring-cloud-dependencies</artifactId>
      <version>2023.0.1</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
  
  <!-- Plus a discovery client -->
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
</dependencies>
```

> Note: Spring Cloud OpenFeign **transitively** brings LoadBalancer. Adding it explicitly is fine for clarity.

### Discovery Config

```yaml
spring:
  application:
    name: checkout-service

eureka:
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka
    register-with-eureka: true
    fetch-registry: true
```

---

## @LoadBalanced RestTemplate

### Configure

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### Use

```java
@Service
@RequiredArgsConstructor
public class CheckoutService {
    
    private final RestTemplate restTemplate;
    
    public Order getOrder(Long id) {
        return restTemplate.getForObject(
            "http://order-service/orders/" + id,    // logical name "order-service"
            Order.class);
    }
}
```

→ `http://order-service` resolved via discovery → instance picked → real call.

### Without `@LoadBalanced`

```java
@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();   // no LB
}

restTemplate.getForObject("http://order-service/...", Order.class);
// → DNS lookup of literal "order-service" — only works if it's a real DNS name
```

### Multiple RestTemplates (Mixed Usage)

```java
@Bean
@LoadBalanced
@Qualifier("internal")
public RestTemplate internalRestTemplate() { return new RestTemplate(); }

@Bean
@Qualifier("external")
public RestTemplate externalRestTemplate() { return new RestTemplate(); }
```

```java
@Service
public class CheckoutService {
    
    public CheckoutService(
        @Qualifier("internal") RestTemplate internal,
        @Qualifier("external") RestTemplate external) { ... }
}
```

→ Internal calls load-balanced; external calls bypass LB.

---

## @LoadBalanced WebClient

### Configure

```java
@Configuration
public class WebClientConfig {
    
    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }
}
```

### Use

```java
@Service
@RequiredArgsConstructor
public class CheckoutService {
    
    private final WebClient.Builder webClientBuilder;
    
    public Mono<Order> getOrder(Long id) {
        return webClientBuilder.build()
            .get()
            .uri("http://order-service/orders/{id}", id)
            .retrieve()
            .bodyToMono(Order.class);
    }
}
```

→ Reactive variant — non-blocking.

### Or Build Once with Base URL

```java
@Bean
@LoadBalanced
public WebClient orderWebClient(WebClient.Builder builder) {
    return builder
        .baseUrl("http://order-service")
        .build();
}
```

```java
public Mono<Order> getOrder(Long id) {
    return orderWebClient.get()
        .uri("/orders/{id}", id)
        .retrieve()
        .bodyToMono(Order.class);
}
```

⚠️ Note: combining `@LoadBalanced` with bound base URL — the URL host = service name; LB rewrites it.

---

## Integration with Feign

Feign uses LoadBalancer **transparently** when service discovery is configured.

```java
@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);
}
```

→ `name = "order-service"` resolves via discovery → LoadBalancer picks instance → call.

### Force `lb://`

```java
@FeignClient(name = "order-service", url = "lb://order-service")
```

Same effect.

### Disable for Specific Client

```java
@FeignClient(name = "order-service", url = "http://order-service:8081")   // direct URL bypasses LB
```

---

## Service Discovery Integration

LoadBalancer needs `ServiceInstanceListSupplier` which usually comes from a discovery client.

### Eureka

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka
```

→ Auto-wires Eureka-backed `DiscoveryClient`. Spring Cloud LoadBalancer picks it up.

### Consul

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    consul:
      host: consul
      port: 8500
```

### Nacos

```xml
<dependency>
  <groupId>com.alibaba.cloud</groupId>
  <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
```

### Kubernetes

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-kubernetes-client-loadbalancer</artifactId>
</dependency>
```

→ Lists pods directly via K8s API. Bypasses K8s Service.

→ Cross-ref: `Microservices Architecture/04-Service-Discovery.md`.

---

## Load Balancer Strategies

### 1. Round Robin (Default)

```
Request 1 → Instance A
Request 2 → Instance B
Request 3 → Instance C
Request 4 → Instance A
...
```

✅ Simple, even distribution
✅ Default

### 2. Random

```
Each request → random pick
```

✅ Stateless
❌ Less predictable distribution

### 3. Custom — RandomLoadBalancer

```java
@Configuration
public class CustomLoadBalancerConfig {
    
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment env,
            LoadBalancerClientFactory factory) {
        String name = env.getProperty(LoadBalancerClientFactory.PROPERTY_NAME);
        return new RandomLoadBalancer(
            factory.getLazyProvider(name, ServiceInstanceListSupplier.class),
            name);
    }
}
```

```java
@Configuration
@LoadBalancerClient(name = "order-service", configuration = CustomLoadBalancerConfig.class)
public class OrderServiceLoadBalancerConfig {}
```

→ Per-service LB strategy.

### 4. Weighted Response Time / Power-Of-Two-Choices

Not built-in, but can be implemented:
- Track latency per instance
- Pick "best two random" + faster of two
- → Reduces tail latency

### Multiple Strategies for Multiple Services

```java
@LoadBalancerClient(name = "order-service", configuration = RandomConfig.class)
@LoadBalancerClient(name = "payment-service", configuration = RoundRobinConfig.class)
public class LoadBalancerConfigs {}
```

### YAML — `health-check` & related

```yaml
spring:
  cloud:
    loadbalancer:
      configurations: health-check        # built-in profile
```

| Profile | Behavior |
|---------|----------|
| `default` | Standard round-robin, no health check |
| `zone-preference` | Prefer instances in same zone |
| `health-check` | Active health checking |

---

## Caching ServiceInstances

Hitting discovery server on **every** request is expensive. LoadBalancer caches instance list.

### Default Cache (Caffeine-Based)

```yaml
spring:
  cloud:
    loadbalancer:
      cache:
        enabled: true               # default true
        ttl: 35s                    # cache time-to-live
        capacity: 256               # max entries
```

→ Default supplier: `CachingServiceInstanceListSupplier` wraps the discovery-based one.

### Cache Hierarchy

```
ReactiveLoadBalancer
   ↓
CachingServiceInstanceListSupplier
   ↓
DiscoveryClientServiceInstanceListSupplier
   ↓
DiscoveryClient (Eureka/Consul/...)
```

### Disable Cache

```yaml
spring:
  cloud:
    loadbalancer:
      cache:
        enabled: false
```

→ Each request hits discovery. Avoid in production.

### Manually Refresh

```java
@Autowired
LoadBalancerClientFactory factory;

ServiceInstanceListSupplier supplier = factory
    .getLazyProvider("order-service", ServiceInstanceListSupplier.class)
    .getIfAvailable();

if (supplier instanceof CachingServiceInstanceListSupplier cached) {
    cached.refresh();
}
```

---

## Custom ServiceInstanceListSupplier

For advanced needs (filtering, hint-based routing, etc.).

### Example — Filter by Metadata

```java
public class MetadataFilteringSupplier implements ServiceInstanceListSupplier {
    
    private final ServiceInstanceListSupplier delegate;
    private final String requiredVersion;
    
    public MetadataFilteringSupplier(ServiceInstanceListSupplier delegate, String requiredVersion) {
        this.delegate = delegate;
        this.requiredVersion = requiredVersion;
    }
    
    @Override
    public String getServiceId() {
        return delegate.getServiceId();
    }
    
    @Override
    public Flux<List<ServiceInstance>> get() {
        return delegate.get()
            .map(instances -> instances.stream()
                .filter(i -> requiredVersion.equals(i.getMetadata().get("version")))
                .collect(Collectors.toList()));
    }
}
```

### Register

```java
@Configuration
public class CustomSupplierConfig {
    
    @Bean
    public ServiceInstanceListSupplier discoveryClientServiceInstanceListSupplier(
            ConfigurableApplicationContext context) {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()
            .withCaching()
            .withSameInstancePreference()
            .build(context);
    }
}
```

### Built-In Wrappers

```java
ServiceInstanceListSupplier.builder()
    .withDiscoveryClient()              // base — read from discovery
    .withCaching()                       // wrap with caching
    .withHealthChecks()                  // active health probes
    .withZonePreference()                // prefer same zone
    .withSameInstancePreference()        // sticky to same instance per call (within retry)
    .withRequestBasedStickySession()     // session affinity via cookie/header
    .withHints()                         // hint-based filtering
    .build(context);
```

---

## Health Checks

Discovery says instance is up, but actual instance might be hung. Active health check catches it.

### Enable

```yaml
spring:
  cloud:
    loadbalancer:
      configurations: health-check
      health-check:
        initial-delay: 0s
        interval: 25s
        path:
          default: /actuator/health
        port: ${server.port}
```

### Behavior

```
Every 25s, LoadBalancer pings each instance's /actuator/health.
Instances responding 200 → in pool.
Instances failing → removed temporarily.
```

### Per-Service Health Path

```yaml
spring:
  cloud:
    loadbalancer:
      health-check:
        path:
          order-service: /health
          payment-service: /actuator/health
        port: 8080
```

### Health Check via Code

```java
@Bean
public ServiceInstanceListSupplier listSupplier(ConfigurableApplicationContext context) {
    return ServiceInstanceListSupplier.builder()
        .withDiscoveryClient()
        .withHealthChecks()
        .withCaching()
        .build(context);
}
```

---

## Zone-Based / Hint-Based Routing

### Zone Preference

For multi-AZ deployments — prefer instances in same zone (cheaper bandwidth, lower latency).

```yaml
spring:
  cloud:
    loadbalancer:
      configurations: zone-preference

eureka:
  instance:
    metadata-map:
      zone: ap-south-1a            # tag instance with zone
```

→ When instances tagged, LoadBalancer prefers same-zone.

### Hint-Based Routing

```yaml
spring:
  cloud:
    loadbalancer:
      hint:
        order-service: experimental
        default: stable
      hint-header-name: X-LB-Hint
```

```java
ServiceInstanceListSupplier.builder()
    .withDiscoveryClient()
    .withCaching()
    .withHints()
    .build(context);
```

→ Instances with metadata `hint=experimental` get traffic when `X-LB-Hint: experimental` header present.

### Same-Instance Preference (Sticky)

For multi-step flows — keep same instance:

```java
.withSameInstancePreference()
```

→ Within a single request chain (with retry), same instance picked.

### Request-Based Sticky Session

```java
.withRequestBasedStickySession()
```

→ Hash on session cookie → same instance for same user.

---

## Retries

Spring Cloud LoadBalancer **integrates** with Spring Retry — retry on different instance.

### Setup

```xml
<dependency>
  <groupId>org.springframework.retry</groupId>
  <artifactId>spring-retry</artifactId>
</dependency>
```

### Config

```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true
        max-retries-on-same-service-instance: 0   # don't retry same instance
        max-retries-on-next-service-instance: 1   # try next instance once
        retry-on-all-operations: false            # only on idempotent (GET)
        retryable-status-codes:
          - 502
          - 503
          - 504
```

### How It Works

```
Request → Instance A → 503 → 
LoadBalancer says "different instance" → 
Request → Instance B → 200 ✓
```

### With RestTemplate

```java
@Bean
@LoadBalanced
public RestTemplate restTemplate() {
    return new RestTemplate();
}
```

→ With `spring-retry` on classpath + config above, retries auto-applied.

⚠️ Be careful with **POST** — retries can duplicate. `retry-on-all-operations: false` keeps it safer.

### With Feign

Use Resilience4j Retry instead (covered in `03-OpenFeign.md`).

---

## Common Pitfalls

### 1. Forgetting `@LoadBalanced`

```java
@Bean
public RestTemplate restTemplate() {        // ❌ no LB!
    return new RestTemplate();
}
```

→ `http://order-service/...` does literal DNS lookup. Fails or hits wrong target.

### 2. Mixed Service Names + Hard URLs

```java
restTemplate.getForObject("http://order-service:8080/orders/" + id, ...)
```

→ Port `:8080` baked in. Discovery returns instance with port `:8081` → mismatch. Don't include port; LB sets it.

### 3. No Discovery Client = No Instances

LoadBalancer needs `DiscoveryClient` (Eureka/Consul/etc.) or manual `ServiceInstanceListSupplier`. Without either:

```
No instances available for order-service
```

### 4. Stale Cache Issue

Default 35s cache → if instance dies, requests routed to dead one for up to 35s.

→ Combine with health checks + circuit breaker for robust handling.

### 5. Health Endpoint Slow

If `/actuator/health` does heavy DB checks, health probe blocks. Use **lightweight** health (or separate `/actuator/health/liveness` and `/actuator/health/readiness`).

### 6. Eureka Lag

Eureka has 30s update propagation by default. Instance dies → LB still has it for ~30-90s.

```yaml
eureka:
  instance:
    lease-renewal-interval-in-seconds: 5      # heartbeat every 5s
    lease-expiration-duration-in-seconds: 10  # mark down after 10s
```

⚠️ Aggressive timing → load on Eureka. Tune carefully.

### 7. Round-Robin + Sticky State

Round-robin distributes evenly but if you have local state (ill-advised in microservices), users hit different instances → broken UX.

→ Either go stateless or use sticky session.

### 8. Failing Open

If discovery returns **empty list** + LB falls back to `http://order-service` literal → DNS lookup → unexpected behavior.

→ Always have fallback / circuit breaker.

### 9. Test Setup Without Discovery

```java
@SpringBootTest
class CheckoutServiceTest {
    // No Eureka in tests → restTemplate calls fail
}
```

→ Use static URL in tests:

```yaml
# application-test.yml
spring:
  cloud:
    discovery:
      enabled: false
order-service:
  url: http://localhost:${wiremock.port}
```

### 10. WebClient Builder Reuse

```java
@Bean
@LoadBalanced
public WebClient.Builder builder() { return WebClient.builder(); }

// Inject and call .build() each time → ❌ creates new WebClient (waste)
WebClient wc = builder.build();   // do once, reuse
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Spring Cloud LoadBalancer** | Client-side LB, replaces Ribbon |
| **`@LoadBalanced`** | Enables LB on RestTemplate / WebClient.Builder |
| **`http://service-name/...`** | Logical URL → LB resolves |
| **Default strategy** | Round-Robin |
| **Custom strategy** | `@LoadBalancerClient(configuration=...)` |
| **Cache** | Caffeine-based, TTL 35s default |
| **Discovery integration** | Eureka, Consul, Nacos, K8s |
| **Feign** | Uses LoadBalancer transparently |
| **Health checks** | Active probes via `/actuator/health` |
| **Zone preference** | Same-zone instances favored |
| **Hints** | Header-based routing |
| **Retries** | Spring Retry integration |

| Strategy | When |
|----------|------|
| **RoundRobin** (default) | General use |
| **Random** | Simple, stateless |
| **ZonePreference** | Multi-AZ for cost/latency |
| **SameInstance** | Multi-step flows |
| **RequestBasedSticky** | Session affinity |
| **HealthCheck** | Detect hung instances |

| Discovery | Setup |
|-----------|-------|
| **Eureka** | `spring-cloud-starter-netflix-eureka-client` |
| **Consul** | `spring-cloud-starter-consul-discovery` |
| **Nacos** | `spring-cloud-starter-alibaba-nacos-discovery` |
| **Kubernetes** | `spring-cloud-starter-kubernetes-client-loadbalancer` |

---

## Practice

1. Set up Eureka + register two instances of `order-service`.
2. Configure a `@LoadBalanced RestTemplate` and call `http://order-service/...` 10 times — observe round-robin distribution.
3. Switch from RoundRobin to Random for one service via `@LoadBalancerClient`.
4. Add `withHealthChecks()` — kill one instance and verify it's removed.
5. Tag instances with `zone` metadata; configure `zone-preference`; verify same-zone preferred.
6. Configure cache TTL to 60s; observe with logs.
7. Configure retry for `503` to next instance.
8. Compare RestTemplate, WebClient, Feign — all using LoadBalancer.
9. Implement custom `ServiceInstanceListSupplier` filtering by metadata `version=v2`.
10. Disable LB for one specific RestTemplate (mixed usage with `@Qualifier`).
