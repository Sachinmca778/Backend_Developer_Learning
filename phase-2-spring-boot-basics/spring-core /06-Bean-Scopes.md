# Bean Scopes

## Status: Not Started

---

## Table of Contents

1. [What is Bean Scope?](#what-is-bean-scope)
2. [singleton (Default)](#singleton-default)
3. [prototype](#prototype)
4. [request](#request)
5. [session](#session)
6. [application](#application)
7. [websocket](#websocket)

---

## What is Bean Scope?

**Scope ka matlab:** Ek bean kitne instances create honge aur kitni der tak accessible rahega.

| Scope | Instances | Use Case |
|-------|-----------|----------|
| **singleton** (default) | 1 per container | Stateless services (90% cases) |
| **prototype** | New instance each time | Stateful beans |
| **request** | 1 per HTTP request | Web apps - request data |
| **session** | 1 per HTTP session | User session data |
| **application** | 1 per ServletContext | Global web app data |
| **websocket** | 1 per WebSocket session | WebSocket apps |

---

## singleton (Default)

**Matlab:** Ek bean ka sirf **ek hi instance** hoga pura Spring container mein.

```java
@Component
public class UserService {  // Default: singleton
    public void createUser(String name) {
        System.out.println("Creating user: " + name);
    }
}

// Usage
UserService s1 = context.getBean(UserService.class);
UserService s2 = context.getBean(UserService.class);
System.out.println(s1 == s2);  // true (same instance)
```

### Thread Safety Warning

Singleton beans **thread-safe nahi hote automatically**. Agar bean mein mutable state hai to synchronization khud handle karna padta hai.

```java
// ❌ Unsafe - Singleton with mutable state
@Component
public class UnsafeCounter {
    private int count = 0;  // Mutable state - multiple threads access karenge
    
    public void increment() {
        count++;  // Race condition possible
    }
}

// ✅ Safe - Stateless singleton
@Component
public class SafeUserService {
    // No mutable state - thread safe by default
    public User findById(Long id) {
        return userRepository.findById(id);
    }
}
```

**Kab use karein:** Jab bean stateless ho - sirf business logic ho, koi mutable field nahi.

---

## prototype

**Matlab:** Har baar `getBean()` call pe **naya instance** milega.

```java
@Component
@Scope("prototype")
public class ReportGenerator {
    private String title;
    private List<String> data = new ArrayList<>();
    
    public ReportGenerator() {
        System.out.println("ReportGenerator instance created");
    }
    
    public void addData(String item) {
        data.add(item);
    }
}

// Usage
ReportGenerator r1 = context.getBean(ReportGenerator.class);
ReportGenerator r2 = context.getBean(ReportGenerator.class);
System.out.println(r1 == r2);  // false (different instances)

r1.addData("Item 1");
System.out.println(r2.data);   // [] (r2 ka alag data hai)
```

### Important

Spring prototype beans ka **lifecycle manage nahi karta** after creation. `@PreDestroy` call nahi hoga automatically.

```java
@Component
@Scope("prototype")
public class PrototypeBean {
    
    @PreDestroy
    public void cleanup() {
        System.out.println("Cleanup");  // Automatically call NAHI hoga
    }
}

// Manual cleanup zaruri
PrototypeBean bean = context.getBean(PrototypeBean.class);
// Use bean
((ConfigurableApplicationContext) context).getBeanFactory()
    .destroyBean(bean);  // Manually destroy karo
```

**Kab use karein:** Jab bean stateful ho - har user/request ke liye alag data chahiye.

---

## request

**Matlab:** Ek HTTP request ke liye **ek bean instance**. Alag requests ko alag instances milenge.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, 
       proxyMode = ScopedProxyMode.TARGET_CLASS_PROXY)
public class RequestData {
    private String requestId = UUID.randomUUID().toString();
    private String clientIp;
    
    public String getRequestId() { return requestId; }
}

@RestController
public class RequestController {
    
    private final RequestData requestData;  // Proxy inject hoga
    
    public RequestController(RequestData requestData) {
        this.requestData = requestData;
    }
    
    @GetMapping("/request")
    public Map<String, String> handleRequest() {
        return Map.of("requestId", requestData.getRequestId());
    }
}
```

**Note:** `proxyMode = ScopedProxyMode.TARGET_CLASS_PROXY` zaruri hai kyunki singleton bean mein request-scoped bean inject ho rahi hai.

**Kab use karein:** Request-specific data store karna ho (logging context, tracing ID, etc.)

---

## session

**Matlab:** Ek user session ke liye **ek bean instance**. Session khatam hote hi bean destroy ho jayega.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, 
       proxyMode = ScopedProxyMode.TARGET_CLASS_PROXY)
public class UserSession {
    private String userId;
    private String username;
    private List<String> cartItems = new ArrayList<>();
    
    public void login(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }
    
    public void addToCart(String item) {
        cartItems.add(item);
    }
    
    public List<String> getCartItems() {
        return cartItems;
    }
}
```

**Kab use karein:** Shopping cart, user preferences, login session data.

---

## application

**Matlab:** Pura web application (ServletContext) ke liye **ek bean instance**. Singleton se thoda different - multiple apps ek server pe deploy hon toh har app ka alag instance.

```java
@Component
@Scope(value = WebApplicationContext.SCOPE_APPLICATION, 
       proxyMode = ScopedProxyMode.TARGET_CLASS_PROXY)
public class AppConfig {
    private String appName = "My Application";
    private String version = "1.0.0";
}
```

**Kab use karein:** Global web app configuration ya shared data.

---

## websocket

**Matlab:** Ek WebSocket connection ke liye **ek bean instance**.

```java
@Component
@Scope(value = "websocket", 
       proxyMode = ScopedProxyMode.TARGET_CLASS_PROXY)
public class WebSocketSession {
    private String sessionId;
    private WebSocketSession session;
    
    public void connect(WebSocketSession session) {
        this.sessionId = session.getId();
        this.session = session;
    }
}
```

**Kab use karein:** Real-time chat apps, live notifications, collaborative editing.

---

## Scope Comparison

| Feature | singleton | prototype | request | session | application |
|---------|-----------|-----------|---------|---------|-------------|
| **Instances** | 1 | New each time | 1 per request | 1 per session | 1 per ServletContext |
| **Lifecycle** | Full | Creation only | Request lifetime | Session lifetime | App lifetime |
| **Thread Safe?** | Nahi (khud handle karo) | Haan (alag instances) | Haan | Haan | Nahi |
| **Web App Required?** | Nahi | Nahi | Haan | Haan | Haan |

---

## Quick Reference

```java
@Scope("singleton")     // Default - 1 instance per container
@Scope("prototype")     // New instance each getBean()
@Scope("request")       // 1 per HTTP request (web only)
@Scope("session")       // 1 per HTTP session (web only)
@Scope("application")   // 1 per ServletContext (web only)
@Scope("websocket")     // 1 per WebSocket session
```

---

## Summary

| Scope | Use When |
|-------|----------|
| **singleton** | Stateless services (default - 90% cases) |
| **prototype** | Stateful beans, new instance each time needed |
| **request/session** | Web apps - per request/session data |
| **application** | Global web app data |
| **websocket** | Real-time WebSocket connections |
