###########################################################
# tc_push.rb
#
# Test suite for the Array#push instance method.
###########################################################
require "test/unit"

class TC_Array_Push_Instance < Test::Unit::TestCase
   def setup
      @array = [1,2,3]
   end

   def test_push_basic
      assert_respond_to(@array, :push)
      assert_nothing_raised{ @array.push(1) }
      assert_nothing_raised{ @array.push(1, "a") }
   end

   def test_push
      assert_equal([1,2,3,4], @array.push(4))
      assert_equal([1,2,3,4,"five"], @array.push("five"))
      assert_equal([1,2,3,4,"five",nil], @array.push(nil))
      assert_equal([1,2,3,4,"five",nil,false,true], @array.push(false,true))
   end

   def test_push_infinity
      @array.push(@array)
      assert_equal(1, @array[0])
      assert_equal(2, @array[1])
      assert_equal(3, @array[2])
      assert_equal(@array, @array[3])
   end

   def teardown
      @array = nil
   end
end
