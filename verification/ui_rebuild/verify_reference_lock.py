#!/usr/bin/env python3
"""Source integrity, protected-file snapshot, and strict decoded-pixel comparison.

These checks do not establish capture provenance, functional correctness or VPN E2E.
Python 3 + Pillow; NumPy is required only for the pixels subcommand.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8'))


def safe_source(relative):
    path = (ROOT / relative).resolve()
    if not path.is_relative_to(ROOT):
        raise ValueError('Source path escapes repository')
    return path


def integrity():
    from PIL import Image
    lock = read_json(ROOT / 'design/HOTFOX_REFERENCE_LOCK.json')
    screens = lock['screens']
    if [s['id'] for s in screens] != [f'{n:02d}' for n in range(1, 19)]:
        raise ValueError('Exactly 18 distinct ordered screen IDs are required')
    failures = []
    for entry in lock['immutable_files'] + screens:
        path = safe_source(entry['path'])
        if not path.is_file():
            failures.append({'path': entry['path'], 'reason': 'MISSING'})
        elif path.stat().st_size != entry['bytes'] or digest(path) != entry['sha256']:
            failures.append({'path': entry['path'], 'reason': 'HASH_OR_SIZE_MISMATCH'})
        elif 'width' in entry:
            with Image.open(path) as im:
                if im.size != (entry['width'], entry['height']):
                    failures.append({'path': entry['path'], 'reason': 'DIMENSION_MISMATCH'})
                im.verify()
    archive = safe_source('design/HOTFOX_18_FINAL_STYLE_REFERENCE.zip')
    if archive.is_file():
        expected = {Path(e['path']).name: e for e in screens}
        manifest = next(e for e in lock['immutable_files'] if e['path'].endswith('/MANIFEST.txt'))
        expected['MANIFEST.txt'] = manifest
        with zipfile.ZipFile(archive) as z:
            members = z.namelist()
            names = ['HOTFOX_18_FINAL_STYLE_REFERENCE/' + n for n in expected]
            if len(members) != len(names) or set(members) != set(names):
                failures.append({'path': str(archive.relative_to(ROOT)), 'reason': 'ARCHIVE_MEMBERS_MISMATCH'})
            else:
                for member in members:
                    entry = expected[Path(member).name]
                    if hashlib.sha256(z.read(member)).hexdigest() != entry['sha256']:
                        failures.append({'path': member, 'reason': 'ARCHIVE_CONTENT_MISMATCH'})
    result = {'check': 'originals_integrity', 'status': 'FAIL' if failures else 'PASS',
              'screen_count': len(screens), 'failures': failures,
              'limitation': 'Originals integrity only; no implementation or visual acceptance claim.'}
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if failures else 0


def is_protected(path):
    # UI command semantics still require independent review; path is not proof.
    if path.startswith('bootstrap/hotfox_2_2_0/app/src/main/res/'):
        return False
    if path.startswith('bootstrap/hotfox_2_2_0/app/src/main/java/com/v2ray/ang/ui/'):
        return False
    if path.startswith('verification/ui_rebuild/'):
        return False
    return path.startswith(('bootstrap/', '.github/', 'verification/'))


def core():
    baseline = read_json(ROOT / 'design/HOTFOX_TECHNICAL_BASELINE.json')
    differences = []
    originals = {e['path']: e for e in baseline['protected_files']}
    for name, entry in originals.items():
        path = safe_source(name)
        if not path.is_file():
            differences.append({'path': name, 'reason': 'MISSING'})
        elif digest(path) != entry['sha256']:
            differences.append({'path': name, 'reason': 'CHANGED'})
    output = subprocess.check_output(
        ['git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard'], cwd=ROOT)
    for raw in sorted(set(output.split(b'\0'))):
        if not raw:
            continue
        name = raw.decode('utf-8')
        if is_protected(name) and name not in originals:
            differences.append({'path': name, 'reason': 'ADDED_TECHNICAL_FILE'})
    print(json.dumps({'check': 'protected_technical_snapshot',
                      'status': 'CHANGED' if differences else 'UNCHANGED',
                      'base_sha': baseline['runtime_base_sha'],
                      'protected_file_count': len(originals), 'differences': differences,
                      'limitation': 'Snapshot only; UI callbacks, runtime effects and VPN E2E require separate checks.'},
                     ensure_ascii=False, indent=2))
    return 1 if differences else 0


def pixels(args):
    import numpy as np
    from PIL import Image
    ref_path, actual_path, out = (Path(p).resolve() for p in (args.reference, args.actual, args.out))
    if out.is_relative_to(ROOT / 'design'):
        raise ValueError('Evidence output may not overwrite design sources')
    targets = {out / name for name in ('diff.png', 'overlay.png', 'metrics.json')}
    if ref_path in targets or actual_path in targets:
        raise ValueError('Evidence output collides with an input file')
    with Image.open(ref_path) as ref_file, Image.open(actual_path) as actual_file:
        ref_file.load()
        actual_file.load()
        if ref_file.size != actual_file.size:
            raise ValueError(f'Dimension mismatch: {ref_file.size} != {actual_file.size}; no automatic resize')
        ref = ref_file.convert('RGBA')
        actual = actual_file.convert('RGBA')
    a, b = np.asarray(ref).astype(np.int16), np.asarray(actual).astype(np.int16)
    delta = np.abs(a - b)
    changed = np.any(delta != 0, axis=2)
    count = int(changed.sum())
    ys, xs = np.nonzero(changed)
    bbox = [int(xs.min()), int(ys.min()), int(xs.max()) + 1, int(ys.max()) + 1] if count else None
    result = {'check': 'strict_decoded_pixel_equality', 'status': 'EXACT' if count == 0 else 'FAIL',
              'reference_sha256': digest(ref_path), 'actual_sha256': digest(actual_path),
              'width': ref.width, 'height': ref.height, 'pixels': int(changed.size),
              'changed_pixels': count, 'changed_percent': 100.0 * count / changed.size,
              'mean_channel_delta': float(delta.mean()), 'max_channel_delta': int(delta.max()),
              'diff_bbox': bbox, 'channel_tolerance': 0,
              'limitation': 'Equality of supplied images only. Source-bound viewport, APK/SHA capture provenance and functional tests must be verified separately.'}
    out.mkdir(parents=True, exist_ok=True)
    # Alpha differences remain visible in the diagnostic RGB heatmap.
    visible_delta = np.maximum(delta[:, :, :3], delta[:, :, 3:4]).astype(np.uint8)
    Image.fromarray(visible_delta).save(out / 'diff.png')
    Image.blend(ref, actual, 0.5).save(out / 'overlay.png')
    (out / 'metrics.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 1 if count else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    commands.add_parser('integrity')
    commands.add_parser('core')
    pixel_parser = commands.add_parser('pixels')
    pixel_parser.add_argument('--reference', required=True)
    pixel_parser.add_argument('--actual', required=True)
    pixel_parser.add_argument('--out', required=True)
    args = parser.parse_args()
    try:
        if args.command == 'integrity':
            return integrity()
        if args.command == 'core':
            return core()
        return pixels(args)
    except Exception as error:
        print(json.dumps({'status': 'ERROR', 'error': str(error)}, ensure_ascii=False), file=sys.stderr)
        return 2


if __name__ == '__main__':
    sys.exit(main())
