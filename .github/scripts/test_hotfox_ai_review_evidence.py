#!/usr/bin/env python3
"""Unit tests for trusted exact-head CI evidence parsing/selection."""

from __future__ import annotations

import io
import unittest
import zipfile

from hotfox_ai_review_evidence import (
    FAILED,
    MISMATCH,
    MISSING,
    PRESENT,
    HostCiEvidence,
    extract_candidate_evidence,
    fetch_host_ci_evidence,
    format_host_ci_evidence,
    parse_candidate_evidence,
    required_jobs_succeeded,
    select_bootstrap_run,
)

SHA = "a729b1cbce719278840caf9ab09a5350e6d554e4"
OTHER = "775ff6c93fc922b97646b6d12a092a5218494067"


def evidence_text(sha: str = SHA) -> str:
    return (
        f"candidate_sha={sha}\n"
        "ref=cursor/hotfox-2.4-sync-rules-22f7\n"
        "apk_sha256:\n"
        "aaa  HotFox_Proxy_2.2.0_arm64-v8a.apk\n"
        "bbb  HotFox_Proxy_2.2.0_universal.apk\n"
    )


def zip_evidence(text: str) -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as archive:
        archive.writestr("candidate-evidence.txt", text)
        archive.writestr("apk-sha256.txt", "aaa  HotFox_Proxy_2.2.0_arm64-v8a.apk\n")
    return buf.getvalue()


class ParseTests(unittest.TestCase):
    def test_parse_candidate_evidence(self) -> None:
        parsed = parse_candidate_evidence(evidence_text())
        self.assertEqual(parsed["candidate_sha"], SHA)
        self.assertEqual(parsed["ref"], "cursor/hotfox-2.4-sync-rules-22f7")
        self.assertIn("arm64-v8a.apk", parsed["apk_sha256"])
        self.assertIn("universal.apk", parsed["apk_sha256"])

    def test_extract_from_zip(self) -> None:
        text = extract_candidate_evidence(zip_evidence(evidence_text()))
        self.assertIn(f"candidate_sha={SHA}", text)


class SelectRunTests(unittest.TestCase):
    def test_prefers_successful_push(self) -> None:
        runs = [
            {
                "id": 1,
                "head_sha": SHA,
                "path": ".github/workflows/hotfox-bootstrap-ci.yml",
                "name": "HotFox bootstrap and Android CI",
                "conclusion": "success",
                "event": "workflow_dispatch",
            },
            {
                "id": 2,
                "head_sha": SHA,
                "path": ".github/workflows/hotfox-bootstrap-ci.yml",
                "name": "HotFox bootstrap and Android CI",
                "conclusion": "success",
                "event": "push",
            },
            {
                "id": 3,
                "head_sha": OTHER,
                "path": ".github/workflows/hotfox-bootstrap-ci.yml",
                "name": "HotFox bootstrap and Android CI",
                "conclusion": "success",
                "event": "push",
            },
        ]
        selected = select_bootstrap_run(runs, SHA)
        self.assertIsNotNone(selected)
        self.assertEqual(selected["id"], 2)

    def test_required_jobs(self) -> None:
        self.assertTrue(
            required_jobs_succeeded(
                [
                    ("Payload integrity", "success"),
                    ("Reconstruct and build Android app", "success"),
                    ("Emulator UI smoke (manual, not VPN E2E)", "skipped"),
                ]
            )
        )
        self.assertFalse(
            required_jobs_succeeded(
                [("Payload integrity", "success"), ("Reconstruct and build Android app", "failure")]
            )
        )


class FormatTests(unittest.TestCase):
    def test_present_record_includes_checksums(self) -> None:
        text = format_host_ci_evidence(
            HostCiEvidence(
                sha=SHA,
                status=PRESENT,
                run_id=34538372425,
                run_url="https://github.com/M1ST7K/VPN/actions/runs/34538372425",
                event="push",
                conclusion="success",
                jobs=[
                    ("Payload integrity", "success"),
                    ("Reconstruct and build Android app", "success"),
                ],
                candidate_sha=SHA,
                ref="cursor/hotfox-2.4-sync-rules-22f7",
                apk_sha256="aaa  HotFox_Proxy_2.2.0_arm64-v8a.apk",
                detail="exact-head bootstrap CI and APK checksums match reviewed SHA",
            )
        )
        self.assertIn("status=PRESENT", text)
        self.assertIn(f"reviewed_sha={SHA}", text)
        self.assertIn(f"candidate_sha={SHA}", text)
        self.assertIn("required_jobs_ok=true", text)
        self.assertIn("aaa  HotFox_Proxy_2.2.0_arm64-v8a.apk", text)


