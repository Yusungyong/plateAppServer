#!/bin/bash
set -euo pipefail

APP_DIR="/opt/plate-main"
ENV_FILE="/etc/plate-main.env"

mkdir -p "$APP_DIR"
chown ec2-user:ec2-user "$APP_DIR"

if [ ! -f "$ENV_FILE" ]; then
  touch "$ENV_FILE"
  chmod 600 "$ENV_FILE"
fi
