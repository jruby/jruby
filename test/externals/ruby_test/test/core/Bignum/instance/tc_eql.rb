###############################################################################
# tc_eql.rb
#
# Test case for the Bignum#eql? instance method.
###############################################################################
require 'test/unit'

class TC_Bignum_Eql_InstanceMethod < Test::Unit::TestCase
   def setup
      @bignum1 = 2**64
      @bignum2 = 2**64
      @bignum3 = 2**64 + 1
      @bignum4 = 18446744073709551616.0 # 2**64, as a float
   end

   def test_equality_basic
      assert_respond_to(@bignum1, :eql?)
      assert_nothing_raised{ @bignum1.eql? @bignum2 }
   end

   def test_equality
      assert_equal(true, @bignum1.eql?(@bignum1))
      assert_equal(true, @bignum1.eql?(@bignum2))
      assert_equal(false, @bignum1.eql?(@bignum3))
      assert_equal(false, @bignum1.eql?(@bignum4))
   end

   def test_equality_expected_errors
      assert_raise(ArgumentError){ @bignum1.send(:eql?, @bignum2, @bignum3) }
   end

   def teardown
      @bignum1 = nil
      @bignum2 = nil
      @bignum3 = nil
      @bignum4 = nil
   end
end
