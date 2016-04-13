#-*- mode: ruby -*-

use Rack::ShowExceptions

require 'hello_world'
require 'yaml'

run lambda { |env|
  require 'flickraw'
  [
    200, 
    {
      'Content-Type'  => 'text/html', 
      'Cache-Control' => 'public, max-age=86400' 
    },
    [ "self: #{__FILE__}\n", "PWD: #{Dir.pwd}\n", "Gem.path: #{Gem.path.inspect}\n","Gem::Specification.dirs: #{Gem::Specification.dirs.inspect}\n", Gem.loaded_specs['flickraw'].gem_dir + "\n", HelloWorld.new + "\n", "snakeyaml-#{Psych::SNAKEYAML_VERSION}\n" ]
  ]
}

# vim: syntax=Ruby
