##########################################################################
# tc_flatten.rb
#
# Test suite for the Array#flatten and Array#flatten! instance methods.
##########################################################################
require "test/unit"

class TC_Array_Flatten_Instance < Test::Unit::TestCase
   def setup
      @array = [1,[2,3,[4,5]]]
   end

   def test_flatten_basic
      assert_respond_to(@array, :flatten)
      assert_respond_to(@array, :flatten!)
      assert_nothing_raised{ @array.flatten }
      assert_nothing_raised{ @array.flatten! }
   end

   def test_flatten
      assert_equal([1,2,3,4,5], @array.flatten) # Non-destructive
      assert_equal([1,[2,3,[4,5]]], @array)     # Ensure original unmodified
      assert_equal([], [[],[],[]].flatten)
      assert_equal([1,2,3], [1,2,3].flatten)
   end

   def test_flatten_bang
      assert_equal([1,2,3,4,5], @array.flatten!)
      assert_equal([], [[],[],[]].flatten!)
      assert_equal([1,2,3,4,5], @array)
      assert_equal(nil, [1,2,3].flatten!)
   end

   def test_flatten_expected_errors
      assert_raises(ArgumentError){ @array.flatten(1) }
      assert_raises(ArgumentError){ @array.flatten!(1) }
   end

   def teardown
      @array = nil
   end
end
