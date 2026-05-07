# Incident Management

## Status: Not Started

---

## Table of Contents

1. [What's an Incident?](#whats-an-incident)
2. [Incident Lifecycle](#incident-lifecycle)
3. [Severity Levels (P0–P4)](#severity-levels-p0p4)
4. [Detection](#detection)
5. [Triage](#triage)
6. [Mitigation](#mitigation)
7. [Resolution](#resolution)
8. [Communication During Incidents](#communication-during-incidents)
9. [On-Call Rotation](#on-call-rotation)
10. [Escalation Paths](#escalation-paths)
11. [Blameless Post-Mortems](#blameless-post-mortems)
12. [Common Pitfalls](#common-pitfalls)
13. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## What's an Incident?

**Matlab:** Unplanned event causing or threatening **service degradation** for users.

### Examples

- Site fully down (HTTP 5xx for everyone)
- Login broken for 20% users
- Latency P99 spiked from 200ms → 5s
- Data corruption discovered
- Security breach
- Payment processing failing

### Incident vs Event

```
Event: alert fired (CPU > 80%)
Incident: customer impact ya potential
```

→ All incidents start as events, not all events become incidents.

---

## Incident Lifecycle

```
Detect  →  Triage  →  Mitigate  →  Resolve  →  Post-Mortem  →  Action Items
  ↑                                                                  ↓
  └────────── monitoring + alerting + runbook updates ←──────────────┘
```

### Time Metrics

- **MTTD** — Mean Time To Detect (alert fires)
- **MTTA** — Mean Time To Acknowledge (someone responds)
- **MTTR** — Mean Time To Resolve / Recover

> Goal: shrink all three.

---

## Severity Levels (P0–P4)

Different orgs use different schemes; common mapping:

### P0 / SEV-1 — Critical

- Total outage / data loss / security breach
- All-hands response, war room
- 24/7 — wake people up
- Customer-facing comms required

**Examples:** site down, payments completely failing, customer data leaked.

**Response:** immediate acknowledge (< 5 min), war room within 15 min, hourly comms.

### P1 / SEV-2 — High

- Major feature broken / large subset of users affected
- Significant business impact
- Working hours — page on-call

**Examples:** checkout broken for 10% users, search down, key API at 50% error rate.

**Response:** ack < 15 min, mitigate ASAP.

### P2 / SEV-3 — Medium

- Single feature degraded / workaround exists
- Limited user impact

**Examples:** export feature slow, non-critical batch job failing, small user segment affected.

**Response:** business hours, fix in current sprint.

### P3 / SEV-4 — Low

- Minor / cosmetic / internal-only

**Examples:** intermittent low-impact bug, internal admin slow.

**Response:** backlog ticket.

### P4 — Informational

- No customer impact yet but monitoring observed

**Examples:** disk usage trending up, will be issue in 30 days.

### Severity Matrix Template

| Severity | Customer impact | Response time | Comms |
|----------|----------------|---------------|-------|
| P0 | Total outage / data | < 5 min ack | Status page + email |
| P1 | Major degradation | < 15 min | Status page if external |
| P2 | Limited / workaround | < 1 hr (biz) | Internal Slack |
| P3 | Minor | Same day (biz) | Ticket |
| P4 | Informational | Next sprint | Ticket |

---

## Detection

### Sources of Alerts

- **Synthetic monitoring** — pings every minute
- **Real-user monitoring (RUM)** — actual traffic
- **APM** (Datadog, NewRelic) — anomaly detection
- **Error tracking** (Sentry, Rollbar) — exception spikes
- **Logs aggregation** alerts (ELK, Datadog Logs)
- **Customer reports** (support tickets, Twitter)

→ Cross-ref: `DevOps-&-CI-CD/07-Monitoring-and-Alerting.md`

### Alert on Symptoms, Not Causes

```
✅ Alert: P99 latency > 1s (user-visible)
❌ Alert: CPU > 70% (might be fine — irrelevant if no user impact)
```

→ Symptom alerts are fewer, more meaningful.

### Reduce Alert Fatigue

- Tune thresholds
- Suppress duplicates (group by service)
- Auto-resolve when condition clears
- Escalate to higher tier if not acknowledged

### Customer Reports

Often the **first** signal. Treat support reports as observability signal.

```
> 3 reports in 30 min → escalate to incident
```

---

## Triage

> "Hai problem? Kitni badi? Kya scope?"

### First 5 Minutes — Quick Assessment

```
1. Acknowledge alert
2. Open dashboard / metrics
3. Check status page for related issues
4. Determine severity:
   - How many users affected?
   - How critical the feature?
   - Trending worse / stable / improving?
5. Declare incident if needed
```

### Declare an Incident

For P0/P1 — make it formal.

#### Tools

- **PagerDuty / OpsGenie / Squadcast** — paging
- **Slack incident channels** — `#incident-2024-05-08-orders-down`
- **Statuspage / Atlassian Statuspage** — public status
- **Incident.io / FireHydrant / Rootly** — orchestration platforms

### Roles (For Bigger Incidents)

| Role | Responsibility |
|------|---------------|
| **Incident Commander (IC)** | Decisions, coordinator, doesn't debug |
| **Tech Lead** | Drives investigation + fix |
| **Comms Lead** | Internal + external updates |
| **Scribe** | Log timeline + key actions |
| **SMEs** | Domain experts pulled in as needed |

For small incidents — same person may wear hats.

### What NOT to Do

❌ Everyone debugging in silence
❌ Multiple people changing prod simultaneously
❌ Random fixes without observation
❌ Skipping IC role for big incidents

---

## Mitigation

> **Stop the bleeding first.** Fix root cause later.

### Mitigation > Resolution

| Action | Goal |
|--------|------|
| **Mitigate** | Reduce/stop user impact NOW |
| **Resolve** | Fix root cause permanently |

```
Mitigate: rollback deploy, scale up, disable feature flag, redirect traffic
Resolve: find bug, fix, test, deploy
```

→ Often **mitigation = revert + breath** → then debug calmly.

### Common Mitigations

#### 1. Rollback

If incident started after a deploy → **revert that deploy**.

```bash
kubectl rollout undo deployment/orders-service
```

→ Often fastest fix. Don't try to "fix forward" under pressure.

#### 2. Feature Flag Off

```
LaunchDarkly: NEW_CHECKOUT_FLOW = false
```

→ Disable new code path, fall back to old.

#### 3. Scale Up

Out of capacity? Add replicas / increase resources.

```bash
kubectl scale deployment/api --replicas=20
```

#### 4. Restart

Memory leak / stuck connection pool — sometimes restart works (don't make this a habit).

#### 5. Throttle / Block Traffic

Bad client storming? Rate-limit or block.

#### 6. Failover

Active-passive setup → flip to standby.

#### 7. Circuit Break

Cut dependency that's failing — degrade gracefully.

### Stabilize Then Resolve

```
T+0:    detect, triage
T+5:    mitigate (rollback)
T+10:   user impact stops
T+10:   downgrade severity, calmer debugging
T+1hr:  root cause identified
T+2hr:  fix made + tested
T+3hr:  re-deploy carefully
T+4hr:  confirm resolved
```

---

## Resolution

After mitigation, find + fix **root cause**.

### Resolved Means

- ✅ Fix deployed
- ✅ Confirmed via metrics + sample traffic
- ✅ All affected systems back to normal
- ✅ Incident channel closed
- ✅ Status page updated to "resolved"
- ✅ Post-mortem scheduled

### Don't Close Too Early

```
Symptoms gone → wait 30+ min → confirm stable → close.
```

Some incidents recur as load comes back.

---

## Communication During Incidents

### Inside the Org

#### Slack War Room

- Pinned channel topic: "P1 — Orders API down — IC: @sachin"
- Threaded debugging discussions
- Top-level messages = **status updates only**

#### Status Update Cadence

- P0: every 15-30 min minimum
- P1: every 30-60 min
- Even "no progress" is an update — silence breeds panic.

```
[14:30] Status: investigating
[14:45] Update: Identified DB connection pool exhaustion. 
                Mitigation: increasing pool size + restarting one replica.
                ETA fix: 15 min.
[15:00] Update: Pool size increased. Errors dropping.
[15:15] Update: Confirmed normal. Monitoring 30 min before close.
[15:50] Resolved.
```

### External Communication (Customers)

#### Status Page

- Acknowledge the issue **publicly** within 5-15 min
- Post updates as situation evolves
- Don't speculate on cause (keep generic until known)

#### Templates

```
[Investigating]
We're investigating reports of degraded performance on the Orders API.
Updates to follow.

[Identified]
We've identified the issue — increased load causing database slowness.
Implementing a fix now.

[Monitoring]
A fix has been applied. Monitoring for stability.

[Resolved]
The issue is resolved. We'll publish a post-mortem within 5 business days.
```

### Things to Avoid

- ❌ Vague: "Things are slow somewhere"
- ❌ Premature root cause: "It was a typo" (often wrong on first guess)
- ❌ Naming individuals or blaming
- ❌ Snark or sarcasm — even internally

### Stakeholder-Specific Updates

- **Customers**: status page, plain language
- **Support**: detailed enough to help customers
- **Sales/leadership**: business impact, ETA
- **Engineering**: full technical details

---

## On-Call Rotation

### Why On-Call?

Production never sleeps. Someone needs to respond to alerts 24/7 (for systems that demand it).

### Rotation Models

#### Follow-the-Sun

Different timezones cover their daytime → no nights for anyone.

```
APAC (00-08 UTC) → EU (08-16 UTC) → Americas (16-24 UTC)
```

#### Weekly Rotation

```
Week 1: Alice
Week 2: Bob
Week 3: Carol
...
```

#### Primary + Secondary

Primary takes alerts; secondary backs up if primary unavailable.

### On-Call Best Practices

#### For the Engineer

- ✅ Carry phone, charge it, working sound
- ✅ Have laptop + VPN ready
- ✅ Know runbooks for top 5 scenarios
- ✅ Pre-paged response: ack within 5-15 min
- ✅ Don't do high-focus work (refactor) — be available
- ✅ Don't drink heavily / be unable to respond
- ✅ Hand off properly at end of shift

#### For the Org

- ✅ Compensate (extra pay / time off / both)
- ✅ Limit hours (don't burn out — 1 week per N people)
- ✅ Postpone non-critical alerts to business hours
- ✅ Alert quality (no false alarms!)
- ✅ Runbooks for every alert
- ✅ Onboarding for new on-call (shadow first)

### Runbooks

For every alert: documented response.

```markdown
## Alert: HighDatabaseConnections

### Severity
P1 if duration > 5 min; P2 below.

### Symptoms
- DB connection pool > 90% utilization
- Possible 500s on app side

### Quick Diagnosis
1. Open DB dashboard: <link>
2. Check `pg_stat_activity`: any long-running queries?
3. Check app logs: errors? connection wait times?

### Mitigations
1. Identify slow query → kill it: `SELECT pg_terminate_backend(pid);`
2. If sustained traffic spike: scale app replicas
3. If connection leak: restart suspicious instance

### Escalation
- DBA: @dba-oncall
- Service owner: @platform-team
```

→ Reduces 3am brain-dump pressure.

---

## Escalation Paths

### When to Escalate

- Alert not acknowledged in N minutes
- Beyond your knowledge (call SME)
- Severity is bigger than expected
- Need leadership decision (e.g., disable revenue feature)

### Escalation Tiers

```
Tier 1: On-call engineer
Tier 2: Backup / senior engineer
Tier 3: Tech lead / staff
Tier 4: Engineering manager
Tier 5: Director / VP
```

### Cross-Team Escalation

```
"DB symptom — paging DBA team"
"Network issue — paging Platform team"
```

→ Documented contacts in runbook.

### Don't Hesitate

> "Should I escalate?" → If unsure, **yes**. Cheaper to wake someone up than to fail a customer.

---

## Blameless Post-Mortems

**Matlab:** Incident review focused on **systems and processes**, not individuals.

### Why Blameless?

- People hide mistakes when blamed → lose learning
- Incidents are usually **multiple failures aligning** (Swiss cheese model) — no single cause
- Blame discourages risk-taking → slows innovation

### Format

```markdown
# Post-Mortem: Orders API Outage 2024-05-08

## Summary
~30 minutes of elevated 5xx errors on /api/orders, ~25% of users affected.

## Impact
- Duration: 14:30-15:05 UTC (35 min)
- Errors: 12,000 failed requests
- Revenue loss: estimated $X
- Customer complaints: 14 tickets

## Timeline (UTC)
| Time | Event |
|------|-------|
| 14:25 | Deploy v1.42.0 to production |
| 14:30 | Error rate alert fires |
| 14:31 | On-call engineer acknowledges |
| 14:35 | Incident declared as P1 |
| 14:40 | Rollback initiated |
| 14:50 | Errors return to normal |
| 15:05 | Confirmed stable, incident closed |

## Root Cause
v1.42.0 introduced a bug in connection pool config — `maxPoolSize` decreased to 5 (typo from 50). Under normal traffic, pool exhausted within 5 min.

## What Went Well
- Detection in 5 min (good monitoring)
- Rollback was clean (no DB schema changes in release)
- Communication clear in #incident channel

## What Went Poorly
- Config typo wasn't caught in PR review
- No staging soak time / load test in CI
- Connection pool metrics weren't on default dashboard

## Action Items
| Action | Owner | Due | Status |
|--------|-------|-----|--------|
| Add config validation in CI (range checks for pool size) | Alice | 2024-05-15 | Open |
| Add load test stage to release pipeline | Bob | 2024-05-30 | Open |
| Add connection pool to dashboard | Carol | 2024-05-12 | Open |
| Update runbook for connection issues | Dan | 2024-05-15 | Open |

## Lessons Learned
- Configuration changes need same rigor as code
- Runtime metrics for resource pools should be part of standard dashboard
```

### Action Items

- **Specific** + **owner** + **deadline**
- Track to completion (don't let postmortems gather dust)
- Categorize: detection / response / prevention

### Five Whys (Beware)

```
Why did the site go down?       → connection pool exhausted
Why was pool small?              → typo in config
Why wasn't typo caught?          → no validation
Why no validation?               → assumed code review enough
Why review missed it?            → reviewer focused on logic, not numbers
```

→ Useful but **don't stop at single chain** — usually multi-cause.

### Don't

❌ "Engineer X made the typo" (blame)
✅ "Process didn't catch typo" (system)

❌ "Should have known better" (judgment)
✅ "Lacked tooling to verify config range" (concrete fix)

### Share Widely

Post-mortems = team / org learning. Publish broadly (with sensitive details redacted).

---

## Common Pitfalls

### 1. Hero Culture

> "I'll just stay up all night fixing it."

→ Burnout, single-point-of-failure. **Hand off**, document, sleep.

### 2. Ignoring Action Items

Post-mortems with great action items… never done → next incident, same cause.

→ Put action items in regular sprint. Track to closure.

### 3. Skipping Post-Mortem for "Small" Incidents

Even P2/P3 worth quick write-up if learning available.

### 4. Blame Culture

People hide near-misses → no learning. Engineering health declines.

### 5. Premature Resolution

Closing while still flaky. Wait stability + observe.

### 6. Multiple Cooks in Production

3 people simultaneously running commands → confusion. **One driver** under direction of IC.

### 7. Runbooks Outdated

Service evolves, runbook doesn't → "step 3 doesn't exist anymore". **Test runbooks** in chaos drills.

### 8. No Drills

First incident with new on-call = panic. Run **fire drills** / chaos engineering / incident simulation.

### 9. Saying "Resolved" Externally Too Soon

Status page green but issue recurring → trust damage. Wait + confirm + then publish.

### 10. Skipping Communication

Engineers debugging silently → leadership/customers anxious. **Frequent** updates even if "no progress".

---

## Summary Cheat Sheet

| Phase | Key Action |
|-------|-----------|
| **Detect** | Symptom-based alerts, low fatigue |
| **Triage** | Assess severity, declare formally |
| **Mitigate** | Stop bleeding (rollback, flag, scale) |
| **Resolve** | Root cause + fix + verify |
| **Comms** | Frequent updates internal + external |
| **Post-mortem** | Blameless, action items tracked |

| Severity | Response |
|----------|---------|
| P0 | < 5 min ack, war room, public comms |
| P1 | < 15 min ack, mitigate ASAP |
| P2 | < 1 hr biz, sprint fix |
| P3 | Backlog |
| P4 | Informational |

| Roles | Job |
|-------|-----|
| **IC** | Decisions, coordination |
| **Tech Lead** | Investigates / fixes |
| **Comms** | Internal + external updates |
| **Scribe** | Timeline log |
| **SMEs** | Domain expertise on demand |

| ❌ Anti-pattern | ✅ Better |
|----------------|----------|
| Fix forward under pressure | Rollback first |
| Silent debugging | Status updates every 15-30 min |
| Blame engineer | Blame system / process |
| Skip post-mortem (small) | Document learning regardless |
| Action items ignored | Tracked + closed |
| Hero solo | Hand off, sleep, document |
| One person changing prod | One driver under IC |

---

## Practice

1. Write a runbook for your service's most common alert.
2. Define severity levels for your team (P0-P4).
3. Identify on-call rotation: backup, escalation paths, contacts.
4. Author a fictional post-mortem for a recent issue (even minor).
5. Set up a status page (Statuspage / Better Uptime / Cstatus).
6. Run a tabletop exercise: pretend a P1 happens — walk through response.
7. Audit alert fatigue: how many alerts/week? false positives? tune.
8. Track action items from past 3 post-mortems — which still open?
