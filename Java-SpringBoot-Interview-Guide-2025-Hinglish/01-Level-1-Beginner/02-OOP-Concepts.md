# Level 1: OOP Concepts (Q10 - Q15)

---

### Q10: What is inheritance? Types of inheritance in Java?
**Answer (Hinglish):**

- **Inheritance Kya Hai:**  
  Inheritance Java ka ek fundamental mechanism hai jisme ek nayi class (child/subclass) purani existing class (parent/superclass) ke fields aur methods ko acquire (inherit) karti hai using `extends` keyword.
  - **IS-A Relationship:** Example: `Dog IS-A Animal`, `SavingsAccount IS-A BankAccount`.
  - **Main Benefit:** Code reusability and method overriding (runtime polymorphism).

**Types of Inheritance in Java:**

1. **Single Inheritance:**  
   Ek parent class aur ek child class.  
   `Class B extends Class A` (Allowed in Java).

2. **Multilevel Inheritance:**  
   Inheritance ki chain.  
   `Class C extends Class B`, aur `Class B extends Class A` (Allowed in Java).

3. **Hierarchical Inheritance:**  
   Ek parent class ko multiple child classes extend karti hain.  
   `Class B extends Class A` & `Class C extends Class A` (Allowed in Java).

4. **Multiple Inheritance (Classes ke through Not Allowed):**  
   Ek child class ke do parent classes hona (`Class C extends Class A, B`).  
   - **Kyu nahi allowed?** -> **The Diamond Problem**. Agar Class A aur Class B dono ke paas same signature ka `display()` method ho, to Class C confuse ho jayegi ki kiska method call karna hai.
   - **Solution in Java:** Multiple inheritance **Interfaces** ke through possible hai (`implements InterfaceA, InterfaceB`).

5. **Hybrid Inheritance:**  
   Hierarchical + Multiple inheritance ka combination (Sirf Interfaces ke through possible).

---

### Q11: What is polymorphism? Give examples
**Answer (Hinglish):**

Polymorphism do Greek words se bana hai: *Poly* (Many) + *Morph* (Forms). Matlab **"Ek hi action ko alag-alag tariko se perform karna"**.

Java me Polymorphism 2 types ka hota hai:

#### 1. Compile-Time Polymorphism (Static / Early Binding):
- **Mechanism:** Method Overloading.
- Compiler method signature (method name, parameter count/types) dekh kar compile time par decide karta hai ki kaun sa method call hoga.
```java
public class Calculator {
    // 2 integers add karega
    public int add(int a, int b) {
        return a + b;
    }
    // 3 integers add karega (Overloaded)
    public int add(int a, int b, int c) {
        return a + b + c;
    }
}
```

#### 2. Runtime Polymorphism (Dynamic / Late Binding):
- **Mechanism:** Method Overriding.
- JVM runtime par object ke actual instance type ko dekh kar call resolve karta hai.
```java
class Payment {
    void pay() {
        System.out.println("Processing generic payment");
    }
}

class UpiPayment extends Payment {
    @Override
    void pay() {
        System.out.println("Payment via UPI QR code");
    }
}

public class Main {
    public static void main(String[] args) {
        Payment p = new UpiPayment(); // Parent reference, Child object
        p.pay(); // Output: Payment via UPI QR code (Resolved at Runtime)
    }
}
```

---

### Q12: What is encapsulation and abstraction?
**Answer (Hinglish):**

Dono OOPs ke pillars hain, lekin inka focus alag hota hai:

| Feature | Encapsulation (Data Protection) | Abstraction (Complexity Hiding) |
| :--- | :--- | :--- |
| **Main Focus** | *Data security & internal state protection*. | *Implementation hide karna aur sirf interface provide karna*. |
| **Kaise hota hai** | Variables ko `private` karo, `public` getters/setters do. | `abstract class` aur `interface` use karke. |
| **Question Answered** | "Internal data ko unauthorized access se kaise bachayein?" | "User ko internal complex logic se kaise bacha kar simple control dein?" |
| **Real World Analogy** | **Medical Capsule:** Medicine powder capsule ke andar packed hai. | **Car Dashboard / ATM:** Screen par Withdraw button dabao, cash bahar. Machine ke internal hardware motor ka pata nahi hota. |

