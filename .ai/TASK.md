# Active task

STATUS: ACTIVE
TASK_ID: hotfox-final-release-validation-20260924
OWNER: Maxim
ORCHESTRATOR: ChatGPT
PRIMARY_EXECUTOR: Cursor background agent + authorized Mac validation
BASE_BRANCH: main
TASK_BRANCH: cursor/hotfox-final-release-validation-20260924
TARGET_END_STATE: Factual final release verdict from the canonical validation gate

## Objective
Execute `docs/HOTFOX_FINAL_RELEASE_VALIDATION_EXECUTION_20260924.md` against one exact release-candidate SHA/artifact.

## Scope
- repo/CI preflight and candidate freeze;
- release signing availability/provenance check without exposing secrets;
- existing secret-driven emulator/runtime VPN E2E;
- local Mac emulator setup/validation where feasible;
- physical R1–R8 when an authorized Android device is actually available;
- fix/rebuild/rerun for blocking defects;
- evidence ledger and final verdict.

## Non-goals
- no UI redesign;
- no new product features;
- no fake VPN/payment/protection evidence;
- no new production signing identity without explicit owner authorization.

## Acceptance criteria
- exact candidate SHA/artifact recorded;
- every gate labeled PASS/FAIL/BLOCKED/NOT EXECUTED;
- all available runtime/emulator validation executed;
- physical R1–R8 executed if a device is available;
- no secret exposure;
- RELEASE READY only if required signed-RC/runtime/physical evidence passes.

## Required verification
Follow `docs/phases/final-release-validation-gate.md` and `docs/HOTFOX_FINAL_RELEASE_VALIDATION_EXECUTION_20260924.md`.

## Evidence required
`verification/release_gate_20260924/`

## Production permission
PRODUCTION_ALLOWED: false

## Executor instructions
Read repository rules and canonical gate first. Execute everything available now. Missing external prerequisites are blockers, not reasons to stop independent work.
