# OpenFeign

## Status: Not Started

---

## Table of Contents

1. [OpenFeign Kya Hai?](#openfeign-kya-hai)
2. [Why Feign?](#why-feign)
3. [Setup](#setup)
4. [Defining a Feign Client](#defining-a-feign-client)
5. [@FeignClient Attributes](#feignclient-attributes)
6. [Method Signatures Mirror REST](#method-signatures-mirror-rest)
7. [Configuration](#configuration)
8. [Request Interceptors](#request-interceptors)
9. [Error Decoder](#error-decoder)
10. [Logging](#logging)
11. [Timeouts & Retries](#timeouts--retries)
12. [Service Discovery Integration](#service-discovery-integration)
13. [Resilience4j Fallback](#resilience4j-fallback)
14. [Testing Feign Clients](#testing-feign-clients)
15. [Common Pitfalls](#common-pitfalls)
16. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## OpenFeign Kya Hai?

**Matlab:** Declarative HTTP client — REST API ke liye **interface** likhte ho, Feign **proxy** banata hai jo HTTP calls karta hai.

> "REST API ko Java method call ki tarah use karo."

```java
// Define interface
@FeignClient(name = "order-service", url = "http://order-service:8081")
public interface OrderServiceClient {
    
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);
    
    @PostMapping("/orders")
    Order createOrder(@RequestBody CreateOrderRequest request);
}

// Use it like a Spring bean
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final OrderServiceClient orderClient;
    
    public void process(Long orderId) {
        Order order = orderClient.getOrder(orderId);   // HTTP call
        // ...
    }
}
```

→ No `RestTemplate.exchange(...)`, no manual URL building, no manual JSON parsing.

### Origin

- Created at **Netflix** (Feign)
- Donated to OpenFeign org
- Spring Cloud OpenFeign integrates it with Spring annotations + service discovery + resilience

### Key Features

✅ Declarative — interface = client
✅ Spring MVC annotations support (`@GetMapping`, `@RequestParam`, `@RequestBody`, etc.)
✅ Pluggable encoders/decoders (Jackson default)
✅ Service discovery integration (Eureka, Consul, Nacos)
✅ Load balancing (Spring Cloud LoadBalancer)
✅ Interceptors for cross-cutting concerns (auth headers, tracing)
✅ Error handling
✅ Resilience4j integration (circuit breaker + fallback)

---

## Why Feign?

### Without Feign — RestTemplate / WebClient

```java
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final RestTemplate restTemplate;
    
    public Order getOrder(Long id) {
        return restTemplate.getForObject(
            "http://order-service:8081/orders/" + id, Order.class);
    }
    
    public Order createOrder(CreateOrderRequest req) {
        ResponseEntity<Order> resp = restTemplate.postForEntity(
            "http://order-service:8081/orders",
            req,
            Order.class);
        return resp.getBody();
    }
}
```

❌ URL strings everywhere
❌ Easy to typo path / make verb wrong
❌ Headers/auth manual on every call
❌ Error mapping repetitive
❌ Hard to refactor

### With Feign

```java
@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);
    
    @PostMapping("/orders")
    Order createOrder(@RequestBody CreateOrderRequest req);
}
```

✅ Strongly typed
✅ Clean code
✅ Centralized config (timeouts, interceptors, error decoder)
✅ Easy refactoring (rename method = compile error)
✅ Testable (mock interface)

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
    <artifactId>spring-cloud-starter-openfeign</artifactId>
  </dependency>
  
  <!-- Optional: Resilience4j integration -->
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
  </dependency>
  
  <!-- Optional: load balancer + service discovery -->
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
  </dependency>
</dependencies>
```

### Enable Feign Clients

```java
@SpringBootApplication
@EnableFeignClients
public class CheckoutApplication {
    public static void main(String[] args) {
        SpringApplication.run(CheckoutApplication.class, args);
    }
}
```

```java
// Or with explicit base packages
@EnableFeignClients(basePackages = "com.example.checkout.clients")
```

---

## Defining a Feign Client

### Basic Example

```java
package com.example.checkout.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service", url = "http://order-service:8081")
public interface OrderClient {
    
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable("id") Long id);
    
    @GetMapping("/orders")
    List<Order> listOrders(@RequestParam("userId") Long userId,
                           @RequestParam("status") String status);
    
    @PostMapping("/orders")
    Order createOrder(@RequestBody CreateOrderRequest request);
    
    @PutMapping("/orders/{id}/cancel")
    void cancelOrder(@PathVariable Long id);
    
    @DeleteMapping("/orders/{id}")
    void deleteOrder(@PathVariable Long id);
}
```

### DTO Classes

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private Long id;
    private Long userId;
    private List<OrderItem> items;
    private BigDecimal total;
    private String status;
    private Instant createdAt;
}

@Data
public class CreateOrderRequest {
    private Long userId;
    private List<OrderItem> items;
    private String currency;
}
```

→ Same DTOs as the server (often shared via library).

### Usage

```java
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final OrderClient orderClient;
    
    public Order placeOrder(Long userId, List<OrderItem> items) {
        CreateOrderRequest req = new CreateOrderRequest(userId, items, "INR");
        return orderClient.createOrder(req);
    }
    
    public void cancel(Long orderId) {
        orderClient.cancelOrder(orderId);
    }
}
```

---

## @FeignClient Attributes

```java
@FeignClient(
    name = "order-service",                       // logical name (for discovery + config)
    url = "http://order-service:8081",            // optional — for URL-based (no discovery)
    path = "/api/v1",                              // common path prefix
    contextId = "orderClientV1",                  // unique bean ID if multiple clients to same service
    qualifiers = {"primary"},                     // Spring qualifiers
    primary = true,                                // primary bean if multiple
    configuration = OrderClientConfig.class,      // per-client config class
    fallback = OrderClientFallback.class,         // Hystrix-style fallback (legacy)
    fallbackFactory = OrderClientFallbackFactory.class
)
public interface OrderClient { ... }
```

### `name` — Service Discovery

```java
@FeignClient(name = "order-service")    // resolved via discovery
```

→ With Eureka/Consul/Nacos registered: Feign + LoadBalancer pick an instance.

### `url` — Direct URL (Bypass Discovery)

```java
@FeignClient(name = "external-api", url = "${external.api.url}")
```

```yaml
external:
  api:
    url: https://api.thirdparty.com
```

→ Useful for **third-party** APIs.

### `path` — Common Prefix

```java
@FeignClient(name = "order-service", path = "/api/v1")
public interface OrderClient {
    @GetMapping("/orders/{id}")    // resolves to /api/v1/orders/{id}
    Order get(@PathVariable Long id);
}
```

### `contextId` — When Multiple Clients to Same Service

```java
@FeignClient(name = "order-service", contextId = "orderQueryClient", path = "/queries")
public interface OrderQueryClient { ... }

@FeignClient(name = "order-service", contextId = "orderCommandClient", path = "/commands")
public interface OrderCommandClient { ... }
```

→ Without `contextId`, second one fails (bean conflict).

---

## Method Signatures Mirror REST

### Path Variables

```java
@GetMapping("/users/{userId}/orders/{orderId}")
Order get(@PathVariable Long userId, @PathVariable Long orderId);
```

⚠️ **Always name the path variable** in older versions:

```java
@GetMapping("/orders/{id}")
Order get(@PathVariable("id") Long id);    // explicit name
```

In modern Spring + `-parameters` compile flag, the name can be inferred. Best practice: be explicit.

### Query Parameters

```java
@GetMapping("/orders")
List<Order> search(@RequestParam("userId") Long userId,
                   @RequestParam(name = "status", required = false) String status);
```

### Multiple Query Params via Map

```java
@GetMapping("/orders")
List<Order> search(@SpringQueryMap OrderFilter filter);

@Data
public class OrderFilter {
    private Long userId;
    private String status;
    private Instant from;
    private Instant to;
}
```

→ Filter fields become query params.

### Headers

```java
@GetMapping("/orders/{id}")
Order get(@PathVariable Long id,
          @RequestHeader("X-Tenant-Id") String tenantId,
          @RequestHeader("Authorization") String authToken);
```

### Static Headers

```java
@GetMapping(value = "/orders", headers = "X-Source=checkout-service")
List<Order> listOrders();
```

### Request Body

```java
@PostMapping("/orders")
Order create(@RequestBody CreateOrderRequest request);
```

### Multipart

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
String upload(@RequestPart("file") MultipartFile file,
              @RequestPart("description") String description);
```

→ Requires `feign-form` dependency:

```xml
<dependency>
  <groupId>io.github.openfeign.form</groupId>
  <artifactId>feign-form</artifactId>
  <version>3.8.0</version>
</dependency>
<dependency>
  <groupId>io.github.openfeign.form</groupId>
  <artifactId>feign-form-spring</artifactId>
  <version>3.8.0</version>
</dependency>
```

### Form-Encoded

```java
@PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
TokenResponse login(@RequestParam("username") String username,
                    @RequestParam("password") String password);
```

### Returning ResponseEntity

```java
@GetMapping("/orders/{id}")
ResponseEntity<Order> getOrder(@PathVariable Long id);
```

→ Access status code + headers in addition to body.

### Returning Optional / Stream

```java
@GetMapping("/orders/{id}")
Optional<Order> findOrder(@PathVariable Long id);

@GetMapping("/orders")
Stream<Order> streamOrders();
```

→ Optional needs `Module` configured. Stream consumes incrementally.

---

## Configuration

### Per-Client Configuration Class

```java
public class OrderClientConfig {
    
    @Bean
    public Logger.Level loggerLevel() {
        return Logger.Level.FULL;
    }
    
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5_000,                // connect timeout (ms)
            10_000,               // read timeout (ms)
            true                   // follow redirects
        );
    }
    
    @Bean
    public ErrorDecoder errorDecoder() {
        return new OrderErrorDecoder();
    }
    
    @Bean
    public RequestInterceptor authInterceptor() {
        return template -> template.header("X-Service-Key", "internal-key-123");
    }
    
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(100, 1000, 3);
    }
}
```

```java
@FeignClient(name = "order-service", configuration = OrderClientConfig.class)
public interface OrderClient { ... }
```

⚠️ **Don't** annotate `OrderClientConfig` with `@Configuration` if it's referenced by `configuration =` — Spring will pick it up globally otherwise.

### Global Configuration

#### YAML

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:                          # all clients
            connect-timeout: 5000
            read-timeout: 10000
            logger-level: basic
            error-decoder: com.example.GlobalErrorDecoder
          
          order-service:                    # specific client (by name)
            connect-timeout: 2000
            read-timeout: 5000
            logger-level: full
            request-interceptors:
              - com.example.AuthInterceptor
```

#### Programmatic

```java
@Configuration
public class FeignGlobalConfig {
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
```

→ Applied to **all** Feign clients via `@EnableFeignClients(defaultConfiguration = FeignGlobalConfig.class)`.

---

## Request Interceptors

**Matlab:** Pre-call hook to modify outgoing request — auth, tracing, headers.

### Built-In Use Cases

#### 1. Forward Auth Header

```java
public class AuthForwardingInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) 
            RequestContextHolder.getRequestAttributes();
        
        if (attrs != null) {
            String auth = attrs.getRequest().getHeader("Authorization");
            if (auth != null) {
                template.header("Authorization", auth);
            }
        }
    }
}
```

→ Forward incoming user's JWT to downstream service.

#### 2. Service-to-Service Auth

```java
public class ServiceKeyInterceptor implements RequestInterceptor {
    @Value("${service.api-key}")
    private String apiKey;
    
    @Override
    public void apply(RequestTemplate template) {
        template.header("X-Service-Key", apiKey);
    }
}
```

#### 3. OAuth2 Client Credentials

```java
@Component
@RequiredArgsConstructor
public class OAuth2FeignInterceptor implements RequestInterceptor {
    private final OAuth2AuthorizedClientManager clientManager;
    
    @Override
    public void apply(RequestTemplate template) {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
            .withClientRegistrationId("internal-services")
            .principal("system")
            .build();
        
        OAuth2AuthorizedClient client = clientManager.authorize(request);
        if (client != null) {
            template.header("Authorization", "Bearer " + client.getAccessToken().getTokenValue());
        }
    }
}
```

#### 4. Correlation ID (Distributed Tracing)

```java
public class CorrelationIdInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String corrId = MDC.get("correlation-id");
        if (corrId == null) corrId = UUID.randomUUID().toString();
        template.header("X-Correlation-Id", corrId);
    }
}
```

#### 5. Tenant ID

```java
public class TenantHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        String tenant = TenantContext.getCurrent();
        if (tenant != null) {
            template.header("X-Tenant-Id", tenant);
        }
    }
}
```

### Register Interceptor

#### Globally

```java
@Bean
public RequestInterceptor authForwardingInterceptor() {
    return new AuthForwardingInterceptor();
}
```

→ Auto-picked by all Feign clients.

#### Per-Client

```java
public class OrderClientConfig {
    @Bean
    public RequestInterceptor orderClientAuth() {
        return new ServiceKeyInterceptor();
    }
}
```

```java
@FeignClient(name = "order-service", configuration = OrderClientConfig.class)
```

#### YAML

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          order-service:
            request-interceptors:
              - com.example.OrderAuthInterceptor
              - com.example.TracingInterceptor
```

---

## Error Decoder

**Matlab:** HTTP error response (4xx, 5xx) ko **typed exception** mein convert karna.

### Default Behavior

Default `ErrorDecoder.Default` throws `FeignException` — generic.

### Custom Error Decoder

```java
public class OrderErrorDecoder implements ErrorDecoder {
    
    private final ErrorDecoder defaultDecoder = new Default();
    
    @Override
    public Exception decode(String methodKey, Response response) {
        ErrorBody body = parseBody(response);
        
        switch (response.status()) {
            case 400:
                return new BadRequestException(body.getMessage());
            
            case 401:
                return new UnauthorizedException(body.getMessage());
            
            case 404:
                return new OrderNotFoundException(body.getMessage());
            
            case 409:
                return new ConflictException(body.getMessage());
            
            case 429:
                return new RateLimitException(body.getMessage());
            
            case 500:
            case 502:
            case 503:
            case 504:
                return new RetryableException(
                    response.status(),
                    body.getMessage(),
                    response.request().httpMethod(),
                    null,                       // retryAfter
                    response.request());        // make Feign retry this
            
            default:
                return defaultDecoder.decode(methodKey, response);
        }
    }
    
    private ErrorBody parseBody(Response response) {
        try (InputStream is = response.body().asInputStream()) {
            return new ObjectMapper().readValue(is, ErrorBody.class);
        } catch (IOException e) {
            return new ErrorBody("unknown", "Unable to parse error body");
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ErrorBody {
        private String code;
        private String message;
    }
}
```

### Register

```java
public class OrderClientConfig {
    @Bean
    public ErrorDecoder errorDecoder() {
        return new OrderErrorDecoder();
    }
}
```

### Custom Exceptions

```java
public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String message) { super(message); }
}
```

### Catching in Service

```java
try {
    Order order = orderClient.getOrder(id);
} catch (OrderNotFoundException e) {
    return Optional.empty();
} catch (RetryableException e) {
    log.warn("Temporary failure", e);
    throw e;   // let Feign retry / circuit breaker handle
}
```

---

## Logging

### Log Levels

| Level | Logs |
|-------|------|
| `NONE` | Nothing (default) |
| `BASIC` | Request method + URL, response status + duration |
| `HEADERS` | BASIC + request/response headers |
| `FULL` | HEADERS + request/response bodies |

### Set Globally

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            logger-level: basic
```

### Per-Client

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          order-service:
            logger-level: full
```

### Required: Set Java Logger to DEBUG

Feign logs at DEBUG level:

```yaml
logging:
  level:
    com.example.checkout.clients.OrderClient: DEBUG
    feign: DEBUG
```

### Sample Output (FULL)

```
[OrderClient#getOrder] ---> GET http://order-service:8081/orders/123 HTTP/1.1
[OrderClient#getOrder] X-Source: checkout-service
[OrderClient#getOrder] ---> END HTTP (0-byte body)
[OrderClient#getOrder] <--- HTTP/1.1 200 OK (52ms)
[OrderClient#getOrder] content-type: application/json
[OrderClient#getOrder] {"id": 123, "status": "PLACED", ...}
[OrderClient#getOrder] <--- END HTTP (152-byte body)
```

⚠️ Don't use `FULL` in production — leaks sensitive data.

---

## Timeouts & Retries

### Timeouts

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          default:
            connect-timeout: 5000     # ms — TCP connect
            read-timeout: 10000       # ms — wait for response
```

### Retryer

```java
@Bean
public Retryer feignRetryer() {
    return new Retryer.Default(
        100,        // initial interval (ms)
        1000,       // max interval (ms)
        3           // max attempts
    );
}
```

→ Retry on `RetryableException` only (your error decoder decides).

### Disable Retries (Default!)

```java
@Bean
public Retryer feignRetryer() {
    return Retryer.NEVER_RETRY;     // default — Feign doesn't retry
}
```

→ For non-idempotent requests (POST), retry can cause duplicates. Combine with **idempotency keys** if retrying.

### Better — Resilience4j Retry

```yaml
resilience4j:
  retry:
    instances:
      orderClient:
        max-attempts: 3
        wait-duration: 200ms
        retry-exceptions:
          - feign.RetryableException
          - java.io.IOException
        ignore-exceptions:
          - com.example.OrderNotFoundException
```

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderClient orderClient;
    
    @Retry(name = "orderClient")
    public Order getOrder(Long id) {
        return orderClient.getOrder(id);
    }
}
```

---

## Service Discovery Integration

### With Eureka

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://eureka:8761/eureka
```

### Feign Client (No URL)

```java
@FeignClient(name = "order-service")    // resolved via Eureka
public interface OrderClient { ... }
```

→ Feign + Spring Cloud LoadBalancer auto-resolve `order-service` to instance, load balance.

### `lb://` Style

```java
@FeignClient(name = "order-service", url = "lb://order-service")
```

→ Same effect. `lb://` triggers load balancer resolution explicitly.

### Without Discovery (Static URL)

```yaml
order-service:
  url: http://order-service.internal:8081
```

```java
@FeignClient(name = "order-service", url = "${order-service.url}")
public interface OrderClient { ... }
```

---

## Resilience4j Fallback

Combine Feign with circuit breaker + fallback for resilience.

### Setup

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

### Enable in Spring Cloud OpenFeign

```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
```

### Fallback Class

```java
@Component
public class OrderClientFallback implements OrderClient {
    
    @Override
    public Order getOrder(Long id) {
        // Return cached / default / minimal data
        return new Order(id, null, List.of(), BigDecimal.ZERO, "UNKNOWN", Instant.now());
    }
    
    @Override
    public List<Order> listOrders(Long userId, String status) {
        return List.of();
    }
    
    @Override
    public Order createOrder(CreateOrderRequest req) {
        throw new ServiceUnavailableException("Order service is down");
    }
    
    @Override
    public void cancelOrder(Long id) {
        throw new ServiceUnavailableException("Cannot cancel — order service down");
    }
    
    @Override
    public void deleteOrder(Long id) {
        throw new ServiceUnavailableException("Cannot delete — order service down");
    }
}
```

### Wire Fallback

```java
@FeignClient(
    name = "order-service",
    fallback = OrderClientFallback.class
)
public interface OrderClient { ... }
```

### FallbackFactory (Access to Cause)

For richer fallbacks that can inspect the exception:

```java
@Component
@RequiredArgsConstructor
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {
    
    @Override
    public OrderClient create(Throwable cause) {
        return new OrderClient() {
            @Override
            public Order getOrder(Long id) {
                log.warn("Falling back for order {}: {}", id, cause.getMessage());
                
                if (cause instanceof OrderNotFoundException) {
                    return null;
                }
                
                // Default
                return Order.unknown(id);
            }
            // ... others
        };
    }
}
```

```java
@FeignClient(
    name = "order-service",
    fallbackFactory = OrderClientFallbackFactory.class
)
public interface OrderClient { ... }
```

### Circuit Breaker Config

```yaml
resilience4j:
  circuitbreaker:
    instances:
      OrderClient:                          # name = client class name
        sliding-window-size: 20
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5
```

---

## Testing Feign Clients

### Unit Test (Mock)

```java
@Service
@RequiredArgsConstructor
public class CheckoutService {
    private final OrderClient orderClient;
    
    public Order checkout(Long userId) {
        return orderClient.createOrder(new CreateOrderRequest(userId, items, "INR"));
    }
}

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {
    
    @Mock
    OrderClient orderClient;
    
    @InjectMocks
    CheckoutService service;
    
    @Test
    void checkout_calls_order_service() {
        when(orderClient.createOrder(any())).thenReturn(new Order(1L, ...));
        
        Order result = service.checkout(123L);
        
        assertThat(result.getId()).isEqualTo(1L);
        verify(orderClient).createOrder(any());
    }
}
```

### Integration Test with WireMock

```xml
<dependency>
  <groupId>com.github.tomakehurst</groupId>
  <artifactId>wiremock-jre8</artifactId>
  <version>2.35.1</version>
  <scope>test</scope>
</dependency>
```

```java
@SpringBootTest
@AutoConfigureWireMock(port = 0)    // random port
class OrderClientIntegrationTest {
    
    @Autowired
    OrderClient orderClient;
    
    @Value("${wiremock.server.port}")
    int wireMockPort;
    
    @DynamicPropertySource
    static void overrideUrl(DynamicPropertyRegistry registry) {
        registry.add("order-service.url", () -> "http://localhost:${wiremock.server.port}");
    }
    
    @Test
    void getOrder_returns_order() {
        stubFor(get(urlEqualTo("/orders/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\":123,\"status\":\"PLACED\"}")));
        
        Order order = orderClient.getOrder(123L);
        
        assertThat(order.getId()).isEqualTo(123L);
        assertThat(order.getStatus()).isEqualTo("PLACED");
    }
}
```

### Contract Testing (Spring Cloud Contract)

For producer-consumer testing across services. See `Microservices Architecture/03-Service-Communication.md` (if exists).

---

## Common Pitfalls

### 1. Missing `@EnableFeignClients`

```java
@SpringBootApplication
// @EnableFeignClients   ← forgotten!
```

→ Beans not created. Cryptic NoSuchBeanDefinitionException at runtime.

### 2. `@PathVariable` Without Name (Older Spring)

```java
@GetMapping("/orders/{id}")
Order get(@PathVariable Long id);
```

→ Without `-parameters` compile flag, name is lost. Be explicit:

```java
@PathVariable("id") Long id
```

Or set Maven plugin:

```xml
<plugin>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-maven-plugin</artifactId>
  <configuration>
    <parameters>true</parameters>
  </configuration>
</plugin>
```

### 3. `@Configuration` on Per-Client Config

```java
@Configuration   // ❌ — picked up globally, breaks isolation
public class OrderClientConfig {
    @Bean
    public ErrorDecoder errorDecoder() { ... }
}
```

→ Don't annotate `@Configuration`; it's referenced by `configuration =` parameter, which is enough.

### 4. Multiple Clients Same Service Without `contextId`

```java
@FeignClient(name = "order-service", path = "/v1")
public interface OrderClientV1 { ... }

@FeignClient(name = "order-service", path = "/v2")
public interface OrderClientV2 { ... }
```

→ Bean conflict. Add `contextId`:

```java
@FeignClient(name = "order-service", contextId = "v1", path = "/v1")
@FeignClient(name = "order-service", contextId = "v2", path = "/v2")
```

### 5. POST Retry Without Idempotency

```yaml
retryer:
  max-attempts: 3
```

→ Network blip → POST retried → 2x records created. Use idempotency keys (cross-ref: `API-Design-&-Architecture/07-Idempotency.md` if exists).

### 6. FULL Logging in Prod

Leaks JWT, PII to logs. Use `BASIC` or `HEADERS` (with sensitive masking).

### 7. Forgetting Circuit Breaker

External call without resilience → cascading failures. Use Resilience4j `@CircuitBreaker` or fallback.

### 8. Long Default Read Timeout

Feign default `60_000ms` (1 min) → users wait forever, threads exhausted. Set lower:

```yaml
read-timeout: 5000
```

### 9. Sharing DTOs vs Decoupling

Tempting to share Java models in a "common" library. **Risk**: tight coupling, both services must redeploy on contract change.

**Alternative**: separate DTOs in client side, or generate from OpenAPI.

### 10. Forwarding User Auth to Internal Services

```java
template.header("Authorization", incomingAuth);   // forwards user's JWT
```

→ Risk: user's token has scope to internal admin services? **Better:** mint a service-internal token (less scope) or use mTLS.

### 11. URL Manually Built (Defeats Purpose)

```java
@GetMapping
Order get(@RequestParam("url") String fullUrl);    // ❌
```

→ Defeats Feign. Define proper paths/params.

### 12. WebFlux Reactive Conflict

Feign is **blocking**. In a reactive app, blocking calls in event loop = trouble. Use `WebClient` or run Feign on separate thread pool.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **`@EnableFeignClients`** | On main class |
| **`@FeignClient(name, url)`** | Define client interface |
| **`@FeignClient(name, contextId)`** | Multiple clients to same service |
| **`@FeignClient(path = "/v1")`** | Common path prefix |
| **Method = HTTP** | `@GetMapping`, `@PostMapping`, etc. |
| **`@RequestInterceptor`** | Auth, tracing, headers |
| **`ErrorDecoder`** | HTTP errors → typed exceptions |
| **`Logger.Level`** | NONE/BASIC/HEADERS/FULL |
| **Timeouts** | `connect-timeout`, `read-timeout` |
| **Retryer** | Default off; use Resilience4j |
| **`fallback`** | Static fallback impl |
| **`fallbackFactory`** | Fallback with cause |
| **`lb://`** | Load-balanced via discovery |
| **`@SpringQueryMap`** | Object → query params |

| ✅ Do | ❌ Don't |
|-------|---------|
| Always name `@PathVariable("id")` | Trust parameter name inference |
| Set explicit timeouts | Use defaults (60s) |
| Custom `ErrorDecoder` for typed exceptions | Catch `FeignException` everywhere |
| Disable retry by default | Retry POSTs without idempotency |
| Use `BASIC` log in prod | `FULL` in prod (leaks data) |
| Use circuit breaker + fallback | No resilience |
| `contextId` for multiple clients | Hope auto-naming works |
| Avoid sharing DTO library | Tight cross-service coupling |

---

## Practice

1. Define `OrderClient` Feign interface; replace existing `RestTemplate` calls.
2. Add `RequestInterceptor` to forward `Authorization` header.
3. Write a custom `ErrorDecoder` mapping 404 → `OrderNotFoundException`.
4. Set per-client timeouts via YAML.
5. Add Resilience4j circuit breaker + fallback for the client.
6. Use `@SpringQueryMap` for a search endpoint.
7. Integrate with Eureka — change client to discovery-based.
8. Write WireMock-based integration test.
9. Add correlation ID interceptor that forwards `X-Correlation-Id`.
10. Compare Feign vs WebClient for a reactive service.
