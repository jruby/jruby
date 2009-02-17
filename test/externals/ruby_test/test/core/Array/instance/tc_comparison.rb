###############################################################################
# tc_comparison.rb
#
# Test suite for the Array#<=> method. Note that I've added a custom class
# with its own to_ary method to ensure that Array#<=> responds to it properly.
###############################################################################
require "test/unit"

class TC_Array_Comparison_Instance < Test::Unit::TestCase
   class ACompare
      def to_ary
         ['a', 'b', 'c']
      end
   end

   def setup
      @array_chr1 = ['a', 'a', 'c']
      @array_chr2 = ['a', 'b', 'c']
      @array_int1 = [1, 2, 3, 4, 5]
      @array_int2 = [1, 2]
      @array_int3 = [1, 2]

      @nested1 = [1, [1, 2], 3]
      @nested2 = [1, [1, 2], 3]
      @nested3 = [1, [1, 2, 3]]

      @custom = ACompare.new
   end


   def test_comparison_method_exists
      assert_respond_to(@array_chr1, :<=>)
   end

   def test_comparison_basic
      assert_equal(-1, @array_chr1 <=> @array_chr2)
      assert_equal(1, @array_int1 <=> @array_int2)
      assert_equal(0, @array_int2 <=> @array_int3)
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

   def test_comparison_against_custom_to_ary_method
      assert_nothing_raised{ @array_chr1 <=> @custom }
      assert_equal(0, @array_chr2 <=> @custom)
   end

   # Not all objects are guaranteed to be comparable.  They must define
   # a <=> method, or a NoMethodError is raised.
   def test_comparison_expected_errors
      assert_raises(NoMethodError){ [nil] <=> [nil] }
      assert_raises(NoMethodError){ [false] <=> [false] }
      assert_raises(NoMethodError){ [true] <=> [true] }

      assert_nothing_raised{ @array_chr1.push(@array_chr1) }
      
      # potentially hazardous
      #assert_raises(SystemStackError){ @array_chr1 <=> @array_chr1 }
   end

   def teardown
      @array_chr1 = nil
      @array_chr2 = nil
      @array_int1 = nil
      @array_int2 = nil
      @array_int3 = nil

      @nested1 = nil
      @nested2 = nil
      @nested3 = nil
   end
end
