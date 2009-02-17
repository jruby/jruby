########################################################################
# tc_keys.rb
#
# Test case for the Thread#keys instance method.
########################################################################
require 'test/unit'

class TC_Thread_Keys_InstanceMethod < Test::Unit::TestCase
   def setup
      @thread = Thread.new{
         Thread.current['foo'] = 'test'
         Thread.current[:bar] = 7
      }
   end

   def test_keys_basic
      assert_respond_to(@thread, :keys)
      assert_nothing_raised{ @thread.keys }
      assert_kind_of(Array, @thread.keys)
   end

   def test_keys
#      assert_equal(['bar', 'foo'], @thread.keys.map{ |k| k.to_s }.sort)
      assert_equal([], Thread.new{}.keys)
   end

   def test_keys_expected_errors
      assert_raise(ArgumentError){ @thread.keys(1) }
   end

   def teardown
      @thread.exit
      @thread = nil
   end
end
