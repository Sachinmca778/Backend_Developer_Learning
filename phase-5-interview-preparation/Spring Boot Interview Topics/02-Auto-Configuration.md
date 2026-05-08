# Auto-Configuration

## Status: Not Started

---

## Table of Contents

1. [Auto-Configuration Kya Hai?](#auto-configuration-kya-hai)
2. [`@SpringBootApplication` Breakdown](#springbootapplication-breakdown)
3. [Discovery — `spring.factories` vs `AutoConfiguration.imports`](#discovery--springfactories-vs-autoconfigurationimports)
4. [Conditional Annotations](#conditional-annotations)
5. [Property-Based Conditions](#property-based-conditions)
6. [Order of Auto-Configuration](#order-of-auto-configuration)
7. [Custom Auto-Configuration Step-by-Step](#custom-auto-configuration-step-by-step)
8. [Configuration Properties](#configuration-properties)
9. [Debugging Auto-Config](#debugging-auto-config)
10. [Excluding Auto-Configurations](#excluding-auto-configurations)
11. [Common Output Traps](#common-output-traps)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Auto-Configuration Kya Hai?

**Matlab:** Spring Boot **classpath dekh kar** sensible default beans automatically register kar deta — DataSource, JPA, MVC, Kafka, Redis — sab "convention over configuration" se mil jata.

> "I added `spring-boot-starter-data-jpa` aur DataSource auto-banaya gaya — kaise?"

---

## `@SpringBootApplication` Breakdown

```java
@SpringBootApplication       // ← composite of three
public class App { }
```

Breakdown:

```java
@SpringBootConfiguration     // ≈ @Configuration with marker semantics
@EnableAutoConfiguration     // turns on auto-config
@ComponentScan               // scans package + sub-packages
public @interface SpringBootApplication { }
```

### `@EnableAutoConfiguration` ka kaam

→ Internally `@Import(AutoConfigurationImportSelector.class)` use karta hai jo classpath se auto-config classes load karta hai.

---

## Discovery — `spring.factories` vs `AutoConfiguration.imports`

### Pre Spring Boot 2.7

```
META-INF/spring.factories
```

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyAutoConfiguration,\
com.example.OtherAutoConfiguration
```

### Spring Boot 2.7+ (preferred)

```
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

```
com.example.MyAutoConfiguration
com.example.OtherAutoConfiguration
```

→ One class per line — simpler, easier IDE support.

### Boot 3.x

`spring.factories` deprecated for auto-config (still used for some other extension points). Use **`AutoConfiguration.imports`**.

---

## Conditional Annotations

| Annotation | Purpose |
|------------|---------|
| `@ConditionalOnClass` | Class on classpath |
| `@ConditionalOnMissingClass` | Class **not** on classpath |
| `@ConditionalOnBean` | Specified bean exists |
| `@ConditionalOnMissingBean` | No such bean (very common — "register only if user hasn't") |
| `@ConditionalOnProperty` | Property matches value |
| `@ConditionalOnExpression` | SpEL expression true |
| `@ConditionalOnWebApplication` | Web context |
| `@ConditionalOnNotWebApplication` | Non-web |
| `@ConditionalOnResource` | File on classpath |
| `@ConditionalOnSingleCandidate` | Exactly one bean of type |

### Example

```java
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "app.db", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyDataSourceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(Properties props) {
        return new HikariDataSource(...);
    }
}
```

→ "Class hai + property `app.db.enabled=true`" satisfy ho to register; user ne already `DataSource` bean diya to skip.

---

## Property-Based Conditions

```java
@ConditionalOnProperty(
    prefix = "feature.cache",
    name = "type",
    havingValue = "redis",
    matchIfMissing = false
)
```

Useful for **toggling features**:

```yaml
feature:
  cache:
    type: redis      # or caffeine
```

---

## Order of Auto-Configuration

```java
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@AutoConfiguration(before = JpaRepositoriesAutoConfiguration.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
```

→ Auto-configs ek dependency graph banti hain — order chahiye to declare karo.

---

## Custom Auto-Configuration Step-by-Step

### Goal

`my-feature-spring-boot-starter` library — user `pom.xml` mein add kare, **bean automatically register**.

### Steps

1. Properties class

```java
@ConfigurationProperties(prefix = "my.feature")
public class MyFeatureProperties {
    private boolean enabled = true;
    private String host = "localhost";
    // getters/setters
}
```

2. Auto-config class (Boot 3 style)

```java
@AutoConfiguration
@ConditionalOnClass(MyFeatureClient.class)
@ConditionalOnProperty(prefix = "my.feature", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(MyFeatureProperties.class)
public class MyFeatureAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MyFeatureClient myFeatureClient(MyFeatureProperties props) {
        return new MyFeatureClient(props.getHost());
    }
}
```

3. Imports file

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.example.feature.MyFeatureAutoConfiguration
```

4. (Optional) Metadata for IDE hints — `META-INF/additional-spring-configuration-metadata.json`.

### Result

User adds the dependency → `MyFeatureClient` bean automatically available, configurable via `application.yml`.

---

## Configuration Properties

```java
@ConfigurationProperties(prefix = "app")
public record AppProperties(String name, int retries, Duration timeout) { }
```

```yaml
app:
  name: orders
  retries: 3
  timeout: 5s
```

Enable:

```java
@EnableConfigurationProperties(AppProperties.class)
```

OR (if class is `@Component`):

```java
@ConfigurationPropertiesScan
```

→ Type-safe, validation-friendly (`@Validated`), records-friendly.

---

## Debugging Auto-Config

### Auto-config report on startup

```bash
java -jar app.jar --debug
```

Output sections:

- **Positive matches** (auto-configs activated)
- **Negative matches** (skipped, with reason — class missing, property false)
- **Exclusions**
- **Unconditional classes**

### Actuator endpoint

```yaml
management.endpoints.web.exposure.include: conditions
```

```
GET /actuator/conditions
```

Same info via REST.

### `ConditionEvaluationReport` programmatically (advanced)

---

## Excluding Auto-Configurations

### Annotation

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
```

### Property

```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

→ Useful when no DB but `spring-data-jpa` accidentally pulled.

---

## Common Output Traps

### Q1. Two `DataSource` beans

If you provide a `DataSource` bean, Boot's `DataSourceAutoConfiguration` `@ConditionalOnMissingBean` triggers → no second bean. ✅

But if both auto-configs match (e.g., HikariCP + Tomcat) — Boot prefers based on classpath order; check.

### Q2. Property typo

```yaml
my.featur.enabled: true   # typo — silently ignored
```

→ IDE metadata hints help (`additional-spring-configuration-metadata.json`).

### Q3. `@ConditionalOnMissingBean` of interface

Boot 2.x looked at declared bean type; if you registered concrete type but condition checks interface — both register. Use type carefully.

---

## Pitfalls

1. **Pre-2.7 `spring.factories` use** — works but deprecated; migrate to imports file.
2. **Auto-config in main app package** — works but better as starter library.
3. **Eagerly creating expensive beans** in auto-config — increases startup; lazy where possible.
4. **No `@ConditionalOnMissingBean`** — overrides user customization → "why is my bean ignored?".
5. **Wrong property prefix** in `@ConfigurationProperties` — silent ignore.
6. **Multiple matching auto-configs** without `@AutoConfigureOrder` → ordering surprises.
7. **Forgot `@EnableConfigurationProperties`** for properties class — bean not registered.
8. **`exclude` typo** — silent: still loads.

---

## Cheat Sheet

| Item | Boot ≥ 2.7 |
|------|------------|
| Discovery file | `META-INF/spring/...AutoConfiguration.imports` |
| Class annotation | `@AutoConfiguration` |
| Conditional missing bean | `@ConditionalOnMissingBean` |
| Conditional on classpath | `@ConditionalOnClass` |
| Conditional property | `@ConditionalOnProperty` |
| Order | `@AutoConfiguration(after=, before=)` |
| Properties bind | `@ConfigurationProperties` + `@EnableConfigurationProperties` |
| Debug | `--debug` / `/actuator/conditions` |
| Exclude | annotation `exclude` / `spring.autoconfigure.exclude` |

---

## Practice

1. Existing app → `--debug` → identify 5 positive + 5 negative matches.
2. Custom starter banayo (greeter library) — bean auto-registered when dependency added.
3. `@ConditionalOnMissingBean` test: user provides own bean → auto-config skip.
4. Property toggle (`havingValue`) for enabling/disabling feature.
5. Exclude a dataSource auto-config; show error; provide manual config.
