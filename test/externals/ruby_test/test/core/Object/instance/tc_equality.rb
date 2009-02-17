########################################################################
# tc_equality.rb
#
# Test case for the Object#== instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Object_Equality_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @object1 = Object.new
      @object2 = Object.new
   end

   def test_equality_basic
      assert_respond_to(@object1, :==)
      assert_nothing_raised{ @object1 == @object2 }
      assert_kind_of(Boolean, @object1 == @object2)
   end

   def test_equality
      assert_equal(true, @object1 == @object1)
      assert_equal(false, @object1 == @object2)
   end

   def test_equality_with_other_types_of_objects
      assert_equal(false, @object1 == [])
      assert_equal(false, @object1 == nil)
      assert_equal(false, @object1 == true)
      assert_equal(false, @object1 == false)
      assert_equal(false, [] == @object1)
   end

   def test_equality_expected_errors
      assert_raise(ArgumentError){ @object1.send(:==) }
      assert_raise(ArgumentError){ @object1.send(:==, @object2, 1) }
   end

   def teardown
      @object1 = nil
      @object2 = nil
   end
end
