require 'rbconfig'
require 'jruby' if defined?(JRUBY_VERSION)
require 'tempfile'

module TestHelper
  RUBY = File.join(Config::CONFIG['bindir'], Config::CONFIG['ruby_install_name'])
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/

  def jruby(*args)
    prev_in_process = JRuby.runtime.instance_config.run_ruby_in_process
    JRuby.runtime.instance_config.run_ruby_in_process = false
    `#{RUBY} #{args.join(' ')}`
  ensure
    JRuby.runtime.instance_config.run_ruby_in_process = prev_in_process
  end

  def with_temp_script(script)
    t = Tempfile.new("test-script")
    t << script
    t.close
    yield t
  ensure
    t.unlink rescue nil
  end
end