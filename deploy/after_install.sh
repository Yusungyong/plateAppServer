#!/bin/bash
set -euo pipefail

APP_DIR="/opt/plate-main"
JAR_FILE="$(ls -1t ${APP_DIR}/*.jar | head -n1)"

if [ -z "$JAR_FILE" ]; then
  echo "No jar found in ${APP_DIR}"
  exit 1
fi

mv "$JAR_FILE" "${APP_DIR}/app.jar"
chown ec2-user:ec2-user "${APP_DIR}/app.jar"
