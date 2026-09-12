#!/usr/bin/env python3
"""Build side-by-side, 50% overlay and absdiff for 18 V5 screens."""
from __future__ import annotations

import csv
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFont
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
REF = ROOT / "verification" / "ui_rebuild" / "reference_v4"
ACT = ROOT / "verification" / "ui_rebuild" / "actual_v5"
CMP = ROOT / "verification" / "ui_rebuild" / "compare_v5"
CSV_PATH = ROOT / "verification" / "ui_rebuild" / "HOTFOX_PIXEL_DIFF_RESULTS_V5.csv"
SIZE = (1080, 2400)


def load(path: Path) -> Image.Image:
    im = Image.open(path).convert("RGB")
    if im.size != SIZE:
        im = im.resize(SIZE, Image.Resampling.LANCZOS)
    return im


def mean_abs(a: Image.Image, b: Image.Image) -> float:
    na = np.asarray(a).astype(np.float32)
    nb = np.asarray(b).astype(np.float32)
    return float(np.mean(np.abs(na - nb)))


def main() -> int:
    (CMP / "side_by_side").mkdir(parents=True, exist_ok=True)
    (CMP / "overlay").mkdir(parents=True, exist_ok=True)
    (CMP / "diff").mkdir(parents=True, exist_ok=True)
    rows = []
    for i in range(1, 19):
        sid = f"{i:02d}"
        ref_p = REF / f"{sid}.png"
        act_p = ACT / f"{sid}.png"
        status = "PASS" if ref_p.is_file() and act_p.is_file() else "FAIL"
        notes = []
        if not ref_p.is_file():
            notes.append("missing normalized reference")
        if not act_p.is_file():
            notes.append("missing actual capture")
        mad = ""
        if ref_p.is_file() and act_p.is_file():
            ref = load(ref_p)
            act = load(act_p)
            sbs = Image.new("RGB", (SIZE[0] * 2 + 24, SIZE[1] + 48), (13, 12, 18))
            sbs.paste(ref, (0, 48))
            sbs.paste(act, (SIZE[0] + 24, 48))
            draw = ImageDraw.Draw(sbs)
            draw.text((24, 12), f"{sid} reference (cropped board)", fill=(243, 237, 233))
            draw.text((SIZE[0] + 48, 12), f"{sid} actual emulator", fill=(243, 237, 233))
            sbs.save(CMP / "side_by_side" / f"{sid}.png")
            overlay = Image.blend(ref, act, 0.5)
            overlay.save(CMP / "overlay" / f"{sid}.png")
            diff = ImageChops.difference(ref, act)
            diff.save(CMP / "diff" / f"{sid}.png")
            mad = f"{mean_abs(ref, act):.2f}"
            notes.append("OWNER_OVERRIDE_CELESTIAL_REMOVAL on 01/02/05/07 if board still shows a planet")
            # Diff is evidence, not automatic pixel-perfect.
            visual = "CAPTURED_COMPARED_NOT_PIXEL_PASS"
        else:
            visual = "FAIL"
        rows.append(
            {
                "screen_id": sid,
                "reference": str(ref_p.relative_to(ROOT)) if ref_p.is_file() else "",
                "actual": str(act_p.relative_to(ROOT)) if act_p.is_file() else "",
                "capture_status": "CAPTURED" if act_p.is_file() else "NOT_CAPTURED",
                "compare_status": "COMPARED" if ref_p.is_file() and act_p.is_file() else "MISSING",
                "mean_abs_diff": mad,
                "visual_status": visual,
                "notes": "; ".join(notes),
            }
        )
        print(sid, visual, mad)
    with CSV_PATH.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)
    captured = sum(1 for r in rows if r["capture_status"] == "CAPTURED")
    compared = sum(1 for r in rows if r["compare_status"] == "COMPARED")
    print(f"captured={captured}/18 compared={compared}/18 csv={CSV_PATH}")
    return 0 if captured == 18 and compared == 18 else 1


if __name__ == "__main__":
    raise SystemExit(main())
