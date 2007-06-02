#########################################################################
# tc_collect.rb
#
# Test suite for the Array#collect and Array#collect! instance methods.
#########################################################################
require "test/unit"

class TC_Array_Collect_Instance < Test::Unit::TestCase
   def setup
      @array = [1, 2, 3] 
   end

   def test_collect_basic
      assert_respond_to(@array, :collect)
      assert_respond_to(@array, :collect!)
   end

   def test_collect_results
      assert_equal([2,3,4], @array.collect{ |e| e += 1 })
      assert_equal([7,7,7], @array.collect{ 7 })
      assert_equal([1,2,3], @array)
   end

   def test_collect_bang_results
      assert_equal([2,3,4], @array.collect!{ |e| e += 1 })
      assert_equal([7,7,7], @array.collect!{ 7 })
      assert_equal([7,7,7], @array)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.collect(5) }
      assert_raises(ArgumentError){ @array.collect!(5) }
   end

   def teardown
      @array = nil
   end
end
