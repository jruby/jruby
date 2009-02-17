###############################################################################
# tc_to_f.rb
#
# Test case for the Bignum#to_f instance method. Note that the test for
# Infinity will cause a warning. You can ignore it.
###############################################################################
require 'test/unit'

class TC_Bignum_ToF_InstanceMethod < Test::Unit::TestCase
   def setup
      @bignum1 = 2**64
      @bignum2 = 255**640
   end

   def test_to_f_basic
      assert_respond_to(@bignum1, :to_f)
      assert_nothing_raised{ @bignum1.to_f }
   end

   # I'm not sure how to use assert_in_delta for numbers this big
   #
   def test_to_f
      possible = ['1.84467440737096e+19', '1.84467440737096e+019', '1.8446744073709552e+19']
      assert(possible.include?(@bignum1.to_f.to_s))
      #assert_equal('Infinity', @bignum2.to_f.to_s) # Passes, but noisily
   end

   def test_to_f_expected_errors
      assert_raise(ArgumentError){ @bignum1.to_f(1.0) }
   end

   def teardown
      @bignum1 = nil
      @bignum2 = nil
   end
end
