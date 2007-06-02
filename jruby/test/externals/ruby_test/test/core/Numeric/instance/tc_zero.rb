#####################################################################
# tc_zero.rb
#
# Test case for the Numeric#zero? instance method.
#####################################################################
require 'test/unit'

class TC_Numeric_Zero_InstanceMethod < Test::Unit::TestCase
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
      assert_equal(false, @nonzero.zero?)
   end
   
   def teardown
      @zero    = nil
      @nonzero = nil
   end
end