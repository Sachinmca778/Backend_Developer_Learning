# Part 2: Strings, Arrays & Object-Oriented Programming (OOPs)

---

## 46. What is String in Java?

**Matlab:** Java mein `String` ek object hai jo characters ke sequence ko represent karta hai (`java.lang.String`). Java mein String primitive type nahi, balki ek Class hai.

### Key Characteristics:

1. **Immutable:** Once created, String object ka state/content change nahi kiya ja sakta.
2. **Backing Array:** Internal level par String character array (`char[]` / `byte[]` in Java 9+) hold karta hai.
3. **String Pool:** Memory optimization ke liye String literals String Constant Pool (SCP) mein manage hote hain.

---

## 47. What are the differences between String and StringBuffer?

| Feature | String | StringBuffer |
|---|---|---|
| **Mutability** | **Immutable** (Content update nahi ho sakta). | **Mutable** (Same memory location par update hota hai). |
| **Performance** | Frequent changes par slow (Naye objects bante rahte hain Heap mein). | Fast (Existing object in-place modify hota hai). |
| **Thread Safety** | Thread-safe by default (Immutability ki wajah se). | Thread-safe (Methods **`synchronized`** hain). |
| **Memory Allocation** | SCP / Heap memory storage. | Heap memory storage. |

---

## 48. What are the differences between StringBuffer and StringBuilder?

| Feature | StringBuffer | StringBuilder |
|---|---|---|
| **Thread Safety** | **Thread-safe** (Methods synchronized hain). | **Not Thread-safe** (Methods synchronized nahi hain). |
| **Performance** | Comparative Slower (Overhead of synchronization locks). | **Faster** (No locking overhead). |
| **Introduction** | Java 1.0 | Java 1.5 |
| **Use Case** | Multi-threaded environment jahan shared string modifies ho rahi ho. | Single-threaded environment (99% cases mein prefer kiya jata hai). |

---

## 49. Which among String, StringBuilder, or StringBuffer should be preferred when there are a lot of updates required to be done in data?

**Matlab:** Agar data mein continuously modifications (Concatenation, Append, Insert, Delete) ho rahe hain:
1. **Single-threaded environment (Standard):** **`StringBuilder`** prefer karna chahiye kyunki yeh fastest execution deta hai (bina synchronization lock cost ke).
2. **Multi-threaded environment:** **`StringBuffer`** prefer karna chahiye thread safety assurance ke liye.
3. **`String` avoid karna chahiye** kyunki har `+` concatenation operation par naya String object Heap mein allocate hota hai jisse Garbage Collector overload hota hai.

---

## 50. Why is StringBuffer called mutable?

**Matlab:** `StringBuffer` ko mutable isliye kehte hain kyunki iske memory internal buffer (`char[]`) ki contents ko bina koi naya object allocate kiye modify kiya ja sakta hai.

```java
StringBuffer sb = new StringBuffer("Hello");
sb.append(" World"); // Same object in memory is updated to "Hello World"
```

---

## 51. How is the creation of a String using `new()` different from that of a literal?

- **String Literal (`String s = "Java"`):**
  - Search SCP. If found -> returns SCP reference. If not found -> creates 1 object in SCP.
- **Using `new` (`String s = new String("Java")`):**
  - Always creates **1 Object in Normal Heap Memory**.
  - Additionally, checks if `"Java"` exists in SCP; if absent, creates **1 Object in SCP** as well.

---

## 52. What is an Array in Java?

**Matlab:** Array ek indexed data structure hai jo same data type ke fixed-size elements ko contiguous memory location par store karta hai.

```java
int[] arr = new int[5]; // Fixed size of 5 elements
```

---

## 53. On which memory are arrays created in Java?

**Matlab:** Java mein Arrays **Objects** hote hain, isliye Array object hamesha **Heap Memory** mein create hota hai, chahe array primitive values hold karta ho ya reference values. Variable ka reference Stack Memory mein store hota hai.

---

## 54. What are the types of Array in Java?

1. **Single-Dimensional Array:** Linearly elements arrange hote hain (`int[] arr`).
2. **Multi-Dimensional Array:** Arrays of arrays (`int[][] matrix` - Rows & Columns matrix structure).

