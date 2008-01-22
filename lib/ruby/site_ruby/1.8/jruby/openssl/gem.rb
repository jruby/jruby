begin
  old_verbose, $VERBOSE = $VERBOSE, nil # silence warnings
  require 'rubygems'
  gem 'jruby-openssl'
  require 'openssl.rb'
ensure
  $VERBOSE = old_verbose
end

