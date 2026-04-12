# Date/Time API (java.time)

## Status: Not Started

---

## Table of Contents

1. [LocalDate, LocalTime, LocalDateTime](#localdate-localtime-localdatetime)
2. [ZonedDateTime & Instant](#zoneddatetime--instant)
3. [Duration & Period](#duration--period)
4. [DateTimeFormatter](#datetimeformatter)
5. [ZoneId & ChronoUnit](#zoneid--chronounit)
6. [TemporalAdjusters](#temporaladjusters)

---

## LocalDate, LocalTime, LocalDateTime

**Matlab:** Timezone ke bina date/time represent karna — most common use cases ke liye.

### LocalDate

```java
// Current date
LocalDate today = LocalDate.now();  // 2024-01-15

// Specific date
LocalDate date1 = LocalDate.of(2024, 1, 15);
LocalDate date2 = LocalDate.of(2024, Month.JANUARY, 15);

// Parse from string
LocalDate date3 = LocalDate.parse("2024-01-15");

// Get parts
int year = today.getYear();          // 2024
int month = today.getMonthValue();   // 1
Month monthEnum = today.getMonth();  // JANUARY
int day = today.getDayOfMonth();     // 15
DayOfWeek dow = today.getDayOfWeek(); // MONDAY
int dayOfYear = today.getDayOfYear(); // 15

// Check leap year
boolean isLeap = today.isLeapYear();  // true

// Compare
LocalDate d1 = LocalDate.of(2024, 1, 15);
LocalDate d2 = LocalDate.of(2024, 2, 1);
boolean isBefore = d1.isBefore(d2);   // true
boolean isAfter = d1.isAfter(d2);     // false
boolean isEqual = d1.isEqual(d1);     // true
```

### LocalTime

```java
// Current time
LocalTime now = LocalTime.now();  // 14:30:00.123

// Specific time
LocalTime time1 = LocalTime.of(14, 30);         // 14:30
LocalTime time2 = LocalTime.of(14, 30, 45);     // 14:30:45
LocalTime time3 = LocalTime.of(14, 30, 45, 123_000_000);  // with nanos

// Parse from string
LocalTime time4 = LocalTime.parse("14:30:45");

// Get parts
int hour = now.getHour();           // 14
int minute = now.getMinute();       // 30
int second = now.getSecond();       // 0
int nano = now.getNano();           // 123000000
```

### LocalDateTime

```java
// Current date-time
LocalDateTime now = LocalDateTime.now();  // 2024-01-15T14:30:00.123

// From LocalDate + LocalTime
LocalDateTime dt1 = LocalDateTime.of(2024, 1, 15, 14, 30);
LocalDateTime dt2 = LocalDateTime.of(LocalDate.now(), LocalTime.now());

// Parse from string
LocalDateTime dt3 = LocalDateTime.parse("2024-01-15T14:30:00");

// Extract date/time
LocalDate date = dt1.toLocalDate();
LocalTime time = dt1.toLocalTime();
```

### Adding/Subtracting

```java
LocalDate date = LocalDate.of(2024, 1, 15);

// Add
LocalDate nextWeek = date.plusWeeks(1);       // 2024-01-22
LocalDate nextMonth = date.plusMonths(1);     // 2024-02-15
LocalDate nextYear = date.plusYears(1);       // 2025-01-15
LocalDate nextDays = date.plusDays(10);       // 2024-01-25

// Subtract
LocalDate prevWeek = date.minusWeeks(1);      // 2024-01-08
LocalDate prevMonth = date.minusMonths(1);    // 2023-12-15

// With ChronoUnit (covered below)
LocalDate result = date.plus(5, ChronoUnit.DAYS);
```

---

## ZonedDateTime & Instant

### ZonedDateTime

**Matlab:** Timezone ke saath date/time — daylight saving handle karta hai.

```java
// Current time in default timezone
ZonedDateTime now = ZonedDateTime.now();

// Specific timezone
ZoneId zone = ZoneId.of("Asia/Kolkata");
ZonedDateTime indiaTime = ZonedDateTime.now(zone);

// Specific date-time-zone
ZonedDateTime specific = ZonedDateTime.of(
    2024, 1, 15, 14, 30, 0, 0, ZoneId.of("America/New_York")
);

// Convert between timezones
ZonedDateTime nyTime = ZonedDateTime.now(ZoneId.of("America/New_York"));
ZonedDateTime londonTime = nyTime.withZoneSameInstant(ZoneId.of("Europe/London"));

// Get timezone info
ZoneId zone = now.getZone();  // Asia/Kolkata
String zoneName = zone.getId();  // "Asia/Kolkata"
```

### Instant

**Matlab:** Machine-readable timestamp — epoch seconds/nanos since 1970-01-01T00:00:00Z.

```java
// Current instant
Instant now = Instant.now();  // 2024-01-15T09:00:00.123Z

// Epoch
long epochSeconds = now.getEpochSecond();  // 1705309200
long epochMillis = now.toEpochMilli();     // 1705309200123

// From epoch
Instant fromEpoch = Instant.ofEpochSecond(1705309200);
Instant fromMillis = Instant.ofEpochMilli(1705309200123L);

// Parse
Instant parsed = Instant.parse("2024-01-15T09:00:00Z");

// Add/subtract
Instant later = now.plusSeconds(60);
Instant earlier = now.minus(Duration.ofHours(1));

// Convert to ZonedDateTime
ZonedDateTime zoned = now.atZone(ZoneId.of("Asia/Kolkata"));
```

---

## Duration & Period

### Duration

**Matlab:** Time-based amount — seconds, minutes, hours, nanos.

```java
// Create duration
Duration d1 = Duration.ofSeconds(60);
Duration d2 = Duration.ofMinutes(30);
Duration d3 = Duration.ofHours(2);
Duration d4 = Duration.ofMillis(500);
Duration d5 = Duration.between(LocalTime.of(9, 0), LocalTime.of(17, 30));  // PT8H30M

// Operations
Duration total = d1.plus(d2);           // 30 minutes + 60 seconds
Duration diff = d2.minus(d1);           // 29 minutes
Duration multiplied = d1.multipliedBy(3); // 3 minutes

// Get parts
long seconds = d1.getSeconds();         // 60
long minutes = d1.toMinutes();          // 1
long hours = d1.toHours();              // 0
long millis = d1.toMillis();            // 60000

// Check
boolean isNegative = d1.isNegative();   // false
boolean isZero = d1.isZero();           // false

// Between two instants
Instant start = Instant.now();
Thread.sleep(100);
Instant end = Instant.now();
Duration elapsed = Duration.between(start, end);
```

### Period

**Matlab:** Date-based amount — years, months, days.

```java
// Create period
Period p1 = Period.ofYears(2);
Period p2 = Period.ofMonths(6);
Period p3 = Period.ofDays(15);
Period p4 = Period.of(2, 6, 15);  // 2 years, 6 months, 15 days

// Between two dates
Period age = Period.between(
    LocalDate.of(1995, 5, 20),
    LocalDate.of(2024, 1, 15)
);
System.out.println(age.getYears());   // 28
System.out.println(age.getMonths());  // 7
System.out.println(age.getDays());    // 26

// Operations
Period total = p1.plus(p2);  // 2 years + 6 months
```

---

## DateTimeFormatter

**Matlab:** Date/time ko format karna aur parse karna.

### Built-in Formatters

```java
LocalDateTime now = LocalDateTime.now();

// ISO formats
String iso = now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
// 2024-01-15T14:30:00

// Built-in formatters
String isoDate = now.format(DateTimeFormatter.ISO_LOCAL_DATE);  // 2024-01-15
```

### Custom Patterns

```java
// Custom pattern
DateTimeFormatter custom = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
String formatted = now.format(custom);  // 15/01/2024 14:30:00

// With locale
DateTimeFormatter french = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);
String frenchDate = LocalDate.now().format(french);  // "15 janvier 2024"

// Parse
LocalDate date = LocalDate.parse("15/01/2024", DateTimeFormatter.ofPattern("dd/MM/yyyy"));
```

### Pattern Letters

| Letter | Meaning | Example |
|--------|---------|---------|
| `yyyy` | 4-digit year | 2024 |
| `yy` | 2-digit year | 24 |
| `MM` | 2-digit month | 01 |
| `MMM` | Short month name | Jan |
| `MMMM` | Full month name | January |
| `dd` | 2-digit day | 15 |
| `EEE` | Short day name | Mon |
| `EEEE` | Full day name | Monday |
| `HH` | 24-hour | 14 |
| `hh` | 12-hour | 02 |
| `mm` | Minutes | 30 |
| `ss` | Seconds | 45 |
| `a` | AM/PM | PM |

---

## ZoneId & ChronoUnit

### ZoneId

```java
// Get all available zone IDs
Set<String> zones = ZoneId.getAvailableZoneIds();

// Create ZoneId
ZoneId india = ZoneId.of("Asia/Kolkata");
ZoneId ny = ZoneId.of("America/New_York");
ZoneId london = ZoneId.of("Europe/London");

// System default
ZoneId systemDefault = ZoneId.systemDefault();

// Check DST
ZoneRules rules = india.getRules();
boolean isDST = rules.isDaylightSavings(Instant.now());
```

### ChronoUnit

```java
LocalDate date1 = LocalDate.of(2024, 1, 15);
LocalDate date2 = LocalDate.of(2025, 3, 20);

// Between — days
long days = ChronoUnit.DAYS.between(date1, date2);     // 430
long weeks = ChronoUnit.WEEKS.between(date1, date2);   // 61
long months = ChronoUnit.MONTHS.between(date1, date2); // 14
long years = ChronoUnit.YEARS.between(date1, date2);   // 1

// With LocalTime
LocalTime t1 = LocalTime.of(9, 0);
LocalTime t2 = LocalTime.of(17, 30);
long hours = ChronoUnit.HOURS.between(t1, t2);         // 8
long minutes = ChronoUnit.MINUTES.between(t1, t2);     // 510
```

---

## TemporalAdjusters

**Matlab:** Date ko adjust karne ke liye pre-built strategies.

### Built-in Adjusters

```java
LocalDate date = LocalDate.of(2024, 1, 15);  // Monday

// First/Last day of month
LocalDate firstDay = date.with(TemporalAdjusters.firstDayOfMonth());    // 2024-01-01
LocalDate lastDay = date.with(TemporalAdjusters.lastDayOfMonth());      // 2024-01-31

// First/Last day of year
LocalDate firstDayOfYear = date.with(TemporalAdjusters.firstDayOfYear());  // 2024-01-01
LocalDate lastDayOfYear = date.with(TemporalAdjusters.lastDayOfYear());    // 2024-12-31

// Next/Previous day of week
LocalDate nextMonday = date.with(TemporalAdjusters.next(DayOfWeek.MONDAY));     // 2024-01-22
LocalDate prevMonday = date.with(TemporalAdjusters.previous(DayOfWeek.MONDAY)); // 2024-01-08

// First/Next Monday
LocalDate firstMonday = date.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));  // 2024-01-01
LocalDate nextOrSame = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));     // 2024-01-15 (today is Monday)

// First day of next month
LocalDate firstNextMonth = date.with(TemporalAdjusters.firstDayOfNextMonth());  // 2024-02-01
```

### Custom Adjuster

```java
// Next working day (skip weekends)
TemporalAdjuster nextWorkingDay = temporal -> {
    DayOfWeek dow = DayOfWeek.of(temporal.get(ChronoField.DAY_OF_WEEK));
    int add = 1;
    if (dow == DayOfWeek.FRIDAY) add = 3;
    else if (dow == DayOfWeek.SATURDAY) add = 2;
    return temporal.plus(add, ChronoUnit.DAYS);
};

LocalDate friday = LocalDate.of(2024, 1, 19);
LocalDate nextWorkDay = friday.with(nextWorkingDay);  // 2024-01-22 (Monday)
```

---

## Summary

| Class | Purpose | Example |
|-------|---------|---------|
| **LocalDate** | Date only (no time, no timezone) | `2024-01-15` |
| **LocalTime** | Time only (no date, no timezone) | `14:30:00` |
| **LocalDateTime** | Date + Time (no timezone) | `2024-01-15T14:30:00` |
| **ZonedDateTime** | Date + Time + Timezone | `2024-01-15T14:30:00+05:30[Asia/Kolkata]` |
| **Instant** | Machine timestamp (epoch) | `2024-01-15T09:00:00Z` |
| **Duration** | Time-based amount | `PT2H30M` |
| **Period** | Date-based amount | `P2Y6M15D` |
| **DateTimeFormatter** | Format/parse dates | `dd/MM/yyyy` |
| **TemporalAdjusters** | Date adjustment strategies | `firstDayOfMonth()` |
