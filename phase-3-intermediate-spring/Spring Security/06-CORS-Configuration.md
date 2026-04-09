# CORS Configuration

## Status: Not Started

---

## Table of Contents

1. [CORS Overview](#cors-overview)
2. [How CORS Works](#how-cors-works)
3. [CorsConfigurationSource](#corsconfigurationsource)
4. [@CrossOrigin Annotation](#crossorigin-annotation)
5. [Global CORS via HttpSecurity](#global-cors-via-httpsecurity)
6. [Preflight Request Handling](#preflight-request-handling)
7. [Credentialed Requests](#credentialed-requests)
8. [Common CORS Mistakes](#common-cors-mistakes)

---

## CORS Overview

**CORS (Cross-Origin Resource Sharing) ka matlab:** Browser ki same-origin policy ko relax karne ka mechanism. Ek domain ka frontend doosre domain ke backend se baat kar sake.

### Same-Origin Policy

```
Same Origin = Same Protocol + Same Host + Same Port

Frontend: http://localhost:3000
Backend:  http://localhost:8080

❌ Different ports → Different origins → Browser blocks request (without CORS)
✅ CORS header → Browser allows request
```

### When CORS Applies

| Scenario | CORS Needed? |
|----------|-------------|
| Same domain, same port | ❌ No |
| Different domain | ✅ Yes |
| Same domain, different port | ✅ Yes |
| HTTP → HTTPS | ✅ Yes |
| Backend → Backend (server-side) | ❌ No (CORS is browser-only) |

**Important:** CORS sirf **browser** ka restriction hai. Server-to-server calls (Postman, curl, microservices) mein CORS apply nahi hota.

---

## How CORS Works

### Simple Request

```
Browser → Server
  Origin: http://localhost:3000

Server → Browser
  Access-Control-Allow-Origin: http://localhost:3000
  [Response body]
```

### Preflight Request (Complex Requests)

```
Browser → Server (OPTIONS request)
  Origin: http://localhost:3000
  Access-Control-Request-Method: DELETE
  Access-Control-Request-Headers: Authorization

Server → Browser (OPTIONS response)
  Access-Control-Allow-Origin: http://localhost:3000
  Access-Control-Allow-Methods: DELETE
  Access-Control-Allow-Headers: Authorization
  Access-Control-Max-Age: 3600

Browser → Server (Actual request)
  Origin: http://localhost:3000
  Authorization: Bearer eyJ...

Server → Browser
  Access-Control-Allow-Origin: http://localhost:3000
  [Response body]
```

### When Preflight Happens

| Condition | Preflight? |
|-----------|-----------|
| GET/POST/HEAD with standard headers | ❌ No |
| PUT, DELETE, PATCH | ✅ Yes |
| Custom headers (Authorization, X-Custom) | ✅ Yes |
| Content-Type: application/json | ✅ Yes |
| Credentials included | ✅ Yes |

---

## CorsConfigurationSource

**Matlab:** Programmatic way to configure CORS. Most flexible approach.

### Bean Definition

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    // Allowed origins
    configuration.setAllowedOrigins(List.of(
        "http://localhost:3000",   // React dev server
        "http://localhost:5173",   // Vite dev server
        "https://myapp.com"        // Production
    ));

    // Allowed HTTP methods
    configuration.setAllowedMethods(List.of(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
    ));

    // Allowed headers
    configuration.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type",
        "X-Requested-With",
        "Accept",
        "Origin"
    ));

    // Exposed headers (client can access these)
    configuration.setExposedHeaders(List.of(
        "X-Total-Count",
        "X-Page-Number"
    ));

    // Allow credentials (cookies, Authorization header)
    configuration.setAllowCredentials(true);

    // Preflight cache duration (seconds)
    configuration.setMaxAge(3600L);

    // Apply to all paths
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);

    return source;
}
```

### Register in Security Config

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))  // CORS config
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );

    return http.build();
}
```

### Pattern-Based CORS

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration apiConfig = new CorsConfiguration();
    apiConfig.setAllowedOrigins(List.of("https://admin.myapp.com"));
    apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    apiConfig.setAllowCredentials(true);

    CorsConfiguration publicConfig = new CorsConfiguration();
    publicConfig.setAllowedOrigins(List.of("*"));  // Public API
    publicConfig.setAllowedMethods(List.of("GET"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/admin/**", apiConfig);   // Strict
    source.registerCorsConfiguration("/api/public/**", publicConfig);  // Open

    return source;
}
```

---

## @CrossOrigin Annotation

**Matlab:** Class ya method level pe CORS configure karne ka quick way.

### Controller Level

```java
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:3000")  // Single origin
public class UserController {

    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

### Method Level

```java
@RestController
@RequestMapping("/api/data")
public class DataController {

    // Specific origin for this method
    @CrossOrigin(origins = {"http://localhost:3000", "https://myapp.com"})
    @GetMapping("/sensitive")
    public SensitiveData getSensitiveData() {
        return dataService.getSecretData();
    }

    // Allow all origins (public endpoint)
    @CrossOrigin(origins = "*")
    @GetMapping("/public")
    public PublicData getPublicData() {
        return dataService.getPublicData();
    }

    // Full configuration
    @CrossOrigin(
        origins = "http://localhost:3000",
        methods = {RequestMethod.GET, RequestMethod.POST},
        allowedHeaders = {"Authorization", "Content-Type"},
        exposedHeaders = {"X-Total-Count"},
        allowCredentials = "true",
        maxAge = 3600
    )
    @PostMapping("/create")
    public User createUser(@RequestBody UserDto dto) {
        return userService.createUser(dto);
    }
}
```

### Class-Level with Defaults

```java
// Sab methods pe same CORS config
@RestController
@RequestMapping("/api")
@CrossOrigin(
    origins = {"http://localhost:3000", "https://myapp.com"},
    allowedHeaders = "*",
    allowCredentials = "true"
)
public class ApiController {
    // All methods inherit this CORS config
}
```

**When to Use:** Chhoti apps ya specific endpoints pe quick CORS fix. Large apps mein global config better hai.

---

## Global CORS via HttpSecurity

**Matlab:** WebMvcConfigurer interface use karke ya directly HttpSecurity mein CORS config karna.

### Option 1: HttpSecurity (Recommended)

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

    return http.build();
}

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();

    // Production-ready setup
    config.setAllowedOriginPatterns(List.of(
        "http://localhost:*",        // Dev: all ports
        "https://*.myapp.com",       // Prod: subdomains
        "https://myapp.com"          // Prod: main domain
    ));

    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));  // All headers allowed
    config.setExposedHeaders(List.of("X-Total-Count", "X-Page-Number"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
```

### Option 2: WebMvcConfigurer

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:3000", "https://myapp.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("X-Total-Count")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

### allowedOrigins vs allowedOriginPatterns

```java
// ❌ Exact match only
config.setAllowedOrigins(List.of("http://localhost:3000"));

// ✅ Pattern matching (Spring 5.3+)
config.setAllowedOriginPatterns(List.of(
    "http://localhost:*",      // Any localhost port
    "https://*.example.com",   // Any subdomain
    "https://example.com"      // Exact
));
```

**Note:** `allowedOrigins("*")` aur `allowCredentials(true)` ek saath use nahi kar sakte. Pattern use karo.

---

## Preflight Request Handling

**Matlab:** Browser ka OPTIONS request jo actual request se pehle aata hai toh check kare ki server CORS allow karta hai ya nahi.

### How Spring Handles Preflight

```
Browser: OPTIONS /api/users
  Origin: http://localhost:3000
  Access-Control-Request-Method: DELETE
  Access-Control-Request-Headers: Authorization

Spring CORS Filter:
  1. Check if origin is allowed → yes
  2. Check if method is allowed → yes (DELETE)
  3. Check if headers are allowed → yes (Authorization)
  4. Return 200 with CORS headers

Spring Response: 200 OK
  Access-Control-Allow-Origin: http://localhost:3000
  Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
  Access-Control-Allow-Headers: Authorization, Content-Type
  Access-Control-Max-Age: 3600
```

### Common Preflight Issues

```java
// ❌ Problem: Methods missing
config.setAllowedMethods(List.of("GET", "POST"));
// DELETE/PUT requests fail — preflight rejected

// ✅ Fix: Include all methods you use
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

// ❌ Problem: Headers missing
config.setAllowedHeaders(List.of("Content-Type"));
// Authorization header requests fail

// ✅ Fix: Include all custom headers
config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
```

### Preflight Cache (Max-Age)

```java
// Browser preflight result cache karta hai
config.setMaxAge(3600L);  // 1 hour — browser 1 hour tak preflight skip karega

// Development (short cache)
config.setMaxAge(0L);  // Always preflight — debugging mein help karta hai

// Production (long cache)
config.setMaxAge(86400L);  // 24 hours
```

---

## Credentialed Requests

**Matlab:** Requests jo cookies, Authorization headers, ya client certificates bhejte hain.

### Server Configuration

```java
CorsConfiguration config = new CorsConfiguration();

// Credentials allow karo
config.setAllowCredentials(true);

// ⚠️ Cannot use wildcard origin with credentials
// ❌ This will FAIL:
config.setAllowedOrigins(List.of("*"));
config.setAllowCredentials(true);  // Error!

// ✅ Use specific origins
config.setAllowedOrigins(List.of("http://localhost:3000"));
config.setAllowCredentials(true);

// ✅ Or use patterns
config.setAllowedOriginPatterns(List.of("http://localhost:*"));
config.setAllowCredentials(true);
```

### Frontend Configuration

```javascript
// React (fetch)
fetch('http://localhost:8080/api/users', {
    method: 'GET',
    credentials: 'include',  // Cookies send karo
    headers: {
        'Authorization': 'Bearer eyJ...',
        'Content-Type': 'application/json'
    }
});

// Axios
axios.get('http://localhost:8080/api/users', {
    withCredentials: true  // Cookies send karo
});

// Angular
this.http.get('http://localhost:8080/api/users', {
    withCredentials: true
});
```

### Cookie-Based Auth with CORS

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOriginPatterns(List.of("http://localhost:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // Cookies ke liye zaruri
        config.setExposedHeaders(List.of("X-XSRF-TOKEN"));  // CSRF token expose karo

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

## Common CORS Mistakes

### Mistake 1: Using Wildcard with Credentials

```java
// ❌ Error!
config.setAllowedOrigins(List.of("*"));
config.setAllowCredentials(true);

// ✅ Fix
config.setAllowedOriginPatterns(List.of("*"));  // Pattern use karo
config.setAllowCredentials(true);
```

### Mistake 2: Forgetting OPTIONS Method

```java
// ❌ Preflight fail
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));

// ✅ Include OPTIONS
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
```

### Mistake 3: CORS on Server-to-Server Calls

```
Microservice A → Microservice B

❌ CORS configure karne ki zarurat nahi (server-to-server)
✅ CORS sirf browser requests ke liye hai
```

### Mistake 4: Overly Permissive CORS

```java
// ❌ Security risk — koi bhi domain access kar sakta hai
config.setAllowedOrigins(List.of("*"));
config.setAllowCredentials(true);

// ✅ Restrict to known origins
config.setAllowedOriginPatterns(List.of(
    "https://myapp.com",
    "https://admin.myapp.com",
    "http://localhost:*"  // Dev only — remove in production
));
```

### Mistake 5: Missing Exposed Headers

```java
// ❌ Client can't read custom response headers
// Response has X-Total-Count but client can't access it

// ✅ Expose custom headers
config.setExposedHeaders(List.of("X-Total-Count", "X-Page-Number", "Link"));
```

---

## Quick Reference

```java
// Global CORS (recommended approach)
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("http://localhost:*", "https://*.myapp.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setExposedHeaders(List.of("X-Total-Count"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}

// In SecurityFilterChain
http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

// Quick annotation
@CrossOrigin(origins = "http://localhost:3000")
```

---

## Summary

| Feature | Use When |
|---------|----------|
| **CorsConfigurationSource** | Global, programmatic CORS config (recommended) |
| **@CrossOrigin** | Quick per-controller/method config |
| **allowedOriginPatterns** | Dynamic origins (wildcards with credentials) |
| **allowCredentials(true)** | Cookies/Authorization headers ke saath |
| **setMaxAge()** | Preflight cache duration |
| **setExposedHeaders()** | Custom response headers client ko dikhane |

**Security Warning:** Overly permissive CORS (`*` + credentials) is a vulnerability. Always restrict origins to known domains in production.
