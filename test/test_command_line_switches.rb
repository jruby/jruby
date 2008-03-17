require 'test/unit'
require 'test/test_helper'

class TestCommandLineSwitches < Test::Unit::TestCase
  include TestHelper

  def test_dash_little_s_one_keyval
    with_temp_script(%q{puts $v}) do |s|
      assert_equal "123", `#{RUBY} -s #{s.path} -v=123`.chomp
    end
  end

  def test_dash_little_s_two_keyvals
    with_temp_script(%q{puts $v, $foo}) do |s|
      assert_equal "123\nbar", `#{RUBY} -s #{s.path} -v=123 -foo=bar`.chomp
    end
  end

  def test_dash_little_s_removes_options_from_argv
    with_temp_script(%q{puts $v, *ARGV}) do |s|
      assert_equal "123\n4\n5\n6", `#{RUBY} -s #{s.path} -v=123 4 5 6`.chomp
    end
  end

  def test_dash_little_s_options_must_come_after_script
    with_temp_script(%q{puts $v, *ARGV}) do |s|
      assert_equal "nil\na\n-v=123\nb\nc", `#{RUBY} -s #{s.path} a -v=123 b c`.chomp
    end
  end

  def test_dash_little_s_options_ignores_invalid_global_var_names
    with_temp_script(%q{puts $v}) do |s|
      assert_equal "nil", `#{RUBY} -s #{s.path} -v-a=123`.chomp
    end
  end
end