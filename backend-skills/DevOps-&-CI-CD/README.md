# DevOps & CI/CD

Backend developer ke liye **production-grade delivery** — Git workflows, CI/CD concepts + GitHub Actions, Docker in pipelines, Infrastructure as Code, environment management & deployment strategies, aur monitoring/alerting/incident response. Sab Hinglish mein, real configs aur examples ke saath.

---

## Topics & Status

| # | Topic | File | Status |
|---|-------|------|--------|
| 1 | Git Best Practices | [01-Git-Best-Practices.md](./01-Git-Best-Practices.md) | Not Started |
| 2 | CI/CD Concepts | [02-CI-CD-Concepts.md](./02-CI-CD-Concepts.md) | Not Started |
| 3 | GitHub Actions | [03-GitHub-Actions.md](./03-GitHub-Actions.md) | Not Started |
| 4 | Docker in CI/CD | [04-Docker-in-CI-CD.md](./04-Docker-in-CI-CD.md) | Not Started |
| 5 | Infrastructure as Code | [05-Infrastructure-as-Code.md](./05-Infrastructure-as-Code.md) | Not Started |
| 6 | Environment Management | [06-Environment-Management.md](./06-Environment-Management.md) | Not Started |
| 7 | Monitoring & Alerting | [07-Monitoring-and-Alerting.md](./07-Monitoring-and-Alerting.md) | Not Started |

---

## What's Inside Each File?

### [01 — Git Best Practices](./01-Git-Best-Practices.md)
Branching strategies (GitFlow, GitHub Flow, Trunk-based), conventional commits (feat/fix/chore/docs/refactor/test), semantic versioning (MAJOR.MINOR.PATCH), rebase vs merge vs squash, PR hygiene, force-push safely (`--force-with-lease`), command cheat sheet (daily, branching, sync, undo, inspection, tagging).

### [02 — CI/CD Concepts](./02-CI-CD-Concepts.md)
CI vs Continuous Delivery vs Continuous Deployment, full pipeline stages (lint → build → unit/integration tests → security scan → package → deploy → smoke test), best practices (fail fast, same artifact, pipeline as code, caching, secrets, parallelization), tools landscape.

### [03 — GitHub Actions](./03-GitHub-Actions.md)
Workflow YAML structure, triggers (push, PR, schedule, manual), jobs/steps/actions, env vars + contexts, secrets + OIDC, artifacts (upload/download), caching (Maven, Gradle, npm, Docker layers), matrix builds, reusable workflows + composite actions, real-world Spring Boot + Docker example, best practices (pin SHAs, minimal permissions, concurrency).

### [04 — Docker in CI/CD](./04-Docker-in-CI-CD.md)
Multi-stage Dockerfile (Spring Boot example), tagging strategy (commit SHA, SemVer, latest), pushing to registries (DockerHub, GHCR, ECR, GCR), multi-platform builds (ARM + AMD64) with `buildx`, image scanning (Trivy, Snyk, Grype), layer caching in CI (`type=gha`, registry cache), best practices (distroless, non-root, `.dockerignore`, health checks, signing, SBOM).

### [05 — Infrastructure as Code](./05-Infrastructure-as-Code.md)
IaC fundamentals, **Terraform** (provider/resource/variable/output, state with S3+DynamoDB lock, modules), Terraform vs **Pulumi** vs **AWS CDK** vs **CloudFormation**, **Ansible** for OS-level config management, **GitOps** (ArgoCD, Flux) — Git as source of truth for K8s state, app-of-apps, Kustomize/Helm.

### [06 — Environment Management](./06-Environment-Management.md)
Environment tiers (dev/staging/prod), promotion (build once, promote artifact), 12-factor app + dev/prod parity, **feature flags** (LaunchDarkly, Flagsmith, Unleash, Togglz, Spring) — flag types and debt, deployment strategies: **blue-green** vs **canary** (Argo Rollouts) vs **rolling** (K8s default) vs **recreate**, expand-contract DB schema migrations.

