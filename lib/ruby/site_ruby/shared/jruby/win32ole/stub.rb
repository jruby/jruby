require 'rubygems'
begin
  gem 'jruby-win32ole'
rescue LoadError
  warn "!!!! Missing jruby-win32ole gem: jruby -S gem install jruby-win32ole"
  raise $!
end
require 'jruby-win32ole'
