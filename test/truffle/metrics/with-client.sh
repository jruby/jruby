#!/bin/bash

trap "kill -- -$$" SIGINT SIGTERM
bin/jruby test/truffle/metrics/client.rb &
sleep 10
"$@" 2>&1
kill -9 $!
