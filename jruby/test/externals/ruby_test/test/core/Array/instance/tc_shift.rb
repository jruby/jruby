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
   end

   def test_shift_expected_errors
      assert_raises(ArgumentError){ @array.shift("foo") }
   end

   def teardown
      @array = nil
   end
end
