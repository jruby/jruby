###########################################################
# tc_trueclass.rb
#
# Test case for the TrueClass itself.
###########################################################
require 'test/unit'

class TC_TrueClass < Test::Unit::TestCase
   def test_true
      assert_kind_of(TrueClass, true)
      assert_kind_of(TrueClass, TRUE)
   end

   def test_true_constant
      assert(true == TRUE)
      assert(true != FALSE)
   end

   def test_true_to_s
      assert_equal('true', true.to_s)
      assert_equal('true', TRUE.to_s)
   end
end
