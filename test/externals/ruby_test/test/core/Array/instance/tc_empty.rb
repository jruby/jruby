###########################################################
# tc_empty.rb
#
# Test suite for the Array#empty? instance method.
###########################################################
require "test/unit"

class TC_Array_Empty_InstanceMethod < Test::Unit::TestCase
   def setup
      @array1 = [1,2,3]
      @array2 = []
      @array3 = [nil]
   end

   def test_empty_basic
      assert_respond_to(@array1, :empty?)
      assert_nothing_raised{ @array1.empty? }
   end

   def test_empty
      assert_equal(false, @array1.empty?)
      assert_equal(true, @array2.empty?)
      assert_equal(false, @array3.empty?)
   end

   def test_empty_expected_errors
      assert_raise(ArgumentError){ @array1.empty?(false) }
   end

   def teardown
      @array1 = nil
      @array2 = nil
      @array3 = nil
   end
end
