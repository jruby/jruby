###############################################################################
# tc_to_s.rb
#
# Test case for the Exception#to_s instance method.
###############################################################################
require 'test/unit'

class TC_Exception_ToS_InstanceMethod < Test::Unit::TestCase
   def setup
      @err1 = Exception.new
      @err2 = Exception.new('hello')
   end

   def test_to_s_basic
      assert_respond_to(@err1, :to_s)
      assert_nothing_raised{ @err1.to_s }
      assert_kind_of(String, @err1.to_s)
   end

   def test_to_s
      assert_equal('Exception', @err1.to_s)
      assert_equal('hello', @err2.to_s)
   end

   # If the Exception object is tainted, so is the message
   def test_to_s_tainted
      assert_nothing_raised{ @err2.taint }
      assert_equal(true, @err2.message.tainted?)
   end

   def test_to_s_expected_errors
      assert_raise(ArgumentError){ @err1.to_s(true) }
   end

   def teardown
      @err1 = nil
      @err2 = nil
   end
end
