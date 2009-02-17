########################################################################
# tc_const_defined.rb
#
# Test case for the Module#const_get instance method.
########################################################################
require 'test/unit'
require 'test/helper'

module CG_Mod_A
   TEST  = 1
   EMPTY = ''
   BLANK = nil
end

class CG_Class_A
   include CG_Mod_A
end

class TC_Module_ConstGet_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def test_const_defined_basic
      assert_respond_to(CG_Mod_A, :const_get)
      assert_nothing_raised{ CG_Mod_A.const_get('TEST') }
      assert_kind_of(Integer, CG_Mod_A.const_get('TEST'))
   end

   def test_const_defined
      assert_equal(1, CG_Mod_A.const_get('TEST'))
      assert_equal(1, CG_Class_A.const_get('TEST'))
   end

   def test_const_defined_with_symbols
      assert_equal(1, CG_Mod_A.const_get(:TEST))
      assert_equal(1, CG_Class_A.const_get(:TEST))
   end

   def test_const_defined_edge_cases
      assert_equal('', CG_Mod_A.const_get('EMPTY'))
      assert_equal(nil, CG_Mod_A.const_get('BLANK'))
   end

   def test_const_defined_expected_errors
      assert_raise(NameError){ CG_Mod_A.const_get('BOGUS') }
      assert_raise(ArgumentError){ CG_Mod_A.const_get }
      assert_raise(TypeError){ CG_Mod_A.const_get([]) }
      assert_raise(NameError){ CG_Mod_A.const_get('?') }
   end
end
