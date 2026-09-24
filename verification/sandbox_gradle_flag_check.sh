#!/usr/bin/env bash
# Proves HOTFOX_SANDBOX_COMMERCE can configure debug while release tasks are rejected.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="${1:-$ROOT/V2rayNG}"
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

[[ -x "$PROJECT/gradlew" ]] || fail "gradlew missing at $PROJECT"
cd "$PROJECT"
chmod +x gradlew

HOTFOX_SANDBOX_COMMERCE=true ./gradlew --no-daemon --stacktrace :app:generatePlaystoreDebugBuildConfig
debug_buildconfig="$(find app/build -name BuildConfig.java | grep '/playstore/debug/' | head -n 1 || true)"
[[ -n "$debug_buildconfig" ]] || fail "debug BuildConfig not generated"
grep -q 'HOTFOX_SANDBOX_COMMERCE = true' "$debug_buildconfig" \
  || fail "sandbox-enabled debug BuildConfig must set HOTFOX_SANDBOX_COMMERCE true"

set +e
release_output="$(HOTFOX_SANDBOX_COMMERCE=true ./gradlew --no-daemon --stacktrace :app:generatePlaystoreReleaseBuildConfig 2>&1)"
release_code=$?
set -e
printf '%s\n' "$release_output"
[[ "$release_code" -ne 0 ]] || fail "sandbox-enabled release configuration must fail"
printf '%s\n' "$release_output" | grep -q 'HOTFOX_SANDBOX_COMMERCE cannot be enabled for release builds' \
  || fail "sandbox-enabled release must fail with the documented GradleException"

printf 'PASS sandbox Gradle flag check\n'
