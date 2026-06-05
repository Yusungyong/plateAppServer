#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [validate_service] $*"
}

APP_URL="http://127.0.0.1:8090/api/health"
MAX_RETRIES=30
SLEEP_SECONDS=2

log "Checking systemd active state for plate-main."
systemctl is-active --quiet plate-main
log "plate-main is active. Starting health checks: ${APP_URL}"

for ((i=1; i<=MAX_RETRIES; i++)); do
  log "Health check attempt ${i}/${MAX_RETRIES}"
  if curl -fsS --max-time 3 "$APP_URL" > /dev/null; then
    log "Health check succeeded."
    exit 0
  fi
  log "Health check failed. Sleeping ${SLEEP_SECONDS}s before retry."
  sleep "$SLEEP_SECONDS"
done

log "Health check failed after ${MAX_RETRIES} attempts: $APP_URL"
systemctl status plate-main -l --no-pager || true
journalctl -u plate-main -n 200 --no-pager || true
exit 1
