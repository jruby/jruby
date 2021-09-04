# frozen_string_literal: true
require 'rbconfig'
require 'jruby/util'

module Gem

  class << self
    alias_method :__ruby__, :ruby
    def ruby
      ruby_path = __ruby__
      ruby_path = JRuby::Util.classpath_launcher if jarred_path?(ruby_path)
      ruby_path
    end

    private
    def jarred_path?(p)
      p =~ /^(file|uri|jar|classpath):/
    end
  end

  def self.platform_defaults
    return {
        'install' => '--env-shebang',
        'update' => '--env-shebang',
        'setup' => '--env-shebang',
        'pristine' => '--env-shebang'
    }
  end

  # Default home directory path to be used if an alternate value is not
  # specified in the environment.
  #
  # JRuby: We don't want gems installed in lib/jruby/gems, but rather
  # to preserve the old location: lib/ruby/gems.
  def self.default_dir
    dir = RbConfig::CONFIG["default_gem_home"]
    dir ||= File.join(RbConfig::CONFIG['libdir'], 'ruby', 'gems', 'shared')
    dir
  end

  # Default locations for RubyGems' .rb and bin files
  def self.default_rubygems_dirs
    [
        File.join(RbConfig::CONFIG['libdir'], 'ruby', 'stdlib'),
        RbConfig::CONFIG['bindir']
    ]
  end

  # Allow specifying jar and classpath type gem path entries
  def self.path_separator
    return File::PATH_SEPARATOR unless File::PATH_SEPARATOR == ':'
    /#{JRuby::Util::SEPARATOR}/
  end
end

## JAR FILES: Allow gem path entries to contain jar files
class Gem::Specification
  class << self
    # Replace existing dirs
    def dirs
      @@dirs ||= Gem.path.collect {|dir|
        if File.file?(dir) && dir =~ /\.jar$/
          "file:#{dir}!/specifications"
        elsif File.directory?(File.join(dir, "specifications")) || dir =~ /^file:/
          File.join(dir, "specifications")
        end
      }.compact + spec_directories_from_classpath
    end

    # Replace existing dirs=
    def dirs= dirs
      self.reset

      @@dirs = Array(dirs).map { |d| File.join d, "specifications" } + spec_directories_from_classpath
    end

    private
    def spec_directories_from_classpath
      stuff = [ 'uri:classloader://specifications' ]
      JRuby::Util.extra_gem_paths.each do |path|
        stuff << File.join(path, 'specifications')
      end
      stuff += JRuby::Util.class_loader_resources('specifications', path: true)
      # some classloader return directory info.
      # use only the "protocols" which jruby understands
      stuff.select { |s| File.directory?( s ) }
    end
  end
end
## END JAR FILES

# Check for jruby_native and load it if present. jruby_native
# indicates the native launcher is installed and will override
# env-shebang and possibly other options.
begin
  if File.exist?(File.join(File.dirname(__FILE__), "jruby_native.rb"))
    require 'rubygems/defaults/jruby_native'
  end
rescue LoadError
end

begin
  require 'jar_install_post_install_hook'
rescue LoadError
end
