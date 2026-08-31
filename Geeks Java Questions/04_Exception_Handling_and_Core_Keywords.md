# Part 4: Exception Handling & Core Keywords

---

## 151. Explain Fail-Fast iterator and Fail-Safe iterator along with examples for each.

### 1. Fail-Fast Iterator:

**Matlab:** Jab aap kisi collection ko iterate kar rahe hon aur iteration ke dauran original collection modify (element add/remove) ho jaye (bina iterator ke apne `remove()` method ke), toh iterator immediately **`ConcurrentModificationException`** throw karta hai.

- **Mechanism:** Internal **`modCount` (Modification Count)** value check karta hai. Iteration ke beech `modCount != expectedModCount` milte hi fail ho jata hai.
- **Examples:** `ArrayList`, `HashSet`, `HashMap` ke iterators.

```java
List<String> list = new ArrayList<>(List.of("A", "B", "C"));
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String val = it.next();
    if (val.equals("B")) {
        list.add("D"); // Concurrent modification -> Throws ConcurrentModificationException!
    }
}
```

### 2. Fail-Safe Iterator:

**Matlab:** Iteration ke dauran collection mein changes hone par exception throw **NAHI** karta.

- **Mechanism:** Original collection ki **Copy (Snapshot)** par iterate karta hai ya lock-free concurrent structure operate karta hai.
- **Examples:** `CopyOnWriteArrayList`, `ConcurrentHashMap`.

```java
List<String> list = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String val = it.next();
    if (val.equals("B")) {
        list.add("D"); // Allowed! No exception.
    }
}
```

---

## 152. What is Exception Handling?

**Matlab:** Exception Handling ek mechanism hai jo runtime errors (jaise `ClassNotFoundException`, `IOException`, `ArithmeticException`) ko gracefully handle karke application flow ko crash hone se bachata hai.

---

## 153. How many types of exceptions can occur in a Java program?

Broadly 2 categories of Exceptions:

1. **Checked Exceptions (Compile-Time Exceptions):** Compiler dwara force kiye jaane wale exceptions (e.g., `IOException`, `SQLException`).
2. **Unchecked Exceptions (Runtime Exceptions):** Runtime par occur hone wale exceptions (e.g., `NullPointerException`, `ArithmeticException`).

---

## 154. Difference between Error and Exception.

| Property | Error | Exception |
|---|---|---|
| **Category** | `java.lang.Error` | `java.lang.Exception` |
| **Cause** | System-level resources failure (JVM out of memory, stack overflow). | Application-level conditions/bugs (File missing, null reference). |
| **Recoverability** | **Irrecoverable** (Program handle nahi kar sakta). | **Recoverable** (`try-catch` blocks se handle kiya ja sakta hai). |
| **Examples** | `OutOfMemoryError`, `StackOverflowError`. | `IOException`, `NullPointerException`. |

---

## 155. Explain the hierarchy of Java Exception classes.

```

                    Throwable (java.lang)
                              |
         +--------------------+--------------------+
         v                                         v
   Exception (Class)                         Error (Class)
         |                                   • OutOfMemoryError
  +------+----------------------+            • StackOverflowError
  v                             v
Checked Exceptions      RuntimeException (Unchecked)
• IOException           • NullPointerException
• SQLException          • ArithmeticException
• ClassNotFoundException • ArrayIndexOutOfBoundsException
```

---

## 156. Explain Runtime Exceptions.

**Matlab:** `java.lang.RuntimeException` ke sub-classes ko **Unchecked Exceptions** kehte hain. Compiler compile time par inke handle hone ki checking enforce nahi karta. Yeh bad logic / programming flaws ki wajah se aate hain.

---

## 157. What is `NullPointerException` (NPE)?

**Matlab:** NPE tab throw hota hai jab aap kisi variable par action perform karne ki koshish karte ho jo current mein **`null` object reference** ko point kar raha hota hai.

```java
String str = null;
int len = str.length(); // Throws NullPointerException!
```

---

## 158. When is `ArrayStoreException` thrown?

