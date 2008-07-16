require 'test/unit'
require 'test/test_helper'

class TestLaunchingByShellScript < Test::Unit::TestCase
  include TestHelper

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
    
    # JRUBY-2615
    def test_interactive_child_process
      lines = []
      IO.popen(%q{sh -c 'echo enter something:; read value; echo got: $value; read value'}, 'r+') do |handle|
        begin
          while (line = handle.readline)
            lines << line
            handle.puts('foobar')
          end
        rescue EOFError
          lines << "STDIN closed"
        end
      end
      assert_equal(["enter something:\n", "got: foobar\n", "STDIN closed"], lines)
    end
  end

  def test_at_exit
    assert_equal "", jruby('-e "at_exit { exit 0 }"').chomp
    assert_equal 0, $?.exitstatus
    assert_equal "", jruby('-e "at_exit { exit 1 }"').chomp
    assert_equal 1, $?.exitstatus
  end
end
