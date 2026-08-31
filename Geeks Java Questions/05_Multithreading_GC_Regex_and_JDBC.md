# Part 5: Multithreading, Concurrency, Garbage Collection, Regex & JDBC

---

## 169. What is Multitasking?

**Matlab:** Multitasking CPU ka ek feature hai jisme Multiple Tasks / Processes ko simultaneously execute kiya jata hai CPU time share karke.

1. **Process-based Multitasking:** Multiple applications ek saath run karna (e.g., Browser + IDE + Music Player).
2. **Thread-based Multitasking (Multithreading):** Ek hi application ke andar Multiple Threads (tasks) ek saath run karna.

---

## 170. What do you mean by a Multithreaded program?

**Matlab:** Aisa program jisme execution ke 2 ya usse zyada concurrent paths (**Threads**) ek saath run ho sakte hain. Har thread independent code execution path follow karta hai aur shared memory utilize karta hai.

---

## 171. What are the advantages of Multithreading?

1. **Better Resource Utilization:** CPU idle waste nahi hota (I/O wait ke dauran dusra thread CPU use kar leta hai).
2. **High Throughput & Responsiveness:** UI freeze nahi hota, background tasks smooth chalte hain.
3. **Shared Memory Economy:** Threads same process heap memory share karte hain, isliye inter-thread communication fast hota hai.

---

## 172. What are the two ways in which Thread can be created in Java?

### 1. Extending `Thread` Class:
```java
class MyThread extends Thread {
    @Override
    public void run() {
        System.out.println("Thread running via Thread class extension");
    }
}
// Execution: new MyThread().start();
```

### 2. Implementing `Runnable` Interface (Recommended Best Practice):
```java
class MyTask implements Runnable {
    @Override
    public void run() {
        System.out.println("Thread running via Runnable interface implementation");
    }
}
// Execution: new Thread(new MyTask()).start();
```

> [!TIP]
> **Kyun Runnable Interface Better Hai?**
> Class extension Java mein single class restrict hoti hai. Runnable implement karne par aapki class abhi bhi kisi dusri Parent Class ko `extends` kar sakti hai.

---

## 173. What is a Thread?

**Matlab:** Thread execution ki sabse smallest unit (**Lightweight Process**) hoti hai jiska apna Program Counter (PC), Call Stack aur Local Variables hote hain, par Heap memory process-level shared hoti hai.

---

## 174. Differentiate between Process and Thread.

