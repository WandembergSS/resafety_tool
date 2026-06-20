#!/usr/bin/env bash
#
# Phase 0 egress probe - run ON the CIn VM (vm-resafetytool.cin.ufpe.br).
#
# Tests whether the VM can reach the registries needed to BUILD the image, and
# whether it can reach the Gemini API needed at RUNTIME, then prints a recommended
# build path. Read-only: it makes no changes.
#
# Usage (on the VM):  ./egress-probe.sh
set -uo pipefail

probe() {  # $1 = label, $2 = url
  local label="$1" url="$2" code
  code="$(curl -sS -o /dev/null -m 10 -w '%{http_code}' "$url" 2>/dev/null || echo 000)"
  # A reachable endpoint may answer 2xx/3xx or an auth challenge (401/403).
  # Only 000 (timeout/refused) means the host is unreachable / blocked.
  case "$code" in
    2*|3*|401|403) printf '  OK    (%s)  %s\n' "$code" "$label"; return 0 ;;
    *)             printf '  BLOCK (%s)  %s\n' "$code" "$label"; return 1 ;;
  esac
}

echo "== Build-time registries =="
build_ok=0
probe "Docker Hub"    "https://registry-1.docker.io/v2/"      || build_ok=1
probe "Maven Central" "https://repo1.maven.org/maven2/"       || build_ok=1
probe "npm registry"  "https://registry.npmjs.org/"           || build_ok=1
probe "Red Hat UBI"   "https://registry.access.redhat.com/"   || build_ok=1

echo "== Runtime egress =="
gemini_ok=0
probe "Gemini API"    "https://generativelanguage.googleapis.com/" || gemini_ok=1

echo
if [ "$build_ok" -eq 0 ]; then
  echo ">> Build path A: registries reachable - build on the VM:"
  echo "     docker compose build app && docker compose up -d"
else
  echo ">> Build path C: a registry is blocked - build elsewhere and transfer the image"
  echo "   (run on your workstation, on the CIn VPN):"
  echo "     deploy/vm/transfer-image.sh wss2@vm-resafetytool.cin.ufpe.br"
  echo "   then on the VM:  docker compose up -d"
fi

if [ "$gemini_ok" -ne 0 ]; then
  echo ">> Gemini egress BLOCKED - AI features will fail. Ask CIn helpdesk to allow"
  echo "   outbound 443 to generativelanguage.googleapis.com. The tool still runs without AI."
fi
