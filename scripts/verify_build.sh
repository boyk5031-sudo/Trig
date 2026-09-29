#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
TASKS=(lint test verifyReleaseResources assembleRelease)

if [[ -x "$ROOT_DIR/gradlew" ]]; then
  exec "$ROOT_DIR/gradlew" --no-daemon "${TASKS[@]}"
fi
if command -v gradle >/dev/null 2>&1; then
  echo "WARNING: Gradle wrapper is absent; using installed Gradle." >&2
  exec gradle --no-daemon "${TASKS[@]}"
fi
echo "ERROR: No Gradle wrapper or system Gradle was found. Install Gradle or add the project wrapper, then rerun scripts/verify_build.sh." >&2
exit 127
