require 'rubygems/config_file'
require 'rbconfig'

module Gem

  ConfigFile::PLATFORM_DEFAULTS['install'] = '--env-shebang'
  ConfigFile::PLATFORM_DEFAULTS['update']  = '--env-shebang'

  @jar_paths = []

  class << self
    alias_method :original_ensure_gem_subdirectories, :ensure_gem_subdirectories
    def ensure_gem_subdirectories(gemdir)
      original_ensure_gem_subdirectories(gemdir) unless jarred_path? gemdir.to_s
    end

    alias_method :original_set_paths, :set_paths
    def set_paths(gpaths)
      original_set_paths(gpaths)
      @gem_path.reject! {|p| !readable_path? p }
      @jar_paths.each {|p| @gem_path << p unless @gem_path.include?(p) } if @jar_paths
    end

    alias_method :original_default_path, :default_path
    def default_path
      paths = RbConfig::CONFIG["default_gem_path"]
      paths = paths.split(':').reject {|p| p.empty? }.compact if paths
      paths ||= original_default_path
      @jar_paths = paths.select {|p| jarred_path? p }
      paths.reject {|p| jarred_path? p }
    end

    alias_method :original_ruby, :ruby
    def ruby
      ruby_path = original_ruby
      if jarred_path?(ruby_path)
        ruby_path = "java -jar #{ruby_path.sub(/^file:/,"").sub(/!.*/,"")}"
      end
      ruby_path
    end

    def readable_path?(p)
      p =~ /^file:/ || File.exists?(p)
    end

    def jarred_path?(p)
      p =~ /^file:/
    end
  end

  # Default home directory path to be used if an alternate value is not
  # specified in the environment.
  #
  # JRuby: We don't want gems installed in lib/jruby/gems, but rather
  # to preserve the old location: lib/ruby/gems.
  def self.default_dir
    dir = RbConfig::CONFIG["default_gem_home"]
    dir ||= File.join(ConfigMap[:libdir], 'ruby', 'gems', '1.8')
    dir
  end

  ##
  # Is this a windows platform?
  #
  # JRuby: Look in CONFIG['host_os'] as well.
  def self.win_platform?
    if @@win_platform.nil? then
      @@win_platform = !!WIN_PATTERNS.find { |r| RUBY_PLATFORM =~ r || Config::CONFIG["host_os"] =~ r }
    end

    @@win_platform
  end

end

## JAR FILES: Allow gem path entries to contain jar files
require 'rubygems/source_index'
class Gem::SourceIndex
  class << self
    def installed_spec_directories
      # TODO: fix remaining glob tests
      Gem.path.collect do |dir|
        if File.file?(dir) && dir =~ /\.jar$/
          "file:#{dir}!/specifications"
        elsif File.directory?(dir) || dir =~ /^file:/
          File.join(dir, "specifications")
        end
      end.compact + spec_directories_from_classpath
    end

    def spec_directories_from_classpath
      require 'jruby/util'
      stuff = JRuby::Util.classloader_resources("specifications")
    end
  end
end
## END JAR FILES

if (Gem::win_platform?)
  module Process
    def self.uid
      0
    end
  end
end

# Check for jruby_native and load it if present. jruby_native
# indicates the native launcher is installed and will override
# env-shebang and possibly other options.
begin
  require 'rubygems/defaults/jruby_native'
rescue LoadError
end
