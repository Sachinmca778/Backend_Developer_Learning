# Quick Reference Charts & Interview Preparation Strategy

Yeh cheatsheet PDF ke pages 13-15 ke standard reference charts aur proven preparation strategy ka summary hai.

---

## 📌 1. Key Annotations Comparison

| Annotation | Purpose | Scope | Layer |
| :--- | :--- | :--- | :--- |
| **`@Component`** | Generic stereotype for any Spring-managed component | Class level | Cross-cutting / Utility |
| **`@Service`** | Business logic facade, transactions integration | Class level | Service Layer |
| **`@Repository`** | Data access, automatic SQL exception translation | Class level | DAO / Persistence Layer |
| **`@Controller`** | Web requests handler, returns view names (HTML/JSP) | Class level | Web Presentation Layer |
| **`@RestController`**| Combines `@Controller` and `@ResponseBody` (Returns JSON/XML) | Class level | REST API Layer |

---

## 🌐 2. HTTP Status Codes Cheat Sheet

| Code | Meaning | When to Use |
| :--- | :--- | :--- |
| **200** | OK | Successful request (GET, PUT, PATCH). |
| **201** | Created | Resource successfully created (POST). |
| **204** | No Content | Action succeeded but no body to return (DELETE). |
| **400** | Bad Request | Client validation failed / invalid JSON payload. |
| **401** | Unauthorized | Authentication token missing ya invalid hai. |
| **403** | Forbidden | Authenticated user lacks required permission/role. |
| **404** | Not Found | Requested URL ya resource database me nahi mila. |
| **409** | Conflict | Duplicate entry (e.g. Email already registered). |
| **429** | Too Many Requests | Rate limit exceeded. |
| **500** | Internal Server Error | Uncaught exception ya server code crash. |
| **502** | Bad Gateway | Reverse proxy received invalid upstream response. |
| **503** | Service Unavailable | Server overloaded ya service down hai. |
| **504** | Gateway Timeout | Upstream service did not reply in time. |

---

## ⚡ 3. Java Collection Performance (Big-O Time Complexity)

| Operation | `ArrayList` | `LinkedList` | `HashMap` | `TreeMap` | `HashSet` |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Add / Insert** | $O(1)$ amortized | $O(1)$ at ends | $O(1)$ | $O(\log n)$ | $O(1)$ |
| **Get (by Index / Key)** | $O(1)$ | $O(n)$ | $O(1)$ | $O(\log n)$ | N/A |
| **Remove** | $O(n)$ | $O(1)$ (with ref) | $O(1)$ | $O(\log n)$ | $O(1)$ |
| **Search / Contains** | $O(n)$ | $O(n)$ | $O(1)$ | $O(\log n)$ | $O(1)$ |

---

## 🗓️ 4. 8-Week Preparation Roadmap

- **Week 1-2: Core Java Mastery**
  - Focus on Questions 1 to 100.
  - Practice OOP principles, Strings pool, Collections internals, and Exceptions.
- **Week 3-4: Spring Boot Deep Dive**
  - Focus on Questions 101 to 200.
  - Master Dependency Injection, Auto-Configuration, JPA/Hibernate, Spring Security.
- **Week 5-6: Advanced Concepts & System Design**
  - Focus on Questions 201 to 300.
  - Concurrency, Kafka, Microservices patterns, Resilience4j, Caching, Docker/K8s.
- **Week 7-8: Modern Java & Scenario Practice**
  - Focus on Questions 301 to 330.
  - Java 21 Virtual Threads, Real-world production troubleshooting scenarios, STAR method behavioral prep.

---

## 💡 5. Final Tips for the Interview

### Before the Interview:
1. Company ka tech stack aur architecture deeply research karein.
2. 2-3 detailed project architecture examples prepare karein jisme aapka individual impact clear ho.
3. Code whiteboard / shared screen par bina IDE auto-complete ke type karne ki practice karein.

### During the Interview:
1. **Think Out Loud:** Problem solve karte waqt silent na rahein; apna thought process interviewer ko batate rahein.
2. **Ask Clarifying Questions:** Edge cases, scale (RPS), aur requirements confirm karein.
3. **Trade-offs Explain Karein:** "Main approach A use kar sakta hoon jisme O(1) time hai par space O(n) lagegi, ya approach B jisme space O(1) hai par time O(n) lagegi."
4. **Show Curiosity & Humility:** Agar koi specific syntax nahi aata, to honestly admit karein aur conceptual logic share karein.
