#!/usr/bin/env bash

set -e

PORT=14873

ruby -X+T -Xtruffle.instrumentation_server_port=$PORT test/truffle/integration/instrumentation-server/subject.rb &
pid=$!

sleep 6

while ! (curl -s http://localhost:$PORT/stacks > /dev/null);
do
  echo -n .
  sleep 1
done

if [[ $(curl -s http://localhost:$PORT/stacks) != *"test/truffle/integration/instrumentation-server/subject.rb:1"* ]]
then
  echo Expected line not found in stacks
  exit 1
fi

kill -9 $pid || true
wait $pid || true

( echo backtrace ; echo 20000+1400 ; echo continue ) > in.txt
ruby -X+T -Xtruffle.instrumentation_server_port=$PORT test/truffle/integration/instrumentation-server/subject.rb < in.txt > out.txt &
pid=$!

sleep 6

while ! (curl -s http://localhost:$PORT/stacks > /dev/null);
do
  echo -n .
  sleep 1
done

curl -s http://localhost:$PORT/break

sleep 1
kill -9 $pid || true
wait $pid || true

session=$(cat out.txt)
rm -f in.txt out.txt

if [[ $session != *"test/truffle/integration/instrumentation-server/subject.rb:1"* ]]
then
  echo $session
  echo Expected line not found in backtrace
  exit 1
fi

if [[ $session != *21400* ]]
then
  echo $session
  echo Expected value not found after eval
  exit 1
fi
