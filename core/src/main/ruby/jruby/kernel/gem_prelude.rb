if defined?(Gem)
  begin
    require 'rubygems'
  rescue LoadError
    # For JRUBY-5333, gracefully fail to load, since stdlib may not be available
    warn 'RubyGems not found; disabling gems' if $VERBOSE
  else
    begin
      require 'did_you_mean'
    rescue LoadError
    end if defined?(DidYouMean)
  end
end
