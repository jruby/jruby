#!/usr/bin/env bash

source test/truffle/common.sh.inc

jt ruby --graal -J-G:+TruffleCompilationExceptionsAreFatal test/truffle/compiler/osr/osr.rb
