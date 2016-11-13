#!/usr/bin/env bash

set -e
set -x

cd ../jruby-truffle-gem-test-pack/gem-testing/rails-app

JRUBY_BIN=../../../jruby/bin
JRUBY=$JRUBY_BIN/jruby
JTR=$JRUBY_BIN/jruby-truffle-tool

if [ -n "$CI" -a -z "$HAS_REDIS" ]
then
    echo "No Redis. Skipping rails test."

else

    if [ -f tmp/pids/server.pid ]
    then
        kill $(cat tmp/pids/server.pid) || true
        rm tmp/pids/server.pid
    fi

    $JRUBY $JRUBY_BIN/gem install bundler

    $JTR setup --offline
    $JTR run -r rubygems -- bin/rails server &
    serverpid=$!
    url=http://localhost:3000

    set +x
    while ! curl -s "$url/people.json";
    do
        echo -n .
        sleep 1
    done
    set -x

    echo Server is up

    curl -s -X "DELETE" "$url/people/destroy_all.json"
    test "$(curl -s "$url/people.json")" = '[]'
    curl -s --data 'name=Anybody&email=ab@example.com' "$url/people.json"
    echo "$(curl -s "$url/people.json")" | grep '"name":"Anybody","email":"ab@example.com"'
    curl -s -X "DELETE" "$url/people/destroy_all.json"

    kill %1
    kill $(cat tmp/pids/server.pid)

fi
