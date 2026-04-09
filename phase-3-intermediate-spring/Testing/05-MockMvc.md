# MockMvc

## Status: Not Started

---

## Table of Contents

1. [MockMvc Overview](#mockmvc-overview)
2. [MockMvc Setup](#mockmvc-setup)
3. [perform() with HTTP Methods](#perform-with-http-methods)
4. [content().json()](#contentjson)
5. [status() Assertions](#status-assertions)
6. [jsonPath Assertions](#jsonpath-assertions)
7. [Content Type Headers](#content-type-headers)
8. [MockMvcResultHandlers.print()](#mockmvcresulthandlersprint)
9. [@AutoConfigureMockMvc](#autoconfiguremockmvc)

---

## MockMvc Overview

**Matlab:** Spring MVC ko bina actual server start kiye test karna. HTTP requests ko simulate karta hai — MockMvc request bhejta hai aur response verify karta hai.

### Why MockMvc?

```
Real Server Test:
    → Server start karo (10-30 seconds)
    → HTTP client se request bhejo
    → Response check karo
    → Server band karo
    → Slow, fragile

MockMvc Test:
    → No server needed
    → MockMvc se request simulate karo
    → Response verify karo
    → Fast, reliable
```

### MockMvc Flow

```
Test Code
    ↓
mockMvc.perform(get("/api/users/1"))
    ↓
Spring DispatcherServlet (mocked)
    ↓
Controller → Service → Repository
    ↓
Response sent back to MockMvc
    ↓
Test assertions on response
```

---

## MockMvc Setup

### Auto-Setup via @WebMvcTest

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;  // Auto-configured — no manual setup needed

    @MockBean
    private UserService userService;
}
```

### Manual Setup

```java
@ExtendWith(MockitoExtension.class)
class UserControllerManualTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        // Manual MockMvc setup
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
    }

    @Test
    void testManualSetup() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk());
    }
}
```

---

## perform() with HTTP Methods

### GET Request

```java
@Test
void testGetRequest() throws Exception {
    when(userService.findById(1L)).thenReturn(new User(1L, "sachin", "sachin@email.com"));

    mockMvc.perform(get("/api/users/1")              // GET request
            .contentType(MediaType.APPLICATION_JSON) // Headers
            .header("Authorization", "Bearer token") // Custom header
            .param("include", "posts"))              // Query params
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("sachin"));
}
```

### POST Request

```java
@Test
void testPostRequest() throws Exception {
    when(userService.createUser(any(CreateUserRequest.class)))
        .thenReturn(new User(1L, "sachin", "sachin@email.com"));

    mockMvc.perform(post("/api/users")                     // POST request
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "sachin",
                    "email": "sachin@email.com"
                }
                """))
        .andExpect(status().isCreated())                   // 201 Created
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.username").value("sachin"));
}
```

### PUT Request

```java
@Test
void testPutRequest() throws Exception {
    User updated = new User(1L, "sachin_updated", "updated@email.com");
    when(userService.updateUser(eq(1L), any(UpdateUserRequest.class)))
        .thenReturn(updated);

    mockMvc.perform(put("/api/users/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "username": "sachin_updated",
                    "email": "updated@email.com"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("sachin_updated"));
}
```

### DELETE Request

```java
@Test
void testDeleteRequest() throws Exception {
    doNothing().when(userService).deleteUser(1L);

    mockMvc.perform(delete("/api/users/1"))
        .andExpect(status().isOk());  // or .isNoContent() for 204

    verify(userService).deleteUser(1L);
}
```

### PATCH Request

```java
@Test
void testPatchRequest() throws Exception {
    User patched = new User(1L, "sachin", "new@email.com");
    when(userService.patchUser(eq(1L), any(Map.class)))
        .thenReturn(patched);

    mockMvc.perform(patch("/api/users/1")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "new@email.com"
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("new@email.com"));
}
```

---

## content().json()

**Matlab:** Response body ko JSON string se compare karna. Exact ya lenient comparison.

### Exact JSON Match

```java
@Test
void testExactJsonMatch() throws Exception {
    when(userService.findById(1L))
        .thenReturn(new UserResponse(1L, "sachin", "sachin@email.com"));

    mockMvc.perform(get("/api/users/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().json("""
            {
                "id": 1,
                "username": "sachin",
                "email": "sachin@email.com"
            }
            """));
}
```

### Lenient JSON Match (Extra Fields Ignore)

```java
@Test
void testLenientJsonMatch() throws Exception {
    // Response mein extra fields ho sakte hain
    when(userService.findById(1L))
        .thenReturn(new UserResponse(1L, "sachin", "sachin@email.com", LocalDateTime.now()));

    mockMvc.perform(get("/api/users/1"))
        .andExpect(status().isOk())
        .andExpect(content().json("""
            {
                "username": "sachin",
                "email": "sachin@email.com"
            }
            """, false));  // false = strict match, true = lenient
    // Lenient mode mein extra fields (id, createdAt) ignore honge
}
```

### JSON Comparison Modes

```java
// Strict — sab fields match hone chahiye, order matter nahi
content().json("{\"name\":\"sachin\"}")

// Strict with order
content().json("{\"name\":\"sachin\"}", false)

// Lenient — subset match (extra fields OK)
content().json("{\"name\":\"sachin\"}", true)
```

---

## status() Assertions

**Matlab:** HTTP status code verify karna.

```java
@Test
void testStatusAssertions() throws Exception {
    // Success statuses
    mockMvc.perform(get("/api/users/1"))
        .andExpect(status().isOk());              // 200

    mockMvc.perform(post("/api/users"))
        .andExpect(status().isCreated());          // 201

    mockMvc.perform(delete("/api/users/1"))
        .andExpect(status().isNoContent());        // 204

    // Error statuses
    mockMvc.perform(get("/api/users/999"))
        .andExpect(status().isNotFound());         // 404

    mockMvc.perform(post("/api/users")
            .content("{invalid json}"))
        .andExpect(status().isBadRequest());       // 400

    mockMvc.perform(get("/api/admin/users"))
        .andExpect(status().isForbidden());        // 403

    mockMvc.perform(get("/api/protected"))
        .andExpect(status().isUnauthorized());     // 401

    // Any status
    mockMvc.perform(get("/api/health"))
        .andExpect(status().is2xxSuccessful());    // 2xx

    // Custom status check
    mockMvc.perform(get("/api/users"))
        .andExpect(status().value(200));           // Direct value
}
```

---

## jsonPath Assertions

**Matlab:** JSON response ke specific fields ko extract aur verify karna. JSONPath syntax use karta hai.

### Basic jsonPath

```java
@Test
void testJsonPathBasic() throws Exception {
    when(userService.findById(1L))
        .thenReturn(new UserResponse(1L, "sachin", "sachin@email.com", "ADMIN"));

    mockMvc.perform(get("/api/users/1"))
        .andExpect(status().isOk())
        // String values
        .andExpect(jsonPath("$.username").value("sachin"))
        .andExpect(jsonPath("$.email").value("sachin@email.com"))
        // Number values
        .andExpect(jsonPath("$.id").value(1))
        // With type check
        .andExpect(jsonPath("$.role").exists());
}
```

### Nested jsonPath

```java
// Response structure:
// {
//     "id": 1,
//     "user": {
//         "name": "sachin",
//         "address": {
//             "city": "Mumbai",
//             "zip": "400001"
//         }
//     },
//     "posts": [
//         {"title": "Post 1"},
//         {"title": "Post 2"}
//     ]
// }

@Test
void testNestedJsonPath() throws Exception {
    mockMvc.perform(get("/api/users/1/detail"))
        .andExpect(status().isOk())
        // Nested objects
        .andExpect(jsonPath("$.user.name").value("sachin"))
        .andExpect(jsonPath("$.user.address.city").value("Mumbai"))
        .andExpect(jsonPath("$.user.address.zip").value("400001"))
        // Array elements
        .andExpect(jsonPath("$.posts[0].title").value("Post 1"))
        .andExpect(jsonPath("$.posts[1].title").value("Post 2"))
        // Array size
        .andExpect(jsonPath("$.posts.length()").value(2));
}
```

### jsonPath Matchers

```java
@Test
void testJsonPathMatchers() throws Exception {
    mockMvc.perform(get("/api/users/1"))
        // Value match
        .andExpect(jsonPath("$.username").value("sachin"))

        // Exists / not exists
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.deletedAt").doesNotExist())

        // Type checks
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.username").isString())
        .andExpect(jsonPath("$.roles").isArray())

        // Null check
        .andExpect(jsonPath("$.deletedAt").isEmpty())

        // String matchers
        .andExpect(jsonPath("$.email").value(containsString("@")))
        .andExpect(jsonPath("$.username").value(startsWith("sach")))

        // Array checks
        .andExpect(jsonPath("$.roles", hasSize(2)))
        .andExpect(jsonPath("$.roles", hasItem("ADMIN")))
        .andExpect(jsonPath("$.posts[*].title", contains("Post 1", "Post 2")));
}
```

### jsonPath with Hamcrest

```java
import static org.hamcrest.Matchers.*;

@Test
void testJsonPathWithHamcrest() throws Exception {
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isOk())
        // Array of users
        .andExpect(jsonPath("$", hasSize(greaterThan(0))))
        .andExpect(jsonPath("$[*].username", hasItem("sachin")))
        .andExpect(jsonPath("$[0].id", greaterThan(0)))

        // Complex assertions
        .andExpect(jsonPath("$.total", allOf(
            greaterThan(0),
            lessThan(1000)
        )))

        // Empty checks
        .andExpect(jsonPath("$.errors", empty()))
        .andExpect(jsonPath("$.data", not(emptyOrNullString())));
}
```

---

## Content Type Headers

**Matlab:** Response ka Content-Type verify karna aur request ka Content-Type set karna.

### Setting Request Content-Type

```java
mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)       // Request body is JSON
        .content("{\"username\":\"sachin\"}"))

mockMvc.perform(get("/api/users")
        .accept(MediaType.APPLICATION_JSON))           // We want JSON response
```

### Verifying Response Content-Type

```java
@Test
void testContentTypeAssertions() throws Exception {
    mockMvc.perform(get("/api/users/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
}
```

### Character Encoding

```java
mockMvc.perform(get("/api/users/1")
        .accept(MediaType.APPLICATION_JSON))
    .andExpect(content().contentType("application/json;charset=UTF-8"));
```

---

## MockMvcResultHandlers.print()

**Matlab:** Full request aur response ko console pe print karna — debugging ke liye bahut useful.

### Basic Print

```java
@Test
void testPrintResult() throws Exception {
    when(userService.findById(1L))
        .thenReturn(new UserResponse(1L, "sachin", "sachin@email.com"));

    mockMvc.perform(get("/api/users/1")
            .contentType(MediaType.APPLICATION_JSON))
        .andDo(print())  // Full request + response print karo
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("sachin"));
}
```

### Print Output

```
MockHttpServletRequest:
      HTTP Method = GET
      Request URI = /api/users/1
       Parameters = {}
          Headers = [Content-Type:"application/json"]
             Body = null

Handler:
            Type = com.example.UserController
          Method = com.example.UserController#getUser(Long)

Async:
    Async started = false
     Async result = null

Resolved Exception:
            Type = null

ModelAndView:
        View name = null
             View = null
            Model = null

FlashMap:
       Attributes = null

MockHttpServletResponse:
           Status = 200
    Error message = null
          Headers = [Content-Type:"application/json"]
     Content type = application/json
             Body = {"id":1,"username":"sachin","email":"sachin@email.com"}
    Forwarded URL = null
   Redirected URL = null
          Cookies = []
```

### Conditional Print

```java
// Sirf failure pe print karo
@AutoConfigureMockMvc(printDefaultResultHandlers = false)
class UserControllerTest {

    @Test
    void testPrintOnlyOnFailure() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andDo(MvcResult::getResponse)
            .andExpect(status().isOk())  // Fail hone pe result nahi dikhega
            .andDo(result -> {
                if (result.getResponse().getStatus() != 200) {
                    System.out.println("Response body: " + result.getResponse().getContentAsString());
                }
            });
    }
}
```

---

## @AutoConfigureMockMvc

**Matlab:** MockMvc ko automatically configure karna with additional options.

### Basic Usage

```java
@SpringBootTest  // Full context
@AutoConfigureMockMvc  // MockMvc auto-configure
class FullIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testWithFullContext() throws Exception {
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk());
    }
}
```

### AutoConfigure Options

```java
// Print request/response for every test
@AutoConfigureMockMvc(print = MockMvcPrint.ON)

// Print only on failure
@AutoConfigureMockMvc(print = MockMvcPrint.FAILURE)

// Don't print anything
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)

// Disable security auto-configuration (for auth tests)
@AutoConfigureMockMvc(addFilters = false)  // No security filters
```

### With Security

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Test
    void testUnauthenticatedAccess() throws Exception {
        // No auth → 401
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticatedAccess() throws Exception {
        // With authentication
        mockMvc.perform(get("/api/admin/users")
                .with(httpBasic("admin", "admin123")))
            .andExpect(status().isOk());
    }

    @Test
    void testWithJwt() throws Exception {
        // Custom JWT in header
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + generateJwtToken()))
            .andExpect(status().isOk());
    }
}
```

---

## Quick Reference

```java
// HTTP Methods
mockMvc.perform(get("/api/users/1"))
mockMvc.perform(post("/api/users").content(json).contentType(MediaType.APPLICATION_JSON))
mockMvc.perform(put("/api/users/1").content(json))
mockMvc.perform(delete("/api/users/1"))
mockMvc.perform(patch("/api/users/1").content(json))

// Headers & Params
    .contentType(MediaType.APPLICATION_JSON)
    .accept(MediaType.APPLICATION_JSON)
    .header("Authorization", "Bearer token")
    .param("page", "0")
    .param("size", "10")

// Status assertions
    .andExpect(status().isOk())           // 200
    .andExpect(status().isCreated())      // 201
    .andExpect(status().isNotFound())     // 404
    .andExpect(status().isBadRequest())   // 400

// Content assertions
    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    .andExpect(content().json("{...}"))

// jsonPath assertions
    .andExpect(jsonPath("$.field").value("value"))
    .andExpect(jsonPath("$.nested.field").exists())
    .andExpect(jsonPath("$.array").isArray())
    .andExpect(jsonPath("$.array", hasSize(3)))

// Debug
    .andDo(print())

// Setup
@WebMvcTest(Controller.class) → @Autowired MockMvc
@SpringBootTest + @AutoConfigureMockMvc → Full context with MockMvc
```

---

## Summary

| Feature | Usage |
|---------|-------|
| **perform(get/post/put/delete)** | HTTP requests simulate karna |
| **content().json()** | Response body JSON se compare karna |
| **status().isOk()** | HTTP status code verify karna |
| **jsonPath("$.field")** | JSON response ke specific fields access karna |
| **contentType()** | Request/Response content type set/verify karna |
| **MockMvcResultHandlers.print()** | Full request/response debug output |
| **@AutoConfigureMockMvc** | Auto-configure MockMvc with options |
