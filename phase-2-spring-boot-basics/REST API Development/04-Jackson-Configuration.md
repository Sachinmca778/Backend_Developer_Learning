# Jackson Configuration

## Status: Not Started

---

## Table of Contents

1. [Jackson Overview](#jackson-overview)
2. [ObjectMapper Configuration](#objectmapper-configuration)
3. [@JsonProperty](#jsonproperty)
4. [@JsonIgnore](#jsonignore)
5. [@JsonInclude](#jsoninclude)
6. [@JsonFormat](#jsonformat)
7. [@JsonSerialize / @JsonDeserialize](#jsonserialize--jsondeserialize)
8. [Custom Serializers](#custom-serializers)
9. [Jackson Module Registration](#jackson-module-registration)

---

## Jackson Overview

**Jackson** Spring Boot ka default JSON library hai. Java objects ko JSON mein convert (serialize) aur JSON ko Java objects mein convert (deserialize) karta hai.

### How It Works

```
Java Object ←—— Jackson ObjectMapper ——→ JSON
              (Serialization/Deserialization)
```

### Spring Boot Auto-Configuration

Spring Boot automatically `ObjectMapper` configure kar deta hai. Aapko bas annotations ya properties use karni hoti hain.

---

## ObjectMapper Configuration

### Global Configuration via application.properties

```properties
# Default property inclusion
spring.jackson.default-property-inclusion=non_null

# Date format
spring.jackson.date-format=yyyy-MM-dd HH:mm:ss
spring.jackson.time-zone=Asia/Kolkata

# Pretty print (development mein readable JSON)
spring.jackson.serialization.indent-output=true

# Write dates as timestamps
spring.jackson.serialization.write-dates-as-timestamps=false

# Fail on unknown properties
spring.jackson.deserialization.fail-on-unknown-properties=false
```

### Java Config (Custom ObjectMapper Bean)

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Serialization settings
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Deserialization settings
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // Date/Time settings
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Custom date format
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        return mapper;
    }
}
```

### Common Serialization Features

| Feature | Enable | Disable | Description |
|---------|--------|---------|-------------|
| **INDENT_OUTPUT** | `enable()` | `disable()` | Pretty print JSON |
| **WRITE_DATES_AS_TIMESTAMPS** | `enable()` | `disable()` | Dates as timestamps |
| **WRITE_NULL_MAP_VALUES** | `enable()` | `disable()` | Null map values write karo |

### Common Deserialization Features

| Feature | Enable | Disable | Description |
|---------|--------|---------|-------------|
| **FAIL_ON_UNKNOWN_PROPERTIES** | `enable()` | `disable()` | Unknown properties pe fail |
| **ACCEPT_SINGLE_VALUE_AS_ARRAY** | `enable()` | `disable()` | Single value ko array maan lo |
| **FAIL_ON_NULL_FOR_PRIMITIVES** | `enable()` | `disable()` | Null → primitive conversion fail |

---

## @JsonProperty

Field ka JSON name change karta hai.

### Basic Usage

```java
public class UserResponse {

    @JsonProperty("user_id")
    private Long id;

    @JsonProperty("full_name")
    private String name;

    @JsonProperty("email_address")
    private String email;
}
```

**Output:**
```json
{
  "user_id": 1,
  "full_name": "Sachin",
  "email_address": "sachin@example.com"
}
```

### Read-Only / Write-Only Properties

```java
public class UserResponse {

    // Serialization only (response mein aayega, request mein ignore)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    // Write only (request mein aayega, response mein nahi)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    // Read and Write (default)
    @JsonProperty(access = JsonProperty.Access.READ_WRITE)
    private String name;
}
```

**Request JSON (Input):**
```json
{
  "password": "mySecret123",
  "name": "Sachin"
}
```

**Response JSON (Output):**
```json
{
  "id": 1,
  "name": "Sachin"
  // password nahi hai (WRITE_ONLY)
}
```

---

## @JsonIgnore

Field ko JSON serialization/deserialization se completely exclude karta hai.

### Basic Usage

```java
public class User {

    private Long id;
    private String name;

    @JsonIgnore
    private String password;

    @JsonIgnore
    private String resetToken;

    @JsonIgnore
    private LocalDateTime lastPasswordChange;
}
```

**Output:**
```json
{
  "id": 1,
  "name": "Sachin"
  // password, resetToken, lastPasswordChange — sab hidden
}
```

### @JsonIgnore on Getter vs Field

```java
public class User {

    private String name;
    private String internalNotes;

    // Getter pe @JsonIgnore — serialization skip hoga
    @JsonIgnore
    public String getInternalNotes() {
        return internalNotes;
    }

    // Lekin setter pe nahi — deserialization mein accept hoga
    public void setInternalNotes(String notes) {
        this.internalNotes = notes;
    }
}
```

### @JsonIgnore vs @JsonInclude(NON_NULL)

| Annotation | Behavior |
|------------|----------|
| **@JsonIgnore** | Hamesha skip karo (chahe null ho ya not) |
| **@JsonInclude(NON_NULL)** | Sirf tab skip karo jab null ho |

---

## @JsonInclude

Null, empty, ya default values ko conditionally skip karta hai.

### Class Level

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;       // null hai toh skip
    private String address;     // null hai toh skip
}
```

**Input:**
```java
new UserResponse(1L, "Sachin", "sachin@example.com", null, null);
```

**Output:**
```json
{
  "id": 1,
  "name": "Sachin",
  "email": "sachin@example.com"
  // phone aur address skip ho gaye (null the)
}
```

### Field Level

```java
public class UserResponse {

    private Long id;
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String phone;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> tags;  // Empty list bhi skip hoga

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int age;  // 0 (default) skip hoga
}
```

### Include Options

| Option | Behavior |
|--------|----------|
| `ALWAYS` | Hamesha include karo (default) |
| `NON_NULL` | Null fields skip karo |
| `NON_EMPTY` | Null ya empty collections/strings skip karo |
| `NON_DEFAULT` | Default values (0, false, "") skip karo |
| `NON_ABSENT` | Optional/AtomicReference ke absent values skip karo |

---

## @JsonFormat

Date, Time, Number formatting control karta hai.

### Date/Time Formatting

```java
public class EventResponse {

    private Long id;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startDateTime;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Kolkata")
    private LocalDate eventDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private Date legacyDate;
}
```

**Output:**
```json
{
  "id": 1,
  "title": "Spring Boot Workshop",
  "startDateTime": "2024-03-15 10:00:00",
  "eventDate": "2024-03-15",
  "legacyDate": "15/03/2024"
}
```

### Number Formatting

```java
public class ProductResponse {

    private String name;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal price;  // Scientific notation se bachne ke liye

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private boolean inStock;  // true/false instead of "true"/"false"
}
```

**Output:**
```json
{
  "name": "Laptop",
  "price": "49999.99",
  "inStock": true
}
```

### Timezone Handling

```java
public class MeetingResponse {

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "UTC")
    private LocalDateTime utcTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "Asia/Kolkata")
    private LocalDateTime istTime;
}
```

---

## @JsonSerialize / @JsonDeserialize

Custom serialization/deserialization logic likhne ke liye.

### Custom Serializer

```java
// Currency format karna
public class CurrencySerializer extends JsonSerializer<BigDecimal> {

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(value.setScale(2, RoundingMode.HALF_UP) + " INR");
    }
}

// Usage
public class ProductResponse {
    private String name;

    @JsonSerialize(using = CurrencySerializer.class)
    private BigDecimal price;
}
```

**Output:**
```json
{
  "name": "Laptop",
  "price": "50000.00 INR"
}
```

### Custom Deserializer

```java
// Currency parse karna
public class CurrencyDeserializer extends JsonDeserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        String text = p.getText();
        if (text == null || text.isEmpty()) {
            return null;
        }
        // "50000.00 INR" → 50000.00
        String number = text.replace(" INR", "").trim();
        return new BigDecimal(number);
    }
}

// Usage
public class CreateProductRequest {
    private String name;

    @JsonDeserialize(using = CurrencyDeserializer.class)
    private BigDecimal price;
}
```

**Input:**
```json
{
  "name": "Laptop",
  "price": "50000.00 INR"
}
```

### Both Serializer + Deserializer

```java
public class OrderResponse {

    private Long orderId;

    @JsonSerialize(using = CurrencySerializer.class)
    @JsonDeserialize(using = CurrencyDeserializer.class)
    private BigDecimal totalAmount;
}
```

---

## Custom Serializers

### Global Custom Serializer (Module Registration)

```java
@Configuration
public class JacksonConfig {

    @Bean
    public Module customSerializationModule() {
        SimpleModule module = new SimpleModule("CustomModule");

        // Custom serializers register karo
        module.addSerializer(BigDecimal.class, new CurrencySerializer());
        module.addDeserializer(BigDecimal.class, new CurrencyDeserializer());

        // LocalDate/LocalDateTime serializer
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeserializer());

        return module;
    }
}
```

### Contextual Serializer

Dynamic formatting — field ke annotation ke basis pe.

```java
public class DynamicDateFormatSerializer extends JsonSerializer<Date>
        implements ContextualSerializer {

    private String pattern = "yyyy-MM-dd";  // Default

    public DynamicDateFormatSerializer() { }

    public DynamicDateFormatSerializer(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        JsonFormat format = property.getAnnotation(JsonFormat.class);
        if (format != null && format.pattern() != null) {
            return new DynamicDateFormatSerializer(format.pattern());
        }
        return this;
    }

    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeString(new SimpleDateFormat(pattern).format(value));
    }
}
```

---

## Jackson Module Registration

### JavaTimeModule (Java 8 Date/Time)

```xml
<!-- Spring Boot mein usually included hai -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

```java
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());  // LocalDate, LocalDateTime, etc.
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

### JodaModule (Joda Time)

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-joda</artifactId>
</dependency>
```

```java
mapper.registerModule(new JodaModule());
```

### Hibernate5Module (Lazy Loading)

```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-hibernate5</artifactId>
</dependency>
```

```java
mapper.registerModule(new Hibernate5Module());
```

### KotlinModule (Kotlin Support)

```xml
<dependency>
    <groupId>com.fasterxml.jackson.module</groupId>
    <artifactId>jackson-module-kotlin</artifactId>
</dependency>
```

```java
mapper.registerModule(new KotlinModule());
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **ObjectMapper** | JSON ↔ Java conversion engine |
| **Global Config** | `application.properties` ya `@Bean ObjectMapper` |
| **@JsonProperty** | JSON field name change karo, READ_ONLY/WRITE_ONLY access |
| **@JsonIgnore** | Field ko completely hide karo |
| **@JsonInclude** | Null, empty, ya default values conditionally skip karo |
| **@JsonFormat** | Date, Time, Number formatting control karo |
| **@JsonSerialize** | Custom serialization logic |
| **@JsonDeserialize** | Custom deserialization logic |
| **JavaTimeModule** | Java 8 Date/Time (LocalDate, LocalDateTime) support |
| **Hibernate5Module** | Lazy-loaded collections handle karo |
| **Modules** | Extra data types aur custom serializers register karo |
