######################################################################
# tc_finite.rb
#
# Test case for the Float#finite? instance method.
######################################################################
require 'test/unit'

class TC_Float_Finite_InstanceMethod < Test::Unit::TestCase
   def setup
      @float_one  = 1.0
      @float_zero = 0.0
   end

   def test_finite_basic
      assert_respond_to(@float_one, :finite?)
      assert_nothing_raised{ @float_one.finite? }
   end

   def test_finite
      assert_equal(true, @float_one.finite?)
      assert_equal(true, @float_zero.finite?)
      assert_equal(true, (10.0/3.0).finite?)
      assert_equal(false, (@float_one/@float_zero).finite?)
   end

   def test_finite_expected_errors
      assert_raises(ArgumentError){ @float_one.finite?(1) }
   end

   def teardown
      @float_one  = nil
      @float_zero = nil
   end
end
