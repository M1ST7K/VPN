# HotFox — ChatGPT-led execution entrypoint

Before any work:
1. Read `.ai/ORCHESTRATION.md`, `.ai/TASK.md` and `.ai/STATE.json`.
2. Read every file in `.cursor/rules/` that applies to the current HotFox task. Those existing HotFox engineering/release rules remain mandatory.
3. Perform only the active task scope on its assigned branch/worktree.
4. Do not merge main or deploy production unless the active task explicitly opens that gate.
5. At handoff, update `.ai/HANDOFF.md` with factual evidence, including tests/device checks that were actually run.

For difficult Android/VPN failures, investigate root cause across VpnService, tunnel/core, DNS, routing, lifecycle and device behavior rather than masking symptoms. Preserve existing security and release gates.
