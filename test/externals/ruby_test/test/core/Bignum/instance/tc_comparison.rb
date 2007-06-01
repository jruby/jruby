######################################################################
# tc_comparison.rb
#
# Test case for the Bignum#<=> method.
######################################################################
require 'test/unit'

class TC_Bignum_Comparison_InstanceMethod < Test::Unit::TestCase
   def setup
      @bignum1  = 2**64
      @bignum2  = 2**65
      @nbignum1 = -(2**64)
      @nbignum2 = -(2**65)
   end

   def test_comparison_basic
      assert_respond_to(@bignum1, :<=>)
      assert_kind_of(Fixnum, @bignum1 <=> @bignum2)
   end

   def test_comparison_positive
      assert_equal(-1, @bignum1 <=> @bignum2)
      assert_equal(1, @bignum2 <=> @bignum1)
      assert_equal(0, @bignum1 <=> 2**64)
      assert_equal(0, @bignum2 <=> 2**65)
   end

   def test_comparison_negative
      assert_equal(1, @nbignum1 <=> @nbignum2)
      assert_equal(-1, @nbignum2 <=> @nbignum1)
      assert_equal(0, @nbignum1 <=> -(2**64))
      assert_equal(0, @nbignum2 <=> -(2**65))
   end

   def test_comparison_mixed
      assert_equal(1, @bignum1 <=> @nbignum1)
      assert_equal(1, @bignum2 <=> @nbignum1)
      assert_equal(-1, @nbignum1 <=> @bignum1)
      assert_equal(-1, @nbignum1 <=> @bignum2)
   end

   def teardown
      @bignum1  = nil
      @bignum2  = nil
      @nbignum1 = nil
      @nbignum2 = nil
   end
end
