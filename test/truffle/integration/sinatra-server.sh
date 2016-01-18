#!/usr/bin/env bash

bin/jruby -X-T bin/gem install sinatra
source test/truffle/integration/common/test_server.sh.inc
bin/jruby -X+T test/truffle/integration/sinatra-server/sinatra-server.rb & test_server
