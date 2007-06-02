###################################################################
# tc_or.rb
#
# Test suite for the NilClass#| instance method.
###################################################################
require "test/unit"

class TC_NilClass_Or_Instance < Test::Unit::TestCase
   def setup
      @x = nil
   end
   
   def test_basic
      assert_respond_to(@x, :|)
      assert_nothing_raised{ @x | nil }
      assert_equal(false, @x | false)
      assert_equal(false, @x | nil)
      assert_equal(true, @x | true)
      assert_equal(true, @x | 0)
      assert_equal(true, @x | "hello")
   end
end
