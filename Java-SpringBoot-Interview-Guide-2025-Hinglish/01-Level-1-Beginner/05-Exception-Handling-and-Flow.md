# Level 1: Exception Handling & Control Flow (Q30 - Q41)

---

### Q30: What is an Exception? Types of exceptions?
**Answer (Hinglish):**

- **Exception Kya Hai:**  
  Exception ek aisi abnormal condition ya event hoti hai jo program ke execution ke dauran (runtime par) occur hoti hai aur program ke normal execution flow ko disrupt (crash) kar deti hai.
- **Hierarchy:**
  ```
                 Throwable
                 /       \
              Error     Exception
                         /       \
             RuntimeException   Checked Exceptions
             (Unchecked)        (IOException, SQLException, etc.)
  ```
- **Main Types:**
  1. **Checked Exceptions:** Compile-time exceptions. Compiler force karta hai ki inhe handle karo (try-catch ya throws).
  2. **Unchecked Exceptions (`RuntimeException` subclasses):** Runtime exceptions. Programming mistakes ya bad logic ki wajah se hoti hain.

---

### Q31: What is the difference between checked and unchecked exceptions?
**Answer (Hinglish):**

| Difference Point | Checked Exception | Unchecked Exception |
| :--- | :--- | :--- |
| **Parent Class** | `Exception` (Directly, excluding `RuntimeException`). | `RuntimeException` aur uske sub-classes. |
| **Check Time** | **Compile time** par compiler check karta hai. | **Runtime** par occur hoti hai. |
| **Handling Mandate** | **Mandatory** (Agar try-catch ya `throws` nahi likha, to code compile hi nahi hoga). | **Optional** (Compiler force nahi karta). |
| **Examples** | `IOException`, `SQLException`, `ClassNotFoundException`. | `NullPointerException`, `ArithmeticException`, `ArrayIndexOutOfBoundsException`. |
| **Philosophy** | External systems failure (e.g. file missing, network down) jo recover ho sakti hai. | Developer ki coding flaws jo fix karni chahiye (e.g. null pointer pass karna). |

---

### Q32: What is try-catch-finally block?
**Answer (Hinglish):**

1. **`try` block:**  
   Iske andar wo risky code likhte hain jisme exception aane ki probability hoti hai.
2. **`catch` block:**  
   Agar try block ke andar exception throw hoti hai, to catch block use intercept karke handle/log karta hai taaki program crash na ho.
3. **`finally` block:**  
   Yeh block **guaranteed execute hota hai**, chahe exception aaye ya na aaye, ya catch block me handle ho ya na ho.  
   - **Primary Use:** Resource clean-up (Database connection close karna, File I/O stream close karna).

> **Tricky Interview Question:**  
> *"Kya koi aisi situation hai jisme `finally` block execute NAHI hota?"*  
> **Jawab:** Haan! Agar try ya catch block me `System.exit(0)` call ho jaye, ya JVM crash/OS kill ho jaye, tab finally block execute nahi hota.

---

### Q33: Can we use try without catch?
**Answer (Hinglish):**

**Haan, bilkul!**  
1. **`try-finally` combination:**
   ```java
   try {
       // risky code
   } finally {
       // cleanup code runs guaranteed
   }
   ```
2. **`try-with-resources` (Java 7+):**  
   Jo classes `AutoCloseable` interface implement karti hain, unhe parenthesis me declare karke bina catch aur bina finally ke likh sakte hain (JVM resources ko automatically close kar deta hai):
   ```java
   try (FileReader fr = new FileReader("test.txt")) {
       // file automatically closed at the end!
   }
   ```

---

### Q34: What is throw vs throws?
**Answer (Hinglish):**

| Point | `throw` keyword | `throws` keyword |
| :--- | :--- | :--- |
| **Usage** | Exception object ko **explicitly throw** karne ke liye. | Method signature me declare karne ke liye ki yeh method exception throw kar sakta hai. |
| **Location** | Method body ke andar likha jata hai. | Method signature ke saath likha jata hai. |
| **Syntax** | Followed by an **instance** (`throw new CustomException();`). | Followed by exception **class names** (`throws IOException, SQLException`). |
| **Quantity** | Ek time par sirf 1 exception throw ho sakti hai. | Multiple exceptions comma-separated declare ho sakti hain. |

---

### Q35 & Q36: User-defined exceptions ke use cases aur unhe handle kaise karein?
**Answer (Hinglish):**

- **Use Case:**  
  Standard Java exceptions generic hoti hain (jaise `IllegalArgumentException`). Business logic specific errors ko meaningful identify karne ke liye Custom Exceptions banayi jati hain (e.g. `InsufficientFundsException`, `UserNotFoundException`, `InvalidOtpException`).

- **Custom Exception Create Karna:**
  ```java
  // RuntimeException ko extend karein to Unchecked banegi (Spring me transaction rollback ke liye standard hai)
  public class InsufficientBalanceException extends RuntimeException {
      private final String errorCode;

      public InsufficientBalanceException(String message, String errorCode) {
          super(message);
          this.errorCode = errorCode;
      }
      public String getErrorCode() { return errorCode; }
  }
  ```

