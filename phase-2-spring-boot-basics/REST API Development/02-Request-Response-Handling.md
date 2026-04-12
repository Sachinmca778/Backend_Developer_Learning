# Request/Response Handling

## Status: Not Started

---

## Table of Contents

1. [@RequestBody](#requestbody)
2. [@ResponseBody](#responsebody)
3. [@ResponseStatus](#responsestatus)
4. [ResponseEntity<T>](#responseentityt)
5. [HttpServletRequest](#httpservletrequest)
6. [HttpServletResponse](#httpservletresponse)

---

## @RequestBody

**Matlab:** Incoming JSON ko Java object mein **deserialize** (convert) karta hai.

### Basic Usage

```java
@PostMapping("/users")
public User createUser(@RequestBody User user) {
    return userService.save(user);
}
```

**Request JSON:**
```json
{
  "name": "Sachin",
  "email": "sachin@example.com",
  "age": 25
}
```

**How it Works:**
```
JSON Request Body
    ↓
Jackson ObjectMapper reads JSON
    ↓
Deserializes into Java object
    ↓
User object controller method mein inject hota hai
```

### Optional @RequestBody

```java
// Required by default — body nahi aaya to 400 Bad Request
@PostMapping("/users")
public User createUser(@RequestBody User user) { }

// Optional — body nahi aaya to null milega
@PostMapping("/users")
public User createUser(@RequestBody(required = false) User user) { }
```

### Validation with @RequestBody

```java
@PostMapping("/users")
public User createUser(@Valid @RequestBody User user,
                       BindingResult result) {

    if (result.hasErrors()) {
        throw new MethodArgumentNotValidException(result);
    }

    return userService.save(user);
}
```

---

## @ResponseBody

**Matlab:** Return value ko JSON/XML mein serialize karke response body mein likhta hai.

### Usage

```java
// @RestController mein automatically included hai — likhne ki zarurat nahi
@RestController
public class UserController {

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return new User(id, "Sachin");  // Automatically JSON
    }
}

// @Controller mein manually @ResponseBody lagana padta hai
@Controller
public class OldStyleController {

    @GetMapping("/api/users/{id}")
    @ResponseBody  // Zaruri hai @Controller mein
    public User getUser(@PathVariable Long id) {
        return new User(id, "Sachin");
    }
}
```

### @ResponseBody vs @RestController

```java
// @Controller + @ResponseBody = @RestController

// Option 1: @Controller with @ResponseBody on each method
@Controller
public class Controller1 {

    @GetMapping("/users")
    @ResponseBody
    public List<User> getUsers() {
        return List.of(new User(1, "Sachin"));
    }
}

// Option 2: @RestController (recommended)
@RestController
@RequestMapping("/api/users")
public class Controller2 {

    @GetMapping
    public List<User> getUsers() {
        return List.of(new User(1, "Sachin"));
    }
}
```

---

## @ResponseStatus

**Matlab:** Response ka HTTP status code set karta hai.

### On Method

```java
@PostMapping("/users")
@ResponseStatus(HttpStatus.CREATED)  // 201 Created
public User createUser(@RequestBody User user) {
    return userService.save(user);
}

@DeleteMapping("/users/{id}")
@ResponseStatus(HttpStatus.NO_CONTENT)  // 204 No Content
public void deleteUser(@PathVariable Long id) {
    userService.deleteById(id);
}
```

### On Exception Class

```java
@ResponseStatus(HttpStatus.NOT_FOUND)  // 404
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// Usage
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
}
```

### Common HTTP Status Codes

| Status | Code | Use Case |
|--------|------|----------|
| **200 OK** | Success | GET/PUT/PATCH success |
| **201 Created** | Created | POST — resource bana |
| **204 No Content** | No Content | DELETE success |
| **400 Bad Request** | Bad Request | Invalid input |
| **401 Unauthorized** | Unauthorized | Not authenticated |
| **403 Forbidden** | Forbidden | Not authorized |
| **404 Not Found** | Not Found | Resource nahi mila |
| **500 Internal Server Error** | Server Error | Unexpected error |

---

## ResponseEntity<T>

**Matlab:** Response pe **full control** deta hai — status code, headers, aur body teen customize kar sakte ho.

### Basic Usage

```java
// 200 OK with body
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userService.findById(id);
    return ResponseEntity.ok(user);
}

// 201 Created with Location header
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    User created = userService.save(user);
    URI location = URI.create("/api/users/" + created.getId());
    return ResponseEntity.created(location).body(created);
}

// 204 No Content
@DeleteMapping("/users/{id}")
public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteById(id);
    return ResponseEntity.noContent().build();
}
```

### ResponseEntity Builder Methods

| Method | Status | Use Case |
|--------|--------|----------|
| `ResponseEntity.ok(body)` | 200 | Success response |
| `ResponseEntity.created(uri).body(body)` | 201 | Resource created |
| `ResponseEntity.accepted().body(body)` | 202 | Request accepted, processing |
| `ResponseEntity.noContent().build()` | 204 | Success, no body |
| `ResponseEntity.badRequest().build()` | 400 | Invalid input |
| `ResponseEntity.notFound().build()` | 404 | Resource not found |
| `ResponseEntity.status(418).body(body)` | Custom | Any status code |

### Full Control Example

```java
@PostMapping("/users")
public ResponseEntity<User> createUser(@RequestBody User user) {
    User created = userService.save(user);

    URI location = URI.create("/api/users/" + created.getId());

    return ResponseEntity
        .created(location)
        .header("X-Custom-Header", "CustomValue")
        .header("X-Request-Id", UUID.randomUUID().toString())
        .body(created);
}
```

### Conditional Responses

```java
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    return userRepository.findById(id)
        .map(user -> ResponseEntity.ok(user))
        .orElse(ResponseEntity.notFound().build());
}
```

### ResponseEntity with Headers

```java
@GetMapping("/download/{filename}")
public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
    Resource resource = fileStorageService.loadFile(filename);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(resource.contentLength()))
        .body(resource);
}
```

---

## HttpServletRequest

**Matlab:** Raw HTTP request object. Tab use karo jab built-in annotations (`@RequestParam`, `@PathVariable`, etc.) kaafi na hon.

### Usage

```java
@GetMapping("/request-info")
public Map<String, Object> getRequestInfo(HttpServletRequest request) {
    return Map.of(
        "method", request.getMethod(),
        "uri", request.getRequestURI(),
        "queryString", request.getQueryString(),
        "remoteAddr", request.getRemoteAddr(),
        "serverName", request.getServerName(),
        "serverPort", request.getServerPort(),
        "protocol", request.getProtocol()
    );
}
```

### Getting Headers

```java
@GetMapping("/headers")
public Map<String, String> getHeaders(HttpServletRequest request) {
    Map<String, String> headers = new HashMap<>();

    Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        headers.put(headerName, request.getHeader(headerName));
    }

    return headers;
}
```

### Getting Request Body Raw

```java
@PostMapping("/raw-body")
public String getRawBody(HttpServletRequest request) throws IOException {
    BufferedReader reader = request.getReader();
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
        sb.append(line);
    }
    return sb.toString();
}
```

---

## HttpServletResponse

**Matlab:** Raw HTTP response object. Tab use karo jab ResponseEntity se zyada control chahiye.

### Setting Headers

```java
@GetMapping("/download")
public void downloadFile(HttpServletResponse response) throws IOException {
    // Headers set karo
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "attachment; filename=report.pdf");

    // File stream response mein likho
    try (InputStream is = new FileInputStream("report.pdf");
         OutputStream os = response.getOutputStream()) {
        is.transferTo(os);
    }
}
```

### Setting Status Code

```java
@PostMapping("/legacy-create")
public void createUser(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Business logic
    User user = userService.save(new User());

    // Status set karo
    response.setStatus(HttpServletResponse.SC_CREATED);  // 201
    response.setContentType("application/json");

    // Body likho
    response.getWriter().write("{\"id\": " + user.getId() + "}");
}
```

### Redirect

```java
@GetMapping("/old-endpoint")
public void redirect(HttpServletResponse response) throws IOException {
    response.sendRedirect("/new-endpoint");  // 302 Redirect
}
```

---

## Summary

| Annotation/Class | Purpose | Best For |
|------------------|---------|----------|
| **@RequestBody** | JSON → Java object deserialize | POST/PUT/PATCH payloads |
| **@ResponseBody** | Java object → JSON serialize | @Controller mein JSON return |
| **@ResponseStatus** | HTTP status code set karna | Simple responses, exceptions |
| **ResponseEntity<T>** | Full control (status + headers + body) | Production APIs |
| **HttpServletRequest** | Raw request object access | Low-level access when needed |
| **HttpServletResponse** | Raw response object access | File downloads, redirects |

**Best Practice:** Hamesha `ResponseEntity<T>` use karo jab status ya headers customize karne hon. Simple cases mein `@ResponseStatus` kaafi hai.
