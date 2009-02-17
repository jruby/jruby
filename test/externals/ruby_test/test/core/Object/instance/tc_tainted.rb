########################################################################
# tc_tainted.rb
#
# Test case for the Object#tainted? instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Object_Tainted_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @object = Object.new
   end

   def test_tainted_basic
      assert_respond_to(@object, :tainted?)
      assert_nothing_raised{ @object.tainted? }
      assert_kind_of(Boolean, @object.tainted?)
   end

   def test_tainted
      assert_equal(false, @object.tainted?)
      assert_nothing_raised{ @object.taint }
      assert_equal(true, @object.tainted?)
   end

   def test_tainted_expected_errors
      assert_raise(ArgumentError){ @object.tainted?(false) }
   end

   def teardown
      @object = nil
   end
end
