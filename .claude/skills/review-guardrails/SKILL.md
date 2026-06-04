---
description: Review changed Moongchijang backend code against project guardrails before commit. Use when the user asks for review, risk check, or pre-commit validation.
argument-hint: "[optional diff scope]"
---

Review changed files with this priority:

1. Bugs or behavior regressions.
2. Security/auth/ownership issues.
3. Transaction, concurrency, and idempotency issues.
4. API response or ErrorCode mismatches.
5. Missing tests for changed behavior.
6. Spec/OpenAPI drift.

Use `.claude/core/essential-rules.yaml` and `.claude/core/system-design.yaml`.

Lead with findings ordered by severity. Include file and line references. If there are no findings, state that and mention residual test risk.
