#!/usr/bin/env bash

set -e

bin/jruby+truffle --dir ../jruby-truffle-gem-test-pack/gem-testing ci --offline --batch test/truffle/ecosystem/batch.yaml
