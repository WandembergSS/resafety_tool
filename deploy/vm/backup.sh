#!/usr/bin/env bash
#
# Daily PostgreSQL backup for the RESafety Tool VM deployment. Writes a gzipped
# pg_dump into ./backups and prunes dumps older than RETENTION_DAYS (default 14).
#
# Self-managed backups replace Cloud SQL's automated ones.
#
# Run manually:  ./backup.sh
# Cron (as the VM app user, e.g. wss2 -- run `crontab -e`):
#   0 3 * * * cd /opt/docker/resafetytool && ./backup.sh >> backups/backup.log 2>&1
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${DIR}/backups"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
DB_CONTAINER="${DB_CONTAINER:-resafetytool-db}"

# Read just the DB name/user from .env (avoid sourcing the whole secrets file).
DB_NAME="$(grep -E '^DB_NAME=' "${DIR}/.env" 2>/dev/null | head -n1 | cut -d= -f2-)"
DB_USER="$(grep -E '^DB_USER=' "${DIR}/.env" 2>/dev/null | head -n1 | cut -d= -f2-)"
DB_NAME="${DB_NAME:-resafetytool}"
DB_USER="${DB_USER:-resafetytool}"

mkdir -p "${BACKUP_DIR}"
OUT="${BACKUP_DIR}/${DB_NAME}-$(date +%F_%H%M%S).sql.gz"

echo "$(date -Is) starting backup -> ${OUT}"
docker exec "${DB_CONTAINER}" pg_dump -U "${DB_USER}" "${DB_NAME}" | gzip > "${OUT}"
echo "$(date -Is) backup complete ($(du -h "${OUT}" | cut -f1))"

find "${BACKUP_DIR}" -name "${DB_NAME}-*.sql.gz" -type f -mtime "+${RETENTION_DAYS}" -delete
echo "$(date -Is) pruned backups older than ${RETENTION_DAYS} days"
