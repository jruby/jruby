########################################################################
# tc_const_defined.rb
#
# Test case for the Module#const_defined? instance method.
########################################################################
require 'test/unit'
require 'test/helper'

module CD_Mod_A
   TEST  = 1
   EMPTY = ''
   BLANK = nil
end

class CD_Class_A
   include CD_Mod_A
end

class TC_Module_ConstDefined_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def test_const_defined_basic
      assert_respond_to(CD_Mod_A, :const_defined?)
      assert_nothing_raised{ CD_Mod_A.const_defined?('TEST') }
      assert_kind_of(Boolean, CD_Mod_A.const_defined?('TEST'))
   end

   def test_const_defined
      assert_equal(true, CD_Mod_A.const_defined?('TEST'))
      assert_equal(false, CD_Class_A.const_defined?('TEST'))
      assert_equal(false, CD_Mod_A.const_defined?('BOGUS'))
   end

   def test_const_defined_with_symbols
      assert_equal(true, CD_Mod_A.const_defined?(:TEST))
      assert_equal(false, CD_Class_A.const_defined?(:TEST))
      assert_equal(false, CD_Mod_A.const_defined?(:BOGUS))
   end

   def test_const_defined_edge_cases
      assert_equal(true, CD_Mod_A.const_defined?('EMPTY'))
      assert_equal(true, CD_Mod_A.const_defined?('BLANK'))
   end

   def test_const_defined_expected_errors
      assert_raise(ArgumentError){ CD_Mod_A.const_defined? }
      assert_raise(TypeError){ CD_Mod_A.const_defined?([]) }
      assert_raise(NameError){ CD_Mod_A.const_defined?('?') }
   end
end
