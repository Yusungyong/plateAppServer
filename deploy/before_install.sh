#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [before_install] $*"
}

APP_DIR="/opt/plate-main"
ENV_FILE="/etc/plate-main.env"

log "Preparing application directory: ${APP_DIR}"
mkdir -p "$APP_DIR"
chown ec2-user:ec2-user "$APP_DIR"
log "Application directory is ready."

if [ ! -f "$ENV_FILE" ]; then
  log "Environment file does not exist. Creating ${ENV_FILE}"
  touch "$ENV_FILE"
  chmod 600 "$ENV_FILE"
  log "Environment file created."
else
  log "Environment file already exists: ${ENV_FILE}"
fi
log "BeforeInstall hook completed."
