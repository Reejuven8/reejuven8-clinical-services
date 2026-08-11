#!/usr/bin/env bash
# Start the local infrastructure: PostgreSQL, MongoDB, Redis, Kafka, RabbitMQ,
# Prometheus, Zipkin, Grafana. Services (run-service.sh) expect these on localhost.
#
# NOTE: --project-directory is required. The compose file lives in infrastructure/
# and its volume paths (./init-scripts/...) resolve relative to that directory.
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$BACKEND_DIR"

if ! docker info >/dev/null 2>&1; then
  echo "docker is not running — start OrbStack/Docker Desktop first (orb start)."
  exit 1
fi

docker compose \
  --project-directory infrastructure \
  -f infrastructure/docker-compose.infra.yml \
  up -d "$@"

echo
echo "==> waiting for health ..."
sleep 5
docker ps --format '{{.Names}}\t{{.Status}}' | sort

cat <<'EOF'

PostgreSQL  localhost:5432   reejuven8 / dev_password
MongoDB     localhost:27017  reejuven8 / dev_password  (authSource=admin)
Redis       localhost:6379   dev_password
Kafka       localhost:9092
RabbitMQ    localhost:5672   (management UI :15672)
EOF
