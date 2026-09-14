#!/usr/bin/env python3
"""V10 planet: dark atmospheric volume, cinematic directional rim, not a glowing dome."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT_RES = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi"
QA = ROOT / "verification/ui_rebuild/v10_assets"
FOX = OUT_RES / "hf_fox_bust_transparent.png"
BG = np.array([13, 12, 18], dtype=np.float32)


def sphere(
    width: int,
    height: int,
    cx: float,
    cy: float,
    radius: float,
    rim_gain: float,
) -> Image.Image:
    yy, xx = np.mgrid[0:height, 0:width].astype(np.float32)
    dx = xx - cx
    dy = yy - cy
    dist = np.sqrt(dx * dx + dy * dy)
    nx = np.divide(dx, radius, out=np.zeros_like(dx), where=radius != 0)
    ny = np.divide(dy, radius, out=np.zeros_like(dy), where=radius != 0)
    # Light from upper-left, not a uniform halo.
    ndl = np.clip(-nx * 0.46 - ny * 0.78, 0.0, 1.0)
    night = np.clip(1.0 - ndl, 0.0, 1.0)

    body = np.zeros((height, width, 4), dtype=np.float32)
    # Interior must stay readable as mass vs bg (13,12,18) — not a rim-only dome.
    lit = BG + np.array([34.0, 22.0, 38.0], dtype=np.float32)
    shade = BG + np.array([6.0, 4.0, 9.0], dtype=np.float32)
    rgb = shade + (lit - shade) * (ndl ** 1.12)[..., None]
    terminator = np.exp(-((ndl - 0.22) ** 2) / (2 * 0.13**2))
    copper_vol = np.array([36.0, 18.0, 12.0], dtype=np.float32)
    rgb += terminator[..., None] * copper_vol * 0.14
    rgb *= 1.0 - 0.12 * night[..., None]
    body[..., :3] = rgb

    limb_fade = np.clip((radius - dist) / (radius * 0.16), 0.0, 1.0)
    body[..., 3] = np.where(dist <= radius * 1.03, limb_fade**0.7, 0.0)
    interior = dist <= radius * 0.88
    body[..., 3] = np.where(interior, np.maximum(body[..., 3], 0.96), body[..., 3])

    ang = np.arctan2(-dy, dx)
    # Short cinematic crescent on the upper-left limb — not an even halo.
    window = np.exp(-((ang - 0.92) ** 2) / (2 * 0.28**2))
    window *= np.clip(ndl * 1.8 + 0.05, 0.0, 1.0)
    rim_band = np.exp(-((dist - radius * 0.978) ** 2) / (2 * (radius * 0.012) ** 2))
    rim = rim_band * window * rim_gain
    rim_col = np.array([168.0, 92.0, 50.0], dtype=np.float32)
    body[..., :3] = body[..., :3] * (1 - rim[..., None] * 0.12) + rim_col * rim[..., None] * 0.38
    body[..., 3] = np.clip(body[..., 3] + rim * 0.18, 0, 1)
    body[..., 3] *= np.clip((radius * 1.05 - dist) / (radius * 0.10), 0, 1)

    rgba = np.zeros((height, width, 4), dtype=np.uint8)
    rgba[..., :3] = np.clip(body[..., :3], 0, 255).astype(np.uint8)
    rgba[..., 3] = np.clip(body[..., 3] * 255, 0, 255).astype(np.uint8)
    return Image.fromarray(rgba, "RGBA")


def on_black(planet: Image.Image) -> Image.Image:
    canvas = Image.new("RGB", planet.size, (13, 12, 18))
    canvas.paste(planet, (0, 0), planet)
    return canvas


def with_fox(planet: Image.Image, fox: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    canvas = on_black(planet).convert("RGBA")
    fx = fox.resize((box[2], box[3]), Image.Resampling.LANCZOS)
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    layer.paste(fx, (box[0], box[1]), fx)
    return Image.alpha_composite(canvas, layer).convert("RGB")


def main() -> None:
    OUT_RES.mkdir(parents=True, exist_ok=True)
    QA.mkdir(parents=True, exist_ok=True)
    # Lower-field planetary mass: filled disk, one directional rim.
    home = sphere(1600, 1100, cx=800, cy=900, radius=700, rim_gain=0.40)
    home.save(OUT_RES / "hf_native_planet_backdrop.png", optimize=True)
    support = sphere(1600, 1100, cx=800, cy=980, radius=640, rim_gain=0.24)
    support.save(OUT_RES / "hf_native_planet_support.png", optimize=True)
    on_black(home).save(QA / "planet_on_black.png", optimize=True)
    fox = Image.open(FOX).convert("RGBA")
    with_fox(home, fox, (470, 210, 660, 660)).save(QA / "planet_with_fox_01.png", optimize=True)
    compact = home.resize((1100, 756), Image.Resampling.LANCZOS)
    with_fox(compact, fox, (330, 90, 440, 440)).save(QA / "planet_with_fox_05.png", optimize=True)
    print("wrote V10 planet family")


if __name__ == "__main__":
    main()
