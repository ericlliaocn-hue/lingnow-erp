#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "Missing .env. Copy .env.example to .env and replace CHANGE_ME values." >&2
  exit 1
fi

if grep -q 'CHANGE_ME' .env; then
  echo ".env still contains CHANGE_ME placeholders." >&2
  exit 1
fi

docker compose --env-file .env up -d --build
docker compose --env-file .env ps
