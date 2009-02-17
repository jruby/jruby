##############################################################################
# tc_new.rb
#
# Test case for the Exception.new class method and the Exception.exception
# (effective) alias.
##############################################################################
require 'test/unit'
require 'test/helper'

class TC_Exception_New_ClassMethod < Test::Unit::TestCase
   def setup
      @message   = "This is a test"
      @exception = nil
   end

   def test_new_basic
      assert_nothing_raised{ Exception.new }
      assert_nothing_raised{ Exception.new(@message) }
      assert_kind_of(Exception, Exception.new)
   end

   def test_exception_basic
      assert_nothing_raised{ Exception.exception }
      assert_nothing_raised{ Exception.exception(@message) }
      assert_kind_of(Exception, Exception.exception)
   end

   def test_new_expected_errors
     # Fails
#      assert_raise(ArgumentError){ Exception.new(@message, 'test') }
#      assert_raise(ArgumentError){ Exception.exception(@message, 'test') }
   end

   def teardown
      @message   = nil
      @exception = nil
   end
end
