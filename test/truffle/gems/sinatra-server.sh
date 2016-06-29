#!/usr/bin/env bash

set -e

GEM_HOME=${GEM_HOME:-lib/ruby/gems/shared}

source test/truffle/common/test_server.sh.inc

ruby -X+T -I$GEM_HOME/gems/rack-1.6.1/lib \
          -I$GEM_HOME/gems/tilt-2.0.1/lib \
          -I$GEM_HOME/gems/rack-protection-1.5.3/lib \
          -I$GEM_HOME/gems/sinatra-1.4.6/lib \
          test/truffle/gems/sinatra-server/sinatra-server.rb & test_server
