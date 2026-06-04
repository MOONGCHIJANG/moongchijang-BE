---
description: Create a GitHub issue that triggers Jira issue and branch automation, then work from the generated branch and open a PR with the repository template. Use when the user asks to use the issue skill, create an issue, get the Jira branch, or apply the PR template.
argument-hint: "[parent-ticket] [branch-prefix] [issue-title]"
---

Use this workflow when starting Moongchijang backend work that must go through GitHub Issue -> Jira issue -> generated branch -> PR.

## 1. Preflight

1. Run `git status --short --branch`.
2. Confirm the current repository is `MOONGCHIJANG/moongchijang-BE`.
3. Confirm GitHub authentication is active with `gh auth status`.
4. Check the current GitHub user with `gh api user --jq .login`.
5. Do not proceed as `xeraph040`. The expected author for eun-seoo work is `eun-seoo`.
6. If the worktree has unrelated local changes, leave them unstaged and use a separate worktree for the new task.

## 2. Create GitHub Issue

Create the issue with the repository issue form values expected by `.github/ISSUE_TEMPLATE/issue_form.yml`.

Required fields:

- `상위 작업 Ticket Number`: parent Jira ticket such as `SCRUM-00`
- `브랜치 전략(GitFlow)`: one of `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- `📄 설명`: concise background and implementation scope
- `✅ 작업할 내용`: checkbox list of implementation tasks
- `🙋🏻 참고 자료`: related PRs, docs, screenshots, or empty when not needed

The `Create Jira issue` workflow listens to opened GitHub issues and will:

- create a Jira task under the provided parent ticket
- create a branch from `develop`
- name the branch as `{branchPrefix}/#{githubIssueNumber}-{jiraIssueKey}`
- update the GitHub issue title to include the Jira issue key

## 3. Wait For Generated Branch

After opening the issue:

1. Watch the `Create Jira issue` GitHub Actions run.
2. Wait until it succeeds.
3. Read the generated branch name from the action log or issue title.
4. Fetch remote branches with `git fetch origin`.
5. Check out the generated branch in a separate worktree when the current worktree has unrelated changes.

Example:

```bash
git fetch origin
git worktree add ../moongchijang-BE-issue-123 origin/feat/#123-SCRUM-456
```

## 4. Implement

1. Work only on the generated Jira branch.
2. Keep changes scoped to the issue checklist.
3. Do not revert unrelated user or teammate changes.
4. Run focused tests first.
5. Run broader verification when shared payment, auth, notification, migration, or infrastructure behavior changed.
6. For DB changes, add a Flyway migration and verify migration order against latest `develop`.
7. Never commit `.env*`, secrets, local build output, or generated IDE files.

## 5. Commit And Push

1. Review `git diff --name-only`.
2. Stage only files for this issue.
3. Commit with the actual work summary.
4. Push the generated branch to origin.

Commit message examples:

```text
docs: add payment failure notification guide
feat: add payment failure audit alert
fix: verify portone webhook signature
```

## 6. Open PR With Template

Use `.github/pull_request_template.md` and fill every section.

Required PR body shape:

```markdown
## 📌 작업한 내용
- ...

## 🔍 참고 사항
- ...

## 🖼️ 스크린샷
- 해당 없음

## 🔗 관련 이슈
- closes #<issue-number>

## ✅ 체크리스트
- [x] 로컬에서 빌드 및 테스트 완료
- [x] 코드 리뷰 반영 완료
- [x] 문서화 필요 여부 확인
```

Open the PR into `develop` unless the user explicitly requests another base.

After opening the PR:

1. Confirm the PR title includes the Jira key.
2. Confirm CI status.
3. If Gemini or review feedback arrives, apply only relevant feedback on the same branch.
4. Push follow-up commits without rewriting teammate history.

## 7. Final Report

Report:

- issue number
- Jira ticket key
- generated branch name
- PR URL
- changed files
- verification result
- remaining manual setup, if any

