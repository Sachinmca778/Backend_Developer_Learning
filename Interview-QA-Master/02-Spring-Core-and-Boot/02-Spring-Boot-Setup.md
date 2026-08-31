# Spring Boot Project Setup - Top 10 Interview Questions & Answers

> Covering: Project Structure, Spring Initializr, application.properties/yml, Auto-Configuration, and Maven/Gradle Build

---

## Q1: Spring Boot project ka standard structure kya hota hai?

**Answer:**

Spring Boot project ek predefined folder structure follow karta hai jo Maven/Gradle conventions pe based hai.

### Standard Project Structure:

```
my-spring-boot-app/
├── pom.xml                          # Maven build file (ya build.gradle for Gradle)
├── src/
│   ├── main/
│   │   ├── java/                    # Source code (application code)
│   │   │   └── com/example/myapp/
│   │   │       ├── MyAppApplication.java    # Main class (@SpringBootApplication)
│   │   │       ├── controller/      # REST Controllers / MVC Controllers
│   │   │       ├── service/         # Business logic (@Service)
│   │   │       ├── repository/      # Data access layer (@Repository)
│   │   │       ├── model/           # Entity classes / Domain models
│   │   │       ├── dto/             # Data Transfer Objects
│   │   │       ├── config/          # Configuration classes (@Configuration)
│   │   │       └── exception/       # Custom exceptions + handlers
│   │   └── resources/
│   │       ├── application.properties     # OR application.yml (config file)
│   │       ├── application-dev.properties # Profile-specific config
│   │       ├── application-prod.properties
│   │       ├── static/              # Static files (CSS, JS, images)
│   │       ├── templates/           # Thymeleaf / JSP templates
│   │       └── db/migration/        # Flyway/Liquibase migration scripts
│   └── test/
│       ├── java/                    # Test source code
│       │   └── com/example/myapp/
│       │       ├── controller/      # Controller tests (@WebMvcTest)
│       │       ├── service/         # Service tests
│       │       └── repository/      # Repository tests (@DataJpaTest)
│       └── resources/
│           └── application-test.properties  # Test-specific config
└── target/                          # Build output (compiled classes, JAR)
```

### Key Directories:

