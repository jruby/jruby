###############################################################################
# tc_remainder.rb
#
# Test case for the Bignum#remainder instance method.
###############################################################################
require 'test/unit'

class TC_Bignum_Remainder_InstanceMethod < Test::Unit::TestCase
   def setup
      @bignum_pos  = 18446744073709551616
      @bignum_neg  = -18446744073709551616
      @bignum_posf = 18446744073709551616.34
      @bignum_negf = -18446744073709551616.97
   end

   def test_remainder_basic
      assert_respond_to(@bignum_pos, :remainder)
      assert_nothing_raised{ @bignum_pos.remainder(1) }
   end

   def test_remainder_integer_remainder
      assert_equal(12265, @bignum_pos.remainder(13731))
      assert_equal(-12265, @bignum_neg.remainder(13731))
      assert_equal(12265.0, @bignum_posf.remainder(13731))
      assert_equal(-12265.0, @bignum_negf.remainder(13731))
   end

   def test_remainder_float_remainder
      assert_in_delta(4694.51876357853, @bignum_pos.remainder(13731.24), 0.0000000001)
      assert_in_delta(-4694.51876357853, @bignum_neg.remainder(13731.24), 0.0000000001)
      assert_in_delta(4694.51876357853, @bignum_pos.remainder(13731.24), 0.0000000001)
      assert_in_delta(-4694.51876357853, @bignum_neg.remainder(13731.24), 0.0000000001)
   end

   def test_remainder_edge_cases
      assert_equal(0, @bignum_pos.remainder(1))
      assert_equal(0, @bignum_pos.remainder(1.0))
   end

   def test_remainder_expected_errors
      assert_raise(ArgumentError){ @bignum_pos.remainder(1, 2) }
      assert_raise(ZeroDivisionError){ @bignum_pos.remainder(0) }
   end

   def teardown
      @bignum_pos = nil
      @bignum_neg = nil
      @bignum_posf = nil
      @bignum_negf = nil
   end
end
