########################################################################
# tc_public_instance_methods.rb
#
# Test case for the Module#public_instance_methods instance method.
########################################################################
require 'test/unit'

module PubInstMethA
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

class PubInstMethB
   include PubInstMethA
end

class PubInstMethC < PubInstMethB
end

class TC_Module_PublicInstanceMethods_InstanceMethod < Test::Unit::TestCase
   def test_public_instance_methods_basic
      assert_respond_to(PubInstMethA, :public_instance_methods)
      assert_nothing_raised{ PubInstMethA.public_instance_methods }
      assert_kind_of(Array, PubInstMethA.public_instance_methods)
   end

   def test_public_instance_methods
      assert_equal(['m_public'], PubInstMethA.public_instance_methods)
      assert_equal(true, PubInstMethB.public_instance_methods.include?('m_public'))
      assert_equal(true, PubInstMethC.public_instance_methods.include?('m_public'))
   end

   def test_public_instance_methods_with_super
      assert_equal(['m_public'], PubInstMethA.public_instance_methods(false))
      assert_equal([], PubInstMethB.public_instance_methods(false))
      assert_equal([], PubInstMethC.public_instance_methods(false))
   end

   def test_public_instance_methods_expected_errors
      assert_raise(ArgumentError){ PubInstMethA.public_instance_methods(true, false) }
   end
end
