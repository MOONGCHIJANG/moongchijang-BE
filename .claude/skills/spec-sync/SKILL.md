---
description: Reconcile Moongchijang feature specs with Kotlin code and openapi.yaml. Use when docs, API behavior, ErrorCode, or response shape may be out of sync.
argument-hint: "[feature-spec-path or domain]"
---

Spec sync checklist:

1. Read the relevant `.claude/features/**` file and listed `code_paths`.
2. Compare endpoints, auth, request fields, response DTO fields, business rules, and ErrorCode usage with code.
3. Compare public API behavior with `src/main/resources/static/openapi.yaml`.
4. Update the spec if code is the source of truth for already-implemented behavior.
5. Flag code/OpenAPI changes separately if the code appears wrong.
6. Keep feature specs concise and operational: paths, rules, errors, tests.

Output must include:

- mismatches found
- files updated
- remaining follow-up if OpenAPI or tests need separate work
