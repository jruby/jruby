#!/usr/bin/env bash
# Run a CI Maven build. When MAVEN_OFFLINE=true, build offline first: the precache
# job already filled ~/.m2, and offline Maven makes no maven-metadata.xml requests,
# so version ranges cannot fail on a Maven Central 404 or 429. If the cache lacks
# something the build needs, warn and build online with tool/ci-retry.sh.
set -u

if [ "${MAVEN_OFFLINE:-}" = true ]; then
  output=$(mktemp "${TMPDIR:-/tmp}/ci-maven-build.XXXXXX")
  trap 'rm -f "$output"' EXIT

  ./mvnw -o "$@" 2>&1 | tee "$output"
  status=${PIPESTATUS[0]}
  if [ "$status" -eq 0 ]; then
    exit 0
  fi

  # Only a missing artifact is a reason to go online; any other failure is real.
  if ! grep -Eq 'in offline mode|No versions available for .* within specified range' "$output"; then
    exit "$status"
  fi
  echo "::warning title=Maven offline build::The Maven cache is missing artifacts for '$*'; building online"
fi

exec tool/ci-retry.sh ./mvnw "$@"
