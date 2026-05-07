# Estimation Skills

## Status: Not Started

---

## Table of Contents

1. [Why Estimation Matters](#why-estimation-matters)
2. [Why Estimation Is Hard](#why-estimation-is-hard)
3. [Story Points](#story-points)
4. [T-Shirt Sizing](#t-shirt-sizing)
5. [Planning Poker](#planning-poker)
6. [Time-Based Estimates](#time-based-estimates)
7. [Breaking Down Tasks](#breaking-down-tasks)
8. [Buffer for Unknowns](#buffer-for-unknowns)
9. [Velocity & Tracking](#velocity--tracking)
10. [When You Realize the Estimate Is Wrong](#when-you-realize-the-estimate-is-wrong)
11. [Common Anti-Patterns](#common-anti-patterns)
12. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Why Estimation Matters

**Matlab:** Stakeholders ko **planning** karni hoti hai — "kab milega?" answer chahiye.

### Stakeholders' Use

- **Product**: roadmap planning, customer commitments
- **Marketing**: launch announcements
- **Sales**: deal closure
- **Engineering**: prioritization, capacity, on-call

### Without Estimates

Chaos: "kab kya hoga" pata nahi. Endless slip. Trust loss.

### Reality Check

> Estimates are **forecasts**, not commitments. Adjust as new info emerges.

---

## Why Estimation Is Hard

### Hofstadter's Law

> "It always takes longer than you expect, even when you take into account Hofstadter's Law."

### Reasons

- **Unknowns** — discovered during work
- **Optimism bias** — "happy path" assumed
- **Forgotten work** — testing, deployment, docs, code review, bug fixes
- **Interruptions** — meetings, support, on-call
- **Coordination** — waiting on other teams
- **Context switching** — multitasking penalty

### "Programmer Time" vs Real

```
Programmer thinks:  "2 hours of coding"
Real world:         + 1h tests
                    + 30m PR review back-and-forth
                    + 30m staging deploy + verify
                    + 1h fixing edge case found in QA
                    + 30m docs / handoff
                    = 5h actual
```

→ Apne estimates **mein dev ke aas-paas saari activity count karo**.

---

## Story Points

**Matlab:** Relative complexity unit (not hours). Compare task to **other tasks**.

### Common Scales

#### Fibonacci-ish (Most Common)

```
1, 2, 3, 5, 8, 13, 20, 40, 100
```

> Bigger numbers → less precise (right — bigger work has more uncertainty).

#### Linear

```
1, 2, 3, 4, 5, ...
```

→ Discouraged — implies false precision at higher numbers.

### Why Points Not Hours?

| Hours | Points |
|-------|--------|
| Implies precision (ya 4h ya 5h) | Comparative (this is bigger than that) |
| Different devs different speeds | Team-level abstraction |
| Tied to "should take this long" | Tied to **complexity + risk + size** |

### What Goes Into a Point?

- **Complexity** (logic difficulty)
- **Effort** (sheer amount)
- **Uncertainty** (unknowns)

### Reference Stories

Anchor your scale:

```
"Adding a new endpoint with simple CRUD" = 2 points
"Migrating column from VARCHAR to JSONB" = 5 points
"Implementing pub/sub messaging" = 13 points
```

→ Compare new tasks to these references.

### Don't Convert to Hours

```
❌ "1 point = 4 hours"
```

Defeats the purpose. Stay relative.

---

## T-Shirt Sizing

**Matlab:** Even rougher — for **early** estimates / roadmap.

```
XS — trivial
S  — small (a day or two)
M  — medium (~week)
L  — large (~couple weeks)
XL — huge (month+)
XXL — needs to be broken down (don't accept)
```

### When to Use

- Roadmap discussions ("Q3 me yeh feature kya scale ka hai?")
- Pre-grooming triage
- Quick "yes/no/maybe" capacity check

### Convert Later

XS/S/M etc. → broken into stories with points during sprint planning.

---

## Planning Poker

**Matlab:** Team estimation ritual. Reduces anchoring + leverages collective wisdom.

### Process

```
1. PM/lead reads story aloud
2. Q&A — clarifying questions
3. Each engineer simultaneously reveals card (1, 2, 3, 5, 8, ...)
4. If close (2 vs 3): take median or discuss briefly
5. If wide (2 vs 13): highest + lowest explain
6. Re-vote
7. Converge to single estimate
```

### Why Simultaneous?

Avoids **anchoring**: if senior says "8" first, juniors anchor to 8.

### Tools

- Web: PlanITPoker, Scrum Poker, Jira native
- Physical cards (remote: emoji vote)

### Healthy Signs

- Discussion when wide spread
- Newer team members feel safe to vote low/high
- Estimates don't always match — variance is information

### Unhealthy Signs

- One loud voice always wins
- Junior devs always pick same as senior
- Same number every time → no thought

---

## Time-Based Estimates

When asked for hours/days (some teams skip points):

### Use Ranges

```
❌ "It'll take 3 days"
✅ "2-5 days, most likely 3"
```

→ Communicates uncertainty.

### Three-Point Estimate (PERT)

```
Best case (B)     = 1 day
Most likely (M)   = 3 days
Worst case (W)    = 7 days

Expected = (B + 4M + W) / 6 = (1 + 12 + 7) / 6 = ~3.3 days
Std dev  = (W - B) / 6 = 1 day
```

→ Communicates: ~3.3 days expected, but ±1 day variance.

### Time Per Activity

```
Total work = code + tests + review + deploy + buffer
Code 60% | Tests 20% | Review 10% | Deploy/buffer 10%
```

If "core code" feels like 3 days → total maybe 5 days.

---

## Breaking Down Tasks

> Smaller pieces = better estimates.

### Rule of Thumb

> Each task ≤ **1-2 days**. If bigger, break further.

### Why?

- Smaller = less variance
- Daily progress visible
- Easier to PR review
- Better feedback loop
- Can hand off / parallelize

### Decomposition Strategies

#### Vertical Slice (Preferred)

```
Notifications:
├── Story 1: Send + persist email notification (DB + email integration)
├── Story 2: List endpoint (return user's notifications)
├── Story 3: Mark-as-read endpoint
└── Story 4: Push notification channel
```

→ Each slice **shippable** (end-to-end through layers).

#### Horizontal Slice (Avoid)

```
Notifications:
├── Story 1: All DB schema
├── Story 2: All services
├── Story 3: All controllers
└── Story 4: All tests
```

→ Nothing shippable until end. Hard to test integration.

### "Tracer Bullet" / "Walking Skeleton"

Smallest end-to-end version of feature: simplest path through all layers, then iterate.

---

## Buffer for Unknowns

### Why Buffer?

- Forgotten edge cases
- Bugs found in QA
- Code review feedback (rework)
- Production fire while working
- Tech surprises ("oh that lib doesn't support X")

### Standard Buffer

```
Known unknowns:    +20-30%
Big unknowns:      +50-100% (or spike first)
```

### Don't Hide Buffer

Communicate it: *"This is 5 days estimate including 1 day buffer for unknowns."*

→ Stakeholders trust honest ranges over inflated single numbers.

### When Buffer Won't Save You

```
Unknown unknowns — you didn't know to budget for these.
→ Spike (time-boxed) before estimating.
```

---

## Velocity & Tracking

**Matlab:** Team capacity over time — "average points per sprint."

### How

```
Sprint 1: 28 points completed
Sprint 2: 32 points
Sprint 3: 25 points
Average velocity ≈ 28 points / sprint
```

→ Use to plan: "Next sprint we can take ~28 points."

### Stable Velocity Takes Time

5-6 sprints minimum to converge. Early sprints have variance.

### Velocity Gotchas

- ❌ **Comparing teams** ("Team A does 50 points, Team B does 25") — points are team-specific!
- ❌ **Pressure to inflate** — velocity becomes target, gaming begins
- ❌ **Counting incomplete** — only count Done

### Burn Charts

#### Burn-down

```
Y: remaining points
X: sprint days
Trend: down to 0
```

#### Burn-up

```
Y: completed points
X: sprint days
Trend: up to total
```

→ Reveals scope creep visibly (total line goes up).

---

## When You Realize the Estimate Is Wrong

> **Tell people early.** Don't hope it'll work out.

### When You're Behind

```
Day 2 of "5-day task" — already realize: it's 8-9 days.

❌ Stay quiet, hope to catch up
✅ "Update: I hit X complication. New estimate: 8-9 days. Options:
    a) Take longer
    b) Cut scope of Y
    c) Add help
    d) Defer to next sprint"
```

### Communication Template

```
Subject: [Slip Update] Feature ABC — new estimate

Original: 5 days (ending Friday)
Now expecting: ~9 days (ending next Wednesday)

Why:
- Discovered legacy code requires refactoring before adding new endpoint
- Auth migration in dependency took 1 day not budgeted

Options:
1. Continue as-is, push by 4 days
2. Drop refactor (technical debt added) → finish Friday
3. Pair with @other-dev to parallelize

Recommendation: Option 1.
```

### Why Early > Late

- Stakeholders have **time** to adjust plans
- Scope cuts can be discussed
- Trust is built on **honest comms**, not perfect estimates
- Late slip = blindside = trust damage

### When You're Ahead

```
✅ "Done early — what next?"
```

→ Pull next-priority work or polish (tests, docs, refactoring).

❌ Don't fake busy. Time gained from estimation buffer is fine.

---

## Common Anti-Patterns

### 1. "Padding" Estimates Secretly

Hidden 50% buffer. Eventually transparent → trust eroded.

### 2. Compressing Under Pressure

> "Boss wants it Friday — say 5 days even though it's 10."

→ Slip guaranteed → worse than honest 10 days.

### 3. Estimating Without Understanding

Without breakdown / questions → numbers are guesses.

### 4. Forgetting Non-Coding Work

Tests, code review, deploy, docs, monitoring — all count.

### 5. Single-Point Estimates

```
❌ "3 days"
✅ "3-5 days, most likely 4"
```

### 6. Estimating Other People's Work

Engineer doing the work estimates — they're closest to it.

### 7. "Done" Without Definition of Done

Define DoD upfront:
- Code merged
- Tests passing (incl. coverage threshold)
- Code reviewed
- Documented
- Deployed to staging
- QA signed off
- Monitoring/alerting set

### 8. Reusing Old Estimates Blindly

Similar feature ≠ same effort. Re-estimate with current context.

---

## Worked Example

**Task:** "Add CSV export to orders page."

### Bad Estimate

> "1 day."

### Better Estimate Process

#### Clarify

- All-orders or filtered? → filtered (current view)
- Synchronous or background? → background, email when ready
- Format? → CSV with column header
- Max rows? → 100K
- Auth? → only own orders

#### Break Down

```
- DB query for filtered orders                             0.5d
- CSV generation (streaming for large)                     0.5d
- Background job (Spring @Async or queue)                  0.5d
- Email integration (SES)                                  0.5d
- Frontend "Export" button + status display                 1d
- Unit + integration tests                                  1d
- Docs + review iterations                                 0.5d
                                                          ----
Subtotal:                                                   4.5d
Buffer (25%):                                               1.0d
                                                          ----
Total:                                                      ~5.5d
```

→ "5-7 days, 6 most likely" — far more credible.

### Communicate

> "Range 5-7 days. Risks: large CSV memory, email deliverability. I'll check after day 2."

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| **Story points** | Relative complexity, Fibonacci, not hours |
| **T-shirt sizes** | Roadmap-level XS/S/M/L/XL |
| **Planning poker** | Simultaneous reveal, discuss outliers |
| **Time-based** | Use ranges, PERT for variance |
| **Decomposition** | Each task ≤ 1-2 days, vertical slices |
| **Buffer** | 20-30% standard, more for unknowns |
| **Spike** | Time-box (2-4h) before estimating big unknowns |
| **Velocity** | Average / sprint; team-specific |
| **Wrong estimate** | Tell early, give options |
| **DoD** | Code + tests + review + deploy + docs |

| ❌ Anti-Pattern | ✅ Better |
|----------------|----------|
| Single-point estimates | Range with most-likely |
| Hidden padding | Transparent buffer |
| Estimating without understanding | Clarify + decompose first |
| Hours → "1 point = 4 hours" | Stay relative |
| Stay silent on slip | Early heads-up |
| Forget non-code work | Include test/review/deploy |

---

## Practice

1. Estimate next 3 stories: give range + most-likely + buffer.
2. Break down a "Large" story into pieces ≤ 2 days each.
3. Track actual time vs estimate for 2 sprints; calibrate.
4. Run planning poker for one sprint with team.
5. Write your team's Definition of Done.
6. When you slip next: send "early heads up" with options.
