#!/usr/bin/env bash

source test/truffle/common.sh.inc

# relies on value profiling
jt ruby --graal -J-Dgraal.TruffleCompilationExceptionsAreFatal -Xtruffle.basic_ops.inline=false test/truffle/compiler/attachments-optimise/attachments-optimise.rb
