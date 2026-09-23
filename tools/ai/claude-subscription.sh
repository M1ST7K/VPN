#!/usr/bin/env bash
set -euo pipefail

# Force Claude Code to use native subscription auth rather than accidental API billing.
unset ANTHROPIC_API_KEY
unset ANTHROPIC_AUTH_TOKEN
unset CLAUDE_CODE_USE_BEDROCK
unset CLAUDE_CODE_USE_VERTEX
unset CLAUDE_CODE_USE_FOUNDRY

if ! command -v claude >/dev/null 2>&1; then
  echo "CLAUDE_CODE_NOT_INSTALLED"
  exit 127
fi

exec claude "$@"
