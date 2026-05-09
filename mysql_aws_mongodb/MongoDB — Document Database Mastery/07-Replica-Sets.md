# Replica Sets

## Status: Complete

---

## Table of Contents

1. [What is a Replica Set](#what-is-a-replica-set)
2. [Minimum Topology](#minimum-topology)
3. [Primary & Secondaries](#primary--secondaries)
4. [Elections & failover](#elections--failover)
5. [Oplog](#oplog)
6. [Read Preferences](#read-preferences)
7. [Hidden & Delayed Members](#hidden--delayed-members)
8. [Arbiters](#arbiters)
9. [Write Concern & Replication](#write-concern--replication)
10. [Operational Commands](#operational-commands)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## What is a Replica Set

> "**Same data** multiple `mongod` nodes — **automatic failover**, read scaling, backup base."

```
Primary ──replicates──► Secondary
   │                      │
   └──────────────────────┘
            Oplog
```

---

## Minimum Topology

### Production-friendly

- **3 data-bearing members** (P + S + S) — tolerance **1 node failure** with majority

### Cost-saving (not ideal)

- **2 data + 1 arbiter** — arbiter stores **no data**, only votes in election

### Why odd voters?

> "**Majority** election — tie-breaking needs odd voter count ideally."

---

## Primary & Secondaries

| Role | Behavior |
|------|----------|
| **Primary** | All writes default land here |
| **Secondary** | Applies oplog from primary — read-only unless configured |

### Secondary reads

Must set **`readPreference`** in driver — default **`primary`**.

---

## Elections & failover

> "Primary unreachable **`electionTimeoutMillis`** (~10s default) — secondaries trigger **election**."

Factors:

- **Priority** — higher preferred primary
- **Votes** — default 1 per member up to 7 votes members rules (check version)

### Split brain prevention

**Majority** required for primary — minority partition steps down.

---

## Oplog

> "**Capped collection** of operations — replication + change streams backbone."

Properties:

- **Idempotent application** order on secondaries
- Size capped — too small → secondary can't catch up if offline long (**replica lag** risk)

### Check oplog window

```javascript
rs.printReplicationInfo()
```

---

## Read Preferences

| Mode | Meaning |
|------|---------|
| **primary** | Default — consistent writes visibility |
| **primaryPreferred** | Primary if up, else secondary |
| **secondary** | Any secondary |
| **secondaryPreferred** | Secondary if available |
| **nearest** | Lowest latency member (rack awareness tags) |

### Staleness

Secondary reads may lag — **not linearizable** by default.

### maxStalenessSeconds

Driver option — avoid laggy secondaries beyond threshold.

---

## Hidden & Delayed Members

### Hidden

```javascript
cfg.members[2].hidden = true
cfg.members[2].priority = 0
```

> "**Replication yes, client reads no** — backups/analytics replica."

### Delayed

```javascript
cfg.members[2].slaveDelay = 3600   // seconds behind primary
```

> "**Human error protection** — drop wrong DB → recover from delayed member window."

Tradeoff: **not voter** sometimes configured — plan carefully.

---

## Arbiters

> "**Vote only** — no data, minimal resources."

Downsides:

- Can't become primary
- **No data redundancy** — 2+1 means only **two copies** of data

---

## Write Concern & Replication

**`w: "majority"`** ensures write replicated to majority before ack — survives single node loss with 3-node RS.

---

## Operational Commands

```javascript
rs.status()
rs.conf()
rs.initiate({ _id: "rs0", members: [...] })
rs.stepDown()    // primary voluntarily steps down — maintenance
```

### Add member

```javascript
rs.add({ host: "mongo4:27017" })
```

---

## Pitfalls

1. **Standalone prod** — no HA.
2. **Arbiter misunderstanding** — not a third data copy.
3. **Secondary reads** for money-critical **just-after-write** UX — stale surprises.
4. **Tiny oplog** — replica rebuild needed after maintenance window.
5. **Priority 0 hidden** misconfiguration — accidental primary candidates.
6. **Even voter mis-design** — election edge cases.

---

## Cheat Sheet

| Concept | Quick |
|---------|-------|
| Replica set | HA group of mongod |
| Primary | Writes |
| Secondary | Copies + optional reads |
| Oplog | Op log |
| Election | New primary on failure |
| Read pref | Where reads go |
| Hidden | No client traffic |
| Delayed | Lagged backup safety |
| Arbiter | Vote only |

---

## Practice

1. Draw timeline: primary dies → election → app reconnect.
2. When **`secondaryPreferred`** hurts correctness?
3. Configure **delayed secondary** rationale for your org.
4. Compare **3 data nodes** vs **2+1 arbiter** failure tolerance.
5. How **`readConcern majority`** interacts with secondary read (still usually primary for that guarantee)?
