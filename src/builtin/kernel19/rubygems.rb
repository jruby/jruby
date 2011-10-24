# Note: We have disabled all of gem_prelude and just load rubygems here.

begin
  require 'rubygems'
rescue LoadError
  # For JRUBY-5333, gracefully fail to load, since stdlib may not be available
  warn 'rubygems.rb not found; disabling gems' if $VERBOSE
end
