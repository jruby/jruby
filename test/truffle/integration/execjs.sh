#!/usr/bin/env bash

set -e

# Run with for example JRUBY_OPTS='-J-cp ..../trufflejs.jar'

if [[ $JRUBY_OPTS != *"trufflejs.jar"* ]]
then
  echo 'No trufflejs.jar found in $JRUBY_OPTS - skipping ExecJS integration test'
  exit 0
fi

bin/jruby bin/gem install execjs -v 2.6.0
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/integration/execjs/checkruntime.rb
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/integration/execjs/simple.rb
ruby -X+T -Ilib/ruby/gems/shared/gems/execjs-2.6.0/lib test/truffle/integration/execjs/coffeescript.rb
