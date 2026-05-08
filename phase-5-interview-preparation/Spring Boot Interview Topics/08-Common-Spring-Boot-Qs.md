# Common Spring Boot Questions

## Status: Not Started

---

## Table of Contents

1. [Multiple Data Sources](#multiple-data-sources)
2. [Read/Write Routing DataSource](#readwrite-routing-datasource)
3. [Spring Boot 2 vs 3 Differences](#spring-boot-2-vs-3-differences)
4. [Embedded Server Customization](#embedded-server-customization)
5. [`CommandLineRunner` vs `ApplicationRunner`](#commandlinerunner-vs-applicationrunner)
6. [Externalized Configuration Order](#externalized-configuration-order)
7. [Profiles](#profiles)
8. [Startup Optimization](#startup-optimization)
9. [Spring Boot Native (AOT / GraalVM)](#spring-boot-native-aot--graalvm)
10. [Common Output Traps](#common-output-traps)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## Multiple Data Sources

Common need — primary OLTP DB + analytics DB / multiple tenants.

### Properties

```yaml
spring:
  datasource:
    primary:
      jdbc-url: jdbc:postgresql://primary:5432/app
      username: ...
      password: ...
      hikari.maximum-pool-size: 20
    
    secondary:
      jdbc-url: jdbc:postgresql://reports:5432/analytics
      username: ...
      password: ...
      hikari.maximum-pool-size: 10
```

### Beans

```java
@Configuration
public class DataSourceConfig {

    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "secondaryDataSource")
    @ConfigurationProperties("spring.datasource.secondary")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    // Per-DS EntityManager + TX manager
    @Primary
    @Bean(name = "primaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean primaryEmf(
            EntityManagerFactoryBuilder builder,
            @Qualifier("primaryDataSource") DataSource ds) {
        return builder.dataSource(ds).packages("com.example.primary").build();
    }

    @Bean(name = "secondaryEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean secondaryEmf(
            EntityManagerFactoryBuilder builder,
            @Qualifier("secondaryDataSource") DataSource ds) {
        return builder.dataSource(ds).packages("com.example.secondary").build();
    }

    @Primary
    @Bean(name = "primaryTransactionManager")
    public PlatformTransactionManager primaryTx(
            @Qualifier("primaryEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean(name = "secondaryTransactionManager")
    public PlatformTransactionManager secondaryTx(
            @Qualifier("secondaryEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
```

### Repositories

```java
@EnableJpaRepositories(
    basePackages = "com.example.primary",
    entityManagerFactoryRef = "primaryEntityManagerFactory",
    transactionManagerRef = "primaryTransactionManager"
)

@EnableJpaRepositories(
    basePackages = "com.example.secondary",
    entityManagerFactoryRef = "secondaryEntityManagerFactory",
    transactionManagerRef = "secondaryTransactionManager"
)
```

### Service usage

```java
@Service
class S {
    @Transactional("secondaryTransactionManager")
    public void run() { ... }
}
```

⚠️ **No XA/2PC by default** — distributed TX requires JTA. Most apps avoid by using **outbox pattern** (cross-ref Microservices folder).

---

## Read/Write Routing DataSource

```java
public class RoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly()
            ? "replica" : "primary";
    }
}

@Bean
public DataSource dataSource(@Qualifier("primary") DataSource primary,
                              @Qualifier("replica") DataSource replica) {
    RoutingDataSource ds = new RoutingDataSource();
    ds.setTargetDataSources(Map.of("primary", primary, "replica", replica));
    ds.setDefaultTargetDataSource(primary);
    return ds;
}
```

→ `@Transactional(readOnly = true)` → routes to replica.

⚠️ Replication lag — eventual consistency caveats.

---

## Spring Boot 2 vs 3 Differences

| Aspect | Spring Boot 2 | Spring Boot 3 |
|--------|--------------|---------------|
| Java baseline | 8/11/17 | **17 minimum**, 21 recommended |
| Namespace | `javax.*` | **`jakarta.*`** (Servlet, JPA, Validation, etc.) |
| Spring Framework | 5.x | **6.x** |
| Spring Security | 5.x (older DSL) | **6.x** (lambda DSL strongly emphasized; `WebSecurityConfigurerAdapter` removed) |
| Observability | Sleuth + Brave | **Micrometer Tracing** (Brave/OTel) |
| HTTP trace endpoint | `httptrace` | `httpexchanges` |
| Native | Experimental | **First-class GraalVM Native + AOT** |
| Logging | Logback | Logback (continued) |
| Metrics | Micrometer 1.x | Micrometer 1.10+ with native histograms |

### Migration big-rocks

1. **Java 17 upgrade**.
2. **Find/replace `javax.persistence` → `jakarta.persistence`**, similarly Servlet, Validation, Annotation, MailSender, etc.
3. Spring Security DSL refactor (`WebSecurityConfigurerAdapter` deleted).
4. Sleuth → Micrometer Tracing dependencies swap.
5. Verify property keys (some renamed; deprecation report on startup).

→ `spring-boot-properties-migrator` dependency helps detect renamed properties.

---

## Embedded Server Customization

### Default

Tomcat (servlet) — Jetty / Undertow / Netty (reactive) optional.

### Switch

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-tomcat</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### Common props

```yaml
server:
  port: 8080
  address: 0.0.0.0
  servlet:
    context-path: /api
  tomcat:
    threads:
      max: 200
      min-spare: 10
    connection-timeout: 20000
    max-connections: 8192
    accept-count: 100
```

### Programmatic

```java
@Bean
public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
    return factory -> {
        factory.addConnectorCustomizers(connector -> {
            connector.setAttribute("relaxedQueryChars", "[]|{}^`\"<>");
        });
    };
}
```

---

## `CommandLineRunner` vs `ApplicationRunner`

```java
@Component
class Init implements CommandLineRunner {
    public void run(String... args) { ... }
}

@Component
class Init2 implements ApplicationRunner {
    public void run(ApplicationArguments args) {
        if (args.containsOption("init")) ...
    }
}
```

| | CommandLineRunner | ApplicationRunner |
|--|-------------------|-------------------|
| Args | `String[]` | `ApplicationArguments` (parsed flags) |
| Multi | yes (`@Order`) | yes |
| When | After context startup | Same |

→ `ApplicationRunner` slightly newer + nicer API.

⚠️ **Don't put long-running tasks** here (blocks startup, breaks readiness probe).

---

## Externalized Configuration Order

Spring Boot order (highest priority first — top wins):

```
1. Devtools global settings
2. @TestPropertySource
3. Command-line args (--server.port=9090)
4. SPRING_APPLICATION_JSON env / property
5. ServletConfig / ServletContext init params
6. JNDI java:comp/env
7. Java System properties (-Dkey=val)
8. OS Environment variables
9. RandomValuePropertySource (random.*)
10. Profile-specific application-{profile}.properties OUTSIDE jar
11. Profile-specific INSIDE jar
12. application.properties OUTSIDE jar
13. application.properties INSIDE jar
14. @PropertySource on @Configuration
15. Default properties (SpringApplication.setDefaultProperties)
```

→ Memorize top 4-5 for interview: **CLI args > env vars > application.properties**.

### Profiles

```yaml
spring:
  profiles:
    active: prod
```

```bash
java -jar app.jar --spring.profiles.active=prod
SPRING_PROFILES_ACTIVE=prod java -jar app.jar
```

`application-prod.yml` overrides `application.yml` for matching keys.

### YAML profile-specific docs

```yaml
# application.yml
spring:
  application.name: orders
---
spring:
  config.activate.on-profile: prod
server.port: 80
---
spring:
  config.activate.on-profile: dev
server.port: 8080
```

---

## Startup Optimization

### Lazy Initialization

```yaml
spring.main.lazy-initialization: true
```

→ Beans created on first use → faster startup. But errors deferred to runtime.

### Conditional auto-config exclusion

```yaml
spring.autoconfigure.exclude:
  - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

→ If feature unused.

### Reduce classpath

Avoid heavy starters not used (e.g., Web + WebFlux both).

### Native (AOT / GraalVM)

`spring-boot-starter-parent` 3.x supports:

```bash
./mvnw -Pnative native:compile
```

→ Native image: startup **<100ms**, lower memory; build slow + reflection limitations.

### JVM tuning at startup

```bash
-XX:+UseG1GC -Xms512m -Xmx512m
-XX:TieredStopAtLevel=1     # for short-lived JVMs (CLI tools)
```

→ Cross-ref: `Performance & Optimization / 01-JVM-Performance-Tuning.md`.

### Class Data Sharing (CDS) — Java 21+

Helps cold start.

---

## Spring Boot Native (AOT / GraalVM)

### Pros

- Startup ~100ms
- Tiny memory footprint
- Single binary

### Cons

- No JIT improvements at runtime
- Reflection / proxies need hints
- Build time long (minutes)
- Less mature ecosystem (some libs not compatible)

### Annotations

```java
@RegisterReflectionForBinding(MyDto.class)
@ImportRuntimeHints(MyHints.class)
```

→ For cloud functions / CLI tools / serverless — strong fit.

---

## Common Output Traps

### Q1. Two DataSources, no `@Primary`

`NoUniqueBeanDefinitionException` — Spring Data JPA can't pick.

### Q2. `@Transactional` without manager qualifier in multi-DS

Default uses primary; secondary DS writes outside any TX → silent commit / inconsistency.

### Q3. Boot 2 → 3 namespace

`javax.persistence.Entity` → `jakarta.persistence.Entity` (compile error — missing import).

### Q4. CommandLineRunner blocking

```java
@Component
class Slow implements CommandLineRunner {
    public void run(String... a) { Thread.sleep(60_000); }
}
// /actuator/health/readiness fails for 60s
```

### Q5. Lazy init + missing bean

```yaml
spring.main.lazy-initialization: true
```

→ Wrong bean wiring not caught until first use.

---

## Pitfalls

1. **Multi-DS without explicit `@Transactional("...")`** — wrong DS used.
2. **Boot 2→3 mixed `javax`/`jakarta`** — runtime ClassCastException.
3. **Sleuth left in Boot 3** — replace with Micrometer Tracing.
4. **`WebSecurityConfigurerAdapter` in Boot 3** — class deleted.
5. **CommandLineRunner long-running** — blocks startup; use `@Async` task or scheduled job.
6. **Profile typo** — silent fallback to default.
7. **Embedded server thread pool default 200** — load test before assuming.
8. **Native build flakiness** — reflection hints missing → runtime fail.
9. **`spring.main.lazy-initialization=true`** in production — runtime errors instead of startup.
10. **`@ConfigurationProperties` not bound** — missing `@EnableConfigurationProperties` / scan.

---

## Cheat Sheet

| Topic | Quick |
|-------|-------|
| Multi-DS | `@Primary` + qualified beans + per-EMF/TXM |
| Read/write split | AbstractRoutingDataSource |
| Boot 2→3 baseline | Java 17, jakarta.* |
| Embedded server | Tomcat default; switchable; tune via `server.tomcat.*` |
| CLR / AR | Run after startup; non-blocking only |
| Config order | CLI args > env > app.yml |
| Lazy init | Faster startup, deferred errors |
| Native | <100ms startup, reflection hints required |

---

## Practice

1. Set up multi-DS app — primary + reporting; verify queries land on correct DB.
2. Implement read/write routing DS; verify `readOnly=true` hits replica.
3. Migrate small Boot 2.7 app to Boot 3 — observe `javax → jakarta` rename + Security DSL changes.
4. Switch from Tomcat to Undertow; run load test; compare.
5. Add `CommandLineRunner` for DB seeding (dev profile only).
6. Enable `lazy-initialization` and observe startup time delta.
7. Try native build: `./mvnw -Pnative native:compile`; benchmark startup.
