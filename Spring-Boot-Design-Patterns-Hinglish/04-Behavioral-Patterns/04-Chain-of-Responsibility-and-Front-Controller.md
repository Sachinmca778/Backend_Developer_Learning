# Behavioral Pattern: Chain of Responsibility & Front Controller ⛓️

---

## ⛓️ 1. Chain of Responsibility Pattern

### Core Concept:
> **A request passes through a sequential chain of handlers. Each handler processes its specific duty, then passes the request to the next handler in the chain — or STOPS the execution right there.**  
> *(Analogy: Airport Security Check:  
> Step 1: Airport Entry par Ticket Check $\rightarrow$  
> Step 2: ID verification $\rightarrow$  
> Step 3: Baggage Security Scan [Agar illegal item mila, yahi par STOP aur arrest!] $\rightarrow$  
> Step 4: Boarding Gate).*

---

### In Spring Boot: The Servlet Filter Chain

Spring Security aur Web requests ka poora architecture **Chain of Responsibility Pattern** par bana hai:

```
[ Incoming HTTP Request ]
          │
          ▼
   [ CorsFilter ] ──────────────────> (Handles cross-origin CORS headers)
          │
          ▼
 [ JwtAuthFilter ] ─────────────────> (Validates Bearer token. If INVALID ──> Returns 401 & STOPS CHAIN!)
          │
          ▼
  [ LoggingFilter ] ────────────────> (Logs latency & request URI)
          │
          ▼
[ AuthorizationFilter ] ────────────> (Checks user role permissions. If NO ACCESS ──> Returns 403 & STOPS!)
          │
          ▼
  [ DispatcherServlet ] ────────────> [ Target REST Controller ]
```

---

### Custom Filter Implementation (`OncePerRequestFilter`)

Spring Boot me custom handler chain step banana bahut simple hai:

```java
@Component
public class RequestPerformanceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestPerformanceFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();

        // 1. Pre-Processing Logic: Check custom header
        String apiKey = request.getHeader("X-API-KEY");
        if (apiKey != null && apiKey.equals("BLOCKED")) {
            // STOP THE CHAIN RIGHT HERE!
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("Blocked API Key");
            return; // Do NOT call chain.doFilter -> Request stops!
        }

        // 2. PASS TO NEXT HANDLER IN THE CHAIN
        filterChain.doFilter(request, response);

        // 3. Post-Processing Logic: Calculate execution latency
        long duration = System.currentTimeMillis() - startTime;
        log.info("URI: {} | Status: {} | Duration: {} ms", 
                 request.getRequestURI(), response.getStatus(), duration);
    }
}
```

---

### Other Chains Inside Spring Boot:

1. **`SecurityFilterChain`:** Spring Security 6+ me saare security rules (`UsernamePasswordAuthenticationFilter`, `BearerTokenAuthenticationFilter`, `ExceptionTranslationFilter`) isi chain me order-wise run hote hain.
2. **`HandlerInterceptor`:** Controller level par pre/post execution chain:
   - `preHandle()`: Controller execute hone se pehle (return `false` karoge to execution ruk jayegi).
   - `postHandle()`: Controller execute hone ke baad.
   - `afterCompletion()`: View render hone ya response stream complete hone ke baad.

---

## 🏛️ 2. Front Controller Pattern

### Core Concept:
> **A single, centralized entry-point controller that receives ALL incoming requests for an application and routes them to appropriate individual handlers.**

- Traditional Java Web (Servlets) me har URL ke liye alag servlet configure karna padta tha (`/user` ke liye `UserServlet`, `/order` ke liye `OrderServlet`). Maintenance disaster tha!
- Spring MVC ka **`DispatcherServlet`** pure application ke liye **Front Controller** ki tarah kaam karta hai.

```
Client Requests (GET /users, POST /orders, DELETE /items)
                         │
                         ▼
             [ DispatcherServlet ]  <─── (The Front Controller)
                         │
       ┌─────────────────┼─────────────────┐
       ▼                 ▼                 ▼
[ UserController ] [ OrderController ] [ ProductController ]
```

---

### 💡 Pro Interview Tip:
> **Question:** *"What is the best real-world example of Chain of Responsibility in enterprise Java?"*  
> **Your Answer:** *"Spring Security's **`SecurityFilterChain`**. Every HTTP request flows through an ordered sequence of servlet filters (CORS, CSRF, JWT Authentication, Role Authorization). Any filter can validate the request and call `chain.doFilter()`, or immediately reject the request (HTTP 401/403) and terminate the chain without hitting the controller!"*
