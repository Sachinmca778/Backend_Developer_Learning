# Spring Cloud Gateway

## Status: Not Started

---

## Table of Contents

1. [Spring Cloud Gateway Kya Hai?](#spring-cloud-gateway-kya-hai)
2. [Why API Gateway?](#why-api-gateway)
3. [Architecture & Request Flow](#architecture--request-flow)
4. [Setup](#setup)
5. [Route Configuration](#route-configuration)
6. [Predicates Deep Dive](#predicates-deep-dive)
7. [Filters Deep Dive](#filters-deep-dive)
8. [CircuitBreaker Filter (Resilience4j)](#circuitbreaker-filter-resilience4j)
9. [Rate Limiting Filter](#rate-limiting-filter)
10. [Custom Filters](#custom-filters)
11. [Global Filters](#global-filters)
12. [Service Discovery Integration](#service-discovery-integration)
13. [Security & Authentication](#security--authentication)
14. [Observability](#observability)
15. [Common Pitfalls](#common-pitfalls)
16. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Spring Cloud Gateway Kya Hai?

**Matlab:** Modern **API Gateway** for Spring ecosystem — built on **Spring WebFlux + Reactor + Netty** (reactive, non-blocking). Successor to **Zuul 1** (which was blocking).

```
         ┌──────────────────────────────────────────┐
         │           Spring Cloud Gateway           │
         │  ┌────────────────────────────────────┐  │
         │  │  Routes:                            │  │
         │  │  - Predicates (when to match)       │  │
         │  │  - Filters (transform req/resp)     │  │
         │  └────────────────────────────────────┘  │
         └─────────────────┬──────────────────────────┘
                           │
       ┌───────────────────┼───────────────────────┐
       │                   │                       │
  ┌────▼─────┐      ┌──────▼─────┐         ┌──────▼─────┐
  │  Order   │      │  Payment    │         │  User      │
  │  Service │      │  Service    │         │  Service   │
  └──────────┘      └─────────────┘         └────────────┘
```

### Why Reactive?

- **High concurrency** — single thread can serve thousands of slow connections
- **Backpressure** — protects against overload
- **Non-blocking I/O** — better resource usage

### Key Concepts

| Concept | Meaning |
|---------|---------|
| **Route** | URI + Predicates + Filters → forward target |
| **Predicate** | Java 8 `Predicate<ServerWebExchange>` — should this route match? |
| **Filter** | Modifies request/response — pre + post |
| **GatewayFilter** | Per-route filter |
| **GlobalFilter** | Applies to all routes |

---

## Why API Gateway?

### Without Gateway

```
Client → Service A (auth + ratelimit + logging + ...)
       → Service B (auth + ratelimit + logging + ...)
       → Service C (auth + ratelimit + logging + ...)
```

❌ Each service duplicates cross-cutting concerns.
❌ Client knows internal service URLs.
❌ Auth scattered.
❌ No central rate limiting.

### With Gateway

```
Client → Gateway (auth + ratelimit + logging + tracing)
              │
              ├→ Service A (focused on business logic)
              ├→ Service B
              └→ Service C
```

✅ Single entry point.
✅ Cross-cutting concerns centralized.
✅ Service URLs hidden.
✅ Versioning, deprecation handled at gateway.
✅ Easy to add WAF, mTLS, CORS, etc.

### Common Gateway Responsibilities

- Authentication / Authorization
- Rate limiting / throttling
- Circuit breaking
- Retry / fallback
- Path rewriting
- Request/response transformation
- Header manipulation
- Logging / metrics / tracing
- Caching
- Routing (based on path/host/headers)

→ Cross-ref: `Microservices Architecture/02-API-Gateway-Pattern.md`.

---

## Architecture & Request Flow

```
┌────────────────────────────────────────────────────────────────┐
│                    Spring Cloud Gateway                         │
│                                                                 │
│  Request                                                        │
│    │                                                            │
│    ▼                                                            │
│  ┌────────────────┐                                             │
│  │ Gateway Handler │                                            │
│  │     Mapping     │  ← match against route predicates          │
│  └────────┬────────┘                                            │
│           │ matched route                                       │
│           ▼                                                     │
│  ┌─────────────────────────────────────────────────┐            │
│  │ Filter Chain (pre-filters)                       │            │
│  │  1. Global filter A                              │            │
│  │  2. Route filter X                               │            │
│  │  3. Route filter Y                               │            │
│  └────────┬─────────────────────────────────────────┘            │
│           │                                                      │
│           ▼                                                      │
│  ┌────────────────┐                                              │
│  │ Proxy to URI    │ → upstream service                          │
│  │  (NettyClient)  │                                             │
│  └────────┬────────┘                                             │
│           │ response                                             │
│           ▼                                                      │
│  ┌─────────────────────────────────────────────────┐             │
│  │ Filter Chain (post-filters)                      │             │
│  │  3. Route filter Y                               │             │
│  │  2. Route filter X                               │             │
│  │  1. Global filter A                              │             │
│  └────────┬─────────────────────────────────────────┘             │
│           ▼                                                      │
│         Response                                                 │
└────────────────────────────────────────────────────────────────┘
```

→ Filters execute **before** + **after** proxying. Pre-phase top-down, post-phase bottom-up.

---

## Setup

### Maven Dependencies

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.2.5</version>
</parent>

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
    <artifactId>spring-cloud-starter-gateway</artifactId>
  </dependency>
  
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
</dependencies>
```

⚠️ **Don't add** `spring-boot-starter-web` — Gateway uses **WebFlux**, will conflict.

### Main Class

```java
@SpringBootApplication
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

### Basic `application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: api-gateway
  
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://localhost:8081
          predicates:
            - Path=/api/orders/**
        
        - id: payment-service
          uri: http://localhost:8082
          predicates:
            - Path=/api/payments/**

management:
  endpoints:
    web:
      exposure:
        include: health, info, gateway
```

---

## Route Configuration

### Anatomy of a Route

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service-v1                  # unique route ID
          uri: http://order-service:8081        # target
          predicates:                            # when to match
            - Path=/api/v1/orders/**
            - Method=GET,POST
          filters:                               # transformations
            - StripPrefix=2
            - AddRequestHeader=X-Source,gateway
          metadata:                              # custom metadata
            response-timeout: 5000
            connect-timeout: 1000
```

### Java DSL (Programmatic)

```java
@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("order-service", r -> r
                .path("/api/v1/orders/**")
                .filters(f -> f
                    .stripPrefix(2)
                    .addRequestHeader("X-Source", "gateway")
                    .circuitBreaker(c -> c.setName("orderCB").setFallbackUri("forward:/fallback")))
                .uri("http://order-service:8081"))
            .route("payment-service", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f
                    .rewritePath("/api/v1/payments/(?<segment>.*)", "/${segment}")
                    .requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter())))
                .uri("lb://payment-service"))    // load-balanced via discovery
            .build();
    }
}
```

→ YAML for simple, Java DSL for dynamic / complex.

### Route Discovery from Config Server

```yaml
# In Config Server's Git
# api-gateway-prod.yml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service
          predicates:
            - Path=/api/orders/**
```

→ Routes can be reloaded via `/actuator/refresh` (with `@RefreshScope` on RouteDefinitionLocator).

---

## Predicates Deep Dive

Predicates decide whether a request matches a route.

### Built-In Predicates

#### 1. Path

```yaml
predicates:
  - Path=/api/orders/**, /api/v1/orders/**
```

Patterns:
- `*` — single segment
- `**` — any nested path
- `{var}` — captures variable (usable in filters)

#### 2. Method

```yaml
- Method=GET,POST
```

#### 3. Host

```yaml
- Host=*.example.com,api.example.org
```

→ Useful for multi-tenant subdomain routing.

#### 4. Header

```yaml
- Header=X-Request-Id, \d+
- Header=Authorization, Bearer .+
```

→ Header name + optional regex value.

#### 5. Query

```yaml
- Query=red          # query param "red" must exist
- Query=color, blue|green   # value matches regex
```

#### 6. Cookie

```yaml
- Cookie=session, abc.*
```

#### 7. After / Before / Between (Time-Based)

```yaml
- After=2024-12-25T00:00:00+05:30[Asia/Kolkata]
- Before=2025-01-01T00:00:00+05:30
- Between=2024-12-25T00:00:00+05:30, 2025-01-01T00:00:00+05:30
```

→ Useful for scheduled feature releases or sunsetting endpoints.

#### 8. RemoteAddr

```yaml
- RemoteAddr=10.0.0.0/8, 192.168.0.0/16
```

→ IP range allow.

#### 9. Weight (A/B Testing)

```yaml
- id: order-v1
  uri: http://order-v1:8081
  predicates:
    - Path=/api/orders/**
    - Weight=group1, 80      # 80% to v1
- id: order-v2
  uri: http://order-v2:8081
  predicates:
    - Path=/api/orders/**
    - Weight=group1, 20      # 20% to v2
```

→ Same weight group; ratio decides traffic split. Great for canary releases.

### Combining Predicates

All predicates must match (AND logic):

```yaml
predicates:
  - Path=/api/orders/**
  - Method=POST
  - Header=X-Tenant-Id, .+
  - Host=*.example.com
```

### Custom Predicate

```java
@Component
public class TenantPredicateFactory 
    extends AbstractRoutePredicateFactory<TenantPredicateFactory.Config> {
    
    public TenantPredicateFactory() {
        super(Config.class);
    }
    
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("tenantId");
    }
    
    @Override
    public Predicate<ServerWebExchange> apply(Config config) {
        return exchange -> {
            String tenant = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
            return config.getTenantId().equals(tenant);
        };
    }
    
    public static class Config {
        private String tenantId;
        public String getTenantId() { return tenantId; }
        public void setTenantId(String t) { this.tenantId = t; }
    }
}
```

```yaml
- Tenant=acme       # custom shortcut
```

---

## Filters Deep Dive

### Filter Categories

| Category | Examples |
|----------|----------|
| **Routing** | StripPrefix, PrefixPath, RewritePath, SetPath |
| **Headers** | AddRequestHeader, AddResponseHeader, RemoveRequestHeader, MapRequestHeader |
| **Body** | ModifyRequestBody, ModifyResponseBody (use carefully) |
| **Cookie** | AddRequestCookie, RemoveRequestCookie |
| **Resilience** | CircuitBreaker, Retry, FallbackHeaders |
| **Rate Limiting** | RequestRateLimiter |
| **Status** | SetStatus, RewriteResponseHeader |
| **Cache** | LocalResponseCache (since SCG 4.x) |

### Common Filters

#### 1. StripPrefix

Removes N path segments before forwarding:

```yaml
- id: orders
  uri: http://order-service:8081
  predicates:
    - Path=/api/v1/orders/**
  filters:
    - StripPrefix=2
```

→ Client: `GET /api/v1/orders/123` → forwarded as: `GET /orders/123`.

#### 2. PrefixPath

Adds prefix:

```yaml
filters:
  - PrefixPath=/api/v1
```

→ `/orders/123` → `/api/v1/orders/123` upstream.

#### 3. RewritePath

Regex-based path rewrite:

```yaml
filters:
  - RewritePath=/api/v1/(?<segment>.*), /${segment}
```

→ `/api/v1/orders/123` → `/orders/123`.

```yaml
filters:
  - RewritePath=/red/(?<segment>.*), /$\{segment}    # YAML escape ${ → $\{
```

#### 4. SetPath

```yaml
filters:
  - SetPath=/orders/{id}
```

#### 5. AddRequestHeader / AddResponseHeader

```yaml
filters:
  - AddRequestHeader=X-Request-Source, gateway
  - AddResponseHeader=X-Server-Region, ap-south-1
```

#### 6. RemoveRequestHeader

```yaml
filters:
  - RemoveRequestHeader=Cookie
```

→ Strip sensitive headers before forwarding.

#### 7. SetStatus

```yaml
filters:
  - SetStatus=401
```

→ Force response status.

#### 8. Retry

```yaml
filters:
  - name: Retry
    args:
      retries: 3
      statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE
      methods: GET
      backoff:
        firstBackoff: 100ms
        maxBackoff: 500ms
        factor: 2
        basedOnPreviousValue: false
```

⚠️ Only on **idempotent** methods.

#### 9. RedirectTo

```yaml
filters:
  - RedirectTo=302, https://new.example.com
```

#### 10. RequestSize

```yaml
filters:
  - name: RequestSize
    args:
      maxSize: 5000000   # 5MB
```

→ Reject big payloads early.

---

## CircuitBreaker Filter (Resilience4j)

Wrap downstream calls with circuit breaker — see `Microservices Architecture/05-Resilience-Patterns.md`.

### Setup

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
</dependency>
```

### Route with CircuitBreaker

```yaml
- id: orders
  uri: http://order-service:8081
  predicates:
    - Path=/api/orders/**
  filters:
    - name: CircuitBreaker
      args:
        name: ordersCB
        fallbackUri: forward:/fallback/orders
        statusCodes:
          - 500
          - GATEWAY_TIMEOUT
```

### Circuit Breaker Config

```yaml
resilience4j:
  circuitbreaker:
    instances:
      ordersCB:
        sliding-window-size: 20
        sliding-window-type: COUNT_BASED
        failure-rate-threshold: 50          # 50% failures → OPEN
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 5
        automatic-transition-from-open-to-half-open-enabled: true
  
  timelimiter:
    instances:
      ordersCB:
        timeout-duration: 3s
```

### Fallback Endpoint

```java
@RestController
public class FallbackController {
    
    @RequestMapping("/fallback/orders")
    public ResponseEntity<Map<String, Object>> ordersFallback() {
        return ResponseEntity.status(503)
            .body(Map.of(
                "error", "service_unavailable",
                "message", "Orders service is temporarily down. Please retry."
            ));
    }
}
```

### State Transitions

```
CLOSED ─── failures > threshold ───▶ OPEN
   ▲                                   │
   │                              wait-duration
   │                                   │
HALF-OPEN ◀─────── timeout ────────────┘
   │
   │  successful calls > threshold
   ▼
 CLOSED
```

---

## Rate Limiting Filter

### Redis-Backed Rate Limiter

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

```yaml
spring:
  redis:
    host: redis
    port: 6379
  
  cloud:
    gateway:
      routes:
        - id: orders
          uri: http://order-service:8081
          predicates:
            - Path=/api/orders/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10    # tokens/sec
                redis-rate-limiter.burstCapacity: 20    # max burst
                redis-rate-limiter.requestedTokens: 1
                key-resolver: '#{@userKeyResolver}'
```

### Key Resolver

```java
@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> Mono.justOrEmpty(
            exchange.getRequest().getHeaders().getFirst("X-User-Id"))
            .switchIfEmpty(Mono.just("anonymous"));
    }
    
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }
}
```

### Algorithm: Token Bucket

```
Bucket capacity:    20 tokens (burst)
Refill rate:        10 tokens/sec

Each request consumes 1 token.
- Bucket has tokens → allow
- Bucket empty → 429 Too Many Requests
```

→ Cross-ref: `API-Design-&-Architecture/06-Rate-Limiting.md` (if exists).

### Response When Limited

```http
HTTP/1.1 429 Too Many Requests
X-RateLimit-Remaining: 0
X-RateLimit-Burst-Capacity: 20
X-RateLimit-Replenish-Rate: 10
```

### Tier-Based Limits (Multi-Route)

```yaml
- id: orders-free
  uri: http://order-service
  predicates:
    - Path=/api/orders/**
    - Header=X-Plan, free
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 5
        redis-rate-limiter.burstCapacity: 10

- id: orders-pro
  uri: http://order-service
  predicates:
    - Path=/api/orders/**
    - Header=X-Plan, pro
  filters:
    - name: RequestRateLimiter
      args:
        redis-rate-limiter.replenishRate: 100
        redis-rate-limiter.burstCapacity: 200
```

---

## Custom Filters

### GatewayFilter (Per-Route)

```java
@Component
public class LoggingGatewayFilterFactory
        extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {
    
    private static final Logger log = LoggerFactory.getLogger(LoggingGatewayFilterFactory.class);
    
    public LoggingGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("baseMessage");
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            log.info("[{}] PRE: {} {}", config.getBaseMessage(),
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath());
            
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                log.info("[{}] POST: {}", config.getBaseMessage(),
                    exchange.getResponse().getStatusCode());
            }));
        };
    }
    
    @Data
    public static class Config {
        private String baseMessage;
    }
}
```

```yaml
filters:
  - Logging=order-service
```

### Order Header Adder Filter

```java
@Component
public class CorrelationIdFilterFactory
        extends AbstractGatewayFilterFactory<Object> {
    
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders()
                .getFirst("X-Correlation-Id");
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }
            
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-Correlation-Id", correlationId)
                .build();
            
            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }
}
```

### Auth Header Injection

```java
@Component
public class AuthHeaderFilterFactory
        extends AbstractGatewayFilterFactory<AuthHeaderFilterFactory.Config> {
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return validateToken(exchange)
                .flatMap(userId -> {
                    ServerHttpRequest request = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .build();
                    return chain.filter(exchange.mutate().request(request).build());
                })
                .switchIfEmpty(unauthorized(exchange));
        };
    }
    
    private Mono<String> validateToken(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) return Mono.empty();
        // Validate JWT, return user ID
        return Mono.just("user-123");   // placeholder
    }
    
    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
    
    public static class Config {}
}
```

---

## Global Filters

Apply to **every** route automatically.

```java
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalLoggingFilter.class);
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        
        log.info(">>> {} {}", request.getMethod(), request.getURI());
        
        return chain.filter(exchange).doFinally(signal -> {
            long duration = System.currentTimeMillis() - start;
            log.info("<<< {} {} {}ms", 
                exchange.getResponse().getStatusCode(),
                request.getURI(),
                duration);
        });
    }
    
    @Override
    public int getOrder() {
        return -1;   // execute very early
    }
}
```

### Order Convention

```
Lower order  =  earlier execution (in pre-phase)
Higher order =  earlier in post-phase

Common conventions:
  -2147483648  = highest priority (Integer.MIN_VALUE)
  -1           = before standard filters
  0            = standard
  +1           = after standard
  +2147483647  = lowest priority
```

### Built-In Global Filters

| Filter | Purpose |
|--------|---------|
| `RemoveCachedBodyFilter` | Clean up cached body |
| `AdaptCachedBodyGlobalFilter` | Cache body if needed by other filters |
| `NettyWriteResponseFilter` | Write proxied response back |
| `RouteToRequestUrlFilter` | Set destination URI |
| `LoadBalancerClientFilter` | Resolve `lb://service` URIs |
| `WebsocketRoutingFilter` | WebSocket support |

### Disable Built-Ins

```yaml
spring:
  cloud:
    gateway:
      filter:
        secure-headers:
          enabled: false
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
      defaultZone: http://eureka-server:8761/eureka

spring:
  cloud:
    gateway:
      discovery:
        locator:
          enabled: true               # auto-create routes from Eureka
          lower-case-service-id: true
      routes:
        - id: orders
          uri: lb://order-service     # lb:// = load-balanced via discovery
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1
```

### `lb://` URI

```yaml
uri: lb://order-service
```

→ Gateway resolves `order-service` to instances via discovery + load balances.

→ Cross-ref: `Microservices Architecture/04-Service-Discovery.md`, file `04-Spring-Cloud-LoadBalancer.md` next door.

### Auto Routes from Discovery

With `discovery.locator.enabled: true`:

```
Service registered in Eureka: payment-service
Gateway auto-creates route:
  /payment-service/**  →  lb://payment-service
```

→ Useful for dev; for prod, **define routes explicitly** for clarity.

---

## Security & Authentication

### Spring Security WebFlux

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health", "/public/**").permitAll()
                .pathMatchers("/api/admin/**").hasRole("ADMIN")
                .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .build();
    }
}
```

### JWT Validation at Gateway

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: https://auth.example.com/.well-known/jwks.json
```

→ Gateway validates JWT once; downstream services trust forwarded headers (use mTLS or signed forwarded token to prevent spoofing).

### Forward User Identity

```java
@Component
public class UserIdHeaderFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
            .map(ctx -> ctx.getAuthentication())
            .map(auth -> auth.getName())
            .defaultIfEmpty("anonymous")
            .flatMap(userId -> {
                ServerHttpRequest req = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .build();
                return chain.filter(exchange.mutate().request(req).build());
            });
    }
    
    @Override
    public int getOrder() { return 0; }
}
```

### CORS

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins:
              - https://app.example.com
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            allowCredentials: true
            maxAge: 3600
```

### Security Headers

Built-in `secure-headers` filter sets:
- `X-Frame-Options: DENY`
- `X-Content-Type-Options: nosniff`
- `X-XSS-Protection: 1; mode=block`
- `Strict-Transport-Security`
- `Referrer-Policy`

---

## Observability

### Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, gateway, metrics, prometheus
```

### Useful Endpoints

```bash
# List all routes
GET /actuator/gateway/routes

# Specific route details
GET /actuator/gateway/routes/order-service

# Refresh routes from config
POST /actuator/gateway/refresh

# Global filters
GET /actuator/gateway/globalfilters

# Available route filters / predicates
GET /actuator/gateway/routefilters
GET /actuator/gateway/routepredicates
```

### Metrics (Micrometer)

Auto-exposed:
```
spring.cloud.gateway.requests        ← per-route counter
gateway.requests                     ← duration histogram
http.server.requests                 ← incoming
```

### Distributed Tracing

```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-zipkin</artifactId>
</dependency>
```

→ Gateway propagates `traceparent` to downstream services. Cross-ref: `Microservices Architecture/06-Distributed-Tracing.md`.

### Access Log

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        wiretap: true        # log full HTTP at DEBUG
      httpserver:
        wiretap: true

logging:
  level:
    reactor.netty: DEBUG     # incl. wiretap
```

⚠️ Verbose. For production, custom GlobalFilter writing structured access logs is better.

---

## Common Pitfalls

### 1. Adding `spring-boot-starter-web`

Conflicts with WebFlux Netty. **Remove** if accidentally included.

### 2. Blocking Code in Filters

```java
// ❌ BLOCKING in reactive flow!
return chain.filter(exchange).doOnNext(x -> {
    Thread.sleep(1000);   // blocks event loop
    callDatabase();       // JDBC = blocking
});

// ✅ Use reactive APIs
return chain.filter(exchange).flatMap(x ->
    redisReactiveTemplate.opsForValue().get("key"));
```

### 3. RewritePath Regex Mistakes

Reserved characters in YAML need careful escaping:

```yaml
# YAML treats $ as variable interpolation
- RewritePath=/api/(?<segment>.*), /${segment}    # gets parsed wrong!

# Escape $:
- RewritePath=/api/(?<segment>.*), /$\{segment}
```

### 4. StripPrefix Counting

```yaml
predicates:
  - Path=/api/v1/orders/**
filters:
  - StripPrefix=2     # strips /api/v1
```

→ Count carefully. Off-by-one common.

### 5. Forgetting `lb://` for Discovery

```yaml
uri: http://order-service     # ❌ DNS lookup, won't load balance
uri: lb://order-service       # ✅ uses discovery + LB
```

### 6. Order Mishaps in Filters

```java
@Override
public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;   // careful — runs before security
}
```

→ Auth filter must run **before** custom logic that assumes user is auth'd.

### 7. Memory Leak with Modify Body

`ModifyRequestBody` / `ModifyResponseBody` cache bodies → memory pressure for large payloads. Don't use unless needed.

### 8. Long Timeouts Blocking Threads

Default Netty timeout high. Configure per-route:

```yaml
spring:
  cloud:
    gateway:
      httpclient:
        connect-timeout: 1000          # ms
        response-timeout: 5s
      routes:
        - id: orders
          uri: http://...
          metadata:
            response-timeout: 3000
            connect-timeout: 500
```

### 9. Rate Limiter Without Redis

`redis-rate-limiter` requires Redis. Without it, app fails. Or use **in-memory** version for single-node only (not for cluster!).

### 10. Auto-Routes in Production

```yaml
discovery.locator.enabled: true
```

→ Convenient in dev, but **explicit routes** in prod for predictability + filter assignment.

### 11. SSE / WebSocket

WebFlux supports them, but WebSocket via gateway needs:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: ws
          uri: ws://chat-service:8080
          predicates:
            - Path=/ws/**
```

→ Cross-ref: `Networking-&-Protocols/05-WebSocket-and-SSE.md`.

### 12. Memory Pressure

Gateway is **single chokepoint**. Provision generously:
- Heap 1-2 GB
- Off-heap (Netty direct) — set `-XX:MaxDirectMemorySize`
- Multiple replicas for HA

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Built on** | Spring WebFlux + Reactor + Netty |
| **Route** | id + uri + predicates + filters |
| **Predicate** | When does route match? |
| **Filter** | Transform request/response |
| **Global filter** | Applies to all routes |
| **Path predicate** | `Path=/api/orders/**` |
| **lb://** | Load-balanced via discovery |
| **StripPrefix** | Remove path segments |
| **CircuitBreaker** | Resilience4j integration |
| **RequestRateLimiter** | Redis token bucket |
| **CORS** | `globalcors` config |
| **Auth** | Spring Security WebFlux + JWT |

| Predicate | Example |
|-----------|---------|
| Path | `Path=/api/**` |
| Method | `Method=GET,POST` |
| Host | `Host=*.example.com` |
| Header | `Header=X-API-Key, .+` |
| Query | `Query=tenant, .+` |
| Cookie | `Cookie=session, .+` |
| Time | `After=2025-01-01...` |
| RemoteAddr | `RemoteAddr=10.0.0.0/8` |
| Weight | `Weight=group1, 80` |

| Filter | Example |
|--------|---------|
| StripPrefix | `StripPrefix=2` |
| PrefixPath | `PrefixPath=/api` |
| RewritePath | `RewritePath=/api/(?<s>.*), /$\{s}` |
| AddRequestHeader | `AddRequestHeader=X-Foo, bar` |
| RemoveRequestHeader | `RemoveRequestHeader=Cookie` |
| CircuitBreaker | `name: ordersCB, fallbackUri: forward:/fallback` |
| RequestRateLimiter | Redis token bucket, key resolver |
| Retry | `retries: 3, statuses: 5xx` |
| RedirectTo | `302, https://...` |
| RequestSize | `maxSize: 5MB` |
| SetStatus | `SetStatus=401` |

---

## Practice

1. Set up gateway routing two services on different paths.
2. Add `StripPrefix=1` to remove `/api` before forwarding.
3. Add `AddRequestHeader=X-Source, gateway` and verify in downstream.
4. Add CircuitBreaker filter with fallback endpoint; simulate downtime.
5. Add Redis-backed RequestRateLimiter; test 429 response.
6. Write a custom `GatewayFilter` that validates JWT and forwards `X-User-Id`.
7. Implement Weight predicate for 80/20 canary between v1/v2 services.
8. Integrate with Eureka — use `lb://service-name`.
9. Add OAuth2 Resource Server JWT validation at gateway.
10. Configure CORS for SPA; verify pre-flight succeeds.
11. Set per-route timeouts; verify 504 on slow upstream.
12. Add a global filter for correlation ID propagation.
