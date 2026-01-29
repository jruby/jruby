#!/usr/bin/env bash
set -v -e

# set up PATH for JRuby
export PATH=`pwd`/bin:$PATH

# set up sequel
git clone --depth=10 https://github.com/jeremyevans/sequel.git
cd sequel
cp .ci.gemfile Gemfile
bundle install

# run tests
DEFAULT_DATABASE=1 MYSQL_ROOT_PASSWORD=1 bundle exec rake spec_ci
