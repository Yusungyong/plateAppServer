#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [validate_service] $*"
}

ENV_FILE="/etc/plate-main.env"
SERVER_PORT="$(grep -E "^[[:space:]]*SERVER_PORT=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
SERVER_PORT="${SERVER_PORT:-8090}"
APP_URL="http://127.0.0.1:${SERVER_PORT}/api/health"
MAX_RETRIES=30
SLEEP_SECONDS=2
CURL_TIMEOUT_SECONDS=2

log "Checking systemd active state for plate-main."
if ! systemctl is-active --quiet plate-main; then
  log "plate-main is not active before health checks."
  systemctl status plate-main -l --no-pager || true
  journalctl -u plate-main -n 200 --no-pager || true
  exit 1
fi
log "plate-main is active. Starting health checks: ${APP_URL}"

for ((i=1; i<=MAX_RETRIES; i++)); do
  log "Health check attempt ${i}/${MAX_RETRIES}"
  if systemctl is-failed --quiet plate-main; then
    log "plate-main entered failed state during health checks."
    systemctl status plate-main -l --no-pager || true
    journalctl -u plate-main -n 200 --no-pager || true
    exit 1
  fi

  if ! systemctl is-active --quiet plate-main; then
    log "plate-main is no longer active during health checks."
    systemctl status plate-main -l --no-pager || true
    journalctl -u plate-main -n 200 --no-pager || true
    exit 1
  fi

  http_code="$(curl -sS -o /tmp/plate-main-health.out -w "%{http_code}" --max-time "$CURL_TIMEOUT_SECONDS" "$APP_URL" || true)"
  if [ "$http_code" = "200" ]; then
    log "Health check succeeded."
    exit 0
  fi
  log "Health check failed with http_code=${http_code}. Sleeping ${SLEEP_SECONDS}s before retry."
  sleep "$SLEEP_SECONDS"
done

log "Health check failed after ${MAX_RETRIES} attempts: $APP_URL"
if [ -s /tmp/plate-main-health.out ]; then
  log "Last health response body:"
  tail -c 2000 /tmp/plate-main-health.out || true
fi
systemctl status plate-main -l --no-pager || true
journalctl -u plate-main -n 200 --no-pager || true
exit 1
