#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROJECT="$ROOT/V2rayNG"
fail() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
sha_check() { local f="$1" e="$2" a; [[ -f "$f" ]] || fail "missing $f"; a="$(sha256sum "$f" | awk '{print $1}')"; [[ "$a" == "$e" ]] || fail "SHA mismatch for $f: got $a expected $e"; }

[[ -f "$PROJECT/settings.gradle.kts" ]] || fail "V2rayNG/settings.gradle.kts missing"
[[ -f "$PROJECT/gradlew" ]] || fail "Gradle wrapper missing"
[[ -f "$PROJECT/app/build.gradle.kts" ]] || fail "app/build.gradle.kts missing"

sha_check "$ROOT/docs/CURSOR_PRO_ULTRA_MASTER_PROMPT_HOTFOX_PROXY_2.2.0.txt" "4acc5869c3ee00d2f1605bb1bae916548bc56b7d90f95bbc71d03132fea4a25a"
sha_check "$ROOT/docs/HOTFOX_UI_REFERENCE.jpeg" "d023044443a37d39cf39ab8df6ee8d08c0ae0ec7d95e1059f6f7ced52492dfe1"
sha_check "$PROJECT/settings.gradle.kts" "5fa5f0f2b831c187ce829b72e8b89d3738f3e10c50eea10143568bd27f902d92"
sha_check "$PROJECT/app/build.gradle.kts" "63f07114bc6162d83b4f1a5d38e2168d09f312381764b5b56b0e9b98eeb482c8"
sha_check "$PROJECT/app/src/main/java/com/v2ray/ang/ui/MainActivity.kt" "6c5c6c52570e4869d05040283ff4f14763d5390604e3e1080dd3c268d8cb6399"
sha_check "$PROJECT/app/src/main/res/layout/activity_main.xml" "10d082c6c9fc419cadc58117d9ecff696443ee112a0135df1215911698f31452"
sha_check "$PROJECT/app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt" "9801660be9fde2235ed49b3a2c1b7645b991118caadef1d62b7b9df8d6b9b5e9"
sha_check "$PROJECT/app/src/main/java/com/v2ray/ang/service/TProxyService.kt" "c6e8b7d0cf9a8dbe8a62bb56cf3d83f8abf0e15d0eeefe7013e2458a1df694a0"
sha_check "$PROJECT/app/src/main/java/com/v2ray/ang/handler/SettingsManager.kt" "3cd567cf425a01a361b6ab667cfb8b8486f4656cd3c82816add47cf08cf9e191"
sha_check "$PROJECT/app/libs/libv2ray.aar" "7846eb7f663d1d8ae931034faa7a56cccc82d618c2d029198e6e91a77fd8de1e"

for abi in arm64-v8a armeabi-v7a x86 x86_64; do
  for lib in libhev-socks5-tunnel.so libhevsockstun.so; do
    f="$PROJECT/app/libs/$abi/$lib"
    [[ -s "$f" ]] || fail "missing/empty native library $f"
    if command -v readelf >/dev/null; then
      readelf -h "$f" >/dev/null || fail "$f is not a readable ELF file"
      while read -r align; do
        [[ -z "$align" ]] && continue
        (( align >= 0x4000 )) || fail "$f has LOAD alignment $align (<0x4000)"
      done < <(readelf -lW "$f" | awk '$1=="LOAD" {print $NF}')
    fi
  done
done

grep -q 'applicationId = "com.hotfox.vpn"' "$PROJECT/app/build.gradle.kts" || fail "HotFox applicationId missing"
grep -q 'versionName = "2.1.0"' "$PROJECT/app/build.gradle.kts" || fail "baseline versionName 2.1.0 missing"
grep -q 'System.loadLibrary("hev-socks5-tunnel")' "$PROJECT/app/src/main/java/com/v2ray/ang/service/TProxyService.kt" || fail "HEV JNI load path missing"
grep -q 'HotFox' "$PROJECT/app/src/main/res/layout/activity_main.xml" || fail "HotFox UI marker missing"

if grep -R -I -n -E "https://nox\\.hotto-fox\\.st/|vless://[^[:space:]\"]{20,}" --exclude-dir=build --exclude-dir=test --exclude-dir=androidTest --exclude="*.md" --exclude="*.txt" "$PROJECT" >/tmp/hotfox-secret-scan.txt 2>/dev/null; then
  cat /tmp/hotfox-secret-scan.txt >&2
  fail "possible personal subscription/VLESS secret found in source tree"
fi
rm -f /tmp/hotfox-secret-scan.txt

printf 'PASS reconstruction verification\n'
