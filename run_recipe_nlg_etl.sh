#!/usr/bin/env bash
set -euo pipefail

CSV_PATH="${1:-}"
if [[ -z "$CSV_PATH" ]]; then
  echo "Usage: $0 /absolute/path/to/full_dataset.csv" >&2
  exit 2
fi

if [[ ! -f "$CSV_PATH" ]]; then
  echo "CSV not found: $CSV_PATH" >&2
  exit 2
fi

cd "$(dirname "$0")"

./mvnw -DskipTests package

java -jar target/fitapp-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=etl \
  --etl.recipenlg.csv-path="$CSV_PATH" \
  --etl.recipenlg.top-dishes=5000 \
  --etl.recipenlg.min-recipes-per-dish=50 \
  --etl.recipenlg.min-ingredient-support=0.4 \
  --etl.recipenlg.max-ingredients=8 \
  --etl.recipenlg.default-serving-g=300 \
  --etl.recipenlg.truncate=true
