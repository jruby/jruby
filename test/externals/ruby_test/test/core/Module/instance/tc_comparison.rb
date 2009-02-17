########################################################################
# tc_comparison.rb
#
# Test case for the Module#<=> instance method.
########################################################################
require 'test/unit'

# We'll use these predeclared modules in the test case
module Compare_Mixin_A
end

module Compare_Mixin_B
end

module Compare_Parent
   include Compare_Mixin_A
   include Compare_Mixin_B
end

module Compare_Unrelated
end

module Compare_Nested
   include Compare_Parent
end

class TCompare_Module_Comparison_InstanceMethod < Test::Unit::TestCase
   def test_comparison_basic
      assert_respond_to(Compare_Mixin_A, :<=>)
      assert_nothing_raised{ Compare_Mixin_A <=> Compare_Parent }
      assert_kind_of(Integer, Compare_Mixin_A <=> Compare_Parent)
   end

   def test_comparison
      assert_equal(1, Compare_Mixin_A <=> Compare_Parent)
      assert_equal(1, Compare_Mixin_B <=> Compare_Parent)
      assert_equal(nil, Compare_Unrelated <=> Compare_Parent)
      assert_equal(-1, Compare_Parent <=> Compare_Mixin_A)
      assert_equal(-1, Compare_Parent <=> Compare_Mixin_B)
      assert_equal(nil, Compare_Parent <=> Compare_Unrelated)
      assert_equal(0, Compare_Parent <=> Compare_Parent)
   end

   def test_comparison_nested
      assert_equal(-1, Compare_Nested <=> Compare_Parent)
      assert_equal(-1, Compare_Nested <=> Compare_Mixin_A)
      assert_equal(-1, Compare_Nested <=> Compare_Mixin_B)
   end

   def test_comparison_edge_cases
      assert_equal(nil, Compare_Parent <=> 'a')
      assert_equal(nil, Compare_Parent <=> 1)
      assert_equal(nil, Compare_Parent <=> nil)
   end

   def test_comparison_expected_errors
      assert_raise(ArgumentError){ Compare_Parent.send(:<=>) }
      assert_raise(ArgumentError){ Compare_Parent.send(:<=>, 1, 1) }
   end
end
