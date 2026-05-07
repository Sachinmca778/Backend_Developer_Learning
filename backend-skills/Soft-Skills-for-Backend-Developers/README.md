# Soft Skills for Backend Developers

Code likhne ke beyond — wo skills jo **senior** engineer banate hain: clear thinking, accurate estimates, smooth cross-functional work, aur calm incident response. Sab Hinglish mein, real templates aur examples ke saath.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Problem-Solving Approach | [01-Problem-Solving-Approach.md](./01-Problem-Solving-Approach.md) | Not Started |
| 2 | Estimation Skills | [02-Estimation-Skills.md](./02-Estimation-Skills.md) | Not Started |
| 3 | Cross-team Collaboration | [03-Cross-team-Collaboration.md](./03-Cross-team-Collaboration.md) | Not Started |
| 4 | Incident Management | [04-Incident-Management.md](./04-Incident-Management.md) | Not Started |

---

## What's Inside Each File?

### [01 — Problem-Solving Approach](./01-Problem-Solving-Approach.md)
9-step approach: understand problem → clarifying questions → break down → identify unknowns → MVP thinking → edge cases (empty, boundary, concurrent, time/date, volume) → algorithm + data structure choice → time/space complexity (Big-O quick ref + N+1 query trap) → code/test/iterate. Worked example. Common pitfalls (code-before-think, over-engineer, premature optimize, no questions, solo heroics, endless spike).

### [02 — Estimation Skills](./02-Estimation-Skills.md)
Why estimation hard (Hofstadter, optimism bias, forgotten work, context switch), **story points** (Fibonacci, complexity not hours), **t-shirt sizing** (XS/S/M/L/XL), **planning poker** (simultaneous reveal, anchoring), time-based with PERT, breaking down (≤1-2 days, vertical slice > horizontal), **buffer** (20-30%), velocity tracking, "tell early when slip happens" template, anti-patterns (hidden padding, single-point estimates, forgetting test/review/deploy).

### [03 — Cross-team Collaboration](./03-Cross-team-Collaboration.md)
**Frontend** (OpenAPI contracts, mock servers with Prism, Springdoc, shared error envelope, CORS), **DevOps** (Actuator health probes, 12-factor config, deployment README), **QA** (test data seeds, bug report template, regression tests, test pyramid), **PM** (trade-off options, weekly updates, ADRs), **Security & Compliance**, other backend teams (versioning, async events). Communication tactics (async > sync, X-Y-A-B context pattern, acknowledge receipt, disagree respectfully). Common conflicts + resolutions.

### [04 — Incident Management](./04-Incident-Management.md)
Lifecycle (detect → triage → mitigate → resolve → post-mortem), **severity levels P0-P4** with response SLAs, detection (symptom-based alerts, alert fatigue tuning), triage (5-min checklist, declaring incident, IC/Tech Lead/Comms/Scribe roles), **mitigation > resolution** (rollback, feature flag off, scale up, restart, failover, circuit break), communication (internal Slack war room cadence, status page templates), **on-call rotation** (follow-the-sun, weekly, comp + best practices), runbook template, escalation paths, **blameless post-mortems** (full template with timeline, root cause, what went well/poorly, action items), pitfalls (hero culture, blame, action items ignored, premature resolution).

---

## Recommended Learning Order

```
1. Problem-Solving (01)        ← foundational thinking
2. Estimation (02)              ← apply thinking to scope work
3. Cross-team Collaboration (03) ← work with humans, not just code
4. Incident Management (04)     ← handle pressure when things break
```

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| New ticket — kaise approach karun? | 01 | Steps 1-2: Understand + Clarify |
| Edge cases miss kar deta hun | 01 | Step 6: Edge Cases |
| Big-O analysis | 01 | Step 8: Time/Space |
| Estimate dena hai — kaise? | 02 | Story Points + Buffer |
| Estimate galat tha — kya karun? | 02 | When You Realize Wrong |
| Frontend dev ke saath kaam | 03 | Working with Frontend |
| API contract kaha define karun? | 03 | API Contracts & Mock Servers |
| QA bug report bahut vague hai | 03 | Working with QA |
| PM ko status update | 03 | Weekly Update Format |
| 3 AM page aaya — kya karun? | 04 | Triage + Mitigate |
| Severity decide karna | 04 | Severity Levels P0-P4 |
| Rollback ya forward fix? | 04 | Mitigation > Resolution |
| Post-mortem kaise likhun? | 04 | Blameless Post-Mortems |
| Runbook chahiye | 04 | On-Call Rotation |

---

## Templates Reference

This folder has ready-to-use templates for:

- **Clarifying questions checklist** (file 01)
- **Edge case checklist** (file 01)
- **Estimate communication** (file 02)
- **Slip / "early heads up" message** (file 02)
- **Definition of Done** (file 02)
- **Deployment README** (file 03)
- **Bug report format** (file 03)
- **PM weekly update** (file 03)
- **ADR (Architecture Decision Record)** (file 03)
- **Status page updates** (file 04)
- **Runbook structure** (file 04)
- **Post-mortem template** (file 04)

→ Copy-paste, customize for your team.

---

## Companion Folders

- [API Design & Architecture](../API-Design-&-Architecture/) — contracts, versioning, idempotency
- [Database Mastery](../Database-Mastery/) — query understanding for estimates
- [Networking & Protocols](../Networking-&-Protocols/) — debugging during incidents
- [Code Quality & Best Practices](../Code-Quality-&-Best-Practices/) — clean code, reviews, ADRs
- [DevOps & CI/CD](../DevOps-&-CI-CD/) — monitoring, alerting, environment management
- [Security Best Practices](../Security-Best-Practices/) — security collaborator workflow

---

## Status Tracker

```
[ ] 01 — Problem-Solving Approach
[ ] 02 — Estimation Skills
[ ] 03 — Cross-team Collaboration
[ ] 04 — Incident Management
```

Topic complete hone par file header aur is README dono mein status update kar lena.

> Soft skills ka real test = production fire mein team ka reliable + calm member banna. Practice till instinctive.
