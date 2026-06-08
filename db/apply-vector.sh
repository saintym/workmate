#!/usr/bin/env bash
# Apply the pgvector document_chunks schema to the running Postgres container.
# Use this when the container volume already exists and 03-vector.sql won't
# run automatically on init.
#
# The script drops any existing document_chunks table first (including any
# JPA-managed version from earlier phases) so the DDL in 03-vector.sql is
# applied cleanly.
#
# Usage:  bash db/apply-vector.sh
# Requires: Docker running with the workmate-postgres container up.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Drop the old JPA-created table if present, then re-create via the new DDL.
docker exec -i workmate-postgres psql -U workmate -d workmate <<'EOF'
DROP TABLE IF EXISTS document_chunks CASCADE;
EOF

docker exec -i workmate-postgres psql -U workmate -d workmate \
  < "${SCRIPT_DIR}/init/03-vector.sql"

echo "03-vector.sql applied successfully."
