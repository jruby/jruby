########################################################################
# tc_join.rb
#
# Test case for the Thread#join instance method.
########################################################################
require 'test/unit'

class TC_Thread_Join_InstanceMethod < Test::Unit::TestCase
   def setup
      @file   = 'thread_join.txt'
      @thread = Thread.new{ sleep 2 }
   end

   def test_join_basic
      assert_respond_to(@thread, :join)
      assert_nothing_raised{ @thread.join }
      assert_kind_of(Thread, @thread.join)
   end

   def test_join
      assert_nothing_raised{ @thread.join }
      assert_equal(@thread, @thread.join)
   end

   def test_join_with_timeout
      assert_nil(@thread.join(0))
      assert_equal(@thread, @thread.join(5))
   end

   def test_join_expected_errors
      @thread = Thread.new{ raise 'Thread join test' }
      assert_raise(RuntimeError){ @thread.join }
      assert_raise(ArgumentError){ @thread.join(1, 2) }
   end

   def teardown
      @thread.exit
      @thread = nil
   end
end