- **How to Handle (Spring Boot Best Practice):**
  Spring Boot me Global Exception Handler (`@RestControllerAdvice`) ke through clean JSON response banate hain:
  ```java
  @RestControllerAdvice
  public class GlobalExceptionHandler {

      @ExceptionHandler(InsufficientBalanceException.class)
      public ResponseEntity<ErrorResponse> handleBalance(InsufficientBalanceException ex) {
          ErrorResponse err = new ErrorResponse(ex.getErrorCode(), ex.getMessage(), LocalDateTime.now());
          return new ResponseEntity<>(err, HttpStatus.BAD_REQUEST);
      }
  }
  ```

---

### Q37: What is a NullPointerException & how to prevent it?
**Answer (Hinglish):**

- **Kya hai:** Jab aap kisi aise reference variable par method call ya property access karte hain jo memory me kisi object ko point nahi kar raha (`null` point kar raha hai), to JVM runtime par `NullPointerException` (NPE) throw karta hai.
- **Top 5 Ways to Prevent NPE:**
  1. **Literals first in equals:**  
     `"SUCCESS".equals(status)` use karein, `status.equals("SUCCESS")` nahi (agar status null hua to crash nahi hoga).
  2. **Java 8 `Optional<T>`:**  
     Methods se nullable values return karte waqt `Optional.ofNullable(user)` use karein.
  3. **Objects utility methods:**  
     `Objects.requireNonNull(input, "Input cannot be null");`
  4. **Apache Commons / Spring StringUtils:**  
     `StringUtils.hasText(str)` ya `CollectionUtils.isEmpty(list)`.
  5. **Modern Java:** Java 14+ "Helpful NullPointerExceptions" batata hai ki exact kaun sa sub-expression null tha (`a.b.c.getName()` me exact `c` null tha).

---

### Q38: What is a ClassCastException?
**Answer (Hinglish):**

- **Kya hai:** Yeh ek Unchecked runtime exception hai jo tab aati hai jab aap kisi object ko kisi aisi subclass ya incompatible type me explicitly cast karne ki koshish karte hain jiska wo actual instance nahi hai.
```java
Object obj = Integer.valueOf(100);
String str = (String) obj; // Throws ClassCastException: Integer cannot be cast to String!
```
- **Prevention:** Casting se pehle hamesha `instanceof` check karein (Java 16+ Pattern Matching syntax):
```java
if (obj instanceof String s) {
    System.out.println(s.length()); // Safe, no exception
}
```

---

### Q39: What is Error vs Exception?
**Answer (Hinglish):**

Dono `Throwable` ke direct child hain:

| Criteria | Error | Exception |
| :--- | :--- | :--- |
| **Nature** | System-level catastrophic failures (Hardware/JVM issues). | Application-level conditions jo program code me aati hain. |
| **Recoverable?** | **Non-recoverable**. Application ko crash hone se bachana lagbhag impossible hota hai. | **Recoverable**. `try-catch` lagakar safely recover kiya ja sakta hai. |
| **Examples** | `OutOfMemoryError` (OOM), `StackOverflowError`, `VirtualMachineError`. | `NullPointerException`, `SQLException`, `IOException`. |
| **Handling Advice**| Isko catch nahi karna chahiye. Infrastructure ya memory increase karni padti hai. | Code me properly catch/handle karna chahiye. |

---

### Q40: Explain the difference between break and continue statements
**Answer (Hinglish):**

| Statement | Behavior | Real-world Use Case |
| :--- | :--- | :--- |
| **`break`** | Current loop (ya switch block) ko **immediately terminate** kar deta hai aur loop ke bahar nikal deta hai. | Linear search me element mil jane par aage ke iterations stop karna. |
| **`continue`** | Current iteration ko **skip** kar deta hai aur seedha agle iteration par jump kar jata hai. | Odd/Even filter karna ya invalid items ko skip karke agle par badhna. |

```java
for (int i = 1; i <= 5; i++) {
    if (i == 2) continue; // 2 skip ho jayega
    if (i == 4) break;    // 4 aate hi loop band ho jayega
    System.out.print(i + " ");
}
// Output: 1 3 
```

---

### Q41: What is the difference between == and .equals()?
**Answer (Hinglish):**

| Aspect | `==` Operator | `.equals()` Method |
| :--- | :--- | :--- |
| **Kya Check Karta Hai** | **Reference / Memory Address:** Check karta hai ki dono variables memory ke bilkul exact same location ko point kar rahe hain ya nahi. | **Content / Value Equality:** Objects ke andar ka actual data/content compare karta hai (agar method overridden ho). |
| **Customization** | Iska behavior change (override) nahi kiya ja sakta. | Classes apni zaroorat ke mutabiq `@Override` kar sakti hain. |
| **Primitives** | Primitive values (`int a == int b`) ko directly compare karne ke liye use hota hai. | Primitives par call nahi ho sakta (sirf objects par). |

```java
String s1 = new String("Java");
String s2 = new String("Java");

System.out.println(s1 == s2);      // false (Dono heap me alag-alag memory addresses par hain)
System.out.println(s1.equals(s2));  // true (Dono ka content bilkul identical "Java" hai)
```
