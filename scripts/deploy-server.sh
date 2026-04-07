#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$DEPLOY_DIR"

if [[ ! -f "ss.env" ]]; then
  echo "ss.env not found in ${DEPLOY_DIR}"
  exit 1
fi

echo "[deploy] starting docker compose deployment"
docker compose --env-file ss.env up -d --build --wait

echo "[deploy] current service status"
docker compose --env-file ss.env ps
