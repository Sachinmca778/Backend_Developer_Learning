# 04 — Service Discovery in Microservices

> **Service Discovery** microservices architecture ka ek bahut important pattern hai. Jab tumhare paas dozens ya hundreds of services hote hain, toh har service ko hardcode karke dusre services ka IP/port dena impossible ho jata hai. Service Discovery isi problem ko solve karta hai.

---

## Table of Contents

1. [Why Service Discovery?](#why-service-discovery)
2. [Client-Side vs Server-Side Discovery](#client-side-vs-server-side-discovery)
3. [Netflix Eureka — Client-Side Discovery](#netflix-eureka--client-side-discovery)
4. [HashiCorp Consul](#hashicorp-consul)
5. [Apache Zookeeper](#apache-zookeeper)
6. [Spring Cloud LoadBalancer (Modern Replacement)](#spring-cloud-loadbalancer-modern-replacement)
7. [Ribbon Load Balancer (Legacy)](#ribbon-load-balancer-legacy)
8. [Comparison Table](#comparison-table)
9. [Best Practices](#best-practices)

---

## Why Service Discovery?

Problem samjho ek simple scenario se:

```
Without Service Discovery:
+-----------+     http://192.168.1.50:8081     +-----------+
|  Order    | --------------------------------> | Payment   |
| Service   |     (HARDCODED IP - BAD!)         | Service   |
+-----------+                                   +-----------+

Problem: Agar Payment Service ka IP change ho gaya ya naya instance
         spin up hua, toh Order Service ko manually update karna padega.
         Scaling karna bahut mushkil ho jata hai.
```

```
With Service Discovery:
+-----------+     lookup "payment-service"      +-----------+
|  Order    | --------------------------------> | Payment   |
| Service   |     (via Service Registry)         | Service   |
+-----------+                                   +-----------+
                            |
                    +----------------+
                    |  Service       |
                    |  Registry      |
                    |  (Eureka/      |
                    |   Consul)      |
                    +----------------+
```

**Key Benefits:**
- Dynamic service registration/deregistration
- Automatic load balancing across instances
- Zero-downtime deployments possible
- Health checks built-in
- Auto-scaling friendly

---

## Client-Side vs Server-Side Discovery

### Client-Side Discovery

```
                    +-------------------+
                    |   Service Registry |
                    |   (Eureka Server)  |
                    +--------+----------+
                             ^
                             | register / heartbeat
                             |
+-----------+                |                +-----------+
|  Order    | ---lookup---->+                | Payment   |
| Service   |<--IP:Port-----|                | Service   |
| (Client)  |                |                | (Server)  |
+-----------+                |                +-----------+
      |                      |                      ^
      +----------------------+----------------------+
              Client ko IP milta hai, client khud call karta hai
```

- **Client khud** service registry se IP/port fetch karta hai
- Client-side load bhi kar sakta hai (Ribbon, Spring Cloud LoadBalancer)
- **Examples:** Netflix Eureka

### Server-Side Discovery

```
+-----------+                                 +------------------+
|  Order    | ----> http://payment-service    |  Load Balancer   |
| Service   |         (virtual name)           |  (nginx/Envoy)   |
+-----------+                                 +--------+---------+
                                                       |
                                            route to actual instance
                                                       |
                                            +----------v----------+
                                            | Payment Service     |
                                            | (192.168.1.50:8081) |
                                            | (192.168.1.51:8082) |
                                            +---------------------+
```

- Client sirf logical name use karta hai
- Router/LB actual IP resolve karta hai
- **Examples:** Consul + nginx, Kubernetes Services, AWS ELB

---

## Netflix Eureka — Client-Side Discovery

Eureka Netflix ka open-source service registry hai. Spring Cloud mein iska integration bahut smooth hai.

### Architecture

```
+-----------------------------------------------------------+
|                   Eureka Server (Registry)                |
|                                                           |
|  +-----------+    +-----------+    +-----------+          |
|  | Instance 1|    | Instance 2|    | Instance 3|          |
|  | (RENEW)   |    | (RENEW)   |    | (RENEW)   |          |
|  +-----------+    +-----------+    +-----------+          |
|                                                           |
|  Heartbeat: Har 30 seconds mein instances heartbeat bhejte|
|  Eviction: Agar 90 seconds mein heartbeat nahi aaya ->    |
|            instance ko registry se hata do                |
+-----------------------------------------------------------+
         ^                    ^                    ^
         | register           | register           | register
         |                    |                    |
  +------+------+      +------+------+      +------+------+
  |Order-Service|      |Payment-Svc  |      |Inventory-Svc|
  |  :8081      |      |  :8082      |      |  :8083      |
  +-------------+      +-------------+      +-------------+
```

### Step 1: Eureka Server Setup

**pom.xml** (Eureka Server ke liye):

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
</dependency>
```

**Application class:**

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer  // <-- Ye annotation isko Eureka Server banata hai
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**application.yml:**

```yaml
server:
  port: 8761  # Eureka default port

eureka:
  client:
    register-with-eureka: false  # Server khud ko register na kare
    fetch-registry: false        # Server ko registry fetch nahi karni
  server:
    wait-time-in-ms-when-sync-empty: 0
    enable-self-preservation: false  # Production mein true rakho
```

> **Self-Preservation Mode:** Agar network issue hai aur bahut saare instances heartbeat nahi bhej pa rahe, toh Eureka unko accidentally evict na kare — isliye self-preservation mode hota hai. Development mein off kar sakte ho debugging ke liye.

### Step 2: Eureka Client Setup

**pom.xml:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

**Application class:**

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient  // <-- Ye annotation isko Eureka Client banata hai
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

**application.yml:**

```yaml
spring:
  application:
    name: order-service  # <-- Ye naam registry mein dikhega

server:
  port: 8081

eureka:
  client:
    service-url:
      default-zone: http://localhost:8761/eureka/  # Eureka server ka address
    register-with-eureka: true
    fetch-registry: true
  instance:
    prefer-ip-address: true  # IP address dikhaye hostname ke jagah
    instance-id: ${spring.application.name}:${server.port}
```

### Step 3: Service-to-Service Communication

Eureka client registered ho gaya. Ab ek service se dusre service ko kaise call kare?

**RestTemplate + @LoadBalanced:**

```java
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentClient {

    @LoadBalanced  // <-- Ye annotation client-side load balancing enable karta hai
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String makePayment(Long orderId, double amount) {
        // Notice: "payment-service" logical name use ho raha hai, IP nahi!
        String url = "http://payment-service/api/payments";
        return restTemplate.postForObject(url, request, String.class);
    }
}
```

**WebClient approach:**

```java
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PaymentClient {

    @LoadBalanced
    @Bean
    public WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    public String makePayment(Long orderId, double amount) {
        return loadBalancedWebClientBuilder
            .baseUrl("http://payment-service")
            .build()
            .post()
            .uri("/api/payments")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(String.class)
            .block();
    }
}
```

### Eureka Dashboard

Eureka server start karne ke baad: `http://localhost:8761`

Yahan tumhe dikhega:
- Registered instances list
- Instance status (UP/DOWN)
- Last heartbeat time
- Datacenter info

### Eureka Internals — How It Works

```
Registration Flow:
1. Client starts up
2. Reads eureka.client.service-url.default-zone
3. Sends POST /eureka/apps/{appName} with metadata
4. Eureka stores: { appName, instanceId, ip, port, status, metadata }

Heartbeat (Renewal) Flow:
1. Every 30 seconds (default lease-renewal-interval)
2. Client sends PUT /eureka/apps/{appName}/{instanceId}
3. Eureka updates last-timestamp

Query Flow:
1. Client needs to call "payment-service"
2. Client sends GET /eureka/apps/payment-service
3. Gets list of all instances: [{ip:port}, {ip:port}, ...]
4. Load balancer picks ONE instance
5. Client makes direct HTTP call to that instance
```

### Eureka Configuration Reference

```yaml
eureka:
  client:
    service-url:
      default-zone: http://localhost:8761/eureka/
    registry-fetch-interval-seconds: 30    # Client kitni frequently registry fetch kare
    register-with-eureka: true
    fetch-registry: true
  instance:
    lease-renewal-interval-in-seconds: 30   # Heartbeat interval
    lease-expiration-duration-in-seconds: 90 # Agar itne time mein heartbeat nahi aaya -> evict
    prefer-ip-address: true
    metadata-map:
      zone: us-east-1a                     # Custom metadata
      version: v2.1
  server:
    enable-self-preservation: true
    eviction-interval-timer-in-ms: 60000    # Eviction check interval
```

---

## HashiCorp Consul

Consul Eureka se zyada feature-rich hai. Sirf service discovery nahi — health checking, KV store, multi-datacenter support bhi deta hai.

### Consul Architecture

```
+--------------------------------------------------------+
|                  Consul Cluster                        |
|                                                        |
|  +------------+    +------------+    +------------+    |
|  | Consul     |    | Consul     |    | Consul     |    |
|  | Server 1   |<-->| Server 2   |<-->| Server 3   |    |
|  | (Leader)   |    | (Follower) |    | (Follower) |    |
|  +------------+    +------------+    +------------+    |
|         ^                                              |
|         | gossip protocol (Serf)                       |
|         v                                              |
|  +------------+    +------------+    +------------+    |
|  | Consul     |    | Consul     |    | Consul     |    |
|  | Agent 1    |    | Agent 2    |    | Agent 3    |    |
|  | (Service A)|    | (Service B)|    | (Service C)|    |
|  +------------+    +------------+    +------------+    |
+--------------------------------------------------------+
```

### Spring Boot + Consul Integration

**pom.xml:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-consul-discovery</artifactId>
</dependency>
```

**Application class:**

```java
@SpringBootApplication
@EnableDiscoveryClient  // <-- Generic annotation, Consul/Eureka dono ke liye kaam karta hai
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
```

**application.yml:**

```yaml
spring:
  application:
    name: order-service
  cloud:
    consul:
      host: localhost
      port: 8500  # Consul default port
      discovery:
        prefer-ip-address: true
        health-check-path: /actuator/health
        health-check-interval: 10s
        instance-id: ${spring.application.name}:${server.port}

server:
  port: 8081
```

### Consul Health Check

Consul actively health check karta hai:

```
Consul sends HTTP GET to: http://service-host:port/actuator/health

Response:
{
  "status": "UP",
  "checks": [
    { "name": "diskSpace", "status": "UP" },
    { "name": "db", "status": "UP" }
  ]
}

Agar status != UP -> Consul service ko DOWN mark karta hai
-> Traffic uss instance pe nahi jayega
```

### Consul KV Store (Bonus Feature)

Consul sirf service discovery nahi, key-value store bhi hai:

```bash
# Consul CLI commands
consul kv put config/payment-service/max-retries 3
consul kv put config/payment-service/timeout 5000
consul kv get config/payment-service/max-retries

# Spring Boot mein @Value se access kar sakte ho
@Value("${config.max-retries:3}")
private int maxRetries;
```

### Consul vs Eureka Quick Comparison

```
Feature                  Eureka              Consul
---------------------------------------------------------------
Service Discovery        ✅ Yes              ✅ Yes
Health Checks            Passive (heartbeat) Active (HTTP/TCP/script)
KV Store                 ❌ No               ✅ Yes
Multi-Datacenter         Limited             ✅ Native support
Consensus                AP (eventual)       CP (strong via Raft)
UI Dashboard             ✅ Basic            ✅ Rich
Spring Boot Integration  ✅ Excellent        ✅ Excellent
```

---

## Apache Zookeeper

Zookeeper originally Hadoop ecosystem ka coordination service tha. Ab service discovery ke liye bhi use hota hai.

### Zookeeper Architecture

```
+--------------------------------------------------+
|               Zookeeper Ensemble                 |
|               (Odd number of nodes)              |
|                                                  |
|  +------+   Zab Protocol   +------+   +------+  |
|  |Node 1| <--------------> |Node 2|   |Node 3|  |
|  |Leader|                  |Follower|  |Follower| |
|  +------+                  +------+   +------+  |
|                                                  |
|  ZAB = Zookeeper Atomic Broadcast                |
|  Strong consistency guarantee                      |
+--------------------------------------------------+
          |              |              |
          |              |              |
    +-----+-----+  +-----+-----+  +-----+-----+
    | Service A |  | Service B |  | Service C |
    | Client    |  | Client    |  | Client    |
    +-----------+  +-----------+  +-----------+
```

### Zookeeper ke Special Features:

1. **Ephemeral Nodes:** Jab client disconnect hota hai, node automatically delete ho jata hai
2. **Watchers:** Client ko notification milta hai jab koi node change hota hai
3. **Sequential Nodes:** Auto-increment naming for ordering

### Spring Boot + Zookeeper

**pom.xml:**

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-zookeeper-discovery</artifactId>
</dependency>
```

**application.yml:**

```yaml
spring:
  application:
    name: order-service
  cloud:
    zookeeper:
      connect-string: localhost:2181
      discovery:
        register: true
        instance-id: ${spring.application.name}:${server.port}
```

### CAP Theorem Context

```
         Consistency (C)
              /\
             /  \
            /    \
           /      \
    Zookeeper      \
    (CP System)     \
                     \
                      \
              Availability (A) ------ Partition Tolerance (P)

Eureka:  AP system (availability pe focus, eventual consistency)
Zookeeper: CP system (consistency pe focus, strong consistency)
Consul:  CP system (Raft consensus)

Production mein: Dono ka use case alag hai.
- CP chahiye (strong consistency) -> Zookeeper / Consul
- AP chahiye (high availability) -> Eureka
```

---

## Spring Cloud LoadBalancer (Modern Replacement)

> **Important:** Netflix Ribbon ab deprecated hai. Spring Cloud LoadBalancer uska official replacement hai.

### Ribbon vs Spring Cloud LoadBalancer

```
Timeline:

Spring Cloud 2020.0 (2021)
    |
    |-- Ribbon REMOVED from Spring Cloud Netflix
    |-- Spring Cloud LoadBalancer introduced as replacement
    |
    v
Today: Always use Spring Cloud LoadBalancer for new projects
```

### How Spring Cloud LoadBalancer Works

```
+-------------+      logical name           +------------------+
| Order       | ----> http://payment-svc -->| Spring Cloud     |
| Service     |                             | LoadBalancer     |
+-------------+                             +--------+---------+
                                                    |
                                         picks one instance using strategy
                                                    |
                                         +----------v----------+
                                         | Instances:          |
                                         | 192.168.1.50:8081   |
                                         | 192.168.1.51:8082   |
                                         | 192.168.1.52:8083   |
                                         +---------------------+
```

### Setup

**pom.xml:**

```xml
<!-- Eureka client already brings in Spring Cloud LoadBalancer automatically -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>

<!-- Agar explicitly add karna ho -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### Load Balancing Strategies

```java
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@LoadBalancerClient(name = "payment-service", configuration = CustomLBConfig.class)
public class LoadBalancerConfig {

    // Round Robin (default) - Har request pe next instance
    @Bean
    public ReactorLoadBalancer<ServiceInstance> roundRobinLB(
            LoadBalancerClientFactory factory) {
        return new RoundRobinLoadBalancer(
            factory.getLazyProvider("payment-service", ServiceInstanceListSupplier.class),
            "payment-service"
        );
    }

    // Random - Random instance pick karega
    @Bean
    public ReactorLoadBalancer<ServiceInstance> randomLB(
            LoadBalancerClientFactory factory) {
        return new RandomLoadBalancer(
            factory.getLazyProvider("payment-service", ServiceInstanceListSupplier.class),
            "payment-service"
        );
    }
}
```

### Available Strategies

| Strategy | Description | Use Case |
|----------|-------------|----------|
| `RoundRobinLoadBalancer` | Sequential rotation mein instances | Default, even distribution |
| `RandomLoadBalancer` | Random instance selection | Testing, non-critical |
| `StickySessionLoadBalancer` | Same instance for same session | Session affinity |
| `HealthCheckServiceInstanceListSupplier` | Sirf healthy instances | Production recommended |

### Custom Load Balancer with Health Check

```java
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientConfiguration;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class HealthCheckLBConfig {

    @Bean
    public ServiceInstanceListSupplier healthCheckingSupplier(
            Environment env) {
        return ServiceInstanceListSupplier.builder()
            .withDiscoveryClient()           // Eureka/Consul se instances lao
            .withHealthChecks()              // Health check add karo
            .withBlockingDiscoveryClient()   // Sync discovery
            .build(env);
    }
}
```

### Programmatic Load Balancer Usage

```java
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OrderService {

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private RestTemplate restTemplate;

    public String callPayment(Long orderId) {
        // Manually instance pick karo
        ServiceInstance instance = loadBalancerClient.choose("payment-service");

        String url = "http://" + instance.getHost() + ":" + instance.getPort()
                     + "/api/payments";

        return restTemplate.postForObject(url, request, String.class);
    }
}
```

---

## Ribbon Load Balancer (Legacy)

> **WARNING:** Ribbon is **DEPRECATED** since Spring Cloud 2020.0 (Jan 2021). Ye sirf legacy understanding ke liye hai.

### Why Was Ribbon Used?

Ribbon client-side load balancer tha jo Eureka ke saath kaam karta tha:

```
Old Setup (Pre-2021):

pom.xml:
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
</dependency>
```

### Ribbon Configuration (Legacy)

```yaml
payment-service:
  ribbon:
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.RoundRobinRule
    ConnectTimeout: 5000
    ReadTimeout: 5000
    MaxAutoRetries: 0
    MaxAutoRetriesNextServer: 1
    eureka:
      enabled: true
```

### Ribbon Rules (Available Strategies)

```
Rule Class                          | Behavior
------------------------------------|---------------------------
RoundRobinRule                      | Simple rotation (default)
RandomRule                          | Random pick
RetryRule                           | Retry with another instance if failed
BestAvailableRule                   | Picks instance with least active connections
AvailabilityFilteringRule           | Filters out unhealthy/throttled instances
WeightedResponseTimeRule            | Faster instances get more traffic
ZoneAvoidanceRule                   | Zone + availability based
```

### Migration: Ribbon -> Spring Cloud LoadBalancer

```
Before (Ribbon):
@RibbonClient(name = "payment-service", configuration = RibbonConfig.class)

After (Spring Cloud LoadBalancer):
@LoadBalancerClient(name = "payment-service", configuration = LBConfig.class)

Before (Ribbon config):
@Bean
public IRule ribbonRule() {
    return new RoundRobinRule();
}

After (Spring Cloud LoadBalancer config):
@Bean
public ReactorLoadBalancer<ServiceInstance> lb(LoadBalancerClientFactory factory) {
    return new RoundRobinLoadBalancer(
        factory.getLazyProvider("payment-service", ServiceInstanceListSupplier.class),
        "payment-service"
    );
}
```

---

## Comparison Table

| Feature | Netflix Eureka | HashiCorp Consul | Apache Zookeeper |
|---------|---------------|------------------|------------------|
| **Type** | Client-side | Client/Server-side | Client/Server-side |
| **CAP** | AP (Availability) | CP (Consistency) | CP (Consistency) |
| **Health Checks** | Passive (heartbeat) | Active (HTTP/TCP) | Session-based |
| **KV Store** | No | Yes | Yes |
| **Multi-DC** | Limited | Native | Yes (Zab) |
| **Spring Integration** | `@EnableEurekaClient` | `@EnableDiscoveryClient` | `@EnableDiscoveryClient` |
| **UI Dashboard** | Basic | Rich | None (needs external tools) |
| **Consensus** | Peer-to-peer | Raft | ZAB |
| **Best For** | Simple microservices | Full service mesh | Hadoop/ZK ecosystem |
| **Language** | Java | Go | Java |

---

## Best Practices

### 1. Always Use Logical Service Names

```java
// BAD - Hardcoded IP
String url = "http://192.168.1.50:8081/api/payments";

// GOOD - Logical name (load balancer resolve karega)
String url = "http://payment-service/api/payments";
```

### 2. Use `@EnableDiscoveryClient` Instead of `@EnableEurekaClient`

```java
// Generic approach - future-proof agar registry change karna ho
@EnableDiscoveryClient  // Works with Eureka, Consul, Zookeeper

// Specific approach - sirf Eureka ke liye
@EnableEurekaClient     // Only works with Eureka
```

### 3. Health Check Endpoint Expose Karo

```java
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Custom health check logic
        if (isDatabaseConnected()) {
            return Health.up().build();
        }
        return Health.down()
            .withDetail("error", "Database connection lost")
            .build();
    }

    private boolean isDatabaseConnected() {
        try {
            // db ping logic
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

### 4. Multiple Eureka Servers for HA

```yaml
eureka:
  client:
    service-url:
      default-zone: http://eureka1:8761/eureka/,http://eureka2:8762/eureka/,http://eureka3:8763/eureka/
```

### 5. Graceful Shutdown

```yaml
server:
  shutdown: graceful  # Spring Boot 2.3+

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s

# Eureka client ko deregister karne ka time do
eureka:
  instance:
    lease-expiration-duration-in-seconds: 5
    lease-renewal-interval-in-seconds: 3
```

### 6. Security for Eureka Server

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```yaml
eureka:
  client:
    service-url:
      default-zone: http://user:password@localhost:8761/eureka/
```

### 7. Monitoring

```xml
<!-- Actuator for health and metrics -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,eureka
  endpoint:
    health:
      show-details: always
```

---

## Common Interview Questions

**Q1: Eureka aur Consul mein kya difference hai?**
A: Eureka AP system hai (availability focus), Consul CP system hai (consistency focus). Consul mein health checks, KV store, multi-DC support hai jo Eureka mein nahi.

**Q2: Ribbon deprecated kyun hua?**
A: Netflix ne Ribbon maintenance mode mein daal diya. Spring Cloud ne apna native Spring Cloud LoadBalancer banaya jo reactive stack (WebFlux) ke saath bhi kaam karta hai.

**Q3: @EnableEurekaClient vs @EnableDiscoveryClient?**
A: `@EnableEurekaClient` sirf Eureka ke liye hai. `@EnableDiscoveryClient` generic hai — Eureka, Consul, Zookeeper kisi ke saath bhi kaam karega. Spring Cloud 2020+ mein `@EnableDiscoveryClient` optional bhi hai (auto-detection ho jata hai).

**Q4: Service Discovery mein Self-Preservation Mode kya hai?**
A: Eureka ka safety mechanism. Agar network partition hai aur bahut saare instances heartbeat nahi bhej pa rahe, toh Eureka unko evict nahi karega. False positive se bachata hai.

---

> **Next Step:** Service discovery set up ho gaya. Ab socho — kya hoga agar koi service down ho jaye? Ya slow respond kare? Iska jawab hai **Resilience Patterns** — Circuit Breaker, Retry, Bulkhead. Next padho: [`05-Resilience-Patterns.md`](./05-Resilience-Patterns.md)
