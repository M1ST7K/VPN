#!/usr/bin/env python3
"""Generate V9 atmospheric planet: filled dark mass, one soft copper rim."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT_RES = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi"
QA = ROOT / "verification/ui_rebuild/v9_assets"
FOX = OUT_RES / "hf_fox_bust_transparent.png"


def sphere(width: int, height: int, cx: float, cy: float, radius: float, rim_gain: float) -> Image.Image:
    yy, xx = np.mgrid[0:height, 0:width].astype(np.float32)
    dx = xx - cx
    dy = yy - cy
    dist = np.sqrt(dx * dx + dy * dy)
    inside = dist <= radius
    nx = np.divide(dx, radius, out=np.zeros_like(dx), where=radius != 0)
    ny = np.divide(dy, radius, out=np.zeros_like(dy), where=radius != 0)
    light = np.clip((-nx * 0.38 - ny * 0.82) * 0.42 + 0.36, 0.0, 1.0)

    body = np.zeros((height, width, 4), dtype=np.float32)
    core = np.array([20, 17, 26], dtype=np.float32)
    edge = np.array([13, 12, 18], dtype=np.float32)
    t = np.clip(dist / radius, 0, 1) ** 1.15
    rgb = (1 - t)[..., None] * core + t[..., None] * edge
    rgb = rgb * (0.78 + 0.28 * light[..., None])
    body[..., :3] = rgb
    body[..., 3] = np.where(
        inside,
        np.clip(1.0 - np.power(np.clip((dist - radius * 0.90) / (radius * 0.10), 0, 1), 1.8), 0, 1),
        0,
    )

    ang = np.arctan2(-dy, dx)
    window = np.clip(np.cos(np.clip((ang - 0.78) / 1.22, -np.pi / 2, np.pi / 2)), 0, 1) ** 1.55
    limb = np.exp(-((dist - radius * 0.94) ** 2) / (2 * (radius * 0.06) ** 2))
    atmos = limb * window * inside.astype(np.float32)
    copper = np.array([168, 92, 52], dtype=np.float32)
    body[..., :3] += atmos[..., None] * copper * 0.16

    rim_band = np.exp(-((dist - radius * 0.997) ** 2) / (2 * (radius * 0.010) ** 2))
    rim = rim_band * window * np.clip((radius + 5 - dist) / 7, 0, 1) * rim_gain
    rim_col = np.array([208, 118, 64], dtype=np.float32)
    body[..., :3] = body[..., :3] * (1 - rim[..., None] * 0.28) + rim_col * rim[..., None]
    body[..., 3] = np.clip(body[..., 3] + rim * 0.62, 0, 1)
    body[..., 3] *= np.clip((radius + 12 - dist) / 16, 0, 1)

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
    home = sphere(1400, 900, cx=700, cy=840, radius=660, rim_gain=0.72)
    home.save(OUT_RES / "hf_native_planet_backdrop.png", optimize=True)
    support = sphere(1400, 900, cx=700, cy=900, radius=620, rim_gain=0.48)
    support.save(OUT_RES / "hf_native_planet_support.png", optimize=True)
    on_black(home).save(QA / "planet_on_black.png", optimize=True)
    fox = Image.open(FOX).convert("RGBA")
    with_fox(home, fox, (430, 160, 540, 540)).save(QA / "planet_with_fox_01.png", optimize=True)
    compact = home.resize((980, 630), Image.Resampling.LANCZOS)
    with_fox(compact, fox, (300, 70, 380, 380)).save(QA / "planet_with_fox_05.png", optimize=True)
    print("wrote planet family")


if __name__ == "__main__":
    main()
