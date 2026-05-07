## What are Microservices?

**Matlab:** Ek large application ko chhote-chhote independent services mein tod dena, jahan har service ek specific business capability ko handle karta hai.


### Monolith vs Microservices

```
MONOLITHIC ARCHITECTURE
┌─────────────────────────────────────────────┐
│              Monolith Application             │
│  ┌──────────┬──────────┬──────────────────┐  │
│  │  User    │  Order   │   Payment        │  │
│  │  Module  │  Module  │   Module         │  │
│  ├──────────┴──────────┴──────────────────┤  │
│  │         Single Database                 │  │
│  └─────────────────────────────────────────┘  │
│                                                 │
│  ❌ Ek module mein bug = pura app down         │
│  ❌ Ek team doosri team ka code block karti hai │
│  ❌ Scaling = pura app scale karo               │
└─────────────────────────────────────────────────┘


MICROSERVICES ARCHITECTURE
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway                             │
└─────────────┬──────────────┬──────────────┬─────────────────┘
              │              │              │
     ┌────────▼────┐ ┌──────▼──────┐ ┌─────▼──────┐
     │   User      │ │   Order     │ │  Payment   │
     │   Service   │ │   Service   │ │  Service   │
     │             │ │             │ │            │
     │ ┌─────────┐ │ │ ┌─────────┐ │ │ ┌────────┐ │
     │ │User DB  │ │ │ │Order DB │ │ │ │Pay DB  │ │
     │ └─────────┘ │ │ └─────────┘ │ │ └────────┘ │
     └─────────────┘ └─────────────┘ └────────────┘

  ✅ Har service independent deploy hoti hai
  ✅ Ek service fail ho baaki kaam karti hain
  ✅ Har service alag tech stack use kar sakti hai
  ✅ Teams independently kaam kar sakti hain
```

### Microservices Key Characteristics

| Characteristic | Description |
|----------------|-------------|
| **Single Responsibility** | Har service ek hi kaam karti hai — aur woh acche se |
| **Loose Coupling** | Services ek doosre se loosely coupled hoti hain |
| **Independent Deploy** | Ek service ko bina baaki services ko touch kiye deploy kar sakte ho |
| **Decentralized Data** | Har service apna data manage karti hai — shared database nahi |
| **Failure Isolation** | Ek service ka failure doosri services ko affect nahi karta |
| **Polyglot** | Har service apni language/database choose kar sakti hai |


## Single Responsibility Principle

**Matlab:** Ek service ka ek hi reason to change hona chahiye. Ek service = ek business capability.

### SRP in Monolith (BAD)
```java
// ❌ GOD CLASS — sab kuch ek hi class mein
// Problem:
// - User feature change karo toh Order module ka test bhi chahiye
// - Ek bug fix = pura app redeploy
// - Multiple teams ek hi file mein conflict
```


### SRP in Microservices (GOOD)

```
┌──────────────────────────────────────────────────────┐
│              Single Responsibility Model               │
├──────────────┬──────────────┬─────────────────────────┤
│ User Service │Order Service │   Notification Service   │
├──────────────┼──────────────┼─────────────────────────┤
│ • Register   │ • Create     │ • Send Email             │
│ • Login      │   Order      │ • Send SMS               │
│ • Profile    │ • Cancel     │ • Push Notification      │
│ • Auth       │ • Track      │ • Webhook                │
│ • Password   │ • History    │ • Template Management    │
└──────────────┴──────────────┴─────────────────────────┘

Har service ka EK hi reason to change:
- User Service → User management change hone pe
- Order Service → Order logic change hone pe
- Notification Service → Notification channel change hone pe
```

### SRP Best Practices

| Rule | Description |
|------|-------------|
| **One business capability** | Ek service = ek domain (User, Order, Payment, etc.) |
| **One reason to change** | Agar do alag reasons se change ho rahi hai, toh todo |
| **Small team ownership** | Ek service = 1-2 developers (pizza rule: 2 pizza se zyada log = service badi hai) |
| **Cohesive functionality** | Service ke andar ke methods related hone chahiye |
| **API boundaries clear** | Har service ka API clearly define karo — kya expose karna hai, kya nahi |


## Loose Coupling, High Cohesion

### Loose Coupling

**Matlab:** Services ek doosre se minimally depend karein. Ek service change karo toh doosri services ko change na karna pade.

### Tight Coupling (BAD)

```java
// ❌ Tight Coupling — Service A ko Service B ki internal details 

// Problem:
// - UserRepository change hua → OrderService break hoga
// - EmailSender ka implementation change hua → OrderService update karna padega
// - Test karna muskil — mock bahut saare dependencies
```

### Loose Coupling (GOOD)

```java
// ✅ Loose Coupling — interfaces aur events se communication
```

### High Cohesion

**Matlab:** Ek service ke andar ke elements (classes, methods) closely related hone chahiye.

