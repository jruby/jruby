############################################################
# tc_append.rb
#
# Test suite for the Array#<< instance method.
############################################################
require "test/unit"

class TC_Array_Append_InstanceMethod < Test::Unit::TestCase
   def setup
      @array1 = [1,2,3]
      @array2 = ['hello', 'world']
      @nested = [[1,2], ['hello','world']]
   end

   def test_append_basic
      assert_respond_to(@array1, :<<)
      assert_nothing_raised{ @array1 << 4 }
      assert_nothing_raised{ @array1 << [1, 2] }
      assert_kind_of(Array, @array1 << 5)
   end

   def test_append
      assert_equal([1, 2, 3, 4], @array1 << 4)
      assert_equal([1, 2, 3, 4, [1, 2]], @array1 << [1, 2])
   end

   def test_append_chained
      assert_equal([1, 2, 3, 1, "hello", 4.0], @array1 << 1 << 'hello' << 4.0)
   end

   def test_append_nested
      assert_equal([[1,2], ['hello','world'], 3], @nested << 3)
   end

   def test_append_returns_original_array
      assert_equal(true, (@array1 << 3).object_id == @array1.object_id)
      assert_equal([1,2,3,3], @array1)
   end

   def test_append_edge_cases
      assert_equal(["hello", "world", nil], @array2 << nil)
      assert_equal(["hello", "world", nil, false], @array2 << false)
      assert_equal(["hello", "world", nil, false, true], @array2 << true)
   end

   def test_append_expected_errors
      assert_raises(ArgumentError){ @array1.send(:<<, @array2, @nested) }
   end

   def teardown
      @array1 = nil
      @array2 = nil
      @nested = nil
   end
end