---

## 55. Why does the Java array index start with 0?

**Matlab:** Array index **Offset (distance)** represent karta hai array ke Base Address se.

**Formula:**
`Address = Base_Address + (Index * Element_Size)`

Pehle element ka offset Base Address se `0` distance par hota hai (`Base + 0*Size = Base Address`), isliye index `0` se start hota hai.

---

## 56. What is the difference between `int array[]` and `int[] array`?

- Both syntax-wise valid hain in Java.
- **`int[] array` (Java Style - Recommended):** Code readability improve karta hai kyunki type (`int[]`) aur variable name (`array`) clean demark hote hain.
- **`int array[]` (C/C++ Style):** Legacy C/C++ developers ke transition support ke liye valid rakha gaya tha.

---

## 57. How to copy an array in Java?

Java mein Arrays copy karne ke 4 tarike:

1. **`System.arraycopy(src, srcPos, dest, destPos, length)`:** Native method, sabse fast.
2. **`Arrays.copyOf(arr, newLength)`:** Utility method, new array instantiate karta hai.
3. **`Arrays.copyOfRange(arr, from, to)`:** Range specified copy.
4. **`arr.clone()`:** Object clone method.

---

## 58. What do you understand by Jagged Array?

**Matlab:** Jagged Array ek multi-dimensional array hota hai jiske individual sub-arrays (rows) ki size/length alag-alag (unequal) ho sakti hai.

```java
public class JaggedArrayDemo {
    public static void main(String[] args) {
        int[][] jagged = new int[3][];
        jagged[0] = new int[2]; // Row 0 has 2 elements
        jagged[1] = new int[4]; // Row 1 has 4 elements
        jagged[2] = new int[1]; // Row 2 has 1 element
    }
}
```

---

## 59. Is it possible to make an array `volatile`?

**Matlab:** Haan, array reference variable ko `volatile` declare kar sakte hain (`volatile int[] arr`).

> **Warning:**
> **Important Catch:** `volatile` keyword sirf **Array Reference** ki visibility guarantee karta hai (agar Poora array dusre array se replace ho jaye). Array ke **individual elements (`arr[i]`) volatile nahi hote**! Individual element visibility ke liye `AtomicIntegerArray` use kiya jata hai.

---

## 60. What are the advantages and disadvantages of an array?

### Advantages:

- **Fast Random Access:** `O(1)` time complexity via index lookups.
- **Cache Locality:** Contiguous memory allocation CPU cache performance improve karti hai.

### Disadvantages:

- **Fixed Size:** Array length runtime par dynamic grow nahi ho sakti.
- **Homogeneous Data:** Sirf same type ke elements store hote hain.
- **Costly Insertion/Deletion:** Intermediate elements shift karne par `O(N)` cost aati hai.

---

## 61. What is Object-Oriented Paradigm?

**Matlab:** Object-Oriented Programming (OOP) ek software design architecture paradigm hai jo code ko real-world Entities (Objects) aur unke Behaviors ke form mein structure karta hai, taaki modularity, reusability aur scalability achieve ho sake.

---

## 62. What are the main concepts (4 Pillars) of OOPs in Java?

1. **Encapsulation:** Data hiding and bundling fields and methods together using private fields and getters/setters.
2. **Abstraction:** Hiding internal implementation details and exposing only necessary functionality to the user (via Interfaces & Abstract Classes).
3. **Inheritance:** Enabling a class to acquire fields and methods of a parent class (`extends`) for code reusability.
4. **Polymorphism:** Ability to present the same interface in different underlying forms (Method Overloading & Overriding).

---

## 63. Difference between Object-Oriented and Object-Based Languages.

| Feature | Object-Oriented (Java, C++) | Object-Based (JavaScript, VB Script) |
|---|---|---|
| **Inheritance & Polymorphism** | Full support (Inheritance, Overriding). | Partial or No Inheritance support. |
| **Built-in Objects** | Custom classes & full OO concepts. | Premade objects encapsulation. |

---

## 64. How is `new` operator different from `newInstance()` method?

