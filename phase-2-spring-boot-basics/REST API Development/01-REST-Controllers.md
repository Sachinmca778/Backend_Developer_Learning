# REST Controllers

## Status: Not Started

---

## Table of Contents

1. [@RestController](#restcontroller)
2. [@RequestMapping](#requestmapping)
3. [@GetMapping](#getmapping)
4. [@PostMapping](#postmapping)
5. [@PutMapping](#putmapping)
6. [@PatchMapping](#patchmapping)
7. [@DeleteMapping](#deletemapping)
8. [@RequestParam](#requestparam)
9. [@PathVariable](#pathvariable)
10. [@RequestHeader](#requestheader)
11. [@CookieValue](#cookievalue)

---

## @RestController

**Matlab:** Ek special `@Controller` jo automatically JSON/XML response return karta hai. Internally `@Controller` + `@ResponseBody` ka combination hai.

```
@RestController = @Controller + @ResponseBody
```

### @Controller vs @RestController

| Feature | @Controller | @RestController |
|---------|-------------|-----------------|
| **Returns** | View name (JSP, Thymeleaf) | JSON/XML response body |
| **@ResponseBody** | Manually add karna padta hai | Automatically included |
| **Use Case** | Server-side rendering (web pages) | REST APIs |

### Example

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // Return object → Automatically JSON mein convert hoga
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return new User(id, "Sachin", "sachin@example.com");
    }

    // Return String → Plain text response
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com"
}
```

---

## @RequestMapping

**Matlab:** Class ya method level pe URL routes map karta hai. Most generic mapping annotation hai.

### Class Level (Base Path)

```java
@RestController
@RequestMapping("/api/users")  // Base path for all methods
public class UserController {

    @GetMapping  // /api/users
    public List<User> getAllUsers() {
        return List.of(new User(1, "Sachin"));
    }

    @GetMapping("/{id}")  // /api/users/1
    public User getUser(@PathVariable Long id) {
        return new User(id, "Sachin");
    }
}
```

### Method Level with Full Options

```java
@RequestMapping(
    value = "/search",
    method = RequestMethod.GET,
    params = {"name", "email"},
    headers = {"X-Custom-Header"},
    produces = MediaType.APPLICATION_JSON_VALUE
)
public User searchUser(@RequestParam String name,
                       @RequestParam String email) {
    return new User(1, name, email);
}
```

### @RequestMapping Attributes

| Attribute | Description |
|-----------|-------------|
| **value / path** | URL pattern |
| **method** | HTTP method (GET, POST, PUT, etc.) |
| **params** | Required query parameters |
| **headers** | Required headers |
| **produces** | Response content type |
| **consumes** | Request content type |

---

## @GetMapping

**Matlab:** HTTP GET requests handle karta hai. Data fetch/read karne ke liye use hota hai.

```java
@GetMapping("/users")
public List<User> getAllUsers() {
    return userService.findAll();
}

@GetMapping("/users/{id}")
public User getUserById(@PathVariable Long id) {
    return userService.findById(id);
}

// Multiple path variables
@GetMapping("/users/{userId}/posts/{postId}")
public Post getUserPost(@PathVariable Long userId,
                        @PathVariable Long postId) {
    return postService.findById(postId);
}
```

---

## @PostMapping

**Matlab:** HTTP POST requests handle karta hai. Naya resource create karne ke liye use hota hai.

```java
@PostMapping("/users")
@ResponseStatus(HttpStatus.CREATED)  // 201 Created
public User createUser(@RequestBody User user) {
    return userService.save(user);
}

// With ResponseEntity (full control)
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    User createdUser = userService.save(user);
    URI location = URI.create("/api/users/" + createdUser.getId());

    return ResponseEntity
        .created(location)  // 201 + Location header
        .body(createdUser);
}
```

---

## @PutMapping

**Matlab:** HTTP PUT requests handle karta hai. Resource ko **completely replace** karne ke liye use hota hai.

```java
@PutMapping("/users/{id}")
public ResponseEntity<User> updateUser(@PathVariable Long id,
                                       @RequestBody User user) {
    User updatedUser = userService.update(id, user);
    return ResponseEntity.ok(updatedUser);
}
```

**PUT vs PATCH:**
| Feature | PUT | PATCH |
|---------|-----|-------|
| **Update Type** | Complete replacement | Partial update |
| **Payload** | Poora object bhejo | Sirf changed fields bhejo |

---

## @PatchMapping

**Matlab:** HTTP PATCH requests handle karta hai. Resource ko **partially update** karne ke liye use hota hai.

```java
@PatchMapping("/users/{id}")
public ResponseEntity<User> patchUser(@PathVariable Long id,
                                      @RequestBody Map<String, Object> updates) {
    User updatedUser = userService.partialUpdate(id, updates);
    return ResponseEntity.ok(updatedUser);
}
```

**Example Payload (Partial):**
```json
{
  "name": "Sachin Updated"
  // Sirf name update hoga, email same rahega
}
```

---

## @DeleteMapping

**Matlab:** HTTP DELETE requests handle karta hai. Resource delete karne ke liye use hota hai.

```java
@DeleteMapping("/users/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)  // 204 No Content
public void deleteUser(@PathVariable Long id) {
    userService.deleteById(id);
}

// With ResponseEntity
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

---

## @RequestParam

**Matlab:** Query parameters extract karta hai URL se (`?key=value` format).

```java
@GetMapping("/users")
public List<User> getUsers(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String sortBy,
    @RequestParam(defaultValue = "asc") String sortDir
) {
    return userService.findPaginated(page, size, sortBy, sortDir);
}
```

**Request:** `GET /users?page=2&size=20&sortBy=name&sortDir=desc`

### @RequestParam Options

```java
// Required by default
@RequestParam String name

// Optional
@RequestParam(required = false) String email

// With default value
@RequestParam(defaultValue = "0") int page

// Different name
@RequestParam("user_id") Long userId

// All params as Map
@RequestParam Map<String, String> allParams
```

---

## @PathVariable

**Matlab:** URL path se value extract karta hai (`/users/{id}` format).

```java
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}

// Multiple path variables
@GetMapping("/users/{userId}/posts/{postId}")
public Post getPost(@PathVariable Long userId,
                    @PathVariable Long postId) {
    return postService.findById(postId);
}

// Different variable name
@GetMapping("/users/{id}")
public User getUser(@PathVariable("id") Long userId) {
    return userService.findById(userId);
}
```

### @RequestParam vs @PathVariable

| Feature | @RequestParam | @PathVariable |
|---------|---------------|---------------|
| **URL** | `/users?id=1` | `/users/1` |
| **Use Case** | Optional params, filtering, pagination | Resource identification |
| **REST Style** | Less RESTful | More RESTful |
| **Required** | Default: true | Default: true |

---

## @RequestHeader

**Matlab:** HTTP request headers se values extract karta hai.

```java
@GetMapping("/users/me")
public User getCurrentUser(@RequestHeader("Authorization") String authHeader) {
    String token = authHeader.substring(7);  // Remove "Bearer "
    return userService.findByToken(token);
}

// Optional header
@GetMapping("/info")
public String getInfo(@RequestHeader(value = "X-Custom-Header", required = false) String customHeader) {
    return customHeader != null ? customHeader : "No header";
}

// With default value
@GetMapping("/locale")
public String getLocale(@RequestHeader(value = "Accept-Language", defaultValue = "en") String lang) {
    return lang;
}

// All headers as Map
@GetMapping("/headers")
public Map<String, String> getAllHeaders(@RequestHeader Map<String, String> headers) {
    return headers;
}
```

---

## @CookieValue

**Matlab:** HTTP cookies se values extract karta hai.

```java
@GetMapping("/user/session")
public String getSessionId(@CookieValue("JSESSIONID") String sessionId) {
    return "Session: " + sessionId;
}

// Optional cookie
@GetMapping("/tracking")
public String getTrackingId(@CookieValue(value = "tracking_id", required = false) String trackingId) {
    return trackingId != null ? trackingId : "No tracking";
}

// With default value
@GetMapping("/theme")
public String getTheme(@CookieValue(value = "theme", defaultValue = "light") String theme) {
    return theme;
}
```

---

## Summary

| Annotation | Purpose | Example |
|------------|---------|---------|
| **@RestController** | JSON response return karta hai | `@RestController` |
| **@RequestMapping** | URL map karta hai (generic) | `@RequestMapping("/api/users")` |
| **@GetMapping** | GET request — data read | `@GetMapping("/{id}")` |
| **@PostMapping** | POST request — data create | `@PostMapping` |
| **@PutMapping** | PUT request — full update | `@PutMapping("/{id}")` |
| **@PatchMapping** | PATCH request — partial update | `@PatchMapping("/{id}")` |
| **@DeleteMapping** | DELETE request — data delete | `@DeleteMapping("/{id}")` |
| **@RequestParam** | Query params (`?key=value`) | `@RequestParam String name` |
| **@PathVariable** | Path params (`/users/{id}`) | `@PathVariable Long id` |
| **@RequestHeader** | Header se value extract | `@RequestHeader("Authorization")` |
| **@CookieValue** | Cookie se value extract | `@CookieValue("sessionId")` |
