###########################################################
# tc_transpose.rb
#
# Test suite for the Array#transpose method.
###########################################################
require "test/unit"

class TC_Array_Transpose_Instance < Test::Unit::TestCase
   def setup
      @array = [["a","b"],["c","d"],["e","f"],["g","h"]]
   end

   def test_basic
      assert_respond_to(@array, :transpose)
      assert_nothing_raised{ @array.transpose }
   end

   def test_transpose
      assert_equal([["a","c","e","g"],["b","d","f","h"]], @array.transpose)
      assert_equal([["a","b"],["c","d"],["e","f"],["g","h"]], @array)
      assert_equal([["a","b"],["c","d"],["e","f"],["g","h"]], @array.transpose.transpose)
      assert_nothing_raised{ [[nil,nil],[false,true]].transpose }
   end

   def test_expected_errors
      assert_raises(TypeError){ [1,2,3].transpose }
      assert_raises(IndexError){ [[1,2],[3]].transpose }
   end

   def teardown
      @array = nil
   end
end
