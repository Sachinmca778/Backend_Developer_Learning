# Level 1: String Handling (Q16 - Q20)

---

### Q16: What is the difference between String, StringBuilder, and StringBuffer?
**Answer (Hinglish):**

Java me strings handle karne ke 3 main classes hain:

| Feature | String | StringBuilder | StringBuffer |
| :--- | :--- | :--- | :--- |
| **Mutability** | **Immutable** (Modify nahi ho sakta, naya object banta hai) | **Mutable** (Same memory me modify ho jata hai) | **Mutable** (Same memory me modify ho jata hai) |
| **Thread-Safety** | **Thread-safe** (Kyunki immutable hai) | **Not Thread-safe** (Methods synchronized nahi hain) | **Thread-safe** (Methods synchronized hain) |
| **Performance** | String concatenation (`+`) loop me bohot slow hota hai | **Sabse fast** (No thread-safety overhead) | Slower than StringBuilder (Sync lock overhead) |
| **Introduction** | Java 1.0 | Java 1.5 | Java 1.0 |
| **Memory** | Heap & String Constant Pool | Heap | Heap |

**Industry Best Practice:**
- Agar string constant hai aur change nahi honi: **`String`** use karo.
- Single-threaded environment me repeated modifications ya loop concatenation hai: **`StringBuilder`** use karo.
- Multi-threaded environment me concurrent updates ho rahe hain: **`StringBuffer`** use karo.

---

### Q17: Difference between creating String with literal and new operator
**Answer (Hinglish):**

Java me String create karne ke do tarike hain:

#### 1. String Literal (`String s = "Java";`)
- Java JVM check karta hai **String Constant Pool (SCP)** me:
  - Agar `"Java"` pool me already exist karta hai, to wahi purana reference return kar deta hai (naya object nahi banta).
  - Agar pool me nahi hai, to pool me naya string object create karke reference de deta hai.
- **Memory Saved:** Reusable pool ki wajah se memory bachti hai.

#### 2. `new` Operator (`String s = new String("Java");`)
- `new` keyword hamesha **Heap Memory** me ek fresh naya object allocate karta hai chahe content same hi kyu na ho.
- Saath hi, agar `"Java"` SCP pool me nahi tha, to SCP me bhi ek copy banti hai.
- Yani total **2 objects** ban sakte hain (1 Heap me, 1 Pool me).

```java
String s1 = "Hello";
String s2 = "Hello";
String s3 = new String("Hello");

System.out.println(s1 == s2); // true (Dono pool ke same object ko point kar rahe hain)
System.out.println(s1 == s3); // false (s3 Heap me alag address par hai)
System.out.println(s1.equals(s3)); // true (Content dono ka "Hello" hai)
```

---

### Q18: What is String pool and how does it work?
**Answer (Hinglish):**

- **String Constant Pool (SCP / String Pool) Kya Hai:**  
  Yeh Java Heap memory ke andar ek special reserved memory area hota hai (Java 7 se pehle PermGen me tha, Java 7 ke baad Heap me move ho gaya).
- **Kaise Work Karta Hai:**
  - Jab bhi aap literal se string banate ho jaise `String str = "apple"`, JVM pool me check karta hai.
  - Internally, String Pool ek **Hashtable** ki tarah maintain hota hai.
  - Agar string already hash table me match ho gayi, to exist karta hua pointer return ho jata hai.
  - Agar nahi mili, to entry create hoti hai.

```
       HEAP MEMORY
  +--------------------------------------------+
  |                                            |
  |   +------------------------------------+   |
  |   |    String Constant Pool (SCP)      |   |
  |   |                                    |   |
  |   |   s1 ----> [ "Apple" ] <---- s2    |   |
  |   +------------------------------------+   |
  |                                            |
  |   s3 --------> [ "Apple" (new Object)]     |
  +--------------------------------------------+
```

> **Interning (`str.intern()`):**  
> Agar heap wale object ka pool reference chahiye, to aap `s3.intern()` call kar sakte ho. Yeh pool me check karke pool ka reference de deta hai.

---

### Q19: What are the advantages of String pool?
**Answer (Hinglish):**

1. **Huge Memory Savings:**  
   Enterprise applications me 30% se 50% memory Strings occupy karti hain. Agar hajaro users ki country "India" hai, to pool ki wajah se lakhon objects nahi bante, sirf ek single pool instance reuse hota hai.
2. **Fast Comparison via `==` when Interned:**  
   Agar strings interned hain, to direct memory address check (`==`) fast hota hai compared to character-by-character equals.
3. **Optimized Garbage Collection:**  
   Pool ke objects ko bar-bar garbage collect aur allocate karne ki jarurat nahi padti, jisse GC pause time kam hota hai.

---

### Q20: Why String is immutable?
**Answer (Hinglish):**

Java me `String` class ko **`final`** banaya gaya hai aur iski internal `char[]` (ya Java 9+ me `byte[]`) array ko `private final` rakha gaya hai.

**String ko Immutable banane ke 4 critical reasons:**

1. **String Constant Pool (SCP) Ka Concept:**
   - Agar string mutable hoti aur 10 reference variables ek hi `"Admin"` pool object ko point kar rahe hote, aur agar ek variable ne value change karke `"User"` kar di, to baki sabhi 9 references ke liye value silently corrupt ho jati!
2. **Security:**
   - Network connections, database URLs, usernames, passwords, file paths sab Strings me store hote hain.
   - Agar String mutable hoti, to koi malicious code validation ke baad bhi string ki value memory me modify kar sakta tha (Time-of-Check to Time-of-Use security flaw).
3. **Thread-Safety (Concurrency):**
   - Immutable objects naturally thread-safe hote hain. Unhe kisi synchronization lock ki jarurat nahi hoti. Multiple threads bina kisi race-condition ke safely string read kar sakte hain.
4. **HashMap Caching (HashCode Cache):**
   - String ka `hashCode()` pehli baar calculate hone ke baad internal variable `hash` me cache ho jata hai.
   - Kyunki string change nahi ho sakti, iska hash code hamesha guaranteed same rahega. Is wajah se HashMap aur HashSet me String sabse popular aur fast key banti hai.
