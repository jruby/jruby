require 'bundler/setup'

require './app/hellowarld'

map '/' do
  run Sinatra::Application
end
