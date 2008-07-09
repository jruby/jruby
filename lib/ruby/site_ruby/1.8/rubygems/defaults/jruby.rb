require 'rubygems/config_file'

module Gem

  ConfigFile::PLATFORM_DEFAULTS['install'] = '--env-shebang'
  ConfigFile::PLATFORM_DEFAULTS['update']  = '--env-shebang'

  # Default home directory path to be used if an alternate value is not
  # specified in the environment.
  #
  # JRuby: We don't want gems installed in lib/jruby/gems, but rather
  # to preserve the old location: lib/ruby/gems.
  def self.default_dir
    File.join ConfigMap[:libdir], 'ruby', 'gems', ConfigMap[:ruby_version]
  end

  ##
  # The path to the running Ruby interpreter.
  #
  # JRuby: Don't append ConfigMap[:EXEEXT] to @jruby, since that would
  # make it jruby.bat.bat on Windows.
  def self.ruby
    if @ruby.nil? then
      @ruby = File.join(ConfigMap[:bindir],
                        ConfigMap[:ruby_install_name])
      # @ruby << ConfigMap[:EXEEXT]
    end

    @ruby
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

if (Gem::win_platform?)
  module Process
    def self.uid
      0
    end
  end
end

