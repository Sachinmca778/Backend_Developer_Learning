# CSRF Protection

## Status: Not Started

---

## Table of Contents

1. [CSRF Overview](#csrf-overview)
2. [CSRF Attack Mechanics](#csrf-attack-mechanics)
3. [CsrfTokenRepository](#csrftokenrepository)
4. [CookieCsrfTokenRepository (SPA-Friendly)](#cookiecsrftokenrepository-spa-friendly)
5. [Disabling CSRF for Stateless REST APIs](#disabling-csrf-for-stateless-rest-apis)
6. [CSRF + JWT](#csrf--jwt)
7. [SameSite Cookie Attribute](#samesite-cookie-attribute)
8. [CSRF Decision Guide](#csrf-decision-guide)

---

## CSRF Overview

**CSRF (Cross-Site Request Forgery) ka matlab:** Ek attack jahan attacker user ko trick karta hai ek malicious request bhejne ke liye — aur user ki authenticated state ka fayda uthata hai.

### The Problem

```
User logged in hai banking app mein → Browser mein session cookie stored hai
User ek malicious site visit karta hai → Wo site automatically banking app ko request bhejti hai
Browser automatically session cookie attach karta hai → Bank request ko legit samajh leta hai
Money transfer ho jata hai → User ko pata bhi nahi chala
```

### Why CSRF Works

| Condition | Result |
|-----------|--------|
| User authenticated hai | Session cookie browser mein stored |
| Browser auto-sends cookies | Har request mein cookie attach hota hai |
| Request state-changing hai | POST, PUT, DELETE — data modify hota hai |
| No CSRF token | Server ko pata nahi chalta ki request forged hai |

---

## CSRF Attack Mechanics

### Attack Example

```
Legitimate Banking Form (bank.com):
<form action="https://bank.com/transfer" method="POST">
    <input name="toAccount" value="12345">
    <input name="amount" value="100">
    <input type="submit" value="Transfer">
</form>

Malicious Site (evil.com):
<body onload="document.forms[0].submit()">
    <form action="https://bank.com/transfer" method="POST" style="display:none">
        <input name="toAccount" value="ATTACKER_ACCOUNT">
        <input name="amount" value="10000">
    </form>
</body>

User evil.com visit karta hai (bank.com mein logged in hai)
→ Form auto-submit hota hai
→ Browser bank.com cookie attach karta hai
→ Bank ko lagta hai legit request hai
→ ₹10,000 transfer ho gaya attacker ko!
```

### Why GET Requests Are Safe (Usually)

```
Browser same-origin policy:
- GET requests can be made cross-origin (reading data)
- POST/PUT/DELETE with cookies work cross-origin (dangerous)
- But reading response is blocked (can't see result)

CSRF doesn't need response:
- Attack sirf request bhejta hai (state change)
- Response dekhne ki zarurat nahi
- Money transfer already ho gaya
```

### Attack Vectors

```html
<!-- Hidden form (auto-submit) -->
<form action="https://victim.com/api/transfer" method="POST">
    <input name="amount" value="99999">
</form>
<script>document.forms[0].submit();</script>

<!-- Image tag trick (if GET changes state — bad design) -->
<img src="https://victim.com/api/delete-account?id=123" />

<!-- Fetch API (modern attack) -->
<script>
    fetch('https://victim.com/api/transfer', {
        method: 'POST',
        credentials: 'include',  // Auto-attach cookies
        body: JSON.stringify({amount: 99999, to: 'attacker'})
    });
</script>
```

---

## CsrfTokenRepository

**Matlab:** CSRF token generate, save, aur load karne ka interface. Spring Security ka core CSRF component.

### How CSRF Protection Works

```
1. User visits page → Spring CSRF token generate karta hai
2. Token form mein hidden field ya cookie mein store hota hai
3. User form submit karta hai → Token bhi submit hota hai
4. Server token verify karta hai → Match → Process request
5. No token / wrong token → 403 Forbidden

Attacker:
- Token nahi jaanta (same-origin policy)
- Forged request mein token nahi hai
- Server rejects → 403 Forbidden
```

### Default CsrfTokenRepository

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                // Default: HttpSessionCsrfTokenRepository
                // Token session mein store hota hai
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            );

        return http.build();
    }
}
```

### Form with CSRF Token

```html
<!-- Spring Security automatically adds CSRF token -->
<form action="/transfer" method="POST">
    <!-- Hidden CSRF token -->
    <input type="hidden" name="_csrf" value="a1b2c3d4-..." />

    <input name="toAccount" value="12345" />
    <input name="amount" value="100" />
    <input type="submit" value="Transfer" />
</form>

<!-- Thymeleaf automatically adds CSRF token in <form> -->
<form th:action="@{/transfer}" method="post">
    <!-- No need to manually add — Thymeleaf does it -->
</form>
```

### CSRF Token in AJAX Requests

```javascript
// CSRF token read karo (from meta tag)
const csrfToken = document.querySelector('meta[name="_csrf"]').getAttribute('content');
const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

// Request mein token include karo
fetch('/api/transfer', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        [csrfHeader]: csrfToken  // X-XSRF-TOKEN: a1b2c3d4-...
    },
    body: JSON.stringify({amount: 100, toAccount: '12345'})
});

// Axios (automatic with CookieCsrfTokenRepository)
axios.defaults.headers.common['X-XSRF-TOKEN'] = getCookie('XSRF-TOKEN');
```

### Meta Tag Setup (Thymeleaf)

```html
<head>
    <meta name="_csrf" th:content="${_csrf.token}" />
    <meta name="_csrf_header" th:content="${_csrf.headerName}" />
</head>
```

---

## CookieCsrfTokenRepository (SPA-Friendly)

**Matlab:** CSRF token ko cookie mein store karta hai. SPAs (React, Angular, Vue) ke liye ideal.

### Why Cookie-Based?

```
Traditional (HttpSession):
- Token server session mein stored
- SPA ko alag se fetch karna padta hai

Cookie-based:
- Token cookie mein → Browser automatically manages
- JS can read cookie → Token easily available
- Framework axios interceptors automatically add token
```

### Configuration

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            // Cookie name customize karna ho toh
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()
                .withCookieCustomizer(cookie -> {
                    cookie.setPath("/");
                    cookie.setSecure(true);     // Production: HTTPS only
                    cookie.setHttpOnly(false);  // JS-readable (required for SPA)
                    cookie.setAttribute("SameSite", "Lax");
                }))
        );

    return http.build();
}
```

### How It Works

```
1. Server response mein cookie set hoti hai:
   Set-Cookie: XSRF-TOKEN=abc123; Path=/; HttpOnly=false

2. Client JS cookie read karta hai:
   const token = getCookie('XSRF-TOKEN');  // "abc123"

3. Request mein header attach karta hai:
   X-XSRF-TOKEN: abc123

4. Server verifies: Cookie token == Header token → Valid
```

### Angular Integration (Automatic)

```typescript
// Angular automatically reads XSRF-TOKEN cookie and sends X-XSRF-TOKEN header
// Default cookie name: XSRF-TOKEN
// Default header name: X-XSRF-TOKEN

// No extra code needed — Angular's HttpClient does it automatically
this.http.post('/api/transfer', {amount: 100}).subscribe();
```

### React Integration

```javascript
// Helper function: Cookie read karo
function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Axios interceptor: Automatically add CSRF token
axios.interceptors.request.use(config => {
    const token = getCookie('XSRF-TOKEN');
    if (token) {
        config.headers['X-XSRF-TOKEN'] = token;
    }
    return config;
});

// Fetch wrapper
function fetchWithCSRF(url, options = {}) {
    const csrfToken = getCookie('XSRF-TOKEN');
    if (csrfToken && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(options.method)) {
        options.headers = {
            ...options.headers,
            'X-XSRF-TOKEN': csrfToken
        };
    }
    return fetch(url, options);
}
```

### HttpOnly: true vs false

```java
// ❌ HttpOnly=true → JS can't read cookie → SPA can't send CSRF token
CookieCsrfTokenRepository.withHttpOnlyTrue();

// ✅ HttpOnly=false → JS can read → SPA can send CSRF token header
CookieCsrfTokenRepository.withHttpOnlyFalse();

// Security trade-off:
// HttpOnly=false → XSS attacker cookie read kar sakta hai
// But CSRF token alone kaafi nahi — session cookie bhi chahiye
// Risk is acceptable for SPAs
```

---

## Disabling CSRF for Stateless REST APIs

**Matlab:** Jab aap JWT-based stateless API use kar rahe ho toh CSRF protection disable karna safe hai.

### When to Disable

```
CSRF needed (session cookies):
├── Form-based login
├── Session authentication
└── Cookie-based auth (JSESSIONID)

CSRF NOT needed (stateless):
├── JWT authentication (Bearer token in header)
├── API consumed by mobile apps
├── Microservice-to-microservice calls
└── CORS properly configured
```

### Why JWT Doesn't Need CSRF

```
Session Cookie Auth:
- Browser auto-attaches cookie → CSRF possible
- Attacker forges request → Browser sends cookie → Server accepts

JWT Auth:
- Token must be manually added to Authorization header
- Browser doesn't auto-attach JWT
- Attacker can't forge Authorization header
- No CSRF needed
```

### How to Disable

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())  // Disable CSRF for stateless API
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
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

### Conditional CSRF (Some Endpoints Public)

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .requireCsrfProtectionMatcher(request -> {
                // GET, HEAD, TRACE, OPTIONS ko skip karo
                String method = request.getMethod();
                if (method.equals("GET") || method.equals("HEAD") ||
                    method.equals("TRACE") || method.equals("OPTIONS")) {
                    return false;  // No CSRF needed
                }

                // API endpoints → no CSRF (JWT auth)
                if (request.getRequestURI().startsWith("/api/")) {
                    return false;
                }

                // Form submissions → CSRF needed
                return true;
            })
        );

    return http.build();
}
```

### Spring Security 6+ Approach

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .ignoringRequestMatchers("/api/**", "/h2-console/**")  // Skip CSRF for these
        );

    return http.build();
}
```

