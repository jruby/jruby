#!/usr/bin/env bash

set -e

bin/jruby bin/gem install rack -v 1.6.1
source test/truffle/gems/common/test_server.sh.inc
ruby -X+T -Ilib/ruby/gems/shared/gems/rack-1.6.1/lib test/truffle/gems/rack-server/rack-server.rb & test_server
