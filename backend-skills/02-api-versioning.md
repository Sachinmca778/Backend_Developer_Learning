# API Versioning

**Date learned:** 04 April 2025  
**Phase:** Backend Skills  
**Status:** ✅ Done / 🔄 In Progress / ⏳ Not Started  
**Laravel equivalent:** Routes with prefix (`/api/v1`) — same concept

---

## Why Versioning?

When API changes break existing clients, versioning lets old 
and new versions coexist.

Socho tumhara ek API hai jo 10,000 users use kar rahe hain:
GET /api/users → { "name": "Rahul", "phone": "9999999999" }
Ab tumhe phone field hataani hai aur mobile rakhna hai. Agar seedha change kar do toh sab clients toot jaayenge. Isliye versioning hoti hai — purana version chalta rahe, naya version bhi available ho.

**Breaking changes** → need new version:
- Removing a field (`phone` deleted)
- Renaming a field (`phone` → `mobile`)
- Changing data type (String → Integer)

**Non-breaking changes** → no new version needed:
- Adding optional new field
- Bug fixes
- Performance improvements

---

## 3 Approaches

### 1. URI Versioning ✅ (Recommended)   // Version URL mein hi daal do.
```
GET /api/v1/users
GET /api/v2/users
```
**Pros:** Easy to test in browser, cacheable, obvious  
**Cons:** Longer URLs  
**Use when:** Public APIs

### 2. Header Versioning
```
Accept: application/vnd.myapp.v2+json
```
**Pros:** Clean URLs  
**Cons:** Can't test directly in browser  
**Use when:** Internal/private APIs

### 3. Query Param Versioning
```
GET /api/users/1?version=1    ← V1
GET /api/users/1?version=2    ← V2
```
**Pros:** Simple  
**Cons:** Caching issues  
**Use when:** Quick projects (not recommended for production)

---

## Code — Spring Boot Implementation

### URI Versioning (V1 Controller)
```java
@RestController
@RequestMapping("/api/v1/users")
public class UserControllerV1 {

    @GetMapping("/{id}")
    public UserResponseV1 getUser(@PathVariable Long id) {
        return new UserResponseV1("Rahul", "9999999999");
    }
}
```

### URI Versioning (V2 Controller)
```java
@RestController
@RequestMapping("/api/v2/users")
public class UserControllerV2 {

    @GetMapping("/{id}")
    public UserResponseV2 getUser(@PathVariable Long id) {
        return new UserResponseV2("Rahul", "9999999999");
    }
}
```

DTO classes:
```java
// V1 DTO — old
public class UserResponseV1 {
    private String name;
    private String phone;    // old field name
}

// V2 DTO — new
public class UserResponseV2 {
    private String name;
    private String mobile;   // new field name
}
```

### Header Versioning
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // V1 — jab client v1 header bheje
    @GetMapping(
        value = "/{id}",
        produces = "application/vnd.myapp.v1+json"
    )
    public UserResponseV1 getUserV1(@PathVariable Long id) {
        return new UserResponseV1("Rahul", "9999999999");
    }

    // V2 — jab client v2 header bheje
    @GetMapping(
        value = "/{id}",
        produces = "application/vnd.myapp.v2+json"
    )
    public UserResponseV2 getUserV2(@PathVariable Long id) {
        return new UserResponseV2("Rahul", "9999999999");
    }
}
```

### Query Parameter Versioning
```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int version) {

        if (version == 1) {
            return ResponseEntity.ok(new UserResponseV1("Rahul", "9999999999"));
        } else if (version == 2) {
            return ResponseEntity.ok(new UserResponseV2("Rahul", "9999999999"));
        }

        return ResponseEntity.badRequest().body("Invalid version");
    }
}
```


### Deprecation Headers (Version ko retire)
Step 1 — Sunset Header bhejo (announce karo ki kab band hoga) :
```java
// Ek din V1 band karni padegi. Iske liye proper process hota hai:

@GetMapping("/api/v1/users/{id}")
public ResponseEntity<UserResponseV1> getUserV1(@PathVariable Long id) {

    return ResponseEntity.ok()
        .header("Deprecation", "true")
        .header("Sunset", "Sat, 31 Dec 2025 23:59:59 GMT")  // ← is date ke baad band
        .header("Link", "</api/v2/users>; rel=\"successor-version\"")  // ← naya version yahan hai
        .body(new UserResponseV1("Rahul", "9999999999"));
}
// Jab bhi koi V1 call karega, yeh headers milenge. Client developers ko pata chal jaayega ki migrate karo.
```

Step 2 — Warning bhi daal sakte ho response body mein:
```java
public class UserResponseV1 {
    @JsonProperty("_warning")
    private String warning = "V1 is deprecated. Migrate to /api/v2/users by Dec 2025";

    private String name;
    private String phone;
}
```

Step 3 — Sunset date ke baad 410 Gone return karo:
```java
@GetMapping("/api/v1/users/{id}")
public ResponseEntity<?> getUserV1(@PathVariable Long id) {
    return ResponseEntity
        .status(HttpStatus.GONE)   // 410 — resource permanently gone
        .body("V1 API has been retired. Please use /api/v2/users");
}
```

### Backward Compatibility — Real example
```java
// Smart approach — ek hi response mein dono fields rakho temporarily
public class UserResponseV2 {
    private String name;
    private String mobile;      // naya field

    @Deprecated
    @JsonProperty("phone")      // purana field bhi bhejo — purane clients na tooten
    private String phone;

    // Constructor mein dono set karo
    public UserResponseV2(String name, String mobile) {
        this.name = name;
        this.mobile = mobile;
        this.phone = mobile;    // purana field mein bhi same value
    }
}
// Isse purane V1 clients bhi kaam karte rehte hain kyunki unhe phone field milti rehti hai.
```

---

## Folder Structure for Multi-version Project
```
controller/
  ├── v1/UserControllerV1.java
  └── v2/UserControllerV2.java
dto/
  ├── v1/UserResponseV1.java
  └── v2/UserResponseV2.java
```

---

## When to Retire Old Version

1. Add `Sunset` header → announce 3-6 months before
2. Check logs → who is still using V1?
3. Notify those clients → email/docs update
4. After sunset date → return `410 Gone`
5. Finally → delete the code

---

## Interview Questions

**Q: URI vs Header versioning — which is better?**  
URI versioning for public APIs — browser testable, cacheable, obvious.
Header versioning for internal APIs.

**Q: When to increment version?**  
Only for breaking changes. Non-breaking = no version change.

**Q: When to delete old version?**  
After sunset announcement period (3-6 months) + confirming 
zero active clients.

---

## My Confusion / Doubts

- [ ] Kya ek hi controller mein dono versions handle ho sakte hain?
- [ ] Header versioning Postman mein kaise test karein?

---

## Resources

- [Baeldung — API Versioning](https://www.baeldung.com/rest-versioning)
