###############################################################################
# tc_exception.rb
#
# Test case for the Exception#exception instance method.
###############################################################################
require 'test/unit'

class TestException < Exception; end

class TC_Exception_Exception_InstanceMethod < Test::Unit::TestCase
   def setup
      @exception = TestException.new
      @object    = nil
   end

   def test_exception_basic
      assert_respond_to(@exception, :exception)
      assert_nothing_raised{ @exception.exception }
   end

   def test_exception_no_message
      assert_kind_of(TestException, @exception.exception)
      assert_equal(@exception.object_id, @exception.exception.object_id)
   end

   def test_exception_with_message
      assert_nothing_raised{ @object = @exception.exception('this is a test') }
      assert_kind_of(TestException, @object)
      assert_not_equal(@object.object_id, @exception.object_id)
      assert_not_equal(@object.message, @exception.message)
   end

   def test_exception_with_self
      assert_nothing_raised{ @exception.exception(@exception) }
      assert_nothing_raised{ @exception.exception(TestException) }
   end

   def test_exception_expected_errors
      assert_raise(ArgumentError){ @exception.message('hello', 'world') }
   end

   def teardown
      @exception = nil
      @object    = nil
   end
end
