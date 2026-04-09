# OAuth2 & OpenID Connect

## Status: Not Started

---

## Table of Contents

1. [OAuth2 Overview](#oauth2-overview)
2. [OAuth2 Roles](#oauth2-roles)
3. [Grant Types](#grant-types)
4. [Authorization Code + PKCE](#authorization-code--pkce)
5. [Client Credentials Grant](#client-credentials-grant)
6. [Refresh Token Grant](#refresh-token-grant)
7. [Spring Security OAuth2 Resource Server](#spring-security-oauth2-resource-server)
8. [JWT Decoder](#jwt-decoder)
9. [Opaque Token Introspection](#opaque-token-introspection)
10. [OpenID Connect (OIDC)](#openid-connect-oidc)
11. [Complete OAuth2 Setup](#complete-oauth2-setup)

---

## OAuth2 Overview

**OAuth2 ka matlab:** Ek authorization framework jo third-party apps ko limited access deta hai user ke resources pe — without sharing password.

### Real-World Example

```
Scenario: Aap ek app ko apna Google photos access karne dete ho

Traditional (unsafe):
    App ko apna Google password do → App photos access kare
    ❌ Password leak risk, full account access

OAuth2 (safe):
    App Google pe redirect karta hai → Google login karo → App ko sirf photos access do
    ✅ Password app ko nahi mila, sirf photos access (scope limited)
```

### OAuth2 vs Authentication

```
OAuth2 = Authorization (access delegation)
    "I allow this app to access my Google Calendar"

OpenID Connect = Authentication (identity verification)
    "Prove you are this Google user"

OIDC is built ON TOP of OAuth2
```

---

## OAuth2 Roles

**Matlab:** OAuth2 ecosystem mein 4 key players hote hain.

### The 4 Roles

```
┌─────────────────────────────────────────────────────────────┐
│                   OAuth2 Ecosystem                           │
│                                                             │
│  1. Resource Owner (User)                                   │
│     → Woh insaan jiska data access ho raha hai              │
│                                                             │
│  2. Client (App)                                            │
│     → Woh app jo resource access karna chahti hai            │
│     → Frontend app (React, mobile), ya backend service       │
│                                                             │
│  3. Authorization Server (Auth Provider)                    │
│     → Google, GitHub, Okta jo tokens issue karta hai        │
│     → User authenticate karta hai yahan                      │
│     → Access tokens + Refresh tokens deta hai                │
│                                                             │
│  4. Resource Server (API)                                   │
│     → Woh server jiska data access ho raha hai               │
│     → Access token validate karta hai                        │
│     → Protected resources return karta hai                   │
└─────────────────────────────────────────────────────────────┘
```

### Flow Diagram

```
User (Resource Owner)
    ↓ "Login with Google" pe click karta hai
Client App (your frontend)
    ↓ Google Auth Server pe redirect
Authorization Server (Google)
    ↓ User login karta hai → consent deta hai
    ↓ Authorization Code → Client ko milta hai
Client App
    ↓ Auth Code → Auth Server → Access Token
    ↓ Access Token → Resource Server (API)
Resource Server (your backend API)
    ↓ Token validate → Protected data return
```

### Real-World Mapping

```
Google Login Example:
┌────────────────────┬─────────────────────────────────┐
│     Role           │     Example                     │
├────────────────────┼─────────────────────────────────┤
│ Resource Owner     → | You (the user)                  │
│ Client             → | Spotify app (wants Google data) │
│ Auth Server        → | Google (accounts.google.com)    │
│ Resource Server    → | Google Calendar API             │
└────────────────────┴─────────────────────────────────┘

Microservices Example:
┌────────────────────┬─────────────────────────────────┐
│     Role           │     Example                     │
├────────────────────┼─────────────────────────────────┤
│ Resource Owner     → | System (no human user)          │
│ Client             → | Order Service                   │
│ Auth Server        → | Keycloak / Auth0                │
│ Resource Server    → | Inventory Service               │
└────────────────────┴─────────────────────────────────┘
```

---

## Grant Types

**Matlab:** Different ways jinse client access token obtain kar sakta hai. Har grant type ek specific use case ke liye hai.

### Available Grant Types

| Grant Type | Use Case | User Involved? |
|------------|----------|----------------|
| **Authorization Code + PKCE** | SPAs, mobile apps | ✅ Yes |
| **Client Credentials** | Service-to-service (M2M) | ❌ No |
| **Refresh Token** | Token renewal | ❌ No |
| **Authorization Code** | Server-side web apps | ✅ Yes |
| **Device Code** | IoT, TV, CLI tools | ✅ Yes |
| **Implicit** | ❌ Deprecated (use PKCE) | ✅ Yes |

### Quick Comparison

```
Authorization Code + PKCE:
    User → App → Auth Server (login) → Code → Token
    Best for: React, Angular, Vue, Mobile apps

Client Credentials:
    Service → Auth Server → Token
    Best for: Microservice → Microservice

Refresh Token:
    Service → Auth Server (with refresh_token) → New access_token
    Best for: Token renewal without re-login
```

---

## Authorization Code + PKCE

**Matlab:** Most secure grant type for public clients (SPAs, mobile apps). PKCE (Proof Key for Code Exchange) code interception attacks se bachata hai.

### Why PKCE?

```
Without PKCE:
    Attacker authorization code intercept karta hai
    Code ko token exchange kar leta hai
    Attacker ko access token mil gaya

With PKCE:
    Client code_verifier generate karta hai (secret)
    code_challenge = hash(code_verifier) → Auth Server ko bhejta hai
    Code → Token exchange pe code_verifier bhejna padta hai
    Attacker ke paas code_verifier nahi → Token nahi milega
```

### PKCE Flow

```
┌─────────────────────────────────────────────────────────────┐
│  Step 1: Client generates PKCE pair                         │
│                                                             │
│  code_verifier = randomString(128)                          │
│  code_challenge = SHA256(code_verifier) → Base64URL         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Step 2: User redirected to Auth Server                     │
│                                                             │
│  GET /authorize?                                            │
│    response_type=code                                       │
│    client_id=myApp                                          │
│    redirect_uri=https://myapp.com/callback                  │
│    scope=openid profile email                               │
│    code_challenge=E9Melhoa2OwvFrEMTJ...                     │
│    code_challenge_method=S256                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Step 3: User logs in → Consents → Auth Server redirects    │
│                                                             │
│  GET https://myapp.com/callback?code=SplxlOBeZQQY...        │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│  Step 4: Client exchanges code + code_verifier for token    │
│                                                             │
│  POST /token                                                │
│    grant_type=authorization_code                            │
│    code=SplxlOBeZQQY...                                     │
│    client_id=myApp                                          │
│    redirect_uri=https://myapp.com/callback                  │
│    code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW...       │
│                                                             │
│  Response: {                                                │
│    "access_token": "eyJ...",                                │
│    "token_type": "Bearer",                                  │
│    "expires_in": 3600,                                      │
│    "refresh_token": "dGhpc..."                              │
│  }                                                          │
└─────────────────────────────────────────────────────────────┘
```

### Spring Security OAuth2 Client Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

### application.yml

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id
            client-secret: your-client-secret
            scope: openid, profile, email
            redirect-uri: "{baseUrl}/login/oauth2/code/{registrationId}"
          github:
            client-id: your-github-client-id
            client-secret: your-github-client-secret
            scope: read:user
        provider:
          google:
            issuer-uri: https://accounts.google.com
```

### OAuth2 Login Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())
                )
            );

        return http.build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return userRequest -> {
            OAuth2User oauth2User = new DefaultOAuth2UserService()
                .loadUser(userRequest);

            // Custom logic: User ko database mein save/update karo
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String provider = userRequest.getClientRegistration().getRegistrationId();

            // Save or update user in database
            // ...

            return oauth2User;
        };
    }
}
```

### Accessing OAuth2 User Info

```java
@RestController
public class UserController {

    @GetMapping("/user/oauth2")
    public Map<String, Object> getUser(OAuth2User oauth2User) {
        return Map.of(
            "name", oauth2User.getAttribute("name"),
            "email", oauth2User.getAttribute("email"),
            "picture", oauth2User.getAttribute("picture"),
            "attributes", oauth2User.getAttributes()
        );
    }

    // With authentication
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            return ResponseEntity.ok(Map.of(
                "type", "OAuth2",
                "provider", authentication.getName(),
                "name", oauth2User.getAttribute("name"),
                "email", oauth2User.getAttribute("email")
            ));
        }

        // Regular login
        return ResponseEntity.ok(Map.of(
            "type", "Local",
            "username", authentication.getName()
        ));
    }
}
```

---

## Client Credentials Grant

**Matlab:** Service-to-service (machine-to-machine) authentication. User involved nahi hota. Microservice authentication ke liye use hota hai.

### Use Case

```
Order Service → Inventory Service (check stock)
Order Service → Payment Service (process payment)
Notification Service → Email Service (send email)

No user login — services directly authenticate each other
```

### Flow

```
┌─────────────────────────────────────────────────────────────┐
│  Client Service wants to access Resource Service            │
│                                                             │
│  1. Client → Auth Server (with client_id + client_secret)   │
│                                                             │
│     POST /oauth/token                                       │
│       grant_type=client_credentials                         │
│       client_id=order-service                               │
│       client_secret=service-secret                          │
│       scope=inventory:read                                  │
│                                                             │
│  2. Auth Server → Client (access token)                     │
│                                                             │
│     {                                                       │
│       "access_token": "eyJ...",                             │
│       "token_type": "Bearer",                               │
│       "expires_in": 3600,                                   │
│       "scope": "inventory:read"                             │
│     }                                                       │
│                                                             │
│  3. Client → Resource Service (with Bearer token)           │
│                                                             │
│     GET /api/inventory/123                                  │
│     Authorization: Bearer eyJ...                            │
│                                                             │
│  4. Resource Service → Validates token → Returns data       │
└─────────────────────────────────────────────────────────────┘
```

### Spring Security Resource Server

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### application.yml

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://auth-server:9000  # Auth server URL
          jwk-set-uri: http://auth-server:9000/oauth2/jwks  # Public key endpoint
```

### Resource Server Config

```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/inventory/**").hasAuthority("SCOPE_inventory:read")
                .requestMatchers("/api/inventory/write").hasAuthority("SCOPE_inventory:write")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(Customizer.withDefaults())
            );

        return http.build();
    }
}
```

### Client Side (Calling Another Service)

```java
@Service
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final OAuth2AuthorizedClientService clientService;

    public InventoryResponse checkStock(Long productId) {
        // Get access token
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
            .withClientRegistrationId("inventory-service")
            .principal("order-service")  // Service identity
            .build();

        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
            "inventory-service", "order-service"
        );

        String token = client.getAccessToken().getTokenValue();

        // Call inventory service
        return restTemplate.exchange(
            "http://inventory-service/api/inventory/{id}",
            HttpMethod.GET,
            new HttpEntity<>(Map.of("Authorization", "Bearer " + token)),
            InventoryResponse.class,
            productId
        ).getBody();
    }
}
```

### WebClient Approach (Recommended)

```java
@Configuration
public class WebClientConfig {

    @Bean
    @RequestScope
    public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2 =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);

        oauth2.setDefaultClientRegistrationId("inventory-service");

        return WebClient.builder()
            .filter(oauth2)
            .baseUrl("http://inventory-service")
            .build();
    }
}

@Service
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient webClient;

    public InventoryResponse checkStock(Long productId) {
        return webClient.get()
            .uri("/api/inventory/{id}", productId)
            .retrieve()
            .bodyToMono(InventoryResponse.class)
            .block();
        // Token automatically added by WebClient filter
    }
}
```

---

## Refresh Token Grant

**Matlab:** Access token expire hone pe refresh token se naya access token obtain karna — without user re-login.

### Flow

```
Access token expired (after 1 hour)
    ↓
Client → Auth Server (with refresh_token)

    POST /oauth/token
      grant_type=refresh_token
      refresh_token=dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4...
      client_id=myApp
      scope=openid profile email  (can be narrower than original)
    ↓
Auth Server → New tokens

    {
      "access_token": "eyJ...",           // New access token
      "token_type": "Bearer",
      "expires_in": 3600,
      "refresh_token": "bmV3IHJlZnJlc2g...", // New refresh token (rotation)
      "scope": "openid profile email"
    }
```

### Spring Security Refresh Token

```java
// OAuth2 Client automatically handles refresh
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .oauth2Login(oauth2 -> oauth2
            .tokenEndpoint(token -> token
                .accessTokenResponseClient(accessTokenResponseClient())
            )
        );

    return http.build();
}

// Manual refresh (if needed)
@Service
public class TokenRefreshService {

    private final OAuth2AuthorizedClientService clientService;
    private final ClientRegistrationRepository clientRegistrationRepo;

    public OAuth2AccessToken refreshToken(String registrationId, String principalName) {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient(
            registrationId, principalName
        );

        if (client.getAccessToken().isExpired()) {
            // Spring auto-refresh karta hai normally
            // But manual refresh if needed
            return refreshToken(client);
        }

        return client.getAccessToken();
    }
}
```

---

## Spring Security OAuth2 Resource Server

**Matlab:** Aapka backend API jo incoming JWT/Opaque tokens validate karta hai aur protected resources serve karta hai.

### JWT Resource Server

```java
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    // JWT se custom authorities extract karo
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            // Roles/authorities JWT claims se nikalo
            List<String> roles = jwt.getClaimAsStringList("roles");

            List<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

            return new JwtAuthenticationToken(
                jwt,
                authorities,
                jwt.getSubject()  // Principal name
            );
        };
    }
}
```

### Custom Claim Converter

```java
@Bean
public Converter<Jwt, AbstractAuthenticationToken> jwtAuthConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // Roles claim
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .forEach(authorities::add);
        }

        // Scope-based authorities
        String scope = jwt.getClaimAsString("scope");
        if (scope != null) {
            Arrays.stream(scope.split(" "))
                .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                .forEach(authorities::add);
        }

        return authorities;
    });

    return converter;
}
```

---

## JWT Decoder

**Matlab:** Resource server incoming JWT token ko decode aur validate karta hai.

### JWK Set URI Approach (Recommended)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # Auth server se public keys auto-fetch hote hain
          issuer-uri: https://accounts.google.com
          # OR manually specify JWK endpoint
          jwk-set-uri: https://auth-server.com/oauth2/jwks
```

### Public Key Approach (RS256)

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          # RSA public key directly
          public-key-location: classpath:public-key.pem
```

### Decoder Validation

```java
@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder
        .withJwkSetUri("https://auth-server.com/oauth2/jwks")
        .jwsAlgorithm(RS256)
        .build();
}

// Custom validation
@Bean
public JwtDecoder jwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder
        .withIssuerLocation("https://auth-server.com")
        .build();

    // Additional validation
    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(
        "https://auth-server.com"
    );

    OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(
        withIssuer,
        new JwtTimestampValidator(),
        jwt -> {
            // Custom: audience check
            List<String> aud = jwt.getAudience();
            if (!aud.contains("my-api")) {
                return OAuth2TokenValidatorResult.error(
                    new OAuth2Error("invalid_token", "Wrong audience", null)
                );
            }
            return OAuth2TokenValidatorResult.success();
        }
    );

    decoder.setJwtValidator(withAudience);
    return decoder;
}
```

---

## Opaque Token Introspection

**Matlab:** Jab access token JWT nahi hai (opaque string) toh resource server ko auth server se token validate karwana padta hai.

### Opaque Token vs JWT

```
JWT (self-contained):
    eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJzYWNoaW4ifQ.abc123
    → Resource server locallyally decode + verify kar sakta hai
    → Auth server se baat karne ki zarurat nahi

Opaque Token:
    4b079673-9f8e-4c8a-a1d2-3e4f5a6b7c8d
    → Resource server ko auth server se introspect karna padta hai
    → Extra network call (slower but more control)
```

### Introspection Setup

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### Configuration

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        opaque-token:
          # Auth server introspection endpoint
          introspection-uri: https://auth-server.com/oauth2/introspect
          # Client credentials for introspection
          client-id: resource-server
          client-secret: resource-secret
```

### Java Config

```java
@Configuration
@EnableWebSecurity
public class OpaqueTokenConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .opaqueToken(opaque -> opaque
                    .introspectionUri("https://auth-server.com/oauth2/introspect")
                    .introspectionClientCredentials("resource-server", "resource-secret")
                )
            );

        return http.build();
    }
}
```

### Introspection Request

```
POST /oauth2/introspect
  Content-Type: application/x-www-form-urlencoded
  Authorization: Basic base64(client_id:client_secret)

  token=4b079673-9f8e-4c8a-a1d2-3e4f5a6b7c8d

Response:
{
  "active": true,
  "sub": "sachin",
  "scope": "read write",
  "client_id": "myApp",
  "username": "sachin",
  "exp": 1712345678,
  "roles": ["USER", "ADMIN"]
}
```

### When to Use Opaque Tokens

| Scenario | Use Opaque? |
|----------|-------------|
| Auth server needs revocation control | ✅ Yes |
| Don't want token details exposed | ✅ Yes |
| Microservices (performance matters) | ❌ JWT better |
| External clients (mobile, SPAs) | ❌ JWT better |
| Banking/gov (strict token control) | ✅ Yes |

---

## OpenID Connect (OIDC)

**Matlab:** OAuth2 ke upar ek identity layer. User ki identity verify karne ka standard way.

### OAuth2 vs OIDC

```
OAuth2: "User ne access diya hai" (authorization)
OIDC:   "Yeh user actually wohi hai jo claim kar raha hai" (authentication)

OAuth2 gives: Access Token (for API access)
OIDC gives:   ID Token (for identity proof) + Access Token
```

### ID Token

```json
{
  "iss": "https://accounts.google.com",       // Issuer
  "sub": "109283748573",                       // Subject (unique user ID)
  "aud": "myAppClientId",                      // Audience (your app)
  "exp": 1712345678,                           // Expiration
  "iat": 1712342078,                           // Issued at
  "name": "Sachin Kumar",                      // User's name
  "email": "sachin@example.com",              // User's email
  "picture": "https://...",                    // Profile picture
  "email_verified": true                       // Email verified?
}
```

### OIDC Scopes

| Scope | Claims Returned |
|-------|----------------|
| **openid** | Required for OIDC (gives ID token) |
| **profile** | name, family_name, picture, etc. |
| **email** | email, email_verified |
| **address** | address |
| **phone** | phone_number, phone_number_verified |

### Spring Security OIDC Login

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-client-id
            client-secret: your-client-secret
            scope: openid, profile, email  # openid scope zaruri hai OIDC ke liye
```

```java
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .oauth2Login(Customizer.withDefaults())  // OIDC login enable
        .logout(logout -> logout.logoutSuccessUrl("/"));

    return http.build();
}
```

### Accessing OIDC User Info

```java
@RestController
public class OidcController {

    @GetMapping("/oidc-user")
    public Map<String, Object> getUser(@AuthenticationPrincipal OidcUser oidcUser) {
        return Map.of(
            "id", oidcUser.getSubject(),
            "name", oidcUser.getName(),
            "email", oidcUser.getEmail(),
            "picture", oidcUser.getPicture(),
            "emailVerified", oidcUser.getEmailVerified(),
            "issuer", oidcUser.getIssuer().toString(),
            "claims", oidcUser.getClaims()
        );
    }
}
```

---

## Complete OAuth2 Setup

### Microservice-to-Microservice Auth

```
┌─────────────────────────────────────────────────────────────┐
│                  Client Credentials Flow                     │
│                     (M2M Authentication)                     │
│                                                             │
│  Order Service                          Payment Service      │
│       │                                      │              │
│       │  1. Get token from Auth Server       │              │
│       ├─────────────────────────────────────►│              │
│       │  POST /oauth/token                   │              │
│       │  grant_type=client_credentials       │              │
│       │  client_id=order-service             │              │
│       │  client_secret=xxx                   │              │
│       │  scope=payment:process               │              │
│       │                                      │              │
│       │  2. Token response                   │              │
│       │◄─────────────────────────────────────│              │
│       │  {access_token: "eyJ..."}            │              │
│       │                                      │              │
│       │  3. Call with Bearer token           │              │
│       ├─────────────────────────────────────►│              │
│       │  POST /api/payments                  │              │
│       │  Authorization: Bearer eyJ...        │              │
│       │                                      │              │
│       │  4. Payment processed                │              │
│       │◄─────────────────────────────────────│              │
└───────┴──────────────────────────────────────┴──────────────┘
```

### application.yml (Auth Server)

```yaml
# Authorization Server (Keycloak, Auth0, or custom)
server:
  port: 9000

spring:
  security:
    oauth2:
      authorizationserver:
        client:
          order-service:
            registration:
              client-id: order-service
              client-secret: "{bcrypt}$2a$10$..."  # Encrypted secret
              authorization-grant-types:
                - client_credentials
              scopes:
                - payment:process
                - inventory:read
```

---

## Quick Reference

```yaml
# OAuth2 Client (login with Google/GitHub)
spring.security.oauth2.client.registration.google.client-id=xxx
spring.security.oauth2.client.registration.google.client-secret=xxx
spring.security.oauth2.client.registration.google.scope=openid,profile,email

# Resource Server (validate JWT tokens)
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://auth-server:9000

# Opaque Token introspection
spring.security.oauth2.resourceserver.opaque-token.introspection-uri=https://auth-server/oauth2/introspect
spring.security.oauth2.resourceserver.opaque-token.client-id=xxx
spring.security.oauth2.resourceserver.opaque-token.client-secret=xxx
```

```java
// OAuth2 Login
http.oauth2Login(Customizer.withDefaults());

// Resource Server (JWT)
http.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

// Resource Server (Opaque)
http.oauth2ResourceServer(oauth2 -> oauth2
    .opaqueToken(opaque -> opaque
        .introspectionUri("https://auth-server/oauth2/introspect")
        .introspectionClientCredentials("client-id", "client-secret")
    )
);

// Custom JWT authorities
jwt.jwtAuthenticationConverter(customConverter());
```

---

## Summary

| Component | Use Case |
|-----------|----------|
| **Authorization Code + PKCE** | SPAs, mobile apps (user login) |
| **Client Credentials** | Microservice-to-microservice (no user) |
| **Refresh Token** | Token renewal without re-login |
| **Resource Server (JWT)** | Validate JWT tokens locally |
| **Resource Server (Opaque)** | Validate opaque tokens via introspection |
| **OIDC** | User identity verification (ID Token) |

**Key Rule:** Client Credentials grant for microservice-to-microservice auth — no user involved.
