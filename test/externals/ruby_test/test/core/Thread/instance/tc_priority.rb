########################################################################
# tc_priority.rb
#
# Test case for the Thread#priority and Thread#priority= instance
# methods.
########################################################################
require 'test/unit'

class TC_Thread_Priority_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{ sleep 1 while true }
   end

   def test_priority_basic
      assert_respond_to(@thread, :priority)
      assert_nothing_raised{ @thread.priority }
      assert_kind_of(Integer, @thread.priority)
   end

   def test_priority_set_basic
      assert_respond_to(@thread, :priority=)
      assert_nothing_raised{ @thread.priority = 5 }
      assert_kind_of(Integer, @thread.priority = 5)
   end

   # We'll combine priority and priority= here
   def test_priority
      assert_equal(0, @thread.priority) # The default
      assert_nothing_raised{ @thread.priority = 5 }
      assert_equal(5, @thread.priority)
   end

   def test_priority_edge_cases
      assert_nothing_raised{ @thread.priority = -5 }
      assert_equal(-5, @thread.priority)
      assert_nothing_raised{ @thread.priority = 2.5 }
      assert_equal(2, @thread.priority) # Converted to int
   end

   def test_priority_expected_errors
      assert_raise(ArgumentError){ @thread.priority(1) }
   end

   def test_priority_set_expected_errors
      assert_raise(ArgumentError){ @thread.send(:priority=, 1, 2) }
   end

   def teardown
      @thread.exit
      @thread = nil
   end
end
