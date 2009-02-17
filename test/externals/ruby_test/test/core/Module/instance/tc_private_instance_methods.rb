########################################################################
# tc_private_instance_methods.rb
#
# Test case for the Module#private_instance_methods instance method.
########################################################################
require 'test/unit'

module PrivInstMethA
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

class PrivInstMethB
   include PrivInstMethA
end

class PrivInstMethC < PrivInstMethB
end

class TC_Module_PrivatenstanceMethods_InstanceMethod < Test::Unit::TestCase
   def test_private_instance_methods_basic
      assert_respond_to(PrivInstMethA, :private_instance_methods)
      assert_nothing_raised{ PrivInstMethA.private_instance_methods }
      assert_kind_of(Array, PrivInstMethA.private_instance_methods)
   end

   def test_private_instance_methods
      assert_equal(['m_private'], PrivInstMethA.private_instance_methods)
      assert_equal(true, PrivInstMethB.private_instance_methods.include?('m_private'))
      assert_equal(true, PrivInstMethC.private_instance_methods.include?('m_private'))
   end

   def test_private_instance_methods_with_super
      assert_equal(['m_private'], PrivInstMethA.private_instance_methods(false))
      assert_equal([], PrivInstMethB.private_instance_methods(false))
      assert_equal([], PrivInstMethC.private_instance_methods(false))
   end

   def test_private_instance_methods_expected_errors
      assert_raise(ArgumentError){ PrivInstMethA.private_instance_methods(true, false) }
   end
end
