#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PAYLOAD_DIR="$ROOT/bootstrap/payload"
EXPECTED_SHA256="c9dd4656985b73084e3ff9bf16459c93d55e0a2cfb0ce8dd083ac1d1dea85260"
EXPECTED_ENCODED_BYTES=387416
EXPECTED_CHUNK_COUNT=40

fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
[[ -d "$PAYLOAD_DIR" ]] || fail "payload directory is missing: $PAYLOAD_DIR"

mapfile -t CHUNKS < <(find "$PAYLOAD_DIR" -maxdepth 1 -type f -name 'part-*.b64' -printf '%f\n' | LC_ALL=C sort)
[[ ${#CHUNKS[@]} -eq $EXPECTED_CHUNK_COUNT ]] || fail "expected $EXPECTED_CHUNK_COUNT payload files, found ${#CHUNKS[@]}"

expected_names=()
for n in $(seq 0 21); do printf -v nn "%03d" "$n"; expected_names+=("part-$nn.b64"); done
for n in $(seq 0 15); do printf -v nn "%02d" "$n"; expected_names+=("part-022-$nn.b64"); done
expected_names+=("part-023.b64" "part-024.b64")
for i in "${!expected_names[@]}"; do
  [[ "${CHUNKS[$i]}" == "${expected_names[$i]}" ]] || fail "unexpected payload file/order at index $i: got ${CHUNKS[$i]}, expected ${expected_names[$i]}"
done

encoded_bytes=0
for f in "${CHUNKS[@]}"; do
  bytes=$(wc -c < "$PAYLOAD_DIR/$f")
  [[ $bytes -gt 0 ]] || fail "$f is empty"
  encoded_bytes=$((encoded_bytes + bytes))
done
[[ $encoded_bytes -eq $EXPECTED_ENCODED_BYTES ]] || fail "encoded payload size mismatch: got $encoded_bytes, expected $EXPECTED_ENCODED_BYTES"

tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT
for f in "${CHUNKS[@]}"; do cat "$PAYLOAD_DIR/$f"; done | base64 --decode > "$tmp" || fail "base64 decode failed"
actual="$(sha256sum "$tmp" | awk '{print $1}')"
[[ "$actual" == "$EXPECTED_SHA256" ]] || fail "payload SHA-256 mismatch: got $actual expected $EXPECTED_SHA256"

tar -tJf "$tmp" >/dev/null || fail "decoded payload is not a valid xz tar archive"
printf 'PASS payload SHA-256 %s (%d encoded bytes, %d chunks)\n' "$actual" "$encoded_bytes" "${#CHUNKS[@]}"
