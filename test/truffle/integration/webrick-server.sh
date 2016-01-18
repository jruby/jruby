#!/usr/bin/env bash

source test/truffle/integration/common/test_server.sh.inc
bin/jruby -X+T test/truffle/integration/webrick-server/webrick-server.rb & test_server
