######################################################################
# tc_begin.rb
#
# Test case for the Range#begin instance method.
######################################################################
require 'test/unit'

class TC_Range_Begin_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(0, 1)
      @range2 = Range.new(1, 0)
      @range3 = Range.new(-1.5, 1.5)
      @range4 = Range.new('a', 'Z')
      @range5 = Range.new([], [1,2,3])
   end

   def test_begin_basic
      assert_respond_to(@range1, :begin)
      assert_nothing_raised{ @range1.begin }
   end

   def test_begin
      assert_equal(0, @range1.begin)
      assert_equal(1, @range2.begin)
      assert_equal(-1.5, @range3.begin)
      assert_equal('a', @range4.begin)
      assert_equal([], @range5.begin)
   end

   def test_begin_expected_failures
      assert_raises(ArgumentError){ @range1.begin(0) }
   end

   def teardown
      @range1 = nil
      @range2 = nil
      @range3 = nil
      @range4 = nil
      @range5 = nil
   end
end
