#!/usr/bin/env python3
"""Measure how far the CTA icon+label group center is from the button center, in dp."""
import json
import re
from pathlib import Path

import numpy as np
from PIL import Image

HERE = Path(__file__).resolve().parent
DENSITY = 420 / 160
FILES = [
    "actual/05_disconnected",
    "actual/06_connecting",
    "actual/07_connected",
    "matrix/05_home_360",
    "matrix/06_connecting_360",
    "matrix/07_connected_360",
    "matrix/05_home_412",
    "matrix/06_connecting_412",
    "matrix/07_connected_412",
    "matrix/05_home_360_fs115",
    "matrix/06_connecting_360_fs115",
    "matrix/07_connected_360_fs115",
    "matrix/05_home_393_fs115",
    "matrix/06_connecting_393_fs115",
    "matrix/07_connected_393_fs115",
    "matrix/05_home_412_fs115",
    "matrix/06_connecting_412_fs115",
    "matrix/07_connected_412_fs115",
]


def cta_bounds(xml: str):
    m = re.search(r'resource-id="com\.hotfox\.vpn:id/connect_action"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml)
    return tuple(int(v) for v in m.groups())


def main() -> None:
    rows = []
    for name in FILES:
        png = HERE / f"{name}.png"
        xml = png.with_suffix(".xml").read_text()
        text = re.search(r'text="([^"]*)" resource-id="com\.hotfox\.vpn:id/connect_action"', xml).group(1)
        left, top, right, bottom = cta_bounds(xml)
        img = np.asarray(Image.open(png).convert("RGB")).astype(int)
        h = bottom - top
        inner = img[top + h // 4: bottom - h // 4, left + 40: right - 40]
        r, g, b = inner[..., 0], inner[..., 1], inner[..., 2]
        if text == "Отменить":
            content = (r > 200) & (g > 90) & (g < 170) & (b < 110)
        else:
            content = (r > 225) & (g > 225) & (b > 225)
        cols = np.where(content.sum(axis=0) > 0)[0]
        group_left = left + 40 + int(cols.min())
        group_right = left + 40 + int(cols.max())
        delta_dp = ((group_left + group_right) / 2 - (left + right) / 2) / DENSITY
        rows.append({
            "capture": name, "label": text, "button_px": [left, right],
            "group_px": [group_left, group_right], "center_delta_dp": round(delta_dp, 2),
            "pass_le_2dp": abs(delta_dp) <= 2.0,
        })
        print(f"{name:28s} {text:11s} group={group_left}-{group_right} delta={delta_dp:+.2f}dp")
    (HERE / "cta_centering.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
