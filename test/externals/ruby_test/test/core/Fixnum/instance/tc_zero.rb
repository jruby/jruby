#####################################################################
# tc_zero.rb
#
# Test case for the Fixnum#zero? instance method.
#####################################################################
require 'test/unit'

class TC_Fixnum_Zero_InstanceMethod < Test::Unit::TestCase
   def setup
      @zero    = 0
      @nonzero = 1
   end
   
   def test_zero_basic
      assert_respond_to(@zero, :zero?)
      assert_nothing_raised{ @zero.zero? }
   end
   
   def test_zero
      assert_equal(true, @zero.zero?)
      assert_equal(true, -0.zero?)
      assert_equal(false, @nonzero.zero?)
   end

   def test_zero_expected_errors
      assert_raise(ArgumentError){ @zero.zero?(true) }
   end
   
   def teardown
      @zero    = nil
      @nonzero = nil
   end
end
