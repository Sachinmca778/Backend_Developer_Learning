# Spring Cloud - Top 10 Interview Questions & Answers

> Covering: Spring Cloud Config, Config refresh (@RefreshScope), Spring Cloud Gateway, OpenFeign, Spring Cloud LoadBalancer, Service Discovery, and Spring Cloud Bus

---

## Q1: Spring Cloud kya hai aur kaunse problems solve karta hai?

**Answer:**

Spring Cloud distributed systems (microservices) ke common patterns ke liye tools ka collection hai — Spring Boot ke upar bana.

### Main Components:

| Component | Purpose |
|-----------|---------|
| **Config Server** | Centralized configuration |
| **Gateway** | API Gateway (routing, filters) |
| **OpenFeign** | Declarative REST client |
| **LoadBalancer** | Client-side load balancing |
| **Eureka/Consul** | Service Discovery |
| **Spring Cloud Bus** | Config broadcast refresh |
| **Sleuth/Micrometer** | Distributed tracing |
| **Resilience4j** | Circuit breaker, retry |

### Distributed System Problems Solved:

```
Config management → Config Server
Service-to-service calls → OpenFeign + LoadBalancer
Finding services → Service Discovery
Single entry point → Gateway
Fault tolerance → Resilience4j
Observability → Sleuth/Micrometer + Zipkin
```

**Short Interview Answer:**

Spring Cloud microservices ke distributed system challenges solve karta hai — centralized config (Config Server), API Gateway, declarative HTTP clients (OpenFeign), service discovery (Eureka), client-side load balancing, circuit breakers (Resilience4j), aur distributed tracing. Ye Spring Boot par bana ek toolkit hai.

---

## Q2: Spring Cloud Config Server kya hai aur kaise kaam karta hai?

**Answer:**

Config Server distributed system ke liye **centralized external configuration** deta hai — sab services ki config ek single source of truth (Git, Vault, DB) mein.

### Problem Without Config Server:

```
❌ Har service mein config duplicate
❌ Config change karne ke liye sab redeploy
❌ Secrets har jagah copy
```

### Architecture:

```
              ┌──────────────────┐
              │  Config Server   │
              │  ┌────────────┐  │
              │  │ Git Repo   │  │ ← single source of truth
              │  └────────────┘  │
              └────────┬─────────┘
        ┌──────────────┼──────────────┐
   ┌────▼────┐   ┌─────▼─────┐   ┌────▼────┐
   │Service A│   │ Service B │   │Service C│
   └─────────┘   └───────────┘   └─────────┘
```

### Config Server Setup:

```java
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication { }
```

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/myorg/config-repo
```

### Client Setup:

```yaml
spring:
  config:
    import: optional:configserver:http://localhost:8888
  application:
    name: order-service   # → order-service.yml fetch hoti hai
```

**Important:**

Config Server config ko ek jagah rakhta hai (Git = version control + audit), refresh without redeploy allow karta hai, aur encryption built-in hai. Client `spring.application.name` ke basis par apni config fetch karta hai. Profiles (`order-service-prod.yml`) se environment-specific config milti hai.

---

## Q3: @RefreshScope kya hai aur runtime config refresh kaise karte hain?

**Answer:**

Normally config startup par load hoti hai. `@RefreshScope` beans ko runtime par config refresh karne deta hai bina restart ke.

### Usage:

```java
@RefreshScope
@RestController
public class FeatureController {
    @Value("${feature.enabled}")
    private boolean featureEnabled;   // refresh par update ho jaayega
}
```

### Trigger Refresh:

```bash
# Actuator refresh endpoint
POST http://localhost:8080/actuator/refresh
```

→ Ye sirf **us instance** ki config refresh karta hai.

### Multiple Instances — Spring Cloud Bus:

Har instance ko manually refresh karna impractical hai. Spring Cloud Bus (Kafka/RabbitMQ) se ek refresh event saare instances ko broadcast hota hai.

```bash
POST http://localhost:8080/actuator/busrefresh
```

```
Config change in Git → /busrefresh → Bus (Kafka/RabbitMQ) → all instances refresh
```

**Short Interview Answer:**

`@RefreshScope` beans ko runtime par config change pick karne deta hai bina restart ke — `POST /actuator/refresh` se trigger hota hai. Ek hi instance refresh hota hai. Saare instances ek saath refresh karne ke liye Spring Cloud Bus (Kafka/RabbitMQ) use karo jo `/busrefresh` event broadcast karta hai.

---

## Q4: Spring Cloud Gateway kya hai aur Zuul se kaise better hai?

**Answer:**

Spring Cloud Gateway modern API Gateway hai jo **Spring WebFlux + Reactor + Netty** (reactive, non-blocking) par bana hai. Zuul 1 ka successor (jo blocking tha).

### Key Concepts:

| Concept | Meaning |
|---------|---------|
| **Route** | URI + Predicates + Filters → forward target |
| **Predicate** | Kab route match kare (path, header, method) |
| **Filter** | Request/response transform karna |

### Route Configuration:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service       # load-balanced via discovery
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
            - name: CircuitBreaker
              args:
                name: orderCB
                fallbackUri: forward:/fallback
```

