require 'sinatra'
 
# bin/jruby bin/gem install sinatra
# jt run -Ilib/ruby/gems/shared/gems/rack-1.6.1/lib -Ilib/ruby/gems/shared/gems/tilt-2.0.1/lib -Ilib/ruby/gems/shared/gems/rack-protection-1.5.3/lib -Ilib/ruby/gems/shared/gems/sinatra-1.4.6/lib test/truffle/simple-sinatra-server.rb

set :port, 8080

get '/' do
  "Hello Sinatra!"
end
