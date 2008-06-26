module Gem

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

end
