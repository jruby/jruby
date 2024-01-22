if Object.const_defined?(:Gem)
  begin
    require 'rubygems.rb'
  rescue LoadError # java -jar lib/jruby.jar -e '...'
    warn 'RubyGems not found; disabling gems' if $VERBOSE
  else
    begin
      gem 'did_you_mean'
      require 'did_you_mean'
      Gem.clear_paths
    rescue LoadError # Gem::LoadError < LoadError
    end if Object.const_defined?(:DidYouMean)
  end
end
