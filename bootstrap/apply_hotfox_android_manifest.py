#!/usr/bin/env python3
"""Idempotent AndroidManifest patches for HotFox overlay features."""
from __future__ import annotations

import re
import sys
from pathlib import Path

BOOT_RECEIVER = """
        <receiver
            android:name="com.v2ray.ang.vpn.HotfoxAutopilotBootReceiver"
            android:enabled="true"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
"""

PAUSE_RECEIVER = """
        <receiver
            android:name="com.v2ray.ang.vpn.HotfoxAutopilotPauseReceiver"
            android:enabled="true"
            android:exported="false" />
"""

PERMISSION = '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />\n'

ACTIVITIES = {
    "HotfoxOnboardingActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxOnboardingActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
    "HotfoxAutopilotActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxAutopilotActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
    "HotfoxRoutingPrivacyActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxRoutingPrivacyActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
    "HotfoxHttpsSubscriptionActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxHttpsSubscriptionActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme"
            android:windowSoftInputMode="adjustResize" />
""",
    "HotfoxServerDetailsActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxServerDetailsActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
    "HotfoxSettingsActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxSettingsActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
    "HotfoxShadowActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxShadowActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
    "HotfoxAlwaysOnActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxAlwaysOnActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
    "HotfoxAppsRulesActivity": """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxAppsRulesActivity"
            android:exported="false"
            android:theme="@style/HotFoxEditorialTheme" />
""",
}

SPLASH_ACTIVITY = """
        <activity
            android:name="com.v2ray.ang.ui.HotfoxSplashActivity"
            android:exported="true"
            android:theme="@style/HotFoxSplashTheme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
"""


def _insert_before_application_end(text: str, snippet: str) -> str:
    marker = "</application>"
    idx = text.rfind(marker)
    if idx < 0:
        raise SystemExit("no </application> in manifest")
    return text[:idx] + snippet + "    " + text[idx:]


def _move_launcher_to_splash(text: str) -> str:
    if "HotfoxSplashActivity" in text:
        return text
    text = _insert_before_application_end(text, SPLASH_ACTIVITY)
    main_idx = text.find('android:name=".ui.MainActivity"')
    if main_idx < 0:
        main_idx = text.find("android:name=\"com.v2ray.ang.ui.MainActivity\"")
    if main_idx < 0:
        return text
    window = text[main_idx : main_idx + 1200]
    stripped = window.replace(
        '                <category android:name="android.intent.category.LAUNCHER" />\n',
        "",
        1,
    )
    if stripped == window:
        stripped = window.replace(
            '<category android:name="android.intent.category.LAUNCHER" />',
            "",
            1,
        )
    return text[:main_idx] + stripped + text[main_idx + len(window) :]


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
        text = _insert_before_application_end(text, BOOT_RECEIVER)
        changed = True
    if "HotfoxAutopilotPauseReceiver" not in text:
        text = _insert_before_application_end(text, PAUSE_RECEIVER)
        changed = True
    for name, snippet in ACTIVITIES.items():
        if name not in text:
            text = _insert_before_application_end(text, snippet)
            changed = True
    if "HotfoxSplashActivity" not in text:
        text = _move_launcher_to_splash(text)
        changed = True
    text = re.sub(
        r'(android:name="com\.v2ray\.ang\.ui\.Hotfox(?:Onboarding|Autopilot|RoutingPrivacy)Activity"\s+android:exported="false"\s+android:theme=")@style/AppThemeDayNight\.NoActionBar(")',
        r"\1@style/HotFoxEditorialTheme\2",
        text,
    )
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
