# Spring Initializr

## Status: Not Started

---

## Table of Contents

1. [Spring Initializr Overview](#spring-initializr-overview)
2. [How to Use Spring Initializr](#how-to-use-spring-initializr)
3. [Maven vs Gradle](#maven-vs-gradle)
4. [Group/Artifact/Name Conventions](#groupartifactname-conventions)
5. [Packaging: Jar vs War](#packaging-jar-vs-war)
6. [Common Dependencies](#common-dependencies)
7. [Importing into IntelliJ IDEA](#importing-into-intellij-idea)

---

## Spring Initializr Overview

**Spring Initializr** (start.spring.io) ek web tool hai jo Spring Boot project scaffold karta hai — dependencies, build file, main class automatically generate ho jaati hai.

### Website

🌐 **https://start.spring.io/**

---

## How to Use Spring Initializr

### Step-by-Step Setup

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

**Step 3:** Dependencies add karo (search box mein type karke)

**Step 4:** "Generate" button click → ZIP download → Extract → IDE mein import karo

---

## Maven vs Gradle

| Feature | Maven | Gradle |
|---------|-------|--------|
| **Config File** | `pom.xml` (XML) | `build.gradle` (Groovy/Kotlin DSL) |
| **Learning Curve** | Easy | Medium |
| **Build Speed** | Slower | Faster (incremental builds) |
| **Dependency Management** | Good | Better (conflict resolution) |
| **Standard in Enterprise** | ✅ Yes | Growing |
| **Convention over Configuration** | Less | More |

### Maven (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
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

### Gradle (build.gradle)

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

---

## Group/Artifact/Name Conventions

| Field | Example | Description |
|-------|---------|-------------|
| **Group** | `com.example`, `com.companyname` | Reverse domain name — package structure |
| **Artifact** | `my-app`, `user-service` | Project name (lowercase, hyphenated) |
| **Name** | `My Application` | Readable name |
| **Version** | `0.0.1-SNAPSHOT` | SNAPSHOT = development version |

---

## Packaging: Jar vs War

| Feature | Jar (Default) | War |
|---------|---------------|-----|
| **Server** | Embedded (Tomcat) | External (deploy to Tomcat) |
| **Run** | `java -jar app.jar` | Deploy to application server |
| **Best for** | Microservices, standalone apps | Traditional enterprise deployments |
| **Spring Boot setup** | Zero config | Need `SpringBootServletInitializer` |

### Jar Configuration (Default)

```xml
<!-- pom.xml - Default hai, kuch extra nahi chahiye -->
<packaging>jar</packaging>
```

### War Configuration

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

## Common Dependencies

| Dependency | Starter | Use Case |
|------------|---------|----------|
| **Spring Web** | `spring-boot-starter-web` | REST APIs, web apps |
| **Spring Data JPA** | `spring-boot-starter-data-jpa` | Database with Hibernate |
| **Spring Security** | `spring-boot-starter-security` | Authentication & Authorization |
| **MySQL Driver** | `mysql-connector-j` | MySQL database |
| **H2 Database** | `h2` | In-memory DB (testing) |
| **Validation** | `spring-boot-starter-validation` | Bean validation (@Valid) |
| **Lombok** | `lombok` | Boilerplate reduction |
| **Spring Boot DevTools** | `spring-boot-devtools` | Auto-reload during dev |
| **Spring Boot Actuator** | `spring-boot-starter-actuator` | Production monitoring |
| **Thymeleaf** | `spring-boot-starter-thymeleaf` | Server-side templates |

---

## Importing into IntelliJ IDEA

### Steps

1. **File → Open** → Project folder select karo
2. **pom.xml** ya **build.gradle** pe right-click → "Import as Maven/Gradle Project"
3. IntelliJ automatically dependencies download karega
4. Main class pe right-click → "Run" → Application start ho jayegi

### Quick Run

```bash
# Maven se run karo
mvn spring-boot:run

# Ya direct main class run karo
# IntelliJ mein main class pe right-click → Run 'MyAppApplication'
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Spring Initializr** | start.spring.io — project scaffold karne ka tool |
| **Maven** | XML-based, beginner-friendly, enterprise standard |
| **Gradle** | Faster builds, incremental, better conflict resolution |
| **Jar** | Default packaging, embedded server, `java -jar` se run |
| **War** | External Tomcat mein deploy, `SpringBootServletInitializer` chahiye |
| **Group** | Reverse domain name (`com.example`) |
| **Artifact** | Project name (`my-app`) |
