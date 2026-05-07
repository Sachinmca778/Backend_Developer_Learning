# Spring Cloud Config

## Status: Not Started

---

## Table of Contents

1. [Spring Cloud Config Kya Hai?](#spring-cloud-config-kya-hai)
2. [Why Centralized Configuration?](#why-centralized-configuration)
3. [Architecture Overview](#architecture-overview)
4. [Config Server Setup](#config-server-setup)
5. [Git Backend](#git-backend)
6. [Config Client Setup](#config-client-setup)
7. [bootstrap.yml vs application.yml](#bootstrapyml-vs-applicationyml)
8. [Profiles & Environment-Specific Config](#profiles--environment-specific-config)
9. [Encryption / Decryption](#encryption--decryption)
10. [Refresh Scope (@RefreshScope)](#refresh-scope-refreshscope)
11. [Spring Cloud Bus](#spring-cloud-bus)
12. [Other Backends (Vault, JDBC, Native)](#other-backends-vault-jdbc-native)
13. [Security & Production Hardening](#security--production-hardening)
14. [Monitoring & Health](#monitoring--health)
15. [Common Pitfalls](#common-pitfalls)
16. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Spring Cloud Config Kya Hai?

**Matlab:** Distributed system ke liye **centralized external configuration** management — ek **Config Server** sab services ko config bhej deta hai, sab properties ek single source of truth (Git, Vault, DB) mein rakhi jaati hain.

```
TRADITIONAL (without Config Server)
┌────────────┐    ┌────────────┐    ┌────────────┐
│ Service A  │    │ Service B  │    │ Service C  │
│            │    │            │    │            │
│ app.yml    │    │ app.yml    │    │ app.yml    │
│ secrets    │    │ secrets    │    │ secrets    │
└────────────┘    └────────────┘    └────────────┘
   ❌ Har service mein config duplicate
   ❌ Update karne ke liye sab redeploy
   ❌ Secrets har jagah copy

WITH SPRING CLOUD CONFIG
                  ┌──────────────────┐
                  │  Config Server   │
                  │  ┌────────────┐  │
                  │  │ Git Repo   │  │  ← single source of truth
                  │  │ /configs   │  │
                  │  └────────────┘  │
                  └────────┬─────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐      ┌──────▼─────┐      ┌────▼─────┐
   │Service A│      │ Service B  │      │Service C │
   └─────────┘      └────────────┘      └──────────┘
   ✅ Config one place
   ✅ Refresh without redeploy
   ✅ Audit via Git
   ✅ Encryption built-in
```

### Built On

- **Spring Boot** (auto-config)
- **Spring Cloud** (umbrella project for distributed system patterns)
- **Spring Cloud Bus** (optional — for broadcast refresh)

---

## Why Centralized Configuration?

### Problems Without It

| Problem | Example |
|---------|---------|
| **Duplication** | Same DB URL in 10 services |
| **Drift** | Service A updated, Service B forgot — bugs |
| **Redeploy for tweak** | Logging level change = redeploy |
| **Secrets scattered** | DB password in 10 git repos |
| **Multi-environment chaos** | dev/staging/prod values mixed |
| **No audit trail** | Who changed what, when? |

### Benefits of Spring Cloud Config

✅ **Single source of truth** — Git/Vault as backend
✅ **Versioned configs** — full history (Git)
✅ **Environment-specific** — `application-dev.yml`, `application-prod.yml`
✅ **Refresh without restart** — `@RefreshScope` + `/actuator/refresh`
✅ **Encryption** — encrypted values stored in Git
✅ **Audit** — Git log = who/when/what
✅ **Rollback** — revert Git commit

### Cloud Config vs Cloud-Native Alternatives

| Tool | Best For |
|------|----------|
| **Spring Cloud Config** | Spring-heavy shops, Git-as-config |
| **HashiCorp Consul** | Service mesh + config + discovery |
| **HashiCorp Vault** | Secrets-first, dynamic credentials |
| **Kubernetes ConfigMaps + Secrets** | K8s-native |
| **AWS Parameter Store / Secrets Manager** | AWS-native |
| **etcd** | K8s internals, low-level |

**Modern combo:** Vault for secrets + Config Server for non-secret app config + K8s ConfigMaps for infra-level.

---

## Architecture Overview

```
                        ┌─────────────────────┐
                        │    Git Repository    │
                        │  ────────────────   │
                        │  application.yml    │
                        │  service-a.yml      │
                        │  service-a-prod.yml │
                        │  service-b.yml      │
                        └──────────┬──────────┘
                                   │ (clone/pull)
                                   │
                        ┌──────────▼──────────┐
                        │   Config Server      │
                        │   Port: 8888         │
                        │                      │
                        │   GET /service-a/    │
                        │       prod           │
                        └──────────┬──────────┘
                                   │
                ┌──────────────────┼──────────────────┐
                │                  │                  │
       ┌────────▼────────┐ ┌──────▼─────────┐ ┌─────▼─────────┐
       │  Service A      │ │  Service B     │ │  Service C    │
       │  (Config        │ │  (Config       │ │  (Config      │
       │   Client)       │ │   Client)      │ │   Client)     │
       └─────────────────┘ └────────────────┘ └───────────────┘
```

### Config Resolution Order

```
1. Service starts → reads bootstrap.yml
2. bootstrap.yml mein Config Server URL milta hai
3. Service Config Server se HTTP call karta hai:
     GET http://config-server:8888/{application}/{profile}/{label}
4. Config Server Git pull karta hai → properties return karta hai (JSON/YAML/Properties)
5. Service properties merge karta hai apne local + remote config se
6. Spring context boot
```

### URL Convention

```
GET /{application}/{profile}/{label}

Examples:
GET /order-service/dev/main      ← order-service, dev profile, main branch
GET /order-service/prod          ← prod profile, default label (main)
GET /order-service/default       ← only application defaults
```

Other formats:
```
/{application}-{profile}.yml
/{application}-{profile}.properties
/{application}-{profile}.json
/{label}/{application}-{profile}.yml
```

---

## Config Server Setup

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
    <artifactId>spring-cloud-config-server</artifactId>
  </dependency>
  
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
</dependencies>
```

### Main Application Class

```java
package com.example.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
```

### `application.yml`

```yaml
server:
  port: 8888

spring:
  application:
    name: config-server
  
  cloud:
    config:
      server:
        git:
          uri: https://github.com/myorg/config-repo.git
          default-label: main
          clone-on-start: true
          search-paths:
            - '{application}'
            - shared
          # Authentication (for private repos)
          username: ${GIT_USERNAME}
          password: ${GIT_TOKEN}
        
        # Health check config
        health:
          repositories:
            order-service:
              label: main
              name: order-service
              profiles: prod

# Basic auth for config server itself
spring.security.user:
  name: configuser
  password: ${CONFIG_SERVER_PASSWORD}

management:
  endpoints:
    web:
      exposure:
        include: health, info, env, refresh
```

### Test Config Server

```bash
curl -u configuser:secret \
  http://localhost:8888/order-service/dev/main

# Returns JSON
{
  "name": "order-service",
  "profiles": ["dev"],
  "label": "main",
  "version": "abc123...",
  "propertySources": [
    {
      "name": "https://github.com/.../order-service-dev.yml",
      "source": {
        "database.url": "jdbc:postgres://...",
        "log.level": "DEBUG"
      }
    },
    {
      "name": "https://github.com/.../order-service.yml",
      "source": { ... }
    },
    {
      "name": "https://github.com/.../application.yml",
      "source": { ... }
    }
  ]
}
```

---

## Git Backend

### Repository Structure

```
config-repo/
├── application.yml                    ← shared by all services
├── application-dev.yml                ← shared, dev only
├── application-prod.yml               ← shared, prod only
├── order-service.yml                  ← order-service defaults
├── order-service-dev.yml              ← order-service dev overrides
├── order-service-prod.yml             ← order-service prod overrides
├── payment-service.yml
├── payment-service-prod.yml
└── shared/
    └── common-secrets.yml
```

### Property Resolution Order (Highest to Lowest)

```
1. {application}-{profile}.yml         (most specific)
2. {application}.yml
3. application-{profile}.yml
4. application.yml                     (fallback)
```

→ Lower-priority defaults can be overridden by higher-priority specific.

### Example: `application.yml` (Shared)

```yaml
server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health, info, refresh

spring:
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
        jdbc:
          time_zone: UTC

logging:
  level:
    root: INFO
```

### Example: `order-service.yml`

```yaml
spring:
  application:
    name: order-service
  
  datasource:
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20

order:
  max-items-per-order: 100
  default-currency: INR
  cancellation-window-minutes: 30
```

### Example: `order-service-prod.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://prod-db.internal:5432/orders
    username: order_app
    password: '{cipher}AQA...'    ← encrypted!
    hikari:
      maximum-pool-size: 50

logging:
  level:
    com.example.order: INFO

order:
  cancellation-window-minutes: 15      ← override default for prod
```

### Multiple Repositories (Pattern Matching)

```yaml
spring:
  cloud:
    config:
      server:
        git:
          uri: https://github.com/myorg/default-config.git    # default
          repos:
            payment:
              pattern: payment-*
              uri: https://github.com/myorg/payment-config.git
              # Separate repo for sensitive payment configs
            
            secure:
              pattern: '*/prod'
              uri: https://github.com/myorg/secure-config.git
              # All prod profiles from secure repo
```

### Branch Strategies

```
main                ← prod baseline
staging             ← staging branch
dev                 ← dev experiments
feature/xyz         ← feature-specific
```

Use `label` parameter to fetch from specific branch:
```
GET /order-service/dev/feature-xyz
```

---

## Config Client Setup

### Dependencies (Service Side)

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-config</artifactId>
</dependency>

<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### `bootstrap.yml` (or `application.yml` with import)

#### Old Way — `bootstrap.yml`

```yaml
spring:
  application:
    name: order-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:dev}
  
  cloud:
    config:
      uri: http://config-server:8888
      username: configuser
      password: ${CONFIG_SERVER_PASSWORD}
      fail-fast: true               # don't start if config server unavailable
      retry:
        initial-interval: 1000
        max-attempts: 6
        multiplier: 1.5
```

#### New Way — Spring Boot 2.4+ `spring.config.import`

```yaml
# application.yml
spring:
  application:
    name: order-service
  
  config:
    import: optional:configserver:http://configuser:secret@config-server:8888
  
  cloud:
    config:
      fail-fast: true
```

→ More flexible, doesn't need separate `bootstrap.yml`.

### Config Client in Action

```java
@Service
public class OrderService {

    @Value("${order.max-items-per-order}")
    private int maxItems;
    
    @Value("${order.default-currency}")
    private String defaultCurrency;
    
    public void place(Order order) {
        if (order.getItems().size() > maxItems) {
            throw new IllegalArgumentException("Too many items");
        }
        // ...
    }
}
```

→ Properties are resolved from Config Server at startup.

### `@ConfigurationProperties` (Type-Safe)

```java
@Component
@ConfigurationProperties(prefix = "order")
@Data
public class OrderProperties {
    private int maxItemsPerOrder;
    private String defaultCurrency;
    private int cancellationWindowMinutes;
}
```

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderProperties props;
    
    public void place(Order o) {
        if (o.getItems().size() > props.getMaxItemsPerOrder()) ...
    }
}
```

---

## bootstrap.yml vs application.yml

### Historical (Before Spring Boot 2.4)

| File | Loaded When | Purpose |
|------|------------|---------|
| `bootstrap.yml` | **Earliest** — before main context | Config Server URL, security creds, encryption keys |
| `application.yml` | Standard Spring Boot context | Application properties |

### Why `bootstrap.yml` Existed

Service ko Config Server ka URL chahiye **before** main `application.yml` properties resolve hote hain. So:

```
1. Spring Boot starts
2. Reads bootstrap.yml      ← needs Config Server location here
3. Connects to Config Server
4. Pulls remote application.yml properties
5. Reads local application.yml (lower priority)
6. Merges → main context boots
```

### Modern Approach (Spring Boot 2.4+)

`bootstrap.yml` deprecated in favor of `spring.config.import`:

```yaml
# application.yml — single file
spring:
  application:
    name: order-service
  config:
    import:
      - optional:configserver:http://config-server:8888
      - optional:vault:secret/order-service
```

→ Simpler, more flexible, supports multiple sources.

#### Re-enable Bootstrap (If Needed)

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
```

→ Adding this dependency back enables `bootstrap.yml` again.

### When to Use Which?

| Scenario | Use |
|----------|-----|
| Spring Boot 2.4+ new project | `spring.config.import` |
| Legacy upgrade | Keep `bootstrap.yml` until migration |
| Multi-source (Config Server + Vault + K8s ConfigMap) | `spring.config.import` (cleaner) |

---

## Profiles & Environment-Specific Config

### Profile Activation

```bash
# Via env var
SPRING_PROFILES_ACTIVE=prod java -jar order-service.jar

# Via JVM arg
java -Dspring.profiles.active=prod -jar order-service.jar

# Via application.yml
spring:
  profiles:
    active: prod
```

### Multiple Profiles

```bash
SPRING_PROFILES_ACTIVE=prod,asia,monitoring
```

→ Properties merged: rightmost has highest priority for matching keys.

### Profile-Specific Files in Git Repo

```
order-service.yml             ← always loaded
order-service-dev.yml         ← dev only
order-service-staging.yml     ← staging only
order-service-prod.yml        ← prod only
order-service-prod-asia.yml   ← prod + asia profile
```

### Profile Groups (Spring Boot 2.4+)

```yaml
# application.yml
spring:
  profiles:
    group:
      production:
        - prod
        - monitoring
        - audit
```

→ Activate `production` → activates `prod`, `monitoring`, `audit` automatically.

### Conditional Beans

```java
@Service
@Profile("prod")
public class ProductionPaymentGateway implements PaymentGateway { ... }

@Service
@Profile({"dev", "test"})
public class MockPaymentGateway implements PaymentGateway { ... }
```

### Environment Indicator in Logs

```yaml
# application.yml
logging:
  pattern:
    level: "%5p [${spring.application.name},${spring.profiles.active}]"
```

```
2024-05-08 14:23:11 INFO [order-service,prod] OrderService - Order placed
```

---

## Encryption / Decryption

### Why Encrypt?

DB passwords, API keys in Git **plain-text** = security risk. Spring Cloud Config supports **encrypted values** committed to Git, decrypted on retrieval.

### Setup — Symmetric Encryption (Easy)

```yaml
# Config Server application.yml
encrypt:
  key: my-super-secret-32-character-key
```

⚠️ **Never** commit this key — env var only:

```bash
ENCRYPT_KEY=<generated-32-char-key> java -jar config-server.jar
```

### Symmetric Endpoints

```bash
# Encrypt a value
curl -u user:pass -X POST \
  --data-urlencode "myDbPassword123" \
  http://localhost:8888/encrypt

# Returns:
AQAxxxxxxxxxxxxxxxxxxxx....

# Decrypt (admin only — usually disabled in prod)
curl -u user:pass -X POST \
  --data-urlencode "AQAxxx...." \
  http://localhost:8888/decrypt
```

### Storing Encrypted Values

```yaml
# order-service-prod.yml in Git
spring:
  datasource:
    password: '{cipher}AQAxxxxxxxxxxxxxxxxxxxx....'
    
external-api:
  key: '{cipher}AQByyyyyy...'
```

→ Config Server **automatically decrypts** before sending to client.

### Asymmetric Encryption (Production)

JKS keystore for stronger security + key separation:

```bash
# Generate keystore
keytool -genkeypair -alias config-server-key \
  -keyalg RSA -keysize 2048 -validity 365 \
  -keystore config-server.jks \
  -storepass changeme -keypass changeme \
  -dname "CN=Config Server, OU=Eng, O=Example, L=BLR, ST=KA, C=IN"
```

```yaml
# Config Server application.yml
encrypt:
  key-store:
    location: classpath:/config-server.jks
    password: ${KEYSTORE_PASSWORD}
    alias: config-server-key
    secret: ${KEYSTORE_SECRET}
```

### Key Rotation

```yaml
encrypt:
  key-store:
    location: classpath:/config-server.jks
  
# Use cipher with key alias  
spring:
  cloud:
    config:
      server:
        encrypt:
          enabled: true
```

```yaml
# In config file — explicit alias
db.password: '{cipher}{key:config-server-key-v2}AQA...'
```

→ Re-encrypt with new alias, decommission old gradually.

### Disable Decryption Endpoints

```yaml
spring:
  cloud:
    config:
      server:
        encrypt:
          enabled: false      # don't auto-decrypt at server, send {cipher}... to client
```

→ Client decrypts locally with shared key. Useful for **never** logging plain secrets at server.

---

## Refresh Scope (@RefreshScope)

**Matlab:** `@Value` properties **don't auto-refresh** when Config Server updates. Need explicit refresh.

### The Problem

```java
@Service
public class OrderService {
    @Value("${order.max-items}")
    private int maxItems;   // resolved at startup, frozen
}
```

→ Change in Git → Config Server reflects immediately, but service has old value until restart.

### `@RefreshScope` to the Rescue

```java
@Service
@RefreshScope
public class OrderService {
    @Value("${order.max-items}")
    private int maxItems;
}
```

→ Bean is **proxied**; on `/actuator/refresh` it's recreated with new values.

### Triggering Refresh

```bash
# POST to /actuator/refresh
curl -X POST http://order-service:8080/actuator/refresh

# Response — list of changed properties
[
  "order.max-items",
  "order.default-currency"
]
```

→ All `@RefreshScope` beans re-created with new values.

### Enable Refresh Endpoint

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, refresh
```

### What Gets Refreshed?

✅ `@Value("${prop}")` in `@RefreshScope` beans
✅ `@ConfigurationProperties` (auto-refresh)
✅ Spring `Environment`

❌ Beans without `@RefreshScope` — old values cached
❌ Connections, pools, caches — generally not (need careful handling)

### `@ConfigurationProperties` (Auto-Refresh)

```java
@Component
@ConfigurationProperties(prefix = "order")
@Data
public class OrderProperties {
    private int maxItemsPerOrder;
    private String defaultCurrency;
}
```

→ Automatically re-bound on `/refresh` — no `@RefreshScope` needed!

### Things That Don't Refresh Easily

| Property | Why |
|----------|-----|
| DataSource URL | Connection pool already initialized |
| Server port | Tomcat already bound |
| Logging level | Use Spring Boot's `/loggers` endpoint |
| Cache size | Cache constructed at boot |

→ For these, **restart** is honest answer.

### Refresh Pattern with Health-Check

```java
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final ContextRefresher refresher;
    
    @PostMapping("/config/refresh")
    public Set<String> refresh() {
        return refresher.refresh();
    }
}
```

### Manual Bean Refresh

```java
@Autowired
ContextRefresher refresher;

public void doRefresh() {
    Set<String> changed = refresher.refresh();
    log.info("Refreshed properties: {}", changed);
}
```

---

## Spring Cloud Bus

**Problem:** 50 services running. Config update karna hai. Har service par manually `/actuator/refresh` POST karna painful.

**Solution:** **Spring Cloud Bus** — broadcast refresh event via message broker (RabbitMQ / Kafka) to all listeners.

### Architecture

```
                    ┌─────────────┐
                    │   Git Repo  │
                    └──────┬──────┘
                           │ webhook
                           ▼
                    ┌─────────────────┐
                    │  Config Server  │
                    │   /monitor      │
                    └────────┬────────┘
                             │ publish "RefreshRemoteApplicationEvent"
                             ▼
                    ┌─────────────────┐
                    │   RabbitMQ /    │
                    │   Kafka topic   │
                    │ springCloudBus  │
                    └────────┬────────┘
                             │ broadcast
            ┌────────────────┼────────────────┐
            │                │                │
       ┌────▼────┐      ┌────▼─────┐    ┌────▼─────┐
       │Service A│      │Service B │    │Service C │
       │ /refresh│      │ /refresh │    │ /refresh │
       └─────────┘      └──────────┘    └──────────┘
       all auto-refresh!
```

### Setup — RabbitMQ Backend

#### Server + All Clients Add

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-bus-amqp</artifactId>
</dependency>
```

#### Configure RabbitMQ

```yaml
spring:
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: ${RABBIT_PASSWORD}

management:
  endpoints:
    web:
      exposure:
        include: health, info, refresh, busrefresh
```

### Trigger Bus Refresh

```bash
# Send refresh event to ALL services on the bus
curl -X POST http://config-server:8888/actuator/busrefresh

# Or limit to specific app
curl -X POST http://config-server:8888/actuator/busrefresh/order-service
curl -X POST http://config-server:8888/actuator/busrefresh/order-service:8080
```

### Webhook from Git → Auto Refresh

```yaml
spring:
  cloud:
    config:
      server:
        monitor:
          # Watches the Git repo and triggers /busrefresh
          fixedDelay: 60000
          enabled: true
```

#### GitHub Webhook

```
GitHub → POST → Config Server /monitor
                       │
                       ▼
           publishes RefreshRemoteApplicationEvent
                       │
                       ▼
                Bus broadcasts to all services
```

```yaml
spring:
  cloud:
    config:
      server:
        monitor:
          # Allowed origins
          enabled: true
```

→ Configure GitHub webhook → POST `https://config-server:8888/monitor` on push to config-repo.

### Kafka Backend

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-bus-kafka</artifactId>
</dependency>
```

```yaml
spring:
  cloud:
    stream:
      kafka:
        binder:
          brokers: kafka:9092
```

### Custom Events on Bus

```java
public class CustomRemoteEvent extends RemoteApplicationEvent {
    private final String message;
    
    public CustomRemoteEvent(Object source, String origin, String message) {
        super(source, origin);
        this.message = message;
    }
    
    public String getMessage() { return message; }
}

@Component
public class CustomEventListener {
    @EventListener
    public void handle(CustomRemoteEvent event) {
        log.info("Received: {}", event.getMessage());
    }
}
```

```java
@Autowired ApplicationEventPublisher publisher;

publisher.publishEvent(new CustomRemoteEvent(this, "order-service", "Cache invalidate"));
```

### When to Use Spring Cloud Bus

✅ Many services (10+)
✅ Frequent config updates
✅ Infrastructure for messaging exists already (Rabbit/Kafka)

❌ Few services — manual refresh okay
❌ No messaging infra — overhead of adding broker just for this

---

## Other Backends (Vault, JDBC, Native)

### Native (Filesystem)

For dev/testing — config from local files:

```yaml
spring:
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          search-locations: file:///configs
          # or classpath:/configs
```

### Vault Backend (Secrets)

```yaml
spring:
  profiles:
    active: vault
  cloud:
    config:
      server:
        vault:
          host: vault.internal
          port: 8200
          scheme: https
          backend: secret
          default-key: application
          token: ${VAULT_TOKEN}
```

→ Service requests `/order-service/prod` → Config Server fetches from `secret/order-service/prod`.

### Composite Backend (Multiple Sources)

```yaml
spring:
  profiles:
    active: git, vault
  cloud:
    config:
      server:
        composite:
          - type: git
            uri: https://github.com/myorg/config.git
          - type: vault
            host: vault.internal
            port: 8200
```

→ Config Server queries both, merges results.

### JDBC Backend

```yaml
spring:
  profiles:
    active: jdbc
  
  datasource:
    url: jdbc:postgresql://...
    username: ...
    password: ...
  
  cloud:
    config:
      server:
        jdbc:
          sql: "SELECT KEY, VALUE FROM PROPERTIES WHERE APPLICATION=? AND PROFILE=? AND LABEL=?"
```

```sql
CREATE TABLE PROPERTIES (
  APPLICATION VARCHAR(50),
  PROFILE VARCHAR(50),
  LABEL VARCHAR(50),
  KEY VARCHAR(100),
  VALUE TEXT
);
```

→ Useful when teams want admin UI to manage configs.

---

## Security & Production Hardening

### 1. Authenticate Config Server Endpoints

```yaml
spring.security.user:
  name: configuser
  password: ${CONFIG_SERVER_PASSWORD}
```

→ Clients use Basic auth.

### 2. Use OAuth2 / mTLS for Inter-Service

For production: OAuth2 client credentials or mTLS instead of Basic.

### 3. Encrypt Sensitive Values

Never plain-text passwords in Git. Use `{cipher}...`.

### 4. Restrict Git Repo Access

- Private repo
- Limit who can push (CODEOWNERS)
- Branch protection (PR + reviews + tests)

### 5. Disable Decrypt Endpoint in Prod

```yaml
spring:
  cloud:
    config:
      server:
        encrypt:
          enabled: false   # never expose /decrypt
```

### 6. Rate Limit + Health-Check IPs

Use API gateway / firewall to restrict who can hit Config Server.

### 7. HA Config Server

Multiple instances behind load balancer (stateless — Git is source of truth).

```
LB → ConfigServer-1 → Git
LB → ConfigServer-2 → Git
LB → ConfigServer-3 → Git
```

### 8. Cache Git

Server clones Git locally; configure refresh cadence:

```yaml
spring:
  cloud:
    config:
      server:
        git:
          refresh-rate: 30   # seconds
          timeout: 5
```

→ Avoid hammering GitHub on every client request.

### 9. Self-Hosted Config Repo

For air-gapped / regulated: GitLab self-managed / Bitbucket Server / Gitea.

### 10. Audit & Logging

- Git log = who changed what + when
- Config Server access logs
- Alert on prod-affecting changes (CODEOWNERS approval gates)

---

## Monitoring & Health

### Actuator Endpoints

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, env, refresh, busrefresh, configprops
```

### Useful Endpoints

```bash
# Server health (incl. Git connectivity)
GET /actuator/health

# Current loaded properties
GET /actuator/env
GET /actuator/configprops

# Refresh
POST /actuator/refresh
POST /actuator/busrefresh

# Git refresh on server
POST /actuator/refresh
```

### Custom Health Indicator

```yaml
spring:
  cloud:
    config:
      server:
        health:
          repositories:
            order-service:
              label: main
              name: order-service
              profiles: prod
```

→ Health check tries fetching `/order-service/prod/main` to ensure Git accessible.

### Metrics

Spring Boot Actuator + Micrometer auto-exposes:

```
config.server.git.fetch.time   ← Git fetch duration
http.server.requests           ← /encrypt, /decrypt, etc.
```

→ Send to Prometheus / Datadog for dashboards.

---

## Common Pitfalls

### 1. Hardcoding Encrypt Key

```yaml
encrypt:
  key: my-actual-key   # ❌ committed to Git
```

→ Always env var.

### 2. Storing Plain Secrets

```yaml
# Git repo — order-service-prod.yml
db.password: superSecret123   # ❌
```

→ Use `{cipher}...` always.

### 3. Forgetting `@RefreshScope`

Config updated via `/refresh` but bean still has old value → because `@RefreshScope` missing.

### 4. Restarting Pool-Holding Beans

`@RefreshScope` on `DataSource` → on refresh, **all connections dropped** → app degraded. Be selective.

### 5. Bootstrap.yml Confusion (Spring Boot 2.4+)

```yaml
# bootstrap.yml — silently ignored without bootstrap starter
spring:
  cloud:
    config:
      uri: ...
```

→ Use `spring.config.import` or add `spring-cloud-starter-bootstrap`.

### 6. Config Server Single Point of Failure

If Config Server down + service tries to start → fail. Use:
- `fail-fast: true` + retry config
- Multiple Config Server instances behind LB
- Local fallback config

### 7. Profile Mismatch

Service running with `prod` profile but `application-prod.yml` typo'd as `application-Prod.yml` (case-sensitive on Linux) → properties not loaded.

### 8. Webhook Storms

Every Git push triggers refresh of all 100 services → connection storm. Throttle / batch.

### 9. Sensitive Logs

`/actuator/env` exposes properties — restrict access in prod:

```yaml
management:
  endpoint:
    env:
      keys-to-sanitize:
        - password
        - secret
        - key
        - token
        - credentials
```

### 10. Merging Surprises

Local `application.yml` overrides remote — confusion when "I changed Config Server but my service uses local". Disable local overrides for prod or document priority clearly.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Config Server** | `@EnableConfigServer`, port 8888 |
| **Backend** | Git (most common), Vault, JDBC, Native |
| **Client setup** | `spring.config.import: configserver:...` |
| **Profile** | `application-{profile}.yml` |
| **Encryption** | `{cipher}...` value, `encrypt.key` config |
| **Refresh** | `@RefreshScope` + `POST /actuator/refresh` |
| **Auto-refresh** | `@ConfigurationProperties` |
| **Bus** | RabbitMQ/Kafka → broadcast `/busrefresh` |
| **HA** | Multiple Config Servers, stateless |
| **Security** | Basic/OAuth2/mTLS + private Git + encrypt |

| Endpoint | Purpose |
|----------|---------|
| `GET /{app}/{profile}/{label}` | Get config |
| `POST /encrypt` | Encrypt value |
| `POST /decrypt` | Decrypt (disable in prod) |
| `POST /actuator/refresh` | Refresh single service |
| `POST /actuator/busrefresh` | Refresh all (with bus) |
| `POST /monitor` | Webhook from Git |

| Pattern | Use |
|---------|-----|
| Single source of truth | Git as backend |
| Versioned configs | Git history |
| Encryption | `{cipher}` for secrets |
| Refresh without restart | `@RefreshScope` + `/refresh` |
| Broadcast refresh | Spring Cloud Bus |
| Multi-source | `spring.config.import` |
| Audit | Git log |

---

## Practice

1. Set up a Config Server pointing to a local Git repo with `order-service.yml`.
2. Create a client service that reads `order.max-items` from Config Server.
3. Add a `prod` profile override; activate via env var.
4. Encrypt a DB password with `/encrypt`; commit `{cipher}...`; verify decryption.
5. Add `@RefreshScope` + change Git value + `POST /actuator/refresh` → confirm new value in service.
6. Set up RabbitMQ + Spring Cloud Bus; refresh multiple services with `/busrefresh`.
7. Configure Git webhook to auto-refresh on push.
8. Set up multiple Config Server instances behind nginx LB.
9. Switch to asymmetric encryption with JKS keystore.
10. Add Vault to composite backend for secrets.
