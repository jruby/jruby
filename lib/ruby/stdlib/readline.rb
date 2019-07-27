begin
  gem 'reline'
  require 'reline'
rescue LoadError
  $stderr << "You must install the `reline` gem to use readline"
end

Readline = Reline