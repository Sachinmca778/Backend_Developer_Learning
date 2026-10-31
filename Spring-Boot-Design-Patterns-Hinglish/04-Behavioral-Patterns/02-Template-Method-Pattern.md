# Behavioral Pattern: Template Method Pattern in Spring Boot 📝

---

### 1. Core Concept (Template Method Kya Hai?)
**Answer (Hinglish):**

> **The parent class defines the FIXED SKELETON (order of steps) of an algorithm. Subclasses fill in only the specific steps that change.**  
> *(Analogy: Chai (Tea) vs Coffee Recipe:  
> Step 1: Paani boil karo (Fixed) $\rightarrow$  
> Step 2: Tea leaves daalo vs Coffee powder daalo (Changes) $\rightarrow$  
> Step 3: Cup me pour karo (Fixed) $\rightarrow$  
> Step 4: Milk & sugar add karo (Fixed).*  
> Algorithm ka flow fix hai, bas taste/ingredients child decide karta hai).

---

### 2. In Spring: `JdbcTemplate` & other `*Template` Classes

Spring Framework ke andar jitni bhi classes me **`*Template`** suffix laga hota hai (`JdbcTemplate`, `KafkaTemplate`, `RestTemplate`, `TransactionTemplate`), wo sabhi Template Method pattern follow karti hain!

#### Example: `JdbcTemplate` Internals
Traditional JDBC me developers ko 15 lines ka boilerplate code likhna padta tha:
1. DataSource se connection lo.
2. PreparedStatement banao.
3. SQL execute karo.
4. SQLException handle karo.
5. ResultSet iterate karo.
6. Connection close karo finally block me.

```
+--------------------------------------------------------------------+
|                       JDBCTEMPLATE (PARENT/SKELETON)               |
|                                                                    |
|  [ Open Connection ] ──> [ Prepare Statement ] ──> [ Execute SQL ] |
|                                                           │        |
|  [ Close Connection & Handle Errors ] <── [ Fetch Row ] <─┘        |
+--------------------------------------------------------------------+
                                 │
                 (Only step YOU write: RowMapper)
                                 ▼
                     (rs, rowNum) -> new User(...)
```

**`JdbcTemplate` handles 90% fixed boilerplate:**
- Connection open karna
- Statement banana
- Exception translation (SQLException to Spring `DataAccessException`)
- Connection safely close karna

**Aap sirf wo step likhte ho jo change hota hai (SQL & Row Mapping):**
```java
List<User> users = jdbcTemplate.query(
    "SELECT id, name FROM users",
    (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name"))
);
```

---

### 3. Real Project Architecture: Report Export Pipeline

Maan lijiye aapko order reports export karni hain (CSV format, PDF format, Excel format). Pipeline ke steps fix hain:
1. Data validate karo.
2. Data format karo (CSV vs PDF).
3. File disk ya S3 par save karo.

#### The Abstract Template Class:
```java
public abstract class ReportExporter {

    // 1. SKELETON METHOD: final banaya taaki child classes execution sequence change na kar sakein!
    public final void export(List<Order> data) {
        validate(data);                 // Step 1: Fixed
        String formatted = format(data);// Step 2: Varies by format! (Abstract)
        saveToStorage(formatted);       // Step 3: Fixed
        sendNotification();             // Step 4: Fixed
    }

    private void validate(List<Order> data) {
        if (data == null || data.isEmpty()) throw new IllegalArgumentException("No data to export");
    }

    // Step jo har child class apne tarike se implement karegi:
    protected abstract String format(List<Order> data);

    private void saveToStorage(String content) {
        System.out.println("Saving report file to AWS S3 bucket...");
    }

    private void sendNotification() {
        System.out.println("Notification: Report exported successfully!");
    }
}
```

#### The Concrete Subclasses:
```java
@Component
public class CsvReportExporter extends ReportExporter {
    @Override
    protected String format(List<Order> data) {
        return "id,customer,amount\n" + data.stream()
            .map(o -> o.getId() + "," + o.getCustomer() + "," + o.getAmount())
            .collect(Collectors.joining("\n"));
    }
}

@Component
public class PdfReportExporter extends ReportExporter {
    @Override
    protected String format(List<Order> data) {
        return "%PDF-1.4 [Binary PDF layout for " + data.size() + " orders]";
    }
}
```

---

### 4. ⚖️ Strategy Pattern vs Template Method Pattern

Yeh interview me dono ke beech ka sabse crucial difference hai:

| Difference Point | Template Method Pattern | Strategy Pattern |
| :--- | :--- | :--- |
| **Primary Mechanism** | **Inheritance** (Subclassing abstract class). | **Composition** (Injecting interface references). |
| **Control of Algorithm**| Parent class controls the fixed skeleton (`final` method). | Context class delegates the whole algorithm to strategy. |
| **Granularity** | Algorithm ke sirf **kuch specific steps** badalte hain. | **Poora algorithm completely swap** ho jata hai. |

---

### 💡 Pro Interview Tip:
> **Question:** *"Template Method vs Strategy me kab kaun sa choose karein?"*  
> **Your Answer:** *"Template Method tab choose karein jab process ka skeleton/steps hamesha same sequence me chalna ho (like JDBC connection handling or batch export pipeline). Strategy tab choose karein jab pure ke pure algorithm ko runtime par bina inheritance ke flexibly swap karna ho (like payment options or sorting algorithms)."*
