#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e

GEM_HOME=${GEM_HOME:-lib/ruby/gems/shared}

jt ruby -I$GEM_HOME/gems/execjs-2.6.0/lib test/truffle/gems/execjs/checkruntime.rb
jt ruby -I$GEM_HOME/gems/execjs-2.6.0/lib test/truffle/gems/execjs/simple.rb
jt ruby -I$GEM_HOME/gems/execjs-2.6.0/lib test/truffle/gems/execjs/coffeescript.rb
