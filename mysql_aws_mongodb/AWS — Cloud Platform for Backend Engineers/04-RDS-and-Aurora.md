# RDS & Aurora

## Status: Complete

---

## Table of Contents

1. [RDS — Managed Relational](#rds--managed-relational)
2. [Multi-AZ Deployment](#multi-az-deployment)
3. [Read Replicas](#read-replicas)
4. [Parameter & Option Groups](#parameter--option-groups)
5. [Backups & PITR](#backups--pitr)
6. [RDS Proxy](#rds-proxy)
7. [Aurora — Cloud-Native](#aurora--cloud-native)
8. [Aurora Read Replicas](#aurora-read-replicas)
9. [Aurora Serverless v2](#aurora-serverless-v2)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## RDS — Managed Relational

> "**Amazon RDS** = managed MySQL / PostgreSQL / MariaDB / Oracle / SQL Server / **Db2** instances. AWS handles install, patch, backup, failover, monitoring."

### What you still own

- **Schema design** + indexes
- **Slow queries** + N+1 in app
- **Connection management** (pooling)
- **Choosing instance size** + storage

### What AWS owns

- OS + DB engine patching
- Failover orchestration
- Snapshot infra
- Replication plumbing

---

## Multi-AZ Deployment

> "**Standby replica in another AZ — synchronous replication. Automatic failover ~60s on primary failure / AZ outage / patching.**"

```
Primary (AZ-a)  ─── synchronous ──►  Standby (AZ-b)
       │                                 │
       └────── single endpoint ──────────┘
              (DNS flips on failover)
```

### Important

- **Standby is NOT readable** — purely for HA (RDS classic)
- **Multi-AZ Cluster** (newer, MySQL/Postgres) — **2 readable standbys** + faster failover
- Failover triggers: instance failure, AZ failure, **maintenance**, instance class change, **OS patching**

### When to use

- **Always** for production
- Adds ~2x cost — worth it for SLA

---

## Read Replicas

> "**Asynchronous** copies for **read scaling** + cross-region DR. Up to **5** for RDS, **15** for Aurora."

```
Primary  ──async──►  Read Replica 1
         ──async──►  Read Replica 2
         ──async──►  Cross-region Replica (DR)
```

### Use cases

- Heavy **reporting / analytics** queries off primary
- **Read scale-out** for read-heavy apps
- **Promote** to primary on disaster (manual)

### Caveats

- **Replication lag** — eventual consistency
- App must **route writes → primary, reads → replicas** (RDS Proxy or app-level)
- Some changes (DDL on primary) replicate slowly

### Multi-AZ vs Read Replica

| | Multi-AZ | Read Replica |
|--|---------|--------------|
| Replication | Synchronous | Asynchronous |
| Purpose | HA | Read scale + DR |
| Readable | No (classic) | Yes |
| Failover | Auto | Manual promote |
| Same AZ? | Diff AZ | Anywhere (incl. cross-region) |

---

## Parameter & Option Groups

### Parameter Group

> "**Engine config** — like `my.cnf` for MySQL, `postgresql.conf` for Postgres."

```
Examples:
  max_connections, innodb_buffer_pool_size, work_mem,
  log_min_duration_statement, slow_query_log
```

- **Default PG** = read-only
- Create **custom PG** to tune
- Some changes need **reboot**, some are dynamic

### Option Group

> "**Optional engine features**: Oracle Native Network Encryption, SQL Server SSAS, MariaDB audit plugin, etc."

- MySQL/Postgres mostly use parameter groups; option groups important for Oracle/SQL Server.

---

## Backups & PITR

### Automated backups

- Daily **snapshot** + **transaction logs every 5 min**
- Retention **0–35 days** (set 7+ for prod)
- **Point-in-Time Recovery (PITR)** — restore to **any second** within retention
- Stored in **S3** (managed by AWS)

### Manual snapshots

- Triggered by you / Lambda / script
- **No expiry** — kept until deleted
- Use for **pre-major-change** safety, **cross-account/region** sharing

### Restore

- Always creates a **new RDS instance** (different endpoint)
- App needs to update DNS / connection string

### Cross-region snapshot copy

- For DR — copy snapshot to other region

---

## RDS Proxy

> "**Connection pooling layer** managed by AWS. Sits between app and DB. Critical for **Lambda + RDS**."

```
Lambda (1000 concurrent) → RDS Proxy (pool: 100) → RDS
```

### Why

- Lambda **opens new connection per cold start** — RDS dies under spike
- Proxy **multiplexes** 1000s of client conns over a small DB pool
- **Failover-aware** — reduces failover time from ~60s to ~few seconds
- **IAM auth** support — no password in code

### Cost

- Per-instance hourly + per-vCPU charge — for serverless/Lambda workloads worth it

---

## Aurora — Cloud-Native

> "**MySQL/Postgres-compatible** but rewrote storage layer. **Up to 5x MySQL, 3x Postgres** throughput. **6-way replication across 3 AZs** at storage layer."

### Architecture

```
Writer  ── shared distributed storage ──  Readers (up to 15)
                  │
            6 copies across 3 AZs
            (lose 2 copies = OK for reads, 3 = OK for writes)
```

- **Compute** + **storage** decoupled
- Storage **auto-grows** in 10 GB chunks up to **128 TB**
- Backups continuous to **S3** — near-zero overhead

### Cost vs RDS

- ~20–30% **more expensive** per instance hour
- But: better perf, near-zero replica lag, fast failover, less ops pain → usually worth it

---

## Aurora Read Replicas

| | Aurora | RDS |
|--|--------|-----|
| Max replicas | **15** | 5 |
| Replication | **Storage-level**, sub-millisecond lag | Logical, log-based, secs lag |
| Failover | **~30s** to replica | ~60s to standby |
| Replica readable | Yes | No (classic Multi-AZ) |

### Endpoints

- **Cluster (Writer) endpoint** — always points to primary
- **Reader endpoint** — load-balances across replicas
- **Custom endpoints** — group of specific replicas (e.g., reporting nodes)

### Auto-scaling replicas

- Set rules to add/remove replicas based on CPU / connections

---

## Aurora Serverless v2

> "**Auto-scaling Aurora compute** — measured in **Aurora Capacity Units (ACUs)**. Scales in seconds, no downtime."

### v2 vs v1

| | v1 | v2 |
|--|----|----|
| Scaling | Cold (pause/resume) | **Hot, sub-second**, no disconnects |
| Min | 0 (pause) | 0.5 ACU min |
| Use case | Dev / spiky low | Production-grade variable |

### When to use

- **Spiky traffic** (event-driven)
- **Multi-tenant** SaaS (variable per-tenant load)
- **Dev/test** — auto-pause v1
- Avoid for: **steady high load** (provisioned cheaper)

---

## Pitfalls

1. **No Multi-AZ in prod** — single AZ failure = downtime.
2. **Reading from primary always** — replica is sitting idle.
3. **`max_connections` default too low** — Lambda swarm crashes DB.
4. **No RDS Proxy with Lambda** — connection storm.
5. **Backup retention = 1 day** — can't recover yesterday's bug.
6. **Manual snapshots forever** — bill creep.
7. **Public RDS endpoint** — exposed to internet (don't!).
8. **Major version upgrade** in place — break apps; **blue/green deployment** introduced for safer.
9. **Aurora reader endpoint stale** — after replica add, DNS TTL caching can mis-route.
10. **Instance type too small** — IOPS bottleneck, not just CPU/RAM.

---

## Cheat Sheet

| Need | Use |
|------|-----|
| HA | **Multi-AZ** |
| Read scale | Read Replicas |
| MySQL/Postgres compat + better perf | **Aurora** |
| Lambda + DB | **RDS Proxy** |
| Spiky workload | **Aurora Serverless v2** |
| Restore to T-1h | **PITR** within retention |
| Cross-region DR | Replica or snapshot copy |
| Engine tuning | **Parameter Group** |

| Aurora endpoint | Use |
|-----------------|-----|
| Cluster | Writes |
| Reader | Reads (load-balanced) |
| Custom | Reporting cohort |
| Instance | Specific node |

---

## Practice

1. Spin up **MySQL RDS Multi-AZ**; trigger reboot with failover; observe DNS flip.
2. Add a **Read Replica**; route reads via app config.
3. Enable **Performance Insights** + **Slow Query Log** via parameter group.
4. Configure **PITR** restore to a test instance — verify data.
5. Compare **Aurora Serverless v2** scale-up vs **provisioned** for a load test.
6. Add **RDS Proxy** in front of an RDS instance + IAM auth from Lambda.
