#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e

jt ruby test/truffle/integration/js/eval.rb
jt ruby test/truffle/integration/js/inline-exported.rb
