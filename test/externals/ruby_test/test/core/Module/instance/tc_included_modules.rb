########################################################################
# tc_included_modules.rb
#
# Test case for the Module#included_modules instance method.
########################################################################
require 'test/unit'

module IncludedMod_A; end

module IncludedMod_B
   include IncludedMod_A
end

class Included_Class_A
   include IncludedMod_B
end

class TC_Module_IncludeModules_InstanceMethod < Test::Unit::TestCase
   def test_included_modules_basic
      assert_respond_to(IncludedMod_A, :included_modules)
      assert_nothing_raised{ IncludedMod_A.included_modules }
      assert_kind_of(Array, IncludedMod_A.included_modules)
   end

   def test_included_modules
      assert_equal([], IncludedMod_A.included_modules)
      assert_equal([IncludedMod_A], IncludedMod_B.included_modules)

      assert_equal(
         [IncludedMod_B, IncludedMod_A, Config, Kernel],
         Included_Class_A.included_modules
      )
   end

   def test_included_modules_expected_errors
      assert_raise(ArgumentError){ IncludedMod_A.included_modules(true) }
   end
end
