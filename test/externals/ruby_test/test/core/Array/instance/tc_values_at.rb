##############################################################################
# tc_values_at.rb
#
# Test suite for the Array#values_at instance method.
##############################################################################
require "test/unit"

class TC_Array_ValuesAt_Instance < Test::Unit::TestCase
   def setup
      @array  = %w/a b c d e f/
      @nested = [[1,2], 3, ['a', 'b','c']]
   end

   def test_basic
      assert_respond_to(@array, :values_at)
      assert_nothing_raised{ @array.values_at }
   end

   def test_values_at
      assert_equal(["b","d","f"], @array.values_at(1, 3, 5))
      assert_equal(["b","d","f"], @array.values_at(1.0, 3.0, 5.0))
      assert_equal(["b","d","f",nil], @array.values_at(1, 3, 5, 7))
      assert_equal(["f","d","b",nil], @array.values_at(-1, -3, -5, -7))
      assert_equal(["b","c","d","c","d","e"], @array.values_at(1..3, 2...5))      
   end
   
   def test_values_at_nested
      assert_equal([[1,2]], @nested.values_at(0))
      assert_equal([['a', 'b', 'c'], 3, [1,2]], @nested.values_at(2, 1, 0))
      assert_equal([[1,2], 3, ['a', 'b','c']], @nested.values_at(0..2))
      assert_equal([[1,2], 3, ['a', 'b','c'], nil], @nested.values_at(0..3))
   end
   
   def test_values_at_edge_cases
      assert_equal([nil], @array.values_at(99))
      assert_equal([nil], [nil].values_at(0))
      assert_equal([true], [true].values_at(0))
      assert_equal([false], [false].values_at(0))
      assert_equal([0], [0, 0, 0].values_at(0))
   end

   def test_expected_errors
      assert_raises(TypeError){ @array.values_at(nil) }
      assert_raises(TypeError){ @array.values_at([]) }
      assert_raises(TypeError){ @array.values_at("foo") }
      assert_raises(TypeError){ @array.values_at(true) }
   end

   def teardown
      @array  = nil
      @nested = nil
   end
end
