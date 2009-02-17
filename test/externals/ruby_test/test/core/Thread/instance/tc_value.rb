########################################################################
# tc_value.rb
#
# Test case for the Thread#value instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Thread_Value_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{ 2 + 2 }
      @nil_thread = Thread.new{ }
      @obj_thread = Thread.new{ %w/foo bar baz/ }
   end

   def test_value_basic
      assert_respond_to(@thread, :value)
      assert_nothing_raised{ @thread.value }
   end

   def test_value
      assert_equal(4, @thread.value)
      assert_equal(nil, @nil_thread.value)
      assert_equal(%w/foo bar baz/, @obj_thread.value)
   end

   def test_value_expected_errors
      assert_raise(ArgumentError){ @thread.value(1) }
   end

   def teardown
      @thread.exit
      @nil_thread.exit
      @obj_thread.exit

      @thread = nil
      @nil_thread = nil
      @obj_thread = nil
   end
end