### [07 — Monitoring & Alerting](./07-Monitoring-and-Alerting.md)
**SLI / SLO / SLA** + **error budget** (and burn rate), three pillars (metrics, logs, traces), RED + USE methods, **alert fatigue** + symptom-based alerts, on-call best practices, **incident response** workflow (detect → triage → mobilize → mitigate → communicate → resolve), **blameless post-mortems** (with template), **runbooks**, OpenTelemetry, tools landscape (Prometheus, Grafana, Datadog, PagerDuty).

---

## Recommended Learning Order

```
1. Git Best Practices (01)        ← team foundation
2. CI/CD Concepts (02)            ← mental model
3. GitHub Actions (03)            ← practical CI
4. Docker in CI/CD (04)           ← packaging
5. Environment Management (06)    ← deploy strategies
6. Infrastructure as Code (05)    ← reproducible infra
7. Monitoring & Alerting (07)     ← run + improve
```

---

## Quick Reference

### "Mujhe X karna hai" → kahan dekhun?

| Task | File | Section |
|------|------|---------|
| Choose branching strategy | 01 | Branching Strategies |
| Write good commit messages | 01 | Conventional Commits |
| Bump version | 01 | SemVer |
| Set up CI for Java app | 02, 03 | Pipeline Stages, Examples |
| Cache Maven deps in CI | 03 | Caching |
| Deploy with manual approval | 03 | Real-World Examples |
| Multi-platform Docker image | 04 | Multi-Platform Builds |
| Scan image for vulns | 04 | Image Scanning |
| Avoid `latest` tag in prod | 04 | Image Tagging Strategy |
| Manage AWS infra in code | 05 | Terraform Basics |
| Deploy K8s from Git | 05 | GitOps |
| Roll out feature gradually | 06 | Canary / Feature Flags |
| Zero-downtime deploy | 06 | Blue-Green / Rolling |
| Define service quality target | 07 | SLI / SLO |
| Reduce noisy alerts | 07 | Symptom-Based Alerts |
| Run an incident calmly | 07 | Incident Response |
| Write a post-mortem | 07 | Blameless Post-Mortems |

---

## Tools Reference

### Git / Hosting
- `git`, GitHub, GitLab, Bitbucket
- Conventional Commits → `commitlint`, `semantic-release`

### CI/CD
- GitHub Actions, GitLab CI, Jenkins, CircleCI, Azure DevOps, AWS CodePipeline

### Containers
- Docker / Buildx, BuildKit
- Trivy, Snyk, Grype (scanning)
- Cosign (signing), SBOM generation

### IaC
- Terraform, Pulumi, AWS CDK, CloudFormation, Ansible
- ArgoCD, Flux (GitOps)

### Deployment
- Kubernetes + Helm + Kustomize
- Argo Rollouts, Flagger (canary)
- LaunchDarkly, Flagsmith, Unleash (feature flags)

### Observability
- Prometheus + Grafana
- Loki / ELK / Splunk (logs)
- Jaeger / Tempo / Datadog APM (traces)
- OpenTelemetry (instrumentation)
- PagerDuty / Opsgenie (paging)

---

## Companion Folders

- [API Design & Architecture](../API-Design-&-Architecture/)
- [Database Mastery](../Database-Mastery/)
- [Networking & Protocols](../Networking-&-Protocols/)
- [Code Quality & Best Practices](../Code-Quality-&-Best-Practices/)

---

## Status Tracker

```
[ ] 01 — Git Best Practices
[ ] 02 — CI/CD Concepts
[ ] 03 — GitHub Actions
[ ] 04 — Docker in CI/CD
[ ] 05 — Infrastructure as Code
[ ] 06 — Environment Management
[ ] 07 — Monitoring & Alerting
```

Topic complete hone par file header aur is README dono mein status update kar lena.

Happy shipping!
