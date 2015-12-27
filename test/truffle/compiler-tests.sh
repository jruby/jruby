#!/bin/bash

bin/jruby -X+T test/truffle/attachments-optimise.rb || exit 1
