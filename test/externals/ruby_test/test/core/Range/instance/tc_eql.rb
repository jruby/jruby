######################################################################
# tc_eql.rb
#
# Test case for the Range#eql? instance method.
######################################################################
require 'test/unit'

class TC_Range_Eql_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(1, 25)
      @range2 = Range.new('a', 'z')
   end

   def test_eql_basic
      assert_respond_to(@range1, :eql?)
      assert_nothing_raised{ @range1.eql?(@range1) }
      assert_nothing_raised{ @range1.eql?(@range2) }
   end

   def test_eql_numeric
      assert_equal(true, @range1.eql?(Range.new(1, 25)))
      assert_equal(false, @range1.eql?(Range.new(1.0, 25)))
      assert_equal(false, @range1.eql?(Range.new(1, 25, true)))
   end

   def test_eql_alphabetic
      assert_equal(true, @range2.eql?(Range.new('a', 'z')))
      assert_equal(false, @range2.eql?(Range.new('a', 'z', true)))
   end

   def test_eql_edge_cases
      assert_equal(true, Range.new([], []).eql?(Range.new([], [])))
      assert_equal(false, @range1.eql?(7))
   end

   def test_eql_against_non_range
      assert_equal(false, @range1.eql?('hello'))
   end

   def teardown
      @range1 = nil
      @range2 = nil
   end
end
