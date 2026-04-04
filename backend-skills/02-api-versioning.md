# API Versioning

**Date learned:** 04 April 2025  
**Phase:** Backend Skills  
**Status:** ✅ Done / 🔄 In Progress / ⏳ Not Started  
**Laravel equivalent:** Routes with prefix (`/api/v1`) — same concept

---

## Why Versioning?

When API changes break existing clients, versioning lets old 
and new versions coexist.

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

### 1. URI Versioning ✅ (Recommended)
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
GET /api/users?version=2
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

### Deprecation Headers
```java
@GetMapping("/api/v1/users/{id}")
public ResponseEntity<UserResponseV1> getUserV1(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header("Deprecation", "true")
        .header("Sunset", "Sat, 31 Dec 2025 23:59:59 GMT")
        .body(new UserResponseV1("Rahul", "9999999999"));
}
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
