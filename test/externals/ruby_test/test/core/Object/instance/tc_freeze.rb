########################################################################
# tc_freeze.rb
#
# Test case for the Object#freeze instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Object_Freeze_InstanceMethod < Test::Unit::TestCase
   include Test::Helper
   
   def setup
      @object = Object.new
   end

   def test_freeze_basic
      assert_respond_to(@object, :freeze)
      assert_nothing_raised{ @object.freeze }
   end

   def test_freeze
      assert_equal(@object, @object.freeze)
      assert_equal(@object, @object.freeze) # Duplicate intentional
      assert_equal(true, @object.frozen?)
   end

   unless JRUBY
      # You can freeze an object at $SAFE level 4 only if it's tainted
      def test_freeze_safe_environment
         assert_raise(SecurityError){
            proc do
               $SAFE = 4
               @object.freeze
            end.call
         }

         assert_nothing_raised{ @object.taint }

         assert_nothing_raised{
            proc do
               $SAFE = 4
               @object.freeze
            end.call
         }
      end
   end
   
   def test_freeze_expected_errors
      assert_raise(ArgumentError){ @object.freeze(true) }
   end

   def teardown
      @object = nil
   end
end
