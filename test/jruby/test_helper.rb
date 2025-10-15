require 'rbconfig'
require 'jruby' if defined?(JRUBY_VERSION)

module TestHelper
  WINDOWS = Gem.win_platform?
  SEPARATOR = WINDOWS ? '\\' : '/'
  # using the classloader setup to determine whether it runs inside ScriptingContainer or via commandline
  if defined?(JRUBY_VERSION)
    IS_COMMAND_LINE_EXECUTION = JRuby.runtime.jruby_class_loader == java.lang.Thread.current_thread.context_class_loader
  else
    IS_COMMAND_LINE_EXECUTION = true
  end

  IS_JAR_EXECUTION = RbConfig::CONFIG['bindir'].match(/!\//) || (!WINDOWS && RbConfig::CONFIG['bindir'].match(/:\//))
  RUBY = if IS_JAR_EXECUTION
           exe = 'java'
           exe += RbConfig::CONFIG['EXEEXT'] if RbConfig::CONFIG['EXEEXT']
           # assume the parent CL of jruby-classloader has a getUrls method
           urls = JRuby.runtime.getJRubyClassLoader.parent.getURLs.collect { |u| u.path }
           urls.unshift '.'
           exe += " -cp #{urls.join(File::PATH_SEPARATOR)} org.jruby.main.Main"
           exe
         else
           exe = '"' + File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['RUBY_INSTALL_NAME'])
           exe += RbConfig::CONFIG['EXEEXT'] if RbConfig::CONFIG['EXEEXT']
           exe += '"'
           exe
         end

  if WINDOWS
    RUBY.gsub!('/', '\\')
    DEVNULL = 'NUL:'
  else
    DEVNULL = '/dev/null'
  end

  IS_JRUBY = RUBY_ENGINE == 'jruby'

  if IS_JRUBY
    arch = java.lang.System.getProperty('sun.arch.data.model')
    WINDOWS_JVM_64 = (WINDOWS && arch == '64')
  end

  IBM_JVM = RbConfig::CONFIG['host_vendor'] =~ /IBM Corporation/

  JAVA_9 = ENV_JAVA['java.specification.version'] > '1.8' rescue nil

  def q
    WINDOWS ? '"' : '\''
  end

  protected

  def with_temp_script(script, filename="test-script"); require 'tempfile'
    Tempfile.open([filename, ".rb"]) do |f|
      begin
        # we ignore errors writing to the tempfile to ensure the test tries to run
        f.syswrite(script) rescue 1
        return yield f
      ensure
        f.close!
      end
    end
  end

  def interpreter( options = {} )
    options = options.collect { |k,v| "-D#{k}=\"#{v}\"" }
    if RUBY =~ /-cp /
      RUBY.sub(/-cp [.]/, "-cp .#{File::PATH_SEPARATOR}#{ENV['CLASSPATH']}").sub(/-cp /, options.join(' ') + ' -cp ')
    else
      RUBY
    end
  end

  def jruby(*args)
    options = []
    if args.last.is_a? Hash
      options = args.last
      args = args[0..-2]
    end
    options.each { |k,v| args.unshift "-J-D#{k}=\"#{v}\"" } unless RUBY =~ /-cp /
    with_jruby_shell_spawning { sh "#{interpreter(options)} #{args.join(' ')}" }
  end

  def jruby(*args)
    options = []
    if args.last.is_a? Hash
      options = args.last
      args = args[0..-2]
    end
    sh "#{interpreter(options)} #{args.join(' ')}"
  end unless IS_JRUBY

  def jruby_with_pipe(pipe, *args)
    options = []
    if args.last.is_a? Hash
      options = args.last
      args = args[0..-2]
    end
    options.each { |k,v| args.unshift "-J-D#{k}=\"#{v}\"" } unless RUBY =~ /-cp /
    with_jruby_shell_spawning { sh "#{pipe} | #{interpreter(options)} #{args.join(' ')}" }
  end

  def sh(cmd)
    puts cmd if $DEBUG
    return `#{cmd}`
  end

  private

  def with_jruby_shell_spawning
    prev_in_process = JRuby.runtime.instance_config.run_ruby_in_process
    JRuby.runtime.instance_config.run_ruby_in_process = false
    yield
  ensure
    JRuby.runtime.instance_config.run_ruby_in_process = prev_in_process
  end

  def quiet(&block)
    io = [STDOUT.dup, STDERR.dup]
    STDOUT.reopen DEVNULL
    STDERR.reopen DEVNULL
    block.call
  ensure
    STDOUT.reopen io.first
    STDERR.reopen io.last
  end

  def run_in_sub_runtime(script)
    container = org.jruby.embed.ScriptingContainer.new(org.jruby.embed.LocalContextScope::SINGLETHREAD)
    container.setLoadPaths(['.'])
    container.runScriptlet("require 'java'")
    container.runScriptlet(script)
  end

  def assert_in_sub_runtime(script)
    assert run_in_sub_runtime(script)
  end

  def assert_java_raises(type)
    begin
      yield
      fail("expected to raise (#{type}) but did not")
    rescue java.lang.Throwable => ex
      raise(ex) unless ex.is_a?(type)
    end
  end

  def self.included(base)
    if defined? Test::Unit::TestCase
      if base < Test::Unit::TestCase
        Test::Unit::TestCase.class_eval do
          unless method_defined?(:skip)
            if method_defined?(:omit)
              alias skip omit
            else
              def skip(msg = nil)
                warn "Skipped: #{caller[0]} #{msg}"
              end
            end
          end
        end
      end
    end
  end

end
