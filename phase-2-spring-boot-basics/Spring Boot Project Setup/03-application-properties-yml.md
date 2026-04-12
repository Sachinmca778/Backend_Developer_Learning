# application.properties / application.yml

## Status: Not Started

---

## Table of Contents

1. [Properties vs YAML](#properties-vs-yaml)
2. [application.properties (Key-Value Format)](#applicationproperties-key-value-format)
3. [application.yml (Hierarchical YAML Format)](#applicationyml-hierarchical-yaml-format)
4. [Server Configuration](#server-configuration)
5. [Datasource Configuration](#datasource-configuration)
6. [JPA Properties](#jpa-properties)
7. [Logging Levels](#logging-levels)
8. [Custom Properties](#custom-properties)
9. [YAML Multi-Document Files (---)](#yaml-multi-document-files)
10. [Relaxed Binding](#relaxed-binding)
11. [Property Placeholders (${...})](#property-placeholders)
12. [Reading Properties in Code](#reading-properties-in-code)

---

## Properties vs YAML

**application.properties** aur **application.yml** dono configuration files hain — application settings externalize karne ke liye. Functionally same hain, bas syntax different hai.

| Feature | .properties | .yml |
|---------|------------|------|
| **Syntax** | Flat key-value | Hierarchical (nested) |
| **Readability** | Repetitive prefixes | Cleaner, grouped |
| **Multi-document** | ❌ No | ✅ Yes (`---` separator) |
| **Lists** | `key[0]=val, key[1]=val` | Natural list syntax |
| **Comments** | ✅ `#` | ✅ `#` |

---

## application.properties (Key-Value Format)

```properties
# Server Configuration
server.port=8081
server.servlet.context-path=/api

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=pass
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate Properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Logging Levels
logging.level.root=INFO
logging.level.com.example.myapp=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Custom Properties
app.name=My Application
app.version=1.0.0
app.feature.email-enabled=true
app.max-upload-size=10MB
```

---

## application.yml (Hierarchical YAML Format)

```yaml
server:
  port: 8081
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: pass
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

logging:
  level:
    root: INFO
    com.example.myapp: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

app:
  name: My Application
  version: 1.0.0
  feature:
    email-enabled: true
  max-upload-size: 10MB
```

---

## Server Configuration

```properties
# Port change karna ho
server.port=8081

# Context path (base URL)
server.servlet.context-path=/api

# Error page details
server.error.include-message=always
server.error.include-binding-errors=always
server.error.include-stacktrace=on-param

# Tomcat specific settings
server.tomcat.threads.max=200
server.tomcat.connection-timeout=10000

# SSL/HTTPS
server.port=8443
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
server.ssl.key-alias=tomcat
```

---

## Datasource Configuration

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=pass
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=postgres
spring.datasource.password=pass
spring.datasource.driver-class-name=org.postgresql.Driver

# H2 (In-Memory, Testing)
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.username=sa
spring.datasource.password=
spring.datasource.driver-class-name=org.h2.Driver

# H2 Console enable (browser mein DB dekhne ke liye)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

---

## JPA Properties

```properties
# Hibernate DDL Auto Options
# create: Har baar naya schema banao (dev only)
# update: Schema update karo, data lose nahi hoga (dev/staging)
# create-drop: Create karo, shutdown pe drop karo (testing only)
# validate: Schema validate karo, changes nahi karo (production)
# none: Kuch nahi karo (manual migration use karo)
spring.jpa.hibernate.ddl-auto=update

# SQL logging
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Hibernate Dialect (usually auto-detected)
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Lazy loading (performance optimization)
spring.jpa.open-in-view=false

# Second Level Cache
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

### DDL Auto Options Explained

| Option | Behavior | Use Case |
|--------|----------|----------|
| **create** | Har baar naya schema banao (data delete hoga) | Development only |
| **update** | Schema update karo, data preserve rehta hai | Development/Staging |
| **create-drop** | Start pe create, shutdown pe drop | Testing only |
| **validate** | Schema validate karo, changes nahi karo | Production |
| **none** | Kuch nahi karo (manual migration) | Production (Flyway/Liquibase) |

---

## Logging Levels

```properties
# Root log level (sabse basic)
logging.level.root=INFO

# Package-specific levels
logging.level.com.example.myapp=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE

# Log file mein likho
logging.file.name=logs/myapp.log

# Ya log directory (spring.log banega)
logging.file.path=logs/

# Log pattern customize karo
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### Log Levels (Low to High)

| Level | Use Case |
|-------|----------|
| **TRACE** | Sabse detailed — har chhoti detail |
| **DEBUG** | Development mein detailed debugging |
| **INFO** | Default — important events (startup, shutdown) |
| **WARN** | Potential issues (deprecated API usage) |
| **ERROR** | Errors (exceptions, failures) |
| **OFF** | Logging disable |

---

## Custom Properties

```properties
# Custom application properties
app.name=My Application
app.version=1.0.0
app.description=Welcome to ${app.name}
app.feature.email-enabled=true
app.max-upload-size=10MB
app.support-email=support@example.com
```

---

## YAML Multi-Document Files

Ek hi file mein multiple profiles define kar sakte ho `---` separator se.

```yaml
# application.yml (common config)
server:
  port: 8080
app:
  name: My Application

---
# Dev profile (embedded in same file)
spring:
  config:
    activate:
      on-profile: dev
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:
logging:
  level:
    com.example: DEBUG

---
# Prod profile (embedded in same file)
spring:
  config:
    activate:
      on-profile: prod
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
logging:
  level:
    com.example: WARN
```

---

## Relaxed Binding

Spring Boot property names ko flexibly match karta hai — exact case match zaruri nahi.

| YAML | Java Field | Works? |
|------|-----------|--------|
| `user-name` | `userName` | ✅ |
| `user_name` | `userName` | ✅ |
| `USERNAME` | `userName` | ✅ |
| `user.name` | `userName` | ❌ (dot = nested) |

```yaml
# YAML mein
my:
  app:
    user-name: sachin
    user_name: sachin
    userName: sachin
    USERNAME: sachin
```

```java
// Sab bind honge is field pe
@ConfigurationProperties(prefix = "my.app")
public class MyAppProperties {
    private String userName;  // Ya user_name, USERNAME — sab kaam karega
}
```

---

## Property Placeholders (${...})

Properties file ke andar dusri properties reference kar sakte ho.

```properties
app.name=MyApp
app.description=Welcome to ${app.name} - Version ${app.version}
app.version=1.0.0

# Environment variable se value lo
spring.datasource.password=${DB_PASSWORD:defaultpass}
# Agar DB_PASSWORD env var nahi hai to "defaultpass" use hoga

# System property
app.home=${user.home}/.myapp
```

```yaml
# YAML mein bhi same
app:
  name: MyApp
  description: "Welcome to ${app.name} - Version ${app.version}"
  datasource:
    password: ${DB_PASSWORD:defaultpass}
```

---

## Reading Properties in Code

### Method 1: @Value (Simple, 1-2 properties)

```java
@Component
public class AppConfig {

    @Value("${app.name}")
    private String appName;

    @Value("${app.feature.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${app.max-upload-size:10MB}")
    private String maxUploadSize;

    // Default value ke saath
    @Value("${app.unknown.property:defaultValue}")
    private String unknownProperty;
}
```

### Method 2: @ConfigurationProperties (Type-Safe, Recommended for many)

```java
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {
    private String name;
    private String version;
    private long maxUploadSize;
    private Feature feature = new Feature();

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public static class Feature {
        private boolean emailEnabled;
        public boolean isEmailEnabled() { return emailEnabled; }
        public void setEmailEnabled(boolean enabled) { this.emailEnabled = enabled; }
    }
}
```

### Method 3: Environment Interface (Programmatic)

```java
@Component
public class ConfigService {

    private final Environment env;

    public ConfigService(Environment env) {
        this.env = env;
    }

    public String getAppName() {
        return env.getProperty("app.name");
    }

    public int getServerPort() {
        return env.getProperty("server.port", Integer.class, 8080);  // Default: 8080
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **.properties vs .yml** | Functionally same — YAML hierarchical hai, cleaner dikhta hai |
| **Server Config** | `server.port`, `server.servlet.context-path` |
| **Datasource** | `spring.datasource.url`, `username`, `password` |
| **JPA** | `spring.jpa.hibernate.ddl-auto` — production mein `validate` use karo |
| **Logging** | `logging.level.{package}=DEBUG` — package-specific levels set karo |
| **Multi-document YAML** | `---` se ek file mein multiple profiles define kar sakte ho |
| **Relaxed Binding** | `user-name`, `user_name`, `USERNAME` sab same field pe map hote hain |
| **Placeholders** | `${property:default}` — dusri properties ya env vars reference karo |
| **@Value** | Simple, 1-2 properties ke liye |
| **@ConfigurationProperties** | Type-safe, multiple related properties ke liye |
| **Environment** | Programmatic access jab runtime pe chahiye |
