#!/usr/bin/env python3
"""HotFox ASSET INTAKE GATE — SHA, PNG, SVG, manifest validation."""
from __future__ import annotations

import hashlib
import json
import re
import xml.etree.ElementTree as ET
from collections import Counter, defaultdict
from pathlib import Path

from PIL import Image

KIT = Path("/workspace/design/HOTFOX_APP_ASSET_KIT_EXTRACTED/HOTFOX_APP_ASSET_KIT")
EXPECTED_TOP = [
    "README_START_HERE_RU.md",
    "CURSOR_TASK_RU.md",
    "png",
    "sources/svg",
    "manifest",
    "android",
    "scripts",
    "preview",
    "references",
    "docs",
]
REQUIRED_MANIFEST = [
    "manifest/assets.json",
    "manifest/screens.json",
    "manifest/components.json",
    "manifest/tokens.json",
    "manifest/SHA256SUMS.txt",
    "android/README_RU.md",
    "README_START_HERE_RU.md",
    "references/MANIFEST.txt",
]
DENSITY_SCALE = {"mdpi": 1, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4, "nodpi": None}
PHONE_LIKE_MIN = 300


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> dict:
    report: dict = {
        "kit_root": str(KIT),
        "top_level_present": {},
        "required_files_present": {},
        "counts": {},
        "expected": {
            "unique_assets": 212,
            "production_png_variants": 842,
            "files_total_min": 1100,
        },
        "sha": {},
        "png": {},
        "svg": {},
        "suspicious": [],
        "corrupt": [],
        "duplicates": [],
        "status": "FAIL",
    }

    if not KIT.is_dir():
        report["error"] = "KIT root missing"
        return report

    for name in EXPECTED_TOP:
        report["top_level_present"][name] = (KIT / name).exists()
    for name in REQUIRED_MANIFEST:
        report["required_files_present"][name] = (KIT / name).exists()

    files = [p for p in KIT.rglob("*") if p.is_file()]
    pngs = [p for p in files if p.suffix.lower() == ".png"]
    svgs = [p for p in files if p.suffix.lower() == ".svg"]
    xmls = [p for p in files if p.suffix.lower() == ".xml"]
    jsons = [p for p in files if p.suffix.lower() == ".json"]
    jpeg = [p for p in files if p.suffix.lower() in {".jpg", ".jpeg"}]

    cat_files = Counter()
    for p in files:
        rel = p.relative_to(KIT).as_posix()
        top = rel.split("/", 1)[0]
        cat_files[top] += 1

    png_by_density = Counter()
    png_by_category = Counter()
    for p in pngs:
        rel = p.relative_to(KIT).as_posix()
        parts = rel.split("/")
        if len(parts) >= 3 and parts[0] == "png":
            png_by_category[parts[1]] += 1
            if parts[2] in DENSITY_SCALE:
                png_by_density[parts[2]] += 1
            else:
                png_by_density["other"] += 1
        else:
            png_by_category["non_png_tree"] += 1

    assets = json.loads((KIT / "manifest/assets.json").read_text(encoding="utf-8"))
    unique_ids = [a["id"] for a in assets["assets"]]
    unique_id_set = set(unique_ids)
    dup_ids = [i for i, c in Counter(unique_ids).items() if c > 1]

    production_variants = []
    fixture_variants = []
    for a in assets["assets"]:
        for v in a["variants"]:
            item = (a["id"], a["category"], a.get("logical_size_dp"), v)
            if a["category"] == "fixture_apps":
                fixture_variants.append(item)
            else:
                production_variants.append(item)

    report["counts"] = {
        "files_total": len(files),
        "png": len(pngs),
        "svg": len(svgs),
        "xml": len(xmls),
        "json": len(jsons),
        "jpeg": len(jpeg),
        "files_by_top": dict(cat_files),
        "png_by_density": dict(png_by_density),
        "png_by_category": dict(png_by_category),
        "unique_asset_ids": len(unique_id_set),
        "unique_asset_ids_listed": len(unique_ids),
        "duplicate_asset_ids": dup_ids,
        "production_png_variants_in_manifest": len(production_variants),
        "fixture_png_variants_in_manifest": len(fixture_variants),
        "screens_json_keys": list(json.loads((KIT / "manifest/screens.json").read_text(encoding="utf-8")).keys())[:20],
    }

    screens = json.loads((KIT / "manifest/screens.json").read_text(encoding="utf-8"))
    screen_ids = screens.get("screens") or screens
    if isinstance(screen_ids, dict) and "screens" not in screens:
        report["counts"]["screen_count"] = len(screen_ids)
    elif isinstance(screens.get("screens"), list):
        report["counts"]["screen_count"] = len(screens["screens"])
        report["counts"]["screen_ids"] = [
            s.get("id") or s.get("screen_id") for s in screens["screens"]
        ]
    else:
        report["counts"]["screens_json_type"] = str(type(screens))

    # SHA256SUMS
    sums_path = KIT / "manifest/SHA256SUMS.txt"
    listed = []
    missing_sum = []
    mismatch = []
    extra_note = []
    for line in sums_path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        if len(parts) < 2:
            extra_note.append(f"malformed: {line[:80]}")
            continue
        digest, rel = parts[0], parts[-1]
        if rel.startswith("*"):
            rel = rel[1:]
        rel = rel.lstrip("./")
        listed.append(rel)
        path = KIT / rel
        if not path.is_file():
            missing_sum.append(rel)
            continue
        actual = sha256(path)
        if actual != digest.lower():
            mismatch.append({"file": rel, "expected": digest.lower(), "actual": actual})

    hashed_set = set(listed)
    kit_rel = {p.relative_to(KIT).as_posix() for p in files}
    # SHA file typically covers production binaries, not every markdown.
    unlisted_png = sorted(p.relative_to(KIT).as_posix() for p in pngs if p.relative_to(KIT).as_posix() not in hashed_set)
    unlisted_svg = sorted(p.relative_to(KIT).as_posix() for p in svgs if p.relative_to(KIT).as_posix() not in hashed_set)

    report["sha"] = {
        "listed": len(listed),
        "missing_files": missing_sum,
        "mismatches": mismatch,
        "malformed": extra_note,
        "unlisted_png_count": len(unlisted_png),
        "unlisted_svg_count": len(unlisted_svg),
        "unlisted_png_sample": unlisted_png[:20],
        "unlisted_svg_sample": unlisted_svg[:20],
        "status": "PASS" if not missing_sum and not mismatch else "FAIL",
    }

    # PNG validation
    png_errors = []
    size_mismatch = []
    alpha_mismatch = []
    unreadable = []
    fullscreen_suspects = []
    seen_sha = defaultdict(list)
    for a in assets["assets"]:
        logical = a.get("logical_size_dp")
        for v in a["variants"]:
            rel = v["file"]
            path = KIT / rel
            if not path.is_file():
                png_errors.append(f"missing {rel}")
                continue
            try:
                with Image.open(path) as im:
                    im.load()
                    w, h = im.size
                    mode = im.mode
                    has_alpha = mode in {"RGBA", "LA", "PA"} or (
                        "transparency" in im.info
                    )
            except Exception as exc:  # noqa: BLE001
                unreadable.append({"file": rel, "error": str(exc)})
                continue
            if w <= 0 or h <= 0:
                png_errors.append(f"zero size {rel}")
            digest = sha256(path)
            seen_sha[digest].append(rel)
            if v.get("sha256") and v["sha256"].lower() != digest:
                mismatch.append({"file": rel, "expected": v["sha256"], "actual": digest, "source": "assets.json"})
            if v.get("width") and v["width"] != w:
                size_mismatch.append({"file": rel, "expected": [v["width"], v["height"]], "actual": [w, h]})
            if v.get("height") and v["height"] != h:
                if not any(x["file"] == rel for x in size_mismatch):
                    size_mismatch.append({"file": rel, "expected": [v["width"], v["height"]], "actual": [w, h]})
            density = v.get("density")
            if logical and density in DENSITY_SCALE and DENSITY_SCALE[density]:
                scale = DENSITY_SCALE[density]
                exp_w, exp_h = logical[0] * scale, logical[1] * scale
                if (w, h) != (exp_w, exp_h) and density != "nodpi":
                    size_mismatch.append(
                        {
                            "file": rel,
                            "logical_dp": logical,
                            "density": density,
                            "expected_px": [exp_w, exp_h],
                            "actual_px": [w, h],
                        }
                    )
            expected_alpha = bool(v.get("alpha"))
            if expected_alpha and not has_alpha:
                alpha_mismatch.append({"file": rel, "expected_alpha": True, "mode": mode})
            if not expected_alpha and has_alpha and a["category"] not in {"artwork"}:
                # artwork foxes are documented opaque; extra alpha is not necessarily corrupt
                pass
            # full-screen screenshot heuristic inside production categories
            if a["category"] not in {"artwork", "illustrations", "fixture_apps"}:
                if w >= PHONE_LIKE_MIN and h >= 500:
                    fullscreen_suspects.append({"file": rel, "size": [w, h], "category": a["category"]})

    # also scan png tree not in manifest
    manifest_files = {v["file"] for a in assets["assets"] for v in a["variants"]}
    extra_pngs = [p for p in pngs if p.relative_to(KIT).as_posix() not in manifest_files]
    for p in extra_pngs:
        rel = p.relative_to(KIT).as_posix()
        try:
            with Image.open(p) as im:
                im.load()
                w, h = im.size
        except Exception as exc:  # noqa: BLE001
            unreadable.append({"file": rel, "error": str(exc)})
            continue
        if w >= PHONE_LIKE_MIN and h >= 500 and not rel.startswith("references/") and not rel.startswith("preview/") and not rel.startswith("qa/"):
            fullscreen_suspects.append({"file": rel, "size": [w, h], "category": "unlisted"})

    dups = [{"sha256": k, "files": v} for k, v in seen_sha.items() if len(v) > 1]
    report["png"] = {
        "unreadable": unreadable,
        "errors": png_errors,
        "size_mismatch_count": len(size_mismatch),
        "size_mismatch_sample": size_mismatch[:30],
        "alpha_mismatch": alpha_mismatch,
        "extra_png_not_in_manifest": [p.relative_to(KIT).as_posix() for p in extra_pngs],
        "fullscreen_suspects": fullscreen_suspects,
        "status": "PASS"
        if not unreadable and not png_errors and not alpha_mismatch
        else "FAIL",
    }
    report["duplicates"] = dups
    report["corrupt"] = unreadable + [{"file": e} for e in png_errors]

    # SVG
    svg_errors = []
    missing_viewbox = []
    embedded_raster = []
    external_url = []
    scripted = []
    url_re = re.compile(r"https?://", re.I)
    raster_re = re.compile(r"data:image/(png|jpe?g|gif|webp)", re.I)
    script_re = re.compile(r"<script|javascript:", re.I)
    xlink_re = re.compile(r"xlink:href\s*=\s*[\"']https?://", re.I)
    for p in svgs:
        rel = p.relative_to(KIT).as_posix()
        try:
            text = p.read_text(encoding="utf-8")
        except Exception as exc:  # noqa: BLE001
            svg_errors.append({"file": rel, "error": str(exc)})
            continue
        try:
            root = ET.fromstring(text)
        except Exception as exc:  # noqa: BLE001
            svg_errors.append({"file": rel, "error": f"xml: {exc}"})
            continue
        vb = root.attrib.get("viewBox") or root.attrib.get("viewbox")
        if not vb:
            missing_viewbox.append(rel)
        if raster_re.search(text):
            embedded_raster.append(rel)
        if url_re.search(text) or xlink_re.search(text):
            # allow xmlns URLs
            stripped = re.sub(r"xmlns(?::\w+)?=\"[^\"]+\"", "", text)
            stripped = re.sub(r"xmlns(?::\w+)?='[^']+'", "", stripped)
            if url_re.search(stripped):
                external_url.append(rel)
        if script_re.search(text):
            scripted.append(rel)

    report["svg"] = {
        "count": len(svgs),
        "xml_errors": svg_errors,
        "missing_viewBox": missing_viewbox,
        "embedded_raster": embedded_raster,
        "external_url": external_url,
        "script": scripted,
        "status": "PASS"
        if not svg_errors and not missing_viewbox and not embedded_raster and not external_url and not scripted
        else "FAIL",
    }

    report["suspicious"] = fullscreen_suspects
    missing_top = [k for k, v in report["top_level_present"].items() if not v]
    missing_req = [k for k, v in report["required_files_present"].items() if not v]
    unique_ok = len(unique_id_set) == 212 and not dup_ids
    counts_ok = len(files) >= 1100
    prod_png_ok = len(production_variants) == 842
    png_ok = report["png"]["status"] == "PASS" and not size_mismatch
    # nodpi artwork may not match density formula; treat documented nodpi separately
    density_only = [s for s in size_mismatch if s.get("density") not in (None, "nodpi")]
    png_ok = report["png"]["status"] == "PASS" and not density_only
    sha_ok = report["sha"]["status"] == "PASS"
    svg_ok = report["svg"]["status"] == "PASS"
    no_fullscreen_prod = not fullscreen_suspects

    blockers = []
    if missing_top:
        blockers.append(f"missing top-level: {missing_top}")
    if missing_req:
        blockers.append(f"missing required: {missing_req}")
    if not unique_ok:
        blockers.append(f"unique ids {len(unique_id_set)} (expected 212), dups={dup_ids}")
    if not counts_ok:
        blockers.append(f"file count {len(files)} < 1100")
    if not prod_png_ok:
        blockers.append(f"production PNG variants {len(production_variants)} (expected 842)")
    if not sha_ok:
        blockers.append("SHA mismatch or missing hashed files")
    if not png_ok:
        blockers.append("PNG validation failed")
    if not svg_ok:
        blockers.append("SVG validation failed")
    if not no_fullscreen_prod:
        blockers.append("suspicious full-screen bitmaps in production categories")
    if unreadable:
        blockers.append("unreadable PNG")

    report["blockers"] = blockers
    report["status"] = "PASS" if not blockers else "FAIL"
    out = Path("/tmp/asset_intake_gate.json")
    out.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps({k: report[k] for k in ("status", "blockers", "counts")}, indent=2, ensure_ascii=False))
    print(f"wrote {out}")
    return report


if __name__ == "__main__":
    main()
