# Level 2: Testing & Security Basics (Q137 - Q146)

---

### Q137: What is JUnit? Basic Annotations in JUnit 5 (Jupiter)?
**Answer (Hinglish):**

- **JUnit:** Java applications me automated unit testing perform karne ka standard framework hai. JUnit 5 latest standard hai.
- **Key JUnit 5 Annotations:**
  - `@Test`: Method ko test case mark karta hai.
  - `@BeforeEach`: Har individual `@Test` method se pehle run hota hai (Test setup).
  - `@AfterEach`: Har `@Test` ke baad run hota hai (Cleanup).
  - `@BeforeAll`: Saare test methods se pehle sirf ek baar run hota hai (Must be `static`).
  - `@AfterAll`: Sabhi tests complete hone ke baad ek baar run hota hai (Must be `static`).
  - `@Disabled`: Test method ko temporarily skip/ignore karne ke liye.
  - `@ParameterizedTest`: Same test logic ko multiple input datasets ke sath run karne ke liye.

---

### Q138 & Q139: Mockito, @Mock vs @MockBean vs @Spy
**Answer (Hinglish):**

- **Mockito:** Ek powerful mocking library hai jo dependencies (DB, 3rd party APIs) ko fake (mock) karne ke kaam aati hai taaki isolated unit test run ho sake.

| Annotation | Framework | Working & Difference |
| :--- | :--- | :--- |
| **`@Mock`** | Pure Mockito | Dependency ka 100% dummy mock banata hai. Default methods empty ya `null/0` return karte hain jab tak `when(...).thenReturn(...)` na likhein. Spring context load nahi karta (Lightning fast). |
| **`@MockBean`** | Spring Boot Test | Spring ApplicationContext ke andar kisi existing bean ko mock bean se replace kar deta hai. Integration / Controller testing me use hota hai. |
| **`@Spy`** | Pure Mockito | **Partial Mocking**. Real object create karta hai; agar koi specific method stub nahi kiya to real implementation chalti hai. |

---

### Q140 & Q141: MockMvc & @SpringBootTest vs @WebMvcTest
**Answer (Hinglish):**

| Feature | `@SpringBootTest` | `@WebMvcTest` |
| :--- | :--- | :--- |
| **Scope** | **Full Integration Test**. Poora Spring Boot ApplicationContext load karta hai (Database, Services, Repositories sabhi beans). | **Slice Test (Web Layer Only)**. Sirf Controllers, ControllerAdvice, aur Security load karta hai; Services/DB load nahi karta. |
| **Speed** | Slow (Kyunki poora enterprise context load hota hai). | **Fast** (Lightweight slice context). |
| **Usage** | End-to-end integration flow test karne ke liye. | Isolated Controller request/response testing ke liye. |

**MockMvc Controller Test Example:**
```java
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void testGetUserSuccess() throws Exception {
        when(userService.getUserById(1L)).thenReturn(new User(1L, "Sachin"));

        mockMvc.perform(get("/api/v1/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Sachin"));
    }
}
```

---

### Q142: What is Authentication vs Authorization?
**Answer (Hinglish):**

- **Authentication (401 Unauthorized):**
  - **Question:** *"Who are you?"* (Aap kaun ho?)
  - Identity verify karna (Username/Password, OTP, Biometrics, JWT).
  - Agar galat credentials hain ya token missing hai: **HTTP 401**.

- **Authorization (403 Forbidden):**
  - **Question:** *"What permissions do you have?"* (Kya aapko yeh action karne ki permission hai?)
  - Permissions and Role-Based Access Control (RBAC).
  - Example: Ek normal user login ho chuka hai (Authenticated), lekin wo Admin panel open kar raha hai: **HTTP 403 Forbidden**.

---

### Q143 & Q144: Spring Security Architecture & Securing REST APIs
**Answer (Hinglish):**

- **Spring Security Internals:**  
  Spring Security **Servlet Filter Chain (`SecurityFilterChain`)** ke pattern par kaam karta hai. Request controller tak pahunchne se pehle security filters ke series se pass hoti hai.

```
Client Request ──> [CorsFilter] ──> [JwtAuthenticationFilter] ──> [AuthorizationFilter] ──> DispatcherServlet ──> Controller
```

- **Modern Spring Security 6+ (Spring Boot 3) Configuration:**
  ```java
  @Configuration
  @EnableWebSecurity
  public class SecurityConfig {

      @Bean
      public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
          return http
              .csrf(AbstractHttpConfigurer::disable) // REST APIs ke liye CSRF disable
              .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless JWT
              .authorizeHttpRequests(auth -> auth
                  .requestMatchers("/api/auth/**", "/public/**").permitAll() // Public endpoints
                  .requestMatchers("/api/admin/**").hasRole("ADMIN")
                  .anyRequest().authenticated()
              )
              .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
              .build();
      }
  }
  ```

---

### Q145: BasicAuth vs JWT (JSON Web Token)?
**Answer (Hinglish):**

| Feature | Basic Authentication | JWT (JSON Web Token) |
| :--- | :--- | :--- |
| **Mechanism** | Har request ke header me Base64 encoded `username:password` bheja jata hai. | Login par digitally signed encrypted token issue hota hai jo request header (`Bearer <token>`) me pass hota hai. |
| **State & Scaling** | Stateless to hota hai par credentials har request me network par expose hote hain. | Stateless & Decentralized. Microservices bina central DB check kiye cryptographic signature verify kar sakti hain. |
| **Expiry Control** | No built-in expiry; password change karne par hi invalidate hota hai. | Token ke andar expiration time (`exp`) claims me embed hota hai. |

---

### Q146: HTTP Status Codes: 401, 403, 404, 500, 502, 503
**Answer (Hinglish):**

- **401 Unauthorized:** Authentication missing ya invalid hai (Invalid JWT token ya no login).
- **403 Forbidden:** Authenticated user ke paas requested action perform karne ka required role/permission nahi hai.
- **404 Not Found:** Requested URL path ya resource database me exist nahi karta.
- **500 Internal Server Error:** Application code me unhandled exception crash hua.
- **502 Bad Gateway:** Gateway/Nginx upstream microservice se invalid ya empty response receive karta hai.
- **503 Service Unavailable:** Server overloaded hai ya service maintenance/down chal rahi hai.
