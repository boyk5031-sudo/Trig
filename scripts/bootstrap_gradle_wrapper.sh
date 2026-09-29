#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"
if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle 8.9 is required once to generate the checked-in wrapper. Install Gradle, then rerun this script." >&2
  exit 127
fi
VERSION="$(gradle --version | awk '/^Gradle / {print $2; exit}')"
if [[ "$VERSION" != "8.9" ]]; then
  echo "This project wrapper is pinned to Gradle 8.9 (AGP 8.7.3); found ${VERSION:-unknown}." >&2
  echo "Install Gradle 8.9 or adjust the project and wrapper versions together." >&2
  exit 2
fi
gradle wrapper --gradle-version 8.9 --distribution-type bin
