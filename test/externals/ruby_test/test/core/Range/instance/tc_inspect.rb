######################################################################
# tc_inspect.rb
#
# Test case for the Range#inspect instance method. I've added this as
# a separate test because range.c has a custom implementation.
######################################################################
require 'test/unit'

class TC_Range_Inspect_InstanceMethod < Test::Unit::TestCase
   def setup
      @range1 = Range.new(0, 1, false)
      @range2 = Range.new(0, 1, true)
   end

   def test_inspect_basic
      assert_respond_to(@range1, :inspect)
      assert_nothing_raised{ @range1.inspect }
   end

   def test_inspect
      assert_equal('0..1', @range1.inspect)
      assert_equal('0...1', @range2.inspect)
      assert_equal('0..0', Range.new(0, 0).inspect)
      assert_equal('0...0', Range.new(0, 0, true).inspect)
   end

   def test_inspect_edge_cases
      assert_equal('""..""', Range.new('', '').inspect)
      assert_equal('[]..[]', Range.new([], []).inspect)
      assert_equal('[]..[1]', Range.new([], [1]).inspect)
      assert_equal('[1]..[]', Range.new([1], []).inspect)
   end

   def test_inspect_expected_failures
      assert_raises(ArgumentError){ @range1.inspect(0) }
   end

   def teardown
      @range1 = nil
      @range2 = nil
   end
end
