######################################################################
# tc_equal.rb
#
# Test case for the Range#== instance method.
######################################################################
require 'test/unit'

class TC_Range_Equal_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(1, 100)
      @range2 = Range.new('a', 'z')
   end

   def test_equal_basic
      assert_respond_to(@range1, :==)
      assert_nothing_raised{ @range1 == @range1 }
      assert_nothing_raised{ @range1 == @range2 }
   end

   def test_equal_numeric
      assert_equal(true, @range1 == Range.new(1, 100))
      assert_equal(true, @range1 == Range.new(1.0, 100))
      assert_equal(false, @range1 == Range.new(1, 100, true))
   end

   def test_equal_alphabetic
      assert_equal(true, @range2 == Range.new('a', 'z'))
      assert_equal(false, @range2 == Range.new('a', 'z', true))
   end

   def test_equal_edge_cases
      assert_equal(true, Range.new([], []) == Range.new([], []))
      assert_equal(false, @range1 == 7)
   end

   def test_equal_against_non_range
      assert_equal(false, @range1 == 'hello')
   end

   def teardown
      @range1 = nil
      @range2 = nil
   end
end
