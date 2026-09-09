#!/usr/bin/env python3
"""Idempotent AndroidManifest patches for HotFox overlay features."""
from __future__ import annotations

import sys
from pathlib import Path

RECEIVER = """
        <receiver
            android:name="com.v2ray.ang.vpn.HotfoxAutopilotBootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
"""

PERMISSION = '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n'


def patch(manifest: Path) -> None:
    text = manifest.read_text(encoding="utf-8")
    changed = False
    if "RECEIVE_BOOT_COMPLETED" not in text:
        marker = "<application"
        idx = text.find(marker)
        if idx < 0:
            raise SystemExit(f"no <application in {manifest}")
        text = text[:idx] + PERMISSION + text[idx:]
        changed = True
    if "HotfoxAutopilotBootReceiver" not in text:
        marker = "</application>"
        idx = text.rfind(marker)
        if idx < 0:
            raise SystemExit(f"no </application> in {manifest}")
        text = text[:idx] + RECEIVER + "    " + text[idx:]
        changed = True
    if changed:
        manifest.write_text(text, encoding="utf-8")


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: apply_hotfox_android_manifest.py <AndroidManifest.xml>", file=sys.stderr)
        return 2
    path = Path(sys.argv[1])
    if not path.is_file():
        print(f"missing {path}", file=sys.stderr)
        return 1
    patch(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
