#!/usr/bin/env bash
#
# Phase 1 bootstrap - run from a workstation with network access to the VM, piped into the VM:
#   ssh <user>@<vm-host> 'bash -s' < deploy/vm/bootstrap.sh
#
# Clones/updates the repo under /opt/docker/resafetytool, creates a .env skeleton
# with a generated DB password (GOOGLE_API_KEY and AUTH_USERS left empty for the
# operator to fill directly on the VM), then builds the app image.
set -euo pipefail

APP_DIR=/opt/docker/resafetytool
REPO="${REPO:-https://github.com/example/resafety_tool.git}"

echo "== 1. Clone/update repo =="
if [ -d "$APP_DIR/.git" ]; then
  git -C "$APP_DIR" fetch --depth 1 origin main
  git -C "$APP_DIR" reset --hard origin/main
else
  rm -rf "$APP_DIR"
  git clone --depth 1 --branch main "$REPO" "$APP_DIR"
fi

cd "$APP_DIR/deploy/vm"

echo "== 2. Create .env skeleton (if absent) =="
if [ ! -f .env ]; then
  cp .env.example .env
  DBPW="$(openssl rand -hex 24)"
  sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=${DBPW}|" .env
  sed -i "s|^GOOGLE_API_KEY=.*|GOOGLE_API_KEY=|" .env
  sed -i "s|^AUTH_USERS=.*|AUTH_USERS=|" .env
  chmod 600 .env
  echo ".env created (DB password generated; GOOGLE_API_KEY and AUTH_USERS empty)."
else
  echo ".env already exists -> preserved."
fi

echo "== 3. Build image (this can take several minutes) =="
docker compose build app
echo "BUILD_DONE_OK"
