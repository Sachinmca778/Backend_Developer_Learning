# Authentication

## Status: Not Started

---

## Table of Contents

1. [Authentication Overview](#authentication-overview)
2. [AuthenticationManager](#authenticationmanager)
3. [AuthenticationProvider](#authenticationprovider)
4. [UserDetailsService](#userdetailsservice)
5. [UserDetails](#userdetails)
6. [DaoAuthenticationProvider](#daoauthenticationprovider)
7. [In-Memory Authentication](#in-memory-authentication)
8. [JDBC Authentication](#jdbc-authentication)
9. [Custom Authentication](#custom-authentication)
10. [Complete Authentication Flow](#complete-authentication-flow)

---

## Authentication Overview

**Authentication ka matlab:** Verify karna ki user wohi hai jo claim kar raha hai (username/password check karna).

### Authentication Components Flow

```
User sends credentials
    ↓
Authentication Filter intercepts
    ↓
Creates AuthenticationToken (unauthenticated)
    ↓
AuthenticationManager.authenticate(token)
    ↓
AuthenticationProvider.validate()
    ↓
UserDetailsService.loadUserByUsername()
    ↓
Returns UserDetails
    ↓
Password match check
    ↓
Returns authenticated AuthenticationToken
    ↓
SecurityContextHolder.setAuthentication()
    ↓
User is authenticated
```

### Key Interfaces & Classes

| Component | Role |
|-----------|------|
| **AuthenticationManager** | Orchestrator - authentication start karta hai |
| **AuthenticationProvider** | Validator - credentials verify karta hai |
| **UserDetailsService** | User fetcher - database se user data lata hai |
| **UserDetails** | User contract - Spring Security ke liye user object |
| **DaoAuthenticationProvider** | Built-in provider - UserDetailsService + password encoder |

---

## AuthenticationManager

**Matlab:** Interface jo authentication process ko **orchestrate** karta hai. Yeh ek facade hai - actual kaam AuthenticationProviders ko delegate karta hai.

### Interface

```java
public interface AuthenticationManager {

    // Authenticate karo aur authenticated token return karo
    Authentication authenticate(Authentication authentication)
        throws AuthenticationException;
}
```

### ProviderManager (Default Implementation)

```java
public class ProviderManager implements AuthenticationManager {

    private List<AuthenticationProvider> providers;

    @Override
    public Authentication authenticate(Authentication authentication) {
        // Har provider ko try karo
        for (AuthenticationProvider provider : providers) {
            // Kya yeh provider is type ka authentication support karta hai?
            if (provider.supports(authentication.getClass())) {
                try {
                    // Provider se authenticate karwao
                    Authentication result = provider.authenticate(authentication);
                    if (result != null) {
                        return result;  // Success!
                    }
                } catch (AuthenticationException e) {
                    lastException = e;
                    // Next provider try karo
                }
            }
        }
        throw new ProviderNotFoundException("No provider found");
    }
}
```

### Usage

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        // Spring Boot automatically configures providers
        return authConfig.getAuthenticationManager();
    }

    // Manual usage in service
    @Service
    public class LoginService {

        private final AuthenticationManager authenticationManager;

        public LoginService(AuthenticationManager authenticationManager) {
            this.authenticationManager = authenticationManager;
        }

        public String login(String username, String password) {
            try {
                // Authentication token banao (unauthenticated)
                Authentication token = new UsernamePasswordAuthenticationToken(
                    username, password
                );

                // Authenticate karo
                Authentication authenticated = authenticationManager.authenticate(token);

                // SecurityContext mein set karo
                SecurityContextHolder.getContext().setAuthentication(authenticated);

                return "Login successful for: " + authenticated.getName();

            } catch (BadCredentialsException e) {
                return "Invalid username or password";
            } catch (DisabledException e) {
                return "Account is disabled";
            } catch (LockedException e) {
                return "Account is locked";
            }
        }
    }
}
```

### Authentication Exceptions

| Exception | When |
|-----------|------|
| **BadCredentialsException** | Wrong username/password |
| **UsernameNotFoundException** | User exists nahi karta |
| **DisabledException** | Account disabled hai |
| **LockedException** | Account locked hai |
| **AccountExpiredException** | Account expire ho gaya |
| **CredentialsExpiredException** | Password expire ho gaya |
| **ProviderNotFoundException** | Koi suitable provider nahi mila |

---

## AuthenticationProvider

**Matlab:** Interface jo actual authentication logic implement karta hai. Har provider ek specific authentication type handle karta hai.

### Interface

```java
public interface AuthenticationProvider {

    // Authenticate karo
    Authentication authenticate(Authentication authentication)
        throws AuthenticationException;

    // Kya yeh provider is type ka authentication support karta hai?
    boolean supports(Class<?> authentication);
}
```

### Common Providers

| Provider | Handles |
|----------|---------|
| **DaoAuthenticationProvider** | Username + Password (database) |
| **JwtAuthenticationProvider** | JWT token validation |
| **OAuth2LoginAuthenticationProvider** | OAuth2/OIDC login |
| **RememberMeAuthenticationProvider** | Remember-me cookie |
| **AnonymousAuthenticationProvider** | Anonymous access |

### Custom AuthenticationProvider

```java
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        // Database se user fetch karo
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Password verify karo
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        // Check if account is active
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled");
        }

        // Authorities build karo
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        // Authenticated token return karo (credentials = null)
        return new UsernamePasswordAuthenticationToken(
            user,           // Principal (full user object)
            null,           // Credentials (cleared after auth)
            authorities     // Roles
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        // Sirf username+password type authentication support karo
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

// Provider register karna
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomAuthenticationProvider customProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(customProvider)
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

### Multiple Providers

```java
@Configuration
@EnableWebSecurity
public class MultiProviderConfig {

    @Bean
    public DaoAuthenticationProvider daoProvider(UserDetailsService uds, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(uds);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    public JwtAuthenticationProvider jwtProvider() {
        return new JwtAuthenticationProvider(jwtDecoder());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(daoProvider(userDetailsService(), passwordEncoder()))
            .authenticationProvider(jwtProvider())
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());

        return http.build();
    }
}
```

**Note:** Providers ek list mein hote hain. Pehla matching provider try hoga, fail hua toh next.

---

## UserDetailsService

**Matlab:** Interface jo user data fetch karne ka contract define karta hai. Sirf **ek method** hai.

### Interface

```java
public interface UserDetailsService {

    // Username se user fetch karo
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
}
```

### Built-in Implementation: InMemoryUserDetailsManager

```java
@Bean
public UserDetailsService userDetailsService() {
    UserDetails user = User.builder()
        .username("sachin")
        .password(passwordEncoder().encode("password123"))
        .roles("USER")
        .build();

    UserDetails admin = User.builder()
        .username("admin")
        .password(passwordEncoder().encode("admin123"))
        .roles("ADMIN", "USER")
        .build();

    return new InMemoryUserDetailsManager(user, admin);
}
```

### Database Implementation

```java
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with username: " + username
            ));

        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),
            user.isEnabled(),
            true,   // accountNonExpired
            true,   // credentialsNonExpired
            true,   // accountNonLocked
            user.getAuthorities()
        );
    }
}
```

### Custom UserDetailsService with Extra Details

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsernameWithRoles(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Database se roles fetch karo
        List<GrantedAuthority> authorities = roleRepository.findByUserId(user.getId())
            .stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(authorities)
            .disabled(!user.isActive())
            .build();
    }
}
```

### Key Points

| Point | Description |
|-------|-------------|
| Sirf ek method | `loadUserByUsername(String)` - simple contract |
| UsernameNotFoundException | User na mile toh yeh exception throw karna zaruri hai |
| Return type hamesha UserDetails | Raw User entity return nahi kar sakte |
| Password encoding | UserDetailsService mein password encode nahi karna, already encoded hona chahiye |
| Eager loading | Roles/authorities eagerly load karna padta hai (lazy initialization issues) |

---

## UserDetails

**Matlab:** Interface jo Spring Security ke liye **user contract** define karta hai. Aapka user entity isko implement karega.

### Interface

```java
public interface UserDetails extends Serializable {

    // User ki roles/permissions
    Collection<? extends GrantedAuthority> getAuthorities();

    // Password (encoded form)
    String getPassword();

    // Username (unique identifier)
    String getUsername();

    // Account expire toh nahi hua?
    boolean isAccountNonExpired();

    // Account lock toh nahi hua?
    boolean isAccountNonLocked();

    // Password expire toh nahi hua?
    boolean isCredentialsNonExpired();

    // Account enabled hai?
    boolean isEnabled();
}
```

### Implementation Option 1: Implement UserDetails in Entity

```java
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private boolean enabled = true;
    private boolean accountNonExpired = true;
    private boolean accountNonLocked = true;
    private boolean credentialsNonExpired = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
```

### Implementation Option 2: Separate UserDetails (Recommended)

```java
// User Entity (sirf database mapping)
@Entity
public class User {
    private Long id;
    private String username;
    private String password;
    private boolean enabled;
    // ... roles, etc.

    // UserDetails nahi implement karta
}

// Separate UserDetails wrapper
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user, List<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    // Factory method
    public static CustomUserDetails fromUser(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        return new CustomUserDetails(user, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    // Original user entity access if needed
    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }
}
```

### Using User.builder() (Spring's built-in)

```java
// Built-in Spring User class use karo (entity nahi)
UserDetails userDetails = User.builder()
    .username("sachin")
    .password("$2a$10$encodedPasswordHere")  // Already encoded
    .roles("USER")                              // Automatically adds ROLE_ prefix
    .authorities("READ_PERMISSION", "WRITE_PERMISSION")  // Without ROLE_ prefix
    .disabled(false)
    .accountExpired(false)
    .accountLocked(false)
    .credentialsExpired(false)
    .build();

// Result:
// authorities = [ROLE_USER, READ_PERMISSION, WRITE_PERMISSION]
```

### UserDetails in Controller

```java
@RestController
public class UserController {

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
            "id", userDetails.getId(),
            "username", userDetails.getUsername(),
            "roles", userDetails.getAuthorities()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return ResponseEntity.ok(Map.of(
            "id", userDetails.getId(),
            "email", userDetails.getUser().getEmail()
        ));
    }
}
```

---

## DaoAuthenticationProvider

**Matlab:** Spring Security ka built-in provider jo **UserDetailsService + PasswordEncoder** combine karta hai. Yeh most commonly used provider hai.

### How it Works

```
DaoAuthenticationProvider.authenticate(token)
    ↓
1. UserDetailsService.loadUserByUsername(username)
    ↓
2. Returns UserDetails
    ↓
3. PasswordEncoder.matches(rawPassword, encodedPassword)
    ↓
4. Check account status (enabled, locked, expired)
    ↓
5. Returns authenticated AuthenticationToken
```

### Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);

        // Optional: Hide user not found exception (security best practice)
        provider.setHideUserNotFoundExceptions(true);  // Default: true

        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authenticationProvider(userDetailsService(), passwordEncoder()))
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
```

### PasswordEncoder Types

```java
@Configuration
public class PasswordConfig {

    // BCrypt (Recommended)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Other options (rarely used)
    // new Pbkdf2PasswordEncoder()     // Slower but more secure
    // new SCryptPasswordEncoder()    // Memory-hard function
    // new Argon2PasswordEncoder()    // Modern, recommended for new apps
}
```

### BCrypt Examples

```java
PasswordEncoder encoder = new BCryptPasswordEncoder();

// Encode password
String encoded = encoder.encode("myPassword123");
// Output: $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

// Match password
boolean matches = encoder.matches("myPassword123", encoded);  // true
boolean wrong = encoder.matches("wrongPassword", encoded);    // false
```

### BCrypt Strength

```java
// Default strength: 10
new BCryptPasswordEncoder();

// Custom strength (4-31, higher = slower but more secure)
new BCryptPasswordEncoder(12);  // Production recommended
new BCryptPasswordEncoder(16);  // High security (slower)
```

---

## In-Memory Authentication

**Matlab:** Users directly code/configuration mein define hote hain. Database nahi chahiye. Testing aur development ke liye use hota hai.

### Option 1: Using UserDetailsService Bean

```java
@Configuration
@EnableWebSecurity
public class InMemorySecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("user123"))
            .roles("USER")
            .build();

        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin123"))
            .roles("ADMIN", "USER")
            .build();

        return new InMemoryUserDetailsManager(user, admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
```

### Option 2: Using User.withDefaultPasswordEncoder() (Dev Only!)

```java
// ⚠️ WARNING: Only for development, not for production!
@Bean
public UserDetailsService userDetailsService() {
    UserDetails user = User.withDefaultPasswordEncoder()
        .username("dev")
        .password("dev123")
        .roles("USER")
        .build();

    return new InMemoryUserDetailsManager(user);
}
```

### Option 3: Using application.properties

```properties
# application.properties
spring.security.user.name=user
spring.security.user.password=user123
spring.security.user.roles=USER
```

### Option 4: Multiple Users via Properties

```java
@Configuration
public class MultipleUsersConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user1 = User.builder()
            .username("sachin")
            .password(passwordEncoder().encode("pass1"))
            .roles("USER")
            .build();

        UserDetails user2 = User.builder()
            .username("john")
            .password(passwordEncoder().encode("pass2"))
            .roles("USER", "MANAGER")
            .build();

        UserDetails user3 = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(user1, user2, user3);
    }
}
```

### When to Use In-Memory Auth

| Scenario | Use? |
|----------|------|
| Development/Testing | ✅ Yes |
| Quick prototype | ✅ Yes |
| Microservice internal auth | ✅ Sometimes |
| Production app with users | ❌ Use JDBC/JPA |
| Dynamic user management | ❌ Need database |

---

## JDBC Authentication

**Matlab:** Spring Security ka built-in JDBC support jo directly database queries run karta hai. JPA se lighter alternative hai.

### Schema Setup

```sql
-- Users table
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- Authorities table (Spring Security default schema)
CREATE TABLE authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    FOREIGN KEY (username) REFERENCES users(username),
    UNIQUE (username, authority)
);

-- Insert sample data
INSERT INTO users (username, password, enabled) VALUES
('sachin', '$2a$10$encoded_password_here', TRUE);

INSERT INTO authorities (username, authority) VALUES
('sachin', 'ROLE_USER');
```

### Configuration

```java
@Configuration
@EnableWebSecurity
public class JdbcSecurityConfig {

    @Autowired
    private DataSource dataSource;

    // JDBC UserDetailsService
    @Bean
    public UserDetailsService userDetailsService() {
        JdbcUserDetailsManager users = new JdbcUserDetailsManager(dataSource);

        // Default queries:
        // SELECT username, password, enabled FROM users WHERE username = ?
        // SELECT username, authority FROM authorities WHERE username = ?

        return users;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/public/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .jdbcAuthentication(auth -> auth
                .dataSource(dataSource)
            );

        return http.build();
    }
}
```

### Custom Queries

```java
@Bean
public UserDetailsService userDetailsService(DataSource dataSource) {
    JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);

    // Custom queries (agar table schema different hai)
    manager.setUsersByUsernameQuery(
        "SELECT email, password, is_active FROM app_users WHERE email = ?"
    );

    manager.setAuthoritiesByUsernameQuery(
        "SELECT u.email, r.name FROM app_users u " +
        "JOIN user_roles ur ON u.id = ur.user_id " +
        "JOIN roles r ON ur.role_id = r.id " +
        "WHERE u.email = ?"
    );

    return manager;
}
```

### Creating Users via JDBC

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final JdbcUserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public void createUser(String username, String password, String role) {
        UserDetails user = User.builder()
            .username(username)
            .password(passwordEncoder.encode(password))
            .roles(role)
            .build();

        userDetailsManager.createUser(user);
    }

    public void deleteUser(String username) {
        userDetailsManager.deleteUser(username);
    }

    public void changePassword(String oldPassword, String newPassword) {
        userDetailsManager.changePassword(oldPassword, newPassword);
    }

    public boolean userExists(String username) {
        return userDetailsManager.userExists(username);
    }
}
```

### application.properties

```properties
# Database connection
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=root

# JdbcTemplate logging (for debugging)
logging.level.org.springframework.jdbc.core=DEBUG
```

### JDBC vs JPA Authentication

| Feature | JDBC Auth | JPA Auth |
|---------|-----------|----------|
| Complexity | Low | Medium |
| Control | Full SQL control | ORM benefits |
| Custom logic | Manual | Entity lifecycle |
| Performance | Faster (direct SQL) | Slight overhead |
| Best for | Simple apps | Complex domain models |

---

## Custom Authentication

**Matlab:** Apna khud ka authentication flow banana — custom UserDetailsService, custom filter, custom provider.

### Complete Custom Auth Setup

#### Step 1: Entity Classes

```java
@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String fullName;

    private boolean enabled = true;

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
}

@Entity
@Table(name = "roles")
@Data
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;  // USER, ADMIN, MANAGER
}
```

#### Step 2: Repository

```java
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);
}

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
```

#### Step 3: Custom UserDetails

```java
public class CustomUserDetails implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public CustomUserDetails(User user, List<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public static CustomUserDetails fromUser(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
            .collect(Collectors.toList());

        return new CustomUserDetails(user, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    // Extra helpers
    public Long getId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFullName();
    }

    public User getUser() {
        return user;
    }
}
```

#### Step 4: Custom UserDetailsService

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRoles(email)
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with email: " + email
            ));

        return CustomUserDetails.fromUser(user);
    }
}
```

#### Step 5: Custom AuthenticationFilter

```java
public class CustomAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;

    public CustomAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        setFilterProcessesUrl("/api/auth/login");  // Custom login URL
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request,
                                                 HttpServletResponse response) {
        try {
            // JSON body se credentials extract karo
            LoginRequest loginRequest = new ObjectMapper()
                .readValue(request.getInputStream(), LoginRequest.class);

            UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                );

            return authenticationManager.authenticate(token);

        } catch (IOException e) {
            throw new RuntimeException("Failed to parse login request", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain,
                                            Authentication authResult) {
        // Login success - JWT token generate karo ya response bhejo
        CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();

        response.setStatus(HttpStatus.OK.value());
        response.setContentType("application/json");

        try {
            response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                "message", "Login successful",
                "user", userDetails.getFullName(),
                "roles", userDetails.getAuthorities()
            )));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request,
                                               HttpServletResponse response,
                                               AuthenticationException failed) {
        // Login failed
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");

        try {
            response.getWriter().write(new ObjectMapper().writeValueAsString(Map.of(
                "error", "Authentication failed",
                "message", failed.getMessage()
            )));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

// Login request DTO
@Data
public class LoginRequest {
    private String email;
    private String password;
}
```

#### Step 6: Security Configuration

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()    // Login endpoints public
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/user/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

#### Step 7: Registration Endpoint

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // Check if user exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Email already registered"));
        }

        // Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setEnabled(true);

        // Default role: USER
        Role userRole = roleRepository.findByName("USER")
            .orElseGet(() -> {
                Role newRole = new Role();
                newRole.setName("USER");
                return roleRepository.save(newRole);
            });

        user.setRoles(Set.of(userRole));

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "message", "User registered successfully",
            "email", user.getEmail()
        ));
    }
}

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String fullName;
}
```

---

## Complete Authentication Flow

### End-to-End Login Flow

```
┌──────────────────────────────────────────────────────────────┐
│                     User submits login                       │
│              POST /login {username, password}                │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│          UsernamePasswordAuthenticationFilter                 │
│  1. Extract username & password from request                  │
│  2. Create UsernamePasswordAuthenticationToken (unauthenticated)│
│  3. Call authenticationManager.authenticate(token)            │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│                  ProviderManager                              │
│  (AuthenticationManager implementation)                       │
│  Iterate through providers to find matching one               │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│             DaoAuthenticationProvider                         │
│  1. Check if supports(token) - yes for username+password      │
│  2. Call userDetailsService.loadUserByUsername(username)      │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│             CustomUserDetailsService                          │
│  1. Query database: SELECT * FROM users WHERE email = ?       │
│  2. Load roles: SELECT * FROM roles JOIN user_roles           │
│  3. Create CustomUserDetails object                           │
│  4. Return UserDetails                                        │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│              DaoAuthenticationProvider (contd)                │
│  1. passwordEncoder.matches(rawPassword, userDetails.getPassword())│
│  2. If match → check account status (enabled, locked, expired) │
│  3. Create authenticated UsernamePasswordAuthenticationToken  │
│  4. Return authenticated token                               │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│          UsernamePasswordAuthenticationFilter (contd)         │
│  1. successfulAuthentication() called                         │
│  2. SecurityContextHolder.getContext().setAuthentication(auth)│
│  3. Remember-me services (if configured)                      │
│  4. SuccessHandler - redirect or JSON response                │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│          SecurityContextPersistenceFilter                     │
│  Save SecurityContext to HttpSession                          │
└──────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────────────────────────────────────────────────────┐
│                   Response to User                            │
│            Redirect to /dashboard or JSON response            │
└──────────────────────────────────────────────────────────────┘
```

### Component Relationship

```
AuthenticationManager (interface)
    │
    └── ProviderManager (default implementation)
            │
            ├── AuthenticationProvider (interface)
            │       │
            │       ├── DaoAuthenticationProvider ← Most common
            │       │       ├── UserDetailsService.loadUserByUsername()
            │       │       │       └── Returns UserDetails
            │       │       └── PasswordEncoder.matches()
            │       │
            │       ├── JwtAuthenticationProvider
            │       ├── OAuth2LoginAuthenticationProvider
            │       └── RememberMeAuthenticationProvider
            │
            └── More providers...
