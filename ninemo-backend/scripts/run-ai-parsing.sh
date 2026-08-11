#!/usr/bin/env bash
# Run ai-parsing-service (Python 3.11 + FastAPI) on :8083.
# Creates .venv and installs requirements on first run.
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SVC_DIR="$BACKEND_DIR/services/ai-parsing-service"
cd "$SVC_DIR"

if [ ! -d .venv ]; then
  echo "==> creating .venv ..."
  python3 -m venv .venv
  ./.venv/bin/pip install -q --upgrade pip
  ./.venv/bin/pip install -q -r requirements.txt
fi

echo "==> ai-parsing-service  http://localhost:8083"
exec ./.venv/bin/uvicorn app.main:app --host 0.0.0.0 --port 8083 --reload
