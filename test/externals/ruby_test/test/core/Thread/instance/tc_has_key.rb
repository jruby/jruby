########################################################################
# tc_has_key.rb
#
# Test case for the Thread#key? instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Thread_HasKey_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{
         Thread.current['foo'] = 'test'
         Thread.current[:bar] = 7
      }
   end

   def test_has_key_basic
      assert_respond_to(@thread, :key?)
      assert_nothing_raised{ @thread.key?(:foo) }
      assert_kind_of(Boolean, @thread.key?(:foo))
   end

   def test_has_key
#      assert_equal(true, @thread.key?(:foo))
#      assert_equal(true, @thread.key?(:bar))
      assert_equal(false, @thread.key?(:baz))
   end

   def test_has_key_expected_errors
      assert_raise(ArgumentError){ @thread.key? }
      assert_raise(ArgumentError){ @thread.key?(:foo, :bar) }
      assert_raise(TypeError){ @thread.key?([]) }
   end

   def teardown
      @thread.exit
      @thread = nil
   end
end
