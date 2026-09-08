#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

UPSTREAM_TAG="2.2.6"
UPSTREAM_COMMIT="15b4fff8e45da9bc0acaa5cc1d80a1d3531e8712"
NDK_VERSION="29.0.14206865"
PAYLOAD_SHA256="c9dd4656985b73084e3ff9bf16459c93d55e0a2cfb0ce8dd083ac1d1dea85260"
OVERLAY_SHA256="fe5973326591083839fae72993840dbfc5b897bfaf45477e547c97604a0c80ed"
MASTER_XZ_SHA256="d777a3025853722946324201e8145197e3abddfcd674ff403f07bdec55852a77"
MASTER_TXT_SHA256="4acc5869c3ee00d2f1605bb1bae916548bc56b7d90f95bbc71d03132fea4a25a"
UI_REFERENCE_SHA256="d023044443a37d39cf39ab8df6ee8d08c0ae0ec7d95e1059f6f7ced52492dfe1"
AAR_SHA256="7846eb7f663d1d8ae931034faa7a56cccc82d618c2d029198e6e91a77fd8de1e"
AAR_URL="https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.6.27/libv2ray.aar"

log() { printf '\n[HotFox bootstrap] %s\n' "$*"; }
fail() { printf '\n[HotFox bootstrap] ERROR: %s\n' "$*" >&2; exit 1; }
sha_check() {
  local file="$1" expected="$2" label="$3" actual
  [[ -f "$file" ]] || fail "$label missing: $file"
  actual="$(sha256sum "$file" | awk '{print $1}')"
  [[ "$actual" == "$expected" ]] || fail "$label SHA-256 mismatch: got $actual expected $expected"
}

command -v bash >/dev/null || fail "bash is required"
command -v base64 >/dev/null || fail "base64 is required"
command -v sha256sum >/dev/null || fail "sha256sum is required"
command -v tar >/dev/null || fail "tar is required"
command -v xz >/dev/null || fail "xz is required"
command -v rsync >/dev/null || fail "rsync is required"

log "verifying embedded bootstrap payload"
bash "$ROOT/bootstrap/verify_payload.sh"

PAYLOAD_DIR="$ROOT/bootstrap/payload"
ARCHIVE="$ROOT/bootstrap/.hotfox-bootstrap.tar.xz"
mapfile -t CHUNKS < <(find "$PAYLOAD_DIR" -maxdepth 1 -type f -name 'part-*.b64' -printf '%f\n' | LC_ALL=C sort)
trap 'rm -f "$ARCHIVE"' EXIT
for f in "${CHUNKS[@]}"; do cat "$PAYLOAD_DIR/$f"; done | base64 --decode > "$ARCHIVE"
sha_check "$ARCHIVE" "$PAYLOAD_SHA256" "bootstrap payload"

TMP="$(mktemp -d)"
cleanup() { rm -rf "$TMP"; rm -f "$ARCHIVE"; }
trap cleanup EXIT INT TERM

tar -xJf "$ARCHIVE" -C "$TMP"
sha_check "$TMP/hotfox_overlay_text_2_1_0.tar.xz" "$OVERLAY_SHA256" "HotFox overlay"
sha_check "$TMP/master_prompt.txt.xz" "$MASTER_XZ_SHA256" "master prompt archive"
sha_check "$TMP/HOTFOX_UI_REFERENCE.jpeg" "$UI_REFERENCE_SHA256" "UI reference"

log "restoring normative specification and project documentation"
mkdir -p "$ROOT/docs"
xz -dc "$TMP/master_prompt.txt.xz" > "$ROOT/docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt"
sha_check "$ROOT/docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt" "$MASTER_TXT_SHA256" "master prompt"
cp "$TMP/HOTFOX_UI_REFERENCE.jpeg" "$ROOT/docs/HOTFOX_UI_REFERENCE.jpeg"
cp "$TMP/PAYLOAD_MANIFEST.txt" "$ROOT/docs/BOOTSTRAP_PAYLOAD_MANIFEST.txt"
if [[ -d "$TMP/docs" ]]; then
  cp -a "$TMP/docs/." "$ROOT/docs/"
fi

UPSTREAM=""
if [[ -n "${HOTFOX_UPSTREAM_DIR:-}" ]]; then
  UPSTREAM="$(cd "$HOTFOX_UPSTREAM_DIR" && pwd)"
  [[ -d "$UPSTREAM/V2rayNG" ]] || fail "HOTFOX_UPSTREAM_DIR must contain V2rayNG/: $UPSTREAM"
  [[ -d "$UPSTREAM/hev-socks5-tunnel" ]] || fail "HOTFOX_UPSTREAM_DIR must contain hev-socks5-tunnel/: $UPSTREAM"
  log "using supplied upstream tree: $UPSTREAM"
else
  command -v git >/dev/null || fail "git is required"
  UPSTREAM="$TMP/upstream"
  log "cloning exact upstream v2rayNG tag $UPSTREAM_TAG with submodules"
  git clone --depth 1 --branch "$UPSTREAM_TAG" --recurse-submodules --shallow-submodules \
    https://github.com/2dust/v2rayNG.git "$UPSTREAM"
  actual_commit="$(git -C "$UPSTREAM" rev-parse HEAD)"
  [[ "$actual_commit" == "$UPSTREAM_COMMIT" ]] || fail "upstream commit mismatch: got $actual_commit expected $UPSTREAM_COMMIT"
  git -C "$UPSTREAM" submodule update --init --recursive --depth 1
fi