### Why Reactive?

- **High concurrency** — ek thread hazaaron slow connections serve kare
- **Backpressure** — overload protection
- **Non-blocking I/O** — better resource usage

**Important:**

Spring Cloud Gateway reactive (non-blocking) hai — high concurrency handle karta hai kam threads mein. Zuul 1 blocking (thread-per-request) tha. Gateway Routes = Predicates (kab match) + Filters (transform). Cross-cutting concerns (auth, rate limiting, circuit breaker) filters ke through centralize hote hain. Business logic gateway mein mat daalo.

---

## Q5: OpenFeign kya hai aur RestTemplate se kaise better hai?

**Answer:**

OpenFeign ek **declarative HTTP client** hai — REST API ke liye interface likho, Feign proxy banata hai jo actual HTTP calls karta hai.

### Feign Client:

```java
@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);

    @PostMapping("/orders")
    Order createOrder(@RequestBody CreateOrderRequest request);
}
```

### Usage — Just Inject:

```java
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final OrderClient orderClient;

    public void process(Long id) {
        Order order = orderClient.getOrder(id);  // HTTP call, but looks local
    }
}
```

### Feign vs RestTemplate:

| | RestTemplate | OpenFeign |
|--|-------------|-----------|
| Style | Imperative (manual) | Declarative (interface) |
| Boilerplate | High (URL, parsing) | Minimal |
| Load balancing | Manual `@LoadBalanced` | Built-in |
| Discovery | Manual | Automatic (logical name) |

### Enable:

```java
@EnableFeignClients
@SpringBootApplication
public class Application { }
```

**Short Interview Answer:**

OpenFeign declarative REST client hai — interface likho annotations ke saath, Feign implementation generate karta hai. RestTemplate ke manual URL building, JSON parsing, error handling nahi karna padta. Service discovery aur load balancing built-in hai (logical service name se). REST call ko local method call jaisa banata hai.

---

## Q6: Spring Cloud LoadBalancer kya hai? Client-side vs server-side load balancing?

**Answer:**

Spring Cloud LoadBalancer **client-side** load balancer hai — client khud decide karta hai kaunse service instance ko call kare (strategy use karke). Netflix Ribbon ka successor.

### Client-Side vs Server-Side:

| | Client-Side (Spring Cloud LB) | Server-Side (nginx, ALB) |
|--|------------------------------|--------------------------|
| Decision | Client instance choose karta | Dedicated LB choose karta |
| Extra hop | Nahi | Haan (LB → service) |
| Discovery | Client registry query karta | LB registry query karta |

### How It Works:

```
Client → Discovery (Eureka) → gets instances [A, B, C]
       → LoadBalancer picks one (round-robin) → calls directly
```

### Usage:

```java
@Bean
@LoadBalanced   // enable load balancing
public RestTemplate restTemplate() {
    return new RestTemplate();
}

// logical name use karo, IP nahi
restTemplate.getForObject("http://order-service/orders/1", Order.class);
```

→ OpenFeign, `@LoadBalanced RestTemplate/WebClient` internally isi ko use karte hain.

### Strategies:

- **Round Robin** (default)
- **Random**
- Custom `ReactorServiceInstanceLoadBalancer`

**Important:**

Client-side load balancing mein client hi instance choose karta hai (discovery se list leke), server-side mein dedicated LB. Client-side extra network hop avoid karta hai. Spring Cloud LoadBalancer (Ribbon replacement) OpenFeign aur `@LoadBalanced` clients ke andar built-in hai. `lb://service-name` URI Gateway mein bhi isi ko trigger karta hai.

---

## Q7: Service Discovery Spring Cloud mein kaise kaam karta hai (Eureka)?

**Answer:**

Service Discovery services ko dynamic IPs ke bawajood ek doosre ko dhundhne deta hai.

### Eureka Server (Registry):

```java
@EnableEurekaServer
@SpringBootApplication
public class RegistryApplication { }
```

```yaml
eureka:
  client:
    register-with-eureka: false   # server khud register nahi karta
    fetch-registry: false
```

### Eureka Client (Service):

```java
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceApplication { }
```

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Flow:

```
1. Service starts → Eureka par register (name + IP + port)
2. Heartbeat bhejti rehti hai (alive)
3. Consumer Eureka se "order-service kahan hai?" poochta hai
4. Eureka instances list return karta hai
5. LoadBalancer ek instance choose karta hai
```

**Short Interview Answer:**

