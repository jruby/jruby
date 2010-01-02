require 'test/unit'
require 'test/test_helper'

class TestRi < Test::Unit::TestCase
  include TestHelper

  def test_ri_runs_in_1_8
    assert jruby("--1.8 -S ri --help").include?('Usage:')
    assert_equal $?, 0
  end

  def test_ri_runs_in_1_9
    assert jruby("--1.9 -S ri --help").include?('Usage:')
    assert_equal $?, 0
  end
end
