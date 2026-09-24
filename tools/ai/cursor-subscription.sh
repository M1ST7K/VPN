#!/usr/bin/env bash
set -euo pipefail

# Force Cursor CLI to use account login rather than accidental API-key billing.
unset CURSOR_API_KEY

if ! command -v agent >/dev/null 2>&1; then
  echo "CURSOR_CLI_NOT_INSTALLED"
  exit 127
fi

exec agent "$@"
