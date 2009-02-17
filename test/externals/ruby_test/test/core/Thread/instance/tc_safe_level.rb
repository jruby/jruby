########################################################################
# tc_safe_level.rb
#
# Test case for the Thread#safe_level instance method.
########################################################################
require 'test/unit'

class TC_Thread_SafeLevel < Test::Unit::TestCase
   def setup
      @thread0 = Thread.new{ sleep }
      @thread1 = Thread.new{ $SAFE = 1; sleep }
      @thread2 = Thread.new{ $SAFE = 2; sleep }
      @thread3 = Thread.new{ $SAFE = 3; sleep }
      @thread4 = Thread.new{ $SAFE = 4; sleep }
   end

   def test_safe_level_basic
      assert_respond_to(@thread0, :safe_level)
      assert_nothing_raised{ @thread0.safe_level }
      assert_kind_of(Integer, @thread0.safe_level)
   end

   def test_safe_level
      assert_equal(0, @thread0.safe_level)
      assert_equal(1, @thread1.safe_level)
      assert_equal(2, @thread2.safe_level)
      assert_equal(3, @thread3.safe_level)
      assert_equal(4, @thread4.safe_level)
   end

   # TODO: Is this a bug?
   def test_safe_level_edge_cases
      assert_equal(99, Thread.new{ $SAFE = 99; sleep }.safe_level)
   end

   def test_safe_level_expected_errors
      assert_raise(ArgumentError){ @thread0.safe_level(1) }
   end

   def teardown
      @thread0.exit
      @thread1.exit
      @thread2.exit
      @thread3.exit
      @thread4.exit
   end
end
