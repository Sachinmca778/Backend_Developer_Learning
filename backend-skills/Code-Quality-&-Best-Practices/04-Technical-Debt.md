# Technical Debt

## Status: Not Started

---

## Table of Contents

1. [Technical Debt Kya Hai?](#technical-debt-kya-hai)
2. [Types of Debt](#types-of-debt)
3. [Measuring Debt](#measuring-debt)
4. [Addressing Strategies](#addressing-strategies)
5. [Documentation in Code](#documentation-in-code)
6. [Communication with Stakeholders](#communication-with-stakeholders)
7. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Technical Debt Kya Hai?

**Matlab:** Aaj **shortcut** liya — kal **extra cost** (slower features, more bugs, harder onboarding). Ward Cunningham metaphor: borrowing against future velocity.

**Not always bad:** Conscious trade-off ("ship MVP, refactor next sprint") vs unconscious mess.

---

## Types of Debt

### Intentional / Prudent

- Time-boxed prototype → production hardening planned
- Temporary feature flag + quick path → cleanup ticket filed
- **Documented** decision with owner & timeline

### Unintentional / Reckless

- "Copy-paste karke chalao"
- No tests "baad mein likhenge"
- No reviews — debt silently grows

### Environmental / Bit Rot

- Dependencies age → security / compat debt
- Team churn → knowledge lost
- Architecture once fit, ab scale par break

---

## Measuring Debt

No single number — **signals** combine karo.

### Code Complexity

- **Cyclomatic complexity** — branches per method (SonarQube, IDE inspections)
- Large god classes, deep nesting

### Test Coverage

- Line/branch coverage **with quality** — 80% meaningless if asserts weak
- Mutation testing (PIT) — tests actually catch bugs?

### Dependency Age / Risk

- Dependabot / Snyk / OWASP — CVE count, how far behind latest?
- Deprecated APIs still in use?

### Change Failure Rate / Lead Time

- DORA metrics — debt often shows as slow delivery & incidents

### Static Analysis

- SonarQube "technical debt ratio", code smells
- SpotBugs, Error Prone (Java)

### Qualitative

- Developer surveys ("where afraid to touch?")
- Onboarding time new hires

---

## Addressing Strategies

### Boy Scout Rule

**"Leave the campground cleaner than you found it."**

Har PR touch karte waqt **chhota** improvement: rename unclear variable, extract method, one extra test — **without** blowing scope.

### Refactoring Sprints / Cleanup Budget

- Reserve **10–20%** capacity recurring
- Or quarterly "quality sprint"

### Strangler Fig Pattern

Legacy system ko **dhire-dhire** replace — new path alongside old, traffic shift incrementally.

```
Old Monolith          New Service
    │                      │
    └── facade/router ─────┘
         gradually % to new
```

### Branch by Abstraction

Feature toggle + abstraction layer — big bang rewrite avoid.

### Pay Debt When Touching

Rule: **same area** change ho rahi hai toh related debt fix — unrelated giant refactor same PR mat mix karo.

---

## Documentation in Code

### TODO / FIXME with Ticket Reference

```java
// FIXME GH-1234: Replace sync HTTP call with async + circuit breaker
// TODO APP-567: Remove after migration v2 complete (target: 2026-Q2)
```

**Avoid:** naked `TODO` with no owner/trackability — becomes cemetery.

### ADRs for Big Debt Decisions

Architecture Decision Record — **why** shortcut taken, **when** revisit (see `05-Documentation.md`).

### Debt Register (Optional)

Spreadsheet or wiki: item, impact, cost to fix, priority — visible to PM.

---

## Communication with Stakeholders

- Translate tech debt to **risk & velocity**: "This area causes 40% of incidents"
- Offer **options**: fix now vs next quarter vs accept risk
- Link debt payoff to **business outcomes** (reliability, compliance)

---

## Summary Cheat Sheet

| Type | Example |
|------|---------|
| Intentional | Documented MVP shortcut |
| Unintentional | No tests, no review |
| Environmental | Old deps, scale mismatch |

| Measure | Tools / Ideas |
|---------|----------------|
| Complexity | SonarQube, IDE |
| Coverage | JaCoCo + review tests |
| Security debt | Snyk, Dependabot |
| Delivery | DORA metrics |

| Strategy | Use when |
|----------|----------|
| Boy scout | Every PR |
| Refactor sprint | Accumulated mess |
| Strangler fig | Legacy replacement |
| TODO+ticket | Track unfinished work |

---

## Practice

1. Apni codebase mein 3 debt items list karo — intentional vs unintentional classify karo.
2. Ek naked `TODO` ko ticket-linked comment mein rewrite karo.
3. Strangler fig se ek legacy endpoint migrate karne ka high-level plan likho.