```
HIGH COHESION (✅)                    LOW COHESION (❌)
┌─────────────────────┐              ┌─────────────────────────┐
│   Order Service     │              │   Utility Service       │
│                     │              │                         │
│  OrderController    │              │  sendEmail()            │
│  OrderService       │              │  calculateTax()         │
│  OrderRepository    │              │  generatePDF()          │
│  OrderValidator     │              │  resizeImage()          │
│  OrderMapper        │              │  sendSMS()              │
│                     │              │  parseCSV()             │
│  Sab related ✅     │              │  unrelated methods ❌    │
└─────────────────────┘              └─────────────────────────┘

### Coupling vs Cohesion Comparison

| Aspect | Tight Coupling | Loose Coupling |
|--------|---------------|----------------|
| **Dependency** | Direct class/database access | Interface/HTTP/Event-based |
| **Change Impact** | Ek change = multiple services update | Ek change = sirf wohi service |
| **Testing** | Muskil — bahut saare mocks | Aasaan — contract testing |
| **Deployment** | Saath mein deploy karna padega | Independent deploy |
| **Technology Lock** | Same tech stack forced | Different tech stack possible |

| Aspect | High Cohesion | Low Cohesion |
|--------|--------------|--------------|
| **Focus** | Ek domain pe focused | Multiple unrelated domains |
| **Understandability** | Code samajhna aasaan | Code samajhna mushkil |
| **Maintainability** | Easy to maintain | Hard to maintain |
| **Reusability** | Service reusable | Service specific, not reusable |

---
```

























# 02 - API Gateway Pattern

> API Gateway microservices architecture ka **single entry point** hota hai — jaise building ka security guard, sab requests pehle uske paas aati hain, phir woh decide karta hai kis service ko bhejna hai.

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
# NiftyLoadBalancer (Spring Cloud 2020+)
# Ribbon is DEPRECATED — ab Spring Cloud LoadBalancer use hota hai
```

### Custom Load Balancer Configuration:

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


### Role-Based Access at Gateway:

   
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

### Key Resolver (IP-based, User-based, etc.):

```java
    // IP address ke basis pe rate limit
    // User ID ke basis pe rate limit (authenticated users)
    // API key ke basis pe rate limit
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


### Spring Cloud Gateway SSL Config:


**Important:** Internal network **trusted** hona chahiye. Agar untrusted network hai (jaise public cloud VPC), toh **mTLS** (mutual TLS) use karo services ke beech bhi.


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

### Custom Filter — Response Time Add Karna:

### Circuit Breaker Filter with Fallback:

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

### Monitoring Circuit Breaker:


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
### 4. Distributed Tracing Enable Karo
### 5. Security Headers Add Karo
### 6. CORS Gateway Pe Handle Karo
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




# 03 - Service Communication

> Microservices architecture mein services ko **aapas mein baat karni padti hai** — data share karna, commands bhejna, events publish karna. Yeh communication do tarike se hota hai: **Synchronous** (real-time, blocking) aur **Asynchronous** (message-based, non-blocking).

---

## Table of Contents