```

---

## Quick Reference

```java
// 1. AuthenticationManager usage
Authentication auth = authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(username, password)
);
SecurityContextHolder.getContext().setAuthentication(auth);

// 2. Custom UserDetailsService
@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Not found"));
        return CustomUserDetails.fromUser(user);
    }
}

// 3. UserDetails implementation
public class CustomUserDetails implements UserDetails {
    private final User user;
    // Implement all methods
}

// 4. DaoAuthenticationProvider setup
@Bean
public DaoAuthenticationProvider authProvider(UserDetailsService uds, PasswordEncoder pe) {
    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
    p.setUserDetailsService(uds);
    p.setPasswordEncoder(pe);
    return p;
}

// 5. In-Memory auth (dev only)
@Bean
public UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager(
        User.builder().username("user").password(encoder.encode("pass")).roles("USER").build()
    );
}

// 6. JDBC auth
JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
```

---

## Summary

| Component | Purpose | Key Method |
|-----------|---------|------------|
| **AuthenticationManager** | Orchestrates authentication | `authenticate(Authentication)` |
| **AuthenticationProvider** | Validates credentials | `authenticate()`, `supports()` |
| **UserDetailsService** | Fetches user from data source | `loadUserByUsername(String)` |
| **UserDetails** | User contract for Spring Security | `getUsername()`, `getAuthorities()` |
| **DaoAuthenticationProvider** | Built-in provider (UserDetailsService + PasswordEncoder) | Pre-built |
| **In-Memory Auth** | Hardcoded users for dev/testing | `InMemoryUserDetailsManager` |
| **JDBC Auth** | Direct DB queries without JPA | `JdbcUserDetailsManager` |
| **Custom Auth** | Full control over authentication | Custom filter + provider |
