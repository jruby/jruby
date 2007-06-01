#################################################################
# tc_compact.rb
#
# Test suite for the Array#compact instance and Array#compact!
# instance methods.
#################################################################
require "test/unit"

class TC_Array_Compact_Instance < Test::Unit::TestCase
   def setup
      @array1 = [1, "two", nil, false, nil]
      @array2 = [0, 1, 2, 3]
   end

   def test_compact_basic
      assert_respond_to(@array1, :compact)
      assert_respond_to(@array1, :compact!)
      assert_nothing_raised{ @array1.compact }
      assert_nothing_raised{ @array1.compact! }
   end

   def test_compact_return_values
      assert_equal([1,"two",false], @array1.compact)
      assert_equal([0,1,2,3], @array2.compact)
   end

   def test_compact_bang_return_values
      assert_equal([1,"two",false], @array1.compact!)
      assert_equal(nil, @array2.compact!)
   end

   def test_compact_returns_copy
      assert_nothing_raised{ @array1.compact }
      assert_equal([1, "two", nil, false, nil], @array1)
   end

   def test_compact_bang_modifies_receiver
      assert_nothing_raised{ @array1.compact! }
      assert_equal([1, "two", false], @array1)
   end

   def test_compact_expected_errors
      assert_raises(ArgumentError){ @array1.compact(1) }
      assert_raises(ArgumentError){ @array1.compact!(1) }
   end

   def teardown
      @array1 = nil
      @array2 = nil
   end
end
