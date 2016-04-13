#!/usr/bin/env bash

ruby -X+T -Xtruffle.coverage=true test/truffle/integration/coverage/test.rb