**Matlab:** Tab throw hota hai jab aap kisi object array (`Object[]`) mein incompatible type ka element store karne ki koshish karte ho jiska runtime type match nahi karta.

```java
Object[] strArray = new String[5];
strArray[0] = Integer.valueOf(100); // Throws ArrayStoreException at runtime!
```

---

## 159. Difference between Checked Exception and Unchecked Exception.

| Property | Checked Exception | Unchecked Exception |
|---|---|---|
| **Compilation** | Compiler forces handling (`try-catch` or `throws`). | Compiler does NOT force handling. |
| **Inheritance** | Directly extends `java.lang.Exception`. | Extends `java.lang.RuntimeException`. |
| **Occur Time** | Compile Time awareness. | Pure Runtime occurrence. |

---

## 160. What is the base class for `Error` and `Exception`?

**Matlab:** Base superclass **`java.lang.Throwable`** hai.

---

## 161. Is it necessary that each `try` block must be followed by a `catch` block?

**Matlab:** **Nahi!** Mandatory nahi hai ki `try` ke baad `catch` hi ho. A `try` block can be followed by:
1. Only `catch` block(s).
2. Only `finally` block (`try { ... } finally { ... }`).
3. Both `catch` and `finally` blocks.

---

## 162. What is Exception Propagation?

**Matlab:** Jab kisi method mein exception occur hota hai aur wahan handle nahi hota, toh woh method Call Stack mein backward propagate hokar Calling Method ke paas chala jata hai. Is stack traversal ko **Exception Propagation** kehte hain.

> Unchecked exceptions automatically propagate hote hain call stack mein.

---

## 163. What will happen if you put `System.exit(0)` in `try` or `catch` block? Will `finally` block execute?

**Matlab:** **`finally` block EXECUTE NAHI HOGA!**

`System.exit(0)` JVM Process ko immediate terminate kar deta hai, isliye JVM shutdown hone ki wajah se `finally` block execute hone ka mauka hi nahi milta.

---

## 164. What do you understand by Object Cloning and how do you achieve it in Java?

**Matlab:** Object Cloning existing object ka exact duplicate copy create karne ka process hai.

### Steps to Achieve Cloning:

1. Class ko **`java.lang.Cloneable`** marker interface implement karna padta hai.
2. `Object` class ke **`clone()`** method ko override karna padta hai.

```java
public class Person implements Cloneable {
    String name;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone(); // Shallow copy
    }
}
```

### Shallow vs Deep Copy:

- **Shallow Copy:** Primitive values duplicate hoti hain, par nested objects ke references copy hote hain (shared object).
- **Deep Copy:** Nested objects ki bhi alag new object copy create hoti hai.

---

## 165. How do exceptions affect the program if it doesn't handle them?

**Matlab:** Exception unhandled rahne par Java execution thread abnormally terminate (**crash**) ho jata hai, error stack trace console par print hota hai, aur calling code execution stop ho jata hai.

---

## 166. What is the use of `final` keyword?

`final` keyword in 3 places use hota hai:

1. **Final Variable:** Variable **Constant** ban jata hai (Re-assignment illegal).
2. **Final Method:** Method **Overriding block** ho jati hai.
3. **Final Class:** Class **Inheritance block** ho jati hai (Cannot be extended - e.g., `java.lang.String`).

---

## 167. What purpose do `final`, `finally`, and `finalize` fulfill?

| Keyword / Term | Category | Purpose |
|---|---|---|
| **`final`** | Modifier | Variables constant, methods non-overridable, classes non-inheritable banane ke liye. |
| **`finally`** | Block | Exception Handling mein Mandatory Cleanup Code (File Close, DB Disconnect) run karne ke liye. |
| **`finalize()`** | Method | `java.lang.Object` ka deprecated method jo GC se pehle cleanup triggers karta tha (Deprecated in Java 9). |

---

## 168. What is the difference between `this()` and `super()` in Java?

- **`this()`:** Same class ke kisi dusre constructor ko invoke karta hai (Constructor Chaining inside class).
- **`super()`:** Parent class ke constructor ko invoke karta hai (Parent Constructor Invocation).
- Both MUST be the **first statement** in a constructor.
