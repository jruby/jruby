#!/usr/bin/env bash

# relies on value profiling
ruby -X+T -J-G:+TruffleCompilationExceptionsAreFatal -Xtruffle.basic_ops.inline=false test/truffle/compiler/attachments-optimise/attachments-optimise.rb
