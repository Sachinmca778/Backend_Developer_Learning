# Git Best Practices

## Status: Not Started

---

## Table of Contents

1. [Git Quick Mental Model](#git-quick-mental-model)
2. [Branching Strategies](#branching-strategies)
3. [Conventional Commits](#conventional-commits)
4. [Semantic Versioning (SemVer)](#semantic-versioning-semver)
5. [Rebase vs Merge](#rebase-vs-merge)
6. [PR Hygiene](#pr-hygiene)
7. [Common Commands Cheat Sheet](#common-commands-cheat-sheet)
8. [Summary Cheat Sheet](#summary-cheat-sheet)

---

## Git Quick Mental Model

```
Working Directory  →  Staging (Index)  →  Local Repo  →  Remote
        ↑                  ↑                ↑
       edit            git add          git commit       git push
```

- **Commit** = snapshot + parent pointer + author/message
- **Branch** = movable pointer to a commit
- **HEAD** = "where am I" — usually a branch tip

---

## Branching Strategies

Team size, release cadence, deployment risk — strategy isi par depend.

### 1. GitFlow

**Matlab:** Multiple long-lived branches — older big-release model.

```
main      ────────●──────────●─────────●  (production tags v1.0, v1.1)
                  │          │         │
release    ──────●──────●───/         /
                  │      │            /
develop   ───●────●──────●──────●────●  (integration)
              │        │       │
feature   ───●        │       /
                       │      /
hotfix    ────────────●──────●  (urgent prod fix → merged to main + develop)
```

**Branches:**
- `main` — production
- `develop` — integration of upcoming release
- `feature/*` — new work, off `develop`
- `release/*` — stabilization
- `hotfix/*` — urgent prod patches off `main`

**Use when:** Versioned releases (mobile, libraries, on-prem).
**Avoid when:** Continuous deployment / SaaS — overhead high.

### 2. GitHub Flow

**Matlab:** Simple — `main` always deployable; har feature branch → PR → merge → deploy.

```
main      ──●──────●──────●──────●─────●
            ↑      ↑      ↑      ↑
         feature/ feature/ feature/ feature/
```

- One main branch
- Short-lived feature branches
- Merge via PR with CI green + reviews
- Deploy `main` (often after merge)

**Use when:** Web apps, SaaS, frequent deploys.
**Most popular** for modern teams.

### 3. Trunk-Based Development

**Matlab:** Sab developers **directly trunk** (`main`) ke close kaam — feature flags se incomplete code hide.

```
main  ──●──●──●──●──●──●──●──●──●──●──●  (every commit deployable)
       small PRs / direct commits, hidden behind flags
```

**Practices:**
- Branches **very short** (hours, not days)
- Feature flags for incomplete features
- Continuous integration multiple times daily
- High test coverage required

**Use when:** Mature teams + strong CI + feature flag infra.
**Benefit:** No long-lived branches → no merge hell.

### Comparison

| Strategy | Branches | Cadence | Best for |
|----------|----------|---------|----------|
| GitFlow | Many long-lived | Versioned releases | Libraries, mobile, on-prem |
| GitHub Flow | One main + short features | Continuous | SaaS, web apps |
| Trunk-based | Just main | Multiple/day | Elite/high-perf teams |

---

## Conventional Commits

**Matlab:** Commit message ka **standard format** — automation friendly (changelogs, version bumps).

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Common Types

| Type | When |
|------|------|
| `feat` | New feature |
| `fix` | Bug fix |
| `chore` | Tooling, deps, no prod logic |
| `docs` | Documentation only |
| `refactor` | Internal restructure, no behavior change |
| `test` | Add/update tests |
| `perf` | Performance improvement |
| `style` | Formatting (no code change) |
| `build` | Build system / CI changes |
| `ci` | CI config |
| `revert` | Revert previous commit |

### Examples

```
feat(auth): add JWT refresh token endpoint

Implements POST /auth/refresh with rotating tokens.
Closes #123
```

```
fix(orders): prevent duplicate charge on retry

Idempotency-Key now hashed in Redis before processing.

BREAKING CHANGE: clients must send Idempotency-Key header.
Co-authored-by: ...
```

### Why?

- Auto-generate **CHANGELOG**
- Auto-bump **SemVer** (`feat` → minor, `fix` → patch, `BREAKING CHANGE` → major)
- Tools: `commitlint`, `semantic-release`, `release-please`

---

## Semantic Versioning (SemVer)

**Format:** `MAJOR.MINOR.PATCH` (e.g., `2.4.1`)

| Bump | When |
|------|------|
| **MAJOR** | Breaking change (incompatible API) |
| **MINOR** | Backward-compatible feature added |
| **PATCH** | Backward-compatible bug fix |

### Pre-release / Build Metadata

```
1.0.0-alpha.1
1.0.0-rc.1
1.0.0+20260507.commit-abc
```

### Examples

```
1.2.3 → 1.2.4   bug fix
1.2.4 → 1.3.0   new endpoint
1.3.0 → 2.0.0   removed deprecated endpoint
```

**Library publishing:** Strict SemVer trust — consumers rely on it.
**Apps:** Often loose (calendar versioning `2026.05.01` also common).

---

## Rebase vs Merge

### Merge

**Matlab:** Branches **combine** — preserves history, creates "merge commit" for non-fast-forward.

```
main:    A───B───C────────M
                  \      /
feature:           D───E
```

```bash
git checkout main
git merge feature
```

✅ Non-destructive
✅ Real history preserved
❌ Many merge commits clutter

### Rebase

**Matlab:** Feature branch ke commits ko `main` ke top par **replay** — linear history.

```
Before:
main:     A───B───C
                \
feature:        D───E

After git rebase main:
main:     A───B───C
                  \
feature:           D'──E'  (new commits!)
```

```bash
git checkout feature
git rebase main
```

✅ Clean linear history
✅ Easier to read `git log`
❌ Rewrites history — **never rebase shared/public branches**
❌ Conflicts resolved per commit (more work for big branches)

### Squash Merge

**Matlab:** Feature ke saare commits ko ek single commit mein collapse karke main par add.

```bash
git merge --squash feature
```

GitHub/GitLab UI option.

✅ Cleanest history (one commit per feature)
❌ Lose granular history of feature

### When to Use What?

| Scenario | Use |
|----------|-----|
| Local cleanup before pushing | `rebase -i` |
| Long-lived feature behind main | `rebase` periodically |
| Protected/shared branches | `merge` (never rebase) |
| Feature with many WIP commits | `squash merge` |
| Want full granular history | `merge` (no squash) |

### Golden Rule

> Never rebase commits that have been pushed to a shared/public branch.

### Interactive Rebase (`rebase -i`)

Powerful — reorder, squash, edit, drop commits before pushing.

```bash
git rebase -i HEAD~5
# Opens editor with last 5 commits
# Change `pick` → `squash`/`fixup`/`edit`/`drop`/`reword`
```

### `git pull --rebase`

```bash
git pull --rebase    # avoid noisy "Merge branch main into ..." commits
```

Set as default:
```bash
git config --global pull.rebase true
```

---

## PR Hygiene

- **Small** PRs (covered in Code Review file)
- **Descriptive title** (conventional commit format works as title)
- **Link issues** (`Closes #123`)
- **Self-review diff** before requesting reviewers
- **Resolve conflicts** locally (rebase on main)
- **Don't force-push** to others' branches without notice

### Force Push Safely

```bash
# DANGEROUS — overwrites remote history
git push --force

# SAFER — fails if someone else pushed
git push --force-with-lease
```

Always prefer `--force-with-lease`.

---

## Common Commands Cheat Sheet

### Daily

```bash
git status                          # what changed
git diff                            # unstaged diff
git diff --staged                   # staged diff
git add -p                          # interactive staging
git commit -m "feat(x): ..."
git push -u origin feature/x        # first push
```

### Branching

```bash
git checkout -b feature/login       # create + switch
git switch main                     # modern alternative
git branch -D old-branch            # delete local force
git push origin --delete old-branch # delete remote
```

### Sync

```bash
git fetch                           # update refs without merge
git pull --rebase                   # fetch + rebase
git pull origin main --rebase       # specific
```

### Undo / Recovery

```bash
git reset HEAD~1                    # undo last commit, keep changes
git reset --hard HEAD~1             # undo + discard changes (DANGER)
git revert <sha>                    # safe — creates new commit reversing
git restore file.txt                # discard unstaged changes
git restore --staged file.txt       # unstage
git reflog                          # history of HEAD moves — recovery
git cherry-pick <sha>               # apply single commit
git stash                           # save WIP
git stash pop                       # restore WIP
```

### Inspection

```bash
git log --oneline --graph --all     # visual history
git log -p file.txt                 # changes per commit on file
git blame file.txt                  # who wrote each line
git show <sha>                      # commit details
git diff main..feature              # branch comparison
```

### Tagging (releases)

```bash
git tag -a v1.2.0 -m "Release 1.2.0"
git push origin v1.2.0
git push --tags                     # all tags
```

---

## Summary Cheat Sheet

| Concept | Quick Note |
|---------|-----------|
| GitFlow | Multi-branch, versioned releases |
| GitHub Flow | One main + feature PRs |
| Trunk-based | Direct to main + feature flags |
| feat/fix/chore | Conventional commit types |
| MAJOR.MINOR.PATCH | SemVer bumps |
| Merge | Preserves history |
| Rebase | Linear history (never on shared) |
| Squash merge | One commit per feature |
| `--force-with-lease` | Safer force push |
| `git revert` | Safe undo for shared branches |
| `git reflog` | Recovery oracle |

---

## Practice

1. Apne current repo ki branching strategy identify karo — fits team size?
2. Last 5 commits messages dekho — conventional format mein rewrite (mentally).
3. Local feature branch banao, 3 commits, `rebase -i` se squash + reorder.
4. Ek bug fix ka SemVer bump decide karo (PATCH vs MINOR vs MAJOR).
5. `git reflog` se accidental `reset --hard` recover karna practice.
