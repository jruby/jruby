require 'rbconfig'
require 'test/unit'

class TestRbconfig < Test::Unit::TestCase
  def test_constants
    assert Config
    assert Config::CONFIG
    assert RbConfig
    assert_equal Config, RbConfig
  end
end