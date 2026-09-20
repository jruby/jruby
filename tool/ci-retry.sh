#!/usr/bin/env bash
# Retry a command that has to reach the network (rubygems.org, Maven Central).
# Only a transient network failure is retried: these wrap whole Maven builds, so
# any other failure has to fail the job instead of being retried into a green run.
set -u

attempts=${CI_RETRY_ATTEMPTS:-3}
delay=${CI_RETRY_DELAY:-15}

# One signature per line; grep -E reads the lines as alternatives. Nothing
# ambiguous belongs here: "Could not resolve dependencies" is also a real miss.
network_failure='Could not reach host|Could not transfer artifact|Network error while fetching
Connection reset|Connection refused|Connection timed out|Read timed out
Temporary failure in name resolution|Temporary failure resolving|Name or service not known
nodename nor servname|Failed to connect to|Failed to fetch|Unable to fetch some archives
The remote end hung up|RPC failed|Unexpected end of file from server
Remote host terminated the handshake|SSLException
502 Bad Gateway|504 Gateway|Service Unavailable
Errno::ECONNRESET|Errno::ETIMEDOUT'

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

  echo "ci-retry: network failure on attempt ${attempt} of ${attempts} (exit ${status}); retrying in ${delay}s" >&2
  sleep "$delay"
  attempt=$((attempt + 1))
  delay=$((delay * 2))
done
