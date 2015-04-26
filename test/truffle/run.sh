#!/bin/bash

echo "Building..."
mvn install -Pbootstrap

echo "Array#pack with real usage..."
bin/jruby -X+T test/truffle/pack-real-usage.rb
