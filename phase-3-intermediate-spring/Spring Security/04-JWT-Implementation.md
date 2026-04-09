# JWT Implementation

## Status: Not Started

---

## Table of Contents

1. [JWT Overview](#jwt-overview)
2. [JWT Structure](#jwt-structure)
3. [JJWT Library Setup](#jjwt-library-setup)
4. [Token Generation](#token-generation)
5. [Token Validation](#token-validation)
6. [Claims Extraction](#claims-extraction)
7. [JwtFilter (OncePerRequestFilter)](#jwtfilter-onceperrequestfilter)
8. [Refresh Token Strategy](#refresh-token-strategy)
9. [Token Blacklisting](#token-blacklisting)
10. [Complete JWT Setup](#complete-jwt-setup)

---

## JWT Overview

**JWT (JSON Web Token) ka matlab:** Ek stateless authentication mechanism jo user ki identity ko ek signed token mein encode karta hai.

### Key Principle

```
JWT is STATELESS — server doesn't store session.
Validate signature + expiry + claims on every request.
```

### Session vs JWT

| Feature | Session-based | JWT |
|---------|--------------|-----|
| **State** | Server pe store hota hai | Stateless — server kuch store nahi karta |
| **Storage** | Server memory / Redis | Client side (localStorage, cookie) |
| **Scalability** | Sticky sessions needed | Easy horizontal scaling |
| **Size** | Small (cookie mein sirf session ID) | Bada (pura token har request mein) |
| **Logout** | Simple (session invalidate) | Complex (token blacklist chahiye) |
| **Best for** | Monolith, web apps | REST APIs, microservices, SPAs |

### JWT Flow

```
1. User login karta hai (username/password)
    ↓
2. Server credentials verify karta hai
    ↓
3. Server JWT generate karta hai (signed token)
    ↓
4. Token client ko return hota hai
    ↓
5. Client har request mein token bhejta hai (Authorization header)
    ↓
6. Server token validate karta hai (signature + expiry)
    ↓
7. Valid → user authenticated, Invalid → 401
```

---

## JWT Structure

**Matlab:** JWT teen parts ka hota hai — `header.payload.signature` — jo dot (.) se join hote hain.

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYWNoaW4iLCJyb2xlIjoiQURNSU4iLCJleHAiOjE3MTIzNDU2Nzh9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
│                          │                                                           │
├────── Header ────────────┤ ├─────────────────── Payload ──────────────────────────────┤ ├────── Signature ───────────────────────────────────────────────────┤
```

### Header

```json
{
  "alg": "HS256",     // Algorithm: HMAC SHA-256
  "typ": "JWT"        // Token type
}
```

| Algorithm | Description |
|-----------|-------------|
| **HS256** | HMAC + SHA-256 (symmetric — same secret key) |
| **RS256** | RSA + SHA-256 (asymmetric — public/private key pair) |
| **ES256** | ECDSA + SHA-256 (elliptic curve) |

### Payload (Claims)

```json
{
  "sub": "sachin",                 // Subject (username)
  "iat": 1712345678,               // Issued At (timestamp)
  "exp": 1712432078,               // Expiration Time (1 hour)
  "role": "ADMIN",                 // Custom claim
  "userId": 42,                    // Custom claim
  "email": "sachin@example.com"   // Custom claim
}
```

### Standard Claims

| Claim | Full Name | Description |
|-------|-----------|-------------|
| **sub** | Subject | User identifier (username, email, user ID) |
| **iat** | Issued At | Token generate hone ka time |
| **exp** | Expiration | Token expire hone ka time |
| **iss** | Issuer | Token kisne issue kiya |
| **aud** | Audience | Token kis ke liye hai |
| **jti** | JWT ID | Unique token ID (blacklisting ke liye) |

### Signature

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

**Signature ka kaam:** Token tamper toh nahi hua yeh verify karna. Agar koi payload change karega toh signature mismatch ho jayega.

---

## JJWT Library Setup

**Matlab:** Java ka most popular JWT library. Modern API, type-safe claims.

### Maven Dependency

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.5</version>
    <scope>runtime</scope>
</dependency>
```

### Gradle Dependency

```groovy
implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
```

### application.properties

```properties
# JWT Configuration
jwt.secret=mySuperSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm
jwt.expiration=86400000          # 24 hours in milliseconds
jwt.refresh-expiration=604800000 # 7 days in milliseconds
```

---

## Token Generation

**Matlab:** User authenticate hone ke baad JWT banana.

### JwtService (Utility Class)

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Token generate karo
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // Custom claims add karo
        if (userDetails instanceof CustomUserDetails customUser) {
            extraClaims.put("userId", customUser.getId());
            extraClaims.put("role", customUser.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        }

        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    // Core token builder
    private String buildToken(Map<String, Object> extraClaims,
                              UserDetails userDetails,
                              long expiration) {
        return Jwts.builder()
            .claims(extraClaims)                              // Custom claims
            .subject(userDetails.getUsername())               // Subject
            .issuedAt(new Date(System.currentTimeMillis()))   // Issued at
            .expiration(new Date(System.currentTimeMillis() + expiration))  // Expiry
            .signWith(getSigningKey())                        // Sign with secret
            .compact();                                       // Build token
    }

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### Token Generation in Auth Controller

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        // 1. Authenticate
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        // 2. Load UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        // 3. Generate JWT
        String token = jwtService.generateToken(userDetails);

        // 4. Return token
        return ResponseEntity.ok(new AuthResponse(token));
    }
}

// Response DTOs
public record LoginRequest(String email, String password) {}
public record AuthResponse(String accessToken) {}
```

### Token Response with Extra Info

```java
public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String refreshToken  // Optional
) {
    public AuthResponse(String accessToken, long expiresIn) {
        this(accessToken, "Bearer", expiresIn, null);
    }
}

@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
    );

    UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
    String token = jwtService.generateToken(userDetails);

    return ResponseEntity.ok(new AuthResponse(token, jwtService.getExpiration()));
}
```

---

## Token Validation

**Matlab:** Client se aaye token ko verify karna — signature check, expiry check, claims extract karna.

### Validation Methods

```java
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    // Username extract karo token se
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Expiration extract karo
    public Date extractExpiration(String token) {
        return extractClaim(Claims::getExpiration);
    }

    // Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // All claims extract karo
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    // Token valid hai ya nahi
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Expired hai ya nahi
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Key getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### Exception Handling

```java
public class JwtService {

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        } catch (ExpiredJwtException e) {
            // Token expire ho gaya
            throw new JwtAuthenticationException("Token has expired");

        } catch (UnsupportedJwtException e) {
            // Token format support nahi hai
            throw new JwtAuthenticationException("Unsupported JWT token");

        } catch (MalformedJwtException e) {
            // Token structure galat hai
            throw new JwtAuthenticationException("Invalid JWT token");

        } catch (SignatureException e) {
            // Signature match nahi hua — tampered token
            throw new JwtAuthenticationException("Invalid JWT signature");

        } catch (IllegalArgumentException e) {
            // Token empty ya null hai
            throw new JwtAuthenticationException("JWT token is empty");
        }
    }
}
```

---

## Claims Extraction

**Matlab:** Token se user ki info (username, roles, userId, etc.) nikalna.

### Extract All Claims

```java
@Service
public class JwtService {

    // Simple claims extraction
    public Claims getAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    // Specific claim
    public String getUserId(String token) {
        Claims claims = getAllClaims(token);
        return claims.get("userId", String.class);
    }

    public List<String> getRoles(String token) {
        Claims claims = getAllClaims(token);
        return claims.get("role", List.class);
    }

    public boolean isAdmin(String token) {
        List<String> roles = getRoles(token);
        return roles.contains("ROLE_ADMIN");
    }
}
```

### Using Claims in Controller

```java
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final JwtService jwtService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);  // Remove "Bearer "

        Claims claims = jwtService.getAllClaims(token);

        return ResponseEntity.ok(Map.of(
            "username", claims.getSubject(),
            "userId", claims.get("userId", Long.class),
            "roles", claims.get("role", List.class),
            "issuedAt", claims.getIssuedAt(),
            "expiresAt", claims.getExpiration()
        ));
    }
}
```

---

## JwtFilter (OncePerRequestFilter)

**Matlab:** Custom filter jo har request se JWT token extract karta hai, validate karta hai, aur SecurityContext set karta hai.

### JwtAuthenticationFilter

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization header se token extract karo
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No token → filter chain continue karo (maybe public endpoint)
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);  // "Bearer " hata do

        try {
            // 2. Token se username extract karo
            final String username = jwtService.extractUsername(jwt);

            // 3. Agar username hai aur SecurityContext empty hai
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 4. UserDetailsService se UserDetails load karo
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. Token validate karo
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // 6. Authentication token banao
                    UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,  // No credentials needed (already validated via JWT)
                            userDetails.getAuthorities()
                        );

                    // Request details add karo
                    authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 7. SecurityContext mein set karo
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

        } catch (Exception e) {
            logger.error("Cannot process JWT: " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid or expired token\"}");
            return;
        }

        // 8. Filter chain continue karo
        filterChain.doFilter(request, response);
    }
}
```

### Register Filter in Security Config

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Stateless API → CSRF disable
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // Login/register public
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // No session
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);  // JWT filter

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## Refresh Token Strategy

**Matlab:** Access token chhoti expiry ka hota hai (15 min - 1 hour). Refresh token lambi expiry ka hota hai (7-30 days) jo naya access token generate karta hai.

### Why Refresh Tokens?

```
Problem: Access token 15 min ka hai. Har 15 min baad user ko login karna padega.

Solution: Refresh token 7 days ka do. Jab access token expire ho,
          refresh token se naya access token generate karo (without login).
```

### Token Pair Flow

```
┌─────────────────────────────────────────────────────────────┐
│                     Initial Login                            │
│                                                             │
│  Client → POST /api/auth/login {email, password}            │
│                                                             │
│  Server → {                                                │
│      "accessToken": "eyJ...",     (15 min)                  │
│      "refreshToken": "eyJ...",    (7 days)                  │
│      "tokenType": "Bearer"                                  │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              Access Token Expired (after 15 min)             │
│                                                             │
│  Client → POST /api/auth/refresh {refreshToken}             │
│                                                             │
│  Server → {                                                 │
│      "accessToken": "eyJ...",     (new, 15 min)             │
│      "refreshToken": "eyJ...",    (new, 7 days - rotated)   │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

### Refresh Token Implementation

```java
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    // Refresh token generate karo
    public String generateRefreshToken(UserDetails userDetails) {
        return Jwts.builder()
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
            .claim("type", "refresh")  // Custom claim: yeh refresh token hai
            .signWith(jwtService.getSigningKey())
            .compact();
    }

    // Refresh token se naya access token banao
    public AuthResponse refreshAccessToken(String refreshToken) {
        // 1. Validate refresh token
        if (!jwtService.isTokenValid(refreshToken,
                userDetailsService.loadUserByUsername(
                    jwtService.extractUsername(refreshToken)))) {
            throw new JwtAuthenticationException("Invalid refresh token");
        }

        // 2. Check ki yeh actually refresh token hai
        String type = jwtService.extractClaim(refreshToken,
            claims -> claims.get("type", String.class));

        if (!"refresh".equals(type)) {
            throw new JwtAuthenticationException("Invalid token type");
        }

        // 3. Naya access token generate karo
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String newAccessToken = jwtService.generateToken(userDetails);

        // 4. Naya refresh token bhi do (rotation)
        String newRefreshToken = generateRefreshToken(userDetails);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }
}

// Updated response
public record AuthResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn
) {
    public AuthResponse(String accessToken, String refreshToken) {
        this(accessToken, refreshToken, "Bearer", 900000);  // 15 min
    }

    public AuthResponse(String accessToken) {
        this(accessToken, null, "Bearer", 900000);
    }
}
```

### Refresh Endpoint

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        AuthResponse response = refreshTokenService.refreshAccessToken(request.refreshToken());
        return ResponseEntity.ok(response);
    }
}

public record RefreshRequest(String refreshToken) {}
```

### Token Rotation (Security Best Practice)

```
Old refresh token → Naya refresh token → Purana wala invalidate

Benefit: Agar refresh token steal ho gaya toh:
1. Victim uses stolen token → gets new pair
2. Attacker uses original token → server detects (already used)
3. Server invalidates both tokens → attacker caught
```

---

## Token Blacklisting

**Matlab:** Logout pe token ko invalidate karna. Kyunki JWT stateless hai, server automatically logout nahi kar sakta — isliye blacklist chahiye.

### Why Blacklist?

```
JWT stateless hai → server token store nahi karta
Problem: User logout kare toh token abhi bhi valid hai (expiry tak)
Solution: Blacklist mein token ka JTI (JWT ID) store karo
```

### Redis-based Blacklist

```java
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Token blacklist mein add karo
    public void blacklistToken(String token) {
        Claims claims = extractClaims(token);
        String jti = claims.getId();

        if (jti == null) {
            jti = token.hashCode() + "";  // Fallback
        }

        // TTL = token ki remaining expiry
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();

        redisTemplate.opsForValue().set(
            "blacklist:" + jti,
            "blacklisted",
            remainingTime,
            TimeUnit.MILLISECONDS
        );
    }

    // Check if token blacklisted hai
    public boolean isBlacklisted(String token) {
        Claims claims = extractClaims(token);
        String jti = claims.getId();

        if (jti == null) {
            jti = token.hashCode() + "";
        }

        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
    }
}
```

### Update JwtService with JTI

```java
// Token mein JTI add karo
public String generateToken(UserDetails userDetails) {
    Map<String, Object> extraClaims = new HashMap<>();
    extraClaims.put("jti", UUID.randomUUID().toString());  // Unique ID

    return buildToken(extraClaims, userDetails, jwtExpiration);
}

// Validation mein blacklist check
public boolean isTokenValid(String token, UserDetails userDetails) {
    // Check blacklist
    if (tokenBlacklistService.isBlacklisted(token)) {
        return false;  // Token blacklisted hai
    }

    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
}
```

### Logout Endpoint

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenBlacklistService blacklistService;

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        blacklistService.blacklistToken(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
```

### Blacklist Storage Options

| Storage | Pros | Cons |
|---------|------|------|
| **Redis** | Fast, TTL auto-expiry | Extra dependency |
| **Database** | No extra dependency | Slower, cleanup needed |
| **In-Memory (ConcurrentHashMap)** | Simple, no dependency | Lost on restart, not distributed |
| **Short-lived tokens** | No blacklist needed | User ko baar-baar login |

---

## Complete JWT Setup

### Project Structure

```
src/main/java/com/app/
├── config/
│   └── SecurityConfig.java          # SecurityFilterChain + JWT filter
├── filter/
│   └── JwtAuthenticationFilter.java # OncePerRequestFilter for JWT
├── service/
│   ├── JwtService.java              # Token generate/validate/extract
│   └── RefreshTokenService.java     # Refresh token logic
│   └── TokenBlacklistService.java   # Logout/blacklist
├── controller/
│   └── AuthController.java          # Login, register, refresh, logout
├── model/
│   └── User.java                    # JPA entity
├── repository/
│   └── UserRepository.java          # Spring Data JPA
└── dto/
    ├── LoginRequest.java
    ├── AuthResponse.java
    └── RefreshRequest.java
```

### Complete SecurityConfig

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

## Quick Reference

```java
// Token generate
String token = Jwts.builder()
    .claims(extraClaims)
    .subject(username)
    .issuedAt(new Date())
    .expiration(new Date(System.currentTimeMillis() + expiration))
    .signWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
    .compact();

// Token validate
Claims claims = Jwts.parser()
    .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes()))
    .build()
    .parseSignedClaims(token)
    .getPayload();

// JWT Filter chain
.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

// Stateless session
.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

// Token structure
header.payload.signature  (base64Url encoded, separated by dots)
```

---

## Summary

| Component | Purpose |
|-----------|---------|
| **JWT Structure** | header.algorithm, payload.claims, signature.verification |
| **JJWT Library** | Modern Java JWT library (jjwt-api, jjwt-impl, jjwt-jackson) |
| **Token Generation** | User authenticate hone ke baad signed token banao |
| **Token Validation** | Signature + expiry + username match check karo |
| **Claims Extraction** | Token se user info nikalo (username, roles, userId) |
| **JwtFilter** | OncePerRequestFilter — har request mein token validate karo |
| **Refresh Token** | Short access token + long refresh token (rotation recommended) |
| **Token Blacklisting** | Logout pe token invalidate karo (Redis with TTL recommended) |
