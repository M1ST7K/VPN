"""Exact-head host CI evidence for the trusted HotFox AI reviewer.

The reviewer checks out trusted main and must not treat PR/docs claims as
build proof. This module fetches the bootstrap workflow record and the
`candidate-evidence.txt` artifact for the reviewed SHA via the GitHub API.
"""

from __future__ import annotations

import io
import zipfile
from dataclasses import dataclass, field

GITHUB_API = "https://api.github.com"
BOOTSTRAP_WORKFLOW_PATH = ".github/workflows/hotfox-bootstrap-ci.yml"
BOOTSTRAP_WORKFLOW_NAME = "HotFox bootstrap and Android CI"
EVIDENCE_ARTIFACT_NAME = "hotfox-playstore-debug-apk-sha256"
REQUIRED_JOBS = (
    "Payload integrity",
    "Reconstruct and build Android app",
)
PRESENT = "PRESENT"
MISSING = "MISSING"
MISMATCH = "MISMATCH"
FAILED = "FAILED"


@dataclass
class HostCiEvidence:
    sha: str
    status: str
    run_id: int | None = None
    run_url: str = ""
    event: str = ""
    conclusion: str = ""
    jobs: list[tuple[str, str]] = field(default_factory=list)
    candidate_sha: str = ""
    ref: str = ""
    apk_sha256: str = ""
    detail: str = ""


def parse_candidate_evidence(text: str) -> dict[str, str]:
    candidate_sha = ""
    ref = ""
    apk_lines: list[str] = []
    in_apk = False
    for raw in (text or "").splitlines():
        line = raw.strip()
        if not line:
            continue
        if line.startswith("candidate_sha="):
            candidate_sha = line.split("=", 1)[1].strip().lower()
            in_apk = False
            continue
        if line.startswith("ref="):
            ref = line.split("=", 1)[1].strip()
            in_apk = False
            continue
        if line == "apk_sha256:":
            in_apk = True
            continue
        if in_apk:
            apk_lines.append(line)
    return {
        "candidate_sha": candidate_sha,
        "ref": ref,
        "apk_sha256": "\n".join(apk_lines),
    }


def extract_candidate_evidence(zip_bytes: bytes) -> str:
    with zipfile.ZipFile(io.BytesIO(zip_bytes)) as archive:
        names = archive.namelist()
        for name in names:
            if name.endswith("candidate-evidence.txt") and not name.endswith("/"):
                return archive.read(name).decode("utf-8", errors="replace")
    raise RuntimeError("candidate-evidence.txt missing from artifact; entries=" + ",".join(names))


def select_bootstrap_run(runs: list[dict], sha: str) -> dict | None:
    wanted = sha.lower()
    matches: list[dict] = []
    for run in runs:
        if (run.get("head_sha") or "").strip().lower() != wanted:
            continue
        path = run.get("path") or ""
        name = run.get("name") or ""
        if path != BOOTSTRAP_WORKFLOW_PATH and name != BOOTSTRAP_WORKFLOW_NAME:
            continue
        matches.append(run)
    success_push = [
        run
        for run in matches
        if run.get("conclusion") == "success" and run.get("event") == "push"
    ]
    if success_push:
        return success_push[0]
    success = [run for run in matches if run.get("conclusion") == "success"]
    if success:
        return success[0]
    return matches[0] if matches else None


def required_jobs_succeeded(jobs: list[tuple[str, str]]) -> bool:
    by_name = {name: conclusion for name, conclusion in jobs}
    return all(by_name.get(name) == "success" for name in REQUIRED_JOBS)


def format_host_ci_evidence(record: HostCiEvidence) -> str:
    job_lines = "\n".join(f"- {name}: {conclusion}" for name, conclusion in record.jobs) or "- (none)"
    apk = record.apk_sha256 or "(none)"
    return "\n".join(
        [
            f"status={record.status}",
            f"reviewed_sha={record.sha}",
            f"candidate_sha={record.candidate_sha or '(none)'}",
            f"ref={record.ref or '(none)'}",
            f"run_id={record.run_id or '(none)'}",
            f"run_url={record.run_url or '(none)'}",
            f"event={record.event or '(none)'}",
            f"workflow_conclusion={record.conclusion or '(none)'}",
            f"required_jobs_ok={str(required_jobs_succeeded(record.jobs)).lower()}",
            "jobs:",
            job_lines,
            "apk_sha256:",
            apk,
            f"detail={record.detail or '(none)'}",
        ]
    )


def _job_pairs(payload: dict) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    for job in payload.get("jobs") or []:
        name = (job.get("name") or "").strip()
        if not name:
            continue
        out.append((name, (job.get("conclusion") or job.get("status") or "").strip()))
    return out


def fetch_host_ci_evidence(
    sha: str,
    *,
    repo: str,
    github_json,
    request_bytes,
) -> HostCiEvidence:
    """Load bootstrap CI + candidate-evidence.txt for an exact commit SHA."""
    record = HostCiEvidence(sha=sha.lower(), status=MISSING, detail="bootstrap run not found")
    runs_payload = github_json(f"/repos/{repo}/actions/runs?head_sha={sha}&per_page=30")
    run = select_bootstrap_run(runs_payload.get("workflow_runs") or [], sha)
    if run is None:
        return record

    record.run_id = run.get("id")
    record.run_url = run.get("html_url") or ""
    record.event = run.get("event") or ""
    record.conclusion = run.get("conclusion") or ""
    if record.run_id:
        record.jobs = _job_pairs(github_json(f"/repos/{repo}/actions/runs/{record.run_id}/jobs"))

    if record.conclusion != "success":
        record.status = FAILED
        record.detail = f"bootstrap conclusion={record.conclusion or 'unknown'}"
        return record

    artifacts = github_json(f"/repos/{repo}/actions/runs/{record.run_id}/artifacts")
    artifact_id = None
    for item in artifacts.get("artifacts") or []:
        if item.get("name") == EVIDENCE_ARTIFACT_NAME and not item.get("expired"):
            artifact_id = item.get("id")
            break
    if artifact_id is None:
        record.status = MISSING
        record.detail = f"artifact {EVIDENCE_ARTIFACT_NAME} missing from run {record.run_id}"
        return record

    zip_bytes = request_bytes(
        "GET",
        f"{GITHUB_API}/repos/{repo}/actions/artifacts/{artifact_id}/zip",
        accept="application/vnd.github+json",
    )
    parsed = parse_candidate_evidence(extract_candidate_evidence(zip_bytes))
    record.candidate_sha = parsed["candidate_sha"]
    record.ref = parsed["ref"]
    record.apk_sha256 = parsed["apk_sha256"]
    if record.candidate_sha != record.sha:
        record.status = MISMATCH
        record.detail = (
            f"candidate_sha {record.candidate_sha or '(empty)'} != reviewed {record.sha}"
        )
        return record
    if not record.apk_sha256.strip():
        record.status = MISSING
        record.detail = "candidate-evidence.txt has no apk_sha256 lines"
        return record
    if not required_jobs_succeeded(record.jobs):
        record.status = FAILED
        record.detail = "required bootstrap jobs are not all success"
        return record
    record.status = PRESENT
    record.detail = "exact-head bootstrap CI and APK checksums match reviewed SHA"
    return record
