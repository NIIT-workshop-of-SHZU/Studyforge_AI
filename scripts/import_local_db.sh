#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-test_studyforge_ai_v2}"
DB_USER="${DB_USER:-lynn}"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
SCHEMA_FILE="$(mktemp)"
SEED_FILE="$(mktemp)"

cleanup() {
    rm -f "$SCHEMA_FILE" "$SEED_FILE"
}
trap cleanup EXIT

sed "s/studyforge_ai/${DB_NAME}/g" "$ROOT_DIR/sql/001_schema.sql" > "$SCHEMA_FILE"
sed "s/studyforge_ai/${DB_NAME}/g" "$ROOT_DIR/sql/002_seed_data.sql" > "$SEED_FILE"

mariadb -u"$DB_USER" < "$SCHEMA_FILE"
mariadb -u"$DB_USER" < "$SEED_FILE"

mariadb -u"$DB_USER" -N -B -e "USE ${DB_NAME}; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';"
