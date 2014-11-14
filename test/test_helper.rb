require 'rbconfig'
require 'jruby' if defined?(JRUBY_VERSION)
require 'tempfile'

module TestHelper
  # TODO: Consider how this should work if we have --windows or similiar
  WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/
  SEPARATOR = WINDOWS ? '\\' : '/'
  IS_JAR_EXECUTION = RbConfig::CONFIG['bindir'].match( /!\//) || RbConfig::CONFIG['bindir'].match( /:\//)
  RUBY = if IS_JAR_EXECUTION
           exe = 'java'
           exe += RbConfig::CONFIG['EXEEXT'] if RbConfig::CONFIG['EXEEXT']
           file = File.expand_path('maven/jruby-complete/target/jruby-complete-*.jar')
           file = Dir[ file ].first
           exe += " -cp .:#{file} org.jruby.Main"
           exe
         else
           exe = '"' + File.join(RbConfig::CONFIG['bindir'], RbConfig::CONFIG['RUBY_INSTALL_NAME'])
           exe += RbConfig::CONFIG['EXEEXT']  if RbConfig::CONFIG['EXEEXT']
           exe += '"'
           exe
         end

  if (WINDOWS)
    RUBY.gsub!('/', '\\')
    DEVNULL = 'NUL:'
  else
    DEVNULL = '/dev/null'
  end

  if defined? JRUBY_VERSION
    arch = java.lang.System.getProperty('sun.arch.data.model')
    WINDOWS_JVM_64 = (WINDOWS && arch == '64')
  end

  IBM_JVM = RbConfig::CONFIG['host_vendor'] =~ /IBM Corporation/

  def q
    WINDOWS ? '"' : '\''
  end

  def jruby(*args)
    options = []
    if args.last.is_a? Hash
      options = args.last.collect { |k,v| "-D#{k}=\"#{v}\"" }
      args = args[0..-2]
    end
    if RUBY =~ /-cp /
      ruby = RUBY.sub(/-cp [.]/, "-cp #{ENV["CLASSPATH"]}").sub(/-cp /, options.join(' ') + ' -cp ')
    else
      options.each { |a| args.unshift "-J#{a}" }
      ruby = RUBY
    end
    with_jruby_shell_spawning { `#{ruby} #{args.join(' ')}` }
  end

  def jruby_with_pipe(pipe, *args)
    with_jruby_shell_spawning { `#{pipe} | #{RUBY} #{args.join(' ')}` }
  end

  def with_temp_script(script, filename="test-script")
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
end
