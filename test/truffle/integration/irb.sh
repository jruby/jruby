#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e

jt ruby -rirb -e 'IRB.start(__FILE__)' < test/truffle/integration/irb/input.txt > temp.txt

if ! cmp --silent temp.txt test/truffle/integration/irb/output.txt
then
  echo IRB output was not as expected
  rm -f temp.txt
  exit 1
else
  rm -f temp.txt
fi
