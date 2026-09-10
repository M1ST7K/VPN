#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PAYLOAD_DIR="$ROOT/bootstrap/payload"
OUT="$ROOT/bootstrap/hotfox-bootstrap.tar.xz"

if [[ ! -d "$PAYLOAD_DIR" ]]; then
  echo "Missing bootstrap payload directory: $PAYLOAD_DIR" >&2
  exit 2
fi

cat "$PAYLOAD_DIR"/part-*.b64 | base64 -d > "$OUT"
EXPECTED="c9dd4656985b73084e3ff9bf16459c93d55e0a2cfb0ce8dd083ac1d1dea85260"
ACTUAL="$(sha256sum "$OUT" | awk '{print $1}')"
if [[ "$ACTUAL" != "$EXPECTED" ]]; then
  echo "Bootstrap payload SHA-256 mismatch" >&2
  echo "expected: $EXPECTED" >&2
  echo "actual:   $ACTUAL" >&2
  exit 3
fi

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP" "$OUT"' EXIT

tar -xJf "$OUT" -C "$TMP"

# Restore specification, reference and the complete HotFox text overlay.
mkdir -p "$ROOT/docs"
xz -dc "$TMP/master_prompt.txt.xz" > "$ROOT/docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt"
cp "$TMP/HOTFOX_UI_REFERENCE.jpeg" "$ROOT/docs/HOTFOX_UI_REFERENCE.jpeg"
cp -R "$TMP/docs/." "$ROOT/docs/"

# Obtain the exact upstream base if the Android project has not yet been reconstructed.
if [[ ! -d "$ROOT/V2rayNG/.git" && ! -f "$ROOT/V2rayNG/settings.gradle.kts" ]]; then
  echo "Fetching v2rayNG 2.2.6 upstream source..."
  git clone --depth 1 --branch 2.2.6 https://github.com/2dust/v2rayNG.git "$TMP/upstream"
  mkdir -p "$ROOT/V2rayNG"
  rsync -a --exclude='.git/' "$TMP/upstream/" "$ROOT/V2rayNG/"
fi

# Apply the prepared HotFox 2.1.0 text/source overlay over the upstream Android project.
tar -xJf "$TMP/hotfox_overlay_text_2_1_0.tar.xz" -C "$ROOT/V2rayNG"

# Download verified Xray AAR. Do not commit this large third-party binary.
mkdir -p "$ROOT/V2rayNG/app/libs"
AAR="$ROOT/V2rayNG/app/libs/libv2ray.aar"
AAR_URL="https://github.com/2dust/AndroidLibXrayLite/releases/download/v26.6.27/libv2ray.aar"
AAR_SHA="7846eb7f663d1d8ae931034faa7a56cccc82d618c2d029198e6e91a77fd8de1e"
if [[ ! -f "$AAR" ]]; then
  curl -fL --retry 4 --retry-delay 2 "$AAR_URL" -o "$AAR"
fi
AAR_ACTUAL="$(sha256sum "$AAR" | awk '{print $1}')"
if [[ "$AAR_ACTUAL" != "$AAR_SHA" ]]; then
  echo "libv2ray.aar SHA-256 mismatch" >&2
  rm -f "$AAR"
  exit 4
fi

cat <<'EOF'
HotFox bootstrap reconstructed.
Next steps for Cursor:
  1. Read AGENTS.md.
  2. Read docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt completely.
  3. Inspect docs/HOTFOX_UI_REFERENCE.jpeg.
  4. Inspect Git status and the reconstructed V2rayNG project.
  5. Build/test/fix and execute the specification; do not stop at planning.
EOF
