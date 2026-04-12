# Actuator Endpoints

## Status: Not Started

---

## Table of Contents

1. [Spring Boot Actuator Overview](#spring-boot-actuator-overview)
2. [Setup & Configuration](#setup--configuration)
3. [/actuator/health](#actuatorhealth)
4. [/actuator/info](#actuatorinfo)
5. [/actuator/metrics](#actuatormetrics)
6. [/actuator/env](#actuatorenv)
7. [/actuator/beans](#actuatorbeans)
8. [/actuator/mappings](#actuatormappings)
9. [/actuator/loggers](#actuatorloggers)
10. [/actuator/threaddump](#actuatorthreaddump)
11. [/actuator/heapdump](#actuatorheapdump)
12. [/actuator/httptrace](#actuatorhttptraces)
13. [Security for Actuator Endpoints](#security-for-actuator-endpoints)

---

## Spring Boot Actuator Overview

**Matlab:** Production-ready monitoring aur management features — application ka health, metrics, environment, logs sab dekh sakte ho.

### What Actuator Provides

| Feature | Description |
|---------|-------------|
| **Health checks** | Database, disk, external services status |
| **Metrics** | CPU, memory, HTTP requests, custom metrics |
| **Environment** | All properties, config values |
| **Beans** | All Spring beans in context |
| **Mappings** | All request mappings (controllers) |
| **Logs** | View and change log levels at runtime |
| **Thread dump** | Running threads dekho |
| **Heap dump** | Memory snapshot lo |
| **HTTP traces** | Recent HTTP requests dekho |

---

## Setup & Configuration

### Dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Basic Configuration

```properties
# application.properties

# All endpoints enable karo (default: sirf health aur info)
management.endpoints.web.exposure.include=*

# Ya specific endpoints
management.endpoints.web.exposure.include=health,info,metrics,env,loggers

# Specific endpoints exclude karo
management.endpoints.web.exposure.exclude=env,beans

# Custom base path
management.endpoints.web.base-path=/monitor

# Enable JMX exposure (for JConsole/VisualVM)
management.endpoints.jmx.exposure.include=*

# Endpoint enable/disable
management.endpoint.health.enabled=true
management.endpoint.info.enabled=true
management.endpoint.env.enabled=false  # Production mein band rakho
```

### Accessing Endpoints

```
Base URL: http://localhost:8080/actuator

Endpoints:
  /actuator/health
  /actuator/info
  /actuator/metrics
  /actuator/metrics/{metricName}
  /actuator/env
  /actuator/beans
  /actuator/mappings
  /actuator/loggers
  /actuator/threaddump
  /actuator/heapdump
  /actuator/httptraces
```

---

## /actuator/health

**Matlab:** Application ki health status — database, disk space, external services sab ka status.

### Basic Response

```
GET /actuator/health

{
  "status": "UP"
}
```

### Detailed Response

```properties
# Health details show karo
management.endpoint.health.show-details=always
# Options: never, when-authorized, always
```

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "MySQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10000000,
        "path": "/"
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Status Values

| Status | Meaning |
|--------|---------|
| **UP** | Sab theek hai |
| **DOWN** | Koi component down hai |
| **OUT_OF_SERVICE** | Intentionally disabled |
| **UNKNOWN** | Status pata nahi chala |

### HTTP Status Mapping

```
UP → 200 OK
DOWN → 503 Service Unavailable
OUT_OF_SERVICE → 503 Service Unavailable
```

---

## /actuator/info

**Matlab:** Application ki custom information — version, name, description, etc.

### Setup

```properties
# application.properties
info.app.name=My Application
info.app.version=1.0.0
info.app.description=This is a sample Spring Boot app
info.app.java.version=${java.version}
info.app.encoding=${project.build.sourceEncoding:UTF-8}
```

### Response

```
GET /actuator/info

{
  "app": {
    "name": "My Application",
    "version": "1.0.0",
    "description": "This is a sample Spring Boot app",
    "java": {
      "version": "17"
    },
    "encoding": "UTF-8"
  }
}
```

### Git Info

```xml
<plugin>
    <groupId>io.github.git-commit-id</groupId>
    <artifactId>git-commit-id-maven-plugin</artifactId>
</plugin>
```

```
GET /actuator/info

{
  "git": {
    "branch": "main",
    "commit": {
      "id": "abc1234",
      "time": "2024-01-15T10:30:00Z"
    }
  }
}
```

### Build Info

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>build-info</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

```
GET /actuator/info

{
  "build": {
    "artifact": "my-app",
    "name": "My Application",
    "version": "1.0.0",
    "time": "2024-01-15T10:30:00Z"
  }
}
```

---

## /actuator/metrics

**Matlab:** Application metrics — CPU, memory, HTTP requests, JVM, custom metrics.

### List Available Metrics

```
GET /actuator/metrics

{
  "names": [
    "jvm.memory.max",
    "jvm.memory.used",
    "jvm.gc.pause",
    "process.cpu.usage",
    "system.cpu.usage",
    "http.server.requests",
    "tomcat.sessions.active.current",
    "logback.events"
  ]
}
```

### Get Specific Metric

```
GET /actuator/metrics/jvm.memory.used

{
  "name": "jvm.memory.used",
  "description": "The amount of used memory",
  "baseUnit": "bytes",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 256000000
    }
  ],
  "availableTags": [
    {
      "tag": "id",
      "values": ["G1 Eden Space", "G1 Survivor Space", "G1 Old Gen"]
    }
  ]
}
```

### Get Metric with Tags

```
GET /actuator/metrics/http.server.requests?tag=status:200&tag=method:GET

{
  "name": "http.server.requests",
  "measurements": [
    { "statistic": "COUNT", "value": 1500 },
    { "statistic": "TOTAL_TIME", "value": 45.5 },
    { "statistic": "MAX", "value": 0.8 }
  ]
}
```

### Common Metrics

| Metric | Description |
|--------|-------------|
| **jvm.memory.max** | Maximum JVM memory |
| **jvm.memory.used** | Used JVM memory |
| **jvm.gc.pause** | GC pause times |
| **process.cpu.usage** | Process CPU usage |
| **system.cpu.usage** | System CPU usage |
| **http.server.requests** | HTTP request counts and times |
| **tomcat.sessions.active.current** | Active Tomcat sessions |
| **logback.events** | Log events count |
| **disk.free** | Free disk space |

---

## /actuator/env

**Matlab:** All environment properties — system properties, environment variables, application properties.

### Response

```
GET /actuator/env

{
  "activeProfiles": ["dev"],
  "propertySources": [
    {
      "name": "systemProperties",
      "properties": {
        "java.version": {
          "value": "17"
        },
        "user.home": {
          "value": "/home/user"
        }
      }
    },
    {
      "name": "systemEnvironment",
      "properties": {
        "PATH": {
          "value": "/usr/bin:/bin"
        }
      }
    },
    {
      "name": "applicationConfig: [classpath:/application.properties]",
      "properties": {
        "server.port": {
          "value": "8081"
        },
        "spring.datasource.url": {
          "value": "jdbc:mysql://localhost:3306/mydb"
        }
      }
    }
  ]
}
```

### Get Specific Property

```
GET /actuator/env/server.port

{
  "property": {
    "value": "8081"
  },
  "activeProfiles": ["dev"]
}
```

**⚠️ Security Warning:** Sensitive properties (passwords, secrets) masked nahi hote — production mein disable rakho ya secure karo.

---

## /actuator/beans

**Matlab:** All Spring beans in the application context — type, scope, dependencies.

### Response

```
GET /actuator/beans

{
  "contexts": {
    "application": {
      "beans": {
        "userService": {
          "aliases": [],
          "scope": "singleton",
          "type": "com.example.service.UserService",
          "resource": "file [/path/to/UserService.class]",
          "dependencies": ["userRepository"]
        },
        "userRepository": {
          "aliases": [],
          "scope": "singleton",
          "type": "com.example.repository.UserRepository",
          "resource": "file [/path/to/UserRepository.class]"
        },
        "dataSource": {
          "aliases": [],
          "scope": "singleton",
          "type": "com.zaxxer.hikari.HikariDataSource",
          "resource": null,
          "dependencies": []
        }
      }
    }
  }
}
```

---

## /actuator/mappings

**Matlab:** All request mappings — controllers, endpoints, HTTP methods.

### Response

```
GET /actuator/mappings

{
  "contexts": {
    "application": {
      "mappings": {
        "dispatcherServlet": [
          {
            "handler": "UserController#getUser(Long)",
            "predicate": "{GET /api/users/{id}}",
            "details": {
              "handlerMethod": {
                "className": "com.example.controller.UserController",
                "name": "getUser",
                "descriptor": "(Ljava/lang/Long;)Lcom/example/model/User;"
              },
              "requestMappingConditions": {
                "methods": ["GET"],
                "patterns": ["/api/users/{id}"]
              }
            }
          },
          {
            "handler": "UserController#createUser(User)",
            "predicate": "{POST /api/users}",
            "details": {
              "handlerMethod": {
                "className": "com.example.controller.UserController",
                "name": "createUser"
              },
              "requestMappingConditions": {
                "methods": ["POST"],
                "patterns": ["/api/users"]
              }
            }
          }
        ]
      }
    }
  }
}
```

---

## /actuator/loggers

**Matlab:** View and change log levels at runtime — bina restart kiye.

### List All Loggers

```
GET /actuator/loggers

{
  "loggers": {
    "ROOT": {
      "configuredLevel": "INFO",
      "effectiveLevel": "INFO"
    },
    "com.example": {
      "configuredLevel": "DEBUG",
      "effectiveLevel": "DEBUG"
    },
    "com.example.service.UserService": {
      "configuredLevel": null,
      "effectiveLevel": "DEBUG"
    },
    "org.springframework": {
      "configuredLevel": null,
      "effectiveLevel": "INFO"
    }
  }
}
```

### Get Specific Logger

```
GET /actuator/loggers/com.example.service

{
  "configuredLevel": "DEBUG",
  "effectiveLevel": "DEBUG"
}
```

### Change Log Level (POST)

```
POST /actuator/loggers/com.example.service
Content-Type: application/json

{
  "configuredLevel": "TRACE"
}

Response: 204 No Content
```

**Result:** Ab `com.example.service` package ka log level TRACE ho gaya — restart nahi chahiye!

---

## /actuator/threaddump

**Matlab:** Current running threads ka snapshot — deadlocks, blocked threads dekhne ke liye.

### Response

```
GET /actuator/threaddump

{
  "threads": [
    {
      "threadName": "http-nio-8080-exec-1",
      "threadId": 45,
      "blockedTime": -1,
      "blockedCount": 0,
      "waitedTime": -1,
      "waitedCount": 25,
      "lockName": "java.util.concurrent.locks.ReentrantLock$NonfairSync@abc123",
      "lockOwnerId": -1,
      "lockOwnerName": null,
      "inNative": false,
      "suspended": false,
      "threadState": "RUNNABLE",
      "stackTrace": [
        {
          "classLoaderName": null,
          "moduleName": null,
          "moduleVersion": null,
          "methodName": "run",
          "fileName": "ThreadPoolExecutor.java",
          "lineNumber": 1136,
          "className": "java.util.concurrent.ThreadPoolExecutor$Worker"
        }
      ],
      "lockedMonitors": [],
      "lockedSynchronizers": []
    }
  ]
}
```

### Download as Text

```
GET /actuator/threaddump
Accept: text/plain

"main" #1 prio=5 os_prio=31 cpu=123.45ms elapsed=456.78s
  java.lang.Thread.State: RUNNABLE
    at java.base@17.0.1/java.lang.Thread.sleep(Native Method)
    at app//com.example.MyClass.myMethod(MyClass.java:42)
```

---

## /actuator/heapdump

**Matlab:** JVM heap dump — memory analysis ke liye (VisualVM, Eclipse MAT mein open karo).

### Download Heap Dump

```
GET /actuator/heapdump

→ Downloads heap dump file (large binary file)
```

### Analysis Tools

| Tool | Description |
|------|-------------|
| **VisualVM** | Free — heap dump analyze karne ka tool |
| **Eclipse MAT** | Memory Analyzer Tool — leak detection |
| **JProfiler** | Commercial — profiling + memory analysis |

### ⚠️ Warning

- Heap dump bahut bada hota hai (GBs mein)
- Production mein carefully use karo — performance impact
- Sensitive data contain kar sakta hai — secure karo

---

## /actuator/httptrace

**Matlab:** Recent HTTP requests aur responses — debugging ke liye useful.

### Setup

```java
@Configuration
public class HttpTraceConfig {

    @Bean
    public InMemoryHttpTraceRepository httpTraceRepository() {
        InMemoryHttpTraceRepository repository = new InMemoryHttpTraceRepository();
        repository.setCapacity(100);  // Last 100 requests store karo
        return repository;
    }
}
```

**Note:** Spring Boot 2.x mein `HttpTrace` tha, Spring Boot 3.x mein `HttpExchange` use hota hai.

### Spring Boot 3.x (HttpExchange)

```java
@Configuration
public class HttpExchangeConfig {

    @Bean
    public InMemoryHttpExchangeTraceRepository httpExchangeRepository() {
        InMemoryHttpExchangeTraceRepository repository = new InMemoryHttpExchangeTraceRepository();
        repository.setCapacity(100);
        return repository;
    }
}
```

### Response

```
GET /actuator/httptraces

{
  "traces": [
    {
      "timestamp": "2024-01-15T10:30:00.000+00:00",
      "principal": null,
      "session": null,
      "request": {
        "method": "GET",
        "uri": "http://localhost:8080/api/users",
        "headers": {
          "Accept": ["application/json"],
          "User-Agent": ["Mozilla/5.0"]
        },
        "remoteAddress": null
      },
      "response": {
        "status": 200,
        "headers": {
          "Content-Type": ["application/json"]
        }
      },
      "timeTaken": 45
    }
  ]
}
```

**⚠️ Production Warning:** HTTP traces mein sensitive data (tokens, passwords) aa sakte hain — production mein enable mat karo.

---

## Security for Actuator Endpoints

### Disable Sensitive Endpoints

```properties
# Production mein sirf health aur info
management.endpoints.web.exposure.include=health,info

# Environment disable karo
management.endpoint.env.enabled=false

# Beans disable karo
management.endpoint.beans.enabled=false
```

### Spring Security Integration

```java
@Configuration
@EnableWebSecurity
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Actuator endpoints — sirf ADMIN access
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Health endpoint — sabko access
                .requestMatchers("/actuator/health").permitAll()
                // Baaki app
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### Custom Management Port

```properties
# Actuator alag port pe run karo
management.server.port=9090
management.server.address=127.0.0.1  # Sirf localhost

# Ab actuator endpoints: http://localhost:9090/actuator/health
# Main app: http://localhost:8080/api/users
```

---

## Summary

| Endpoint | Description | Production Safe? |
|----------|-------------|-----------------|
| **/actuator/health** | Application health status | ✅ Yes |
| **/actuator/info** | Custom app info | ✅ Yes |
| **/actuator/metrics** | Performance metrics | ✅ Yes |
| **/actuator/env** | All properties | ❌ Sensitive data |
| **/actuator/beans** | All Spring beans | ⚠️ Internal info |
| **/actuator/mappings** | All request mappings | ⚠️ Internal info |
| **/actuator/loggers** | View/change log levels | ⚠️ Can modify logs |
| **/actuator/threaddump** | Thread snapshot | ✅ Debugging ke liye |
| **/actuator/heapdump** | Memory snapshot | ⚠️ Large file, sensitive |
| **/actuator/httptraces** | Recent HTTP requests | ❌ Sensitive data |
