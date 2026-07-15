#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [validate_service] $*"
}

ENV_FILE="/etc/plate-main.env"
SERVER_PORT="$(grep -E "^[[:space:]]*SERVER_PORT=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
SERVER_PORT="${SERVER_PORT:-8090}"
APP_URL="http://127.0.0.1:${SERVER_PORT}/actuator/health/readiness"
CORS_SMOKE_ORIGIN="$(grep -E "^[[:space:]]*CORS_SMOKE_ORIGIN=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
MAX_RETRIES="$(grep -E "^[[:space:]]*HEALTH_MAX_RETRIES=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
SLEEP_SECONDS="$(grep -E "^[[:space:]]*HEALTH_SLEEP_SECONDS=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
CURL_TIMEOUT_SECONDS="$(grep -E "^[[:space:]]*HEALTH_CURL_TIMEOUT_SECONDS=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2- | tr -d '"' || true)"
MAX_RETRIES="${MAX_RETRIES:-90}"
SLEEP_SECONDS="${SLEEP_SECONDS:-2}"
CURL_TIMEOUT_SECONDS="${CURL_TIMEOUT_SECONDS:-5}"

log "Checking systemd active state for plate-main."
if ! systemctl is-active --quiet plate-main; then
  log "plate-main is not active before health checks."
  systemctl status plate-main -l --no-pager || true
  journalctl -u plate-main -n 200 --no-pager || true
  exit 1
fi
log "plate-main is active. Starting DB-aware readiness checks: ${APP_URL} max_retries=${MAX_RETRIES} sleep_seconds=${SLEEP_SECONDS} curl_timeout_seconds=${CURL_TIMEOUT_SECONDS}"

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
    log "Readiness check succeeded."
    if [ -n "$CORS_SMOKE_ORIGIN" ]; then
      cors_code="$(curl -sS -o /dev/null -D /tmp/plate-main-cors.headers -w "%{http_code}" \
        --max-time "$CURL_TIMEOUT_SECONDS" -X OPTIONS \
        -H "Origin: ${CORS_SMOKE_ORIGIN}" \
        -H "Access-Control-Request-Method: GET" \
        -H "Access-Control-Request-Headers: authorization,content-type" \
        "http://127.0.0.1:${SERVER_PORT}/api/admin/stores" || true)"
      if [ "$cors_code" != "200" ] || ! grep -Fqi "access-control-allow-origin: ${CORS_SMOKE_ORIGIN}" /tmp/plate-main-cors.headers; then
        log "CORS preflight failed for ${CORS_SMOKE_ORIGIN} with http_code=${cors_code}."
        cat /tmp/plate-main-cors.headers || true
        exit 1
      fi
      log "CORS preflight succeeded for ${CORS_SMOKE_ORIGIN}."
    fi
    exit 0
  fi
  log "Readiness check failed with http_code=${http_code}. Sleeping ${SLEEP_SECONDS}s before retry."
  sleep "$SLEEP_SECONDS"
done

log "Readiness check failed after ${MAX_RETRIES} attempts: $APP_URL"
if [ -s /tmp/plate-main-health.out ]; then
  log "Last health response body:"
  tail -c 2000 /tmp/plate-main-health.out || true
fi
systemctl status plate-main -l --no-pager || true
journalctl -u plate-main -n 200 --no-pager || true
exit 1