**Code Example:**
```java
// Abstraction: User ko sirf contract dikhta hai
public interface PaymentGateway {
    void processPayment(double amount);
}

// Encapsulation: State private hai aur safely modify hoti hai
public class RazorpayGateway implements PaymentGateway {
    private String apiKey; // encapsulated

    public RazorpayGateway(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void processPayment(double amount) {
        // complex network handshake & encryption logic hidden inside
        System.out.println("Paid " + amount + " via Razorpay");
    }
}
```

---

### Q13: What is method overloading vs method overriding?
**Answer (Hinglish):**

| Difference Point | Method Overloading | Method Overriding |
| :--- | :--- | :--- |
| **Location** | Same class ke andar hota hai. | Parent-Child (Inheritance) classes me hota hai. |
| **Method Name** | Same | Same |
| **Parameters** | **Must be different** (type, number, ya order). | **Must be exactly same**. |
| **Return Type** | Can be same or different (return type badal kar overload nahi hota). | Must be same or covariant (subtype). |
| **Binding Time** | Compile-time (Static binding). | Runtime (Dynamic binding). |
| **Private/Static** | Static/Private methods overload ho sakte hain. | Static/Private methods override **nahi** ho sakte! (Static methods me method hiding hoti hai). |

> **⚠️ Common Interview Trap:**  
> *"Kya hum sirf return type change karke method overload kar sakte hain?"*  
> **Jawab:** **Nahi!** Compiler error dega: `method already defined`. Parameters me change hona mandatory hai.

---

### Q14: When to use Interface vs Abstract Class?
**Answer (Hinglish):**

Yeh interviewers ka favourite architecture question hai.

| Criteria | Abstract Class | Interface |
| :--- | :--- | :--- |
| **Relationship** | **"IS-A"** (Tight coupling, closely related classes). | **"CAN-DO" / Contract** (Loose coupling, unrelated classes). |
| **Multiple Inheritance** | Ek class sirf 1 abstract class extend kar sakti hai. | Ek class multiple interfaces implement kar sakti hai. |
| **State (Variables)** | Instance variables, non-final, static sab rakh sakti hai. | Variables hamesha `public static final` (constants) hote hain. |
| **Constructor** | Constructors ho sakte hain (child ke super() se call hote hain). | Constructors **nahi** ho sakte. |
| **Default Methods** | Full concrete methods rakh sakti hai. | Java 8+ se `default` aur `static` methods support karta hai. |

**When to choose what:**
- **Abstract Class tab choose karein:** Jab closely related classes ke beech code share karna ho, common state/variables rakhne hon, aur partial implementation deni ho (e.g., `BaseController`, `AbstractRepositoryTemplate`).
- **Interface tab choose karein:** Jab completely unrelated classes ko ek common behavior enforce karna ho (e.g., `Serializable`, `Comparable`, `Payable`). Spring architecture me service layer hamesha interfaces par depend karti hai loose coupling ke liye.

---

### Q15: What is a Marker Interface? Why use it?
**Answer (Hinglish):**

- **Definition:** Marker Interface (ya Tag Interface) ek aisa interface hota hai jisme **koi bhi method ya constant variable nahi hota (Empty Interface)**.
- **Example in JDK:**
  - `java.io.Serializable`
  - `java.lang.Cloneable`
  - `java.util.RandomAccess`

```java
package java.io;
public interface Serializable {
    // Khali interface, zero methods
}
```

- **Kyu use karte hain (Why use it):**
  - Yeh JVM ya compiler ko ek **Signal / Metadata** deta hai ki is class ke objects ke paas koi special capability hai.
  - E.g., Jab JVM dekhta hai ki object `Serializable` implement kar raha hai, tabhi wo use byte stream me convert karke disk ya network par write hone deta hai. Warna runtime par `NotSerializableException` throw kar deta hai.

> **Modern Java Note:**  
> Java 5 ke baad Marker Interfaces ki jagah **Annotations** (`@Entity`, `@Deprecated`, `@Override`) preferred hain, kyunki annotations me parameters pass kar sakte hain aur target restrict kar sakte hain.
