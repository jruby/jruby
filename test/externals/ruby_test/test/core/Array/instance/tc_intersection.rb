###########################################################
# tc_intersection.rb
#
# Test suite for the Array#& instance method.
###########################################################
require "test/unit"

class TC_Array_Intersection_InstanceMethod < Test::Unit::TestCase
   def setup
      @array1 = [1, 2, 2, 3, "hello", "world", 4, 5, 6, nil, false]
      @array2 = [2, 3, "hello"]
      @array3 = [2, nil, false, true]
      @nested1 = [[1,2], ['a','b'], ['c','d']]
      @nested2 = [[1,2], ['a','z'], ['c','d']]
      @nested3 = [['a','z'], ['c','d'], [2,1]]
   end

   def test_intersection_exists
      assert_respond_to(@array1, :&)
   end

   def test_intersection_basic
      assert_nothing_raised{ @array1 & @array2 }
      assert_nothing_raised{ @array1 & @array2 & @array3}
      assert_nothing_raised{ @array1 & [1,2,3] }
   end

   def test_intersection
      assert_equal([2, 3, "hello"], @array1 & @array2)
      assert_equal([2], @array1 & @array2 & @array3)
      assert_equal([1,2,3,'hello','world',4,5,6,nil,false], @array1 & @array1)
      assert_equal([], @array1 & [])
   end

   def test_intersection_nested
      assert_equal([[1,2],['c','d']], @nested1 & @nested2)
      assert_equal([['a','z'], ['c','d']], @nested2 & @nested3)
   end

   def test_intersection_edge_cases
      assert_equal([2, nil, false], @array1 & @array3)
      assert_equal([], [] & [])
      assert_equal([], [] & [0])
      assert_equal([], [] & [nil])
      assert_equal([nil], [nil] & [nil, 1])
      assert_equal([[]], [[],[]] & [[],[],[]])
   end

   def test_intersection_expected_errors
      assert_raises(TypeError){ @array1 & 1 }
      assert_raises(TypeError){ @array1 & nil }
      assert_raises(TypeError){ @array1 & true }
      assert_raises(TypeError){ @array1 & false }
      assert_raises(TypeError){ @array1 & "hello" }
      assert_raises(ArgumentError){ @array1.send(:&, @array2, @array3) }
   end
   
   def teardown
      @array1  = nil
      @array2  = nil
      @array3  = nil
      @nested1 = nil
      @nested2 = nil
      @nested3 = nil
   end
end
