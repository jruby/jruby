########################################################################
# tc_ancestors.rb
#
# Test case for the Module#ancestors instance method.
########################################################################
require 'test/unit'

module AncModA; end

class AncFoo
   module AncModB; end

   include AncModA
   include AncModB
end

class TC_Module_Ancestors_InstanceMethod < Test::Unit::TestCase
   def test_ancestors_basic
      assert_respond_to(AncFoo, :ancestors)
      assert_respond_to(AncModA, :ancestors)
      assert_respond_to(AncFoo::AncModB, :ancestors)
   end

   def test_ancestors
      assert_equal([AncFoo, AncFoo::AncModB, AncModA], AncFoo.ancestors[0..2])
      assert_equal([AncFoo::AncModB], AncFoo::AncModB.ancestors)
      assert_equal([AncModA], AncModA.ancestors)
   end
end
