########################################################################
# tc_name.rb
#
# Test case for the Module#name instance method.
########################################################################
require 'test/unit'

module NameModA
end

module NameModB
   include NameModA
end

class TC_Module_Name_InstanceMethod < Test::Unit::TestCase
   def test_name_basic
      assert_respond_to(NameModA, :name)
      assert_nothing_raised{ NameModA.name }
      assert_kind_of(String, NameModA.name)
   end

   def test_name
      assert_equal('NameModA', NameModA.name)
      assert_equal('NameModB', NameModB.name)
   end

   def test_name_expected_errors
      assert_raise(ArgumentError){ NameModA.name(true) }
   end
end
