#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e
set -x

# TODO CS 13-Dec-16 We'd like to run this ourselves, but we get issues when we try. Fix later.
# jt ruby -rbundler-workarounds bin/gem install ...
GEM_HOME=lib/ruby/gems/shared gem install execjs -v 2.6.0
GEM_HOME=lib/ruby/gems/shared gem install rack -v 1.6.1
GEM_HOME=lib/ruby/gems/shared gem install tilt -v 2.0.1
GEM_HOME=lib/ruby/gems/shared gem install rack-protection -v 1.5.3
GEM_HOME=lib/ruby/gems/shared gem install sinatra -v 1.4.6
GEM_HOME=lib/ruby/gems/shared gem install asciidoctor -v 1.5.4
