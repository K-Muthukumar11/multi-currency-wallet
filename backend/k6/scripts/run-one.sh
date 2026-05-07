#!/usr/bin/env bash
# ============================================================
# scripts/run-one.sh
# Run a single test suite and open its HTML report.
#
# Usage:
#   ./scripts/run-one.sh smoke
#   ./scripts/run-one.sh auth
#   ./scripts/run-one.sh load
#   BASE_URL=http://myserver:8080 ./scripts/run-one.sh deposit
# ============================================================

set -euo pipefail

NAME="${1:-smoke}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
REPORTS_DIR="$ROOT_DIR/reports"

mkdir -p "$REPORTS_DIR"

# Map short name → file
declare -A FILE_MAP=(
  [smoke]="tests/00_smoke.test.js"
  [auth]="tests/01_auth.test.js"
  [accounts]="tests/02_accounts.test.js"
  [deposit]="tests/03_deposit.test.js"
  [transfer]="tests/04_transfer.test.js"
  [transactions]="tests/05_transactions.test.js"
  [reversals]="tests/06_reversals.test.js"
  [load]="tests/07_load.test.js"
)

TEST_FILE="${FILE_MAP[$NAME]:-}"
if [ -z "$TEST_FILE" ]; then
  echo "Unknown test suite: $NAME"
  echo "Available: ${!FILE_MAP[*]}"
  exit 1
fi

JSON_OUT="$REPORTS_DIR/${NAME}.json"
HTML_OUT="$REPORTS_DIR/${NAME}.html"

echo "▶  k6 run $ROOT_DIR/$TEST_FILE"
k6 run \
  --env BASE_URL="$BASE_URL" \
  --summary-export="$JSON_OUT" \
  "$ROOT_DIR/$TEST_FILE" || true

echo ""
echo "📊  Generating HTML report → $HTML_OUT"
node "$SCRIPT_DIR/generate-report.js" "$JSON_OUT" --out "$HTML_OUT"

echo "   Done: $HTML_OUT"
