#!/usr/bin/env bash
set -v

# set up JRuby
mvn clean package
export PATH=`pwd`/bin:$PATH
gem install bundler

# set up databases
mysql -e 'create database sequel_test;'
psql -c 'create database sequel_test;' -U postgres

# set up sequel
git clone --depth=10 https://github.com/jeremyevans/sequel.git
cd sequel
cp .travis.gemfile Gemfile
bundle install

# run tests
bundle exec rake spec_travis
