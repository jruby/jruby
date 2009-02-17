############################################################################
# tc_difference.rb
#
# Test suite for the Array#- instance method. Note that we define a class
# with a custom to_ary method to ensure that Array#- uses it properly.
############################################################################
require "test/unit"

class TC_Array_Difference_InstanceMethod < Test::Unit::TestCase
   class ADiff
      def to_ary
         [1, 2]
      end
   end

   def setup
      @array1 = [1, 2, 2, 3, 3, 3]
      @array2 = [2, 3]
      @array3 = [1, "hello", "world", nil, true, false]
      @array4 = [nil, true, false]
      @nested = [[1,2], ['hello', 'world']]
      @custom = ADiff.new
   end

   def test_difference_custom_to_ary_method
      assert_nothing_raised{ @array1 - @custom }
      assert_equal([3, 3, 3], @array1 - @custom)
   end

   def test_difference_basic
      assert_respond_to(@array1, :-)
      assert_nothing_raised{ @array1 - @array2 }
      assert_nothing_raised{ @array2 - @array1 }
      assert_kind_of(Array, @array1 - @array2)
   end

   def test_difference_numbers_only
      assert_equal([1], @array1 - @array2)
      assert_equal([], @array2 - @array1)
   end

   def test_difference_mixed_types
      assert_nothing_raised{ @array3 - @array4 }
      assert_nothing_raised{ @array4 - @array3 }
      assert_equal([1, "hello", "world"], @array3 - @array4)
      assert_equal([], @array4 - @array3)
   end

   def test_difference_nested
      assert_equal([], @nested - [['hello','world'], [1,2]])
      assert_equal([[1,2]], @nested - [['hello','world']])
      assert_equal([[1,2], ['hello','world']], @nested - [1,2,'hello','world'])
   end

   # Ensure that a new array is created when diff'ing two arrays, and
   # that the original arrays are unchanged.
   def test_difference_new_array
      assert_nothing_raised{ @array1 - @array2 }
      assert_equal([1, 2, 2, 3, 3, 3], @array1)
      assert_equal([2, 3], @array2)
   end

   def test_difference_edge_cases
      assert_equal([2, 2, 3, 3, 3], @array1 - [1, 1, 1, 1, 1])
      assert_equal([1, 2, 2, 3, 3, 3], @array1 - [])
      assert_equal([1, 2, 2, 3, 3, 3], @array1 - [nil])
      assert_equal([1, 2, 2, 3, 3, 3], @array1 - [[1, 2, 2, 3, 3, 3]])
      assert_equal([], [[],[],[]] - [[]])
   end

   def test_difference_expected_errors
      assert_raises(TypeError){ @array1 - nil }
      assert_raises(TypeError){ @array1 - 1 }
      assert_raises(TypeError){ @array1 - "hello" }
      assert_raises(ArgumentError){ @array1.send(:-, @array2, @array3) }
   end

   def teardown
      @array1 = nil
      @array2 = nil
      @array3 = nil
      @array4 = nil
      @nested = nil
   end
end
