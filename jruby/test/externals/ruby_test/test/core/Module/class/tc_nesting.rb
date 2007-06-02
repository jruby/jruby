######################################################################
# tc_nesting.rb
#
# Test case for the Module.nesting module method.
######################################################################
require 'test/unit'

$nesting1 = Module.nesting

module NestingModule
   $nesting2 = Module.nesting
   class One
      $nesting3 = Module.nesting
   end
end

class TC_Module_Nesting_ModuleMethod < Test::Unit::TestCase
   def test_nesting_basic
      assert_respond_to(Module, :nesting)
      assert_nothing_raised{ Module.nesting }
      assert_kind_of(Array, Module.nesting)
   end

   def test_nesting_self
      assert_equal([TC_Module_Nesting_ModuleMethod], Module.nesting)
   end

   def test_nesting
      assert_equal([], $nesting1)
      assert_equal([NestingModule], $nesting2)
      assert_equal([NestingModule, NestingModule::One],
         $nesting3.sort_by{ |x| x.to_s }
      )
   end

   def test_nesting_expected_errors
      assert_raises(ArgumentError){ Module.nesting(1) }
   end
end
