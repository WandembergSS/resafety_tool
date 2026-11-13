#!/usr/bin/env bash
# Launch the stack and wait for the app to become healthy.
set -uo pipefail
cd /opt/docker/resafetytool/deploy/vm

echo "== existing project volumes =="
docker volume ls | grep -i resafety || echo "(none)"

echo "== docker compose up -d =="
docker compose up -d

echo "== wait for app health (up to ~150s) =="
st="n/a"
for i in $(seq 1 30); do
  st="$(docker inspect -f '{{.State.Health.Status}}' resafetytool-app 2>/dev/null || echo n/a)"
  echo "  [$i] app health: $st"
  [ "$st" = "healthy" ] && break
  [ "$st" = "unhealthy" ] && break
  sleep 5
done

echo "== docker compose ps =="
docker compose ps

echo "== db logs (tail) =="
docker compose logs --tail=15 db

echo "== app logs (tail) =="
docker compose logs --tail=50 app

echo "== local health on :80 =="
curl -sS -m 8 -o /dev/null -w 'health -> %{http_code}\n' http://localhost/q/health/ready || echo "curl failed"
echo "FINAL_APP_HEALTH=$st"
echo "LAUNCH_DONE"
