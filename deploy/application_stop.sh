#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [application_stop] $*"
}

STOP_TIMEOUT_SECONDS=30
KILL_WAIT_SECONDS=5
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log "Running disk maintenance before downloading the next revision."
timeout 45s "${SCRIPT_DIR}/disk_maintenance.sh" \
  || log "Warning: disk maintenance did not complete within 45 seconds."

log "Checking plate-main service before deployment stop hook."
if ! systemctl cat plate-main.service >/dev/null 2>&1; then
  log "plate-main service is not installed yet. Nothing to stop."
  exit 0
fi

service_state="$(systemctl is-active plate-main || true)"
if [ "$service_state" = "inactive" ] || [ "$service_state" = "failed" ] || [ "$service_state" = "unknown" ]; then
  log "plate-main state is ${service_state}. Nothing to stop."
  exit 0
fi

log "plate-main state is ${service_state}. Requesting stop."
systemctl stop --no-block plate-main

for ((i=1; i<=STOP_TIMEOUT_SECONDS; i++)); do
  service_state="$(systemctl is-active plate-main || true)"
  if [ "$service_state" = "inactive" ] || [ "$service_state" = "failed" ] || [ "$service_state" = "unknown" ]; then
    log "plate-main stopped with state ${service_state} after ${i}s."
    exit 0
  fi

  if (( i % 5 == 0 )); then
    log "Waiting for plate-main to stop: ${i}/${STOP_TIMEOUT_SECONDS}s (state=${service_state})"
  fi
  sleep 1
done

log "Graceful stop exceeded ${STOP_TIMEOUT_SECONDS}s. Sending SIGKILL."
systemctl kill --kill-who=all --signal=SIGKILL plate-main || true

for ((i=1; i<=KILL_WAIT_SECONDS; i++)); do
  service_state="$(systemctl is-active plate-main || true)"
  if [ "$service_state" = "inactive" ] || [ "$service_state" = "failed" ] || [ "$service_state" = "unknown" ]; then
    log "plate-main stopped after forced termination with state ${service_state}."
    exit 0
  fi
  sleep 1
done

log "plate-main did not stop after forced termination. Final state: ${service_state}"
systemctl status plate-main --no-pager -l || true
exit 1
