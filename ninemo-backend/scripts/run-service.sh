#!/usr/bin/env bash
# Run one NineMo backend service as a standalone Spring Boot server.
#
#   scripts/run-service.sh <service-name> [--build]
#
# Every service defaults (application.yml) to localhost + the docker-compose infra
# credentials, so no env vars are needed — just have the infra running first:
#   scripts/infra-up.sh
#
# Written for bash 3.2 (stock macOS): no associative arrays.
set -euo pipefail

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SERVICE="${1:-}"
FORCE_BUILD="${2:-}"

port_for() {
  case "$1" in
    api-gateway)              echo 8080 ;;
    identity-abha-service)    echo 8081 ;;
    health-data-service)      echo 8082 ;;
    ai-parsing-service)       echo 8083 ;;
    ninemo-clinical-service)  echo 8084 ;;
    notification-service)     echo 8085 ;;
    ninemo-community-service) echo 8086 ;;
    *)                        echo "" ;;
  esac
}

if [ -z "$SERVICE" ] || [ -z "$(port_for "$SERVICE")" ]; then
  echo "usage: $(basename "$0") <service-name> [--build]"
  echo
  echo "services:"
  echo "  api-gateway               :8080  routing + JWT validation"
  echo "  identity-abha-service     :8081  auth, ABHA, consent      (PostgreSQL + Redis)"
  echo "  health-data-service       :8082  FHIR lake, file vault    (MongoDB + S3)"
  echo "  ai-parsing-service        :8083  OCR/NER  — Python, use run-ai-parsing.sh"
  echo "  ninemo-clinical-service   :8084  gestational engine       (PostgreSQL + MongoDB)"
  echo "  notification-service      :8085  WhatsApp/SMS/FCM/email   (PostgreSQL)"
  echo "  ninemo-community-service  :8086  clubs, STOMP chat        (MongoDB)"
  exit 1
fi

if [ "$SERVICE" = "ai-parsing-service" ]; then
  echo "ai-parsing-service is Python/FastAPI — use scripts/run-ai-parsing.sh instead."
  exit 1
fi

PORT="$(port_for "$SERVICE")"
JAR="$BACKEND_DIR/services/$SERVICE/target/$SERVICE-1.0.0-SNAPSHOT.jar"

# ── JDK ──────────────────────────────────────────────────────────────────────
# Backend targets Java 26 (Homebrew openjdk@26). It is not symlinked into
# /Library/Java/JavaVirtualMachines, so /usr/libexec/java_home cannot see it and
# the /usr/bin/java stub fails — JAVA_HOME must be set explicitly.
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
  for candidate in \
    /opt/homebrew/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home \
    /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
    /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  do
    if [ -x "$candidate/bin/java" ]; then JAVA_HOME="$candidate"; break; fi
  done
fi
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "ERROR: no JDK found. Install one:  brew install openjdk@26"
  echo "       or export JAVA_HOME=/path/to/jdk before running."
  exit 1
fi
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"

# ── port conflict ────────────────────────────────────────────────────────────
HOLDER="$(lsof -nP -iTCP:"$PORT" -sTCP:LISTEN -t 2>/dev/null | head -1 || true)"
if [ -n "$HOLDER" ]; then
  echo "ERROR: port $PORT already in use by pid $HOLDER"
  echo "       stop it with:  kill $HOLDER"
  exit 1
fi

# ── build if needed ──────────────────────────────────────────────────────────
if [ ! -f "$JAR" ] || [ "$FORCE_BUILD" = "--build" ]; then
  echo "==> building $SERVICE ..."
  ( cd "$BACKEND_DIR" && mvn -q -pl "services/$SERVICE" package -DskipTests )
fi
if [ ! -f "$JAR" ]; then
  echo "ERROR: jar not found after build: $JAR"
  exit 1
fi

# ── infra reachability hint (non-fatal) ──────────────────────────────────────
if ! docker info >/dev/null 2>&1; then
  echo "WARN: docker is not running — databases/brokers are down."
  echo "      start them first:  scripts/infra-up.sh"
fi

echo "==> $SERVICE  http://localhost:$PORT   (JDK: $("$JAVA" -version 2>&1 | head -1))"
echo "==> health:   http://localhost:$PORT/actuator/health"
exec "$JAVA" -jar "$JAR"
