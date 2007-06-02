#####################################################################
# tc_or.rb
#
# Test case for 'false |'.
#####################################################################
require 'test/unit'

class TC_FalseClass_Or_InstanceMethod < Test::Unit::TestCase
   def test_or_basic
      assert_respond_to(false, :|)
      assert_nothing_raised{ false.|(0) }
      assert_nothing_raised{ false | 0 }
   end

   def test_or
      assert_equal(true, false | 0)
      assert_equal(true, false | 1)
      assert_equal(true, false | true)

      assert_equal(false, false | false)
      assert_equal(false, false | nil)
   end

   def test_or_expected_errors
      assert_raises(ArgumentError){ false.| }
   end
end
