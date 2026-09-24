# ChatGPT-led AI orchestration contract

This repository is operated through a three-role workflow. The human owner gives the goal to ChatGPT. ChatGPT is the orchestrator and independent reviewer. Claude Code and Cursor Agent are execution engines. GitHub is the durable task/state bus. Production deployment is a separate gated step.

## Authority and precedence

1. Existing repository-specific engineering, product, security, visual, release and compliance rules remain authoritative.
2. This file controls orchestration, handoff, state and agent routing only.
3. The active task is defined by `.ai/TASK.md`.
4. Machine-readable task state is stored in `.ai/STATE.json`.
5. The latest executor handoff is stored in `.ai/HANDOFF.md`.
6. No executor may merge to the protected/default branch or deploy production unless the active task explicitly says that ChatGPT has opened that gate.

## Roles

### ChatGPT — orchestrator / reviewer
ChatGPT owns intake, task decomposition, executor choice, branch strategy, independent review, evidence validation, preview verification, PR review, merge decision and production verification. It may use GitHub/Vercel connectors directly and may control the owner's authorized computer through Desktop Commander.

### Claude Code — senior implementation engine
Use Claude Code for difficult root-cause debugging, architectural changes, large multi-file changes, unfamiliar subsystems, Android/VPN internals, complex migrations and deep refactors. Prefer normal/medium or high effort. Escalate to the most expensive frontier model/effort only after cheaper reasoning is insufficient or the task is genuinely high-risk.

### Cursor Agent — fast implementation / polish engine
Use Cursor for bounded implementation, repetitive edits, frontend polish, straightforward refactors, tests, lint/type fixes, asset changes, responsive fixes and small follow-up batches. Use a worktree or task branch so its changes are isolated.

## Subscription-only rule — no API billing

The intended execution path uses the owner's paid Claude and Cursor subscriptions via their native account logins.

- Claude Code must authenticate with Claude.ai / Max, not an Anthropic API key.
- Cursor CLI must authenticate with `agent login`, not a Cursor API key.
- When launched by the orchestrator, use `bash tools/ai/claude-subscription.sh ...` and `bash tools/ai/cursor-subscription.sh ...`. They remove API-key environment variables before execution.
- Never print, commit, paste or log secrets/tokens.
- If a native login expires, stop only the dependent executor and report AUTH_REQUIRED. Do not silently fall back to API billing.

## Task lifecycle

Allowed states:

`NO_ACTIVE_TASK -> INTAKE -> PLANNED -> EXECUTING_CLAUDE|EXECUTING_CURSOR -> VERIFYING -> REVIEWING -> PREVIEW_READY -> MERGE_READY -> MERGED -> PROD_VERIFYING -> PROD_VERIFIED`

Exceptional state: `BLOCKED`.

The state file must always contain one concrete `next_action`. A blocker blocks only the dependent step; independent work continues.

## Standard execution loop

1. ChatGPT writes or refreshes `.ai/TASK.md` and `.ai/STATE.json`.
2. ChatGPT chooses one primary executor. Do not run Claude and Cursor on the same write task concurrently.
3. Executor reads project rules first, then this contract and the active task.
4. Executor works only on the assigned branch/worktree.
5. Executor runs the verification commands required by the active task and existing project rules.
6. Executor updates `.ai/HANDOFF.md` with factual evidence and remaining risks.
7. Changes are pushed to GitHub.
8. ChatGPT independently reviews diff, CI, logs and relevant live/visual evidence.
9. Small deterministic follow-ups go to Cursor. Deep or ambiguous failures go to Claude.
10. Only after review gates pass may ChatGPT mark `MERGE_READY`.
11. Merge and production deployment are distinct gates. After production deploy, ChatGPT verifies production before marking `PROD_VERIFIED`.

## Routing policy

Use the cheapest capable path.

- trivial/local edit: Cursor
- bounded feature or test fix: Cursor
- multi-file feature with non-trivial reasoning: Claude Code
- root-cause bug across subsystems: Claude Code
- visual/UX audit: ChatGPT review, Cursor implementation
- architecture/security/release-risk audit: ChatGPT + Claude independent review
- frontier/max-effort model: escalation only, never default

Do not spend premium reasoning on formatting, spacing, obvious copy edits or mechanical fixes.

## Evidence rules

"Done" is not evidence. A valid handoff names:
- branch and commit SHA
- files materially changed
- commands actually run
- test/build/lint results
- screenshots/logs/URLs when relevant
- known limitations
- unresolved blockers
- exact recommended next action

Never claim a test, visual check, device check or deployment was performed unless it actually was.

## Git discipline

- One active write executor per task branch.
- Prefer task branches/worktrees over direct edits on main.
- Preserve unrelated user changes.
- No destructive reset/force push unless the active task explicitly authorizes it.
- PRs should stay reviewable; unrelated work belongs in another task.
- Executors do not self-approve their own architectural decisions. ChatGPT performs independent review.

## Security

Never move secrets into prompts, task files, Git history, screenshots or logs. Do not weaken auth, TLS, signing, payment, VPN routing, data-protection, CI security or production controls merely to make tests pass. Production mutations require an explicit production gate in the active task.

## Completion

An executor finishing is not task completion. The task is complete only when the state reaches the terminal state required by `.ai/TASK.md`, with evidence accepted by ChatGPT.
