###########################################################
# tc_union.rb
#
# Test suite for the Array#| instance method.
###########################################################
require "test/unit"

class TC_Array_Union_Instance < Test::Unit::TestCase
   def setup
      @array1 = [1, 2, 3]
      @array2 = [3, 4, 5]
      @array3 = [4, nil, false]
   end

   def test_union_exists
      assert_respond_to(@array1, :|)
   end

   def test_union_basic
      assert_nothing_raised{ @array1 | @array2 }
      assert_nothing_raised{ @array1 | @array2 | @array3 }

      assert_equal([1, 2, 3, 4, 5], @array1 | @array2)
      assert_equal([1, 2, 3, 4, nil, false], @array1 | @array3)
   end

   def test_union_edge_cases
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
   end
end
