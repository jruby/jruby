######################################################################
# tc_threequals.rb
#
# Test case for the Range#=== instance method.
######################################################################
require 'test/unit'

class TC_Range_Threequals_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(-1, 1)
      @range2 = Range.new(-1, 1, true)
   end

   def test_threequals_basic
      assert_respond_to(@range1, :===)
      assert_nothing_raised{ @range1 === @range2 }
   end

   def test_threequals_inclusive
      assert_equal(true, @range1 === -1)
      assert_equal(true, @range1 === -1.0)
      assert_equal(true, @range1 === 0)
      assert_equal(true, @range1 === 0)
      assert_equal(true, @range1 === 1)
      assert_equal(true, @range1 === 0.5)

      assert_equal(false, @range1 === 2)
      assert_equal(false, @range1 === -2)
      assert_equal(false, @range1 === -1.1)
   end

   def test_threequals_exclusive
      assert_equal(true, @range2 === -1)
      assert_equal(true, @range2 === -1.0)
      assert_equal(true, @range2 === 0)
      assert_equal(true, @range2 === 0)
      assert_equal(true, @range2 === 0.999999)

      assert_equal(false, @range2 === 1)
      assert_equal(false, @range1 === 2)
      assert_equal(false, @range1 === -2)
      assert_equal(false, @range1 === -1.1)
   end

   def teardown
      @range1 = nil
      @range2 = nil
   end
end
