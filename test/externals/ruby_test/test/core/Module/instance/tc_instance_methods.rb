########################################################################
# tc_instance_methods.rb
#
# Test case for the Module#instance_methods instance method.
########################################################################
require 'test/unit'

module InstMethA
   def method_a
   end
end

class InstMethB
   include InstMethA
end

class InstMethC < InstMethB
end

class TC_Module_InstanceMethods_InstanceMethod < Test::Unit::TestCase
   def test_instance_methods_basic
      assert_respond_to(InstMethA, :instance_methods)
      assert_nothing_raised{ InstMethA.instance_methods }
      assert_kind_of(Array, InstMethA.instance_methods)
   end

   def test_instance_methods
      assert_equal(['method_a'], InstMethA.instance_methods)
      assert_equal(true, InstMethB.instance_methods.include?('method_a'))
      assert_equal(true, InstMethC.instance_methods.include?('method_a'))
   end

   def test_instance_methods_with_super
      assert_equal(['method_a'], InstMethA.instance_methods(false))
      assert_equal([], InstMethB.instance_methods(false))
      assert_equal([], InstMethC.instance_methods(false))
   end

   def test_instance_methods_expected_errors
      assert_raise(ArgumentError){ InstMethA.instance_methods(true, false) }
   end
end
