require 'test/unit'
require 'rbconfig'
require 'jruby'

class TestLaunchingByShellScript < Test::Unit::TestCase
  WINDOWS = Config::CONFIG['host_os'] =~ /Windows|mswin/
  RUBY = File.join(Config::CONFIG['bindir'], Config::CONFIG['ruby_install_name'])
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

  def test_minus_e
    assert_equal "true", jruby('-e "puts true"').chomp
    assert_equal 0, $?.exitstatus
  end

  def test_launch_script
    jruby "test/fib.rb"
    assert_equal 0, $?.exitstatus
  end

  def test_system_call_without_stdin_data_doesnt_hang
    out = jruby(%q{-e "system 'dir'"})
    assert(out =~ /COPYING.LGPL/)
  end

  if WINDOWS
    def test_system_call_with_stdin_data_doesnt_hang_on_windows
      out = jruby_with_pipe("echo echo 'one_two_three_test'", %q{-e "system 'cmd'"})
      assert(out =~ /one_two_three_test/)
    end
  else
    def test_system_call_with_stdin_data_doesnt_hang
      out = jruby_with_pipe("echo 'vvs'", %q{-e "system 'cat'"})
      assert_equal("vvs\n", out)
    end
  end

  def test_at_exit
    assert_equal "", jruby("-e 'at_exit { exit 0 }'").chomp
    assert_equal 0, $?.exitstatus
    assert_equal "", jruby("-e 'at_exit { exit 1 }'").chomp
    assert_equal 1, $?.exitstatus
  end
end