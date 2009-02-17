########################################################################
# tc_taint.rb
#
# Test case for the Object#taint instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Object_Taint_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @object = Object.new
   end

   def test_taint_basic
      assert_respond_to(@object, :taint)
      assert_nothing_raised{ @object.taint }
   end

   def test_taint
      assert_equal(@object, @object.taint)
      assert_equal(true, @object.tainted?)
   end

   def test_taint_frozen_objects
      assert_nothing_raised{ @object.freeze }
      assert_raise(TypeError){ @object.taint }
   end

   unless JRUBY
      def test_taint_safe_environment
         assert_nothing_raised{
            proc do
               $SAFE = 3
               @object.taint
            end.call
         }
         assert_raise(SecurityError){
            proc do
               $SAFE = 4
               @object.taint
            end.call
         }
      end
   end

   def test_taint_expected_errors
      assert_raise(ArgumentError){ @object.taint(true) }
   end

   def teardown
      @object = nil
   end
end
