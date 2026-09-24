#!/usr/bin/env python3
"""A/B font comparison: pre-Onest Pass 3 capture (same renderer, viewport, density) vs Onest Pass 4."""
from pathlib import Path

from PIL import Image, ImageDraw

HERE = Path(__file__).resolve().parent
BEFORE = HERE.parent / "final_polishing_pass3_20260923" / "actual"
AFTER = HERE / "actual"
OUT = HERE / "comparison"
NAMES = ["01_splash", "02_connect", "03_auto", "04_https", "05_disconnected", "06_connecting", "07_connected"]
FOCUS = {  # full-res crops (x0, y0, x1, y1) at 1032x2292 for typography close-ups
    "05_disconnected": [(0, 60, 1032, 560), (0, 1240, 1032, 2292)],
    "02_connect": [(0, 60, 1032, 560), (0, 1900, 1032, 2292)],
    "04_https": [(0, 60, 1032, 680)],
}


def pair(a: Image.Image, b: Image.Image, label: str) -> Image.Image:
    sheet = Image.new("RGB", (a.width * 2 + 20, a.height + 50), (24, 24, 28))
    draw = ImageDraw.Draw(sheet)
    sheet.paste(a, (0, 50))
    sheet.paste(b, (a.width + 20, 50))
    draw.text((10, 16), f"{label}  A: pre-Onest (pass 3)", fill=(230, 230, 230))
    draw.text((a.width + 30, 16), "B: Onest (pass 4)", fill=(230, 230, 230))
    return sheet


def main() -> None:
    OUT.mkdir(exist_ok=True)
    thumbs = []
    for name in NAMES:
        a = Image.open(BEFORE / f"{name}.png").convert("RGB")
        b = Image.open(AFTER / f"{name}.png").convert("RGB").resize(a.size)
        sheet = pair(a, b, name)
        sheet.save(OUT / f"{name}_A_vs_B.png")
        thumbs.append(sheet.resize((sheet.width * 640 // sheet.height, 640)))
        for i, box in enumerate(FOCUS.get(name, [])):
            pair(a.crop(box), b.crop(box), f"{name} zoom {i + 1}").save(OUT / f"{name}_zoom{i + 1}_A_vs_B.png")
    width = max(t.width for t in thumbs)
    cols = 4
    rows = (len(thumbs) + cols - 1) // cols
    contact = Image.new("RGB", (cols * (width + 10), rows * 650), (24, 24, 28))
    for i, thumb in enumerate(thumbs):
        contact.paste(thumb, ((i % cols) * (width + 10), (i // cols) * 650))
    contact.save(OUT / "contact_A_pre_onest_vs_B_onest.png")


if __name__ == "__main__":
    main()
