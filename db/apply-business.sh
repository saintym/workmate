#!/usr/bin/env bash
# Apply the business-domain schema + seed data to the running Postgres container.
# Use this when the container volume already exists and 02-business.sql won't
# run automatically on init.
#
# Usage:  bash db/apply-business.sh
# Requires: Docker running with the workmate-postgres container up.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

docker exec -i workmate-postgres psql -U workmate -d workmate \
  < "${SCRIPT_DIR}/init/02-business.sql"

echo "02-business.sql applied successfully."
