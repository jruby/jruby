require 'test/unit'

class TestAssignment19 < Test::Unit::TestCase
  def test_eaching_over_array_of_arrays
    arrays = [[:a, :b], [:c, :d]]
    agg = []
    arrays.each {|x,y| agg << x; agg << y}
    assert_equal [:a, :b, :c, :d], agg
  end
end
