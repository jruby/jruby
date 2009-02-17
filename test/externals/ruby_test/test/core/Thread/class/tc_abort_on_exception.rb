###############################################################################
# tc_abort_on_exception.rb
#
# Test case for the Thread.abort_on_exception and Thread.abort_on_exception=
# class methods
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Thread_AbortOnException_ClassMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @thread = nil
      Thread.abort_on_exception = false
   end

   def test_abort_on_exception_basic
      assert_respond_to(Thread, :abort_on_exception)
      assert_respond_to(Thread, :abort_on_exception=)
      assert_kind_of(Boolean, Thread.abort_on_exception)
   end

   def test_abort_on_exception
      assert_equal(false, Thread.abort_on_exception)
      assert_equal(true, Thread.abort_on_exception = true)
      assert_equal(true, Thread.abort_on_exception)
   end

   # TODO: How do I test Thread.abort_on_exception = true without breaking
   # the test suite?
   #
   def test_abort_on_exception_behavior
      assert_nothing_raised{ @thread = Thread.new{ raise 'AOE TEST' } }
      assert_raise(RuntimeError){ @thread.join }
   end

   def test_abort_on_exception_expected_errors
      assert_raise(ArgumentError){ Thread.abort_on_exception(true) }
   end

   def teardown
      @thread = nil
      Thread.abort_on_exception = false
   end
end
