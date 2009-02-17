###############################################################################
# tc_concat.rb
#
# Test suite for the Array#concat instance method. I've added a class with
# a custom to_ary method to ensure that it behaves properly with Array#concat.
###############################################################################
require "test/unit"

class TC_Array_Concat_Instance < Test::Unit::TestCase
   class AConcat
      def to_ary
         ['a', 'b', 'c']
      end
   end

   def setup
      @array1 = [1,2,3]
      @array2 = [4,5,6]
      @custom = AConcat.new
   end

   def test_concat_basic
      assert_respond_to(@array1, :concat)
      assert_nothing_raised{ @array1.concat(@array2) }
   end

   def test_concat_results
      assert_equal([1, 2, 3, 4, 5, 6], @array1.concat(@array2))
      assert_equal([1, 2, 3, 4, 5, 6, 'a', 'b', 'c'], @array1.concat(@custom))
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
      @custom = nil
   end
end
