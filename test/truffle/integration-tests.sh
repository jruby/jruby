#!/bin/bash

PORT=8080

function wait_until_port_open {
  until lsof -i :$PORT
  do
    sleep 1
  done
  sleep 1
}

function test_server {
  wait_until_port_open
  response=`curl http://localhost:$PORT/`
  kill -9 $!
  wait
  if [[ $response != *"Hello"* ]]
  then
    echo Response not expected
    exit 1
  fi
}

echo "Array#pack with real usage..."
bin/jruby -X+T test/truffle/pack-real-usage.rb || exit 1

echo "Simple web server..."
bin/jruby -X+T test/truffle/simple-server.rb &
test_server

echo "Simple Webrick web server..."
bin/jruby -X+T test/truffle/simple-webrick-server.rb &
test_server

echo "Simple Rack web server..."
bin/jruby -X-T bin/gem install rack
bin/jruby -X+T -Ilib/ruby/gems/shared/gems/rack-1.6.4/lib test/truffle/simple-webrick-server.rb &
test_server

echo "Simple Sinatra web server..."
bin/jruby -X-T bin/gem install sinatra
bin/jruby -X+T -Ilib/ruby/gems/shared/gems/rack-1.6.4/lib -Ilib/ruby/gems/shared/gems/tilt-2.0.1/lib -Ilib/ruby/gems/shared/gems/rack-protection-1.5.3/lib -Ilib/ruby/gems/shared/gems/sinatra-1.4.6/lib test/truffle/simple-sinatra-server.rb &
test_server

echo "Coverage..."
bin/jruby -X+T -Xtruffle.coverage=true test/truffle/coverage/test.rb
