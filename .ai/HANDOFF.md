# Executor handoff

TASK_ID: hotfox-final-release-validation-20260924
EXECUTOR: Cursor background agent + ChatGPT/Mac validation
STATUS: STARTING
BRANCH: cursor/hotfox-final-release-validation-20260924
COMMIT_SHA: pending

## Summary
Final release validation gate opened after engineering/UI integration and green merge-main CI.

## Preflight facts already established
- Android SDK and adb exist on authorized Mac.
- No Android device was attached at preflight.
- Emulator binary exists but no AVD was listed at preflight.
- HOTFOX_KEYSTORE_PATH / PASSWORD / ALIAS / KEY_PASSWORD were unset in the Mac shell.
- HOTFOX_TEST_SUBSCRIPTION_URL was unset in the Mac shell.
- Repository already contains a secret-driven GitHub VPN E2E workflow.
- No secret values were read or printed.

## Verification actually performed
- main merge CI was confirmed green for payload integrity, reconstruction, static check, debug build, unit tests, lint, unsigned release compile and publish.
- historical UI rules 23–37 were archived from automatic application.

## Evidence
See current GitHub Actions for main and the new release-gate evidence directory once populated.

## Known risks / limitations
Physical device, signed RC material and dedicated test subscription availability are not yet proven.

## Recommended next action
Run the validation execution plan and update this handoff with factual evidence.
