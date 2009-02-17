########################################################################
# tc_group.rb
#
# Test case for the Thread#group instance method.
########################################################################
require 'test/unit'

class TC_Thread_Group_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{ sleep 1 while true }
      @thread_group = ThreadGroup.new
   end

   def test_group_basic
      assert_respond_to(@thread, :group)
      assert_nothing_raised{ @thread.group }
      assert_kind_of(ThreadGroup, @thread.group)
   end

   def test_group
      assert_nothing_raised{ @thread_group.add(@thread) }
      assert_equal(@thread_group, @thread.group)
   end

   def test_group_expected_errors
      assert_raise(ArgumentError){ @thread.group(1) }
   end

   def teardown
      @thread.exit
      @thread = nil
      @thread_group = nil
   end
end
