#!/usr/bin/env bash
# BE-034: fails when a pull request changes, deletes or renames a migration that is already on the target branch.
# A merged migration may have run on a database (the Supabase dev database today, production later), and Flyway refuses
# to start when a file it has applied changes. MigrationTest can't see an edit: it builds its database from the edited
# files. The fix is always a new V<n>__fix_<what>.sql.
#
# Usage: merged-migrations-unchanged.sh <target branch commit> <pull request commit>
set -euo pipefail

base="$1"
head="$2"
dir="backend/src/main/resources/db/migration"

changed="$(git diff --name-status --find-renames --diff-filter=MDRT "$base" "$head" -- "$dir")"
if [ -n "$changed" ]; then
  echo "::error::A merged migration was changed, deleted or renamed. Add a new $dir/V<n>__fix_<what>.sql instead."
  echo "$changed"
  exit 1
fi
echo "No merged migration was changed."
