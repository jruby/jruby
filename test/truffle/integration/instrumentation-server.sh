#!/usr/bin/env bash

set -e

ruby -X+T -Xtruffle.instrumentation_server_port=8080 test/truffle/integration/instrumentation-server/subject.rb &
pid=$!

while ! (curl -s http://localhost:8080/stacks > /dev/null);
do
  echo -n .
  sleep 1
done

if [[ $(curl -s http://localhost:8080/stacks) != *"test/truffle/integration/instrumentation-server/subject.rb:1"* ]]
then
  echo Expected line not found in stacks
  exit 1
fi

kill -9 $pid || true
wait $pid || true

( echo backtrace ; echo 20000+1400 ; echo continue ) > in.txt
ruby -X+T -Xtruffle.instrumentation_server_port=8080 test/truffle/integration/instrumentation-server/subject.rb < in.txt > out.txt &
pid=$!

while ! (curl -s http://localhost:8080/stacks > /dev/null);
do
  echo -n .
  sleep 1
done

curl -s http://localhost:8080/break

sleep 1
kill -9 $pid || true
wait $pid || true

session=$(cat out.txt)
rm -f in.txt out.txt

if [[ $session != *"test/truffle/integration/instrumentation-server/subject.rb:1"* ]]
then
  echo Expected line not found in backtrace
  exit 1
fi

if [[ $session != *21400* ]]
then
  echo Expected value not found after eval
  exit 1
fi
