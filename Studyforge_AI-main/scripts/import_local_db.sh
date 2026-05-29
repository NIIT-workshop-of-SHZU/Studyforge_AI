#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-test_studyforge_ai_v2}"
DB_USER="${DB_USER:-lynn}"
RESET_SEED="${RESET_SEED:-0}"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCHEMA_FILE="$(mktemp)"
SEED_FILE=""

cleanup() {
    rm -f "$SCHEMA_FILE"
    if [ -n "$SEED_FILE" ]; then
        rm -f "$SEED_FILE"
    fi
}
trap cleanup EXIT

sed "s/studyforge_ai/${DB_NAME}/g" "$ROOT_DIR/sql/001_schema.sql" > "$SCHEMA_FILE"

mariadb -u"$DB_USER" < "$SCHEMA_FILE"

if [ "$RESET_SEED" = "1" ]; then
    SEED_FILE="$(mktemp)"
    sed "s/studyforge_ai/${DB_NAME}/g" "$ROOT_DIR/sql/002_seed_data.sql" > "$SEED_FILE"
    echo "RESET_SEED=1: importing seed data and resetting business tables in ${DB_NAME}" >&2
    mariadb -u"$DB_USER" < "$SEED_FILE"
else
    echo "Schema imported into ${DB_NAME}. Existing user content was preserved." >&2
    echo "To reset local data intentionally, run: RESET_SEED=1 $0" >&2
fi

mariadb -u"$DB_USER" -N -B -e "USE ${DB_NAME}; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';"
