#!/usr/bin/env bash

set -e

bin/jruby bin/gem install execjs -v 2.6.0
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/gems/execjs/checkruntime.rb
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/gems/execjs/simple.rb
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/gems/execjs/coffeescript.rb
