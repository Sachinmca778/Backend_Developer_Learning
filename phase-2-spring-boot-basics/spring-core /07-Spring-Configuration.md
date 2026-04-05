# Spring Configuration

## Status: Not Started

---

## Table of Contents

1. [@Configuration + @Bean (Java Config)](#configuration--bean-java-config)
2. [application.properties / application.yml](#applicationproperties--applicationyml)
3. [Externalized Config (@Value, @ConfigurationProperties, Environment)](#externalized-config)
4. [Profile-specific Configs](#profile-specific-configs)
5. [@PropertySource](#propertysource)

---

## @Configuration + @Bean (Java Config)

**Matlab:** Java classes se beans configure karna. XML configuration ka modern replacement.

```java
@Configuration  // Yeh class bean definitions contain karti hai
public class AppConfig {
    
    @Bean  // Method ka return value ek bean banega
    public RestTemplate restTemplate() {
        RestTemplate template = new RestTemplate();
        template.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return template;
    }
    
    // @Bean methods ke parameters automatically inject hote hain
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);  // DataSource auto-inject
    }
}
```

### @Bean Lifecycle Methods
```java
@Configuration
public class AppConfig {
    
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server server() {
        return new Server();
    }
}

public class Server {
    public void start() { System.out.println("Server started"); }
    public void stop() { System.out.println("Server stopped"); }
}
```

### @Bean with Parameters
```java
@Configuration
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("pass");
        return ds;
    }
    
    // DataSource automatically inject hoga
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    // Multiple dependencies
    @Bean
    public OrderService orderService(DataSource dataSource, 
                                     JdbcTemplate jdbcTemplate) {
        return new OrderService(dataSource, jdbcTemplate);
    }
}
```

### @Component vs @Bean

| Feature | @Component | @Bean |
|---------|-----------|-------|
| **Kahan lagta hai** | Class pe | Method pe |
| **Control** | Spring khud object banata hai | Aap manually object return karte ho |
| **Use Case** | Apni classes ke liye | Third-party classes ke liye |
| **Customization** | Limited | Full control over bean creation |

### Third-party Bean Example
```java
// Third-party class - aap @Component nahi laga sakte
public class ExternalCache {
    private int maxSize;
    
    public ExternalCache(int maxSize) {
        this.maxSize = maxSize;
    }
}

// Solution: @Bean use karo
@Configuration
public class CacheConfig {
    
    @Bean
    public ExternalCache externalCache() {
        return new ExternalCache(1000);  // Manual configuration possible
    }
}
```

---

## application.properties / application.yml

**Matlab:** External configuration files - values change karni hon toh code nahi, sirf properties file change karo.

### application.properties (Key-Value Format)
```properties
# application.properties
app.name=My Application
app.version=1.0.0
server.port=8081

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=pass

# Custom
app.feature.new-users-enabled=true
app.max-upload-size=10MB
```

### application.yml (Hierarchical Format)
```yaml
# application.yml
app:
  name: My Application
  version: 1.0.0

server:
  port: 8081

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: pass

app:
  feature:
    new-users-enabled: true
  max-upload-size: 10MB
```

**properties vs yml:** Functionally same, but YAML hierarchical hai - related config group karna easy hai.

---

## Externalized Config

### @Value (Simple Values)

```java
@Component
public class AppInfo {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.version}")
    private String version;
    
    @Value("${server.port}")
    private int serverPort;
    
    @Value("${app.feature.new-users-enabled:false}")
    private boolean newUsersEnabled;  // Default value: false
    
    @Value("${app.non-existent:default-value}")
    private String customValue;  // Property nahi milega to "default-value" use hoga
}
```

### SpEL with @Value
```java
@Value("#{systemProperties['user.home']}")
private String userHome;

@Value("#{100 * 1024 * 1024}")  // 100MB
private long maxFileSize;

@Value("#{T(java.lang.Math).random()}")
private double randomValue;
```

---

### @ConfigurationProperties (Type-Safe - Recommended)

**Best practice for multiple properties.** Ek dedicated class banao jo configuration map kare.

```java
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {
    private String name;
    private String version;
    private long maxUploadSize;
    private Feature feature = new Feature();
    
    // Getters and setters (zaruri hain!)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Feature getFeature() { return feature; }
    public void setFeature(Feature feature) { this.feature = feature; }
    
    // Nested properties
    public static class Feature {
        private boolean newUsersEnabled;
        
        public boolean isNewUsersEnabled() { return newUsersEnabled; }
        public void setNewUsersEnabled(boolean enabled) { 
            this.newUsersEnabled = enabled; 
        }
    }
}
```

**Properties file:**
```properties
app.name=MyApp
app.version=1.0.0
app.max-upload-size=10485760
app.feature.new-users-enabled=true
```

### @Value vs @ConfigurationProperties

| Feature | @Value | @ConfigurationProperties |
|---------|--------|--------------------------|
| **Type-Safe** | ❌ No (String injection) | ✅ Yes (Strong typing) |
| **Validation** | ❌ No | ✅ Yes (`@Validated`, JSR-303) |
| **Refactoring** | Difficult (string keys) | Easy (IDE autocomplete) |
| **Best For** | Few properties | Many related properties |
| **Relaxed Binding** | ❌ No | ✅ Yes (`app.name` = `APP_NAME`) |

**Rule:** `@ConfigurationProperties` use karo jab 3+ related properties hon. `@Value` sirf 1-2 values ke liye.

---

### Environment Interface

Programmatic property access:

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
    
    public boolean isFeatureEnabled() {
        return env.getProperty("app.feature.new-users-enabled", Boolean.class, false);
    }
}
```

---

## Profile-specific Configs

Different environments ke liye different config files:

```
src/main/resources/
├── application.yml              # Common config (sab environments ke liye)
├── application-dev.yml          # Development specific
├── application-prod.yml         # Production specific
└── application-test.yml         # Testing specific
```

### application.yml (Common)
```yaml
app:
  name: My Application
  feature:
    logging: true
```

### application-dev.yml
```yaml
server:
  port: 8081

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:
```

### application-prod.yml
```yaml
server:
  port: 80

spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

**Active profile set karo:**
```properties
# application.properties
spring.profiles.active=dev
```

---

## @PropertySource

Custom properties file load karna:

```java
@Configuration
@PropertySource("classpath:custom.properties")
public class CustomConfig {
    
    @Value("${custom.property}")
    private String customValue;
}
```

### Multiple Property Files
```java
@Configuration
@PropertySource({
    "classpath:database.properties",
    "classpath:security.properties"
})
public class MultiConfig { }
```

### @PropertySource with YAML
```java
@Configuration
@PropertySource("classpath:app-config.properties")
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig { }
```

---

## Quick Reference

```java
// Java Config
@Configuration
@Bean
@Bean(initMethod = "start", destroyMethod = "stop")

// Property Files
application.properties
application.yml

// Externalized Config
@Value("${app.name}")
@Value("${app.name:default}")
@ConfigurationProperties(prefix = "app")
Environment env.getProperty("key")

// Custom Properties
@PropertySource("classpath:custom.properties")
```

---

## Summary

| Method | Best For |
|--------|----------|
| **@Configuration + @Bean** | Third-party beans, custom bean creation |
| **@Value** | 1-2 simple properties |
| **@ConfigurationProperties** | Multiple related properties (type-safe) |
| **Environment** | Programmatic access |
| **@PropertySource** | Custom properties files |
