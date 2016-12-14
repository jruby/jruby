#!/usr/bin/env bash

source test/truffle/common.sh.inc

jt ruby --graal --stress --trace -e "puts 'hello'"
