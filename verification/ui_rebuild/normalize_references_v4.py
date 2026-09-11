#!/usr/bin/env python3
"""Deterministic crop/normalization of the 18 HotFox phone-board references."""
from __future__ import annotations

import json
from pathlib import Path

from PIL import Image
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
REF_DIR = ROOT / "design" / "hotfox_18_final_style_reference"
OUT_DIR = ROOT / "verification" / "ui_rebuild" / "reference_v4"
MANIFEST = OUT_DIR / "crop_manifest.json"
TARGET = (1080, 2400)

FILES = {
    "01": "01_splash_brand_entry.jpeg",
    "02": "02_onboarding_connect_hotfox.jpeg",
    "03": "03_onboarding_auto.jpeg",
    "04": "04_onboarding_ready.jpeg",
    "05": "05_main_not_protected.jpeg",
    "06": "06_connecting.png",
    "07": "07_protected.png",
    "08": "08_add_connection.png",
    "09": "09_https_subscription.png",
    "10": "10_servers.png",
    "11": "11_server_details.png",
    "12": "12_subscription.png",
    "13": "13_settings.png",
    "14": "14_smart_routing.png",
    "15": "15_apps_and_rules.png",
    "16": "16_autopilot.png",
    "17": "17_shadow.png",
    "18": "18_always_on_kill_switch.png",
}


def phone_box(path: Path) -> tuple[int, int, int, int]:
    im = Image.open(path).convert("RGB")
    a = np.asarray(im).astype(np.float32)
    h, w = a.shape[:2]
    lum = a.mean(axis=2)
    bezel = (lum > 85) & (lum < 200)
    col_dark = (lum < 36).mean(axis=0)
    good = col_dark > 0.38
    runs: list[tuple[int, int, int, float]] = []
    i = 0
    while i < w:
        if good[i]:
            j = i
            while j < w and good[j]:
                j += 1
            cx = (i + j - 1) / 2
            if 220 < (j - i) < int(w * 0.55):
                runs.append((i, j - 1, j - i, abs(cx - w / 2)))
            i = j
        else:
            i += 1
    runs.sort(key=lambda x: (x[3], -x[2]))
    x0, x1 = runs[0][0], runs[0][1]
    width = x1 - x0 + 1
    edge = bezel[:, x0 : x0 + 6].mean(axis=1) + bezel[:, x1 - 6 : x1 + 1].mean(axis=1)
    ok = edge > 0.12
    best = None
    i = 0
    while i < h:
        if ok[i]:
            j = i
            while j < h and ok[j]:
                j += 1
            cy = (i + j - 1) / 2
            score = (j - i) - abs(cy - h / 2) * 0.15
            if best is None or score > best[2]:
                best = (i, j - 1, score)
            i = j
        else:
            i += 1
    if best and (best[1] - best[0]) > int(width * 1.4):
        y0, y1 = best[0], best[1]
    else:
        target_h = int(width * 19.5 / 9)
        y0 = max(0, (h - target_h) // 2)
        y1 = min(h - 1, y0 + target_h - 1)
    ix = int(width * 0.048)
    iy = int((y1 - y0) * 0.018)
    return x0 + ix, y0 + iy, x1 - ix, min(h - 1, y1 + int(width * 0.04) - iy)


def main() -> int:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    manifest: dict[str, object] = {
        "algorithm": "bezel-edge + dark-column center, aspect-assisted, recorded once",
        "target_size": list(TARGET),
        "screens": {},
    }
    for sid, name in FILES.items():
        src = REF_DIR / name
        x0, y0, x1, y1 = phone_box(src)
        im = Image.open(src).convert("RGB")
        crop = im.crop((x0, y0, x1 + 1, y1 + 1)).resize(TARGET, Image.Resampling.LANCZOS)
        out = OUT_DIR / f"{sid}.png"
        crop.save(out, "PNG")
        manifest["screens"][sid] = {
            "source": str(src.relative_to(ROOT)),
            "crop_xyxy": [x0, y0, x1, y1],
            "source_size": [im.size[0], im.size[1]],
            "output": str(out.relative_to(ROOT)),
            "output_size": list(TARGET),
        }
        print(f"{sid} crop=({x0},{y0},{x1},{y1}) -> {out}")
    MANIFEST.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"wrote {MANIFEST}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
