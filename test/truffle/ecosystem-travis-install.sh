#!/usr/bin/env bash

set -e
set -x

unset GEM_HOME GEM_PATH

bin/jruby bin/gem install bundler

git clone \
    --branch master \
    https://github.com/jruby/jruby-truffle-gem-test-pack.git \
    ../jruby-truffle-gem-test-pack

test/truffle/gem-test-pack-checkout-revision.sh

