# Moongchijang Backend Harness

This repository uses Claude Code project memory, skills, and subagents as a shared engineering harness.

## Load Order

Always ground backend work in these files before editing Kotlin, SQL, OpenAPI, Docker, or infrastructure:

1. `.claude/core/essential-rules.yaml`
2. `.claude/core/system-design.yaml`
3. Relevant feature spec under `.claude/features/**`
4. Relevant convention under `.claude/references/conventions/**`

Claude Code project features used here:

- Project memory: `.claude/CLAUDE.md`
- Shared project settings: `.claude/settings.json`
- Shared project subagents: `.claude/agents/*.md`
- Shared project skills: `.claude/skills/*/SKILL.md`

## Current Codebase Facts

- Kotlin 2.2.21, JDK 21, Spring Boot 4.0.5, Gradle Kotlin DSL.
- Base package is `com.moongchijang`, not `com.moongchijang.server`.
- Domain packages follow `domain/{bounded-context}/{presentation|application|domain|infrastructure}`.
- Controllers return `ResponseEntity<ApiResponse<T>>`.
- Request DTOs use Jakarta Bean Validation when they accept body payloads.
- Business exceptions use `CustomException(ErrorCode.X)`.
- Tests live under `src/test/kotlin/com/moongchijang/domain/**` and support fixtures live under `src/test/kotlin/com/moongchijang/support/**`.

## Working Principles

1. Spec first: find the matching `.claude/features/**` spec. If none exists, create or update one before implementation.
2. Keep layer boundaries: controller parses HTTP, application services own transactions and business rules, domain owns entities/repositories, infrastructure owns external clients/adapters.
3. Keep contracts synchronized: update feature spec and `src/main/resources/static/openapi.yaml` when behavior or response shape changes.
4. Tests are part of the change: add or update service/controller/repository tests according to risk.
5. Do not overwrite unrelated local changes. Check git status before committing.

## Implementation Flow

1. Read the relevant feature spec and core rules.
2. Inspect existing controller, service, DTO, entity, repository, and tests for the same bounded context.
3. Implement in this order when creating a new flow:
   - domain entity/value/state changes
   - repository or Querydsl query
   - application DTO
   - application service
   - presentation controller
   - tests
   - OpenAPI/spec sync
4. Run the narrowest meaningful test first, then a broader verification if the change touches shared behavior.

## Harness Shortcuts

Use these project skills when they match the task:

- `/feature-implementer`: implement or update a feature from a `.claude/features/**` spec.
- `/spec-sync`: reconcile code, feature spec, and OpenAPI.
- `/review-guardrails`: check changed code against project rules before commit.
- `/develop-merge`: prepare a direct commit/push to `develop` without PR.

Use these project subagents when a task is self-contained:

- `moongchijang-spec-auditor`: read-only spec/code mismatch analysis.
- `moongchijang-code-reviewer`: read-only guardrail and regression review.
- `moongchijang-test-runner`: focused test selection and verification.

## Commit Policy

- Direct `develop` pushes are allowed only when explicitly requested.
- Keep commits scoped. Do not include unrelated `.env*`, generated build output, or local IDE files.
- Preferred commit format: `docs: ...`, `test: ...`, `fix: ...`, `feat: ...`; Jira-style prefixes are used only when the task already has an issue key.
