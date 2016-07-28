#!/usr/bin/env bash

set -e

GEM_HOME=${GEM_HOME:-lib/ruby/gems/shared}

ruby -X+T $GEM_HOME/gems/asciidoctor-1.5.4/bin/asciidoctor test/truffle/gems/asciidoctor/userguide.adoc

if ! cmp --silent test/truffle/gems/asciidoctor/userguide.html test/truffle/gems/asciidoctor/userguide-expected.html
then
  echo Asciidoctor output was not as expected
  diff -u test/truffle/gems/asciidoctor/userguide-expected.html test/truffle/gems/asciidoctor/userguide.html
  rm -f test/truffle/gems/asciidoctor/userguide.html
  exit 1
else
  rm -f test/truffle/gems/asciidoctor/userguide.html
fi
