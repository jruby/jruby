#!/usr/bin/env bash

set -e

bin/jruby bin/gem install execjs -v 2.6.0
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/integration/execjs/checkruntime.rb
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/integration/execjs/simple.rb
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/integration/execjs/coffeescript.rb
