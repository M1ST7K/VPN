#!/usr/bin/env python3
"""Extract a clean transparent fox: opaque interior, feathered silhouette only."""
from __future__ import annotations

from collections import deque
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi/hf_fox_bust.png"
OUT_RES = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi/hf_fox_bust_transparent.png"
QA = ROOT / "verification/ui_rebuild/v5_assets"
BG = (0x0D, 0x0C, 0x12)


def dilate(mask: np.ndarray, radius: int) -> np.ndarray:
    im = Image.fromarray((mask.astype(np.uint8) * 255), mode="L")
    for _ in range(radius):
        im = im.filter(ImageFilter.MaxFilter(3))
    return np.asarray(im) > 127


def erode(mask: np.ndarray, radius: int) -> np.ndarray:
    im = Image.fromarray((mask.astype(np.uint8) * 255), mode="L")
    for _ in range(radius):
        im = im.filter(ImageFilter.MinFilter(3))
    return np.asarray(im) > 127


def close_mask(mask: np.ndarray, radius: int = 3) -> np.ndarray:
    return erode(dilate(mask, radius), radius)


def flood_background(rgb: np.ndarray, threshold: float) -> np.ndarray:
    h, w, _ = rgb.shape
    border = np.concatenate(
        [rgb[0, :, :], rgb[-1, :, :], rgb[:, 0, :], rgb[:, -1, :]],
        axis=0,
    )
    seed = np.median(border, axis=0).astype(np.float32)
    dist = np.linalg.norm(rgb.astype(np.float32) - seed, axis=2)
    similar = dist <= threshold
    bg = np.zeros((h, w), dtype=bool)
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        if similar[0, x]:
            bg[0, x] = True
            q.append((0, x))
        if similar[h - 1, x]:
            bg[h - 1, x] = True
            q.append((h - 1, x))
    for y in range(h):
        if similar[y, 0]:
            bg[y, 0] = True
            q.append((y, 0))
        if similar[y, w - 1]:
            bg[y, w - 1] = True
            q.append((y, w - 1))
    while q:
        y, x = q.popleft()
        for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
            if 0 <= ny < h and 0 <= nx < w and (not bg[ny, nx]) and similar[ny, nx]:
                bg[ny, nx] = True
                q.append((ny, nx))
    return bg


def fill_interior_holes(fg: np.ndarray) -> np.ndarray:
    empty = ~fg
    h, w = empty.shape
    vis = np.zeros_like(empty)
    q: deque[tuple[int, int]] = deque()
    for x in range(w):
        if empty[0, x]:
            vis[0, x] = True
            q.append((0, x))
        if empty[h - 1, x]:
            vis[h - 1, x] = True
            q.append((h - 1, x))
    for y in range(h):
        if empty[y, 0]:
            vis[y, 0] = True
            q.append((y, 0))
        if empty[y, w - 1]:
            vis[y, w - 1] = True
            q.append((y, w - 1))
    while q:
        y, x = q.popleft()
        for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
            if 0 <= ny < h and 0 <= nx < w and empty[ny, nx] and not vis[ny, nx]:
                vis[ny, nx] = True
                q.append((ny, nx))
    return fg | (empty & ~vis)


def extract(threshold: float = 14.0) -> tuple[Image.Image, dict[str, float]]:
    rgb = np.asarray(Image.open(SRC).convert("RGB"))
    border = np.concatenate(
        [rgb[0, :, :], rgb[-1, :, :], rgb[:, 0, :], rgb[:, -1, :]],
        axis=0,
    )
    seed = np.median(border, axis=0).astype(np.float32)
    dist = np.linalg.norm(rgb.astype(np.float32) - seed, axis=2)
    r = rgb[:, :, 0].astype(np.int16)
    g = rgb[:, :, 1].astype(np.int16)
    b = rgb[:, :, 2].astype(np.int16)
    # Warm/orange hair and any pixel clearly unlike the matte.
    obvious = (dist > threshold) | ((r > g + 8) & (r > b + 8) & (r > 35))
    # Close the glowing silhouette so dark interior fur is enclosed, then fill.
    fg = close_mask(obvious, 10)
    fg = fill_interior_holes(fg)
    fg = close_mask(fg, 2)

    interior = erode(fg, 2)
    alpha = np.zeros(fg.shape, dtype=np.uint8)
    alpha[interior] = 255
    near = dilate(interior, 1) & ~interior
    alpha[near] = 180
    outer = dilate(interior, 2) & ~dilate(interior, 1) & fg
    alpha[outer] = 80
    alpha[interior] = 255

    rgba = np.dstack([rgb, alpha])
    im = Image.fromarray(rgba, mode="RGBA")

    dark = (rgb.mean(axis=2) < 40) & (alpha > 0)
    dark_trans = dark & (alpha < 220)
    stats = {
        "threshold": threshold,
        "pct_alpha_0": float((alpha == 0).mean() * 100),
        "pct_alpha_255": float((alpha == 255).mean() * 100),
        "pct_partial": float(((alpha > 0) & (alpha < 255)).mean() * 100),
        "pct_dark_fg_alpha_lt_220": float(dark_trans.mean() * 100),
        "dark_fg_pixels": int(dark.sum()),
        "dark_translucent_pixels": int(dark_trans.sum()),
    }
    return im, stats


def checkerboard(size: tuple[int, int], cell: int = 24) -> Image.Image:
    w, h = size
    board = Image.new("RGB", (w, h), (40, 40, 40))
    draw = ImageDraw.Draw(board)
    for y in range(0, h, cell):
        for x in range(0, w, cell):
            if ((x // cell) + (y // cell)) % 2 == 0:
                draw.rectangle([x, y, x + cell, y + cell], fill=(210, 210, 210))
    return board


def main() -> int:
    QA.mkdir(parents=True, exist_ok=True)
    im, stats = extract()
    im.save(OUT_RES, optimize=True)
    cb = checkerboard(im.size)
    cb.paste(im, mask=im.split()[-1])
    cb.save(QA / "fox_checkerboard.png")
    bg = Image.new("RGB", im.size, BG)
    bg.paste(im, mask=im.split()[-1])
    bg.save(QA / "fox_hotfox_bg.png")
    a = np.asarray(im)[:, :, 3]
    diag = np.zeros((*a.shape, 3), dtype=np.uint8)
    diag[a == 0] = (20, 20, 30)
    diag[a == 255] = (40, 180, 90)
    diag[(a > 0) & (a < 255)] = (230, 160, 40)
    Image.fromarray(diag).save(QA / "fox_alpha_diagnostic.png")
    (QA / "fox_alpha_stats.txt").write_text(
        "\n".join(f"{k}={v}" for k, v in stats.items()) + "\n",
        encoding="utf-8",
    )
    print(stats)
    if stats["pct_dark_fg_alpha_lt_220"] > 1.5:
        print("WARN: dark fur still materially translucent")
        return 2
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
