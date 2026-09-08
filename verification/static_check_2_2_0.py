#!/usr/bin/env python3
"""Static acceptance checks for HotFox Proxy 2.2.0 reconstructed source."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROJECT = ROOT / "V2rayNG"
failures: list[str] = []


def fail(message: str) -> None:
    failures.append(message)


def read(rel: str) -> str:
    path = PROJECT / rel
    if not path.is_file():
        fail(f"missing file: {rel}")
        return ""
    return path.read_text(encoding="utf-8", errors="replace")


def must_contain(rel: str, needle: str, label: str | None = None) -> None:
    text = read(rel)
    if text and needle not in text:
        fail(f"{label or needle} not found in {rel}")


def main() -> int:
    gradle = read("app/build.gradle.kts")
    if gradle:
        if 'applicationId = "com.hotfox.vpn"' not in gradle:
            fail("applicationId is not com.hotfox.vpn")
        if "minSdk = 24" not in gradle:
            fail("minSdk is not 24")
        if 'versionName = "2.2.0"' not in gradle:
            fail("versionName is not 2.2.0")
        if "versionCode = 22000" not in gradle:
            fail("versionCode is not 22000")

    vpn = read("app/src/main/java/com/v2ray/ang/service/CoreVpnService.kt")
    if vpn:
        if "VpnReadiness.waitForLocalSocks" not in vpn:
            fail("CoreVpnService does not wait for local SOCKS before HEV")
        if "STARTING_HEV" not in vpn:
            fail("STARTING_HEV is missing")
        if "VERIFYING_PATH" not in vpn:
            fail("VERIFYING_PATH is missing")
        if "markConnected()" not in vpn:
            fail("CONNECTED is not published via coordinator")
        if "protect(" not in vpn and "vpnProtect" not in vpn:
            fail("VpnService.protect path missing")
        if 'addRoute("::", 0)' not in vpn:
            fail("IPv6 ::/0 capture missing")
        if "addDisallowedApplication(selfPackageName)" not in vpn:
            fail("self package is not excluded from TUN")

    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreConfigManager.kt",
        'ip = listOf("::/0")',
        "IPv6 blackhole rule",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/handler/SettingsManager.kt",
        "hotfox_hev_stability_migrated_2_0_2",
        "HEV migration",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/service/TProxyService.kt",
        'System.loadLibrary("hev-socks5-tunnel")',
        "HEV JNI",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/core/CoreServiceManager.kt",
        "waitForLocalSocksBlocking",
        "reload SOCKS wait",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/vpn/ConnectionUiMapper.kt",
        "WAITING_SOCKS",
        "UI mapper startup states",
    )
    must_contain(
        "app/src/main/java/com/v2ray/ang/ui/MainActivity.kt",
        "hotfox_headline_connected",
        "Защищено string resource",
    )
    must_contain(
        "app/src/main/res/layout/activity_main.xml",
        "HotFox",
        "editorial HotFox UI",
    )

    logo = PROJECT / "app/src/main/res/drawable/hotfox_logo.xml"
    if not logo.is_file() or logo.stat().st_size == 0:
        fail("drawable/hotfox_logo.xml missing")

    secret_re = re.compile(r"https://nox\.hotto-fox\.st/|vless://[^\s\"]{20,}")
    skip_dirs = {"build", "test", "androidTest"}
    for path in PROJECT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in skip_dirs for part in path.parts):
            continue
        if path.suffix.lower() in {".md", ".txt", ".so", ".aar", ".apk"}:
            continue
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        if secret_re.search(text):
            fail(f"possible embedded subscription/VLESS secret in {path.relative_to(PROJECT)}")

    if failures:
        print("FAIL static_check_2_2_0")
        for item in failures:
            print(f" - {item}")
        return 1
    print("PASS static_check_2_2_0")
    return 0


if __name__ == "__main__":
    sys.exit(main())
