########################################################################
# tc_exit.rb
#
# Test case for the Thread#exit instance method and the Thread#kill
# alias.
########################################################################
require 'test/unit'

class TC_Thread_Exit_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{ n = 0; n += 1 while n < 100000 }
   end

   def test_exit_basic
      assert_respond_to(@thread, :exit)
      assert_nothing_raised{ @thread.exit }
   end

   def test_kill_alias_basic
      assert_respond_to(@thread, :kill)
      assert_nothing_raised{ @thread.kill }
   end

   def test_exit
      assert_equal(@thread, @thread.exit)
      assert_equal(@thread, @thread.exit) # Duplicate intentional
   end

   def test_kill_alias
      assert_equal(@thread, @thread.kill)
      assert_equal(@thread, @thread.kill) # Duplicate intentional
   end

   def test_exit_expected_errors
      assert_raise(ArgumentError){ @thread.exit(1) }
   end

   def test_kill_alias_expected_errors
      assert_raise(ArgumentError){ @thread.kill(1) }
   end

   def teardown
      @thread.join
      @thread = nil
   end
end
