#!/bin/bash

PORT=8080

function wait_until_port_open {
  until lsof -i :$PORT
  do
    sleep 1
  done
  sleep 1
}

echo "Building..."
mvn install -Pbootstrap

echo "Array#pack with real usage..."
bin/jruby -X+T test/truffle/pack-real-usage.rb

echo "Simple web server..."
bin/jruby -X+T test/truffle/simple-server.rb &
wait_until_port_open
curl http://localhost:$PORT/
kill -9 $!
wait

echo "Simple Webrick web server..."
bin/jruby -X+T test/truffle/simple-webrick-server.rb &
wait_until_port_open
curl http://localhost:$PORT/
kill -9 $!
wait

echo "Simple Rack web server..."
bin/jruby bin/gem install rack
bin/jruby -X+T -Ilib/ruby/gems/shared/gems/rack-1.6.1/lib test/truffle/simple-webrick-server.rb &
wait_until_port_open
curl http://localhost:$PORT/
kill -9 $!
wait

echo "Simple Sinatra web server..."
bin/jruby bin/gem install sinatra
bin/jruby -X+T -Ilib/ruby/gems/shared/gems/rack-1.6.1/lib -Ilib/ruby/gems/shared/gems/tilt-2.0.1/lib -Ilib/ruby/gems/shared/gems/rack-protection-1.5.3/lib -Ilib/ruby/gems/shared/gems/sinatra-1.4.6/lib test/truffle/simple-sinatra-server.rb &
wait_until_port_open
curl http://localhost:$PORT/
kill -9 $!
wait
