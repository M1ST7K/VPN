#!/usr/bin/env python3
"""Generate review sheets from committed baselines and fresh app captures."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "verification/ui/final_visual_consistency_20260923/comparison"
CURRENT = ROOT / "verification/ui/final_visual_consistency_20260923/actual"

ROWS = [
    (
        "01 SPLASH",
        ROOT / "verification/ui/onboarding_01_03_corrective_20260923/actual/01_standard.png",
        CURRENT / "01_splash.png",
    ),
    (
        "02 CONNECT",
        ROOT / "verification/ui/onboarding_01_03_corrective_20260923/actual/02_standard.png",
        CURRENT / "02_connect.png",
    ),
    (
        "03 AUTO",
        ROOT / "verification/ui/onboarding_01_03_corrective_20260923/actual/03_standard.png",
        CURRENT / "03_auto.png",
    ),
    (
        "04 HTTPS IMPORT",
        ROOT / "design/hotfox_18_final_style_reference/09_https_subscription.png",
        CURRENT / "04_https.png",
    ),
    (
        "05 HOME DISCONNECTED",
        ROOT / "verification/ui/v13/actual_disconnected.png",
        CURRENT / "05_disconnected.png",
    ),
    (
        "06 HOME CONNECTING",
        ROOT / "verification/ui/v13/actual_connecting.png",
        CURRENT / "06_connecting.png",
    ),
    (
        "07 HOME CONNECTED",
        ROOT / "verification/ui/v13/actual_connected.png",
        CURRENT / "07_connected.png",
    ),
]

FONT = ImageFont.load_default()
CELL_WIDTH = 320
CELL_HEIGHT = 692
LABEL_HEIGHT = 28
GUTTER = 12


def fit(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGB")
    image.thumbnail((CELL_WIDTH, CELL_HEIGHT), Image.Resampling.LANCZOS)
    canvas = Image.new("RGB", (CELL_WIDTH, CELL_HEIGHT), "#09090d")
    canvas.paste(image, ((CELL_WIDTH - image.width) // 2, (CELL_HEIGHT - image.height) // 2))
    return canvas


def label(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str) -> None:
    draw.text(xy, text, fill="#f3eee9", font=FONT)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    width = CELL_WIDTH * 2 + GUTTER * 3
    row_height = CELL_HEIGHT + LABEL_HEIGHT * 2
    sheet = Image.new("RGB", (width, row_height * len(ROWS) + GUTTER), "#09090d")
    draw = ImageDraw.Draw(sheet)

    for index, (name, baseline_path, current_path) in enumerate(ROWS):
        top = GUTTER + index * row_height
        label(draw, (GUTTER, top), f"{name}  /  BASELINE")
        label(draw, (GUTTER * 2 + CELL_WIDTH, top), f"{name}  /  CURRENT")
        image_y = top + LABEL_HEIGHT
        sheet.paste(fit(baseline_path), (GUTTER, image_y))
        sheet.paste(fit(current_path), (GUTTER * 2 + CELL_WIDTH, image_y))

        pair = Image.new("RGB", (width, CELL_HEIGHT + LABEL_HEIGHT + GUTTER), "#09090d")
        pair_draw = ImageDraw.Draw(pair)
        label(pair_draw, (GUTTER, 8), f"{name}  /  BASELINE")
        label(pair_draw, (GUTTER * 2 + CELL_WIDTH, 8), f"{name}  /  CURRENT")
        pair.paste(fit(baseline_path), (GUTTER, LABEL_HEIGHT))
        pair.paste(fit(current_path), (GUTTER * 2 + CELL_WIDTH, LABEL_HEIGHT))
        pair.save(OUT / f"{index + 1:02d}_side_by_side.png", optimize=True)

    sheet.save(OUT / "contact_01_07.png", optimize=True)


if __name__ == "__main__":
    main()