- **`new` Operator:** Compile-time static creation. Class name known hona chahiye (`Student s = new Student();`).
- **`newInstance()` (Reflection API):** Runtime dynamic class instantiation. Class name String runtime parameter se load ho sakta hai (`Class.forName(className).getDeclaredConstructor().newInstance()`).

---

## 65. What are Classes in Java?

**Matlab:** Class ek **Blueprint / Template** hai jo objects ke state (properties/fields) aur behavior (methods) ko define karti hai. Class logical entity hoti hai (memory assign tab hoti hai jab object banta hai).

---

## 66. Difference between Static (Class) Method and Instance Method.

| Property | Static Method | Instance Method |
|---|---|---|
| **Association** | Class se associated hota hai (`static`). | Object instance se associated hota hai. |
| **Invocation** | Class Name se call hota hai (`Math.sqrt()`). | Object reference se call hota hai (`obj.display()`). |
| **`this` / `super`** | `this` aur `super` keywords accessible nahi hote. | Accessible hote hain. |
| **Access Rules** | Sirf static fields aur static methods access kar sakta hai. | Both static and non-static members access kar sakta hai. |

---

## 67. What is `this` keyword in Java?

**Matlab:** `this` ek reference variable hai jo current class ke current object instance ko point karta hai.

### Use Cases:

1. Instance fields ko local variable shadowing se differentiate karne ke liye (`this.name = name`).
2. Constructor chaining ke liye (`this(10, 20)`).
3. Current instance ko parameter ke form mein pass karne ke liye.

---

## 68. What is `super` keyword in Java?

**Matlab:** `super` reference variable hai jo immediate parent (superclass) object ko point karta hai.

### Use Cases:

1. Parent class ke hidden fields ko access karne ke liye (`super.variableName`).
2. Parent class ke overridden method ko invoke karne ke liye (`super.methodName()`).
3. Parent class constructor invoke karne ke liye (`super()`).

---

## 69. Can we use `this()` and `super()` both in the same constructor?

**Matlab:** **Nahi!** Dono `this()` aur `super()` ko ek hi constructor mein statement ke roop mein ek saath call nahi kiya ja sakta, kyunki Java Language Specification ke anusar dono mein se kisi ek ka **Constructor ki FIRST LINE par hona mandatory hai**!

---

## 70. What is Constructor Chaining?

**Matlab:** Ek constructor se dusre constructor ko call karne ke process ko **Constructor Chaining** kehte hain (`this()` same class mein, ya `super()` parent class mein).

---

## 71. Can a class extend more than one class in Java? (Multiple Inheritance)

**Matlab:** **Nahi!** Java classes mein Direct Multiple Inheritance support nahi karta (Diamond Problem ko avoid karne ke liye). Ek class ek hi parent class ko `extends` kar sakti hai. Multiple inheritance Interfaces (`implements`) dwara achieve hota hai.

---

## 72. What is the Diamond Problem in Multiple Inheritance?

**Matlab:** Suppose Class B aur Class C dono Class A ko extend karti hain aur same `display()` method override karti hain. Agar Class D dono Class B aur C ko extend karti hai, toh Class D ke object par `display()` call hone par compiler confused ho jata hai ki B ka method execute kare ya C ka. Is ambiguity ko **Diamond Problem** kehte hain.

---

## 73. What is Access Modifier in Java?

**Matlab:** Access Modifiers keywords hain jo Classes, Constructors, Fields aur Methods ki accessibility / visibility scope control karte hain.

| Access Modifier | Same Class | Same Package | Subclass (Other Package) | World (Anywhere) |
|---|---|---|---|---|
| **`private`** | Yes | No | No | No |
| **`default` (package-private)** | Yes | Yes | No | No |
| **`protected`** | Yes | Yes | Yes | No |
| **`public`** | Yes | Yes | Yes | Yes |

---

## 74. What is a Constructor in Java?

**Matlab:** Constructor ek special block of code hai jo Object initialization ke waqt automatically call hota hai. Iska name Class name ke exactly identical hota hai aur iska koi return type (bina `void` ke) nahi hota.

---

## 75. What happens if you don't provide a constructor in a class?

**Matlab:** Agar aap class mein koi bhi constructor explicitly nahi likhte, toh Java compiler (`javac`) automatically ek **Default No-Arg Constructor** generate kar deta hai.

