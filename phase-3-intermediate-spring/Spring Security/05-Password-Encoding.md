# Password Encoding

## Status: Not Started

---

## Table of Contents

1. [PasswordEncoder Overview](#passwordencoder-overview)
2. [BCryptPasswordEncoder (Recommended)](#bcryptpasswordencoder-recommended)
3. [Argon2PasswordEncoder](#argon2passwordencoder)
4. [Pbkdf2PasswordEncoder](#pbkdf2passwordencoder)
5. [DelegatingPasswordEncoder](#delegatingpasswordencoder)
6. [Password Encoder Comparison](#password-encoder-comparison)
7. [Best Practices](#best-practices)

---

## PasswordEncoder Overview

**Matlab:** Passwords ko hash karne ka interface. Plain text passwords ko **encode** karta hai taaki database mein safe store ho.

### Interface

```java
public interface PasswordEncoder {

    // Password encode karo
    String encode(CharSequence rawPassword);

    // Raw password encoded password se match karta hai ya nahi
    boolean matches(CharSequence rawPassword, String encodedPassword);

    // Upgrade needed? (optional)
    default boolean upgradeEncoding(String encodedPassword) {
        return false;
    }
}
```

### Why Encoding?

```
❌ Plain text (database breach = all passwords exposed)
    users: [{email: "sachin@mail.com", password: "mySecret123"}]

✅ Encoded (breach hone pe bhi passwords safe)
    users: [{email: "sachin@mail.com", password: "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG"}]
```

### Golden Rules

| Rule | Description |
|------|-------------|
| **Never store plain text** | Hamesha encode karo |
| **One-way function** | Hash se wapas password nahi nikal sakta |
| **Salt use karo** | Same password, different hash (rainbow table attack se bachata hai) |
| **Adaptive cost** | Strength badhate raho — computers fast hote jaate hain |

---

## BCryptPasswordEncoder (Recommended)

**Matlab:** Most popular aur recommended password encoder. Adaptive cost factor ke saath aata hai.

### How BCrypt Works

```
BCrypt(password, salt, rounds)
    ↓
$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
│  │  │
│  │  └── Cost factor (10 = 2^10 iterations)
│  └───── Salt (22 chars, randomly generated)
└──────── BCrypt version identifier
```

### Basic Usage

```java
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Default strength: 10
    }
}

// Usage in service
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public User registerUser(String email, String rawPassword) {
        // Encode before saving
        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);

        return userRepository.save(user);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
```

### Strength Configuration

```java
// Default strength (10) — production recommended
new BCryptPasswordEncoder();
new BCryptPasswordEncoder(10);

// Higher strength (more secure but slower)
new BCryptPasswordEncoder(12);  // High security apps
new BCryptPasswordEncoder(14);  // Very high security (banking, gov)

// Lower strength (faster but less secure — dev only)
new BCryptPasswordEncoder(4);
```

### Strength vs Time

| Strength | Iterations | Approx Time | Use Case |
|----------|------------|-------------|----------|
| **4** | 16 | ~1ms | Development only |
| **8** | 256 | ~10ms | Testing |
| **10** | 1,024 | ~70ms | ✅ Production standard |
| **12** | 4,096 | ~280ms | High security |
| **14** | 16,384 | ~1.1s | Very high security |
| **16** | 65,536 | ~4.4s | Overkill for most apps |

**Rule of thumb:** Target ~250ms per hash. Strength badhao jab computers faster hote jaayein.

### BCrypt Examples

```java
PasswordEncoder encoder = new BCryptPasswordEncoder(10);

// Same password, different hashes (random salt)
String hash1 = encoder.encode("myPassword123");
// $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG

String hash2 = encoder.encode("myPassword123");
// $2a$10$8K1p/a0dUR1AmkVfT5hFp.3bFkXM/ql5RXNlO7XvJzKQYqFqXGvGm

// Both match same password
encoder.matches("myPassword123", hash1);  // true
encoder.matches("myPassword123", hash2);  // true

// Wrong password
encoder.matches("wrongPassword", hash1);  // false
```

### Password Upgrade

```java
// Check if password needs re-encoding (strength badhaya hai)
PasswordEncoder encoder = new BCryptPasswordEncoder(12);  // Upgraded from 10

public void changePassword(Long userId, String oldPassword, String newPassword) {
    User user = userRepository.findById(userId).orElseThrow();

    // Verify old password
    if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
        throw new BadCredentialsException("Invalid old password");
    }

    // Encode and save new password
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
}

// Check if upgrade needed
if (passwordEncoder.upgradeEncoding(user.getPassword())) {
    // Password was encoded with old strength
    // Re-encode with new strength on next login
    String newPassword = passwordEncoder.encode(rawPassword);
    user.setPassword(newPassword);
    userRepository.save(user);
}
```

---

## Argon2PasswordEncoder

**Matlab:** Modern password encoder, winner of Password Hashing Competition (2015). Memory-hard function — GPU/ASIC attacks ke against strong.

### Setup

```xml
<!-- Bouncy Castle dependency chahiye -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.78</version>
</dependency>
```

### Usage

```java
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Default: parallelism=1, memory=10240 KB, iterations=3
        return new Argon2PasswordEncoder();
    }

    // Custom parameters
    @Bean
    public PasswordEncoder strongPasswordEncoder() {
        return new Argon2PasswordEncoder(
            32,     // hash length (bytes)
            64,     // salt length (bytes)
            4,      // parallelism (threads)
            65536,  // memory (KB) — 64 MB
            3       // iterations
        );
    }
}
```

### Parameters Explained

| Parameter | Default | Description |
|-----------|---------|-------------|
| **hashLength** | 32 | Output hash size (bytes) |
| **saltLength** | 16 | Random salt size (bytes) |
| **parallelism** | 1 | Number of parallel threads |
| **memory** | 10240 | Memory usage in KB (10 MB) |
| **iterations** | 3 | Number of passes |

### When to Use Argon2

| Scenario | Use Argon2? |
|----------|-------------|
| New project, high security | ✅ Yes |
| Government/compliance requirements | ✅ Yes |
| Existing BCrypt system | ❌ Migration overhead |
| Low-memory environment | ❌ BCrypt better |
| Standard web app | ❌ BCrypt is enough |

---

## Pbkdf2PasswordEncoder

**Matlab:** Password-Based Key Derivation Function 2. NIST recommended, FIPS compliant.

### Usage

```java
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Default: salt=16, iterations=185000, hashWidth=256
        return new Pbkdf2PasswordEncoder();
    }

    // Custom parameters
    @Bean
    public PasswordEncoder customPbkdf2Encoder() {
        return new Pbkdf2PasswordEncoder(
            "mySecretSalt",    // Secret key (optional)
            185000,             // Iterations
            32,                 // Hash width (bytes)
            Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256
        );
    }
}
```

### Available Algorithms

```java
// PBKDF2 with different hash functions
PBKDF2WithHmacSHA256   // Standard (recommended)
PBKDF2WithHmacSHA384   // Stronger
PBKDF2WithHmacSHA512   // Strongest
```

### When to Use PBKDF2

| Scenario | Use PBKDF2? |
|----------|-------------|
| FIPS compliance needed | ✅ Yes |
| Government/enterprise | ✅ Yes |
| Standard web app | ❌ BCrypt is simpler |
| GPU attack resistance | ❌ Argon2 better (memory-hard) |

---

## DelegatingPasswordEncoder

**Matlab:** Multiple password encoders ko handle karta hai. `{encoderId}encodedPassword` format use karta hai taaki different encoders ek saath chal sakein.

### Why Needed?

```
Problem: App BCrypt se passwords store karti thi. Ab Argon2 use karna hai.
         Purane passwords BCrypt hain, naye Argon2 honge.
         Dono ko ek saath handle karna hai.

Solution: DelegatingPasswordEncoder — prefix se encoder identify karo
```

### Format

```
{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
{argon2}$argon2id$v=19$m=65536,t=3,p=4$c29tZXNhbHQ$RdescudvJCsgt3ub+b+daw
{pbkdf2}5d9c3b...
{noop}myPlainPassword  // No encoding (dev only!)
```

### Spring Boot Default

```java
// Spring Boot automatically creates DelegatingPasswordEncoder
// Default encoder is BCrypt

@Bean
public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

### Behind the Scenes

```java
public class DelegatingPasswordEncoder implements PasswordEncoder {

    private String idForEncode = "bcrypt";  // Default encoder for encoding
    private Map<String, PasswordEncoder> delegates;  // All encoders

    @Override
    public String encode(CharSequence rawPassword) {
        // Default encoder se encode karo
        return "{" + idForEncode + "}" + delegates.get(idForEncode).encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String prefixEncodedPassword) {
        // Prefix se encoder id nikalo
        String id = extractId(prefixEncodedPassword);

        // Usi encoder se match karo
        PasswordEncoder delegate = delegates.get(id);
        return delegate.matches(rawPassword, prefixEncodedPassword);
    }

    private String extractId(String prefixEncodedPassword) {
        // "{bcrypt}$2a$10$..." → "bcrypt"
        int start = prefixEncodedPassword.indexOf("{");
        int end = prefixEncodedPassword.indexOf("}", start);
        if (start >= 0 && end >= 0) {
            return prefixEncodedPassword.substring(start + 1, end);
        }
        return null;
    }
}
```

### Custom DelegatingPasswordEncoder

```java
@Bean
public PasswordEncoder delegatingPasswordEncoder() {
    // Encoder map banao
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder(12));
    encoders.put("argon2", new Argon2PasswordEncoder());
    encoders.put("pbkdf2", new Pbkdf2PasswordEncoder());
    encoders.put("noop", NoOpPasswordEncoder.getInstance());  // Dev only!

    DelegatingPasswordEncoder delegatingEncoder =
        new DelegatingPasswordEncoder("bcrypt", encoders);  // Default: bcrypt

    // Set fallback for legacy passwords without prefix
    delegatingEncoder.setDefaultPasswordEncoderForMatches(
        new BCryptPasswordEncoder(10)  // No prefix → assume bcrypt
    );

    return delegatingEncoder;
}
```

### Migration Example

```java
// Scenario: BCrypt (strength 10) → Argon2 migrate karna hai

@Bean
public PasswordEncoder passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("bcrypt", new BCryptPasswordEncoder(10));
    encoders.put("argon2", new Argon2PasswordEncoder());

    DelegatingPasswordEncoder delegating =
        new DelegatingPasswordEncoder("argon2", encoders);  // New passwords: Argon2

    return delegating;
}

// Login pe automatically upgrade
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;

    public void onSuccessfulLogin(User user, String rawPassword) {
        // Check if password needs re-encoding (different encoder use ho raha hai)
        if (passwordEncoder.upgradeEncoding(user.getPassword())) {
            // Re-encode with new encoder (Argon2)
            user.setPassword(passwordEncoder.encode(rawPassword));
            userRepository.save(user);
        }
    }
}
```

---

## Password Encoder Comparison

| Feature | BCrypt | Argon2 | PBKDF2 | NoOp |
|---------|--------|--------|--------|------|
| **Security** | Good ✅ | Excellent 🏆 | Good ✅ | None ❌ |
| **Speed** | Medium (~70ms) | Slower (~280ms) | Medium (~100ms) | Instant |
| **Memory-hard** | No | Yes ✅ | No | No |
| **GPU resistant** | Partial | Yes ✅ | Partial | No |
| **Dependencies** | None | Bouncy Castle | None | None |
| **Standard** | Widely used | PHC winner | NIST/FIPS | Dev only |
| **Recommended** | ✅ Production | High security | Enterprise | ❌ Never prod |

### When to Use What

| Scenario | Encoder |
|----------|---------|
| Standard web app | **BCrypt (strength 10-12)** |
| High security (banking, gov) | **Argon2** |
| FIPS compliance needed | **PBKDF2** |
| Migration between encoders | **DelegatingPasswordEncoder** |
| Development/testing | **{noop}** (never production) |

---

## Best Practices

### 1. Never Store Plain Text

```java
// ❌ NEVER DO THIS
user.setPassword("mySecretPassword");  // Plain text in database

// ✅ Always encode
user.setPassword(passwordEncoder.encode("mySecretPassword"));
```

### 2. Use Appropriate Strength

```java
// Development — fast
new BCryptPasswordEncoder(4);

// Production — balanced
new BCryptPasswordEncoder(10);  // Default, recommended

// High security — slower but safer
new BCryptPasswordEncoder(12);

// Don't go too high (performance impact)
new BCryptPasswordEncoder(16);  // 4+ seconds per hash — overkill
```

### 3. Password Validation on Input

```java
@Service
public class UserService {

    // Password strength validate before encoding
    public void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain uppercase");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain lowercase");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("Password must contain a digit");
        }
        if (!password.matches(".*[!@#$%^&*].*")) {
            throw new IllegalArgumentException("Password must contain a special character");
        }
    }

    public User register(String email, String password) {
        validatePassword(password);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }
}
```

### 4. Check Password Breach (Optional)

```java
// Have I Been Pwned API se check karo
@Service
public class PasswordBreachChecker {

    private final RestTemplate restTemplate;

    public boolean isPasswordBreached(String password) {
        try {
            // SHA-1 hash ka first 5 chars
            String sha1 = DigestUtils.sha1Hex(password).toUpperCase();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            // API call
            String response = restTemplate.getForObject(
                "https://api.pwnedpasswords.com/range/" + prefix,
                String.class
            );

            // Check if suffix exists in response
            return response != null && response.contains(suffix);
        } catch (Exception e) {
            return false;  // API error — assume not breached
        }
    }
}
```

### 5. Change Password Flow

```java
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Validate old password
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Validate new password strength
        validatePassword(newPassword);

        // Check new password is not same as old
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different");
        }

        // Encode and save
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // Forgot password — reset with token
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        User user = userRepository.findByResetToken(resetToken)
            .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        // Check token expiry
        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token expired");
        }

        // Validate and set new password
        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}