| Directory | Purpose |
|-----------|---------|
| **src/main/java** | Application source code — controllers, services, repositories |
| **src/main/resources** | Configuration files, templates, static assets |
| **src/test/java** | Unit tests, integration tests |
| **src/test/resources** | Test-specific configuration |
| **pom.xml / build.gradle** | Build configuration + dependencies |
| **target/** (Maven) / **build/** (Gradle) | Compiled output, packaged JAR/WAR |

### Main Class (@SpringBootApplication):

```java
package com.example.myapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication  // @Configuration + @EnableAutoConfiguration + @ComponentScan
public class MyAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyAppApplication.class, args);
    }
}
```

**Important:**
- Main class ko **root package** mein rakho (`com.example.myapp`)
- `@SpringBootApplication` automatically us package aur sub-packages ko scan karta hai
- Isliye controllers, services, repositories sub-packages mein hote hain

### Banner Customization:

Spring Boot start hone pe jo banner print hota hai usko customize kar sakte ho.

**Method 1: Text File**
```
# src/main/resources/banner.txt
  ____    _    ____   ___  ____
 / ___|  / \  |  _ \ |_ _||  _ \
 \___ \ / _ \ | |_) | | | | | | |
  ___) / ___ \|  __/  | | | |_| |
 |____/_/   \_\_|    |___||____/

Application Name: ${application.title}
Version: ${application.version}
Spring Boot: ${spring-boot.version}
```

**Method 2: Disable Banner**
```properties
# application.properties
spring.main.banner-mode=off
```

```java
// Or programmatically
SpringApplication app = new SpringApplication(MyAppApplication.class);
app.setBannerMode(Banner.Mode.OFF);
app.run(args);
```

**Method 3: Image Banner**
```
# src/main/resources/banner.png (ya banner.jpg, banner.gif)
# Properties:
spring.banner.image.location=classpath:banner.png
spring.banner.image.width=50
spring.banner.image.height=30
```

**Banner Variables:**
```
${application.title}     - Application title
${application.version}   - Version from pom.xml
${spring-boot.version}   - Spring Boot version
${spring-boot.formatted-version} - Formatted version
${AnsiColor.GREEN}       - ANSI color codes
```

---

## Q2: Spring Initializr kya hai aur project setup kaise karte hain?

**Answer:**

**Spring Initializr** (start.spring.io) ek web tool hai jo Spring Boot project scaffold karta hai — dependencies, build file, main class automatically generate ho jaati hai.

### How to Use:

**Step 1:** https://start.spring.io/ jaao

**Step 2:** Options select karo:

| Option | Choices | Recommendation |
|--------|---------|----------------|
| **Project** | Maven / Gradle | Maven (beginner), Gradle (large projects) |
| **Language** | Java / Kotlin / Groovy | Java (standard) |
| **Spring Boot** | Latest stable (3.x) | Hamesha latest stable version |
| **Project Metadata** | Group, Artifact, Name | `com.example`, `my-app`, `My Application` |
| **Packaging** | Jar / War | Jar (default, embedded server) |
| **Java Version** | 17 / 21 | Java 17 (LTS, Spring Boot 3 minimum) |

**Step 3:** Dependencies add karo:

| Dependency | Starter | Use Case |
|------------|---------|----------|
| Spring Web | `spring-boot-starter-web` | REST APIs, web apps |
| Spring Data JPA | `spring-boot-starter-data-jpa` | Database with Hibernate |
| Spring Security | `spring-boot-starter-security` | Authentication & Authorization |
| MySQL Driver | `mysql-connector-j` | MySQL database |
| H2 Database | `h2` | In-memory DB (testing) |
| Validation | `spring-boot-starter-validation` | Bean validation (@Valid) |
| Lombok | `lombok` | Boilerplate reduction |
| Spring Boot DevTools | `spring-boot-devtools` | Auto-reload during dev |
| Spring Boot Actuator | `spring-boot-starter-actuator` | Production monitoring |
| Thymeleaf | `spring-boot-starter-thymeleaf` | Server-side templates |

**Step 4:** "Generate" button click → ZIP download → Extract → IDE mein import karo

### Maven vs Gradle:

| Feature | Maven | Gradle |
|---------|-------|--------|
| **Config File** | `pom.xml` (XML) | `build.gradle` (Groovy/Kotlin DSL) |
| **Learning Curve** | Easy | Medium |
| **Build Speed** | Slower | Faster (incremental builds) |
| **Dependency Management** | Good | Better (conflict resolution) |
| **Standard in Enterprise** | ✅ Yes | Growing |
| **Convention over Configuration** | Less | More |

**Maven (pom.xml):**
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>My Application</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

**Gradle (build.gradle):**
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = '17'
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

### Group/Artifact/Name Conventions:

| Field | Example | Description |
|-------|---------|-------------|
| **Group** | `com.example`, `com.companyname` | Reverse domain name — package structure |
| **Artifact** | `my-app`, `user-service` | Project name (lowercase, hyphenated) |
| **Name** | `My Application` | Readable name |
| **Version** | `0.0.1-SNAPSHOT` | SNAPSHOT = development version |

### Importing into IntelliJ IDEA:

1. **File → Open** → Project folder select karo
2. **pom.xml** ya **build.gradle** pe right-click → "Import as Maven/Gradle Project"
3. IntelliJ automatically dependencies download karega
4. Main class pe right-click → "Run" → Application start ho jayegi

### Jar vs War:

| Feature | Jar (Default) | War |
|---------|---------------|-----|
| **Server** | Embedded (Tomcat) | External (deploy to Tomcat) |
| **Run** | `java -jar app.jar` | Deploy to application server |
| **Best for** | Microservices, standalone apps | Traditional enterprise deployments |
| **Spring Boot setup** | Zero config | Need `SpringBootServletInitializer` |

**War Configuration:**
```xml
<!-- pom.xml -->
<packaging>war</packaging>

<!-- Embedded server ko optional banao -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

```java
// Main class
public class MyAppApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MyAppApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(MyAppApplication.class, args);
    }
}
```

---

## Q3: application.properties aur application.yml mein kya difference hai? Kaise configure karte hain?

**Answer:**

**application.properties** aur **application.yml** dono configuration files hain — application settings externalize karne ke liye. Functionally same hain, bas syntax different hai.

### application.properties (Key-Value Format):

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

### application.yml (Hierarchical YAML Format):

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

### Properties vs YAML Comparison:

| Feature | .properties | .yml |
|---------|------------|------|
| **Syntax** | Flat key-value | Hierarchical (nested) |
| **Readability** | Repetitive prefixes | Cleaner, grouped |
| **Multi-document** | ❌ No | ✅ Yes (`---` separator) |
| **Lists** | `key[0]=val, key[1]=val` | Natural list syntax |
| **Comments** | ✅ `#` | ✅ `#` |

### YAML Multi-Document Files:

Ek hi file mein multiple profiles define kar sakte ho `---` separator se.

```yaml
# application.yml (common config)
server:
  port: 8080
app:
  name: My Application

---
# application-dev.yml (embedded in same file)
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
# application-prod.yml (embedded in same file)
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

### Relaxed Binding:

Spring Boot property names ko flexibly match karta hai — exact case match zaruri nahi.

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

| YAML | Java Field | Works? |
|------|-----------|--------|
| `user-name` | `userName` | ✅ |
| `user_name` | `userName` | ✅ |
| `USERNAME` | `userName` | ✅ |
| `user.name` | `userName` | ❌ (dot = nested) |

### Property Placeholders (${...}):

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

### Custom Properties Use Karna:

```java
// Method 1: @Value (simple, 1-2 properties)
@Component
public class AppConfig {

    @Value("${app.name}")
    private String appName;

    @Value("${app.feature.email-enabled:false}")
    private boolean emailEnabled;

    @Value("${app.max-upload-size:10MB}")
    private String maxUploadSize;
}

// Method 2: @ConfigurationProperties (type-safe, recommended for many)
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

---

## Q4: Spring Boot Auto-Configuration kaise kaam karta hai?

**Answer:**

**Auto-Configuration** Spring Boot ki sabse powerful feature hai — classpath dekh ke automatically beans configure ho jaate hain.

### How It Works:

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

### Auto-Configuration Registration:

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

### Conditional Annotations (The Real Magic):

| Annotation | Matlab |
|------------|--------|
| `@ConditionalOnClass` | Agar yeh class classpath pe hai to configure karo |
| `@ConditionalOnMissingClass` | Agar yeh class nahi hai to configure karo |
| `@ConditionalOnBean` | Agar yeh bean already exist karta hai |
| `@ConditionalOnMissingBean` | Agar yeh bean nahi hai to configure karo |
| `@ConditionalOnProperty` | Agar yeh property set hai |
| `@ConditionalOnWebApplication` | Agar yeh web application hai |
| `@ConditionalOnMissingBean` | Default bean provide karo (user override kar sakta hai) |

### Example: DataSource Auto-Configuration

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

**Samjho:**
1. Aapne `spring-boot-starter-data-jpa` add kiya
2. Classpath pe `DataSource` class aa gayi
3. Aapne `application.properties` mein DB URL, username, password diya
4. Spring Boot ne automatically DataSource bean bana diya
5. **Aapko kuch manually configure nahi karna pada!**

### Auto-Configuration Exclude Karna:

**Method 1: @SpringBootApplication exclude attribute**
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

**Method 2: Properties file mein**
```properties
spring.autoconfigure.exclude=\
    org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,\
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

**Method 3: @EnableAutoConfiguration exclude**
```java
@Configuration
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class AppConfig { }
```

### Debugging Auto-Configuration:

```properties
# application.properties
debug=true
```

Ya command line se:
```bash
java -jar myapp.jar --debug
```

**Condition Evaluation Report** milega jo dikhata hai:
- ✅ **Positive matches** — Kya configure hua aur kyun
- ❌ **Negative matches** — Kya configure nahi hua aur kyun
- 📋 **Unconditional classes** — Bina conditions ke configurations

**Example Output:**
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

### Custom Auto-Configuration Banana:

```java
// Spring Boot 3+ style
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

// Registration file
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.MyServiceAutoConfiguration
```

---

## Q5: Maven/Gradle build process kaise kaam karta hai? spring-boot-starter-parent kya hai?

**Answer:**

### spring-boot-starter-parent:

Yeh ek special Maven parent POM hai jo:

1. **Default Java version** set karta hai
2. **Dependency versions** manage karta hai (aapko version likhne ki zarurat nahi)
3. **Plugin configuration** provide karta hai
4. **Resource filtering** enable karta hai

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
</parent>
```

**Without starter-parent (dependency management only):**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.2.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Dependency Management:

Starter parent add karne ke baad — dependencies ka version likhne ki zarurat nahi.

```xml
<!-- Versions automatically managed by spring-boot-starter-parent -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <!-- Version nahi likhna — parent manage karta hai -->
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

**Version override karna ho toh:**
```xml
<properties>
    <java.version>17</java.version>
    <!-- Specific version override -->
    <spring-security.version>6.2.0</spring-security.version>
    <h2.version>2.2.224</h2.version>
</properties>
```

### spring-boot-maven-plugin:

Yeh plugin Spring Boot application ko **executable JAR/WAR** mein package karta hai.

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

**Plugin kya karta hai:**
1. **Repackage goal** — Normal JAR ko executable "fat JAR" mein convert karta hai
2. **Dependencies bundle** — Saari dependencies JAR ke andar include hoti hain
3. **Main class detect** — `@SpringBootApplication` class automatically detect hoti hai

**Fat JAR Structure:**
```
myapp.jar
├── META-INF/
│   └── MANIFEST.MF              # Main-Class: org.springframework.boot.loader.JarLauncher
├── BOOT-INF/
│   ├── classes/                 # Application code
│   │   └── com/example/myapp/
│   │       └── *.class
│   └── lib/                     # All dependencies (JARs)
│       ├── spring-web-6.1.0.jar
│       ├── spring-boot-3.2.0.jar
│       └── ...
└── org/
    └── springframework/boot/loader/   # Spring Boot loader classes
```

### Maven Build Lifecycle:

| Phase | Command | Kya hota hai |
|-------|---------|-------------|
| **Clean** | `mvn clean` | Target folder delete hota hai |
| **Compile** | `mvn compile` | Source code `.class` files mein compile hota hai |
| **Test** | `mvn test` | Unit tests run hote hain |
| **Package** | `mvn package` | JAR/WAR file banti hai |
| **Verify** | `mvn verify` | Integration tests run hote hain |
| **Install** | `mvn install` | Local Maven repository (`~/.m2`) mein install hota hai |
| **Deploy** | `mvn deploy` | Remote repository (Nexus/Artifactory) mein upload hota hai |

### Common Maven Commands:

```bash
# Clean + Compile
mvn clean compile

# Run tests
mvn test

# Package (JAR/WAR) — tests skip karke
mvn package -DskipTests

# Full build + install
mvn clean install

# Run application
mvn spring-boot:run

# Package with specific profile
mvn package -Pprod

# View dependency tree
mvn dependency:tree

# Check for dependency updates
mvn versions:display-dependency-updates
```

### Running Spring Boot Application:

```bash
# Method 1: Maven plugin se
mvn spring-boot:run

# Method 2: Packaged JAR se
java -jar target/myapp-0.0.1-SNAPSHOT.jar

# Method 3: With profile
java -jar target/myapp.jar --spring.profiles.active=prod

# Method 4: With JVM args
java -Xmx512m -jar target/myapp.jar

# Method 5: With debug flag
java -jar target/myapp.jar --debug
```

### Gradle Build (Alternative):

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.0'
    id 'io.spring.dependency-management' version '1.1.4'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}
```

**Gradle Commands:**
```bash
# Clean + Build
./gradlew clean build

# Run tests
./gradlew test

# Run application
./gradlew bootRun

# Build JAR (skip tests)
./gradlew bootJar -x test

# View dependencies
./gradlew dependencies
```

### Maven vs Gradle Commands:

| Task | Maven | Gradle |
|------|-------|--------|
| Clean build | `mvn clean package` | `./gradlew clean build` |
| Run app | `mvn spring-boot:run` | `./gradlew bootRun` |
| Skip tests | `mvn package -DskipTests` | `./gradlew bootJar -x test` |
| View deps | `mvn dependency:tree` | `./gradlew dependencies` |
| Install locally | `mvn install` | `./gradlew publishToMavenLocal` |

---

## Q6: Multi-module Maven project kaise setup karte hain?

**Answer:**

**Multi-module project** mein ek parent POM multiple child modules ko manage karta hai. Large applications ke liye useful hai.

### Project Structure:

```
my-company-app/
├── pom.xml                          # Parent POM
├── common-module/
│   ├── pom.xml
│   └── src/main/java/
├── user-service/
│   ├── pom.xml
│   └── src/main/java/
├── order-service/
│   ├── pom.xml
│   └── src/main/java/
└── api-gateway/
    ├── pom.xml
    └── src/main/java/
```

### Parent POM:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-company-app</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>  <!-- Important: packaging = pom -->

    <modules>
        <module>common-module</module>
        <module>user-service</module>
        <module>order-service</module>
        <module>api-gateway</module>
    </modules>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Common dependency versions for all modules -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>common-module</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Child Module POM (user-service):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project>
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.example</groupId>
        <artifactId>my-company-app</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>user-service</artifactId>  <!-- Parent se groupId aur version inherit hota hai -->

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>common-module</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### Build Commands:

```bash
# Build all modules
mvn clean install

# Build specific module
mvn clean install -pl user-service

# Build with dependencies
mvn clean install -pl user-service -am  # -am = also-make (dependencies bhi build honge)

# Run specific module
cd user-service && mvn spring-boot:run
```

---

## Q7: Profile-specific configurations kaise manage karte hain?

**Answer:**

Different environments (dev, test, prod) ke liye different config files use karte hain.

### File Naming Convention:

```
src/main/resources/
├── application.yml              # Common config (sab environments ke liye)
├── application-dev.yml          # Development specific
├── application-test.yml         # Testing specific
├── application-prod.yml         # Production specific
└── application-staging.yml      # Staging environment
```

### application.yml (Common):

```yaml
app:
  name: My Application
  version: 1.0.0
  feature:
    logging: true

server:
  error:
    include-message: always
```

### application-dev.yml:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true

logging:
  level:
    com.example: DEBUG
    org.springframework.web: DEBUG

server:
  port: 8081
```

### application-prod.yml:

```yaml
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/mydb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

logging:
  level:
    com.example: WARN

server:
  port: 80
```

### Active Profile Set Karna:

```properties
# application.properties
spring.profiles.active=dev
```

```bash
# Command line
java -jar app.jar --spring.profiles.active=prod

# Environment variable
export SPRING_PROFILES_ACTIVE=prod

# JVM argument
java -jar app.jar -Dspring.profiles.active=prod
```

**Priority order (high to low):**
1. Command line arguments (`--spring.profiles.active=prod`)
2. JVM system properties (`-Dspring.profiles.active`)
3. Environment variables (`SPRING_PROFILES_ACTIVE`)
4. application.properties (`spring.profiles.active`)

---

## Q8: Spring Boot DevTools kya hai aur kaise use karte hain?

**Answer:**

**DevTools** development experience improve karta hai — automatic restart, live reload, cache disable.

### Setup:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Important:** `<optional>true</optional>` aur `<scope>runtime</scope>` — production mein include nahi hoga.

### Features:

| Feature | Description |
|---------|-------------|
| **Automatic Restart** | Code change pe application auto-restart hota hai |
| **Live Reload** | Browser auto-refresh hota hai (with livereload extension) |
| **Cache Disable** | Templates (Thymeleaf) aur static resources cache disable hote hain |
| **Remote DevTools** | Remote application pe bhi auto-restart (SSH tunnel) |

### Configuration:

```properties
# DevTools config
spring.devtools.restart.enabled=true
spring.devtools.restart.poll-interval=2s        # File check interval
spring.devtools.restart.quiet-period=400ms      # Wait before restart
spring.devtools.restart.exclude=static/**,public/**

# Live reload
spring.devtools.livereload.enabled=true
spring.devtools.livereload.port=35729
```

### How Restart Works:

```
Code change detected
    ↓
DevTools restarts application (fast restart)
    ↓
Uses two classloaders:
    Base ClassLoader: Unchanged classes (dependencies, config)
    Restart ClassLoader: Application code (faster reload)
```

---

## Q9: Spring Boot Actuator kya hai aur kaise configure karte hain?

**Answer:**

**Actuator** production-ready monitoring aur management endpoints provide karta hai.

### Setup:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Built-in Endpoints:

| Endpoint | URL | Description |
|----------|-----|-------------|
| **health** | `/actuator/health` | Application health status |
| **info** | `/actuator/info` | Application info (version, name) |
| **metrics** | `/actuator/metrics` | JVM, CPU, memory metrics |
| **env** | `/actuator/env` | Environment properties |
| **beans** | `/actuator/beans` | All Spring beans |
| **mappings** | `/actuator/mappings` | All request mappings |
| **loggers** | `/actuator/loggers` | Log levels (changeable at runtime) |

### Configuration:

```properties
# All endpoints enable karo
management.endpoints.web.exposure.include=*

# Specific endpoints
management.endpoints.web.exposure.include=health,info,metrics,env

# Custom base path
management.endpoints.web.base-path=/manage

# Health details show
management.endpoint.health.show-details=always

# Info endpoint properties
info.app.name=My Application
info.app.version=1.0.0
info.app.java.version=${java.version}
```

---

## Q10: Spring Boot mein logging kaise configure karte hain?

**Answer:**

Spring Boot default mein **Logback** use karta hai. Logging levels aur output customize kar sakte hain.

### Basic Logging Config:

```properties
# application.properties

# Root log level
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

# Log pattern
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

### YAML Format:

```yaml
logging:
  level:
    root: INFO
    com.example.myapp: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
  file:
    name: logs/myapp.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

### Log Levels (Low to High):

| Level | Use Case |
|-------|----------|
| **TRACE** | Sabse detailed — har chhoti detail |
| **DEBUG** | Development mein detailed debugging |
| **INFO** | Default — important events (startup, shutdown) |
| **WARN** | Potential issues (deprecated API usage) |
| **ERROR** | Errors (exceptions, failures) |
| **OFF** | Logging disable |

### Custom Logback Config (Advanced):

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File appender -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/myapp.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/myapp.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>10MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Profile-specific logging -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <root level="INFO">
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

---

## Quick Reference Summary

```bash
# Maven Commands
mvn clean compile          # Clean + Compile
mvn test                   # Run tests
mvn package                # Build JAR/WAR
mvn spring-boot:run        # Run application
mvn clean install          # Full build + install

# Running Application
java -jar target/myapp.jar
java -jar myapp.jar --spring.profiles.active=prod
java -jar myapp.jar --debug

# Project Structure
src/main/java/             # Source code
src/main/resources/        # Config files, templates
src/test/java/             # Tests
pom.xml / build.gradle     # Build config

# Key Annotations
@SpringBootApplication     # @Configuration + @EnableAutoConfiguration + @ComponentScan
@SpringBootTest           # Integration testing
@ConfigurationProperties  # Type-safe config binding

# Config Files
application.properties     # Key-value config
application.yml            # Hierarchical config
application-{profile}.yml  # Profile-specific config
logback-spring.xml         # Custom logging config
```

---

## Yaad Rakhne Wali Baatein

1. **@SpringBootApplication** teen annotations ka combo hai — @Configuration, @EnableAutoConfiguration, @ComponentScan
2. **Main class root package** mein rakho — sub-packages automatically scan honge
3. **Jar default hai** — embedded server ke saath, `java -jar` se run hota hai
4. **spring-boot-starter-parent** dependency versions manage karta hai — version likhne ki zarurat nahi
5. **Auto-Configuration** classpath dekh ke beans configure karta hai — conditional annotations use hote hain
6. **application.yml** hierarchical hai — related config grouped rehti hai
7. **Relaxed Binding** — `user-name`, `user_name`, `USERNAME` sab same field pe map hote hain
8. **Multi-document YAML** — `---` se ek file mein multiple profiles define kar sakte ho
9. **spring-boot-maven-plugin** fat JAR banata hai — saari dependencies bundled
10. **DevTools** development ke liye hai — auto-restart, live reload (production mein nahi jayega)
11. **Actuator** production monitoring ke liye hai — health, metrics, info endpoints
12. **Banner customize** kar sakte ho — `banner.txt` ya `banner.png` resources mein daalo
