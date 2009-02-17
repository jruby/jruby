###############################################################################
# tc_aref.rb
#
# Test case for the Bignum#[] instance method.
###############################################################################
require 'test/unit'

class TC_Bignum_Aref_InstanceMethod < Test::Unit::TestCase
   def setup
      @bignum = 3**65
   end

   def test_aref_basic
      assert_respond_to(@bignum, :[])
   end

   def test_aref
      assert_equal(1, @bignum[0])
      assert_equal(1, @bignum[1])
      assert_equal(0, @bignum[5])
      assert_equal(0, @bignum[9999999999999999999999999])
   end

   def test_aref_expected_errors
      assert_raise(ArgumentError){ @bignum[0,1] }
   end

   def teardown
      @bignum = nil
   end
end
