######################################################################
# tc_to_s.rb
#
# Test case for the Bignum#to_s instance method.
#
# As a result of some overzealous optimizations that occurred in
# Ruby 1.8.6, I've added several extra tests for certain radix values
# for bignums less than 2**64 (for 32 bit Ruby interpreters).
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Bignum_ToS_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @bignum1 = 18446744073709551616 # 2**64
      if BIT_32
         @bignum2 = 8589934592 # 2**33
      end
   end

   def test_to_s_basic
      assert_respond_to(@bignum1, :to_s)
      assert_kind_of(String, @bignum1.to_s)
   end

   def test_to_s
      assert_equal("18446744073709551616", @bignum1.to_s)
   end

   def test_to_s_with_base
      assert_equal("18446744073709551616", @bignum1.to_s)

      assert_equal(
         "10000000000000000000000000000000000000000000000000000000000000000",
         @bignum1.to_s(2)
      )

      assert_equal("2000000000000000000000", @bignum1.to_s(8))
      assert_equal("10000000000000000", @bignum1.to_s(16))
      assert_equal("g000000000000", @bignum1.to_s(32))
   end

   if BIT_32
      def test_to_s_with_base_8
         assert_equal("100000000000", @bignum2.to_s(8))
         assert_equal("-100000000000", -8589934592.to_s(8))   
      end
      
      def test_to_s_with_base_36
         assert_equal("3y283y8", @bignum2.to_s(36))
         assert_equal("-3y283y8", -8589934592.to_s(36))
      end
   end

   def test_to_s_expected_errors
      assert_raises(ArgumentError){ @bignum1.to_s(0) }
      assert_raises(ArgumentError){ @bignum1.to_s(37) } # Max is 36
   end

   def teardown
      @bignum1 = nil
      if BIT_32
         @bignum2 = nil
      end
   end
end
