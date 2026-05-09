# ECS, EKS & Container Orchestration

## Status: Complete

---

## Table of Contents

1. [Why Containers on AWS](#why-containers-on-aws)
2. [ECS — Elastic Container Service](#ecs--elastic-container-service)
3. [Launch Types: Fargate vs EC2](#launch-types-fargate-vs-ec2)
4. [Task Definitions](#task-definitions)
5. [Services](#services)
6. [Service Auto Scaling](#service-auto-scaling)
7. [EKS — Elastic Kubernetes Service](#eks--elastic-kubernetes-service)
8. [Fargate for EKS](#fargate-for-eks)
9. [ECR — Elastic Container Registry](#ecr--elastic-container-registry)
10. [Service Connect & App Mesh](#service-connect--app-mesh)
11. [ECS vs EKS](#ecs-vs-eks)
12. [Pitfalls](#pitfalls)
13. [Cheat Sheet](#cheat-sheet)

---

## Why Containers on AWS

> "**Package once, run anywhere.** Standard for modern microservices. AWS gives multiple orchestrators — ECS (simpler, AWS-native) vs EKS (Kubernetes)."

Choose container if:

- Multiple language runtimes per team
- Need consistent dev → prod environments
- Microservice architecture
- Faster than EC2-direct for blue/green and rollouts

---

## ECS — Elastic Container Service

> "**AWS-native container orchestrator.** Simpler than Kubernetes, deeply integrated with IAM/ALB/CloudWatch/Secrets Manager."

### Components

```
Cluster
  ├── Task Definition (recipe)
  └── Service (running tasks managed by ECS)
        ├── Tasks (containers running)
        └── Load Balancer (ALB target group)
```

| Concept | Meaning |
|---------|---------|
| **Cluster** | Logical grouping of compute (EC2 or Fargate) |
| **Task Definition** | Blueprint: image, CPU/mem, env vars, secrets, logging |
| **Task** | Running instance(s) of a task definition |
| **Service** | Long-running, maintains desired count of tasks |

---

## Launch Types: Fargate vs EC2

| | **Fargate** | **EC2** |
|--|-------------|---------|
| Manage servers | **No** (serverless containers) | Yes (you patch, scale EC2 fleet) |
| Pricing | per vCPU + per GB-mem per second | EC2 hourly (full instance) |
| Per-task isolation | Strong (own VM per task) | Shared kernel between tasks on same EC2 |
| Cold start | Fast (seconds) | None (warm fleet) |
| Use | Most cases — start here | Custom kernels, GPUs, daemonsets, large fleets where EC2 cheaper |

### Fargate cost gotcha

- Fargate **~20–30% more expensive** per CPU/RAM than equivalent EC2 reserved
- For **steady high load**, EC2 cheaper at scale
- For **spiky / few services**, Fargate ops savings outweigh cost

### Fargate Spot

- ~70% discount, can be interrupted
- Great for batch / stateless background workers

---

## Task Definitions

> "**JSON spec** of what runs inside a task. Versioned (revisions)."

```json
{
  "family": "api-server",
  "networkMode": "awsvpc",
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::123:role/ecsTaskExecutionRole",
  "taskRoleArn": "arn:aws:iam::123:role/myAppTaskRole",
  "containerDefinitions": [
    {
      "name": "api",
      "image": "123.dkr.ecr.ap-south-1.amazonaws.com/api:v42",
      "portMappings": [{ "containerPort": 8080 }],
      "environment": [
        { "name": "ENV", "value": "prod" }
      ],
      "secrets": [
        { "name": "DB_PASSWORD", "valueFrom": "arn:aws:secretsmanager:...:password::" }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/api-server",
          "awslogs-region": "ap-south-1",
          "awslogs-stream-prefix": "api"
        }
      }
    }
  ]
}
```

### Two roles, important to differentiate

- **Execution role** (`executionRoleArn`) — used by ECS agent to **pull image, fetch secrets, write logs**
- **Task role** (`taskRoleArn`) — used by **your app code inside container** to call AWS APIs

### Network modes

- **`awsvpc`** (recommended, only one for Fargate) — task gets its own ENI + security group + private IP
- **bridge / host** — only EC2 launch type, legacy

### Secrets injection

- From **Secrets Manager** (rotation built in) or **SSM Parameter Store** (cheaper for static)
- Injected as env vars, **not in image** ✅

---

## Services

> "**Service** maintains N tasks of a task definition; auto-replaces unhealthy; integrates with load balancer; rolling deploys."

### Deployment strategies

| Type | Behavior |
|------|----------|
| **Rolling update** | Default — replace tasks gradually (`minimumHealthyPercent`, `maximumPercent`) |
| **Blue/Green** (CodeDeploy) | New task set, shift traffic at ALB, rollback fast |
| **External** | Custom orchestration |

### Rolling update knobs

```
desiredCount = 4
minimumHealthyPercent = 50  → never drop below 2 healthy during deploy
maximumPercent      = 200   → can run up to 8 during deploy
```

### Health checks

- **Container health check** (`HEALTHCHECK` in Dockerfile or task def)
- **Load balancer health check** (HTTP path, ALB target group)
- ECS replaces task if either fails consistently

---

## Service Auto Scaling

> "**Application Auto Scaling** scales `desiredCount` based on metrics."

Common policies:

- **Target tracking**: keep CPU 50% / `ALBRequestCountPerTarget` = 100
- **Step scaling**: thresholds → +N tasks
- **Scheduled**: predictable load (e.g., scale up at 9am)

### Plus: Cluster Auto Scaling (EC2 launch type)

- ECS Capacity Providers + ASG
- Scale EC2 fleet up when tasks can't be placed (resource starved)

---

## EKS — Elastic Kubernetes Service

> "**Managed Kubernetes** — AWS runs the control plane (etcd, API server, scheduler), you bring worker nodes."

### Architecture

```
EKS Control Plane (managed)
  ├── API server, etcd, scheduler — multi-AZ HA
  └── ~$0.10/hr cluster fee

Worker nodes (you manage)
  ├── EC2 nodes (Managed Node Groups or self-managed)
  ├── Fargate profiles (serverless pods)
  └── Karpenter (modern node autoscaler — recommended)
```

### Cluster components

- `kubectl` to manage
- IAM mapping via `aws-auth` ConfigMap or **Access Entries** (newer)
- **IRSA** (IAM Roles for Service Accounts) — pod-level AWS permissions via OIDC
- **AWS Load Balancer Controller** — provisions ALB/NLB from K8s Ingress/Service
- **EBS CSI / EFS CSI** — persistent volumes
- **Cluster Autoscaler** or **Karpenter** for node scaling

### When EKS

- Already use Kubernetes elsewhere
- Need K8s ecosystem (Helm, Operators, Service Mesh, KEDA)
- Multi-cloud / hybrid strategy
- Team has K8s expertise

### When NOT EKS

- Small team, simple workload — ECS Fargate is enough
- Don't want to manage Kubernetes upgrades / addons / node taints

---

## Fargate for EKS

> "**Run pods on Fargate** — no node management. Defined per **Fargate Profile** (namespace + label selectors)."

### Trade-offs

- No DaemonSets (because no nodes you control)
- Some K8s features limited (privileged pods, hostPath, etc.)
- Per-pod billing — good for **low-density** workloads
- Mix: Fargate for some namespaces + EC2 nodes for others

---

## ECR — Elastic Container Registry

> "**Private Docker registry** for your images. IAM-integrated, region-scoped."

### Features

- **Image scanning** (basic via Clair, enhanced via Inspector)
- **Lifecycle policies** — auto-delete old images
- **Cross-region replication**
- **Pull through cache** for Docker Hub / public ECR / quay (cache + reduce egress)
- **ECR Public** for public images

### CLI essentials

```bash
aws ecr get-login-password --region ap-south-1 | \
  docker login --username AWS --password-stdin 123.dkr.ecr.ap-south-1.amazonaws.com

docker tag api:v42 123.dkr.ecr.ap-south-1.amazonaws.com/api:v42
docker push       123.dkr.ecr.ap-south-1.amazonaws.com/api:v42
```

### Lifecycle policy example

```json
{
  "rules": [{
    "rulePriority": 1,
    "selection": {
      "tagStatus": "untagged",
      "countType": "sinceImagePushed",
      "countUnit": "days",
      "countNumber": 14
    },
    "action": { "type": "expire" }
  }]
}
```

→ Without lifecycle, old images accumulate forever.

---

## Service Connect & App Mesh

### Service Connect (ECS)

> "**Built-in service mesh** for ECS. Automatic service discovery + load balancing + telemetry between services. Replaces older patterns of ALB-per-service for east-west traffic."

```
order-service ──Service Connect──> payment-service
   (DNS: payment-service.local) — no internal ALB needed
```

Benefits:

- Automatic mTLS-ish via VPC, retries, timeout
- CloudWatch metrics per dependency
- Cheaper than internal ALBs

### AWS App Mesh

- Envoy-based service mesh (now in maintenance mode — being deprioritized; AWS pushing Service Connect & VPC Lattice)
- For new projects: **Service Connect** (ECS) or **VPC Lattice** (cross-VPC service-to-service) preferred

---

## ECS vs EKS

| | **ECS** | **EKS** |
|--|---------|---------|
| Learning curve | Low | High (K8s) |
| Vendor lock-in | High (AWS-only) | Low (portable K8s manifests) |
| Control plane cost | Free | ~$73/mo per cluster |
| Ecosystem | Smaller, AWS-integrated | Massive K8s ecosystem |
| Scaling | Service auto-scaling, simple | HPA/VPA + Karpenter |
| Service mesh | Service Connect | Istio/Linkerd/AppMesh |
| IAM per workload | Task role | IRSA (OIDC) or Pod Identity |
| Best for | Most AWS-only shops | Multi-cloud / advanced needs |

---

## Pitfalls

1. **Wrong network mode** — `bridge` for Fargate fails; use `awsvpc`.
2. **Execution vs Task role confusion** — secrets don't load or app can't call AWS.
3. **No log driver** configured → no CloudWatch logs.
4. **No `minimumHealthyPercent`** tuning → all tasks killed at once during deploy.
5. **No image tag discipline** (`:latest` everywhere) → can't rollback, can't reproduce.
6. **No ECR lifecycle policy** → registry storage bill.
7. **EKS without IRSA** → app uses node IAM role with too-broad perms.
8. **Cluster autoscaler too slow** → use **Karpenter** for fast bin-packing.
9. **No HPA + ASG coordination** → pods schedule but no nodes; deadlock.
10. **Privileged container** in production — security risk.

---

## Cheat Sheet

| Need | Use |
|------|-----|
| Simple containers, AWS-only | **ECS Fargate** |
| Many K8s manifests, multi-cloud | **EKS** |
| No infra mgmt | Fargate (ECS or EKS) |
| Custom kernels / GPUs | EC2 launch type |
| Pod-level IAM | **IRSA** (EKS) / Task role (ECS) |
| Secrets in container | Secrets Manager / SSM in task def |
| East-west service mesh | Service Connect (ECS) / Istio (EKS) |
| Image registry | **ECR** with lifecycle |
| Fast K8s node scaling | **Karpenter** |

---

## Practice

1. Deploy a Node.js API to **ECS Fargate** behind ALB; rolling update with new image.
2. Inject DB password via **Secrets Manager** into task def.
3. Set **target tracking** auto-scaling at 60% CPU; load test.
4. Spin up **EKS** with **Managed Node Group**; deploy nginx + service of type LoadBalancer.
5. Configure **IRSA** so a pod can read S3 with limited bucket scope.
6. Add **ECR lifecycle** to delete untagged images > 14 days.
