######################################################################
# tc_zero.rb
#
# Test case for the Float#zero? instance method.
######################################################################
require 'test/unit'

class TC_Float_Zero_InstanceMethod < Test::Unit::TestCase
   def setup
      @zero_pos = 0.0
      @zero_neg = -0.0
      @float_pos = 1.0
   end

   def test_nan_basic
      assert_respond_to(@zero_pos, :zero?)
      assert_nothing_raised{ @zero_pos.zero? }
   end

   def test_nan
      assert_equal(true, @zero_pos.zero?)
      assert_equal(true, @zero_neg.zero?)
      assert_equal(false, @float_pos.zero?)
   end

   def test_nan_expected_errors
      assert_raises(ArgumentError){ @zero_pos.zero?(1) }
   end

   def teardown
      @zero_pos  = nil
      @zero_neg  = nil
      @float_pos = nil
   end
end
