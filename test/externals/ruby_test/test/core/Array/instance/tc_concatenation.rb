##########################################################################
# tc_concatenation.rb
#
# Test suite for the Array#+ instance method. I've added a class with a
# to_ary method to ensure that Array#+ handles it properly.
##########################################################################
require "test/unit"

class TC_Array_Concatenation_InstanceMethod < Test::Unit::TestCase
   class AConcatenation
      def to_ary
         ['x', 'y', 'z']
      end
   end

   def setup
      @array_int1 = [1,2,3]
      @array_chr  = ["hello", "world"]
      @array_mix  = [true, false, nil]
      @array_int2 = [2,3,4]
      @nested     = [[1,2],['a','b']]
      @custom     = AConcatenation.new
   end

   def test_concatentation_basic
      assert_respond_to(@array_int1, :+)
      assert_nothing_raised{ @array_int1 + @array_mix }
      assert_kind_of(Array, @array_int1 + @array_chr)
   end

   def test_concatenation
      assert_equal([1, 2, 3, "hello", "world"], @array_int1 + @array_chr)
      assert_equal([1, 2, 3, true, false, nil], @array_int1 + @array_mix)
      assert_equal([1, 2, 3, 2, 3, 4], @array_int1 + @array_int2)
      assert_equal([1, 2, 3, [1,2], ['a','b']], @array_int1 + @nested)
      assert_equal([1, 2, 3, "hello", "world", true, false, nil],
         @array_int1 + @array_chr + @array_mix
      )
   end

   def test_concatenation_custom_to_ary
      assert_equal([1, 2, 3, 'x', 'y', 'z'], @array_int1 + @custom)
   end

   # Ensure that a new array is created when two or more arrays are
   # concatenated, and that the originals are unchanged.
   def test_concatenation_new_array
      assert_nothing_raised{ @array_int1 + @array_chr }
      assert_equal([1, 2, 3], @array_int1)
      assert_equal(["hello", "world"], @array_chr)
   end

   def test_concatenation_edge_cases
      assert_equal([1, 2, 3], @array_int1 + [])
      assert_equal([1, 2, 3, []], @array_int1 + [[]])
      assert_equal([1, 2, 3, nil], @array_int1 + [nil])
      assert_equal([1, 2, 3, false], @array_int1 + [false])
   end

   def test_concatenation_expected_errors
      assert_raises(TypeError){ @array_int1 + nil }
      assert_raises(TypeError){ @array_int1 + 1 }
      assert_raises(TypeError){ @array_int1 + "hello" }
   end

   def teardown
      @array_int1 = nil
      @array_chr  = nil
      @array_mix  = nil
      @array_int2 = nil
      @nested     = nil
      @custom     = nil
   end
end
