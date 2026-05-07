# Spring Cloud

Distributed systems mein **Spring Cloud** ke 4 core building blocks — config server, API gateway, declarative HTTP clients (Feign), aur client-side load balancing. Sab Hinglish mein, deep code examples + production patterns ke saath. Companion to **Microservices Architecture** folder.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Spring Cloud Config | [01-Spring-Cloud-Config.md](./01-Spring-Cloud-Config.md) | Not Started |
| 2 | Spring Cloud Gateway | [02-Spring-Cloud-Gateway.md](./02-Spring-Cloud-Gateway.md) | Not Started |
| 3 | OpenFeign | [03-OpenFeign.md](./03-OpenFeign.md) | Not Started |
| 4 | Spring Cloud LoadBalancer | [04-Spring-Cloud-LoadBalancer.md](./04-Spring-Cloud-LoadBalancer.md) | Not Started |

---

## What's Inside Each File?

### [01 — Spring Cloud Config](./01-Spring-Cloud-Config.md)
Centralized config server architecture, **Git backend** (repo structure, profile-specific files, multi-repo patterns), config client setup, **`bootstrap.yml` vs `spring.config.import`** (modern way), profile activation + groups, **encryption** (symmetric `{cipher}` + asymmetric JKS keystore + key rotation), **`@RefreshScope`** + `/actuator/refresh`, `@ConfigurationProperties` auto-refresh, **Spring Cloud Bus** (RabbitMQ/Kafka broadcast refresh + Git webhook), other backends (Vault, JDBC, native, composite), HA setup, security hardening.

