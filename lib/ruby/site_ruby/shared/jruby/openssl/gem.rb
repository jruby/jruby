# try to activate jruby-openssl gem, and only require from the gem if it's there
tried_gem = false
begin
  require 'openssl.rb'
rescue LoadError
  unless tried_gem
    require 'rubygems'
    begin
      gem 'jruby-openssl'
    rescue LoadError
    end
    tried_gem = true
    retry
  end
end
