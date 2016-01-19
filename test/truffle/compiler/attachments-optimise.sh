#!/usr/bin/env bash

bin/jruby -X+T -J-G:+TruffleCompilationExceptionsAreFatal test/truffle/compiler/attachments-optimise/attachments-optimise.rb
