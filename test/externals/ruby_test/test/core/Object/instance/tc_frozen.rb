########################################################################
# tc_frozen?.rb
#
# Test case for the Object#frozen? instance method.
########################################################################
require 'test/unit'
require 'test/helper'

class TC_Object_Frozen_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @object = Object.new
   end

   def test_frozen_basic
      assert_respond_to(@object, :frozen?)
      assert_nothing_raised{ @object.frozen? }
      assert_kind_of(Boolean, @object.frozen?)
   end

   def test_frozen
      assert_equal(false, @object.frozen?)
      assert_nothing_raised{ @object.freeze }
      assert_equal(true, @object.frozen?)
   end

   def test_frozen_expected_errors
      assert_raise(ArgumentError){ @object.frozen?(true) }
   end

   def teardown
      @object = nil
   end
end
