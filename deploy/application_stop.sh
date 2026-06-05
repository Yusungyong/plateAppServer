#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [application_stop] $*"
}

log "Checking plate-main service before deployment stop hook."
if systemctl list-units --type=service --all | grep -q "plate-main.service"; then
  if systemctl is-active --quiet plate-main; then
    log "plate-main is active. Stopping service..."
    systemctl stop plate-main
    log "plate-main stop command completed."
  else
    log "plate-main service exists but is not active. Nothing to stop."
  fi
else
  log "plate-main service is not installed yet. Nothing to stop."
fi
log "ApplicationStop hook completed."
