#!/usr/bin/env bash

set -e
set -x

unset GEM_HOME GEM_PATH

# TODO CS 13-Dec-16 We'd like to run this ourselves, but we get issues when we try. Fix later.
#tool/jt.rb ruby -rbundler-workarounds bin/gem install bundler
GEM_HOME=lib/ruby/gems/shared gem install bundler

git clone \
    --branch master \
    https://github.com/jruby/jruby-truffle-gem-test-pack.git \
    ../jruby-truffle-gem-test-pack

test/truffle/gem-test-pack-checkout-revision.sh
