#!/usr/bin/env python3
"""Generate V8 filled planetary mass with one fading copper rim."""
from __future__ import annotations

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT_RES = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi"
QA = ROOT / "verification/ui_rebuild/v8_assets"
FOX = OUT_RES / "hf_fox_bust_transparent.png"
BG = np.array([13, 12, 18], dtype=np.float32)


def sphere(width: int, height: int, cx: float, cy: float, radius: float) -> Image.Image:
    yy, xx = np.mgrid[0:height, 0:width].astype(np.float32)
    dx = xx - cx
    dy = yy - cy
    dist = np.sqrt(dx * dx + dy * dy)
    inside = dist <= radius
    nx = np.divide(dx, radius, out=np.zeros_like(dx), where=radius != 0)
    ny = np.divide(dy, radius, out=np.zeros_like(dy), where=radius != 0)
    # Lit from upper-right.
    light = np.clip((-nx * 0.42 - ny * 0.78) * 0.5 + 0.42, 0.0, 1.0)

    body = np.zeros((height, width, 4), dtype=np.float32)
    core = np.array([22, 18, 28], dtype=np.float32)
    mid = np.array([16, 14, 21], dtype=np.float32)
    edge = np.array([12, 11, 16], dtype=np.float32)
    t = np.clip(dist / radius, 0, 1)
    rgb = (1 - t)[..., None] * core + t[..., None] * edge
    rgb = rgb * (0.72 + 0.38 * light[..., None])
    rgb = np.minimum(rgb, mid * 1.35)
    body[..., :3] = rgb
    body[..., 3] = np.where(inside, np.clip(1.0 - np.power(np.clip((dist - radius * 0.92) / (radius * 0.08), 0, 1), 1.6), 0, 1), 0)

    # Soft inner atmospheric falloff near the limb, not a second stroke.
    limb = np.exp(-((dist - radius * 0.93) ** 2) / (2 * (radius * 0.055) ** 2))
    ang = np.arctan2(-dy, dx)  # 0 = right, +pi/2 = up
    # Peak rim on upper-right; fade before hard left/bottom ends.
    window = np.clip(np.cos(np.clip((ang - 0.72) / 1.15, -np.pi / 2, np.pi / 2)), 0, 1) ** 1.35
    atmos = limb * window * inside.astype(np.float32)
    copper = np.array([196, 106, 58], dtype=np.float32)
    body[..., :3] += atmos[..., None] * copper * 0.22
    body[..., 3] = np.clip(body[..., 3] + atmos * 0.08, 0, 1)

    # ONE principal rim: thin gaussian on the circumference.
    rim_band = np.exp(-((dist - radius * 0.995) ** 2) / (2 * (radius * 0.012) ** 2))
    rim = rim_band * window * np.clip((radius + 6 - dist) / 8, 0, 1)
    rim_col = np.array([224, 132, 72], dtype=np.float32)
    body[..., :3] = body[..., :3] * (1 - rim[..., None] * 0.35) + rim_col * rim[..., None]
    body[..., 3] = np.clip(body[..., 3] + rim * 0.85, 0, 1)

    # Fade alpha at the very outer edge so endpoints dissolve into background.
    body[..., 3] *= np.clip((radius + 10 - dist) / 14, 0, 1)

    out = np.clip(body, 0, 255)
    rgba = np.zeros((height, width, 4), dtype=np.uint8)
    rgba[..., :3] = np.clip(out[..., :3], 0, 255).astype(np.uint8)
    rgba[..., 3] = np.clip(out[..., 3] * 255, 0, 255).astype(np.uint8)
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
    planet = sphere(1400, 900, cx=700, cy=820, radius=640)
    planet.save(OUT_RES / "hf_native_planet_backdrop.png", optimize=True)
    on_black(planet).save(QA / "planet_on_black.png", optimize=True)
    fox = Image.open(FOX).convert("RGBA")
    with_fox(planet, fox, (430, 180, 540, 540)).save(QA / "planet_with_fox_01.png", optimize=True)
    compact = planet.resize((980, 630), Image.Resampling.LANCZOS)
    with_fox(compact, fox, (300, 90, 380, 380)).save(QA / "planet_with_fox_05.png", optimize=True)
    print("wrote", OUT_RES / "hf_native_planet_backdrop.png")


if __name__ == "__main__":
    main()
