require 'rbconfig'
require 'jruby' if defined?(JRUBY_VERSION)
require 'tempfile'

module TestHelper
  RUBY = File.join(Config::CONFIG['bindir'], Config::CONFIG['ruby_install_name'])
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/
  
  arch = java.lang.System.getProperty('sun.arch.data.model')
  WINDOWS_JVM_64 = (WINDOWS && arch == '64')

  def jruby(*args)
    prev_in_process = JRuby.runtime.instance_config.run_ruby_in_process
    JRuby.runtime.instance_config.run_ruby_in_process = false
    `#{RUBY} #{args.join(' ')}`
  ensure
    JRuby.runtime.instance_config.run_ruby_in_process = prev_in_process
  end

  def jruby_with_pipe(pipe, *args)
    prev_in_process = JRuby.runtime.instance_config.run_ruby_in_process
    JRuby.runtime.instance_config.run_ruby_in_process = false
    `#{pipe} | "#{RUBY}" #{args.join(' ')}`
   ensure
    JRuby.runtime.instance_config.run_ruby_in_process = prev_in_process
  end

  def with_temp_script(script)
    Tempfile.open(["test-script", ".rb"]) do |f|
      begin
        f << script
        f.close
      ensure
        begin
          # Should always yield, even in case of exceptions, otherwise
          # some tests won't even execute, and no failures will be shown
          return yield f
        ensure
          f.unlink rescue nil
        end
      end
    end
  end

  def with_jruby_shell_spawning
    prev_in_process = JRuby.runtime.instance_config.run_ruby_in_process
    JRuby.runtime.instance_config.run_ruby_in_process = false
    yield
    JRuby.runtime.instance_config.run_ruby_in_process = prev_in_process
  end
end