if [[ ! -f "$ROOT/V2rayNG/settings.gradle.kts" ]]; then
  log "installing pristine Android project from upstream/V2rayNG"
  mkdir -p "$ROOT/V2rayNG"
  rsync -a "$UPSTREAM/V2rayNG/" "$ROOT/V2rayNG/"
else
  log "V2rayNG project already exists; preserving existing tree and applying overlay in place"
fi

log "applying HotFox Proxy 2.1.0 source overlay"
tar -xJf "$TMP/hotfox_overlay_text_2_1_0.tar.xz" -C "$ROOT/V2rayNG"

# Text overlay cannot carry binary branding bitmaps. Install the vector HotFox
# mark referenced by nav_header.xml and activity_renewal.xml after the overlay.
mkdir -p "$ROOT/V2rayNG/app/src/main/res/drawable"
cp "$ROOT/bootstrap/assets/drawable/hotfox_logo.xml" \
  "$ROOT/V2rayNG/app/src/main/res/drawable/hotfox_logo.xml"
[[ -s "$ROOT/V2rayNG/app/src/main/res/drawable/hotfox_logo.xml" ]] || fail "hotfox_logo drawable was not installed"

if [[ ! -f "$ROOT/V2rayNG/local.properties" && -n "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]]; then
  printf 'sdk.dir=%s\n' "${ANDROID_HOME:-$ANDROID_SDK_ROOT}" > "$ROOT/V2rayNG/local.properties"
fi

mkdir -p "$ROOT/V2rayNG/app/libs"

if [[ -n "${HOTFOX_PREBUILT_HEV_DIR:-}" ]]; then
  log "using supplied prebuilt HEV libraries"
  for abi in arm64-v8a armeabi-v7a x86 x86_64; do
    mkdir -p "$ROOT/V2rayNG/app/libs/$abi"
    for lib in libhev-socks5-tunnel.so libhevsockstun.so; do
      src="$HOTFOX_PREBUILT_HEV_DIR/$abi/$lib"
      [[ -s "$src" ]] || fail "prebuilt HEV library missing: $src"
      cp "$src" "$ROOT/V2rayNG/app/libs/$abi/$lib"
    done
  done
else
  NDK_DIR="${NDK_HOME:-${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-}}}"
  if [[ -z "$NDK_DIR" && -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME/ndk/$NDK_VERSION" ]]; then
    NDK_DIR="$ANDROID_HOME/ndk/$NDK_VERSION"
  fi
  [[ -n "$NDK_DIR" && -x "$NDK_DIR/ndk-build" ]] || fail "Android NDK $NDK_VERSION is required. Set NDK_HOME or install an NDK at ANDROID_HOME/ndk/$NDK_VERSION"

  log "building HEV tun2socks for arm64-v8a, armeabi-v7a, x86 and x86_64"
  HEV_WORK="$TMP/hev-build"
  mkdir -p "$HEV_WORK"
  cp "$TMP/compile-hevtun.sh" "$HEV_WORK/compile-hevtun.sh"
  cp -a "$UPSTREAM/hev-socks5-tunnel" "$HEV_WORK/hev-socks5-tunnel"
  chmod +x "$HEV_WORK/compile-hevtun.sh"
  (cd "$HEV_WORK" && NDK_HOME="$NDK_DIR" bash ./compile-hevtun.sh)

  for abi in arm64-v8a armeabi-v7a x86 x86_64; do
    mkdir -p "$ROOT/V2rayNG/app/libs/$abi"
    for lib in libhev-socks5-tunnel.so libhevsockstun.so; do
      src="$HEV_WORK/libs/$abi/$lib"
      [[ -s "$src" ]] || fail "HEV build did not produce $src"
      cp "$src" "$ROOT/V2rayNG/app/libs/$abi/$lib"
    done
  done
fi

AAR="$ROOT/V2rayNG/app/libs/libv2ray.aar"
if [[ -n "${HOTFOX_LIBV2RAY_AAR:-}" ]]; then
  log "using supplied libv2ray.aar"
  cp "$HOTFOX_LIBV2RAY_AAR" "$AAR"
else
  command -v curl >/dev/null || fail "curl is required"
  log "downloading pinned libv2ray.aar"
  curl --fail --location --retry 5 --retry-all-errors --connect-timeout 20 "$AAR_URL" -o "$AAR.tmp"
  mv "$AAR.tmp" "$AAR"
fi
sha_check "$AAR" "$AAR_SHA256" "libv2ray.aar"

log "running reconstruction verification"
bash "$ROOT/bootstrap/verify_reconstruction.sh"

if [[ -d "$ROOT/bootstrap/hotfox_2_2_0" ]]; then
  log "applying HotFox Proxy 2.2.0 overlay"
  rsync -a "$ROOT/bootstrap/hotfox_2_2_0/" "$ROOT/V2rayNG/"
  [[ -f "$ROOT/V2rayNG/app/src/main/java/com/v2ray/ang/vpn/VpnSessionCoordinator.kt" ]] \
    || fail "2.2.0 overlay did not install VpnSessionCoordinator"
  grep -q 'versionName = "2.2.0"' "$ROOT/V2rayNG/app/build.gradle.kts" \
    || fail "2.2.0 overlay did not bump versionName"
fi

cat <<'MSG'

HotFox bootstrap completed successfully.

Cursor must now:
  1. Read AGENTS.md completely.
  2. Read docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt completely.
  3. Inspect docs/HOTFOX_UI_REFERENCE.jpeg.
  4. Work in V2rayNG/ and execute the specification instead of merely planning.
  5. Build, test, fix and repeat until the release gates in AGENTS.md are satisfied.
MSG
