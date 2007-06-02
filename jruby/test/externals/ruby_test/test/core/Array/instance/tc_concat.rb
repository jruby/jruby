######################################################################
# tc_concat.rb
#
# Test suite for the Array#concat instance method.
######################################################################
require "test/unit"

class TC_Array_Concat_Instance < Test::Unit::TestCase
   def setup
      @array1 = [1,2,3]
      @array2 = [4,5,6]
   end

   def test_concat_basic
      assert_respond_to(@array1, :concat)
      assert_nothing_raised{ @array1.concat(@array2) }
   end

   def test_concat_results
      assert_equal([1,2,3,4,5,6], @array1.concat(@array2))
   end

   def test_concat_slice
      assert_equal([1,4,5,6], @array1[0,1].concat(@array2))
   end

   def test_concat_range
      assert_equal([1,2,4,5], @array1[0..1].concat(@array2[0,2]))
   end
  
   def test_concat_empty
      assert_equal([1,2,3], @array1.concat([]))
   end

   def test_expected_errors
      assert_raises(TypeError){ @array1.concat(1) }
      assert_raises(TypeError){ @array1.concat("foo") }
   end

   def teardown
      @array1 = nil
      @array2 = nil
   end
end
