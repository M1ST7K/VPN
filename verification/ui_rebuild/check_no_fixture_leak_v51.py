#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path('bootstrap/hotfox_2_2_0/app/src/main')

FORBIDDEN = (
    'HotfoxUiVisualOverride',
    'ReferenceServerRow',
    'ReferenceAppRow',
    'serverDetailsFixture',
    'serversFixture',
    'subscriptionFixture',
    'appsFixture',
    'referenceServers',
    'referenceApps',
    'renderFixture(',
    'applySubscriptionVisualFixture',
    'debug-ui-fixture-server',
)

SAFE_FIXTURE_LITERALS = (
    '"18 ms"',
    '"12%"',
    '"31.12.2026"',
)


def main() -> int:
    if not ROOT.is_dir():
        print(f'FAIL: missing production source root: {ROOT}')
        return 2

    hits = []
    for path in ROOT.rglob('*'):
        if not path.is_file() or path.suffix not in {'.kt', '.java', '.xml'}:
            continue
        try:
            text = path.read_text(encoding='utf-8')
        except UnicodeDecodeError:
            continue
        for lineno, line in enumerate(text.splitlines(), 1):
            for needle in FORBIDDEN + SAFE_FIXTURE_LITERALS:
                if needle in line:
                    hits.append(f'{path}:{lineno}: {needle}: {line.strip()}')

    if hits:
        print('FAIL: debug/reference fixture logic or fixture literals leaked into src/main')
        print('\n'.join(hits))
        print('\nMove fixture-specific state/data/rendering to src/debug. Production may expose only generic presentation seams with no fake data or screenshot-specific names.')
        return 1

    print('PASS: no fixture-specific HotFox V5.1 symbols/literals found in src/main')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
