if Object.const_defined?(:Gem)
  if JRuby::Util.retrieve_option("rubygems.lazy")
    load 'jruby/kernel/lazy_rubygems.rb'
  else
    begin
      require 'rubygems.rb'
    rescue LoadError
      warn 'RubyGems not found; disabling gems' if $VERBOSE
    end
  end
end

if Object.const_defined?(:DidYouMean)
  # did_you_mean is a default gem  and can load with or without RubyGems
  begin
    require 'did_you_mean'
  rescue LoadError # Gem::LoadError < LoadError
  end
end