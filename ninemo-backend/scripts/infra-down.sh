#!/usr/bin/env bash
# Stop the local infrastructure. Pass -v to also drop the data volumes
# (wipes Postgres/Mongo data and forces the init scripts to re-run).
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BACKEND_DIR"

docker compose \
  --project-directory infrastructure \
  -f infrastructure/docker-compose.infra.yml \
  down "$@"
