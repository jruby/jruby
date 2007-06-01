######################################################################
# tc_floor.rb
#
# Test case for the Float#floor instance method.
######################################################################
require 'test/unit'

class Test_Float_Floor_InstanceMethod < Test::Unit::TestCase
   def setup
      @float_pos = 1.07
      @float_neg = -0.93
   end

   def test_floor_basic
      assert_respond_to(@float_pos, :floor)
      assert_nothing_raised{ @float_pos.floor }
      assert_kind_of(Integer, @float_pos.floor)
   end

   def test_floor
      assert_equal(1, @float_pos.floor)
      assert_equal(-1, @float_neg.floor)
      assert_equal(1, (1.0).floor)
      assert_equal(0, (0.0).floor)
   end

   def test_floor_expected_errors
      assert_raises(ArgumentError){ @float_pos.floor(1) }
   end

   def teardown
      @float_pos = nil
      @float_neg = nil
   end
end
