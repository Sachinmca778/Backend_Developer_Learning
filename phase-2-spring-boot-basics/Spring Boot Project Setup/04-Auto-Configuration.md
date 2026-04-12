# Auto-Configuration

## Status: Not Started

---

## Table of Contents

1. [Auto-Configuration Overview](#auto-configuration-overview)
2. [How Auto-Configuration Works](#how-auto-configuration-works)
3. [Conditional Annotations](#conditional-annotations)
4. [DataSource Auto-Configuration Example](#datasource-auto-configuration-example)
5. [Common Auto-Configurations](#common-auto-configurations)
6. [Excluding Auto-Configurations](#excluding-auto-configurations)
7. [Debugging Auto-Configuration](#debugging-auto-configuration)
8. [Creating Custom Auto-Configuration](#creating-custom-auto-configuration)

---

## Auto-Configuration Overview

**Auto-Configuration** Spring Boot ki sabse powerful feature hai — classpath dekh ke automatically beans configure ho jaate hain.

### The Magic

```
Spring Boot application starts
    ↓
@EnableAutoConfiguration activate hota hai
    ↓
Spring Boot spring.factories / AutoConfiguration.imports file read karta hai
    ↓
Har auto-configuration class check hoti hai
    ↓
Conditional annotations evaluate hote hain (@ConditionalOnClass, @ConditionalOnMissingBean)
    ↓
Agar conditions match karein → Bean create ho jata hai
    ↓
User ne khud bean banaya hai → Auto-config backs off
```

---

## How Auto-Configuration Works

### @SpringBootApplication Breakdown

```
@SpringBootApplication = @Configuration 
                       + @EnableAutoConfiguration 
                       + @ComponentScan
```

| Annotation | Kaam |
|------------|------|
| **@Configuration** | Yeh class ek configuration class hai |
| **@EnableAutoConfiguration** | Spring Boot ko bolta hai: "Mujhe auto-configure karo" |
| **@ComponentScan** | `@Component`, `@Service`, `@Repository` classes ko scan karta hai |

### Registration Files

**Spring Boot 2.x (spring.factories):**
```
# META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
    com.example.MyAutoConfiguration,\
    com.example.AnotherAutoConfiguration
```

**Spring Boot 3+ (AutoConfiguration.imports):**
```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.MyAutoConfiguration
com.example.AnotherAutoConfiguration
```

---

## Conditional Annotations

Yeh annotations decide karte hain ki auto-configuration run hoga ya nahi.

| Annotation | Matlab |
|------------|--------|
| `@ConditionalOnClass` | Agar yeh class classpath pe hai to configure karo |
| `@ConditionalOnMissingClass` | Agar yeh class nahi hai to configure karo |
| `@ConditionalOnBean` | Agar yeh bean already exist karta hai |
| `@ConditionalOnMissingBean` | Agar yeh bean nahi hai to configure karo |
| `@ConditionalOnProperty` | Agar yeh property set hai |
| `@ConditionalOnWebApplication` | Agar yeh web application hai |
| `@ConditionalOnExpression` | SpEL expression evaluate karo |

### Conditional Annotations Examples

| Annotation | Use Case |
|------------|----------|
| `@ConditionalOnClass` | Agar DataSource classpath pe hai to DataSource configure karo |
| `@ConditionalOnMissingBean` | Agar user ne khud DataSource nahi banaya to default banao |
| `@ConditionalOnProperty` | Agar `app.feature.enabled=true` hai to feature enable karo |
| `@ConditionalOnWebApplication` | Agar yeh web app hai to DispatcherServlet configure karo |

---

## DataSource Auto-Configuration Example

```java
@Configuration
@ConditionalOnClass(DataSource.class)           // Agar DataSource classpath pe hai
@ConditionalOnMissingBean(DataSource.class)     // Aur user ne khud DataSource nahi banaya
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create()
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
    }
}
```

### Samjho:

1. Aapne `spring-boot-starter-data-jpa` add kiya
2. Classpath pe `DataSource` class aa gayi
3. Aapne `application.properties` mein DB URL, username, password diya
4. Spring Boot ne automatically DataSource bean bana diya
5. **Aapko kuch manually configure nahi karna pada!**

---

## Common Auto-Configurations

| Starter | What it Auto-configures |
|---------|------------------------|
| `spring-boot-starter-web` | DispatcherServlet, Jackson (JSON), Embedded Tomcat |
| `spring-boot-starter-data-jpa` | DataSource, EntityManager, TransactionManager |
| `spring-boot-starter-security` | Authentication, CSRF, Default login page |
| `spring-boot-starter-thymeleaf` | Template engine, view resolver |
| `spring-boot-starter-actuator` | Health, metrics, info endpoints |

---

## Excluding Auto-Configurations

### Method 1: @SpringBootApplication exclude attribute

```java
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    SecurityAutoConfiguration.class
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### Method 2: Properties file mein

```properties
spring.autoconfigure.exclude=\
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

### Method 3: @EnableAutoConfiguration exclude

```java
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class AppConfig { }
```

---

## Debugging Auto-Configuration

### Enable Debug Mode

```properties
# application.properties
debug=true
```

Ya command line se:
```bash
java -jar myapp.jar --debug
```

### Condition Evaluation Report

**Condition Evaluation Report** milega jo dikhata hai:

- ✅ **Positive matches** — Kya configure hua aur kyun
- ❌ **Negative matches** — Kya configure nahi hua aur kyun
- 📋 **Unconditional classes** — Bina conditions ke configurations

### Example Output

```
Positive matches:
-----------------

   DataSourceAutoConfiguration matched:
      - @ConditionalOnClass found required class 'javax.sql.DataSource'
      - @ConditionalOnMissingBean found no beans of type 'DataSource'

Negative matches:
-----------------

   DataSourceAutoConfiguration did not match:
      - @ConditionalOnMissingBean found existing DataSource bean
```

---

## Creating Custom Auto-Configuration

### Step 1: Configuration Class

```java
@Configuration
@ConditionalOnClass(MyService.class)
@ConditionalOnMissingBean(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyServiceAutoConfiguration {

    @Bean
    public MyService myService(MyProperties properties) {
        return new MyService(properties.getName(), properties.getTimeout());
    }
}
```

### Step 2: Properties Class

```java
@ConfigurationProperties(prefix = "my.service")
public class MyProperties {
    private String name;
    private long timeout = 5000;  // Default value

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getTimeout() { return timeout; }
    public void setTimeout(long timeout) { this.timeout = timeout; }
}
```

### Step 3: Registration File

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.MyServiceAutoConfiguration
```

### Step 4: Usage

```properties
# application.properties
my.service.name=MyCustomService
my.service.timeout=10000
```

```java
// Ab auto-configured MyService use kar sakte ho
@Service
@RequiredArgsConstructor
public class ConsumerService {
    private final MyService myService;  // Automatically injected
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **@EnableAutoConfiguration** | Spring Boot ko bolta hai: "Mujhe auto-configure karo" |
| **Conditional Annotations** | Decide karte hain ki configuration run hoga ya nahi |
| **@ConditionalOnClass** | Class classpath pe hai to configure karo |
| **@ConditionalOnMissingBean** | Bean nahi hai to default configure karo |
| **spring.factories / AutoConfiguration.imports** | Auto-configuration classes register hote hain |
| **debug=true** | Condition Evaluation Report milega |
| **exclude attribute** | Specific auto-configurations disable kar sakte ho |
| **Custom Auto-Config** | Apni library ke liye auto-configuration bana sakte ho |
