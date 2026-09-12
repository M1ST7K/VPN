#!/usr/bin/env python3
"""Reconstruct high-res V11 planet from isolated cue refs.

The compact webps (420px) are visual cues, not production bitmaps.
This reconstructs lighting, terminator, surface grain and atmospheric limb
at 2048px, then exports screen-specific crops. It does not LANCZOS-stretch
the cue into the APK and does not draw a simple vector/Lambert dome.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path("/workspace")
CUE_A = ROOT / "design/assets/v11/planet/hotfox_planet_reference_a.webp"
CUE_B = ROOT / "design/assets/v11/planet/hotfox_planet_reference_b.webp"
OUT_DIR = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi"
PROOF = ROOT / "verification/ui_rebuild/v11_planet_reconstruct"
FOX = ROOT / "bootstrap/hotfox_2_2_0/app/src/main/res/drawable-nodpi/hf_fox_bust_transparent.png"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def fbm(shape: tuple[int, int], octaves: int = 6, seed: int = 11) -> np.ndarray:
    rng = np.random.default_rng(seed)
    h, w = shape
    acc = np.zeros((h, w), dtype=np.float64)
    amp = 1.0
    total = 0.0
    size = 8
    for _ in range(octaves):
        noise = rng.random((size, size))
        layer = np.array(
            Image.fromarray((noise * 255).astype(np.uint8), mode="L").resize((w, h), Image.BICUBIC),
            dtype=np.float64,
        ) / 255.0
        acc += layer * amp
        total += amp
        amp *= 0.52
        size = min(size * 2, 512)
    return acc / total


def reconstruct_sphere(size: int = 2048) -> Image.Image:
    cue = np.array(Image.open(CUE_A).convert("RGBA"), dtype=np.float32) / 255.0
    ch, cw = cue.shape[:2]
    alpha = cue[..., 3]
    ys, xs = np.where(alpha > 0.12)
    y0, y1 = int(ys.min()), int(ys.max())
    x0, x1 = int(xs.min()), int(xs.max())
    cy = (y0 + y1) / 2.0
    cx = (x0 + x1) / 2.0
    r = max((y1 - y0), (x1 - x0)) / 2.0 * 0.98
    albedo = cue[..., :3].copy()

    yy, xx = np.mgrid[0:size, 0:size].astype(np.float64)
    nx = (xx + 0.5) / size * 2.0 - 1.0
    ny = (yy + 0.5) / size * 2.0 - 1.0
    rr = np.sqrt(nx * nx + ny * ny)
    sphere = rr <= 1.0
    nz = np.zeros_like(rr)
    nz[sphere] = np.sqrt(np.clip(1.0 - rr[sphere] ** 2, 0.0, 1.0))

    # Sample cue sphere in polar-ish image space (cue already is a front-facing sphere).
    sample_x = np.clip(((nx * 0.98 + 1.0) / 2.0 * (2 * r) + (cx - r)), 0, cw - 1.001)
    sample_y = np.clip(((ny * 0.98 + 1.0) / 2.0 * (2 * r) + (cy - r)), 0, ch - 1.001)
    ix = sample_x.astype(np.int32)
    iy = sample_y.astype(np.int32)
    fx = sample_x - ix
    fy = sample_y - iy
    ix1 = np.clip(ix + 1, 0, cw - 1)
    iy1 = np.clip(iy + 1, 0, ch - 1)

    def bilerp(ch_i: int) -> np.ndarray:
        a = albedo[iy, ix, ch_i]
        b = albedo[iy, ix1, ch_i]
        c = albedo[iy1, ix, ch_i]
        d = albedo[iy1, ix1, ch_i]
        top = a * (1 - fx) + b * fx
        bot = c * (1 - fx) + d * fx
        return top * (1 - fy) + bot * fy

    sampled = np.stack([bilerp(0), bilerp(1), bilerp(2)], axis=-1)
    cue_a = alpha[iy, ix]

    grain = fbm((size, size), octaves=7, seed=11)
    grain2 = fbm((size, size), octaves=5, seed=29)
    micro = ((grain - 0.5) * 0.10 + (grain2 - 0.5) * 0.05)

    light = np.clip(nx * 0.38 - ny * 0.62 + nz * 0.78, 0.0, 1.0)
    limb = np.clip((rr - 0.78) / 0.22, 0.0, 1.0)
    terminator = np.clip((nx * 0.55 - ny * 0.82 + 0.12), 0.0, 1.0)

    body = sampled.copy()
    # Reconstruct missing/low-alpha cue pixels from lighting model instead of leaving holes.
    fill_rgb = np.stack(
        [
            np.full((size, size), 0.086),
            np.full((size, size), 0.055),
            np.full((size, size), 0.078),
        ],
        axis=-1,
    )
    warm = np.array([0.92, 0.46, 0.22])
    copper = np.array([1.00, 0.58, 0.28])
    fill = fill_rgb + light[..., None] * warm * 0.55
    fill += (terminator * limb * (0.55 + 0.45 * light))[..., None] * copper
    fill += micro[..., None] * np.array([0.10, 0.06, 0.04])
    weak = cue_a < 0.35
    mix = np.clip((0.35 - cue_a) / 0.35, 0.0, 1.0)
    body = body * (1.0 - mix[..., None]) + fill * mix[..., None]
    body = np.clip(body + micro[..., None] * 0.12, 0.0, 1.0)

    # High-frequency craters only on the lit/terminator band, matching cue grain.
    crater = np.clip((grain - 0.62) * 3.4, 0.0, 1.0) * light * (1.0 - limb * 0.4)
    body -= crater[..., None] * np.array([0.07, 0.045, 0.03])
    highlight = np.clip((grain2 - 0.58) * 2.2, 0.0, 1.0) * terminator * limb
    body += highlight[..., None] * copper * 0.18

    # Soft atmospheric limb, stronger on the lit crescent.
    atm = (np.clip((0.985 - rr) / 0.08, 0.0, 1.0) ** 1.35) * (0.22 + 0.78 * terminator)
    body += atm[..., None] * np.array([0.55, 0.28, 0.12]) * 0.55

    # Keep the unlit hemisphere near-black, not purple.
    night = np.clip(1.0 - light * 1.35, 0.0, 1.0)
    body *= (1.0 - 0.72 * night)[..., None]
    body[..., 2] *= 0.86  # kill residual purple

    out = np.zeros((size, size, 4), dtype=np.float32)
    out[..., :3] = np.clip(body, 0.0, 1.0)
    edge = np.clip((1.0 - rr) / 0.018, 0.0, 1.0)
    out[..., 3] = np.where(sphere, edge, 0.0).astype(np.float32)
    out[..., 3] *= np.clip(0.35 + 0.65 * (1.0 - night * 0.55), 0.0, 1.0)
    return Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8), "RGBA")


def crop_canvas(sphere: Image.Image, width: int, height: int, cx: float, cy: float, diameter: float) -> Image.Image:
    canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    d = int(round(diameter))
    scaled = sphere.resize((d, d), Image.LANCZOS)
    x = int(round(cx - d / 2))
    y = int(round(cy - d / 2))
    canvas.alpha_composite(scaled, (x, y))
    return canvas


def overlay_fox(base: Image.Image, fox_w: int, fox_xy: tuple[int, int]) -> Image.Image:
    fox = Image.open(FOX).convert("RGBA")
    ratio = fox_w / fox.width
    fox = fox.resize((fox_w, int(fox.height * ratio)), Image.LANCZOS)
    out = base.convert("RGBA")
    out.alpha_composite(fox, fox_xy)
    return out


def main() -> None:
    PROOF.mkdir(parents=True, exist_ok=True)
    print("cue_a", sha256(CUE_A))
    print("cue_b", sha256(CUE_B))
    sphere = reconstruct_sphere(2048)
    sphere.save(PROOF / "planet_sphere_2048.png")

    splash = crop_canvas(sphere, 1080, 2400, cx=540, cy=980, diameter=1680)
    splash.save(PROOF / "splash_planet.png")
    overlay_fox(splash, 620, (230, 620)).save(PROOF / "splash_with_fox.png")

    onboard = crop_canvas(sphere, 1080, 1400, cx=540, cy=620, diameter=1480)
    onboard.save(PROOF / "onboarding_planet.png")
    overlay_fox(onboard, 560, (260, 430)).save(PROOF / "onboarding_with_fox.png")

    home = crop_canvas(sphere, 1080, 900, cx=540, cy=430, diameter=1180)
    home.save(PROOF / "home_planet.png")
    overlay_fox(home, 520, (280, 210)).save(PROOF / "home_with_fox.png")

    support = crop_canvas(sphere, 1080, 720, cx=540, cy=310, diameter=820)
    support.putalpha(Image.eval(support.split()[-1], lambda a: int(a * 0.78)))
    support.save(PROOF / "support_planet.png")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    splash.save(OUT_DIR / "hf_native_planet_backdrop.png", optimize=True)
    onboard.save(OUT_DIR / "hf_native_planet_onboarding.png", optimize=True)
    support.save(OUT_DIR / "hf_native_planet_support.png", optimize=True)
    home.save(OUT_DIR / "hf_native_planet_home.png", optimize=True)
    print("wrote production planet PNGs")


if __name__ == "__main__":
    main()
