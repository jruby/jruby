###############################################################################
# tc_abort_on_exception.rb
#
# Test case for the Thread#abort_on_exception and Thread#abort_on_exception=
# instance methods
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Thread_AbortOnException_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @thread = Thread.new{ raise 'AOE TEST' }
   end

   def test_abort_on_exception_basic
      assert_respond_to(@thread, :abort_on_exception)
      assert_respond_to(@thread, :abort_on_exception=)
      assert_kind_of(Boolean, @thread.abort_on_exception)
   end

   def test_abort_on_exception
      assert_equal(false, @thread.abort_on_exception)
      assert_equal(true, @thread.abort_on_exception = true)
      assert_equal(true, @thread.abort_on_exception)
   end

   # TODO: How do I test Thread#abort_on_exception = true without breaking
   # the test suite?
   #
   def test_abort_on_exception_behavior
      assert_raise(RuntimeError){ @thread.join }
   end

   def test_abort_on_exception_expected_errors
      assert_raise(ArgumentError){ Thread.abort_on_exception(true) }
   end

   def teardown
      @thread.exit
      @thread.abort_on_exception = false
      @thread = nil
   end
end
