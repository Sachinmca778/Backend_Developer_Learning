# Spring Profiles

## Status: Not Started

---

## Table of Contents

1. [@Profile Annotation](#profile-annotation)
2. [spring.profiles.active](#springprofilesactive)
3. [Profile-specific Beans & Configs](#profile-specific-beans--configs)
4. [Testing with Specific Profiles](#testing-with-specific-profiles)
5. [Conditional Beans (@Conditional, @ConditionalOnProperty, @ConditionalOnClass)](#conditional-beans)

---

## @Profile Annotation

**Matlab:** Beans ya configurations ko **specific environments** ke liye enable/disable karna.

```java
// Development ke liye
@Component
@Profile("dev")
public class DevLogger implements Logger {
    public void log(String msg) {
        System.out.println("DEV LOG: " + msg);
    }
}

// Production ke liye
@Component
@Profile("prod")
public class ProdLogger implements Logger {
    public void log(String msg) {
        LoggerFactory.getLogger(getClass()).info(msg);
    }
}
```

### Multiple Profiles
```java
@Component
@Profile({"dev", "test"})  // Dev ya Test dono mein active hoga
public class TestDataService { }
```

### Negation
```java
@Component
@Profile("!prod")  // Production ke alawa sab mein active
public class DebugService { }
```

### Default Behavior
Agar koi `@Profile` annotation nahi lagata toh bean **sab profiles mein active** hoga.

```java
@Component  // Sab profiles mein active
public class CommonService { }
```

---

## spring.profiles.active

**Profile activate karne ke tarike:**

### 1. application.properties
```properties
spring.profiles.active=dev
```

### 2. Command Line
```bash
java -jar app.jar --spring.profiles.active=prod
```

### 3. Environment Variable
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar app.jar
```

### 4. Programmatically
```java
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Application.class);
        app.setAdditionalProfiles("dev", "h2");
        app.run(args);
    }
}
```

### 5. IDE Run Configuration
```
Run Configurations → Arguments → VM Arguments:
-Dspring.profiles.active=dev
```

### Multiple Profiles
```bash
java -jar app.jar --spring.profiles.active=dev,h2
```

---

## Profile-specific Beans & Configs

### @Configuration with @Profile
```java
@Configuration
@Profile("prod")
public class ProductionConfig {
    
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://prod:3306/mydb")
            .build();
    }
}

@Configuration
@Profile("dev")
public class DevConfig {
    
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:h2:mem:testdb")
            .build();
    }
}
```

### @Component with @Profile
```java
@Service
@Profile("prod")
public class RealPaymentService implements PaymentService { }

@Service
@Profile("dev")
public class MockPaymentService implements PaymentService { }
```

### Profile-specific Config Files
```
src/main/resources/
├── application.yml              # Common config
├── application-dev.yml          # Dev specific
├── application-prod.yml         # Prod specific
└── application-test.yml         # Test specific
```

**application.yml (Common):**
```yaml
app:
  name: My Application
```

**application-dev.yml:**
```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:h2:mem:testdb
```

**application-prod.yml:**
```yaml
server:
  port: 80
spring:
  datasource:
    url: jdbc:mysql://prod:3306/mydb
```

---

## Testing with Specific Profiles

### @ActiveProfiles
```java
@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testCreateUser() {
        // Test profile ke beans use honge
        userService.createUser(new User("test"));
    }
}
```

### Multiple Test Profiles
```java
@SpringBootTest
@ActiveProfiles({"test", "h2"})  // Multiple profiles
class UserRepositoryTest { }
```

### Test-specific Properties
```java
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.feature.enabled=true",
    "app.max-retries=3"
})
class FeatureTest { }
```

### application-test.yml
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:

app:
  feature:
    email-notifications: false  # Test mein emails band
```

---

## Conditional Beans

### @Conditional (Generic)

**Matlab:** Custom logic se decide karo bean create hoga ya nahi.

```java
// Custom Condition
public class LinuxCondition implements Condition {
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return System.getProperty("os.name").contains("Linux");
    }
}

@Configuration
public class OSConfig {
    
    @Bean
    @Conditional(LinuxCondition.class)
    public CommandLine linuxCommand() {
        return new LinuxCommand();
    }
    
    @Bean
    @Conditional(WindowsCondition.class)
    public CommandLine windowsCommand() {
        return new WindowsCommand();
    }
}
```

