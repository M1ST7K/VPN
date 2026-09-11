#!/usr/bin/env python3
"""Rewrite Utils.isXray() so HotFox package name still selects the Xray core path."""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPLACEMENT = '''    fun isXray(): Boolean {
        return com.v2ray.ang.vpn.HotfoxXrayCapability.isXrayCore(BuildConfig.APPLICATION_ID)
    }'''

LEGACY_PATTERNS = (
    re.compile(
        r"fun isXray\(\):\s*Boolean\s*\{[^{}]*APPLICATION_ID\.startsWith\(\s*\"com\.v2ray\.ang\"\s*\)[^{}]*\}",
        re.MULTILINE,
    ),
    re.compile(
        r"fun isXray\(\):\s*Boolean\s*=\s*BuildConfig\.APPLICATION_ID\.startsWith\(\s*\"com\.v2ray\.ang\"\s*\)",
        re.MULTILINE,
    ),
)


def patch(utils: Path) -> None:
    text = utils.read_text(encoding="utf-8")
    if "HotfoxXrayCapability.isXrayCore" in text:
        return
    new = text
    for pattern in LEGACY_PATTERNS:
        new = pattern.sub(REPLACEMENT, new)
    if new == text:
        if "fun isXray(" not in text:
            raise SystemExit(f"Utils.isXray() not found in {utils}")
        # Fallback: replace the function body after the signature.
        new = re.sub(
            r"fun isXray\(\):\s*Boolean\s*\{.*?\n    \}",
            REPLACEMENT,
            text,
            count=1,
            flags=re.DOTALL,
        )
        if new == text:
            raise SystemExit(f"could not rewrite Utils.isXray() in {utils}")
    utils.write_text(new, encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: apply_hotfox_utils_isxray.py <Utils.kt>", file=sys.stderr)
        return 2
    path = Path(sys.argv[1])
    if not path.is_file():
        print(f"missing {path}", file=sys.stderr)
        return 1
    patch(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
