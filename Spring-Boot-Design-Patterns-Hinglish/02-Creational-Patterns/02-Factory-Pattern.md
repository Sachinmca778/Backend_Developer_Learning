# Creational Pattern: Factory Pattern in Spring Boot

---

### 1. Core Concept (Factory Pattern Kya Hai?)
**Answer (Hinglish):**

> **A Factory creates objects for you. You ask for WHAT you need, not HOW to build it.**

Client ko object ke concrete instantiation logic (jaise `new EmailSender()`) ki chinta nahi karni padti. Client sirf type ya requirement batata hai (e.g., `type = "UPI"`), aur Factory class corresponding fully configured object create/provide karke de deti hai.

```
                  ┌────────────────────────┐
                  │        FACTORY         │
Client ──────────>│ (Hides object creation │──────────> [ Specific Object Instance ]
(Says: "UPI")     │  and dependency logic) │            (e.g., UpiPayment)
                  └────────────────────────┘
```

---

### 2. Spring Me Factory Kahan Hai?

1. **`BeanFactory` & `ApplicationContext`:**  
   Spring Framework ka heart khud **Duniyaka sabse bada Factory** hai! Jab aap `ctx.getBean(PaymentService.class)` call karte ho, to Spring IoC container ek master factory ki tarah bean dhoond kar ya create karke deta hai.
2. **`@Bean` Methods:**  
   Configuration class ke andar har ek `@Bean` method ek **Factory Method** ki tarah kaam karta hai jo object instantiate karke container ko handover karta hai:
   ```java
   @Configuration
   public class AppConfig {
       @Bean // Factory Method
       public RestTemplate restTemplate() {
           return new RestTemplate();
       }
   }
   ```

---

### 3. Real Project Architecture: Notification Factory

Ek real-world scenario dekhte hain jahan user ko Email, SMS ya WhatsApp bhejna hota hai.

#### ❌ The Bad Way (Without Factory - The If-Else Mess):
```java
public void notifyUser(String type, String message) {
    NotificationSender sender;
    if ("EMAIL".equalsIgnoreCase(type)) {
        sender = new EmailSender(); // ❌ Tight coupling, new keyword everywhere!
    } else if ("SMS".equalsIgnoreCase(type)) {
        sender = new SmsSender();
    } else if ("WHATSAPP".equalsIgnoreCase(type)) {
        sender = new WhatsAppSender();
    } // ❌ Har naye channel ke liye yeh if-else modify karna padega (Violates Open/Closed Principle)
    sender.send("user@test.com", message);
}
```

---

#### ✅ The Spring Boot Production Way (With Factory + Spring DI Magic):

Spring Boot me Factory pattern implement karne ka ek magical feature hota hai: **Spring auto-injects all beans of an interface into a `Map<String, Interface>` where the bean name becomes the key!**

#### Step 1: Define Interface
```java
public interface NotificationSender {
    void send(String to, String message);
}
```

#### Step 2: Implementations with Named Beans
```java
@Component("EMAIL")
public class EmailSender implements NotificationSender {
    @Override
    public void send(String to, String message) {
        System.out.println("Sending Email to " + to + ": " + message);
    }
}

@Component("SMS")
public class SmsSender implements NotificationSender {
    @Override
    public void send(String to, String message) {
        System.out.println("Sending SMS to " + to + ": " + message);
    }
}

@Component("WHATSAPP")
public class WhatsAppSender implements NotificationSender {
    @Override
    public void send(String to, String message) {
        System.out.println("Sending WhatsApp to " + to + ": " + message);
    }
}
```

#### Step 3: The Spring Factory Class
```java
@Component
public class NotificationFactory {

    // Spring Boot automatically populates this map with:
    // {"EMAIL" -> EmailSender, "SMS" -> SmsSender, "WHATSAPP" -> WhatsAppSender}
    private final Map<String, NotificationSender> senders;

    public NotificationFactory(Map<String, NotificationSender> senders) {
        this.senders = senders;
    }

    public NotificationSender getSender(String type) {
        NotificationSender sender = senders.get(type.toUpperCase());
        if (sender == null) {
            throw new IllegalArgumentException("Unknown notification type: " + type);
        }
        return sender;
    }
}
```

#### Step 4: Clean Usage in Service
```java
@Service
public class OrderService {

    private final NotificationFactory notificationFactory;

    public OrderService(NotificationFactory notificationFactory) {
        this.notificationFactory = notificationFactory;
    }

    public void processOrder(String notificationType) {
        // Zero if-else!
        NotificationSender sender = notificationFactory.getSender(notificationType);
        sender.send("customer@domain.com", "Order Confirmed!");
    }
}
```

---

### 4. Is Approach Ka Sabse Bada Fayda:
Agar kal company me **"SLACK"** notifications add karni hon:
- Aapko sirf ek nayi class `@Component("SLACK") public class SlackSender implements NotificationSender` banani hai.
- Factory class ya OrderService me **1 line bhi code change nahi karna padega**! (100% Open for extension, Closed for modification).

---

### 💡 Pro Interview Tip:
> **Interviewers love this question:** *"How do you implement Factory Pattern without writing a switch-case or if-else ladder?"*  
> **Your Answer:** *"By leveraging Spring's built-in `Map<String, Interface>` constructor injection. Spring automatically registers all beans implementing the interface into the map with their bean names as keys. This keeps the factory completely decoupled and dynamic!"*