### [02 — Spring Cloud Gateway](./02-Spring-Cloud-Gateway.md)
Reactive gateway on **WebFlux + Reactor + Netty**, route configuration (YAML + Java DSL), **predicates** (Path/Method/Host/Header/Query/Cookie/Time/RemoteAddr/**Weight** for canary), **filters** (StripPrefix, RewritePath, AddRequestHeader, RemoveRequestHeader, Retry, RequestSize), **CircuitBreaker filter** with Resilience4j + fallback endpoint, **RequestRateLimiter** with Redis token bucket + key resolvers (user/IP/tier-based), custom `GatewayFilter` and `GlobalFilter`, service discovery integration (`lb://`), Spring Security WebFlux + JWT validation at gateway, CORS, observability (gateway endpoints, Micrometer metrics, distributed tracing).

### [03 — OpenFeign](./03-OpenFeign.md)
Declarative HTTP client — interface = REST API, `@FeignClient` attributes (name, url, path, contextId, primary, qualifiers, configuration, fallback, fallbackFactory), method signatures with Spring MVC annotations, `@PathVariable`/`@RequestParam`/`@RequestBody`/`@RequestHeader`/`@SpringQueryMap`/multipart/form-encoded, configuration (per-client + global YAML + programmatic), **RequestInterceptor** (auth forward, service key, OAuth2 client credentials, correlation ID, tenant), **ErrorDecoder** (HTTP errors → typed exceptions, RetryableException for retry), logging levels (NONE/BASIC/HEADERS/FULL), timeouts + Retryer + Resilience4j Retry, service discovery (`lb://`), **circuit breaker fallback** + FallbackFactory with cause access, testing (mock + WireMock + contract).

### [04 — Spring Cloud LoadBalancer](./04-Spring-Cloud-LoadBalancer.md)
Client-side load balancer (Ribbon successor), `@LoadBalanced` on RestTemplate / WebClient.Builder, integration with Feign, service discovery integration (Eureka, Consul, Nacos, **Kubernetes**), strategies (**RoundRobin** default, Random, custom), **caching** (`CachingServiceInstanceListSupplier`, TTL config), custom `ServiceInstanceListSupplier` (filtering by metadata), **active health checks** (`/actuator/health` probes), zone-based + hint-based routing for multi-AZ, sticky session preferences, retries via Spring Retry (max-retries-on-same vs next instance, retryable status codes).

---

## Recommended Learning Order

```
1. Spring Cloud Config (01)        ← centralized configuration
2. Spring Cloud LoadBalancer (04)  ← client-side LB foundation
3. OpenFeign (03)                  ← service-to-service calls (uses LB)
4. Spring Cloud Gateway (02)       ← edge gateway (top of stack)
```

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Centralize app config in Git | 01 | Git Backend |
| Refresh config without restart | 01 | @RefreshScope |
| Encrypt DB password in config | 01 | Encryption |
| Broadcast refresh to all services | 01 | Spring Cloud Bus |
| API Gateway pattern in Spring | 02 | Setup + Route Configuration |
| Path-based routing | 02 | Predicates |
| Rate limit at gateway | 02 | Rate Limiting Filter |
| Circuit breaker at gateway | 02 | CircuitBreaker Filter |
| Canary deployment (80/20) | 02 | Weight Predicate |
| JWT validation at gateway | 02 | Security & Authentication |
| Replace RestTemplate with declarative | 03 | Defining a Feign Client |
| Forward auth header downstream | 03 | Request Interceptors |
| Map 404 → custom exception | 03 | Error Decoder |
| Feign + circuit breaker fallback | 03 | Resilience4j Fallback |
| Round-robin between instances | 04 | Round Robin |
| Same-zone preference | 04 | Zone-Based Routing |
| Active health checks | 04 | Health Checks |
| Caching service instances | 04 | Caching |

---

## Stack Overview

```
┌────────────────────────────────────────────────────────────────┐
│                           Clients                               │
└────────────────────────────────┬────────────────────────────────┘
                                 │
                                 ▼
┌────────────────────────────────────────────────────────────────┐
│              Spring Cloud Gateway (file 02)                     │
│   • Routes • Predicates • Filters                               │
│   • Auth • Rate Limit • Circuit Breaker                         │
└────────────────────────────────┬────────────────────────────────┘
                                 │
            ┌────────────────────┼────────────────────┐
            │                    │                    │
            ▼                    ▼                    ▼
   ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
   │  Order Service  │  │ Payment Service │  │  User Service   │
   │                 │  │                 │  │                 │
   │  OpenFeign (03) │  │  OpenFeign      │  │  OpenFeign      │
   │  + LoadBalancer │  │  + LoadBalancer │  │  + LoadBalancer │
   │  (file 04)      │  │  (04)           │  │  (04)           │
   └────────┬────────┘  └────────┬────────┘  └────────┬────────┘
            │                    │                    │
            └────────────────────┼────────────────────┘
                                 │
                                 ▼
                   ┌──────────────────────────┐
                   │  Spring Cloud Config     │
                   │  Server (file 01)        │
                   │  ─────────────────────   │
                   │  Git as source of truth  │
                   │  Bus broadcasts refresh  │
                   └──────────────────────────┘
                                 │
                                 ▼
                       ┌──────────────────┐
                       │   Discovery       │
                       │  (Eureka/Consul/  │
                       │   Nacos/K8s)      │
                       └──────────────────┘
```

---

## Companion Folders

- [Microservices Architecture](../Microservices%20Architecture/) — patterns this folder implements (API Gateway, Service Discovery, Resilience, etc.)
- [API Design & Architecture](../../backend-skills/API-Design-&-Architecture/) — REST design that Feign clients consume
- [Networking & Protocols](../../backend-skills/Networking-&-Protocols/) — load balancing fundamentals (file 04 there)
- [Security Best Practices](../../backend-skills/Security-Best-Practices/) — API security, secrets management
- [DevOps & CI/CD](../../backend-skills/DevOps-&-CI-CD/) — environment management ties to Config Server

---

## Tools / Libraries Reference

### Core
- **spring-cloud-config-server** / **spring-cloud-starter-config**
- **spring-cloud-starter-gateway**
- **spring-cloud-starter-openfeign**
- **spring-cloud-starter-loadbalancer**

### Discovery
- **spring-cloud-starter-netflix-eureka-server / -client**
- **spring-cloud-starter-consul-discovery**
- **spring-cloud-starter-alibaba-nacos-discovery**
- **spring-cloud-starter-kubernetes-client-loadbalancer**

### Resilience
- **spring-cloud-starter-circuitbreaker-resilience4j** (Servlet stack)
- **spring-cloud-starter-circuitbreaker-reactor-resilience4j** (Reactive)

### Bus (Config Refresh Broadcast)
- **spring-cloud-starter-bus-amqp** (RabbitMQ)
- **spring-cloud-starter-bus-kafka** (Kafka)

### Testing
- **WireMock** — HTTP mocking
- **Spring Cloud Contract** — consumer-driven contracts

---

## Status Tracker

```
[ ] 01 — Spring Cloud Config
[ ] 02 — Spring Cloud Gateway
[ ] 03 — OpenFeign
[ ] 04 — Spring Cloud LoadBalancer
```

Topic complete hone par file header aur is README dono mein status update kar lena.

> Spring Cloud is a **toolbox** — yeh patterns combine ho kar production-grade microservices stack banate hain. Combine with `Microservices Architecture` folder for complete picture.
