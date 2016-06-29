#!/usr/bin/env bash

ruby -X+T -J-G:+TruffleCompilationExceptionsAreFatal test/truffle/compiler/attachments-optimise/attachments-optimise.rb
