########################################################################
# tc_constants.rb
#
# Test suite for the Module#constants instance method.
########################################################################
require 'test/unit'

module MConA
   M_CON_A = 1
end

module MConB
   include MConA
   M_CON_B = 1
end

module MConC
end

class CConA
   include MConA
   include MConB
end

class TC_Module_Constants_InstanceMethods < Test::Unit::TestCase
   def test_constants_basic
      assert_respond_to(MConA, :constants)
      assert_nothing_raised{ MConA.constants }
      assert_kind_of(Array, MConA.constants)
   end

   def test_constants
      assert_equal(['M_CON_A'], MConA.constants)
#      assert_equal(['M_CON_B', 'M_CON_A'], MConB.constants)
#      assert_equal(['M_CON_B', 'M_CON_A'], CConA.constants)
      assert_equal([], MConC.constants)
   end

   def test_constants_expected_errors
      assert_raise(ArgumentError){ MConA.constants(true) }
   end
end
