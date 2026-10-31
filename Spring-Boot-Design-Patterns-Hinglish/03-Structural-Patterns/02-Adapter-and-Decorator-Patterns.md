# Structural Patterns: Adapter & Decorator Patterns in Spring Boot

---

## 🔌 1. Adapter Pattern

### Core Concept:
> **Converts one interface into another interface that the client expects.**  
> *(Analogy: Indian 3-pin plug ko European 2-pin socket me lagane ke liye hum Travel Adapter use karte hain).*

Jab aap kisi 3rd-party library ya external legacy system ke sath integrate karte hain, to unka method signature aur data format aapke system ke format se match nahi karta. Adapter un dono ke beech ek translator ban jata hai.

---

### Real Project Example: 3rd-Party Payment Gateway Integration

Maan lijiye aapke core business logic me ek standard contract bana hua hai:

```java
// 1. Our standard application interface
public interface PaymentGateway {
    PaymentResult charge(Order order);
}
```

Ab aapko **Razorpay SDK** integrate karna hai, jiska method totally alag hai:
`razorClient.createPayment(long amountInPaise, String currency);`

#### The Adapter Implementation:
```java
@Component
public class RazorpayAdapter implements PaymentGateway {

    private final RazorClient razorClient;

    public RazorpayAdapter(RazorClient razorClient) {
        this.razorClient = razorClient;
    }

    @Override
    public PaymentResult charge(Order order) {
        // Step 1: Adapt input (Rupees to Paise)
        long paise = (long) (order.getAmount() * 100);

        // Step 2: Call third-party API
        RazorResponse response = razorClient.createPayment(paise, "INR");

        // Step 3: Adapt output into our system's PaymentResult
        return new PaymentResult(response.getId(), response.isSuccess());
    }
}
```

> **Huge Benefit:** Agar kal company Razorpay chhod kar **Stripe** ya **PayTM** par switch karegi, to core business logic me **zero changes** honge! Sirf ek naya `StripeAdapter` implement karna padega.

---

### In Spring MVC: `HandlerAdapter`
Spring ka `DispatcherServlet` alag-alag type ke web controllers (Annotations-based `@Controller`, legacy `HttpRequestHandler`, `SimpleControllerHandlerAdapter`) ko execute karne ke liye internally **`HandlerAdapter`** use karta hai.

---

## ☕ 2. Decorator Pattern

### Core Concept:
> **Wraps an object dynamically to ADD new behavior/features without modifying its original class.**  
> *(Analogy: Plain Black Coffee $\rightarrow$ add Milk $\rightarrow$ add Sugar $\rightarrow$ add Whipped Cream. Base coffee same rehti hai, upar se features wrap hote jaate hain).*

---

### Classic Java I/O Example:
Java ka standard I/O library pure Decorator pattern par based hai:
```java
BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                new FileInputStream("data.txt")));
```
- `FileInputStream`: Raw bytes read karta hai.
- `InputStreamReader`: Bytes ko characters me wrap (decorate) karta hai.
- `BufferedReader`: Memory buffer aur line-by-line reading add karta hai.

---

### Spring Boot Production Example: `ContentCachingRequestWrapper`
- **Problem:** Standard `HttpServletRequest.getInputStream()` ko network stream se **sirf ek hi baar** read kiya ja sakta hai. Agar aapne Logging Filter me body read kar li, to Controller me payload empty milega!
- **Decorator Solution:** Spring Boot provide karta hai **`ContentCachingRequestWrapper`** (Decorator):
```java
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Decorate / Wrap original request
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        filterChain.doFilter(wrappedRequest, response);

        // Ab hum request body ko multiple times safely read aur log kar sakte hain!
        String requestBody = new String(wrappedRequest.getContentAsByteArray());
        log.info("Request Body: {}", requestBody);
    }
}
```

---

## ⚖️ 3. Master Comparison: Adapter vs Decorator vs Proxy

Yeh table interviewers ka favourite conceptual differentiator hai:

| Pattern | Primary Purpose | Interface Status | Real-World Spring Example |
| :--- | :--- | :--- | :--- |
| **Adapter** | **Interface badalna** (Incompatible interfaces ko connect karna). | **Changes the interface** to match client expectations. | `HandlerAdapter`, 3rd party SDK adapters. |
| **Decorator** | **Naye features add karna** dynamically. | **Keeps the same interface**, enhances functionality. | `ContentCachingRequestWrapper`, Java I/O streams. |
| **Proxy** | **Access control & orchestration** (Security, caching, transactions). | **Keeps the same interface**, controls access to real object. | `@Transactional`, `@Cacheable`, Spring AOP. |

---

### 💡 Pro Interview Tip:
> *"Adapter changes the INTERFACE.*  
> *Decorator keeps the same interface and ADDS FEATURES.*  
> *Proxy keeps the same interface and CONTROLS ACCESS."*  
> (Sirf yeh 3 lines bolne par interviewer 100% impress ho jata hai!)
