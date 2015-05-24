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
