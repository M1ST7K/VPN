#!/usr/bin/env python3
"""Import the owner fox JPEG as drawable-nodpi/hotfox_fox_master.png.

Does not redraw or resample the fox. Only keys the white chat backdrop to
alpha and trims fully transparent margins.
"""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SRC = Path("/home/ubuntu/.cursor/projects/workspace/assets/07AB3456-1966-4684-A11D-073BD716350D_L0_001.jpg")
OUT = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi/hotfox_fox_master.png"


def main() -> int:
    rgb = np.asarray(Image.open(SRC).convert("RGB"))
    r = rgb[:, :, 0].astype(np.float32)
    g = rgb[:, :, 1].astype(np.float32)
    b = rgb[:, :, 2].astype(np.float32)
    minc = np.minimum(np.minimum(r, g), b)
    maxc = np.maximum(np.maximum(r, g), b)
    sat = maxc - minc
    alpha = np.full(minc.shape, 255.0, dtype=np.float32)
    white = (minc >= 246) & (sat <= 10)
    alpha[white] = 0
    feather = (minc > 228) & (sat < 22) & ~white
    t = np.clip((255.0 - minc[feather]) / 27.0, 0.0, 1.0)
    alpha[feather] = t * 255.0
    rgba = np.dstack([rgb, np.clip(alpha, 0, 255).astype(np.uint8)])
    ys, xs = np.where(rgba[:, :, 3] > 8)
    pad = 4
    y0 = max(int(ys.min()) - pad, 0)
    x0 = max(int(xs.min()) - pad, 0)
    y1 = min(int(ys.max()) + pad + 1, rgba.shape[0])
    x1 = min(int(xs.max()) + pad + 1, rgba.shape[1])
    OUT.parent.mkdir(parents=True, exist_ok=True)
    Image.fromarray(rgba[y0:y1, x0:x1], "RGBA").save(OUT, format="PNG", optimize=True)
    print(f"wrote {OUT} {x1 - x0}x{y1 - y0}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
