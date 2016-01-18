#!/usr/bin/env bash

source test/truffle/integration/common/test_server.sh.inc
bin/jruby -X+T test/truffle/integration/tcp-server/tcp-server.rb & test_server
