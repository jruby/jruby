########################################################################
# tc_const_set.rb
#
# Test case for the Module#const_set instance method.
########################################################################
require 'test/unit'
require 'test/helper'

module CSet_Mod_A
end

class CSet_Class_A
   include CSet_Mod_A
end

class TC_Module_ConstSet_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def test_const_set_basic
      assert_respond_to(CSet_Mod_A, :const_set)
      assert_nothing_raised{ CSet_Mod_A.const_set('TEST_A', 7) }
      assert_kind_of(Fixnum, CSet_Mod_A.const_set('TEST_B', 8))
   end

   def test_const_set
      assert_equal(9, CSet_Mod_A.const_set('TEST_C', 9))
      assert_equal('a', CSet_Mod_A.const_set('TEST_D', 'a'))
   end

   def test_const_set_symbol
      assert_equal(9, CSet_Mod_A.const_set(:TEST_E, 9))
      assert_equal('a', CSet_Mod_A.const_set(:TEST_F, 'a'))
   end

   def test_const_set_edge_cases
      assert_equal(nil, CSet_Mod_A.const_set('TEST_NIL', nil))
      assert_equal(false, CSet_Mod_A.const_set('TEST_FALSE', false))
      assert_equal(7, CSet_Mod_A.const_set('CSet_Mod_A', 7))
   end

   def test_const_defined_expected_errors
      assert_raise(ArgumentError){ CSet_Mod_A.const_set }
      assert_raise(ArgumentError){ CSet_Mod_A.const_set('TEST') }
      assert_raise(ArgumentError){ CSet_Mod_A.const_set('TEST', 1, 2) }
      assert_raise(TypeError){ CSet_Mod_A.const_set([], 1) }
   end
end
