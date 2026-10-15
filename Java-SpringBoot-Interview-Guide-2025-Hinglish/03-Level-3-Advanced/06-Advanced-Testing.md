# Level 3: Advanced Testing (Q242 - Q250)

---

### Q242: Unit vs Integration vs End-to-End (E2E) Testing
**Answer (Hinglish):**

```
           / \
          /E2E\       (Slowest, Most Expensive, Fewest tests)
         /-----\
        / Inte- \     (Medium speed, Tests DB, MQ, Spring Context)
       / gration \
      /-----------\
     /    Unit     \  (Fastest, Mocked dependencies, Isolated methods)
    /_______________\
```

- **Unit Testing:** Sirf ek isolated class ya method ke logic ko test karta hai. Saari external dependencies (DB, APIs) Mockito se mock hoti hain.
- **Integration Testing:** Multiple components ka aapsi interaction verify karta hai (e.g. Service + Spring Data JPA + Real DB container).
- **E2E Testing:** Pure system ko real environment me user perspective se test karta hai (UI se lekar DB aur 3rd party gateways tak).

---

### Q244: What are Testcontainers? How to use with Spring Boot?
**Answer (Hinglish):**

- **The Problem with H2 In-Memory DB:** Integration test me log H2 database use karte the, jabki production me PostgreSQL/MySQL hota tha. Postgres ke specific JSONB columns, sequences ya triggers H2 me fail ho jaate the.
- **Testcontainers Solution:** Testcontainers ek Java library hai jo unit/integration test chalte waqt Docker containers ke roop me **Real PostgreSQL, Redis, Kafka, ya MongoDB** automatically spin-up karti hai aur test complete hone par destroy kar deti hai!
- **Spring Boot 3.1+ `@ServiceConnection` Integration:**
  ```java
  @Testcontainers
  @SpringBootTest
  class OrderServiceIntegrationTest {

      @Container
      @ServiceConnection
      static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

      @Autowired
      private OrderRepository orderRepo;

      @Test
      void shouldSaveOrderInRealPostgres() {
          Order order = orderRepo.save(new Order("ORD-101", 500.0));
          assertThat(order.getId()).isNotNull();
      }
  }
  ```

---

### Q248: What is Contract Testing (Pact Framework)?
**Answer (Hinglish):**

- **Problem in Microservices:** Provider Service B ne apna API response schema badal diya (`user_name` se badal kar `fullName` kar diya). Consumer Service A crash ho gayi. E2E tests maintain karna impossible tha.
- **Contract Testing Solution (Pact):**  
  Consumer aur Provider ke beech ek formal "Contract" (Pact JSON file) sign hota hai.
  - Consumer test chala kar contract file generate karta hai (ki use kaun se fields chahiye).
  - Provider apne CI/CD pipeline me contract verify karta hai. Agar Provider koi breaking change karta hai, to Provider ka build turant fail ho jata hai production deploy hone se pehle!

---

### Q250: What is Mutation Testing (PITest)?
**Answer (Hinglish):**

- **Code Coverage vs Mutation Coverage:** High code coverage (90%) ka matlab yeh nahi hai ki test cases acche hain; agar test me assertions hi weak hon to code execute to ho jata hai par bugs catch nahi hote.
- **Mutation Testing:**  
  Tool aapke production bytecode me chhotey mutants (faults) introduce karta hai:
  - `>` ko `<` kar deta hai.
  - `+` ko `-` kar deta hai.
  - `true` ko `false` kar deta hai.
  - Agar aapke test suite ne is error ko pakad kar fail kiya -> **Mutant Killed** (Test quality high hai).
  - Agar mutants ke bawajood test pass ho gaya -> **Mutant Survived** (Test case me flaw hai).
