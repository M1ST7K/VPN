"""Phase-bound checkpoint-cap accounting for the HotFox AI reviewer.

The safety cap is keyed by an explicit trusted engineering-phase identifier,
not by approval text. Historical APPROVED comments from other phases, CAP
pauses, and verdict-like prose do not reset or consume the current window.
"""

from __future__ import annotations

import os
import re
from pathlib import Path

PHASE_ID_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$")
PHASE_ID_FILE = Path("docs") / "AI_REVIEW_PHASE_ID"
MARKER_RE = re.compile(
    r"<!-- HOTFOX_AI_REVIEW head=([0-9a-f]{40}) round=(\d+)(?: phase=([A-Za-z0-9._-]+))? -->"
)
CANONICAL_VERDICTS = {"APPROVED": "VERDICT: APPROVED", "CHANGES_REQUIRED": "VERDICT: CHANGES_REQUIRED"}


def load_review_phase(
    environ: dict[str, str] | None = None,
    repo_root: Path | None = None,
) -> str:
    env = os.environ if environ is None else environ
    raw = (env.get("HOTFOX_REVIEW_PHASE") or "").strip()
    if not raw:
        root = Path.cwd() if repo_root is None else Path(repo_root)
        path = root / PHASE_ID_FILE
        if path.is_file():
            first = path.read_text(encoding="utf-8").splitlines()
            raw = first[0].strip() if first else ""
    if not PHASE_ID_RE.fullmatch(raw):
        raise RuntimeError(
            "Invalid or missing trusted engineering-phase identifier "
            f"(HOTFOX_REVIEW_PHASE / {PHASE_ID_FILE}): {raw!r}"
        )
    return raw


def format_review_marker(head: str, round_no: int, phase: str) -> str:
    if not re.fullmatch(r"[0-9a-f]{40}", head):
        raise ValueError(f"invalid review head sha: {head!r}")
    if not isinstance(round_no, int) or round_no < 1:
        raise ValueError(f"invalid review round: {round_no!r}")
    if not PHASE_ID_RE.fullmatch(phase):
        raise ValueError(f"invalid review phase id: {phase!r}")
    return f"<!-- HOTFOX_AI_REVIEW head={head} round={round_no} phase={phase} -->"


def parse_review_marker(body: str) -> dict | None:
    match = MARKER_RE.search(body or "")
    if not match:
        return None
    return {
        "head": match.group(1),
        "round": int(match.group(2)),
        "phase": match.group(3),
    }


def canonical_verdict(body: str) -> str | None:
    """Return APPROVED / CHANGES_REQUIRED only from an exact verdict line."""
    for line in (body or "").splitlines():
        stripped = line.strip()
        if stripped == CANONICAL_VERDICTS["APPROVED"]:
            return "APPROVED"
        if stripped == CANONICAL_VERDICTS["CHANGES_REQUIRED"]:
            return "CHANGES_REQUIRED"
    return None


def phase_cap_round(old: list[dict], current_phase: str) -> int:
    """Next cap-window round for current_phase.

    Counts only completed reviews whose marker phase equals current_phase.
    An APPROVED verdict does not start a new window. Comments without the
    current phase id (legacy markers, prior phases) are ignored. CAP comments
    have no canonical verdict line and are ignored.
    """
    if not PHASE_ID_RE.fullmatch(current_phase or ""):
        raise ValueError(f"invalid current_phase: {current_phase!r}")
    completed = 0
    for item in old:
        if (item.get("phase") or "") != current_phase:
            continue
        if canonical_verdict(item.get("body") or "") in {"APPROVED", "CHANGES_REQUIRED"}:
            completed += 1
    return completed + 1
