#####################################################################
# tc_or.rb
#
# Test case for 'true &'.
#####################################################################
require 'test/unit'

class TC_trueClass_Or_InstanceMethod < Test::Unit::TestCase
   def test_or_basic
      assert_respond_to(true, :&)
      assert_nothing_raised{ true.&(0) }
      assert_nothing_raised{ true & 0 }
   end

   def test_or
      assert_equal(true, true & 0)
      assert_equal(true, true & 1)
      assert_equal(true, true & true)

      assert_equal(true, true & true)
      assert_equal(false, true & nil)
   end

   def test_or_expected_errors
      assert_raises(ArgumentError){ true.& }
   end
end
