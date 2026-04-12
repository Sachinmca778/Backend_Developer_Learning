# Content Negotiation

## Status: Not Started

---

## Table of Contents

1. [Content Negotiation Overview](#content-negotiation-overview)
2. [produces / consumes in @RequestMapping](#produces--consumes-in-requestmapping)
3. [MediaType Constants](#mediatype-constants)
4. [Accept Header](#accept-header)
5. [Content-Type Header](#content-type-header)
6. [Returning XML Alongside JSON](#returning-xml-alongside-json)

---

## Content Negotiation Overview

**Matlab:** Client aur server ke beech yeh negotiate karna ki data kis format (JSON, XML, etc.) mein exchange hoga.

### How It Works

```
Client Request:
    Accept: application/json        → "Mujhe JSON chahiye"
    Content-Type: application/json  → "Main JSON bhej raha hoon"

Server Response:
    Content-Type: application/json  → "Yeh lo JSON"
    Body: { "name": "Sachin" }
```

### Supported Formats

| Format | Media Type | Use Case |
|--------|-----------|----------|
| **JSON** | `application/json` | REST APIs (most common) |
| **XML** | `application/xml` | Legacy systems, enterprise |
| **Plain Text** | `text/plain` | Simple text responses |
| **HTML** | `text/html` | Web pages |
| **CSV** | `text/csv` | Data exports |
| **YAML** | `application/x-yaml` | Configuration |

---

## produces / consumes in @RequestMapping

### produces Attribute

**Response mein kaunsa format return hoga.**

```java
// Sirf JSON return karega
@GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}

// Sirf XML return karega
@GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_XML_VALUE)
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}

// Multiple formats — client Accept header ke basis pe decide hoga
@GetMapping(value = "/users/{id}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

### consumes Attribute

**Request mein kaunsa format accept hoga.**

```java
// Sirf JSON accept karega
@PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
public User createUser(@RequestBody User user) {
    return userService.save(user);
}

// JSON aur XML dono accept karega
@PostMapping(value = "/users",
             consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public User createUser(@RequestBody User user) {
    return userService.save(user);
}
```

### What Happens If Format Mismatch?

```java
// Sirf JSON accept karta hai
@PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
public User createUser(@RequestBody User user) { }
```

**Agar client XML bheje:**
```
POST /users
Content-Type: application/xml

<user><name>Sachin</name></user>

Response: 415 Unsupported Media Type
```

---

## MediaType Constants

Spring ke built-in MediaType constants use karo — magic strings se bacho.

```java
// Common MediaTypes
MediaType.APPLICATION_JSON         // application/json
MediaType.APPLICATION_JSON_VALUE   // "application/json"
MediaType.APPLICATION_XML          // application/xml
MediaType.APPLICATION_XML_VALUE    // "application/xml"
MediaType.TEXT_PLAIN               // text/plain
MediaType.TEXT_HTML                // text/html
MediaType.MULTIPART_FORM_DATA      // multipart/form-data
MediaType.APPLICATION_FORM_URLENCODED  // application/x-www-form-urlencoded
```

### Usage Example

```java
@GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}

@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
    // File handle karo
    return ResponseEntity.ok("File uploaded: " + file.getOriginalFilename());
}
```

---

## Accept Header

**Client batata hai ki woh kaunsa format chahta hai.**

### Client Requests

```
Request 1: JSON chahiye
GET /api/users/1
Accept: application/json

Response:
HTTP 200 OK
Content-Type: application/json
{"id": 1, "name": "Sachin"}
```

```
Request 2: XML chahiye
GET /api/users/1
Accept: application/xml

Response:
HTTP 200 OK
Content-Type: application/xml
<user><id>1</id><name>Sachin</name></user>
```

### Multiple Accept Headers (Quality Factor)

Client preference order bata sakta hai:

```
GET /api/users/1
Accept: application/xml;q=0.9, application/json;q=1.0

Server: JSON ko priority dega (q=1.0 > q=0.9)
```

### Spring Boot mein Content Negotiation Strategy

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            // Accept header se decide karo
            .favorPathExtension(false)           // .json, .xml extension disable
            .favorParameter(false)               // ?format=json disable
            .parameterName("format")
            .ignoreAcceptHeader(false)           // Accept header use karo
            .defaultContentType(MediaType.APPLICATION_JSON);  // Default JSON
    }
}
```

### Path Extension Strategy (Old Style)

```
GET /api/users/1.json   → JSON response
GET /api/users/1.xml    → XML response
```

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer
            .favorPathExtension(true)           // .json, .xml enable
            .favorParameter(false)
            .mediaType("json", MediaType.APPLICATION_JSON)
            .mediaType("xml", MediaType.APPLICATION_XML)
            .defaultContentType(MediaType.APPLICATION_JSON);
    }
}
```

---

## Content-Type Header

**Client batata hai ki woh kaunsa format bhej raha hai.**

### JSON Request

```
POST /api/users
Content-Type: application/json

{
  "name": "Sachin",
  "email": "sachin@example.com"
}
```

### XML Request

```
POST /api/users
Content-Type: application/xml

<user>
  <name>Sachin</name>
  <email>sachin@example.com</email>
</user>
```

### Form Data Request

```
POST /api/users
Content-Type: application/x-www-form-urlencoded

name=Sachin&email=sachin%40example.com
```

### Multipart Request (File Upload)

```
POST /api/upload
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary...

------WebKitFormBoundary...
Content-Disposition: form-data; name="file"; filename="report.pdf"
Content-Type: application/pdf

[file content]
------WebKitFormBoundary...--
```

---

## Returning XML Alongside JSON

### Setup XML Support

```xml
<!-- jackson-dataformat-xml dependency add karo -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

### Entity with XML Annotations

```java
// Jackson XML annotations
@XmlRootElement(name = "user")
@XmlAccessorType(XmlAccessType.FIELD)
public class User {

    private Long id;

    @XmlElement(name = "full_name")
    private String name;

    private String email;

    @JsonIgnore  // XML mein bhi nahi chahiye
    private String password;

    // Getters and Setters
}
```

### Controller Supporting Both Formats

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    // Dono JSON aur XML support karega
    @GetMapping(value = "/{id}",
                produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }

    // Dono accept karega
    @PostMapping(value = "/",
                 consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
                 produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public User createUser(@RequestBody User user) {
        return userService.save(user);
    }
}
```

### Client Decides Format

**JSON Request:**
```
GET /api/users/1
Accept: application/json

Response:
Content-Type: application/json
{"id": 1, "name": "Sachin", "email": "sachin@example.com"}
```

**XML Request:**
```
GET /api/users/1
Accept: application/xml

Response:
Content-Type: application/xml
<user>
  <id>1</id>
  <name>Sachin</name>
  <email>sachin@example.com</email>
</user>
```

### XML-Specific Configuration

```java
@Configuration
public class XmlConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer xmlCustomizer() {
        return builder -> {
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Content Negotiation** | Client aur server format agree karte hain |
| **produces** | Response format specify karo |
| **consumes** | Request format specify karo |
| **Accept Header** | Client batata hai kaunsa format chahta hai |
| **Content-Type Header** | Client batata hai kaunsa format bhej raha hai |
| **MediaType Constants** | `MediaType.APPLICATION_JSON_VALUE` — magic strings avoid karo |
| **jackson-dataformat-xml** | XML support enable karne ke liye |
| **favorPathExtension** | `.json`, `.xml` URL extensions (legacy style) |
| **defaultContentType** | Default format jab client kuch specify na kare |
