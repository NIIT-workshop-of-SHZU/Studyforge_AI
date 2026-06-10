#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/common.sh
source "$ROOT_DIR/scripts/lib/common.sh"
PORT="${PORT:-8080}"
PID_FILE="${PID_FILE:-/tmp/studyforge-api-maven.pid}"
LOG_FILE="${LOG_FILE:-/tmp/studyforge-api-maven.log}"
JETTY_PLUGIN_VERSION="${JETTY_PLUGIN_VERSION:-12.1.9}"

if [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "StudyForge API already running with PID $(cat "$PID_FILE")"
    echo "URL http://localhost:${PORT}/api/v1/health"
    exit 0
fi

if port_in_use "$PORT"; then
    echo "Port ${PORT} is already in use" >&2
    exit 1
fi

apply_local_proxy_if_available

cd "$ROOT_DIR/studyforge-server"

start_detached "$LOG_FILE" mvn -pl studyforge-webapi -am -DskipTests -Djetty.http.port="$PORT" \
    org.eclipse.jetty.ee10:jetty-ee10-maven-plugin:"$JETTY_PLUGIN_VERSION":run

echo "$!" > "$PID_FILE"

for _ in $(seq 1 60); do
    if curl -fsS "http://localhost:${PORT}/api/v1/health" >/dev/null 2>&1; then
        echo "StudyForge API started with PID $(cat "$PID_FILE")"
        echo "URL http://localhost:${PORT}/api/v1/health"
        echo "Log $LOG_FILE"
        exit 0
    fi

    if ! kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "StudyForge API failed to start. Log: $LOG_FILE" >&2
        tail -n 80 "$LOG_FILE" >&2 || true
        exit 1
    fi

    sleep 1
done

echo "StudyForge API is still starting. PID $(cat "$PID_FILE"), log $LOG_FILE"