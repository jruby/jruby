#!/usr/bin/env bash

set -e

gem install bundler

mkdir -p test/truffle/ecosystem/gem-testing
jruby+truffle --dir test/truffle/ecosystem/gem-testing ci --batch lib/ruby/truffle/jruby+truffle/gem_ci/travis.txt