class FetchTests(unittest.TestCase):
    def test_present_exact_head(self) -> None:
        def github_json(path: str):
            if path.startswith("/repos/o/r/actions/runs?") and "head_sha=" in path:
                return {
                    "workflow_runs": [
                        {
                            "id": 99,
                            "html_url": "https://example.test/99",
                            "head_sha": SHA,
                            "path": ".github/workflows/hotfox-bootstrap-ci.yml",
                            "name": "HotFox bootstrap and Android CI",
                            "conclusion": "success",
                            "event": "push",
                        }
                    ]
                }
            if path == "/repos/o/r/actions/runs/99/jobs":
                return {
                    "jobs": [
                        {"name": "Payload integrity", "conclusion": "success"},
                        {"name": "Reconstruct and build Android app", "conclusion": "success"},
                    ]
                }
            if path == "/repos/o/r/actions/runs/99/artifacts":
                return {"artifacts": [{"id": 7, "name": "hotfox-playstore-debug-apk-sha256", "expired": False}]}
            raise AssertionError(path)

        def request_bytes(method: str, url: str, **_kwargs):
            self.assertEqual(method, "GET")
            self.assertIn("/artifacts/7/zip", url)
            return zip_evidence(evidence_text())

        record = fetch_host_ci_evidence(
            SHA,
            repo="o/r",
            github_json=github_json,
            request_bytes=request_bytes,
        )
        self.assertEqual(record.status, PRESENT)
        self.assertEqual(record.candidate_sha, SHA)
        self.assertIn("universal.apk", record.apk_sha256)

    def test_mismatch_when_artifact_sha_differs(self) -> None:
        def github_json(path: str):
            if "actions/runs?" in path:
                return {
                    "workflow_runs": [
                        {
                            "id": 99,
                            "html_url": "https://example.test/99",
                            "head_sha": SHA,
                            "path": ".github/workflows/hotfox-bootstrap-ci.yml",
                            "name": "HotFox bootstrap and Android CI",
                            "conclusion": "success",
                            "event": "push",
                        }
                    ]
                }
            if path.endswith("/jobs"):
                return {
                    "jobs": [
                        {"name": "Payload integrity", "conclusion": "success"},
                        {"name": "Reconstruct and build Android app", "conclusion": "success"},
                    ]
                }
            if path.endswith("/artifacts"):
                return {"artifacts": [{"id": 7, "name": "hotfox-playstore-debug-apk-sha256", "expired": False}]}
            raise AssertionError(path)

        record = fetch_host_ci_evidence(
            SHA,
            repo="o/r",
            github_json=github_json,
            request_bytes=lambda *_args, **_kwargs: zip_evidence(evidence_text(OTHER)),
        )
        self.assertEqual(record.status, MISMATCH)

    def test_failed_run(self) -> None:
        def github_json(path: str):
            if "actions/runs?" in path:
                return {
                    "workflow_runs": [
                        {
                            "id": 5,
                            "html_url": "https://example.test/5",
                            "head_sha": SHA,
                            "path": ".github/workflows/hotfox-bootstrap-ci.yml",
                            "name": "HotFox bootstrap and Android CI",
                            "conclusion": "failure",
                            "event": "push",
                        }
                    ]
                }
            if path.endswith("/jobs"):
                return {"jobs": [{"name": "Payload integrity", "conclusion": "failure"}]}
            raise AssertionError(path)

        record = fetch_host_ci_evidence(
            SHA,
            repo="o/r",
            github_json=github_json,
            request_bytes=lambda *_args, **_kwargs: b"",
        )
        self.assertEqual(record.status, FAILED)

    def test_missing_when_no_run(self) -> None:
        record = fetch_host_ci_evidence(
            SHA,
            repo="o/r",
            github_json=lambda _path: {"workflow_runs": []},
            request_bytes=lambda *_args, **_kwargs: b"",
        )
        self.assertEqual(record.status, MISSING)


if __name__ == "__main__":
    unittest.main()
