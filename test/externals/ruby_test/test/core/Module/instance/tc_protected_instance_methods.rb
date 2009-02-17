########################################################################
# tc_protected_instance_methods.rb
#
# Test case for the Module#protected_instance_methods instance method.
########################################################################
require 'test/unit'

module ProtInstMethA
   private
      def m_private
      end

   protected
      def m_protected
      end
   
   public
      def m_public
      end
end

class ProtInstMethB
   include ProtInstMethA
end

class ProtInstMethC < ProtInstMethB
end

class TC_Module_ProtectedInstanceMethods_InstanceMethod < Test::Unit::TestCase
   def test_protected_instance_methods_basic
      assert_respond_to(ProtInstMethA, :protected_instance_methods)
      assert_nothing_raised{ ProtInstMethA.protected_instance_methods }
      assert_kind_of(Array, ProtInstMethA.protected_instance_methods)
   end

   def test_protected_instance_methods
      assert_equal(['m_protected'], ProtInstMethA.protected_instance_methods)
      assert_equal(true, ProtInstMethB.protected_instance_methods.include?('m_protected'))
      assert_equal(true, ProtInstMethC.protected_instance_methods.include?('m_protected'))
   end

   def test_protected_instance_methods_with_super
      assert_equal(['m_protected'], ProtInstMethA.protected_instance_methods(false))
      assert_equal([], ProtInstMethB.protected_instance_methods(false))
      assert_equal([], ProtInstMethC.protected_instance_methods(false))
   end

   def test_protected_instance_methods_expected_errors
      assert_raise(ArgumentError){ ProtInstMethA.protected_instance_methods(true, false) }
   end
end
