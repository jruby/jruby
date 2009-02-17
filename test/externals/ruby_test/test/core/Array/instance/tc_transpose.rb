#############################################################################
# tc_transpose.rb
#
# Test suite for the Array#transpose method. These tests are skipped in the
# Amber implementation, via the Rakefile.
#############################################################################
require "test/unit"

class TC_Array_Transpose_InstanceMethod < Test::Unit::TestCase
   class ATranspose < Array
      def to_ary
         [[1,2], [3,4]]
      end
   end

   def setup
      @array  = [["a","b"],["c","d"],["e","f"],["g","h"]]
      @custom = ATranspose.new
   end

   def test_basic
      assert_respond_to(@array, :transpose)
      assert_nothing_raised{ @array.transpose }
   end

   def test_transpose
      assert_equal([["a","c","e","g"],["b","d","f","h"]], @array.transpose)
      assert_equal([["a","b"],["c","d"],["e","f"],["g","h"]], @array)
      assert_equal([["a","b"],["c","d"],["e","f"],["g","h"]], @array.transpose.transpose)
   end

   def test_transpose_custom_to_ary
      assert_nothing_raised{ @custom.transpose }
      # assert_equal([[1,3], [2,4]], @custom.transpose) # Undefined. TODO: Revisit.
   end

   def test_transpose_edge_cases
      assert_equal([], [[], []].transpose)
      assert_equal([[nil, false], [nil, true]], [[nil, nil], [false, true]].transpose) 
   end

   def test_expected_errors
      assert_raise(TypeError){ [1,2,3].transpose }
      assert_raise(IndexError){ [[1,2],[3]].transpose }
      assert_raise(ArgumentError){ @array.transpose(1) }
   end

   def teardown
      @array  = nil
      @custom = nil
   end
end
