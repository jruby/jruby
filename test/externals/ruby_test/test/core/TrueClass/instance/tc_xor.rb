#####################################################################
# tc_xor.rb
#
# Test case for 'true ^'.
#####################################################################
require 'test/unit'

class TC_TrueClass_ExclusiveOr_InstanceMethod < Test::Unit::TestCase
   def test_xor_basic
      assert_respond_to(true, :^)
      assert_nothing_raised{ true.^(0) }
      assert_nothing_raised{ true ^ 0 }
   end

   def test_xor
      assert_equal(false, true ^ 0)
      assert_equal(false, true ^ 1)
      assert_equal(false, true ^ true)

      assert_equal(true, true ^ false)
      assert_equal(true, true ^ nil)
   end

   def test_xor_expected_errors
      assert_raises(ArgumentError){ true.^ }
   end
end
