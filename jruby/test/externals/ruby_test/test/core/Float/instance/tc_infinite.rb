######################################################################
# tc_infinite.rb
#
# Test case for the Float#infinite? instance method.
######################################################################
require 'test/unit'

class TC_Float_Infinite_InstanceMethod < Test::Unit::TestCase
   def setup
      @float_one  = 1.0
      @float_zero = 0.0
      @float_neg  = -1.0
   end

   def test_infinite_basic
      assert_respond_to(@float_one, :infinite?)
      assert_nothing_raised{ @float_one.infinite? }
   end

   def test_infinite
      assert_equal(nil, @float_one.infinite?)
      assert_equal(nil, @float_zero.infinite?)
      assert_equal(nil, (10.0/3.0).infinite?)
      assert_equal(1, (@float_one/@float_zero).infinite?)
      assert_equal(-1, (@float_neg/@float_zero).infinite?)
   end

   def test_infinite_expected_errors
      assert_raises(ArgumentError){ @float_one.infinite?(1) }
   end

   def teardown
      @float_one  = nil
      @float_zero = nil
      @float_neg  = nil
   end
end
