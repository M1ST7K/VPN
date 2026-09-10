#!/usr/bin/env bash
# Install Android SDK packages required to compile HotFox (compileSdk = 37).
#
# Platform package vs build-tools:
#   - compileSdk 37 uses the platform directory platforms/android-37
#   - build-tools stay on the versioned package build-tools;37.0.0
#
# Android 17 currently publishes the platform as platforms;android-37.0
# (directory platforms/android-37.0) in addition to, or instead of, the
# integer API id platforms;android-37. Install whichever exists, then make
# sure AGP can resolve the integer compileSdk directory.
set -euo pipefail

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${SDK_ROOT}" ]]; then
  echo "ANDROID_SDK_ROOT or ANDROID_HOME is required" >&2
  exit 1
fi
NDK_VERSION="${NDK_VERSION:?NDK_VERSION is required}"

if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "sdkmanager not on PATH" >&2
  exit 1
fi

yes | sdkmanager --licenses >/dev/null 2>&1 || true

install_pkg() {
  local pkg="$1"
  if sdkmanager --channel=3 "${pkg}"; then
    echo "installed ${pkg}"
    return 0
  fi
  echo "sdkmanager could not install ${pkg}" >&2
  return 1
}

install_pkg "build-tools;37.0.0"
install_pkg "platform-tools"
install_pkg "ndk;${NDK_VERSION}"

platform_ok=0
if install_pkg "platforms;android-37"; then
  platform_ok=1
fi
if install_pkg "platforms;android-37.0"; then
  platform_ok=1
fi
if [[ "${platform_ok}" -ne 1 ]]; then
  echo "failed to install an Android SDK platform for API 37" >&2
  ls -la "${SDK_ROOT}/platforms" >&2 || true
  exit 1
fi

if [[ ! -d "${SDK_ROOT}/platforms/android-37" ]]; then
  if [[ -d "${SDK_ROOT}/platforms/android-37.0" ]]; then
    ln -sfn android-37.0 "${SDK_ROOT}/platforms/android-37"
  fi
fi

test -d "${SDK_ROOT}/platforms/android-37"
test -x "${SDK_ROOT}/ndk/${NDK_VERSION}/ndk-build"

if [[ -n "${GITHUB_ENV:-}" ]]; then
  echo "NDK_HOME=${SDK_ROOT}/ndk/${NDK_VERSION}" >> "${GITHUB_ENV}"
fi
