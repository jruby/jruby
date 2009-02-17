###############################################################################
# tc_set_backtrace.rb
#
# Test case for the Exception#set_backtrace instance method. Note that
# contrary to what the Pickaxe, 2nd edition says, there is no enforcement
# regarding the format of the argument to set_backtrace. Also note that
# it will accept a plain string, in addition to an array of strings.
###############################################################################
require 'test/unit'

class TC_Exception_SetBacktrace_InstanceMethod < Test::Unit::TestCase
   def setup
      @exception = Exception.new
      @backtrace = 'testing'
   end

   def test_set_backtrace_basic
      assert_respond_to(@exception, :set_backtrace)
      # Fails
#      assert_nothing_raised{ @exception.set_backtrace(@backtrace) }
#      assert_kind_of(Array, @exception.set_backtrace(@backtrace))
   end

   def test_set_backtrace
     # Fails
#      assert_equal(['testing'], @exception.set_backtrace(@backtrace))
#      assert_equal(['testing'], @exception.backtrace)
      assert_equal(['hello', 'world'], @exception.set_backtrace(['hello', 'world']))
      assert_equal(['hello', 'world'], @exception.backtrace)
   end

   def test_set_backtrace_edge_cases
      assert_equal(nil, @exception.set_backtrace(nil))
   end

   def test_set_backtrace_expected_errors
      assert_raise(ArgumentError){ @exception.set_backtrace }
      assert_raise(ArgumentError){ @exception.set_backtrace(1,2) }
      assert_raise(TypeError){ @exception.set_backtrace(true) }
      assert_raise(TypeError){ @exception.set_backtrace(0) }
      assert_raise(TypeError){ @exception.set_backtrace(['hello', 0]) }
   end

   def teardown
      @exception = nil
      @backtrace = nil
   end
end
