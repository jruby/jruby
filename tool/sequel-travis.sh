#!/usr/bin/env bash
set -v -e

# set up JRuby
./mvnw clean package

bin/jruby -S gem install bundler -v "~>1.17.3"

# set up databases
mysql -e 'create database sequel_test;'
psql -c 'create database sequel_test;' -U postgres

# set up sequel
git clone --depth=10 https://github.com/jeremyevans/sequel.git
cd sequel
cp .ci.gemfile Gemfile
../bin/jruby -S bundle install

# run tests
../bin/jruby -rbundler/setup -S rake spec_ci
