#!/usr/bin/env bash
set -euo pipefail

echo "== AI orchestration doctor =="
echo "repo: $(git rev-parse --show-toplevel 2>/dev/null || echo NOT_A_GIT_REPO)"
echo "branch: $(git branch --show-current 2>/dev/null || true)"
echo "head: $(git rev-parse --short HEAD 2>/dev/null || true)"

if command -v claude >/dev/null 2>&1; then
  echo "claude: $(claude --version 2>/dev/null | head -n1 || echo installed)"
else
  echo "claude: MISSING"
fi

if command -v agent >/dev/null 2>&1; then
  echo "cursor-agent: $(agent --version 2>/dev/null | head -n1 || echo installed)"
else
  echo "cursor-agent: MISSING"
fi

if [[ -n "${ANTHROPIC_API_KEY:-}" ]]; then
  echo "warning: ANTHROPIC_API_KEY is present in shell; subscription launcher will unset it"
else
  echo "ANTHROPIC_API_KEY: not present"
fi

if [[ -n "${CURSOR_API_KEY:-}" ]]; then
  echo "warning: CURSOR_API_KEY is present in shell; subscription launcher will unset it"
else
  echo "CURSOR_API_KEY: not present"
fi

echo "git status:"
git status --short 2>/dev/null || true
