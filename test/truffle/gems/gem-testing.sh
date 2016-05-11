#!/usr/bin/env bash

gem install bundler

mkdir -p test/truffle/gems/gem-testing
jruby+truffle --dir test/truffle/gems/gem-testing ci --batch lib/ruby/truffle/jruby+truffle/gem_ci/travis.txt
