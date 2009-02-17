########################################################################
# tc_hierarchy.rb
#
# Test case for the various module hierarchy operators, i.e. >, <,
# >= and <=.
########################################################################
require 'test/unit'
require 'test/helper'

# We'll use these predeclared modules in the test case
module H_Mixin_A
end

module H_Mixin_B
end

module H_Parent
   include H_Mixin_A
   include H_Mixin_B
end

module H_Unrelated
end

module H_Nested
   include H_Parent
end

class TC_Module_Hierarchy_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def test_less_than_basic
      assert_respond_to(H_Mixin_A, :<)
      assert_nothing_raised{ H_Mixin_A < H_Parent }
      assert_kind_of(Boolean, H_Mixin_A < H_Parent)
   end

   def test_less_than_or_equal_to_basic
      assert_respond_to(H_Mixin_A, :<=)
      assert_nothing_raised{ H_Mixin_A <= H_Parent }
      assert_kind_of(Boolean, H_Mixin_A <= H_Parent)
   end

   def test_greater_than_basic
      assert_respond_to(H_Mixin_A, :>)
      assert_nothing_raised{ H_Mixin_A > H_Parent }
      assert_kind_of(Boolean, H_Mixin_A > H_Parent)
   end

   def test_greater_than_or_equal_to_basic
      assert_respond_to(H_Mixin_A, :>=)
      assert_nothing_raised{ H_Mixin_A >= H_Parent }
      assert_kind_of(Boolean, H_Mixin_A >= H_Parent)
   end

   def test_less_than
      assert_equal(true, H_Parent < H_Mixin_A)
      assert_equal(false, H_Mixin_A < H_Parent)
      assert_equal(nil, H_Parent < H_Unrelated)
      assert_equal(nil, H_Mixin_A < H_Mixin_B)
      assert_equal(false, H_Parent < H_Parent)
   end

   def test_less_than_nested
      assert_equal(true, H_Nested < H_Parent)
      assert_equal(true, H_Nested < H_Mixin_A)
   end

   def test_less_than_or_equal_to
      assert_equal(true, H_Parent <= H_Mixin_A)
      assert_equal(false, H_Mixin_A <= H_Parent)
      assert_equal(nil, H_Parent <= H_Unrelated)
      assert_equal(nil, H_Mixin_A <= H_Mixin_B)
      assert_equal(true, H_Parent <= H_Parent)
   end

   def test_less_than_or_equal_to_nested
      assert_equal(true, H_Nested <= H_Parent)
      assert_equal(true, H_Nested <= H_Mixin_A)
   end

   def test_greater_than
      assert_equal(false, H_Parent > H_Mixin_A)
      assert_equal(true, H_Mixin_A > H_Parent)
      assert_equal(nil, H_Parent > H_Unrelated)
      assert_equal(nil, H_Mixin_A > H_Mixin_B)
      assert_equal(false, H_Parent > H_Parent)
   end

   def test_greater_than_nested
      assert_equal(false, H_Nested > H_Parent)
      assert_equal(false, H_Nested > H_Mixin_A)
   end

   def test_greater_than_or_equal_to
      assert_equal(false, H_Parent >= H_Mixin_A)
      assert_equal(true, H_Mixin_A >= H_Parent)
      assert_equal(nil, H_Parent >= H_Unrelated)
      assert_equal(nil, H_Mixin_A >= H_Mixin_B)
      assert_equal(true, H_Parent >= H_Parent)
   end
end