```

---

## Quick Reference

```java
// BCrypt (recommended for most apps)
PasswordEncoder encoder = new BCryptPasswordEncoder(10);
String hash = encoder.encode("password123");
boolean matches = encoder.matches("password123", hash);

// Delegating (multiple encoders support)
PasswordEncoder delegating = PasswordEncoderFactories.createDelegatingPasswordEncoder();
// "{bcrypt}$2a$10$..." or "{argon2}$argon2id$..."

// Prefix format
{bcrypt}$2a$10$...      // BCrypt encoded
{argon2}$argon2id$...   // Argon2 encoded
{pbkdf2}5d9c3b...       // PBKDF2 encoded
{noop}plaintext          // No encoding (dev only!)

// Best practice
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);  // Strength 10 for production
}
```

---

## Summary

| Encoder | Use When | Strength |
|---------|----------|----------|
| **BCryptPasswordEncoder** | Standard web apps (90% cases) | 10-12 |
| **Argon2PasswordEncoder** | High security, modern apps | Memory-hard |
| **Pbkdf2PasswordEncoder** | FIPS compliance needed | NIST standard |
| **DelegatingPasswordEncoder** | Migration between encoders | Multiple |
| **NoOpPasswordEncoder** | Development only | None ❌ |

**Golden Rule:** BCrypt with strength 10-12 is the default choice. Upgrade to Argon2 only if compliance or high security requirements.
