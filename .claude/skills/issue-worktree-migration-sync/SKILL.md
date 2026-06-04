---
description: Create or continue a Moongchijang GitHub/Jira issue branch safely across worktrees, with mandatory develop sync and Flyway migration version checks. Use before starting issue work, adding DB migrations, rebasing/merging develop into an issue branch, or opening/updating a PR that touches migrations.
argument-hint: "[issue title or existing issue/branch]"
---

# Issue Worktree Migration Sync

Use this for issue-based work, especially when `src/main/resources/db/migration` may change.

Goal: prevent duplicate or stale Flyway versions caused by multiple worktrees or old local `develop`.

## Non-Negotiable Rules

- `origin/develop` is the source of truth. Do not trust a local `develop` branch until it has been fetched and fast-forwarded.
- Never choose a migration version from memory, another worktree, or an old branch.
- Create or rename migrations only after syncing the issue branch with the latest `origin/develop`.
- Before PR push, check whether `origin/develop` added newer migrations. If yes, merge it and fix migration numbering before pushing.
- Keep one active worktree per issue branch. If another worktree already owns the branch, use that worktree or intentionally close/remove the stale one.

## Start Or Continue Issue Work

1. Inspect state:

```bash
git status --short --branch
git worktree list
git branch --show-current
gh auth status
```

2. Protect local changes:

- If dirty changes are unrelated, stop and ask before moving branches.
- If dirty changes are intended for this task, commit or stash them before switching worktrees.
- Never stage `.env*`, build output, IDE files, or unrelated generated files.

3. Sync local `develop`:

```bash
git fetch origin develop
git checkout develop
git pull --ff-only origin develop
```

4. Create or reuse the GitHub issue:

- Use the repository issue template fields:
  - `상위 작업 Ticket Number`
  - `브랜치 전략(GitFlow)`
  - `📄 설명`
  - `✅ 작업할 내용`
- Wait for the Jira automation workflow to finish.
- Confirm the issue title contains the created `MCJ-####` key.
- Fetch the generated branch:

```bash
git fetch origin
git checkout -B "feat/#<issue>-MCJ-####" "origin/feat/#<issue>-MCJ-####"
```

Use the actual generated prefix/key. Do not invent a branch name when automation created one.

## Before Adding A Migration

Run this immediately before creating any `V*.sql` file:

```bash
git fetch origin develop
git merge origin/develop
find src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' | sort -V | tail -10
```

Then choose the next version from the latest file in the current branch.

Duplicate check:

```bash
find src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' \
  | sed -E 's#.*/(V[0-9]+)__.*#\1#' \
  | sort \
  | uniq -d
```

This command must print nothing. If it prints a version, resolve before continuing.

## If Migration Conflict Happens

When both the issue branch and latest `origin/develop` contain the same `V##` with different names:

1. Keep the already-landed `origin/develop` migration number.
2. Rename the issue branch migration to the next available version.
3. Re-run duplicate check.
4. Run focused tests or at least application context/Flyway validation if available.

Never delete or renumber migrations already present on `origin/develop`.

## Before PR Push Or PR Update

Always do this after implementation and before pushing:

```bash
git fetch origin develop
git diff --name-status HEAD..origin/develop -- src/main/resources/db/migration
git merge origin/develop
find src/main/resources/db/migration -maxdepth 1 -type f -name 'V*.sql' | sort -V | tail -10
```

If the diff shows new migrations from `origin/develop`, re-check and fix numbering before push.

Then verify:

```bash
git status --short
git diff --cached --name-only
```

Run the narrowest meaningful tests. For migration changes, include a test or startup/Flyway validation when practical.

## PR Completion Checklist

- Branch came from Jira automation or matches the generated issue key.
- Branch was synced with latest `origin/develop` before migration creation.
- Branch was synced again immediately before push.
- `src/main/resources/db/migration` has no duplicate `V##`.
- Latest migration list was checked and mentioned in the PR.
- PR template includes tests run and migration sync result.