Service Discovery mein services ek registry (Eureka/Consul) par apna naam + address register karti hain aur heartbeats bhejti hain. Consumers logical name se instances lookup karte hain — hardcoded IPs nahi. Eureka Server registry hai (`@EnableEurekaServer`), services `@EnableDiscoveryClient` se register hoti hain. Kubernetes mein DNS-based discovery built-in hoti hai.

---

## Q8: OpenFeign mein error handling aur resilience (fallback, timeout) kaise implement karte hain?

**Answer:**

### Fallback with Resilience4j:

```java
@FeignClient(name = "order-service", fallback = OrderClientFallback.class)
public interface OrderClient {
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);
}

@Component
public class OrderClientFallback implements OrderClient {
    @Override
    public Order getOrder(Long id) {
        return Order.unavailable();   // graceful degradation
    }
}
```

```yaml
feign:
  circuitbreaker:
    enabled: true
```

### Timeouts:

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000
            read-timeout: 5000
```

### Custom Error Decoder:

```java
public class CustomErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 404) return new NotFoundException();
        return new DefaultErrorDecoder().decode(methodKey, response);
    }
}
```

### Request Interceptor (auth propagation):

```java
@Bean
public RequestInterceptor authInterceptor() {
    return template -> template.header("Authorization", getCurrentToken());
}
```

**Important:**

OpenFeign Resilience4j ke saath integrate hota hai — `fallback` class provide karo graceful degradation ke liye (jab downstream fail ho). Timeouts hamesha set karo taaki calls hamesha ke liye block na hon. Error decoder se HTTP status codes ko domain exceptions mein map karo. Request interceptor se auth tokens propagate karo.

---

## Q9: Spring Cloud Bus kya hai aur config refresh mein kaise help karta hai?

**Answer:**

Spring Cloud Bus distributed services ko ek **message broker** (Kafka/RabbitMQ) se link karta hai taaki state changes (jaise config refresh) saare instances ko broadcast ho sakein.

### Problem:

```
Config change → 50 instances ko manually /actuator/refresh karna? Impractical!
```

### Solution:

```
Config change in Git
        ↓
POST /actuator/busrefresh (kisi ek instance par)
        ↓
Spring Cloud Bus (Kafka/RabbitMQ) event broadcast
        ↓
Saare instances config refresh karte hain
```

### Setup:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

```yaml
spring:
  rabbitmq:
    host: localhost
```

### Auto Refresh with Webhook:

Git repo ka webhook Config Server ko notify karta hai → Bus refresh trigger → automatic propagation (koi manual step nahi).

**Short Interview Answer:**

Spring Cloud Bus services ko message broker (Kafka/RabbitMQ) se connect karta hai taaki config refresh jaise events saare instances par broadcast ho sakein. `POST /actuator/busrefresh` ek jagah call karo, sab instances refresh ho jaate hain — har ek ko manually refresh nahi karna. Git webhook se ye fully automatic bhi ban sakta hai.

---

## Q10: Spring Cloud mein ek complete microservices architecture kaise banate hain?

**Answer:**

### Typical Architecture:

```
                    [Config Server] (Git-backed)
                          ↑ (sab services config fetch karti hain)
Client → [API Gateway] → [Service Discovery: Eureka]
              ↓                    ↑ (register/lookup)
        ┌─────┼─────┬─────────┐
   [User Svc] [Order Svc] [Payment Svc]
        └── OpenFeign + LoadBalancer (inter-service calls)
        └── Resilience4j (circuit breaker)
        └── Micrometer Tracing → Zipkin (observability)
```

### Components Ka Role:

| Component | Role |
|-----------|------|
| **Config Server** | Centralized config sab services ko |
| **Eureka** | Service registration + discovery |
| **API Gateway** | Single entry, routing, auth, rate limit |
| **OpenFeign** | Service-to-service calls (declarative) |
| **LoadBalancer** | Instance selection |
| **Resilience4j** | Circuit breaker, retry, fallback |
| **Spring Cloud Bus** | Config broadcast refresh |
| **Micrometer + Zipkin** | Distributed tracing |

### Startup Order:

```
1. Config Server (config source)
2. Eureka Server (registry)
3. Business services (register + fetch config)
4. API Gateway (routes discovered services)
```

**Important:**

Complete Spring Cloud stack: Config Server (centralized config) + Eureka (discovery) + Gateway (entry point) + OpenFeign/LoadBalancer (inter-service calls) + Resilience4j (fault tolerance) + Micrometer/Zipkin (tracing). Startup order matter karta hai — Config Server aur Eureka pehle. Ye sab milkar independent, resilient, observable microservices architecture banate hain.

**Short Interview Answer:**

Config Server (config), Eureka (discovery), API Gateway (entry point + routing), OpenFeign + LoadBalancer (service calls), Resilience4j (circuit breaker/fallback), Spring Cloud Bus (config refresh), aur Micrometer + Zipkin (tracing) — ye sab milkar ek complete microservices platform banate hain. Config Server aur Eureka pehle start hone chahiye.
