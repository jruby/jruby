######################################################################
# tc_constants.rb
#
# Test case for the Module.constants module method.
######################################################################
require 'test/unit'

class TC_Module_Constants_ModuleMethod < Test::Unit::TestCase
   def test_constants_basic
      assert_respond_to(Module, :constants)
      assert_nothing_raised{ Module.constants }
      assert_kind_of(Array, Module.constants)
   end

   def test_constants
      assert_equal(true, Module.constants.include?('Module'))
      assert_equal(true, Module.constants.include?('Class'))
      assert_equal(true, Module.constants.include?('Object'))
      assert_equal(true, Module.constants.include?('Array'))
      assert_equal(true, Module.constants.include?('ARGV'))
      assert_equal(true, Module.constants.include?('ARGF'))
   end

   def test_constants_expected_errors
      assert_raises(ArgumentError){ Module.constants(1) }
   end
end
