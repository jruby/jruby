# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'rack'
 
# bin/jruby bin/gem install rack
# jt run -Ilib/ruby/gems/shared/gems/rack-1.6.1/lib simple-rack-server.rb
 
class Example
  def call(env)
    return [200, {}, ["Hello Rack!\n"]]
  end
end
 
Rack::Handler::WEBrick.run(
  Example.new,
  :Port => 8080
)
