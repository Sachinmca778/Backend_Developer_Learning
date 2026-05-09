# Distributed Consensus

## Status: Complete

---

## Table of Contents

1. [The Consensus Problem](#the-consensus-problem)
2. [Why Hard? FLP Impossibility](#why-hard-flp-impossibility)
3. [Quorum & Majority](#quorum--majority)
4. [Paxos](#paxos)
5. [Raft](#raft)
6. [Raft Leader Election](#raft-leader-election)
7. [Raft Log Replication](#raft-log-replication)
8. [Raft Safety](#raft-safety)
9. [ZAB (ZooKeeper)](#zab-zookeeper)
10. [Real Systems Using Consensus](#real-systems-using-consensus)
11. [Use Cases](#use-cases)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## The Consensus Problem

> "**Multiple distributed nodes** ko **same value pe agree** karna hai — even with network failures, slow nodes, partial info. Sounds simple, kaafi hard hai."

Examples of "agreement":

- **Who is the leader?** (1 of 5 nodes promoted)
- **What's the next entry in the log?** (DB write order)
- **Is this lock held?** (distributed mutex)
- **What's the current cluster membership?**
- **What's the current config value?**

### Properties needed

| Property | Meaning |
|----------|---------|
| **Agreement** | All correct nodes decide on same value |
| **Validity** | Decided value was proposed by someone |
| **Termination** | Eventually decision is reached (under assumptions) |
| **Fault tolerance** | Works despite node / network failures |

---

## Why Hard? FLP Impossibility

> "**Fischer-Lynch-Paterson (1985)**: in **fully async** system with **even one** crash failure, **no deterministic** consensus algorithm can guarantee termination."

So real algorithms (Paxos, Raft) **cheat slightly**:

- Use **timeouts** (partial synchrony assumption)
- Use **randomization** to break ties
- Trade off **liveness** (might be slow) for **safety** (always correct)

> "**Safety > liveness.** A consensus algo that's wrong sometimes is useless. Slow + correct = workable."

---

## Quorum & Majority

> "**Most consensus algorithms need a majority** to agree → can tolerate `(N-1)/2` failures."

### Math

```
N = 3 → quorum = 2 → tolerate 1 failure
N = 5 → quorum = 3 → tolerate 2 failures
N = 7 → quorum = 4 → tolerate 3 failures
```

### Why majority?

- **Two majorities can't both decide differently** — they'd overlap by ≥ 1 node
- That overlap node prevents conflicting decisions

### Why odd N?

- Even N (e.g., 4) → quorum = 3 → tolerates 1 (same as N=3 but more cost)
- Odd N is **cheaper** for same fault tolerance
- Standard: **3, 5, 7** node clusters

### Why not larger N?

- More nodes = more network traffic for each consensus round → slower
- 5 is sweet spot for most production systems
- Beyond 7 → diminishing returns + latency

---

## Paxos

> "**Leslie Lamport, 1989.** Theoretically optimal — minimum messages, proved correct. **Brutally hard to understand** and implement correctly."

### Roles

- **Proposer** — proposes a value
- **Acceptor** — votes yes/no
- **Learner** — learns the decided value

### Two-phase

```
Phase 1 (Prepare):
  Proposer sends prepare(n) to acceptors
  Acceptors promise to ignore lower n; return any prior accepted value
  
Phase 2 (Accept):
  Proposer sends accept(n, value) to acceptors
  Acceptors accept if no higher promise made
  Once majority accepts → value is chosen
```

### Multi-Paxos

- Pure Paxos = one decision per run; impractical for log replication
- **Multi-Paxos** = elect a stable leader, run cheap Paxos rounds for each log entry
- Used in Google Chubby, parts of Spanner

### Why hard

- Edge cases: dueling proposers, network partitions, partial accepts
- Original paper notoriously dense ("The Part-Time Parliament")
- Lamport later wrote "Paxos Made Simple" — still complex

---

## Raft

> "**Diego Ongaro, 2014.** Designed for **understandability**. Same fault tolerance as Paxos, but **clear separation** of concerns + better explanations."

### Three sub-problems Raft solves

1. **Leader election** — pick one leader
2. **Log replication** — leader replicates log entries to followers
3. **Safety** — guarantees no two leaders commit different entries at same index

### Roles

- **Leader** — handles all client requests; replicates log
- **Follower** — passive; replies to leader / candidate
- **Candidate** — campaigning to become leader

### Term

> "**Logical clock** for the cluster. Each election bumps the term. Higher term wins ties."

```
Term 1: Node A is leader
(Node A crashes)
Term 2: Election → Node B becomes leader
(Network partition)
Term 3: Election → Node C becomes leader
```

---

## Raft Leader Election

```
1. Followers wait for heartbeat from leader (election timeout: 150–300 ms randomized)
2. No heartbeat → become Candidate, increment term, vote for self
3. Send RequestVote(term, candidateId) to all
4. Receive votes:
   - If majority → become Leader → send heartbeats
   - If higher term seen from another → step down to Follower
   - If split vote → election timeout → new election
5. Randomized timeouts prevent perpetual split votes
```

### Why randomized timeout

> "Without randomness, all followers would time out simultaneously → repeated split votes → no progress. Randomness breaks ties."

### Heartbeats

- Leader sends `AppendEntries` (possibly empty) every 50–150 ms
- Followers reset election timer on receipt
- Lost heartbeat → new election

---

## Raft Log Replication

```
1. Client sends command → Leader
2. Leader appends to its own log (uncommitted)
3. Leader sends AppendEntries(prevIndex, prevTerm, entry) to followers
4. Followers append if their log matches (consistency check)
5. When majority ack → leader marks entry COMMITTED
6. Leader applies to state machine → respond to client
7. Followers learn commit index via next AppendEntries → apply too
```

### Log structure

```
Index:    1   2   3   4   5
Term:     1   1   2   2   3
Cmd:    SET CMP DEL SET CMP
                  ^committed
```

- **Same index + same term + same command** at all nodes (Log Matching Property)
- Inconsistencies resolved by leader **forcing followers to overwrite** their log

---

## Raft Safety

Five guarantees:

1. **Election Safety** — at most one leader per term
2. **Leader Append-Only** — leader never overwrites/deletes its own log
3. **Log Matching** — same index + term → identical entries on all nodes up to that index
4. **Leader Completeness** — committed entry from term T present in all leaders of terms > T
5. **State Machine Safety** — same index → same command applied on all nodes

### Pre-vote optimization

- Before incrementing term + voting for self, candidate "pre-asks" if it could win
- Prevents disruptive elections by partitioned candidate (term inflation)
- Used in modern Raft implementations (etcd, CockroachDB)

---

## ZAB (ZooKeeper)

> "**ZooKeeper Atomic Broadcast** — ZooKeeper's consensus protocol. Pre-Raft, similar in spirit, optimized for ZK's primary-backup model."

### Phases

1. **Leader election** (Fast Leader Election)
2. **Discovery** — new leader learns latest state
3. **Synchronization** — followers sync to leader's log
4. **Broadcast** — normal operation, leader broadcasts updates

### Used in

- **Apache ZooKeeper** itself
- Used by **Kafka** (until KRaft replacement), **HBase**, **Solr**, **Hadoop NameNode HA**

---

## Real Systems Using Consensus

| System | Algorithm | Use |
|--------|-----------|-----|
| **etcd** | Raft | K8s control plane backing store |
| **Consul** | Raft | Service discovery, config, locks |
| **CockroachDB** | Raft per range | Distributed SQL |
| **TiDB** | Raft | Distributed SQL |
| **MongoDB** | Raft-based replication protocol | Replica set election + replication |
| **Kafka KRaft** | Raft | Replaces ZooKeeper for metadata |
| **Vitess** | Raft (semi-sync MySQL) | Sharded MySQL coordination |
| **Spanner / TrueTime** | Multi-Paxos | Google's globally consistent DB |
| **Chubby** | Multi-Paxos | Google's lock service |
| **ZooKeeper** | ZAB | Coordination (locks, config, leader election) |
| **Nomad** | Raft | HashiCorp scheduler |
| **HashiCorp Vault** | Raft (or Consul) | Secrets HA |
| **YugabyteDB** | Raft | Distributed SQL |

---

## Use Cases

### 1. Leader election

> "**Cluster needs one coordinator** — only the leader does writes / decisions; followers stand by."

- Postgres / MySQL primary
- Kafka controller (KRaft)
- Job scheduler (Nomad, Quartz cluster)
- ETL coordinator

### 2. Distributed locks

> "**Mutual exclusion** across services / nodes — only one process holds the lock at a time."

- Run cron job once across N replicas
- Stripe-style idempotent operations
- Tools: **etcd lock**, **Consul lock**, **ZooKeeper lock recipe**, **Redis Redlock** (controversial)

```
acquire_lock("nightly-batch-job", ttl=5min)
  if I got it → run job
  else → skip (another node has it)
release_lock()
```

### 3. Configuration management

> "**Dynamic config** with strong consistency** — every node reads same value."

- Feature flags
- Service discovery (Consul, etcd)
- Load balancer membership

### 4. Cluster membership

> "**Who is in the cluster right now?** Add/remove nodes, agree on members."

- K8s scheduler (etcd backs it)
- Kafka brokers (controller via KRaft)
- Cassandra gossip (eventual, not consensus)

### 5. Distributed log / state machine replication

> "**Replicate ordered log** across nodes — basis of Raft itself."

- Replicated state machines for fault-tolerant services
- Event sourcing backbone

---

## Pitfalls

1. **Even N nodes** → cost without benefit (5 ≠ 4 effectively).
2. **Geographically distant cluster** → high latency per consensus round → slow writes.
3. **Putting consensus on hot path of high-QPS writes** → bottleneck (consensus is for **rare** decisions).
4. **No fencing token** in distributed lock → zombie holder (after expired lock) corrupts state.
5. **Redlock controversy** — Martin Kleppmann critiqued; use Raft-based locks (etcd) for safety-critical.
6. **Cluster split-brain** if quorum config wrong (e.g., 2-node "cluster" with no tie-breaker).
7. **Storing huge values in etcd / ZK** → slow consensus; meant for small metadata.
8. **No backups of consensus store** → cluster wipe = production gone.
9. **Upgrading consensus cluster carelessly** → election storms; use rolling upgrade with care.
10. **Cross-region consensus** for low-latency apps — consider regional clusters with eventual cross-region sync.
11. **Treating ZooKeeper / etcd as KV DB** for general use — they're for **coordination**, not data.
12. **No monitoring of leader changes** — frequent re-elections = symptom of network / GC issues.

---

## Cheat Sheet

| Algorithm | Notes |
|-----------|-------|
| **Paxos** | Theoretically optimal, hard to implement |
| **Multi-Paxos** | Stable leader + many decisions |
| **Raft** | Modern, understandable, default |
| **ZAB** | ZooKeeper-specific, pre-Raft |

| Quorum |  |
|--------|--|
| N=3 | tolerate 1 fail |
| N=5 | tolerate 2 fail |
| N=7 | tolerate 3 fail |

| Use case | Tool |
|----------|------|
| K8s metadata | **etcd** |
| Service discovery / config / locks | **Consul / etcd / ZooKeeper** |
| Distributed SQL | **CockroachDB / TiDB / Spanner** |
| Kafka metadata | **KRaft** (replaces ZK) |
| Generic coordination | **ZooKeeper** |

| Anti-patterns |  |
|---------------|--|
| High-QPS data writes | Don't put on consensus path |
| Cross-region cluster | Latency tax per write |
| Storing big blobs | Use S3 / DB, link from etcd |
| Even N | Use odd N |

---

## Practice

1. Sketch Raft leader election with 5 nodes; what happens if 2 partitioned off?
2. Implement distributed lock with etcd lease + fencing token in Python.
3. Compare Raft vs ZAB conceptually (both for ordered broadcast).
4. Why does Spanner use Paxos + TrueTime — what does TrueTime add?
5. Design service discovery with Consul: 3-node cluster across 3 AZs.
6. Why is Kafka removing ZooKeeper in favor of KRaft? Pros + cons.
