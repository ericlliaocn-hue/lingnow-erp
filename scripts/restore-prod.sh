#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

backup_dir="${1:-}"
if [[ -z "$backup_dir" || ! -f "$backup_dir/mysql.sql" ]]; then
  echo "Usage: scripts/restore-prod.sh <backup-dir>" >&2
  exit 1
fi

set -a
source .env
set +a

docker compose --env-file .env exec -T mysql \
  mysql -u"${MYSQL_USER:-lingnow}" -p"${MYSQL_PASSWORD}" "${MYSQL_DATABASE:-lingnow_erp}" \
  < "$backup_dir/mysql.sql"

if [[ -f "$backup_dir/files.tar.gz" ]]; then
  docker run --rm \
    -v lingnow-erp_app_files:/data \
    -v "$(pwd)/$backup_dir:/backup" \
    alpine sh -c 'rm -rf /data/files && tar xzf /backup/files.tar.gz -C /data'
fi

echo "Restore completed from $backup_dir"
