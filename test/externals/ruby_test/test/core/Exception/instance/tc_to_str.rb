###############################################################################
# tc_to_str.rb
#
# Test case for the Exception#to_str instance method.
###############################################################################
require 'test/unit'

class TC_Exception_ToStr_InstanceMethod < Test::Unit::TestCase
   def setup
      @err1 = Exception.new
      @err2 = Exception.new('hello')
      @str  = 'to_str_test_'
   end

   def test_to_str_basic
      assert_respond_to(@err1, :to_str)
      assert_nothing_raised{ @err1.to_str }
      assert_kind_of(String, @err1.to_str)
   end

   def test_to_str
      assert_equal('Exception', @err1.to_str)
      assert_equal('hello', @err2.to_str)
      assert_equal('to_str_test_Exception', @str + Exception.new)
      assert_equal('to_str_test_hello', @str + Exception.new('hello'))
   end

   def test_to_str_expected_errors
      assert_raise(ArgumentError){ @err1.to_str(true) }
   end

   def teardown
      @err1 = nil
      @err2 = nil
      @str  = nil
   end
end
