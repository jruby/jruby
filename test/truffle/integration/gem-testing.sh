#!/usr/bin/env bash

gem install bundler

mkdir -p test/truffle/integration/gem-testing
jruby+truffle --dir test/truffle/integration/gem-testing ci --batch lib/ruby/truffle/jruby+truffle/gem_ci/travis.txt

