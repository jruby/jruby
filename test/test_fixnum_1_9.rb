require 'test/unit'

class TestFixnum19 < Test::Unit::TestCase
  def test_even?
    assert 0.even?
    assert 2.even?
  end
  
  def test_odd?
    assert 1.odd?
    assert -1.odd?
  end
  
  def test_pred
    assert_equal 1, 2.pred
    assert_equal 0, 1.pred
    assert_equal -1, 0.pred
  end
end