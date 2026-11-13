#!/usr/bin/env bash
#
# Build path C - build the image LOCALLY and transfer it to the VM over SSH, so
# the VM does not need build-registry egress (Docker Hub / Maven / npm / Red Hat).
#
# Run from your workstation with VPN/network access to the VM. Requires Docker
# locally and SSH access to the VM (the VM user must be in the `docker` group).
#
# Usage:
#   deploy/vm/transfer-image.sh <user>@<vm-host>
#
# After it finishes, on the VM:
#   cd /opt/docker/resafetytool && docker compose up -d
set -euo pipefail

REMOTE="${1:?Usage: transfer-image.sh <user@host>  (e.g. deploy@vm-host.example.com)}"
APP_IMAGE="${APP_IMAGE:-resafetytool:latest}"
DB_IMAGE="${DB_IMAGE:-postgres:16}"

# Repo root = two levels up from this script (deploy/vm/ -> repo root).
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo ">> Building ${APP_IMAGE} from ${REPO_ROOT} ..."
docker build -t "${APP_IMAGE}" "${REPO_ROOT}"

echo ">> Pulling ${DB_IMAGE} locally so it can be transferred too ..."
docker pull "${DB_IMAGE}"

echo ">> Streaming images to ${REMOTE} (compressed; this can take a while) ..."
docker save "${APP_IMAGE}" "${DB_IMAGE}" | gzip | ssh "${REMOTE}" 'gunzip | docker load'

echo ">> Done. Now on the VM:  cd /opt/docker/resafetytool && docker compose up -d"
