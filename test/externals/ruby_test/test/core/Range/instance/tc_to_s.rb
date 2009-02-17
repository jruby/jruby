######################################################################
# tc_to_s.rb
#
# Test case for the Range#to_s instance method. I've added this as a
# separate test because range.c has a custom implementation.
######################################################################
require 'test/unit'

class TC_Range_ToS_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(0, 1, false)
      @range2 = Range.new(0, 1, true)
   end

   def test_to_s_basic
      assert_respond_to(@range1, :to_s)
      assert_nothing_raised{ @range1.to_s }
   end

   def test_to_s
      assert_equal('0..1', @range1.to_s)
      assert_equal('0...1', @range2.to_s)
      assert_equal('0..0', Range.new(0, 0).to_s)
      assert_equal('0...0', Range.new(0, 0, true).to_s)
   end

   def test_to_s_edge_cases
      assert_nothing_raised{ Range.new('', '').to_s } # ""..""
      assert_nothing_raised{ Range.new([], []).to_s } # ""..""
      assert_equal('..1', Range.new([], [1]).to_s)
      assert_equal('1..', Range.new([1], []).to_s)
   end

   def test_to_s_expected_failures
      assert_raises(ArgumentError){ @range1.to_s(0) }
   end

   def teardown
      @range1 = nil
      @range2 = nil
   end
end
