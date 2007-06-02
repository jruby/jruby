######################################################################
# tc_constants.rb
#
# Test case for the constants associated with the Float class.
######################################################################
require 'test/unit'

class Test_Float_Constants < Test::Unit::TestCase
   def test_float_constants
      assert_not_nil(Float::DIG)
      assert_not_nil(Float::EPSILON)
      assert_not_nil(Float::MANT_DIG)
      assert_not_nil(Float::MAX)
      assert_not_nil(Float::MAX_10_EXP)
      assert_not_nil(Float::MAX_EXP)
      assert_not_nil(Float::MIN)
      assert_not_nil(Float::MIN_10_EXP)
      assert_not_nil(Float::MIN_EXP)
      assert_not_nil(Float::RADIX)
   end

   def test_float_rounds
      assert_not_nil(Float::ROUNDS)
      assert_equal(true, [-1,0,1,2,3].include?(Float::ROUNDS))
   end
end
