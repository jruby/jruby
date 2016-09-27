#!/usr/bin/env bash

ruby -X+T -J-G:+TruffleCompilationExceptionsAreThrown -Xtruffle.basic_ops.inline=false test/truffle/compiler/pe/pe.rb
