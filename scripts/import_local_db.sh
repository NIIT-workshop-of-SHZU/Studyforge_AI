#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-test_studyforge_ai_v2}"
DB_USER="${DB_USER:-lynn}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_HOST="${DB_HOST:-}"
DB_PORT="${DB_PORT:-}"
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

MARIADB_ARGS=(-u"$DB_USER")
if [ -n "$DB_PASSWORD" ]; then
    MARIADB_ARGS+=("-p${DB_PASSWORD}")
fi
if [ -n "$DB_HOST" ]; then
    MARIADB_ARGS+=("-h${DB_HOST}")
fi
if [ -n "$DB_PORT" ]; then
    MARIADB_ARGS+=("-P${DB_PORT}")
fi

mariadb "${MARIADB_ARGS[@]}" < "$SCHEMA_FILE"

if [ "$RESET_SEED" = "1" ]; then
    SEED_FILE="$(mktemp)"
    sed "s/studyforge_ai/${DB_NAME}/g" "$ROOT_DIR/sql/002_seed_data.sql" > "$SEED_FILE"
    echo "RESET_SEED=1: importing seed data and resetting business tables in ${DB_NAME}" >&2
    mariadb "${MARIADB_ARGS[@]}" < "$SEED_FILE"
else
    echo "Schema imported into ${DB_NAME}. Existing user content was preserved." >&2
    echo "To reset local data intentionally, run: RESET_SEED=1 $0" >&2
fi

for migration in "$ROOT_DIR"/sql/[0-9][0-9][0-9]_*.sql; do
    case "$(basename "$migration")" in
        001_schema.sql|002_seed_data.sql)
            continue
            ;;
    esac
    echo "Applying migration $(basename "$migration") to ${DB_NAME}" >&2
    mariadb "${MARIADB_ARGS[@]}" "$DB_NAME" < "$migration"
done

mariadb "${MARIADB_ARGS[@]}" -N -B -e "USE ${DB_NAME}; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';"
