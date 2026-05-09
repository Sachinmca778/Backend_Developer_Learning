# EC2 & Compute

## Status: Complete

---

## Table of Contents

1. [What is EC2](#what-is-ec2)
2. [Instance Families](#instance-families)
3. [Generation & Naming](#generation--naming)
4. [AMIs](#amis)
5. [EBS & Instance Store](#ebs--instance-store)
6. [Purchasing Models](#purchasing-models)
7. [Auto Scaling Groups](#auto-scaling-groups)
8. [Scaling Policies](#scaling-policies)
9. [Placement Groups](#placement-groups)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## What is EC2

> "**Elastic Compute Cloud** — virtual servers (Linux/Windows) on demand. Most other AWS compute (ECS/EKS/RDS internally) sits on EC2 fleets."

Key knobs:

- **Instance type** (size + family)
- **AMI** (image)
- **Network** (VPC + subnet + SG)
- **Storage** (EBS volumes)
- **IAM role** (instance profile)
- **User data** (bootstrap script)

---

## Instance Families

| Family | Use case | Examples |
|--------|----------|----------|
| **General Purpose** | Balanced CPU/mem — web apps, microservices | `t3`, `t4g`, `m6i`, `m7g` |
| **Compute Optimized** | High CPU per $ — batch, video encoding, ML inference | `c6i`, `c7g` |
| **Memory Optimized** | DBs, in-memory cache, big JVM heaps | `r6i`, `r7g`, `x2idn` |
| **Storage Optimized** | NVMe SSDs, big sequential I/O — Cassandra, Elasticsearch | `i3`, `i4i`, `d3` |
| **Accelerated** | GPUs / FPGAs — ML training, graphics | `p4d`, `g5`, `inf2`, `trn1` |

### `t` family quirk — burstable

- **CPU credits** — bursts above baseline; runs out → throttled
- `t3` **unlimited mode** ON by default → can spike bill
- Don't use `t` for **steady high CPU** workloads

---

## Generation & Naming

```
m  6  i  .  large
│  │  │     │
│  │  │     └─ size (nano/micro/small/medium/large/xlarge/2xlarge...)
│  │  └─ processor: i=Intel, a=AMD, g=Graviton (ARM, ~20% cheaper + better perf/$)
│  └─ generation (newer = better perf/$ usually)
└─ family
```

### Always prefer

- **Latest generation** (price/perf)
- **Graviton (`g`)** if your stack supports ARM (Java, Node, Go, Python — yes)

---

## AMIs

> "**Amazon Machine Image** — snapshot to launch instances from."

Sources:

- AWS-managed (Amazon Linux 2023, Ubuntu, Windows)
- Marketplace (paid + bundled software)
- **Custom AMIs** baked via **Packer** / **EC2 Image Builder**

### Golden AMI pattern

- Pre-bake **OS + agents (CloudWatch, SSM) + dependencies** into AMI
- Faster boot, immutable infra, fewer cold-launch failures
- ASG launches from this AMI, no SSH config drift

---

## EBS & Instance Store

### EBS (Elastic Block Store) — network attached

| Type | Use |
|------|-----|
| **gp3** | Default SSD — independent IOPS/throughput tuning, cheaper than gp2 |
| **gp2** | Older default — IOPS tied to size |
| **io2 / io2 Block Express** | Mission-critical DBs — high IOPS guarantees |
| **st1** | Throughput HDD — big sequential (logs, data warehouse) |
| **sc1** | Cold HDD — infrequent |

- **Snapshots → S3** (incremental, only changed blocks)
- **Encryption** at rest — enable **default EBS encryption** account-wide
- Survives instance termination (if `DeleteOnTermination=false`)

### Instance store — local NVMe

- Physically attached
- **Lost on stop/terminate** — ephemeral
- Use for **scratch / cache / shuffle**, never source of truth

---

## Purchasing Models

| Model | Discount | Commitment | Use |
|-------|----------|------------|-----|
| **On-Demand** | 0 | None | Dev, spiky |
| **Reserved Instance (RI)** | up to ~72% | 1 / 3 yr, instance type | Steady predictable |
| **Savings Plans** | up to ~72% | 1 / 3 yr, $/hr commitment | More flexible than RI (covers EC2/Fargate/Lambda) |
| **Spot** | up to ~90% | None | Fault-tolerant batch / stateless |
| **Dedicated Host / Instance** | premium | physical isolation | Compliance / BYOL licenses |

### Spot

- Can be **interrupted** with **2-min warning**
- Use **Spot Fleet** + multiple instance types + AZs to reduce interruption probability
- Pair with **ASG mixed instances policy**
- Great for: **CI runners, batch ETL, ML training, Kubernetes worker nodes (Karpenter)**

### Savings Plans vs RI

- **Compute Savings Plan** — most flexible (any region, any family, EC2/Fargate/Lambda)
- **EC2 Instance SP** — region + family locked, deeper discount
- **Reserved Instance** — legacy, less flexible — generally prefer Savings Plans

---

## Auto Scaling Groups

> "**ASG** = group of EC2 instances managed as one — scale in/out + replace unhealthy."

```
Launch Template → ASG (min/desired/max) → Instances across AZs
                       │
                       └── attaches to Target Group (ALB/NLB)
```

### Components

| Piece | Role |
|-------|------|
| **Launch Template** | Recipe (AMI, type, SG, user-data, role) — versioned |
| **Min / Desired / Max** | Bounds |
| **AZs / Subnets** | Multi-AZ for HA |
| **Health checks** | EC2 (status check) or **ELB** (HTTP) |
| **Cooldown** | Wait after scaling event |

### Lifecycle hooks

- **Pending → InService** — pre-warm cache, fetch secrets
- **Terminating → Terminated** — drain connections, flush logs

---

## Scaling Policies

| Policy | Logic |
|--------|-------|
| **Target Tracking** | "Keep avg CPU = 50%" — ASG figures math (preferred default) |
| **Step Scaling** | Add N if metric > threshold A; add M if > threshold B |
| **Simple Scaling** | One alarm → one action (legacy) |
| **Scheduled** | Cron — e.g., scale up Mon 9am IST |
| **Predictive** | ML forecasts traffic → pre-scale |

### Tips

- **Scale-out fast, scale-in slow** — avoid thrashing
- Use **multiple metrics** if needed (CPU + RequestCountPerTarget)
- ASG **`HealthCheckGracePeriod`** matters for slow-starting apps

---

## Placement Groups

> "**Hint to AWS** about how to place instances physically."

| Type | Goal | Example |
|------|------|---------|
| **Cluster** | Same rack / very low latency, high bandwidth | HPC, distributed cache nodes |
| **Spread** | Each instance on different hardware | Small fleet of critical nodes (≤7 per AZ) |
| **Partition** | Logical partitions, no shared hardware between partitions | Big distributed systems — **Cassandra**, **Kafka**, **HDFS** |

### Cluster

- All instances **same AZ** typically
- 10–100 Gbps networking benefits
- Single rack failure = lose all → use only with replication elsewhere

### Spread

- HA paranoid setup
- Limit **7 instances per AZ** per spread group

### Partition

- Up to **7 partitions per AZ**
- Each partition = isolated rack
- Map app's replication topology to partitions

---

## Pitfalls

1. **`t3` for prod heavy CPU** — credits exhaust → throttled latency.
2. **Forget gp3** — still using gp2 = paying more for less perf.
3. **No instance profile** — devs put access keys in instance — leaks.
4. **IMDSv1** open — SSRF → temporary creds stolen.
5. **No multi-AZ in ASG** — one AZ outage = full downtime.
6. **Spot for stateful** — DB on Spot = data loss on interruption.
7. **`DeleteOnTermination=true`** on root EBS — lose data on accidental terminate.
8. **No encryption at rest** by default — fails audit.
9. **Over-large instance** — no CPU/RAM monitoring → paying 4x.
10. **Snapshots forgotten** — old EBS snapshots accumulate → S3 bill creep.

---

## Cheat Sheet

| Family | Workload |
|--------|----------|
| t / m | Web, microservices |
| c | Compute-heavy batch |
| r | DB, cache, big JVM |
| i | Local NVMe IOPS |
| g/p | GPU/ML |

| Buy | When |
|-----|------|
| On-Demand | Dev, spiky |
| Savings Plan | Steady prod |
| Spot | Batch / stateless workers |
| Dedicated | Compliance |

| Placement | Why |
|-----------|-----|
| Cluster | Low latency |
| Spread | HA few critical |
| Partition | Big distributed |

---

## Practice

1. Launch one **gp3** vs **gp2** EBS, run `fio` — compare IOPS.
2. Create **ASG** with **target tracking** at 50% CPU; load-test, watch scale-out.
3. Try **Spot** request via **Spot Fleet** with diversified types.
4. Bake **golden AMI** with Packer (nginx + cloudwatch agent baked in).
5. Switch a `t3.medium` workload to `t4g.medium` (Graviton) — measure cost + perf.
