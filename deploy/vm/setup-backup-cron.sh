#!/usr/bin/env bash
# Install the daily DB backup cron (idempotent) and run one test backup.
set -uo pipefail

BVM=/opt/docker/resafetytool/deploy/vm
chmod +x "$BVM/backup.sh" 2>/dev/null || true

echo "== cron available? =="
command -v crontab >/dev/null 2>&1 && echo "crontab: yes" || { echo "crontab: NO (install cron)"; }

CRON_LINE="0 3 * * * ${BVM}/backup.sh >> ${BVM}/backups/backup.log 2>&1"
( crontab -l 2>/dev/null | grep -v 'deploy/vm/backup.sh' ; echo "$CRON_LINE" ) | crontab -
echo "== installed crontab (backup line) =="
crontab -l 2>/dev/null | grep 'backup.sh' || echo "(failed to read crontab)"

echo "== run a test backup now =="
"$BVM/backup.sh"

echo "== backups dir =="
ls -la "$BVM/backups/" 2>/dev/null
echo "BACKUP_SETUP_DONE"
