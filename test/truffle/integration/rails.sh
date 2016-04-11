#!/usr/bin/env bash

set -e

cd test/truffle/integration/rails

JRUBY_BIN=../../../../bin
JRUBY=$JRUBY_BIN/jruby
JTR=$JRUBY_BIN/jruby+truffle

if [ -n "$CI" -a -z "$HAS_REDIS" ]
then
    echo "No Redis. Skipping rails test."

else

    $JRUBY $JRUBY_BIN/gem install bundler

    $JTR setup
    $JTR run -r rubygems -- bin/rails server &
    serverpid=$!
    url=http://localhost:3000

    while ! curl -s "$url/people.json";
    do
      echo -n .
      sleep 1
    done

    echo Server is up

    set -x
    curl -s -X "DELETE" "$url/people/destroy_all.json"
    test "$(curl -s "$url/people.json")" = '[]'
    curl -s --data 'name=Anybody&email=ab@example.com' "$url/people.json"
    echo "$(curl -s "$url/people.json")" | grep '"name":"Anybody","email":"ab@example.com"'
    curl -s -X "DELETE" "$url/people/destroy_all.json"

    kill %1
    kill $(cat tmp/pids/server.pid)

    set +x
    set +e

fi
