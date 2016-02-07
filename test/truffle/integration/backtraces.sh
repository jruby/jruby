#!/usr/bin/env bash

set -e

for f in test/truffle/integration/backtraces/*.rb
do
  echo $f
  ruby -X+T $f
done
