#!/usr/bin/env bash
set -v -e

# set up JRuby
./mvnw clean package

bin/jruby -S gem install bundler --no-document

# prep for test
git clone --depth=10 https://github.com/ruby-concurrency/concurrent-ruby.git
cd concurrent-ruby
../bin/jruby -S bundle install

# run tests
../bin/jruby -rbundler/setup -S rake ci
