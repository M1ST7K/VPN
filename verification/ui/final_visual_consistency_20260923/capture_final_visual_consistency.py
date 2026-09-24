#!/usr/bin/env python3
"""Capture the real debug renderer for HotFox visual consistency screens 01–07."""
from __future__ import annotations

import json
import subprocess
import time
from io import BytesIO
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "verification" / "ui" / "final_visual_consistency_20260923"
PKG = "com.hotfox.vpn"
HARNESS = f"{PKG}/com.v2ray.ang.ui.HotfoxUiScreenshotHarnessActivity"
SERIAL = "emulator-5554"
DENSITY = 420

SCENARIOS = {
    "01_splash": "01_SPLASH",
    "02_connect": "02_ONBOARD_CONNECT",
    "03_auto": "03_ONBOARD_AUTO",
    "04_https": "09_HTTPS_SUBSCRIPTION",
    "05_disconnected": "05_DISCONNECTED",
    "06_connecting": "06_CONNECTING_VISUAL",
    "07_connected": "07_PROTECTED_VISUAL",
}

READY_TEXT = {
    "01_SPLASH": "HotFox",
    "02_ONBOARD_CONNECT": "У меня уже есть подписка",
    "03_ONBOARD_AUTO": "Использовать AUTO",
    "09_HTTPS_SUBSCRIPTION": "HTTPS-подписка",
    "05_DISCONNECTED": "Не защищено",
    "06_CONNECTING_VISUAL": "Подключаем",
    "07_PROTECTED_VISUAL": "Вы защищены",
}

JOBS = [
    ("actual", "01_splash", "1.0", (1032, 2231)),
    ("actual", "02_connect", "1.0", (1032, 2231)),
    ("actual", "03_auto", "1.0", (1032, 2231)),
    ("actual", "04_https", "1.0", (1032, 2231)),
    ("actual", "05_disconnected", "1.0", (1032, 2231)),
    ("actual", "06_connecting", "1.0", (1032, 2231)),
    ("actual", "07_connected", "1.0", (1032, 2231)),
    ("matrix", "03_auto_compact", "1.0", (945, 2100)),
    ("matrix", "04_https_compact", "1.0", (945, 2100)),
    ("matrix", "05_home_compact", "1.0", (945, 2100)),
    ("matrix", "02_connect_fs115", "1.15", (1032, 2231)),
    ("matrix", "03_auto_fs115", "1.15", (1032, 2231)),
]


def adb(*args: str, timeout: int = 90) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=False,
        text=True,
        capture_output=True,
        timeout=timeout,
    )


def scenario_for(name: str) -> str:
    if name.startswith("03_auto"):
        return SCENARIOS["03_auto"]
    if name.startswith("04_https"):
        return SCENARIOS["04_https"]
    if name.startswith("05_home"):
        return SCENARIOS["05_disconnected"]
    if name.startswith("02_connect"):
        return SCENARIOS["02_connect"]
    return SCENARIOS[name]


def dismiss_environment_dialogs() -> None:
    adb("shell", "settings", "put", "global", "hide_error_dialogs", "1")
    adb("shell", "input", "tap", "516", "1306")


def launch(scenario: str) -> None:
    adb("shell", "am", "force-stop", PKG)
    time.sleep(1)
    adb(
        "shell",
        "am",
        "start",
        "-n",
        HARNESS,
        "-a",
        "com.hotfox.vpn.action.UI_SCREENSHOT",
        "--es",
        "scenario",
        scenario,
    )


def wait_until_rendered(scenario: str, timeout_seconds: int = 120) -> None:
    marker = READY_TEXT[scenario]
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        adb("shell", "rm", "-f", "/sdcard/hotfox-window.xml")
        adb("shell", "uiautomator", "dump", "/sdcard/hotfox-window.xml", timeout=30)
        window = adb("exec-out", "cat", "/sdcard/hotfox-window.xml", timeout=30)
        if window.returncode == 0 and marker in window.stdout:
            # Let posted decorators and compound drawables finish after the
            # semantic content first appears, especially on TCG emulators.
            time.sleep(5)
            return
        time.sleep(2)
    raise RuntimeError(f"screen did not render marker {marker!r} for {scenario}")


def frame_quality(png: bytes) -> tuple[bool, float, float]:
    if len(png) < 40_000:
        return False, 0.0, 0.0
    image = Image.open(BytesIO(png)).convert("RGB")
    pixels = np.asarray(image)
    mean = float(pixels.mean())
    std = float(pixels.std())
    return mean > 8.0 and std > 9.0, mean, std


def capture(dest: Path) -> dict[str, object]:
    last = (0, 0.0, 0.0)
    for _ in range(30):
        raw = subprocess.run(
            ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
            check=False,
            capture_output=True,
            timeout=90,
        )
        ok, mean, std = frame_quality(raw.stdout)
        last = (len(raw.stdout), mean, std)
        if raw.returncode == 0 and ok:
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(raw.stdout)
            image = Image.open(BytesIO(raw.stdout))
            print(f"CAPTURED {dest.name} {image.size} mean={mean:.1f} std={std:.1f}", flush=True)
            return {
                "path": str(dest.relative_to(ROOT)),
                "viewport": f"{image.width}x{image.height}",
                "bytes": len(raw.stdout),
                "mean": round(mean, 2),
                "std": round(std, 2),
            }
        time.sleep(2.5)
        dismiss_environment_dialogs()
    raise RuntimeError(f"invalid frame {dest}: bytes={last[0]} mean={last[1]:.1f} std={last[2]:.1f}")


def main() -> int:
    adb("wait-for-device")
    adb("shell", "settings", "put", "global", "animator_duration_scale", "0")
    adb("shell", "settings", "put", "global", "transition_animation_scale", "0")
    adb("shell", "settings", "put", "global", "window_animation_scale", "0")
    adb("shell", "cmd", "locale", "set-app-locales", PKG, "--locales", "ru-RU")
    records = []
    for folder, name, font_scale, size in JOBS:
        dismiss_environment_dialogs()
        adb("shell", "wm", "size", f"{size[0]}x{size[1]}")
        adb("shell", "wm", "density", str(DENSITY))
        adb("shell", "settings", "put", "system", "font_scale", font_scale)
        time.sleep(2)
        scenario = scenario_for(name)
        launch(scenario)
        wait_until_rendered(scenario)
        record = capture(OUT / folder / f"{name}.png")
        record.update(
            {
                "screen": name,
                "scenario": scenario_for(name),
                "density": DENSITY,
                "fontScale": font_scale,
                "locale": "ru-RU",
            }
        )
        records.append(record)
    manifest = {
        "task": "HOTFOX-FINAL-VISUAL-CONSISTENCY-20260923",
        "source_sha": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "captures": records,
    }
    (OUT / "capture_manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
