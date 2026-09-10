#!/usr/bin/env bash
# Idempotent Cloud Agent bootstrap for HotFox Proxy (Android 17 / NDK 29).
# Does not download the Android Emulator (that zip has flaked CI).
# Does not run unit tests, lint, or release signing.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

NDK_VERSION="${NDK_VERSION:-29.0.14206865}"
export ANDROID_HOME="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"

log() { printf '[hotfox-env] %s\n' "$*"; }

install_apt() {
  if command -v sudo >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
      openjdk-17-jdk python3 rsync xz-utils unzip wget curl git ca-certificates binutils
  fi
}

ensure_java() {
  if [[ -x "$JAVA_HOME/bin/java" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
    return
  fi
  JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
}

sdkmanager_bin() {
  local candidate
  for candidate in \
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
    "$ANDROID_HOME/cmdline-tools/bin/sdkmanager"; do
    if [[ -x "$candidate" ]]; then
      printf '%s' "$candidate"
      return 0
    fi
  done
  return 1
}

ensure_cmdline_tools() {
  if sdkmanager_bin >/dev/null; then
    return
  fi
  mkdir -p "$ANDROID_HOME"
  local zip="$ANDROID_HOME/cmdline-tools.zip"
  log "installing Android cmdline-tools"
  curl -fsSL -o "$zip" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  unzip -q -o "$zip" -d "$ANDROID_HOME/cmdline-tools"
  rm -f "$zip"
  if [[ -d "$ANDROID_HOME/cmdline-tools/cmdline-tools" && ! -d "$ANDROID_HOME/cmdline-tools/latest" ]]; then
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  fi
}

ensure_android_sdk() {
  mkdir -p "$ANDROID_HOME"
  ensure_cmdline_tools
  local sdk
  sdk="$(sdkmanager_bin)"
  export PATH="$(dirname "$sdk"):$PATH"
  # Do not install emulator here.
  # compileSdk=37: platforms;android-37 → platforms/android-37
  # Keep build-tools;37.0.0 separate from the platform package.
  bash "$ROOT/.github/scripts/install_hotfox_android_sdk.sh"
}

bootstrap_and_warm() {
  if [[ ! -f "$ROOT/V2rayNG/settings.gradle.kts" ]]; then
    log "reconstructing V2rayNG"
    bash "$ROOT/bootstrap/bootstrap_source.sh"
  else
    log "applying overlay onto existing V2rayNG"
    rsync -a "$ROOT/bootstrap/hotfox_2_2_0/" "$ROOT/V2rayNG/"
  fi
  if [[ -x "$ROOT/V2rayNG/gradlew" ]]; then
    log "warming Gradle (playstore debug compile)"
    (
      cd "$ROOT/V2rayNG"
      chmod +x gradlew
      ./gradlew --no-daemon --stacktrace :app:assemblePlaystoreDebug
    )
  fi
}

install_apt
ensure_java
ensure_android_sdk
bootstrap_and_warm
log "install complete ANDROID_HOME=$ANDROID_HOME JAVA_HOME=$JAVA_HOME"
