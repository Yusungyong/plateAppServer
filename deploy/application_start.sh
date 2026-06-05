#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [application_start] $*"
}

APP_DIR="/opt/plate-main"
SERVICE_FILE="/etc/systemd/system/plate-main.service"

if [ ! -f "$SERVICE_FILE" ]; then
  log "Systemd service file does not exist. Creating ${SERVICE_FILE}"
  cat <<'EOF' > "$SERVICE_FILE"
[Unit]
Description=plate-main service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/plate-main
EnvironmentFile=/etc/plate-main.env
ExecStart=/usr/bin/java -jar /opt/plate-main/app.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
else
  log "Systemd service file already exists: ${SERVICE_FILE}"
fi

log "Reloading systemd daemon."
systemctl daemon-reload
log "Enabling plate-main service."
systemctl enable plate-main
log "Restarting plate-main service."
systemctl restart plate-main
log "plate-main restart command completed."
systemctl status plate-main --no-pager -l || true
log "ApplicationStart hook completed."
