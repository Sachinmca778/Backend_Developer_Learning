# CI/CD on AWS

## Status: Complete

---

## Table of Contents

1. [The AWS CI/CD Stack](#the-aws-cicd-stack)
2. [CodeCommit (Deprecated)](#codecommit-deprecated)
3. [CodeBuild](#codebuild)
4. [CodeDeploy](#codedeploy)
5. [CodePipeline](#codepipeline)
6. [GitHub Actions + AWS via OIDC](#github-actions--aws-via-oidc)
7. [ECR & Image Scanning](#ecr--image-scanning)
8. [Infrastructure as Code](#infrastructure-as-code)
9. [Pipeline Patterns](#pipeline-patterns)
10. [Pitfalls](#pitfalls)
11. [Cheat Sheet](#cheat-sheet)

---

## The AWS CI/CD Stack

```
[Source]      →  [Build]      →  [Test]    →  [Deploy]
  CodeCommit/    CodeBuild       CodeBuild    CodeDeploy
  GitHub/                                     CloudFormation
  GitLab                                      ECS/Lambda

  ───────── orchestrated by CodePipeline ────────────
                  (or GitHub Actions)
```

| Service | Role | Status (2026) |
|---------|------|---------------|
| **CodeCommit** | Git hosting | **Deprecated for new accounts** — use GitHub/GitLab |
| **CodeBuild** | Build runner (`buildspec.yml`) | Active |
| **CodeDeploy** | Deployment automation (EC2/ECS/Lambda) | Active |
| **CodePipeline** | Orchestration | Active |
| **CodeArtifact** | Private package registry (npm/PyPI/Maven/NuGet) | Active |
| **CodeStar Connections** | Connect to GitHub/GitLab/Bitbucket | Active |

---

## CodeCommit (Deprecated)

> "**Managed Git** — same protocol as GitHub. **Closed to new customers** (mid-2024+) — AWS recommends GitHub/GitLab going forward."

If you're starting fresh: **don't use CodeCommit**. Use GitHub or GitLab + integrate via OIDC or CodeStar Connections.

---

## CodeBuild

> "**Fully managed build service.** Reads `buildspec.yml`, runs in isolated container, supports Docker builds, multiple parallel jobs."

### `buildspec.yml` example

```yaml
version: 0.2

phases:
  install:
    runtime-versions:
      nodejs: 20
    commands:
      - npm ci
  pre_build:
    commands:
      - aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $REGISTRY
  build:
    commands:
      - npm run lint
      - npm test
      - docker build -t $REGISTRY/api:$IMAGE_TAG .
  post_build:
    commands:
      - docker push $REGISTRY/api:$IMAGE_TAG
      - printf '{"ImageURI":"%s"}' $REGISTRY/api:$IMAGE_TAG > imageDetail.json

artifacts:
  files:
    - imageDetail.json
    - taskdef.json
    - appspec.yml

cache:
  paths:
    - 'node_modules/**/*'
```

### Pricing

- **Per build minute** by compute type (general1.small / medium / large / GPU / ARM)
- ARM (Graviton) builds cheaper for compatible workloads
- **Local cache** (S3) speeds repeat builds

### Tips

- Use **VPC mode** if you need access to private resources (RDS, internal API)
- Use **Reserved Capacity** for high-volume orgs (cheaper)
- **Parallelize** with `batch builds` for monorepos

---

## CodeDeploy

> "**Deployment automation** for EC2 / ECS / Lambda. Handles rollouts, health checks, **automatic rollback** on alarm."

### Compute platforms

| Platform | What it deploys |
|----------|-----------------|
| **EC2/On-prem** | Files + scripts via CodeDeploy agent on instances |
| **Lambda** | New version + traffic shifting (canary, linear, all-at-once) |
| **ECS** | Blue/green via ALB target group swap |

### Deployment configs

| Strategy | Behavior |
|----------|----------|
| **In-place** (EC2) | Deploy to same instances (downtime per instance) |
| **Blue/Green** (EC2/ECS) | New environment, swap traffic, rollback fast |
| **Canary** (Lambda) | Shift 10% then rest after wait |
| **Linear** (Lambda) | Shift 10% every N minutes |
| **All-at-once** | Cut over immediately (risky) |

### `appspec.yml` (ECS example)

```yaml
version: 0.0
Resources:
  - TargetService:
      Type: AWS::ECS::Service
      Properties:
        TaskDefinition: <TASK_DEFINITION>
        LoadBalancerInfo:
          ContainerName: api
          ContainerPort: 8080
Hooks:
  - BeforeAllowTraffic: arn:aws:lambda:...:function:smoke-test
  - AfterAllowTraffic:  arn:aws:lambda:...:function:notify-slack
```

### Auto rollback

- On **CloudWatch Alarm** breach (5xx, latency)
- On deployment failure
- Manual trigger

---

## CodePipeline

> "**Orchestrates the full pipeline** — Source → Build → Test → Deploy. Each stage = one or more actions in parallel."

### Stage example

```
Source: GitHub (via CodeStar Connection)
   ↓
Build: CodeBuild
   ↓
Approval: Manual approval (SNS)
   ↓
Deploy-Staging: CodeDeploy (ECS staging cluster)
   ↓
Test-Smoke: CodeBuild (run integration tests)
   ↓
Deploy-Prod: CodeDeploy (ECS prod cluster, blue/green)
```

### Modern alternative: V2 with triggers + variables

- **Pipeline V2** supports trigger filters (branch, tag, file path), variables, queueing modes
- Cheaper than V1 for many small pipelines

### Pricing

- **V1**: $1 per active pipeline per month (free first 30 days)
- **V2**: per execution

---

## GitHub Actions + AWS via OIDC

> "**Modern best practice**: GitHub Actions for CI, AWS for runtime. Authenticate via **OIDC**, never store long-lived AWS keys in GitHub."

### Why OIDC

- **No `AWS_ACCESS_KEY_ID` secret** in GitHub
- Each workflow run gets short-lived (~1 hr) credentials assumed via IAM role
- Scoped to **specific repo / branch / environment**

### Setup

1. In AWS: create **OIDC identity provider** for `token.actions.githubusercontent.com`
2. Create **IAM role** with trust policy referencing the GitHub repo
3. In workflow:

```yaml
permissions:
  id-token: write
  contents: read

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/github-actions-deploy
          aws-region: ap-south-1

      - run: aws s3 cp ./dist s3://my-app-prod/ --recursive
```

### Trust policy snippet

```json
{
  "Effect": "Allow",
  "Principal": { "Federated": "arn:aws:iam::123:oidc-provider/token.actions.githubusercontent.com" },
  "Action": "sts:AssumeRoleWithWebIdentity",
  "Condition": {
    "StringEquals": {
      "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
    },
    "StringLike": {
      "token.actions.githubusercontent.com:sub": "repo:my-org/my-repo:ref:refs/heads/main"
    }
  }
}
```

→ Restrict by branch / environment / tag — least privilege.

---

## ECR & Image Scanning

### ECR features (recap)

- **Image scanning** — basic (Clair, free) or **enhanced** (Inspector, paid, deeper CVE + language deps)
- **Push to ECR** triggers scan
- View results: critical / high / medium / low CVEs

### Pipeline gate

```
Build image → Push to ECR → Scan
   → If CRITICAL/HIGH count > threshold → Fail pipeline
   → Else proceed to deploy
```

### Third-party scanners

- **Snyk**, **Trivy**, **Grype** — integrate in CodeBuild / GH Actions
- More flexible policy enforcement than built-in

---

## Infrastructure as Code

| Tool | Description |
|------|-------------|
| **CloudFormation** | AWS-native YAML/JSON templates. Free service, declarative, supports stack drift detection. |
| **AWS CDK** | Define infra in TS/Python/Java/Go/.NET → synthesizes CloudFormation. **Most loved AWS IaC** for app teams. |
| **Terraform** | HashiCorp, multi-cloud, huge ecosystem. AWS provider mature. State management critical (S3 + DynamoDB lock). |
| **Pulumi** | Like CDK but multi-cloud, in real programming languages |
| **AWS SAM** | Serverless-focused (CloudFormation extension) — Lambda + API GW + DDB |

### Choosing

- **AWS-only + app team writes IaC** → CDK
- **Multi-cloud / huge org with platform team** → Terraform
- **Pure serverless** → SAM (or CDK)
- **Just one stack** → CloudFormation directly

### Modern best practices

- IaC **in same repo** as app code (or sibling repo)
- **PR review** for infra changes
- **Plan/diff** in CI (`cdk diff`, `terraform plan`)
- **Apply on merge** to main, not on PR
- **State backups** (Terraform), **stack drift detection** (CloudFormation)

---

## Pipeline Patterns

### Trunk-based deploy on every merge

```
PR → Tests → Merge to main → Auto-deploy to staging → Smoke tests → Manual approval → Prod
```

### Tag-based releases

```
git tag v1.2.3 → push → workflow triggers prod deploy
```

### Mono-repo with selective builds

- Detect changed paths → only build affected services
- GitHub Actions: `paths` filter; CodePipeline: source filter

### Multi-account deploy

- Deploy from **central CI/CD account** to **prod/staging accounts** via cross-account IAM role
- Use **AWS Organizations + IAM Identity Center**

---

## Pitfalls

1. **Long-lived AWS keys in GitHub Secrets** — leak risk. Use **OIDC**.
2. **No automated rollback** — bad deploy = manual emergency.
3. **No staging** — prod is your test environment.
4. **`:latest` tag in images** — non-reproducible deploys, no rollback target.
5. **Pipeline secrets in plaintext env vars** — use Secrets Manager / SSM Parameter Store.
6. **No image vulnerability scan** — known CVEs ship to prod.
7. **CodeBuild not in VPC** — can't reach private RDS for migration tests.
8. **No deploy permissions audit** — over-broad IAM role on CI.
9. **No CodeDeploy alarms** for auto-rollback — bad deploy goes 100%.
10. **IaC drift unmanaged** — manual console changes break next deploy.

---

## Cheat Sheet

| Need | Use |
|------|-----|
| Source | GitHub / GitLab (CodeCommit deprecated) |
| Build | CodeBuild or GitHub Actions |
| Image registry | **ECR** with scanning + lifecycle |
| Deploy ECS | **CodeDeploy blue/green** |
| Deploy Lambda | CodeDeploy canary / linear |
| Orchestrate | CodePipeline V2 or GitHub Actions |
| Auth from GH to AWS | **OIDC** (no static keys) |
| IaC AWS-only | **CDK** |
| IaC multi-cloud | **Terraform** |
| Approval gate | Manual approval action / GitHub environment protection |
| Auto-rollback | CodeDeploy + CloudWatch Alarm |

---

## Practice

1. Migrate a workflow from access-key auth to **OIDC** (GitHub → AWS).
2. Build a **CodePipeline**: GitHub → CodeBuild → CodeDeploy (ECS blue/green) with auto-rollback alarm.
3. Add **ECR image scan** gate — fail build on CRITICAL CVE.
4. Write **CDK stack** for a simple Lambda + API GW + DynamoDB; deploy via CDK CLI.
5. Configure **Lambda canary deploy** (10% for 10 min, then 100%).
6. Implement multi-account deploy: tooling account assumes role in prod/staging accounts.
