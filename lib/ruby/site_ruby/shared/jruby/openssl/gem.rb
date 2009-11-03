require 'rubygems'

# try to activate jruby-openssl gem, and only require from the gem if it's there
begin
  gem 'jruby-openssl'
  require 'openssl.rb'
rescue LoadError
end

