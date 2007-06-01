#####################################################################
# tc_nonzero.rb
#
# Test case for the Numeric#nonzero? instance method.
#####################################################################
require 'test/unit'

class TC_Numeric_Nonzero_InstanceMethod < Test::Unit::TestCase
   def setup
      @zero    = 0
      @nonzero = 1
   end
   
   def test_nonzero_basic
      assert_respond_to(@zero, :nonzero?)
      assert_nothing_raised{ @zero.nonzero? }
   end
   
   def test_nonzero
      assert_nil(nil, @zero.nonzero?)
      assert_equal(1, @nonzero.nonzero?)
   end
   
   def teardown
      @zero    = nil
      @nonzero = nil
   end
end