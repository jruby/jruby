# frozen_string_literal: true
require 'rbconfig'
require 'jruby/util'

# Because this file redefines several methods from RubyGems, we do the
# alias dance to avoid method redefinition warnings.
# See https://github.com/jruby/jruby/pull/9979
class << Gem
  alias_method :__ruby__, :ruby
  def ruby
    ruby_path = __ruby__
    ruby_path = JRuby::Util.classpath_launcher if jarred_path?(ruby_path)
    ruby_path
  end

  private def jarred_path?(p)
    p =~ /^(file|uri|jar|classpath):/
  end

  unless RbConfig::CONFIG['host_os'].match?(/linux/)
    alias _old_platform_defaults platform_defaults
    def platform_defaults
      return {
          'install' => '--env-shebang',
          'update' => '--env-shebang',
          'setup' => '--env-shebang',
          'pristine' => '--env-shebang'
      }
    end
    undef _old_platform_defaults
  end

  # Default home directory path to be used if an alternate value is not
  # specified in the environment.
  #
  # JRuby: We don't want gems installed in lib/jruby/gems, but rather
  # to preserve the old location: lib/ruby/gems.
  alias _old_default_dir default_dir
  def default_dir
    dir = RbConfig::CONFIG["default_gem_home"]
    dir ||= File.join(RbConfig::CONFIG['libdir'], 'ruby', 'gems', 'shared')
    dir
  end
  undef _old_default_dir

  # Default locations for RubyGems' .rb and bin files
  alias _old_default_rubygems_dirs default_rubygems_dirs
  def default_rubygems_dirs
    [
        File.join(RbConfig::CONFIG['libdir'], 'ruby', 'stdlib'),
        RbConfig::CONFIG['bindir']
    ]
  end
  undef _old_default_rubygems_dirs

  # Allow specifying jar and classpath type gem path entries
  alias _old_path_separator path_separator
  def path_separator
    return File::PATH_SEPARATOR unless File::PATH_SEPARATOR == ':'
    /#{JRuby::Util::SEPARATOR}/
  end
  undef _old_path_separator
end

## JAR FILES: Allow gem path entries to contain jar files
class Gem::Specification
  ##
  # Ignore missing extensions on JRuby
  #
  # See https://github.com/ruby/rubygems/issues/3520
  alias _old_missing_extensions? missing_extensions?
  def missing_extensions?
    false
  end
  undef _old_missing_extensions?

  class << self
    # Replace existing dirs
    alias _old_dirs dirs
    def dirs
      @@dirs ||= Gem.path.collect {|dir|
        if File.file?(dir) && dir =~ /\.jar$/
          "file:#{dir}!/specifications"
        elsif File.directory?(File.join(dir, "specifications")) || dir =~ /^file:/
          File.join(dir, "specifications")
        end
      }.compact + spec_directories_from_classpath
    end
    undef _old_dirs

    # Replace existing dirs=
    alias _old_dirs= dirs=
    def dirs= dirs
      self.reset

      @@dirs = Array(dirs).map { |d| File.join d, "specifications" } + spec_directories_from_classpath
    end
    undef _old_dirs=

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

if defined?(JRUBY_VERSION) && Gem.post_install_hooks.empty?
  Gem.post_install do |gem_installer|
    unless (ENV['JARS_SKIP'] || ENV_JAVA['jars.skip']) == 'true'
      require 'jars/installer'
      jars = Jars::Installer.new(gem_installer.spec)
      jars.ruby_maven_install_options = gem_installer.options || {}
      jars.vendor_jars
    end
  end
end
