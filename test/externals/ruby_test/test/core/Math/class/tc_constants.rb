#####################################################################
# tc_constants.rb
#
# Test cases for constants in the Math module.
#####################################################################
require 'test/unit'

class TC_Math_Constants_Class < Test::Unit::TestCase
   def test_e
      assert_not_nil(Math::E)
      assert_kind_of(Float, Math::E)
   end

   def test_pi
      assert_not_nil(Math::PI)
      assert_in_delta(3.14, Math::PI, 0.01)
   end
end
