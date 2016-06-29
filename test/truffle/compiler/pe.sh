#!/usr/bin/env bash

ruby -X+T -J-G:+TruffleCompilationExceptionsAreThrown test/truffle/compiler/pe/pe.rb
