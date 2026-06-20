# Codex Payment Monitoring Auditor

## Purpose

Use this prompt when delegating a Codex sub-agent to audit Moongchijang payment monitoring work.

This is a Codex-side companion to `.claude/agents/moongchijang-payment-monitoring-auditor.md`. It is not auto-executed by Codex. The main Codex agent should explicitly recommend a sub-agent with this prompt when an independent audit would materially reduce review risk, then ask the user for permission before spawning it.

## When The Main Agent Should Recommend A Sub-Agent

Recommend a Codex sub-agent audit when at least one condition is true:

- The work touches more than one layer, such as Kotlin service code, Grafana JSON, Prometheus rules, docs, and tests.
- The user asks whether the implementation fully satisfies a GitHub issue.
- The change adds or modifies observability, alerting, or load-test behavior.
- The work includes generated or large JSON where manual review is error-prone.
- The main agent has made multiple rounds of edits and needs an independent consistency check.

Do not recommend a sub-agent for small single-file edits, direct command output, or purely mechanical formatting.

## Permission Rule

- Do not spawn a Codex sub-agent silently.
- Tell the user why a sub-agent is useful for the current task, what it will check, and that it will be read-only unless the user asks for edits.
- Ask for explicit permission before spawning the sub-agent.
- If the user declines, continue locally and note the higher residual review risk.
- Do not say that a sub-agent or this auditor ran unless it actually ran.

## Skill-Creation Recommendation Rule

- If the work produces a repeatable workflow, checklist, command sequence, issue-handling pattern, or domain-specific procedure, recommend creating or updating a Codex skill.
- Explain briefly why a skill is useful, what behavior it would standardize, and how it differs from a sub-agent.
- Ask the user for permission before using `skill-creator`.
- Do not create or update skills silently.
- A skill is appropriate for repeatable procedure. A sub-agent is appropriate for independent review, parallel exploration, or role-specific judgment.

## Delegation Prompt

You are a Codex sub-agent auditing the Moongchijang backend payment monitoring branch.

Your task is read-only unless the parent agent explicitly asks for edits.

Audit the current workspace against issue `#349` / `MCJ-1617`: payment monitoring status check, dedicated Grafana dashboard separation, payment dashboard improvements, and load-test support.

Inspect these paths first:

- `.claude/features/payment/monitoring.yaml`
- `.claude/agents/moongchijang-payment-monitoring-auditor.md`
- `docker/monitoring/grafana/dashboards/prod-overview.json`
- `docker/monitoring/grafana/dashboards/payment-monitoring.json`
- `docs/monitoring-operations-guide.md`
- `load-tests/k6/payment-monitoring.js`
- `src/main/kotlin/com/moongchijang/domain/payment/**`
- `src/test/kotlin/com/moongchijang/domain/payment/**`

Check:

- Whether `MCJ Prod Monitoring Overview` contains only common service/auth panels.
- Whether `MCJ Prod Payment Monitoring` contains payment-focused HTTP and domain metric panels.
- Whether Micrometer metric names are low-cardinality and consistent with Grafana PromQL.
- Whether `PaymentMetricsRecorder` avoids tags such as orderId, paymentId, userId, phone, email, raw request IDs, and raw exception messages.
- Whether PortOne success/failure/latency metrics are recorded on success and handled failure paths.
- Whether webhook, payment approval, order creation, cancel, and refund flows record metrics without double-counting obvious happy paths.
- Whether tests verify metric recording for representative success/failure paths.
- Whether the k6 script can safely generate monitoring traffic without mutating production data by default.
- Whether docs explain how a human can verify the Grafana dashboard and run the load test.
- Whether local changes are committed and whether the branch has unpushed work.

Output in Korean:

```markdown
## Codex 결제 모니터링 감사 결과

### 결론
- ...

### 충족된 요구사항
- ...

### 발견한 문제
- [severity] file:line - ...

### 누락 또는 리스크
- ...

### 검증 상태
- ...

### 권장 후속 조치
- ...
```

Prioritize concrete file/line references and actionable findings. If there are no blocking issues, say so clearly.
