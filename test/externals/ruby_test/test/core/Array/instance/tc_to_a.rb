###########################################################
# tc_to_a.rb
#
# Test suite for the Array#to_a instance method.
###########################################################
require "test/unit"

class FooArray < Array
   def to_ary
   end
end

class TC_Array_ToA_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3]
   end

   def test_to_a_basic
      assert_respond_to(@array, :to_a)
      assert_nothing_raised{ @array.to_a }
   end
   
   def test_to_a
      assert_equal(@array, @array.to_a)
      assert_equal(@array.object_id, @array.to_a.object_id)
   end

   def test_expected_errors
      assert_raises(ArgumentError){ @array.to_a(1) }
   end

   def teardown
      @array = nil
   end
end
