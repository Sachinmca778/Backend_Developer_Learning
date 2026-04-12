# Spring Boot Project Structure

## Status: Not Started

---

## Table of Contents

1. [Standard Project Structure](#standard-project-structure)
2. [Key Directories Explained](#key-directories-explained)
3. [Main Class (@SpringBootApplication)](#main-class-springbootapplication)
4. [Package Organization](#package-organization)
5. [Banner Customization](#banner-customization)

---

## Standard Project Structure

Spring Boot project ek predefined folder structure follow karta hai jo Maven/Gradle conventions pe based hai.

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

---

## Key Directories Explained

| Directory | Purpose |
|-----------|---------|
| **src/main/java** | Application source code — controllers, services, repositories |
| **src/main/resources** | Configuration files, templates, static assets |
| **src/test/java** | Unit tests, integration tests |
| **src/test/resources** | Test-specific configuration |
| **pom.xml / build.gradle** | Build configuration + dependencies |
| **target/** (Maven) / **build/** (Gradle) | Compiled output, packaged JAR/WAR |

---

## Main Class (@SpringBootApplication)

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

---

## Package Organization

### Layered Architecture (Recommended)

```
com.example.myapp/
├── MyAppApplication.java
├── controller/
│   ├── UserController.java
│   └── OrderController.java
├── service/
│   ├── UserService.java
│   └── OrderService.java
├── repository/
│   ├── UserRepository.java
│   └── OrderRepository.java
├── model/
│   ├── User.java
│   └── Order.java
├── dto/
│   ├── UserDTO.java
│   └── OrderDTO.java
├── config/
│   ├── SecurityConfig.java
│   └── AppConfig.java
└── exception/
    ├── GlobalExceptionHandler.java
    └── ResourceNotFoundException.java
```

### Feature-Based Organization (Large Projects)

```
com.example.myapp/
├── MyAppApplication.java
├── user/
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   └── User.java
├── order/
│   ├── OrderController.java
│   ├── OrderService.java
│   ├── OrderRepository.java
│   └── Order.java
└── common/
    ├── config/
    └── exception/
```

---

## Banner Customization

Spring Boot start hone pe jo banner print hota hai usko customize kar sakte ho.

### Method 1: Text File

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

### Method 2: Disable Banner

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

### Method 3: Image Banner

```
# src/main/resources/banner.png (ya banner.jpg, banner.gif)
# Properties:
spring.banner.image.location=classpath:banner.png
spring.banner.image.width=50
spring.banner.image.height=30
```

### Banner Variables

| Variable | Description |
|----------|-------------|
| `${application.title}` | Application title |
| `${application.version}` | Version from pom.xml |
| `${spring-boot.version}` | Spring Boot version |
| `${spring-boot.formatted-version}` | Formatted version |
| `${AnsiColor.GREEN}` | ANSI color codes |

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Root Package** | Main class yahan rakho — sub-packages auto-scan hote hain |
| **src/main/java** | Source code — controller, service, repository layers |
| **src/main/resources** | application.properties, templates, static files |
| **src/test/java** | Tests — @WebMvcTest, @DataJpaTest, @SpringBootTest |
| **@SpringBootApplication** | @Configuration + @EnableAutoConfiguration + @ComponentScan |
| **Banner** | banner.txt ya banner.png se customize karo |
