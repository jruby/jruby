################################################################
# tc_shift.rb
#
# Test suite for the Array#shift instance method.
################################################################
require "test/unit"

class TC_Array_Shift_Instance < Test::Unit::TestCase
   def setup
      @array = %w/a b c/
   end

   def test_shift_basic
      assert_respond_to(@array, :shift)
      assert_nothing_raised{ @array.shift }
   end

   def test_shift
      assert_equal("a", @array.shift)
      assert_equal("b", @array.shift)
      assert_equal("c", @array.shift)
      assert_equal(nil, @array.shift)
      assert_equal(['a'], [['a'], ['b']].shift)
   end

   def test_shift_edge_cases
      assert_equal(nil, [nil].shift)
      assert_equal(0, [0].shift)
      assert_equal(false, [false].shift)
      assert_equal(true, [true].shift)
      assert_equal([], [[],[]].shift)
   end

   def test_shift_expected_errors
      assert_raises(TypeError){ @array.shift("foo") }
   # No longer a valid test in 1.8.7
=begin
      assert_raises(ArgumentError){ @array.shift(2) }
=end
   end

   def teardown
      @array = nil
   end
end
