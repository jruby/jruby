#!/bin/bash

# Truffle may eventually become part of OpenJDK, and when that happens we won't
# want to package com.oracle.truffle in complete. This script removes those
# packages from a build of complete.

# Takes no arguments. Modifies a copy of
# maven/jruby-complete/target/jruby-complete-$version.jar to create
# maven/jruby-complete/target/jruby-complete-no-truffle-$version.jar.

# Run in the root directory. Run mvn -Pcomplete first.

version=`cat VERSION`

cp maven/jruby-complete/target/jruby-complete-$version.jar maven/jruby-complete/target/jruby-complete-no-truffle-$version.jar
zip -d maven/jruby-complete/target/jruby-complete-no-truffle-$version.jar com/oracle/nfi/* com/oracle/truffle/*
