# 02 - API Gateway Pattern

> API Gateway microservices architecture ka **single entry point** hota hai — jaise building ka security guard, sab requests pehle uske paas aati hain, phir woh decide karta hai kis service ko bhejna hai.

---

## Table of Contents

1. [What is API Gateway?](#1-what-is-api-gateway)
2. [Why Do We Need It? (Without vs With Gateway)](#2-why-do-we-need-it)
3. [Request Routing](#3-request-routing)
4. [Load Balancing](#4-load-balancing)
5. [Authentication & Authorization at Gateway](#5-authentication--authorization-at-gateway)
6. [Rate Limiting](#6-rate-limiting)
7. [SSL Termination](#7-ssl-termination)
8. [Request/Response Transformation](#8-requestresponse-transformation)
9. [Circuit Breaker at Gateway](#9-circuit-breaker-at-gateway)
10. [Popular API Gateways Comparison](#10-popular-api-gateways-comparison)
11. [Best Practices](#11-best-practices)
12. [Summary](#12-summary)

---

## 1. What is API Gateway?

API Gateway ek **reverse proxy** hai jo client aur microservices ke beech mein baithta hai. Saari client requests pehle gateway pe aati hain, aur gateway unhe appropriate backend service ko forward karta hai.

```
                    ┌─────────────────────────────┐
                    │       API Gateway            │
                    │   (Single Entry Point)       │
                    └──────────┬──────────────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
              ▼                ▼                ▼
     ┌────────────┐   ┌────────────┐   ┌────────────┐
     │   User     │   │   Order    │   │  Payment   │
     │  Service   │   │  Service   │   │  Service   │
     └────────────┘   └────────────┘   └────────────┘
```

**Real-world example:** Socho ek e-commerce app hai — `/api/users/*`, `/api/orders/*`, `/api/payments/*` — sab requests pehle API Gateway pe aayengi, phir gateway decide karega kis service ko route karna hai.

### Key Responsibilities:

- **Request Routing** — `/api/users/**` -> User Service
- **Load Balancing** — Multiple instances ke beech traffic distribute karna
- **Authentication/Authorization** — JWT verify karna gateway pe hi
- **Rate Limiting** — Ek user kitni requests bhej sakta hai
- **SSL Termination** — HTTPS ko HTTP mein convert karna
- **Request/Response Transformation** — Format change karna
- **Circuit Breaking** — Down service ko handle karna

---

## 2. Why Do We Need It?

### Without API Gateway (Problem)

```
   Mobile App ──────────► User Service (port 8081)
   Mobile App ──────────► Order Service (port 8082)
   Mobile App ──────────► Payment Service (port 8083)
   Web App   ──────────► User Service (port 8081)
   Web App   ──────────► Order Service (port 8082)
```

**Problems:**
- Client ko har service ka URL/port pata hona padta hai
- Har service ko alag se auth implement karna padta hai
- CORS issues har service pe handle karne padte hain
- Rate limiting har service pe alag se lagani padti hai
- Service change hone pe client code bhi change karna padta hai

### With API Gateway (Solution)

```
   Mobile App ─┐
               ├────► API Gateway (port 8080) ──► Route to correct service
   Web App   ─┘
```

**Benefits:**
- Client ko sirf **ek URL** pata hona hai (gateway ka)
- Auth, rate limiting, logging — sab **ek jagah** handle hota hai
- Backend services change ho jayein, client ko fark nahi padta
- Cross-cutting concerns centralized ho jate hain

---

## 3. Request Routing

Gateway ka sabse basic kaam — request dekh ke decide karna kis service ko bhejna hai.

### Spring Cloud Gateway mein Routing Config

**application.yml:**

```yaml
spring:
  cloud:
    gateway:
      routes:
        # User Service route
        - id: user-service
          uri: lb://USER-SERVICE          # lb = load balanced
          predicates:
            - Path=/api/users/**
          filters:
            - StripPrefix=1

        # Order Service route
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
          filters:
            - StripPrefix=1

        # Payment Service route
        - id: payment-service
          uri: lb://PAYMENT-SERVICE
          predicates:
            - Path=/api/payments/**
          filters:
            - StripPrefix=1
```

### Routing Flow:

```
Request: GET /api/users/123
                │
                ▼
        ┌───────────────┐
        │  API Gateway  │  Predicate match: Path=/api/users/**
        └───────┬───────┘
                │ StripPrefix=1 => /users/123
                ▼
        ┌───────────────┐
        │ User Service  │  GET /users/123
        └───────────────┘
```

### Programmatic Routing (Java Config):

```java
@Configuration
public class GatewayRoutes {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // User Service
            .route("user-service", r -> r
                .path("/api/users/**")
                .filters(f -> f.stripPrefix(1))
                .uri("lb://USER-SERVICE")
            )
            // Order Service
            .route("order-service", r -> r
                .path("/api/orders/**")
                .filters(f -> f.stripPrefix(1))
                .uri("lb://ORDER-SERVICE")
            )
            .build();
    }
}
```

**Important Points:**
- `lb://` prefix ka matlab hai — **load balanced URI**, service discovery se instance dhundega
- `predicates` decide karte hain ki kaunsi route match hogi
- `filters` request/response ko modify kar sakte hain
- `StripPrefix=1` ka matlab — `/api/users/123` se `/api` hata ke `/users/123` bhejega

---

## 4. Load Balancing

Jab ek service ke **multiple instances** hote hain, toh gateway ko decide karna padta hai kis instance pe request bhejni hai.

### Load Balancing Algorithms

```
                    API Gateway
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
     ┌────────┐   ┌────────┐   ┌────────┐
     │Inst: 1 │   │Inst: 2 │   │Inst: 3 │
     │Load: 3 │   │Load: 1 │   │Load: 2 │
     └────────┘   └────────┘   └────────┘
```

| Algorithm | Kaise Kaam Karta Hai | Pros | Cons |
|-----------|---------------------|------|------|
| **Round Robin** | Ek-ek karke sabko bhejta hai | Simple, fair | Load ignore karta hai |
| **Least Connections** | Jiske sabse kam active connections hain | Better distribution | Thoda complex |
| **Weighted Round Robin** | Capacity ke hisaab se weight assign | Powerful servers ko zyada traffic | Manual weight config |
| **Random** | Randomly kisi ko bhi | Bahut simple | Unpredictable |
| **IP Hash** | Same client hamesha same instance | Session affinity | Load imbalance ho sakta hai |

### Spring Cloud LoadBalancer Config:

```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true
      zone: default

# NiftyLoadBalancer (Spring Cloud 2020+)
# Ribbon is DEPRECATED — ab Spring Cloud LoadBalancer use hota hai
```

### Custom Load Balancer Configuration:

```java
@Configuration
public class LoadBalancerConfig {

    @Bean
    @LoadBalancerClient(name = "USER-SERVICE", configuration = UserSvcLBConfig.class)
    public ReactorLoadBalancer<ServiceInstance> userLoadBalancer(
            Environment env, LoadBalancerClientFactory factory) {
        String serviceId = "USER-SERVICE";
        return new RoundRobinLoadBalancer(
            factory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class),
            serviceId
        );
    }
}
```

**Best Practice:** Production mein **Round Robin** ya **Least Connections** use karo. Simple hai aur most cases mein kaafi hai.

---

## 5. Authentication & Authorization at Gateway

Gateway pe auth lagane ka sabse bada fayda — **saari services ko auth se free** rakhte hain. Gateway sab handle kar leta hai.

### Architecture:

```
Client Request (with JWT)
        │
        ▼
┌───────────────┐
│  API Gateway  │
│               │
│  1. JWT Extract & Verify  ◄─── JWT Secret / Public Key
│  2. Validate Expiry
│  3. Check Roles/Permissions
│  4. Add User Context to Headers
│     (X-User-Id, X-User-Roles)
│  5. Forward to Backend Service
└───────┬───────┘
        │ (authenticated request with user context headers)
        ▼
┌──────────────────┐
│ Backend Service  │  Trust gateway, no auth needed
│ (reads X-User-Id)│
└──────────────────┘
```

### JWT Authentication Filter (Spring Cloud Gateway):

```java
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    public AuthGlobalFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Public endpoints — no auth needed
        if (path.startsWith("/api/public/") || path.startsWith("/api/auth/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        try {
            // JWT verify karo
            Claims claims = jwtUtil.validateToken(token);

            // User info headers mein add karo
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Roles", claims.get("roles", String.class))
                .header("X-User-Email", claims.get("email", String.class))
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (JwtException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100; // Early execution
    }
}
```

### Role-Based Access at Gateway:

```java
// Specific routes pe role check
@Bean
public RouteLocator rbacRoutes(RouteLocatorBuilder builder) {
    return builder.routes()
        .route("admin-only", r -> r
            .path("/api/admin/**")
            .and()
            .header("X-User-Roles", ".*ADMIN.*")
            .uri("lb://ADMIN-SERVICE")
        )
        .route("user-service", r -> r
            .path("/api/users/profile")
            .and()
            .header("X-User-Roles", ".*USER.*")
            .uri("lb://USER-SERVICE")
        )
        .build();
}
```

**Important Security Rules:**
1. **Gateway pe JWT verify karo**, backend services mein nahi
2. **Internal network** mein services trust gateway ko (headers spoofing se bachne ke liye internal network secure hona chahiye)
3. **Public endpoints** ko gateway pe hi whitelist karo
4. **Token expiry** gateway pe check karo
5. **Rate limiting** auth ke saath combine karo (per-user rate limits)

---

## 6. Rate Limiting

Rate limiting ka matlab — ek user/kitna **requests per second** bhej sakta hai. DDoS attacks aur abuse se bachne ke liye bahut zaroori hai.

### Rate Limiting Strategies:

| Strategy | Kaise Kaam Karta Hai | Use Case |
|----------|---------------------|----------|
| **Fixed Window** | Har minute mein N requests allowed | Simple cases |
| **Sliding Window** | Last N seconds mein M requests | More accurate |
| **Token Bucket** | Tokens ka bucket, har request ek token consume karta hai | Burst traffic handle karne ke liye best |
| **Leaky Bucket** | Fixed rate pe requests process hoti hain | Smooth traffic flow |

### Spring Cloud Gateway + Redis Rate Limiter:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/users/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10    # 10 tokens/sec add hote hain
                redis-rate-limiter.burstCapacity: 20    # Max 20 requests burst
                redis-rate-limiter.requestedTokens: 1   # Har request 1 token leti hai
                key-resolver: "#{@ipKeyResolver}"       # IP-based limiting
```

### Key Resolver (IP-based, User-based, etc.):

```java
@Configuration
public class RateLimiterConfig {

    // IP address ke basis pe rate limit
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            return Mono.just(ip);
        };
    }

    // User ID ke basis pe rate limit (authenticated users)
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            return Mono.just(userId != null ? userId : "anonymous");
        };
    }

    // API key ke basis pe rate limit
    @Bean
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            return Mono.just(apiKey != null ? apiKey : "unknown");
        };
    }
}
```

### Rate Limit Response:

```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Limit: 10
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1713000060
Retry-After: 5

{
  "error": "Rate limit exceeded",
  "message": "Too many requests. Try again later."
}
```

**Best Practices:**
- **Redis** use karo for distributed rate limiting (multiple gateway instances ke liye)
- **Per-user** rate limits lagao, sirf IP-based nahi (shared IPs pe problem hoti hai)
- **Different endpoints** ke liye different limits (login pe strict, read pe loose)
- **Response headers** mein rate limit info bhejo taaki client ko pata ho

---

## 7. SSL Termination

SSL Termination ka matlab — gateway pe **HTTPS ko HTTP** mein convert karna. Backend services ko plain HTTP pe chalaya ja sakta hai.

### How It Works:

```
                    HTTPS (encrypted)              HTTP (plain)
   Client ◄────────────────► API Gateway ◄────────► Backend Services
                   (SSL certificate
                    yahan terminate
                    hota hai)
```

### Flow:

```
1. Client ----[HTTPS]----► Gateway    (encrypted, port 443)
2. Gateway: SSL decrypt karta hai
3. Gateway ----[HTTP]----► Service   (internal network, plain HTTP)
4. Service response HTTP pe aata hai
5. Gateway: SSL encrypt karta hai
6. Client ----[HTTPS]----► Gateway   (encrypted response)
```

### Benefits:
- **Performance** — SSL/TLS handshake sirf gateway pe hota hai, services ko CPU bachta hai
- **Simplified cert management** — Sirf gateway pe certificate manage karna hai
- **Internal communication** faster hota hai (no encryption overhead)

### Nginx SSL Config Example:

```nginx
server {
    listen 443 ssl;
    server_name api.example.com;

    ssl_certificate     /etc/ssl/certs/api.crt;
    ssl_certificate_key /etc/ssl/private/api.key;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    location /api/ {
        proxy_pass http://gateway-internal:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### Spring Cloud Gateway SSL Config:

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: changeit
    key-store-type: PKCS12
    key-alias: mycert
```

**Important:** Internal network **trusted** hona chahiye. Agar untrusted network hai (jaise public cloud VPC), toh **mTLS** (mutual TLS) use karo services ke beech bhi.

---

## 8. Request/Response Transformation

Gateway request aur response ko **modify** kar sakta hai — headers add/remove karna, body transform karna, path rewrite karna, etc.

### Common Transformations:

| Type | Example | Filter |
|------|---------|--------|
| **Add Request Header** | `X-API-Version: v1` add karna | `AddRequestHeader` |
| **Remove Request Header** | Sensitive header hatana | `RemoveRequestHeader` |
| **Add Response Header** | `X-Response-Time` add karna | `AddResponseHeader` |
| **Rewrite Path** | `/v1/users` → `/users` | `RewritePath` |
| **Prefix Path** | `/api` prefix add karna | `PrefixPath` |
| **Modify Body** | JSON → XML convert karna | `ModifyRequestBody` |

### YAML Config Examples:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/v1/users/**
          filters:
            # Header add karna
            - AddRequestHeader=X-API-Version, v1
            - AddRequestHeader=X-Gateway-Source, api-gateway

            # Sensitive header remove karna
            - RemoveRequestHeader=Cookie

            # Path rewrite: /api/v1/users → /users
            - RewritePath=/api/v1/users/(?<segment>.*), /users/${segment}

            # Response mein header add karna
            - AddResponseHeader=X-Response-Source, api-gateway

            # Request body modify karna
            - name: ModifyRequestBody
              args:
                inClass: String
                outClass: String
                rewriteFunction: |
                  (exchange, body) -> {
                    // body mein modification
                    return Mono.just(body.toUpperCase());
                  }
```

### Custom Filter — Response Time Add Karna:

```java
@Component
public class ResponseTimeGlobalFilter implements GlobalFilter {

    private static final String START_TIME_ATTR = "startTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getAttributes().put(START_TIME_ATTR, System.currentTimeMillis());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            Long startTime = exchange.getAttribute(START_TIME_ATTR);
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                exchange.getResponse().getHeaders().add("X-Response-Time-Ms", String.valueOf(duration));
            }
        }));
    }
}
```

### Circuit Breaker Filter with Fallback:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://ORDER-SERVICE
          predicates:
            - Path=/api/orders/**
          filters:
            - name: CircuitBreaker
              args:
                name: orderServiceCircuitBreaker
                fallbackUri: forward:/fallback/orders
```

```java
@RestController
public class FallbackController {

    @GetMapping("/fallback/orders")
    public ResponseEntity<Map<String, String>> orderFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(Map.of(
                "error", "Order Service is temporarily unavailable",
                "message", "Please try again after some time"
            ));
    }
}
```

---

## 9. Circuit Breaker at Gateway

Circuit Breaker pattern gateway pe lagane se — agar koi backend service **down** hai, toh gateway immediately fail fast karega instead of waiting for timeout.

### Circuit Breaker States:

```
            (failures > threshold)
   CLOSED ────────────────────────► OPEN
     ▲                                │
     │                                │ (all requests fail immediately)
     │                                ▼
     │        (test request passes)  HALF-OPEN
     └────────────────────────────────
```

| State | Behavior | Kab Hota Hai |
|-------|----------|-------------|
| **CLOSED** | Normal flow, requests pass through | Starting state, sab theek hai |
| **OPEN** | Requests immediately fail (no call to backend) | Failures threshold cross ho gaya |
| **HALF-OPEN** | Ek test request bhej ke check karta hai | Wait duration ke baad |

### Resilience4j + Spring Cloud Gateway:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: user-service
          uri: lb://USER-SERVICE
          predicates:
            - Path=/api/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: userCircuitBreaker
                fallbackUri: forward:/fallback/users

resilience4j:
  circuitbreaker:
    instances:
      userCircuitBreaker:
        slidingWindowSize: 10          # Last 10 requests dekho
        failureRateThreshold: 50       # 50%+ fail ho toh OPEN
        waitDurationInOpenState: 30s   # 30 sec baad HALF-OPEN
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

### Monitoring Circuit Breaker:

```java
@RestController
@RequestMapping("/actuator/circuitbreakers")
public class CircuitBreakerMonitorController {

    private final CircuitBreakerRegistry registry;

    @GetMapping
    public Map<String, String> getAllCircuitBreakers() {
        Map<String, String> status = new HashMap<>();
        registry.getAllCircuitBreakers().forEach(cb -> {
            status.put(cb.getName(), cb.getState().toString());
        });
        return status;
    }
}
```

### Complete Gateway Architecture with Circuit Breaker:

```
                    ┌────────────────────────────────────┐
                    │         API Gateway                │
                    │                                    │
   Client ────────► │  1. Auth (JWT Verify)              │
                    │  2. Rate Limit Check               │
                    │  3. Circuit Breaker Check          │
                    │  4. Load Balance                   │
                    │  5. Add Headers & Forward          │
                    └──────────┬─────────────────────────┘
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
     ┌────────────┐   ┌────────────┐   ┌────────────┐
     │   User     │   │   Order    │   │  Payment   │
     │  Service   │   │  Service   │   │  Service   │
     │  (CLOSED)  │   │  (OPEN)    │   │  (CLOSED)  │
     └────────────┘   └────────────┘   └────────────┘
                                           │
                        Fallback Response ◄┘
```

---

## 10. Popular API Gateways Comparison

| Feature | **Spring Cloud Gateway** | **Kong** | **Nginx** | **Envoy** | **APISIX** |
|---------|-------------------------|----------|-----------|-----------|------------|
| **Language** | Java/Reactive | Lua/Nginx | C | C++ | Lua/Nginx |
| **Protocol** | HTTP/2, WebSocket | HTTP, gRPC | HTTP, TCP | HTTP/2, gRPC, TCP | HTTP, gRPC |
| **Service Discovery** | Eureka, Consul, K8s | Consul, K8s | Manual | Consul, K8s, etcd | Consul, etcd, Nacos |
| **Rate Limiting** | Redis-based | Plugin-based | Module-based | Built-in | Plugin-based |
| **Auth** | Custom filters | JWT, OAuth2, OIDC plugins | Auth module | ExtAuth | JWT, OIDC plugins |
| **Hot Reload** | No (restart needed) | Yes (no restart) | Yes | Yes | Yes |
| **Performance** | Good (reactive) | Excellent | Best | Excellent | Excellent |
| **Ease of Use** | Easy (Java devs) | Medium | Medium | Hard | Medium |
| **Best For** | Spring Boot ecosystem | Multi-language | Simple proxy | Service mesh | Cloud-native |

### When to Use What:

- **Spring Cloud Gateway** → Agar poora stack Spring Boot hai
- **Kong** → Multi-language microservices, plugin ecosystem chahiye
- **Nginx** → Simple, high-performance reverse proxy
- **Envoy** → Service mesh (Istio) ke saath
- **APISIX** → Cloud-native, dynamic config chahiye

---

## 11. Best Practices

### 1. Gateway ko **Thin** Rakho
```
Gateway mein sirf cross-cutting concerns rakho:
  - Auth
  - Rate Limiting
  - Routing
  - Logging

Business logic GATEWAY mein MAT likho!
```

### 2. Fallback Responses Hamesha Ready Rakho
```java
// Har service route ke saath fallback define karo
filters:
  - name: CircuitBreaker
    args:
      fallbackUri: forward:/fallback/{service-name}
```

### 3. Health Check Endpoint
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, gateway
```

### 4. Distributed Tracing Enable Karo
```yaml
spring:
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 1.0  # 100% sampling (dev mein)
```

### 5. Security Headers Add Karo
```java
@Bean
public GlobalFilter securityHeadersFilter() {
    return (exchange, chain) -> chain.filter(exchange).then(Mono.fromRunnable(() -> {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-Frame-Options", "DENY");
        response.getHeaders().add("X-Content-Type-Options", "nosniff");
        response.getHeaders().add("X-XSS-Protection", "1; mode=block");
        response.getHeaders().add("Strict-Transport-Security", "max-age=31536000");
    }));
}
```

### 6. CORS Gateway Pe Handle Karo
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "https://myapp.com"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
            allowedHeaders: "*"
            allowCredentials: true
```

### 7. Gateway Clustering
```
Production mein hamesha multiple gateway instances chalaao
              ┌─────────────┐
   Client ───►│ Load Balancer│
              │  (AWS ALB /  │
              │   Nginx)     │
              └──────┬───────┘
                     │
          ┌──────────┼──────────┐
          ▼          ▼          ▼
     ┌────────┐ ┌────────┐ ┌────────┐
     │ GW- 1  │ │ GW- 2  │ │ GW- 3  │
     └────────┘ └────────┘ └────────┘
```

---

## 12. Summary

| Concept | Key Point |
|---------|-----------|
| **Single Entry Point** | Client ko sirf gateway ka URL pata hota hai |
| **Request Routing** | Path-based routing, predicates se decide hota hai |
| **Load Balancing** | Round Robin, Least Connections — Spring Cloud LoadBalancer |
| **Auth at Gateway** | JWT verify gateway pe, user context headers mein pass karo |
| **Rate Limiting** | Token Bucket algorithm, Redis-based distributed limiting |
| **SSL Termination** | Gateway pe HTTPS → HTTP, backend ko plain HTTP |
| **Request/Response Transformation** | Headers add/remove, path rewrite, body modification |
| **Circuit Breaker** | Resilience4j — CLOSED → OPEN → HALF-OPEN states |

**Yaad Rakho:**
- API Gateway **facade** hai — business logic nahi
- Gateway **single point of failure** ban sakta hai — hamesha cluster karo
- Har feature (auth, rate limit, circuit breaker) gateway pe lagana **DRY principle** follow karta hai
- **Kong/APISIX** bhi consider karo agar non-Java ecosystem hai

---

> **Next:** [03 - Service Communication](./03-Service-Communication.md) — Microservices aapas mein kaise baat karte hain? REST, gRPC, Kafka, Feign, WebClient, Service Mesh — sab detail mein.
