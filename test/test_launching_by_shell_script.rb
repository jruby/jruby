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

  # JRUBY-2809
  def test_mulitple_at_exit
    assert_equal "", jruby('-e "at_exit { exit 1 }; at_exit { exit 2 }"').chomp
    # first exit status wins
    assert_equal 1, $?.exitstatus

    # exception in one at_exit doesn't affect other at_exit blocks
    result = with_temp_script(%Q{
      at_exit { exit 111 }
      at_exit { raise ArgumentError }
      at_exit { exit 222 }
      at_exit { raise RuntimeError }
    }) do |s|
      jruby("#{s.path} 2>&1")
    end

    # first goes RuntimeError, then ArgumentError
    assert_match(/RuntimeError.*ArgumentError/m, result)
    # the first exit status wins
    assert_equal 111, $?.exitstatus

    # exit status from exception is 1
    result = with_temp_script(%Q{
      at_exit { raise ArgumentError }
      at_exit { exit 222 }
    }) do |s|
      jruby("#{s.path} 2>&1")
    end

    # first goes RuntimeError, then ArgumentError
    assert_match(/ArgumentError/m, result)
    # the first exit status wins
    assert_equal 1, $?.exitstatus

    # exit in one block doesn't stop other blocks from executing
    result = with_temp_script(%Q{
      at_exit { exit 111 }
      at_exit { print "one" }
      at_exit { exit 222 }
      at_exit { print "two" }
    }) do |s|
      jruby("#{s.path}")
    end

    assert_equal "twoone", result
    assert_equal 111, $?.exitstatus

    # exit! in one block prevents execution of others
    result = with_temp_script(%Q{
      at_exit { exit! 1 }
      at_exit { print "one" }
      at_exit { exit! 2 }
      at_exit { print "two" }
    }) do |s|
      jruby("#{s.path}")
    end

    assert_equal "two", result
    assert_equal 2, $?.exitstatus
  end
end
