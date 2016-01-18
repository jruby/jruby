#!/usr/bin/env bash

bin/jruby -X+T -J-G:+TruffleCompilationExceptionsAreThrown test/truffle/compiler/pe/pe.rb
