#!/usr/bin/env bash

set -e

ruby -X+T -Xtruffle.instrumentation_server_port=8080 test/truffle/integration/instrumentation-server/subject.rb &

while ! (curl -s http://localhost:8080/stacks > /dev/null);
do
  echo -n .
  sleep 1
done

response=$(curl -s http://localhost:8080/stacks)

if [[ $response != *"test/truffle/integration/instrumentation-server/subject.rb:1"* ]]
then
  echo Response not as expected
  exit 1
fi

kill %1
