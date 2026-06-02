#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "Missing .env." >&2
  exit 1
fi

set -a
source .env
set +a

backup_dir="${BACKUP_DIR:-./backups/$(date +%Y%m%d_%H%M%S)}"
mkdir -p "$backup_dir"

docker compose --env-file .env exec -T mysql \
  mysqldump -u"${MYSQL_USER:-lingnow}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE:-lingnow_erp}" \
  > "$backup_dir/mysql.sql"

docker run --rm \
  -v lingnow-erp_app_files:/data/files:ro \
  -v "$(pwd)/$backup_dir:/backup" \
  alpine tar czf /backup/files.tar.gz -C /data files

echo "Backup written to $backup_dir"
