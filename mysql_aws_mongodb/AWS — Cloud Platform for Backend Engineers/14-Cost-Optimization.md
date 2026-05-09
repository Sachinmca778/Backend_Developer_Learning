# Cost Optimization

## Status: Complete

---

## Table of Contents

1. [Mindset: Architecture Decides the Bill](#mindset-architecture-decides-the-bill)
2. [Cost Explorer](#cost-explorer)
3. [AWS Budgets](#aws-budgets)
4. [Trusted Advisor](#trusted-advisor)
5. [Compute Optimizer](#compute-optimizer)
6. [Savings Plans vs Reserved Instances](#savings-plans-vs-reserved-instances)
7. [Spot Instances for Batch](#spot-instances-for-batch)
8. [S3 Cost Levers](#s3-cost-levers)
9. [Data Transfer Costs](#data-transfer-costs)
10. [Database Cost Levers](#database-cost-levers)
11. [Hygiene Cleanups](#hygiene-cleanups)
12. [FinOps Practices](#finops-practices)
13. [Pitfalls](#pitfalls)
14. [Cheat Sheet](#cheat-sheet)

---

## Mindset: Architecture Decides the Bill

> "**Cheaper hardware** doesn't beat **right architecture**. Most savings come from: right-sizing, right purchasing model, killing waste, and avoiding egress."

The 5-bucket framework:

1. **Compute** — EC2, Fargate, Lambda — biggest savings via Savings Plans, right-sizing, Spot.
2. **Storage** — S3, EBS — lifecycle, tiering, snapshot cleanup.
3. **Database** — RDS, Aurora, DynamoDB — Reserved/SP, right-size, archive cold data.
4. **Data transfer** — egress to internet, cross-AZ, cross-region — silent killer.
5. **Idle/forgotten** — old EBS, unattached EIPs, old snapshots, unused Load Balancers, idle dev environments.

---

## Cost Explorer

> "**Visualize spend** across services, regions, accounts, tags. Forecast next month."

### Useful views

- **Daily** by service → spot spikes
- **By tag** (`Env`, `Team`, `Project`) → chargeback
- **By usage type** → "what specifically am I paying for?"
- **Forecast** next 12 months

### Tagging strategy

> Tag **everything** with `Env`, `Team`, `Project`, `CostCenter`. Enforce via **SCP** denying resource creation without required tags.

```json
{
  "Effect": "Deny",
  "Action": "ec2:RunInstances",
  "Resource": "*",
  "Condition": {
    "Null": { "aws:RequestTag/Project": "true" }
  }
}
```

---

## AWS Budgets

> "**Threshold alerts** on actual or forecasted spend / usage. Email / SNS / Lambda when breached."

### Types

- **Cost budget** — $ threshold
- **Usage budget** — e.g., GB-hours of EBS
- **Reservation budget** — RI/SP utilization & coverage
- **Savings Plans budget** — utilization

### Best practices

- One **monthly cost budget per environment** (`prod`, `staging`, `dev`)
- Forecast alert at **80%**, actual at **100%** + **120%**
- Action-enabled budgets — auto-attach restrictive IAM policy when overspend (sandbox accounts)

---

## Trusted Advisor

> "**Recommendations** across cost, performance, security, fault tolerance, service limits."

Cost checks (Business/Enterprise support tier for full):

- Idle EC2 instances
- Underutilized EBS volumes
- Unassociated EIPs
- Idle RDS / Redshift
- Low-utilization Lambda
- Reserved Instance recommendations

→ Free tier sees **basic checks only**. Worth Business support for medium+ accounts.

---

## Compute Optimizer

> "**ML-based right-sizing recommendations** — looks at past 14+ days of metrics and suggests cheaper / better-fit instance type."

Covers:

- **EC2** instances + Auto Scaling Groups
- **EBS volumes** (gp2 → gp3 migrations save real money)
- **Lambda functions** (memory tuning suggestions)
- **ECS on Fargate**
- **RDS** (preview / newer)

Acts on real CloudWatch data — much better than guessing.

---

## Savings Plans vs Reserved Instances

| | **Savings Plans (Compute)** | **Savings Plans (EC2)** | **Reserved Instances** |
|--|------------------------------|--------------------------|--------------------------|
| Commitment | $/hr (any region, family, EC2/Fargate/Lambda) | $/hr (specific family + region) | Specific instance config |
| Discount | up to ~66% | up to ~72% | up to ~72% |
| Flexibility | **Highest** | Medium | Lowest |
| Term | 1 or 3 years | 1 or 3 years | 1 or 3 years |
| Payment | All / Partial / No upfront | Same | Same |
| Best for | Variable workloads, Lambda, Fargate | Steady EC2 in single family | Legacy, very steady fleet |

### Strategy

1. Watch baseline usage for **30+ days**
2. Cover the **steady baseline** (~70%) with **Compute Savings Plan**
3. Leave variable/unknown on **On-Demand**
4. Bursty/batch on **Spot**
5. Re-evaluate every **6 months** as architecture evolves

> Avoid 3-year all-upfront unless you're rock-solid certain — committing to wrong family/region = pain.

---

## Spot Instances for Batch

> "**Up to 90% off** — interruptible. Perfect for stateless / fault-tolerant workloads."

Great fits:

- **CI runners** (CodeBuild Reserved Capacity or Spot via GH Actions runners on EC2)
- **Batch processing** (AWS Batch + Spot)
- **ML training** (with checkpointing)
- **Data ETL** (rerun-safe)
- **Kubernetes worker nodes** (Karpenter handles drains)
- **Fargate Spot** for ECS tasks

### Reduce interruption pain

- Diversify across **multiple instance types + AZs** (Spot Fleet / EC2 Auto Scaling mixed instances policy)
- Use **capacity-optimized** allocation strategy
- Listen for **2-min termination notice** → checkpoint / drain
- Pair with **on-demand baseline** (e.g., 30% on-demand, 70% Spot)

---

## S3 Cost Levers

1. **Lifecycle to cheaper tiers** — IA / Glacier / Deep Archive
2. **Intelligent-Tiering** when access pattern unknown
3. **Delete incomplete multipart uploads** (lifecycle rule)
4. **Expire noncurrent versions** in versioned buckets
5. **Storage Lens** — org-wide visibility into bucket usage / wasted storage
6. **Compress before upload** (esp. logs)
7. **Smaller key prefixes** when listing — fewer LIST API calls

### Hidden cost: **Requests**

- Tons of `LIST` operations on big buckets = $$
- Pre-aggregate inventory via **S3 Inventory** (daily/weekly CSV instead of LIST)

---

## Data Transfer Costs

> "**Egress is where bills surprise.** Bandwidth between AWS and internet, cross-AZ, cross-region all cost differently."

### Rough hierarchy (per GB, varies by region)

| Direction | Cost |
|-----------|------|
| In to AWS (any) | **Free** |
| Same AZ | Free |
| Cross-AZ (within region) | ~$0.01/GB |
| Cross-region | ~$0.02/GB |
| Out to internet | $0.05–0.09/GB (region-dependent) |
| **CloudFront** to internet | discounted vs direct |

### Reduce

- **VPC Endpoints** for S3/DynamoDB → free, skip NAT GW
- **CloudFront** in front of S3/ALB → discounted egress + better cache
- **Same-AZ** placement for DB ↔ app where possible (vs cross-AZ chatter)
- **Compression** at HTTP layer (gzip/brotli)
- **PrivateLink** for cross-VPC service calls (skip internet)
- **VPC Peering / Transit Gateway** instead of routing via internet

### Hidden trap

- **NAT Gateway data processing** — $0.045/GB processed + $0.045/GB egress = **double charge** for traffic to internet via NAT
- VPC Endpoint for AWS services bypasses NAT entirely

---

## Database Cost Levers

### RDS / Aurora

- **Reserved Instances / Savings Plans** for steady DBs
- Right-size: don't keep `db.r5.4xlarge` if metrics show 10% util
- **Aurora Serverless v2** for spiky workloads
- **Stop dev/staging RDS** at night (max 7 days, then auto-restarts — automate with EventBridge)
- **Aurora I/O-optimized** vs **Standard** — choose based on I/O ratio (I/O-Optimized fixed cost includes I/O)

### DynamoDB

- **On-demand** vs **Provisioned** — On-demand simpler but ~7x more per request; switch to provisioned + auto-scaling once workload predictable
- **TTL** to auto-delete old items
- **PITR cost** vs **on-demand backup** — disable PITR on non-critical tables

### ElastiCache

- Right-size node type
- Use **Reserved nodes** for steady cache
- **Serverless** for variable

---

## Hygiene Cleanups

Quick wins, almost zero risk:

| Resource | Action |
|----------|--------|
| **Unattached EBS volumes** | Delete after snapshot |
| **Old EBS snapshots** | Lifecycle / DLM |
| **Unattached Elastic IPs** | $3.60/mo each — release |
| **Idle Load Balancers** | ALB ~$16+/mo, delete if no targets |
| **Empty CloudWatch Log Groups** | Delete |
| **Forgotten dev EC2 instances** | Tag-based scheduler stops nightly |
| **Old AMIs** | Lifecycle in EC2 Image Builder |
| **Unused NAT Gateways** | Each ~$32/mo + data — consolidate or move dev to NAT instance |
| **Idle Aurora replicas** | Reduce reader count |
| **Stale ECR images** | Lifecycle policy delete untagged > 14d |
| **Forgotten Sagemaker notebooks** | Auto-stop schedule |

---

## FinOps Practices

> "**FinOps** = engineering + finance + product collaborate on cloud spend."

### Practices

1. **Tag everything** + **enforce** with SCP
2. **Showback / chargeback** — each team sees their bill
3. **Monthly cost review** ritual — top 5 anomalies
4. **Cost in CI** — `infracost` shows $ delta on PR for IaC changes
5. **Architecture cost reviews** — design docs include estimated cost
6. **Sandbox accounts** with **hard budgets** (action-enabled budget)
7. **Centralized billing** (Organizations) for volume discounts

---

## Pitfalls

1. **No tagging** — can't allocate cost to teams.
2. **Over-committing Savings Plans** — paying for unused commitment.
3. **NAT Gateway in 3 AZs for low-traffic dev** — pay $100+/mo idle.
4. **Logs without retention** — CloudWatch Logs bill grows monthly.
5. **Public bucket data egress** scraped → bandwidth bill shock.
6. **DynamoDB On-Demand for steady workload** — provisioned 7x cheaper.
7. **EC2 instance kept after migration** — left running.
8. **Snapshots from 2019** still on bill.
9. **Cross-region replication** turned on for "backup" without need — egress cost.
10. **Free-tier abuse alarms missing** — surprise bill from new service trial.

---

## Cheat Sheet

| Lever | When |
|-------|------|
| Right-size | Always — Compute Optimizer |
| Savings Plan | Steady baseline |
| Spot | Stateless / batch |
| S3 lifecycle | Logs / archives |
| Intelligent-Tiering | Unknown access pattern |
| VPC Endpoints | Avoid NAT charges |
| CloudFront | Cheaper egress + caching |
| Stop dev at night | Always |
| Tags + Budgets | Day 1 |
| Compute Optimizer + Trusted Advisor | Monthly review |
| Hygiene cleanup | Quarterly sweep |

---

## Practice

1. Set up **Cost Explorer** views grouped by **tag `Project`**.
2. Create **AWS Budget**: alert at 80% forecast + 100% actual to SNS.
3. Run **Compute Optimizer** — apply top 3 EBS gp2 → gp3 migrations.
4. Convert dev environment EC2 to **Spot Fleet**; measure savings.
5. Add **S3 lifecycle**: logs → IA at 30d → Glacier at 90d → expire 2y.
6. Replace NAT Gateway egress for S3 with **gateway endpoint** — measure.
7. Audit account: list all unattached EIPs, old snapshots, idle ALBs — clean up.
