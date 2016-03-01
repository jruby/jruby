#!/usr/bin/env bash

set -e

cd test/truffle/integration/rails

JRUBY_BIN=../../../../bin
JRUBY=$JRUBY_BIN/jruby
JTR=$JRUBY_BIN/jruby+truffle

$JRUBY_BIN/gem install bundler

$JTR setup
$JTR run -r rubygems -- bin/rails server &
serverpid=$!
url=http://localhost:3000/people.json

while ! curl -s $url;
do
  echo -n .
  sleep 1
done

echo Server is up

set -x

test "$(curl -s $url)" = '[{"name":"John Doe","email":"jd@example.com"}]'
curl -s --data 'name=Anybody&email=ab@example.com' $url
test "$(curl -s $url)" = '[{"name":"John Doe","email":"jd@example.com"},{"name":"Anybody","email":"ab@example.com"}]'

kill %1
kill $(cat tmp/pids/server.pid)