> **Note:**
> Agar aapne koi bhi custom constructor (e.g., Parameterized) likh diya, toh compiler default constructor generate **nahi** karega!

---

## 76. How many types of constructors are used in Java?

1. **Default / No-Arg Constructor:** Bina parameters wala constructor.
2. **Parameterized Constructor:** Parameters accept karke instance fields initialize karne wala.
3. **Copy Constructor:** Existing object ki state copy karke naya object create karne wala.

---

## 77. What is the purpose of Default Constructor?

**Matlab:** Instance variables ko unki default initial values (`0`, `null`, `false`) assign karna aur object memory setup ready karna.

---

## 78. What do you understand by Copy Constructor in Java?

**Matlab:** Java mein C++ ki tarah built-in copy constructor nahi hota, par hum same class ke object ko argument accept karne wala constructor banakar Copy Constructor behavior achieve karte hain.

```java
public class Employee {
    String name;
    int age;

    // Parameterized Constructor
    public Employee(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Copy Constructor
    public Employee(Employee other) {
        this.name = other.name;
        this.age = other.age;
    }
}
```

---

## 79. Where and how can you use a private constructor?

### Use Cases:

1. **Singleton Design Pattern:** Object creation restrict karne ke liye (`getInstance()` static method se single object return hota hai).
2. **Utility / Helper Classes:** Class jisme sirf static utility methods hon (`Math` class), object instantiation block karne ke liye private constructor rakhte hain.

---

## 80. Difference between Constructors and Methods.

| Property | Constructor | Method |
|---|---|---|
| **Purpose** | Object state initialize karna. | Object capabilities / behavior execute karna. |
| **Return Type** | No return type (not even `void`). | Must specify return type (`void`, `int`, etc.). |
| **Name** | Must match Class Name. | Can be any valid identifier name. |
| **Invocation** | `new` keyword instantiation par automatically. | Explicitly object/class reference se call hota hai. |

---

## 81. What is an Interface in Java?

**Matlab:** Interface 100% abstract blueprint hota hai (Java 7 tak) jo constant fields (`public static final`) aur abstract method declarations (`public abstract`) hold karta hai. Java 8 se interface mein `default` aur `static` methods support hone lage hain.

---

## 82. Give some features of Interface in Java.

- Multiple inheritance support.
- Loose Coupling achieve karna.
- All fields are implicitly `public static final`.
- Java 8: `default` methods (with body) and `static` utility methods.
- Java 9: `private` helper methods inside interfaces.

---

## 83. What is a Marker Interface?

**Matlab:** Ek aisa interface jiske andar **KOI field ya method declare NAHI hota** (Empty Interface).

### Examples:

- `java.io.Serializable`
- `java.lang.Cloneable`
- `java.util.RandomAccess`

**Purpose:** JVM ko signal / metadata tag dena ki is class par special execution behavior apply karna hai.

---

## 84. What are the differences between Abstract Class and Interface?

| Feature | Abstract Class | Interface |
|---|---|---|
| **Multiple Inheritance** | Classes directly multi-extend nahi kar sakti. | Implements multiple interfaces supported. |
| **Methods Type** | Abstract & Non-abstract (concrete) methods. | Abstract, Default (Java 8), Static (Java 8), Private (Java 9). |
| **Fields** | Final, Non-final, Static, Instance fields. | Fields are ALWAYS `public static final`. |
| **Constructor** | Constructors ho sakte hain (`super()` call ke liye). | Constructors nahi ho sakte. |

---

## 85. What do you mean by Data Encapsulation?

**Matlab:** Data Fields ko `private` mark karke unhe directly access hone se rokna aur controlled access `public` Getters and Setters ke dwaara provide karne ko **Encapsulation** kehte hain (Data Hiding + Bundling).

---

## 86. What are the advantages of Encapsulation in Java?

- **Data Protection:** Fields directly corrupt nahi hoti.
- **Read-Only / Write-Only Classes:** Getters/Setters selectively skip karke.
- **Flexibility:** Validation logic setter method ke andar enforce kiya ja sakta hai (`if(age > 0)`).

---

## 87. What is the primary benefit of Encapsulation?

