# Nested & Inner Classes

## Status: Not Started

---

## Table of Contents

1. [Static Nested Class](#static-nested-class)
2. [Inner Class](#inner-class)
3. [Local Class](#local-class)
4. [Anonymous Class](#anonymous-class)
5. [Lambda as Replacement for Anonymous Class](#lambda-as-replacement-for-anonymous-class)

---

## Static Nested Class

**Matlab:** Static keyword ke saath declared nested class — outer class ka independent member.

### Declaration

```java
class OuterClass {
    private static String staticField = "Static";
    private String instanceField = "Instance";

    // Static nested class
    static class StaticNestedClass {
        void display() {
            // ✅ Static members access kar sakta hai
            System.out.println(staticField);

            // ❌ Instance members directly access nahi kar sakta
            // System.out.println(instanceField);  // ❌ Error

            // ✅ Instance members via object
            OuterClass outer = new OuterClass();
            System.out.println(outer.instanceField);
        }
    }
}

// Usage
OuterClass.StaticNestedClass nested = new OuterClass.StaticNestedClass();
nested.display();
```

### When to Use

```java
// ✅ Use static nested class when:
// 1. Nested class ko outer class ke instance ki zarurat nahi
// 2. Encapsulation maintain karni hai
// 3. Logical grouping

class Map {
    // Logical grouping — Map.Entry sirf Map ke context mein meaningful hai
    static class Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() { return key; }
        public V getValue() { return value; }
    }
}

// Usage
Map.Entry<String, Integer> entry = new Map.Entry<>("age", 25);
System.out.println(entry.getKey());   // "age"
System.out.println(entry.getValue()); // 25
```

### Builder Pattern with Static Nested Class

```java
public class User {
    private final String name;
    private final int age;
    private final String email;

    // Private constructor
    private User(Builder builder) {
        this.name = builder.name;
        this.age = builder.age;
        this.email = builder.email;
    }

    // Static nested Builder class
    public static class Builder {
        private String name;
        private int age;
        private String email;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAge(int age) {
            this.age = age;
            return this;
        }

        public Builder setEmail(String email) {
            this.email = email;
            return this;
        }

        public User build() {
            return new User(this);
        }
    }

    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getEmail() { return email; }
}

// Usage — Builder pattern
User user = new User.Builder()
    .setName("Sachin")
    .setAge(25)
    .setEmail("sachin@example.com")
    .build();
```

---

## Inner Class (Non-Static Nested Class)

**Matlab:** Bina static keyword ke nested class — outer class ke instance pe depend karta hai.

### Declaration

```java
class OuterClass {
    private String outerField = "Outer Field";

    // Inner class (non-static)
    class InnerClass {
        void display() {
            // ✅ Sab outer members access kar sakta hai
            System.out.println(outerField);

            // ✅ Outer class ke private members bhi
            System.out.println(getOuterPrivate());
        }

        private String getOuterPrivate() {
            return "Private accessed!";
        }
    }

    // Outer class method
    public void createInner() {
        InnerClass inner = new InnerClass();  // Direct creation
        inner.display();
    }
}

// Usage
OuterClass outer = new OuterClass();
OuterClass.InnerClass inner = outer.new InnerClass();  // Object chahiye!
inner.display();
```

### Inner Class Features

```java
class Outer {
    private int outerValue = 100;

    class Inner {
        private int innerValue = 200;

        void display() {
            // Outer class ke members
            System.out.println("Outer: " + outerValue);

            // Inner class ke members
            System.out.println("Inner: " + innerValue);

            // This references
            System.out.println("Inner this: " + this);
            System.out.println("Outer this: " + Outer.this);
        }
    }
}
```

### When to Use Inner Class

```java
// ✅ Use inner class when:
// 1. Outer class ke instance members chahiye
// 2. Tight coupling hai outer aur nested class mein

class LinkedList {
    private Node head;

    // Inner class — outer class ke nodes pe kaam karta hai
    class Node {
        int data;
        Node next;

        Node(int data) {
            this.data = data;
            this.next = null;  // Outer class ke private field
        }
    }

    public void add(int data) {
        Node newNode = new Node(data);  // Direct access
        if (head == null) {
            head = newNode;
        }
    }
}
```

---

## Local Class

**Matlab:** Method ke andar defined class — sirf usi method mein accessible.

### Declaration

```java
class Outer {
    private String outerField = "Outer";

    public void doSomething() {
        // Local class — method ke andar
        class LocalClass {
            void display() {
                // Outer class ke members access kar sakta hai
                System.out.println(outerField);
            }
        }

        // Usage — sirf is method mein
        LocalClass local = new LocalClass();
        local.display();
    }

    // ❌ Bahar se access nahi ho sakta
    // LocalClass local = new LocalClass();  // ❌ Error
}
```

### Local Class with Final/Effectively Final

```java
class Outer {
    public void process(String input) {
        // Local variable — effectively final hona chahiye
        String prefix = "Processed: ";

        class LocalClass {
            void display() {
                // ✅ Effectively final variables access kar sakta hai
                System.out.println(prefix + input);
            }
        }

        // ❌ Variable modify nahi kar sakte (agar local class access kar rahi hai)
        // prefix = "Changed";  // ❌ Error!

        LocalClass local = new LocalClass();
        local.display();
    }
}
```

### Local Class in Loops

```java
class Processor {
    public void processAll(List<String> items) {
        for (String item : items) {
            class ItemProcessor {
                void process() {
                    System.out.println("Processing: " + item);
                }
            }

            ItemProcessor processor = new ItemProcessor();
            processor.process();
        }
    }
}
```

### When to Use Local Class

```java
// ✅ Use local class when:
// 1. Sirf ek method mein kaam aati hai
// 2. Method ke variables chahiye
// 3. Baar-baar use nahi hoti

class Comparator {
    public void sortByLength(List<String> list) {
        // Local class — sirf yahi use ho rahi hai
        class LengthComparator implements java.util.Comparator<String> {
            @Override
            public int compare(String s1, String s2) {
                return s1.length() - s2.length();
            }
        }

        list.sort(new LengthComparator());
    }
}
```

---

## Anonymous Class

**Matlab:** Bina naam ki class — ek baar use hoti hai, interface/abstract class ko implement/extend karti hai.

### Declaration

```java
// Interface
interface Greeting {
    void greet(String name);
}

// Anonymous class
Greeting greeting = new Greeting() {
    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name + "!");
    }
};

greeting.greet("Sachin");  // "Hello, Sachin!"
```

### Anonymous Class Extending Class

```java
class Animal {
    public void makeSound() {
        System.out.println("Some sound");
    }
}

Animal dog = new Animal() {
    @Override
    public void makeSound() {
        System.out.println("Bark!");
    }
};

dog.makeSound();  // "Bark!"
```

### Anonymous Class in Methods

```java
// Thread creation
Thread thread = new Thread(new Runnable() {
    @Override
    public void run() {
        System.out.println("Thread running");
    }
});
thread.start();

// Event listener (GUI)
button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Button clicked!");
    }
});

// Comparator
List<String> names = List.of("Sachin", "Rahul", "Priya");
names.sort(new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return s1.compareTo(s2);
    }
});
```

### Anonymous Class with Instance Initializer

```java
List<String> list = new ArrayList<String>() {{
    // Instance initializer block — anonymous class constructor jaisa
    add("Apple");
    add("Banana");
    add("Cherry");
}};

System.out.println(list);  // [Apple, Banana, Cherry]
```

### Limitations

```java
// ❌ No constructor (class ka naam nahi hai)
// new Runnable() {
//     public Runnable() { }  // ❌ Error
// }

// ❌ Sirf ek interface/class extend/implement kar sakta hai
// ❌ Reuse nahi kar sakte — sirf ek baar use

// ✅ Can extend class OR implement interface (not both)
Runnable r = new Runnable() { ... };           // ✅ Interface
Animal a = new Animal() { ... };               // ✅ Class
// Runnable r = new Runnable() extends Animal  // ❌ Both nahi
```

---

## Lambda as Replacement for Anonymous Class

**Matlab:** Lambda expressions — anonymous classes ka short form (Java 8+).

### Anonymous Class vs Lambda

```java
interface Greeting {
    void greet(String name);
}

// Anonymous class — verbose
Greeting greeting1 = new Greeting() {
    @Override
    public void greet(String name) {
        System.out.println("Hello, " + name);
    }
};

// Lambda — concise
Greeting greeting2 = name -> System.out.println("Hello, " + name);

// Dono same kaam karte hain
greeting1.greet("Sachin");
greeting2.greet("Sachin");
```

### Common Replacements

```java
// Runnable
Thread t1 = new Thread(new Runnable() {
    @Override
    public void run() {
        System.out.println("Running");
    }
});

Thread t2 = new Thread(() -> System.out.println("Running"));

// Comparator
List<String> list = new ArrayList<>();

list.sort(new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return s1.length() - s2.length();
    }
});

list.sort((s1, s2) -> s1.length() - s2.length());

// ActionListener
button.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Clicked");
    }
});

button.addActionListener(e -> System.out.println("Clicked"));

// Predicate
List<String> filtered = list.stream()
    .filter(new Predicate<String>() {
        @Override
        public boolean test(String s) {
            return s.length() > 3;
        }
    })
    .collect(Collectors.toList());

List<String> filtered2 = list.stream()
    .filter(s -> s.length() > 3)
    .collect(Collectors.toList());
```

### When Lambda Works

```java
// ✅ Lambda works — functional interface (single abstract method)
interface MyInterface {
    void execute();  // Sirf ek method
}

MyInterface lambda = () -> System.out.println("Executed");

// ❌ Lambda doesn't work — multiple abstract methods
interface NotFunctional {
    void method1();
    void method2();  // Second abstract method
}

// NotFunctional nf = () -> { };  // ❌ Error
```

### Method Reference (Further Simplification)

```java
// Lambda
list.forEach(item -> System.out.println(item));

// Method reference — even shorter
list.forEach(System.out::println);

// Lambda
Function<String, Integer> parse = s -> Integer.parseInt(s);

// Method reference
Function<String, Integer> parse2 = Integer::parseInt;

// Lambda
Supplier<Double> random = () -> Math.random();

// Method reference
Supplier<Double> random2 = Math::random;
```

### Anonymous Class vs Lambda Comparison

| Feature | Anonymous Class | Lambda |
|---------|----------------|--------|
| **Syntax** | Verbose | Concise |
| **this reference** | Anonymous class ko point karta hai | Enclosing class ko point karta hai |
| **Multiple methods** | ✅ Implement kar sakta hai | ❌ Sirf single method |
| **State** | Fields declare kar sakta hai | ❌ No state |
| **Performance** | Naya class file banta hai | InvokeDynamic — faster |
| **Type** | Object type | Functional interface type |

### this Reference Difference

```java
class MyClass {
    private String name = "MyClass";

    void withAnonymousClass() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                System.out.println(this.name);  // ❌ Error! Runnable ka this hai
                System.out.println(MyClass.this.name);  // ✅ "MyClass"
            }
        };
    }

    void withLambda() {
        Runnable r = () -> {
            System.out.println(this.name);  // ✅ "MyClass" — enclosing class ka this
        };
    }
}
```

---

## Summary

| Type | Keyword | Access to Outer Members | Instance Required | Use Case |
|------|---------|------------------------|-------------------|----------|
| **Static Nested** | `static class` | Sirf static | ❌ No | Builder pattern, logical grouping |
| **Inner Class** | `class` | Sab (instance + static) | ✅ Yes | Tight coupling with outer |
| **Local Class** | `class` (inside method) | Sab + effectively final vars | ✅ Yes | One-method use only |
| **Anonymous Class** | `new Interface() {}` | Sab + effectively final vars | ✅ Yes | One-time implementation |
| **Lambda** | `() -> {}` | Sab (via enclosing this) | N/A | Functional interface replacement |

### Quick Decision Guide

```
Nested class chahiye?
├── Outer instance ki zarurat nahi? → Static Nested Class
├── Outer instance ki zarurat hai?
│   ├── Multiple methods/state chahiye? → Inner Class
│   ├── Sirf ek method mein use hoga?
│   │   ├── Ek baar use, functional interface? → Lambda
│   │   └── Multiple methods ya non-functional? → Anonymous Class / Local Class
│   └── Baar-baar use hoga? → Inner Class
└── Functional interface (single method)? → Lambda (Java 8+)
```
