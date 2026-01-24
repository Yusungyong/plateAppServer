#!/bin/bash
set -euo pipefail

if systemctl list-units --type=service --all | grep -q "plate-main.service"; then
  if systemctl is-active --quiet plate-main; then
    systemctl stop plate-main
  fi
fi
