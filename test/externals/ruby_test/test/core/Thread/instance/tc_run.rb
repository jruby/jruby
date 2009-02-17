########################################################################
# tc_run.rb
#
# Test case for the Thread#run instance method.
########################################################################
require 'test/unit'

class TC_Thread_Run_InstanceMethod < Test::Unit::TestCase

   # For the setup I've created multiple break points to ensure that a call
   # to Thread#run only runs up to the next breakpoint.
   #
   def setup
      @num1 = 0
      @num2 = 0
      @thread = Thread.new{
         Thread.stop
         @num1 = 88
         Thread.stop
         @num2 = 99
      }
   end

   def test_run_basic
      assert_respond_to(@thread, :run)
      assert_nothing_raised{ @thread.run }
   end

   def test_run
      assert_equal(0, @num1)
      assert_equal(0, @num2)

      assert_nothing_raised{ @thread.run }
      assert_equal(88, @num1)
      assert_equal(0, @num2)

      assert_nothing_raised{ @thread.run }
      assert_equal(88, @num1)
      assert_equal(99, @num2)
   end

   def test_run_expected_errors
      assert_raise(ArgumentError){ @thread.run(1) }
   end

   def teardown
      @thread.exit
      @thread = nil
   end
end
