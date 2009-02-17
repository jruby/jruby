###############################################################################
# tc_size.rb
#
# Test case for the Bignum#size instance method.
#
# TODO: I'm not entirely sure how to make this cross-platform or
# cross-architecture. The tests below work with what I've got.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Bignum_Size_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @bignum1 = 2**63
      @bignum2 = 2**64
      @bignum3 = 2**65
      @bignum4 = 2**120
      @bignum5 = 2**160
   end

   def test_size_basic
      assert_respond_to(@bignum1, :size)
      assert_nothing_raised{ @bignum1.size }
   end

   if JRUBY
      def test_size
         assert_equal(8, @bignum1.size)
         assert_equal(9, @bignum2.size)
         assert_equal(9, @bignum3.size)
         assert_equal(16, @bignum4.size)
         assert_equal(21, @bignum5.size)
      end
   else
      def test_size
         assert_equal(8, @bignum1.size)
         assert_equal(12, @bignum2.size)
         assert_equal(12, @bignum3.size)
         assert_equal(16, @bignum4.size)
         assert_equal(24, @bignum5.size)
      end
   end

   def test_size_expected_errors
      assert_raise(ArgumentError){ @bignum1.size(1) }
   end

   def teardown
      @bignum1 = nil
      @bignum2 = nil
      @bignum3 = nil
      @bignum4 = nil
      @bignum5 = nil
   end
end
