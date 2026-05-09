# VPC & Networking

## Status: Complete

---

## Table of Contents

1. [What is a VPC](#what-is-a-vpc)
2. [CIDR Block Planning](#cidr-block-planning)
3. [Subnets — Public vs Private](#subnets--public-vs-private)
4. [Internet Gateway](#internet-gateway)
5. [NAT Gateway vs NAT Instance](#nat-gateway-vs-nat-instance)
6. [Route Tables](#route-tables)
7. [Security Groups vs NACLs](#security-groups-vs-nacls)
8. [VPC Peering](#vpc-peering)
9. [VPC Endpoints](#vpc-endpoints)
10. [PrivateLink](#privatelink)
11. [Pitfalls](#pitfalls)
12. [Cheat Sheet](#cheat-sheet)

---

## What is a VPC

> "**Virtual Private Cloud** — your **isolated virtual network** inside AWS region. Apna IP space, subnets, routing, firewalls."

Default region has a **default VPC** — easy but not production-ideal.

### Always per-region

VPC ek region mein hi hota hai. Cross-region = **VPC Peering** / **Transit Gateway** / **VPN** / **Direct Connect**.

---

## CIDR Block Planning

> "VPC banate waqt **IP range (CIDR)** decide karo. Galat planning = re-do later painful."

```
10.0.0.0/16   → 65,536 addresses (typical big VPC)
172.16.0.0/16
192.168.0.0/16
```

### Rules

- VPC CIDR: **/16 to /28**
- Don't overlap with other VPCs / on-prem (peering won't work)
- Reserve **buffer** for future subnets

### Subnet sizes

| CIDR | Usable IPs |
|------|-----------|
| /28 | 11 (5 reserved) |
| /24 | 251 |
| /20 | 4091 |

> AWS reserves **5 IPs per subnet**: network, VPC router, DNS, future, broadcast.

---

## Subnets — Public vs Private

> "**Subnet = AZ-bound IP slice** of VPC. 'Public/Private' is about **routing**, not naming."

| Subnet | Has route to IGW? | Use |
|--------|-------------------|-----|
| **Public** | Yes (`0.0.0.0/0 → IGW`) | ALB, NAT GW, bastion |
| **Private** | No direct → uses NAT for outbound | App servers, DBs, Lambda-in-VPC |
| **Isolated** | No internet at all | Sensitive DBs, internal-only |

### Best-practice topology

```
VPC 10.0.0.0/16 (3 AZs)
 ├── Public  10.0.1.0/24  (AZ a)  → IGW
 ├── Public  10.0.2.0/24  (AZ b)  → IGW
 ├── Public  10.0.3.0/24  (AZ c)  → IGW
 ├── Private 10.0.11.0/24 (AZ a)  → NAT GW (in public AZ a)
 ├── Private 10.0.12.0/24 (AZ b)  → NAT GW (in public AZ b)
 └── Private 10.0.13.0/24 (AZ c)  → NAT GW (in public AZ c)
```

→ App in private subnets, ALB in public, DBs in isolated.

---

## Internet Gateway

> "**IGW** = horizontally scaled, redundant, one-per-VPC. Enables **bidirectional internet** for resources with **public IP**."

- Itself doesn't grant internet — needs **route table entry** + **public IP/EIP** on instance
- No bandwidth limit, no charge for IGW (only data transfer charged)

---

## NAT Gateway vs NAT Instance

> "**NAT** lets **private** subnet instances reach **internet** outbound (egress) — **internet can't initiate inbound**."

### NAT Gateway (managed)

- Highly available **within one AZ**
- Up to **45 Gbps**
- ~**$32/month per AZ + per-GB data processing**
- For multi-AZ: **one NAT GW per AZ** (else cross-AZ traffic + single AZ failure = outage)

### NAT Instance (self-managed EC2)

- Cheaper for low traffic / dev
- You patch + scale
- Single point of failure unless you script it

### Cost trap

> "Three AZs × NAT GW × 24/7 + per-GB charges = surprise bill."

Mitigate:

- **VPC Endpoints** for AWS services → bypass NAT for S3/DynamoDB/ECR/etc.
- Use **NAT Instance** for dev environments
- **Centralized NAT** in shared VPC (Transit Gateway pattern)

---

## Route Tables

> "**Route table** = mapping of destination CIDR → target (IGW, NAT, peering, endpoint, ENI)."

```
Destination       Target
----------------  ----------------
10.0.0.0/16       local         (always present)
0.0.0.0/0         igw-xxx       (public subnet)
0.0.0.0/0         nat-xxx       (private subnet)
pl-xxxxx (S3)     vpce-xxx      (gateway endpoint)
```

- Each subnet → **one** route table
- One route table → **multiple** subnets
- **`local`** route always wins for in-VPC traffic — un-removable

---

## Security Groups vs NACLs

| Property | Security Group | NACL |
|----------|----------------|------|
| Level | **Instance / ENI** | **Subnet** |
| State | **Stateful** (return traffic auto-allowed) | **Stateless** (must allow both directions) |
| Rules | **Allow only** | **Allow + Deny** |
| Default | Implicit deny | Default allow all |
| Order | All rules evaluated | **Numbered** — lowest first |

### Security Groups — daily driver

```
SG: web-tier
  Inbound:  443 from 0.0.0.0/0
  Outbound: all
  
SG: app-tier
  Inbound:  8080 from web-tier (SG-as-source!) ← powerful pattern
  Outbound: all
  
SG: db-tier
  Inbound:  3306 from app-tier
  Outbound: all
```

> SG-as-source rocks — no IP hardcoding, scales with auto-scaling fleets.

### NACLs — extra layer

- Use to **explicitly block IPs / CIDRs** (DDoS specific source)
- Most workloads = leave default open NACL, lock down via SGs

---

## VPC Peering

> "**1-to-1 private connection** between 2 VPCs (same/cross-region, same/cross-account)."

- **Non-transitive** — A-B and B-C peered, but A can't reach C
- **CIDRs cannot overlap**
- Update **route tables** on both sides

For many VPCs → use **Transit Gateway** (hub-and-spoke, transitive).

---

## VPC Endpoints

> "**Private path** from VPC to AWS service — **no NAT, no IGW, no internet exposure, no data transfer to internet charge**."

### Two flavors

| Type | Services | How |
|------|----------|-----|
| **Gateway Endpoint** | **S3, DynamoDB only** | Route table entry — **free** |
| **Interface Endpoint (PrivateLink)** | Most services (SSM, ECR, Secrets Manager, KMS, …) | ENI in subnet + private DNS — **per-hour + per-GB charge** |

### Why use them

- **Security** — traffic stays on AWS backbone
- **Cost** — skip NAT GW data charges (esp. S3 / ECR pulls!)
- **Compliance** — "no public internet path"

### Example

```
EC2 in private subnet → S3
   Without endpoint: → NAT GW → IGW → S3 (NAT cost + slower)
   With gateway endpoint: → S3 directly (free, fast)
```

---

## PrivateLink

> "**Expose your service privately** to other VPCs/accounts via Interface Endpoints."

Pattern:

```
SaaS Provider VPC                Customer VPC
  NLB ── Endpoint Service  →  Interface Endpoint (ENI)
```

- Customer talks to a **private DNS / private IP** in their VPC
- No internet exposure of provider's NLB
- Used by **AWS itself** (KMS, S3 interface endpoint), and by SaaS (Snowflake, Datadog) to expose services privately

---

## Pitfalls

1. **NAT GW per AZ vs single AZ** — wrong choice = cost or HA loss.
2. **Overlapping CIDRs** between VPCs — no peering possible later.
3. **All-open SG** (`0.0.0.0/0:22`) — SSH open to world.
4. **No VPC Endpoints for S3/ECR** — paying NAT GW for S3 traffic.
5. **Default VPC in prod** — shared with everyone, hard to control.
6. **Forgetting NACL stateless** — return traffic blocked, debugging hell.
7. **Public subnet == internet route**, not naming — instance in "public" subnet without public IP can't be reached.
8. **DB in public subnet** — ever. Just don't.
9. **Single AZ deploy** — AZ outage = total downtime.
10. **No flow logs** — can't debug "why connection refused?".

---

## Cheat Sheet

| Concept | Quick |
|---------|-------|
| VPC | Isolated virtual network in region |
| Subnet | AZ-scoped IP slice |
| Public subnet | Has route to IGW |
| Private subnet | NAT for outbound |
| IGW | Internet gateway (in/out) |
| NAT GW | Outbound internet for private (paid, per AZ) |
| Route table | Where packets go |
| Security Group | Stateful, allow-only, instance-level |
| NACL | Stateless, allow+deny, subnet-level |
| Peering | 1-to-1 VPC connection |
| Endpoint (Gateway) | S3/DDB private + free |
| Endpoint (Interface) | PrivateLink — most services |
| Transit Gateway | Hub for many VPCs |

---

## Practice

1. Design a 3-AZ VPC: `10.10.0.0/16` with public/private/isolated subnets.
2. Replace NAT GW egress for S3 with **gateway endpoint** — measure savings.
3. Use **SG-as-source** chain: web → app → db.
4. Enable **VPC Flow Logs** — query in CloudWatch Logs Insights.
5. Add an **Interface Endpoint** for Secrets Manager — prevent NAT egress.
