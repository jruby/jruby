###########################################################
# tc_comparison.rb
#
# Test suite for the Array#<=> method.
###########################################################
require "test/unit"

class TC_Array_Comparison_Instance < Test::Unit::TestCase
   def setup
      @array1 = ["a", "a", "c"]
      @array2 = ["a", "b", "c"]
      @array3 = [1, 2, 3, 4, 5]
      @array4 = [1, 2]
      @array5 = [1, 2]

      @nested1 = [1, [1, 2], 3]
      @nested2 = [1, [1, 2], 3]
      @nested3 = [1, [1, 2, 3]]
   end

   def test_comparison_method_exists
      assert_respond_to(@array1, :<=>)
   end

   def test_comparison_basic
      assert_equal(-1, @array1 <=> @array2)
      assert_equal(1, @array3 <=> @array4)
      assert_equal(0, @array4 <=> @array5)
   end

   # Ensure floats, nested arrays, etc, compare properly.
   def test_comparison_advanced
      assert_equal(0, @nested1 <=> @nested2)
      assert_equal(1, @nested3 <=> @nested1)
      assert_equal(-1, @nested1 <=> @nested3)

      assert_equal(0,  [1] <=> [1.000])
      assert_equal(-1, [1] <=> [1.001])
      assert_equal(1,  [1] <=> [0.999])
   end

   def test_comparison_edge_cases
      assert_equal(0, [] <=> [])
      assert_nil([1,2,3] <=> [1,"two",3])
   end

   # Not all objects are guaranteed to be comparable.  They must define
   # a <=> method, or a NoMethodError is raised.
   def test_comparison_expected_errors
      assert_raises(NoMethodError){ [nil] <=> [nil] }
      assert_raises(NoMethodError){ [false] <=> [false] }
      assert_raises(NoMethodError){ [true] <=> [true] }

      assert_nothing_raised{ @array1.push(@array1) }
      
      # potentially hazardous
      #assert_raises(SystemStackError){ @array1 <=> @array1 }
   end

   def teardown
      @array1 = nil
      @array2 = nil
      @array3 = nil
      @array4 = nil
      @array5 = nil

      @nested1 = nil
      @nested2 = nil
      @nested3 = nil
   end
end
