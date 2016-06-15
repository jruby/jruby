#!/usr/bin/env bash

set -e

GEM_HOME=${GEM_HOME:-lib/ruby/gems/shared}

ruby -X+T -I$GEM_HOME/gems/execjs-2.6.0/lib test/truffle/gems/execjs/checkruntime.rb
ruby -X+T -I$GEM_HOME/gems/execjs-2.6.0/lib test/truffle/gems/execjs/simple.rb
ruby -X+T -I$GEM_HOME/gems/execjs-2.6.0/lib test/truffle/gems/execjs/coffeescript.rb
