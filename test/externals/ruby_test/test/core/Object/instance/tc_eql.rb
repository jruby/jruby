########################################################################
# tc_eql.rb
#
# Test case for the Object#== instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Object_Eql_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @object1 = Object.new
      @object2 = Object.new
   end

   def test_eql_basic
      assert_respond_to(@object1, :eql?)
      assert_nothing_raised{ @object1.eql?(@object2) }
      assert_kind_of(Boolean, @object1.eql?(@object2))
   end

   def test_eql
      assert_equal(true, @object1.eql?(@object1))
      assert_equal(false, @object1.eql?(@object2))
   end

   def test_eql_with_other_types_of_objects
      assert_equal(false, @object1.eql?([]))
      assert_equal(false, @object1.eql?(nil))
      assert_equal(false, @object1.eql?(true))
      assert_equal(false, @object1.eql?(false))
      assert_equal(false, [].eql?(@object1))
   end

   def test_eql_expected_errors
      assert_raise(ArgumentError){ @object1.send(:eql?) }
      assert_raise(ArgumentError){ @object1.send(:eql?, @object2, 1) }
   end

   def teardown
      @object1 = nil
      @object2 = nil
   end
end
