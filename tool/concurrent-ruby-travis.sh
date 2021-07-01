#!/usr/bin/env bash
set -v -e

# set up JRuby
mvn clean package
export PATH=`pwd`/bin:$PATH
gem install bundler --no-document

# prep for test
git clone --depth=10 https://github.com/ruby-concurrency/concurrent-ruby.git
cd concurrent-ruby
bundle install

# run tests
bundle exec rake ci
