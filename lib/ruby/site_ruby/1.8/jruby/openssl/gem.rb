begin
  old_verbose, $VERBOSE = $VERBOSE, nil # silence warnings
  require 'rubygems'
  tried_gem = false
  begin
    require 'openssl.rb'
  rescue LoadError
    unless tried_gem
      tried_gem = true
      gem 'jruby-openssl'
      retry
    end
  end
ensure
  $VERBOSE = old_verbose
end