1. [Overview: Service Communication Patterns](#1-overview-service-communication-patterns)
2. [Synchronous Communication](#2-synchronous-communication)
3. [REST Communication](#3-rest-communication)
4. [gRPC Communication](#4-grpc-communication)
5. [Asynchronous Communication](#5-asynchronous-communication)
6. [Message Queues — Kafka](#6-message-queues--kafka)
7. [Message Queues — RabbitMQ](#7-message-queues--rabbitmq)
8. [OpenFeign — Declarative REST Client](#8-openfeign--declarative-rest-client)
9. [RestTemplate (Legacy)](#9-resttemplate-legacy)
10. [WebClient (Reactive, Recommended)](#10-webclient-reactive-recommended)
11. [Service Mesh Basics (Istio)](#11-service-mesh-basics-istio)
12. [Comparison & When to Use What](#12-comparison--when-to-use-what)
13. [Best Practices](#13-best-practices)
14. [Summary](#14-summary)

---

## 1. Overview: Service Communication Patterns

```
┌─────────────────────────────────────────────────────────────────┐
│                  Service Communication                          │
├──────────────────────────┬──────────────────────────────────────┤
│    SYNCHRONOUS           │      ASYNCHRONOUS                    │
│    (Blocking)            │      (Non-Blocking)                  │
│                          │                                      │
│  ┌────────────────────┐  │  ┌────────────────────────────────┐ │
│  │ REST (HTTP/JSON)   │  │  │ Message Queues                 │ │
│  │ gRPC (Protobuf)    │  │  │   - Apache Kafka               │ │
│  │ OpenFeign          │  │  │   - RabbitMQ                   │ │
│  │ RestTemplate       │  │  │   - AWS SQS                    │ │
│  │ WebClient          │  │  │ Event Streaming                │ │
│  └────────────────────┘  │  │   - Kafka Streams              │ │
│                          │  │   - Redis Pub/Sub              │ │
│  Client waits for        │  │  Producer ─► Queue ─► Consumer │ │
│  response                │  │  (Fire and forget / callback)  │ │
└──────────────────────────┴──────────────────────────────────────┘
```

### Sync vs Async at a Glance:

| Aspect | Synchronous | Asynchronous |
|--------|-------------|--------------|
| **Blocking** | Haan, client wait karta hai | Nahi, fire-and-forget |
| **Response** | Immediate chahiye | Later mil sakta hai |
| **Coupling** | Tight (both services up hone chahiye) | Loose (queue buffer karta hai) |
| **Use Case** | Query, real-time data | Events, notifications, batch processing |
| **Example** | Order service → Inventory check | Order placed → Email notification |

---

## 2. Synchronous Communication

Synchronous mein **caller wait karta hai** jab tak callee response nahi de deta. Jaise phone call — dono line pe hone chahiye.

```
Service A ──[request]──► Service B
           ◄──[response]──
           (wait karta raha)
```

### When to Use Sync:
- User ko **immediate response** chahiye (e.g., product details)
- **Query operations** jahan data abhi chahiye
- **Transactional operations** jahan confirmation chahiye

### When NOT to Use Sync:
- **Fire-and-forget** operations (e.g., email bhejna)
- **Long-running** processes (e.g., report generation)
- **High traffic** scenarios jahan cascading failures ho sakte hain

---

## 3. REST Communication

REST (Representational State Transfer) sabse common tarika hai microservices ke beech communicate karne ka. HTTP + JSON use hota hai.

### REST Architecture:

```
┌──────────────┐    GET /api/products/123     ┌──────────────┐
│   Order      │ ──────────────────────────► │   Product    │
│  Service     │                             │   Service    │
│              │ ◄────────────────────────── │              │
│              │   {"id":123,"name":"Phone"} │              │
└──────────────┘                             └──────────────┘
```

### Spring Boot REST Client Options:

| Option | Status | Type | Recommendation |
|--------|--------|------|----------------|
| **RestTemplate** | Legacy (Maintenance Mode) | Blocking | Don't use for new code |
| **WebClient** | Active | Reactive/Non-blocking | **RECOMMENDED** |
| **OpenFeign** | Active | Declarative | **RECOMMENDED** for simplicity |
| **HTTP Client (Java 11+)** | Active | Blocking | Good for simple cases |

---

## 4. gRPC Communication

gRPC Google ka high-performance RPC framework hai. **Protobuf** (binary format) use karta hai, JSON se 5-10x faster hai.

### gRPC vs REST:

| Feature | gRPC | REST |
|---------|------|------|
| **Format** | Protobuf (binary) | JSON (text) |
| **Size** | Chhoti (binary) | Badi (text) |
| **Speed** | 5-10x faster | Slower |
| **Protocol** | HTTP/2 (multiplexing) | HTTP/1.1 or HTTP/2 |
| **Streaming** | Built-in (bidirectional) | Limited |
| **Code Generation** | .proto file se auto-generated | Manual / Swagger |
| **Browser Support** | gRPC-Web needed (limited) | Native support |
| **Debugging** | Hard (binary data) | Easy (readable JSON) |

### When to Use gRPC:
- **Internal service-to-service** communication
- **High throughput** chahiye (low latency critical)
- **Streaming** chahiye (real-time data)
- **Polyglot** environment (different languages)

### When NOT to Use gRPC:
- Public API (browsers support limited hai)
- Simple CRUD apps (REST kaafi hai)
- Jab human-readable format chahiye

### gRPC Setup:

**1. Proto File Define Karo:**

**2. Server Implementation:**

```java
@GrpcService
public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responseObserver) {
        UserEntity entity = userRepository.findById(request.getId())
            .orElseThrow(() -> new StatusRuntimeException(Status.NOT_FOUND));

        User user = User.newBuilder()
            .setId(entity.getId())
            .setName(entity.getName())
            .setEmail(entity.getEmail())
            .build();

        GetUserResponse response = GetUserResponse.newBuilder()
            .setUser(user)
            .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // Streaming: multiple users bhejta hai
    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<User> responseObserver) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        List<UserEntity> users = userRepository.findAll(pageable).getContent();

        for (UserEntity entity : users) {
            User user = User.newBuilder()
                .setId(entity.getId())
                .setName(entity.getName())
                .setEmail(entity.getEmail())
                .build();
            responseObserver.onNext(user);
        }
        responseObserver.onCompleted();
    }
}
```

**3. Client Implementation:**

```java
@Service
public class GrpcUserClient {

    private final UserServiceGrpc.UserServiceStub stub;

    public GrpcUserClient(@GrpcClient("user-service") UserServiceGrpc.UserServiceStub stub) {
        this.stub = stub;
    }

    public User getUser(long id) {
        CompletableFuture<User> future = new CompletableFuture<>();

        stub.getUser(GetUserRequest.newBuilder().setId(id).build(),
            new StreamObserver<>() {
                @Override
                public void onNext(GetUserResponse value) {
                    future.complete(value.getUser());
                }
                @Override
                public void onError(Throwable t) {
                    future.completeExceptionally(t);
                }
                @Override
                public void onCompleted() {
                    // Already completed in onNext
                }
            }
        );

        return future.join();
    }
}
```

**Dependencies (pom.xml):**

```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-client-spring-boot-starter</artifactId>
    <version>3.1.0.RELEASE</version>
</dependency>
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-server-spring-boot-starter</artifactId>
    <version>3.1.0.RELEASE</version>
</dependency>
```

---

## 5. Asynchronous Communication

Asynchronous mein caller request bhej ke **wait nahi karta**. Message queue/stream mein daal deta hai, consumer apne time pe process karta hai.

```
┌────────────┐    Message     ┌──────────┐    Message     ┌────────────┐
│  Producer  │ ─────────────► │  Queue   │ ─────────────► │  Consumer  │
│ (Service A)│                │(Kafka/   │                │ (Service B)│
│            │                │ RabbitMQ)│                │            │
└────────────┘                └──────────┘                └────────────┘

   Fire & Forget                                          Process Later
   (Don't wait)                                           (Own pace)
```

### Async Communication Types:

| Type | Description | Example |
|------|-------------|---------|
| **Message Queue** | Point-to-point, ek consumer ko milta hai | Order process karna |
| **Pub/Sub** | Multiple subscribers ko milta hai | Event broadcasting |
| **Event Streaming** | Ordered, persistent events | Audit log, CDC |

### Benefits of Async:
- **Decoupling** — Producer ko consumer ki availability ki tension nahi
- **Scalability** — Multiple consumers, load distribute hota hai
- **Resilience** — Consumer down ho, messages queue mein safe rehte hain
- **Load Leveling** — Traffic spikes queue absorb kar leta hai
- **Different Speeds** — Producer fast bhej sakta hai, consumer slow process kare

---

## 6. Message Queues — Kafka

Apache Kafka ek **distributed event streaming platform** hai. High throughput, fault-tolerant, aur real-time data pipelines ke liye best hai.

### Kafka Architecture:

```
┌─────────────┐
│  Producer   │  (Order Service)
│  (Service)  │
└──────┬──────┘
       │ publish
       ▼
┌──────────────────────────────────────────────────────────┐
│                    Kafka Cluster                         │
│                                                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐               │
│  │ Broker 1 │  │ Broker 2 │  │ Broker 3 │               │
│  │          │  │          │  │          │               │
│  │ Topic:   │  │ Topic:   │  │ Topic:   │               │
│  │ orders   │  │ orders   │  │ orders   │               │
│  │ [P0][P1] │  │ [P2][P3] │  │ [P4][P5] │               │
│  └──────────┘  └──────────┘  └──────────┘               │
└──────────────────────┬───────────────────────────────────┘
                       │ subscribe
                       ▼
              ┌────────────────┐
              │   Consumer     │
              │ (Email Service)│
              └────────────────┘
```

### Kafka Key Concepts:

| Concept | Description |
|---------|-------------|
| **Topic** | Messages ka category (e.g., `orders`, `payments`) |
| **Partition** | Topic ko split kiya gaya — parallel processing ke liye |
| **Producer** | Message publish karne wala service |
| **Consumer** | Message subscribe karne wala service |
| **Consumer Group** | Multiple consumers — load balance hota hai |
| **Offset** | Consumer ne kahan tak read kiya — tracking ke liye |
| **Broker** | Kafka server/instance |

### Spring Boot + Kafka Integration:

**1. Dependency:**

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

**2. Producer (Message Bhejna):**

```java
@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendOrderCreatedEvent(OrderEvent event) {
        // Key = orderId (same key wahi partition mein jayega — ordering guaranteed)
        kafkaTemplate.send("order-events", event.getOrderId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    System.out.println("Message sent: " + result.getRecordMetadata());
                } else {
                    System.err.println("Failed to send: " + ex.getMessage());
                }
            });
    }

    // With callback
    public CompletableFuture<SendResult<String, OrderEvent>> sendWithCallback(OrderEvent event) {
        return kafkaTemplate.send("order-events", event.getOrderId().toString(), event)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    System.err.println("Send failed for order: " + event.getOrderId());
                    // Retry logic ya dead letter queue mein bhejo
                }
            });
    }
}
```

**3. Consumer (Message Receive Karna):**

```java
@Component
public class OrderEventConsumer {

    @KafkaListener(
        topics = "order-events",
        groupId = "email-service-group",
        concurrency = "3"  // 3 threads se parallel consume
    )
    public void handleOrderEvent(OrderEvent event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {

        System.out.printf("Received event from %s-%d@%d: %s%n",
            topic, partition, offset, event);

        switch (event.getType()) {
            case ORDER_CREATED:
                sendOrderConfirmationEmail(event);
                break;
            case ORDER_SHIPPED:
                sendShippingEmail(event);
                break;
            case ORDER_CANCELLED:
                sendCancellationEmail(event);
                break;
        }
    }

    private void sendOrderConfirmationEmail(OrderEvent event) {
        // Email bhejne ka logic
    }
}
```

**4. Application Config:**

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                    # All brokers acknowledge karein
      retries: 3                   # Retry count
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest  # Start se padho
      properties:
        spring.json.trusted.packages: "com.example.*"
      enable-auto-commit: false    # Manual commit for exactly-once
    listener:
      ack-mode: record             # Har record process hone pe commit
```

**5. Kafka Streams (Stream Processing):**

```java
@Bean
public KStream<String, OrderEvent> processOrderEvents(StreamsBuilder builder) {
    KStream<String, OrderEvent> stream = builder.stream("order-events");

    // Filter: Sirf high-value orders
    KStream<String, OrderEvent> highValueOrders = stream
        .filter((key, event) -> event.getAmount() > 10000);

    // KSI aggregate: Per-user total
    KTable<String, Long> userOrderTotal = stream
        .groupBy((key, event) -> event.getUserId())
        .count();

    highValueOrders.to("high-value-orders");

    return stream;
}
```

### Kafka Best Practices:
- **Key-based partitioning** — same order ke messages same partition mein jayenge (ordering)
- **Consumer groups** — scalability ke liye multiple consumers
- **Dead Letter Topic** — failed messages alag topic mein bhejo
- **Idempotent Producer** — duplicate messages se bacho
- **Compacted Topics** — latest state rakhne ke liye

---

## 7. Message Queues — RabbitMQ

RabbitMQ ek traditional **message broker** hai. AMQP protocol use karta hai. Kafka se simpler hai aur advanced routing features deta hai.

### RabbitMQ Architecture:

```
┌──────────┐     ┌──────────┐     ┌──────────┐     ┌──────────┐
│ Producer │────►│ Exchange │────►│  Queue   │────►│ Consumer │
│          │     │ (routes) │     │(stores)  │     │          │
└──────────┘     └──────────┘     └──────────┘     └──────────┘
```

### Exchange Types:

```
1. Direct Exchange:    Exact routing key match
   "orders.created" ──► "orders.created" queue

2. Topic Exchange:     Pattern match with wildcards
   "order.*.created" ──► "order.usa.created", "order.india.created"

3. Fanout Exchange:    All queues ko broadcast
   "order-events" ──► Email Queue + SMS Queue + Analytics Queue

4. Headers Exchange:   Headers ke basis pe route
   (rarely used)
```

### Kafka vs RabbitMQ:

| Feature | Kafka | RabbitMQ |
|---------|-------|----------|
| **Model** | Event Streaming (log-based) | Message Broker (queue-based) |
| **Throughput** | Very High (millions/sec) | High (thousands/sec) |
| **Message Retention** | Days/weeks (configurable) | Till consumed (usually) |
| **Ordering** | Partition-level guaranteed | Queue-level guaranteed |
| **Routing** | Topic-based (simple) | Advanced (direct, topic, fanout, headers) |
| **Replay** | Haan (offset se replay kar sakte hain) | Nahi (consume ho gaya toh gaya) |
| **Best For** | Event streaming, analytics, CDC | Task queues, RPC, complex routing |

### Spring Boot + RabbitMQ:

**1. Dependency:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

**2. Config:**

```java
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_QUEUE = "order-queue";
    public static final String ORDER_EXCHANGE = "order-exchange";
    public static final String ORDER_ROUTING_KEY = "order.created";

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE).build();
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Binding binding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue)
            .to(orderExchange)
            .with(ORDER_ROUTING_KEY);
    }
}
```

**3. Producer:**

```java
@Service
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderCreatedMessage(OrderEvent event) {
        rabbitTemplate.convertAndSend(
            ORDER_EXCHANGE,
            ORDER_ROUTING_KEY,
            event,
            message -> {
                message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                message.getMessageProperties().setHeader("retry-count", 0);
                return message;
            }
        );
    }
}
```

**4. Consumer:**

```java
@Component
public class OrderMessageConsumer {

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderCreated(OrderEvent event) {
        System.out.println("Received order event: " + event);
        // Process order
    }

    // Manual acknowledgment with error handling
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleWithAck(OrderEvent event, Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // Process karne ka logic
            processOrder(event);
            channel.basicAck(deliveryTag, false);  // Success
        } catch (Exception e) {
            channel.basicNack(deliveryTag, false, true);  // Requeue
        }
    }
}
```

---

## 8. OpenFeign — Declarative REST Client

OpenFeign ek **declarative HTTP client** hai. Interface define karo, annotations lagao, baaki Spring automatically implement kar dega. Bahut clean code hota hai.

### Why OpenFeign?
- **Boilerplate kam** — manually HTTP call likhne ki zaroorat nahi
- **Service Discovery** integration — Eureka/Consul se automatic URL resolve
- **Load Balancing** — Spring Cloud LoadBalancer ke saath
- **Error Handling** — Custom error decoders
- **Retry** — Built-in retry support

### Setup:

**1. Dependency:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

**2. Enable Feign:**

```java
@SpringBootApplication
@EnableFeignClients
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

**3. Feign Client Interface:**

```java
@FeignClient(
    name = "product-service",           // Service discovery name
    url = "${services.product-service}", // Fallback URL (optional)
    configuration = FeignConfig.class,   // Custom config (optional)
    fallbackFactory = ProductClientFallbackFactory.class  // Circuit breaker fallback
)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProduct(@PathVariable("id") Long id);

    @GetMapping("/api/products")
    List<ProductResponse> getProducts(@RequestParam("category") String category);

    @PostMapping("/api/products")
    ProductResponse createProduct(@RequestBody CreateProductRequest request);

    @PutMapping("/api/products/{id}")
    ProductResponse updateProduct(@PathVariable("id") Long id,
                                  @RequestBody UpdateProductRequest request);

    @DeleteMapping("/api/products/{id}")
    void deleteProduct(@PathVariable("id") Long id);

    // Custom headers
    @GetMapping("/api/products/{id}/stock")
    @Headers({"X-API-Version: v2"})
    StockResponse checkStock(@PathVariable("id") Long id,
                             @RequestHeader("X-Tenant-Id") String tenantId);
}
```

**4. Usage (Kitna Simple!):**

```java
@Service
public class OrderService {

    private final ProductClient productClient;  // Autowired!

    public OrderService(ProductClient productClient) {
        this.productClient = productClient;
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        // Feign client directly call karega Product Service ko
        ProductResponse product = productClient.getProduct(request.getProductId());

        if (product.getStock() < request.getQuantity()) {
            throw new InsufficientStockException(product.getName());
        }

        // Order create karo...
        return orderDTO;
    }
}
```

**5. Feign Configuration:**

```java
public class FeignConfig {

    // Custom error decoder
    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 404) {
                return new ResourceNotFoundException("Resource not found");
            }
            if (response.status() >= 500) {
                return new RetryableException(
                    response.status(),
                    "Server error, retrying...",
                    HttpMethod.POST,
                    new Date(),
                    response.request()
                );
            }
            return new FeignException.FeignServerException(
                response.status(), "Error", response.request(), null);
        };
    }

    // Request interceptor (auth header add karna)
    @Bean
    public RequestInterceptor authInterceptor() {
        return template -> {
            template.header("Authorization", "Bearer " + getCurrentToken());
            template.header("X-Request-Id", UUID.randomUUID().toString());
        };
    }

    // Logger
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;  // NONE, BASIC, HEADERS, FULL
    }
}
```

**6. YAML Config:**

```yaml
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
        loggerLevel: full
      product-service:
        connectTimeout: 3000
        readTimeout: 5000

spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true  # Resilience4j integration
```

**7. Fallback with Circuit Breaker:**

```java
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return new ProductClient() {
            @Override
            public ProductResponse getProduct(Long id) {
                // Cached response ya default value return karo
                return ProductResponse.builder()
                    .id(id)
                    .name("Product (Service Unavailable)")
                    .available(false)
                    .build();
            }

            @Override
            public List<ProductResponse> getProducts(String category) {
                return Collections.emptyList();
            }

            @Override
            public ProductResponse createProduct(CreateProductRequest request) {
                throw new ServiceUnavailableException("Product service is down");
            }

            @Override
            public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
                throw new ServiceUnavailableException("Product service is down");
            }

            @Override
            public void deleteProduct(Long id) {
                // Silently ignore ya queue mein daalo for later
            }

            @Override
            public StockResponse checkStock(Long id, String tenantId) {
                return StockResponse.builder().available(false).build();
            }
        };
    }
}
```

---

## 9. RestTemplate (Legacy)

RestTemplate Spring ka **original** HTTP client tha. Ab **maintenance mode** mein hai — naye projects mein use mat karo. Legacy code mein milega toh samajh lo.

### Why Deprecated (Maintenance Mode):
- **Blocking** — thread wait karta hai response tak
- **No reactive support** — WebFlux ke saath kaam nahi karta
- **Verbose code** — bahut boilerplate
- **WebClient better hai** — reactive, non-blocking, modern

### RestTemplate Example (For Reference):

```java
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(10))
            .additionalInterceptors(new LoggingInterceptor())
            .build();
    }
}

@Service
public class LegacyOrderService {

    private final RestTemplate restTemplate;

    public void createOrder(Long productId) {
        // GET request
        ProductResponse product = restTemplate.getForObject(
            "http://product-service/api/products/" + productId,
            ProductResponse.class
        );

        // POST request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<OrderRequest> request = new HttpEntity<>(orderRequest, headers);

        ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
            "http://product-service/api/orders",
            request,
            OrderResponse.class
        );

        // Exchange (most flexible)
        ResponseEntity<ProductResponse> exchange = restTemplate.exchange(
            "http://product-service/api/products/{id}",
            HttpMethod.GET,
            new HttpEntity<>(headers),
            ProductResponse.class,
            productId
        );
    }
}
```

**Verdict:** RestTemplate legacy hai. Naye code mein **WebClient** ya **OpenFeign** use karo.

---

## 10. WebClient (Reactive, Recommended)

WebClient Spring WebFlux ka **reactive, non-blocking** HTTP client hai. Yeh **recommended** approach hai modern Spring Boot applications ke liye.

### WebClient vs RestTemplate:

| Feature | WebClient | RestTemplate |
|---------|-----------|--------------|
| **Type** | Reactive, Non-blocking | Blocking |
| **Performance** | High (async, event-driven) | Lower (thread per request) |
| **Streaming** | Haan (Server-Sent Events, streaming) | Nahi |
| **Functional API** | Haan (fluent builder) | Haan |
| **Backpressure** | Support karta hai | Nahi |
| **Spring WebFlux** | Native support | Nahi |
| **Status** | **Active, Recommended** | Maintenance Mode |

### WebClient Setup:

**1. Dependency:**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

**2. WebClient Bean Config:**

```java
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
            .baseUrl("http://product-service")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("X-Service-Name", "order-service")
            .filter(ExchangeFilterFunctions.basicAuthentication("user", "pass"))
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
    }

    // With retry
    @Bean
    public WebClient retryingWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl("http://product-service")
            .filter((request, next) -> next.exchange(request)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                    .filter(this::isRetryable)
                    .onRetryExhaustedThrow((spec, signal) -> signal.failure())))
            .build();
    }

    private boolean isRetryable(Throwable e) {
        return e instanceof WebClientResponseException &&
               ((WebClientResponseException) e).getStatusCode().is5xxServerError();
    }
}
```

**3. Usage — GET Request:**

```java
@Service
public class ProductClientService {

    private final WebClient webClient;

    public ProductClientService(WebClient webClient) {
        this.webClient = webClient;
    }

    // Single object
    public Mono<ProductResponse> getProduct(Long id) {
        return webClient
            .get()
            .uri("/api/products/{id}", id)
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, response ->
                Mono.error(new ResourceNotFoundException("Product not found: " + id)))
            .onStatus(HttpStatus::is5xxServerError, response ->
                Mono.error(new ServiceUnavailableException("Product service unavailable")))
            .bodyToMono(ProductResponse.class);
    }

    // List of objects
    public Flux<ProductResponse> getProductsByCategory(String category) {
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/products")
                .queryParam("category", category)
                .build())
            .retrieve()
            .bodyToFlux(ProductResponse.class);
    }

    // With timeout
    public Mono<ProductResponse> getProductWithTimeout(Long id) {
        return webClient
            .get()
            .uri("/api/products/{id}", id)
            .retrieve()
            .bodyToMono(ProductResponse.class)
            .timeout(Duration.ofSeconds(5))
            .onErrorTimeout(TimeoutException.class,
                () -> new ServiceUnavailableException("Product service timed out"));
    }
}
```

**4. Usage — POST/PUT/DELETE:**

```java
// POST request
public Mono<ProductResponse> createProduct(CreateProductRequest request) {
    return webClient
        .post()
        .uri("/api/products")
        .bodyValue(request)
        .retrieve()
        .onStatus(HttpStatus::isError, response ->
            response.bodyToMono(String.class).flatMap(body ->
                Mono.error(new RuntimeException("Failed: " + body))))
        .bodyToMono(ProductResponse.class);
}

// PUT request
public Mono<ProductResponse> updateProduct(Long id, UpdateProductRequest request) {
    return webClient
        .put()
        .uri("/api/products/{id}", id)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(ProductResponse.class);
}

// DELETE request
public Mono<Void> deleteProduct(Long id) {
    return webClient
        .delete()
        .uri("/api/products/{id}", id)
        .retrieve()
        .bodyToMono(Void.class);
}
```

**5. Streaming (Server-Sent Events):**

```java
public Flux<ProductEvent> streamProductUpdates() {
    return webClient
        .get()
        .uri("/api/products/stream")
        .accept(MediaType.TEXT_EVENT_STREAM)
        .retrieve()
        .bodyToFlux(ProductEvent.class);
}

// Usage:
productClient.streamProductUpdates()
    .doOnNext(event -> System.out.println("New event: " + event))
    .subscribe();
```

**6. Concurrent Multiple Calls:**

```java
// Multiple services se parallel mein data lao
public Mono<OrderSummary> getOrderSummary(Long orderId) {
    Mono<OrderResponse> orderMono = webClient
        .get()
        .uri("http://order-service/api/orders/{id}", orderId)
        .retrieve()
        .bodyToMono(OrderResponse.class);

    Mono<ProductResponse> productMono = webClient
        .get()
        .uri("http://product-service/api/products/{id}", productId)
        .retrieve()
        .bodyToMono(ProductResponse.class);

    // Zip — dono complete hone pe combine karo
    return Mono.zip(orderMono, productMono)
        .map(tuple -> {
            OrderResponse order = tuple.getT1();
            ProductResponse product = tuple.getT2();
            return new OrderSummary(order, product);
        });
}
```

---

## 11. Service Mesh Basics (Istio)

Service Mesh microservices ke beech communication ko **manage, secure, aur observe** karne ka infrastructure layer hai. Istio sabse popular service mesh hai (Kubernetes ke saath kaam karta hai).

### Problem Statement:

```
Without Service Mesh:
┌─────────┐   ┌─────────┐   ┌─────────┐
│Service A│   │Service B│   │Service C│
│         │   │         │   │         │
│ + retry │   │ + retry │   │ + retry │
│ + auth  │   │ + auth  │   │ + auth  │
│ + TLS   │   │ + TLS   │   │ + TLS   │
│ + metrics│  │ + metrics│  │ + metrics│
│ + tracing│  │ + tracing│  │ + tracing│
│ + load balance│+ load balance│+ load balance│
└─────────┘   └─────────┘   └─────────┘

Har service mein SAME code duplicate — DRY violation!
```

### Solution — Service Mesh:

```
With Service Mesh (Istio + Envoy Sidecars):
┌──────────────────────────────────────────────────────┐
│  Kubernetes Pod                                       │
│                                                      │
│  ┌─────────┐      ┌────────────────────┐             │
│  │Service A│◄────►│  Envoy Sidecar      │             │
│  │(Business│      │  - Retry            │             │
│  │ Logic)  │      │  - Auth (mTLS)      │             │
│  │         │      │  - Load Balance     │             │
│  │         │      │  - Metrics          │             │
│  │         │      │  - Tracing          │             │
│  │         │      │  - Rate Limit       │             │
│  └─────────┘      └─────────┬──────────┘             │
└─────────────────────────────┼────────────────────────┘
                              │
                    ┌─────────┼─────────┐
                    │  Istio Control Plane │
                    │  (Pilot, Citadel,    │
                    │   Galley, Mixer)     │
                    └─────────────────────┘
```

### Istio Key Components:

| Component | Kaam |
|-----------|------|
| **Envoy Proxy (Sidecar)** | Har pod mein chalta hai, saara traffic intercept karta hai |
| **Pilot** | Routing rules distribute karta hai (traffic management) |
| **Citadel** | Certificates manage karta hai (mTLS, security) |
| **Galley** | Configuration validate aur distribute karta hai |
| **Ingress Gateway** | External traffic ko mesh mein laata hai |
| **Egress Gateway** | Mesh se bahar jane wala traffic control karta hai |

### Istio Features:

**1. mTLS (Mutual TLS):**
```yaml
# Sab services ke beech encrypted communication
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT  # STRICT, PERMISSIVE, DISABLE
```

**2. Traffic Management (Canary Deployment):**
```yaml
# 90% traffic v1 ko, 10% traffic v2 ko
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product-service
spec:
  hosts:
    - product-service
  http:
    - route:
        - destination:
            host: product-service
            subset: v1
          weight: 90
        - destination:
            host: product-service
            subset: v2
          weight: 10
```

**3. Circuit Breaking:**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: product-service
spec:
  host: product-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        h2UpgradePolicy: DEFAULT
        http1MaxPendingRequests: 100
        http2MaxRequests: 1000
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

**4. Retry & Timeout:**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: product-service
spec:
  http:
    - route:
        - destination:
            host: product-service
      timeout: 2s
      retries:
        attempts: 3
        perTryTimeout: 1s
        retryOn: 5xx,connect-failure,refused-stream
```

**5. Rate Limiting:**
```yaml
apiVersion: networking.istio.io/v1alpha3
kind: EnvoyFilter
metadata:
  name: ratelimit
spec:
  configPatches:
    - applyTo: HTTP_FILTER
      match:
        context: SIDECAR_INBOUND
      patch:
        operation: INSERT_BEFORE
        value:
          name: envoy.filters.http.local_ratelimit
          typed_config:
            "@type": type.googleapis.com/udpa.type.v1.TypedStruct
            type_url: type.googleapis.com/envoy.extensions.filters.http.local_ratelimit.v3.LocalRateLimit
            value:
              stat_prefix: http_local_rate_limiter
              token_bucket:
                max_tokens: 100
                tokens_per_fill: 100
                fill_interval: 60s
```

### When to Use Service Mesh:

| Scenario | Use Service Mesh? |
|----------|-------------------|
| 5-10 services | Nahi, overkill hai |
| 10-50 services | Consider karo |
| 50+ services | **Haan, highly recommended** |
| Multiple teams, polyglot | **Haan** |
| Strict security/compliance | **Haan** (mTLS, audit) |
| Kubernetes environment | **Haan** (native support) |
| Simple CRUD app | Nahi |

### Service Mesh vs API Gateway:

| Aspect | API Gateway | Service Mesh |
|--------|-------------|--------------|
| **Location** | Edge (external traffic) | Internal (service-to-service) |
| **Scope** | North-South traffic | East-West traffic |
| **Features** | Auth, rate limiting, routing | mTLS, retry, circuit breaking, observability |
| **Deployment** | Single component | Sidecar per pod |
| **Performance** | Single hop | Extra hop (sidecar) |

---

## 12. Comparison & When to Use What

### Communication Method Decision Tree:

```
Client ko immediate response chahiye?
├── Haan → Synchronous
│   ├── Public API / Browser client? → REST (WebClient / OpenFeign)
│   ├── Internal service, high performance? → gRPC
│   └── Simple Java-to-Java call? → OpenFeign (declarative)
│
└── Nahi → Asynchronous
    ├── Event streaming, high throughput? → Kafka
    ├── Complex routing, task queue? → RabbitMQ
    ├── Simple pub/sub? → Redis Pub/Sub
    └── Cloud-native (AWS)? → SNS + SQS
```

### Complete Comparison Table:

| Feature | REST (WebClient) | gRPC | OpenFeign | Kafka | RabbitMQ |
|---------|-----------------|------|-----------|-------|----------|
| **Type** | Sync | Sync | Sync | Async | Async |
| **Protocol** | HTTP/JSON | HTTP/2 + Protobuf | HTTP/JSON | TCP | AMQP |
| **Blocking** | Non-blocking | Blocking | Blocking | Non-blocking | Non-blocking |
| **Latency** | Medium | Very Low | Medium | Low | Low |
| **Throughput** | Medium | Very High | Medium | Very High | High |
| **Ordering** | N/A | N/A | N/A | Partition-level | Queue-level |
| **Retry** | Manual | Manual | Built-in | Built-in | Built-in |
| **Service Discovery** | Yes (LB) | Yes | Yes (Eureka) | No | No |
| **Best For** | General REST calls | High-perf internal | Simple declarative | Event streaming | Task queues |

---

## 13. Best Practices

### 1. Communication Pattern Choose Karo Wisely
```
- Query operations → Synchronous (REST/gRPC)
- Command operations → Asynchronous (Kafka/RabbitMQ)
- Events broadcast → Pub/Sub (Kafka topics)
- Real-time streaming → Server-Sent Events / gRPC streaming
```

### 2. Always Add Timeouts
```java
// WebClient
.timeout(Duration.ofSeconds(5))

// OpenFeign
connectTimeout: 5000
readTimeout: 10000

// gRPC
.withDeadlineAfter(5, TimeUnit.SECONDS)
```

### 3. Implement Retry with Backoff
```java
.retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
    .maxBackoff(Duration.ofSeconds(10))
    .filter(this::isRetryable))
```

### 4. Use Circuit Breaker
```
Har inter-service call ke saath circuit breaker lagao
→ Cascading failures se bacho
→ Fast fail karo when service is down
```

### 5. Distributed Tracing Enable Karo
```yaml
spring:
  zipkin:
    base-url: http://zipkin:9411
  sleuth:
    sampler:
      probability: 1.0
```

### 6. Idempotency
```
Async messages hamesha idempotent hone chahiye
→ Duplicate message se data corrupt nahi hona chahiye
→ Unique request ID use karo
→ Check karo pehle se processed toh nahi
```

### 7. Dead Letter Queue
```
Failed messages ko DLQ mein bhejo
→ Baad mein analyze kar sakte hain
→ Manual retry kar sakte hain
→ Data loss nahi hoga
```

### 8. Schema Registry (Kafka)
```
Kafka messages ka schema manage karo
→ Breaking changes se bacho
→ Backward/forward compatibility
→ Confluent Schema Registry use karo
```

---

## 14. Summary

| Topic | Key Points |
|-------|------------|
| **Synchronous** | REST, gRPC — client wait karta hai, real-time response |
| **REST** | HTTP + JSON, sabse common, WebClient recommended |
| **gRPC** | Protobuf + HTTP/2, 5-10x faster, internal services ke liye |
| **Asynchronous** | Kafka, RabbitMQ — fire-and-forget, decoupled |
| **Kafka** | Event streaming, high throughput, partition-based ordering |
| **RabbitMQ** | Message broker, advanced routing, task queues |
| **OpenFeign** | Declarative REST client, clean code, service discovery |
| **RestTemplate** | Legacy, maintenance mode, WebClient use karo |
| **WebClient** | Reactive, non-blocking, streaming support, recommended |
| **Service Mesh** | Istio + Envoy, mTLS, traffic management, 50+ services ke liye |

**Golden Rules:**
1. **Sync for queries, Async for commands** — CQRS principle
2. **Always add timeouts** — Default wait infinite mat rakho
3. **Circuit breaker lagao** — Cascading failure se bacho
4. **Idempotent operations** — Retry safe ho
5. **Distributed tracing** — Request flow track karo
6. **Schema evolution** — Breaking changes avoid karo
7. **Dead letter queues** — Failed messages handle karo

---

> **Previous:** [02 - API Gateway Pattern](./02-API-Gateway-Pattern.md)
>
> **Next:** Continue exploring Microservices Architecture patterns — Saga Pattern, CQRS, Event Sourcing, etc.
