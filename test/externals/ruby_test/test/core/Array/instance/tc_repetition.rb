################################################
# tc_repetition.rb
#
# Test suite for the Array#* instance method.
################################################
require "test/unit"

class TC_Array_Repetition_InstanceMethod < Test::Unit::TestCase
   def setup
      @array1 = [1, 2, 3]
      @array2 = ["hello", "world"]
      @array3 = [true, false, nil]
      @nested = [[1,2], ['a','b']]
   end

   def test_repetition_method_exists
      assert_respond_to(@array1, :*)
   end

   def test_repetition_numeric
      assert_nothing_raised{ @array1 * 2 }
      assert_equal([1, 2, 3, 1, 2, 3], @array1 * 2)
      assert_equal(["hello", "world", "hello", "world"], @array2 * 2)
      assert_equal([true, false, nil, true, false, nil], @array3 * 2)
   end

   def test_repetition_string_join
      assert_nothing_raised{ @array1 * "-" }
      assert_equal("1-2-3", @array1 * "-")
      assert_equal("hello-world", @array2 * "-")
      assert_equal("true-false-", @array3 * "-")
   end

   def test_repetition_nested
      assert_nothing_raised{ @nested * 2 }
      assert_equal([[1,2],['a','b'],[1,2],['a','b']], @nested * 2)
      assert_equal("1-2-a-b", @nested * "-")
   end

   def test_repetition_edge_cases
      assert_equal("", [nil] * "-")
      assert_equal("", [] * "-")
      assert_equal("false", [false] * "-")
      assert_equal([1,2,3], @array1 * 1.9)
   end

   def test_repetition_expected_errors
      assert_raises(TypeError){ @array1 * nil }
      assert_raises(TypeError){ @array1 * @array2 }
      assert_raises(TypeError){ @array1 * false }
      assert_raises(ArgumentError){ @array1 * -3 }
      assert_raises(ArgumentError, RangeError){ @array1 * (256**64) }
      assert_raises(ArgumentError){ @array1.send(:*, @array2, @array3) }
   end
   
   def teardown
      @array1 = nil
      @array2 = nil
      @array3 = nil 
      @nested = nil
   end
end