| Property | Process | Thread |
|---|---|---|
| **Definition** | An executing instance of a program. | A lightweight path of execution within a process. |
| **Memory** | Own independent memory space. | Shares Heap memory with sibling threads of same process. |
| **Creation Cost** | High creation & Context Switching overhead. | Low overhead & fast context switching. |
| **Isolation** | High isolation (One process crash doesn't crash another). | Lower isolation (One thread uncaught crash can affect process). |

---

## 175. Describe the Life Cycle of a Thread in Java.

```
 [ NEW ] ─────(start())─────► [ RUNNABLE ] ◄───(Scheduler)───► [ RUNNING ]
                                   │                              │
                           (sleep / wait / block)            (run() ends)
                                   ▼                              ▼
                             [ WAITING / ]                  [ TERMINATED ]
                             [ BLOCKED   ]
```

1. **NEW:** Thread object created (`new Thread()`), `start()` call nahi hua.
2. **RUNNABLE:** `start()` call ho gaya, Ready for CPU scheduling.
3. **RUNNING:** CPU scheduler ne CPU time slice assign kar diya. `run()` code execute ho raha hai.
4. **BLOCKED / WAITING / TIMED_WAITING:** Thread I/O wait, lock acquisition (`synchronized`), `sleep()`, ya `wait()` call ki wajah se paused hai.
5. **TERMINATED (DEAD):** `run()` method finish ho gaya ya uncaught exception throw hua.

---

## 176. Explain `sleep()` method under Thread class.

**Matlab:** `Thread.sleep(millis)` current executing thread ke execution ko specified milliseconds ke liye pause / suspend kar deta hai.

- **Lock Release:** `sleep()` call **MONITOR LOCK RELEASE NAHI KARTA** (`synchronized` locks retain rahte hain).
- **Checked Exception:** `InterruptedException` throw karta hai.

---

## 177. Explain the Main Thread under Thread class execution.

**Matlab:** Jab Java program start hota hai, JVM automatic **Main Thread** (`main` method execute karne ke liye) create karta hai. Pure application execution flow main thread se spawn hokar baaki child threads launch karta hai.

---

## 178. What is a Daemon Thread?

**Matlab:** Daemon Thread Low Priority Background Service Provider Thread hota hai jo User Threads ko support services (e.g., Garbage Collection, Housekeeping) offer karta hai.

```java
Thread t = new Thread(task);
t.setDaemon(true); // Must be called BEFORE t.start()
t.start();
```

> [!IMPORTANT]
> **JVM Shutdown Behavior:** Jab sabhi non-daemon (User) threads finish ho jaate hain, JVM daemon threads ko wait kiye bina application shutdown kar deta hai.

---

## 179. What are the ways in which a thread can enter the Waiting State?

1. `Object.wait()` call karne par.
2. `Thread.join()` call karne par (Dusre thread ke finish hone ke wait mein).
3. `LockSupport.park()` call karne par.

---

## 180. How does Multithreading take place on a computer with a single CPU?

**Matlab:** Single Core CPU par Multithreading **Time Slicing (Context Switching)** mechanism se hota hai.

OS Thread Scheduler milliseconds scale par continuously threads ke beech CPU swap (Context Switch) karta hai. Yeh swap itna ultra-fast hota hai ki user ko feel hota hai ki saare threads simultaneously execute ho rahe hain (Concurrent Execution).

---

## 181. What's the difference between `sleep()` and `wait()` methods?

| Property | `Thread.sleep()` | `Object.wait()` |
|---|---|---|
| **Class Owner** | `java.lang.Thread` static method. | `java.lang.Object` instance method. |
| **Lock Release** | **Does NOT release locks** (Monitors retain rahte hain). | **Releases the lock** (Monitor lock release ho jata hai). |
| **Synchronization** | Synchronized block ki zaroorat nahi. | Must be called inside `synchronized` block/method. |
| **Wakeup Condition** | Time duration expire hone par ya `interrupt()` se. | `notify()` / `notifyAll()` call hone par. |

---

## 182. What are Thread Priorities in Java & default priority assigned by JVM?

**Matlab:** Thread priority (Range **1 to 10**) Thread Scheduler ko hint deti hai ki kis thread ko preference deni chahiye.

- **`Thread.MIN_PRIORITY`:** `1`
- **`Thread.NORM_PRIORITY`:** `5` (Default Priority assigned by JVM)
- **`Thread.MAX_PRIORITY`:** `10`

---

## 183. Why Garbage Collection is necessary in Java?

**Matlab:** Manual Memory Management (C++ `delete`) memory leaks aur dangling pointers introduce karti hai. Garbage Collector (GC) Heap Memory se unreferenced / unreachable objects ko automatic clear karke memory free karta hai, jisse memory leaks prevention aur system stability milti hai.

---

## 184. What is the drawback of Garbage Collection?

1. **Stop-The-World (STW) Pauses:** GC execution ke dauran application threads temporarily pause ho sakte hain, jisse latency spikes aati hain.
2. **CPU Overhead:** GC background monitoring and sweeping CPU resources consume karti hai.

---

## 185. Explain the difference between Minor, Major, and Full Garbage Collection.

1. **Minor GC:** Eden Space / Young Generation area se short-lived dead objects clean karta hai (Fast STW pause).
2. **Major GC:** Tenured / Old Generation area clean karta hai (Slower than Minor GC).
3. **Full GC:** Entire Heap (Young + Old + Metaspace) complete clean karta hai. High STW latency pause time.

---

## 186. What is a Memory Leak, and how does it affect Garbage Collection?

**Matlab:** Memory Leak tab hota hai jab objects application mein ab use nahi ho rahe hote, par unke references accidentally static fields, unclosed connections ya collections mein maintain rahte hain.

- **GC Impact:** GC in objects ko "Reachable" samajhkar clear nahi kar pata. Heap gradually fill ho jati hai aur eventually **`java.lang.OutOfMemoryError: Java heap space`** crash throwing hota hai.

---

## 187. Name some classes present in `java.util.regex` package.

1. **`Pattern`:** Compiled representation of Regular Expression.
2. **`Matcher`:** Engine jo input string par pattern match search karta hai.
3. **`PatternSyntaxException`:** Syntax error exception.

---

## 188. Write a Regular Expression to validate a Password (Alphabet start, Alphanumeric follow, Length 8-20).

```java
import java.util.regex.Pattern;

public class RegexDemo {
    public static void main(String[] args) {
        // Password must start with alphabet [a-zA-Z], followed by alphanumeric [a-zA-Z0-9], total length 8 to 20
        String regex = "^[a-zA-Z][a-zA-Z0-9]{7,19}$";
        
        String pass = "Sachin1234";
        boolean isValid = Pattern.matches(regex, pass);
        System.out.println("Is Password Valid: " + isValid); // Output: true
    }
}
```

---

## 189. What is JDBC? (Java Database Connectivity)

**Matlab:** JDBC Java API (`java.sql` package) hai jo Java Applications ko Relational Databases (MySQL, PostgreSQL, Oracle) se connect karke SQL Queries run karne standard specification offer karti hai.

---

## 190. What is a JDBC Driver?

**Matlab:** JDBC Driver ek software component hai jo Java JDBC API calls को Database specific protocol format mein translate karta hai.

---

## 191. What are the 5 Steps to connect to a Database in Java?

1. **Register JDBC Driver Class:** `Class.forName("com.mysql.cj.jdbc.Driver");`
2. **Create Connection:** `Connection con = DriverManager.getConnection(url, user, pass);`
3. **Create Statement:** `Statement stmt = con.createStatement();`
4. **Execute Query:** `ResultSet rs = stmt.executeQuery("SELECT * FROM users");`
5. **Close Connection:** `con.close();`

---

## 192. What are the JDBC API Components?

- **`DriverManager`:** Drivers manage karta hai.
- **`Connection`:** Database session interface.
- **`Statement` / `PreparedStatement` / `CallableStatement`:** SQL queries run karne ke liye interfaces.
- **`ResultSet`:** Query result data fetch interface.
- **`SQLException`:** Database errors handle karne ke liye.

---

## 193. What is JDBC Connection interface?

**Matlab:** `java.sql.Connection` database session represent karne wala interface hai. Isse transaction management (`setAutoCommit(false)`, `commit()`, `rollback()`) aur statements create kiye jaate hain.

---

## 194. What does the JDBC `ResultSet` interface do?

**Matlab:** `ResultSet` SQL `SELECT` query execute hone par return hone wala tabular data grid view hold karta hai (`rs.next()`, `rs.getString()`).

---

## 195. What is the JDBC `RowSet`?

**Matlab:** `RowSet` (`javax.sql.RowSet`) JDBC `ResultSet` ka wrapper hai jo **Disconnected Mode** support karta hai (Database connection close hone ke baad bhi data memory mein read/write accessible rehta hai).

---

## 196. What is the role of the JDBC `DriverManager` class?

**Matlab:** `DriverManager` service class registered JDBC drivers tracking rakhti hai aur connection strings ke basis par database connection establish karti hai.
