require 'test/unit'

class TestTime19 < Test::Unit::TestCase
  def test_equal
    assert_equal(false, Time.now == nil)
  end
end