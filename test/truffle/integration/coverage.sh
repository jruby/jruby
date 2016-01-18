#!/usr/bin/env bash

bin/jruby -X+T -Xtruffle.coverage=true test/truffle/integration/coverage/test.rb
