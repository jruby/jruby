###############################################################################
# tc_equality.rb
#
# Test case for the Bignum#== instance method.
###############################################################################
require 'test/unit'

class TC_Bignum_Equality_InstanceMethod < Test::Unit::TestCase
   def setup
      @bignum1 = 2**64
      @bignum2 = 2**64
      @bignum3 = 2**64 + 1
      @bignum4 = 18446744073709551616.0 # 2**64, as a float
   end

   def test_equality_basic
      assert_respond_to(@bignum1, :==)
      assert_nothing_raised{ @bignum1 == @bignum2 }
   end

   def test_equality
      assert_equal(true, @bignum1 == @bignum1)
      assert_equal(true, @bignum1 == @bignum2)
      assert_equal(false, @bignum1 == @bignum3)
      assert_equal(true, @bignum1 == @bignum4)
   end

   def test_equality_expected_errors
      assert_raise(ArgumentError){ @bignum1.send(:==, @bignum2, @bignum3) }
   end

   def teardown
      @bignum1 = nil
      @bignum2 = nil
      @bignum3 = nil
   end
end
