###############################################################################
# tc_abs.rb
#
# Test case for the Bignum#abs instance method.
###############################################################################
require 'test/unit'

class TC_Bignum_Abs_InstanceMethod < Test::Unit::TestCase
   def setup
      @bignum_pos  = 18446744073709551616
      @bignum_neg  = -18446744073709551616
      @bignum_posf = 18446744073709551616.34
      @bignum_negf = -18446744073709551616.89
   end

   def test_abs_basic
      assert_respond_to(@bignum_pos, :abs)
      assert_nothing_raised{ @bignum_pos.abs }
   end

   def test_abs
      assert_equal(18446744073709551616, @bignum_pos.abs)
      assert_equal(18446744073709551616, @bignum_neg.abs)
      assert_equal(18446744073709551616, @bignum_posf.abs)
      assert_equal(18446744073709551616, @bignum_negf.abs)
   end

   def test_abs_expected_errors
      assert_raise(ArgumentError){ @bignum_pos.abs(true) }
   end

   def teardown
      @bignum_pos  = nil
      @bignum_neg  = nil
      @bignum_posf = nil
      @bignum_negf = nil
   end
end
