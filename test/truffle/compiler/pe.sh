#!/usr/bin/env bash

source test/truffle/common.sh.inc

jt ruby --graal -J-G:+TruffleCompilationExceptionsAreThrown -Xtruffle.basic_ops.inline=false test/truffle/compiler/pe/pe.rb
