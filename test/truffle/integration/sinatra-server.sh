#!/usr/bin/env bash

set -e

bin/jruby -X-T bin/gem install rack -v 1.6.1
bin/jruby -X-T bin/gem install tilt -v 2.0.1
bin/jruby -X-T bin/gem install rack-protection -v 1.5.3
bin/jruby -X-T bin/gem install sinatra -v 1.4.6
source test/truffle/integration/common/test_server.sh.inc
bin/jruby -X+T -Ilib/ruby/gems/shared/gems/rack-1.6.1/lib -Ilib/ruby/gems/shared/gems/tilt-2.0.1/lib -Ilib/ruby/gems/shared/gems/rack-protection-1.5.3/lib -Ilib/ruby/gems/shared/gems/sinatra-1.4.6/lib test/truffle/integration/sinatra-server/sinatra-server.rb & test_server
