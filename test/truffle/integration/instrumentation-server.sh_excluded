#!/usr/bin/env bash

source test/truffle/common.sh.inc

set -e
set -x

PORT=14873
SLEEP_LINE=4

# Test /stacks

jt ruby -Xtruffle.instrumentation_server_port=$PORT test/truffle/integration/instrumentation-server/subject.rb &
pid=$!

while [ ! -e ready.txt ]; do
  sleep 1
done

while ! (curl -s http://localhost:$PORT/stacks > /dev/null);
do
  echo -n .
  sleep 1
done

if [[ $(curl -s http://localhost:$PORT/stacks) != *"test/truffle/integration/instrumentation-server/subject.rb:$SLEEP_LINE"* ]]
then
  echo Expected line not found in stacks
  exit 1
fi

kill -9 $pid || true
wait $pid || true
rm ready.txt

# Test /break

( echo backtrace ; echo 20000+1400 ; echo exit ) > in.txt
jt ruby -Xtruffle.instrumentation_server_port=$PORT test/truffle/integration/instrumentation-server/subject.rb < in.txt > out.txt &
pid=$!

while [ ! -e ready.txt ]; do
  sleep 1
done

while ! (curl -s http://localhost:$PORT/stacks > /dev/null);
do
  echo -n .
  sleep 1
done

curl -s http://localhost:$PORT/break

# Wait for the script to finish and write the output
wait $pid

session=$(cat out.txt)
rm -f ready.txt in.txt out.txt

if [[ $session != *"test/truffle/integration/instrumentation-server/subject.rb:$SLEEP_LINE"* ]]
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