**Primary Benefit:** Implementation details changes karne par client code break nahi hota, aur field level validation enforcement strict hota hai.

---

## 88. What do you mean by Aggregation?

**Matlab:** Aggregation **HAS-A relationship** (Weak Bonding) ko represent karta hai jahan ek class dusri class ka reference hold karti hai, lekin dono objects ka independent lifecycle hota hai.

**Example:** `Department` HAS-A `Teacher`. Agar Department closed ho jaye, Teacher object tab bhi independently exist karta hai.

---

## 89. What is the 'IS-A' relationship in OOPs Java?

**Matlab:** 'IS-A' relationship **Inheritance** (`extends` / `implements`) ko represent karta hai.

**Example:** `Dog` IS-A `Animal`.

---

## 90. Define Inheritance.

**Matlab:** Ek mechanism jisme Child Class (Subclass) Parent Class (Superclass) ke properties (fields) aur behaviors (methods) ko inherit karke reuse/extend karti hai.

---

## 91. What are the different types of Inheritance in Java?

1. **Single Inheritance:** Class B extends Class A.
2. **Multilevel Inheritance:** Class C extends Class B, Class B extends Class A.
3. **Hierarchical Inheritance:** Class B extends Class A, Class C extends Class A.
4. **Multiple Inheritance (Via Interfaces only):** Class C implements Interface A, Interface B.
5. **Hybrid Inheritance:** Mixture of two or more types (Interfaces se achievable).

---

## 92. What is Multiple Inheritance? Is it supported by Java?

**Matlab:** Ek class ka multiple parent classes se inherit hona Multiple Inheritance kehta hai. Java **Classes ke through isse support NAHI karta**, lekin **Interfaces ke through support karta hai**.

---

## 93. How is inheritance in C++ different from Java?

- C++ classes mein Multiple Inheritance direct allow karta hai (leads to Diamond Problem). Java classes mein Single class extension restriction hoti hai.
- Java mein Sabhi classes implicitly `Object` class ko inherit karti hain. C++ mein koi universal root class nahi hoti.

---

## 94. Is there any limitation to using Inheritance?

- **Tight Coupling:** Parent class mein change hone par subclasses par direct impact padta hai.
- Superclass internal implementation details leak ho sakti hain.

---

## 95. Why is Composition preferred over Inheritance? ("Favor Composition over Inheritance")

**Matlab:**
- Inheritance Tight Coupling create karta hai.
- Composition (**HAS-A**) mein references encapsulate hote hain, jisse runtime flexibility milti hai (Dynamic strategy swapping) bina parent class internal bugs ko inherit kiye.

---

## 96. What is Association?

**Matlab:** Association do independent classes ke beech ka connection / relationship (HAS-A) hota hai. Yeh 2 forms mein hota hai: **Aggregation** (Weak) aur **Composition** (Strong).

---

## 97. What is Aggregation? (Weak HAS-A)

Objects ek dusre se link hote hain par **lifecycles independent** rehte hain (e.g., `Car` aur `Driver`).

---

## 98. What is Composition in Java? (Strong HAS-A)

**Matlab:** Composition strong relationship form karta hai jahan Container Object ke destroy hone par Component Objects bhi destroy ho jaate hain.

**Example:** `Car` HAS-A `Engine`. Agar Car destroy hogi, Engine instance bhi clear ho jayega.

---

## 99. Difference between Composition and Aggregation.

| Feature | Composition | Aggregation |
|---|---|---|
| **Relationship Type** | Strong HAS-A | Weak HAS-A |
| **Object Lifetime** | Dependent (Child parent ke bina survive nahi kar sakta). | Independent (Dono objects independently survive kar sakte hain). |

---

## 100. Can constructors be inherited in Java?

**Matlab:** **Nahi!** Constructors inherit nahi hote. Subclass parent class constructor ko `super()` keyword dwara invoke kar sakti hai, par inherit nahi kar sakti.

---

## 101. What is Polymorphism?

**Matlab:** Polymorphism (Poly = Many, Morph = Forms) ek concept hai jisme ek hi action alag-alag ways mein execute ho sakta hai.

