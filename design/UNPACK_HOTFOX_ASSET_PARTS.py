#!/usr/bin/env python3
from pathlib import Path
import zipfile

base = Path(__file__).resolve().parent
target = base / "HOTFOX_APP_ASSET_KIT_EXTRACTED"
target.mkdir(exist_ok=True)

parts = [
    "HOTFOX_ASSETS_PART_A_CORE.zip",
    "HOTFOX_ASSETS_PART_B_REFERENCES_01_09.zip",
    "HOTFOX_ASSETS_PART_C_REFERENCES_10_14.zip",
    "HOTFOX_ASSETS_PART_D_REFERENCES_15_18.zip",
]

for name in parts:
    p = base / name
    if not p.exists():
        raise SystemExit(f"MISSING: {p}")
    print(f"Extracting {name}...")
    with zipfile.ZipFile(p) as z:
        z.extractall(target)

kit = target / "HOTFOX_APP_ASSET_KIT"
if not kit.exists():
    raise SystemExit("FAILED: HOTFOX_APP_ASSET_KIT not created")

count = sum(1 for p in kit.rglob("*") if p.is_file())
print(f"OK: {kit}")
print(f"Files: {count}")
if count < 1100:
    raise SystemExit(f"FAILED: expected >1100 files, got {count}")
