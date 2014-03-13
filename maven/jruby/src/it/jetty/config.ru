#-*- mode: ruby -*-

use Rack::ShowExceptions

require 'hello_world'

run lambda { |env|
  Gem.path.clear
  require 'flickraw'
  [
    200, 
    {
      'Content-Type'  => 'text/html', 
      'Cache-Control' => 'public, max-age=86400' 
    },
    [ HelloWorld.new, Gem.loaded_specs['flickraw'].gem_dir ]
  ]
}

# vim: syntax=Ruby