1. **Compile-time Polymorphism (Static Binding):** Method Overloading.
2. **Runtime Polymorphism (Dynamic Binding):** Method Overriding.

---

## 102. What is Runtime Polymorphism or Dynamic Method Dispatch?

**Matlab:** Dynamic Method Dispatch ek process hai jisme overridden method call runtime par object instance type ke aadhar par resolve hota hai (reference type ke aadhar par nahi).

```java
Parent obj = new Child(); // Upcasting
obj.makeSound(); // Child class method executes at runtime
```

---

## 103. What is Method Overriding?

**Matlab:** Child class mein Parent class ke same signature (name, parameter list, return type) wale method ki specific implementation define karna **Method Overriding** kehta hai.

---

## 104. What is Method Overloading?

**Matlab:** Same class ke andar Multiple Methods ka name identical hona par **Parameter List (Number, Data Types, Sequence) alag hona** Method Overloading kehta hai.

---

## 105. Can we override a `static` method in Java?

**Matlab:** **Nahi!** Static methods ko override **NAHI** kiya ja sakta. Agar aap child class mein same static method defining karoge, toh isse **Method Hiding** kehte hain, overriding nahi (Kyunki static methods compile-time par bind hote hain).

---

## 106. Can we override an overloaded method?

**Matlab:** **Haan!** Child class overloaded methods me se kisi ko bhi override kar sakti hai.

---

## 107. Can we overload the `main()` method in Java?

**Matlab:** **Haan!** Java mein `main()` method ko overload kar sakte hain (`main(int a)`, `main(String s)`), lekin JVM hamesha `public static void main(String[] args)` signature wale method ko hi entry point treat karega.

---

## 108. Difference between Method Overloading and Method Overriding.

| Feature | Method Overloading | Method Overriding |
|---|---|---|
| **Binding Time** | Compile-Time (Static). | Runtime (Dynamic). |
| **Parameters** | Must be Different. | Must be Exactly Same. |
| **Private/Static/Final** | Overload ho sakte hain. | Override NAHI ho sakte. |
| **Location** | Same Class. | Parent & Child Classes (Inheritance). |

---

## 109. Can we override `private` methods?

**Matlab:** **Nahi!** Private methods outer/subclasses ko visible hi nahi hote, isliye overriding impossible hai.

---

## 110. Can we change the scope of an overridden method in the subclass?

**Matlab:** **Haan**, lekin aap Access Scope **badha (increase) sakte hain, kam (restrict) nahi kar sakte**!
- Parent `protected` -> Child can be `protected` or `public`.
- Parent `protected` -> Child `private` or `default` is **COMPILE ERROR** (`cannot reduce visibility`).

---

## 111. Can we modify the `throws` clause of superclass method while overriding it?

### Rules for Exception Handling in Overriding:

1. Superclass method declares NO checked exception -> Subclass can ONLY declare Unchecked Exceptions (RuntimeException).
2. Superclass method declares Checked Exception -> Subclass can declare same, child checked exception, or NO exception, but CANNOT declare broader super-type checked exception.

---

## 112. Can you have Virtual Functions in Java?

**Matlab:** Java mein **Sabhi Non-Static, Non-Final, Non-Private methods by default Virtual Functions hote hain**! (Implicitly virtual).

---

## 113. What is Abstraction?

**Matlab:** Essential details user ko dikhana aur background implementation complexity ko hide karna **Abstraction** kehta hai.

---

## 114. What is an Abstract Class?

**Matlab:** `abstract` keyword se declared class jo 0 ya 1+ abstract methods hold kar sakti hai. Abstract class ka **direct object creation (`new`) allowed NAHI hota**.

---

## 115. When are Abstract methods used?

**Matlab:** Jab Parent class signature/contract define karna chahti hai, par actual calculation logic child classes par leave karna chahti hai (e.g., `Shape` class with `abstract calculateArea()`).

---

## 116. How can you avoid serialization in child class if base class implements `Serializable`?

**Matlab:** Subclass mein `writeObject()` aur `readObject()` custom implementation likhkar unme `NotSerializableException` throw kar do:

```java
private void writeObject(ObjectOutputStream out) throws IOException {
    throw new NotSerializableException("Serialization blocked for child class!");
}
```
