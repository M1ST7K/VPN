# HotFox AI Review Policy — Checkpoint Model

## Why this exists

HotFox uses GitHub CI for mechanical verification and GPT-5.6 Sol for expensive, high-value senior engineering review.

The reviewer is **not** a per-commit lint bot.

This policy reduces repeated model spend and context noise while keeping independent review on meaningful architecture/security checkpoints.

## Ordinary push

Normal development commits should trigger normal CI/builds but should **not** call OpenAI.

Examples:
- implementation commit;
- focused test commit;
- CI fix;
- XML/string polish;
- follow-up cleanup inside one coherent task block.

The trusted reviewer workflow may start a small job on PR synchronization, but the trusted script exits before the OpenAI API call unless a checkpoint is explicitly requested.

## Requesting a checkpoint

After a coherent task/fix block is complete and applicable automated gates are green, the final commit message must contain:

`[hotfox-review]`

Example:

`fix: make stop finalization generation-safe [hotfox-review]`

This requests one GPT-5.6 Sol checkpoint review of the current PR head.

`workflow_dispatch` remains an explicit forced review mechanism for maintainers when needed.

## What the reviewer receives

Trusted context from `main`:
- `docs/AI_REVIEW_GUARDRAILS.md`;
- `docs/AI_REVIEW_CURRENT_PHASE.md`;
- trusted reviewer system instructions;
- host CI evidence fetched by the trusted script from GitHub Actions for the exact reviewed SHA (`candidate-evidence.txt`, required bootstrap jobs, workflow conclusion).

Untrusted review material:
- PR metadata/body;
- changed-file list;
- current diff;
- previous trusted review comment;
- feature-branch documentation that merely *claims* CI passed.

The full master roadmap is intentionally **not** sent on every review.

## Diff strategy

Default: incremental diff since the previous trusted checkpoint.

Periodic full-PR review: first review and every fourth round, plus fallback when history is non-linear.

Large diffs are trimmed with release-sensitive paths prioritized (VPN/Xray/HEV/TUN/DNS/routing/subscription/billing/security/CI).

## Verdict contract

`VERDICT: CHANGES_REQUIRED`
- only for substantiated P0/P1 defects;
- triggers one `@cursor` fix handoff.

`VERDICT: APPROVED`
- no substantiated P0/P1 in the review scope;
- P2 suggestions may remain;
- no Cursor handoff.

P2-only feedback must not create an autonomous loop.

## Cursor behavior after CHANGES_REQUIRED

Cursor should:

1. read `AGENTS.md`;
2. read `docs/CURRENT_PHASE.md` and the linked phase spec;
3. fix all substantiated P0/P1 findings;
4. run focused + applicable full automated gates;
5. use ordinary commits while iterating and fixing CI;
6. **not** request GPT review for every intermediate commit;
7. once the whole fix block is green, make one final commit containing `[hotfox-review]`.

## Safety cap

The trusted script keeps a finite checkpoint-round cap (currently 40) **per engineering phase**.

The current phase is the explicit trusted identifier in `docs/AI_REVIEW_PHASE_ID`, supplied to the reviewer as workflow env `HOTFOX_REVIEW_PHASE`. It must match the trusted active milestone in `docs/AI_REVIEW_CURRENT_PHASE.md`. Each review marker records `phase=<id>`. PR-head content cannot choose a different phase or reset the cap.

The cap counts only completed reviews whose marker phase equals that identifier, and only when the comment has a canonical verdict line (`VERDICT: APPROVED` or `VERDICT: CHANGES_REQUIRED` as the full line). Historical approvals from other phases, legacy markers without a phase id, automation-paused CAP comments, and `VERDICT:` text that is not a canonical verdict line do not consume or reset the current-phase window.

An `APPROVED` verdict is not a phase boundary. The counter resets only when the trusted phase identifier itself changes.

If the current-phase cap is reached, automation pauses rather than spending indefinitely. A non-converging review loop in the current phase should be inspected architecturally before raising the cap.

## Physical-device boundary

Neither GPT review nor emulator smoke can prove physical Android VPN E2E.

Reviewer may verify that code/evidence is plausible and truthful, but release acceptance still requires the physical-device gate defined by the current phase.

## Model policy

Current senior reviewer model is pinned to `gpt-5.6-sol` with high reasoning.

Do not switch to an automatic/unknown model selector inside the trusted review workflow without explicit owner approval.

## Cost principle

Spend model tokens on questions CI cannot answer well:
- lifecycle/races;
- security;
- VPN datapath correctness;
- DNS/IPv6 fail-closed behavior;
- protocol semantics;
- billing/entitlement security;
- architecture regressions;
- truthful UI state.

Let CI handle:
- compile;
- unit tests;
- lint;
- static verifiers;
- secret scans;
- APK builds;
- emulator install/navigation smoke.
