---
name: github-jira-issue-workflow
description: Use for Moongchijang GitHub issue, Jira-generated branch, commit splitting, commit conventions, push, and PR workflow. Trigger when working from an issue key/number, creating or checking an issue branch, deciding how to split commits, or preparing a PR.
---

# GitHub Jira Issue Workflow

Use this skill for Moongchijang backend work that must follow GitHub Issue -> Jira issue -> generated branch -> commits -> PR.

## Preflight

1. Run `git status --short --branch`.
2. Confirm the repository is `MOONGCHIJANG/moongchijang-BE`.
3. Use the generated issue branch, for example `chore/#349-MCJ-1617`.
4. Keep unrelated local changes unstaged.
5. Do not push unless the user explicitly approves.

## Issue And Branch

- Initial issue title: `TYPE: 한국어 요약`.
- After Jira automation: `[MCJ-####] TYPE: 한국어 요약`.
- Branch format: `{branchPrefix}/#{issueNumber}-{jiraKey}`.
- Branch prefix must match GitFlow: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, or `chore`.

Verification commands:

```bash
gh issue view <issue-number> --json title,url
git ls-remote --heads origin '<branchPrefix>/#<issue-number>-*'
```

## Commit Message

- With Jira key: `[MCJ-####] type: 한국어 요약`
- Without Jira key: `type: 한국어 요약`
- Use lowercase type: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, or `chore`.

Examples:

```text
[MCJ-1617] chore: 결제 전용 Grafana 대시보드 분리
[MCJ-1617] test: 결제 모니터링 지표 검증 추가
```

## Commit Splitting

Default to reviewable logical commits. Split when reviewers would naturally inspect files with different mental models.

Use `.claude/references/conventions/git-conventions.yaml` as the shared source of truth for commit splitting. It includes the project policy and the external basis from Conventional Commits and Google Engineering Practices.

Split commits when:

- Agent/skill/docs harness changes are mixed with product behavior.
- Grafana/Prometheus/Docker observability config is mixed with Kotlin code.
- Production code and broad tests are both substantial.
- Load-test scripts or operational runbooks are added.
- One commit message needs multiple independent clauses.

Keep together when:

- A small code change and its direct small test are easier to review together.
- A dashboard panel move requires deleting from one JSON and adding to another JSON.
- A rename requires mechanical import/path updates.

Recommended order:

1. Harness/spec/docs setup.
2. Dashboard or infrastructure config.
3. Application code changes.
4. Tests and load tests.
5. Operator documentation.

Before every commit:

```bash
git status --short
git diff --cached --name-status
git diff --cached --check
```

## History Rewriting Policy

- Before push: local commit history may be cleaned with `git reset --soft`, `git commit --amend`, or interactive rebase.
- After push: do not amend/rebase/force-push without explicit user approval.
- If the user prefers granular commits, split instead of amending unrelated work into the previous commit.

## PR

- PR title: `[MCJ-####] TYPE: 한국어 요약`.
- Base branch: `develop` unless the user requests otherwise.
- Fill `.github/pull_request_template.md`.
- Report issue number, Jira key, branch, commits, verification, and remaining manual steps.
