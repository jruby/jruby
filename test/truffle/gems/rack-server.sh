#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e

GEM_HOME=${GEM_HOME:-lib/ruby/gems/shared}

jt ruby -I$GEM_HOME/gems/rack-1.6.1/lib test/truffle/gems/rack-server/rack-server.rb & test_server
