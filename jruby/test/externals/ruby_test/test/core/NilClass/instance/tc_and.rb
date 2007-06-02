###################################################################
# tc_and.rb
#
# Test suite for the NilClass#& instance method.
###################################################################
require "test/unit"

class TC_NilClass_And_Instance < Test::Unit::TestCase
   def setup
      @x = nil
   end
   
   def test_basic
      assert_respond_to(@x, :&)
      assert_nothing_raised{ @x & "hello" }
      assert_equal(false, @x & "hello")
      assert_equal(false, @x & @x)
   end
   
   def test_edge_cases
      assert_equal(false, @x & true)
      assert_equal(false, @x & nil)
      assert_equal(false, @x & 0)
      assert_equal(false, @x & false)
   end
end