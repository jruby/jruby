#!/usr/bin/env bash

set -e

GEM_HOME=${GEM_HOME:-lib/ruby/gems/shared}

source test/truffle/common/test_server.sh.inc

ruby -X+T -I$GEM_HOME/gems/rack-1.6.1/lib test/truffle/gems/rack-server/rack-server.rb & test_server
