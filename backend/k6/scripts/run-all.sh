#!/usr/bin/env bash
# ============================================================
# scripts/run-all.sh
# Runs every k6 test suite in order, collects JSON summaries,
# then generates a single HTML report.
#
# Prerequisites:
#   - k6 installed (https://k6.io/docs/get-started/installation/)
#   - node >= 14 installed
#   - API running at BASE_URL (default: http://localhost:8080)
#
# Usage:
#   chmod +x scripts/run-all.sh
#   ./scripts/run-all.sh                    # uses default BASE_URL
#   BASE_URL=http://myserver:8080 ./scripts/run-all.sh
# ============================================================

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
REPORTS_DIR="reports"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"

mkdir -p "$ROOT_DIR/$REPORTS_DIR"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║   Mini Wallet — k6 Test Suite Runner                ║"
echo "╚══════════════════════════════════════════════════════╝"
echo "  BASE_URL  : $BASE_URL"
echo "  Reports   : $ROOT_DIR/$REPORTS_DIR"
echo ""

# List of test files in execution order
declare -a TESTS=(
  "tests/00_smoke.test.js"
  "tests/01_auth.test.js"
  "tests/02_accounts.test.js"
  "tests/03_deposit.test.js"
  "tests/04_transfer.test.js"
  "tests/05_transactions.test.js"
  "tests/06_reversals.test.js"
)

LOAD_TEST="tests/07_load.test.js"

PASS_COUNT=0
FAIL_COUNT=0
JSON_FILES=()

run_test() {
  local test_file="$1"
  local name
  name=$(basename "$test_file" .test.js)
  local out_json="$ROOT_DIR/$REPORTS_DIR/${name}.json"

  echo "▶  Running: $test_file"

  set +e
  k6 run \
    --env BASE_URL="$BASE_URL" \
    --summary-export="$out_json" \
    --quiet \
    "$ROOT_DIR/$test_file"
  local exit_code=$?
  set -e

  if [ $exit_code -eq 0 ]; then
    echo "   ✓ PASSED"
    PASS_COUNT=$((PASS_COUNT + 1))
  else
    echo "   ✗ FAILED (exit $exit_code)"
    FAIL_COUNT=$((FAIL_COUNT + 1))
  fi

  JSON_FILES+=("$out_json")
  echo ""
}

# ── Run functional tests ──────────────────────────────────────
for t in "${TESTS[@]}"; do
  run_test "$t"
done

# ── Run load test (optional — skip with NO_LOAD=1) ───────────
if [ "${NO_LOAD:-0}" != "1" ]; then
  echo "▶  Running load test (set NO_LOAD=1 to skip)..."
  run_test "$LOAD_TEST"
else
  echo "⏭   Skipping load test (NO_LOAD=1)"
fi

# ── Generate HTML report ──────────────────────────────────────
echo "📊  Generating HTML report..."
node "$SCRIPT_DIR/generate-report.js" \
  "${JSON_FILES[@]}" \
  --out "$ROOT_DIR/$REPORTS_DIR/wallet-test-report.html"

# ── Summary ───────────────────────────────────────────────────
echo ""
echo "══════════════════════════════════════════════════════"
echo "  Suites passed : $PASS_COUNT"
echo "  Suites failed : $FAIL_COUNT"
echo "  Report        : $ROOT_DIR/$REPORTS_DIR/wallet-test-report.html"
echo "══════════════════════════════════════════════════════"
echo ""

[ $FAIL_COUNT -eq 0 ] && exit 0 || exit 1
