#!/usr/bin/env bash

ruby -X+T -J-G:+TruffleCompilationExceptionsAreFatal test/truffle/compiler/osr/osr.rb
