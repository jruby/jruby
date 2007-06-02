##############################################
# tc_equality.rb
#
# Test suite for Array#== instance method.
##############################################
require "test/unit"

class TC_Array_Equality_Instance < Test::Unit::TestCase
   def setup
      @array1 = [1,2,3]
      @array2 = [1,2,3]
      @array3 = [3,1,2]
      @array4 = ["1","2","3"]
   end

   def test_basic
      assert_respond_to(@array1, :==)
      assert_nothing_raised{ @array1 == @array2 }
   end

   def test_equality_success
      assert_equal(true, @array1 == @array2)
   end

   def test_equality_failure
      assert_equal(false, @array1 == @array3)
      assert_equal(false, @array1 == @array4)
      assert_equal(false, @array1 == [])
      assert_equal(false, @array1 == nil)
      assert_equal(false, @array1 == 0)
   end

   def test_edge_cases
      assert_equal(false, [0] == 0)
      assert_equal(false, [nil] == nil)
      assert_equal(false, [true] == true)
      assert_equal(false, [false] == false)
   end

   def teardown
      @array1 = nil
      @array2 = nil
      @array3 = nil
      @array4 = nil
   end
end
