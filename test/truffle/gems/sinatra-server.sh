#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e

GEM_HOME=${GEM_HOME:-lib/ruby/gems/shared}

jt ruby -I$GEM_HOME/gems/rack-1.6.1/lib \
        -I$GEM_HOME/gems/tilt-2.0.1/lib \
        -I$GEM_HOME/gems/rack-protection-1.5.3/lib \
        -I$GEM_HOME/gems/sinatra-1.4.6/lib \
        test/truffle/gems/sinatra-server/sinatra-server.rb & test_server
