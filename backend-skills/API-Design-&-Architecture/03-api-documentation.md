# API Documentation

## Status: Not Started

---

## Table of Contents

1. [Why API Documentation?](#why-api-documentation)
2. [OpenAPI 3.0 Spec](#openapi-30-spec)
3. [Swagger UI with springdoc-openapi](#swagger-ui-with-springdoc-openapi)
4. [Annotations (@Operation, @ApiResponse, @Schema, @Parameter)](#annotations)
5. [Generating Client SDKs from Spec](#generating-client-sdks-from-spec)
6. [API-First Design Approach](#api-first-design-approach)

---

## Why API Documentation?

**API documentation kyun zaruri hai?**

- Frontend developers ko pata chalega ki kaunse endpoints available hain
- Request/response format clear hoga
- Manual testing easy hogi
- Client SDKs automatically generate kar sakte ho
- Team collaboration improve hoga

**Without Documentation:**
- Frontend team ko backend team se baar-baar poochna padta hai
- Guesswork pe kaam hota hai
- Bugs zyada aate hain

**With Documentation:**
- Self-service - koi bhi docs dekh ke integrate kar sakta hai
- Clear contract between frontend and backend
- Auto-generated UI (Swagger UI) for testing

---

## OpenAPI 3.0 Spec

**OpenAPI kya hai?**
Ek standard format jo REST APIs ko describe karta hai - machine-readable aur human-readable dono.

**Pehle Swagger 2.0 tha, ab OpenAPI 3.0 hai** - zyada features aur better structure.

### OpenAPI 3.0 Structure

```yaml
openapi: 3.0.3
info:
  title: User Management API
  version: 1.0.0
  description: API for managing users
servers:
  - url: http://localhost:8080
paths:
  /api/users:
    get:
      summary: Get all users
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/User'
    post:
      summary: Create a new user
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateUserRequest'
      responses:
        '201':
          description: User created
components:
  schemas:
    User:
      type: object
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
        email:
          type: string
          format: email
    CreateUserRequest:
      type: object
      required:
        - name
        - email
      properties:
        name:
          type: string
        email:
          type: string
```

### Key Components

| Section | Purpose |
|---------|---------|
| **info** | API title, version, description |
| **servers** | Base URLs (dev, staging, prod) |
| **paths** | Endpoints + methods (GET, POST, etc.) |
| **components** | Reusable schemas (models, responses) |
| **security** | Authentication schemes |

---

## Swagger UI with springdoc-openapi

**Swagger UI kya hai?**
OpenAPI spec ko ek interactive web UI mein dikhata hai - endpoints test kar sakte ho browser se.

### Setup (Spring Boot)

#### 1. Add Dependency
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**For WebFlux:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

#### 2. That's It!
Application start karo aur access karo:

```
Swagger UI:     http://localhost:8080/swagger-ui.html
OpenAPI JSON:   http://localhost:8080/v3/api-docs
```

### Configuration
```yaml
# application.yml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    operations-sorter: alpha        # Alphabetical sort
    tags-sorter: alpha
  packages-to-scan: com.example.controller  # Specific package scan
  paths-to-match: /api/**           # Specific paths match
```

---

## Annotations

### @Operation

**Endpoint describe karta hai.**

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Operation(
        summary = "Get user by ID",
        description = "Returns a single user by their ID. If user not found, returns 404."
    )
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.findById(id);
    }
}
```

---

### @ApiResponse

**Different response codes describe karta hai.**

```java
@Operation(summary = "Get user by ID")
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "User found",
        content = @Content(schema = @Schema(implementation = User.class))
    ),
    @ApiResponse(
        responseCode = "404",
        description = "User not found"
    ),
    @ApiResponse(
        responseCode = "500",
        description = "Internal server error"
    )
})
@GetMapping("/{id}")
public User getUser(@PathVariable Long id) {
    return userService.findById(id);
}
```

---

### @Schema

**Model/DTO fields describe karta hai.**

```java
public class CreateUserRequest {
    
    @Schema(description = "User's full name", example = "John Doe", required = true)
    private String name;
    
    @Schema(description = "User's email address", example = "john@example.com", required = true)
    private String email;
    
    @Schema(description = "User's age", example = "25", minimum = "18", maximum = "100")
    private Integer age;
    
    @Schema(description = "User's role", 
            example = "USER", 
            allowableValues = {"USER", "ADMIN", "MODERATOR"})
    private String role;
    
    // Getters and setters
}
```

### @Schema on Response DTO
```java
public class UserResponse {
    
    @Schema(description = "Unique user ID", example = "1")
    private Long id;
    
    @Schema(description = "User's full name")
    private String name;
    
    @Schema(description = "User's email")
    private String email;
    
    @Schema(description = "Account creation timestamp")
    private LocalDateTime createdAt;
}
```

---

### @Parameter

**Request parameters describe karta hai.**

```java
@Operation(summary = "Search users")
@GetMapping("/search")
public List<User> searchUsers(
    @Parameter(description = "Search by name", example = "John")
    @RequestParam(required = false) String name,
    
    @Parameter(description = "Page number (0-based)", example = "0")
    @RequestParam(defaultValue = "0") int page,
    
    @Parameter(description = "Page size", example = "10")
    @RequestParam(defaultValue = "10") int size,
    
    @Parameter(description = "Sort field", example = "name")
    @RequestParam(defaultValue = "id") String sortBy
) {
    return userService.search(name, page, size, sortBy);
}
```

### @Parameter with Path Variable
```java
@Operation(summary = "Get user by ID")
@GetMapping("/{id}")
public User getUser(
    @Parameter(description = "User ID", required = true, example = "1")
    @PathVariable Long id
) {
    return userService.findById(id);
}
```

---

### Complete Controller Example

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {
    
    @Operation(summary = "Get all users", description = "Returns paginated list of users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Success"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    @GetMapping
    public Page<UserResponse> getAllUsers(
        @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size
    ) {
        return userService.findAll(page, size);
    }
    
    @Operation(summary = "Create user")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody @Valid CreateUserRequest request) {
        return userService.create(request);
    }
    
    @Operation(summary = "Delete user")
    @ApiResponse(responseCode = "204", description = "User deleted")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(
        @Parameter(description = "User ID") @PathVariable Long id
    ) {
        userService.delete(id);
    }
}
```

---

## Generating Client SDKs from Spec

**OpenAPI spec se automatically client SDKs generate kar sakte ho** - Java, TypeScript, Python, Go, etc.

### OpenAPI Generator

#### 1. Install OpenAPI Generator
```bash
# Using npm
npm install @openapitools/openapi-generator-cli -g

# Using Homebrew (Mac)
brew install openapi-generator
```

#### 2. Download OpenAPI Spec
```bash
# Application run karo aur spec download karo
curl http://localhost:8080/v3/api-docs -o openapi.json
```

#### 3. Generate Java Client
```bash
openapi-generator-cli generate \
  -i openapi.json \
  -g java \
  -o ./generated-client \
  --additional-properties=library=resteasy,groupId=com.example,artifactId=user-api-client
```

#### Generated Code Example
```java
// Auto-generated client
UserApi api = new UserApi();
api.setBasePath("http://localhost:8080");

// Call API
UserResponse user = api.getUserById(1L);
List<UserResponse> users = api.getAllUsers(0, 10);
```

#### Generate TypeScript Client
```bash
openapi-generator-cli generate \
  -i openapi.json \
  -g typescript-axios \
  -o ./generated-ts-client
```

```typescript
// Auto-generated TypeScript
const api = new UserApi();
const user = await api.getUserById(1);
const users = await api.getAllUsers(0, 10);
```

### Available Generators

| Language | Generator | Use Case |
|----------|-----------|----------|
| **Java** | `java`, `java-okhttp`, `java-resteasy` | Android, Java apps |
| **TypeScript** | `typescript-axios`, `typescript-fetch` | Frontend (React, Angular) |
| **Python** | `python`, `python-fastapi` | Python apps |
| **Go** | `go` | Go microservices |
| **C#** | `csharp` | .NET apps |

### Gradle Plugin (Auto-generate on build)
```gradle
plugins {
    id 'org.openapi.generator' version '7.0.0'
}

openApiGenerate {
    generatorName = "java"
    inputSpec = "$rootDir/openapi.json"
    outputDir = "$buildDir/generated"
    configOptions = [
        library: "resteasy",
        groupId: "com.example",
        artifactId: "user-api-client"
    ]
}

// Auto-generate before compile
compileJava.dependsOn tasks.openApiGenerate
```

---

## API-First Design Approach

**API-First kya hai?**
Pehle API design karo (OpenAPI spec), fir code likho. Code pehle nahi.

### Traditional vs API-First

| Approach | Process | Problem |
|----------|---------|---------|
| **Code-First** | Code likho → Spec generate karo | Spec incomplete ho sakta hai |
| **API-First** | Spec design karo → Code generate karo | Spec single source of truth hai |

### API-First Workflow

```
1. API Spec Design (OpenAPI YAML/JSON)
       ↓
2. Review with Team (Frontend, Backend, QA)
       ↓
3. Finalize Spec (Contract)
       ↓
4. Generate Client SDKs (Frontend starts work)
       ↓
5. Implement Backend (Contract ke hisaab se)
       ↓
6. Test against Spec
       ↓
7. Deploy
```

### Step 1: Design API Spec
```yaml
# api.yaml (Hand-written, not generated)
openapi: 3.0.3
info:
  title: E-Commerce API
  version: 1.0.0
paths:
  /api/products:
    get:
      summary: List all products
      parameters:
        - name: category
          in: query
          schema:
            type: string
        - name: page
          in: query
          schema:
            type: integer
            default: 0
      responses:
        '200':
          description: Success
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductPage'
  /api/products/{id}:
    get:
      summary: Get product by ID
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: integer
      responses:
        '200':
          description: Product found
        '404':
          description: Product not found
components:
  schemas:
    Product:
      type: object
      properties:
        id:
          type: integer
        name:
          type: string
        price:
          type: number
        category:
          type: string
    ProductPage:
      type: object
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/Product'
        totalPages:
          type: integer
        totalElements:
          type: integer
```

### Step 2: Generate Server Stubs
```bash
openapi-generator-cli generate \
  -i api.yaml \
  -g spring \
  -o ./server-stubs \
  --additional-properties=interfaceOnly=true,useTags=true
```

**Generated Interface:**
```java
// Auto-generated - aapko sirf implement karna hai
public interface ProductsApi {
    @GetMapping("/api/products")
    ResponseEntity<ProductPage> getProducts(
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "page", defaultValue = "0") Integer page
    );
    
    @GetMapping("/api/products/{id}")
    ResponseEntity<Product> getProductById(@PathVariable Integer id);
}
```

### Step 3: Implement Business Logic
```java
@RestController
public class ProductsApiController implements ProductsApi {
    
    private final ProductRepository productRepository;
    
    public ProductsApiController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    
    @Override
    public ResponseEntity<ProductPage> getProducts(String category, Integer page) {
        Page<Product> products = productRepository.findByCategory(category, PageRequest.of(page, 10));
        return ResponseEntity.ok(new ProductPage(products));
    }
    
    @Override
    public ResponseEntity<Product> getProductById(Integer id) {
        return productRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
```

### Step 4: Validate Implementation against Spec
```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>validate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <inputSpec>${project.basedir}/api.yaml</inputSpec>
    </configuration>
</plugin>
```

### Benefits of API-First

| Benefit | Explanation |
|---------|-------------|
| **Contract-First** | Clear agreement between frontend and backend |
| **Parallel Development** | Frontend can use mock servers while backend builds |
| **Consistency** | Spec enforce karta hai consistency |
| **Documentation** | Spec = Living documentation |
| **Testing** | Spec se tests generate kar sakte ho |
| **Versioning** | Spec versions se API versioning easy |

### Mock Server for Frontend
```bash
# Prism - OpenAPI mock server
npm install -g @stoplight/prism-cli

# Run mock server from spec
prism mock api.yaml
# http://localhost:4010/api/products - Fake data return karega
```

Frontend team backend complete hone se pehle kaam start kar sakta hai.

---

## Quick Reference

```java
// Setup
springdoc-openapi-starter-webmvc-ui dependency add karo
Swagger UI: http://localhost:8080/swagger-ui.html

// Annotations
@Operation(summary = "...", description = "...")
@ApiResponses({ @ApiResponse(responseCode = "200", ...) })
@Schema(description = "...", example = "...")
@Parameter(description = "...", example = "...")

// Client Generation
openapi-generator-cli generate -i openapi.json -g java -o ./client

// API-First
Spec Design → Generate Stubs → Implement → Validate
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **OpenAPI 3.0** | Standard spec format - machine-readable API description |
| **springdoc-openapi** | Spring Boot + Swagger UI integration |
| **@Operation** | Endpoint describe karta hai |
| **@ApiResponse** | Response codes aur descriptions |
| **@Schema** | Model/DTO fields describe karta hai |
| **@Parameter** | Query/path params describe karta hai |
| **Client SDKs** | OpenAPI spec se auto-generate (Java, TypeScript, etc.) |
| **API-First** | Pehle spec design karo, fir code likho - contract-driven development |
