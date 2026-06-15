#!/bin/bash
set -uo pipefail

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S %Z')] [disk_maintenance] $*"
}

CODEDEPLOY_ROOT="${CODEDEPLOY_ROOT:-/opt/codedeploy-agent/deployment-root}"
JOURNAL_CONFIG="${JOURNAL_CONFIG:-/etc/systemd/journald.conf.d/plate-main.conf}"
MAINTENANCE_STAMP="${MAINTENANCE_STAMP:-/run/plate-disk-maintenance.stamp}"
SKIP_SYSTEM_CLEANUP="${DISK_MAINTENANCE_SKIP_SYSTEM_CLEANUP:-false}"
KEEP_DEPLOYMENTS=2
MAX_REMOVALS=5
MIN_INTERVAL_SECONDS=600

log_disk_usage() {
  log "Root filesystem usage:"
  df -h / || true

  if [ -d "$CODEDEPLOY_ROOT" ]; then
    log "CodeDeploy archive usage: $(du -sh "$CODEDEPLOY_ROOT" 2>/dev/null | cut -f1 || echo unknown)"
  fi
}

configure_journal_retention() {
  local config_dir
  local temp_config

  config_dir="$(dirname "$JOURNAL_CONFIG")"
  mkdir -p "$config_dir" || {
    log "Warning: could not create ${config_dir}."
    return
  }

  temp_config="$(mktemp "${JOURNAL_CONFIG}.tmp.XXXXXX")" || {
    log "Warning: could not create a temporary journald configuration."
    return
  }

  cat > "$temp_config" <<'EOF'
[Journal]
SystemMaxUse=200M
RuntimeMaxUse=50M
SystemKeepFree=1G
MaxRetentionSec=14day
EOF

  if [ ! -f "$JOURNAL_CONFIG" ] || ! cmp -s "$temp_config" "$JOURNAL_CONFIG"; then
    install -o root -g root -m 0644 "$temp_config" "$JOURNAL_CONFIG" || {
      log "Warning: could not install ${JOURNAL_CONFIG}."
      rm -f "$temp_config"
      return
    }
    restorecon "$JOURNAL_CONFIG" 2>/dev/null || true
    systemctl restart systemd-journald || log "Warning: could not restart systemd-journald."
    log "Configured systemd journal retention."
  fi

  rm -f "$temp_config"
  timeout 20s journalctl --rotate || log "Warning: journal rotation did not complete."
  timeout 20s journalctl --vacuum-size=200M --vacuum-time=14d \
    || log "Warning: journal cleanup did not complete."
}

cleanup_old_codedeploy_revisions() {
  local deployment_group_id="${DEPLOYMENT_GROUP_ID:-}"
  local deployment_id="${DEPLOYMENT_ID:-}"
  local group_dir
  local group_real
  local root_real
  local script_real
  local protected_dir
  local retained=0
  local removed=0
  local entry
  local candidate
  local candidate_real
  local candidate_id
  local -a revisions=()

  if [ -z "$deployment_group_id" ]; then
    log "Deployment group ID is unavailable. Skipping archive cleanup."
    return
  fi

  group_dir="${CODEDEPLOY_ROOT}/${deployment_group_id}"
  if [ ! -d "$group_dir" ]; then
    log "CodeDeploy deployment group directory was not found: ${group_dir}"
    return
  fi

  root_real="$(realpath -e "$CODEDEPLOY_ROOT" 2>/dev/null || true)"
  group_real="$(realpath -e "$group_dir" 2>/dev/null || true)"
  script_real="$(realpath -e "${BASH_SOURCE[0]}" 2>/dev/null || true)"
  protected_dir="$(dirname "$(dirname "$(dirname "$script_real")")")"

  if [ -z "$root_real" ] || [ -z "$group_real" ] || [[ "$group_real" != "$root_real/"* ]]; then
    log "Warning: CodeDeploy archive paths could not be verified. Skipping cleanup."
    return
  fi

  mapfile -t revisions < <(
    find "$group_real" -mindepth 1 -maxdepth 1 -type d -name 'd-*' \
      -printf '%T@ %p\n' 2>/dev/null | sort -nr
  )

  for entry in "${revisions[@]}"; do
    candidate="${entry#* }"
    candidate_real="$(realpath -e "$candidate" 2>/dev/null || true)"
    candidate_id="$(basename "$candidate_real")"

    if [ -z "$candidate_real" ] || [[ "$candidate_real" != "$group_real"/d-* ]]; then
      log "Warning: refusing to remove an unverified path: ${candidate}"
      continue
    fi

    if [ "$candidate_real" = "$protected_dir" ] || [ "$candidate_id" = "$deployment_id" ]; then
      retained=$((retained + 1))
      log "Keeping active CodeDeploy revision: ${candidate_id}"
      continue
    fi

    if [ "$retained" -lt "$KEEP_DEPLOYMENTS" ]; then
      retained=$((retained + 1))
      log "Keeping recent CodeDeploy revision: ${candidate_id}"
      continue
    fi

    if [ "$removed" -ge "$MAX_REMOVALS" ]; then
      log "Removal limit reached. Remaining old revisions will be handled by a later deployment."
      break
    fi

    log "Removing old CodeDeploy revision: ${candidate_id}"
    if timeout 10s rm -rf --one-file-system -- "$candidate_real"; then
      removed=$((removed + 1))
    else
      log "Warning: could not remove ${candidate_real}."
    fi
  done
}

maintenance_ran_recently() {
  local now
  local last_run

  if [ ! -f "$MAINTENANCE_STAMP" ]; then
    return 1
  fi

  now="$(date +%s)"
  last_run="$(stat -c %Y "$MAINTENANCE_STAMP" 2>/dev/null || echo 0)"
  [ $((now - last_run)) -lt "$MIN_INTERVAL_SECONDS" ]
}

cleanup_package_cache() {
  if command -v dnf >/dev/null 2>&1; then
    timeout 20s dnf clean all || log "Warning: dnf cache cleanup did not complete."
  fi

  if command -v systemd-tmpfiles >/dev/null 2>&1; then
    timeout 20s systemd-tmpfiles --clean \
      || log "Warning: temporary file cleanup did not complete."
  fi
}

if maintenance_ran_recently; then
  log "Disk maintenance ran within the last ${MIN_INTERVAL_SECONDS}s. Skipping duplicate work."
  exit 0
fi

log "Starting non-blocking disk maintenance."
log_disk_usage
if [ "$SKIP_SYSTEM_CLEANUP" != "true" ]; then
  configure_journal_retention
fi
cleanup_old_codedeploy_revisions
if [ "$SKIP_SYSTEM_CLEANUP" != "true" ]; then
  cleanup_package_cache
fi
log_disk_usage
touch "$MAINTENANCE_STAMP" || true
log "Disk maintenance completed."
exit 0
