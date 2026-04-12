# Maven/Gradle Build

## Status: Not Started

---

## Table of Contents

1. [spring-boot-starter-parent](#spring-boot-starter-parent)
2. [Dependency Management](#dependency-management)
3. [spring-boot-maven-plugin](#spring-boot-maven-plugin)
4. [Build Lifecycle](#build-lifecycle)
5. [Common Maven Commands](#common-maven-commands)
6. [Gradle Build](#gradle-build)
7. [Maven vs Gradle Commands](#maven-vs-gradle-commands)
8. [Running Spring Boot Application](#running-spring-boot-application)
9. [Fat JAR Structure](#fat-jar-structure)

---

## spring-boot-starter-parent

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

### Without starter-parent (dependency management only)

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

---

## Dependency Management

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

### Version Override Karna

```xml
<properties>
    <java.version>17</java.version>
    <!-- Specific version override -->
    <spring-security.version>6.2.0</spring-security.version>
    <h2.version>2.2.224</h2.version>
</properties>
```

---

## spring-boot-maven-plugin

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

### Plugin kya karta hai:

1. **Repackage goal** — Normal JAR ko executable "fat JAR" mein convert karta hai
2. **Dependencies bundle** — Saari dependencies JAR ke andar include hoti hain
3. **Main class detect** — `@SpringBootApplication` class automatically detect hoti hai

---

## Fat JAR Structure

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

---

## Build Lifecycle

| Phase | Command | Kya hota hai |
|-------|---------|-------------|
| **Clean** | `mvn clean` | Target folder delete hota hai |
| **Compile** | `mvn compile` | Source code `.class` files mein compile hota hai |
| **Test** | `mvn test` | Unit tests run hote hain |
| **Package** | `mvn package` | JAR/WAR file banti hai |
| **Verify** | `mvn verify` | Integration tests run hote hain |
| **Install** | `mvn install` | Local Maven repository (`~/.m2`) mein install hota hai |
| **Deploy** | `mvn deploy` | Remote repository (Nexus/Artifactory) mein upload hota hai |

---

## Common Maven Commands

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

---

## Gradle Build

### build.gradle

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

### Gradle Commands

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

---

## Maven vs Gradle Commands

| Task | Maven | Gradle |
|------|-------|--------|
| Clean build | `mvn clean package` | `./gradlew clean build` |
| Run app | `mvn spring-boot:run` | `./gradlew bootRun` |
| Skip tests | `mvn package -DskipTests` | `./gradlew bootJar -x test` |
| View deps | `mvn dependency:tree` | `./gradlew dependencies` |
| Install locally | `mvn install` | `./gradlew publishToMavenLocal` |

---

## Running Spring Boot Application

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

# Method 6: With custom port
java -jar target/myapp.jar --server.port=9090
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **spring-boot-starter-parent** | Dependency versions manage karta hai — version likhne ki zarurat nahi |
| **spring-boot-maven-plugin** | Executable fat JAR banata hai — saari dependencies bundled |
| **Build Lifecycle** | Clean → Compile → Test → Package → Install → Deploy |
| **Fat JAR** | Normal JAR + saari dependencies — `java -jar` se direct run hota hai |
| **mvn spring-boot:run** | Bina package kiye application run karo |
| **Gradle** | Faster builds, incremental, better conflict resolution |
| **Maven** | XML-based, beginner-friendly, enterprise standard |
