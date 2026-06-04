---
description: Implement or update a Moongchijang backend feature from a .claude/features YAML spec. Use when the user asks to build, fix, or refactor an API or domain behavior against a spec.
argument-hint: "[feature-spec-path] [task]"
---

Use this workflow for code changes:

1. Read `.claude/core/essential-rules.yaml`, `.claude/core/system-design.yaml`, and the feature spec named in `$ARGUMENTS`.
2. Inspect the matching existing code paths from the spec before editing.
3. Keep package placement aligned with `presentation|application|domain|infrastructure`.
4. Implement the smallest behavior change that satisfies the spec.
5. Add or update tests listed in the spec's `testing_strategy`.
6. If API shape changed, update `src/main/resources/static/openapi.yaml`.
7. Run a focused test first; run broader tests when shared behavior changed.
8. Summarize changed files, verification, and any spec/OpenAPI sync.

Do not invent a new package convention. Do not return entities from controllers. Do not commit unrelated files.
