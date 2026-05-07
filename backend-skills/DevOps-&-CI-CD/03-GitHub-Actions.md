# GitHub Actions

## Status: Not Started

---

## Table of Contents

1. [GitHub Actions Kya Hai?](#github-actions-kya-hai)
2. [Workflow Structure](#workflow-structure)
3. [Triggers](#triggers)
4. [Jobs and Steps](#jobs-and-steps)
5. [Actions (`uses:`)](#actions-uses)
6. [Environment Variables](#environment-variables)
7. [Secrets](#secrets)
8. [Artifacts](#artifacts)
9. [Caching](#caching)
10. [Matrix Builds](#matrix-builds)
11. [Reusable Workflows & Composite Actions](#reusable-workflows--composite-actions)
12. [Real-World Examples](#real-world-examples)
13. [Best Practices](#best-practices)
14. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## GitHub Actions Kya Hai?

**Matlab:** GitHub-native CI/CD platform — workflows YAML mein define karo, GitHub events par run.

- Free tier (public repos unlimited; private: 2000 min/month free)
- Marketplace (`actions/`) for reusable steps
- Hosted runners (Linux/Mac/Windows) or self-hosted

---

## Workflow Structure

Files: `.github/workflows/*.yml`

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - run: ./mvnw -B verify
```

### Top-level Keys

| Key | Purpose |
|-----|---------|
| `name` | Display name in UI |
| `on` | Triggers |
| `env` | Workflow-level env vars |
| `jobs` | Parallel/sequential jobs |
| `permissions` | Token scopes |
| `concurrency` | Cancel/queue duplicate runs |

---

## Triggers

### Push / PR

```yaml
on:
  push:
    branches: [main, 'release/**']
    paths:
      - 'src/**'
      - 'pom.xml'
  pull_request:
    types: [opened, synchronize, reopened]
```

### Schedule (cron)

```yaml
on:
  schedule:
    - cron: '0 6 * * *'   # daily 6:00 UTC
```

### Manual (workflow_dispatch)

```yaml
on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Target environment'
        required: true
        type: choice
        options: [staging, prod]
      version:
        type: string
```

Trigger from GitHub UI or `gh workflow run`.

### Other

| Trigger | When |
|---------|------|
| `release` | Release published |
| `issues` | Issue events |
| `workflow_call` | Reusable workflow invocation |
| `repository_dispatch` | External webhook |

---

## Jobs and Steps

### Multiple Jobs

```yaml
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: ./mvnw spotless:check

  test:
    runs-on: ubuntu-latest
    needs: lint           # waits for lint to succeed
    steps:
      - uses: actions/checkout@v4
      - run: ./mvnw test

  deploy:
    runs-on: ubuntu-latest
    needs: [lint, test]   # waits for both
    if: github.ref == 'refs/heads/main'
    steps:
      - run: echo deploy
```

### Step Types

```yaml
steps:
  - name: Step name (optional)
    uses: actions/checkout@v4         # use a marketplace action
  
  - name: Run shell command
    run: |
      echo "Multi-line"
      ls -la
  
  - name: With env
    run: ./script.sh
    env:
      DB_URL: ${{ secrets.DB_URL }}
```

### Conditional Steps

```yaml
- if: github.event_name == 'pull_request'
  run: echo "PR-only step"

- if: failure()
  run: echo "Run only when previous step failed"

- if: always()
  run: echo "Run regardless of previous outcome"
```

---

## Actions (`uses:`)

Reusable steps from marketplace or your own.

### Common Actions

```yaml
- uses: actions/checkout@v4

- uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: maven                    # auto-cache m2

- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'

- uses: actions/setup-go@v5
  with:
    go-version: '1.22'

- uses: docker/setup-buildx-action@v3

- uses: docker/login-action@v3
  with:
    registry: ghcr.io
    username: ${{ github.actor }}
    password: ${{ secrets.GITHUB_TOKEN }}

- uses: docker/build-push-action@v6
  with:
    push: true
    tags: |
      ghcr.io/${{ github.repository }}:${{ github.sha }}
      ghcr.io/${{ github.repository }}:latest

- uses: aws-actions/configure-aws-credentials@v4
  with:
    role-to-assume: arn:aws:iam::123:role/deploy
    aws-region: ap-south-1
```

### Pinning

```yaml
# ❌ Floating tag — could change
- uses: actions/checkout@v4

# ✅ Pinned to commit SHA — immutable (security best practice)
- uses: actions/checkout@b4ffde65f46336ab88eb53be808477a3936bae11
```

---

## Environment Variables

### Workflow / Job / Step Level

```yaml
env:
  GLOBAL_VAR: value

jobs:
  build:
    env:
      JOB_VAR: value
    steps:
      - run: echo $GLOBAL_VAR $JOB_VAR
        env:
          STEP_VAR: value
```

### Built-in `${{ github.* }}` Context

```yaml
${{ github.sha }}            # commit SHA
${{ github.ref }}            # refs/heads/main
${{ github.ref_name }}       # main
${{ github.actor }}          # username triggering
${{ github.event_name }}     # push, pull_request, etc.
${{ github.repository }}     # owner/repo
${{ github.workspace }}      # checkout path
${{ runner.os }}             # Linux/macOS/Windows
```

### Outputs (Step → Step / Job → Job)

```yaml
- id: meta
  run: echo "version=1.2.3" >> $GITHUB_OUTPUT

- run: echo "Version is ${{ steps.meta.outputs.version }}"
```

```yaml
jobs:
  build:
    outputs:
      tag: ${{ steps.meta.outputs.tag }}
    steps:
      - id: meta
        run: echo "tag=v$(date +%Y%m%d)" >> $GITHUB_OUTPUT

  deploy:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - run: echo "Deploying ${{ needs.build.outputs.tag }}"
```

---

## Secrets

### Where to Set

- Repo: Settings → Secrets and variables → Actions
- Org-level: shared across repos
- Environment-level: gated per env (with approval)

### Use

```yaml
env:
  DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
```

### Secret Behavior

- Masked in logs (`***`)
- Not passed to forked-PR workflows (security)
- `${{ secrets.GITHUB_TOKEN }}` auto-provided per workflow

### OIDC for Cloud Auth (No Long-Lived Secrets!)

```yaml
permissions:
  id-token: write
  contents: read

steps:
  - uses: aws-actions/configure-aws-credentials@v4
    with:
      role-to-assume: arn:aws:iam::123:role/deploy
      aws-region: us-east-1
```

→ AWS trusts GitHub's OIDC issuer; no static keys.

---

## Artifacts

**Matlab:** Workflow output files store karna (test reports, build outputs, logs) — across jobs ya for download.

### Upload

```yaml
- name: Upload test report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: test-report
    path: target/surefire-reports/
    retention-days: 7
```

### Download (later job)

```yaml
deploy:
  needs: build
  steps:
    - uses: actions/download-artifact@v4
      with:
        name: build-output
        path: ./dist
```

---

## Caching

**Matlab:** Dependencies cache — har run pe re-download save karta hai.

### Maven

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: |
      ${{ runner.os }}-maven-
```

Or simpler:
```yaml
- uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'temurin'
    cache: maven    # built-in!
```

### Gradle

```yaml
- uses: gradle/actions/setup-gradle@v3
```

### Node

```yaml
- uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'
```

### Docker Layer Cache

```yaml
- uses: docker/build-push-action@v6
  with:
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

### Cache Key Strategy

```
${{ runner.os }}-tool-${{ hashFiles('lockfile') }}
```

- OS in key (cross-OS caches break)
- Hash of lockfile → invalidate when deps change
- `restore-keys` for partial fallback

---

## Matrix Builds

**Matlab:** Same job multiple combinations par parallel run — Java versions, OS, etc.

```yaml
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      fail-fast: false
      matrix:
        java: ['17', '21']
        os: [ubuntu-latest, windows-latest, macos-latest]
        include:
          - java: '21'
            os: ubuntu-latest
            extra: 'experimental'
        exclude:
          - java: '17'
            os: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ matrix.java }}
          distribution: 'temurin'
      - run: ./mvnw test
```

→ Runs `2 × 3 = 6 jobs` in parallel (minus exclusions).

### `fail-fast`

- `true` (default): one failure → cancel rest
- `false`: continue all → see all failures

---

## Reusable Workflows & Composite Actions

### Reusable Workflow

`.github/workflows/build.yml`:
```yaml
on:
  workflow_call:
    inputs:
      java-version:
        type: string
        default: '21'
    secrets:
      DOCKER_PAT:
        required: true

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: ${{ inputs.java-version }}
          distribution: 'temurin'
      - run: ./mvnw verify
```

Calling workflow:
```yaml
jobs:
  call-build:
    uses: ./.github/workflows/build.yml
    with:
      java-version: '21'
    secrets:
      DOCKER_PAT: ${{ secrets.DOCKER_PAT }}
```

### Composite Action

`.github/actions/setup-app/action.yml`:
```yaml
name: Setup App
inputs:
  java-version:
    default: '21'
runs:
  using: composite
  steps:
    - uses: actions/setup-java@v4
      with:
        java-version: ${{ inputs.java-version }}
        distribution: 'temurin'
    - run: ./mvnw -B dependency:resolve
      shell: bash
```

Use:
```yaml
- uses: ./.github/actions/setup-app
```

---

## Real-World Examples

### Spring Boot Build + Test + Docker

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:

env:
  REGISTRY: ghcr.io
  IMAGE: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build & test
        run: ./mvnw -B verify

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/surefire-reports/'

  docker:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4

      - uses: docker/setup-buildx-action@v3

      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - uses: docker/build-push-action@v6
        with:
          push: true
          tags: |
            ${{ env.REGISTRY }}/${{ env.IMAGE }}:${{ github.sha }}
            ${{ env.REGISTRY }}/${{ env.IMAGE }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### Deploy with Manual Approval

```yaml
deploy-prod:
  needs: docker
  runs-on: ubuntu-latest
  environment: production           # gates with approval rules
  steps:
    - run: ./deploy.sh ${{ github.sha }}
```

### PR Auto-Label / Comment

```yaml
- uses: actions/github-script@v7
  with:
    script: |
      github.rest.issues.createComment({
        issue_number: context.issue.number,
        owner: context.repo.owner,
        repo: context.repo.repo,
        body: 'Build succeeded ✅'
      })
```

---

## Best Practices

### 1. Pin Action SHAs

For external (non-`actions/*`) actions, pin to commit SHA — supply chain security.

### 2. Minimal `permissions`

```yaml
permissions:
  contents: read       # only what's needed
  packages: write
  id-token: write
```

Default-restrict at workflow level, expand per job.

### 3. Use `concurrency` to Cancel Stale Runs

```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```

→ Push new commit on PR → cancel old run.

### 4. Fail Fast in Matrix

For PRs (faster feedback): `fail-fast: true`
For full main branch verification: `fail-fast: false`

### 5. Cache Smart

- Lockfile-based keys
- `restore-keys` fallback chain

### 6. Don't Run All Workflows on All Triggers

Use `paths:` filter — irrelevant changes skip CI.

### 7. Use Environment Protection Rules

For `prod` env: require approvals + branch restriction + secrets only here.

### 8. OIDC > Long-Lived Cloud Keys

Eliminates rotation, theft risk.

### 9. Status Checks Required

Repo Settings → Branches → Require status checks → block merges if CI red.

### 10. Test the Workflow Locally with `act`

```bash
brew install act
act push
```

Run workflows in Docker before pushing.

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| File location | `.github/workflows/*.yml` |
| `on:` | Triggers (push, PR, schedule, manual) |
| `jobs.X.needs` | Dependency between jobs |
| `uses:` | Marketplace action |
| `run:` | Shell command |
| `${{ ... }}` | Expression (vars, contexts) |
| `secrets.X` | Repo/org/env secrets |
| `env:` | Variables |
| `actions/checkout@v4` | Clone repo |
| `actions/cache@v4` | Cache deps |
| `actions/upload-artifact` | Save outputs |
| Matrix | Parallel multi-config |
| `concurrency` | Cancel duplicate runs |
| OIDC | Cloud auth without secrets |

---

## Practice

1. Apne repo mein basic CI workflow add karo (build + test).
2. Matrix: Java 17 + 21 dono pe test chalao.
3. Cache Maven deps; before/after run time compare karo.
4. Docker build + push to GHCR add karo.
5. Add `concurrency` to cancel stale PR runs.
6. Deploy job ko `environment: production` ke peeche manual approval ke saath wrap karo.
7. OIDC se AWS auth set up karo (no static credentials).
