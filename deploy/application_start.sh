#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [application_start] $*"
}

APP_DIR="/opt/plate-main"
SERVICE_FILE="/etc/systemd/system/plate-main.service"
ENV_FILE="/etc/plate-main.env"

required_env_keys=(JWT_SECRET)
missing_env_keys=()
for key in "${required_env_keys[@]}"; do
  value="$(grep -E "^[[:space:]]*${key}=" "$ENV_FILE" 2>/dev/null | tail -n1 | cut -d= -f2- || true)"
  if [ -z "$value" ]; then
    missing_env_keys+=("$key")
  fi
done

if [ ${#missing_env_keys[@]} -gt 0 ]; then
  log "Missing required environment keys in ${ENV_FILE}: ${missing_env_keys[*]}"
  exit 1
fi

optional_social_env_keys=(APPLE_CLIENT_ID GOOGLE_CLIENT_ID)
missing_optional_social_env_keys=()
for key in "${optional_social_env_keys[@]}"; do
  if ! grep -Eq "^[[:space:]]*${key}=" "$ENV_FILE" 2>/dev/null; then
    missing_optional_social_env_keys+=("$key")
  fi
done

if [ ${#missing_optional_social_env_keys[@]} -gt 0 ]; then
  log "Optional social login environment keys are missing: ${missing_optional_social_env_keys[*]}. Social login may be unavailable."
fi

log "Writing systemd service file: ${SERVICE_FILE}"
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
Restart=on-failure
RestartSec=5
TimeoutStartSec=90
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

log "Reloading systemd daemon."
systemctl daemon-reload
log "Enabling plate-main service."
systemctl enable plate-main
log "Restarting plate-main service."
systemctl restart plate-main
log "plate-main restart command completed."
systemctl status plate-main --no-pager -l || true
log "ApplicationStart hook completed."
