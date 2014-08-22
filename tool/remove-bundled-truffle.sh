#!/bin/bash

# Truffle may eventually become part of OpenJDK, and when that happens we won't
# want to package com.oracle.truffle in complete. This script removes those
# packages from a build of complete.

# Takes no arguments. Modifies a copy of
# maven/jruby-complete/target/jruby-complete-9000.dev.jar to create
# maven/jruby-complete/target/jruby-complete-no-truffle-9000.dev.jar.

# Chris Seaton, 5 Aug 14

cp maven/jruby-complete/target/jruby-complete-9000.dev.jar maven/jruby-complete/target/jruby-complete-no-truffle-9000.dev.jar
zip -d maven/jruby-complete/target/jruby-complete-no-truffle-9000.dev.jar com/oracle/truffle/*
