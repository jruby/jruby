#!/usr/bin/env bash

# Basic final tests on a tarball distribution - pass the name of a tarball

set -e
set -x

tar -zxf $1

if [ -f */bin/jruby ]
then
  # JRuby tarball
  RUBY=`echo */bin/jruby`
  FLAGS='-X+T'
else
  # GraalVM tarball
  RUBY=`echo */bin/ruby`
  FLAGS=
fi

if [ ! -f $RUBY ]
then
  echo No ruby executable found $RUBY
  exit 1
fi

if [ ! -f $TOOL ]
then
  echo No tool executable found $TOOL
  exit 1
fi

if [ `$RUBY $FLAGS -e 'puts 14'` != 14 ]
then
  echo Basic execution test failed
  exit 1
fi

if [ `$RUBY $FLAGS -e 'puts defined?(Truffle)'` != constant ]
then
  echo Truffle defined test failed
  exit 1
fi

if [ `$RUBY $FLAGS -e 'puts [1, 2, 3][1]'` != 2 ]
then
  echo Core library test failed
  exit 1
fi

if [ `$RUBY $FLAGS -e 'require "digest"; puts Digest::SHA256.hexdigest("test")'` != 9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08 ]
then
  echo Standard library test failed
  exit 1
fi
