# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'sinatra'

# bin/jruby bin/gem install sinatra
# jt run -Ilib/ruby/gems/shared/gems/rack-1.6.1/lib -Ilib/ruby/gems/shared/gems/tilt-2.0.1/lib -Ilib/ruby/gems/shared/gems/rack-protection-1.5.3/lib -Ilib/ruby/gems/shared/gems/sinatra-1.4.6/lib test/truffle/simple-sinatra-server.rb

set :port, 8080

get '/' do
  "Hello Sinatra!"
end
