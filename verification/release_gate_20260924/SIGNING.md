# Signing / release candidate — SIGNED_RC = BLOCKED

## Contract (inspected in reconstructed `V2rayNG/app/build.gradle.kts`)

- `signingConfigs.hotfoxRelease` is created only when all of
  `HOTFOX_KEYSTORE_PATH`, `HOTFOX_KEYSTORE_PASSWORD`, `HOTFOX_KEY_ALIAS`,
  `HOTFOX_KEY_PASSWORD` are non-blank (v1–v4 signing enabled).
- `HOTFOX_REQUIRE_RELEASE_SIGNING=true` with incomplete env fails the build.
- `release` uses `signingConfigs.findByName("hotfoxRelease")`; sandbox commerce is
  forbidden for release builds.

## Availability checks (values never read or printed)

| Location | Result |
|---|---|
| Cursor cloud VM env | all four `HOTFOX_KEYSTORE_*`/`HOTFOX_KEY_*` **UNSET**; `HOTFOX_REQUIRE_RELEASE_SIGNING` UNSET |
| Authorized Mac (owner preflight, `.ai/HANDOFF.md`) | all four UNSET |
| GitHub Actions workflows | no workflow passes `HOTFOX_KEYSTORE_*` / `HOTFOX_KEY_*`; there is **no CI signing path** at all. Secret list itself not readable by the agent token (HTTP 403). |

## Fail-closed check actually executed

`HOTFOX_REQUIRE_RELEASE_SIGNING=true ./gradlew :app:help` on this VM → exit 1 with
`HOTFOX_REQUIRE_RELEASE_SIGNING is set but keystore env is incomplete`. **PASS**
(required signing cannot silently produce an unsigned/debug-signed release).

## Verdict

- No authorized signing identity is available; none was invented or generated.
- `SIGNED_RC = BLOCKED` — owner must provide the existing production keystore
  through a secure channel (local env on the authorized Mac, or new GitHub
  secrets + a signing job), then build `:app:assemblePlaystoreRelease` with
  `HOTFOX_REQUIRE_RELEASE_SIGNING=true`, verify with `apksigner verify --print-certs`,
  and record APK SHA-256 + certificate SHA-256.
- Unsigned release compilation is covered by CI (`Verify unsigned release compilation`).
