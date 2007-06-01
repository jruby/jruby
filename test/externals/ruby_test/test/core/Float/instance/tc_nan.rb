######################################################################
# tc_nan.rb
#
# Test case for the Float#nan? instance method.
######################################################################
require 'test/unit'

class TC_Float_Nan_InstanceMethod < Test::Unit::TestCase
   def setup
      @float_one  = 1.0
      @float_zero = 0.0
   end

   def test_nan_basic
      assert_respond_to(@float_one, :nan?)
      assert_nothing_raised{ @float_one.nan? }
   end

   def test_nan
      assert_equal(false, @float_one.nan?)
      assert_equal(false, @float_zero.nan?)
      assert_equal(false, (10.0/3.0).nan?)
      assert_equal(false, (@float_one/@float_zero).nan?)
      assert_equal(false, (@float_one/@float_one).nan?)
      assert_equal(true, (@float_zero/@float_zero).nan?)
   end

   def test_nan_expected_errors
      assert_raises(ArgumentError){ @float_one.nan?(1) }
   end

   def teardown
      @float_one  = nil
      @float_zero = nil
   end
end
