# Code Quality & Best Practices

Clean code, reviews, layered design, technical debt, documentation, refactoring, aur Java **Lombok** — backend developer perspective se. Hinglish explanations + Java/Spring examples.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Clean Code Principles | [01-Clean-Code-Principles.md](./01-Clean-Code-Principles.md) | Not Started |
| 2 | Code Review Skills | [02-Code-Review-Skills.md](./02-Code-Review-Skills.md) | Not Started |
| 3 | Design Principles | [03-Design-Principles.md](./03-Design-Principles.md) | Not Started |
| 4 | Technical Debt | [04-Technical-Debt.md](./04-Technical-Debt.md) | Not Started |
| 5 | Documentation | [05-Documentation.md](./05-Documentation.md) | Not Started |
| 6 | Refactoring Techniques | [06-Refactoring-Techniques.md](./06-Refactoring-Techniques.md) | Not Started |
| 7 | Lombok | [07-Lombok.md](./07-Lombok.md) | Not Started |

---

## What's Inside Each File?

### [01 — Clean Code Principles](./01-Clean-Code-Principles.md)
SOLID (har principle Java snippet ke saath), DRY, KISS, YAGNI, Law of Demeter, meaningful naming, chhote functions guideline.

### [02 — Code Review Skills](./02-Code-Review-Skills.md)
Correctness, security, performance, readability, tests, error handling — kya dekhein; constructive feedback (question not criticize); feedback lena; PR size; author + reviewer checklist.

### [03 — Design Principles](./03-Design-Principles.md)
Separation of concerns (controller/service/repository), fail fast, defensive programming, immutability (`final`, value objects), composition over inheritance, programming to interfaces, Spring notes.

### [04 — Technical Debt](./04-Technical-Debt.md)
Intentional vs unintentional debt; measuring (complexity, coverage, dependency age, DORA); boy scout rule, refactoring sprints, strangler fig; TODO/FIXME with ticket refs; stakeholders ke saath baat.

### [05 — Documentation](./05-Documentation.md)
Comments (why not what), JavaDoc for public APIs, README sections, ADR template, Confluence/Notion vs repo docs, OpenAPI mention.

### [06 — Refactoring Techniques](./06-Refactoring-Techniques.md)
Extract Method/Class, Rename, Move Method, Introduce Parameter Object, Replace Conditional with Polymorphism, magic numbers → constants, decompose conditional, smell → technique map.

### [07 — Lombok](./07-Lombok.md)
`@Data`, `@Builder`, `@Value`, `@Slf4j`, constructors (`@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`), `@NonNull`, `@SneakyThrows`, **JPA entities + `@Data` pitfalls** (equals/hashCode/toString), builder + no-arg ctor.

---

## Recommended Learning Order

```
1. Clean Code (01)        ← naming + SOLID foundation
2. Design Principles (03) ← layered architecture
3. Refactoring (06)       ← practical moves
4. Code Review (02)        ← team practice
5. Technical Debt (04)   ← lifecycle / prioritization
6. Documentation (05)      ← communicate decisions
7. Lombok (07)             ← Java productivity + pitfalls
```

---

## Quick Reference

| Need | File |
|------|------|
| Split god class | 01 SRP, 06 Extract Class |
| Review checklist | 02 |
| Controller fat hai | 03 SoC |
| TODO cleanup | 04 + 05 |
| Why we chose X | 05 ADR |
| Replace switch | 06 Polymorphism |
| Entity pe Lombok | 07 Pitfalls |

---

## Companion Folders

- [API Design & Architecture](../API-Design-&-Architecture/)
- [Database Mastery](../Database-Mastery/)
- [Networking & Protocols](../Networking-&-Protocols/)

---

## Status Tracker

```
[ ] 01 — Clean Code Principles
[ ] 02 — Code Review Skills
[ ] 03 — Design Principles
[ ] 04 — Technical Debt
[ ] 05 — Documentation
[ ] 06 — Refactoring Techniques
[ ] 07 — Lombok
```

Topic complete hone par file header aur is README dono mein status update kar lena.
