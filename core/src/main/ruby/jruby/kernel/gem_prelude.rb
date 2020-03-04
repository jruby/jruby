if Object.const_defined?(:Gem)
  module JRuby
    module LazyRubyGems
    end
  end

  module Kernel
    # Take this alias name so RubyGems will reuse this copy
    # and skip the method below once RubyGems is loaded.
    alias :gem_original_require :require

    private def require(path)
      begin
        gem_original_require(path)
      rescue LoadError
        gem_original_require 'rubygems'

        # Check that #require was redefined by RubyGems, otherwise we would end up in infinite recursion
        new_require = ::Kernel.instance_method(:require)
        if new_require == JRuby::LazyRubyGems::LAZY_REQUIRE
          raise 'RubyGems did not redefine #require as expected, make sure $LOAD_PATH and home are set correctly'
        end
        new_require.bind(self).call(path)
      end
    end

    JRuby::LazyRubyGems::LAZY_REQUIRE = instance_method(:require)

    private def gem(*args)
      require 'rubygems'
      gem(*args)
    end
  end

  class Object
    autoload :Gem, 'rubygems'

    # RbConfig is required by RubyGems, which makes it available in Ruby by default.
    # Autoload it since we do not load RubyGems eagerly.
    autoload :RbConfig, 'rbconfig'
    # Defined by RbConfig
    autoload :CROSS_COMPILING, 'rbconfig'

    # StringIO is required by RubyGems
    autoload :StringIO, 'stringio'
  end

  if Object.const_defined?(:DidYouMean)
    require 'did_you_mean'
  end
end
