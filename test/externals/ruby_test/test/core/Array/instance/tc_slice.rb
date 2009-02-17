#####################################################################
# tc_slice.rb
#
# Test suite for the Array#slice and Array#slice! instance methods.
#####################################################################
require "test/unit"

class TC_Array_Slice_InstanceMethod < Test::Unit::TestCase
   def setup
      @array = %w/a b c d e/
   end

   def test_basic
      assert_respond_to(@array, :slice)
      assert_respond_to(@array, :slice!)
      assert_nothing_raised{ @array.slice(1) }
      assert_nothing_raised{ @array.slice(1,2) }
      assert_nothing_raised{ @array.slice(1..2) }
      assert_nothing_raised{ @array.slice!(1) }
      assert_nothing_raised{ @array.slice!(1,2) }
      assert_nothing_raised{ @array.slice!(1..2) }
   end

   def test_slice
      assert_equal("c", @array.slice(2))
      assert_equal("e", @array.slice(-1))
      assert_equal(["c","d"], @array.slice(2,2))
      assert_equal(["c","d","e"], @array.slice(2..4))
      assert_equal(["a","b","c","d","e"], @array)
      assert_equal(nil, @array.slice(99))
   end

   def test_slice_bang
      assert_equal("c", @array.slice!(2))
      assert_equal(["a","b","d","e"], @array)
      assert_equal(["d","e"], @array.slice!(2,2))
      assert_equal(["a","b"], @array)
      assert_equal(["a","b"], @array.slice!(0..1))
      assert_equal([], @array)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.slice(1,2,3) }
      assert_raises(TypeError){ @array.slice(true) }
      assert_raises(TypeError){ @array.slice(2, nil) }
   end

   def teardown
      @array = nil
   end
end