---

### @ConditionalOnProperty (Most Common)

**Matlab:** Property value dekh ke bean create karo.

```java
@Configuration
public class FeatureConfig {
    
    @Bean
    @ConditionalOnProperty(name = "app.feature.email.enabled", 
                           havingValue = "true", 
                           matchIfMissing = false)
    public EmailService emailService() {
        return new EmailService();
    }
    
    @Bean
    @ConditionalOnProperty(name = "app.feature.cache.enabled", 
                           havingValue = "true")
    public CacheService cacheService() {
        return new CacheService();
    }
}
```

**Properties:**
```properties
app.feature.email.enabled=true   # EmailService create hoga
app.feature.cache.enabled=false  # CacheService NAHI hoga
```

---

### @ConditionalOnClass

**Matlab:** Agar specific class classpath pe hai to bean banao.

```java
@Configuration
public class LibraryConfig {
    
    // Agar Redis library classpath pe hai to RedisConfig banao
    @Bean
    @ConditionalOnClass(RedisTemplate.class)
    public RedisTemplate<String, Object> redisTemplate() {
        return new RedisTemplate<>();
    }
    
    // Agar MongoDB driver classpath pe hai to MongoDBConfig banao
    @Bean
    @ConditionalOnClass(MongoClient.class)
    public MongoClient mongoClient() {
        return MongoClients.create();
    }
}
```

---

### @ConditionalOnMissingBean

**Matlab:** Agar bean already exist nahi karta to banao.

```java
@Configuration
public class DefaultConfig {
    
    // Agar user ne khud DataSource nahi banaya to default banao
    @Bean
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource defaultDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:h2:mem:testdb")
            .build();
    }
}
```

**Use Case:** Spring Boot auto-configuration internally yahi use karta hai - default beans provide karta hai, lekin user override kar sakta hai.

---

### @ConditionalOnExpression (SpEL)

**Matlab:** SpEL expression evaluate karke decide karo.

```java
@Configuration
public class ConditionalConfig {
    
    @Bean
    @ConditionalOnExpression("${app.feature.advanced.enabled} && ${app.environment} == 'prod'")
    public AdvancedService advancedService() {
        return new AdvancedService();
    }
}
```

---

### @ConditionalOnProperty vs @Profile

| Feature | @Profile | @ConditionalOnProperty |
|---------|----------|------------------------|
| **Granularity** | Coarse (entire environment) | Fine-grained (per bean) |
| **Flexibility** | Limited (predefined profiles) | Very flexible (any property) |
| **Use Case** | Dev/Test/Prod separation | Feature flags |

### Together Use Karna
```java
@Component
@Profile("prod")
@ConditionalOnProperty(name = "app.feature.monitoring", havingValue = "true")
public class MonitoringService {
    // Sirf production mein, aur sirf jab monitoring enabled ho
}
```

---

## Quick Reference

```java
// Profiles
@Profile("dev")
@Profile({"dev", "test"})
@Profile("!prod")

// Activate Profile
spring.profiles.active=dev
java -jar app.jar --spring.profiles.active=prod
export SPRING_PROFILES_ACTIVE=prod

// Testing
@ActiveProfiles("test")
@SpringBootTest
@ActiveProfiles({"test", "h2"})

// Conditional Beans
@ConditionalOnProperty(name = "feature.enabled", havingValue = "true")
@ConditionalOnClass(RedisTemplate.class)
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnExpression("${prop} == 'value'")
```

---

## Summary

| Feature | Use Case |
|---------|----------|
| **@Profile** | Environment-based bean selection (dev/test/prod) |
| **spring.profiles.active** | Active profile set karna (properties, CLI, env var) |
| **Profile-specific Configs** | Alag environments ke liye alag config files |
| **@ConditionalOnProperty** | Feature flags (property value pe bean control) |
| **@ConditionalOnClass** | Optional library support |
| **@ConditionalOnMissingBean** | Default beans (allow user override) |
| **@ConditionalOnExpression** | Complex SpEL-based conditions |
