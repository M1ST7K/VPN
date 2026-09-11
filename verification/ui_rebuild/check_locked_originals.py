#!/usr/bin/env python3
"""Pixel-diff helper. Refuses to fake PASS when SHA-256 originals are absent."""
from __future__ import annotations

import hashlib
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "design" / "hotfox_18_final_style_reference" / "MANIFEST.txt"
REF_DIR = ROOT / "design" / "hotfox_18_final_style_reference"

EXPECTED = {
    "01_splash_brand_entry.jpeg": "8b07bd2fdaba1618ce1ab9ccff07dda678e14df336db401328accb9a22bd200a",
    "02_onboarding_connect_hotfox.jpeg": "1bc006c1b48996e79d706118ab4e9aa35ca35a7999ccdd4cf157fcd4ca6145b4",
    "03_onboarding_auto.jpeg": "466ba7c4ddcded73bf1c9d55e14604f40825e1103bef3fa54f7394ac664fcd82",
    "04_onboarding_ready.jpeg": "2af79525cf77459cbcef71db80b22f59a02fd86e8ece9d433d760f6f1ee923fb",
    "05_main_not_protected.jpeg": "0ab581bb6d810a48b912eac3134f8df0438ee16995db6f1bb7620c8fa2ba0998",
    "06_connecting.png": "d8e25ac3bec19d24d1d25e2e93912bc592807530db48c72a2913b73ab4ff4997",
    "07_protected.png": "b685d00b6311d51e2b8ec9402058a9128450f8a4ce8ad68a62e3c3be77f8eb1e",
    "08_add_connection.png": "9e48bcb4dbb9001e91efdd0c425b8620e010406f2b6d75e65eac38f596cdcbab",
    "09_https_subscription.png": "41655c64d1a43ae6114ed416d60728058bbd6d40dcd8db33acde35af6baecedb",
    "10_servers.png": "6c2eb01c8a0644e8f44fb08b29c2e8631f40ea4a65ab8d39e0f29c2db0cd62ff",
    "11_server_details.png": "9e248acb335e9ec5b2682563f4b4566d137c3e5c3f2c1766cec5a8a21d0170e8",
    "12_subscription.png": "4e33f704fbe3a02d4cc3115c2aaca561a9981d76a68a8c03a6d64f9b6bb3b509",
    "13_settings.png": "7a25e843463f90aa16a9a51b070e877fb0ea1e153e8a1af8d96354f86282f6cf",
    "14_smart_routing.png": "180eb867456219206f492dbb7592d27411ff4b828cd5491ef6bc1a6b760e4b52",
    "15_apps_and_rules.png": "e1d195efdfab00161e10d832685ae088062f5be5fbe1ee2d31523c584a553e7f",
    "16_autopilot.png": "3c50bf3d57d2fff638c388ea4cad28f6f10b0b4be63011b21f03c3d77a83567c",
    "17_shadow.png": "cce2c1b6f70daf8fbe0f1ab742e0db4fb736b920afaae866e51ccd914acd94f0",
    "18_always_on_kill_switch.png": "799b3acec810f39c747a62fa31deb339068945a380b0a918b78a3dc96904feb8",
}


def main() -> int:
    missing = []
    mismatch = []
    for name, expected in EXPECTED.items():
        path = REF_DIR / name
        if not path.is_file():
            missing.append(name)
            continue
        actual = hashlib.sha256(path.read_bytes()).hexdigest()
        if actual != expected:
            mismatch.append(f"{name}: got {actual} expected {expected}")
    print(f"manifest={MANIFEST.is_file()} missing={len(missing)} mismatch={len(mismatch)}")
    if missing or mismatch:
        print("PIXEL_DIFF_STATUS=BLOCKED")
        for item in missing:
            print(f"missing_original={item}")
        for item in mismatch:
            print(f"sha_mismatch={item}")
        print("Refusing to emit an exact-pixel PASS.")
        return 2
    print("PIXEL_DIFF_STATUS=ORIGINALS_PRESENT")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
