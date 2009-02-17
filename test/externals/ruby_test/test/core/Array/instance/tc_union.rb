############################################################################
# tc_union.rb
#
# Test suite for the Array#| instance method. We add a custom class with
# its own to_ary method here to verify that it's handled by the Array#|
# method as well.
############################################################################
require "test/unit"

class TC_Array_Union_Instance < Test::Unit::TestCase
   class AUnion
      def to_ary
         [4, 5, 6]
      end
   end

   def setup
      @array1 = [1, 2, 3]
      @array2 = [3, 4, 5]
      @array3 = [4, nil, false]
      @custom = AUnion.new
   end

   def test_union_basic
      assert_respond_to(@array1, :|)
      assert_nothing_raised{ @array1 | @array2 }
      assert_nothing_raised{ @array1 | @array2 | @array3 }
   end

   def test_union
      assert_equal([1, 2, 3, 4, 5], @array1 | @array2)
      assert_equal([1, 2, 3, 4, nil, false], @array1 | @array3)
   end

   def test_union_custom_to_ary
      assert_nothing_raised{ @array2 | @custom }
      assert_equal([3, 4, 5, 6], @array2 | @custom)
   end

   def test_union_edge_cases
      assert_equal([], [] | [])
      assert_equal([nil], [nil, nil, nil] | [nil, nil, nil])
      assert_equal([nil, false], [nil, false, nil] | [false, nil, nil])
   end

   def test_union_expected_errors
      assert_raises(TypeError){ @array1 | 1 }
      assert_raises(TypeError){ @array1 | nil }
      assert_raises(TypeError){ @array1 | true }
      assert_raises(TypeError){ @array1 | false }
      assert_raises(TypeError){ @array1 | "hello" }
   end
   
   def teardown
      @array1 = nil
      @array2 = nil
      @array3 = nil
      @custom = nil
   end
end
