######################################################################
# tc_end.rb
#
# Test case for the Range#end instance method and the Range#last
# alias.
######################################################################
require 'test/unit'

class TC_Range_End_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(0, 1)
      @range2 = Range.new(1, 0)
      @range3 = Range.new(-1.5, 1.5)
      @range4 = Range.new('a', 'Z')
      @range5 = Range.new([], [1,2,3])
   end

   def test_end_basic
      assert_respond_to(@range1, :end)
      assert_nothing_raised{ @range1.end }
   end

   def test_last_alias_basic
      assert_respond_to(@range1, :last)
      assert_nothing_raised{ @range1.last }
   end

   def test_end
      assert_equal(1, @range1.end)
      assert_equal(0, @range2.end)
      assert_equal(1.5, @range3.end)
      assert_equal('Z', @range4.end)
      assert_equal([1,2,3], @range5.end)
   end

   def test_last_alias
      assert_equal(1, @range1.last)
      assert_equal(0, @range2.last)
      assert_equal(1.5, @range3.last)
      assert_equal('Z', @range4.last)
      assert_equal([1,2,3], @range5.last)
   end

   def test_end_edge_cases
      assert_equal([], Range.new([],[]).end)
      assert_equal('', Range.new('','').end)
   end

   def test_end_expected_failures
      assert_raises(ArgumentError){ @range1.end(0) }
   end

   def teardown
      @range1 = nil
      @range2 = nil
      @range3 = nil
      @range4 = nil
      @range5 = nil
   end
end
