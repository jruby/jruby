###########################################################
# tc_to_ary.rb
#
# Test suite for the Array#to_ary instance method.
###########################################################
require "test/unit"

class TC_Array_ToAry_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3]
   end

   def test_to_ary_basic
      assert_respond_to(@array, :to_ary)
      assert_nothing_raised{ @array.to_ary }
   end
   
   def test_to_ary
      assert_equal(@array, @array.to_ary)
      assert_equal(@array.object_id, @array.to_ary.object_id)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.to_ary(1) }
   end

   def teardown
      @array = nil
   end
end
