if Object.const_defined?(:Gem)
  module JRuby
    module GemUtil
      # Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved. This
      # code is released under a tri EPL/GPL/LGPL license. You can use it,
      # redistribute it and/or modify it under the terms of the:
      #
      # Eclipse Public License version 2.0, or
      # GNU General Public License version 2, or
      # GNU Lesser General Public License version 2.1.

      DEFAULT_GEMS = {
            'bundler' => true,
            'cmath' => true,
            'csv' => true,
            'did_you_mean' => true,
            'e2mmap' => true,
            'ffi' => true,
            'fileutils' => true,
            'forwardable' => true,
            'ipaddr' => true,
            'irb' => true,
            'jar-dependencies' => true,
            'jruby-openssl' => true,
            'jruby-readline' => true,
            'json' => true,
            'logger' => true,
            'matrix' => true,
            'mutex_m' => true,
            'ostruct' => true,
            'prime' => true,
            'psych' => true,
            'racc' => true,
            'rake-ant' => true,
            'rdoc' => true,
            'rexml' => true,
            'rss' => true,
            'scanf' => true,
            'shell' => true,
            'sync' => true,
            'thwait' => true,
            'tracer' => true,
            'webrick' => true,
        }

      def self.upgraded_default_gem?(feature)
        if i = feature.index('/')
          first_component = feature[0...i]
        else
          first_component = feature
        end

        if DEFAULT_GEMS.include?(first_component)
          # No need to check for 'io/nonblock' and 'io/wait', just for 'io/console'
          return false if first_component == 'io' and !feature.start_with?('io/console')

          matcher = "#{first_component}-"
          gem_paths.each do |gem_dir|
            spec_dir = "#{gem_dir}/specifications"
            if File.directory?(spec_dir)
              Dir.each_child(spec_dir) do |spec|
                if spec.start_with?(matcher) and digit = spec[matcher.size] and '0' <= digit && digit <= '9'
                  return true
                end
              end
            end
          end
        end

        false
      end

      # Gem.path, without needing to load RubyGems
      def self.gem_paths
        @gem_paths ||= begin
          home = ENV['GEM_HOME'] || "#{ENV_JAVA['jruby.home']}/lib/ruby/gems/shared"
          paths = [home]

          if gem_path = ENV['GEM_PATH']
            paths.concat gem_path.split(File::PATH_SEPARATOR)
          else
            user_dir = "#{Dir.home}/.gem/jruby/#{RUBY_ENGINE_VERSION}"
            paths << user_dir
          end

          paths.map { |path| expand(path) }.uniq
        end
      end

      def self.expand(path)
        if File.directory?(path)
          File.realpath(path)
        else
          path
        end
      end
    end
  end

  module Kernel
    # Take this alias name so RubyGems will reuse this copy
    # and skip the method below once RubyGems is loaded.
    alias :gem_original_require :require

    def require(feature)
      feature = JRuby::Util.coerce_to_path(feature)

      lazy_rubygems = JRuby::Util.retrieve_option("rubygems.lazy")
      upgraded_default_gem = lazy_rubygems && JRuby::GemUtil.upgraded_default_gem?(feature)

      # TODO find file to know if it will load
      if !upgraded_default_gem #and path = Primitive.find_file(feature)
        # gem_original_require(path)
        gem_original_require(feature)
      else
        if lazy_rubygems
          gem_original_require 'rubygems'

          # Check that #require was redefined by RubyGems, otherwise we would end up in infinite recursion
          new_require = ::Kernel.instance_method(:require)
          if new_require == JRuby::GemUtil::LAZY_REQUIRE
            raise 'RubyGems did not redefine #require as expected, make sure $LOAD_PATH and home are set correctly'
          end
          new_require.bind(self).call(feature)
        else
          raise LoadError.new(feature)
        end
      end
    end
    module_function :require

    JRuby::GemUtil::LAZY_REQUIRE = instance_method(:require)

    def gem(*args)
      require 'rubygems'
      gem(*args)
    end
    private :gem
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
