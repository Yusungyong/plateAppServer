#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [application_start] $*"
}

print_service_diagnostics() {
  log "plate-main status"
  systemctl status plate-main --no-pager -l || true

  log "plate-main root-cause candidates from journal"
  journalctl -u plate-main -n 800 --no-pager -o cat 2>/dev/null \
    | grep -Ei "Caused by|Schema-validation|missing column|missing table|Flyway|Validate failed|BeanCreationException|PersistenceException|PSQLException|Exception|ERROR" \
    | tail -n 120 || true

  log "plate-main journal tail"
  journalctl -u plate-main -n 800 --no-pager || true
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
TimeoutStopSec=25
KillMode=control-group
SendSIGKILL=yes
SuccessExitStatus=143
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=full
ProtectKernelTunables=true
ProtectKernelModules=true
ProtectControlGroups=true
RestrictSUIDSGID=true
UMask=0027
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
if ! systemctl is-active --quiet plate-main; then
  log "plate-main is not active after restart."
  print_service_diagnostics
  exit 1
fi
systemctl status plate-main --no-pager -l || true
log "ApplicationStart hook completed."
