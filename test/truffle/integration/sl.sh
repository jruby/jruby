#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e

jt ruby test/truffle/integration/sl/inline-exported.rb
