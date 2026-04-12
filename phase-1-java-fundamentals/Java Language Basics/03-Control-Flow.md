# Control Flow

## Status: Not Started

---

## Table of Contents

1. [if/else](#ifelse)
2. [switch (Classic)](#switch-classic)
3. [switch Expression (Java 14+)](#switch-expression-java-14)
4. [for Loop](#for-loop)
5. [while Loop](#while-loop)
6. [do-while Loop](#do-while-loop)
7. [Enhanced for-each](#enhanced-for-each)
8. [break, continue, labeled statements](#break-continue-labeled-statements)
9. [return](#return)

---

## if/else

**Matlab:** Condition ke basis pe code execute karna — basic decision making.

### Basic if

```java
int age = 20;

if (age >= 18) {
    System.out.println("Adult");
}
```

### if/else

```java
int age = 15;

if (age >= 18) {
    System.out.println("Adult");
} else {
    System.out.println("Minor");
}
```

### if/else if/else

```java
int score = 75;

if (score >= 90) {
    System.out.println("Grade: A");
} else if (score >= 80) {
    System.out.println("Grade: B");
} else if (score >= 70) {
    System.out.println("Grade: C");
} else if (score >= 60) {
    System.out.println("Grade: D");
} else {
    System.out.println("Grade: F");
}
// Output: Grade: C
```

### Nested if

```java
int age = 25;
boolean hasId = true;

if (age >= 18) {
    if (hasId) {
        System.out.println("Entry allowed");
    } else {
        System.out.println("ID required");
    }
} else {
    System.out.println("Must be 18+");
}
```

### Multiple Conditions

```java
int temperature = 30;
boolean isRaining = false;

if (temperature > 25 && !isRaining) {
    System.out.println("Go for a walk");
} else if (temperature > 25 && isRaining) {
    System.out.println("Stay home or use umbrella");
} else {
    System.out.println("Too cold outside");
}
```

---

## switch (Classic)

**Matlab:** Multiple conditions check karna — ek variable ke different values ke basis pe.

### Basic switch

```java
int day = 3;

switch (day) {
    case 1:
        System.out.println("Monday");
        break;
    case 2:
        System.out.println("Tuesday");
        break;
    case 3:
        System.out.println("Wednesday");
        break;
    case 4:
        System.out.println("Thursday");
        break;
    case 5:
        System.out.println("Friday");
        break;
    case 6:
        System.out.println("Saturday");
        break;
    case 7:
        System.out.println("Sunday");
        break;
    default:
        System.out.println("Invalid day");
}
// Output: Wednesday
```

### Fall-Through (No break)

```java
int month = 2;

switch (month) {
    case 12:
    case 1:
    case 2:
        System.out.println("Winter");
        break;
    case 3:
    case 4:
    case 5:
        System.out.println("Spring");
        break;
    case 6:
    case 7:
    case 8:
        System.out.println("Summer");
        break;
    case 9:
    case 10:
    case 11:
        System.out.println("Fall");
        break;
    default:
        System.out.println("Invalid month");
}
// Output: Winter
```

### ⚠️ Missing break

```java
int num = 1;

switch (num) {
    case 1:
        System.out.println("One");
        // No break — fall-through!
    case 2:
        System.out.println("Two");
        break;
    default:
        System.out.println("Other");
}
// Output:
// One
// Two
```

### Supported Types

```java
// ✅ int, byte, short, char
switch (intVal) { }
switch (byteVal) { }
switch (shortVal) { }
switch (charVal) { }

// ✅ enum
enum Day { MON, TUE, WED }
switch (dayEnum) { }

// ✅ String (Java 7+)
String fruit = "apple";
switch (fruit) {
    case "apple":
        System.out.println("It's an apple");
        break;
    case "banana":
        System.out.println("It's a banana");
        break;
    default:
        System.out.println("Unknown fruit");
}

// ❌ long, float, double, boolean — NOT supported
switch (longVal) { }  // ❌ Error
```

---

## switch Expression (Java 14+)

**Matlab:** switch ko expression ki tarah use karna — value return karta hai, fall-through nahi hota.

### Arrow Syntax (No Fall-Through)

```java
int day = 3;

String dayName = switch (day) {
    case 1 -> "Monday";
    case 2 -> "Tuesday";
    case 3 -> "Wednesday";
    case 4 -> "Thursday";
    case 5 -> "Friday";
    case 6 -> "Saturday";
    case 7 -> "Sunday";
    default -> "Invalid day";
};

System.out.println(dayName);  // Wednesday
```

### Multiple Cases

```java
int month = 2;

String season = switch (month) {
    case 12, 1, 2 -> "Winter";
    case 3, 4, 5 -> "Spring";
    case 6, 7, 8 -> "Summer";
    case 9, 10, 11 -> "Fall";
    default -> "Invalid month";
};
```

### Block Syntax (Multiple Statements)

```java
int num = 5;

String result = switch (num) {
    case 1, 3, 5, 7, 9 -> {
        System.out.println("Odd number");
        yield "Odd";  // yield se value return karo
    }
    case 2, 4, 6, 8, 10 -> {
        System.out.println("Even number");
        yield "Even";
    }
    default -> {
        System.out.println("Out of range");
        yield "Unknown";
    }
};
```

### Exhaustiveness Check

```java
// Enum ke saath — default zaruri nahi agar sab cases cover ho
enum Color { RED, GREEN, BLUE }

Color color = Color.RED;

String hex = switch (color) {
    case RED -> "#FF0000";
    case GREEN -> "#00FF00";
    case BLUE -> "#0000FF";
    // default nahi chahiye — sab cases cover ho gaye
};

// Primitive/String pe default zaruri hai
int num = 5;
String str = switch (num) {
    case 1 -> "One";
    case 2 -> "Two";
    default -> "Other";  // Default required hai
};
```

### Old Syntax with yield

```java
// Java 12-13 style
String result = switch (num) {
    case 1:
        yield "One";
    case 2:
        yield "Two";
    default:
        yield "Other";
};
```

### switch Statement vs Expression

| Feature | Classic switch | switch Expression |
|---------|---------------|-------------------|
| **Returns value** | ❌ No | ✅ Yes |
| **Fall-through** | ✅ Yes (no break) | ❌ No (arrow syntax) |
| **yield keyword** | N/A | Block se value return karne ke liye |
| **Exhaustiveness** | Default optional | Default required (non-enum) |
| **Java version** | All versions | Java 14+ |

---

## for Loop

**Matlab:** Fixed number of times code execute karna.

### Basic for Loop

```java
// 0 se 9 tak print karo
for (int i = 0; i < 10; i++) {
    System.out.println(i);
}

// Structure:
// for (initialization; condition; update) {
//     // body
// }
```

### Components

```java
for (int i = 0;    // Initialization — ek baar run hota hai
     i < 10;       // Condition — har iteration se pehle check
     i++)          // Update — har iteration ke baad run
{
    System.out.println(i);  // Body
}
```

### Variations

```java
// Decrement
for (int i = 10; i > 0; i--) {
    System.out.println(i);  // 10, 9, 8, ..., 1
}

// Step by 2
for (int i = 0; i <= 20; i += 2) {
    System.out.println(i);  // 0, 2, 4, ..., 20
}

// Multiple variables
for (int i = 0, j = 10; i < j; i++, j--) {
    System.out.println("i=" + i + ", j=" + j);
}

// Infinite loop
for (;;) {
    System.out.println("Running forever");
    // break se bahar aao
}
```

---

## while Loop

**Matlab:** Jab tak condition true hai — tab tak code execute karo. Entry-controlled loop.

```java
int count = 0;

while (count < 5) {
    System.out.println("Count: " + count);
    count++;
}
// Output: Count: 0, 1, 2, 3, 4
```

### When to Use while

```java
// Unknown iterations
int num = 12345;
int digitCount = 0;

while (num > 0) {
    num /= 10;
    digitCount++;
}
System.out.println("Digits: " + digitCount);  // 5

// Reading input
Scanner scanner = new Scanner(System.in);
String input = scanner.nextLine();

while (!input.equals("exit")) {
    System.out.println("You entered: " + input);
    input = scanner.nextLine();
}
```

---

## do-while Loop

**Matlab:** Pehle execute karo, phir condition check karo. Exit-controlled loop — kam se kam ek baar run hoga.

```java
int count = 10;

do {
    System.out.println("Count: " + count);  // Ek baar toh run hoga
    count++;
} while (count < 5);

// Output: Count: 10  (condition false hai, phir bhi ek baar run hua)
```

### When to Use do-while

```java
// Menu-driven program
Scanner scanner = new Scanner(System.in);
int choice;

do {
    System.out.println("1. Add");
    System.out.println("2. Delete");
    System.out.println("3. Exit");
    System.out.print("Enter choice: ");
    choice = scanner.nextInt();

    switch (choice) {
        case 1 -> System.out.println("Adding...");
        case 2 -> System.out.println("Deleting...");
        case 3 -> System.out.println("Exiting...");
        default -> System.out.println("Invalid choice");
    }
} while (choice != 3);
```

### Loop Comparison

| Loop | When to Use | Minimum Executions |
|------|-------------|-------------------|
| **for** | Known iterations | 0 |
| **while** | Unknown iterations, condition pehle check | 0 |
| **do-while** | Kam se kam ek baar run chahiye | 1 |

---

## Enhanced for-each

**Matlab:** Arrays aur Collections ko iterate karna — index ki zarurat nahi.

### Arrays

```java
int[] numbers = {10, 20, 30, 40, 50};

for (int num : numbers) {
    System.out.println(num);
}
```

### Collections

```java
List<String> names = List.of("Sachin", "Rahul", "Priya");

for (String name : names) {
    System.out.println(name);
}

Map<String, Integer> ages = Map.of("Sachin", 25, "Rahul", 30, "Priya", 28);

// Key iterate karo
for (String name : ages.keySet()) {
    System.out.println(name);
}

// Value iterate karo
for (int age : ages.values()) {
    System.out.println(age);
}

// Entry iterate karo
for (Map.Entry<String, Integer> entry : ages.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

### Limitations

```java
int[] numbers = {1, 2, 3, 4, 5};

// ❌ Index access nahi hai
for (int num : numbers) {
    // numbers[num] ka access nahi kar sakte
    // Index chahiye toh regular for use karo
}

// ❌ Modify nahi kar sakte (array elements)
for (int num : numbers) {
    num = num * 2;  // Original array change nahi hoga
}

// ✅ Regular for se modify karo
for (int i = 0; i < numbers.length; i++) {
    numbers[i] = numbers[i] * 2;  // ✅ Works
}

// ❌ Remove nahi kar sakte (concurrent modification)
List<Integer> list = new ArrayList<>(List.of(1, 2, 3));
for (Integer num : list) {
    if (num == 2) {
        list.remove(num);  // ❌ ConcurrentModificationException
    }
}

// ✅ Iterator use karo
Iterator<Integer> it = list.iterator();
while (it.hasNext()) {
    if (it.next() == 2) {
        it.remove();  // ✅ Works
    }
}
```

---

## break, continue, labeled statements

### break

**Matlab:** Loop ya switch se bahar aana.

```java
// Loop se bahar
for (int i = 0; i < 10; i++) {
    if (i == 5) {
        break;  // Loop terminate ho jayega
    }
    System.out.println(i);  // 0, 1, 2, 3, 4
}

// switch se bahar (automatic)
switch (day) {
    case 1:
        System.out.println("Monday");
        break;  // switch se bahar
}
```

### continue

**Matlab:** Current iteration skip karo — next iteration pe jao.

```java
for (int i = 0; i < 10; i++) {
    if (i % 2 == 0) {
        continue;  // Even numbers skip karo
    }
    System.out.println(i);  // 1, 3, 5, 7, 9 (sirf odd)
}
```

### Labeled Statements

**Matlab:** Nested loops mein bahar wale loop ko control karna.

```java
// Break outer loop
outer:
for (int i = 0; i < 5; i++) {
    for (int j = 0; j < 5; j++) {
        if (i * j > 6) {
            break outer;  // Outer loop se bahar
        }
        System.out.println("i=" + i + ", j=" + j);
    }
}

// Continue outer loop
outer:
for (int i = 0; i < 5; i++) {
    for (int j = 0; j < 5; j++) {
        if (j == 2) {
            continue outer;  // Next iteration of outer loop
        }
        System.out.println("i=" + i + ", j=" + j);
    }
}
```

### Labeled Block

```java
myBlock: {
    System.out.println("Start");
    if (true) {
        break myBlock;
    }
    System.out.println("This won't print");
}
System.out.println("End");
// Output: Start, End
```

---

## return

**Matlab:** Method se value return karna aur method terminate karna.

### Void Method

```java
public void printMessage(String msg) {
    if (msg == null) {
        return;  // Early return — void method
    }
    System.out.println(msg);
}
```

### Value Return

```java
public int add(int a, int b) {
    return a + b;  // Value return karo
}

public String getGrade(int score) {
    if (score >= 90) return "A";
    if (score >= 80) return "B";
    if (score >= 70) return "C";
    if (score >= 60) return "D";
    return "F";
}
```

### Early Return Pattern

```java
// ❌ Nested if (pyramid of doom)
public String processUser(User user) {
    if (user != null) {
        if (user.isActive()) {
            if (user.hasPermission()) {
                return "Access granted";
            } else {
                return "No permission";
            }
        } else {
            return "User inactive";
        }
    } else {
        return "User is null";
    }
}

// ✅ Early returns (flat structure)
public String processUser(User user) {
    if (user == null) return "User is null";
    if (!user.isActive()) return "User inactive";
    if (!user.hasPermission()) return "No permission";
    return "Access granted";
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **if/else** | Condition-based execution |
| **switch (classic)** | Multiple cases — `break` lagao warna fall-through |
| **switch expression** | Java 14+ — value return karta hai, arrow syntax, no fall-through |
| **for** | Fixed iterations — `for (init; condition; update)` |
| **while** | Entry-controlled — condition pehle check |
| **do-while** | Exit-controlled — kam se kam ek baar run hoga |
| **for-each** | Arrays/Collections iterate — index nahi milta |
| **break** | Loop/switch se bahar aao |
| **continue** | Current iteration skip karo |
| **labeled** | Nested loops mein bahar wale loop ko control karo |
| **return** | Method se value return + terminate |
