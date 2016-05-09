#!/usr/bin/env bash

set -e

ruby -X+T -S irb < test/truffle/integration/irb/input.txt > temp.txt

if ! cmp --silent temp.txt test/truffle/integration/irb/output.txt
then
  echo IRB output was not as expected
  rm -f temp.txt
  exit 1
else
  rm -f temp.txt
fi
