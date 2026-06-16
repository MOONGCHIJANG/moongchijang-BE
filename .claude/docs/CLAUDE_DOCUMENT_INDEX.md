# Claude Document Index

This file is the finder for the Moongchijang backend Claude Code harness.

## Entry Point

- `.claude/CLAUDE.md`: project memory and default workflow
- `.claude/settings.json`: shared Claude Code permissions and hooks

## Core Rules

- `.claude/core/essential-rules.yaml`: stack, package rules, hard guardrails
- `.claude/core/system-design.yaml`: architecture, API wrapper, ErrorCode, security, testing

## Skills

- `.claude/skills/feature-implementer/SKILL.md`: implement or update a feature from a spec
- `.claude/skills/spec-sync/SKILL.md`: sync code, feature specs, ErrorCode, and OpenAPI
- `.claude/skills/review-guardrails/SKILL.md`: review changed code against project guardrails
- `.claude/skills/develop-merge/SKILL.md`: prepare a scoped direct push to develop

## Agents

- `.claude/agents/moongchijang-spec-auditor.md`: read-only spec/code drift auditor
- `.claude/agents/moongchijang-code-reviewer.md`: read-only backend reviewer
- `.claude/agents/moongchijang-test-runner.md`: focused test selection and verification
- `.claude/agents/moongchijang-payment-monitoring-auditor.md`: read-only payment monitoring dashboard, alert, and metric coverage auditor

## Feature Specs

- `.claude/features/admin/admin.yaml`: admin dashboards, orders, settlement, refunds, CS tickets
- `.claude/features/groupbuy/feed-list.yaml`: group buy feed list
- `.claude/features/groupbuy/detail.yaml`: group buy detail
- `.claude/features/groupbuy/progress.yaml`: group buy progress polling
- `.claude/features/groupbuy/share.yaml`: share metadata
- `.claude/features/groupbuy/request.yaml`: consumer group buy request and admin handling
- `.claude/features/payment/participation.yaml`: payment order, PortOne completion, participation
- `.claude/features/payment/refund.yaml`: participation cancel and refund request flow
- `.claude/features/payment/monitoring.yaml`: payment monitoring dashboard, alert channel separation, and metric coverage plan
- `.claude/features/notification/notification.yaml`: notifications, templates, dispatch
- `.claude/features/wishlist/wishlist.yaml`: wishlist command and query
- `.claude/features/pickup/pickup.yaml`: pickup guide, QR, verification
- `.claude/features/user/auth.yaml`: auth, verification, JWT, Kakao/email flows
- `.claude/features/user/mypage.yaml`: mypage, account changes, role switching, withdrawal
- `.claude/features/store/search.yaml`: store search through external local search
- `.claude/features/search/search.yaml`: product/store search and search history
- `.claude/features/owner/owner-groupbuy.yaml`: seller group buy management and requests

## Conventions

- `.claude/references/conventions/coding-style.yaml`: Kotlin/Spring coding conventions
- `.claude/references/conventions/testing-rules.yaml`: test expectations and commands
- `.claude/references/conventions/git-conventions.yaml`: branch, commit, staging, and secret rules

## Templates And Hooks

- `.claude/docs/templates/feature-spec.yaml`: template for a new feature spec
- `.claude/hooks/build_checker.sh`: optional Kotlin compile hook for Claude Code edits
