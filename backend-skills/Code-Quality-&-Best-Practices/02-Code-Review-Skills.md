# Code Review Skills

## Status: Not Started

---

## Table of Contents

1. [Code Review Kyun Zaroori Hai?](#code-review-kyun-zaroori-hai)
2. [Kya Dekhna Hai](#kya-dekhna-hai)
3. [Constructive Feedback](#constructive-feedback)
4. [Feedback Receive Karna](#feedback-receive-karna)
5. [PR Size](#pr-size)
6. [Review Checklist](#review-checklist)
7. [Anti-Patterns](#anti-patterns)
8. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Code Review Kyun Zaroori Hai?

**Matlab:** Code merge hone se pehle **dusri aankh** — bugs kam, knowledge spread, style consistent, security catch.

**Goals (priority order often):**
1. Correctness & safety
2. Security & privacy
3. Maintainability
4. Performance (jab relevant)
5. Style (automate jitna ho sake — linter/formatter)

---

## Kya Dekhna Hai

### 1. Correctness

- Requirement **actually** poori ho rahi hai?
- Edge cases: null, empty list, boundary values, concurrency?
- Idempotency / retries — duplicate safe?
- Business rules galat to nahi?

```java
// ❌ Division by zero possibility?
double avg = sum / count;

// ✅
double avg = count == 0 ? 0 : sum / count;
```

### 2. Security

- Input validation, SQL injection, XSS, path traversal?
- Secrets hardcoded? Logs mein PII?
- AuthZ missing? (sirf auth — role check?)
- Dependency known vulnerabilities?

### 3. Performance

- N+1 queries, unnecessary loops in hot path?
- Big payloads unbounded?
- **Measure first** — premature optimization avoid; still obvious O(n²) where O(n) possible → flag.

### 4. Readability

- Naming, structure, comments (why)?
- Dead code, commented-out blocks?
- Too clever — KISS?

### 5. Test Coverage

- Critical paths covered?
- Tests meaningful (assert behavior, not implementation)?
- Flaky tests?

### 6. Error Handling

- Exceptions swallowed?
- Wrong exception type?
- User-facing errors leak internals?
- Retry/backoff where needed?

### 7. Naming & API Design

- Public API clear? Breaking changes documented?

---

## Constructive Feedback

### Principles

| Do | Avoid |
|----|-------|
| **Question** ("Kya yahan race ho sakti hai?") | Accusation ("Yeh galat hai tumne") |
| Specific + suggest alternative | Vague ("yeh achha nahi hai") |
| Praise useful parts | Sirf negativity |
| Separate nitpick vs blocker | Sab ek severity mein |

### Comment Examples

```text
✅ "Agar `user` null ho toh yahan NPE — early return ya Optional consider karein?"
✅ "Security: yeh endpoint authenticate hai? `@PreAuthorize` missing lag raha hai."
✅ "Nice extraction of `PricingCalculator` — readable ho gaya."

❌ "Bad code."
❌ "Why would anyone do this?"
```

### Severity Labels (team convention)

- **Blocker** — merge nahi until fixed
- **Major** — fix before merge ideally
- **Minor / Nit** — follow-up OK
- **Suggestion** — optional improvement

---

## Feedback Receive Karna

**Professionally:**
- Review **code** hai, personal attack nahi — assume good intent
- Pushback **data se**: benchmark, spec link, trade-off explain
- Small suggestions → accept generously; ego kam, shipping + quality zyada
- Agree to disagree → escalate to tech lead / ADR

```text
Author: "Thanks, valid point — I'll add null check."
Author: "Is pattern hum pehle se PaymentService mein use kar rahe hain — consistency ke liye same rakha?"
```

---

## PR Size

**Matlab:** **Chhote PR** = faster, deeper reviews.

| Size | Typical outcome |
|------|-----------------|
| < ~200 lines (excluding generated) | Reviewers actually read carefully |
| 500+ lines | LGTM without deep read risk |
| 1000+ mixed concerns | Almost impossible good review |

**Tips:**
- Feature ko slices mein todo (API contract → impl → tests)
- Refactor alag PR (possible toh)
- Mechanical rename/format **alag** PR — noise kam

**When big PR unavoidable:** Clear description, commit breakdown, walkthrough call.

---

## Review Checklist

Copy-paste friendly — adapt per team.

### Author (before requesting review)

- [ ] Tests pass locally / CI green
- [ ] Self-review diff (GitHub/GitLab)
- [ ] Description: **what**, **why**, how to test
- [ ] Screenshots / API examples if UX/API change
- [ ] Linked ticket (JIRA/Linear)
- [ ] Migration / flag / rollout notes if needed

### Reviewer

- [ ] Correctness & edge cases
- [ ] Security & sensitive data
- [ ] Error paths & logging (appropriate level)
- [ ] Tests adequate & naming clear
- [ ] Performance obvious issues
- [ ] Naming & structure readable
- [ ] Documentation updated (README, OpenAPI, ADR)
- [ ] No unrelated drive-by changes (scope creep)

---

## Anti-Patterns

| Anti-pattern | Problem |
|--------------|---------|
| Rubber stamping | "LGTM" without reading |
| Bike-shedding | Hours on naming, zero on logic bugs |
| Scope creep in review | "Also rewrite whole module" |
| Delay by perfectionism | Nitpick infinite — ship blocked |
| Only senior reviews | Knowledge silos |

---

## Summary Cheat Sheet

| Dimension | Quick checks |
|-----------|------------|
| Correctness | Requirements, edge cases, concurrency |
| Security | Input, authz, secrets, logs |
| Performance | N+1, hot path, bounds |
| Readability | Naming, structure, KISS |
| Tests | Meaningful, stable |
| Errors | Don't swallow; user-safe messages |
| Feedback | Questions, specifics, severity |
| PR size | Small > large |

---

## Practice

1. Apne ek purane PR par checklist lagao — kya miss hua tha?
2. Do negative comments ko constructive rewrite karo.
3. 800-line PR ko mentally 3 logical PRs mein kaise split karoge — outline likho.
4. Ek security-focused review comment likho (example vulnerability ke liye).
