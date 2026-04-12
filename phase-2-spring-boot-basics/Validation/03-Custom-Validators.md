# Custom Validators

## Status: Not Started

---

## Table of Contents

1. [Creating Custom Annotations](#creating-custom-annotations)
2. [Implementing ConstraintValidator](#implementing-constraintvalidator)
3. [Registering Validator](#registering-validator)
4. [Cross-Field Validation](#cross-field-validation)
5. [Class-Level Constraint](#class-level-constraint)
6. [Complete Examples](#complete-examples)

---

## Creating Custom Annotations

**Matlab:** Apna validation annotation banana — jab built-in annotations kaafi na hon.

### Basic Structure

```java
@Documented
@Constraint(validatedBy = PhoneValidator.class)  // Validator class reference
@Target({ElementType.FIELD, ElementType.PARAMETER})  // Kahan lag sakta hai
@Retention(RetentionPolicy.RUNTIME)  // Runtime pe available
public @interface ValidPhone {

    // Error message
    String message() default "Invalid phone number format";

    // Validation groups (standard)
    Class<?>[] groups() default {};

    // Payload (metadata — rarely used)
    Class<? extends Payload>[] payload() default {};

    // Custom attribute
    String country() default "IN";  // Country code
}
```

### Annotation Elements Explained

| Element | Purpose | Required? |
|---------|---------|-----------|
| **message()** | Error message | ✅ Yes |
| **groups()** | Validation groups | ✅ Yes (standard) |
| **payload()** | Metadata | ✅ Yes (standard) |
| **Custom attributes** | Validator ko parameters pass karne | Optional |

---

## Implementing ConstraintValidator

**Matlab:** Validation logic likhna — `ConstraintValidator<A, T>` implement karna.

### Simple Validator

```java
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private String country;  // Annotation se value milega

    @Override
    public void initialize(ValidPhone constraintAnnotation) {
        // Annotation se value read karo
        this.country = constraintAnnotation.country();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Null check — @NotBlank alag se lagao
        if (value == null || value.isBlank()) {
            return true;  // Null validation @NotBlank karega
        }

        // Country-specific validation
        return switch (country) {
            case "IN" -> value.matches("^\\+91[0-9]{10}$");       // India
            case "US" -> value.matches("^\\+1[0-9]{10}$");        // US
            case "UK" -> value.matches("^\\+44[0-9]{10}$");       // UK
            default -> value.matches("^\\+[0-9]{1,3}[0-9]{6,14}$"); // International
        };
    }
}
```

### Usage

```java
public class UserRequest {

    @NotBlank(message = "Phone is required")
    @ValidPhone(country = "IN", message = "Indian phone number must be in +91XXXXXXXXXX format")
    private String phone;

    @ValidPhone(country = "US")  // Default message use hoga
    private String usPhone;
}
```

---

## Registering Validator

Validator automatically register ho jata hai jab `ConstraintValidator` implement karte ho. Spring Boot auto-detect kar leta hai.

### Manual Registration (Optional)

```java
@Configuration
public class ValidationConfig {

    @Bean
    public Validator validator() {
        return Validation.byProvider(HibernateValidator.class)
            .configure()
            .addValidator(new PhoneValidator())  // Manual registration
            .buildValidatorFactory()
            .getValidator();
    }
}
```

### Multiple Validators for Same Annotation

```java
// Different validators for different types
public class IndianPhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.matches("^\\+91[0-9]{10}$");
    }
}

public class USPhoneValidator implements ConstraintValidator<ValidPhone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.matches("^\\+1[0-9]{10}$");
    }
}

// ConstraintValidatorContext se dynamically decide karo
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private String country;

    @Override
    public void initialize(ValidPhone annotation) {
        this.country = annotation.country();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;

        return switch (country) {
            case "IN" -> value.matches("^\\+91[0-9]{10}$");
            case "US" -> value.matches("^\\+1[0-9]{10}$");
            default -> true;
        };
    }
}
```

---

## Cross-Field Validation

**Matlab:** Multiple fields ko compare karke validate karna — e.g., password == confirmPassword.

### Approach 1: Class-Level Constraint

```java
// Annotation
@Documented
@Constraint(validatedBy = PasswordMatchesValidator.class)
@Target({ElementType.TYPE})  // Class level pe lagega
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatches {
    String message() default "Passwords do not match";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String password();
    String confirmPassword();
}

// Validator
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    private String passwordField;
    private String confirmPasswordField;

    @Override
    public void initialize(PasswordMatchers constraintAnnotation) {
        this.passwordField = constraintAnnotation.password();
        this.confirmPasswordField = constraintAnnotation.confirmPassword();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            // Reflection se fields access karo
            var password = BeanUtils.getProperty(value, passwordField);
            var confirmPassword = BeanUtils.getProperty(value, confirmPasswordField);

            if (password == null || confirmPassword == null) {
                return true;  // @NotBlank handle karega
            }

            return password.equals(confirmPassword);

        } catch (Exception e) {
            return false;
        }
    }
}
```

### Usage

```java
@PasswordMatches(
    password = "password",
    confirmPassword = "confirmPassword",
    message = "Password and Confirm Password must match"
)
public class UserRegistrationRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Confirm Password is required")
    private String confirmPassword;
}
```

---

## Class-Level Constraint

**Matlab:** Pure object pe validation lagana — multiple fields ke basis pe.

### Example: Date Range Validation

```java
// Annotation
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {
    String message() default "Start date must be before end date";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String startDate();
    String endDate();
}

// Validator
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {

    private String startField;
    private String endField;

    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startField = constraintAnnotation.startDate();
        this.endField = constraintAnnotation.endDate();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            LocalDate start = (LocalDate) BeanUtils.getProperty(value, startField);
            LocalDate end = (LocalDate) BeanUtils.getProperty(value, endField);

            if (start == null || end == null) {
                return true;  // Individual @NotNull alag se handle karega
            }

            return !start.isAfter(end);

        } catch (Exception e) {
            return false;
        }
    }
}
```

### Usage

```java
@ValidDateRange(
    startDate = "startDate",
    endDate = "endDate",
    message = "Start date must be before or equal to end date"
)
public class EventRequest {

    @NotBlank(message = "Event name is required")
    private String name;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @FutureOrPresent(message = "End date must be today or in the future")
    private LocalDate endDate;
}
```

---

## Complete Examples

### Example 1: Strong Password Validator

```java
// Annotation
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {
    String message() default "Password must contain uppercase, lowercase, digit, and special character";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int minLength() default 8;
    int maxLength() default 100;
}

// Validator
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    private int minLength;
    private int maxLength;

    @Override
    public void initialize(StrongPassword annotation) {
        this.minLength = annotation.minLength();
        this.maxLength = annotation.maxLength();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;  // @NotBlank handle karega
        }

        if (value.length() < minLength || value.length() > maxLength) {
            return false;
        }

        boolean hasUpper = value.matches(".*[A-Z].*");
        boolean hasLower = value.matches(".*[a-z].*");
        boolean hasDigit = value.matches(".*[0-9].*");
        boolean hasSpecial = value.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}
```

### Example 2: Unique Username Validator (Database Check)

```java
// Annotation
@Documented
@Constraint(validatedBy = UniqueUsernameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueUsername {
    String message() default "Username already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator — Spring dependency inject kar sakte ho
@Component  // Spring bean banao
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserRepository userRepository;

    // Constructor injection
    @Autowired
    public UniqueUsernameValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isBlank()) {
            return true;  // @NotBlank handle karega
        }

        // Database mein check karo
        return !userRepository.existsByUsername(username);
    }
}
```

### Example 3: Valid OTP Validator

```java
// Annotation
@Documented
@Constraint(validatedBy = OtpValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOtp {
    String message() default "OTP must be 6 digits";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    int length() default 6;
}

// Validator
public class OtpValidator implements ConstraintValidator<ValidOtp, String> {

    private int length;

    @Override
    public void initialize(ValidOtp annotation) {
        this.length = annotation.length();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        String regex = "^[0-9]{" + length + "}$";
        return value.matches(regex);
    }
}
```

### Example 4: At Least One Field Required

```java
// Annotation
@Documented
@Constraint(validatedBy = AtLeastOneFieldValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneField {
    String message() default "At least one field must be provided";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String[] fields();
}

// Validator
public class AtLeastOneFieldValidator implements ConstraintValidator<AtLeastOneField, Object> {

    private String[] fieldNames;

    @Override
    public void initialize(AtLeastOneField constraintAnnotation) {
        this.fieldNames = constraintAnnotation.fields();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        try {
            for (String fieldName : fieldNames) {
                Object fieldValue = BeanUtils.getProperty(value, fieldName);
                if (fieldValue != null && !fieldValue.toString().isBlank()) {
                    return true;  // At least one field has value
                }
            }
            return false;  // Sab null ya empty hain
        } catch (Exception e) {
            return false;
        }
    }
}
```

### Usage

```java
@AtLeastOneField(
    fields = {"email", "phone"},
    message = "At least one of email or phone must be provided"
)
public class ContactRequest {

    @Email(message = "Email should be valid")
    private String email;

    @ValidPhone
    private String phone;
}
```

---

## ConstraintValidatorContext — Custom Error Messages

```java
public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Default constraint disable karo
        context.disableDefaultConstraintViolation();

        // Custom error message set karo
        context.buildConstraintViolationWithTemplate("Passwords must match exactly")
            .addPropertyNode("confirmPassword")
            .addConstraintViolation();

        return passwordsMatch;
    }
}
```

---

## Summary

| Concept | Key Point |
|---------|-----------|
| **Custom Annotation** | `@interface` banao — `@Constraint(validatedBy = ...)` lagao |
| **ConstraintValidator** | `implements ConstraintValidator<Annotation, Type>` |
| **initialize()** | Annotation se values read karo |
| **isValid()** | Validation logic likho — null ko skip karo |
| **Class-Level** | `@Target(ElementType.TYPE)` — cross-field validation |
| **Cross-Field** | Reflection se multiple fields access karo |
| **Spring Injection** | `@Component` + constructor injection — DB checks possible |
| **Custom Messages** | `ConstraintValidatorContext` se dynamic error messages |
