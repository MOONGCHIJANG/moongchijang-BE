---
description: Prepare a direct scoped commit and push to develop without PR for Moongchijang. Use only when the user explicitly asks to merge or push directly to develop.
argument-hint: "[commit message]"
disable-model-invocation: true
---

Direct develop push procedure:

1. Run `git status --short --branch`.
2. Identify unrelated local changes and leave them unstaged.
3. Stage only files needed for the requested change.
4. Review `git diff --cached --name-only` and `git diff --cached`.
5. Run relevant verification. For docs-only changes, JSON/YAML parse and shell syntax checks are enough.
6. Commit with the provided or inferred message.
7. Push the current commit to `origin develop` only after confirming the staged diff is scoped.

Never force push. Never stage `.env*`, secrets, build output, or unrelated local modifications.
