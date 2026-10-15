# Level 3: Advanced JPA & Spring Security (Q193 - Q213)

---

### Q193 & Q194: The N+1 Select Problem & Entity Graphs
**Answer (Hinglish):**

- **The N+1 Problem:**  
  Jab aap 1 query chalate hain parent entities fetch karne ke liye (1 query), aur fir har parent ke child collections ko access karte waqt Hibernate loop me **N additional queries** chalata hai. Total queries: `1 + N`.  
  *Example:* Agar 1000 orders fetch kiye, to Hibernate 1001 database queries execute karega -> Database crash!
- **How to Fix N+1 Problem:**
  1. **`JOIN FETCH` in JPQL:**
     ```java
     @Query("SELECT o FROM Order o JOIN FETCH o.items")
     List<Order> findAllWithItems();
     ```
  2. **Entity Graphs (`@EntityGraph`):**
     Query ko bina JPQL rewrite kiye dynamic eager fetch specify karta hai:
     ```java
     @EntityGraph(attributePaths = {"items", "customer"})
     List<Order> findAll();
     ```
  3. **Batch Fetching:** `@BatchSize(size = 50)` on the collection.

---

### Q196 & Q197: Optimistic vs Pessimistic Locking
**Answer (Hinglish):**

| Feature | Optimistic Locking | Pessimistic Locking |
| :--- | :--- | :--- |
| **Philosophy** | "Conflicts rare hain, hum lock nahi lagayenge, bas update ke time verify karenge." | "Conflicts frequent hain, database level row lock lagao taaki doosra wait kare." |
| **Mechanism** | `@Version` annotation (integer ya timestamp column). | SQL level `SELECT ... FOR UPDATE` lock (`LockModeType.PESSIMISTIC_WRITE`). |
| **Conflict Behavior** | Agar version mismatch hua, to `OptimisticLockException` throw hoti hai. | Doosra transaction row access karne ke liye block ho jata hai jab tak lock release na ho. |
| **Best Use Case** | High read, low write systems (e-commerce product descriptions, profile updates). | High contention banking/money transfer, flash sale limited stock inventory deductions. |

---

### Q199 & Q200: ACID Properties & HikariCP Connection Pooling
**Answer (Hinglish):**

- **ACID Properties:**
  - **Atomicity:** All operations succeed ya sabhi rollback honge.
  - **Consistency:** Database valid state se doosri valid state me jata hai (Constraints intact).
  - **Isolation:** Concurrent transactions ek doosre ko corrupt nahi karte.
  - **Durability:** Commit hone ke baad data permanent disk par persist rehta hai (even power loss me).
- **HikariCP (Default Spring Boot Pool):**
  - Ultra-fast, lightweight JDBC connection pool.
  - **Key Properties:**
    ```properties
    spring.datasource.hikari.maximum-pool-size=20
    spring.datasource.hikari.minimum-idle=5
    spring.datasource.hikari.idle-timeout=300000
    spring.datasource.hikari.connection-timeout=20000
    spring.datasource.hikari.max-lifetime=1800000
    ```

---

### Q201: Database Migrations with Flyway / Liquibase
**Answer (Hinglish):**

- **Problem:** Production me `spring.jpa.hibernate.ddl-auto=update` use karna forbidden hai (data loss risk).
- **Flyway Solution:**
  - SQL migration scripts version control me `src/main/resources/db/migration` me rakhte hain.
  - Naming standard: `V1__init_schema.sql`, `V2__add_phone_to_users.sql`.
  - Flyway start hote waqt `flyway_schema_history` table maintain karke check karta hai ki kaun si nayi script execute karni hai. Sabhi environments me schema deterministic rehta hai.

---

### Q203, Q204 & Q208: OAuth2, Stateless JWT & Spring Security 6+
**Answer (Hinglish):**

- **Stateless Session with JWT:**  
  Server par koi in-memory HTTP Session maintain nahi hoti (`SessionCreationPolicy.STATELESS`). Har request ke sath authorization header `Bearer <JWT_TOKEN>` aata hai.
- **JWT Anatomy:**  
  `Header.Payload.Signature`
  - *Header:* Alg type (RS256, HS256).
  - *Payload:* Claims (User ID, roles, exp, iss).
  - *Signature:* Secret key se cryptographically hashed.

---

### Q206: How CSRF Protection Works & Why Disable for REST APIs?
**Answer (Hinglish):**

- **Cross-Site Request Forgery (CSRF):** Ek attack jisme attacker victim ke logged-in browser session cookies ka faayda utha kar malicious site se bank transfer request trigger karwa deta hai.
- **CSRF Token:** Server har session me ek secret random token deta hai jo form submissions ke sath aana mandatory hota hai.
- **REST APIs me CSRF Disable kyu karte hain?**  
  Modern REST APIs stateless JWT tokens use karti hain jo `Authorization: Bearer <token>` header me bheje jaate hain, browser session cookies me nahi. Attackers headers ko auto-attach nahi kar sakte, isliye REST me CSRF ki zaroorat nahi hoti: `http.csrf(AbstractHttpConfigurer::disable)`.

---

### Q210 & Q211: Role-Based Access Control (RBAC) & Method-Level Security
**Answer (Hinglish):**

Enable method security with `@EnableMethodSecurity`:
```java
@Service
public class UserService {

    // Sirf ADMIN role wala user hi delete kar sakta hai
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    // User sirf apna khud ka profile update kar sakta hai
    @PreAuthorize("#username == authentication.principal.username or hasRole('ADMIN')")
    public void updateProfile(String username, UserDTO dto) {
        // ...
    }
}
```

---

### Q212: How to Secure Microservices Communication
**Answer (Hinglish):**

1. **mTLS (Mutual TLS):** Client microservice aur Server microservice dono ek doosre ke SSL certificates verify karte hain (Zero-trust network).
2. **Internal API Gateway / Reverse Proxy:** External traffic gateway par authenticate hota hai, aur internal microservices me JWT header propagate hota hai.
3. **OAuth2 Client Credentials Grant:** Service-to-service machine communication ke liye keycloak/Okta se machine token lekar inter-service communication secure karna.
