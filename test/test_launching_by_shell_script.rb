require 'test/unit'
require 'test/test_helper'

class TestLaunchingByShellScript < Test::Unit::TestCase
  include TestHelper

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

  if WINDOWS
    def test_system_call_without_stdin_data_doesnt_hang
      out = jruby(%q{-e "system 'dir test'"})
      assert(out =~ /fib.rb/)
    end

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

  if (!WINDOWS)
    # JRUBY-2295
    def test_java_props_with_spaces
      res = jruby(%q{-J-Dfoo='a b c' -e "require 'java'; puts java.lang.System.getProperty('foo')"}).chomp
      assert_equal("a b c", res)
    end
  end

  def test_at_exit
    assert_equal "", jruby('-e "at_exit { exit 0 }"').chomp
    assert_equal 0, $?.exitstatus
    assert_equal "", jruby('-e "at_exit { exit 1 }"').chomp
    assert_equal 1, $?.exitstatus
  end
end
