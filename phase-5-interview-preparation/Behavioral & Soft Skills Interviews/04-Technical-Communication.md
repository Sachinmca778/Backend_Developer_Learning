# Technical Communication

## Status: Not Started

---

## Table of Contents

1. [Why Communication Matters](#why-communication-matters)
2. [Whiteboard Coding Etiquette](#whiteboard-coding-etiquette)
3. [Explaining Technical Decisions](#explaining-technical-decisions)
4. [Code Review Etiquette — Giving Feedback](#code-review-etiquette--giving-feedback)
5. [Code Review Etiquette — Receiving Feedback](#code-review-etiquette--receiving-feedback)
6. [Architecture Decision Records (ADRs)](#architecture-decision-records-adrs)
7. [Technical Writing Rules](#technical-writing-rules)
8. [Stand-up Update Template](#stand-up-update-template)
9. [Incident Communication](#incident-communication)
10. [Disagree-and-Commit Pattern](#disagree-and-commit-pattern)
11. [Common Output Traps](#common-output-traps)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Why Communication Matters

> "**Same tech skill, better communicator gets the senior title + 30% more comp.**"

In interviews:
- DSA round: explaining your approach > just solving silently
- Design round: trade-off articulation > technical perfection
- Behavioral: clarity + brevity = senior signal

In job:
- 70% of senior eng time = meetings, docs, reviews, PRs
- "Brilliant jerk" ko fire kiya jata hai eventually
- Communication = career multiplier

---

## Whiteboard Coding Etiquette

### Default flow (5 steps)

```
1. Clarify          (1-2 min)
2. Examples         (1-2 min)
3. Approach         (3-5 min)
4. Code             (15-20 min)
5. Test + Discuss   (5-10 min)
```

### Step 1 — Clarify (most underrated)

```
"Before I start, let me make sure I understand:
- Input format: array of integers? sorted? duplicates allowed?
- Output: indices or values?
- Range: how large can N be? (10⁵? 10⁹?)
- Edge cases: empty input? null? all same?
- Memory constraints: in-place required?"
```

→ Interviewer **scoring criteria**: senior engineers clarify; juniors dive in.

### Step 2 — Walk through example

```
"Let me trace through example: [2, 7, 11, 15], target = 9.
Expected: [0, 1] (indices of 2 + 7).
Edge case: target = 4 → no valid pair → return []."
```

→ Confirms understanding + builds intuition.

### Step 3 — Approach (think aloud!)

```
"My first thought: brute force — nested loop, O(n²). Works but slow.

Better: hash map — store value → index. For each element, check if (target - val)
in map. O(n) time, O(n) space. Trade-off: extra memory.

I'll go with hash map approach unless you have constraint reasons otherwise."
```

→ State trade-offs explicitly. Don't pick "best" silently.

### Step 4 — Code (think aloud while writing)

```
"I'll create a HashMap<Integer, Integer> for value → index.
... iterating through array ...
... at each step, check if complement exists ..."
```

### Step 5 — Test (DON'T skip)

```
"Let me trace through:
- arr = [2, 7, 11, 15], target = 9
- i=0: val=2, complement=7, map empty → put (2, 0)
- i=1: val=7, complement=2, map has 2 → return [0, 1] ✓

Edge cases:
- Empty array → loop doesn't execute, return []. ✓
- Single element → no pair possible, return []. ✓
- Duplicates: [3, 3], target=6 → put(3,0), then i=1: complement=3 in map → [0,1] ✓"
```

### Things to say throughout

| Phrase | Why |
|--------|-----|
| "Let me think out loud..." | Shows thought process |
| "One trade-off here is..." | Senior signal |
| "I'm going to assume X — let me know if that's wrong" | Reduces stuck time |
| "Let me trace through with this example..." | Catches bugs |
| "Time complexity is O(n) because..." | Always state |
| "I'm not 100% sure about this API — let me write the logic and confirm" | Honest |

### Things to NOT say

| Phrase | Why |
|--------|-----|
| Silence for 5+ minutes | Looks stuck |
| "I've seen this problem before — answer is..." | Skip the thinking |
| "This is easy" | Arrogant |
| "I forgot" without recovery | No problem-solving display |
| "Why are you asking this?" | Hostile |

### Stuck? Recovery scripts

```
"I'm stuck on <X>. Let me try a different angle: <approach>."

"Could I have a small hint? I want to make sure I'm exploring the right path."

"Let me re-read the problem... [pause] ... actually, what if we approach it via <Y>?"
```

→ **Asking for hint = OK**, especially after genuine attempt. Better than silence.

---

## Explaining Technical Decisions

> "Senior engineers articulate **trade-offs**. Juniors say 'because I like it'."

### Frame: "X over Y because..."

```
"We chose Redis over in-memory caching because we have 5 instances behind LB —
in-memory cache would inconsistent + we'd duplicate memory across instances.
Trade-off: extra hop adds 1ms latency vs in-memory's 0.1ms — acceptable for
our consistency need."
```

### Components of strong explanation

1. **What** — the choice
2. **Alternative** — what else we considered
3. **Why this** — primary reason
4. **Why not other** — concrete drawback
5. **Trade-off accepted** — what we gave up

### Real example — DB choice

```
"For the order service, we chose PostgreSQL over MongoDB.

Considered: MongoDB for schema flexibility.

But: 70% of our queries involve JOINs (user + items + payments).
Doing this app-side with MongoDB = N+1 queries + complex code.

Trade-off accepted: Less schema flexibility — but our schema was already stable.
We use JSONB column for the flexible parts where needed."
```

### Anti-pattern

❌ "We use Spring Boot because it's standard."

→ Standard for what? Why not Quarkus / Micronaut?

✅ "Spring Boot — team has 5+ years collective experience; ecosystem (security, data, observability) mature; team productive on day 1. Considered Quarkus for faster startup but team training cost not justified for our use case."

---

## Code Review Etiquette — Giving Feedback

### Levels of comments

| Tag | Meaning |
|-----|---------|
| `[blocker]` | Must fix before merge |
| `[major]` | Strongly recommend; can be follow-up if discussed |
| `[minor]` | Suggestion; author can decide |
| `[nit]` | Style / preference; non-blocking |
| `[question]` | Clarification request |
| `[praise]` | Good practice — call out positive too! |

```
[blocker] This is missing a null check; will NPE on empty list.

[major] We should use BigDecimal for currency math, not double. See <link>.

[nit] Could rename `temp` to `existingUser` for clarity.

[question] Why are we caching this for 1 hour? Wouldn't 5 min be safer for stale data?

[praise] Nice use of Optional here — clear intent.
```

### Phrasing — soft + direct

#### Don't

❌ "This is wrong."
❌ "Why didn't you use X?"
❌ "Bad practice."

#### Do

✅ "Consider using X here because <reason>."
✅ "This might fail when <edge case>. Could we add a check?"
✅ "I'd suggest <alternative>; let me know your thoughts."

### Focus

✅ Code, not coder.
❌ "You always forget null checks." → personal.
✅ "This method doesn't handle null input."

### Big PRs

If PR is huge (1000+ lines):

```
"This PR is large — I might miss things. Could we split into:
1. The model + DB migration
2. The service logic
3. The controller

Easier to review thoroughly."
```

→ Saves both your time + bug rate.

---

## Code Review Etiquette — Receiving Feedback

### Mindset

Code review = **learning + collaboration**, not personal attack.

### Default response

✅ Take it as **valid input**, even if you disagree.

### Action options

| Comment | Response |
|---------|----------|
| Agree | Apply fix; reply "done" |
| Disagree (small) | Fix anyway if minor — saves time |
| Disagree (major) | Explain reasoning calmly; ask their take |
| Don't understand | Ask follow-up |

### Don't

❌ "But it works."
❌ "I tested it, it's fine."
❌ Defensive walls of text.

### Do

✅ "Good point — fixed."
✅ "I had X reason for this approach — does that change your view?"
✅ "Could you elaborate? I'm not sure I follow."

### After many comments

```
"Thanks for the thorough review! Most addressed; left 2 questions where I'd love
your take. PR ready for re-look."
```

---

## Architecture Decision Records (ADRs)

> "**ADR** = lightweight document capturing a single technical decision + reasoning. Future you will thank present you."

### Why?

- Months later, "why did we do this?" — answered
- New joiners understand history
- Reversing decisions is informed (not blind)

### Standard ADR template

```markdown
# ADR-001: Use PostgreSQL for Order Service

## Status
Accepted (2024-05-08)

## Context
Order service needs persistent storage. Expected scale: 100K orders/day,
with complex queries joining user + items + payment.

## Decision
Use PostgreSQL 15.

## Considered Alternatives
- MongoDB — flexible schema, but 70% queries need JOINs.
- DynamoDB — managed, but query patterns require GSIs that complicate
  multi-key queries we'd need.
- MySQL — viable, but PostgreSQL has stronger JSON + analytical functions
  we anticipate needing.

## Consequences

### Positive
- Strong consistency, transactions, mature tooling
- Team has Postgres expertise
- Rich indexing (GIN for JSONB, partial indexes)

### Negative
- Vertical scaling cliff at high volume — re-evaluate at >10M orders/day
- Need to manage replication ourselves vs managed alternatives

## Decision Date
2024-05-08

## Decided By
Sachin, Architect-X (reviewed by team Y)
```

### Where to store

- Repo: `/docs/adr/0001-postgres-for-order-service.md`
- Confluence / Notion (if team uses)
- Markdown in main monorepo (preferred)

### When to write ADR

- Database / framework / language choice
- Architecture pattern (microservice split, sync vs async)
- Significant library adoption (Kafka vs RabbitMQ)
- API design (REST vs gRPC)

### When NOT to write

- Trivial / reversible decisions ("which logger lib") — code review fine

### Numbering

- Sequential — ADR-001, ADR-002, ...
- Never delete; superseded ADRs marked `Status: Superseded by ADR-XXX`

---

## Technical Writing Rules

### 1. Audience first

Different reader → different content:

| Reader | Focus |
|--------|-------|
| New dev onboarding | Why + how to start |
| Senior reviewer | Trade-offs |
| Future you | Context + decision |
| External user | Examples + API |

### 2. Structure

```
1. TL;DR / Summary       (3 lines)
2. Context / Problem     (paragraph)
3. Solution / Approach   (with diagrams)
4. Trade-offs            (explicit)
5. Examples / Usage      (concrete)
6. References / Links    (other docs, tickets)
```

### 3. Examples > Prose

Concrete code / config / curl always beats walls of text.

```
❌ "Configure the timeout to a reasonable value."
✅ "spring.cloud.gateway.httpclient.connect-timeout: 2s"
```

### 4. Diagrams

ASCII / mermaid / draw.io — pick one. Visual clarifies.

```
Client → API Gateway → Service A → DB
                    ↓
                 Service B → Cache
```

### 5. Active voice

❌ "The user is created by the controller."
✅ "Controller creates the user."

### 6. Bullets > paragraphs

Skim-friendly.

### 7. Update or archive

Stale docs are worse than no docs.

---

## Stand-up Update Template

> "30-60 sec. Not your life story."

```
[Yesterday]   "Finished the migration script + tested in staging."
[Today]       "Picking up review comments on PR #123 + starting the cache invalidation work."
[Blockers]    "Waiting on access to the staging DB — pinged DevOps. Will follow up if not by 11am."
```

### Don't

- "Had a meeting at 10. Then another. Then I tried the thing. It was hard."
- Mention every git commit
- Long monologue

### Do

- Outcomes (done / in-progress)
- Today's plan
- **Blockers ASAP** — that's the most important part for team

---

## Incident Communication

> "During an outage, **calm + structured updates** = senior engineer."

### Status update format

```
[Severity]    P1 / P2 / Sev2
[Status]      Investigating / Identified / Mitigating / Resolved
[Impact]      ~15% of users seeing 500 errors on checkout endpoint
[Started]     14:32 IST
[Update]      Suspect DB connection pool exhaustion. Currently increasing pool
              size + scaling app. Next update in 15 min.
[Owner]       Sachin
```

### Cadence

- During investigation: every **15 min** even if "no progress"
- Mitigated: announce mitigation
- Resolved: full announcement + post-mortem date

### Don't

- Speculate publicly ("this might be the worst outage ever")
- Blame ("X service is broken — wait for them")
- Go silent

### Do

- Update on cadence even with "still investigating"
- Acknowledge receiving messages
- Take notes — feeds the post-mortem

→ Cross-ref: `backend-skills / Soft-Skills-for-Backend-Developers / 04-Incident-Management.md`.

---

## Disagree-and-Commit Pattern

> "**Express disagreement strongly. Once decision is made, commit fully.**"

(Coined by Amazon, applies broadly.)

### Why?

Endless disagreement = blocked teams. **Decisions need to ship**.

### Pattern

```
1. Discuss + voice your concern (with data)
2. Decision made (you didn't get your way)
3. Commit fully — execute as if it was your idea
```

### Don't

- "I told you so" later
- Half-effort execution because "this won't work"
- Continue arguing publicly

### Do

- Make case once
- After decision: support
- Document concerns in ADR / comments (so future learning preserved)
- Pull lessons later honestly

### Real-world phrasing

> "I disagree with using approach X — I think Y is better because <reasons>. That said, if the team decides X, I'll commit fully and help make it work."

→ Senior signal. Hiring managers love this energy.

---

## Common Output Traps

### Q1. Coding silently in interview

→ No insight into thinking. Talk through.

### Q2. Apologetic / wishy-washy

```
"I think... maybe... it might be O(n)... but I'm not sure..."
```

→ Be confident even when uncertain: "My estimate is O(n); let me verify by re-tracing."

### Q3. Defensive in code review

→ Read review as gift, respond constructively.

### Q4. ADR after the fact only

→ Write before/during decision; capture reasoning fresh.

### Q5. "Just trust me" justifications

→ Always explain reasoning.

---

## Pitfalls

1. **Silent thinking** — interviewer can't score.
2. **Skipping clarifications** — assume wrong → solve wrong problem.
3. **Long monologue** stand-ups — wastes team time.
4. **Personal attacks** in review — "you" instead of "code".
5. **Big PRs** with no context — reviewers skip; bugs slip.
6. **No diagrams** in design docs — wall of text.
7. **Stale docs** — worse than no docs.
8. **Updating during incident only when asked** — be proactive.
9. **Complaining post-decision** — burns capital.
10. **Skipping examples** in technical writing — abstract = unread.

---

## Cheat Sheet

| Activity | Key |
|----------|-----|
| Whiteboard | Clarify → Approach → Trade-off → Code → Test (talk throughout) |
| Tech decision | "X over Y because..." with alternatives |
| Code review (give) | Tag severity + soft phrasing + focus on code |
| Code review (receive) | Take input, ask if unclear, respond constructively |
| ADR | Status / Context / Decision / Alternatives / Consequences |
| Stand-up | Yesterday / Today / Blockers — 30 sec |
| Incident | 15-min cadence + structured format |
| Disagree-commit | Voice once, commit fully |

| Trade-off framing | "We chose X over Y. Y has <benefit>, but X gave us <stronger benefit> at cost of <accepted trade-off>." |

| In doubt | Ask clarifying question |

---

## Practice

1. Mock whiteboard: solve problem live + record yourself; review for: pace, talking, trade-off articulation.
2. Pick recent decision in your code → write an ADR after the fact.
3. Review 1 PR/week from open-source project; observe etiquette.
4. Standup template: practice 30-sec version daily for 1 week.
5. Watch system design videos (CodeKarle, ByteByteGo) — note how they explain trade-offs; mimic style.
6. Disagree-and-commit example from past — frame for behavioral round.
