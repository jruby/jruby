########################################################################
# tc_alive?.rb
#
# Test case for the Thread#alive? instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Thread_Alive_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @thread = Thread.new{ sleep 1 while true }
   end

   def test_alive_basic
      assert_respond_to(@thread, :alive?)
      assert_kind_of(Boolean, @thread.alive?)
   end

   def test_alive
      assert_equal(true, @thread.alive?)
      assert_nothing_raised{ @thread.exit }
      assert_equal(false, @thread.alive?)
   end

   def test_alive?_expected_errors
      assert_raise(ArgumentError){ Thread.alive?(true) }
   end

   def teardown
      @thread.exit
      @thread = nil
   end
end
