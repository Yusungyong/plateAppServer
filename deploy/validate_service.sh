#!/bin/bash
set -euo pipefail

APP_URL="http://127.0.0.1:8090/api/health"
MAX_RETRIES=30
SLEEP_SECONDS=2

systemctl is-active --quiet plate-main

for ((i=1; i<=MAX_RETRIES; i++)); do
  if curl -fsS "$APP_URL" > /dev/null; then
    exit 0
  fi
  sleep "$SLEEP_SECONDS"
done

echo "Health check failed: $APP_URL"
systemctl status plate-main -l --no-pager || true
journalctl -u plate-main -n 200 --no-pager || true
exit 1
