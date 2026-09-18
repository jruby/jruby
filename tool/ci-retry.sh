#!/usr/bin/env bash
# Retry a command that has to reach the network (rubygems.org, Maven Central).
# A single connection reset or DNS blip there otherwise fails a whole CI job.
set -u

attempts=${CI_RETRY_ATTEMPTS:-3}
delay=${CI_RETRY_DELAY:-15}

attempt=1
while true; do
  "$@" && exit 0
  status=$?

  if [ "$attempt" -ge "$attempts" ]; then
    echo "ci-retry: '$*' failed after ${attempts} attempts (exit ${status})" >&2
    exit "$status"
  fi

  echo "ci-retry: attempt ${attempt} of ${attempts} failed (exit ${status}); retrying in ${delay}s" >&2
  sleep "$delay"
  attempt=$((attempt + 1))
  delay=$((delay * 2))
done
