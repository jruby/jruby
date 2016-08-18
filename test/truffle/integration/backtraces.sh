#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e

for f in test/truffle/integration/backtraces/*.rb
do
  echo $f
  jt ruby $f
done
