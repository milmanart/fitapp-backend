#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

echo '== build jar =='
./mvnw -q -DskipTests clean package

echo '== build image (single-platform, no provenance) =='
cp target/fitapp-backend-0.0.1-SNAPSHOT.jar app.jar
docker build --provenance=false --sbom=false -t fitapp:latest .

echo '== recreate container =='
docker rm -f fitapp-backend 2>/dev/null || true
docker run -d --name fitapp-backend   --network fitapp_fitapp-network   --network-alias app   -p 8080:8080   --env-file ./backend.env   -v /opt/fitapp/logs:/app/logs   --restart unless-stopped   fitapp:latest

echo '== wait for health =='
for i in $(seq 1 30); do
  c=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/health || echo 000)
  [ "$c" = '200' ] && { echo 'health=200 OK'; exit 0; }
  sleep 2
done
echo 'WARNING: health check did not pass in time'; exit 1
