#####################################################
# tc_insert.rb
#
# Test suite for the Array#insert instance method.
#####################################################
require "test/unit"

class TC_Array_Insert_Instance < Test::Unit::TestCase
   def setup
      @array = ["a", "b", "c"]
   end

   def test_insert_basic
      assert_respond_to(@array, :insert)
      assert_nothing_raised{ @array.insert(1,1) }
   end

   def test_insert_one_value
      assert_equal(["a","b",7,"c"], @array.insert(2,7))
   end

   def test_insert_multiple_values
      assert_equal(["a","b",7,8,9,"c"], @array.insert(2,7,8,9))
   end

   def test_insert_negative_index
      assert_equal(["a","b",7,8,9,"c"], @array.insert(-2, 7, 8, 9))
   end

   # Using an index of -1 merely concatenates the arrays
   def test_insert_negative_index_append
      assert_equal(["a","b","c",7,8,9], @array.insert(-1, 7, 8, 9))
   end

   def test_insert_index_out_of_bounds
      assert_equal(["a","b","c",nil,nil,7], @array.insert(5, 7))
   end

   def test_expected_errors
      assert_raises(IndexError){ @array.insert(-9, 7) }
   end

   def teardown
      @array = nil
   end
end
