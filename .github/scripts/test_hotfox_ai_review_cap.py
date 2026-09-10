#!/usr/bin/env python3
"""Unit tests for phase-bound HotFox AI review cap accounting."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from hotfox_ai_review_cap import (
    canonical_verdict,
    format_review_marker,
    load_review_phase,
    parse_review_marker,
    phase_cap_round,
)

HEAD_A = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
HEAD_B = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"


def item(round_no: int, phase: str | None, body: str, head: str = HEAD_A) -> dict:
    return {
        "head": head,
        "round": round_no,
        "phase": phase,
        "body": body,
        "created_at": "",
    }


class CanonicalVerdictTests(unittest.TestCase):
    def test_exact_verdict_lines(self) -> None:
        self.assertEqual(canonical_verdict("intro\nVERDICT: APPROVED\n"), "APPROVED")
        self.assertEqual(
            canonical_verdict("VERDICT: CHANGES_REQUIRED\nSUMMARY\n"),
            "CHANGES_REQUIRED",
        )

    def test_prose_substring_is_not_a_verdict(self) -> None:
        body = "Earlier the bot wrote VERDICT: APPROVED inside this sentence.\nNo canonical line."
        self.assertIsNone(canonical_verdict(body))

    def test_blockquote_and_trailing_junk_are_ignored(self) -> None:
        self.assertIsNone(canonical_verdict("> VERDICT: APPROVED\n"))
        self.assertIsNone(canonical_verdict("VERDICT: APPROVED.\n"))
        self.assertIsNone(canonical_verdict("VERDICT: APPROVED extra\n"))

    def test_cap_comment_has_no_verdict(self) -> None:
        body = (
            "### HotFox AI Review — automation paused\n\n"
            "Reached the safety cap of 20 checkpoint rounds. No Cursor handoff was emitted."
        )
        self.assertIsNone(canonical_verdict(body))


class MarkerTests(unittest.TestCase):
    def test_roundtrip_includes_phase(self) -> None:
        marker = format_review_marker(HEAD_A, 22, "3.1")
        parsed = parse_review_marker(marker + "\nbody")
        self.assertEqual(parsed["head"], HEAD_A)
        self.assertEqual(parsed["round"], 22)
        self.assertEqual(parsed["phase"], "3.1")

    def test_legacy_marker_has_no_phase(self) -> None:
        body = f"<!-- HOTFOX_AI_REVIEW head={HEAD_B} round=19 -->\nVERDICT: APPROVED\n"
        parsed = parse_review_marker(body)
        self.assertEqual(parsed["head"], HEAD_B)
        self.assertEqual(parsed["round"], 19)
        self.assertIsNone(parsed["phase"])


class PhaseCapTests(unittest.TestCase):
    def test_historical_approvals_from_prior_phases_are_ignored(self) -> None:
        old = [
            item(4, "2.4", "VERDICT: APPROVED"),
            item(16, "2.9", "VERDICT: APPROVED"),
            item(19, "3.0", "VERDICT: APPROVED"),
            item(20, "3.1", "VERDICT: CHANGES_REQUIRED"),
        ]
        self.assertEqual(phase_cap_round(old, "3.1"), 2)

    def test_approval_does_not_reset_same_phase_window(self) -> None:
        old = [
            item(1, "3.1", "header\nVERDICT: APPROVED\n"),
            item(2, "3.1", "header\nVERDICT: CHANGES_REQUIRED\n"),
            item(3, "3.1", "header\nVERDICT: APPROVED\n"),
        ]
        self.assertEqual(phase_cap_round(old, "3.1"), 4)

    def test_cap_comments_are_not_reviews(self) -> None:
        old = [
            item(
                21,
                "3.1",
                "### HotFox AI Review — automation paused\nReached the safety cap of 20.",
            ),
            item(22, None, "automation paused without a phase id"),
        ]
        self.assertEqual(phase_cap_round(old, "3.1"), 1)

    def test_legacy_markers_without_phase_are_not_current_phase(self) -> None:
        old = [item(19, None, "VERDICT: APPROVED")]
        self.assertEqual(phase_cap_round(old, "3.1"), 1)

    def test_verdict_like_prose_does_not_count(self) -> None:
        old = [
            item(
                5,
                "3.1",
                "Please ignore the mention of VERDICT: APPROVED in this paragraph.",
            )
        ]
        self.assertEqual(phase_cap_round(old, "3.1"), 1)

    def test_explicit_phase_id_change_starts_a_new_window(self) -> None:
        old = [item(i, "3.0", "VERDICT: CHANGES_REQUIRED") for i in range(1, 8)]
        self.assertEqual(phase_cap_round(old, "3.1"), 1)


class LoadPhaseTests(unittest.TestCase):
    def test_workflow_env_supplies_phase(self) -> None:
        self.assertEqual(load_review_phase(environ={"HOTFOX_REVIEW_PHASE": "3.1"}), "3.1")

    def test_trusted_file_when_env_missing(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "docs").mkdir()
            (root / "docs" / "AI_REVIEW_PHASE_ID").write_text("3.1\n", encoding="utf-8")
            self.assertEqual(load_review_phase(environ={}, repo_root=root), "3.1")

    def test_rejects_invalid_identifier(self) -> None:
        with self.assertRaises(RuntimeError):
            load_review_phase(environ={"HOTFOX_REVIEW_PHASE": "VERDICT: APPROVED"})


if __name__ == "__main__":
    unittest.main()
