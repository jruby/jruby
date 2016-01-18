#!/usr/bin/env bash

bin/jruby -X-T bin/gem install rack
source test/truffle/integration/common/test_server.sh.inc
bin/jruby -X+T test/truffle/integration/rack-server/rack-server.rb & test_server
