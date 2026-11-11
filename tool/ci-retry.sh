#!/usr/bin/env bash
# Retry a command that has to reach the network (rubygems.org, Maven Central).
# Only a transient network failure is retried: these wrap whole Maven builds, so
# any other failure has to fail the job instead of being retried into a green run.
set -u

# Seconds to wait before each retry. A short first wait is enough for a dropped
# connection or a 502; the longer ones are for outages and rate limits.
read -ra delays <<< "${CI_RETRY_DELAYS:-15 60 90}"
attempts=$(( ${#delays[@]} + 1 ))

# One signature per line; grep -E reads the lines as alternatives. Nothing
# ambiguous belongs here: "Could not resolve dependencies" is also a real miss.
network_failure='Could not reach host|Could not transfer artifact|Network error while fetching
Connection reset|Connection refused|Connection timed out|Read timed out
Temporary failure in name resolution|Temporary failure resolving|Name or service not known
nodename nor servname|Failed to connect to|Failed to fetch|Unable to fetch some archives
The remote end hung up|RPC failed|Unexpected end of file from server
Remote host terminated the handshake|SSLException
502 Bad Gateway|504 Gateway|Service Unavailable
Too Many Requests|status code: 429
Errno::ECONNRESET|Errno::ETIMEDOUT
Could not find artifact|during a previous attempt
Failed to read artifact descriptor
No versions available for .* within specified range'
# The last one is how Maven reports a version range (e.g. jruby-openssl's
# Bouncy Castle jars) when it could not download maven-metadata.xml.
rate_limited='Too Many Requests|status code: 429'

output=$(mktemp "${TMPDIR:-/tmp}/ci-retry.XXXXXX")
trap 'rm -f "$output"' EXIT

attempt=1
while true; do
  # Live to the CI log and to a file, so the output can be read back after exit.
  "$@" 2>&1 | tee "$output"
  status=${PIPESTATUS[0]}

  if [ "$status" -eq 0 ]; then
    exit 0
  fi

  if ! grep -Eq "$network_failure" "$output"; then
    echo "ci-retry: '$*' failed (exit ${status}), not a network failure; not retrying" >&2
    exit "$status"
  fi

  if [ "$attempt" -ge "$attempts" ]; then
    echo "ci-retry: '$*' failed after ${attempts} attempts (exit ${status})" >&2
    exit "$status"
  fi

  delay=${delays[attempt - 1]}
  # Give a rate limit time to reset instead of adding to it.
  if [ "$delay" -lt 30 ] && grep -Eq "$rate_limited" "$output"; then
    delay=30
  fi

  # Maven records a failed download in ~/.m2 and does not ask the remote again
  # until the update interval passes, so a plain retry fails on the cached miss.
  # -U forces the lookup; Maven 3.9+ reads MAVEN_ARGS, other commands ignore it.
  if [ "$attempt" -eq 1 ]; then
    export MAVEN_ARGS="${MAVEN_ARGS:+$MAVEN_ARGS }-U"
  fi

  echo "ci-retry: network failure on attempt ${attempt} of ${attempts} (exit ${status}); retrying in ${delay}s" >&2
  sleep "$delay"
  attempt=$((attempt + 1))
done