---

## CSRF + JWT

**Matlab:** JWT aur CSRF ka relationship. Generally JWT mein CSRF disable karte hain, par kuch edge cases hain.

### Standard JWT Setup (No CSRF)

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)  // Stateless → no CSRF
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}
```

### When JWT + CSRF Together

```
Scenario: JWT in HttpOnly cookie (not localStorage)

Why HttpOnly cookie for JWT?
- XSS attack se safe (JS can't read HttpOnly cookie)
- But → CSRF attack possible (browser auto-attaches cookie)
- Solution → CSRF token bhi chahiye
```

### JWT in Cookie + CSRF Protection

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf
            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        )
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
        );

    return http.build();
}

// JWT cookie mein set karo
public void addJwtCookie(HttpServletResponse response, String token) {
    ResponseCookie cookie = ResponseCookie.from("JWT", token)
        .httpOnly(true)        // JS can't read (XSS safe)
        .secure(true)          // HTTPS only
        .path("/")
        .maxAge(3600)
        .sameSite("Lax")       // CSRF protection (modern browsers)
        .build();

    response.addHeader("Set-Cookie", cookie.toString());
}
```

### CSRF Token + JWT Cookie Flow

```
Login Response:
    Set-Cookie: JWT=eyJ...; HttpOnly; Secure; SameSite=Lax
    Set-Cookie: XSRF-TOKEN=abc123; HttpOnly=false; Secure

Frontend reads XSRF-TOKEN cookie → JavaScript can access
Frontend sends request:
    Cookie: JWT=eyJ...; XSRF-TOKEN=abc123  (auto-attached)
    X-XSRF-TOKEN: abc123  (manually added by JS)

Server verifies:
    1. JWT valid? → Yes
    2. Cookie XSRF-TOKEN == Header X-XSRF-TOKEN? → Yes
    3. Process request ✅
```

---

## SameSite Cookie Attribute

**Matlab:** Modern browser ka built-in CSRF protection. Cookie ko same-site requests tak limit karta hai.

### SameSite Values

```java
ResponseCookie cookie = ResponseCookie.from("JSESSIONID", sessionId)
    .sameSite("Strict")   // Most restrictive
    .sameSite("Lax")      // Balanced (recommended)
    .sameSite("None")     // No restriction (requires Secure)
    .build();
```

### SameSite Behavior

| Value | Same-site Request | Cross-site Navigation | Cross-site API Call |
|-------|------------------|----------------------|---------------------|
| **Strict** | Cookie sent | ❌ Cookie NOT sent | ❌ Cookie NOT sent |
| **Lax** | Cookie sent | ✅ Cookie sent (GET only) | ❌ Cookie NOT sent |
| **None** | Cookie sent | ✅ Cookie sent | ✅ Cookie sent |

### SameSite as CSRF Alternative

```
Traditional CSRF:
- Token generate → form/cookie → verify → complex

SameSite Cookie:
- Cookie pe SameSite=Lax set karo
- Browser automatically blocks cross-site requests
- No token management needed

⚠️ SameSite is NOT a complete replacement for CSRF:
- Legacy browsers don't support it
- Doesn't protect same-origin attacks (XSS)
- Use both for defense in depth
```

### Recommended Cookie Settings

```java
// Production-ready cookie
ResponseCookie sessionCookie = ResponseCookie.from("JSESSIONID", sessionId)
    .httpOnly(true)        // XSS protection
    .secure(true)          // HTTPS only
    .path("/")
    .maxAge(86400)         // 24 hours
    .sameSite("Lax")       // CSRF protection
    .build();

// JWT cookie
ResponseCookie jwtCookie = ResponseCookie.from("JWT", token)
    .httpOnly(true)
    .secure(true)
    .path("/")
    .maxAge(3600)
    .sameSite("Strict")    // Strict for auth cookies
    .build();

// CSRF token cookie (must be readable by JS)
ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", csrfToken)
    .httpOnly(false)       // JS needs to read
    .secure(true)
    .path("/")
    .sameSite("Lax")
    .build();
```

---

## CSRF Decision Guide

### Quick Decision Tree

```
Is your API stateless (JWT in Authorization header)?
├── Yes → csrf.disable()
└── No → Continue
    │
    Is it a web app with session cookies?
    ├── Yes → Keep CSRF enabled
    │   ├── SPA? → CookieCsrfTokenRepository.withHttpOnlyFalse()
    │   └── Server-rendered? → Default HttpSessionCsrfTokenRepository
    │
    └── No → It's stateless → csrf.disable()
```

### Configuration Cheat Sheet

```java
// 1. Stateless REST API (JWT in header) — NO CSRF
http.csrf(AbstractHttpConfigurer::disable)

// 2. Session-based web app — CSRF ENABLED (default)
http.csrf(Customizer.withDefaults())

// 3. Session-based SPA (Angular/React) — Cookie CSRF
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)

// 4. JWT in HttpOnly cookie — CSRF + SameSite
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)
.cookieCookie(c -> c.sameSite("Lax"))

// 5. Skip CSRF for specific endpoints
http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/webhook/**"))
```

---

## Quick Reference

```java
// Disable CSRF (stateless JWT API)
http.csrf(csrf -> csrf.disable())

// Cookie CSRF for SPAs
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
)

// SameSite cookie attribute
ResponseCookie.from("SESSION", id)
    .sameSite("Lax")
    .httpOnly(true)
    .secure(true)
    .build()

// CSRF token in JS
const token = getCookie('XSRF-TOKEN');
fetch('/api/data', {
    method: 'POST',
    headers: {'X-XSRF-TOKEN': token}
});
```

---

## Summary

| Feature | Use When |
|---------|----------|
| **CSRF enabled (default)** | Session cookies, form-based auth |
| **CSRF disabled** | Stateless JWT API (token in Authorization header) |
| **CookieCsrfTokenRepository** | SPAs (Angular, React, Vue) |
| **HttpSessionCsrfTokenRepository** | Server-rendered pages (Thymeleaf, JSP) |
| **SameSite=Lax** | Modern CSRF protection (all cookies) |
| **SameSite=Strict** | Extra strict CSRF protection |

**Golden Rule:** Disable CSRF only if stateless (JWT in header) — if using session cookies, keep CSRF enabled.
