#!/bin/bash
set -euo pipefail

APP_DIR="/opt/plate-main"
SERVICE_FILE="/etc/systemd/system/plate-main.service"

if [ ! -f "$SERVICE_FILE" ]; then
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
fi

systemctl daemon-reload
systemctl enable plate-main
systemctl restart plate-main
