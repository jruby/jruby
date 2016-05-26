#!/usr/bin/env bash

set -e

ruby -X+T test/truffle/integration/js/eval.rb
ruby -X+T test/truffle/integration/js/inline-exported.rb
