#!/bin/bash
set -euo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [after_install] $*"
}

APP_DIR="/opt/plate-main"

log "Looking for jar files in ${APP_DIR}"
shopt -s nullglob
jars=("${APP_DIR}"/*.jar)
if [ ${#jars[@]} -eq 0 ]; then
  echo "No jar found in ${APP_DIR}"
  exit 1
fi

JAR_FILE="$(ls -1t "${jars[@]}" | head -n1)"
log "Selected jar file: ${JAR_FILE}"

if [ "$JAR_FILE" != "${APP_DIR}/app.jar" ]; then
  log "Renaming ${JAR_FILE} to ${APP_DIR}/app.jar"
  mv "$JAR_FILE" "${APP_DIR}/app.jar"
else
  log "Jar is already named app.jar"
fi
chown ec2-user:ec2-user "${APP_DIR}/app.jar"
log "Jar ownership updated."
log "AfterInstall hook completed."
